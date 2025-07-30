public class RelogioLamport {
    private int relogio;

    public RelogioLamport() {
        this.relogio = 0;
    }

    // Evento interno
    public void eventoInterno() {
        relogio++;
    }

    // Enviar mensagem
    public int enviarMensagem() {
        relogio++;
        return relogio;
    }

    // Receber mensagem
    public void receberMensagem(int timestampRecebido) {
        relogio = Math.max(relogio, timestampRecebido) + 1;
    }

    public int getRelogio() {
        return relogio;
    }
}