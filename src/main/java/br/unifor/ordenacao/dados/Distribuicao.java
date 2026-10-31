package br.unifor.ordenacao.dados;

import java.util.Locale;

public enum Distribuicao {

    ALEATORIO("Aleatório"),
    ORDENADO("Ordenado"),
    INVERSO("Inverso"),
    QUASE_ORDENADO("Quase ordenado"),
    REPETIDOS("Muitos repetidos");

    public static final int VALORES_DISTINTOS_REPETIDOS = 100;

    private final String rotulo;

    Distribuicao(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }

    @Override
    public String toString() {
        return rotulo;
    }

    public int intervaloEfetivo(int valorMaximo) {
        return this == REPETIDOS ? Math.min(valorMaximo, VALORES_DISTINTOS_REPETIDOS) : valorMaximo;
    }

    public static Distribuicao porNome(String nome) {
        String procurado = nome.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        for (Distribuicao d : values()) {
            if (d.name().startsWith(procurado)) {
                return d;
            }
        }
        throw new IllegalArgumentException("Distribuição desconhecida: " + nome);
    }
}
