package ordenacao.grafico;

import java.text.NumberFormat;
import java.util.Locale;

public final class Formatos {

    public static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private Formatos() {
    }

    public static String numero(double v) {
        if (v == 0 || Double.isNaN(v)) {
            return Double.isNaN(v) ? "–" : "0";
        }
        double a = Math.abs(v);
        int casas = a >= 100 ? 0 : a >= 10 ? 1 : a >= 1 ? 2 : (int) Math.min(6, Math.ceil(-Math.log10(a)) + 1);
        NumberFormat f = NumberFormat.getNumberInstance(PT_BR);
        f.setMaximumFractionDigits(casas);
        return f.format(v);
    }

    public static String quantidade(double v) {
        if (Math.abs(v) >= 1_000_000) {
            return numero(v / 1_000_000) + " mi";
        }
        if (Math.abs(v) >= 1_000) {
            return numero(v / 1_000) + " mil";
        }
        return numero(v);
    }

    public static String inteiro(long v) {
        return NumberFormat.getIntegerInstance(PT_BR).format(v);
    }

    public static String ms(double v) {
        return numero(v) + " ms";
    }
}
