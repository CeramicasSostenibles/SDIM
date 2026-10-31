package geoceramiclab.modelo3d;

import geoceramiclab.Tema;
import geoceramiclab.model.BaldosinCatalan;
import geoceramiclab.model.BaldosinCatalan.TipoCara;
import geoceramiclab.model.MunicipiosCO;
import geoceramiclab.model.MunicipiosCO.Municipio;
import geoceramiclab.PanelPdf;
import geoceramiclab.VisorPdf;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.prefs.Preferences;

public class DialogoInicio extends JDialog {

    static Color FONDO = Tema.FONDO;
    static Color PANEL = Tema.SUPERFICIE;
    static Color CABE = Tema.SUPERFICIE_2;
    static final Color ACENTO = Tema.ACENTO;
    static Color TXT = Tema.TEXTO;
    static Color TXT2 = Tema.TEXTO_2;
    static Color HOVER = Tema.SELECCION;
    static Color LINEA = Tema.BORDE;

    static final String EXT = "gcl";
    static final double ANCHO_DEF = 10.0;
    static final double LARGO_DEF = 20.0;

    private static final Preferences PREFS = Preferences.userNodeForPackage(DialogoInicio.class);
    private static final String K_CARPETA   = "carpetaProyecto";
    private static final String K_RECIENTES = "archivosRecientes";

    private BaldosinCatalan resultado = null;
    private TipoCara  seleccion = TipoCara.NERVADA_UNA;
    private Municipio lugar     = null;
    private double ancho = ANCHO_DEF, largo = LARGO_DEF;
    private File carpeta;

    private final CardLayout cards = new CardLayout();
    private final JPanel tarjetas  = new JPanel(cards);

    private final Tarjeta tarjNervada = new Tarjeta(TipoCara.NERVADA_UNA, "Nervada por una cara");
    private final Tarjeta tarjLisa    = new Tarjeta(TipoCara.LISA_AMBAS,  "Lisa por ambas caras");

    private final JLabel lblCarpeta     = new JLabel();
    private final JPanel panelRecientes = new JPanel();

    private final JTextField txtBuscar = new JTextField();
    private final JComboBox<String> cboDepto = new JComboBox<>();
    private final ModeloMunicipios modelo = new ModeloMunicipios();
    private final JList<Municipio> lista = new JList<>(modelo);
    private final MapaColombia mapa = new MapaColombia();
    private final JCheckBox chkAcercar = new JCheckBox("Acercar al elegir", true);
    private final JLabel lblLugar  = new JLabel();
    private final JLabel lblConteo = new JLabel();
    private JButton btnTerminar;

    private final Timer animador = new Timer(33, null);
    private JCheckBoxMenuItem miGirar;
    private boolean sincronizando = false;
    private Timer antirrebote;

    private final JPanel panelDocumento = new JPanel(new BorderLayout());
    private PanelPdf pdfActual;
    private JLabel lblDocTitulo, lblDocSub;
    private String cardActual = "inicio";

    public static BaldosinCatalan pedir(Frame owner) {
        DialogoInicio d = new DialogoInicio(owner);
        d.setVisible(true);
        return d.resultado;
    }

    public DialogoInicio(Frame owner) {
        super(owner, "SDIM", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        carpeta = new File(PREFS.get(K_CARPETA,
                new File(System.getProperty("user.home"), "SDIM").getAbsolutePath()));

        setJMenuBar(construirMenu());

        tarjetas.setOpaque(false);
        tarjetas.add(pasoInicio(),    "inicio");
        tarjetas.add(pasoEstudio(),   "estudio");
        tarjetas.add(pasoDocumento(), "documento");

        JPanel raiz = new JPanel(new GridBagLayout());
        raiz.setBackground(FONDO);
        raiz.add(tarjetas);
        setContentPane(raiz);

        animador.addActionListener(e -> {
            tarjNervada.angulo += 0.015;
            tarjLisa.angulo    += 0.015;
            tarjNervada.repaint();
            tarjLisa.repaint();
        });
        addWindowListener(new WindowAdapter() {
            @Override public void windowOpened(WindowEvent e){ if (miGirar.isSelected()) animador.start(); }
            @Override public void windowClosed(WindowEvent e){ animador.stop(); }
        });

        refrescarRecientes();
        refrescarGeometria();
        filtrar();

        Rectangle scr = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setSize(Math.min(1220, (int)(scr.width * 0.94)), Math.min(820, (int)(scr.height * 0.94)));
        setMinimumSize(new Dimension(1040, 700));
        setLocationRelativeTo(owner);
    }

    private JComponent pasoInicio() {
        JPanel p = marco("SDIM",
                "Dosificación de baldosín a partir de la clasificación física del suelo",
                botonTema());

        JPanel caja = cajaBlanca();
        JPanel columnas = new JPanel(new GridLayout(1, 2, 1, 0));
        columnas.setBackground(LINEA);
        columnas.setPreferredSize(new Dimension(720, 480));
        columnas.add(columnaArchivo());
        columnas.add(columnaUbicacion());
        caja.add(columnas, BorderLayout.CENTER);
        caja.add(pie(null, null, "Continuar  →", () -> ir("estudio")), BorderLayout.SOUTH);

        p.add(caja, BorderLayout.CENTER);
        return p;
    }

    private JComponent columnaArchivo() {
        JPanel col = columna("Archivo");
        JPanel c = cuerpoDe(col);
        c.add(fila("Nuevo…",   "Empezar de cero con la geometría por defecto", "▢", this::nuevo));
        c.add(fila("Abrir…",   "Cargar un estudio .gcl guardado",              "▷", this::abrir));
        c.add(fila("Guardar…", "Guardar la configuración actual",              "▼", this::guardar));
        c.add(Box.createVerticalGlue());
        return col;
    }

    private JComponent columnaUbicacion() {
        JPanel col = columna("Ubicación del proyecto");
        JPanel c = cuerpoDe(col);

        c.add(subtitulo("Se guarda en"));
        lblCarpeta.setFont(Tema.ui(Font.PLAIN, 12));
        lblCarpeta.setForeground(TXT);
        lblCarpeta.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(LINEA), new EmptyBorder(8, 10, 8, 10)));
        lblCarpeta.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblCarpeta.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        c.add(lblCarpeta);

        JButton b = new JButton("Cambiar carpeta…");
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.addActionListener(e -> elegirCarpeta());
        c.add(Box.createVerticalStrut(8));
        c.add(b);

