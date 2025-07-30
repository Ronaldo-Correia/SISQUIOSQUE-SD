import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PedidosServer {
    private static final int PORTA = 12345;
    private static Map<String, Map<String, Object>> estoque = new ConcurrentHashMap<>();
    private static Map<String, Map<String, Object>> pedidos = new ConcurrentHashMap<>();

    private static int relogioLamport = 0; // Relógio de Lamport

    static {
        Map<String, Object> produto1 = new HashMap<>();
        produto1.put("nome", "Hambúrguer");
        produto1.put("preco", 15.90);
        produto1.put("quantidade", 50);

        Map<String, Object> produto2 = new HashMap<>();
        produto2.put("nome", "Refrigerante");
        produto2.put("preco", 7.50);
        produto2.put("quantidade", 100);

        Map<String, Object> produto3 = new HashMap<>();
        produto3.put("nome", "Batata Frita");
        produto3.put("preco", 12.00);
        produto3.put("quantidade", 30);

        Map<String, Object> produto4 = new HashMap<>();
        produto4.put("nome", "Suco de laranja");
        produto4.put("preco", 5.00);
        produto4.put("quantidade", 30);

        estoque.put("1001", produto1);
        estoque.put("1002", produto2);
        estoque.put("1003", produto3);
        estoque.put("1004", produto4);
    }

    private static synchronized void incrementarRelogio() {
        relogioLamport++;
    }

    private static synchronized void atualizarRelogio(int recebido) {
        relogioLamport = Math.max(relogioLamport, recebido) + 1;
    }

    public static void main(String[] args) {
        new Thread(() -> {
            BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                try {
                    String comando = console.readLine();
                    if ("SNAPSHOT".equalsIgnoreCase(comando)) {
                        incrementarRelogio(); 
                        capturarSnapshot();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        try (ServerSocket serverSocket = new ServerSocket(PORTA)) {
            System.out.println("Servidor de pedidos iniciado na porta " + PORTA);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Novo cliente conectado: " + clientSocket.getInetAddress());

                new Thread(() -> {
                    try (
                        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                    ) {
                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            String[] partesMsg = inputLine.split("\\|LAMPORT:");
                            String mensagem = partesMsg[0];
                            int timestampRecebido = 0;
                            if (partesMsg.length > 1) {
                                try {
                                    timestampRecebido = Integer.parseInt(partesMsg[1]);
                                } catch (NumberFormatException e) {
                                }
                            }
                            atualizarRelogio(timestampRecebido); 
                            System.out.println("Mensagem recebida: " + mensagem + " | Lamport: " + relogioLamport);

                            String response = processarPedido(mensagem);

                            incrementarRelogio(); 
                            out.println(response + "|LAMPORT:" + relogioLamport);
                        }
                    } catch (IOException e) {
                        System.err.println("Erro na comunicação com cliente: " + e.getMessage());
                    } finally {
                        try {
                            clientSocket.close();
                        } catch (IOException e) {
                            System.err.println("Erro ao fechar socket: " + e.getMessage());
                        }
                    }
                }).start();
            }
        } catch (IOException e) {
            System.err.println("Erro ao iniciar servidor: " + e.getMessage());
        }
    }

    public static void capturarSnapshot() {
        System.out.println("----- SNAPSHOT -----");
        System.out.println("Relógio de Lamport: " + relogioLamport);
        System.out.println("Estoque:");
        for (Map.Entry<String, Map<String, Object>> entry : estoque.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
        System.out.println("Pedidos:");
        for (Map.Entry<String, Map<String, Object>> entry : pedidos.entrySet()) {
            String pedidoId = entry.getKey();
            Map<String, Object> pedido = entry.getValue();
            Object itensObj = pedido.get("itens");
            String itensFormatados = "";
            if (itensObj instanceof String[]) {
                itensFormatados = String.join(", ", (String[]) itensObj);
            } else if (itensObj instanceof Object[]) {
                Object[] objArr = (Object[]) itensObj;
                String[] itensStr = Arrays.copyOf(objArr, objArr.length, String[].class);
                itensFormatados = String.join(", ", itensStr);
            } else if (itensObj != null) {
                itensFormatados = itensObj.toString();
            }
            System.out.println(pedidoId + ": {itens=" + itensFormatados
                    + ", total=" + pedido.get("total")
                    + ", status=" + pedido.get("status")
                    + ", metodo_pagamento=" + pedido.get("metodo_pagamento") + "}");
        }
        System.out.println("--------------------");
    }

    private static synchronized String processarPedido(String pedidoStr) {
        // Formato esperado:
        // QUISOUE_ID|PRODUTO_ID:QUANTIDADE,PRODUTO_ID:QUANTIDADE|METODO_PAGAMENTO
        String[] partes = pedidoStr.split("\\|");
        if (partes.length != 3) {
            return "ERRO|Formato de pedido inválido";
        }

        String quiosqueId = partes[0];
        String[] itens = partes[1].split(",");
        String metodoPagamento = partes[2];

        double total = 0;
        StringBuilder mensagem = new StringBuilder();

        try {
            for (String item : itens) {
                String[] itemParts = item.split(":");
                String produtoId = itemParts[0];
                int quantidade = Integer.parseInt(itemParts[1]);

                Map<String, Object> produto = estoque.get(produtoId);
                if (produto == null || (int) produto.get("quantidade") < quantidade) {
                    return "ERRO|Estoque insuficiente para o produto " + produtoId;
                }

                total += (double) produto.get("preco") * quantidade;
            }

            // Atualiza estoque
            for (String item : itens) {
                String[] itemParts = item.split(":");
                String produtoId = itemParts[0];
                int quantidade = Integer.parseInt(itemParts[1]);

                Map<String, Object> produto = estoque.get(produtoId);
                produto.put("quantidade", (int) produto.get("quantidade") - quantidade);
            }

            // Registra pedido
            String pedidoId = "PED" + (pedidos.size() + 1);
            Map<String, Object> novoPedido = new HashMap<>();
            novoPedido.put("itens", itens);
            novoPedido.put("total", total);
            novoPedido.put("status", "AGUARDANDO_PAGAMENTO");
            novoPedido.put("metodo_pagamento", metodoPagamento);
            pedidos.put(pedidoId, novoPedido);

            mensagem.append("OK|Pedido registrado com sucesso|")
                    .append(pedidoId).append("|")
                    .append(String.format("%.2f", total)).append("|")
                    .append(metodoPagamento);

            return mensagem.toString();
        } catch (Exception e) {
            return "ERRO|" + e.getMessage();
        }
    }
}