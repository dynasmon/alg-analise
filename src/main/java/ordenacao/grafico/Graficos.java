package ordenacao.grafico;

import ordenacao.benchmark.Analise;
import ordenacao.benchmark.Configuracao;
import ordenacao.benchmark.Execucao;
import ordenacao.benchmark.Resumo;
import ordenacao.dados.Distribuicao;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

public final class Graficos {

    private static final String TAMANHO = Configuracao.EXPERIMENTO_TAMANHO;
    private static final String INTERVALO = Configuracao.EXPERIMENTO_INTERVALO;

    private Graficos() {
    }

    public static Grafico tempoPorTamanho(List<Resumo> resumos, Distribuicao d, Execucao e) {
        List<Serie> series = porAlgoritmo(resumos,
                r -> r.experimento().equals(TAMANHO) && r.distribuicao() == d && r.execucao().equals(e),
                r -> new Serie.Ponto(r.tamanho(), r.mediaMs(), r.ic95Ms(), detalhe(r)), nome -> nome);
        return new Grafico()
                .titulo("Tempo de execução × tamanho da entrada",
                        d.rotulo() + " · " + e.rotulo() + " · escalas logarítmicas · barras: IC 95%")
                .eixoX("Tamanho da entrada (n)", true, Formatos::quantidade)
                .eixoY("Tempo médio (ms)", true, Formatos::numero)
                .series(series);
    }

    public static Grafico speedupPorThreads(List<Resumo> resumos, Distribuicao d, Integer tamanho, boolean relativo) {
        Map<String, Integer> tamanhos = tamanhoPorAlgoritmo(resumos, d, tamanho);
        List<Serie> series = porAlgoritmo(resumos,
                r -> r.paralelo() && noTamanho(r, d, tamanhos),
                r -> new Serie.Ponto(r.threads(), relativo ? r.speedupRelativo() : r.speedup(),
                        erroSpeedup(resumos, r, relativo), detalhe(r)),
                nome -> nome + " (n = " + Formatos.quantidade(tamanhos.get(nome)) + ")");
        series.add(Serie.referencia("Ideal (linear)", ideal(resumos)));
        String base = relativo ? "em relação à versão paralela com 1 thread" : "em relação à versão serial";
        return new Grafico()
                .titulo(relativo ? "Speedup relativo × número de threads" : "Speedup × número de threads",
                        d.rotulo() + " · speedup " + base)
                .eixoX("Threads", false, Formatos::numero)
                .ticksX(ticksThreads(resumos))
                .eixoY("Speedup", false, Formatos::numero)
                .series(series);
    }

    public static Grafico eficienciaPorThreads(List<Resumo> resumos, Distribuicao d, Integer tamanho) {
        Map<String, Integer> tamanhos = tamanhoPorAlgoritmo(resumos, d, tamanho);
        List<Serie> series = porAlgoritmo(resumos,
                r -> r.paralelo() && noTamanho(r, d, tamanhos),
                r -> new Serie.Ponto(r.threads(), r.eficiencia(), 0, detalhe(r)),
                nome -> nome + " (n = " + Formatos.quantidade(tamanhos.get(nome)) + ")");
        return new Grafico()
                .titulo("Eficiência × número de threads", d.rotulo() + " · eficiência = speedup / threads")
                .eixoX("Threads", false, Formatos::numero)
                .ticksX(ticksThreads(resumos))
                .eixoY("Eficiência", false, Formatos::numero)
                .referenciaY(1)
                .series(series);
    }

    public static Grafico speedupPorTamanho(List<Resumo> resumos, Distribuicao d, Execucao e) {
        List<Serie> series = porAlgoritmo(resumos,
                r -> r.experimento().equals(TAMANHO) && r.distribuicao() == d && r.execucao().equals(e) && e.paralelo(),
                r -> new Serie.Ponto(r.tamanho(), r.speedup(), erroSpeedup(resumos, r, false), detalhe(r)),
                nome -> nome);
        return new Grafico()
                .titulo("Speedup × tamanho da entrada",
                        d.rotulo() + " · " + e.rotulo() + " · linha tracejada: sem ganho (speedup 1)")
                .eixoX("Tamanho da entrada (n)", true, Formatos::quantidade)
                .eixoY("Speedup", false, Formatos::numero)
                .referenciaY(1)
                .series(series);
    }