        c.add(Box.createVerticalStrut(16));
        c.add(subtitulo("Archivos recientes"));
        panelRecientes.setLayout(new BoxLayout(panelRecientes, BoxLayout.Y_AXIS));
        panelRecientes.setOpaque(false);
        panelRecientes.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.add(scroll(panelRecientes));
        return col;
    }


    private JComponent pasoEstudio() {
        JPanel p = marco("Geometría de la pieza y lugar de estudio",
                "Elige el tipo de baldosín y el municipio de donde proviene la muestra de suelo.");

        JPanel caja = cajaBlanca();
        JPanel cuerpo = new JPanel(new BorderLayout());
        cuerpo.setBackground(PANEL);
        cuerpo.setPreferredSize(new Dimension(1080, 480));

        cuerpo.add(bloqueFiguras(), BorderLayout.WEST);

        JPanel medio = new JPanel(new BorderLayout());
        medio.setBackground(PANEL);
        medio.add(bloqueMunicipios(), BorderLayout.WEST);
        medio.add(bloqueMapa(),       BorderLayout.CENTER);
        cuerpo.add(medio, BorderLayout.CENTER);

        caja.add(cuerpo, BorderLayout.CENTER);
        caja.add(pie("←  Atrás", () -> ir("inicio"), "Comenzar ensayos  →", this::aceptar),
                 BorderLayout.SOUTH);

        conectarEventos();
        actualizarLugar();
        p.add(caja, BorderLayout.CENTER);
        return p;
    }

    private JComponent pasoDocumento() {
        JPanel p = new JPanel(new BorderLayout(0, 14));
        p.setOpaque(false);

        lblDocTitulo = new JLabel("Documento");
        lblDocTitulo.setFont(lblDocTitulo.getFont().deriveFont(Font.PLAIN, 30f));
        lblDocTitulo.setForeground(Color.WHITE);
        lblDocTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblDocSub = new JLabel(" ");
        lblDocSub.setFont(lblDocSub.getFont().deriveFont(Font.PLAIN, 13f));
        lblDocSub.setForeground(Tema.TEXTO_2);
        lblDocSub.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel cab = new JPanel();
        cab.setLayout(new BoxLayout(cab, BoxLayout.Y_AXIS));
        cab.setOpaque(false);
        cab.setBorder(new EmptyBorder(0, 6, 4, 0));
        cab.add(lblDocTitulo);
        cab.add(Box.createVerticalStrut(4));
        cab.add(lblDocSub);
        p.add(cab, BorderLayout.NORTH);

        JPanel caja = cajaBlanca();
        panelDocumento.setBackground(PANEL);
        panelDocumento.setPreferredSize(new Dimension(1080, 480));
        caja.add(panelDocumento, BorderLayout.CENTER);

        JPanel barra = new JPanel(new BorderLayout());
        barra.setBackground(Tema.SUPERFICIE_2);
        barra.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, LINEA),
                new EmptyBorder(12, 18, 12, 18)));
        JButton volver = new JButton("←  Volver");
        volver.setFocusPainted(false);
        volver.addActionListener(e -> cerrarDocumento());
        JPanel wi = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        wi.setOpaque(false);
        wi.add(volver);
        barra.add(wi, BorderLayout.WEST);
        caja.add(barra, BorderLayout.SOUTH);

        p.add(caja, BorderLayout.CENTER);
        return p;
    }


    private String volverA = "inicio";

    private void cerrarDocumento() {
        soltarPdf();
        ir(volverA == null ? "inicio" : volverA);
    }

    private void soltarPdf() {
        if (pdfActual != null) {
            try { pdfActual.cerrar(); } catch (Throwable ignorada) { }
            panelDocumento.remove(pdfActual);
            pdfActual = null;
        }
    }

    private JComponent bloqueFiguras() {
        JPanel col = new JPanel(new BorderLayout(0, 8));
        col.setBackground(PANEL);
        col.setPreferredSize(new Dimension(290, 10));
        col.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, LINEA),
                new EmptyBorder(14, 16, 14, 14)));

        col.add(encabezado("Geometría de la pieza"), BorderLayout.NORTH);

        JPanel figs = new JPanel(new GridLayout(1, 1, 0, 0));
        figs.setBackground(PANEL);
        figs.add(tarjNervada);
        col.add(figs, BorderLayout.CENTER);
        return col;
    }

    private JComponent bloqueMunicipios() {
        JPanel col = new JPanel(new BorderLayout(0, 8));
        col.setBackground(PANEL);
        col.setPreferredSize(new Dimension(320, 10));
        col.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, LINEA),
                new EmptyBorder(14, 14, 14, 14)));

        JPanel arriba = new JPanel(new BorderLayout(0, 6));
        arriba.setOpaque(false);
        arriba.add(encabezado("Lugar de estudio"), BorderLayout.NORTH);

        JPanel filtros = new JPanel(new GridBagLayout());
        filtros.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(2, 0, 2, 5);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridx = 0; gc.gridy = 0; gc.weightx = 0;
        filtros.add(new JLabel("Buscar:"), gc);
        gc.gridx = 1; gc.weightx = 1;
        txtBuscar.setToolTipText("Nombre del municipio, del departamento o código DANE");
        filtros.add(txtBuscar, gc);
        gc.gridx = 0; gc.gridy = 1; gc.weightx = 0;
        filtros.add(new JLabel("Depto.:"), gc);
        gc.gridx = 1; gc.weightx = 1;
        cboDepto.addItem("Todos");
        for (String d : MunicipiosCO.departamentos()) cboDepto.addItem(d);
        filtros.add(cboDepto, gc);
        arriba.add(filtros, BorderLayout.CENTER);
        col.add(arriba, BorderLayout.NORTH);

        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lista.setCellRenderer(new RenderMunicipio());
        lista.setFixedCellHeight(36);
        JScrollPane sc = new JScrollPane(lista,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sc.setBorder(BorderFactory.createLineBorder(LINEA));
        col.add(sc, BorderLayout.CENTER);

        JPanel abajo = new JPanel(new BorderLayout());
        abajo.setOpaque(false);
        lblConteo.setFont(lblConteo.getFont().deriveFont(Font.PLAIN, 11f));
        lblConteo.setForeground(TXT2);
        abajo.add(lblConteo, BorderLayout.NORTH);
        chkAcercar.setOpaque(false);
        chkAcercar.setFont(chkAcercar.getFont().deriveFont(Font.PLAIN, 11f));
        chkAcercar.setToolTipText("Al elegir en la lista, el mapa se acerca al municipio");
        abajo.add(chkAcercar, BorderLayout.SOUTH);
        col.add(abajo, BorderLayout.SOUTH);
        return col;
    }

    private JComponent bloqueMapa() {
        JPanel col = new JPanel(new BorderLayout(0, 8));
        col.setBackground(PANEL);
        col.setBorder(new EmptyBorder(14, 14, 14, 16));

        col.add(encabezado("Mapa de Colombia"), BorderLayout.NORTH);

        JPanel envolt = new JPanel(new BorderLayout());
        envolt.setBorder(BorderFactory.createLineBorder(LINEA));
        envolt.add(mapa, BorderLayout.CENTER);
        col.add(envolt, BorderLayout.CENTER);

        lblLugar.setBorder(new EmptyBorder(4, 2, 0, 2));
        lblLugar.setForeground(TXT);
        col.add(lblLugar, BorderLayout.SOUTH);
        return col;
    }

    private JLabel encabezado(String s) {
        JLabel l = new JLabel(s);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 13f));
        l.setForeground(TXT);
        l.setBorder(new EmptyBorder(0, 0, 6, 0));
        return l;
    }

    private void conectarEventos() {
        for (Tarjeta t : new Tarjeta[]{tarjNervada, tarjLisa}) {
            t.addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e){ t.hover = true;  t.repaint(); }
                @Override public void mouseExited (MouseEvent e){ t.hover = false; t.repaint(); }
                @Override public void mouseClicked(MouseEvent e){ seleccionar(t.tipo); }
            });
            t.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        antirrebote = new Timer(160, e -> filtrar());
        antirrebote.setRepeats(false);
        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e){ antirrebote.restart(); }
            public void removeUpdate(DocumentEvent e){ antirrebote.restart(); }
            public void changedUpdate(DocumentEvent e){ antirrebote.restart(); }
        });
        cboDepto.addActionListener(e -> {
            if (sincronizando) return;
            String d = (String) cboDepto.getSelectedItem();
            mapa.setDepartamento("Todos".equals(d) ? null : d);
            filtrar();
        });
        lista.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || sincronizando) return;
            Municipio m = lista.getSelectedValue();
            if (m != null) fijarLugar(m, false);
        });
        mapa.setAlSeleccionar(m -> fijarLugar(m, true));
    }

    private void filtrar() {
        String d = (String) cboDepto.getSelectedItem();
        List<Municipio> ms = MunicipiosCO.buscar(txtBuscar.getText(),
                (d == null || "Todos".equals(d)) ? null : d);
        boolean prev = sincronizando;
        sincronizando = true;
        modelo.setDatos(ms);
        if (lugar != null && ms.contains(lugar)) lista.setSelectedValue(lugar, true);
        sincronizando = prev;
        lblConteo.setText(ms.size() + " de " + MunicipiosCO.todos().size() + " municipios");
    }

    private void fijarLugar(Municipio m, boolean desdeMapa) {
        lugar = m;
        if (desdeMapa) {
            mapa.setSeleccionado(m);
            sincronizando = true;
            if (!modelo.contiene(m)) {
                txtBuscar.setText("");
                cboDepto.setSelectedItem("Todos");
                mapa.setDepartamento(null);
                filtrar();
            }
            lista.setSelectedValue(m, true);
            sincronizando = false;
        } else {
            if (chkAcercar.isSelected()) mapa.acercarA(m, Math.max(8, mapa.getZoom()));
            else                         mapa.setSeleccionado(m);
            sincronizando = true;
            lista.setSelectedValue(m, true);
            sincronizando = false;
        }
        actualizarLugar();
    }

    private void actualizarLugar() {
        if (lugar == null) {
            lblLugar.setText("<html><span style='color:#909090'>"
                    + "Elige el municipio en la lista o haz clic en el mapa</span></html>");
            if (btnTerminar != null) btnTerminar.setEnabled(false);
        } else {
            lblLugar.setText(String.format(
                    "<html><b>%s</b> &nbsp;·&nbsp; código DANE %s<br>"
                  + "<span style='color:#707070'>%s</span></html>",
                    lugar.nombreCompleto(), lugar.codigoDane, lugar.coordenadas()));
            if (btnTerminar != null) btnTerminar.setEnabled(true);
        }
    }

    private static class ModeloMunicipios extends AbstractListModel<Municipio> {
        private List<Municipio> datos = new ArrayList<>();

        void setDatos(List<Municipio> nuevos) {
            int antes = datos.size();
            datos = (nuevos == null) ? new ArrayList<>() : nuevos;
            if (antes > 0)        fireIntervalRemoved(this, 0, antes - 1);
            if (!datos.isEmpty()) fireIntervalAdded(this, 0, datos.size() - 1);
        }
        boolean contiene(Municipio m) { return datos.contains(m); }

        @Override public int getSize() { return datos.size(); }
        @Override public Municipio getElementAt(int i) { return datos.get(i); }
    }

    private static class RenderMunicipio extends JPanel implements ListCellRenderer<Municipio> {
        private final JLabel l1 = new JLabel(), l2 = new JLabel();

        RenderMunicipio() {
            super(new BorderLayout());
            setBorder(new EmptyBorder(3, 8, 3, 8));
            l1.setFont(l1.getFont().deriveFont(Font.PLAIN, 13f));
            l2.setFont(l2.getFont().deriveFont(Font.PLAIN, 10.5f));
            add(l1, BorderLayout.CENTER);
            add(l2, BorderLayout.SOUTH);
        }

        @Override public Component getListCellRendererComponent(JList<? extends Municipio> l,
                Municipio m, int i, boolean sel, boolean foco) {
            l1.setText(m.nombre);
            l2.setText(m.departamento + "  ·  " + m.codigoDane);
            setOpaque(true);
            setBackground(sel ? l.getSelectionBackground() : l.getBackground());
            l1.setForeground(sel ? l.getSelectionForeground() : TXT);
            l2.setForeground(sel ? l.getSelectionForeground() : TXT2);
            return this;
        }
    }

    private JPanel marco(String titulo, String subtitulo) {
        return marco(titulo, subtitulo, null);
    }

    private JPanel marco(String titulo, String subtitulo, JComponent este) {
        JPanel p = new JPanel(new BorderLayout(0, 14));
        p.setOpaque(false);

        JLabel t = new JLabel(titulo);
        t.setFont(t.getFont().deriveFont(Font.PLAIN, 30f));
        t.setForeground(TXT);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel s = new JLabel(subtitulo);
        s.setFont(s.getFont().deriveFont(Font.PLAIN, 13f));
        s.setForeground(TXT2);
        s.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel cab = new JPanel();
        cab.setLayout(new BoxLayout(cab, BoxLayout.Y_AXIS));
        cab.setOpaque(false);
        cab.setBorder(new EmptyBorder(0, 6, 4, 0));
        cab.add(t); cab.add(Box.createVerticalStrut(4)); cab.add(s);

        JPanel fila = new JPanel(new BorderLayout());
        fila.setOpaque(false);
        fila.add(cab, BorderLayout.WEST);
        if (este != null) {
            JPanel we = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 6));
            we.setOpaque(false);
            we.add(este);
            fila.add(we, BorderLayout.EAST);
        }
        p.add(fila, BorderLayout.NORTH);
        return p;
    }

    private JButton botonTema() {
        JButton b = new JButton(Tema.esOscuro() ? "  Tema claro" : "  Tema oscuro",
                                new IconoTema(16, TXT));
        b.setFont(Tema.ui(Font.BOLD, 12));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.putClientProperty("JButton.buttonType", "roundRect");
        b.putClientProperty("FlatLaf.style",
            "background: " + Tema.hex(CABE) + "; foreground: " + Tema.hex(TXT)
            + "; borderColor: " + Tema.hex(LINEA) + "; hoverBackground: " + Tema.hex(HOVER));
        b.setToolTipText("Cambiar entre tema claro y oscuro (todo el programa)");
        b.addActionListener(e -> cambiarTema());
        return b;
    }

    private void cambiarTema() {
        boolean nuevoOscuro = !Tema.esOscuro();
        Tema.guardarTema(nuevoOscuro);
        if (reiniciar()) return;
        JOptionPane.showMessageDialog(this,
            "El tema " + (nuevoOscuro ? "oscuro" : "claro") + " se aplicará cuando vuelvas a abrir SDIM.",
            "Tema", JOptionPane.INFORMATION_MESSAGE);
    }

    private boolean reiniciar() {
        try {
            String sep = java.io.File.separator;
            String javaBin = System.getProperty("java.home") + sep + "bin" + sep
                           + (System.getProperty("os.name", "").toLowerCase().contains("win") ? "java.exe" : "java");
            String cp = System.getProperty("java.class.path");
            if (cp == null || cp.isEmpty()) return false;
            java.util.List<String> cmd = new java.util.ArrayList<>();
            cmd.add(javaBin);
            cmd.add("-cp"); cmd.add(cp);
            cmd.add("geoceramiclab.Main");
            new ProcessBuilder(cmd).inheritIO().start();
            Window w = SwingUtilities.getWindowAncestor(this);
            if (w != null) w.dispose();
            System.exit(0);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static final class IconoTema implements Icon {
        private final int t; private final Color c;
        IconoTema(int t, Color c) { this.t = t; this.c = c; }
        @Override public int getIconWidth()  { return t; }
        @Override public int getIconHeight() { return t; }
        @Override public void paintIcon(Component comp, Graphics g0, int x, int y) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.translate(x, y);
            g.setColor(c);
            if (Tema.esOscuro()) {
                int r = t / 4;
                g.fillOval(t/2 - r, t/2 - r, 2*r, 2*r);
                g.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI * i / 4;
                    int x1 = (int)(t/2 + Math.cos(a)*(r+2)), y1 = (int)(t/2 + Math.sin(a)*(r+2));
                    int x2 = (int)(t/2 + Math.cos(a)*(r+4)), y2 = (int)(t/2 + Math.sin(a)*(r+4));
                    g.drawLine(x1, y1, x2, y2);
                }
            } else {
                g.fillOval(1, 1, t-2, t-2);
                g.setColor(comp.getBackground() != null ? comp.getBackground() : Color.WHITE);
                g.fillOval(t/2 - 1, 0, t-2, t-2);
            }
            g.dispose();
        }
    }

    private JPanel cajaBlanca() {
        JPanel caja = new JPanel(new BorderLayout());
        caja.setBackground(PANEL);
        caja.setBorder(BorderFactory.createLineBorder(LINEA, 1));
        return caja;
    }

    private JPanel pie(String txtIzq, Runnable accIzq, String txtDer, Runnable accDer) {
        JPanel barra = new JPanel(new BorderLayout());
        barra.setBackground(CABE);
        barra.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, LINEA),
                new EmptyBorder(12, 18, 12, 18)));

        if (txtIzq != null) {
            JButton a = new JButton(txtIzq);
            a.setFocusPainted(false);
            a.addActionListener(e -> accIzq.run());
            JPanel wi = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            wi.setOpaque(false);
            wi.add(a);
            barra.add(wi, BorderLayout.WEST);
        }

        JButton d = Tema.boton(txtDer, Tema.Tipo.PRIMARIO);
        d.addActionListener(e -> accDer.run());
        JPanel wd = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        wd.setOpaque(false);
        wd.add(d);
        barra.add(wd, BorderLayout.EAST);

        if (txtDer.startsWith("Comenzar")) btnTerminar = d;
        return barra;
    }

    private void ir(String paso) {
        cardActual = paso;
        cards.show(tarjetas, paso);
        if ("estudio".equals(paso)) SwingUtilities.invokeLater(txtBuscar::requestFocusInWindow);
    }

    private JPanel columna(String titulo) {
        JPanel col = new JPanel(new BorderLayout());
        col.setBackground(PANEL);

        JLabel t = new JLabel(titulo);
        t.setFont(t.getFont().deriveFont(Font.PLAIN, 19f));
        t.setForeground(TXT);
        t.setBorder(new EmptyBorder(14, 18, 14, 18));
        JPanel cabe = new JPanel(new BorderLayout());
        cabe.setBackground(CABE);
        cabe.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, LINEA));
        cabe.add(t, BorderLayout.WEST);
        col.add(cabe, BorderLayout.NORTH);

        JPanel cuerpo = new JPanel();
        cuerpo.setLayout(new BoxLayout(cuerpo, BoxLayout.Y_AXIS));
        cuerpo.setOpaque(false);
        cuerpo.setBorder(new EmptyBorder(12, 12, 12, 12));
        col.add(cuerpo, BorderLayout.CENTER);
        return col;
    }

    private JPanel cuerpoDe(JPanel col) {
        return (JPanel) ((BorderLayout) col.getLayout()).getLayoutComponent(BorderLayout.CENTER);
    }

    private JScrollPane scroll(JComponent c) {
        JScrollPane sc = new JScrollPane(c,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sc.setBorder(null);
        sc.setOpaque(false);
        sc.getViewport().setOpaque(false);
        sc.getVerticalScrollBar().setUnitIncrement(16);
        sc.setAlignmentX(Component.LEFT_ALIGNMENT);
        return sc;
    }

    private JLabel subtitulo(String s) {
        JLabel l = new JLabel(s);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 11f));
        l.setForeground(TXT2);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(new EmptyBorder(4, 6, 6, 0));
        return l;
    }

    private JPanel fila(String texto, String sub, String simbolo, Runnable accion) {
        JPanel f = new JPanel(new BorderLayout(10, 0));
        f.setBackground(PANEL);
        f.setBorder(new EmptyBorder(7, 8, 7, 8));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        f.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel ico = new JLabel(simbolo, SwingConstants.CENTER);
        ico.setFont(Tema.ui(Font.PLAIN, 16));
        ico.setForeground(ACENTO);
        ico.setPreferredSize(new Dimension(24, 24));
        f.add(ico, BorderLayout.WEST);

        JLabel l = new JLabel("<html><div style='width:245px'>"
                + "<span style='font-size:11.5pt'>" + escapar(texto) + "</span><br>"
                + "<span style='color:#808080;font-size:9.5pt'>" + escapar(sub == null ? "" : sub)
                + "</span></div></html>");
        l.setForeground(TXT);
        f.add(l, BorderLayout.CENTER);

        f.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e){ f.setBackground(HOVER); }
            @Override public void mouseExited (MouseEvent e){ f.setBackground(PANEL); }
            @Override public void mouseClicked(MouseEvent e){ accion.run(); }
        });
        return f;
    }

    private static String escapar(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }




    private JMenuBar construirMenu() {
        JMenuBar barra = new JMenuBar();
        int at = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        JMenu mA = new JMenu("Archivo"); mA.setMnemonic('A');
        mA.add(item("Nuevo",    KeyEvent.VK_N, at, this::nuevo));
        mA.addSeparator();
        mA.add(item("Abrir…",   KeyEvent.VK_O, at, this::abrir));
        mA.add(item("Guardar…", KeyEvent.VK_S, at, this::guardar));
        mA.add(item("Carpeta del proyecto…", -1, 0, this::elegirCarpeta));
        mA.addSeparator();
        mA.add(item("Salir",    KeyEvent.VK_Q, at, () -> { resultado = null; dispose(); }));

        JMenu mP = new JMenu("Pantallas"); mP.setMnemonic('P');
        mP.add(item("1 · Inicio", -1, 0, () -> ir("inicio")));
        mP.add(item("2 · Geometría y lugar", -1, 0, () -> ir("estudio")));

        JMenu mV = new JMenu("Ver"); mV.setMnemonic('V');
        miGirar = new JCheckBoxMenuItem("Girar figuras", true);
        miGirar.addActionListener(e -> { if (miGirar.isSelected()) animador.start(); else animador.stop(); });
        mV.add(miGirar);
        mV.add(item("Volver a la vista inicial", -1, 0, () -> {
            tarjNervada.angulo = 0; tarjLisa.angulo = 0;
            tarjNervada.repaint(); tarjLisa.repaint();
        }));
        mV.addSeparator();
        mV.add(item("Mapa: ver todo el país", -1, 0, mapa::ajustar));
        mV.add(item("Mapa: acercar al municipio", -1, 0,
                () -> { if (lugar != null) mapa.acercarA(lugar, 12); }));

        JMenu mY = new JMenu("Ayuda"); mY.setMnemonic('y');
        mY.add(item("Pasos de uso…", KeyEvent.VK_F1, 0, this::mostrarPasos));
        mY.addSeparator();
        mY.add(item("Acerca de SDIM…", -1, 0, () ->
                JOptionPane.showMessageDialog(this,
                    "SDIM\n\nDosificación de baldosín a partir de la\n" +
                    "clasificación física del suelo (granulometría, límites de\n" +
                    "Atterberg e hidrómetro).\n\n" +
                    "Geometría: nervada por una cara.\n" +
                    "Municipios: DIVIPOLA — marco geoestadístico DANE 2018 (1.122).",
                    "Acerca de", JOptionPane.INFORMATION_MESSAGE)));

        barra.add(mA); barra.add(mP); barra.add(mV); barra.add(mY);
        return barra;
    }

    private void mostrarPasos() {
        final String[][] pasos = {
            {"1 · Inicio",
             "Elige la geometría (nervada por una cara) y el municipio de estudio: haz clic en el mapa o búscalo en la lista. También puedes cambiar el tema claro u oscuro con el botón de sol/luna."},
            {"2 · Clasificar el suelo",
             "Se hacen tres ensayos, en orden: ① Granulometría (pesos retenidos en cada tamiz → curva granulométrica), ② Hidrómetro (lecturas en el tiempo → % de arena, limo y arcilla en el triángulo textural) y ③ Límites de Atterberg (LL y LP → punto en la carta de plasticidad). Con los tres, SDIM entrega la clasificación SUCS final (por ejemplo CL)."},
            {"3 · Geometría de la pieza (3D)",
             "Escribe ancho, largo, espesor TOTAL y número de nervios, y pulsa «Generar». El espesor se reparte en base (2/3) y nervio (1/3). Estas dimensiones son la única fuente: las usan la dosificación y los ensayos."},
            {"4 · Diseño de mezcla",
             "El volumen viene de la pieza. La DENSIDAD (1,85 g/cm³ por defecto) se puede cambiar por la medida en laboratorio. Las 3 mezclas (M1, M2, M3) son una dosificación SUGERIDA según el suelo (USCS, IP, LP, finos): puedes cambiar los % de relave, caolín, feldespato y agua (Σ sólidos = 100 %). Pulsa «Calcular 3 mezclas» para obtener los gramos y el agua."},
            {"5 · Secado y cocción",
             "Revisa la curva de secado (días hasta estabilizar la humedad) y la curva de cocción con pico de 1100 °C, que el programa genera para cada mezcla."},
            {"6 · Ensayos (NTC 919 / ISO 13006)",
             "Para cada mezcla (empieza por M1): elige prensado o extruido y el número de probetas. Llena absorción y contracción con los valores MEDIDOS. En flexión, «Ensayo a flexión» abre otra ventana: escribe la luz (lo demás ya viene lleno), carga el video de la prensa, la curva se dibuja sola y pulsa «Continuar». El programa compara todo con la norma."},
            {"7 · Informe final",
             "Al terminar los ensayos aparece el resumen con el USO RECOMENDADO de la baldosa. Pulsa «Descargar informe final (PDF)»: el informe reúne lugar, clasificación, dosificación, secciones, secado, cocción, ensayos vs. NTC 919 y uso recomendado."},
            {"8 · Guardar y abrir",
             "Guarda el estudio con el botón «Guardar» (disquete) de la barra superior o con Ctrl+S (archivo .gcl). Al abrirlo se restauran el lugar, la clasificación, las mezclas y los ensayos."}
        };
        final int[] i = {0};
        final double[] fase = {0};

        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(PANEL);
        panel.setBorder(new EmptyBorder(16, 18, 14, 18));
        panel.setPreferredSize(new Dimension(728, 560));

        // Paso 2: simulacion tipo video con las graficas REALES del programa (pre-renderizadas)
        final SimulacionClasificacion sim = new SimulacionClasificacion();
        final SimulacionPieza simPieza = new SimulacionPieza();
        final SimulacionMezcla simMezcla = new SimulacionMezcla();
        final SimulacionSecado simSecado = new SimulacionSecado();
        final SimulacionEnsayos simEnsayos = new SimulacionEnsayos();
        final SimulacionInforme simInforme = new SimulacionInforme();
        final SimulacionGuardar simGuardar = new SimulacionGuardar();
        final long[] t0 = {System.currentTimeMillis()};

        JPanel ilu = new JPanel() {
            @Override protected void paintComponent(Graphics g0) {
                super.paintComponent(g0);
                Graphics2D g = (Graphics2D) g0.create();
                if (i[0] == 1) {   // Clasificacion: simulacion de los 3 ensayos + resultado
                    sim.pintar(g, System.currentTimeMillis() - t0[0], getWidth(), getHeight());
                } else if (i[0] == 2) {   // Pieza: se escriben las medidas, «Generar», 3D y secciones
                    simPieza.pintar(g, System.currentTimeMillis() - t0[0], getWidth(), getHeight());
                } else if (i[0] == 3) {   // Mezcla: densidad editable, dosificacion sugerida editable, calcular
                    simMezcla.pintar(g, System.currentTimeMillis() - t0[0], getWidth(), getHeight());
                } else if (i[0] == 4) {   // Secado y coccion: las curvas se dibujan solas
                    simSecado.pintar(g, System.currentTimeMillis() - t0[0], getWidth(), getHeight());
                } else if (i[0] == 5) {   // Ensayos NTC 919: probetas, absorcion, contraccion y flexion con video
                    simEnsayos.pintar(g, System.currentTimeMillis() - t0[0], getWidth(), getHeight());
                } else if (i[0] == 6) {   // Informe: «Descargar informe final (PDF)» y se hojea el informe
                    simInforme.pintar(g, System.currentTimeMillis() - t0[0], getWidth(), getHeight());
                } else if (i[0] == 7) {   // Guardar: boton del disquete arriba
                    simGuardar.pintar(g, System.currentTimeMillis() - t0[0], getWidth(), getHeight());
                } else {
                    pintarPaso(g, i[0], fase[0], getWidth(), getHeight());
                }
                g.dispose();
            }
        };
        ilu.setBackground(Tema.SUPERFICIE);
        ilu.setPreferredSize(new Dimension(696, 280));
        ilu.setBorder(BorderFactory.createLineBorder(Tema.BORDE));

        final javax.swing.Timer timer = new javax.swing.Timer(40, e -> {
            fase[0] += 0.09;
            if (fase[0] > Math.PI * 2) fase[0] -= Math.PI * 2;
            ilu.repaint();
        });

        JLabel titulo = new JLabel();
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 15f));
        titulo.setForeground(ACENTO);
        JLabel progreso = new JLabel();
        progreso.setForeground(TXT2);
        progreso.setFont(progreso.getFont().deriveFont(11f));

        JTextArea desc = new JTextArea();
        desc.setEditable(false); desc.setLineWrap(true); desc.setWrapStyleWord(true);
        desc.setOpaque(false); desc.setForeground(TXT);
        desc.setFont(desc.getFont().deriveFont(13f));

        JButton ant = new JButton("◀ Anterior");
        JButton sig = new JButton("Siguiente ▶");
        JButton cerrar = new JButton("Cerrar");

        Runnable upd = () -> {
            t0[0] = System.currentTimeMillis();   // cada paso arranca su animacion desde el inicio
            titulo.setText(pasos[i[0]][0]);
            desc.setText(pasos[i[0]][1]);
            progreso.setText("Paso " + (i[0] + 1) + " de " + pasos.length);
            ant.setEnabled(i[0] > 0);
            sig.setEnabled(i[0] < pasos.length - 1);
        };
        ant.addActionListener(e -> { if (i[0] > 0) { i[0]--; upd.run(); } });
        sig.addActionListener(e -> { if (i[0] < pasos.length - 1) { i[0]++; upd.run(); } });

        JPanel norte = new JPanel(new BorderLayout());
        norte.setOpaque(false);
        norte.add(titulo, BorderLayout.WEST);
        norte.add(progreso, BorderLayout.EAST);

        JScrollPane sc = new JScrollPane(desc);
        sc.setBorder(null); sc.setOpaque(false); sc.getViewport().setOpaque(false);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        botones.add(ant); botones.add(sig); botones.add(cerrar);

        JPanel centro = new JPanel(new BorderLayout(0, 10));
        centro.setOpaque(false);
        centro.add(norte, BorderLayout.NORTH);
        centro.add(sc, BorderLayout.CENTER);

        panel.add(ilu, BorderLayout.NORTH);
        panel.add(centro, BorderLayout.CENTER);
        panel.add(botones, BorderLayout.SOUTH);
        upd.run();

        final JDialog d = new JDialog(this, "Pasos de uso de SDIM", true);
        d.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) { timer.stop(); }
        });
        cerrar.addActionListener(e -> d.dispose());
        d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        d.setContentPane(panel);
        d.pack();
        d.setLocationRelativeTo(this);
        timer.start();
        d.setVisible(true);
    }

    static Color aa(Color c, int al) { return new Color(c.getRed(), c.getGreen(), c.getBlue(), al); }

    // Guia animada "tipo video": recrea la interfaz de cada paso (ph = fase 0..2pi que se repite)
    static void pintarPaso(Graphics2D g, int paso, double ph, int w, int h) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double t = ph / (2 * Math.PI);
        Color az = Tema.ACENTO, az2 = Tema.ACENTO_CLARO, gris = Tema.BORDE, verde = Tema.OK;
        Color mut = Tema.TEXTO_2, txt = Tema.TEXTO, sup2 = Tema.SUPERFICIE_2, rojo = Tema.ERROR, amb = Tema.AVISO;
        Color sup = Tema.SUPERFICIE;
        Font f10 = Tema.ui(Font.PLAIN, 10), f11b = Tema.ui(Font.BOLD, 11);
        int P = 12;

        switch (paso) {
            case 0: {   // Lugar: lista real que se filtra al escribir + mapa real de Colombia
                g.setColor(mut); g.setFont(f11b); g.drawString("Lugar de estudio", P, 16);
                int sx = P, sy = 24, sw = 180, sh = 22;
                boolean escribiendo = t < 0.5;
                String full = "Vetas";
                int nch = (int) Math.round(Math.min(1.0, t / 0.4) * full.length());
                nch = Math.max(0, Math.min(full.length(), nch));
                String typed = full.substring(0, nch);
                g.setColor(sup2); g.fillRoundRect(sx, sy, sw, sh, 6, 6);
                g.setColor(escribiendo ? az : gris);
                g.setStroke(new BasicStroke(escribiendo ? 2f : 1f));
                g.drawRoundRect(sx, sy, sw, sh, 6, 6);
                g.setFont(f11b);
                if (typed.isEmpty()) { g.setColor(mut); g.drawString("Buscar municipio…", sx + 8, sy + 15); }
                else { g.setColor(txt); g.drawString(typed, sx + 8, sy + 15); }
                if (escribiendo && ((int) (ph * 4) % 2 == 0)) {
                    int cxp = sx + 8 + (typed.isEmpty() ? 0 : g.getFontMetrics().stringWidth(typed));
                    g.setColor(az); g.fillRect(cxp + 1, sy + 5, 1, 12);
                }
                // lista real filtrada por lo escrito
                java.util.List<geoceramiclab.model.MunicipiosCO.Municipio> todos = geoceramiclab.model.MunicipiosCO.todos();
                String q = typed.toLowerCase(java.util.Locale.ROOT);
                int ly = sy + sh + 8, rh = 21, shown = 0;
                for (geoceramiclab.model.MunicipiosCO.Municipio m : todos) {
                    if (shown >= 5) break;
                    if (!q.isEmpty() && !m.nombre.toLowerCase(java.util.Locale.ROOT).startsWith(q)) continue;
                    int ry = ly + shown * rh;
                    boolean sel = m.nombre.equalsIgnoreCase("Vetas");
                    if (sel) { g.setColor(aa(az, 45)); g.fillRoundRect(sx, ry, sw, rh - 2, 4, 4); }
                    g.setColor(txt); g.setFont(f11b); g.drawString(m.nombre, sx + 6, ry + 10);
                    g.setColor(mut); g.setFont(f10);
                    g.drawString(m.departamento + " · " + m.codigoDane, sx + 6, ry + 19);
                    shown++;
                }
                // mapa real de Colombia: puntos de los municipios (lat/lon)
                int mx = sx + sw + 16, my = 24, mw = w - mx - P, mh = h - 34;
                g.setColor(sup2); g.fillRoundRect(mx, my, mw, mh, 8, 8);
                g.setColor(gris); g.drawRoundRect(mx, my, mw, mh, 8, 8);
                g.setColor(mut); g.setFont(f10); g.drawString("Mapa de Colombia", mx + 8, my + 12);
                double lonMin = -79.1, lonMax = -66.8, latMin = -4.3, latMax = 13.6;
                int pd = 12, px0 = mx + pd, py0 = my + 16, pw = mw - 2 * pd, phh = mh - pd - 16;
                // Proyeccion con proporcion real (Colombia es mas alta que ancha): misma escala lon/lat
                double cosLat = Math.cos(Math.toRadians((latMin + latMax) / 2.0));
                double lonSpan = (lonMax - lonMin) * cosLat, latSpan = latMax - latMin;
                double esc = Math.min(pw / lonSpan, phh / latSpan);
                double ox = px0 + (pw - lonSpan * esc) / 2.0, oy = py0 + (phh - latSpan * esc) / 2.0;
                // contornos reales (departamentos): relleno suave + linea -> se ve como un mapa
                for (double[][] anillo : geoceramiclab.model.MunicipiosCO.contornos()) {
                    if (anillo == null || anillo.length < 2) continue;
                    java.awt.geom.Path2D.Double ru = new java.awt.geom.Path2D.Double();
                    for (int i = 0; i < anillo.length; i++) {
                        double X = ox + (anillo[i][0] - lonMin) * cosLat * esc;
                        double Y = oy + (latMax - anillo[i][1]) * esc;
                        if (i == 0) ru.moveTo(X, Y); else ru.lineTo(X, Y);
                    }
                    ru.closePath();
                    g.setColor(aa(az, 22)); g.fill(ru);
                    g.setColor(aa(az, 150)); g.setStroke(new BasicStroke(0.7f)); g.draw(ru);
                }
                g.setColor(aa(az, 120));
                for (geoceramiclab.model.MunicipiosCO.Municipio m : todos) {
                    int X = (int) (ox + (m.lon - lonMin) * cosLat * esc);
                    int Y = (int) (oy + (latMax - m.lat) * esc);
                    g.fillOval(X, Y, 1, 1);
                }
                int px = (int) (ox + (-72.8816 - lonMin) * cosLat * esc);
                int py = (int) (oy + (latMax - 7.3222) * esc);
                if (t > 0.45) {
                    g.setColor(az); g.fillOval(px - 5, py - 20, 10, 10);
                    java.awt.geom.Path2D.Double pin = new java.awt.geom.Path2D.Double();
                    pin.moveTo(px - 4, py - 12); pin.lineTo(px, py); pin.lineTo(px + 4, py - 12); pin.closePath();
                    g.fill(pin);
                    g.setColor(sup); g.fillOval(px - 2, py - 18, 4, 4);
                    double rr = 8 + 6 * (0.5 + 0.5 * Math.sin(ph * 2));
                    g.setColor(aa(az, 150)); g.setStroke(new BasicStroke(2f));
                    g.drawOval(px - (int) rr, py - 8 - (int) rr, (int) (2 * rr), (int) (2 * rr));
                }
                break;
            }
            case 1: {   // Clasificacion: 3 graficas + resultado USCS
                int cw = (w - 2 * P - 2 * 8) / 3, cy = 22, chh = h - 56;
                String[] tit = {"Granulometría", "Plasticidad", "Textural"};
                for (int c = 0; c < 3; c++) {
                    int cx = P + c * (cw + 8);
                    g.setColor(sup2); g.fillRoundRect(cx, cy, cw, chh, 6, 6);
                    g.setColor(gris); g.drawRoundRect(cx, cy, cw, chh, 6, 6);
                    g.setColor(mut); g.setFont(f10); g.drawString(tit[c], cx + 6, cy + 12);
                    int gx = cx + 8, gy = cy + 18, gw = cw - 16, gh = chh - 26;
                    g.setColor(aa(gris, 160)); g.drawLine(gx, gy + gh, gx + gw, gy + gh); g.drawLine(gx, gy, gx, gy + gh);
                    g.setColor(az); g.setStroke(new BasicStroke(2f));
                    int nn = (int) (Math.min(1, t / 0.7) * 24);
                    if (c == 0) {
                        java.awt.geom.Path2D.Double cv = new java.awt.geom.Path2D.Double();
                        for (int k = 0; k <= nn; k++) { double x = k / 24.0; double y = 1 / (1 + Math.exp(-(x - 0.5) * 9)); double X = gx + x * gw, Y = gy + gh - y * gh; if (k == 0) cv.moveTo(X, Y); else cv.lineTo(X, Y); }
                        g.draw(cv);
                    } else if (c == 1) {
                        g.setColor(aa(gris, 200)); g.drawLine(gx, gy + gh, gx + gw, gy + 2);
                        if (t > 0.3) { g.setColor(az); g.fillOval(gx + gw * 5 / 10 - 3, gy + gh * 5 / 10 - 3, 6, 6); }
                    } else {
                        java.awt.geom.Path2D.Double tr = new java.awt.geom.Path2D.Double();
                        tr.moveTo(gx + gw / 2.0, gy); tr.lineTo(gx, gy + gh); tr.lineTo(gx + gw, gy + gh); tr.closePath();
                        g.setColor(aa(gris, 200)); g.draw(tr);
                        if (t > 0.3) { g.setColor(az); g.fillOval(gx + gw / 2 - 3, gy + gh * 6 / 10 - 3, 6, 6); }
                    }
                }
                if (t > 0.6) {
                    String r = "CL · arcilla de baja plasticidad";
                    g.setFont(f11b);
                    int tw = g.getFontMetrics().stringWidth(r) + 16;
                    int bx = (w - tw) / 2, by = h - 24;
                    g.setColor(aa(verde, 45)); g.fillRoundRect(bx, by, tw, 18, 9, 9);
                    g.setColor(verde); g.drawString(r, bx + 8, by + 13);
                }
                break;
            }
            case 2: {   // Pieza nervada que se balancea suavemente
                double ang = 0.5 + 0.06 * Math.sin(ph);
                int cx = w / 2, cyc = h / 2;
                double ex = 70 * Math.cos(ang), ey = 70 * Math.sin(ang) * 0.5;
                double fx2 = -38 * Math.cos(ang), fy2 = 38 * Math.sin(ang) * 0.5;
                double ax = cx - (ex + fx2) / 2, ay = cyc - (ey + fy2) / 2 - 6;
                java.awt.geom.Path2D.Double top = new java.awt.geom.Path2D.Double();
                top.moveTo(ax, ay); top.lineTo(ax + ex, ay + ey); top.lineTo(ax + ex + fx2, ay + ey + fy2); top.lineTo(ax + fx2, ay + fy2); top.closePath();
                g.setColor(aa(az, 28)); g.fill(top);
                g.setColor(az); g.setStroke(new BasicStroke(1.6f)); g.draw(top);
                int dz = 13;
                g.drawLine((int) (ax + ex), (int) (ay + ey), (int) (ax + ex), (int) (ay + ey + dz));
                g.drawLine((int) (ax + ex + fx2), (int) (ay + ey + fy2), (int) (ax + ex + fx2), (int) (ay + ey + fy2 + dz));
                g.drawLine((int) (ax + fx2), (int) (ay + fy2), (int) (ax + fx2), (int) (ay + fy2 + dz));
                g.drawLine((int) (ax + ex), (int) (ay + ey + dz), (int) (ax + ex + fx2), (int) (ay + ey + fy2 + dz));
                g.drawLine((int) (ax + fx2), (int) (ay + fy2 + dz), (int) (ax + ex + fx2), (int) (ay + ey + fy2 + dz));
                int N = 6, hi = (int) (t * N) % N;
                for (int k = 1; k <= N; k++) {
                    double fr = k / (double) (N + 1);
                    double x1 = ax + fr * ex, y1 = ay + fr * ey, x2 = ax + fx2 + fr * ex, y2 = ay + fy2 + fr * ey;
                    g.setColor(k - 1 == hi ? az2 : az);
                    g.setStroke(new BasicStroke(k - 1 == hi ? 4.5f : 2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
                }
                g.setColor(mut); g.setFont(f10); g.drawString("Modelo 3D · nervada por una cara", P, h - 8);
                break;
            }
            case 3: {   // Dosificacion: tabla con numeros que suben
                g.setColor(mut); g.setFont(f11b);
                g.drawString("Dosificación   ρ = 1,85 g/cm³   mₛ = 108,8 g", P, 16);
                String[] col = {"Mezcla", "Relave", "Caolín", "Feld.", "Agua"};
                int[] cw = {68, 50, 50, 44, 44};
                int x0 = P, y0 = 26, rh = 24, tot = 68 + 50 + 50 + 44 + 44;
                int xx = x0; g.setFont(f10);
                g.setColor(aa(az, 30)); g.fillRect(x0, y0, tot, 18);
                g.setColor(mut);
                for (int c = 0; c < 5; c++) { g.drawString(col[c], xx + 4, y0 + 13); xx += cw[c]; }
                String[] mez = {"M1", "M2", "M3"};
                double[][] val = {{70.7, 27.2, 10.9, 19.6}, {59.8, 32.6, 16.3, 22.8}, {81.6, 21.8, 5.4, 16.3}};
                for (int r = 0; r < 3; r++) {
                    int ry = y0 + 18 + r * rh; xx = x0;
                    g.setColor(txt); g.setFont(f11b); g.drawString(mez[r], xx + 4, ry + 15); xx += cw[0];
                    g.setFont(f10);
                    for (int c = 0; c < 4; c++) {
                        double v = val[r][c] * Math.min(1, t / 0.8);
                        g.setColor(c == 0 ? az : (c == 3 ? az2 : txt));
                        g.drawString(String.format(java.util.Locale.US, "%.1f", v), xx + 4, ry + 15);
                        xx += cw[c + 1];
                    }
                    g.setColor(aa(gris, 120)); g.drawLine(x0, ry + rh - 2, x0 + tot, ry + rh - 2);
                }
                break;
            }
            case 4: {   // Secado y coccion: dos curvas dibujandose
                int cw = (w - 2 * P - 10) / 2, cy = 24, chh = h - 44;
                int sx = P;
                g.setColor(sup2); g.fillRoundRect(sx, cy, cw, chh, 6, 6); g.setColor(gris); g.drawRoundRect(sx, cy, cw, chh, 6, 6);
                g.setColor(mut); g.setFont(f10); g.drawString("Secado", sx + 6, cy + 12);
                int gx = sx + 8, gy = cy + 16, gw = cw - 16, gh = chh - 24;
                int nn = (int) (Math.min(1, t) * 30);
                g.setColor(az); g.setStroke(new BasicStroke(2f));
                java.awt.geom.Path2D.Double c1 = new java.awt.geom.Path2D.Double();
                for (int k = 0; k <= nn; k++) { double x = k / 30.0; double y = Math.exp(-x * 2.3); double X = gx + x * gw, Y = gy + gh - y * gh; if (k == 0) c1.moveTo(X, Y); else c1.lineTo(X, Y); }
                g.draw(c1);
                int fx3 = sx + cw + 10;
                g.setColor(sup2); g.fillRoundRect(fx3, cy, cw, chh, 6, 6); g.setColor(gris); g.drawRoundRect(fx3, cy, cw, chh, 6, 6);
                g.setColor(mut); g.drawString("Cocción · 1100 °C", fx3 + 6, cy + 12);
                int gx2 = fx3 + 8;
                java.awt.geom.Path2D.Double c2 = new java.awt.geom.Path2D.Double();
                for (int k = 0; k <= nn; k++) { double x = k / 30.0; double y = Math.sin(x * Math.PI * 0.95); double X = gx2 + x * gw, Y = gy + gh - y * gh; if (k == 0) c2.moveTo(X, Y); else c2.lineTo(X, Y); }
                g.setColor(az); g.draw(c2);
                int pkx = gx2 + (int) (0.5 * gw), pky = gy;
                int gl = 6 + (int) (3 * (0.5 + 0.5 * Math.sin(ph * 2)));
                g.setColor(aa(new Color(224, 120, 60), 120)); g.fillOval(pkx - gl, pky - gl + 4, 2 * gl, 2 * gl);
                break;
            }
            case 5: {   // Ensayos: barras que crecen + cumple/no
                g.setColor(mut); g.setFont(f11b); g.drawString("Ensayos · resistencia a la flexión (MPa)", P, 16);
                String[] mez = {"M1", "M2", "M3"}; double[] val = {12.0, 15.5, 9.5}; double lim = 13;
                int bw = 40, gap = 28, x0 = (w - (3 * bw + 2 * gap)) / 2, y0 = h - 22, maxh = h - 52;
                int yl = y0 - (int) (lim / 18.0 * maxh);
                g.setColor(aa(gris, 160)); g.drawLine(x0 - 10, yl, x0 + 3 * bw + 2 * gap + 10, yl);
                g.setFont(f10); g.setColor(mut); g.drawString("mín", x0 - 10, yl - 2);
                for (int b = 0; b < 3; b++) {
                    int x = x0 + b * (bw + gap); double v = val[b] * Math.min(1, t / 0.8); int bh = (int) (v / 18.0 * maxh);
                    boolean ok = val[b] >= lim;
                    g.setColor(ok ? verde : amb); g.fillRoundRect(x, y0 - bh, bw, bh, 4, 4);
                    g.setColor(txt); g.setFont(f10); g.drawString(mez[b], x + 12, y0 + 12);
                    if (t > 0.8) { g.setColor(ok ? verde : amb); g.setFont(f11b); g.drawString(ok ? "✓" : "!", x + bw / 2 - 3, y0 - bh - 4); }
                }
                break;
            }
            case 6: {   // Informe: pagina con secciones apareciendo + PDF
                int dx = w / 2 - 60, dy = 14, dw = 120, dh = h - 28;
                g.setColor(sup); g.fillRoundRect(dx, dy, dw, dh, 6, 6); g.setColor(gris); g.drawRoundRect(dx, dy, dw, dh, 6, 6);
                g.setColor(az); g.fillRect(dx, dy, dw, 5);
                int nl = 7, shown = (int) (Math.min(1, t) * nl);
                for (int l = 0; l < shown; l++) {
                    int ly = dy + 16 + l * 14;
                    int lw = (l % 3 == 0) ? dw - 20 : dw - 24 - (l * 7) % 36;
                    g.setColor(l == 0 ? az : aa(mut, 160));
                    g.fillRoundRect(dx + 10, ly, Math.max(20, lw), l == 0 ? 6 : 4, 2, 2);
                }
                g.setColor(rojo); g.fillRoundRect(dx + dw - 34, dy + dh - 16, 30, 13, 3, 3);
                g.setColor(sup); g.setFont(f10); g.drawString("PDF", dx + dw - 31, dy + dh - 6);
                break;
            }
            default: {  // 7 Guardar: barra con disquete + aviso
                g.setColor(sup2); g.fillRoundRect(P, 14, w - 2 * P, 26, 6, 6);
                g.setColor(gris); g.drawRoundRect(P, 14, w - 2 * P, 26, 6, 6);
                int bx = P + 10, by = 18; int ex = (int) (2 * (0.5 + 0.5 * Math.sin(ph * 2)));
                g.setColor(az); g.fillRoundRect(bx - ex, by - ex, 18 + 2 * ex, 18 + 2 * ex, 4, 4);
                g.setColor(sup); g.fillRect(bx + 3, by + 2, 10, 5); g.fillRoundRect(bx + 3, by + 9, 12, 6, 2, 2);
                g.setColor(mut); g.setFont(f10); g.drawString("Ctrl + S", bx + 28, by + 13);
                if (t > 0.4) {
                    double st = Math.min(1, (t - 0.4) / 0.3); int tw = 130; int tx = (int) (w - 10 - tw * st); int ty = h - 30;
                    g.setColor(aa(verde, 235)); g.fillRoundRect(tx, ty, tw, 22, 8, 8);
                    g.setColor(sup); g.setFont(f11b); g.drawString("✓ Estudio guardado", tx + 10, ty + 15);
                }
                break;
            }
        }
    }

    private JMenuItem item(String txt, int tecla, int mod, Runnable acc) {
        JMenuItem mi = new JMenuItem(txt);
        if (tecla > 0) mi.setAccelerator(KeyStroke.getKeyStroke(tecla, mod));
        mi.addActionListener(e -> acc.run());
        return mi;
    }

    private void nuevo() {
        geoceramiclab.Proyecto.archivo = null;
        ancho = ANCHO_DEF; largo = LARGO_DEF;
        seleccionar(TipoCara.NERVADA_UNA);
        lugar = null;
        mapa.setSeleccionado(null);
        mapa.setDepartamento(null);
        mapa.ajustar();
        sincronizando = true;
        txtBuscar.setText("");
        cboDepto.setSelectedItem("Todos");
        sincronizando = false;
        filtrar();
        actualizarLugar();
        ir("inicio");
    }

    private void elegirCarpeta() {
        JFileChooser fc = new JFileChooser(carpeta.getParentFile());
        fc.setDialogTitle("Carpeta donde se guardan los proyectos");
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        carpeta = fc.getSelectedFile();
        PREFS.put(K_CARPETA, carpeta.getAbsolutePath());
        refrescarRecientes();
    }

    private void guardar() {
        if (!carpeta.exists() && !carpeta.mkdirs()) {
            JOptionPane.showMessageDialog(this, "No se pudo crear la carpeta:\n" + carpeta,
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JFileChooser fc = new JFileChooser(carpeta);
        fc.setDialogTitle("Guardar estudio");
        fc.setFileFilter(new FileNameExtensionFilter("SDIM (*." + EXT + ")", EXT));
        fc.setSelectedFile(new File(carpeta, "estudio." + EXT));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File f = fc.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith("." + EXT)) {
            f = new File(f.getParentFile(), f.getName() + "." + EXT);
        }

        Properties p = new Properties();
        if (f.isFile()) try { p = geoceramiclab.Proyecto.leer(f); } catch (IOException ignorar) { }
        p.setProperty("tipo",  seleccion.name());
        p.setProperty("ancho", String.valueOf(ancho));
        p.setProperty("largo", String.valueOf(largo));
        p.setProperty("municipio", lugar == null ? "" : lugar.codigoDane);
        try (OutputStream os = new FileOutputStream(f)) {
            p.store(os, "SDIM - estudio");
            geoceramiclab.Proyecto.archivo = f;
            anotarReciente(f);
            JOptionPane.showMessageDialog(this, "Guardado en:\n" + f.getAbsolutePath(),
                    "Guardar", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo guardar:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void abrir() {
        JFileChooser fc = new JFileChooser(carpeta);
        fc.setDialogTitle("Abrir estudio");
        fc.setFileFilter(new FileNameExtensionFilter("SDIM (*." + EXT + ")", EXT));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) cargar(fc.getSelectedFile());
    }

    private void cargar(File f) {
        Properties p = new Properties();
        try (InputStream is = new FileInputStream(f)) {
            p.load(is);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo abrir:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            seleccionar(TipoCara.valueOf(p.getProperty("tipo", "NERVADA_UNA")));
        } catch (IllegalArgumentException ignored) { }
        try {
            ancho = Double.parseDouble(p.getProperty("ancho", String.valueOf(ANCHO_DEF)));
            largo = Double.parseDouble(p.getProperty("largo", String.valueOf(LARGO_DEF)));
        } catch (NumberFormatException ignored) { }
        refrescarGeometria();

        String cod = p.getProperty("municipio", "");
        Municipio m = cod.isEmpty() ? null : MunicipiosCO.porCodigo(cod);
        if (m != null) fijarLugar(m, true); else actualizarLugar();

        geoceramiclab.Proyecto.archivo = f;
        anotarReciente(f);
        ir("estudio");
    }

    private void refrescarRecientes() {
        lblCarpeta.setText(carpeta.getAbsolutePath());
        lblCarpeta.setToolTipText(carpeta.getAbsolutePath());
        panelRecientes.removeAll();
        List<String> rs = recientes();
        if (rs.isEmpty()) {
            JLabel v = new JLabel("Sin archivos todavía");
            v.setFont(v.getFont().deriveFont(Font.ITALIC, 12f));
            v.setForeground(TXT2);
            v.setAlignmentX(Component.LEFT_ALIGNMENT);
            v.setBorder(new EmptyBorder(6, 2, 0, 0));
            panelRecientes.add(v);
        } else {
            int i = 1;
            for (String r : rs) {
                final File f = new File(r);
                panelRecientes.add(fila(i++ + "  " + f.getName(), f.getParent(), "▫", () -> cargar(f)));
            }
        }
        panelRecientes.revalidate();
        panelRecientes.repaint();
    }

    private List<String> recientes() {
        List<String> out = new ArrayList<>();
        for (String s : PREFS.get(K_RECIENTES, "").split("\n")) {
            if (!s.trim().isEmpty() && new File(s).isFile()) out.add(s);
        }
        return out;
    }

    private void anotarReciente(File f) {
        List<String> rs = recientes();
        rs.remove(f.getAbsolutePath());
        rs.add(0, f.getAbsolutePath());
        while (rs.size() > 6) rs.remove(rs.size() - 1);
        PREFS.put(K_RECIENTES, String.join("\n", rs));
        refrescarRecientes();
    }

    private void seleccionar(TipoCara t) {
        seleccion = TipoCara.NERVADA_UNA;
        refrescarGeometria();
    }

    private void refrescarGeometria() {
        seleccion = TipoCara.NERVADA_UNA;
        tarjNervada.seleccionada = true;
        // Modelo de vista: un baldosin delgado, ancho y con pocos nervios (solo para el icono)
        BaldosinCatalan vista = new BaldosinCatalan(TipoCara.NERVADA_UNA);
        vista.ancho = 22; vista.largo = 26;
        vista.espesorNominal = 0.5;
        vista.numNerviosInt = 5;
        vista.calcularGeometria();
        tarjNervada.modelo = vista;
        tarjNervada.repaint();
    }

    private BaldosinCatalan construir(TipoCara tipo) {
        BaldosinCatalan b = new BaldosinCatalan(tipo);
        b.ancho = ancho; b.largo = largo;
        b.calcularGeometria();
        return b;
    }

    private void aceptar() {
        if (lugar == null) {
            JOptionPane.showMessageDialog(this,
                    "Elige el municipio de estudio en la lista o haz clic en el mapa.",
                    "Falta el lugar", JOptionPane.WARNING_MESSAGE);
            return;
        }
        soltarPdf();
        BaldosinCatalan b = construir(seleccion);
        BaldosinCatalan.setActual(b);
        MunicipiosCO.setActual(lugar);
        resultado = b;
        dispose();
    }

    private static class Tarjeta extends JPanel {
        final TipoCara tipo;
        final String titulo;
        BaldosinCatalan modelo;
        boolean seleccionada = false, hover = false;
        double angulo = 0;

        static final Color TRAZO     = Tema.TEXTO_2;
        static final Color TRAZO_SEL = Tema.ACENTO_CLARO;

        Tarjeta(TipoCara tipo, String titulo) {
            this.tipo = tipo; this.titulo = titulo;
            setOpaque(false);
        }

        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            int w = getWidth(), h = getHeight();
            Color fondo = seleccionada ? HOVER : (hover ? Tema.SUPERFICIE_2 : PANEL);

            g.setColor(fondo);
            g.fillRoundRect(0, 0, w-1, h-1, 12, 12);
            g.setStroke(new BasicStroke(seleccionada ? 2.4f : 1f));
            g.setColor(seleccionada ? ACENTO : LINEA);
            g.drawRoundRect(0, 0, w-1, h-1, 12, 12);

            if (modelo != null) {
                Graphics2D gf = (Graphics2D) g.create();
                gf.translate(12, 12);
                dibujar3D(gf, modelo, w - 24, h - 56, angulo,
                          fondo, seleccionada ? TRAZO_SEL : TRAZO);
                gf.dispose();
            }

            g.setColor(TXT);
            g.setFont(getFont().deriveFont(Font.BOLD, 13f));
            centrar(g, titulo, w, h - 24);

            g.setFont(getFont().deriveFont(seleccionada ? Font.BOLD : Font.PLAIN, 11f));
            g.setColor(seleccionada ? ACENTO : TXT2);
            centrar(g, seleccionada ? "✓ seleccionada" : "clic para elegir", w, h - 8);
            g.dispose();
        }

        private void centrar(Graphics2D g, String s, int w, int y) {
            g.drawString(s, (w - g.getFontMetrics().stringWidth(s)) / 2, y);
        }

        private static final double COS30 = 0.8660254;
        private static final double VE    = 8.0;

        private static class Cara implements Comparable<Cara> {
            final Path2D.Double p = new Path2D.Double();
            double prof;
            @Override public int compareTo(Cara o){ return Double.compare(prof, o.prof); }
        }

        private static double[] proy(double x, double y, double z, double th) {
            double c = Math.cos(th), s = Math.sin(th);
            double xr = x*c - y*s, yr = x*s + y*c;
            return new double[]{ (xr - yr) * COS30, (xr + yr) * 0.5 - z, xr + yr + z };
        }

        private static void caja(List<Cara> lista, double th, double ox, double oy, double s,
                                 double x0, double y0, double z0,
                                 double dx, double dy, double dz) {
            double[][] v = new double[8][];
            for (int iz = 0; iz < 2; iz++)
                for (int iy = 0; iy < 2; iy++)
                    for (int ix = 0; ix < 2; ix++)
                        v[ix + 2*iy + 4*iz] = new double[]{ x0+ix*dx, y0+iy*dy, z0+iz*dz };

            int[][] idx = { {0,1,3,2}, {4,5,7,6}, {0,1,5,4}, {2,3,7,6}, {0,2,6,4}, {1,3,7,5} };

            for (int f = 0; f < 6; f++) {
                Cara cara = new Cara();
                double px = 0, py = 0, pz = 0;
                for (int k = 0; k < 4; k++) {
                    double[] q = v[idx[f][k]];
                    double[] pr = proy(q[0], q[1], q[2], th);
                    double X = ox + pr[0]*s, Y = oy + pr[1]*s;
                    if (k == 0) cara.p.moveTo(X, Y); else cara.p.lineTo(X, Y);
                    px += q[0]; py += q[1]; pz += q[2];
                }
                cara.p.closePath();
                cara.prof = proy(px/4, py/4, pz/4, th)[2];
                lista.add(cara);
            }
        }

        private static void pintar(Graphics2D g, List<Cara> caras, Color fondo, Color trazo) {
            Collections.sort(caras);
            g.setStroke(new BasicStroke(1.15f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (Cara c : caras) {
                g.setColor(fondo);
                g.fill(c.p);
                g.setColor(trazo);
                g.draw(c.p);
            }
        }

        private static void dibujar3D(Graphics2D g, BaldosinCatalan b, int w, int h, double th,
                                      Color fondo, Color trazo) {
            if (w <= 0 || h <= 0) return;

            double W = b.ancho, L = b.largo;
            double t  = b.espesor * VE;
            double hn = b.esNervada() ? b.altoNervio * VE : 0.0;
            double zT = t + hn;

            double R = Math.sqrt(W*W + L*L) / 2.0;
            double s  = Math.min(w / (2 * Math.sqrt(2) * R * COS30),
                                 h / (Math.sqrt(2) * R + zT)) * 0.94;
            double ox = w / 2.0;
            double oy = h / 2.0 + zT * s / 2.0;
            double x0 = -W/2.0, y0 = -L/2.0;

            List<Cara> base = new ArrayList<>();
            caja(base, th, ox, oy, s, x0, y0, 0, W, L, t);
            pintar(g, base, fondo, trazo);

            if (!b.tieneNervios()) return;

            // Nervios anchos y bien repartidos: nervio = ranura, llenando el ancho
            int N = Math.max(1, b.numNerviosInt);
            double borde = W * 0.03;
            double libre = W - 2 * borde;
            double gap   = libre / (2 * N + 1);
            List<Cara> caras = new ArrayList<>();
            double xN = borde + gap;
            for (int i = 0; i < N; i++) {
                caja(caras, th, ox, oy, s, x0 + xN, y0, t, gap, L, hn);
                xN += 2 * gap;
            }
            pintar(g, caras, fondo, trazo);
        }
    }
}