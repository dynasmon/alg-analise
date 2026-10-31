package br.unifor.ordenacao.algoritmos;

import java.util.concurrent.ForkJoinPool;

public final class CountingSort implements AlgoritmoOrdenacao {

    private static final long INTERVALO_MAXIMO = 1L << 27;

    @Override
    public String nome() {
        return "Counting Sort";
    }

    @Override
    public void ordenarSerial(int[] v) {
        if (v.length < 2) {
            return;
        }
        int min = v[0];
        int max = v[0];
        for (int x : v) {
            min = Math.min(min, x);
            max = Math.max(max, x);
        }
        int[] cont = new int[intervalo(min, max)];
        for (int x : v) {
            cont[x - min]++;
        }
        int pos = 0;
        for (int k = 0; k < cont.length; k++) {
            int c = cont[k];
            cont[k] = pos;
            pos += c;
        }
        int[] saida = new int[v.length];
        for (int x : v) {
            saida[cont[x - min]++] = x;
        }
        System.arraycopy(saida, 0, v, 0, v.length);
    }

    @Override
    public void ordenarParalelo(int[] v, int threads) {
        int n = v.length;
        if (n < 2) {
            return;
        }
        ForkJoinPool pool = Paralelismo.pool(threads);
        int blocos = Math.min(threads, n);

        int[] mins = new int[blocos];
        int[] maxs = new int[blocos];
        Paralelismo.paraCadaBloco(pool, blocos, b -> {
            int ini = Paralelismo.inicioBloco(b, blocos, n);
            int fim = Paralelismo.inicioBloco(b + 1, blocos, n);
            int menor = v[ini];
            int maior = v[ini];
            for (int i = ini; i < fim; i++) {
                menor = Math.min(menor, v[i]);
                maior = Math.max(maior, v[i]);
            }
            mins[b] = menor;
            maxs[b] = maior;
        });
        int min = mins[0];
        int max = maxs[0];
        for (int b = 1; b < blocos; b++) {
            min = Math.min(min, mins[b]);
            max = Math.max(max, maxs[b]);
        }
        int k = intervalo(min, max);
        int deslocamento = min;

        // cada bloco conta no seu próprio histograma, sem disputa entre threads
        int[][] cont = new int[blocos][];
        Paralelismo.paraCadaBloco(pool, blocos, b -> {
            int[] c = new int[k];
            int fim = Paralelismo.inicioBloco(b + 1, blocos, n);
            for (int i = Paralelismo.inicioBloco(b, blocos, n); i < fim; i++) {
                c[v[i] - deslocamento]++;
            }
            cont[b] = c;
        });

        // soma de prefixos em paralelo: cont[b][x] passa a ser a 1ª posição de x vinda do bloco b
        int fatias = Math.min(blocos, k);
        int[] total = new int[k];
        int[] inicioFatia = new int[fatias + 1];
        Paralelismo.paraCadaBloco(pool, fatias, f -> {
            int ini = Paralelismo.inicioBloco(f, fatias, k);
            int fim = Paralelismo.inicioBloco(f + 1, fatias, k);
            for (int[] c : cont) {
                for (int x = ini; x < fim; x++) {
                    total[x] += c[x];
                }
            }
            int soma = 0;
            for (int x = ini; x < fim; x++) {
                soma += total[x];
            }
            inicioFatia[f + 1] = soma;
        });
        for (int f = 1; f <= fatias; f++) {
            inicioFatia[f] += inicioFatia[f - 1];
        }
        Paralelismo.paraCadaBloco(pool, fatias, f -> {
            int ini = Paralelismo.inicioBloco(f, fatias, k);
            int fim = Paralelismo.inicioBloco(f + 1, fatias, k);
            int pos = inicioFatia[f];
            for (int x = ini; x < fim; x++) {
                int quantidade = total[x];
                total[x] = pos;
                pos += quantidade;
            }
            for (int[] c : cont) {
                for (int x = ini; x < fim; x++) {
                    int quantidade = c[x];
                    c[x] = total[x];
                    total[x] += quantidade;
                }
            }
        });

        int[] saida = new int[n];
        Paralelismo.paraCadaBloco(pool, blocos, b -> {
            int[] pos = cont[b];
            int fim = Paralelismo.inicioBloco(b + 1, blocos, n);
            for (int i = Paralelismo.inicioBloco(b, blocos, n); i < fim; i++) {
                saida[pos[v[i] - deslocamento]++] = v[i];
            }
        });
        Paralelismo.paraCadaBloco(pool, blocos, b -> {
            int ini = Paralelismo.inicioBloco(b, blocos, n);
            System.arraycopy(saida, ini, v, ini, Paralelismo.inicioBloco(b + 1, blocos, n) - ini);
        });
    }

    private static int intervalo(int min, int max) {
        long k = (long) max - min + 1;
        if (k > INTERVALO_MAXIMO) {
            throw new IllegalArgumentException("Intervalo de valores grande demais para o Counting Sort: " + k);
        }
        return (int) k;
    }
}
