package geoceramiclab.modelo3d;

import geoceramiclab.Tema;
import geoceramiclab.model.BaldosinCatalan;
import geoceramiclab.model.BaldosinCatalan.MezclaBaldosin;
import geoceramiclab.model.SueloData;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.io.File;
import java.net.URL;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

public class EnsayosNTC extends JPanel {

    private static final Color BG     = Tema.FONDO;
    private static final Color BG2    = Tema.SUPERFICIE;
    private static final Color BG3    = Tema.SUPERFICIE_2;
    private static final Color BORD   = Tema.BORDE;
    private static final Color ACCENT = Tema.INFO;
    private static final Color GREEN  = Tema.OK;
    private static final Color AMBER  = Tema.AVISO;
    private static final Color RED    = Tema.ERROR;
    private static final Color MUTED  = Tema.TEXTO_2;
    private static final Color DIM    = Tema.TEXTO_3;
    private static final Color TEXT   = Tema.TEXTO;

    private static final Color C_TIT  = Tema.ACENTO;
    private static final Color C_PROB = new Color(0x3E, 0x7C, 0xC4);
    private static final Color C_ABS  = new Color(0x3E, 0xA6, 0xB0);
    private static final Color C_CONT = new Color(0x6E, 0x7E, 0x9E);
    private static final Color C_DENS = new Color(0x5F, 0xAE, 0x8C);
    private static final Color C_FLEX = new Color(0x9C, 0x8B, 0xC9);
    private static final Color C_RES  = new Color(0x5A, 0x62, 0xB8);

    private static final int NODO_W  = 240;
    private static final int SPINE_X = 104;
    private static final int PILL_W  = 176;
    private static final int PILL_H  = 48;

    private final List<NodoMapa> nodos = new ArrayList<>();

    private static final int T_FLEXION     = 0;
    private static final int T_ABSORCION   = 1;
    private static final int T_CONTRACCION = 2;
    private static final int T_DENSIDAD    = 3;

    private static final double RHO_AGUA = 1.0;

    private static final String[] COLS_ABS = {
        "Probeta",
        "<html><center>M. ap. seca<br>(g)</center></html>",
        "<html><center>M. seca<br>(g)</center></html>",
        "<html><center>Absorción<br>Ev (%)</center></html>"
    };
    private static final String[] COLS_CONT = {
        "Probeta",
        "<html><center>L. verde<br>(mm)</center></html>",
        "<html><center>L. cocida<br>(mm)</center></html>",
        "<html><center>Contracción<br>(%)</center></html>"
    };
    private static final String[] COLS_DENS = {
        "Probeta",
        "<html><center>M. ap. seca<br>(g)</center></html>",
        "<html><center>M. seca<br>(g)</center></html>",
        "<html><center>M. sumergida<br>(g)</center></html>",
        "<html><center>Volumen<br>(cm³)</center></html>",
        "<html><center>Densidad<br>(g/cm³)</center></html>",
        "<html><center>Porosidad<br>ap. (%)</center></html>"
    };
    private static final String[] COLS_FLEX = {
        "Probeta",
        "<html><center>L cocida<br>(mm)</center></html>",
        "<html><center>Luz<br>(mm)</center></html>",
        "<html><center>Ancho<br>(mm)</center></html>",
        "<html><center>Espesor<br>(mm)</center></html>",
        "<html><center>Carga máx.<br>(N)</center></html>",
        "<html><center>S rotura<br>(N)</center></html>",
        "<html><center>R módulo<br>(MPa)</center></html>",
        "<html><center>NTC 919</center></html>"
    };

    private BaldosinCatalan baldosin;
    private SueloData datos;
    private int nProbetas = 0;
    private final List<Registro> registros = new ArrayList<>();
    private int idxMezcla = 0;
    private boolean actualizandoCombo = false;

    private final class Registro {
        String nombre;
        MezclaBaldosin mezcla;

        int n = 0;

        boolean cargado;

        final DefaultTableModel mAbs  = modelo(COLS_ABS,  1, 2);
        final DefaultTableModel mCont = modelo(COLS_CONT, 1, 2);
        final DefaultTableModel mDens = modelo(COLS_DENS, 3);
        final DefaultTableModel mFlex = modelo(COLS_FLEX);
        DetalleFlexion.ResultadoFlexion[] flex = new DetalleFlexion.ResultadoFlexion[0];

        boolean absOk, contOk, densOk;

        boolean absVig, contVig, densVig;

        double[] abs, cont, dens, poros;
        String[] grupo;

        Registro(String nombre, MezclaBaldosin m) {
            this.nombre = nombre;
            this.mezcla = m;
            mAbs.addTableModelListener(e -> onCambioAbs(this, e));
            mCont.addTableModelListener(e -> onCambioCont(this, e));
            mDens.addTableModelListener(e -> onCambioDens(this, e));
            redimensionar(0);
        }

        void redimensionar(int n) {
            ajustarFilas(mAbs,  n);
            ajustarFilas(mCont, n);
            ajustarFilas(mDens, n);
            ajustarFilas(mFlex, n);
            flex = Arrays.copyOf(flex, n);
            for (int i = 0; i < n; i++) {
                mDens.setValueAt(mAbs.getValueAt(i, 1), i, 1);
                mDens.setValueAt(mAbs.getValueAt(i, 2), i, 2);
            }
        }
    }

    private final JComboBox<String> cbMezcla = new JComboBox<>();
    private final JSpinner spProbetas = new JSpinner(new SpinnerNumberModel(5, 1, 50, 1));
    private final JLabel lblProbetas = new JLabel();

    private final JTable tblAbs  = new JTable();
    private final JTable tblCont = new JTable();
    private final JTable tblDens = new JTable();
    private final JScrollPane scAbs  = new JScrollPane(tblAbs);
    private final JScrollPane scCont = new JScrollPane(tblCont);
    private final JScrollPane scDens = new JScrollPane(tblDens);

    private final JLabel resAbs  = new JLabel();
    private final JLabel resCont = new JLabel();
    private final JLabel resDens = new JLabel();
    private final JLabel resFlex = new JLabel();

    private final JComboBox<String> cbProbFlex = new JComboBox<>();
    private final JTable tblFlex = new JTable();
    private final JScrollPane scFlex = new JScrollPane(tblFlex);
    private final JLabel resResumen = new JLabel();

    private final JPanel panelUso = new JPanel();
    private final JLabel lblUso = new JLabel();
    private final JPanel filaImgs = new JPanel(new GridLayout(1, 0, 12, 0));

    private static int anchoHtml = 600, anchoRes = 440;
    private final JButton btnInforme = new JButton("Descargar informe final (PDF)");

    private boolean flexCompleto, flexCumple;

    private final JPanel secAbs, secCont, secDens, secFlex, secRes;

    private final EnsayoImagen imgAbsorcion = new EnsayoImagen(T_ABSORCION);
    private final EnsayoImagen imgContrac   = new EnsayoImagen(T_CONTRACCION);
    private final EnsayoImagen imgDensidad  = new EnsayoImagen(T_DENSIDAD);
    private final EnsayoImagen imgFlexion   = new EnsayoImagen(T_FLEXION);

