package geoceramiclab.modelo3d;

import geoceramiclab.Tema;
import geoceramiclab.model.ContornosMunicipales;
import geoceramiclab.model.MunicipiosCO;
import geoceramiclab.model.MunicipiosCO.Municipio;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class MapaColombia extends JPanel {

    private static final Color MAR     = new Color(0xE8, 0xEE, 0xF3);
    private static final Color TIERRA  = new Color(0xEA, 0xE6, 0xE0);
    private static final Color RESALTE = new Color(0xF3, 0xD8, 0xCF);
    private static final Color BORDE   = new Color(0xA8, 0xA0, 0x98);
    private static final Color PUNTO   = new Color(0xB0, 0xA4, 0x98);
    private static final Color ACENTO  = new Color(0xB0, 0x3A, 0x2E);
    private static final Color TXT     = new Color(0x33, 0x33, 0x33);

    private static final double ZOOM_MIN = 1.0, ZOOM_MAX = 3000.0;
    private static final int TESELA = 256;

    public enum Capa {
        VECTORIAL("Vectorial (sin internet)", null, "Contornos DANE 2018", 0),
        CLARO    ("Mapa claro",
                  "https://basemaps.cartocdn.com/light_all/{z}/{x}/{y}.png",
                  "© OpenStreetMap  ·  © CARTO", 19),
        CALLES   ("Mapa de calles",
                  "https://tile.openstreetmap.org/{z}/{x}/{y}.png",
                  "© OpenStreetMap contributors", 19),
        RELIEVE  ("Relieve",
                  "https://tile.opentopomap.org/{z}/{x}/{y}.png",
                  "© OpenTopoMap (CC-BY-SA)", 17),
        SATELITE ("Satélite",
                  "https://server.arcgisonline.com/ArcGIS/rest/services/"
                  + "World_Imagery/MapServer/tile/{z}/{y}/{x}",
                  "Esri  ·  Maxar  ·  Earthstar Geographics", 19);

        public final String etiqueta, plantilla, credito;
        public final int zMax;

        Capa(String etiqueta, String plantilla, String credito, int zMax) {
            this.etiqueta = etiqueta; this.plantilla = plantilla;
            this.credito = credito;  this.zMax = zMax;
        }
        boolean enLinea() { return plantilla != null; }
        String url(int z, int x, int y) {
            return plantilla.replace("{z}", Integer.toString(z))
                            .replace("{x}", Integer.toString(x))
                            .replace("{y}", Integer.toString(y));
        }
        @Override public String toString() { return etiqueta; }
    }

    private Capa capa = Capa.CLARO;
    private Municipio seleccionado, bajoElMouse;
    private String deptoFiltro;
    private Consumer<Municipio> alSeleccionar;

    private final double lonMin, lonMax, latMin, latMax;
    private double centroLon, centroLat, zoomFactor = 1.0;
    private double nivelBase = 5, nivel = 5, S = 1280;
    private Point arrastreDesde;
    private boolean huboArrastre;

    private final Teselas teselas = new Teselas();
    private JComboBox<Capa> cboCapa;

    public MapaColombia() {
        setBackground(MAR);
        setPreferredSize(new Dimension(430, 470));
        setToolTipText("");
        setLayout(null);

        double[] b = limites();
        lonMin = b[0]; lonMax = b[1]; latMin = b[2]; latMax = b[3];
        centroLon = (lonMin + lonMax) / 2.0;
        centroLat = (latMin + latMax) / 2.0;

        addMouseWheelListener(e -> {
            double antesLon = aLon(e.getX()), antesLat = aLat(e.getY());
            fijarZoom(zoomFactor * Math.pow(1.25, -e.getPreciseWheelRotation()));
            recalcular();
            centroLon += antesLon - aLon(e.getX());
            centroLat += antesLat - aLat(e.getY());
            limitarCentro();
            repaint();
        });

        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                arrastreDesde = e.getPoint(); huboArrastre = false;
            }
            @Override public void mouseReleased(MouseEvent e) {
                arrastreDesde = null;
                setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            }
            @Override public void mouseClicked(MouseEvent e) {
                if (huboArrastre) return;
                if (e.getClickCount() >= 2) { ajustar(); return; }
                Municipio m = enPunto(e.getX(), e.getY());
                if (m != null) {
                    setSeleccionado(m);
                    if (alSeleccionar != null) alSeleccionar.accept(m);
                }
            }
            @Override public void mouseExited(MouseEvent e) { bajoElMouse = null; repaint(); }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                if (arrastreDesde == null) return;
                int dx = e.getX() - arrastreDesde.x, dy = e.getY() - arrastreDesde.y;
                if (Math.abs(dx) > 2 || Math.abs(dy) > 2) {
                    huboArrastre = true;
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                }
                centroLon = aLonDesplazado(-dx);
                centroLat = aLatDesplazado(-dy);
                limitarCentro();
                arrastreDesde = e.getPoint();
                repaint();
            }
            @Override public void mouseMoved(MouseEvent e) {
                Municipio m = enPunto(e.getX(), e.getY());
                if (m != bajoElMouse) { bajoElMouse = m; repaint(); }
            }
        });

        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        construirControles();
    }

    public void setAlSeleccionar(Consumer<Municipio> c) { this.alSeleccionar = c; }

    public Municipio getSeleccionado() { return seleccionado; }

    public void setSeleccionado(Municipio m) { this.seleccionado = m; repaint(); }

    public void setDepartamento(String depto) {
        this.deptoFiltro = (depto == null || depto.isEmpty()) ? null : depto;
        repaint();
    }

    public Capa getCapa() { return capa; }

    public void setCapa(Capa c) {
        if (c == null) return;
        capa = c;
        teselas.olvidarFallos();
        if (cboCapa != null && cboCapa.getSelectedItem() != c) cboCapa.setSelectedItem(c);
        repaint();
    }

    public void acercarA(Municipio m, double factorSiNoHayContorno) {
        if (m == null) return;
        seleccionado = m;

        double[] caja = ContornosMunicipales.caja(m.codigoDane);
        if (caja != null && getWidth() > 0) {
            centroLon = (caja[0] + caja[1]) / 2.0;
            centroLat = (caja[2] + caja[3]) / 2.0;
            double dx = Math.max(1e-4, normX(caja[1]) - normX(caja[0]));
            double dy = Math.max(1e-4, normY(caja[2]) - normY(caja[3]));
            double sNecesaria = Math.min(getWidth() / dx, (getHeight() - 24) / dy) * 0.55;
            fijarZoom(sNecesaria / (TESELA * Math.pow(2, nivelBase)));
        } else {
            centroLon = m.lon;
            centroLat = m.lat;
            fijarZoom(factorSiNoHayContorno);
        }
        recalcular();
        limitarCentro();
        repaint();
    }

    public void ajustar() {
        centroLon = (lonMin + lonMax) / 2.0;
        centroLat = (latMin + latMax) / 2.0;
        zoomFactor = 1.0;
        recalcular();
        repaint();
    }

    public double getZoom() { return zoomFactor; }

    public void limpiarCache() { teselas.borrarDisco(); repaint(); }

    private void fijarZoom(double z) { zoomFactor = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, z)); }

    private void limitarCentro() {
        centroLon = Math.max(lonMin, Math.min(lonMax, centroLon));
        centroLat = Math.max(latMin, Math.min(latMax, centroLat));
    }

    @Override public String getToolTipText(MouseEvent e) {
        Municipio m = enPunto(e.getX(), e.getY());
        return m == null ? null : m.nombreCompleto() + "  ·  " + m.codigoDane;
    }

    private void construirControles() {
        cboCapa = new JComboBox<>(Capa.values());
        cboCapa.setSelectedItem(capa);
        cboCapa.setFont(cboCapa.getFont().deriveFont(Font.PLAIN, 11f));
        cboCapa.setToolTipText("Capa de fondo del mapa");
        cboCapa.addActionListener(e -> setCapa((Capa) cboCapa.getSelectedItem()));
        cboCapa.setSize(178, 24);
        add(cboCapa);

        JButton mas   = botonito("+", "Acercar");
        JButton menos = botonito("−", "Alejar");
        JButton todo  = botonito("⤡", "Ver todo el país");
        mas  .addActionListener(e -> { fijarZoom(zoomFactor * 1.6); recalcular(); repaint(); });
        menos.addActionListener(e -> { fijarZoom(zoomFactor / 1.6); recalcular(); limitarCentro(); repaint(); });
        todo .addActionListener(e -> ajustar());

        JPanel caja = new JPanel(new GridLayout(3, 1, 0, 2));
        caja.setOpaque(false);
        caja.add(mas); caja.add(menos); caja.add(todo);
        caja.setSize(30, 94);
        add(caja);

        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                cboCapa.setLocation(10, 10);
                caja.setLocation(getWidth() - 40, 10);
            }
        });
    }

    private JButton botonito(String txt, String tip) {
        JButton b = new JButton(txt);
        b.setToolTipText(tip);
        b.setMargin(new Insets(0, 0, 0, 0));
        b.setFocusPainted(false);
        b.setFont(Tema.ui(Font.BOLD, 14));
        b.setBackground(Tema.CONTROL);
        b.setForeground(Tema.TEXTO);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private static double normX(double lon) { return (lon + 180.0) / 360.0; }

    private static double normY(double lat) {
        double r = Math.toRadians(Math.max(-85.05112878, Math.min(85.05112878, lat)));
        return (1 - Math.log(Math.tan(r) + 1 / Math.cos(r)) / Math.PI) / 2.0;
    }

    private static double aLonNorm(double nx) { return nx * 360.0 - 180.0; }

    private static double aLatNorm(double ny) {
        return Math.toDegrees(Math.atan(Math.sinh(Math.PI * (1 - 2 * ny))));
    }

    private static double[] limites() {
        double a = 1e9, b = -1e9, c = 1e9, d = -1e9;
        for (double[][] anillo : MunicipiosCO.contornos()) {
            for (double[] p : anillo) {
                if (p[0] < a) a = p[0];
                if (p[0] > b) b = p[0];
                if (p[1] < c) c = p[1];
                if (p[1] > d) d = p[1];
            }
        }
        return new double[]{ a - 0.3, b + 0.3, c - 0.3, d + 0.3 };
    }

    private void recalcular() {
        int w = Math.max(1, getWidth()), h = Math.max(1, getHeight());
        double dx = normX(lonMax) - normX(lonMin);
        double dy = normY(latMin) - normY(latMax);
        double sAjuste = Math.min(w / dx, h / dy) * 0.95;
        nivelBase = Math.log(sAjuste / TESELA) / Math.log(2);
        nivel = nivelBase + Math.log(zoomFactor) / Math.log(2);
        S = TESELA * Math.pow(2, nivel);
    }

    private double px(double lon) { return getWidth()  / 2.0 + (normX(lon) - normX(centroLon)) * S; }
    private double py(double lat) { return getHeight() / 2.0 + (normY(lat) - normY(centroLat)) * S; }
    private double aLon(double x) { return aLonNorm(normX(centroLon) + (x - getWidth()  / 2.0) / S); }
    private double aLat(double y) { return aLatNorm(normY(centroLat) + (y - getHeight() / 2.0) / S); }
    private double aLonDesplazado(int dx) { return aLonNorm(normX(centroLon) + dx / S); }
    private double aLatDesplazado(int dy) { return aLatNorm(normY(centroLat) + dy / S); }

    private Municipio enPunto(int x, int y) {
        if (S <= 0) return null;
        double lon = aLon(x), lat = aLat(y);

        List<Municipio> candidatos = masCercanos(lat, lon, 12);
        for (Municipio m : candidatos) {
            if (ContornosMunicipales.contiene(m.codigoDane, lon, lat)) return m;
        }
        if (candidatos.isEmpty()) return null;

        Municipio m = candidatos.get(0);
        double d = Math.hypot(px(m.lon) - x, py(m.lat) - y);
        return d <= Math.max(12, 40 / Math.sqrt(zoomFactor)) ? m : null;
    }

    private static List<Municipio> masCercanos(double lat, double lon, int k) {
        List<Municipio> mejores = new ArrayList<>(k + 1);
        List<Double>    dists   = new ArrayList<>(k + 1);
        double cosLat = Math.cos(Math.toRadians(lat));
        for (Municipio m : MunicipiosCO.todos()) {
            double dx = (m.lon - lon) * cosLat, dy = m.lat - lat;
            double d = dx * dx + dy * dy;
            int i = 0;
            while (i < dists.size() && dists.get(i) < d) i++;
            if (i < k) {
                mejores.add(i, m); dists.add(i, d);
                if (mejores.size() > k) { mejores.remove(k); dists.remove(k); }
            }
        }
        return mejores;
    }

    @Override protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                           RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        int w = getWidth(), h = getHeight();
        recalcular();

        boolean conTeselas = capa.enLinea();

        dibujarVectorial(g, !conTeselas);

        if (conTeselas) dibujarTeselas(g, w, h);

        if (conTeselas) dibujarBordesDepartamentos(g);

        if (seleccionado != null) dibujarMunicipio(g, seleccionado, true);
        if (bajoElMouse != null && bajoElMouse != seleccionado) dibujarMunicipio(g, bajoElMouse, false);

        dibujarMunicipios(g, w, h);

        if (bajoElMouse != null && bajoElMouse != seleccionado) {
            marca(g, bajoElMouse, new Color(0x50, 0x50, 0x50), 5, false);
            etiqueta(g, bajoElMouse, w, h, new Color(0x50, 0x50, 0x50), false);
        }
        if (seleccionado != null) {
            marca(g, seleccionado, ACENTO, 7, true);
            etiqueta(g, seleccionado, w, h, ACENTO, true);
        }

        barraEstado(g, w, h);
        g.dispose();
    }

    private void dibujarVectorial(Graphics2D g, boolean conRelleno) {
        List<double[][]> anillos = MunicipiosCO.contornos();
        List<String>     nombres = MunicipiosCO.nombresContorno();
        String resaltado = (seleccionado != null) ? seleccionado.departamento : deptoFiltro;

        for (int i = 0; i < anillos.size(); i++) {
            Path2D.Double p = ruta(anillos.get(i));
            boolean res = resaltado != null && resaltado.equals(nombres.get(i));
            g.setColor(res ? RESALTE : TIERRA);
            g.fill(p);
            if (conRelleno) {
                g.setColor(BORDE);
                g.setStroke(new BasicStroke(res ? 1.5f : 0.7f));
                g.draw(p);
            }
        }
    }

    private void dibujarBordesDepartamentos(Graphics2D g) {
        g.setColor(new Color(0x55, 0x4A, 0x42, 150));
        g.setStroke(new BasicStroke(0.9f));
        for (double[][] a : MunicipiosCO.contornos()) g.draw(ruta(a));
    }

    private Path2D.Double ruta(double[][] anillo) {
        Path2D.Double p = new Path2D.Double();
        for (int k = 0; k < anillo.length; k++) {
            double X = px(anillo[k][0]), Y = py(anillo[k][1]);
            if (k == 0) p.moveTo(X, Y); else p.lineTo(X, Y);
        }
        p.closePath();
        return p;
    }

    private void dibujarMunicipio(Graphics2D g, Municipio m, boolean elegido) {
        double[][][] anillos = ContornosMunicipales.anillos(m.codigoDane);
        if (anillos == null) return;
        for (double[][] a : anillos) {
            Path2D.Double p = ruta(a);
            if (elegido) {
                g.setColor(new Color(ACENTO.getRed(), ACENTO.getGreen(), ACENTO.getBlue(), 55));
                g.fill(p);
                g.setColor(ACENTO);
                g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            } else {
                g.setColor(new Color(0x50, 0x50, 0x50, 30));
                g.fill(p);
                g.setColor(new Color(0x50, 0x50, 0x50, 190));
                g.setStroke(new BasicStroke(1.2f));
            }
            g.draw(p);
        }
    }

    private void dibujarMunicipios(Graphics2D g, int w, int h) {
        boolean conNombres = zoomFactor >= 5;
        int r = zoomFactor >= 8 ? 4 : (zoomFactor >= 3 ? 3 : 2);
        g.setFont(getFont().deriveFont(Font.PLAIN, 10.5f));
        int puestos = 0;

        for (Municipio m : MunicipiosCO.todos()) {
            if (deptoFiltro != null && !deptoFiltro.equals(m.departamento)) continue;
            double X = px(m.lon), Y = py(m.lat);
            if (X < -40 || X > w + 40 || Y < -20 || Y > h + 20) continue;

            g.setColor(capa.enLinea() ? new Color(0x44, 0x3C, 0x36, 170) : PUNTO);
            g.fillRect((int) X - r / 2, (int) Y - r / 2, r, r);

            if (conNombres && puestos < 90 && m != seleccionado && m != bajoElMouse) {
                if (capa.enLinea()) {
                    g.setColor(new Color(255, 255, 255, 200));
                    g.drawString(m.nombre, (int) X + r + 1, (int) Y - r + 1);
                }
                g.setColor(new Color(0x3A, 0x33, 0x2E));
                g.drawString(m.nombre, (int) X + r, (int) Y - r);
                puestos++;
            }
        }
    }

    private void dibujarTeselas(Graphics2D g, int w, int h) {
        int z = (int) Math.round(nivel);
        z = Math.max(0, Math.min(capa.zMax, z));
        double escala = Math.pow(2, nivel - z);
        double n = Math.pow(2, z);

        double cx = normX(centroLon) * n * TESELA;
        double cy = normY(centroLat) * n * TESELA;
        double x0 = cx - (w / 2.0) / escala;
        double y0 = cy - (h / 2.0) / escala;

        int tx0 = (int) Math.floor(x0 / TESELA), tx1 = (int) Math.floor((x0 + w / escala) / TESELA);
        int ty0 = (int) Math.floor(y0 / TESELA), ty1 = (int) Math.floor((y0 + h / escala) / TESELA);
        int lado = (int) Math.ceil(TESELA * escala) + 1;
        int maxT = (int) n;

        for (int ty = ty0; ty <= ty1; ty++) {
            if (ty < 0 || ty >= maxT) continue;
            for (int tx = tx0; tx <= tx1; tx++) {
                int txw = ((tx % maxT) + maxT) % maxT;
                Image img = teselas.obtener(capa, z, txw, ty, this::repaint);
                if (img == null) continue;
                int sx = (int) Math.round((tx * (double) TESELA - x0) * escala);
                int sy = (int) Math.round((ty * (double) TESELA - y0) * escala);
                g.drawImage(img, sx, sy, lado, lado, null);
            }
        }
    }

    private void barraEstado(Graphics2D g, int w, int h) {
        g.setColor(new Color(255, 255, 255, 215));
        g.fillRect(0, h - 20, w, 20);
        g.setFont(getFont().deriveFont(Font.PLAIN, 10.5f));
        FontMetrics fm = g.getFontMetrics();

        String izq;
        if (capa.enLinea() && teselas.sinRed()) {
            izq = "Sin conexión — mostrando el mapa vectorial";
            g.setColor(new Color(0xB0, 0x3A, 0x2E));
        } else {
            izq = String.format("Zoom %.0f%%   ·   rueda: acercar   ·   "
                    + "arrastrar: mover   ·   doble clic: todo el país", zoomFactor * 100);
            g.setColor(new Color(0x80, 0x80, 0x80));
        }
        g.drawString(izq, 8, h - 6);

        g.setColor(new Color(0x8A, 0x8A, 0x8A));
        int aw = fm.stringWidth(capa.credito);
        if (8 + fm.stringWidth(izq) + 16 + aw < w) g.drawString(capa.credito, w - aw - 8, h - 6);
    }

    private void marca(Graphics2D g, Municipio m, Color c, int r, boolean cruz) {
        double X = px(m.lon), Y = py(m.lat);
        if (cruz) {
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 55));
            g.fillOval((int) (X - r * 2.2), (int) (Y - r * 2.2), (int) (r * 4.4), (int) (r * 4.4));
            g.setColor(c);
            g.setStroke(new BasicStroke(1.2f));
            g.drawLine((int) X - r * 3, (int) Y, (int) X + r * 3, (int) Y);
            g.drawLine((int) X, (int) Y - r * 3, (int) X, (int) Y + r * 3);
        }
        g.setColor(c);
        g.fillOval((int) (X - r / 2.0), (int) (Y - r / 2.0), r, r);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(1.1f));
        g.drawOval((int) (X - r / 2.0), (int) (Y - r / 2.0), r, r);
    }

    private void etiqueta(Graphics2D g, Municipio m, int w, int h, Color borde, boolean fuerte) {
        g.setFont(getFont().deriveFont(fuerte ? Font.BOLD : Font.PLAIN, 12f));
        FontMetrics fm = g.getFontMetrics();
        String t = m.nombreCompleto();
        int tw = fm.stringWidth(t) + 14, th = fm.getHeight() + 8;

        int x = (int) Math.round(px(m.lon)) + 12;
        int y = (int) Math.round(py(m.lat)) - th - 8;
        if (x + tw > w - 6) x = w - 6 - tw;
        if (x < 6) x = 6;
        if (y < 40) y = (int) Math.round(py(m.lat)) + 14;
        if (y + th > h - 24) y = h - 24 - th;

        g.setColor(new Color(255, 255, 255, 240));
        g.fillRoundRect(x, y, tw, th, 8, 8);
        g.setColor(borde);
        g.setStroke(new BasicStroke(fuerte ? 1.4f : 1f));
        g.drawRoundRect(x, y, tw, th, 8, 8);
        g.setColor(TXT);
        g.drawString(t, x + 7, y + fm.getAscent() + 4);
    }

    private static class Teselas {

        private static final String AGENTE =
                "SDIM/1.0 (proyecto academico; Java " +
                System.getProperty("java.version") + ")";
        private static final int MAX_MEMORIA = 320;
        private static final long REINTENTO_MS = 20000;

        private final Map<String, Image> memoria =
                new LinkedHashMap<String, Image>(64, 0.75f, true) {
                    @Override protected boolean removeEldestEntry(Map.Entry<String, Image> e) {
                        return size() > MAX_MEMORIA;
                    }
                };
        private final Set<String> enCurso = ConcurrentHashMap.newKeySet();
        private final Map<String, Long> fallidas = new ConcurrentHashMap<>();
        private final ExecutorService hilos = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "teselas");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        });

        private volatile int ok = 0, fallos = 0;

        private final File carpeta =
                new File(System.getProperty("user.home"), ".geoceramiclab/teselas");

        boolean sinRed() { return ok == 0 && fallos >= 6; }

        void olvidarFallos() { fallidas.clear(); fallos = 0; }

        Image obtener(Capa capa, int z, int x, int y, Runnable alLlegar) {
            String clave = capa.name() + "/" + z + "/" + x + "/" + y;

            synchronized (memoria) {
                Image img = memoria.get(clave);
                if (img != null) return img;
            }
            Long cuando = fallidas.get(clave);
            if (cuando != null && System.currentTimeMillis() - cuando < REINTENTO_MS) return null;
            if (!enCurso.add(clave)) return null;

            hilos.submit(() -> {
                Image img = null;
                File f = new File(carpeta, clave + ".png");
                try {
                    if (f.isFile() && f.length() > 0) {
                        img = ImageIO.read(f);
                    }
                    if (img == null) {
                        img = descargar(capa.url(z, x, y), f);
                    }
                } catch (Exception ignorada) {
                    img = null;
                }

                if (img != null) {
                    ok++;
                    fallidas.remove(clave);
                    synchronized (memoria) { memoria.put(clave, img); }
                } else {
                    fallos++;
                    fallidas.put(clave, System.currentTimeMillis());
                }
                enCurso.remove(clave);
                if (alLlegar != null) SwingUtilities.invokeLater(alLlegar);
            });
            return null;
        }

        private Image descargar(String url, File destino) throws Exception {
            HttpURLConnection c = (HttpURLConnection)
                    java.net.URI.create(url).toURL().openConnection();
            c.setRequestProperty("User-Agent", AGENTE);
            c.setRequestProperty("Accept", "image/png,image/*;q=0.8");
            c.setConnectTimeout(4000);
            c.setReadTimeout(8000);
            c.setInstanceFollowRedirects(true);
            if (c.getResponseCode() != 200) { c.disconnect(); return null; }

            java.awt.image.BufferedImage img;
            try (InputStream in = c.getInputStream()) {
                img = ImageIO.read(in);
            } finally {
                c.disconnect();
            }
            if (img == null) return null;

            try {
                File padre = destino.getParentFile();
                if (padre != null) padre.mkdirs();
                File tmp = new File(destino.getAbsolutePath() + ".tmp");
                ImageIO.write(img, "png", tmp);
                if (!tmp.renameTo(destino)) tmp.delete();
            } catch (Exception ignorada) { }
            return img;
        }

        void borrarDisco() {
            borrar(carpeta);
            synchronized (memoria) { memoria.clear(); }
            olvidarFallos();
        }

        private static void borrar(File f) {
            File[] hijos = f.listFiles();
            if (hijos != null) for (File h : hijos) borrar(h);
            f.delete();
        }
    }
}