package br.unifor.ordenacao.grafico;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.ToolTipManager;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.DoubleFunction;

public final class Grafico extends JComponent {

    public enum Tipo { LINHAS, BARRAS }

    private static final Font FONTE_TITULO = new Font(Font.SANS_SERIF, Font.BOLD, 15);
    private static final Font FONTE = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    private static final BasicStroke LINHA = new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final BasicStroke FINA = new BasicStroke(1f);
    private static final BasicStroke TRACEJADA = new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
            10f, new float[]{5f, 4f}, 0f);
    private static final int RAIO = 4;
    private static final int LARGURA_MAX_BARRA = 24;
    private static final int MARGEM = 20;

    private Tipo tipo = Tipo.LINHAS;
    private String titulo = "";
    private String subtitulo = "";
    private String rotuloX = "";
    private String rotuloY = "";
    private boolean logX;
    private boolean logY;
    private DoubleFunction<String> formatoX = Formatos::numero;
    private DoubleFunction<String> formatoY = Formatos::numero;
    private double[] ticksX;
    private List<String> categorias = List.of();
    private List<Serie> series = List.of();
    private double referenciaY = Double.NaN;

    private record Alvo(Shape area, double x, double y, Color cor, String texto) {
    }

    private final List<Alvo> alvos = new ArrayList<>();
    private Alvo destacado;

    public Grafico() {
        setPreferredSize(new Dimension(900, 520));
        setOpaque(true);
        ToolTipManager.sharedInstance().registerComponent(this);
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                Alvo alvo = alvoEm(e.getX(), e.getY());
                if (alvo != destacado) {
                    destacado = alvo;
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                destacado = null;
                repaint();
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    public Grafico tipo(Tipo tipo) {
        this.tipo = tipo;
        return this;
    }

    public Grafico titulo(String titulo, String subtitulo) {
        this.titulo = titulo;
        this.subtitulo = subtitulo == null ? "" : subtitulo;
        return this;
    }

    public Grafico eixoX(String rotulo, boolean log, DoubleFunction<String> formato) {
        this.rotuloX = rotulo;
        this.logX = log;
        this.formatoX = formato;
        return this;
    }

    public Grafico eixoY(String rotulo, boolean log, DoubleFunction<String> formato) {
        this.rotuloY = rotulo;
        this.logY = log;
        this.formatoY = formato;
        return this;
    }

    public Grafico ticksX(double[] ticks) {
        this.ticksX = ticks;
        return this;
    }

    public Grafico categorias(List<String> categorias) {
        this.categorias = List.copyOf(categorias);
        return this;
    }

    public Grafico referenciaY(double valor) {
        this.referenciaY = valor;
        return this;
    }

    public Grafico series(List<Serie> series) {
        this.series = List.copyOf(series);
        destacado = null;
        repaint();
        return this;
    }

    public boolean vazio() {
        return series.stream().noneMatch(s -> !s.referencia() && !s.pontos().isEmpty());
    }

    public void salvarPng(Path arquivo, int largura, int altura) throws IOException {
        int escala = 2;
        BufferedImage img = new BufferedImage(largura * escala, altura * escala, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.scale(escala, escala);
        Alvo anterior = destacado;
        destacado = null;
        desenhar(g, largura, altura);
        destacado = anterior;
        g.dispose();
        Path pasta = arquivo.toAbsolutePath().getParent();
        if (pasta != null) {
            Files.createDirectories(pasta);
        }
        ImageIO.write(img, "png", arquivo.toFile());
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        Alvo alvo = alvoEm(e.getX(), e.getY());
        return alvo == null ? null : alvo.texto();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            desenhar(g2, getWidth(), getHeight());
        } finally {
            g2.dispose();
        }
    }

    private Alvo alvoEm(int x, int y) {
        Alvo melhor = null;
        double menor = Double.MAX_VALUE;
        for (Alvo a : alvos) {
            if (a.area().contains(x, y)) {
                double d = Math.hypot(a.x() - x, a.y() - y);
                if (d < menor) {
                    menor = d;
                    melhor = a;
                }
            }
        }
        return melhor;
    }

    private void desenhar(Graphics2D g, int w, int h) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setColor(Paleta.SUPERFICIE);
        g.fillRect(0, 0, w, h);
        alvos.clear();

        int y = 14;
        g.setFont(FONTE_TITULO);
        y += g.getFontMetrics().getAscent();
        g.setColor(Paleta.TINTA);
        g.drawString(titulo, MARGEM, y);
        g.setFont(FONTE);
        if (!subtitulo.isEmpty()) {
            y += g.getFontMetrics().getHeight() + 2;
            g.setColor(Paleta.TINTA_SECUNDARIA);
            g.drawString(subtitulo, MARGEM, y);
        }
        y = desenharLegenda(g, MARGEM, y + 14, w - 2 * MARGEM);

        if (vazio()) {
            g.setColor(Paleta.TINTA_SUAVE);
            String msg = "Sem dados para os filtros selecionados";
            FontMetrics fm = g.getFontMetrics();
            g.drawString(msg, (w - fm.stringWidth(msg)) / 2, (y + h) / 2);
            return;
        }

        Eixo ex = tipo == Tipo.BARRAS ? Eixo.categorico(categorias.size()) : eixoX();
        Eixo ey = eixoY();
        FontMetrics fm = g.getFontMetrics();
        int larguraTicks = 0;
        for (double t : ey.ticks) {
            larguraTicks = Math.max(larguraTicks, fm.stringWidth(formatoY.apply(t)));
        }
        int esquerda = MARGEM + (rotuloY.isEmpty() ? 0 : fm.getHeight() + 6) + larguraTicks + 8;
        int base = h - MARGEM - (rotuloX.isEmpty() ? 0 : fm.getHeight() + 4) - fm.getHeight() - 4;
        int topo = y + 22;
        int direita = w - MARGEM - 6;
        ex.p0 = esquerda;
        ex.p1 = direita;
        ey.p0 = base;
        ey.p1 = topo;

        desenharEixos(g, ex, ey, esquerda, direita, topo, base);
        if (tipo == Tipo.BARRAS) {
            desenharBarras(g, ex, ey);
        } else {
            desenharLinhas(g, ex, ey);
        }
    }

    private int desenharLegenda(Graphics2D g, int x0, int y0, int largura) {
        FontMetrics fm = g.getFontMetrics();
        int x = x0;
        int y = y0;
        for (Serie s : series) {
            if (s.pontos().isEmpty()) {
                continue;
            }
            int item = 26 + fm.stringWidth(s.nome()) + 18;
            if (x + item > x0 + largura && x > x0) {
                x = x0;
                y += fm.getHeight() + 6;
            }
            int meio = y - fm.getAscent() / 2 + 1;
            if (tipo == Tipo.BARRAS && !s.referencia()) {
                g.setColor(s.cor());
                g.fill(new RoundRectangle2D.Double(x + 4, meio - 5, 10, 10, 3, 3));
            } else {
                g.setColor(s.cor());
                g.setStroke(s.referencia() ? TRACEJADA : LINHA);
                g.draw(new Line2D.Double(x, meio, x + 18, meio));
                if (!s.referencia()) {
                    marcador(g, x + 9, meio, s.cor(), RAIO);
                }
            }
            g.setColor(Paleta.TINTA_SECUNDARIA);
            g.drawString(s.nome(), x + 26, y);
            x += item;
        }
        return y + 4;
    }

    private void desenharEixos(Graphics2D g, Eixo ex, Eixo ey, int esq, int dir, int topo, int base) {
        FontMetrics fm = g.getFontMetrics();
        g.setStroke(FINA);
        for (double t : ey.ticks) {
            double py = ey.pixel(t);
            g.setColor(Paleta.GRADE);
            g.draw(new Line2D.Double(esq, py, dir, py));
            String rotulo = formatoY.apply(t);
            g.setColor(Paleta.TINTA_SUAVE);
            g.drawString(rotulo, esq - 8 - fm.stringWidth(rotulo), (float) (py + fm.getAscent() / 2.0 - 1));
        }
        if (tipo == Tipo.BARRAS) {
            for (int i = 0; i < categorias.size(); i++) {
                String rotulo = categorias.get(i);
                double px = ex.pixel(i);
                g.setColor(Paleta.TINTA_SECUNDARIA);
                g.drawString(rotulo, (float) (px - fm.stringWidth(rotulo) / 2.0), base + fm.getAscent() + 6);
            }
        } else {
            for (double t : ex.ticks) {
                double px = ex.pixel(t);
                g.setColor(Paleta.GRADE);
                g.draw(new Line2D.Double(px, topo, px, base));
                String rotulo = formatoX.apply(t);
                g.setColor(Paleta.TINTA_SUAVE);
                g.drawString(rotulo, (float) (px - fm.stringWidth(rotulo) / 2.0), base + fm.getAscent() + 6);
            }
        }
        g.setColor(Paleta.EIXO);
        g.draw(new Line2D.Double(esq, base, dir, base));

        g.setColor(Paleta.TINTA_SECUNDARIA);
        if (!rotuloX.isEmpty()) {
            int largura = fm.stringWidth(rotuloX);
            g.drawString(rotuloX, (esq + dir - largura) / 2f, base + 2 * fm.getHeight() + 8);
        }
        if (!rotuloY.isEmpty()) {
            Graphics2D gr = (Graphics2D) g.create();
            gr.translate(MARGEM + fm.getAscent(), (topo + base) / 2.0 + fm.stringWidth(rotuloY) / 2.0);
            gr.rotate(-Math.PI / 2);
            gr.drawString(rotuloY, 0, 0);
            gr.dispose();
        }
        if (!Double.isNaN(referenciaY) && referenciaY >= ey.min && referenciaY <= ey.max) {
            double py = ey.pixel(referenciaY);
            g.setColor(Paleta.TINTA_SUAVE);
            g.setStroke(TRACEJADA);
            g.draw(new Line2D.Double(esq, py, dir, py));
        }
    }

    private void desenharLinhas(Graphics2D g, Eixo ex, Eixo ey) {
        for (Serie s : series) {
            if (s.referencia()) {
                g.setColor(s.cor());
                g.setStroke(TRACEJADA);
                g.draw(caminho(validos(s), ex, ey));
            }
        }
        for (Serie s : series) {
            if (s.referencia()) {
                continue;
            }
            List<Serie.Ponto> pontos = validos(s);
            g.setColor(s.cor());
            g.setStroke(LINHA);
            g.draw(caminho(pontos, ex, ey));
            g.setStroke(FINA);
            for (Serie.Ponto p : pontos) {
                if (p.erro() > 0) {
                    double px = ex.pixel(p.x());
                    double cima = ey.pixel(Math.min(p.y() + p.erro(), ey.max));
                    double baixo = ey.pixel(Math.max(p.y() - p.erro(), ey.log ? ey.min : p.y() - p.erro()));
                    if (baixo - cima > 3) {
                        g.draw(new Line2D.Double(px, cima, px, baixo));
                        g.draw(new Line2D.Double(px - 3, cima, px + 3, cima));
                        g.draw(new Line2D.Double(px - 3, baixo, px + 3, baixo));
                    }
                }
            }
        }
        for (Serie s : series) {
            if (s.referencia()) {
                continue;
            }
            for (Serie.Ponto p : validos(s)) {
                double px = ex.pixel(p.x());
                double py = ey.pixel(p.y());
                marcador(g, px, py, s.cor(), RAIO);
                String texto = p.detalhe() != null ? p.detalhe()
                        : s.nome() + ": " + formatoX.apply(p.x()) + " → " + formatoY.apply(p.y());
                alvos.add(new Alvo(new Ellipse2D.Double(px - 10, py - 10, 20, 20), px, py, s.cor(), texto));
            }
        }
        if (destacado != null) {
            g.setColor(Paleta.EIXO);
            g.setStroke(FINA);
            g.draw(new Line2D.Double(destacado.x(), ey.p1, destacado.x(), ey.p0));
            marcador(g, destacado.x(), destacado.y(), destacado.cor(), RAIO + 2);
        }
    }

    private void desenharBarras(Graphics2D g, Eixo ex, Eixo ey) {
        List<Serie> visiveis = series.stream().filter(s -> !s.referencia() && !s.pontos().isEmpty()).toList();
        double banda = (ex.p1 - ex.p0) / Math.max(1, categorias.size());
        int m = visiveis.size();
        double largura = Math.min(LARGURA_MAX_BARRA, (banda * 0.75 - 2.0 * (m - 1)) / m);
        double total = m * largura + 2.0 * (m - 1);
        double zero = ey.pixel(ey.log ? ey.min : 0);
        for (int k = 0; k < m; k++) {
            Serie s = visiveis.get(k);
            for (Serie.Ponto p : s.pontos()) {
                if (!valido(p.y(), ey.log)) {
                    continue;
                }
                double x = ex.pixel(p.x()) - total / 2 + k * (largura + 2);
                double topo = ey.pixel(p.y());
                Shape barra = barraArredondada(x, topo, largura, zero - topo);
                g.setColor(s.cor());
                g.fill(barra);
                if (p.erro() > 0) {
                    double cima = ey.pixel(Math.min(p.y() + p.erro(), ey.max));
                    double baixo = ey.pixel(Math.max(p.y() - p.erro(), ey.log ? ey.min : 0));
                    if (baixo - cima > 3) {
                        double cx = x + largura / 2;
                        g.setColor(Paleta.TINTA_SECUNDARIA);
                        g.setStroke(FINA);
                        g.draw(new Line2D.Double(cx, cima, cx, baixo));
                        g.draw(new Line2D.Double(cx - 3, cima, cx + 3, cima));
                        g.draw(new Line2D.Double(cx - 3, baixo, cx + 3, baixo));
                    }
                }
                String texto = p.detalhe() != null ? p.detalhe() : s.nome() + ": " + formatoY.apply(p.y());
                Rectangle2D area = new Rectangle2D.Double(x - 1, Math.min(topo, zero - 8) - 2, largura + 2,
                        Math.max(zero - topo, 8) + 4);
                alvos.add(new Alvo(area, x + largura / 2, topo, s.cor(), texto));
            }
        }
        if (destacado != null) {
            g.setColor(Paleta.TINTA);
            g.setStroke(FINA);
            g.draw(destacado.area());
        }
    }

    private static Shape barraArredondada(double x, double y, double largura, double altura) {
        double r = Math.min(4, Math.min(largura / 2, altura));
        Path2D.Double p = new Path2D.Double();
        p.moveTo(x, y + altura);
        p.lineTo(x, y + r);
        p.quadTo(x, y, x + r, y);
        p.lineTo(x + largura - r, y);
        p.quadTo(x + largura, y, x + largura, y + r);
        p.lineTo(x + largura, y + altura);
        p.closePath();
        return p;
    }

    private static void marcador(Graphics2D g, double x, double y, Color cor, int raio) {
        g.setColor(Paleta.SUPERFICIE);
        g.fill(new Ellipse2D.Double(x - raio - 2, y - raio - 2, 2 * raio + 4, 2 * raio + 4));
        g.setColor(cor);
        g.fill(new Ellipse2D.Double(x - raio, y - raio, 2 * raio, 2 * raio));
    }

    private List<Serie.Ponto> validos(Serie s) {
        return s.pontos().stream()
                .filter(p -> valido(p.x(), logX) && valido(p.y(), logY))
                .sorted(Comparator.comparingDouble(Serie.Ponto::x))
                .toList();
    }

    private static boolean valido(double v, boolean log) {
        return !Double.isNaN(v) && !Double.isInfinite(v) && (!log || v > 0);
    }

    private static Path2D caminho(List<Serie.Ponto> pontos, Eixo ex, Eixo ey) {
        Path2D.Double p = new Path2D.Double();
        for (int i = 0; i < pontos.size(); i++) {
            double px = ex.pixel(pontos.get(i).x());
            double py = ey.pixel(pontos.get(i).y());
            if (i == 0) {
                p.moveTo(px, py);
            } else {
                p.lineTo(px, py);
            }
        }
        return p;
    }

    private Eixo eixoX() {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (Serie s : series) {
            for (Serie.Ponto p : validos(s)) {
                min = Math.min(min, p.x());
                max = Math.max(max, p.x());
            }
        }
        return Eixo.de(logX, min, max, ticksX, false);
    }

    private Eixo eixoY() {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (Serie s : series) {
            for (Serie.Ponto p : validos(s)) {
                min = Math.min(min, logY ? p.y() : p.y() - p.erro());
                max = Math.max(max, p.y() + p.erro());
            }
        }
        if (!Double.isNaN(referenciaY)) {
            min = Math.min(min, referenciaY);
            max = Math.max(max, referenciaY);
        }
        return Eixo.de(logY, min, max, null, !logY);
    }

    private static final class Eixo {

        final boolean log;
        final double min;
        final double max;
        final double[] ticks;
        double p0;
        double p1;

        private Eixo(boolean log, double min, double max, double[] ticks) {
            this.log = log;
            this.min = min;
            this.max = max;
            this.ticks = ticks;
        }

        static Eixo categorico(int n) {
            return new Eixo(false, -0.5, n - 0.5, new double[0]);
        }

        static Eixo de(boolean log, double min, double max, double[] explicitos, boolean incluirZero) {
            if (log) {
                double a = Math.log10(min);
                double b = Math.log10(max);
                if (b - a < 1e-9) {
                    a -= 0.5;
                    b += 0.5;
                }
                double folga = (b - a) * 0.05;
                double lo = Math.pow(10, a - folga);
                double hi = Math.pow(10, b + folga);
                return new Eixo(true, lo, hi, ticksLog(lo, hi));
            }
            if (incluirZero) {
                min = Math.min(0, min);
            }
            if (max - min < 1e-12) {
                max = min + 1;
            }
            if (explicitos != null && explicitos.length > 0) {
                double lo = Math.min(min, explicitos[0]);
                double hi = Math.max(max, explicitos[explicitos.length - 1]);
                double folga = (hi - lo) * 0.04;
                return new Eixo(false, lo - folga, hi + folga, explicitos);
            }
            double passo = passoArredondado((max - min) / 5);
            double lo = Math.floor(min / passo) * passo;
            double hi = Math.ceil(max / passo - 1e-9) * passo;
            int n = (int) Math.round((hi - lo) / passo);
            double[] ticks = new double[n + 1];
            for (int i = 0; i <= n; i++) {
                ticks[i] = lo + i * passo;
            }
            return new Eixo(false, lo, hi, ticks);
        }

        double pixel(double v) {
            double t = log ? (Math.log10(v) - Math.log10(min)) / (Math.log10(max) - Math.log10(min))
                    : (v - min) / (max - min);
            return p0 + t * (p1 - p0);
        }

        private static double passoArredondado(double bruto) {
            double potencia = Math.pow(10, Math.floor(Math.log10(bruto)));
            double f = bruto / potencia;
            return (f <= 1 ? 1 : f <= 2 ? 2 : f <= 5 ? 5 : 10) * potencia;
        }

        private static double[] ticksLog(double lo, double hi) {
            int d0 = (int) Math.floor(Math.log10(lo));
            int d1 = (int) Math.ceil(Math.log10(hi));
            int[] mantissas = d1 - d0 <= 2 ? new int[]{1, 2, 5} : new int[]{1};
            List<Double> ticks = new ArrayList<>();
            for (int d = d0; d <= d1; d++) {
                for (int m : mantissas) {
                    double v = m * Math.pow(10, d);
                    if (v >= lo * 0.999 && v <= hi * 1.001) {
                        ticks.add(v);
                    }
                }
            }
            return ticks.stream().mapToDouble(Double::doubleValue).toArray();
        }
    }
}
