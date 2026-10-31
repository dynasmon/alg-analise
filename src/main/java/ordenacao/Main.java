package ordenacao;

import ordenacao.algoritmos.AlgoritmoOrdenacao;
import ordenacao.algoritmos.Algoritmos;
import ordenacao.benchmark.Analise;
import ordenacao.benchmark.ArquivosCsv;
import ordenacao.benchmark.Benchmark;
import ordenacao.benchmark.Cenario;
import ordenacao.benchmark.Configuracao;
import ordenacao.benchmark.Crescimento;
import ordenacao.benchmark.Medicao;
import ordenacao.benchmark.Resumo;
import ordenacao.dados.Distribuicao;
import ordenacao.dados.GeradorDados;
import ordenacao.grafico.Formatos;
import ordenacao.grafico.Graficos;
import ordenacao.gui.JanelaPrincipal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

public final class Main {

    private static final String AJUDA = """
            Uso: java -jar alg-analise.jar [comando] [opções]

            Comandos:
              gui         abre a interface gráfica (padrão)
              benchmark   executa os experimentos e grava os CSVs e gráficos
              analisar    recalcula resumo, crescimento e gráficos a partir de medicoes.csv
              validar     confere se todos os algoritmos ordenam corretamente

            Opções do benchmark:
              --algoritmos bubble,quick,merge,counting
              --distribuicoes aleatorio,ordenado,inverso,quase_ordenado,repetidos
              --tamanhos 10000,100000,1000000      tamanhos para Quick, Merge e Counting
              --tamanhos-bubble 1000,5000,10000    tamanhos para o Bubble Sort
              --threads 1,2,4,8
              --amostras 5
              --valor-maximo 1000000               valores gerados em [0, valor-maximo)
              --intervalo-n 1000000                n do experimento de intervalo de valores
              --intervalos 100,10000,1000000       valores máximos testados nesse experimento
              --sem-intervalo                      não roda o experimento de intervalo de valores
              --semente 20251008
              --rapido                             configuração reduzida para testes
              --saida resultados                   pasta dos arquivos gerados
            """;

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        String comando = args.length == 0 ? "gui" : args[0];
        String[] opcoes = Arrays.copyOfRange(args, Math.min(1, args.length), args.length);
        switch (comando) {
            case "gui" -> JanelaPrincipal.abrir(pastaSaida(opcoes));
            case "benchmark" -> benchmark(opcoes);
            case "analisar" -> analisar(pastaSaida(opcoes));
            case "validar" -> System.exit(validar() ? 0 : 1);
            case "ajuda", "-h", "--help" -> System.out.print(AJUDA);
            default -> {
                System.err.println("Comando desconhecido: " + comando);
                System.err.print(AJUDA);
                System.exit(2);
            }
        }
    }

    private static void benchmark(String[] opcoes) throws IOException {
        System.setProperty("java.awt.headless", "true");
        Configuracao config = lerConfiguracao(opcoes);
        Path saida = pastaSaida(opcoes);
        List<Cenario> cenarios = config.cenarios();
        System.out.printf("%d cenários, %s medições (%d amostras cada), threads %s%n", cenarios.size(),
                Formatos.inteiro(config.totalMedicoes()), config.amostras(), Arrays.toString(config.threads()));
        System.out.println("Resultados em " + saida.toAbsolutePath());

        Files.createDirectories(saida);
        ArquivosCsv.gravarAmbiente(saida.resolve(ArquivosCsv.AMBIENTE), ambiente(config));
        long inicio = System.nanoTime();
        try (ArquivosCsv.Gravador gravador = new ArquivosCsv.Gravador(saida.resolve(ArquivosCsv.MEDICOES))) {
            new Benchmark(config).executar(new Benchmark.Ouvinte() {
                @Override
                public void aoIniciarCenario(Cenario cenario, int indice, int total) {
                    System.out.printf("[%3d/%d] %s%n", indice, total, cenario.descricao());
                }

                @Override
                public void aoMedir(Medicao medicao) {
                    gravador.gravar(medicao);
                }

                @Override
                public void aoConcluirCenario(Cenario cenario, List<Medicao> medicoes) {
                    System.out.println("          " + linhaResumo(Analise.resumir(medicoes)));
                }
            });
        }
        System.out.println("Tempo total: " + formatarDuracao(Duration.ofNanos(System.nanoTime() - inicio)));
        analisar(saida);
    }

    private static String linhaResumo(List<Resumo> resumos) {
        Resumo serial = resumos.stream().filter(r -> !r.paralelo()).findFirst().orElseThrow();
        Resumo melhor = resumos.stream().filter(Resumo::paralelo)
                .min(Comparator.comparingDouble(Resumo::mediaMs)).orElse(serial);
        return String.format("serial %s | melhor paralelo: %s com %s (speedup %s×, CV %s%%)",
                Formatos.ms(serial.mediaMs()), Formatos.ms(melhor.mediaMs()), melhor.execucao().rotulo(),
                Formatos.numero(melhor.speedup()), Formatos.numero(melhor.coeficienteVariacao()));
    }

    private static void analisar(Path pasta) throws IOException {
        System.setProperty("java.awt.headless", "true");
        List<Medicao> medicoes = ArquivosCsv.lerMedicoes(pasta.resolve(ArquivosCsv.MEDICOES));
        List<Resumo> resumos = Analise.resumir(medicoes);
        List<Crescimento> crescimento = Analise.crescimento(resumos);
        ArquivosCsv.gravarResumo(pasta.resolve(ArquivosCsv.RESUMO), resumos);
        ArquivosCsv.gravarCrescimento(pasta.resolve(ArquivosCsv.CRESCIMENTO), crescimento);
        List<Path> figuras = Graficos.exportarFigurasRelatorio(resumos, pasta.resolve("graficos"));
        System.out.printf("%d medições, %d linhas em %s, %d ajustes em %s, %d gráficos em %s%n",
                medicoes.size(), resumos.size(), ArquivosCsv.RESUMO, crescimento.size(), ArquivosCsv.CRESCIMENTO,
                figuras.size(), pasta.resolve("graficos"));
    }

    private static boolean validar() {
        SplittableRandom rnd = new SplittableRandom(7);
        boolean tudoCerto = true;
        for (AlgoritmoOrdenacao alg : Algoritmos.todos()) {
            int falhas = 0;
            int testes = 0;
            for (int caso = 0; caso < 400; caso++) {
                int n = caso < 20 ? caso : rnd.nextInt(alg.quadratico() ? 3_000 : 200_000);
                Distribuicao d = Distribuicao.values()[caso % Distribuicao.values().length];
                int[] v = GeradorDados.gerar(d, n, 1 + rnd.nextInt(1_000_000), rnd.nextLong());
                if (caso % 3 == 0) {
                    for (int i = 0; i < n; i++) {
                        v[i] -= 500_000;
                    }
                }
                int[] esperado = v.clone();
                Arrays.sort(esperado);
                int[] serial = v.clone();
                alg.ordenarSerial(serial);
                int[] paralelo = v.clone();
                alg.ordenarParalelo(paralelo, 1 + caso % 9);
                testes += 2;
                if (!Arrays.equals(serial, esperado)) {
                    falhas++;
                }
                if (!Arrays.equals(paralelo, esperado)) {
                    falhas++;
                }
            }
            System.out.printf("%-14s %d testes, %d falhas%n", alg.nome(), testes, falhas);
            tudoCerto &= falhas == 0;
        }
        System.out.println(tudoCerto ? "Todos os algoritmos ordenaram corretamente." : "Há falhas!");
        return tudoCerto;
    }

    private static Configuracao lerConfiguracao(String[] opcoes) {
        Configuracao c = Arrays.asList(opcoes).contains("--rapido") ? Configuracao.rapida() : new Configuracao();
        for (int i = 0; i < opcoes.length; i++) {
            String opcao = opcoes[i];
            switch (opcao) {
                case "--rapido" -> {
                }
                case "--sem-intervalo" -> c.experimentoIntervalo(false);
                case "--saida" -> i++;
                case "--algoritmos" -> c.algoritmos(Arrays.stream(valor(opcoes, ++i, opcao).split(","))
                        .map(Algoritmos::porNome).toList());
                case "--distribuicoes" -> c.distribuicoes(Arrays.stream(valor(opcoes, ++i, opcao).split(","))
                        .map(Distribuicao::porNome).toList());
                case "--tamanhos" -> c.tamanhos(inteiros(valor(opcoes, ++i, opcao)));
                case "--tamanhos-bubble" -> c.tamanhosQuadraticos(inteiros(valor(opcoes, ++i, opcao)));
                case "--threads" -> c.threads(inteiros(valor(opcoes, ++i, opcao)));
                case "--amostras" -> c.amostras(Integer.parseInt(valor(opcoes, ++i, opcao)));
                case "--valor-maximo" -> c.valorMaximo(Integer.parseInt(valor(opcoes, ++i, opcao)));
                case "--intervalo-n" -> c.tamanhoIntervalo(Integer.parseInt(valor(opcoes, ++i, opcao)));
                case "--intervalos" -> c.valoresMaximos(inteiros(valor(opcoes, ++i, opcao)));
                case "--semente" -> c.semente(Long.parseLong(valor(opcoes, ++i, opcao)));
                default -> throw new IllegalArgumentException("Opção desconhecida: " + opcao);
            }
        }
        return c;
    }

    private static Path pastaSaida(String[] opcoes) {
        for (int i = 0; i < opcoes.length - 1; i++) {
            if (opcoes[i].equals("--saida")) {
                return Path.of(opcoes[i + 1]);
            }
        }
        return Path.of("resultados");
    }

    private static String valor(String[] opcoes, int i, String opcao) {
        if (i >= opcoes.length) {
            throw new IllegalArgumentException("Faltou o valor de " + opcao);
        }
        return opcoes[i];
    }

    private static int[] inteiros(String lista) {
        return Arrays.stream(lista.split(",")).map(String::trim).map(s -> s.replace("_", ""))
                .mapToInt(Integer::parseInt).toArray();
    }

    private static Map<String, String> ambiente(Configuracao config) {
        Runtime rt = Runtime.getRuntime();
        Map<String, String> m = new LinkedHashMap<>();
        m.put("data", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        m.put("sistema", System.getProperty("os.name") + " " + System.getProperty("os.version"));
        m.put("arquitetura", System.getProperty("os.arch"));
        m.put("cpu", modeloCpu());
        m.put("processadores_logicos", String.valueOf(rt.availableProcessors()));
        m.put("java", System.getProperty("java.vm.name") + " " + System.getProperty("java.version"));
        m.put("heap_max_mb", String.valueOf(rt.maxMemory() / (1024 * 1024)));
        m.put("argumentos_jvm", String.join(" ", ManagementFactory.getRuntimeMXBean().getInputArguments()));
        m.put("amostras", String.valueOf(config.amostras()));
        m.put("threads", Arrays.toString(config.threads()));
        m.put("semente", String.valueOf(config.semente()));
        return m;
    }

    private static String modeloCpu() {
        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.contains("mac")) {
                return executar("sysctl", "-n", "machdep.cpu.brand_string");
            }
            if (os.contains("linux")) {
                return Files.readAllLines(Path.of("/proc/cpuinfo")).stream()
                        .filter(l -> l.startsWith("model name")).map(l -> l.substring(l.indexOf(':') + 1).trim())
                        .findFirst().orElse("desconhecido");
            }
            String id = System.getenv("PROCESSOR_IDENTIFIER");
            return id != null ? id : "desconhecido";
        } catch (IOException e) {
            return "desconhecido";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "desconhecido";
        }
    }

    private static String executar(String... comando) throws IOException, InterruptedException {
        Process p = new ProcessBuilder(comando).redirectErrorStream(true).start();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String linha = r.readLine();
            p.waitFor();
            return linha == null ? "desconhecido" : linha.trim();
        }
    }

    private static String formatarDuracao(Duration d) {
        return String.format("%dmin %02ds", d.toMinutes(), d.toSecondsPart());
    }
}
