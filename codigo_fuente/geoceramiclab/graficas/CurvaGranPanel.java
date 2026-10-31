package geoceramiclab.graficas;

import geoceramiclab.Tema;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class CurvaGranPanel extends JPanel {

    private static final Color FONDO      = new Color(250,251,254);
    private static final Color AREA_PLOT  = Color.WHITE;
    private static final Color BORDE      = new Color(70,82,110);
    private static final Color GRID_MAY   = new Color(196,204,222);
    private static final Color GRID_MEN   = new Color(232,236,245);
    private static final Color TXT        = new Color(36,44,66);
    private static final Color TXT_SUAVE  = new Color(104,116,144);
    private static final Color CURVA      = new Color(37,99,235);
    private static final Color CURVA_AREA = new Color(37,99,235,28);
    private static final Color Z_GRAVA    = new Color(180,140,90,22);
    private static final Color Z_ARENA    = new Color(230,190,80,20);
    private static final Color Z_FINOS    = new Color(120,150,200,24);
    private static final Color TAMIZ_LIN  = new Color(196,120,40);
    private static final Color D_LIN      = new Color(16,150,110);

    private static final double N4   = 4.75;
    private static final double N200 = 0.075;

    private Font F_TITULO, F_EJE, F_TICK, F_PUNTO, F_ZONA, F_NOTA;

    private float esc = 1f;

    private List<double[]> puntos = new ArrayList<>();
    private boolean mostrarEtiquetas = true;
    private boolean mostrarD = true;

    private Rectangle plot = new Rectangle();
    private double logMin = -2, logMax = 2;

    private boolean escalaAuto = true;
    private double yMin = 0;
    private double yTop = 100;
    private double yStep = 10;

    public CurvaGranPanel() {
        setBackground(FONDO);
        setPreferredSize(new Dimension(640, 440));
        setToolTipText("");
        instalarMenuCopiar();
    }

    private void instalarMenuCopiar() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem copiar  = new JMenuItem("Copiar imagen");
        JMenuItem guardar = new JMenuItem("Guardar como PNG…");
        copiar.addActionListener(e -> copiarAlPortapapeles());
        guardar.addActionListener(e -> guardarPNG());
        JCheckBoxMenuItem completa = new JCheckBoxMenuItem("Escala Y completa 0–100 %", !escalaAuto);
        completa.addActionListener(e -> { escalaAuto = !completa.isSelected(); repaint(); });
        menu.add(copiar);
        menu.add(guardar);
        menu.addSeparator();
        menu.add(completa);
        setComponentPopupMenu(menu);

        setFocusable(true);
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mousePressed(java.awt.event.MouseEvent e) { requestFocusInWindow(); }
        });
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("control C"), "copiarImg");
        getActionMap().put("copiarImg", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { copiarAlPortapapeles(); }
        });
    }

    private BufferedImage imagenExportable() {
        int w = Math.max(1400, getWidth()), h = Math.max(820, getHeight());
        return exportarImagen(w, h);
    }

    public void copiarAlPortapapeles() {
        final BufferedImage img = imagenExportable();
        java.awt.datatransfer.Transferable t = new java.awt.datatransfer.Transferable() {
            public java.awt.datatransfer.DataFlavor[] getTransferDataFlavors() {
                return new java.awt.datatransfer.DataFlavor[]{ java.awt.datatransfer.DataFlavor.imageFlavor };
            }
            public boolean isDataFlavorSupported(java.awt.datatransfer.DataFlavor f) {
                return java.awt.datatransfer.DataFlavor.imageFlavor.equals(f);
            }
            public Object getTransferData(java.awt.datatransfer.DataFlavor f)
                    throws java.awt.datatransfer.UnsupportedFlavorException {
                if (!isDataFlavorSupported(f)) throw new java.awt.datatransfer.UnsupportedFlavorException(f);
                return img;
            }
        };
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
            JOptionPane.showMessageDialog(this, "Gráfica copiada. Pégala con Ctrl+V en Word, Excel, etc.",
                    "Copiado", JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, "El portapapeles está ocupado, intenta de nuevo.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void guardarPNG() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new java.io.File("curva_granulometrica.png"));
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Imagen PNG", "png"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        java.io.File f = fc.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith(".png")) f = new java.io.File(f.getParentFile(), f.getName() + ".png");
        try {
            javax.imageio.ImageIO.write(imagenExportable(), "png", f);
        } catch (java.io.IOException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo guardar: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void setData(List<double[]> datos) {
        List<double[]> p = new ArrayList<>();
        if (datos != null)
            for (double[] d : datos)
                if (d != null && d.length >= 2 && d[0] > 0) p.add(new double[]{d[0], d[1]});
        p.sort((a, b) -> Double.compare(b[0], a[0]));
        this.puntos = p;
        repaint();
    }

    public void setMostrarEtiquetas(boolean v) { mostrarEtiquetas = v; repaint(); }
    public void setMostrarD(boolean v)         { mostrarD = v;         repaint(); }

    public void setEscalaAuto(boolean v)       { escalaAuto = v;       repaint(); }

    public BufferedImage exportarImagen(int ancho, int alto) {
        BufferedImage img = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        pintar(g, ancho, alto);
        g.dispose();
        return img;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        pintar(g, getWidth(), getHeight());
        g.dispose();
    }

    private void pintar(Graphics2D g, int W, int H) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        g.setColor(FONDO);
        g.fillRect(0, 0, W, H);

        esc = (float) Math.max(0.85, Math.min(2.0, Math.min(W / 720.0, H / 480.0)));
        F_TITULO = Tema.fuente(Font.BOLD,  Math.round(15 * esc));
        F_EJE    = Tema.fuente(Font.BOLD,  Math.round(12 * esc));
        F_TICK   = Tema.fuente(Font.PLAIN, Math.round(11 * esc));
        F_PUNTO  = Tema.fuente(Font.BOLD,  Math.round(10 * esc));
        F_ZONA   = Tema.fuente(Font.BOLD,  Math.round(10 * esc));
        F_NOTA   = Tema.fuente(Font.PLAIN, Math.round(10 * esc));

        int mIzq = Math.round(64 * esc), mDer = Math.round(22 * esc);
        int mSup = Math.round(72 * esc), mInf = Math.round(58 * esc);
        plot = new Rectangle(mIzq, mSup, Math.max(50, W - mIzq - mDer), Math.max(50, H - mSup - mInf));

        calcularRangos();

        g.setFont(F_TITULO);
        g.setColor(TXT);
        centrar(g, "Curva Granulométrica", plot.x + plot.width / 2, Math.round(24 * esc));

        g.setColor(AREA_PLOT);
        g.fill(plot);

        dibujarZonas(g);
        dibujarGrilla(g);

        if (puntos.isEmpty()) {
            g.setFont(Tema.fuente(Font.ITALIC, Math.round(13 * esc)));
            g.setColor(TXT_SUAVE);
            centrar(g, "Sin datos — presione Calcular", plot.x + plot.width / 2, plot.y + plot.height / 2);
        } else {
            Shape clipOrig = g.getClip();
            g.clip(new Rectangle(plot.x - 6, plot.y - 6, plot.width + 12, plot.height + 12));
            if (mostrarD) dibujarDiametros(g);
            dibujarCurva(g);
            g.setClip(clipOrig);
        }

        g.setColor(BORDE);
        g.setStroke(new BasicStroke(1.2f));
        g.draw(plot);

        dibujarEjes(g);

        if (!puntos.isEmpty() && mostrarEtiquetas) dibujarEtiquetas(g);
    }

    private void calcularRangos() {

        if (puntos.isEmpty()) { logMin = -3; logMax = 2; }
        else {
            double lMax = Math.log10(puntos.get(0)[0]);
            double lMin = Math.log10(puntos.get(puntos.size() - 1)[0]);
            if (escalaAuto) {
                double span = Math.max(lMax - lMin, 1.0);
                double pad  = span * 0.06;
                double mid  = (lMax + lMin) / 2;
                logMax = Math.max(lMax, mid + span / 2) + pad;
                logMin = Math.min(lMin, mid - span / 2) - pad;
            } else {
                logMax = Math.ceil(lMax - 1e-9);
                logMin = Math.floor(lMin + 1e-9);
                if (logMax - logMin < 2) logMin = logMax - 2;
            }
        }

        double minPct = 100;
        for (double[] p : puntos) minPct = Math.min(minPct, p[1]);
        double rango = 100 - minPct;
        if (!escalaAuto || puntos.isEmpty() || rango > 55) {
            yMin = 0; yStep = 10;
        } else {
            double objetivo = Math.max(rango, 2) * 1.15;
            double[] pasos = {0.5, 1, 2, 2.5, 5, 10};
            yStep = 10;
            for (double st : pasos) if (objetivo / st <= 8) { yStep = st; break; }
            yMin = Math.max(0, Math.floor((100 - objetivo) / yStep) * yStep);
            if (100 - yMin < 3 * yStep) yMin = Math.max(0, 100 - 3 * yStep);
        }

        yTop = 100 + (100 - yMin) * 0.07;
    }

    private boolean ejeYRecortado() { return yMin > 0; }

    private void dibujarZonas(Graphics2D g) {
        int xN4 = (int) Math.round(xDe(N4)), xN200 = (int) Math.round(xDe(N200));
        int x0 = plot.x, x1 = plot.x + plot.width;
        int a = clampX(xN4), b = clampX(xN200);

        g.setColor(Z_GRAVA); g.fillRect(x0, plot.y, a - x0, plot.height);
        g.setColor(Z_ARENA); g.fillRect(a, plot.y, b - a, plot.height);
        g.setColor(Z_FINOS); g.fillRect(b, plot.y, x1 - b, plot.height);

        int yb = plot.y - Math.round(24 * esc);
        g.setFont(F_ZONA);
        rotuloZona(g, "GRAVA", x0, a, yb, new Color(150,105,55));
        rotuloZona(g, "ARENA", a, b, yb, new Color(170,125,20));
        rotuloZona(g, "FINOS", b, x1, yb, new Color(70,100,160));

        Stroke dash = new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{5f, 4f}, 0f);
        g.setStroke(dash);
        g.setColor(TAMIZ_LIN);
        if (xN4 > x0 && xN4 < x1)     g.drawLine(xN4, plot.y, xN4, plot.y + plot.height);
        if (xN200 > x0 && xN200 < x1) g.drawLine(xN200, plot.y, xN200, plot.y + plot.height);
        g.setStroke(new BasicStroke(1f));

        g.setFont(F_NOTA);
        if (xN4 > x0 && xN4 < x1)     rotuloTamiz(g, "N° 4", xN4, plot.y - Math.round(7 * esc));
        if (xN200 > x0 && xN200 < x1) rotuloTamiz(g, "N° 200", xN200, plot.y - Math.round(7 * esc));
    }

    private static void rotuloTamiz(Graphics2D g, String s, int x, int baseline) {
        g.setColor(TAMIZ_LIN);
        centrar(g, s, x, baseline);
    }

    private void rotuloZona(Graphics2D g, String t, int xa, int xb, int y, Color c) {
        if (xb - xa < 12) return;
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(t) + 14, cx = (xa + xb) / 2;

        cx = Math.max(plot.x + w / 2, Math.min(plot.x + plot.width - w / 2, cx));
        int ph = fm.getHeight() + 3;
        RoundRectangle2D pill = new RoundRectangle2D.Double(cx - w / 2.0, y - fm.getAscent() - 2, w, ph, ph / 2.0, ph / 2.0);
        g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 30));
        g.fill(pill);
        g.setColor(c);
        centrar(g, t, cx, y + 1);
    }

    private void dibujarGrilla(Graphics2D g) {
        g.setStroke(new BasicStroke(1f));

        int nMen = (int) Math.round((100 - yMin) / (yStep / 2));
        for (int i = 0; i <= nMen; i++) {
            double p = yMin + i * yStep / 2;
            int y = (int) Math.round(yDe(p));
            g.setColor(i % 2 == 0 ? GRID_MAY : GRID_MEN);
            g.drawLine(plot.x, y, plot.x + plot.width, y);
        }

        for (int e = (int) Math.floor(logMin); e <= (int) Math.ceil(logMax); e++) {
            for (int k = 1; k <= 9; k++) {
                double lv = Math.log10(k * Math.pow(10, e));
                if (lv < logMin || lv > logMax) continue;
                int x = (int) Math.round(xDe(k * Math.pow(10, e)));
                g.setColor(k == 1 ? GRID_MAY : GRID_MEN);
                g.drawLine(x, plot.y, x, plot.y + plot.height);
            }
        }
    }

    private void dibujarEjes(Graphics2D g) {
        g.setFont(F_TICK);
        FontMetrics fm = g.getFontMetrics();
        g.setColor(TXT_SUAVE);

        int nMay = (int) Math.round((100 - yMin) / yStep);
        boolean decimales = yStep < 1 || (yStep % 1) != 0;
        for (int i = 0; i <= nMay; i++) {
            double p = yMin + i * yStep;
            int y = (int) Math.round(yDe(p));
            String s = (decimales ? String.format(Locale.US, "%.1f", p) : String.valueOf((int) Math.round(p))) + " %";
            g.drawString(s, plot.x - 8 - fm.stringWidth(s), y + fm.getAscent() / 2 - 1);
            g.drawLine(plot.x - 4, y, plot.x, y);
        }

        if (ejeYRecortado()) {
            int yb = plot.y + plot.height;
            g.setColor(FONDO);
            g.fillRect(plot.x - 2, yb - 12, 4, 7);
            g.setColor(BORDE);
            g.setStroke(new BasicStroke(1.2f));
            g.drawLine(plot.x - 6, yb - 9, plot.x + 6, yb - 13);
            g.drawLine(plot.x - 6, yb - 4, plot.x + 6, yb - 8);
            g.setStroke(new BasicStroke(1f));
            g.setColor(TXT_SUAVE);
        }

        double span = logMax - logMin;
        int[] mult = span < 2.6 ? new int[]{1, 2, 5} : new int[]{1};
        int ultimoX = Integer.MIN_VALUE;
        for (int e = (int) Math.floor(logMin); e <= (int) Math.ceil(logMax); e++) {
            for (int k : mult) {
                double v = k * Math.pow(10, e);
                double lv = Math.log10(v);
                if (lv < logMin - 1e-9 || lv > logMax + 1e-9) continue;
                int x = (int) Math.round(xDe(v));
                String s = fmtValor(v);
                int w = fm.stringWidth(s);

                g.drawLine(x, plot.y + plot.height, x, plot.y + plot.height + 4);
                if (ultimoX != Integer.MIN_VALUE && Math.abs(x - ultimoX) < w + 10) continue;
                centrar(g, s, x, plot.y + plot.height + 8 + fm.getAscent());
                ultimoX = x;
            }
        }

        g.setFont(F_EJE);
        g.setColor(TXT);
        centrar(g, "Diámetro de partícula (mm) — escala logarítmica",
                plot.x + plot.width / 2, plot.y + plot.height + Math.round(44 * esc));

        AffineTransform at = g.getTransform();
        g.rotate(-Math.PI / 2);
        centrar(g, "% que pasa", -(plot.y + plot.height / 2), Math.round(18 * esc));
        g.setTransform(at);
    }

    private void dibujarDiametros(Graphics2D g) {
        Stroke dash = new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{2f, 3f}, 0f);
        String[] nom = {"D₁₀", "D₃₀", "D₆₀"};
        double[] pc = {10, 30, 60};
        g.setFont(F_NOTA);
        for (int i = 0; i < 3; i++) {
            if (pc[i] < yMin) continue;
            Double d = diametro(pc[i]);
            if (d == null) continue;
            int x = (int) Math.round(xDe(d)), y = (int) Math.round(yDe(pc[i]));
            g.setColor(new Color(D_LIN.getRed(), D_LIN.getGreen(), D_LIN.getBlue(), 160));
            g.setStroke(dash);
            g.drawLine(plot.x, y, x, y);
            g.drawLine(x, y, x, plot.y + plot.height);
            g.setStroke(new BasicStroke(1f));
            g.fillOval(x - 3, y - 3, 6, 6);
            String t = nom[i] + " = " + String.format(Locale.US, d >= 1 ? "%.2f" : "%.3f", d) + " mm";
            FontMetrics fm = g.getFontMetrics();

            int tx = plot.x + 6, ty = y - 4;
            g.setColor(new Color(255, 255, 255, 220));
            g.fillRoundRect(tx - 3, ty - fm.getAscent(), fm.stringWidth(t) + 6, fm.getHeight(), 6, 6);
            g.setColor(D_LIN.darker());
            g.drawString(t, tx, ty);
        }
    }

    private void dibujarCurva(Graphics2D g) {
        Path2D linea = new Path2D.Double();
        Path2D area = new Path2D.Double();
        for (int i = 0; i < puntos.size(); i++) {
            double x = xDe(puntos.get(i)[0]), y = yDe(puntos.get(i)[1]);
            if (i == 0) { linea.moveTo(x, y); area.moveTo(x, plot.y + plot.height); area.lineTo(x, y); }
            else        { linea.lineTo(x, y); area.lineTo(x, y); }
        }
        double xu = xDe(puntos.get(puntos.size() - 1)[0]);
        area.lineTo(xu, plot.y + plot.height);
        area.closePath();

        g.setColor(CURVA_AREA);
        g.fill(area);

        g.setColor(new Color(255, 255, 255, 200));
        g.setStroke(new BasicStroke(5f * esc, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(linea);
        g.setColor(CURVA);
        g.setStroke(new BasicStroke(2.4f * esc, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(linea);

        g.setStroke(new BasicStroke(1.8f));
        for (double[] p : puntos) {
            double x = xDe(p[0]), y = yDe(p[1]);
            double r = 4 * esc;
            Ellipse2D e = new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r);
            g.setColor(CURVA);       g.fill(e);
            g.setColor(Color.WHITE); g.draw(e);
        }
        g.setStroke(new BasicStroke(1f));
    }

    private void dibujarEtiquetas(Graphics2D g) {
        g.setFont(F_PUNTO);
        FontMetrics fm = g.getFontMetrics();
        List<Rectangle2D> ocupados = new ArrayList<>();

        for (double[] p : puntos)
            ocupados.add(new Rectangle2D.Double(xDe(p[0]) - 5, yDe(p[1]) - 5, 10, 10));

        for (int i = 0; i < puntos.size(); i++) {
            double[] p = puntos.get(i);

            if (i > 0 && Math.abs(puntos.get(i - 1)[1] - p[1]) < 0.05) continue;

            String t = String.format(Locale.US, "%.1f%%", p[1]);
            int w = fm.stringWidth(t) + 8, h = fm.getHeight() + 2;
            double cx = xDe(p[0]), cy = yDe(p[1]);

            double[][] cand = {
                {cx - w / 2.0, cy - h - 8}, {cx + 4, cy - h - 6}, {cx - w - 4, cy - h - 6},
                {cx - w / 2.0, cy + 8},     {cx + 4, cy + 6},     {cx - w - 4, cy + 6},
                {cx + 9, cy - h / 2.0},     {cx - w - 9, cy - h / 2.0}
            };
            for (double[] c : cand) {
                Rectangle2D r = new Rectangle2D.Double(c[0], c[1], w, h);
                if (!dentroPlot(r)) continue;
                if (choca(r, ocupados)) continue;
                if (cruzaCurva(r)) continue;
                ocupados.add(r);
                RoundRectangle2D caja = new RoundRectangle2D.Double(r.getX(), r.getY(), w, h, 7, 7);
                g.setColor(new Color(255, 255, 255, 235));
                g.fill(caja);
                g.setColor(new Color(37, 99, 235, 90));
                g.draw(caja);
                g.setColor(CURVA.darker());
                g.drawString(t, (float) (r.getX() + 4), (float) (r.getY() + fm.getAscent() + 1));
                break;
            }
        }
    }

    private boolean dentroPlot(Rectangle2D r) {
        return r.getX() >= plot.x + 2 && r.getMaxX() <= plot.x + plot.width - 2
            && r.getY() >= plot.y + 2 && r.getMaxY() <= plot.y + plot.height - 2;
    }

    private static boolean choca(Rectangle2D r, List<Rectangle2D> ocupados) {
        for (Rectangle2D o : ocupados) if (o.intersects(r)) return true;
        return false;
    }

    private boolean cruzaCurva(Rectangle2D r) {
        for (int i = 0; i + 1 < puntos.size(); i++) {
            Line2D seg = new Line2D.Double(xDe(puntos.get(i)[0]), yDe(puntos.get(i)[1]),
                                           xDe(puntos.get(i + 1)[0]), yDe(puntos.get(i + 1)[1]));
            if (seg.intersects(r)) return true;
        }
        return false;
    }

    @Override
    public String getToolTipText(java.awt.event.MouseEvent ev) {
        for (double[] p : puntos) {
            if (Point.distance(ev.getX(), ev.getY(), xDe(p[0]), yDe(p[1])) <= 7)
                return String.format(Locale.US, "<html><b>%s mm</b><br>%.2f %% pasa</html>",
                        p[0] >= 1 ? String.format(Locale.US, "%.2f", p[0]) : String.format(Locale.US, "%.3f", p[0]), p[1]);
        }
        return null;
    }

    private double xDe(double mm) {
        double t = (logMax - Math.log10(mm)) / (logMax - logMin);
        return plot.x + t * plot.width;
    }

    private double yDe(double pct) {
        return plot.y + plot.height - ((pct - yMin) / (yTop - yMin)) * plot.height;
    }

    private int clampX(int x) { return Math.max(plot.x, Math.min(plot.x + plot.width, x)); }

    private Double diametro(double pct) {
        for (int i = 0; i + 1 < puntos.size(); i++) {
            double[] hi = puntos.get(i), lo = puntos.get(i + 1);
            if (hi[1] >= pct && lo[1] <= pct) {
                double den = hi[1] - lo[1];
                if (Math.abs(den) < 1e-9) continue;
                double t = (pct - lo[1]) / den;
                return Math.pow(10, Math.log10(lo[0]) + t * (Math.log10(hi[0]) - Math.log10(lo[0])));
            }
        }
        return null;
    }

    private static String fmtValor(double v) {
        if (v >= 1) return String.valueOf((long) Math.round(v));
        String s = String.format(Locale.US, "%.4f", v);
        return s.replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private static String fmtDecada(int e) {
        if (e >= 0) return String.valueOf((long) Math.pow(10, e));
        StringBuilder sb = new StringBuilder("0.");
        for (int i = 0; i < -e - 1; i++) sb.append('0');
        return sb.append('1').toString();
    }

    private static void centrar(Graphics2D g, String s, int cx, int baseline) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(s, cx - fm.stringWidth(s) / 2, baseline);
    }
}