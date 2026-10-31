package ordenacao.benchmark;

import ordenacao.dados.Distribuicao;

public record Resumo(String experimento, String algoritmo, Distribuicao distribuicao, int tamanho, int valorMaximo,
                     boolean paralelo, int threads, int amostras, double mediaMs, double desvioMs,
                     double medianaMs, double minMs, double maxMs, double ic95Ms,
                     double speedup, double speedupRelativo) {

    public String modo() {
        return paralelo ? "paralelo" : "serial";
    }

    public double coeficienteVariacao() {
        return mediaMs > 0 ? 100 * desvioMs / mediaMs : 0;
    }

    public double eficiencia() {
        return paralelo ? speedup / threads : Double.NaN;
    }

    // Métrica de Karp-Flatt: fração serial estimada a partir do speedup relativo
    public double karpFlatt() {
        if (!paralelo || threads < 2) {
            return Double.NaN;
        }
        return (1 / speedupRelativo - 1.0 / threads) / (1 - 1.0 / threads);
    }

    public Execucao execucao() {
        return new Execucao(paralelo, threads);
    }
}
