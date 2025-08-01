import java.io.*;
import java.net.*;
import java.util.Scanner;

public class QuiosqueCliente {
    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 12345;
    private static int relogioLamport = 0;
    private static String ultimoPedido = "Nenhum pedido feito ainda";
    private static boolean snapshotEmAndamento = false;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Sistema de Pedidos - Quiosque");
        System.out.print("Informe o ID do quiosque: ");
        String quiosqueId = scanner.nextLine();

        try (
                Socket socket = new Socket(SERVER_IP, SERVER_PORT);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            // Thread para escutar mensagens do servidor
            new Thread(() -> {
                try {
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        if (inputLine.startsWith("MARKER|LAMPORT:")) {
                            int tsRecebido = Integer.parseInt(inputLine.split(":")[1]);
                            relogioLamport = Math.max(relogioLamport, tsRecebido) + 1;

                            if (!snapshotEmAndamento) {
                                snapshotEmAndamento = true;

                                String snapshot = "SNAPSHOT_CLIENTE|" +
                                        "QuiosqueID:" + quiosqueId +
                                        "|RelogioLamport:" + relogioLamport +
                                        "|Estado:" + ultimoPedido;

                                out.println(snapshot);
                                System.out.println("Snapshot enviado ao servidor.");
                            }
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Erro na escuta do servidor: " + e.getMessage());
                }
            }).start();

            // Loop principal do menu
            while (true) {
                System.out.println("\nOpções:");
                System.out.println("1 - Fazer novo pedido");
                System.out.println("2 - Sair");
                System.out.print("Escolha: ");

                String opcao = scanner.nextLine();

                if (opcao.equals("2")) {
                    System.out.println("Saindo...");
                    break;
                } else if (opcao.equals("1")) {
                    fazerPedido(quiosqueId, scanner, out, in);
                } else {
                    System.out.println("Opção inválida!");
                }
            }

        } catch (IOException e) {
            System.err.println("Erro ao conectar com servidor: " + e.getMessage());
        }

        scanner.close();
    }

    private static void fazerPedido(String quiosqueId, Scanner scanner, PrintWriter out, BufferedReader in) {
        System.out.println("\nItens disponíveis:");
        System.out.println("1001 - Hambúrguer - R$ 15.90");
        System.out.println("1002 - Refrigerante - R$ 7.50");
        System.out.println("1003 - Batata Frita - R$ 12.00");
        System.out.println("1004 - Suco de laranja - R$ 5.00");

        System.out.print("\nItens (ex: 1001:2,1002:1): ");
        String itens = scanner.nextLine();

        System.out.println("\nMétodos de pagamento:");
        System.out.println("1 - Cartão de Crédito");
        System.out.println("2 - Cartão de Débito");
        System.out.println("3 - Dinheiro");
        System.out.println("4 - Vale Refeição");
        System.out.print("Método: ");
        String metodo = scanner.nextLine();

        String metodoPagamento;
        switch (metodo) {
            case "1":
                metodoPagamento = "CARTAO_CREDITO";
                break;
            case "2":
                metodoPagamento = "CARTAO_DEBITO";
                break;
            case "3":
                metodoPagamento = "DINHEIRO";
                break;
            case "4":
                metodoPagamento = "VALE_REFEICAO";
                break;
            default:
                System.out.println("Método inválido!");
                return;
        }

        // Incrementa o relógio Lamport local antes de enviar
        relogioLamport++;

        // Monta mensagem no formato completo e correto
        String mensagemPedido = String.format("PEDIDO|%s|%s|%s|LAMPORT:%d",
                quiosqueId, itens, metodoPagamento, relogioLamport);

        out.println(mensagemPedido);

        try {
            String resposta = in.readLine();
            if (resposta == null) {
                System.out.println("Servidor desconectado.");
                return;
            }

            String[] partesResposta = resposta.split("\\|LAMPORT:");
            int lamportRecebido = relogioLamport;
            if (partesResposta.length > 1) {
                lamportRecebido = Integer.parseInt(partesResposta[1]);
            }
            relogioLamport = Math.max(relogioLamport, lamportRecebido) + 1;

            String[] partes = partesResposta[0].split("\\|");
            if (partes[0].equalsIgnoreCase("OK")) {
                System.out.println("✅ Pedido realizado com sucesso!");
                System.out.println("Número do pedido: " + partes[2]);
                System.out.println("Total: R$ " + partes[3]);
                System.out.println("Pagamento: " + partes[4]);

                ultimoPedido = "PedidoID: " + partes[2] + ", Itens: " + itens + ", Total: " + partes[3];
            } else {
                System.out.println("Erro no pedido: " + partes[1]);
            }

        } catch (IOException e) {
            System.out.println("Erro ao receber resposta: " + e.getMessage());
        }
    }
}
