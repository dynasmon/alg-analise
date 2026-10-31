package br.unifor.ordenacao.benchmark;

import br.unifor.ordenacao.algoritmos.AlgoritmoOrdenacao;
import br.unifor.ordenacao.algoritmos.Algoritmos;
import br.unifor.ordenacao.dados.Distribuicao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public final class Analise {

    public static final Comparator<Resumo> ORDEM = Comparator.comparing(Resumo::experimento)
            .thenComparingInt((Resumo r) -> ordemAlgoritmo(r.algoritmo()))
            .thenComparing(Resumo::algoritmo)
            .thenComparing(Resumo::distribuicao)
            .thenComparingInt(Resumo::tamanho)
            .thenComparingInt(Resumo::valorMaximo)
            .thenComparing(Resumo::paralelo)
            .thenComparingInt(Resumo::threads);

    private record Chave(String experimento, String algoritmo, Distribuicao distribuicao, int tamanho,
                         int valorMaximo, boolean paralelo, int threads) {

        static Chave de(Medicao m) {
            return new Chave(m.experimento(), m.algoritmo(), m.distribuicao(), m.tamanho(), m.valorMaximo(),
                    m.paralelo(), m.threads());
        }

        Chave comExecucao(boolean paralelo, int threads) {
            return new Chave(experimento, algoritmo, distribuicao, tamanho, valorMaximo, paralelo, threads);
        }
    }

    private record Serie(String algoritmo, Distribuicao distribuicao, boolean paralelo, int threads) {
    }

    private Analise() {
    }

    public static List<Resumo> resumir(Collection<Medicao> medicoes) {
        Map<Chave, List<Double>> grupos = new LinkedHashMap<>();
        for (Medicao m : medicoes) {
            grupos.computeIfAbsent(Chave.de(m), k -> new ArrayList<>()).add(m.tempoMs());
        }
        Map<Chave, Double> medias = new HashMap<>();
        grupos.forEach((k, tempos) -> medias.put(k, Estatistica.media(paraVetor(tempos))));

        List<Resumo> resumos = new ArrayList<>();
        grupos.forEach((k, tempos) -> {
            double[] t = paraVetor(tempos);
            double media = medias.get(k);
            double serial = medias.getOrDefault(k.comExecucao(false, 1), Double.NaN);
            double umaThread = medias.getOrDefault(k.comExecucao(true, 1), Double.NaN);
            resumos.add(new Resumo(k.experimento(), k.algoritmo(), k.distribuicao(), k.tamanho(), k.valorMaximo(),
                    k.paralelo(), k.threads(), t.length, media, Estatistica.desvioPadrao(t),
                    Estatistica.mediana(t), minimo(t), maximo(t), Estatistica.meiaLarguraIc95(t),
                    serial / media, k.paralelo() ? umaThread / media : Double.NaN));
        });
        resumos.sort(ORDEM);
        return resumos;
    }

    public static List<Crescimento> crescimento(List<Resumo> resumos) {
        Map<Serie, List<Resumo>> series = new LinkedHashMap<>();
        for (Resumo r : resumos) {
            if (r.experimento().equals(Configuracao.EXPERIMENTO_TAMANHO)) {
                Serie s = new Serie(r.algoritmo(), r.distribuicao(), r.paralelo(), r.threads());
                series.computeIfAbsent(s, k -> new ArrayList<>()).add(r);
            }
        }
        List<Crescimento> lista = new ArrayList<>();
        series.forEach((s, pontos) -> {
            if (pontos.size() < 3) {
                return;
            }
            pontos.sort(Comparator.comparingInt(Resumo::tamanho));
            double[] x = new double[pontos.size()];
            double[] y = new double[pontos.size()];
            for (int i = 0; i < pontos.size(); i++) {
                x[i] = Math.log(pontos.get(i).tamanho());
                y[i] = Math.log(pontos.get(i).mediaMs());
            }
            Estatistica.Reta reta = Estatistica.minimosQuadrados(x, y);
            lista.add(new Crescimento(s.algoritmo(), s.distribuicao(), s.paralelo(), s.threads(), pontos.size(),
                    pontos.get(0).tamanho(), pontos.get(pontos.size() - 1).tamanho(), reta.inclinacao(), reta.r2()));
        });
        return lista;
    }

    public static int ordemAlgoritmo(String nome) {
        List<String> nomes = Algoritmos.todos().stream().map(AlgoritmoOrdenacao::nome).toList();
        int i = nomes.indexOf(nome);
        return i >= 0 ? i : nomes.size();
    }

    public static List<Integer> threadsDisponiveis(Collection<Resumo> resumos) {
        TreeSet<Integer> threads = new TreeSet<>();
        resumos.stream().filter(Resumo::paralelo).forEach(r -> threads.add(r.threads()));
        return new ArrayList<>(threads);
    }

    private static double[] paraVetor(List<Double> valores) {
        return valores.stream().mapToDouble(Double::doubleValue).toArray();
    }

    private static double minimo(double[] x) {
        double m = Double.POSITIVE_INFINITY;
        for (double v : x) {
            m = Math.min(m, v);
        }
        return m;
    }

    private static double maximo(double[] x) {
        double m = Double.NEGATIVE_INFINITY;
        for (double v : x) {
            m = Math.max(m, v);
        }
        return m;
    }
}
