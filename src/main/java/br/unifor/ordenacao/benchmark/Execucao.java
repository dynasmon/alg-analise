package br.unifor.ordenacao.benchmark;

public record Execucao(boolean paralelo, int threads) {

    public static final Execucao SERIAL = new Execucao(false, 1);

    public String rotulo() {
        if (!paralelo) {
            return "Serial";
        }
        return threads + (threads == 1 ? " thread" : " threads");
    }

    @Override
    public String toString() {
        return rotulo();
    }
}
