import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class PedidosServer {
    private static final int PORT = 12345;
    private static final Map<String, Integer> estoque = new ConcurrentHashMap<>();
    private static final List<PrintWriter> clientesConectados = Collections.synchronizedList(new ArrayList<>());
    private static final Map<String, String> snapshotsRecebidos = new ConcurrentHashMap<>();
    private static int relogioLamport = 0;

    public static void main(String[] args) {
        estoque.put("Hambúrguer", 30);
        estoque.put("Refrigerante", 40);
        estoque.put("Batata Frita", 25);
        estoque.put("Suco de laranja", 20);

        System.out.println("🖥️ Servidor aguardando conexões na porta " + PORT + "...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            // Thread separada para permitir comando manual para snapshot
            new Thread(() -> {
                Scanner scanner = new Scanner(System.in);
                while (true) {
                    System.out.print("\nDigite 'snapshot' para capturar estado global: ");
                    String cmd = scanner.nextLine();
                    if (cmd.equalsIgnoreCase("snapshot")) {
                        capturarSnapshot();
                    }
                }
            }).start();

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("🔌 Cliente conectado: " + socket.getInetAddress());
                new Thread(new ClientHandler(socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void capturarSnapshot() {
        relogioLamport++;
        System.out.println("\n⏱️ Iniciando snapshot global. Lamport: " + relogioLamport);
        System.out.println("📦 Estado local do servidor (estoque): " + estoque);

        synchronized (clientesConectados) {
            snapshotsRecebidos.clear();
            for (PrintWriter cliente : clientesConectados) {
                cliente.println("MARKER|LAMPORT:" + relogioLamport);
            }
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private PrintWriter out;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                out = new PrintWriter(socket.getOutputStream(), true);
                clientesConectados.add(out);

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    System.out.println("📩 Mensagem recebida: " + inputLine);

                    if (inputLine.startsWith("SNAPSHOT_CLIENTE|")) {
                        System.out.println("📥 Snapshot do cliente recebido:");
                        System.out.println(inputLine.replace("SNAPSHOT_CLIENTE|", ""));
                    } else if (inputLine.equalsIgnoreCase("SNAPSHOT")) {
                        capturarSnapshot();
                    } else if (inputLine.startsWith("PEDIDO|")) {
                        String pedidoMsg = inputLine.substring(7); // Remove "PEDIDO|"
                        processarPedido(pedidoMsg);
                    } else {
                        out.println("FALHA|Comando inválido|LAMPORT:" + relogioLamport);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (out != null) {
                    clientesConectados.remove(out);
                    out.close();
                }
                try {
                    socket.close();
                    System.out.println("🔌 Cliente desconectado.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void processarPedido(String pedidoMsg) {
            String[] partes = pedidoMsg.split("\\|");
            if (partes.length < 3) {
                out.println("FALHA|Formato inválido|LAMPORT:" + relogioLamport);
                return;
            }

            String quiosqueId = partes[0].trim();
            String itensStr = partes[1].trim(); // Ex: "1001:2,1002:1"
            String metodoPagamento = partes[2].trim();

            // Extrai Lamport se existir
            int lamportRecebido = 0;
            for (String parte : partes) {
                if (parte.startsWith("LAMPORT:")) {
                    try {
                        lamportRecebido = Integer.parseInt(parte.substring(8).trim());
                    } catch (NumberFormatException e) {
                        System.out.println("⚠️ Erro ao parsear Lamport: " + e.getMessage());
                    }
                    break;
                }
            }
            // Atualiza relógio Lamport do servidor
            synchronized (PedidosServer.class) {
                relogioLamport = Math.max(relogioLamport, lamportRecebido) + 1;
            }

            // Parseia itens e quantidades
            Map<String, Integer> itensPedido = new HashMap<>();
            try {
                String[] itensArray = itensStr.split(",");
                for (String itemQtd : itensArray) {
                    String[] partesItem = itemQtd.split(":");
                    if (partesItem.length != 2)
                        throw new IllegalArgumentException("Formato item inválido: " + itemQtd);

                    String codigo = partesItem[0].trim();
                    int qtd = Integer.parseInt(partesItem[1].trim());
                    itensPedido.put(codigo, qtd);
                }
            } catch (Exception e) {
                out.println("FALHA|Formato de itens inválido|LAMPORT:" + relogioLamport);
                return;
            }

            // Verifica estoque (synchronizado para evitar condições de corrida)
            synchronized (estoque) {
                boolean estoqueSuficiente = true;
                for (Map.Entry<String, Integer> entry : itensPedido.entrySet()) {
                    String codigo = entry.getKey();
                    int qtd = entry.getValue();
                    Integer estoqueAtual = estoque.get(codigo);
                    if (estoqueAtual == null || estoqueAtual < qtd) {
                        estoqueSuficiente = false;
                        break;
                    }
                }

                if (!estoqueSuficiente) {
                    out.println("FALHA|Estoque insuficiente|LAMPORT:" + relogioLamport);
                    System.out.println("❌ Pedido recusado por falta de estoque para quiosque " + quiosqueId);
                    return;
                }

                // Atualiza estoque subtraindo os itens
                for (Map.Entry<String, Integer> entry : itensPedido.entrySet()) {
                    String codigo = entry.getKey();
                    int qtd = entry.getValue();
                    estoque.put(codigo, estoque.get(codigo) - qtd);
                }
            }

            // Simula cálculo de total (exemplo fixo, você pode adaptar)
            double total = 0.0;
            Map<String, Double> precos = Map.of(
                    "1001", 15.90,
                    "1002", 7.50,
                    "1003", 12.00,
                    "1004", 5.00,
                    "Coxinha", 5.00,
                    "Pastel", 4.50,
                    "Refrigerante", 3.00);
            for (Map.Entry<String, Integer> entry : itensPedido.entrySet()) {
                String codigo = entry.getKey();
                int qtd = entry.getValue();
                total += precos.getOrDefault(codigo, 0.0) * qtd;
            }

            String pedidoId = "PED" + System.currentTimeMillis();
            String totalStr = String.format("%.2f", total).replace('.', ',');

            out.println("OK|Pedido registrado com sucesso|" + pedidoId + "|" + totalStr + "|" + metodoPagamento
                    + "|LAMPORT:" + relogioLamport);
            System.out.println("✅ Pedido confirmado para quiosque " + quiosqueId + ", total R$ " + totalStr);
            System.out.println("📦 Estoque atualizado: " + estoque);
        }
    }

}