    public EnsayosNTC() {
        setLayout(new BorderLayout());
        setBackground(BG);

        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(Tema.BARRA);
        hdr.setBorder(BorderFactory.createMatteBorder(0,0,1,0,BORD));
        JLabel t = lbl("Ensayos NTC 919 / ISO 13006", TEXT, 15, true);
        t.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        JLabel h2 = lbl("Ingrese los datos de laboratorio de cada probeta", DIM, 10, false);
        h2.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        hdr.add(t, BorderLayout.WEST);
        hdr.add(h2, BorderLayout.EAST);
        add(hdr, BorderLayout.NORTH);

        LectorVideoEnsayo.EXTRUIDO = "extruido".equals(leerGuardados().getProperty("conformado"));
        registros.add(new Registro("Mezcla 1", null));

        JPanel cont = new AnchoVentana();
        cont.setLayout(new BoxLayout(cont, BoxLayout.Y_AXIS));
        cont.setBackground(BG);
        cont.setBorder(BorderFactory.createEmptyBorder(6,10,10,10));

        JPanel filaTit = new JPanel(new BorderLayout()) {
            @Override public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        filaTit.setOpaque(false);
        filaTit.setAlignmentX(Component.LEFT_ALIGNMENT);
        filaTit.add(new TituloMapa(), BorderLayout.WEST);
        JLabel guia = lbl("Siga el mapa de arriba hacia abajo · cada paso se habilita al calcular el anterior",
                DIM, 10, false);
        guia.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        filaTit.add(guia, BorderLayout.CENTER);
        cont.add(filaTit);

        cont.add(mkSeccion("Mezclas", "y probetas", 1, C_PROB,
                "①  Conformado, mezcla y probetas",
                "Los datos se guardan solos",
                null, buildProbetas()));

        JButton bAbs = mkBtn("Calcular absorción", GREEN);
        bAbs.addActionListener(e -> calcularAbsorcion());
        secAbs = mkSeccion("Absorción", "de agua", 2, C_ABS,
                "②  Absorción de agua (Ev)",
                "NTC 919 · ISO 10545-3 · Ev = (m aparentemente seca − m seca) / m seca × 100",
                imgAbsorcion, contenido(scAbs, bAbs, resAbs));
        cont.add(secAbs);

        JButton bCont = mkBtn("Calcular contracción", GREEN);
        bCont.addActionListener(e -> calcularContraccion());
        secCont = mkSeccion("Contracción", "lineal total", 3, C_CONT,
                "③  Contracción lineal total",
                "Ct = (L en verde − L cocida) / L en verde × 100 · misma marca de referencia en cada probeta",
                imgContrac, contenido(scCont, bCont, resCont));
        cont.add(secCont);

        JButton bDens = mkBtn("Calcular densidad", GREEN);
        bDens.addActionListener(e -> calcularDensidad());
        secDens = mkSeccion("Densidad", "aparente", 4, C_DENS,
                "④  Densidad aparente",
                "ISO 10545-3 (Arquímedes) · V = m ap. seca − m sumergida · ρ = m seca / V · "
              + "P = (m ap. seca − m seca) / V × 100",
                imgDensidad, contenido(scDens, bDens, resDens));

        secDens.setVisible(false);
        nodos.remove(nodos.size() - 1);

        secFlex = mkSeccion("Flexión", "3 puntos", 4, C_FLEX,
                "④  Resistencia a flexión (3 puntos)",
                "NTC 919 · ISO 10545-4",
                imgFlexion, buildFlexion());
        cont.add(secFlex);

        resResumen.setFont(Tema.ui(Font.PLAIN, 11));
        JPanel pRes = new JPanel(new BorderLayout());
        pRes.setOpaque(false);
        pRes.add(resResumen, BorderLayout.NORTH);
        pRes.add(buildUso(), BorderLayout.CENTER);
        secRes = mkSeccion("Resumen", "vs. NTC 919", 5, C_RES,
                "⑤  Resumen y comparación con la norma",
                "NTC 919 · ISO 13006 · ISO 10545-3 / -4 · valores medidos en laboratorio",
                null, pRes);
        cont.add(secRes);
        cont.add(Box.createVerticalStrut(8));

        JLabel ref = new JLabel("<html><font color='#7d8a97' style='font-size:9px'>"
            + "Ref: NTC 919 · ISO 13006 · ISO 10545-3 / -4 · "
            + "Las tablas aceptan pegar desde Excel (Ctrl+V).</font></html>");
        ref.setBorder(BorderFactory.createEmptyBorder(4,4,8,4));
        ref.setAlignmentX(Component.LEFT_ALIGNMENT);

        cont.add(Box.createVerticalGlue());

        for (JTable tb : new JTable[]{tblAbs, tblCont, tblDens}) habilitarPegado(tb);
        for (JScrollPane sc : new JScrollPane[]{scAbs, scCont, scDens, scFlex}) {
            sc.setBorder(BorderFactory.createLineBorder(BORD));
            sc.getViewport().setBackground(BG3);
            sc.setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        tblFlex.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int fila = tblFlex.rowAtPoint(e.getPoint());
                    if (fila >= 0) abrirFlexion(fila);
                }
            }
        });

        JScrollPane scroll = new JScrollPane(cont);
        scroll.setBackground(BG);
        scroll.getViewport().setBackground(BG);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
        scroll.getViewport().addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) {
                int w = scroll.getViewport().getWidth();
                int nh = Math.max(340, Math.min(900, w - NODO_W - 70));
                int nr = Math.max(240, Math.min(700, nh - 265));
                if (nh != anchoHtml || nr != anchoRes) {
                    anchoHtml = nh; anchoRes = nr;
                    SwingUtilities.invokeLater(() -> { ajustarTarjetas(); actualizarVista(); });
                }
            }
        });

        rellenarCombo();
        mostrarRegistro();
    }

    public boolean tieneMezcla() {
        for (Registro r : registros) if (r.mezcla != null) return true;
        return false;
    }

    public boolean ensayosCompletos() {
        boolean alguna = false;
        for (Registro r : registros) {
            if (r.mezcla == null) continue;
            if (!evaluar(r).completo) return false;
            alguna = true;
        }
        return alguna;
    }

    private static final class Eval {
        int k;
        boolean completo;
        double sProm, sMin, rProm, rMin, eProm;
        boolean normaCumple;
        int indivOk;
        String textoNorma = "";
        boolean cumpleTodo() { return normaCumple && indivOk == k && k > 0; }
    }

    private Eval evaluar(Registro r) {
        Eval e = new Eval();
        List<DetalleFlexion.ResultadoFlexion> hechos = new ArrayList<>();
        for (DetalleFlexion.ResultadoFlexion f : r.flex) if (f != null) hechos.add(f);
        e.k = hechos.size();
        double[] sv = new double[e.k], rv = new double[e.k], ev = new double[e.k];
        for (int i = 0; i < e.k; i++) {
            sv[i] = hechos.get(i).sN; rv[i] = hechos.get(i).rMPa; ev[i] = hechos.get(i).espesor;
            if (hechos.get(i).cumple) e.indivOk++;
        }
        if (e.k > 0) {
            double[] eS = estadisticas(sv), eR = estadisticas(rv), eE = estadisticas(ev);
            LectorVideoEnsayo.Evaluacion evn = LectorVideoEnsayo.evaluarNorma(eS[0], eR[0], eE[0],
                                                     r.abs != null ? r.abs[0] : 0);
            e.sProm = eS[0]; e.sMin = eS[2]; e.rProm = eR[0]; e.rMin = eR[2]; e.eProm = eE[0];
            e.normaCumple = evn.cumple;
            e.textoNorma = evn.texto;
        }
        e.completo = r.n > 0 && e.k == r.n && r.absOk && r.abs != null && r.contOk && r.cont != null;
        return e;
    }

    private String estadoMezclasHtml() {
        if (registros.size() < 2) return "";
        StringBuilder h = new StringBuilder("<font color='" + hx(DIM) + "'>&nbsp;&nbsp;·&nbsp;&nbsp;");
        for (int i = 0; i < registros.size(); i++) {
            Registro r = registros.get(i);
            String nm = r.nombre == null ? "M" + (i + 1) : r.nombre.split(" ")[0];
            String est = evaluar(r).completo ? "✔ completa" : "pendiente";
            if (i > 0) h.append(" &nbsp;|&nbsp; ");
            h.append(esc(nm)).append(": ").append(est);
        }
        return h.append("</font>").toString();
    }

    public void setMezcla(MezclaBaldosin m, BaldosinCatalan b, SueloData d) {
        this.baldosin = b;
        this.datos = d;
        if (registros.isEmpty()) registros.add(new Registro("Mezcla 1", m));
        else registros.get(0).mezcla = m;
        rellenarCombo();
        mostrarRegistro();
    }

    public void setMezclas3(MezclaBaldosin[] ms, String[] nombres, BaldosinCatalan b, SueloData d) {
        this.baldosin = b;
        this.datos = d;
        int k = ms == null ? 0 : ms.length;
        if (k > 0) {
            for (int i = 0; i < k; i++) {
                String nom = (nombres != null && i < nombres.length && nombres[i] != null)
                        ? nombres[i] : "Mezcla " + (i + 1);
                if (i < registros.size()) {
                    registros.get(i).nombre = nom;
                    registros.get(i).mezcla = ms[i];
                } else {
                    registros.add(new Registro(nom, ms[i]));
                }
                Registro reg = registros.get(i);
                if (!reg.cargado) { reg.cargado = true; if (reg.n == 0) cargar(reg, i); }
            }
            while (registros.size() > k) registros.remove(registros.size() - 1);
        }
        construirTarjetas();
        idxMezcla = Math.max(0, Math.min(idxMezcla, registros.size() - 1));
        rellenarCombo();
        mostrarRegistro();
    }

    private final JToggleButton tgPrensado = new JToggleButton("Prensado");
    private final JToggleButton tgExtruido = new JToggleButton("Extruido");
    private final JPanel filaTarjetas = new JPanel(new GridLayout(1, 3, 10, 0));
    private final List<TarjetaMezcla> tarjetas = new ArrayList<>();
    private static final Color[] COL_MEZCLA = {
        new Color(0x6F, 0x9F, 0xCC), new Color(0x5F, 0xAE, 0x8C), new Color(0xD9, 0xB4, 0x5A)};

    private JPanel buildProbetas() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        JLabel l1 = lbl("Conformado de las piezas", MUTED, 11, true);
        l1.setAlignmentX(Component.LEFT_ALIGNMENT);
        l1.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        p.add(l1);
        JPanel f1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        f1.setOpaque(false);
        f1.setAlignmentX(Component.LEFT_ALIGNMENT);
        ButtonGroup bg = new ButtonGroup();
        for (JToggleButton t : new JToggleButton[]{tgPrensado, tgExtruido}) {
            t.setFont(Tema.ui(Font.BOLD, 12));
            t.setFocusPainted(false);
            t.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            t.setPreferredSize(new Dimension(110, 30));
            bg.add(t);
            f1.add(t);
        }
        tgPrensado.setToolTipText("Baldosas prensadas · grupos B de la NTC 919 / ISO 13006");
        tgExtruido.setToolTipText("Baldosas extruidas · grupos A de la NTC 919 / ISO 13006");
        (LectorVideoEnsayo.EXTRUIDO ? tgExtruido : tgPrensado).setSelected(true);
        estiloConformado();
        tgPrensado.addActionListener(e -> cambiarConformado(false));
        tgExtruido.addActionListener(e -> cambiarConformado(true));
        p.add(f1);
        p.add(Box.createVerticalStrut(12));

        filaTarjetas.setOpaque(false);
        filaTarjetas.setAlignmentX(Component.LEFT_ALIGNMENT);
        ajustarTarjetas();
        p.add(filaTarjetas);
        construirTarjetas();
        p.add(Box.createVerticalStrut(12));

        JPanel f3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        f3.setOpaque(false);
        f3.setAlignmentX(Component.LEFT_ALIGNMENT);
        f3.add(lbl("Probetas de esta mezcla:", MUTED, 11, true));
        spProbetas.setFont(Tema.ui(Font.BOLD, 12));
        spProbetas.setPreferredSize(new Dimension(64, 28));
        f3.add(spProbetas);
        JButton bAplicar = mkBtn("Aplicar", ACCENT);
        bAplicar.addActionListener(e -> aplicarProbetas());
        f3.add(bAplicar);
        lblProbetas.setFont(Tema.ui(Font.PLAIN, 10));
        f3.add(lblProbetas);
        JButton bBorrar = new JButton("Borrar datos de esta mezcla");
        bBorrar.setFont(Tema.ui(Font.PLAIN, 10));
        bBorrar.setForeground(DIM);
        bBorrar.setContentAreaFilled(false);
        bBorrar.setBorderPainted(false);
        bBorrar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        bBorrar.addActionListener(e -> borrarMezcla());
        f3.add(bBorrar);
        p.add(f3);
        return p;
    }

    private void estiloConformado() {
        for (JToggleButton t : new JToggleButton[]{tgPrensado, tgExtruido}) {
            boolean on = t.isSelected();
            t.putClientProperty("JButton.buttonType", "roundRect");
            t.putClientProperty("FlatLaf.style",
                "selectedBackground: #D9B45A; selectedForeground: #1E1604; arc: 10");
            t.setBackground(on ? C_RES : BG3);
            t.setForeground(on ? new Color(30, 22, 4) : MUTED);
        }
    }

    private void cambiarConformado(boolean extruido) {
        estiloConformado();
        if (LectorVideoEnsayo.EXTRUIDO == extruido) return;
        LectorVideoEnsayo.EXTRUIDO = extruido;
        for (Registro r : registros) {
            if (r.abs != null) r.grupo = grupoISO13006(r.abs[0]);
            double ev = r.abs != null ? r.abs[0] : 0;
            for (int i = 0; i < r.flex.length; i++) {
                DetalleFlexion.ResultadoFlexion f = r.flex[i];
                if (f == null) continue;
                LectorVideoEnsayo.Evaluacion e = LectorVideoEnsayo.evaluarNorma(f.sN, f.rMPa, f.espesor, ev);
                f.cumple = e.cumple; f.textoNorma = e.texto;
                r.mFlex.setValueAt(f.cumple ? "Cumple" : "No cumple", i, 8);
            }
        }
        actualizarVista();
    }

    private void ajustarTarjetas() {
        boolean fila = anchoHtml >= 560;
        int n = Math.max(1, Math.min(3, registros.size()));
        filaTarjetas.setLayout(fila ? new GridLayout(1, 3, 10, 0) : new GridLayout(0, 1, 0, 8));
        Dimension d = fila ? new Dimension(Math.min(760, anchoHtml), 64)
                           : new Dimension(Math.min(420, anchoHtml), n * 58 + (n - 1) * 8);
        filaTarjetas.setPreferredSize(d);
        filaTarjetas.setMaximumSize(new Dimension(fila ? 760 : 420, d.height));
        filaTarjetas.setMinimumSize(new Dimension(240, d.height));
        filaTarjetas.revalidate();
    }

    private void construirTarjetas() {
        filaTarjetas.removeAll();
        tarjetas.clear();
        for (int i = 0; i < registros.size() && i < 3; i++) {
            TarjetaMezcla t = new TarjetaMezcla(i);
            tarjetas.add(t);
            filaTarjetas.add(t);
        }
        ajustarTarjetas();
        filaTarjetas.revalidate();
        filaTarjetas.repaint();
    }

    private void seleccionarMezcla(int i) {
        if (i < 0 || i >= registros.size() || i == idxMezcla) return;
        detenerEdicion();
        idxMezcla = i;
        actualizandoCombo = true;
        if (i < cbMezcla.getItemCount()) cbMezcla.setSelectedIndex(i);
        actualizandoCombo = false;
        mostrarRegistro();
    }

    private final class TarjetaMezcla extends JPanel {
        final int idx;
        boolean sobre;
        TarjetaMezcla(int idx) {
            this.idx = idx;
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { seleccionarMezcla(TarjetaMezcla.this.idx); }
                @Override public void mouseEntered(MouseEvent e) { sobre = true; repaint(); }
                @Override public void mouseExited(MouseEvent e)  { sobre = false; repaint(); }
            });
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            if (idx >= registros.size()) { g.dispose(); return; }
            Registro r = registros.get(idx);
            boolean sel = idx == idxMezcla;
            boolean completa = r.mezcla != null && evaluar(r).completo;
            boolean empezada = r.n > 0;
            Color col = COL_MEZCLA[idx % COL_MEZCLA.length];

            g.setColor(sel ? new Color(col.getRed(), col.getGreen(), col.getBlue(), 38) : (sobre ? BG3 : BG2));
            g.fillRoundRect(0, 0, w - 1, h - 1, 12, 12);
            g.setColor(sel ? col : BORD);
            g.setStroke(new BasicStroke(sel ? 2f : 1f));
            g.drawRoundRect(sel ? 1 : 0, sel ? 1 : 0, w - (sel ? 3 : 1), h - (sel ? 3 : 1), 12, 12);
            g.setColor(col);
            g.fillRoundRect(8, 12, 4, h - 24, 4, 4);

            int d = 22, cx = w - d - 14, cy = (h - d) / 2;
            int libre = cx - 22 - 8;
            String[] partes = (r.nombre == null ? "M" + (idx + 1) : r.nombre).split(" — ", 2);
            g.setFont(Tema.ui(Font.BOLD, 13));
            g.setColor(TEXT);
            FontMetrics fm = g.getFontMetrics();
            int yTit = h / 2 - 4, yEst = h / 2 + 14;
            g.drawString(recortar(partes[0], fm, libre), 22, yTit);
            int x = 22 + fm.stringWidth(partes[0] + "  ");
            if (partes.length > 1 && x < 22 + libre) {
                g.setFont(Tema.ui(Font.PLAIN, 12));
                g.setColor(MUTED);
                g.drawString(recortar(partes[1], g.getFontMetrics(), 22 + libre - x), x, yTit);
            }
            g.setFont(Tema.ui(Font.PLAIN, 11));
            g.setColor(completa ? GREEN : (empezada ? ACCENT : DIM));
            g.drawString(recortar(completa ? "Ensayos completos" : (empezada ? "En proceso" : "Sin datos"),
                                  g.getFontMetrics(), libre), 22, yEst);

            if (completa) {
                g.setColor(GREEN);
                g.fillOval(cx, cy, d, d);
                g.setColor(new Color(16, 32, 22));
                g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawPolyline(new int[]{cx + 6, cx + 10, cx + 16}, new int[]{cy + 11, cy + 15, cy + 7}, 3);
            } else {
                g.setColor(empezada ? ACCENT : DIM);
                g.setStroke(new BasicStroke(1.6f));
                g.drawOval(cx, cy, d, d);
            }
            g.dispose();
        }
    }

    private static final class AnchoVentana extends JPanel implements Scrollable {
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 16; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) {
            return Math.max(16, (o == SwingConstants.VERTICAL ? r.height : r.width) - 32);
        }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    private static String recortar(String s, FontMetrics fm, int ancho) {
        if (s == null) return "";
        if (fm.stringWidth(s) <= ancho) return s;
        String t = s;
        while (t.length() > 1 && fm.stringWidth(t + "…") > ancho) t = t.substring(0, t.length() - 1);
        return t.trim() + "…";
    }

    private void borrarMezcla() {
        Registro r = actual();
        if (JOptionPane.showConfirmDialog(this, "¿Borrar todos los datos de «" + r.nombre + "»?",
                "Borrar datos", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        detenerEdicion();
        r.n = 0;
        r.redimensionar(0);
        r.flex = new DetalleFlexion.ResultadoFlexion[0];
        r.abs = r.cont = r.dens = r.poros = null;
        r.grupo = null;
        r.absOk = r.contOk = r.densOk = r.absVig = r.contVig = r.densVig = false;
        mostrarRegistro();
    }

    private static final File ARCHIVO_DATOS =
        new File(new File(System.getProperty("user.home"), "SDIM"), "ensayos_ntc.properties");
    private boolean cargando;
    private final javax.swing.Timer timerGuardar = new javax.swing.Timer(700, e -> guardarAhora());
    { timerGuardar.setRepeats(false); }

    private void programarGuardado() {
        if (!cargando) timerGuardar.restart();
    }

    private static String une(double[] v) {
        if (v == null) return "";
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < v.length; i++) { if (i > 0) b.append(';'); b.append(v[i]); }
        return b.toString();
    }
    private static double[] separa(String s) {
        if (s == null || s.isEmpty()) return null;
        String[] p = s.split(";");
        double[] v = new double[p.length];
        for (int i = 0; i < p.length; i++) v[i] = Double.parseDouble(p[i]);
        return v;
    }
    private static String txt(Object o) { return o == null ? "" : o.toString(); }

    private void guardarAhora() {
        try {
            java.util.Properties p = new java.util.Properties();
            volcar(p, "");
            ARCHIVO_DATOS.getParentFile().mkdirs();
            try (java.io.OutputStream o = new java.io.FileOutputStream(ARCHIVO_DATOS)) {
                p.store(o, "SDIM - datos de Ensayos NTC (se guardan solos)");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void exportar(java.util.Properties p, String pre) {
        detenerEdicion();
        volcar(p, pre);
    }

    public void importar(java.util.Properties p, String pre) {
        detenerEdicion();
        boolean ext = "extruido".equals(p.getProperty(pre + "conformado"));
        LectorVideoEnsayo.EXTRUIDO = ext;
        (ext ? tgExtruido : tgPrensado).setSelected(true);
        estiloConformado();
        for (int i = 0; i < registros.size(); i++) {
            Registro r = registros.get(i);
            r.n = 0;
            r.redimensionar(0);
            r.flex = new DetalleFlexion.ResultadoFlexion[0];
            r.abs = r.cont = r.dens = r.poros = null;
            r.grupo = null;
            r.absOk = r.contOk = r.densOk = r.absVig = r.contVig = r.densVig = false;
            r.cargado = true;
            cargarDe(p, pre, r, i);
        }
        mostrarRegistro();
        programarGuardado();
    }

    private void volcar(java.util.Properties p, String pre) {
            p.setProperty(pre + "conformado", LectorVideoEnsayo.EXTRUIDO ? "extruido" : "prensado");
            for (int i = 0; i < registros.size(); i++) {
                Registro r = registros.get(i);
                String k = pre + "m" + i + ".";
                p.setProperty(k + "nombre", txt(r.nombre));
                p.setProperty(k + "n", String.valueOf(r.n));
                p.setProperty(k + "absOk", String.valueOf(r.absOk && r.absVig));
                p.setProperty(k + "contOk", String.valueOf(r.contOk && r.contVig));
                p.setProperty(k + "abs", une(r.abs));
                p.setProperty(k + "cont", une(r.cont));
                for (int j = 0; j < r.n; j++) {
                    String q = k + "p" + j + ".";
                    p.setProperty(q + "ma", txt(r.mAbs.getValueAt(j, 1)));
                    p.setProperty(q + "ms", txt(r.mAbs.getValueAt(j, 2)));
                    p.setProperty(q + "lv", txt(r.mCont.getValueAt(j, 1)));
                    p.setProperty(q + "lc", txt(r.mCont.getValueAt(j, 2)));
                    DetalleFlexion.ResultadoFlexion f = j < r.flex.length ? r.flex[j] : null;
                    if (f != null) {
                        p.setProperty(q + "flex", une(new double[]{f.largo, f.luz, f.ancho, f.espesor,
                                f.fMaxN, f.sN, f.rMPa, f.velKNs, f.velMPas}));
                    }
                }
            }
    }

    private java.util.Properties datosGuardados;

    private java.util.Properties leerGuardados() {
        if (datosGuardados == null) {
            datosGuardados = new java.util.Properties();
            if (ARCHIVO_DATOS.isFile()) {
                try (java.io.InputStream in = new java.io.FileInputStream(ARCHIVO_DATOS)) {
                    datosGuardados.load(in);
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        }
        return datosGuardados;
    }

    private void cargar(Registro r, int i) {
        cargarDe(leerGuardados(), "", r, i);
    }

    private void cargarDe(java.util.Properties p, String pre, Registro r, int i) {
        String k = pre + "m" + i + ".";
        if (!txt(r.nombre).equals(p.getProperty(k + "nombre"))) return;
        cargando = true;
        try {
            int n = Integer.parseInt(p.getProperty(k + "n", "0"));
            if (n <= 0) return;
            r.n = n;
            r.redimensionar(n);
            for (int j = 0; j < n; j++) {
                String q = k + "p" + j + ".";
                r.mAbs.setValueAt(p.getProperty(q + "ma", ""), j, 1);
                r.mAbs.setValueAt(p.getProperty(q + "ms", ""), j, 2);
                r.mCont.setValueAt(p.getProperty(q + "lv", ""), j, 1);
                r.mCont.setValueAt(p.getProperty(q + "lc", ""), j, 2);
                double[] fv = separa(p.getProperty(q + "flex"));
                if (fv != null && fv.length >= 9) {
                    DetalleFlexion.ResultadoFlexion f = new DetalleFlexion.ResultadoFlexion();
                    f.probeta = "P" + (j + 1);
                    f.largo = fv[0]; f.luz = fv[1]; f.ancho = fv[2]; f.espesor = fv[3];
                    f.fMaxN = fv[4]; f.sN = fv[5]; f.rMPa = fv[6]; f.velKNs = fv[7]; f.velMPas = fv[8];
                    r.flex[j] = f;
                }
            }
            r.abs = separa(p.getProperty(k + "abs"));
            r.cont = separa(p.getProperty(k + "cont"));
            r.absOk = r.absVig = Boolean.parseBoolean(p.getProperty(k + "absOk")) && r.abs != null;
            r.contOk = r.contVig = Boolean.parseBoolean(p.getProperty(k + "contOk")) && r.cont != null;
            if (r.abs != null) r.grupo = grupoISO13006(r.abs[0]);
            double ev = r.abs != null ? r.abs[0] : 0;
            for (int j = 0; j < n; j++) {
                DetalleFlexion.ResultadoFlexion f = r.flex[j];
                if (f == null) continue;
                LectorVideoEnsayo.Evaluacion e = LectorVideoEnsayo.evaluarNorma(f.sN, f.rMPa, f.espesor, ev);
                f.cumple = e.cumple; f.textoNorma = e.texto;
                r.mFlex.setValueAt(fmt(f.largo, 1), j, 1);
                r.mFlex.setValueAt(fmt(f.luz, 1), j, 2);
                r.mFlex.setValueAt(fmt(f.ancho, 1), j, 3);
                r.mFlex.setValueAt(fmt(f.espesor, 2), j, 4);
                r.mFlex.setValueAt(fmt(f.fMaxN, 0), j, 5);
                r.mFlex.setValueAt(fmt(f.sN, 0), j, 6);
                r.mFlex.setValueAt(fmt(f.rMPa, 2), j, 7);
                r.mFlex.setValueAt(f.cumple ? "Cumple" : "No cumple", j, 8);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            cargando = false;
        }
    }

    private void aplicarProbetas() {
        try { spProbetas.commitEdit(); } catch (Exception ignored) { }
        int n = (Integer) spProbetas.getValue();
        Registro r = actual();
        if (n == r.n) return;
        detenerEdicion();
        r.n = n;
        nProbetas = n;
        r.redimensionar(n);
        r.absVig = r.contVig = r.densVig = false;
        mostrarRegistro();
    }

    private void onCambioAbs(Registro r, TableModelEvent e) {
        if (e.getType() != TableModelEvent.UPDATE) return;
        int c = e.getColumn();
        if (c != 1 && c != 2) return;
        int last = Math.min(e.getLastRow(), r.mAbs.getRowCount() - 1);
        for (int i = Math.max(0, e.getFirstRow()); i <= last; i++) {
            calcFilaAbs(r.mAbs, i);
            r.mDens.setValueAt(r.mAbs.getValueAt(i, c), i, c);
        }
        r.absVig = false;
        r.densVig = false;
        programarGuardado();
        if (r == actual()) actualizarVista();
    }

    private void onCambioCont(Registro r, TableModelEvent e) {
        if (e.getType() != TableModelEvent.UPDATE) return;
        int c = e.getColumn();
        if (c != 1 && c != 2) return;
        int last = Math.min(e.getLastRow(), r.mCont.getRowCount() - 1);
        for (int i = Math.max(0, e.getFirstRow()); i <= last; i++) calcFilaCont(r.mCont, i);
        r.contVig = false;
        programarGuardado();
        if (r == actual()) actualizarVista();
    }

    private void onCambioDens(Registro r, TableModelEvent e) {
        if (e.getType() != TableModelEvent.UPDATE) return;
        int c = e.getColumn();
        if (c < 1 || c > 3) return;
        int last = Math.min(e.getLastRow(), r.mDens.getRowCount() - 1);
        for (int i = Math.max(0, e.getFirstRow()); i <= last; i++) calcFilaDens(r.mDens, i);
        r.densVig = false;
        if (r == actual()) actualizarVista();
    }

    private static void calcFilaAbs(DefaultTableModel m, int i) {
        double ma = num(m.getValueAt(i, 1)), ms = num(m.getValueAt(i, 2));
        String v = "";
        if (ma > 0 && ms > 0) v = ma >= ms ? fmt((ma - ms) / ms * 100, 2) : "⚠ ap. < seca";
        m.setValueAt(v, i, 3);
    }

    private static void calcFilaCont(DefaultTableModel m, int i) {
        double lv = num(m.getValueAt(i, 1)), lc = num(m.getValueAt(i, 2));
        String v = "";
        if (lv > 0 && lc > 0) v = fmt((lv - lc) / lv * 100, 2);
        m.setValueAt(v, i, 3);
    }

    private static void calcFilaDens(DefaultTableModel m, int i) {
        double ma = num(m.getValueAt(i, 1)), ms = num(m.getValueAt(i, 2)), msum = num(m.getValueAt(i, 3));
        String vV = "", vD = "", vP = "";
        if (ma > 0 && ms > 0 && msum >= 0) {
            double V = (ma - msum) / RHO_AGUA;
            if (V > 0) {
                vV = fmt(V, 2);
                vD = fmt(ms / V, 3);
                vP = fmt((ma - ms) / V * 100, 2);
            } else {
                vV = "⚠ sum. ≥ ap.";
            }
        }
        m.setValueAt(vV, i, 4);
        m.setValueAt(vD, i, 5);
        m.setValueAt(vP, i, 6);
    }

    private void calcularAbsorcion() {
        detenerEdicion();
        Registro r = actual();
        if (nProbetas == 0) return;

        List<String> err = new ArrayList<>();
        double[] v = new double[nProbetas];
        for (int i = 0; i < nProbetas; i++) {
            double ma = num(r.mAbs.getValueAt(i, 1)), ms = num(r.mAbs.getValueAt(i, 2));
            if (!(ma > 0) || !(ms > 0))
                err.add("P" + (i + 1) + ": faltan las masas o no son números positivos");
            else if (ma < ms)
                err.add("P" + (i + 1) + ": la masa aparentemente seca no puede ser menor que la seca");
            else
                // Absorcion de agua: Ev = (m. ap. seca - m. seca) / m. seca * 100
                v[i] = (ma - ms) / ms * 100;
        }
        if (!err.isEmpty()) { avisoErrores("Absorción de agua", err); return; }

        r.abs = estadisticas(v);
        r.grupo = grupoISO13006(r.abs[0]);
        r.absOk = r.absVig = true;
        actualizarVista();
    }

    private void calcularContraccion() {
        detenerEdicion();
        Registro r = actual();
        if (nProbetas == 0) return;

        List<String> err = new ArrayList<>();
        double[] v = new double[nProbetas];
        for (int i = 0; i < nProbetas; i++) {
            double lv = num(r.mCont.getValueAt(i, 1)), lc = num(r.mCont.getValueAt(i, 2));
            if (!(lv > 0) || !(lc > 0))
                err.add("P" + (i + 1) + ": faltan las longitudes o no son números positivos");
            else
                // Contraccion lineal: Ct = (L verde - L cocida) / L verde * 100
                v[i] = (lv - lc) / lv * 100;
        }
        if (!err.isEmpty()) { avisoErrores("Contracción lineal", err); return; }

        r.cont = estadisticas(v);
        r.contOk = r.contVig = true;
        actualizarVista();
    }

    private void calcularDensidad() {
        detenerEdicion();
        Registro r = actual();
        if (nProbetas == 0) return;

        List<String> err = new ArrayList<>();
        double[] d = new double[nProbetas], p = new double[nProbetas];
        for (int i = 0; i < nProbetas; i++) {
            double ma = num(r.mDens.getValueAt(i, 1)), ms = num(r.mDens.getValueAt(i, 2));
            double msum = num(r.mDens.getValueAt(i, 3));
            if (!(ma > 0) || !(ms > 0)) {
                err.add("P" + (i + 1) + ": faltan las masas del ensayo de absorción");
            } else if (!(msum >= 0)) {
                err.add("P" + (i + 1) + ": falta la masa sumergida");
            } else if (ma - msum <= 0) {
                err.add("P" + (i + 1) + ": la masa sumergida debe ser menor que la aparentemente seca");
            } else {
                double V = (ma - msum) / RHO_AGUA;
                d[i] = ms / V;
                p[i] = (ma - ms) / V * 100;
            }
        }
        if (!err.isEmpty()) { avisoErrores("Densidad aparente", err); return; }

        r.dens = estadisticas(d);
        r.poros = estadisticas(p);
        r.densOk = r.densVig = true;
        actualizarVista();
    }

    private static double[] estadisticas(double[] v) {
        double s = 0, mn = Double.MAX_VALUE, mx = -Double.MAX_VALUE;
        for (double x : v) { s += x; mn = Math.min(mn, x); mx = Math.max(mx, x); }
        double prom = s / v.length;
        double ss = 0;
        for (double x : v) ss += (x - prom) * (x - prom);
        double desv = v.length > 1 ? Math.sqrt(ss / (v.length - 1)) : 0;
        return new double[]{prom, desv, mn, mx};
    }

    // Grupo por absorcion de agua Ev (ISO 13006 / NTC 919)
    private static String[] grupoISO13006(double ev) {
        String L = LectorVideoEnsayo.EXTRUIDO ? "A" : "B";
        if (ev <= 0.5)  return new String[]{L + "Ia",  "Ev ≤ 0,5 %"};
        if (ev <= 3.0)  return new String[]{L + "Ib",  "0,5 % < Ev ≤ 3 %"};
        if (ev <= 6.0)  return new String[]{L + "IIa", "3 % < Ev ≤ 6 %"};
        if (ev <= 10.0) return new String[]{L + "IIb", "6 % < Ev ≤ 10 %"};
        return new String[]{L + "III", "Ev > 10 %"};
    }

    private static String conformado() { return LectorVideoEnsayo.EXTRUIDO ? "extruidas" : "prensadas"; }

    private JPanel buildFlexion() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        JPanel fila = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        fila.setOpaque(false);
        fila.setAlignmentX(Component.LEFT_ALIGNMENT);
        fila.add(lbl("Probeta:", MUTED, 11, true));
        cbProbFlex.setFont(Tema.ui(Font.PLAIN, 11));
        cbProbFlex.setPreferredSize(new Dimension(110, 28));
        fila.add(cbProbFlex);

        JButton b = Tema.botonSolido("Ensayo a flexión", C_FLEX);
        b.setFont(Tema.ui(Font.BOLD, 13));
        b.addActionListener(e -> {
            int i = cbProbFlex.getSelectedIndex();
            if (i >= 0) abrirFlexion(i);
        });
        fila.add(b);
        p.add(fila);
        p.add(Box.createVerticalStrut(10));

        p.add(scFlex);
        p.add(Box.createVerticalStrut(8));

        resFlex.setFont(Tema.ui(Font.PLAIN, 11));
        resFlex.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(resFlex);
        return p;
    }

    private void abrirFlexion(int i) {
        detenerEdicion();
        Registro r = actual();
        if (i < 0 || i >= nProbetas) return;

        double fac = (r.contOk && r.cont != null) ? 1 - r.cont[0] / 100.0 : 1.0;

        double largo = num(r.mCont.getValueAt(i, 2));
        if (!(largo > 0)) largo = baldosin != null ? baldosin.largo * 10 * fac : Double.NaN;

        DetalleFlexion.ResultadoFlexion prev = r.flex[i];
        double ancho = prev != null ? prev.ancho
                     : (baldosin != null ? baldosin.ancho * 10 * fac : Double.NaN);
        double esp   = prev != null ? prev.espesor
                     : (baldosin != null ? baldosin.espesor * 10 * fac : Double.NaN);

        double luz = Double.NaN;
        if (prev != null) luz = prev.luz;
        else for (DetalleFlexion.ResultadoFlexion f : r.flex) if (f != null) luz = f.luz;

        double abs = (r.absOk && r.abs != null) ? r.abs[0] : Double.NaN;

        DetalleFlexion.ResultadoFlexion res = DetalleFlexion.ensayar(this, baldosin, r.mezcla, datos,
                estimarSigma(r), "P" + (i + 1), largo, ancho, esp, luz, abs);
        if (res == null) return;

        r.flex[i] = res;
        r.mFlex.setValueAt(fmt(res.largo, 1),   i, 1);
        r.mFlex.setValueAt(fmt(res.luz, 1),     i, 2);
        r.mFlex.setValueAt(fmt(res.ancho, 1),   i, 3);
        r.mFlex.setValueAt(fmt(res.espesor, 2), i, 4);
        r.mFlex.setValueAt(fmt(res.fMaxN, 0),   i, 5);
        r.mFlex.setValueAt(fmt(res.sN, 0),      i, 6);
        r.mFlex.setValueAt(fmt(res.rMPa, 2),    i, 7);
        r.mFlex.setValueAt(res.cumple ? "Cumple" : "No cumple", i, 8);

        actualizarVista();

        int sig = -1;
        for (int j = 1; j <= nProbetas; j++) {
            int k = (i + j) % nProbetas;
            if (r.flex[k] == null) { sig = k; break; }
        }
        if (sig >= 0) cbProbFlex.setSelectedIndex(sig);
        final JComponent destino = (sig < 0) ? secRes : secFlex;
        SwingUtilities.invokeLater(() ->
            destino.scrollRectToVisible(new Rectangle(0, 0, destino.getWidth(), destino.getHeight())));
    }

    private double estimarSigma(Registro r) {
        MezclaBaldosin m = r.mezcla;
        if (m == null) return 10;
        double pctArc = (datos != null && datos.hidroCalculada) ? datos.pctArcilla : 20;
        double dens = r.densOk ? r.dens[0] : m.densidadAparente;
        double abs  = r.absOk  ? r.abs[0]  : m.absorcionAgua;
        return clamp(4.5 + dens * 3.8 + m.fracFeldespato * 28 + m.fracSilice * 12
                     - pctArc * 0.04 - abs * 0.15, 4, 22);
    }

    private static int nEnsayadas(Registro r) {
        int k = 0;
        for (DetalleFlexion.ResultadoFlexion f : r.flex) if (f != null) k++;
        return k;
    }

    private void rellenarComboFlex(Registro r) {
        int sel = cbProbFlex.getSelectedIndex();
        cbProbFlex.removeAllItems();
        for (int i = 0; i < nProbetas; i++)
            cbProbFlex.addItem("P" + (i + 1) + (i < r.flex.length && r.flex[i] != null ? "  ✔" : ""));
        if (nProbetas > 0) cbProbFlex.setSelectedIndex(sel >= 0 && sel < nProbetas ? sel : 0);
    }

    private String resumenHtml(Registro r) {
        if (r.abs != null && r.grupo == null) r.grupo = grupoISO13006(r.abs[0]);
        List<DetalleFlexion.ResultadoFlexion> hechos = new ArrayList<>();
        for (DetalleFlexion.ResultadoFlexion f : r.flex) if (f != null) hechos.add(f);
        int k = hechos.size();

        double[] s = new double[k], rr = new double[k], e = new double[k];
        int indivOk = 0;
        for (int i = 0; i < k; i++) {
            s[i]  = hechos.get(i).sN;
            rr[i] = hechos.get(i).rMPa;
            e[i]  = hechos.get(i).espesor;
            if (hechos.get(i).cumple) indivOk++;
        }
        double[] eS = estadisticas(s), eR = estadisticas(rr), eE = estadisticas(e);
        double absEval = (r.abs != null) ? r.abs[0] : 0;
        LectorVideoEnsayo.Evaluacion ev = LectorVideoEnsayo.evaluarNorma(eS[0], eR[0], eE[0], absEval);

        boolean completo = k == nProbetas;
        boolean cumpleTodo = ev.cumple && indivOk == k;
        flexCompleto = completo;
        flexCumple = cumpleTodo;

        StringBuilder h = new StringBuilder("<html><table cellspacing='0' cellpadding='5' width='" + anchoHtml + "'>");
        h.append("<tr bgcolor='" + hx(Tema.BARRA) + "'>")
         .append(celda("Ensayo", MUTED, true))
         .append(celda("Resultado (promedio)", MUTED, true))
         .append(celda("Referencia NTC 919 / ISO 13006", MUTED, true))
         .append(celda("Estado", MUTED, true))
         .append("</tr>");

        int fila = 0;
        if (r.abs != null) {
            h.append(tr(fila++))
             .append(celda("Absorción de agua (Ev)", TEXT, false))
             .append(celda(fmt(r.abs[0], 2) + " %  (σ " + fmt(r.abs[1], 2) + ")", GREEN, true))
             .append(celda("Grupo " + r.grupo[0] + " · " + r.grupo[1] + " (" + conformado() + ")", AMBER, false))
             .append(celda("ℹ Clasifica", AMBER, true))
             .append("</tr>");
        }
        if (r.cont != null) {
            h.append(tr(fila++))
             .append(celda("Contracción lineal total", TEXT, false))
             .append(celda(fmt(r.cont[0], 2) + " %  (σ " + fmt(r.cont[1], 2) + ")", GREEN, true))
             .append(celda("Informativo (control de proceso)", DIM, false))
             .append(celda("—", DIM, false))
             .append("</tr>");
        }
        if (r.dens != null) {
            h.append(tr(fila++))
             .append(celda("Densidad aparente", TEXT, false))
             .append(celda(fmt(r.dens[0], 3) + " g/cm³ · porosidad " + fmt(r.poros[0], 2) + " %", GREEN, true))
             .append(celda("Informativo (caracterización)", DIM, false))
             .append(celda("—", DIM, false))
             .append("</tr>");
        }
        h.append(tr(fila))
         .append(celda("Resistencia a la flexión", TEXT, false))
         .append(celda("S = " + fmt(eS[0], 0) + " N (mín " + fmt(eS[2], 0) + ")<br>"
                     + "R = " + fmt(eR[0], 2) + " MPa (mín " + fmt(eR[2], 2) + ")", GREEN, true))
         .append(celda(esc(ev.texto), AMBER, false))
         .append(celda(ev.cumple ? "CUMPLE" : "NO CUMPLE", ev.cumple ? GREEN : RED, true))
         .append("</tr></table>");

        h.append("<div style='width:" + anchoHtml + "px'><br>")
        ;

        if (!completo) {
            h.append("<font color='").append(hx(AMBER)).append("'>⚠ Faltan probetas por ensayar: "
                   + "el resultado es parcial.</font>");
        } else {
            h.append("<b><font color='").append(hx(cumpleTodo ? GREEN : RED)).append("'>")
             .append("Conclusión: la mezcla «").append(esc(r.nombre)).append("» ")
             .append(cumpleTodo ? "CUMPLE" : "NO CUMPLE")
             .append(" los requisitos de flexión de la NTC 919 para el grupo ")
             .append(r.grupo != null ? r.grupo[0] : "—")
             .append(" (promedio e individuales).</font></b>");
        }
        h.append(comparacionHtml());
        h.append("</div></html>");
        return h.toString();
    }

    private static String tr(int i) {
        return "<tr bgcolor='" + (i % 2 == 0 ? Tema.hex(Tema.SUPERFICIE) : Tema.hex(Tema.SUPERFICIE_2)) + "'>";
    }

    private String celda(String txt, Color c, boolean negrita) {
        return "<td valign='top'><font color='" + hx(c) + "'>" + (negrita ? "<b>" : "")
             + txt + (negrita ? "</b>" : "") + "</font></td>";
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private Registro actual() {
        if (registros.isEmpty()) registros.add(new Registro("Mezcla 1", null));
        idxMezcla = Math.max(0, Math.min(idxMezcla, registros.size() - 1));
        return registros.get(idxMezcla);
    }

    private void rellenarCombo() {
        actualizandoCombo = true;
        cbMezcla.removeAllItems();
        for (Registro r : registros) cbMezcla.addItem(r.nombre);
        if (!registros.isEmpty()) cbMezcla.setSelectedIndex(Math.min(idxMezcla, registros.size() - 1));
        cbMezcla.setEnabled(registros.size() > 1);
        actualizandoCombo = false;
    }

    private void mostrarRegistro() {
        Registro r = actual();
        nProbetas = r.n;
        spProbetas.setValue(r.n > 0 ? r.n : 5);
        tblAbs.setModel(r.mAbs);
        tblCont.setModel(r.mCont);
        tblDens.setModel(r.mDens);
        configurarTabla(tblAbs,  new int[]{3},       new int[]{});
        configurarTabla(tblCont, new int[]{3},       new int[]{});
        configurarTabla(tblDens, new int[]{4, 5, 6}, new int[]{1, 2});
        tblFlex.setModel(r.mFlex);
        configurarTabla(tblFlex, new int[]{6, 7, 8}, new int[]{1, 2, 3, 4, 5});
        ajustarAltura(scAbs, tblAbs);
        ajustarAltura(scCont, tblCont);
        ajustarAltura(scDens, tblDens);
        ajustarAltura(scFlex, tblFlex);
        actualizarVista();
    }

    private void recalcularAnchos() {
        Container pr = resAbs.getParent(), ps = resResumen.getParent();

        if (pr != null && pr.getWidth() > 60) anchoRes = Math.max(160, (int) ((pr.getWidth() - 12) / 1.34));
        if (ps != null && ps.getWidth() > 60) anchoHtml = Math.max(240, (int) ((ps.getWidth() - 12) / 1.34));
    }

    private void actualizarVista() {
        recalcularAnchos();
        Registro r = actual();

        lblProbetas.setText(nProbetas == 0
            ? "<html><font color='" + hx(ACCENT) + "'>← indique cuántas probetas tiene " + esc(r.nombre)
              + " y pulse Aplicar</font></html>"
            : "<html><font color='" + hx(DIM) + "'>" + esc(r.nombre) + "</font></html>");

        secAbs.setVisible(nProbetas > 0);
        secCont.setVisible(nProbetas > 0 && r.absOk);
        secDens.setVisible(false);
        secFlex.setVisible(nProbetas > 0 && r.absOk && r.contOk);

        int ensayadas = nEnsayadas(r);
        secRes.setVisible(secFlex.isVisible() && ensayadas > 0);
        rellenarComboFlex(r);
        resFlex.setText(ensayadas == 0
            ? html(DIM, "Elija la probeta y pulse «Ensayo a flexión». El largo se toma de su longitud cocida. "
                      + "En la ventana del ensayo confirme luz, ancho y espesor y cargue el video; "
                      + "cuando la probeta se rompa, pulse «Continuar».")
            : html(MUTED, "Doble clic en una fila para repetir el ensayo de esa probeta."));
        if (ensayadas > 0) {
            resResumen.setText(resumenHtml(r));
            actualizarUso(r);
        }

        if (!r.absOk) {
            resAbs.setText(html(DIM, "Ingrese las masas de cada probeta y pulse «Calcular absorción»."));
        } else if (!r.absVig) {
            resAbs.setText(html(AMBER, "⟳ Los datos cambiaron: pulse «Calcular absorción» para actualizar."));
        } else {
            resAbs.setText("<html><div style='width:" + anchoRes + "px'>"
                + "<font color='" + hx(MUTED) + "'>Absorción promedio: </font>"
                + "<b><font color='" + hx(GREEN) + "'>" + fmt(r.abs[0], 2) + " %</font></b>"
                + "<font color='" + hx(DIM) + "'>  (σ = " + fmt(r.abs[1], 2)
                + " · mín " + fmt(r.abs[2], 2) + " · máx " + fmt(r.abs[3], 2) + ")</font><br>"
                + "<font color='" + hx(MUTED) + "'>Grupo ISO 13006 / NTC 919 (" + conformado() + "): </font>"
                + "<b><font color='" + hx(AMBER) + "'>" + r.grupo[0] + "</font></b>"
                + "<font color='" + hx(DIM) + "'>  ·  " + r.grupo[1] + "</font></div></html>");
        }

        if (!r.contOk) {
            resCont.setText(html(DIM, "Ingrese la longitud de cada probeta en verde y después de cocida."));
        } else if (!r.contVig) {
            resCont.setText(html(AMBER, "⟳ Los datos cambiaron: pulse «Calcular contracción» para actualizar."));
        } else {
            resCont.setText("<html><div style='width:" + anchoRes + "px'>"
                + "<font color='" + hx(MUTED) + "'>Contracción lineal total promedio: </font>"
                + "<b><font color='" + hx(GREEN) + "'>" + fmt(r.cont[0], 2) + " %</font></b>"
                + "<font color='" + hx(DIM) + "'>  (σ = " + fmt(r.cont[1], 2)
                + " · mín " + fmt(r.cont[2], 2) + " · máx " + fmt(r.cont[3], 2) + ")</font></div></html>");
        }

        if (!r.densOk) {
            resDens.setText(html(DIM, "Las masas seca y aparentemente seca vienen del ensayo de absorción. "
                + "Ingrese la masa sumergida (suspendida en agua) de cada probeta."));
        } else if (!r.densVig) {
            resDens.setText(html(AMBER, "⟳ Los datos cambiaron: pulse «Calcular densidad» para actualizar."));
        } else {
            resDens.setText("<html><div style='width:" + anchoRes + "px'>"
                + "<font color='" + hx(MUTED) + "'>Densidad aparente promedio: </font>"
                + "<b><font color='" + hx(GREEN) + "'>" + fmt(r.dens[0], 3) + " g/cm³</font></b>"
                + "<font color='" + hx(DIM) + "'>  (σ = " + fmt(r.dens[1], 3) + ")</font><br>"
                + "<font color='" + hx(MUTED) + "'>Porosidad aparente promedio: </font>"
                + "<b><font color='" + hx(GREEN) + "'>" + fmt(r.poros[0], 2) + " %</font></b>"
                + "<font color='" + hx(DIM) + "'>  (σ = " + fmt(r.poros[1], 2) + ")</font></div></html>");
        }

        for (TarjetaMezcla t : tarjetas) t.repaint();
        programarGuardado();
        imgAbsorcion.repaint();
        imgContrac.repaint();
        imgDensidad.repaint();
        imgFlexion.repaint();
        revalidate();
        repaint();
    }

    private static String html(Color c, String s) {
        return "<html><div style='width:" + anchoRes + "px'><font color='" + String.format("#%02x%02x%02x",
                c.getRed(), c.getGreen(), c.getBlue()) + "'>" + s + "</font></div></html>";
    }

    private void avisoErrores(String ensayo, List<String> err) {
        StringBuilder sb = new StringBuilder("Revise los datos:\n\n");
        int k = 0;
        for (String s : err) {
            if (k++ == 12) { sb.append("  … y ").append(err.size() - 12).append(" más\n"); break; }
            sb.append("  • ").append(s).append('\n');
        }
        JOptionPane.showMessageDialog(this, sb.toString(), ensayo, JOptionPane.WARNING_MESSAGE);
    }

    private void detenerEdicion() {
        for (JTable t : new JTable[]{tblAbs, tblCont, tblDens})
            if (t.isEditing() && t.getCellEditor() != null) t.getCellEditor().stopCellEditing();
    }

    private static DefaultTableModel modelo(String[] cols, int... editables) {
        Set<Integer> ed = new HashSet<>();
        for (int c : editables) ed.add(c);
        return new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return ed.contains(c); }
        };
    }

    private static void ajustarFilas(DefaultTableModel m, int n) {
        int cols = m.getColumnCount();
        while (m.getRowCount() < n) {
            Object[] fila = new Object[cols];
            fila[0] = "P" + (m.getRowCount() + 1);
            for (int c = 1; c < cols; c++) fila[c] = "";
            m.addRow(fila);
        }
        while (m.getRowCount() > n) m.removeRow(m.getRowCount() - 1);
    }

    private void configurarTabla(JTable t, int[] calculadas, int[] soloLectura) {
        Set<Integer> calc = new HashSet<>(), ro = new HashSet<>();
        for (int c : calculadas)  calc.add(c);
        for (int c : soloLectura) ro.add(c);

        t.setFont(Tema.ui(Font.PLAIN, 11));
        t.setRowHeight(26);
        t.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        t.setBackground(BG3);
        t.setForeground(TEXT);
        t.setGridColor(BORD);
        t.setSelectionBackground(Tema.SELECCION);
        t.setSelectionForeground(Color.WHITE);
        t.setFillsViewportHeight(true);
        t.setSurrendersFocusOnKeystroke(true);
        t.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        t.setCellSelectionEnabled(true);

        JTableHeader th = t.getTableHeader();
        th.setReorderingAllowed(false);
        DefaultTableCellRenderer hr = new DefaultTableCellRenderer() {
            @Override public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                d.height = Math.max(d.height + 8, 42);
                return d;
            }
        };
        hr.setHorizontalAlignment(SwingConstants.CENTER);
        hr.setVerticalAlignment(SwingConstants.CENTER);
        hr.setBackground(Tema.BARRA);
        hr.setForeground(MUTED);
        hr.setFont(Tema.ui(Font.BOLD, 10));
        hr.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, BORD));
        th.setDefaultRenderer(hr);

        DefaultTableCellRenderer cr = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel,
                                                                     boolean foc, int row, int col) {
                super.getTableCellRendererComponent(tb, v, sel, foc, row, col);
                setHorizontalAlignment(SwingConstants.CENTER);
                String s = v == null ? "" : v.toString();
                if (col == 0) {
                    setBackground(BG2); setForeground(MUTED);
                    setFont(Tema.ui(Font.BOLD, 11));
                } else if (calc.contains(col)) {
                    setBackground(Tema.esOscuro() ? new Color(16, 34, 30) : new Color(224, 242, 233));
                    setForeground(s.startsWith("⚠") || s.startsWith("No cumple") || s.startsWith("NO CUMPLE") ? RED : GREEN);
                    setFont(Tema.ui(Font.BOLD, 11));
                } else if (ro.contains(col)) {
                    setBackground(BG2); setForeground(DIM);
                    setFont(Tema.ui(Font.PLAIN, 11));
                } else {
                    setBackground(BG3); setForeground(TEXT);
                    setFont(Tema.ui(Font.PLAIN, 11));
                }
                if (sel && !calc.contains(col) && col != 0) setBackground(Tema.SELECCION);
                return this;
            }
        };
        for (int c = 0; c < t.getColumnCount(); c++) t.getColumnModel().getColumn(c).setCellRenderer(cr);
        t.getColumnModel().getColumn(0).setPreferredWidth(60);
        t.getColumnModel().getColumn(0).setMinWidth(50);
        t.getColumnModel().getColumn(0).setMaxWidth(70);
        for (int c = 1; c < t.getColumnCount(); c++) {
            t.getColumnModel().getColumn(c).setMinWidth(70);
            t.getColumnModel().getColumn(c).setPreferredWidth(120);
        }
        th.setPreferredSize(null);
        th.resizeAndRepaint();

        JTextField tf = new JTextField();
        tf.setHorizontalAlignment(JTextField.CENTER);
        tf.setFont(Tema.ui(Font.PLAIN, 11));
        tf.setBackground(Tema.esOscuro() ? new Color(30, 38, 58) : new Color(226, 236, 246));
        tf.setForeground(Color.WHITE);
        tf.setCaretColor(Color.WHITE);
        tf.setBorder(BorderFactory.createLineBorder(ACCENT));
        DefaultCellEditor ed = new DefaultCellEditor(tf);
        ed.setClickCountToStart(1);
        t.setDefaultEditor(Object.class, ed);
    }

    private static void ajustarAltura(JScrollPane sc, JTable t) {
        int visibles = Math.max(1, Math.min(t.getRowCount(), 10));
        int alto = t.getTableHeader().getPreferredSize().height + visibles * t.getRowHeight() + 4;
        sc.setPreferredSize(new Dimension(300, alto));
        sc.setMinimumSize(new Dimension(200, alto));
        sc.setMaximumSize(new Dimension(Integer.MAX_VALUE, alto));
        sc.revalidate();
    }

    private static void habilitarPegado(JTable t) {
        KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK);
        t.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks, "pegarExcel");
        t.getInputMap(JComponent.WHEN_FOCUSED).put(ks, "pegarExcel");
        t.getActionMap().put("pegarExcel", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                try {
                    String s = (String) Toolkit.getDefaultToolkit().getSystemClipboard()
                                              .getData(DataFlavor.stringFlavor);
                    if (s == null) return;
                    if (t.isEditing() && t.getCellEditor() != null) t.getCellEditor().cancelCellEditing();
                    int r0 = Math.max(0, t.getSelectedRow());
                    int c0 = Math.max(1, t.getSelectedColumn());
                    String[] lineas = s.split("\\r?\\n");
                    for (int i = 0; i < lineas.length; i++) {
                        String[] celdas = lineas[i].split("\t", -1);
                        for (int j = 0; j < celdas.length; j++) {
                            int r = r0 + i, c = c0 + j;
                            if (r < t.getRowCount() && c < t.getColumnCount() && t.isCellEditable(r, c))
                                t.setValueAt(celdas[j].trim(), r, c);
                        }
                    }
                } catch (Exception ignored) { }
            }
        });
    }

    private static double num(Object o) {
        if (o == null) return Double.NaN;
        String s = o.toString().trim().replace(',', '.');
        if (s.isEmpty()) return Double.NaN;
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return Double.NaN; }
    }

    private static String fmt(double v, int dec) {
        if (Double.isNaN(v)) return "";
        return String.format("%." + dec + "f", v);
    }

    private JPanel mkSeccion(String nodo1, String nodo2, int num, Color col,
                             String titulo, String norma, JComponent imagen, JComponent contenido) {
        JPanel fila = new JPanel(new BorderLayout()) {
            @Override public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        fila.setOpaque(false);
        fila.setAlignmentX(Component.LEFT_ALIGNMENT);

        NodoMapa nodo = new NodoMapa(num, nodo1, nodo2, col, fila);
        nodos.add(nodo);
        fila.add(nodo, BorderLayout.WEST);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setBorder(BorderFactory.createEmptyBorder(9, 0, 9, 0));
        wrap.add(mkTarjeta(col, titulo, norma, imagen, contenido), BorderLayout.CENTER);
        fila.add(wrap, BorderLayout.CENTER);
        return fila;
    }

    private JPanel mkTarjeta(Color col, String titulo, String norma, JComponent imagen, JComponent contenido) {
        JPanel card = new JPanel(new BorderLayout(14, 8));
        card.setBackground(BG2);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 3, 0, 0, col),
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORD),
                BorderFactory.createEmptyBorder(10, 12, 12, 12))));

        JPanel tit = new JPanel();
        tit.setLayout(new BoxLayout(tit, BoxLayout.Y_AXIS));
        tit.setOpaque(false);
        tit.add(lbl(titulo, col, 13, true));
        tit.add(Box.createVerticalStrut(2));
        tit.add(lbl(norma, DIM, 10, false));
        card.add(tit, BorderLayout.NORTH);

        if (imagen != null) {
            JPanel wrap = new JPanel(new BorderLayout());
            wrap.setOpaque(false);
            wrap.add(imagen, BorderLayout.NORTH);
            card.add(wrap, BorderLayout.WEST);
        }
        card.add(contenido, BorderLayout.CENTER);
        return card;
    }

    private JPanel contenido(JScrollPane tabla, JButton boton, JLabel resultado) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        p.add(tabla);
        p.add(Box.createVerticalStrut(8));

        JPanel fila = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        fila.setOpaque(false);
        fila.setAlignmentX(Component.LEFT_ALIGNMENT);
        fila.add(boton);
        p.add(fila);
        p.add(Box.createVerticalStrut(8));

        resultado.setFont(Tema.ui(Font.PLAIN, 11));
        resultado.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(resultado);
        return p;
    }

    private JPanel buildUso() {
        panelUso.setLayout(new BoxLayout(panelUso, BoxLayout.Y_AXIS));
        panelUso.setOpaque(false);
        panelUso.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));

        JLabel tit = lbl("USO RECOMENDADO", C_RES, 12, true);
        tit.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelUso.add(tit);
        panelUso.add(Box.createVerticalStrut(6));

        lblUso.setFont(Tema.ui(Font.PLAIN, 11));
        lblUso.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelUso.add(lblUso);
        panelUso.add(Box.createVerticalStrut(10));

        filaImgs.setOpaque(false);
        filaImgs.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelUso.add(filaImgs);

        btnInforme.setFont(Tema.ui(Font.BOLD, 13));
        btnInforme.setForeground(new Color(30, 22, 4));
        btnInforme.setBackground(C_RES);
        btnInforme.setFocusPainted(false);
        btnInforme.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnInforme.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_RES.darker()),
            BorderFactory.createEmptyBorder(9, 22, 9, 22)));
        btnInforme.setToolTipText("Ubicación, clasificación del suelo, baldosín y secciones, "
                                + "secado, cocción y resumen de ensayos.");
        btnInforme.addActionListener(e -> descargarInforme());
        JPanel filaBtn = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        filaBtn.setOpaque(false);
        filaBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        filaBtn.add(btnInforme);
        panelUso.add(Box.createVerticalStrut(16));
        panelUso.add(filaBtn);
        return panelUso;
    }

    private void descargarInforme() {
        detenerEdicion();
        List<Registro> completas = new ArrayList<>();
        List<String> faltan = new ArrayList<>();
        for (Registro x : registros) {
            if (x.mezcla == null) continue;
            if (evaluar(x).completo) completas.add(x); else faltan.add(x.nombre);
        }
        if (completas.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Complete al menos una mezcla (absorción, contracción y flexión de todas sus probetas)\n"
              + "para generar el informe final.", "Informe final", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (!faltan.isEmpty() && JOptionPane.showConfirmDialog(this,
                "Estas mezclas aún no tienen todos los ensayos:\n  · " + String.join("\n  · ", faltan)
              + "\n\nEl informe y la comparación se harán solo con las mezclas completas.\n¿Continuar?",
                "Informe final", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)
                != JOptionPane.YES_OPTION) return;

        String base = (completas.size() == 1 ? "Informe_final_" + completas.get(0).nombre
                                             : "Informe_final_mezclas")
                .replaceAll("[^A-Za-z0-9áéíóúÁÉÍÓÚñÑ_-]+", "_");
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Guardar informe final");
        fc.setSelectedFile(new File(base + ".pdf"));
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Documento PDF", "pdf"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith(".pdf")) f = new File(f.getParentFile(), f.getName() + ".pdf");
        if (f.exists() && JOptionPane.showConfirmDialog(this, "El archivo ya existe. ¿Reemplazarlo?",
                "Informe final", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;

        final File destino = f;
        final List<InformeFinal.Contenido> c = new ArrayList<>();
        for (Registro x : completas) c.add(contenidoInforme(x));
        btnInforme.setEnabled(false);
        btnInforme.setText("⏳  Generando informe…");
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                InformeFinal.generar(destino, c);
                return null;
            }
            @Override protected void done() {
                btnInforme.setEnabled(true);
                btnInforme.setText("Descargar informe final (PDF)");
                try {
                    get();
                    int op = JOptionPane.showConfirmDialog(EnsayosNTC.this,
                        "Informe guardado en:\n" + destino.getAbsolutePath() + "\n\n¿Abrirlo ahora?",
                        "Informe final", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
                    if (op == JOptionPane.YES_OPTION && Desktop.isDesktopSupported())
                        Desktop.getDesktop().open(destino);
                } catch (Exception ex) {
                    Throwable t = ex.getCause() != null ? ex.getCause() : ex;
                    JOptionPane.showMessageDialog(EnsayosNTC.this, "No se pudo generar el informe:\n" + t,
                        "Informe final", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private InformeFinal.Contenido contenidoInforme(Registro r) {
        InformeFinal.Contenido c = new InformeFinal.Contenido();
        c.suelo = datos;
        c.baldosin = baldosin;
        c.mezcla = r.mezcla;
        c.nombreMezcla = r.nombre;
        c.nProbetas = r.n;
        c.extruido = LectorVideoEnsayo.EXTRUIDO;
        for (Registro x : registros) {
            if (x.mezcla == null) continue;
            c.mezclasDiseno.add(x.mezcla);
            c.nombresDiseno.add(x.nombre);
        }
        c.abs = r.abs; c.cont = r.cont; c.dens = null; c.poros = null; c.grupo = r.grupo;

        List<DetalleFlexion.ResultadoFlexion> hechos = new ArrayList<>();
        for (DetalleFlexion.ResultadoFlexion f : r.flex) if (f != null) hechos.add(f);
        c.flexion = hechos;
        Eval e = evaluar(r);
        if (e.k > 0) {
            c.sProm = e.sProm; c.sMin = e.sMin; c.rProm = e.rProm; c.rMin = e.rMin;
            c.flexCumple = e.normaCumple;
            c.textoNorma = e.textoNorma;
        }
        c.flexIndivOk = e.indivOk;

        if (r.abs != null) {
            Uso u = usoRecomendado(r.abs[0], r.grupo != null ? r.grupo[0] : "—", e.cumpleTodo());
            c.usoTitulo = u.titulo; c.usoDetalle = u.detalle;
            c.usoImagenes = u.imagenes; c.usoPies = u.pies;
        }
        return c;
    }

    private String comparacionHtml() {
        List<InformeFinal.Contenido> l = new ArrayList<>();
        for (Registro x : registros) if (x.mezcla != null && evaluar(x).completo) l.add(contenidoInforme(x));
        if (l.size() < 2) return "";
        InformeFinal.Comparacion k = InformeFinal.comparar(l);
        StringBuilder h = new StringBuilder("<br><br><b><font color='" + hx(C_RES) + "'>COMPARACIÓN DE MEZCLAS"
            + "</font></b><br><table cellspacing='0' cellpadding='5' width='" + anchoHtml + "'><tr bgcolor='" + hx(Tema.BARRA) + "'>")
            .append(celda("Mezcla", MUTED, true)).append(celda("Absorción", MUTED, true))
            .append(celda("Contracción", MUTED, true)).append(celda("Flexión R", MUTED, true))
            .append(celda("NTC 919", MUTED, true)).append("</tr>");
        for (int i = 0; i < l.size(); i++) {
            InformeFinal.Contenido c = l.get(i);
            h.append(tr(i))
             .append(celda(c.nombreMezcla, i == k.recomendada ? GREEN : TEXT, true))
             .append(celda(fmt(c.abs[0], 2) + " %", i == k.mejorAbs ? GREEN : TEXT, i == k.mejorAbs))
             .append(celda(fmt(c.cont[0], 2) + " %", i == k.mejorCont ? GREEN : TEXT, i == k.mejorCont))
             .append(celda(fmt(c.rProm, 2) + " MPa", i == k.mejorFlex ? GREEN : TEXT, i == k.mejorFlex))
             .append(celda(c.flexOkTotal() ? "CUMPLE" : "NO CUMPLE", c.flexOkTotal() ? GREEN : RED, true))
             .append("</tr>");
        }
        h.append("</table>");
        if (k.recomendada >= 0) {
            InformeFinal.Contenido r = l.get(k.recomendada);
            h.append("<br><b><font color='").append(hx(GREEN)).append("'>Mezcla recomendada: ")
             .append(esc(r.nombreMezcla)).append(" — ").append(esc(r.usoTitulo)).append("</font></b>");
        }
        return h.toString();
    }

    private static final class Uso {
        final String titulo, detalle;
        final String[] imagenes, pies;
        Uso(String titulo, String detalle, String[] imagenes, String[] pies) {
            this.titulo = titulo; this.detalle = detalle; this.imagenes = imagenes; this.pies = pies;
        }
    }

    private static Uso usoRecomendado(double ev, String grupo, boolean flexionCumple) {
        if (!flexionCumple) {
            return new Uso("Revestimiento de muros interiores (sin tránsito)",
                "La resistencia a la flexión no alcanza el mínimo de la NTC 919 para pisos. "
              + "Úsela como enchape decorativo en muros, cenefas, jardineras o fachadas protegidas, "
              + "donde no recibe cargas de tránsito.",
                new String[]{"muro"},
                new String[]{"Muro · revestimiento decorativo"});
        }
        if (ev <= 3.0) {
            return new Uso("Pisos interiores y exteriores, incluso en clima frío",
                "Absorción baja (grupo " + grupo + ") y flexión conforme: apta para terrazas, patios, "
              + "andenes, baños y cocinas. Con Ev ≤ 3 % tolera mejor la humedad permanente y las heladas.",
                new String[]{"exterior", "interior"},
                new String[]{"Exterior · terrazas y patios", "Interior · pisos residenciales"});
        }
        if (ev <= 10.0) {
            return new Uso("Pisos interiores y exteriores en clima cálido",
                "Absorción media (grupo " + grupo + ") y flexión conforme: apta para salas, corredores y "
              + "cocinas, y para terrazas y patios donde no hay heladas. En exterior conviene sellarla "
              + "para limitar manchas y eflorescencias.",
                new String[]{"exterior", "interior"},
                new String[]{"Exterior · terrazas en clima cálido", "Interior · pisos residenciales"});
        }
        return new Uso("Pisos interiores de tránsito residencial",
            "Absorción alta (grupo " + grupo + ") con flexión conforme: ideal en salas, comedores, cocinas "
          + "y corredores. En exteriores úsela solo bajo cubierta o con sellador, porque absorbe agua "
          + "y se mancha con facilidad.",
            new String[]{"interior"},
            new String[]{"Interior · pisos residenciales"});
    }

    private void actualizarUso(Registro r) {
        filaImgs.removeAll();
        boolean algunaCompleta = false;
        for (Registro x : registros) if (x.mezcla != null && evaluar(x).completo) algunaCompleta = true;
        btnInforme.setVisible(algunaCompleta);
        if (!flexCompleto || r.abs == null) {
            lblUso.setText(html(DIM, "El uso recomendado aparece cuando se hayan ensayado a flexión "
                                   + "todas las probetas de la mezcla."));
            filaImgs.setVisible(false);
        } else {
            Uso u = usoRecomendado(r.abs[0], r.grupo != null ? r.grupo[0] : "—", flexCumple);
            lblUso.setText("<html><div style='width:" + anchoHtml + "px'>"
                + "<b><font color='" + hx(flexCumple ? GREEN : AMBER) + "'>" + esc(u.titulo) + "</font></b><br>"
                + "<font color='" + hx(MUTED) + "'>" + esc(u.detalle) + "</font><br>"
                + "</div></html>");
            for (int i = 0; i < u.imagenes.length; i++) {
                ImagenUso iu = new ImagenUso(u.imagenes[i], u.pies[i]);
                iu.setMinimumSize(new Dimension(160, 120));
                filaImgs.add(iu);
            }
            int nImg = Math.max(1, u.imagenes.length);
            int anchoImg = Math.min(320, (anchoHtml - 12 * (nImg - 1)) / nImg);
            Dimension dImg = new Dimension(anchoImg * nImg + 12 * (nImg - 1), Math.round(anchoImg * 0.66f));
            filaImgs.setPreferredSize(dImg);
            filaImgs.setMaximumSize(dImg);
            filaImgs.setVisible(true);
        }
        filaImgs.revalidate();
        filaImgs.repaint();
    }

    static final class ImagenUso extends JPanel {
        private static final Map<String, Image> CACHE = new HashMap<>();
        private final String clave, pie;

        ImagenUso(String clave, String pie) {
            this.clave = clave;
            this.pie = pie;
            Dimension d = new Dimension(320, 210);
            setPreferredSize(d);
            setMinimumSize(d);
            setMaximumSize(d);
            setOpaque(false);
            setToolTipText(pie);
        }

        static Image foto(String clave) {
            if (CACHE.containsKey(clave)) return CACHE.get(clave);
            Image im = null;
            for (String ext : new String[]{"jpg", "jpeg", "png"}) {
                try {
                    URL u = EnsayosNTC.class.getResource("/geoceramiclab/recursos/uso_" + clave + "." + ext);
                    if (u != null) { im = ImageIO.read(u); break; }
                    File f = new File("recursos", "uso_" + clave + "." + ext);
                    if (f.isFile()) { im = ImageIO.read(f); break; }
                } catch (Exception ignorar) { }
            }
            CACHE.put(clave, im);
            return im;
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            Shape marco = new RoundRectangle2D.Float(0, 0, w - 1, h - 1, 12, 12);
            g.setClip(marco);

            Image im = foto(clave);
            if (im != null) {

                int iw = im.getWidth(null), ih = im.getHeight(null);
                double esc = Math.max((double) w / iw, (double) h / ih);
                int dw = (int) Math.round(iw * esc), dh = (int) Math.round(ih * esc);
                g.drawImage(im, (w - dw) / 2, (h - dh) / 2, dw, dh, null);
            } else {
                pintarEscena(g, clave, w, h);
            }

            g.setPaint(new GradientPaint(0, h - 54, new Color(0, 0, 0, 0), 0, h, new Color(0, 0, 0, 190)));
            g.fillRect(0, h - 54, w, 54);
            g.setPaint(null);
            g.setColor(Color.WHITE);
            g.setFont(Tema.fuente(Font.BOLD, 12));
            g.drawString(pie, 12, h - 12);

            g.setClip(null);
            g.setColor(BORD);
            g.setStroke(new BasicStroke(1.2f));
            g.draw(marco);
            g.dispose();
        }

        private static void pintarEscena(Graphics2D g, String clave, int w, int h) {
            BufferedImage im = Escena.render(clave, Math.max(2, w * 2), Math.max(2, h * 2));
            g.drawImage(im, 0, 0, w, h, null);
        }
    }

    static final class Escena {
        private static final Map<String, BufferedImage> CACHE = new HashMap<>();

        static synchronized BufferedImage render(String clave, int w, int h) {
            String k = clave + "@" + w + "x" + h;
            BufferedImage im = CACHE.get(k);
            if (im == null) {
                im = "muro".equals(clave) ? muro(w, h) : piso(w, h, "exterior".equals(clave));
                CACHE.put(k, im);
            }
            return im;
        }

        private static double hash(int x, int y, int s) {
            int n = x * 374761393 + y * 668265263 + s * 1442695041;
            n = (n ^ (n >>> 13)) * 1274126177;
            n ^= n >>> 16;
            return (n & 0x7fffffff) / (double) 0x7fffffff;
        }
        private static double sv(double t) { return t * t * (3 - 2 * t); }
        private static double valor(double x, double y, int s) {
            int xi = (int) Math.floor(x), yi = (int) Math.floor(y);
            double fx = x - xi, fy = y - yi;
            double a = hash(xi, yi, s), b = hash(xi + 1, yi, s), c = hash(xi, yi + 1, s), d = hash(xi + 1, yi + 1, s);
            double u = sv(fx), v = sv(fy);
            return a + (b - a) * u + (c - a) * v + (a - b - c + d) * u * v;
        }
        private static double fbm(double x, double y, int s, int oct) {
            double t = 0, amp = 0.5, f = 1, norm = 0;
            for (int i = 0; i < oct; i++) { t += amp * valor(x * f, y * f, s + i * 31); norm += amp; f *= 2.03; amp *= 0.5; }
            return t / norm;
        }
        private static double ss(double a, double b, double x) {
            double t = Math.max(0, Math.min(1, (x - a) / (b - a)));
            return t * t * (3 - 2 * t);
        }
        private static int c255(double v) { return (int) Math.max(0, Math.min(255, Math.round(v))); }

        private static double[] baldosa(double U, double V, double ax, boolean trabada) {
            double fila = Math.floor(V);
            double Uo = trabada && ((long) fila & 1L) == 1L ? U + ax / 2 : U;
            int ti = (int) Math.floor(Uo / ax), tj = (int) fila;
            double fu = (Uo / ax - ti), fv = V - tj;
            double gw = 0.035;
            double eu = Math.min(fu, 1 - fu) * ax, ev = Math.min(fv, 1 - fv);
            double e = Math.min(eu, ev);
            if (e < gw) {
                double n = fbm(U * 30, V * 30, 7, 3);
                double k = 0.80 + 0.30 * n;
                double prof = 0.72 + 0.28 * (e / gw);
                return new double[]{158 * k * prof, 148 * k * prof, 134 * k * prof, prof};
            }

            double h1 = hash(ti, tj, 11), h2 = hash(ti, tj, 23), h3 = hash(ti, tj, 37);
            double r = 164 + 24 * (h1 - 0.5), gg = 88 + 14 * (h2 - 0.5), b = 62 + 10 * (h3 - 0.5);
            if (h1 > 0.86) { r -= 22; gg -= 14; b -= 8; }
            if (h2 < 0.12) { r += 14; gg += 14; b += 10; }

            double m = fbm(U * 1.7 + ti * 3.1, V * 1.7 + tj * 2.3, 3, 4) - 0.5;
            double gr = fbm(U * 22, V * 22, 5, 3) - 0.5;
            double f = 1 + 0.22 * m + 0.10 * gr;
            r *= f; gg *= f * (1 + 0.06 * m); b *= f;

            double p = valor(U * 70, V * 70, 9);
            if (p > 0.90) { double q = (p - 0.90) * 6; r -= 55 * q; gg -= 35 * q; b -= 20 * q; }
            double p2 = valor(U * 55 + 9, V * 55, 13);
            if (p2 > 0.93) { double q = (p2 - 0.93) * 8; r += 40 * q; gg += 36 * q; b += 30 * q; }

            double borde = 0.86 + 0.14 * ss(gw, gw + 0.10, e);
            double centro = 1 + 0.05 * (1 - ss(0.0, 0.45, Math.hypot(fu - 0.5, fv - 0.5)));
            return new double[]{r * borde * centro, gg * borde * centro, b * borde * centro, borde};
        }

        private static BufferedImage piso(int w, int h, boolean ext) {
            BufferedImage im = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            int hor = (int) (h * (ext ? 0.40 : 0.44));
            double f = w * 0.95, camH = 2.7, cx = w * 0.5;

            for (int y = 0; y < hor; y++) {
                for (int x = 0; x < w; x++) {
                    double[] c = ext ? cielo(x, y, w, hor) : pared(x, y, w, hor);
                    im.setRGB(x, y, rgb(c));
                }
            }

            double xVent = w * 0.18;
            for (int y = hor; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    double R = 0, G = 0, B = 0;
                    for (int sy = 0; sy < 2; sy++) for (int sx = 0; sx < 2; sx++) {
                        double px = x + 0.25 + sx * 0.5, py = y + 0.25 + sy * 0.5;
                        double dz = Math.max(0.35, py - hor);
                        double Z = camH * f / dz, U = (px - cx) * Z / f;
                        double[] t = baldosa(U * 0.55 + 0.37, Z * 0.55, 1.0, false);
                        double lejos = 0.85 * ss(14, 60, Z);
                        t[0] += (150 - t[0]) * lejos; t[1] += (88 - t[1]) * lejos; t[2] += (66 - t[2]) * lejos;
                        double luz = 0.78 + 0.22 * ss(0, h - hor, py - hor);
                        if (ext) {

                            double sb = ((U * 0.9 + Z * 0.55) % 0.8 + 0.8) % 0.8;
                            double sombra = (Z < 9 && Z > 2.5) ? 1 - 0.32 * ss(0.10, 0.16, sb) * (1 - ss(0.40, 0.46, sb)) : 1;
                            luz *= sombra * 1.06;
                        } else {

                            double lu = ss(-3.4, -2.6, U - 0.35 * Z) * (1 - ss(-1.4, -0.6, U - 0.35 * Z))
                                      * ss(2.6, 3.4, Z) * (1 - ss(6.5, 8.0, Z));
                            luz *= 0.86 + 0.62 * lu;
                        }

                        double spec = Math.exp(-Math.pow((px - (ext ? w * 0.62 : xVent + w * 0.10)) / (w * 0.10), 2))
                                    * ss(hor, hor + (h - hor) * 0.5, py) * (1 - ss(hor + (h - hor) * 0.5, h, py)) * 38 * t[3];
                        double niebla = 0.55 * (1 - Math.exp(-Z / (ext ? 90 : 70)));
                        double[] fondo = ext ? new double[]{196, 170, 150} : new double[]{176, 132, 108};
                        double ao = 1 - 0.38 * Math.exp(-(py - hor) / (h * 0.012));
                        double r = (t[0] * luz + spec) * ao, gg = (t[1] * luz + spec * 0.92) * ao, b = (t[2] * luz + spec * 0.80) * ao;
                        R += r + (fondo[0] - r) * niebla; G += gg + (fondo[1] - gg) * niebla; B += b + (fondo[2] - b) * niebla;
                    }
                    im.setRGB(x, y, rgb(new double[]{R / 4, G / 4, B / 4}));
                }
            }
            if (ext) baranda(im, w, hor);
            else zocalo(im, w, hor, xVent);
            acabado(im, ext ? 41 : 43, true);
            return im;
        }

        private static double[] cielo(int x, int y, int w, int hor) {
            double t = (double) y / hor;
            double r = 112 + 120 * t, g = 156 + 78 * t, b = 214 + 20 * t;
            double nub = ss(0.52, 0.80, fbm(x * 0.006, y * 0.014, 21, 5));
            r += (246 - r) * nub * 0.85; g += (244 - g) * nub * 0.85; b += (240 - b) * nub * 0.85;

            double alto = hor * (0.30 + 0.22 * fbm(x * 0.012, 3, 4, 4));
            if (hor - y < alto) {
                double n = fbm(x * 0.05, y * 0.05, 8, 4);
                double k = 0.55 + 0.6 * n;
                double lejos = 0.35 * (1 - (hor - y) / alto);
                r = (52 * k) * (1 - lejos) + 150 * lejos; g = (82 * k) * (1 - lejos) + 170 * lejos; b = (46 * k) * (1 - lejos) + 170 * lejos;
            }
            return new double[]{r, g, b};
        }

        private static double[] pared(int x, int y, int w, int hor) {
            double n = fbm(x * 0.02, y * 0.02, 17, 5) - 0.5, fino = fbm(x * 0.4, y * 0.4, 19, 2) - 0.5;
            double cxv = w * 0.18, cyv = hor * 0.49;
            double brillo = Math.exp(-(Math.pow((x - cxv) / (w * 0.22), 2) + Math.pow((y - cyv) / (hor * 0.6), 2)));
            double luz = 0.84 + 0.16 * brillo + 0.06 * (1 - ss(0, w, x)) - 0.08 * (1 - (double) y / hor);
            double r = (238 + 10 * n + 4 * fino) * luz, g = (229 + 10 * n + 4 * fino) * luz, b = (214 + 9 * n + 4 * fino) * luz;

            int vx0 = (int) (w * 0.06), vx1 = (int) (w * 0.30), vy0 = (int) (hor * 0.18), vy1 = (int) (hor * 0.80);
            if (x >= vx0 && x <= vx1 && y >= vy0 && y <= vy1) {
                int marco = Math.max(3, w / 140);
                boolean esMarco = x - vx0 < marco || vx1 - x < marco || y - vy0 < marco || vy1 - y < marco
                                 || Math.abs(x - (vx0 + vx1) / 2) < marco / 2 + 1;
                if (esMarco) return new double[]{246, 244, 238};
                double t = (double) (y - vy0) / (vy1 - vy0);
                double nv = fbm(x * 0.01, y * 0.01, 29, 4);
                double verde = ss(0.45, 0.75, t) * ss(0.45, 0.7, nv) * 0.35;
                return new double[]{248 - 70 * verde, 250 - 40 * verde, 252 - 80 * verde};
            }

            int px0 = (int) (w * 0.72), px1 = (int) (w * 0.86), py0 = (int) (hor * 0.20);
            if (x >= px0 && x <= px1 && y >= py0) {
                double veta = Math.sin(x * 0.9 + 6 * fbm(x * 0.05, y * 0.01, 41, 3)) * 0.5 + 0.5;
                double k = 0.85 + 0.15 * veta;
                int mb = Math.max(4, w / 120);
                if (x - px0 < mb || px1 - x < mb || y - py0 < mb) return new double[]{232, 226, 214};
                double borde = (x - px0 < mb + 3 || px1 - x < mb + 3 || y - py0 < mb + 3) ? 0.72 : 1;
                int mx = px1 - mb - (px1 - px0) / 7, my = (int) (hor * 0.62);
                if (Math.abs(x - mx) < Math.max(2, w / 260) && Math.abs(y - my) < Math.max(5, w / 90))
                    return new double[]{200, 196, 186};
                return new double[]{112 * k * borde, 72 * k * borde, 48 * k * borde};
            }
            return new double[]{r, g, b};
        }

        private static void zocalo(BufferedImage im, int w, int hor, double xVent) {
            int alto = Math.max(4, im.getHeight() / 34);
            for (int y = hor - alto; y < hor; y++)
                for (int x = 0; x < w; x++) {
                    double[] t = baldosa(x / (double) (alto * 4), (y - hor + alto) / (double) alto * 0.92, 1.0, false);
                    double k = 0.82 + 0.18 * ((double) (y - hor + alto) / alto);
                    im.setRGB(x, y, rgb(new double[]{t[0] * k, t[1] * k, t[2] * k}));
                }
        }

        private static void baranda(BufferedImage im, int w, int hor) {
            int top = (int) (hor * 0.80), grosor = Math.max(5, im.getHeight() / 55);
            for (int y = top; y < hor; y++)
                for (int x = 0; x < w; x++) {
                    double n = 0.6 * fbm(x * 0.05, y * 0.05, 51, 3) + 0.4 * fbm(x * 0.5, y * 0.5, 53, 2);
                    boolean coping = y < top + grosor;
                    double[] c = coping ? new double[]{176 * (0.9 + 0.2 * n), 92 * (0.9 + 0.2 * n), 60}
                                        : new double[]{232 * (0.92 + 0.1 * n), 226 * (0.92 + 0.1 * n), 214 * (0.92 + 0.1 * n)};
                    if (!coping) {
                        double sombra = 1 - 0.18 * (1 - (double) (y - top) / (hor - top));
                        c[0] *= sombra; c[1] *= sombra; c[2] *= sombra;
                    }
                    im.setRGB(x, y, rgb(c));
                }
        }

        private static BufferedImage muro(int w, int h) {
            BufferedImage im = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            int suelo = (int) (h * 0.84);
            double tam = h / 6.2;
            for (int y = 0; y < suelo; y++)
                for (int x = 0; x < w; x++) {
                    double U = x / tam, V = y / tam;
                    double[] t = baldosa(U, V, 2.0, true);

                    double[] tx = baldosa(U + 0.02, V, 2.0, true), ty = baldosa(U, V + 0.02, 2.0, true);
                    double dx = tx[3] - t[3], dy = ty[3] - t[3];
                    double rel = 1 - 1.6 * dx - 1.2 * dy;
                    double luz = (0.80 + 0.30 * (1 - ss(0, w, x))) * rel;

                    double pl = fbm(x * 0.006, y * 0.006, 61, 3);
                    luz *= 1 - 0.20 * ss(0.48, 0.66, pl) * ss(w * 0.55, w * 0.75, x);
                    im.setRGB(x, y, rgb(new double[]{t[0] * luz, t[1] * luz, t[2] * luz}));
                }
            for (int y = suelo; y < h; y++)
                for (int x = 0; x < w; x++) {
                    double n = fbm(x * 0.03, y * 0.08, 71, 4);
                    double k = (0.62 + 0.25 * n) * (1 - 0.35 * Math.exp(-(y - suelo) / (h * 0.02)));
                    im.setRGB(x, y, rgb(new double[]{150 * k, 144 * k, 136 * k}));
                }
            acabado(im, 47, false);
            return im;
        }

        private static void acabado(BufferedImage im, int semilla, boolean calido) {
            int w = im.getWidth(), h = im.getHeight();
            Random rnd = new Random(semilla);
            for (int y = 0; y < h; y++)
                for (int x = 0; x < w; x++) {
                    int c = im.getRGB(x, y);
                    double r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
                    double dx = (x - w / 2.0) / (w / 2.0), dy = (y - h / 2.0) / (h / 2.0);
                    double vig = 1 - 0.22 * Math.pow(Math.min(1, (dx * dx + dy * dy) / 1.6), 1.3);
                    double grano = (rnd.nextGaussian()) * 3.2;
                    if (calido) { r *= 1.03; b *= 0.97; }
                    im.setRGB(x, y, rgb(new double[]{r * vig + grano, g * vig + grano, b * vig + grano}));
                }
        }

        private static int rgb(double[] c) {
            return (c255(c[0]) << 16) | (c255(c[1]) << 8) | c255(c[2]);
        }
    }

    private class TituloMapa extends JPanel {
        TituloMapa() {
            setOpaque(false);
            setPreferredSize(new Dimension(NODO_W, 84));
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int H = getHeight();
            int bw = PILL_W + 24, bh = 54;
            int bx = SPINE_X - bw / 2, by = 10;

            g.setColor(C_PROB);
            g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(SPINE_X, by + bh, SPINE_X, H);

            g.setColor(new Color(0, 0, 0, 80));
            g.fillRoundRect(bx + 3, by + 4, bw, bh, 10, 10);
            g.setPaint(new GradientPaint(0, by, C_TIT, 0, by + bh, C_TIT.darker()));
            g.fillRoundRect(bx, by, bw, bh, 10, 10);
            g.setPaint(null);
            g.setColor(C_TIT.brighter());
            g.setStroke(new BasicStroke(1.6f));
            g.drawRoundRect(bx, by, bw, bh, 10, 10);

            g.setColor(C_PROB);
            g.fillOval(SPINE_X - 4, by + bh - 4, 8, 8);

            g.setColor(Color.WHITE);
            g.setFont(Tema.fuente(Font.BOLD, 13));
            centrado(g, "Ensayos de laboratorio", SPINE_X, by + 24);
            g.setColor(new Color(255, 226, 232));
            g.setFont(Tema.fuente(Font.PLAIN, 10));
            centrado(g, "NTC 919 · ISO 13006", SPINE_X, by + 41);
            g.dispose();
        }
    }

    private class NodoMapa extends JPanel {
        final int num;
        final String l1, l2;
        final Color col;
        final JComponent fila;

        NodoMapa(int num, String l1, String l2, Color col, JComponent fila) {
            this.num = num; this.l1 = l1; this.l2 = l2; this.col = col; this.fila = fila;
            setOpaque(false);
            setPreferredSize(new Dimension(NODO_W, PILL_H + 30));
        }

        private boolean hayMasAbajo() {
            int i = nodos.indexOf(this);
            for (int j = i + 1; j < nodos.size(); j++)
                if (nodos.get(j).fila.isVisible()) return true;
            return false;
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int W = getWidth(), H = getHeight();
            int cy = H / 2;
            int px = SPINE_X - PILL_W / 2, py = cy - PILL_H / 2;

            g.setColor(col);
            g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            g.drawLine(SPINE_X, 0, SPINE_X, py - 9);
            flecha(g, SPINE_X, py - 1, 0, 1);

            if (hayMasAbajo()) {
                Color sig = nodos.get(nodos.indexOf(this) + 1).col;
                g.setPaint(new GradientPaint(0, py + PILL_H, col, 0, H, sig));
                g.drawLine(SPINE_X, py + PILL_H, SPINE_X, H);
                g.setPaint(null);
            }

            g.setColor(col);
            g.drawLine(px + PILL_W, cy, W - 10, cy);
            flecha(g, W - 2, cy, 1, 0);

            g.setColor(new Color(0, 0, 0, 80));
            g.fillRoundRect(px + 3, py + 4, PILL_W, PILL_H, 16, 16);
            g.setColor(mezclar(col, BG2, 0.28));
            g.fillRoundRect(px, py, PILL_W, PILL_H, 16, 16);
            g.setColor(col);
            g.setStroke(new BasicStroke(1.8f));
            g.drawRoundRect(px, py, PILL_W, PILL_H, 16, 16);

            int d = 24, nx = px + 10, ny = cy - d / 2;
            g.fillOval(nx, ny, d, d);
            g.setColor(BG);
            g.setFont(Tema.fuente(Font.BOLD, 12));
            centrado(g, String.valueOf(num), nx + d / 2, cy + 4);

            int tx0 = nx + d + 6, tw = px + PILL_W - 8 - tx0, tcx = tx0 + tw / 2;
            g.setColor(TEXT);
            g.setFont(Tema.fuente(Font.BOLD, 12));
            centrado(g, l1, tcx, cy - 2);
            g.setColor(MUTED);
            g.setFont(Tema.fuente(Font.PLAIN, 10));
            centrado(g, l2, tcx, cy + 12);

            g.dispose();
        }
    }

    private static void flecha(Graphics2D g, int x, int y, int dx, int dy) {
        int largo = 9, medio = 5;
        int bx = x - dx * largo, by = y - dy * largo;
        int nx = -dy, ny = dx;
        g.fillPolygon(new int[]{x, bx + nx * medio, bx - nx * medio},
                      new int[]{y, by + ny * medio, by - ny * medio}, 3);
    }

    private static void centrado(Graphics2D g, String s, int cx, int baseline) {
        int w = g.getFontMetrics().stringWidth(s);
        g.drawString(s, cx - w / 2, baseline);
    }

    private static Color mezclar(Color a, Color b, double fa) {
        double fb = 1 - fa;
        return new Color((int) (a.getRed() * fa + b.getRed() * fb),
                         (int) (a.getGreen() * fa + b.getGreen() * fb),
                         (int) (a.getBlue() * fa + b.getBlue() * fb));
    }

    private class EnsayoImagen extends JPanel {
        private final int tipo;

        EnsayoImagen(int tipo) {
            this.tipo = tipo;
            setBackground(BG2);
            setOpaque(true);
            setBorder(BorderFactory.createLineBorder(BORD));
            Dimension d = new Dimension(230, 165);
            setPreferredSize(d);
            setMinimumSize(d);
            setMaximumSize(d);
        }

        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            int W = getWidth(), H = getHeight();
            g.setColor(BG2);
            g.fillRect(0, 0, W, H);

            switch (tipo) {
                case T_FLEXION:     pintarFlexion(g, W, H);     break;
                case T_ABSORCION:   pintarAbsorcion(g, W, H);   break;
                case T_CONTRACCION: pintarContraccion(g, W, H); break;
                default:            pintarDensidad(g, W, H);    break;
            }
        }
    }

    private void pintarFlexion(Graphics2D g, int W, int H) {
        int bx = 10, by = 14, bw = W - 20, bh = H - 24;
        int floorY = by + bh - 24;

        g.setColor(new Color(50, 58, 82));
        g.setStroke(new BasicStroke(1f));
        g.drawLine(bx + 8, floorY + 10, bx + bw - 8, floorY + 10);

        int cx = bx + bw / 2;

        double LcmT = (baldosin != null && baldosin.largo > 0) ? baldosin.largo : 28.5;
        double AcmT = (baldosin != null && baldosin.ancho > 0) ? baldosin.ancho : 14.0;
        double EcmT = 2.0;
        if (baldosin != null) {
            double e = Math.max(0, baldosin.espesor);
            double n = Math.max(0, baldosin.altoNervio);
            if (e + n > 0) EcmT = e + n;
        }
        double luzCm = luzEnsayoMm() / 10.0;

        final double COS_ISO = 0.8944, SIN_ISO = 0.4472;
        double anchoProyX = AcmT * COS_ISO;
        double anchoProyY = AcmT * SIN_ISO;

        double sW = (bw * 0.80) / (LcmT + anchoProyX);
        double sH = (bh - 50) / Math.max(0.5, EcmT * 1.8 + anchoProyY);
        double s = Math.max(1.2, Math.min(9.0, Math.min(sW, sH)));

        int tileLen = Math.max(24, (int) Math.round(LcmT * s));
        int esp     = Math.max(2, (int) Math.round(EcmT * s));
        int depthX  = (int) Math.round(anchoProyX * s);
        cx -= depthX / 2;
        int tileX0 = cx - tileLen / 2;

        int span = (int) Math.round(luzCm * s);
        span = Math.max(20, Math.min(span, (int) (tileLen * 0.94)));
        int axL = cx - span / 2;
        int axR = cx + span / 2;

        double diamRodCm = Math.max(0.8, Math.min(2.0, EcmT * 0.9));
        int rA = Math.max(3, (int) Math.round(diamRodCm * s / 2));

        int cyTile = floorY - rA;
        int ay = floorY;

        double vLenT = tileLen * ratioAnchoBaldosin();
        int dxR = (int) Math.round(vLenT * COS_ISO);
        int dyR = -(int) Math.round(vLenT * SIN_ISO);

        cilindro(g, axL, ay, rA, dxR, dyR);
        cilindro(g, axR, ay, rA, dxR, dyR);

        g.setColor(new Color(60, 68, 90));
        g.fillRect(axL - 2, ay + rA - 1, 4, floorY + 10 - (ay + rA - 1));
        g.fillRect(axR - 2, ay + rA - 1, 4, floorY + 10 - (ay + rA - 1));

        drawBaldosinAcostada(g, tileX0, cyTile, tileLen, 0, 1.0);

        int rP = Math.max(3, (int) Math.round(rA * 0.88));
        int yP = cyTile - esp - rP;

        double overP = 0.14;
        double vLenP = vLenT * (1 + 2 * overP);
        int dxP = (int) Math.round(vLenP * COS_ISO);
        int dyP = -(int) Math.round(vLenP * SIN_ISO);
        int offX = (int) Math.round(vLenT * overP * COS_ISO);
        int offY = (int) Math.round(vLenT * overP * SIN_ISO);
        int pcx = cx - offX;
        int pcy = yP + offY;
        int mxP = pcx + dxP / 2;
        int myP = pcy + dyP / 2;

        int headH     = Math.max(6, Math.min(12, tileLen / 10));
        int headHalfW = Math.max(4, headH / 2);
        int headTipY  = myP - rP - 3;
        int headBaseY = headTipY - headH;
        int stemTop   = by + 4;
        Color colVastago = new Color(55, 60, 75);

        g.setColor(colVastago);
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if (headBaseY - 2 > stemTop) g.drawLine(mxP, stemTop, mxP, headBaseY - 2);

        cilindro(g, pcx, pcy, rP, dxP, dyP);

        int[] headX = {mxP - headHalfW, mxP + headHalfW, mxP};
        int[] headY = {headBaseY, headBaseY, headTipY};
        g.setColor(colVastago);
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(mxP, headBaseY, mxP, headTipY);
        g.fillPolygon(headX, headY, 3);
        g.setColor(colVastago.darker());
        g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawPolygon(headX, headY, 3);
    }

    private void cilindro(Graphics2D g, int cx, int cy, int r, int dx, int dy) {
        g.setColor(new Color(0, 0, 0, 45));
        g.fillOval(cx - r + 3, cy + 5, r * 2 + 8, 6);
        g.setColor(new Color(210, 215, 225));
        g.fillOval(cx - r + dx, cy - r + dy, r * 2, r * 2);
        g.setColor(new Color(130, 136, 150));
        g.setStroke(new BasicStroke(1f));
        g.drawOval(cx - r + dx, cy - r + dy, r * 2, r * 2);
        double len = Math.hypot(dx, dy);
        double expand = r + 1.0;
        double perpX = len > 0.001 ? (-dy / len) * expand : 0;
        double perpY = len > 0.001 ? ( dx / len) * expand : -expand;
        int[] xB = {(int) Math.round(cx + perpX), (int) Math.round(cx + dx + perpX),
                    (int) Math.round(cx + dx - perpX), (int) Math.round(cx - perpX)};
        int[] yB = {(int) Math.round(cy + perpY), (int) Math.round(cy + dy + perpY),
                    (int) Math.round(cy + dy - perpY), (int) Math.round(cy - perpY)};
        g.setPaint(new GradientPaint(cx, cy - r, new Color(210, 215, 225),
                                     cx, cy + r, new Color(130, 136, 150)));
        g.fillPolygon(xB, yB, 4);
        g.setPaint(null);
        g.setPaint(new GradientPaint(cx - r, cy - r, new Color(210, 215, 225),
                                     cx + r, cy + r, new Color(130, 136, 150)));
        g.fillOval(cx - r, cy - r, r * 2, r * 2);
        g.setPaint(null);
        g.setColor(new Color(90, 95, 110));
        g.setStroke(new BasicStroke(1.2f));
        g.drawOval(cx - r, cy - r, r * 2, r * 2);
        g.setColor(new Color(255, 255, 255, 90));
        g.fillOval(cx - r + 2, cy - r + 2, Math.max(2, (int) (r * 1.1)), Math.max(1, (int) (r * 0.5)));
    }

    private void pintarAbsorcion(Graphics2D g, int W, int H) {
        int tx = 14, ty = 18, tw = W - 28, th = H - 34;

        g.setColor(new Color(28, 36, 52));
        g.fillRoundRect(tx, ty, tw, th, 6, 6);
        g.setStroke(new BasicStroke(2.5f));
        g.setColor(new Color(90, 120, 160, 180));
        g.drawRoundRect(tx, ty, tw, th, 6, 6);
        g.setColor(new Color(140, 180, 220, 35));
        g.fillRect(tx + 2, ty + 4, 5, th - 8);

        double nivelFrac = 0.70;
        int aguaH = (int) (th * nivelFrac);
        int aguaTop = ty + th - aguaH;

        g.setPaint(new GradientPaint(tx, aguaTop, new Color(40, 130, 210, 140),
                                     tx, ty + th, new Color(20, 70, 140, 200)));
        g.fillRect(tx + 2, aguaTop, tw - 3, aguaH - 1);
        g.setPaint(null);

        g.setStroke(new BasicStroke(1.5f));
        g.setColor(new Color(160, 220, 255, 160));
        Path2D wave = new Path2D.Double();
        wave.moveTo(tx + 2, aguaTop + 2);
        int segs = 16;
        for (int i = 0; i <= segs; i++) {
            double t = (double) i / segs;
            double x = tx + 2 + t * (tw - 4);
            double y = aguaTop + 2 + Math.sin(t * Math.PI * 3) * 2.2 + Math.sin(t * Math.PI * 7) * 1.0;
            wave.lineTo(x, y);
        }
        g.draw(wave);
        g.setColor(new Color(200, 240, 255, 50));
        g.fillRect(tx + 2, aguaTop, tw - 3, 5);

        int scaleTile = fitScalePx((int) (tw * 0.62), (int) (th * 0.40));
        drawBaldosinIso(g, tx + tw / 2, aguaTop + aguaH / 2, scaleTile, 0.85);

        Random rnd = new Random(42);
        for (int i = 0; i < 10; i++) {
            int px = tx + 10 + rnd.nextInt(Math.max(1, tw - 20));
            int py = aguaTop + 6 + rnd.nextInt(Math.max(1, aguaH - 14));
            int rr = 2 + rnd.nextInt(3);
            g.setColor(new Color(180, 230, 255, 100));
            g.fillOval(px - rr, py - rr, rr * 2, rr * 2);
            g.setColor(new Color(220, 245, 255, 200));
            g.setStroke(new BasicStroke(1f));
            g.drawOval(px - rr, py - rr, rr * 2, rr * 2);
            g.setColor(new Color(255, 255, 255, 180));
            g.fillOval(px - rr / 2, py - rr / 2, Math.max(1, rr / 2), Math.max(1, rr / 2));
        }
    }

    private void pintarContraccion(Graphics2D g, int W, int H) {
        int fx = 15, fy = 18, fw = W - 30, fh = H - 34;

        g.setPaint(new GradientPaint(fx, fy, new Color(96, 44, 30),
                                     fx, fy + fh, new Color(52, 28, 24)));
        g.fillRoundRect(fx, fy, fw, fh, 6, 6);
        g.setPaint(null);
        g.setColor(new Color(120, 70, 50));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(fx, fy, fw, fh, 6, 6);

        g.setColor(new Color(230, 120, 50, 110));
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int lado = 0; lado < 2; lado++) {
            int xr = (lado == 0) ? fx + 8 : fx + fw - 8;
            Path2D coil = new Path2D.Double();
            coil.moveTo(xr, fy + 10);
            for (int i = 1; i <= 10; i++) {
                double t = i / 10.0;
                coil.lineTo(xr + ((i % 2 == 0) ? 4 : -4) * (lado == 0 ? 1 : -1),
                            fy + 10 + t * (fh - 20));
            }
            g.draw(coil);
        }

        Registro r = registros.isEmpty() ? null : actual();
        double contrac = (r != null && r.contOk && r.cont != null) ? r.cont[0] : 8.0;
        contrac = clamp(contrac, 0, 30);
        double factor = 1.0 - contrac / 100.0;

        int scale0 = fitScalePx((int) (fw * 0.62), (int) (fh * 0.50));
        int cxF = fx + fw / 2, cyF = fy + fh / 2;

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
        drawBaldosinIso(g, cxF, cyF, scale0, 0, 1.0);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

        drawBaldosinIso(g, cxF, cyF, scale0, 0.30, factor);

        int scale = (int) (scale0 * factor);
        g.setColor(ACCENT);
        g.setStroke(new BasicStroke(1.5f));
        int midY = cyF;
        g.drawLine(fx + 10, midY, cxF - scale / 2 - 4, midY);
        g.drawLine(cxF + scale / 2 + 4, midY, fx + fw - 10, midY);
        g.fillPolygon(new int[]{cxF - scale / 2 - 4, cxF - scale / 2 - 10, cxF - scale / 2 - 10},
                      new int[]{midY, midY - 3, midY + 3}, 3);
        g.fillPolygon(new int[]{cxF + scale / 2 + 4, cxF + scale / 2 + 10, cxF + scale / 2 + 10},
                      new int[]{midY, midY - 3, midY + 3}, 3);
    }

    private void pintarDensidad(Graphics2D g, int W, int H) {
        int bx = 10, by = 14, bw = W - 20, bh = H - 24;
        int cx = bx + bw / 2;
        int baseY = by + bh - 14;

        g.setColor(new Color(80, 90, 110));
        g.fillRoundRect(cx - 46, baseY - 8, 92, 10, 4, 4);
        g.setColor(new Color(60, 68, 86));
        g.drawRoundRect(cx - 46, baseY - 8, 92, 10, 4, 4);

        g.setColor(new Color(16, 26, 22));
        g.fillRoundRect(cx + 8, baseY - 6, 32, 6, 3, 3);
        g.setColor(new java.awt.Color(76, 183, 130, 120));
        g.fillRoundRect(cx + 11, baseY - 5, 18, 4, 2, 2);

        g.setColor(new Color(80, 90, 110));
        g.fillRect(cx - 3, baseY - 32, 6, 25);

        g.setColor(new Color(112, 122, 142));
        g.fillOval(cx - 34, baseY - 40, 68, 15);
        g.setColor(new Color(70, 80, 100));
        g.setStroke(new BasicStroke(1.4f));
        g.drawOval(cx - 34, baseY - 40, 68, 15);
        g.setColor(new Color(255, 255, 255, 55));
        g.fillOval(cx - 26, baseY - 38, 26, 5);

        int scaleTile = fitScalePx((int) (bw * 0.55), baseY - by - 52);
        int altoTile  = altoIsoPx(scaleTile);
        drawBaldosinIso(g, cx, baseY - 36 - altoTile / 2, scaleTile, 0.30);
    }

    private double ratioAnchoBaldosin() {
        double Lcm = baldosin != null && baldosin.largo > 0 ? baldosin.largo : 28.5;
        double Acm = baldosin != null && baldosin.ancho > 0 ? baldosin.ancho : 14.0;
        return Math.max(0.02, Math.min(4.0, Acm / Lcm));
    }

    private double ratioAltoBaldosin() {
        double Lcm = baldosin != null && baldosin.largo > 0 ? baldosin.largo : 28.5;
        double Ecm = 1.2, Ncm = 0.8;
        if (baldosin != null) {
            double e = Math.max(0, baldosin.espesor);
            double n = Math.max(0, baldosin.altoNervio);
            if (e + n > 0) { Ecm = e; Ncm = n; }
        }
        return Math.max(0.005, Math.min(0.9, (Ecm + Ncm) / Lcm));
    }

    private int altoIsoPx(int scalePx) {
        return Math.max(12, (int) Math.round(scalePx * ratioAnchoBaldosin()));
    }

    private int thicknessPx(int scalePx) {
        return (int) Math.max(2, Math.round(scalePx * ratioAltoBaldosin()));
    }

    private void drawBaldosinAcostada(Graphics2D g, int cx, int cy, int scalePx, double wet, double shrink) {
        double Lcm = baldosin != null && baldosin.largo > 0 ? baldosin.largo : 28.5;
        double Acm = baldosin != null && baldosin.ancho > 0 ? baldosin.ancho : 14.0;
        double ratioAncho = ratioAnchoBaldosin();

        final double COS_A = 0.8944, SIN_A = 0.4472;
        double scale = scalePx * shrink;
        double uLen = scale;
        double vLen = scale * ratioAncho;
        double hz   = thicknessPx((int) scale);

        final double ux = uLen, uy = 0;
        final double vx = vLen * COS_A, vy = -vLen * SIN_A;

        int r  = (int) (198 - wet * 50);
        int gr = (int) (124 - wet * 34);
        int b  = (int) (76  - wet * 20);

        Color top    = new Color(clamp255(r + 18), clamp255(gr + 14), clamp255(b + 8));
        Color topLo  = new Color(clamp255(r - 26), clamp255(gr - 22), clamp255(b - 14));
        Color sideR  = new Color(clamp255(r - 22), clamp255(gr - 16), clamp255(b - 10));
        Color sideRLo= new Color(clamp255(r - 40), clamp255(gr - 30), clamp255(b - 18));
        Color sideF  = new Color(clamp255(r - 46), clamp255(gr - 32), clamp255(b - 20));
        Color sideFLo= new Color(clamp255(r - 66), clamp255(gr - 46), clamp255(b - 28));
        Color edge   = new Color(clamp255(r - 70), clamp255(gr - 48), clamp255(b - 30));

        final int fcx = cx, fcy = cy;
        java.util.function.BiFunction<Double,Double,int[]> P = (uu, vv) -> new int[]{
            (int) Math.round(fcx + uu * ux + vv * vx),
            (int) Math.round(fcy + uu * uy + vv * vy - hz)
        };
        java.util.function.BiFunction<Double,Double,int[]> Pb = (uu, vv) -> new int[]{
            (int) Math.round(fcx + uu * ux + vv * vx),
            (int) Math.round(fcy + uu * uy + vv * vy)
        };

        int[] t00 = P.apply(0.0, 0.0), t10 = P.apply(1.0, 0.0);
        int[] t11 = P.apply(1.0, 1.0), t01 = P.apply(0.0, 1.0);
        int[] b00 = Pb.apply(0.0, 0.0), b10 = Pb.apply(1.0, 0.0);
        int[] b11 = Pb.apply(1.0, 1.0);

        Object oldAA = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int shCx = (b00[0] + b10[0]) / 2;
        int shCy = b00[1] + 3;
        int shHalfW = (int) (uLen * 0.5);
        g.setPaint(new RadialGradientPaint(
            new Point(shCx, shCy), Math.max(8, shHalfW),
            new float[]{0f, 1f},
            new Color[]{new Color(0, 0, 0, 90), new Color(0, 0, 0, 0)}));
        g.fillOval(shCx - shHalfW, shCy - 4, shHalfW * 2, 8);

        g.setPaint(new GradientPaint(0, t00[1], sideF, 0, b00[1], sideFLo));
        g.fillPolygon(new int[]{t00[0], t10[0], b10[0], b00[0]},
                      new int[]{t00[1], t10[1], b10[1], b00[1]}, 4);

        g.setPaint(new GradientPaint(0, t10[1], sideR, 0, b10[1], sideRLo));
        g.fillPolygon(new int[]{t10[0], t11[0], b11[0], b10[0]},
                      new int[]{t10[1], t11[1], b11[1], b10[1]}, 4);

        g.setPaint(new GradientPaint(t00[0], t00[1], top, t01[0], t01[1], topLo));
        Polygon topPoly = new Polygon(new int[]{t00[0], t10[0], t11[0], t01[0]},
                                      new int[]{t00[1], t10[1], t11[1], t01[1]}, 4);
        g.fillPolygon(topPoly);
        g.setPaint(null);

        Shape oldClip = g.getClip();
        g.setClip(topPoly);
        Random grain = new Random(Double.doubleToLongBits(Lcm * 31 + Acm * 17 + scalePx));
        int nSpeck = Math.max(8, (int) (uLen * vLen / 340));
        for (int i = 0; i < nSpeck; i++) {
            int[] p = P.apply(grain.nextDouble(), grain.nextDouble());
            int a = 8 + grain.nextInt(10);
            g.setColor(grain.nextBoolean() ? new Color(0, 0, 0, a) : new Color(255, 250, 235, a));
            g.fillOval(p[0] - 1, p[1] - 1, 2, 2);
        }
        g.setPaint(new GradientPaint(
            t00[0], t00[1], new Color(255, 255, 255, 40),
            t00[0], t00[1] + (t01[1] - t00[1]) * 0.5f + (int) (hz * 2), new Color(255, 255, 255, 0)));
        g.fillPolygon(topPoly);
        g.setPaint(null);
        g.setClip(oldClip);

        g.setColor(edge);
        g.setStroke(new BasicStroke(1.1f));
        g.drawPolygon(topPoly);
        g.drawLine(t00[0], t00[1], b00[0], b00[1]);
        g.drawLine(t10[0], t10[1], b10[0], b10[1]);
        g.drawLine(t11[0], t11[1], b11[0], b11[1]);
        g.drawLine(b00[0], b00[1], b10[0], b10[1]);
        g.drawLine(b10[0], b10[1], b11[0], b11[1]);

        g.setColor(new Color(255, 255, 255, 65));
        g.setStroke(new BasicStroke(0.9f));
        g.drawLine(t00[0], t00[1], t10[0], t10[1]);

        if (wet > 0.15) {
            g.setColor(new Color(35, 95, 160, (int) (35 + wet * 75)));
            g.fillPolygon(topPoly);
        }

        if (oldAA != null) g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
    }

    private static final double PX_POR_CM = 4.4;

    private int fitScalePx(int availW, int availH) {
        double Lcm = baldosin != null && baldosin.largo > 0 ? baldosin.largo : 28.5;
        double ratioAncho = ratioAnchoBaldosin();
        double natural = Lcm * PX_POR_CM;
        double capW = Math.max(20, availW);
        double capH = Math.max(20, availH) / ratioAncho;
        double cap = Math.min(capW, capH);
        return (int) Math.max(24, Math.min(natural, cap));
    }

    private void drawBaldosinIso(Graphics2D g, int cx, int cy, int scalePx, double wet) {
        drawBaldosinIso(g, cx, cy, scalePx, wet, 1.0);
    }

    private void drawBaldosinIso(Graphics2D g, int cx, int cy, int scalePx, double wet, double shrink) {
        double Lcm = baldosin != null && baldosin.largo > 0 ? baldosin.largo : 28.5;
        double Acm = baldosin != null && baldosin.ancho > 0 ? baldosin.ancho : 14.0;
        double ratioAncho = ratioAnchoBaldosin();
        int nRibs = baldosin != null
            ? Math.max(3, Math.min(16, (int) Math.round(Acm / 1.3)))
            : 9;

        int w = Math.max(18, (int) Math.round(scalePx * shrink));
        int h = Math.max(12, (int) Math.round(w * ratioAncho));
        int x0 = cx - w / 2;
        int y0 = cy - h / 2;
        int arc = Math.max(3, Math.min(8, h / 6));

        int r  = (int) (176 - wet * 46);
        int gr = (int) (108 - wet * 30);
        int b  = (int) (58  - wet * 18);

        Color top   = new Color(clamp255(r + 26), clamp255(gr + 19), clamp255(b + 12));
        Color topLo = new Color(clamp255(r - 14), clamp255(gr - 16), clamp255(b - 10));
        Color ribHi = new Color(clamp255(r + 40), clamp255(gr + 28), clamp255(b + 16));
        Color ribLo = new Color(clamp255(r - 4),  clamp255(gr - 4),  clamp255(b - 2));
        Color edge  = new Color(clamp255(r - 62), clamp255(gr - 42), clamp255(b - 28));
        Color edgeBottom = new Color(clamp255(r - 44), clamp255(gr - 30), clamp255(b - 18));

        Object oldAA = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setPaint(new RadialGradientPaint(
            new Point(cx, y0 + h + 4), Math.max(10, w / 2),
            new float[]{0f, 1f},
            new Color[]{new Color(0, 0, 0, 80), new Color(0, 0, 0, 0)}));
        g.fillOval(x0, y0 + h - 2, w, Math.max(6, h / 5));
        g.setPaint(null);

        g.setPaint(new GradientPaint(0, y0, top, 0, y0 + h, topLo));
        g.fillRoundRect(x0, y0, w, h, arc, arc);
        g.setPaint(null);

        Shape oldClip = g.getClip();
        g.setClip(new RoundRectangle2D.Float(x0, y0, w, h, arc, arc));
        Random grain = new Random(Double.doubleToLongBits(Lcm * 31 + Acm * 17 + scalePx));
        int nSpeck = Math.max(10, (w * h) / 220);
        for (int i = 0; i < nSpeck; i++) {
            int px = x0 + grain.nextInt(Math.max(1, w));
            int py = y0 + grain.nextInt(Math.max(1, h));
            int a = 12 + grain.nextInt(16);
            g.setColor(grain.nextBoolean() ? new Color(0, 0, 0, a) : new Color(255, 250, 235, a));
            g.fillOval(px, py, 2, 2);
        }
        g.setClip(oldClip);

        for (int i = 0; i < nRibs; i++) {
            double f0 = (i + 0.12) / nRibs, f1 = (i + 0.88) / nRibs;
            int ry0 = y0 + (int) (f0 * h);
            int ry1 = y0 + (int) (f1 * h);
            if (ry1 - ry0 < 1) continue;
            g.setPaint(new GradientPaint(0, ry0, ribHi, 0, ry1, ribLo));
            g.fillRect(x0 + 2, ry0, w - 4, Math.max(1, ry1 - ry0));
            g.setPaint(null);
        }

        g.setColor(edgeBottom);
        g.fillRect(x0 + 1, y0 + h - 2, w - 2, 2);

        g.setColor(edge);
        g.setStroke(new BasicStroke(1.1f));
        g.drawRoundRect(x0, y0, w, h, arc, arc);
        g.setColor(new Color(255, 255, 255, 55));
        g.setStroke(new BasicStroke(0.9f));
        g.drawLine(x0 + arc, y0, x0 + w - arc, y0);

        if (wet > 0.15) {
            g.setColor(new Color(35, 95, 160, (int) (35 + wet * 75)));
            g.fillRoundRect(x0, y0, w, h, arc, arc);
            g.setPaint(new GradientPaint(
                x0 + w * 0.15f, y0 + h * 0.15f, new Color(255, 255, 255, (int) (85 * wet)),
                x0 + w * 0.6f,  y0 + h * 0.55f, new Color(255, 255, 255, 0)));
            g.fillRoundRect(x0, y0, w, h, arc, arc);
            g.setPaint(null);
        }

        if (oldAA != null) g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
    }

    private int luzEnsayoMm() {
        if (baldosin == null) return 300;
        return (int) Math.round(baldosin.largo * 10 * 0.85);
    }

    private static int clamp255(int v) { return Math.max(0, Math.min(255, v)); }

    private JButton mkBtn(String txt, Color c) {
        return Tema.boton(txt, c);
    }

    private static JLabel lbl(String t, Color c, int sz, boolean b) {
        JLabel l = new JLabel(t);
        l.setFont(Tema.fuente(b ? Font.BOLD : Font.PLAIN, sz));
        l.setForeground(c);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private String hx(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    private static double clamp(double v, double mn, double mx) {
        return Math.max(mn, Math.min(mx, v));
    }
}