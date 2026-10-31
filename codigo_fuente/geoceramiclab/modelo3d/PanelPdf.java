package geoceramiclab;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class PanelPdf extends JPanel {

    private static final float[] ZOOMS = {0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f, 3f};

    private static final double SUPERMUESTREO = 2.0;
    private static final float  DPI_MIN = 110f, DPI_MAX = 240f;

    private static final java.util.regex.Pattern CODIGO_NORMA =
            java.util.regex.Pattern.compile(
                    "(?i)\\bE\\s*[-–—]?\\s*(\\d{3})(?:\\s*[-–—]\\s*(\\d{2}))?");

    private final Motor motor;
    private final File archivo;
    private final Lienzo lienzo = new Lienzo();
    private final JLabel lblPagina = new JLabel();
    private final JLabel lblEstado = new JLabel(" ");
    private final Map<String, BufferedImage> cache = new HashMap<>();
    private JTree arbolIndice;
    private JScrollPane scrollPagina;

    private int pagina = 0, paginas = 1;
    private int iZoom = 2;
    private boolean ajustarAncho = true;

    public static boolean hayMotor() { return Motor.disponible(); }

    public PanelPdf(File pdf, String destino) throws Exception {
        this.archivo = pdf;
        this.motor = new Motor(pdf);
        this.paginas = Math.max(1, motor.paginas());

        setLayout(new BorderLayout());
        add(barra(), BorderLayout.NORTH);

        scrollPagina = new JScrollPane(lienzo);
        scrollPagina.setBorder(null);
        scrollPagina.getVerticalScrollBar().setUnitIncrement(24);
        scrollPagina.getViewport().setBackground(new Color(0x55, 0x55, 0x55));

        JComponent indice = construirIndice();
        if (indice != null) {
            JSplitPane div = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, indice, scrollPagina);
            div.setDividerLocation(250);
            div.setBorder(null);
            add(div, BorderLayout.CENTER);
        } else {
            add(scrollPagina, BorderLayout.CENTER);
        }

        lblEstado.setBorder(new EmptyBorder(4, 10, 4, 10));
        lblEstado.setFont(lblEstado.getFont().deriveFont(Font.PLAIN, 11f));
        lblEstado.setForeground(new Color(0x70, 0x70, 0x70));
        add(lblEstado, BorderLayout.SOUTH);

        if (destino != null && !destino.trim().isEmpty()) irAlMarcador(destino);

        scrollPagina.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) {
                if (ajustarAncho) render();
            }
        });
        render();
    }

    public void cerrar() { motor.cerrar(); }

    public File getArchivo() { return archivo; }

    private JComponent barra() {
        JPanel b = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        b.setBackground(new Color(0xF2, 0xF2, 0xF2));
        b.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xD5, 0xD5, 0xD5)));

        JButton ant = new JButton("◀"), sig = new JButton("▶");
        ant.setToolTipText("Página anterior");
        sig.setToolTipText("Página siguiente");
        ant.addActionListener(e -> { if (pagina > 0) { pagina--; render(); } });
        sig.addActionListener(e -> { if (pagina < paginas - 1) { pagina++; render(); } });

        JButton menos = new JButton("−"), mas = new JButton("+");
        JButton ancho = new JButton("Ajustar ancho");
        menos.addActionListener(e -> { ajustarAncho = false; if (iZoom > 0) iZoom--; render(); });
        mas  .addActionListener(e -> { ajustarAncho = false; if (iZoom < ZOOMS.length-1) iZoom++; render(); });
        ancho.addActionListener(e -> { ajustarAncho = true; render(); });

        JButton sistema = new JButton("Abrir en el visor del sistema");
        sistema.addActionListener(e -> VisorPdf.abrirConElSistema(
                SwingUtilities.getWindowAncestor(this), archivo));

        lblPagina.setBorder(new EmptyBorder(0, 8, 0, 8));

        b.add(ant); b.add(sig); b.add(lblPagina);
        b.add(new JSeparator(SwingConstants.VERTICAL));
        b.add(menos); b.add(mas); b.add(ancho);
        b.add(new JSeparator(SwingConstants.VERTICAL));
        b.add(sistema);
        return b;
    }

    private JComponent construirIndice() {
        javax.swing.tree.DefaultMutableTreeNode completo = motor.marcadores();
        if (completo == null || completo.getChildCount() == 0) return null;

        final javax.swing.tree.DefaultMutableTreeNode soloNormas = filtrarNormas(completo);
        final boolean hayNormas = soloNormas.getChildCount() > 0;

        final JTree arbol = new JTree(hayNormas ? soloNormas : completo) {
            @Override public String getToolTipText(java.awt.event.MouseEvent e) {
                javax.swing.tree.TreePath tp = getPathForLocation(e.getX(), e.getY());
                if (tp == null) return null;
                Object o = ((javax.swing.tree.DefaultMutableTreeNode)
                        tp.getLastPathComponent()).getUserObject();
                return (o instanceof Marcador) ? ((Marcador) o).completo : null;
            }
        };
        arbol.setToolTipText("");
        arbolIndice = arbol;
        arbol.setRootVisible(false);
        arbol.setShowsRootHandles(true);
        arbol.getSelectionModel().setSelectionMode(
                javax.swing.tree.TreeSelectionModel.SINGLE_TREE_SELECTION);
        arbol.setFont(arbol.getFont().deriveFont(Font.PLAIN, 12f));

        arbol.addTreeSelectionListener(e -> {
            Object nodo = arbol.getLastSelectedPathComponent();
            if (!(nodo instanceof javax.swing.tree.DefaultMutableTreeNode)) return;
            Object dato = ((javax.swing.tree.DefaultMutableTreeNode) nodo).getUserObject();
            if (dato instanceof Marcador) {
                int pg = ((Marcador) dato).pagina;
                if (pg >= 0 && pg < paginas) { pagina = pg; render(); }
            }
        });

        JPanel lado = new JPanel(new BorderLayout());

        JLabel tit = new JLabel("  " + (hayNormas
                ? "Normas del documento (" + soloNormas.getChildCount() + ")"
                : "Contenido del documento"));
        tit.setFont(tit.getFont().deriveFont(Font.BOLD, 11f));
        tit.setForeground(new Color(0x55, 0x55, 0x55));
        tit.setBorder(new EmptyBorder(8, 4, 6, 4));
        tit.setOpaque(true);
        tit.setBackground(new Color(0xF2, 0xF2, 0xF2));

        JCheckBox chkTodo = new JCheckBox("Todo el contenido");
        chkTodo.setFont(chkTodo.getFont().deriveFont(Font.PLAIN, 11f));
        chkTodo.setBackground(new Color(0xF2, 0xF2, 0xF2));
        chkTodo.setBorder(new EmptyBorder(0, 6, 8, 4));
        chkTodo.setEnabled(hayNormas);
        chkTodo.addActionListener(e -> {
            arbol.setModel(new javax.swing.tree.DefaultTreeModel(
                    chkTodo.isSelected() ? completo : soloNormas));
            tit.setText("  " + (chkTodo.isSelected()
                    ? "Contenido del documento"
                    : "Normas del documento (" + soloNormas.getChildCount() + ")"));
        });

        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.setBackground(new Color(0xF2, 0xF2, 0xF2));
        cabecera.add(tit, BorderLayout.NORTH);
        cabecera.add(chkTodo, BorderLayout.SOUTH);
        lado.add(cabecera, BorderLayout.NORTH);
        JScrollPane si = new JScrollPane(arbol);
        si.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(0xD5, 0xD5, 0xD5)));
        lado.add(si, BorderLayout.CENTER);
        return lado;
    }

    private javax.swing.tree.DefaultMutableTreeNode filtrarNormas(
            javax.swing.tree.DefaultMutableTreeNode origen) {
        javax.swing.tree.DefaultMutableTreeNode raiz =
                new javax.swing.tree.DefaultMutableTreeNode("Normas");
        recolectar(origen, raiz, new java.util.HashSet<String>());
        return raiz;
    }

    private void recolectar(javax.swing.tree.DefaultMutableTreeNode nodo,
                            javax.swing.tree.DefaultMutableTreeNode destino,
                            java.util.Set<String> vistos) {
        for (int i = 0; i < nodo.getChildCount(); i++) {
            javax.swing.tree.DefaultMutableTreeNode h =
                    (javax.swing.tree.DefaultMutableTreeNode) nodo.getChildAt(i);
            Object dato = h.getUserObject();

            if (dato instanceof Marcador) {
                Marcador m = (Marcador) dato;
                java.util.regex.Matcher mt = CODIGO_NORMA.matcher(m.completo);
                if (mt.find()) {
                    String cod = "E-" + mt.group(1)
                               + (mt.group(2) != null ? "-" + mt.group(2) : "");
                    if (vistos.add(cod)) {
                        destino.add(new javax.swing.tree.DefaultMutableTreeNode(
                                new Marcador(cod, m.completo, m.pagina)));
                    }
                }
            }
            recolectar(h, destino, vistos);
        }
    }

    public void irAlMarcador(String texto) {
        if (arbolIndice == null || texto == null) return;
        String busca = VisorPdf.compacto(texto);
        javax.swing.tree.TreeModel m = arbolIndice.getModel();
        javax.swing.tree.TreePath camino = rebuscar(m, m.getRoot(),
                new javax.swing.tree.TreePath(m.getRoot()), busca);
        if (camino == null) {
            lblEstado.setText("No se encontró \"" + texto + "\" en el índice del documento.");
            return;
        }
        Object dato = ((javax.swing.tree.DefaultMutableTreeNode)
                camino.getLastPathComponent()).getUserObject();
        if (dato instanceof Marcador) {
            int pg = ((Marcador) dato).pagina;
            if (pg >= 0 && pg < paginas) pagina = pg;
        }
        arbolIndice.setSelectionPath(camino);
        arbolIndice.scrollPathToVisible(camino);
    }

    private javax.swing.tree.TreePath rebuscar(javax.swing.tree.TreeModel m, Object nodo,
                                               javax.swing.tree.TreePath camino, String busca) {
        Object dato = ((javax.swing.tree.DefaultMutableTreeNode) nodo).getUserObject();
        if (dato instanceof Marcador) {
            Marcador mc = (Marcador) dato;
            if (VisorPdf.compacto(mc.titulo).contains(busca)
                    || VisorPdf.compacto(mc.completo).contains(busca)) return camino;
        }
        for (int i = 0; i < m.getChildCount(nodo); i++) {
            Object h = m.getChild(nodo, i);
            javax.swing.tree.TreePath r = rebuscar(m, h, camino.pathByAddingChild(h), busca);
            if (r != null) return r;
        }
        return null;
    }

    static class Marcador {
        final String titulo;
        final String completo;
        final int pagina;

        Marcador(String titulo, String completo, int pagina) {
            this.titulo = titulo; this.completo = completo; this.pagina = pagina;
        }
        Marcador(String t, int p) { this(t, t, p); }
        @Override public String toString() { return titulo; }
    }

    private void render() {
        lblPagina.setText("Página " + (pagina + 1) + " de " + paginas);
        lblEstado.setText("Generando la vista…");

        final int pag = pagina;
        final int destino = anchoObjetivo();
        final float dpi = dpiPara(destino);
        final String clave = pag + "@" + destino;

        BufferedImage ya = cache.get(clave);
        if (ya != null) { poner(ya, destino); return; }

        new Thread(() -> {
            BufferedImage img = null;
            String error = null;
            try { img = motor.render(pag, dpi); }
            catch (Throwable t) { error = t.toString(); }

            final BufferedImage fin = img;
            final String err = error;
            SwingUtilities.invokeLater(() -> {
                if (fin != null) {
                    if (cache.size() > 5) cache.clear();
                    cache.put(clave, fin);
                    poner(fin, destino);
                } else {
                    lblEstado.setText("No se pudo dibujar la página. " + (err == null ? "" : err));
                }
            });
        }, "pdf-render").start();
    }

    private int anchoObjetivo() {
        if (ajustarAncho) {
            int disponible = (scrollPagina != null && scrollPagina.getViewport().getWidth() > 60)
                    ? scrollPagina.getViewport().getWidth() - 22
                    : Math.max(320, getWidth() - 300);
            return Math.max(240, disponible);
        }
        double ap = motor.anchoPuntos(pagina);
        if (ap <= 0) return 800;
        return (int) Math.round(ap * (96.0 / 72.0) * ZOOMS[iZoom]);
    }

    private float dpiPara(int anchoPantalla) {
        double ap = motor.anchoPuntos(pagina);
        if (ap <= 0) return 150f;
        double d = anchoPantalla * SUPERMUESTREO * 72.0 / ap;
        return (float) Math.max(DPI_MIN, Math.min(DPI_MAX, d));
    }

    private void poner(BufferedImage img, int anchoPantalla) {
        int alto = (int) Math.round(anchoPantalla * (double) img.getHeight() / img.getWidth());
        lienzo.imagen = img;
        lienzo.anchoDibujo = anchoPantalla;
        lienzo.altoDibujo = alto;
        lienzo.setPreferredSize(new Dimension(anchoPantalla, alto));
        lienzo.revalidate();
        lienzo.repaint();
        lblEstado.setText(String.format("%s   ·   %d × %d px   ·   renderizado a %d × %d",
                archivo.getName(), anchoPantalla, alto, img.getWidth(), img.getHeight()));
    }

    private static class Lienzo extends JPanel {
        BufferedImage imagen;
        int anchoDibujo, altoDibujo;

        Lienzo() { setBackground(new Color(0x55, 0x55, 0x55)); }

        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            if (imagen == null) return;
            Graphics2D g = (Graphics2D) g0.create();

            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                               RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING,
                               RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                               RenderingHints.VALUE_ANTIALIAS_ON);

            int x = Math.max(0, (getWidth() - anchoDibujo) / 2);
            g.setColor(new Color(0, 0, 0, 70));
            g.fillRect(x + 5, 5, anchoDibujo, altoDibujo);
            g.drawImage(imagen, x, 0, anchoDibujo, altoDibujo, null);
            g.dispose();
        }
    }

    private static class Motor {

        private static Boolean hay;

        static boolean disponible() {
            if (hay == null) {
                boolean ok;
                try {
                    Class.forName("org.apache.pdfbox.rendering.PDFRenderer");
                    Class.forName("org.apache.pdfbox.pdmodel.PDDocument");
                    ok = true;
                } catch (Throwable t) { ok = false; }
                hay = ok;
            }
            return hay;
        }

        private Object doc, renderer;
        private Method mRender, mNumPag;

        Motor(File f) throws Exception {
            Class<?> cDoc = Class.forName("org.apache.pdfbox.pdmodel.PDDocument");
            try {
                Class<?> cLoader = Class.forName("org.apache.pdfbox.Loader");
                doc = cLoader.getMethod("loadPDF", File.class).invoke(null, f);
            } catch (ClassNotFoundException e) {
                doc = cDoc.getMethod("load", File.class).invoke(null, f);
            }
            Class<?> cRend = Class.forName("org.apache.pdfbox.rendering.PDFRenderer");
            Constructor<?> ctor = cRend.getConstructor(cDoc);
            renderer = ctor.newInstance(doc);
            mRender = cRend.getMethod("renderImageWithDPI", int.class, float.class);
            mNumPag = cDoc.getMethod("getNumberOfPages");
        }

        int paginas() {
            try { return (Integer) mNumPag.invoke(doc); }
            catch (Throwable t) { return 1; }
        }

        BufferedImage render(int pagina, float dpi) throws Exception {
            return (BufferedImage) mRender.invoke(renderer, pagina, dpi);
        }

        javax.swing.tree.DefaultMutableTreeNode marcadores() {
            try {
                Object catalogo = doc.getClass().getMethod("getDocumentCatalog").invoke(doc);
                Object raizPdf  = catalogo.getClass().getMethod("getDocumentOutline").invoke(catalogo);
                if (raizPdf == null) return null;

                javax.swing.tree.DefaultMutableTreeNode raiz =
                        new javax.swing.tree.DefaultMutableTreeNode("Documento");
                int[] tope = { 3000 };
                agregarHijos(raizPdf, raiz, 0, tope);
                return raiz.getChildCount() > 0 ? raiz : null;
            } catch (Throwable t) { return null; }
        }

        private void agregarHijos(Object nodoPdf, javax.swing.tree.DefaultMutableTreeNode destino,
                                  int nivel, int[] tope) throws Exception {
            if (nivel > 6 || tope[0] <= 0) return;
            Object hijo = nodoPdf.getClass().getMethod("getFirstChild").invoke(nodoPdf);
            while (hijo != null && tope[0] > 0) {
                tope[0]--;
                String titulo = null;
                try { titulo = (String) hijo.getClass().getMethod("getTitle").invoke(hijo); }
                catch (Throwable ignorada) { }
                if (titulo == null || titulo.trim().isEmpty()) titulo = "(sin título)";

                javax.swing.tree.DefaultMutableTreeNode n =
                        new javax.swing.tree.DefaultMutableTreeNode(
                                new Marcador(titulo.trim(), paginaDe(hijo)));
                destino.add(n);
                agregarHijos(hijo, n, nivel + 1, tope);
                hijo = hijo.getClass().getMethod("getNextSibling").invoke(hijo);
            }
        }

        private int paginaDe(Object item) {
            try {
                Class<?> cDoc  = Class.forName("org.apache.pdfbox.pdmodel.PDDocument");
                Class<?> cPage = Class.forName("org.apache.pdfbox.pdmodel.PDPage");
                Object pag = item.getClass().getMethod("findDestinationPage", cDoc).invoke(item, doc);
                if (pag == null) return -1;
                Object arbol = doc.getClass().getMethod("getPages").invoke(doc);
                Object idx = arbol.getClass().getMethod("indexOf", cPage).invoke(arbol, pag);
                return ((Number) idx).intValue();
            } catch (Throwable t) { return -1; }
        }

        double anchoPuntos(int pagina) {
            try {
                Object pag = doc.getClass().getMethod("getPage", int.class).invoke(doc, pagina);
                Object caja = pag.getClass().getMethod("getMediaBox").invoke(pag);
                return ((Number) caja.getClass().getMethod("getWidth").invoke(caja)).doubleValue();
            } catch (Throwable t) { return 0; }
        }

        void cerrar() {
            try { doc.getClass().getMethod("close").invoke(doc); }
            catch (Throwable ignorada) { }
        }
    }
}