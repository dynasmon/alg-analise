package br.unifor.ordenacao.grafico;

import java.awt.Color;
import java.util.List;

public record Serie(String nome, Color cor, List<Ponto> pontos, boolean referencia) {

    public record Ponto(double x, double y, double erro, String detalhe) {
    }

    public static Serie de(String nome, Color cor, List<Ponto> pontos) {
        return new Serie(nome, cor, List.copyOf(pontos), false);
    }

    public static Serie referencia(String nome, List<Ponto> pontos) {
        return new Serie(nome, Paleta.TINTA_SUAVE, List.copyOf(pontos), true);
    }
}
