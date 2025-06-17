# 🍔 Sistema de Pedidos para Quiosques de Alimentação

Sistema distribuído cliente-servidor para gerenciamento de pedidos e pagamentos em quiosques de praça de alimentação.

## 📌 Funcionalidades

- **Cadastro de pedidos** com múltiplos itens
- **Controle de estoque** em tempo real
- **Múltiplos métodos de pagamento** (cartão, dinheiro, vale-refeição)
- **Multiplos quiosques** conectados simultaneamente
- **Protocolo de comunicação** simples e eficiente

## 🛠 Tecnologias

- **Java 11+**
- **Sockets TCP** para comunicação em rede
- **Programação concorrente** (Thread-per-request)
- **Protocolo textual** com delimitadores

## 👨‍💻 Como Usar

- **No CMD digite:
- **cd C:\<local dessa pasta>\src2
- **No Primeiro Prompt (Server):
- **java PedidosServer

- **No Segundo Prompt (Cliente):
- **cd C:\<local dessa pasta>\src2
- **java QuiosqueCliente
- ID do Quiosque:12345

- **No Terceiro Prompt (Cliente):
- **cd C:\<local dessa pasta>\src2
- **java QuiosqueCliente2
- ID do Quiosque:12345

