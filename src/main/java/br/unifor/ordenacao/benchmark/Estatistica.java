package br.unifor.ordenacao.benchmark;

import java.util.Arrays;

public final class Estatistica {

    // t de Student bicaudal a 95% para 1..30 graus de liberdade
    private static final double[] T95 = {
            12.706, 4.303, 3.182, 2.776, 2.571, 2.447, 2.365, 2.306, 2.262, 2.228,
            2.201, 2.179, 2.160, 2.145, 2.131, 2.120, 2.110, 2.101, 2.093, 2.086,
            2.080, 2.074, 2.069, 2.064, 2.060, 2.056, 2.052, 2.048, 2.045, 2.042};

    private Estatistica() {
    }

    public static double media(double[] x) {
        double soma = 0;
        for (double v : x) {
            soma += v;
        }
        return x.length == 0 ? Double.NaN : soma / x.length;
    }

    public static double desvioPadrao(double[] x) {
        if (x.length < 2) {
            return 0;
        }
        double m = media(x);
        double soma = 0;
        for (double v : x) {
            soma += (v - m) * (v - m);
        }
        return Math.sqrt(soma / (x.length - 1));
    }

    public static double mediana(double[] x) {
        if (x.length == 0) {
            return Double.NaN;
        }
        double[] ordenado = x.clone();
        Arrays.sort(ordenado);
        int meio = ordenado.length / 2;
        return ordenado.length % 2 == 1 ? ordenado[meio] : (ordenado[meio - 1] + ordenado[meio]) / 2;
    }

    public static double meiaLarguraIc95(double[] x) {
        if (x.length < 2) {
            return 0;
        }
        return t95(x.length - 1) * desvioPadrao(x) / Math.sqrt(x.length);
    }

    public static double t95(int grausLiberdade) {
        return grausLiberdade <= T95.length ? T95[grausLiberdade - 1] : 1.96;
    }

    public record Reta(double inclinacao, double intercepto, double r2) {
    }

    public static Reta minimosQuadrados(double[] x, double[] y) {
        double mx = media(x);
        double my = media(y);
        double sxy = 0;
        double sxx = 0;
        double syy = 0;
        for (int i = 0; i < x.length; i++) {
            sxy += (x[i] - mx) * (y[i] - my);
            sxx += (x[i] - mx) * (x[i] - mx);
            syy += (y[i] - my) * (y[i] - my);
        }
        double a = sxy / sxx;
        double r2 = syy == 0 ? 1 : (sxy * sxy) / (sxx * syy);
        return new Reta(a, my - a * mx, r2);
    }
}