    public static Grafico tempoRelativoPorDistribuicao(List<Resumo> resumos, Execucao e) {
        List<Distribuicao> categorias = List.of(Distribuicao.values());
        Map<String, Integer> tamanhos = tamanhoPorAlgoritmo(resumos, Distribuicao.ALEATORIO, null);
        List<Serie> series = new ArrayList<>();
        tamanhos.forEach((alg, n) -> {
            Optional<Resumo> base = busca(resumos, alg, Distribuicao.ALEATORIO, n, e);
            List<Serie.Ponto> pontos = new ArrayList<>();
            for (int i = 0; i < categorias.size(); i++) {
                Optional<Resumo> r = busca(resumos, alg, categorias.get(i), n, e);
                if (base.isPresent() && r.isPresent()) {
                    double razao = r.get().mediaMs() / base.get().mediaMs();
                    pontos.add(new Serie.Ponto(i, razao, 0, detalhe(r.get())
                            .replace("</html>", "<br>Relativo ao aleatório: " + Formatos.numero(razao) + "×</html>")));
                }
            }
            series.add(Serie.de(alg + " (n = " + Formatos.quantidade(n) + ")", Paleta.doAlgoritmo(alg), pontos));
        });
        return new Grafico()
                .tipo(Grafico.Tipo.BARRAS)
                .titulo("Efeito da natureza dos dados",
                        e.rotulo() + " · tempo dividido pelo tempo com entrada aleatória (linha tracejada = 1)")
                .categorias(categorias.stream().map(Distribuicao::rotulo).toList())
                .eixoY("Tempo relativo", false, Formatos::numero)
                .referenciaY(1)
                .series(series);
    }

    public static Grafico speedupPorDistribuicao(List<Resumo> resumos, Execucao e) {
        List<Distribuicao> categorias = List.of(Distribuicao.values());
        Map<String, Integer> tamanhos = tamanhoPorAlgoritmo(resumos, Distribuicao.ALEATORIO, null);
        List<Serie> series = new ArrayList<>();
        tamanhos.forEach((alg, n) -> {
            List<Serie.Ponto> pontos = new ArrayList<>();
            for (int i = 0; i < categorias.size(); i++) {
                Optional<Resumo> r = busca(resumos, alg, categorias.get(i), n, e);
                if (r.isPresent() && e.paralelo()) {
                    pontos.add(new Serie.Ponto(i, r.get().speedup(), 0, detalhe(r.get())));
                }
            }
            series.add(Serie.de(alg + " (n = " + Formatos.quantidade(n) + ")", Paleta.doAlgoritmo(alg), pontos));
        });
        return new Grafico()
                .tipo(Grafico.Tipo.BARRAS)
                .titulo("Speedup por natureza dos dados", e.rotulo() + " · em relação à versão serial")
                .categorias(categorias.stream().map(Distribuicao::rotulo).toList())
                .eixoY("Speedup", false, Formatos::numero)
                .referenciaY(1)
                .series(series);
    }

    public static Grafico tempoPorIntervalo(List<Resumo> resumos, Execucao e) {
        List<Serie> series = porAlgoritmo(resumos,
                r -> r.experimento().equals(INTERVALO) && r.execucao().equals(e),
                r -> new Serie.Ponto(r.valorMaximo(), r.mediaMs(), r.ic95Ms(), detalhe(r)), nome -> nome);
        int n = resumos.stream().filter(r -> r.experimento().equals(INTERVALO)).mapToInt(Resumo::tamanho)
                .findFirst().orElse(0);
        return new Grafico()
                .titulo("Tempo × intervalo de valores",
                        "Aleatório, n = " + Formatos.quantidade(n) + " · " + e.rotulo() + " · valores em [0, k)")
                .eixoX("Quantidade de valores possíveis (k)", true, Formatos::quantidade)
                .eixoY("Tempo médio (ms)", true, Formatos::numero)
                .series(series);
    }

    public static Grafico speedupPorIntervalo(List<Resumo> resumos, Execucao e) {
        List<Serie> series = porAlgoritmo(resumos,
                r -> r.experimento().equals(INTERVALO) && r.execucao().equals(e) && e.paralelo(),
                r -> new Serie.Ponto(r.valorMaximo(), r.speedup(), erroSpeedup(resumos, r, false), detalhe(r)),
                nome -> nome);
        return new Grafico()
                .titulo("Speedup × intervalo de valores", e.rotulo() + " · linha tracejada: sem ganho")
                .eixoX("Quantidade de valores possíveis (k)", true, Formatos::quantidade)
                .eixoY("Speedup", false, Formatos::numero)
                .referenciaY(1)
                .series(series);
    }

    public static List<Path> exportarFigurasRelatorio(List<Resumo> resumos, Path pasta) throws IOException {
        List<Integer> threads = Analise.threadsDisponiveis(resumos);
        if (threads.isEmpty()) {
            return List.of();
        }
        Execucao maximo = new Execucao(true, threads.get(threads.size() - 1));
        Distribuicao d = Distribuicao.ALEATORIO;
        Map<String, Grafico> figuras = new LinkedHashMap<>();
        figuras.put("01_tempo_tamanho_serial", tempoPorTamanho(resumos, d, Execucao.SERIAL));
        figuras.put("02_tempo_tamanho_paralelo", tempoPorTamanho(resumos, d, maximo));
        figuras.put("03_speedup_threads", speedupPorThreads(resumos, d, null, false));
        figuras.put("04_speedup_relativo_threads", speedupPorThreads(resumos, d, null, true));
        figuras.put("05_eficiencia_threads", eficienciaPorThreads(resumos, d, null));
        figuras.put("06_speedup_tamanho", speedupPorTamanho(resumos, d, maximo));
        figuras.put("07_distribuicao_tempo_relativo", tempoRelativoPorDistribuicao(resumos, Execucao.SERIAL));
        figuras.put("08_distribuicao_speedup", speedupPorDistribuicao(resumos, maximo));
        figuras.put("09_intervalo_tempo", tempoPorIntervalo(resumos, Execucao.SERIAL));
        figuras.put("10_intervalo_speedup", speedupPorIntervalo(resumos, maximo));

        List<Path> gerados = new ArrayList<>();
        for (Map.Entry<String, Grafico> f : figuras.entrySet()) {
            if (!f.getValue().vazio()) {
                Path arquivo = pasta.resolve(f.getKey() + ".png");
                f.getValue().salvarPng(arquivo, 900, 520);
                gerados.add(arquivo);
            }
        }
        return gerados;
    }

