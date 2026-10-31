package br.unifor.ordenacao.algoritmos;

public interface AlgoritmoOrdenacao {

    String nome();

    void ordenarSerial(int[] v);

    void ordenarParalelo(int[] v, int threads);

    default boolean quadratico() {
        return false;
    }
}
