package br.unifor.ordenacao.benchmark;

import br.unifor.ordenacao.algoritmos.AlgoritmoOrdenacao;
import br.unifor.ordenacao.dados.Distribuicao;

import java.util.Locale;

public record Cenario(String experimento, AlgoritmoOrdenacao algoritmo, Distribuicao distribuicao,
                      int tamanho, int valorMaximo) {

    public String descricao() {
        return String.format(Locale.forLanguageTag("pt-BR"), "%s | %s | n=%,d | valores em [0, %,d)",
                algoritmo.nome(), distribuicao.rotulo(), tamanho, valorMaximo);
    }
}
