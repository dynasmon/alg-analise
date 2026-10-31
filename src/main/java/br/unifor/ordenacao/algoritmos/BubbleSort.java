package br.unifor.ordenacao.algoritmos;

public final class BubbleSort implements AlgoritmoOrdenacao {

    @Override
    public String nome() {
        return "Bubble Sort";
    }

    @Override
    public boolean quadratico() {
        return true;
    }

    @Override
    public void ordenarSerial(int[] v) {
        boolean trocou = true;
        for (int fim = v.length - 1; fim > 0 && trocou; fim--) {
            trocou = false;
            for (int i = 0; i < fim; i++) {
                if (v[i] > v[i + 1]) {
                    int t = v[i];
                    v[i] = v[i + 1];
                    v[i + 1] = t;
                    trocou = true;
                }
            }
        }
    }

    // Versão paralela: transposição par-ímpar (odd-even transposition sort)
    @Override
    public void ordenarParalelo(int[] v, int threads) {
        Paralelismo.validar(threads);
        int pares = v.length - 1;
        if (pares < 1) {
            return;
        }
        new TransposicaoParImpar(v, Math.min(threads, pares)).executar();
    }

    private static final class TransposicaoParImpar {

        private final int[] v;
        private final int threads;
        private final boolean[] trocouNaFase;
        private final BarreiraAtiva barreira;
        private boolean trocouNaFaseAnterior = true;
        private boolean ordenado;

        TransposicaoParImpar(int[] v, int threads) {
            this.v = v;
            this.threads = threads;
            this.trocouNaFase = new boolean[threads];
            this.barreira = new BarreiraAtiva(threads, this::fimDaFase);
        }

        void executar() {
            Thread[] trabalhadores = new Thread[threads];
            for (int t = 0; t < threads; t++) {
                int id = t;
                trabalhadores[t] = new Thread(() -> trabalhar(id), "bubble-" + t);
                trabalhadores[t].start();
            }
            try {
                for (Thread t : trabalhadores) {
                    t.join();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Ordenação interrompida", e);
            }
        }

        private void trabalhar(int id) {
            int ini = Paralelismo.inicioBloco(id, threads, v.length - 1);
            int fim = Paralelismo.inicioBloco(id + 1, threads, v.length - 1);
            for (int fase = 0; fase < v.length && !ordenado; fase++) {
                boolean trocou = false;
                for (int i = ini + ((ini ^ fase) & 1); i < fim; i += 2) {
                    if (v[i] > v[i + 1]) {
                        int t = v[i];
                        v[i] = v[i + 1];
                        v[i + 1] = t;
                        trocou = true;
                    }
                }
                trocouNaFase[id] = trocou;
                barreira.aguardar();
            }
        }

        // Executado pela última thread a chegar; duas fases seguidas sem troca => vetor ordenado
        private void fimDaFase() {
            boolean trocou = false;
            for (boolean t : trocouNaFase) {
                trocou |= t;
            }
            ordenado = !trocou && !trocouNaFaseAnterior;
            trocouNaFaseAnterior = trocou;
        }
    }
}
