import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PedidosServer {
    private static final int PORTA = 12345;
    private static Map<String, Map<String, Object>> estoque = new ConcurrentHashMap<>();
    private static Map<String, Map<String, Object>> pedidos = new ConcurrentHashMap<>();

    static {
        // Inicializa o estoque
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

    public static void main(String[] args) {
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
                            System.out.println("Mensagem recebida: " + inputLine);
                            
                            // Processa o pedido (implementação simplificada)
                            String response = processarPedido(inputLine);
                            out.println(response);
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

    private static synchronized String processarPedido(String pedidoStr) {
        // Formato esperado: QUISOUE_ID|PRODUTO_ID:QUANTIDADE,PRODUTO_ID:QUANTIDADE|METODO_PAGAMENTO
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
            // Verifica estoque e calcula total
            for (String item : itens) {
                String[] itemParts = item.split(":");
                String produtoId = itemParts[0];
                int quantidade = Integer.parseInt(itemParts[1]);
                
                Map<String, Object> produto = estoque.get(produtoId);
                if (produto == null || (int)produto.get("quantidade") < quantidade) {
                    return "ERRO|Estoque insuficiente para o produto " + produtoId;
                }
                
                total += (double)produto.get("preco") * quantidade;
            }
            
            // Atualiza estoque
            for (String item : itens) {
                String[] itemParts = item.split(":");
                String produtoId = itemParts[0];
                int quantidade = Integer.parseInt(itemParts[1]);
                
                Map<String, Object> produto = estoque.get(produtoId);
                produto.put("quantidade", (int)produto.get("quantidade") - quantidade);
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