package ordenacao.algoritmos;

import java.util.concurrent.atomic.AtomicInteger;

// Barreira com espera ativa: as fases do odd-even são curtas demais para park/unpark
final class BarreiraAtiva {

    private static final int GIROS_ANTES_DE_CEDER = 1 << 12;

    private final int partes;
    private final Runnable acao;
    private final AtomicInteger chegadas = new AtomicInteger();
    private volatile int geracao;

    BarreiraAtiva(int partes, Runnable acao) {
        this.partes = partes;
        this.acao = acao;
    }

    void aguardar() {
        int g = geracao;
        if (chegadas.incrementAndGet() == partes) {
            acao.run();
            chegadas.set(0);
            geracao = g + 1;
            return;
        }
        for (int giros = 0; geracao == g; giros++) {
            if (giros < GIROS_ANTES_DE_CEDER) {
                Thread.onSpinWait();
            } else {
                Thread.yield();
            }
        }
    }
}
