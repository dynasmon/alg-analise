package br.unifor.ordenacao.benchmark;

import br.unifor.ordenacao.dados.Distribuicao;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ArquivosCsv {

    public static final String MEDICOES = "medicoes.csv";
    public static final String RESUMO = "resumo.csv";
    public static final String CRESCIMENTO = "crescimento.csv";
    public static final String AMBIENTE = "ambiente.csv";

    private static final String CABECALHO_MEDICOES =
            "experimento,algoritmo,modo,threads,distribuicao,tamanho,valor_maximo,amostra,tempo_ms";

    private ArquivosCsv() {
    }

    public static final class Gravador implements Closeable {

        private final BufferedWriter saida;

        public Gravador(Path arquivo) throws IOException {
            criarPasta(arquivo);
            saida = Files.newBufferedWriter(arquivo, StandardCharsets.UTF_8);
            saida.write(CABECALHO_MEDICOES);
            saida.newLine();
        }

        // grava e descarrega na hora, para não perder dados se a execução for interrompida
        public synchronized void gravar(Medicao m) {
            try {
                saida.write(linha(m));
                saida.newLine();
                saida.flush();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public void close() throws IOException {
            saida.close();
        }
    }

    public static void gravarMedicoes(Path arquivo, List<Medicao> medicoes) throws IOException {
        try (Gravador g = new Gravador(arquivo)) {
            medicoes.forEach(g::gravar);
        }
    }

    public static List<Medicao> lerMedicoes(Path arquivo) throws IOException {
        List<String> linhas = Files.readAllLines(arquivo, StandardCharsets.UTF_8);
        if (linhas.isEmpty() || !linhas.get(0).trim().equals(CABECALHO_MEDICOES)) {
            throw new IOException("O arquivo não está no formato de " + MEDICOES + ": " + arquivo);
        }
        List<Medicao> medicoes = new ArrayList<>();
        for (int i = 1; i < linhas.size(); i++) {
            String linha = linhas.get(i).trim();
            if (linha.isEmpty()) {
                continue;
            }
            String[] c = linha.split(",");
            try {
                medicoes.add(new Medicao(c[0], c[1], Distribuicao.valueOf(c[4]), Integer.parseInt(c[5]),
                        Integer.parseInt(c[6]), c[2].equals("paralelo"), Integer.parseInt(c[3]),
                        Integer.parseInt(c[7]), Double.parseDouble(c[8])));
            } catch (RuntimeException e) {
                throw new IOException("Linha " + (i + 1) + " inválida em " + arquivo + ": " + linha, e);
            }
        }
        return medicoes;
    }

    public static void gravarResumo(Path arquivo, List<Resumo> resumos) throws IOException {
        List<String> linhas = new ArrayList<>();
        linhas.add("experimento,algoritmo,modo,threads,distribuicao,tamanho,valor_maximo,amostras,media_ms,"
                + "desvio_ms,cv_pct,mediana_ms,min_ms,max_ms,ic95_ms,speedup,speedup_relativo,eficiencia,karp_flatt");
        for (Resumo r : resumos) {
            linhas.add(String.join(",", r.experimento(), r.algoritmo(), r.modo(), String.valueOf(r.threads()),
                    r.distribuicao().name(), String.valueOf(r.tamanho()), String.valueOf(r.valorMaximo()),
                    String.valueOf(r.amostras()), num(r.mediaMs()), num(r.desvioMs()), num(r.coeficienteVariacao()),
                    num(r.medianaMs()), num(r.minMs()), num(r.maxMs()), num(r.ic95Ms()), num(r.speedup()),
                    num(r.speedupRelativo()), num(r.eficiencia()), num(r.karpFlatt())));
        }
        gravarLinhas(arquivo, linhas);
    }

    public static void gravarCrescimento(Path arquivo, List<Crescimento> lista) throws IOException {
        List<String> linhas = new ArrayList<>();
        linhas.add("algoritmo,modo,threads,distribuicao,pontos,menor_tamanho,maior_tamanho,expoente,r2");
        for (Crescimento c : lista) {
            linhas.add(String.join(",", c.algoritmo(), c.modo(), String.valueOf(c.threads()),
                    c.distribuicao().name(), String.valueOf(c.pontos()), String.valueOf(c.menorTamanho()),
                    String.valueOf(c.maiorTamanho()), num(c.expoente()), num(c.r2())));
        }
        gravarLinhas(arquivo, linhas);
    }

    public static void gravarAmbiente(Path arquivo, Map<String, String> ambiente) throws IOException {
        List<String> linhas = new ArrayList<>();
        linhas.add("chave,valor");
        ambiente.forEach((k, v) -> linhas.add(k + "," + campo(v)));
        gravarLinhas(arquivo, linhas);
    }

    private static String linha(Medicao m) {
        return String.join(",", m.experimento(), m.algoritmo(), m.modo(), String.valueOf(m.threads()),
                m.distribuicao().name(), String.valueOf(m.tamanho()), String.valueOf(m.valorMaximo()),
                String.valueOf(m.amostra()), num(m.tempoMs()));
    }

    private static String num(double v) {
        return Double.isNaN(v) || Double.isInfinite(v) ? "" : String.format(Locale.ROOT, "%.4f", v);
    }

    private static String campo(String valor) {
        if (valor.contains(",") || valor.contains("\"")) {
            return "\"" + valor.replace("\"", "\"\"") + "\"";
        }
        return valor;
    }

    private static void gravarLinhas(Path arquivo, List<String> linhas) throws IOException {
        criarPasta(arquivo);
        Files.write(arquivo, linhas, StandardCharsets.UTF_8);
    }

    private static void criarPasta(Path arquivo) throws IOException {
        Path pasta = arquivo.toAbsolutePath().getParent();
        if (pasta != null) {
            Files.createDirectories(pasta);
        }
    }
}
