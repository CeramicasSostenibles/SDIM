package geoceramiclab;

import geoceramiclab.ensayos.EnsayosPanel;
import geoceramiclab.model.BaldosinCatalan;
import geoceramiclab.model.MunicipiosCO;
import geoceramiclab.model.SueloData;
import geoceramiclab.modelo3d.Ceramica3D;
import geoceramiclab.modelo3d.VentanaMezclas;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.BooleanSupplier;

public class MainWindow extends JFrame {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel      = new JPanel(cardLayout);
    private final JLabel lblEstado      = new JLabel();
    private final List<Paso> pasos      = new ArrayList<>();
    private Paso activo;

    private SueloData datos;
    private EnsayosPanel panelSuelo;
    private VentanaMezclas mezclas;
    private final JLabel lblGuardado = new JLabel(" ");
    private JLabel lblLugar;

    public MainWindow() {
        super(Marca.APP);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1440, 900);
        setMinimumSize(new Dimension(1100, 700));
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        getContentPane().setBackground(Tema.FONDO);
        setLayout(new BorderLayout(0, 0));

        SueloData datos = new SueloData();
        this.datos = datos;

        EnsayosPanel panelSuelo  = new EnsayosPanel(datos, () -> ir("pieza"));
        this.panelSuelo = panelSuelo;
        Ceramica3D   panelPieza  = new Ceramica3D(datos, () -> ir("suelo"));
        VentanaMezclas mezclas   = panelPieza.getMezclas();
        this.mezclas = mezclas;
        mezclas.setAlMostrar(() -> ir("mezcla"));

        cardPanel.setBackground(Tema.FONDO);
        cardPanel.add(panelSuelo,                  "suelo");
        cardPanel.add(panelPieza,                  "pieza");
        cardPanel.add(mezclas.getMezclasPanel(),   "mezcla");
        cardPanel.add(mezclas.getSecadoPanel(),    "secado");
        cardPanel.add(mezclas.getEnsayosPanel(),   "ntc");

        pasos.add(new Paso("suelo",  1, "Ensayos de suelo",  "Clasificación SUCS del suelo",
                () -> !"—".equals(datos.clasificacionUSCS) && datos.clasificacionUSCS != null));
        pasos.add(new Paso("pieza",  2, "Pieza",             "Modelo 3D y secciones",
                null));
        pasos.add(new Paso("mezcla", 3, "Diseño de mezcla",  "Dosificación de 3 mezclas",
                () -> mezclas.getEnsayosPanel().tieneMezcla()));
        pasos.add(new Paso("secado", 4, "Secado y cocción",  "Curvas por mezcla",
                null));
        pasos.add(new Paso("ntc",    5, "Ensayos NTC 919",   "Absorción, flexión e informe",
                () -> mezclas.getEnsayosPanel().ensayosCompletos()));

        add(encabezado(),          BorderLayout.NORTH);
        add(barraLateral(),        BorderLayout.WEST);
        add(cardPanel,             BorderLayout.CENTER);

        ir("suelo");

