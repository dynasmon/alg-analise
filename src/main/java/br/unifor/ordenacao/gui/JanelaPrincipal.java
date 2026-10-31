package br.unifor.ordenacao.gui;

import br.unifor.ordenacao.algoritmos.AlgoritmoOrdenacao;
import br.unifor.ordenacao.algoritmos.Algoritmos;
import br.unifor.ordenacao.benchmark.Analise;
import br.unifor.ordenacao.benchmark.ArquivosCsv;
import br.unifor.ordenacao.benchmark.Benchmark;
import br.unifor.ordenacao.benchmark.Cenario;
import br.unifor.ordenacao.benchmark.Configuracao;
import br.unifor.ordenacao.benchmark.Execucao;
import br.unifor.ordenacao.benchmark.Medicao;
import br.unifor.ordenacao.benchmark.Resumo;
import br.unifor.ordenacao.dados.Distribuicao;
import br.unifor.ordenacao.grafico.Formatos;
import br.unifor.ordenacao.grafico.Grafico;
import br.unifor.ordenacao.grafico.Graficos;
import br.unifor.ordenacao.grafico.Paleta;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

public final class JanelaPrincipal extends JFrame {

    private enum Filtro { DISTRIBUICAO, EXECUCAO, THREADS, TAMANHO, RELATIVO }

    private record AbaGrafico(JPanel painel, EnumSet<Filtro> filtros, Supplier<Grafico> construtor) {
    }

    private static final String MAIOR_TAMANHO = "Maior n de cada algoritmo";

    private final Path pastaResultados;
    private final List<Medicao> medicoes = new ArrayList<>();
    private List<Resumo> resumos = List.of();

    private final Map<AlgoritmoOrdenacao, JCheckBox> algoritmos = new LinkedHashMap<>();
    private final Map<Distribuicao, JCheckBox> distribuicoes = new LinkedHashMap<>();
    private final JTextField campoTamanhos = new JTextField("10000, 100000, 1000000");
    private final JTextField campoTamanhosBubble = new JTextField("1000, 5000, 10000");
    private final JTextField campoThreads = new JTextField(threadsPadrao());
    private final JSpinner campoAmostras = new JSpinner(new SpinnerNumberModel(5, 1, 100, 1));
    private final JTextField campoValorMaximo = new JTextField("1000000");
    private final JCheckBox campoIntervalo = new JCheckBox("Variar também o k");
    private final JButton botaoExecutar = new JButton("Executar");
    private final JButton botaoParar = new JButton("Parar");

    private final JComboBox<Distribuicao> filtroDistribuicao = new JComboBox<>(Distribuicao.values());
    private final JComboBox<Execucao> filtroExecucao = new JComboBox<>();
    private final JComboBox<Execucao> filtroThreads = new JComboBox<>();
    private final JComboBox<Object> filtroTamanho = new JComboBox<>();
    private final JCheckBox filtroRelativo = new JCheckBox("Speedup relativo à versão com 1 thread");
    private final Map<Filtro, List<JComponent>> controlesFiltro = new LinkedHashMap<>();
    private boolean atualizandoFiltros;

    private final JTabbedPane abas = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
    private final List<AbaGrafico> abasGrafico = new ArrayList<>();
    private final Tabelas.ModeloMedicoes modeloMedicoes = new Tabelas.ModeloMedicoes();
    private final Tabelas.ModeloResumo modeloResumo = new Tabelas.ModeloResumo();
    private final JProgressBar progresso = new JProgressBar();
    private final JLabel status = new JLabel("Pronto");
    private final Timer atualizacao = new Timer(400, e -> atualizarTudo());
    private Grafico graficoVisivel;
    private Benchmark benchmark;

