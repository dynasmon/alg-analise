package ordenacao.benchmark;

import ordenacao.algoritmos.AlgoritmoOrdenacao;
import ordenacao.algoritmos.Algoritmos;
import ordenacao.dados.Distribuicao;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public final class Configuracao {

    public static final String EXPERIMENTO_TAMANHO = "tamanho";
    public static final String EXPERIMENTO_INTERVALO = "intervalo";

    private List<AlgoritmoOrdenacao> algoritmos = Algoritmos.todos();
    private List<Distribuicao> distribuicoes = List.of(Distribuicao.values());
    private int[] tamanhos = {10_000, 30_000, 100_000, 300_000, 1_000_000, 3_000_000, 10_000_000};
    private int[] tamanhosQuadraticos = {1_000, 2_500, 5_000, 10_000, 20_000, 40_000};
    private int[] threads = IntStream.rangeClosed(1, Runtime.getRuntime().availableProcessors()).toArray();
    private int amostras = 5;
    private int valorMaximo = 1_000_000;
    private boolean experimentoIntervalo = true;
    private int tamanhoIntervalo = 1_000_000;
    private int[] valoresMaximos = {100, 1_000, 10_000, 100_000, 1_000_000, 10_000_000};
    private long semente = 20251008L;

    public static Configuracao rapida() {
        Configuracao c = new Configuracao();
        c.tamanhos = new int[]{10_000, 100_000, 1_000_000};
        c.tamanhosQuadraticos = new int[]{1_000, 5_000, 10_000};
        c.threads = IntStream.of(1, 2, 4, 8).filter(t -> t <= Runtime.getRuntime().availableProcessors()).toArray();
        c.tamanhoIntervalo = 200_000;
        c.valoresMaximos = new int[]{100, 10_000, 1_000_000};
        return c;
    }

    public List<Cenario> cenarios() {
        List<Cenario> lista = new ArrayList<>();
        for (AlgoritmoOrdenacao alg : algoritmos) {
            for (Distribuicao d : distribuicoes) {
                for (int n : alg.quadratico() ? tamanhosQuadraticos : tamanhos) {
                    lista.add(new Cenario(EXPERIMENTO_TAMANHO, alg, d, n, d.intervaloEfetivo(valorMaximo)));
                }
            }
        }
        if (experimentoIntervalo) {
            for (AlgoritmoOrdenacao alg : algoritmos) {
                if (alg.quadratico()) {
                    continue;
                }
                for (int vmax : valoresMaximos) {
                    lista.add(new Cenario(EXPERIMENTO_INTERVALO, alg, Distribuicao.ALEATORIO, tamanhoIntervalo, vmax));
                }
            }
        }
        return lista;
    }

    public int totalMedicoes() {
        return cenarios().size() * (threads.length + 1) * amostras;
    }

    public List<AlgoritmoOrdenacao> algoritmos() {
        return algoritmos;
    }

    public Configuracao algoritmos(List<AlgoritmoOrdenacao> algoritmos) {
        this.algoritmos = List.copyOf(algoritmos);
        return this;
    }

    public List<Distribuicao> distribuicoes() {
        return distribuicoes;
    }

    public Configuracao distribuicoes(List<Distribuicao> distribuicoes) {
        this.distribuicoes = List.copyOf(distribuicoes);
        return this;
    }

    public int[] tamanhos() {
        return tamanhos.clone();
    }

    public Configuracao tamanhos(int[] tamanhos) {
        this.tamanhos = positivos(tamanhos, "tamanho");
        return this;
    }

    public int[] tamanhosQuadraticos() {
        return tamanhosQuadraticos.clone();
    }

    public Configuracao tamanhosQuadraticos(int[] tamanhos) {
        this.tamanhosQuadraticos = positivos(tamanhos, "tamanho");
        return this;
    }

    public int[] threads() {
        return threads.clone();
    }

    public Configuracao threads(int[] threads) {
        this.threads = positivos(threads, "threads");
        return this;
    }

    public int amostras() {
        return amostras;
    }

    public Configuracao amostras(int amostras) {
        if (amostras < 1) {
            throw new IllegalArgumentException("É preciso pelo menos uma amostra");
        }
        this.amostras = amostras;
        return this;
    }

    public int valorMaximo() {
        return valorMaximo;
    }

    public Configuracao valorMaximo(int valorMaximo) {
        this.valorMaximo = positivos(new int[]{valorMaximo}, "valor máximo")[0];
        return this;
    }

    public boolean experimentoIntervalo() {
        return experimentoIntervalo;
    }

    public Configuracao experimentoIntervalo(boolean ativo) {
        this.experimentoIntervalo = ativo;
        return this;
    }

    public int tamanhoIntervalo() {
        return tamanhoIntervalo;
    }

    public Configuracao tamanhoIntervalo(int tamanho) {
        this.tamanhoIntervalo = positivos(new int[]{tamanho}, "tamanho")[0];
        return this;
    }

    public int[] valoresMaximos() {
        return valoresMaximos.clone();
    }

    public Configuracao valoresMaximos(int[] valores) {
        this.valoresMaximos = positivos(valores, "valor máximo");
        return this;
    }

    public long semente() {
        return semente;
    }

    public Configuracao semente(long semente) {
        this.semente = semente;
        return this;
    }

    private static int[] positivos(int[] valores, String nome) {
        if (valores.length == 0) {
            throw new IllegalArgumentException("Informe pelo menos um valor de " + nome);
        }
        for (int v : valores) {
            if (v < 1) {
                throw new IllegalArgumentException("Valor inválido de " + nome + ": " + v);
            }
        }
        return valores.clone();
    }
}
