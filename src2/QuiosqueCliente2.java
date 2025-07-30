import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class QuiosqueCliente2 {
    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Sistema de Pedidos - Quiosque");
        System.out.print("Informe o ID do quiosque: ");
        String quiosqueId = scanner.nextLine();

        while (true) {
            System.out.println("\nOpções:");
            System.out.println("1 - Fazer novo pedido");
            System.out.println("2 - Sair");
            System.out.print("Escolha: ");

            String opcao = scanner.nextLine();

            if (opcao.equals("2")) {
                break;
            } else if (opcao.equals("1")) {
                fazerPedido(quiosqueId, scanner);
            } else {
                System.out.println("Opção inválida!");
            }
        }

        scanner.close();
    }

    private static void fazerPedido(String quiosqueId, Scanner scanner) {
        System.out.println("\nItens disponíveis:");
        System.out.println("1001 - Hambúrguer - R$ 15.90");
        System.out.println("1002 - Refrigerante - R$ 7.50");
        System.out.println("1003 - Batata Frita - R$ 12.00");
        System.out.println("1004 - Suco de laranja - R$ 5.00");

        System.out.println("\nDigite os itens no formato CODIGO:QUANTIDADE separados por vírgula");
        System.out.println("Exemplo: 1001:2,1002:1");
        System.out.print("Itens: ");
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

        String mensagemPedido = quiosqueId + "|" + itens + "|" + metodoPagamento;

        try (
                Socket socket = new Socket(SERVER_IP, SERVER_PORT);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));) {
            out.println(mensagemPedido);

            String resposta = in.readLine();
            System.out.println("\nResposta do servidor: " + resposta);

            String[] partes = resposta.split("\\|");
            if (partes[0].equals("OK")) {
                System.out.println("Pedido realizado com sucesso!");
                System.out.println("Número do pedido: " + partes[2]);
                System.out.println("Total: R$ " + partes[3]);
                System.out.println("Método de pagamento: " + partes[4]);
            } else {
                System.out.println("Erro no pedido: " + partes[1]);
            }
        } catch (UnknownHostException e) {
            System.err.println("Host desconhecido: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Erro de I/O: " + e.getMessage());
        }
    }
}