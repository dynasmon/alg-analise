package br.unifor.ordenacao.grafico;

import br.unifor.ordenacao.benchmark.Analise;

import java.awt.Color;

public final class Paleta {

    public static final Color SUPERFICIE = new Color(0xfcfcfb);
    public static final Color TINTA = new Color(0x0b0b0b);
    public static final Color TINTA_SECUNDARIA = new Color(0x52514e);
    public static final Color TINTA_SUAVE = new Color(0x898781);
    public static final Color GRADE = new Color(0xe1e0d9);
    public static final Color EIXO = new Color(0xc3c2b7);

    // ordem fixa testada para daltonismo; cada algoritmo mantém sempre a mesma cor
    private static final Color[] CATEGORICAS = {
            new Color(0x2a78d6), new Color(0xeb6834), new Color(0x1baf7a), new Color(0xeda100),
            new Color(0xe87ba4), new Color(0x008300), new Color(0x4a3aa7), new Color(0xe34948)};

    private Paleta() {
    }

    public static Color doAlgoritmo(String nome) {
        int i = Analise.ordemAlgoritmo(nome);
        return i < CATEGORICAS.length ? CATEGORICAS[i] : TINTA_SUAVE;
    }
}
