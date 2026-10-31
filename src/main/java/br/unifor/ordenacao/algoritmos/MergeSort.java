package br.unifor.ordenacao.algoritmos;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public final class MergeSort implements AlgoritmoOrdenacao {

    private static final int LIMIAR_MINIMO = 1 << 13;

    @Override
    public String nome() {
        return "Merge Sort";
    }

    @Override
    public void ordenarSerial(int[] v) {
        ordenar(v, new int[v.length], 0, v.length);
    }

    @Override
    public void ordenarParalelo(int[] v, int threads) {
        ForkJoinPool pool = Paralelismo.pool(threads);
        int limiar = Math.max(LIMIAR_MINIMO, v.length / (threads * 8));
        pool.invoke(new Tarefa(v, new int[v.length], 0, v.length, limiar));
    }

    private static void ordenar(int[] v, int[] aux, int ini, int fim) {
        if (fim - ini < 2) {
            return;
        }
        int meio = (ini + fim) >>> 1;
        ordenar(v, aux, ini, meio);
        ordenar(v, aux, meio, fim);
        intercalar(v, aux, ini, meio, fim);
    }

    private static void intercalar(int[] v, int[] aux, int ini, int meio, int fim) {
        System.arraycopy(v, ini, aux, ini, fim - ini);
        int i = ini;
        int j = meio;
        int k = ini;
        while (i < meio && j < fim) {
            v[k++] = aux[j] < aux[i] ? aux[j++] : aux[i++];
        }
        // o que sobrou da metade direita já está na posição certa
        while (i < meio) {
            v[k++] = aux[i++];
        }
    }

    private static final class Tarefa extends RecursiveAction {

        private final int[] v;
        private final int[] aux;
        private final int ini;
        private final int fim;
        private final int limiar;

        Tarefa(int[] v, int[] aux, int ini, int fim, int limiar) {
            this.v = v;
            this.aux = aux;
            this.ini = ini;
            this.fim = fim;
            this.limiar = limiar;
        }

        @Override
        protected void compute() {
            if (fim - ini <= limiar) {
                ordenar(v, aux, ini, fim);
                return;
            }
            int meio = (ini + fim) >>> 1;
            invokeAll(new Tarefa(v, aux, ini, meio, limiar), new Tarefa(v, aux, meio, fim, limiar));
            intercalar(v, aux, ini, meio, fim);
        }
    }
}
