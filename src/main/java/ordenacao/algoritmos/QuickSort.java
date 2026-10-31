package ordenacao.algoritmos;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public final class QuickSort implements AlgoritmoOrdenacao {

    private static final int LIMIAR_MINIMO = 1 << 13;

    @Override
    public String nome() {
        return "Quick Sort";
    }

    @Override
    public void ordenarSerial(int[] v) {
        ordenar(v, 0, v.length - 1);
    }

    @Override
    public void ordenarParalelo(int[] v, int threads) {
        ForkJoinPool pool = Paralelismo.pool(threads);
        int limiar = Math.max(LIMIAR_MINIMO, v.length / (threads * 8));
        pool.invoke(new Tarefa(v, 0, v.length - 1, limiar));
    }

    private static void ordenar(int[] v, int ini, int fim) {
        // recursão só na parte menor, assim a pilha fica em O(log n)
        while (ini < fim) {
            int p = particionar(v, ini, fim);
            if (p - ini < fim - p) {
                ordenar(v, ini, p);
                ini = p + 1;
            } else {
                ordenar(v, p + 1, fim);
                fim = p;
            }
        }
    }

    // Partição de Hoare com pivô escolhido pela mediana de três
    private static int particionar(int[] v, int ini, int fim) {
        int pivo = medianaDeTres(v[ini], v[(ini + fim) >>> 1], v[fim]);
        int i = ini - 1;
        int j = fim + 1;
        while (true) {
            do {
                i++;
            } while (v[i] < pivo);
            do {
                j--;
            } while (v[j] > pivo);
            if (i >= j) {
                return j;
            }
            int t = v[i];
            v[i] = v[j];
            v[j] = t;
        }
    }

    private static int medianaDeTres(int a, int b, int c) {
        return Math.max(Math.min(a, b), Math.min(Math.max(a, b), c));
    }

    private static final class Tarefa extends RecursiveAction {

        private final int[] v;
        private final int ini;
        private final int fim;
        private final int limiar;

        Tarefa(int[] v, int ini, int fim, int limiar) {
            this.v = v;
            this.ini = ini;
            this.fim = fim;
            this.limiar = limiar;
        }

        @Override
        protected void compute() {
            if (fim - ini < limiar) {
                ordenar(v, ini, fim);
                return;
            }
            int p = particionar(v, ini, fim);
            invokeAll(new Tarefa(v, ini, p, limiar), new Tarefa(v, p + 1, fim, limiar));
        }
    }
}
