package br.unifor.ordenacao.algoritmos;

import java.util.List;
import java.util.Locale;

public final class Algoritmos {

    private static final List<AlgoritmoOrdenacao> TODOS = List.of(
            new BubbleSort(), new QuickSort(), new MergeSort(), new CountingSort());

    private Algoritmos() {
    }

    public static List<AlgoritmoOrdenacao> todos() {
        return TODOS;
    }

    public static AlgoritmoOrdenacao porNome(String nome) {
        String procurado = nome.trim().toLowerCase(Locale.ROOT).replace(" ", "");
        return TODOS.stream()
                .filter(a -> a.nome().toLowerCase(Locale.ROOT).replace(" ", "").startsWith(procurado))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Algoritmo desconhecido: " + nome));
    }
}
