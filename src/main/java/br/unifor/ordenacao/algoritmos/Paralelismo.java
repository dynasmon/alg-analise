package br.unifor.ordenacao.algoritmos;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.function.IntConsumer;

final class Paralelismo {

    // Um pool por quantidade de threads, reaproveitado entre execuções
    private static final Map<Integer, ForkJoinPool> POOLS = new ConcurrentHashMap<>();

    private Paralelismo() {
    }

    static ForkJoinPool pool(int threads) {
        validar(threads);
        return POOLS.computeIfAbsent(threads, ForkJoinPool::new);
    }

    static void validar(int threads) {
        if (threads < 1) {
            throw new IllegalArgumentException("Quantidade de threads inválida: " + threads);
        }
    }

    static int inicioBloco(int bloco, int blocos, int n) {
        return (int) ((long) bloco * n / blocos);
    }

    static void paraCadaBloco(ForkJoinPool pool, int blocos, IntConsumer corpo) {
        pool.invoke(new Blocos(0, blocos, corpo));
    }

    private static final class Blocos extends RecursiveAction {

        private final int ini;
        private final int fim;
        private final IntConsumer corpo;

        Blocos(int ini, int fim, IntConsumer corpo) {
            this.ini = ini;
            this.fim = fim;
            this.corpo = corpo;
        }

        @Override
        protected void compute() {
            if (fim - ini == 1) {
                corpo.accept(ini);
                return;
            }
            int meio = (ini + fim) >>> 1;
            invokeAll(new Blocos(ini, meio, corpo), new Blocos(meio, fim, corpo));
        }
    }
}
