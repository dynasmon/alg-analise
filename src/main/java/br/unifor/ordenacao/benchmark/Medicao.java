package br.unifor.ordenacao.benchmark;

import br.unifor.ordenacao.dados.Distribuicao;

public record Medicao(String experimento, String algoritmo, Distribuicao distribuicao, int tamanho,
                      int valorMaximo, boolean paralelo, int threads, int amostra, double tempoMs) {

    public String modo() {
        return paralelo ? "paralelo" : "serial";
    }

    public Execucao execucao() {
        return new Execucao(paralelo, threads);
    }
}
