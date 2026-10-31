package ordenacao.algoritmos;

import ordenacao.dados.Distribuicao;
import ordenacao.dados.GeradorDados;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.SplittableRandom;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AlgoritmosTest {

    static Stream<Arguments> casos() {
        return Algoritmos.todos().stream().flatMap(alg -> Arrays.stream(Distribuicao.values())
                .map(d -> Arguments.of(alg, d)));
    }

    @ParameterizedTest(name = "{0} - {1}")
    @MethodSource("casos")
    void ordenaTodasAsDistribuicoes(AlgoritmoOrdenacao alg, Distribuicao d) {
        int n = alg.quadratico() ? 4_000 : 250_000;
        int[] original = GeradorDados.gerar(d, n, 1_000_000, 42);
        int[] esperado = original.clone();
        Arrays.sort(esperado);

        int[] serial = original.clone();
        alg.ordenarSerial(serial);
        assertArrayEquals(esperado, serial, "serial");

        for (int threads : new int[]{1, 2, 3, 4, 7, 8, 16}) {
            int[] paralelo = original.clone();
            alg.ordenarParalelo(paralelo, threads);
            assertArrayEquals(esperado, paralelo, threads + " threads");
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("algoritmos")
    void casosDeBorda(AlgoritmoOrdenacao alg) {
        int[][] entradas = {{}, {5}, {2, 1}, {1, 1, 1, 1}, {-3, 7, 0, -3, 1_000_000, 2},
                {-2_000_000, 0, -1_999_990}, {9, 8, 7, 6, 5, 4, 3, 2, 1, 0}};
        for (int[] entrada : entradas) {
            int[] esperado = entrada.clone();
            Arrays.sort(esperado);
            int[] serial = entrada.clone();
            alg.ordenarSerial(serial);
            assertArrayEquals(esperado, serial);
            int[] paralelo = entrada.clone();
            alg.ordenarParalelo(paralelo, 4);
            assertArrayEquals(esperado, paralelo);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("algoritmos")
    void vetoresPequenosAleatorios(AlgoritmoOrdenacao alg) {
        SplittableRandom rnd = new SplittableRandom(1);
        for (int caso = 0; caso < 2_000; caso++) {
            int[] v = rnd.ints(rnd.nextInt(60), -50, 50).toArray();
            int[] esperado = v.clone();
            Arrays.sort(esperado);
            int[] paralelo = v.clone();
            alg.ordenarParalelo(paralelo, 1 + rnd.nextInt(8));
            assertArrayEquals(esperado, paralelo);
        }
    }

    @Test
    void countingSortRecusaIntervaloGigante() {
        int[] v = {Integer.MIN_VALUE, Integer.MAX_VALUE};
        assertThrows(IllegalArgumentException.class, () -> new CountingSort().ordenarSerial(v));
    }

    @Test
    void threadsInvalidas() {
        for (AlgoritmoOrdenacao alg : Algoritmos.todos()) {
            assertThrows(IllegalArgumentException.class, () -> alg.ordenarParalelo(new int[]{3, 2, 1}, 0));
        }
    }

    static Stream<AlgoritmoOrdenacao> algoritmos() {
        return Algoritmos.todos().stream();
    }
}
