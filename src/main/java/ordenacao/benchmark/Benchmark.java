package ordenacao.benchmark;

import ordenacao.algoritmos.AlgoritmoOrdenacao;
import ordenacao.dados.Distribuicao;
import ordenacao.dados.GeradorDados;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Benchmark {

    public interface Ouvinte {

        default void aoIniciarCenario(Cenario cenario, int indice, int total) {
        }

        default void aoMedir(Medicao medicao) {
        }

        default void aoConcluirCenario(Cenario cenario, List<Medicao> medicoes) {
        }
    }

    private final Configuracao config;
    private volatile boolean cancelado;

    public Benchmark(Configuracao config) {
        this.config = config;
    }

    public void cancelar() {
        cancelado = true;
    }

    public boolean cancelado() {
        return cancelado;
    }

    public void executar(Ouvinte ouvinte) {
        List<Cenario> cenarios = config.cenarios();
        aquecerJit();
        for (int i = 0; i < cenarios.size() && !cancelado; i++) {
            Cenario cenario = cenarios.get(i);
            ouvinte.aoIniciarCenario(cenario, i + 1, cenarios.size());
            List<Medicao> medicoes = executarCenario(cenario, ouvinte);
            if (!cancelado) {
                ouvinte.aoConcluirCenario(cenario, medicoes);
            }
        }
    }

    private List<Medicao> executarCenario(Cenario c, Ouvinte ouvinte) {
        int[] original = GeradorDados.gerar(c.distribuicao(), c.tamanho(), c.valorMaximo(), config.semente());
        int[] esperado = original.clone();
        Arrays.sort(esperado);
        int[] trabalho = new int[original.length];

        List<Execucao> execucoes = new ArrayList<>();
        execucoes.add(Execucao.SERIAL);
        for (int t : config.threads()) {
            execucoes.add(new Execucao(true, t));
        }

        // uma rodada descartada antes das amostras
        for (Execucao e : execucoes) {
            medir(c.algoritmo(), e, original, trabalho, esperado);
        }

        // as execuções são intercaladas a cada amostra para diluir variações da máquina entre todas
        List<Medicao> medicoes = new ArrayList<>();
        for (int amostra = 1; amostra <= config.amostras() && !cancelado; amostra++) {
            for (Execucao e : execucoes) {
                double ms = medir(c.algoritmo(), e, original, trabalho, esperado);
                Medicao m = new Medicao(c.experimento(), c.algoritmo().nome(), c.distribuicao(), c.tamanho(),
                        c.valorMaximo(), e.paralelo(), e.threads(), amostra, ms);
                medicoes.add(m);
                ouvinte.aoMedir(m);
            }
        }
        return medicoes;
    }

    private static double medir(AlgoritmoOrdenacao alg, Execucao e, int[] original, int[] trabalho, int[] esperado) {
        System.arraycopy(original, 0, trabalho, 0, original.length);
        System.gc();
        long inicio = System.nanoTime();
        if (e.paralelo()) {
            alg.ordenarParalelo(trabalho, e.threads());
        } else {
            alg.ordenarSerial(trabalho);
        }
        long fim = System.nanoTime();
        if (!Arrays.equals(trabalho, esperado)) {
            throw new IllegalStateException(alg.nome() + " produziu um vetor fora de ordem (" + e + ")");
        }
        return (fim - inicio) / 1_000_000.0;
    }

    private void aquecerJit() {
        for (AlgoritmoOrdenacao alg : config.algoritmos()) {
            int n = alg.quadratico() ? 3_000 : 200_000;
            int[] dados = GeradorDados.gerar(Distribuicao.ALEATORIO, n, config.valorMaximo(), 1);
            int[] copia = new int[n];
            for (int rodada = 0; rodada < 10 && !cancelado; rodada++) {
                System.arraycopy(dados, 0, copia, 0, n);
                alg.ordenarSerial(copia);
                for (int t : config.threads()) {
                    System.arraycopy(dados, 0, copia, 0, n);
                    alg.ordenarParalelo(copia, t);
                }
            }
        }
    }
}
