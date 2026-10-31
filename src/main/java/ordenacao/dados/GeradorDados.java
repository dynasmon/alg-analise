package ordenacao.dados;

import java.util.SplittableRandom;

public final class GeradorDados {

    private GeradorDados() {
    }

    public static int[] gerar(Distribuicao distribuicao, int n, int valorMaximo, long semente) {
        if (n < 0 || valorMaximo < 1) {
            throw new IllegalArgumentException("Tamanho e valor máximo precisam ser positivos");
        }
        SplittableRandom rnd = new SplittableRandom(semente);
        int intervalo = distribuicao.intervaloEfetivo(valorMaximo);
        int[] v = new int[n];
        switch (distribuicao) {
            case ALEATORIO, REPETIDOS -> {
                for (int i = 0; i < n; i++) {
                    v[i] = rnd.nextInt(intervalo);
                }
            }
            case ORDENADO -> preencherCrescente(v, intervalo);
            case INVERSO -> {
                preencherCrescente(v, intervalo);
                inverter(v);
            }
            case QUASE_ORDENADO -> {
                preencherCrescente(v, intervalo);
                // 1% das posições trocadas aleatoriamente
                for (int t = Math.max(1, n / 100); t > 0 && n > 1; t--) {
                    int i = rnd.nextInt(n);
                    int j = rnd.nextInt(n);
                    int aux = v[i];
                    v[i] = v[j];
                    v[j] = aux;
                }
            }
        }
        return v;
    }

    private static void preencherCrescente(int[] v, int intervalo) {
        for (int i = 0; i < v.length; i++) {
            v[i] = (int) ((long) i * intervalo / v.length);
        }
    }

    private static void inverter(int[] v) {
        for (int i = 0, j = v.length - 1; i < j; i++, j--) {
            int aux = v[i];
            v[i] = v[j];
            v[j] = aux;
        }
    }
}
