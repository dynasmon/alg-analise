package ordenacao.gui;

import ordenacao.benchmark.Medicao;
import ordenacao.benchmark.Resumo;
import ordenacao.grafico.Formatos;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.util.ArrayList;
import java.util.List;

final class Tabelas {

    private Tabelas() {
    }

    static JTable criar(AbstractTableModel modelo) {
        JTable tabela = new JTable(modelo);
        tabela.setAutoCreateRowSorter(true);
        tabela.setFillsViewportHeight(true);
        DefaultTableCellRenderer numeros = new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object valor) {
                setText(valor instanceof Double d ? Formatos.numero(d)
                        : valor instanceof Integer i ? Formatos.inteiro(i) : String.valueOf(valor));
            }
        };
        numeros.setHorizontalAlignment(SwingConstants.RIGHT);
        tabela.setDefaultRenderer(Double.class, numeros);
        tabela.setDefaultRenderer(Integer.class, numeros);
        return tabela;
    }

    static final class ModeloMedicoes extends AbstractTableModel {

        private static final String[] COLUNAS = {"Experimento", "Algoritmo", "Execução", "Distribuição", "n", "k",
                "Amostra", "Tempo (ms)"};
        private static final Class<?>[] TIPOS = {String.class, String.class, String.class, String.class,
                Integer.class, Integer.class, Integer.class, Double.class};

        private final List<Medicao> linhas = new ArrayList<>();

        void substituir(List<Medicao> medicoes) {
            linhas.clear();
            linhas.addAll(medicoes);
            fireTableDataChanged();
        }

        void adicionar(List<Medicao> novas) {
            if (novas.isEmpty()) {
                return;
            }
            int inicio = linhas.size();
            linhas.addAll(novas);
            fireTableRowsInserted(inicio, linhas.size() - 1);
        }

        @Override
        public int getRowCount() {
            return linhas.size();
        }

        @Override
        public int getColumnCount() {
            return COLUNAS.length;
        }

        @Override
        public String getColumnName(int coluna) {
            return COLUNAS[coluna];
        }

        @Override
        public Class<?> getColumnClass(int coluna) {
            return TIPOS[coluna];
        }

        @Override
        public Object getValueAt(int linha, int coluna) {
            Medicao m = linhas.get(linha);
            return switch (coluna) {
                case 0 -> m.experimento();
                case 1 -> m.algoritmo();
                case 2 -> m.execucao().rotulo();
                case 3 -> m.distribuicao().rotulo();
                case 4 -> m.tamanho();
                case 5 -> m.valorMaximo();
                case 6 -> m.amostra();
                default -> m.tempoMs();
            };
        }
    }

    static final class ModeloResumo extends AbstractTableModel {

        private static final String[] COLUNAS = {"Experimento", "Algoritmo", "Execução", "Distribuição", "n", "k",
                "Média (ms)", "Desvio (ms)", "CV (%)", "IC 95% (±ms)", "Speedup", "Speedup rel.", "Eficiência",
                "Karp-Flatt"};

        private List<Resumo> linhas = List.of();

        void substituir(List<Resumo> resumos) {
            linhas = List.copyOf(resumos);
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return linhas.size();
        }

        @Override
        public int getColumnCount() {
            return COLUNAS.length;
        }

        @Override
        public String getColumnName(int coluna) {
            return COLUNAS[coluna];
        }

        @Override
        public Class<?> getColumnClass(int coluna) {
            return coluna <= 3 ? String.class : coluna <= 5 ? Integer.class : Double.class;
        }

        @Override
        public Object getValueAt(int linha, int coluna) {
            Resumo r = linhas.get(linha);
            return switch (coluna) {
                case 0 -> r.experimento();
                case 1 -> r.algoritmo();
                case 2 -> r.execucao().rotulo();
                case 3 -> r.distribuicao().rotulo();
                case 4 -> r.tamanho();
                case 5 -> r.valorMaximo();
                case 6 -> r.mediaMs();
                case 7 -> r.desvioMs();
                case 8 -> r.coeficienteVariacao();
                case 9 -> r.ic95Ms();
                case 10 -> r.speedup();
                case 11 -> r.speedupRelativo();
                case 12 -> r.eficiencia();
                default -> r.karpFlatt();
            };
        }
    }
}