    public static void abrir(Path pastaResultados) {
        System.setProperty("apple.awt.application.name", "Análise de Ordenação");
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ReflectiveOperationException | UnsupportedLookAndFeelException e) {
                // segue com o visual padrão do Swing
            }
            JanelaPrincipal janela = new JanelaPrincipal(pastaResultados);
            janela.setVisible(true);
            janela.carregarResultadosIniciais();
        });
    }

    private JanelaPrincipal(Path pastaResultados) {
        super("Análise de desempenho de algoritmos de ordenação");
        this.pastaResultados = pastaResultados;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1050, 680));
        setSize(1340, 860);
        setLocationRelativeTo(null);

        atualizacao.setRepeats(false);
        JPanel centro = new JPanel(new BorderLayout());
        centro.add(criarFiltros(), BorderLayout.NORTH);
        centro.add(criarAbas(), BorderLayout.CENTER);

        getContentPane().setLayout(new BorderLayout());
        JScrollPane rolagem = new JScrollPane(criarPainelConfiguracao(),
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        rolagem.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Paleta.GRADE));
        rolagem.setPreferredSize(new Dimension(270, 0));
        getContentPane().add(rolagem, BorderLayout.WEST);
        getContentPane().add(centro, BorderLayout.CENTER);
        getContentPane().add(criarBarraStatus(), BorderLayout.SOUTH);
        atualizarFiltrosHabilitados();
    }

    private JPanel criarPainelConfiguracao() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;

        adicionar(p, c, secao("Algoritmos"), 0);
        for (AlgoritmoOrdenacao alg : Algoritmos.todos()) {
            JCheckBox caixa = new JCheckBox(alg.nome(), true);
            algoritmos.put(alg, caixa);
            adicionar(p, c, caixa, 0);
        }
        adicionar(p, c, secao("Natureza dos dados"), 10);
        for (Distribuicao d : Distribuicao.values()) {
            JCheckBox caixa = new JCheckBox(d.rotulo(), d == Distribuicao.ALEATORIO || d == Distribuicao.INVERSO);
            distribuicoes.put(d, caixa);
            adicionar(p, c, caixa, 0);
        }
        adicionar(p, c, secao("Parâmetros"), 10);
        adicionar(p, c, rotulo("Tamanhos de n"), 2);
        adicionar(p, c, campoTamanhos, 0);
        adicionar(p, c, rotulo("Tamanhos de n no Bubble Sort"), 6);
        adicionar(p, c, campoTamanhosBubble, 0);
        adicionar(p, c, rotulo("Threads"), 6);
        adicionar(p, c, campoThreads, 0);
        adicionar(p, c, rotulo("Amostras por execução"), 6);
        adicionar(p, c, campoAmostras, 0);
        adicionar(p, c, rotulo("Valores gerados em [0, k), k ="), 6);
        adicionar(p, c, campoValorMaximo, 0);
        adicionar(p, c, campoIntervalo, 6);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        botaoExecutar.addActionListener(e -> executar());
        botaoParar.addActionListener(e -> parar());
        botaoParar.setEnabled(false);
        botoes.add(botaoExecutar);
        botoes.add(botaoParar);
        adicionar(p, c, botoes, 14);

        adicionar(p, c, secao("Arquivos"), 14);
        JButton abrir = new JButton("Abrir medicoes.csv…");
        abrir.addActionListener(e -> abrirCsv());
        JButton salvar = new JButton("Salvar CSVs…");
        salvar.addActionListener(e -> salvarCsv());
        JButton png = new JButton("Exportar gráfico (PNG)…");
        png.addActionListener(e -> exportarPng());
        adicionar(p, c, abrir, 0);
        adicionar(p, c, salvar, 0);
        adicionar(p, c, png, 0);

        c.weighty = 1;
        adicionar(p, c, new JPanel(), 0);
        return p;
    }

    private JPanel criarFiltros() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        p.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Paleta.GRADE));
        registrarFiltro(p, Filtro.DISTRIBUICAO, "Distribuição", filtroDistribuicao);
        registrarFiltro(p, Filtro.EXECUCAO, "Execução", filtroExecucao);
        registrarFiltro(p, Filtro.THREADS, "Threads", filtroThreads);
        registrarFiltro(p, Filtro.TAMANHO, "Tamanho", filtroTamanho);
        controlesFiltro.put(Filtro.RELATIVO, List.of(filtroRelativo));
        filtroRelativo.addActionListener(e -> reconstruirGraficoVisivel());
        p.add(filtroRelativo);
        filtroTamanho.addItem(MAIOR_TAMANHO);
        return p;
    }

    private void registrarFiltro(JPanel p, Filtro filtro, String nome, JComboBox<?> combo) {
        JLabel rotulo = new JLabel(nome);
        combo.addActionListener(e -> {
            if (!atualizandoFiltros) {
                reconstruirGraficoVisivel();
            }
        });
        controlesFiltro.put(filtro, List.of(rotulo, combo));
        p.add(rotulo);
        p.add(combo);
    }

    private JTabbedPane criarAbas() {
        adicionarAba("Tempo × n", EnumSet.of(Filtro.DISTRIBUICAO, Filtro.EXECUCAO),
                () -> Graficos.tempoPorTamanho(resumos, distribuicao(), execucao(filtroExecucao)));
        adicionarAba("Speedup × threads", EnumSet.of(Filtro.DISTRIBUICAO, Filtro.TAMANHO, Filtro.RELATIVO),
                () -> Graficos.speedupPorThreads(resumos, distribuicao(), tamanho(), filtroRelativo.isSelected()));
        adicionarAba("Eficiência", EnumSet.of(Filtro.DISTRIBUICAO, Filtro.TAMANHO),
                () -> Graficos.eficienciaPorThreads(resumos, distribuicao(), tamanho()));
        adicionarAba("Speedup × n", EnumSet.of(Filtro.DISTRIBUICAO, Filtro.THREADS),
                () -> Graficos.speedupPorTamanho(resumos, distribuicao(), execucao(filtroThreads)));
        adicionarAba("Natureza dos dados", EnumSet.of(Filtro.EXECUCAO),
                () -> Graficos.tempoRelativoPorDistribuicao(resumos, execucao(filtroExecucao)));
        adicionarAba("Speedup por dados", EnumSet.of(Filtro.THREADS),
                () -> Graficos.speedupPorDistribuicao(resumos, execucao(filtroThreads)));
        adicionarAba("Intervalo de valores", EnumSet.of(Filtro.EXECUCAO),
                () -> Graficos.tempoPorIntervalo(resumos, execucao(filtroExecucao)));
        adicionarAba("Speedup × intervalo", EnumSet.of(Filtro.THREADS),
                () -> Graficos.speedupPorIntervalo(resumos, execucao(filtroThreads)));
        abas.addTab("Medições", new JScrollPane(Tabelas.criar(modeloMedicoes)));
        abas.addTab("Resumo estatístico", new JScrollPane(Tabelas.criar(modeloResumo)));
        abas.addChangeListener(e -> {
            atualizarFiltrosHabilitados();
            reconstruirGraficoVisivel();
        });
        return abas;
    }

    private void adicionarAba(String titulo, EnumSet<Filtro> filtros, Supplier<Grafico> construtor) {
        JPanel painel = new JPanel(new BorderLayout());
        painel.setBackground(Paleta.SUPERFICIE);
        abasGrafico.add(new AbaGrafico(painel, filtros, construtor));
        abas.addTab(titulo, painel);
    }

    private JPanel criarBarraStatus() {
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setBorder(BorderFactory.createEmptyBorder(4, 10, 6, 10));
        progresso.setStringPainted(true);
        progresso.setPreferredSize(new Dimension(260, 18));
        p.add(status, BorderLayout.CENTER);
        p.add(progresso, BorderLayout.EAST);
        return p;
    }

    private void carregarResultadosIniciais() {
        Path arquivo = pastaResultados.resolve(ArquivosCsv.MEDICOES);
        if (Files.exists(arquivo)) {
            carregar(arquivo);
        } else {
            reconstruirGraficoVisivel();
        }
    }

    private void executar() {
        Configuracao config;
        try {
            config = configuracaoDaTela();
        } catch (IllegalArgumentException e) {
            erro("Configuração inválida", e.getMessage());
            return;
        }
        medicoes.clear();
        modeloMedicoes.substituir(medicoes);
        atualizarTudo();
        benchmark = new Benchmark(config);
        Benchmark atual = benchmark;
        progresso.setMaximum(config.totalMedicoes());
        progresso.setValue(0);
        emExecucao(true);

        new SwingWorker<Void, Medicao>() {
            @Override
            protected Void doInBackground() {
                atual.executar(new Benchmark.Ouvinte() {
                    @Override
                    public void aoIniciarCenario(Cenario cenario, int indice, int total) {
                        SwingUtilities.invokeLater(() ->
                                status.setText("Cenário " + indice + " de " + total + ": " + cenario.descricao()));
                    }

                    @Override
                    public void aoMedir(Medicao medicao) {
                        publish(medicao);
                    }
                });
                return null;
            }

            @Override
            protected void process(List<Medicao> novas) {
                medicoes.addAll(novas);
                modeloMedicoes.adicionar(novas);
                progresso.setValue(medicoes.size());
                if (!atualizacao.isRunning()) {
                    atualizacao.start();
                }
            }

            @Override
            protected void done() {
                emExecucao(false);
                try {
                    get();
                    status.setText((atual.cancelado() ? "Execução interrompida: " : "Concluído: ")
                            + Formatos.inteiro(medicoes.size()) + " medições");
                } catch (ExecutionException e) {
                    erro("Erro durante a execução", String.valueOf(e.getCause().getMessage()));
                    status.setText("Execução terminou com erro");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                atualizarTudo();
            }
        }.execute();
    }

    private void parar() {
        if (benchmark != null) {
            benchmark.cancelar();
            status.setText("Parando ao fim da amostra atual…");
        }
    }

    private void emExecucao(boolean executando) {
        botaoExecutar.setEnabled(!executando);
        botaoParar.setEnabled(executando);
    }

    private Configuracao configuracaoDaTela() {
        List<AlgoritmoOrdenacao> algs = algoritmos.entrySet().stream()
                .filter(e -> e.getValue().isSelected()).map(Map.Entry::getKey).toList();
        List<Distribuicao> dists = distribuicoes.entrySet().stream()
                .filter(e -> e.getValue().isSelected()).map(Map.Entry::getKey).toList();
        if (algs.isEmpty() || dists.isEmpty()) {
            throw new IllegalArgumentException("Selecione pelo menos um algoritmo e uma distribuição.");
        }
        return new Configuracao()
                .algoritmos(algs)
                .distribuicoes(dists)
                .tamanhos(inteiros(campoTamanhos, "tamanhos"))
                .tamanhosQuadraticos(inteiros(campoTamanhosBubble, "tamanhos do Bubble Sort"))
                .threads(inteiros(campoThreads, "threads"))
                .amostras((Integer) campoAmostras.getValue())
                .valorMaximo(inteiros(campoValorMaximo, "k")[0])
                .experimentoIntervalo(campoIntervalo.isSelected())
                .tamanhoIntervalo(Arrays.stream(inteiros(campoTamanhos, "tamanhos")).max().orElse(100_000));
    }

    private void abrirCsv() {
        JFileChooser seletor = new JFileChooser(pastaResultados.toFile());
        seletor.setFileFilter(new FileNameExtensionFilter("Arquivos CSV", "csv"));
        if (seletor.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            carregar(seletor.getSelectedFile().toPath());
        }
    }

    private void carregar(Path arquivo) {
        try {
            List<Medicao> lidas = ArquivosCsv.lerMedicoes(arquivo);
            medicoes.clear();
            medicoes.addAll(lidas);
            modeloMedicoes.substituir(medicoes);
            atualizarTudo();
            status.setText("Carregado " + arquivo + " (" + Formatos.inteiro(lidas.size()) + " medições)");
        } catch (IOException e) {
            erro("Não foi possível abrir o arquivo", e.getMessage());
        }
    }

    private void salvarCsv() {
        JFileChooser seletor = new JFileChooser(pastaResultados.toFile());
        seletor.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        seletor.setDialogTitle("Escolha a pasta onde os CSVs serão gravados");
        if (seletor.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path pasta = seletor.getSelectedFile().toPath();
        try {
            ArquivosCsv.gravarMedicoes(pasta.resolve(ArquivosCsv.MEDICOES), medicoes);
            ArquivosCsv.gravarResumo(pasta.resolve(ArquivosCsv.RESUMO), resumos);
            ArquivosCsv.gravarCrescimento(pasta.resolve(ArquivosCsv.CRESCIMENTO), Analise.crescimento(resumos));
            status.setText("CSVs gravados em " + pasta);
        } catch (IOException e) {
            erro("Erro ao gravar os arquivos", e.getMessage());
        }
    }

    private void exportarPng() {
        if (graficoVisivel == null || graficoVisivel.vazio()) {
            erro("Nada para exportar", "Abra uma aba de gráfico que tenha dados.");
            return;
        }
        JFileChooser seletor = new JFileChooser(pastaResultados.toFile());
        seletor.setSelectedFile(new File("grafico.png"));
        if (seletor.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                graficoVisivel.salvarPng(seletor.getSelectedFile().toPath(), 900, 520);
                status.setText("Gráfico salvo em " + seletor.getSelectedFile());
            } catch (IOException e) {
                erro("Erro ao salvar o gráfico", e.getMessage());
            }
        }
    }

    private void atualizarTudo() {
        resumos = Analise.resumir(medicoes);
        modeloResumo.substituir(resumos);
        atualizarOpcoesDosFiltros();
        reconstruirGraficoVisivel();
    }

    private void atualizarOpcoesDosFiltros() {
        atualizandoFiltros = true;
        try {
            List<Execucao> paralelas = Analise.threadsDisponiveis(resumos).stream()
                    .map(t -> new Execucao(true, t)).toList();
            List<Execucao> todas = new ArrayList<>();
            todas.add(Execucao.SERIAL);
            todas.addAll(paralelas);
            repopular(filtroExecucao, todas, Execucao.SERIAL);
            repopular(filtroThreads, paralelas, paralelas.isEmpty() ? null : paralelas.get(paralelas.size() - 1));

            TreeSet<Integer> tamanhos = new TreeSet<>();
            resumos.stream().filter(r -> r.experimento().equals(Configuracao.EXPERIMENTO_TAMANHO))
                    .forEach(r -> tamanhos.add(r.tamanho()));
            List<Object> opcoes = new ArrayList<>();
            opcoes.add(MAIOR_TAMANHO);
            opcoes.addAll(tamanhos);
            repopular(filtroTamanho, opcoes, MAIOR_TAMANHO);
        } finally {
            atualizandoFiltros = false;
        }
    }

    private static <T> void repopular(JComboBox<T> combo, List<T> itens, T padrao) {
        Object selecionado = combo.getSelectedItem();
        List<T> atuais = new ArrayList<>();
        for (int i = 0; i < combo.getItemCount(); i++) {
            atuais.add(combo.getItemAt(i));
        }
        if (atuais.equals(itens)) {
            return;
        }
        combo.setModel(new DefaultComboBoxModel<>(new Vector<>(itens)));
        combo.setSelectedItem(itens.contains(selecionado) ? selecionado : padrao);
    }

    private void atualizarFiltrosHabilitados() {
        EnumSet<Filtro> ativos = abaAtual() != null ? abaAtual().filtros() : EnumSet.noneOf(Filtro.class);
        controlesFiltro.forEach((filtro, controles) ->
                controles.forEach(c -> c.setEnabled(ativos.contains(filtro))));
    }

    private void reconstruirGraficoVisivel() {
        AbaGrafico aba = abaAtual();
        if (aba == null) {
            graficoVisivel = null;
            return;
        }
        Grafico grafico = aba.construtor().get();
        aba.painel().removeAll();
        aba.painel().add(grafico, BorderLayout.CENTER);
        aba.painel().revalidate();
        aba.painel().repaint();
        graficoVisivel = grafico;
    }

    private AbaGrafico abaAtual() {
        int i = abas.getSelectedIndex();
        return i >= 0 && i < abasGrafico.size() ? abasGrafico.get(i) : null;
    }

    private Distribuicao distribuicao() {
        return (Distribuicao) filtroDistribuicao.getSelectedItem();
    }

    private static Execucao execucao(JComboBox<Execucao> combo) {
        Execucao e = (Execucao) combo.getSelectedItem();
        return e != null ? e : Execucao.SERIAL;
    }

    private Integer tamanho() {
        return filtroTamanho.getSelectedItem() instanceof Integer n ? n : null;
    }

    private static int[] inteiros(JTextField campo, String nome) {
        try {
            return Arrays.stream(campo.getText().split("[,;\\s]+")).filter(s -> !s.isBlank())
                    .map(s -> s.replace(".", "").replace("_", "")).mapToInt(Integer::parseInt).toArray();
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Valor inválido em " + nome + ": " + campo.getText());
        }
    }

    private static String threadsPadrao() {
        int nucleos = Runtime.getRuntime().availableProcessors();
        TreeSet<Integer> threads = new TreeSet<>();
        for (int t = 1; t <= nucleos; t *= 2) {
            threads.add(t);
        }
        threads.add(nucleos);
        return String.join(", ", threads.stream().map(String::valueOf).toList());
    }

    private static JLabel secao(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        return l;
    }

    private static JLabel rotulo(String texto) {
        JLabel l = new JLabel(texto);
        l.setForeground(Paleta.TINTA_SECUNDARIA);
        return l;
    }

    private static void adicionar(JPanel p, GridBagConstraints c, JComponent componente, int espacoAcima) {
        c.insets = new Insets(espacoAcima, 0, 2, 0);
        p.add(componente, c);
        c.gridy++;
    }

    private void erro(String titulo, String mensagem) {
        JOptionPane.showMessageDialog(this, mensagem, titulo, JOptionPane.ERROR_MESSAGE);
    }
}
