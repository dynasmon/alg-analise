package ordenacao.benchmark;

import ordenacao.dados.Distribuicao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnaliseTest {

    @Test
    void estatisticasBasicas() {
        double[] x = {10, 12, 14, 16, 18};
        assertEquals(14, Estatistica.media(x), 1e-9);
        assertEquals(Math.sqrt(10), Estatistica.desvioPadrao(x), 1e-9);
        assertEquals(14, Estatistica.mediana(x), 1e-9);
        assertEquals(2.776 * Math.sqrt(10) / Math.sqrt(5), Estatistica.meiaLarguraIc95(x), 1e-9);
    }

    @Test
    void regressaoRecuperaExpoente() {
        double[] x = new double[6];
        double[] y = new double[6];
        for (int i = 0; i < 6; i++) {
            x[i] = Math.log(1000.0 * (i + 1));
            y[i] = 2 * x[i] + 3;
        }
        Estatistica.Reta reta = Estatistica.minimosQuadrados(x, y);
        assertEquals(2, reta.inclinacao(), 1e-9);
        assertEquals(1, reta.r2(), 1e-9);
    }

    @Test
    void speedupEEficiencia() {
        List<Medicao> medicoes = new ArrayList<>();
        for (int a = 1; a <= 5; a++) {
            medicoes.add(medicao(false, 1, a, 100));
            medicoes.add(medicao(true, 1, a, 110));
            medicoes.add(medicao(true, 4, a, 25));
        }
        Resumo quatro = Analise.resumir(medicoes).stream()
                .filter(r -> r.paralelo() && r.threads() == 4).findFirst().orElseThrow();
        assertEquals(4, quatro.speedup(), 1e-9);
        assertEquals(4.4, quatro.speedupRelativo(), 1e-9);
        assertEquals(1, quatro.eficiencia(), 1e-9);
    }

    @Test
    void csvIdaEVolta(@TempDir Path pasta) throws IOException {
        List<Medicao> medicoes = List.of(medicao(false, 1, 1, 12.3456), medicao(true, 8, 2, 1.5));
        Path arquivo = pasta.resolve(ArquivosCsv.MEDICOES);
        ArquivosCsv.gravarMedicoes(arquivo, medicoes);
        assertEquals(medicoes, ArquivosCsv.lerMedicoes(arquivo));
    }

    private static Medicao medicao(boolean paralelo, int threads, int amostra, double ms) {
        return new Medicao(Configuracao.EXPERIMENTO_TAMANHO, "Merge Sort", Distribuicao.ALEATORIO, 1000,
                1_000_000, paralelo, threads, amostra, ms);
    }
}