        int atajo = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, atajo), "guardarEstudio");
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S,
                 atajo | java.awt.event.InputEvent.SHIFT_DOWN_MASK), "guardarComo");
        getRootPane().getActionMap().put("guardarEstudio", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { guardarEstudio(false); }
        });
        getRootPane().getActionMap().put("guardarComo", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { guardarEstudio(true); }
        });

        SwingUtilities.invokeLater(this::abrirEstudioInicial);

        new Timer(700, e -> pasos.forEach(Paso::actualizar)).start();
    }

    private void ir(String id) {
        cardLayout.show(cardPanel, id);
        for (Paso p : pasos) {
            if (p.id.equals(id)) { activo = p; p.visitado = true; }
        }
        pasos.forEach(Paso::actualizar);
    }

    private JComponent encabezado() {
        JPanel h = new JPanel(new BorderLayout(14, 0));
        h.setBackground(Tema.BARRA);
        h.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.BORDE),
            BorderFactory.createEmptyBorder(8, 16, 8, 18)));

        JPanel izq = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        izq.setOpaque(false);
        Image ico = null;
        try { ico = Marca.icono(); } catch (Throwable ignorada) { }
        if (ico != null) izq.add(new JLabel(new ImageIcon(ico.getScaledInstance(30, 30, Image.SCALE_SMOOTH))));

        JPanel nombre = new JPanel(new GridLayout(2, 1, 0, 0));
        nombre.setOpaque(false);
        nombre.add(Tema.etiqueta(Marca.APP, Tema.TEXTO, Font.BOLD, Tema.T_SECCION));
        nombre.add(Tema.etiqueta(Marca.SUBAPP, Tema.TEXTO_3, Font.PLAIN, Tema.T_PEQUENO));
        izq.add(nombre);
        h.add(izq, BorderLayout.WEST);

        JPanel der = new JPanel(new GridLayout(2, 1, 0, 0));
        der.setOpaque(false);
        JLabel t = Tema.etiqueta("LUGAR DE ESTUDIO", Tema.TEXTO_3, Font.BOLD, Tema.T_PEQUENO);
        t.setHorizontalAlignment(SwingConstants.RIGHT);
        String lugar;
        try {
            MunicipiosCO.Municipio m = MunicipiosCO.actual();
            lugar = m == null ? "Sin definir" : m.nombreCompleto();
        } catch (Throwable e) { lugar = "Sin definir"; }
        lblLugar = Tema.etiqueta(lugar, Tema.TEXTO, Font.BOLD, Tema.T_TEXTO);
        lblLugar.setHorizontalAlignment(SwingConstants.RIGHT);
        der.add(t);
        der.add(lblLugar);
        JPanel este = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        este.setOpaque(false);
        lblGuardado.setFont(Tema.ui(Font.PLAIN, Tema.T_PEQUENO));
        lblGuardado.setForeground(Tema.TEXTO_3);
        este.add(lblGuardado);
        este.add(botonGuardar());
        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        sep.setPreferredSize(new Dimension(1, 30));
        sep.setForeground(Tema.BORDE);
        este.add(sep);
        este.add(der);
        h.add(este, BorderLayout.EAST);
        return h;
    }

    private JComponent barraLateral() {
        JPanel lista = new JPanel();
        lista.setLayout(new BoxLayout(lista, BoxLayout.Y_AXIS));
        lista.setBackground(Tema.BARRA);
        lista.setBorder(BorderFactory.createEmptyBorder(14, 10, 14, 10));

        JLabel t = Tema.etiqueta("PASOS", Tema.TEXTO_3, Font.BOLD, Tema.T_PEQUENO);
        t.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 0));
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        lista.add(t);
        for (Paso p : pasos) {
            p.setAlignmentX(Component.LEFT_ALIGNMENT);
            lista.add(p);
            lista.add(Box.createVerticalStrut(4));
        }
        lista.add(Box.createVerticalGlue());

        JLabel pie = Tema.etiqueta(Marca.SIGLA + " · versión " + Marca.VERSION, Tema.TEXTO_3,
                                   Font.PLAIN, Tema.T_PEQUENO);
        pie.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        pie.setAlignmentX(Component.LEFT_ALIGNMENT);
        lista.add(pie);

        JPanel marco = new JPanel(new BorderLayout());
        marco.setBackground(Tema.BARRA);
        marco.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Tema.BORDE));
        marco.setPreferredSize(new Dimension(236, 0));
        marco.add(lista, BorderLayout.CENTER);
        return marco;
    }

    private final class Paso extends JComponent {
        final String id, titulo, detalle;
        final int numero;
        final BooleanSupplier terminado;
        boolean visitado, hover, hecho;

        Paso(String id, int numero, String titulo, String detalle, BooleanSupplier terminado) {
            this.id = id; this.numero = numero; this.titulo = titulo;
            this.detalle = detalle; this.terminado = terminado;
            Dimension d = new Dimension(216, 56);
            setPreferredSize(d);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
            setMinimumSize(d);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText(titulo + " — " + detalle);
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { ir(Paso.this.id); }
                @Override public void mouseEntered(MouseEvent e) { hover = true;  repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hover = false; repaint(); }
            });
        }

        void actualizar() {
            boolean h;
            try { h = terminado != null ? terminado.getAsBoolean() : (visitado && this != activo); }
            catch (Throwable e) { h = false; }
            if (h != hecho) { hecho = h; }
            repaint();
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            boolean act = this == activo;

            if (act || hover) {
                g.setColor(act ? Tema.SUPERFICIE_2 : Tema.SUPERFICIE);
                g.fillRoundRect(0, 0, w, h, 10, 10);
            }
            if (act) {
                g.setColor(Tema.ACENTO);
                g.fillRoundRect(0, 10, 4, h - 20, 4, 4);
            }

            int d = 28, cx = 14, cy = (h - d) / 2;
            if (hecho) {
                g.setColor(Tema.OK);
                g.fillOval(cx, cy, d, d);
                g.setColor(Tema.BARRA);
                g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawPolyline(new int[]{cx + 8, cx + 12, cx + 20}, new int[]{cy + 14, cy + 19, cy + 9}, 3);
            } else {
                g.setColor(act ? Tema.ACENTO : Tema.CONTROL);
                g.fillOval(cx, cy, d, d);
                g.setColor(act ? Color.WHITE : Tema.TEXTO_2);
                g.setFont(Tema.ui(Font.BOLD, Tema.T_TEXTO));
                String n = String.valueOf(numero);
                FontMetrics fm = g.getFontMetrics();
                g.drawString(n, cx + (d - fm.stringWidth(n)) / 2, cy + (d + fm.getAscent() - fm.getDescent()) / 2);
            }

            int tx = cx + d + 12;
            g.setFont(Tema.ui(Font.BOLD, Tema.T_BOTON));
            g.setColor(act ? Tema.TEXTO : Tema.TEXTO_2);
            g.drawString(titulo, tx, h / 2 - 2);
            g.setFont(Tema.ui(Font.PLAIN, Tema.T_PEQUENO));
            g.setColor(Tema.TEXTO_3);
            g.drawString(recortar(g, detalle, w - tx - 8), tx, h / 2 + 14);
            g.dispose();
        }

        private String recortar(Graphics2D g, String s, int ancho) {
            FontMetrics fm = g.getFontMetrics();
            if (fm.stringWidth(s) <= ancho) return s;
            while (s.length() > 1 && fm.stringWidth(s + "…") > ancho) s = s.substring(0, s.length() - 1);
            return s + "…";
        }
    }

    private JComponent barraEstado(SueloData datos) {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Tema.BARRA);
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.BORDE),
            BorderFactory.createEmptyBorder(5, 16, 5, 16)));
        lblEstado.setText("Complete los ensayos de suelo y pulse «Clasificar suelo».");
        lblEstado.setForeground(Tema.TEXTO_3);
        lblEstado.setFont(Tema.ui(Font.PLAIN, Tema.T_PEQUENO));
        bar.add(lblEstado, BorderLayout.WEST);

        datos.addOyente(() -> SwingUtilities.invokeLater(() -> {
            StringBuilder sb = new StringBuilder("Suelo: ").append(datos.nombreFinal);
            if (datos.atterCalculada) sb.append("   ·   SUCS: ").append(datos.clasificacionUSCS);
            if (datos.hidroCalculada) sb.append("   ·   Textural: ").append(datos.claseTextural);
            if (datos.granCalculada)  sb.append("   ·   Cu = ").append(fmt(datos.Cu))
                                        .append("   Cc = ").append(fmt(datos.Cc));
            lblEstado.setText(sb.toString());
            lblEstado.setForeground(Tema.TEXTO_2);
        }));
        return bar;
    }

    private static String fmt(Double v) {
        return v == null ? "—" : String.format("%.2f", v);
    }

    private JComponent botonGuardar() {
        JButton b = new JButton("Guardar", new IconoDisquete(16, Tema.TEXTO));
        b.setFont(Tema.ui(Font.BOLD, 12));
        b.setIconTextGap(8);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setToolTipText("<html>Guardar el estudio completo (Ctrl+S)<br>"
                       + "Clic derecho o Ctrl+Mayús+S: guardar como…</html>");
        b.putClientProperty("JButton.buttonType", "roundRect");
        b.addActionListener(e -> guardarEstudio(false));
        JPopupMenu menu = new JPopupMenu();
        JMenuItem como = new JMenuItem("Guardar como…");
        como.addActionListener(e -> guardarEstudio(true));
        menu.add(como);
        b.setComponentPopupMenu(menu);
        return b;
    }

    private void guardarEstudio(boolean como) {
        File f = Proyecto.archivo;
        if (como || f == null) {
            JFileChooser fc = new JFileChooser(f != null ? f.getParentFile() : Proyecto.carpeta());
            fc.setDialogTitle("Guardar estudio");
            fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "SDIM (*." + Proyecto.EXT + ")", Proyecto.EXT));
            fc.setSelectedFile(f != null ? f : new File(Proyecto.carpeta(), "estudio." + Proyecto.EXT));
            if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            f = fc.getSelectedFile();
            if (!f.getName().toLowerCase().endsWith("." + Proyecto.EXT))
                f = new File(f.getParentFile(), f.getName() + "." + Proyecto.EXT);
            if (f.exists() && !f.equals(Proyecto.archivo) && JOptionPane.showConfirmDialog(this,
                    "El archivo ya existe. ¿Reemplazarlo?", "Guardar", JOptionPane.YES_NO_OPTION)
                    != JOptionPane.YES_OPTION) return;
        }
        try {
            Properties p = new Properties();
            p.setProperty("estudio.version", "2");

            BaldosinCatalan b = BaldosinCatalan.actual();
            if (b != null) {
                p.setProperty("tipo", b.tipo.name());
                p.setProperty("ancho", String.valueOf(b.ancho));
                p.setProperty("largo", String.valueOf(b.largo));
            }
            MunicipiosCO.Municipio m = MunicipiosCO.actual();
            p.setProperty("municipio", m == null ? "" : m.codigoDane);

            Proyecto.capturar(panelSuelo, "suelo.", p);
            p.setProperty("suelo.granCalculada",  String.valueOf(datos.granCalculada));
            p.setProperty("suelo.hidroCalculada", String.valueOf(datos.hidroCalculada));
            p.setProperty("suelo.atterCalculada", String.valueOf(datos.atterCalculada));
            p.setProperty("suelo.uscs",   String.valueOf(datos.clasificacionUSCS));
            p.setProperty("suelo.nombre", String.valueOf(datos.nombreFinal));

            Proyecto.capturar(mezclas.getMezclasPanel(), "mezcla.", p);
            p.setProperty("mezcla.calculada", String.valueOf(mezclas.getEnsayosPanel().tieneMezcla()));

            mezclas.getEnsayosPanel().exportar(p, "ntc.");

            Proyecto.escribir(f, p);
            Proyecto.archivo = f;
            Proyecto.anotarReciente(f);
            avisoGuardado("✓ Guardado " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                          + " · " + f.getName());
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "No se pudo guardar:\n" + ex.getMessage(),
                    "Guardar", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void avisoGuardado(String t) {
        lblGuardado.setText(t);
        lblGuardado.setForeground(Tema.OK);
        setTitle(Marca.APP);
        Timer tm = new Timer(4000, e -> lblGuardado.setForeground(Tema.TEXTO_3));
        tm.setRepeats(false);
        tm.start();
    }

    private void abrirEstudioInicial() {
        File f = Proyecto.archivo;
        if (f == null) return;
        Properties p;
        try { p = Proyecto.leer(f); } catch (Exception ex) { return; }
        if (!p.containsKey("estudio.version")) {
            avisoGuardado("Estudio: " + f.getName());
            return;
        }
        try {

            // Restaurar el lugar (municipio) elegido y reflejarlo en el encabezado
            String codMun = p.getProperty("municipio");
            if (codMun != null && !codMun.isBlank()) {
                MunicipiosCO.Municipio m = MunicipiosCO.porCodigo(codMun);
                if (m != null) {
                    MunicipiosCO.setActual(m);
                    if (lblLugar != null) lblLugar.setText(m.nombreCompleto());
                }
            }

            Proyecto.restaurar(panelSuelo, "suelo.", p);
            if (Boolean.parseBoolean(p.getProperty("suelo.granCalculada")))
                Proyecto.pulsar(panelSuelo, "Calcular granulometría");
            if (Boolean.parseBoolean(p.getProperty("suelo.hidroCalculada")))
                Proyecto.pulsar(panelSuelo, "Calcular hidrómetro");
            if (Boolean.parseBoolean(p.getProperty("suelo.atterCalculada")))
                Proyecto.pulsar(panelSuelo, "Calcular Atterberg");
            String uscs = p.getProperty("suelo.uscs");
            if (uscs != null && !uscs.equals("null")) datos.clasificacionUSCS = uscs;
            String nom = p.getProperty("suelo.nombre");
            if (nom != null && !nom.equals("null")) datos.nombreFinal = nom;
            datos.notificar();

            Proyecto.restaurar(mezclas.getMezclasPanel(), "mezcla.", p);
            if (Boolean.parseBoolean(p.getProperty("mezcla.calculada")))
                Proyecto.pulsar(mezclas.getMezclasPanel(), "Calcular 3 mezclas");

            mezclas.getEnsayosPanel().importar(p, "ntc.");
            avisoGuardado("Estudio abierto: " + f.getName());
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "El estudio se abrió con advertencias:\n" + ex,
                    "Abrir estudio", JOptionPane.WARNING_MESSAGE);
        }
    }

    private static final class IconoDisquete implements Icon {
        private final int t; private final Color c;
        IconoDisquete(int t, Color c) { this.t = t; this.c = c; }
        @Override public int getIconWidth()  { return t; }
        @Override public int getIconHeight() { return t; }
        @Override public void paintIcon(Component comp, Graphics g0, int x, int y) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.translate(x, y);
            double k = t / 16.0;
            g.scale(k, k);
            g.setColor(c);
            java.awt.geom.Path2D cuerpo = new java.awt.geom.Path2D.Double();
            cuerpo.moveTo(1.5, 1.5); cuerpo.lineTo(12, 1.5); cuerpo.lineTo(14.5, 4); cuerpo.lineTo(14.5, 14.5);
            cuerpo.lineTo(1.5, 14.5); cuerpo.closePath();
            g.setStroke(new BasicStroke(1.4f));
            g.draw(cuerpo);
            g.fill(new java.awt.geom.Rectangle2D.Double(4.2, 1.5, 6.6, 4.2));
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 90));
            g.fill(new java.awt.geom.RoundRectangle2D.Double(3.8, 8.5, 8.4, 6, 1.5, 1.5));
            g.dispose();
        }
    }
}
