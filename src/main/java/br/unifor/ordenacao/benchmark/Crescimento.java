package br.unifor.ordenacao.benchmark;

import br.unifor.ordenacao.dados.Distribuicao;

// Ajuste log(t) = a·log(n) + b: o expoente "a" é a ordem de crescimento observada
public record Crescimento(String algoritmo, Distribuicao distribuicao, boolean paralelo, int threads,
                          int pontos, int menorTamanho, int maiorTamanho, double expoente, double r2) {

    public String modo() {
        return paralelo ? "paralelo" : "serial";
    }
}