    public static String detalhe(Resumo r) {
        StringBuilder sb = new StringBuilder("<html><b>").append(r.algoritmo()).append("</b> · ")
                .append(r.execucao().rotulo())
                .append("<br>").append(r.distribuicao().rotulo())
                .append(", n = ").append(Formatos.inteiro(r.tamanho()))
                .append(", k = ").append(Formatos.inteiro(r.valorMaximo()))
                .append("<br>Média: ").append(Formatos.ms(r.mediaMs()))
                .append(" (IC 95% ± ").append(Formatos.numero(r.ic95Ms())).append(")")
                .append("<br>Desvio padrão: ").append(Formatos.numero(r.desvioMs()))
                .append(" · ").append(r.amostras()).append(" amostras");
        if (r.paralelo()) {
            sb.append("<br>Speedup: ").append(Formatos.numero(r.speedup())).append("×")
                    .append(" · relativo: ").append(Formatos.numero(r.speedupRelativo())).append("×");
        }
        return sb.append("</html>").toString();
    }

    private static List<Serie> porAlgoritmo(List<Resumo> resumos, Predicate<Resumo> filtro,
                                            Function<Resumo, Serie.Ponto> ponto,
                                            Function<String, String> nome) {
        Map<String, List<Serie.Ponto>> grupos = new LinkedHashMap<>();
        for (Resumo r : resumos) {
            if (filtro.test(r)) {
                grupos.computeIfAbsent(r.algoritmo(), k -> new ArrayList<>()).add(ponto.apply(r));
            }
        }
        List<Serie> series = new ArrayList<>();
        grupos.forEach((alg, pontos) -> series.add(Serie.de(nome.apply(alg), Paleta.doAlgoritmo(alg), pontos)));
        return series;
    }

    // n usado em cada algoritmo: o informado, se existir, ou o maior medido (Bubble Sort usa tamanhos menores)
    private static Map<String, Integer> tamanhoPorAlgoritmo(List<Resumo> resumos, Distribuicao d, Integer tamanho) {
        Map<String, Integer> mapa = new LinkedHashMap<>();
        for (Resumo r : resumos) {
            if (!r.experimento().equals(TAMANHO) || r.distribuicao() != d) {
                continue;
            }
            if (tamanho != null) {
                if (r.tamanho() == tamanho) {
                    mapa.put(r.algoritmo(), tamanho);
                }
            } else {
                mapa.merge(r.algoritmo(), r.tamanho(), Math::max);
            }
        }
        return mapa;
    }

    private static boolean noTamanho(Resumo r, Distribuicao d, Map<String, Integer> tamanhos) {
        return r.experimento().equals(TAMANHO) && r.distribuicao() == d
                && Objects.equals(tamanhos.get(r.algoritmo()), r.tamanho());
    }

    private static Optional<Resumo> busca(List<Resumo> resumos, String alg, Distribuicao d, int n, Execucao e) {
        return resumos.stream()
                .filter(r -> r.experimento().equals(TAMANHO) && r.algoritmo().equals(alg)
                        && r.distribuicao() == d && r.tamanho() == n && r.execucao().equals(e))
                .findFirst();
    }

    // propagação do IC das duas médias para a razão entre elas
    private static double erroSpeedup(List<Resumo> resumos, Resumo r, boolean relativo) {
        Execucao base = relativo ? new Execucao(true, 1) : Execucao.SERIAL;
        ToDoubleFunction<Resumo> s = relativo ? Resumo::speedupRelativo : Resumo::speedup;
        return resumos.stream()
                .filter(b -> b.experimento().equals(r.experimento()) && b.algoritmo().equals(r.algoritmo())
                        && b.distribuicao() == r.distribuicao() && b.tamanho() == r.tamanho()
                        && b.valorMaximo() == r.valorMaximo() && b.execucao().equals(base))
                .findFirst()
                .map(b -> s.applyAsDouble(r) * Math.hypot(b.ic95Ms() / b.mediaMs(), r.ic95Ms() / r.mediaMs()))
                .orElse(0.0);
    }

    private static List<Serie.Ponto> ideal(List<Resumo> resumos) {
        return Analise.threadsDisponiveis(resumos).stream().map(t -> new Serie.Ponto(t, t, 0, null)).toList();
    }

    private static double[] ticksThreads(List<Resumo> resumos) {
        return Analise.threadsDisponiveis(resumos).stream().mapToDouble(Integer::doubleValue).toArray();
    }
}
