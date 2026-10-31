package geoceramiclab.modelo3d;

import geoceramiclab.Tema;
import geoceramiclab.model.BaldosinCatalan;
import geoceramiclab.model.BaldosinCatalan.MezclaBaldosin;
import geoceramiclab.model.SueloData;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class DetalleFlexion {

    static final Color FONDO = new Color(243, 243, 244);
    static final Color TINTA = new Color(26, 26, 28);
    static final Color SUAVE = new Color(120, 122, 128);
    static final Color BARRA = new Color(231, 232, 236);
    static final Color AZUL  = new Color(30, 96, 200);
    static final Color ROJO  = new Color(198, 40, 40);
    static final Color VERDE = new Color(28, 128, 72);

    static final Color COTA  = new Color(30, 96, 200);

    static final double K_MAQUINA = 350;

    static final String RUTA_TESSDATA = junto("tessdata");

    private static String junto(String nombre) {
        try {
            File jar = new File(DetalleFlexion.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            File f = new File(jar.isFile() ? jar.getParentFile() : jar, nombre);
            if (f.exists()) return f.getAbsolutePath();
        } catch (Exception ignorar) { }
        return nombre;
    }

    private DetalleFlexion() { }

    public static final class ResultadoFlexion {
        public String  probeta;
        public double  largo, luz, ancho, espesor;
        public double  fMaxN;
        public double  sN;
        public double  rMPa;
        public double  velKNs, velMPas;
        public boolean cumple;
        public String  textoNorma;
    }

    public static ResultadoFlexion ensayar(Component parent, BaldosinCatalan b, MezclaBaldosin m,
                                           SueloData d, double sigmaRef, String probeta,
                                           double largoMm, double anchoMm, double espesorMm,
                                           double luzMm, double absorcion) {
        Window w = SwingUtilities.getWindowAncestor(parent);
        JDialog dlg = new JDialog(w, "Ensayo de flexión en 3 puntos · probeta " + probeta,
                                  Dialog.ModalityType.APPLICATION_MODAL);
        PanelEnsayo panel = new PanelEnsayo(b, m, d, sigmaRef);
        panel.configurar(probeta, largoMm, anchoMm, espesorMm, luzMm, absorcion, dlg::dispose);
        abrir(parent, dlg, panel);
        panel.detenerVideo();
        return panel.resultado;
    }

    public static void mostrar(Component parent, BaldosinCatalan b, double sigmaRef) {
        mostrar(parent, b, null, null, sigmaRef);
    }

    public static void mostrar(Component parent, BaldosinCatalan b,
                               MezclaBaldosin m, SueloData d, double sigmaRef) {
        Window w = SwingUtilities.getWindowAncestor(parent);
        JDialog dlg = new JDialog(w, "Ensayo de flexión en 3 puntos · detalle");
        abrir(parent, dlg, new PanelEnsayo(b, m, d, sigmaRef));
    }

    private static void abrir(Component parent, JDialog dlg, PanelEnsayo panel) {
        dlg.setContentPane(panel);
        dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dlg.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) { panel.detenerVideo(); }
        });

        Rectangle scr = pantallaUtil(parent);
        int ancho = Math.min(1560, Math.max(1040, scr.width  - 80));
        int alto  = Math.min(940,  Math.max(660,  scr.height - 80));
        dlg.setSize(ancho, alto);
        dlg.setMinimumSize(new Dimension(980, 620));
        dlg.setLocationRelativeTo(parent);
        dlg.setVisible(true);
    }

    private static Rectangle pantallaUtil(Component parent) {
        try {
            GraphicsConfiguration gc = (parent != null && parent.getGraphicsConfiguration() != null)
                ? parent.getGraphicsConfiguration()
                : GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice().getDefaultConfiguration();
            Rectangle r = gc.getBounds();
            Insets in = Toolkit.getDefaultToolkit().getScreenInsets(gc);
            return new Rectangle(r.x + in.left, r.y + in.top,
                                 r.width - in.left - in.right,
                                 r.height - in.top - in.bottom);
        } catch (Exception e) {
            return new Rectangle(0, 0, 1280, 800);
        }
    }

    static final class Prediccion {

        static final double SIGMA_MAX = 120;

        double sigma;
        double E;
        double poros;
        double Tc;
        double sinter;
        double fSuelo, fSecado, fCoccion, fPoros;
        double base;
        double arcilla, contrac, absorcion, densCocida;
        boolean conMezcla;

        static Prediccion calcular(MezclaBaldosin m, SueloData d, double sigmaRef) {
            Prediccion p = new Prediccion();
            p.conMezcla = m != null;
            p.base = sigmaRef > 0 ? sigmaRef : 10;

            p.arcilla    = (d != null && d.hidroCalculada) ? d.pctArcilla : 28;
            p.densCocida = (m != null && m.densidadCocida > 0) ? m.densidadCocida
                         : (m != null && m.densidadAparente > 0 ? m.densidadAparente * 0.92 : 1.85);
            p.absorcion  = (m != null && m.absorcionAgua > 0) ? m.absorcionAgua : 14;
            p.contrac    = (m != null && m.contraccionLineal > 0) ? m.contraccionLineal : 7;
            double feld  = m != null ? m.fracFeldespato : 0.08;
            double sil   = m != null ? m.fracSilice : 0.30;

            p.poros  = clamp(p.absorcion * 0.85 + (2.2 - p.densCocida) * 12, 4, 40);
            p.Tc     = clamp(900 + feld * 750 + sil * 90, 900, 1080);
            p.sinter = clamp((p.Tc - 880) / 180.0, 0, 1);

            p.fSuelo = clamp(1.05 - Math.pow((p.arcilla - 30) / 45.0, 2) * 1.6, 0.60, 1.05)
                     * (1 - Math.max(0, sil - 0.35) * 0.55);
            p.fSecado = clamp(1.02 - Math.max(0, p.contrac - 6) * 0.045
                                   - Math.max(0, 3 - p.contrac) * 0.05, 0.60, 1.02);
            p.fCoccion = clamp(0.68 + 0.42 * p.sinter, 0.68, 1.10);
            p.fPoros = clamp(Math.exp(-0.030 * (p.poros - 18)), 0.55, 1.30);

            p.sigma = clamp(p.base * p.fSuelo * p.fSecado * p.fCoccion * p.fPoros, 1.5, SIGMA_MAX);
            p.E     = clamp(26000 * Math.exp(-0.045 * p.poros), 1500, 26000);
            return p;
        }
    }

    static final class PanelEnsayo extends JPanel {

        private final JTextField tfL   = campo("100");
        private final JTextField tfB   = campo("20");
        private final JTextField tfD   = campo("10");
        private final JTextField tfLuz = campo("");
        private final JLabel  aviso    = new JLabel(" ");
        private final JLabel  lblProbeta = new JLabel("Probeta · datos del ensayo");
        private final JButton bContinuar = boton("Continuar  →", Tema.ACENTO);

        private Runnable alContinuar;

        ResultadoFlexion resultado;
        private String nombreProbeta = "";

        private boolean absConocida;

        Prediccion P;
        double L = 100, B = 20, D = 10, LUZ = 85, VEL = 5;

        double Kprob = 1, Keq = 1, FRotPrev, dProbRot, dTotRot, tRotPrev, amplif = 1;

        double t, delta, dProb, F, sigma, energia;
        boolean roto, terminado;
        double tRot, FRot, sigRotReal, dProbRot2, energiaRot, angRot, gapRot;
        double dtSim = 0.01;

        boolean modoReal;
        double velKNs = Double.NaN, velMPas = Double.NaN, sRotura, absReal;
        LectorVideoEnsayo.Resultado datosVideo;
        ReproductorVideo repro;
        int idxVideo;

        final List<double[]> curva = new ArrayList<>();
        final double[][] ref = new double[4][3];
        final double[] debX = new double[16], debY = new double[16],
                       debVX = new double[16], debVY = new double[16], debR = new double[16];
        boolean debOn;

        private final Random rnd = new Random(7);
        private Timer timer;

        private final FemPanel   fem     = new FemPanel();
        private final CurvaPanel grafica = new CurvaPanel();

        private final JPanel izquierda = new JPanel(new BorderLayout());
        private final JPanel sinVideo = crearSinVideo();
        private final JSplitPane vertical = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        PanelEnsayo(BaldosinCatalan b, MezclaBaldosin m, SueloData d, double sigmaRef) {
            setLayout(new BorderLayout());
            setBackground(FONDO);

            P = Prediccion.calcular(m, d, sigmaRef);
            absReal = P.absorcion;

            if (b != null) {
                double lc = b.largo > 0 ? b.largo * 10 : 100;
                double ac = b.ancho > 0 ? b.ancho * 10 : 20;

                double ec = Math.max(0, b.espesor) * 10;
                if (ec <= 0) ec = (Math.max(0, b.espesor) + Math.max(0, b.altoNervio)) * 10;
                if (ec <= 0) ec = 10;
                tfL.setText(fmt(lc)); tfB.setText(fmt(ac)); tfD.setText(fmt(ec));
            }
            leerCampos();

            bContinuar.setVisible(false);
            habilitarContinuar(false);
            escucharCampos();
            fem.ponerControles(controles());

            fem.setMinimumSize(new Dimension(360, 320));
            grafica.setPreferredSize(new Dimension(660, 620));
            grafica.setMinimumSize(new Dimension(400, 320));

            izquierda.setBackground(FONDO);
            izquierda.setMinimumSize(new Dimension(360, 320));
            fem.setPreferredSize(new Dimension(720, 460));
            vertical.setTopComponent(fem);
            vertical.setBottomComponent(sinVideo);
            vertical.setResizeWeight(0.60);
            vertical.setContinuousLayout(true);
            vertical.setDividerSize(7);
            vertical.setBorder(null);
            izquierda.add(vertical, BorderLayout.CENTER);

            JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, izquierda, grafica);
            split.setResizeWeight(0.52);
            split.setContinuousLayout(true);
            split.setDividerSize(7);
            split.setBorder(null);
            split.setBackground(FONDO);
            add(split, BorderLayout.CENTER);
        }

        void configurar(String probeta, double largo, double ancho, double espesor,
                        double luz, double absorcion, Runnable alContinuar) {
            nombreProbeta = probeta;
            lblProbeta.setText("Probeta " + probeta + "  ·  datos del ensayo");
            if (largo   > 0) tfL.setText(fmt(largo));
            if (ancho   > 0) tfB.setText(fmt(ancho));
            if (espesor > 0) tfD.setText(fmt(espesor));
            if (luz     > 0) tfLuz.setText(fmt(luz));
            if (!Double.isNaN(absorcion) && absorcion >= 0) {
                absReal = absorcion;
                absConocida = true;
            }
            this.alContinuar = alContinuar;
            bContinuar.setVisible(alContinuar != null);
            habilitarContinuar(false);
            leerCampos();
            fem.revalidate();
            aviso.setForeground(SUAVE);
            aviso.setText("Complete las medidas en el dibujo y cargue el video del ensayo.");
            repaintTodo();
        }

        private void escucharCampos() {
            for (JTextField tf : new JTextField[]{tfL, tfLuz, tfB, tfD}) {
                tf.addActionListener(e -> alCambiarCampos());
                tf.addFocusListener(new FocusAdapter() {
                    @Override public void focusLost(FocusEvent e) { alCambiarCampos(); }
                });
            }
            tfL.setToolTipText("Largo de la probeta: longitud cocida medida en la contracción lineal.");
            tfLuz.setToolTipText("Distancia real entre los centros de los apoyos. Vacío = 0,85 × largo.");
            tfB.setToolTipText("Ancho de la probeta.");
            tfD.setToolTipText("Espesor mínimo en la línea de rotura (fondo de la ranura si hay nervios).");
        }

        private static JPanel cotaCampo(String nombre, JTextField tf) {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 1));
            p.setBackground(Color.WHITE);
            p.setBorder(BorderFactory.createLineBorder(COTA, 1));
            JLabel n = new JLabel(nombre);
            n.setFont(Tema.ui(Font.BOLD, 11));
            n.setForeground(COTA);
            JLabel u = new JLabel("mm");
            u.setFont(Tema.ui(Font.PLAIN, 10));
            u.setForeground(SUAVE);
            tf.setColumns(6);
            tf.setBackground(Color.WHITE);
            tf.setForeground(TINTA);
            tf.setCaretColor(TINTA);
            tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(170, 180, 200)),
                BorderFactory.createEmptyBorder(1, 3, 1, 3)));
            p.add(n); p.add(tf); p.add(u);
            p.setSize(p.getPreferredSize());
            return p;
        }

        private JPanel controles() {
            JPanel c = new JPanel();
            c.setOpaque(false);
            c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));

            JButton bVid = boton("Cargar video", Tema.ACENTO);
            JButton bSim = boton("Simular", new Color(110, 114, 124));
            bVid.addActionListener(e -> cargarVideo());
            bSim.addActionListener(e -> iniciar());
            bSim.setToolTipText("Vista previa simulada (no se guarda como resultado).");
            bContinuar.addActionListener(e -> continuar());
            bContinuar.setToolTipText("Se habilita cuando la probeta se rompe en el video.");

            lblProbeta.setFont(Tema.ui(Font.BOLD, 12));
            lblProbeta.setForeground(TINTA);

            JPanel fila = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            fila.setOpaque(false);
            fila.add(lblProbeta);
            fila.add(bVid);
            fila.add(bSim);
            fila.add(bContinuar);
            fila.setAlignmentX(Component.RIGHT_ALIGNMENT);
            c.add(fila);

            aviso.setFont(Tema.ui(Font.BOLD, 11));
            aviso.setHorizontalAlignment(SwingConstants.RIGHT);
            aviso.setPreferredSize(new Dimension(460, 18));
            aviso.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
            aviso.setAlignmentX(Component.RIGHT_ALIGNMENT);
            c.add(Box.createVerticalStrut(4));
            c.add(aviso);
            return c;
        }

        private void alCambiarCampos() {
            if (timer != null && timer.isRunning()) return;
            if (leerCampos()) recalcularReal();
            fem.revalidate();
            repaintTodo();
        }

        private void habilitarContinuar(boolean on) {
            bContinuar.setEnabled(on);
            bContinuar.setBackground(on ? Tema.ACENTO : new Color(184, 188, 196));
        }

        private void continuar() {
            if (!modoReal || datosVideo == null || FRot <= 0) return;
            if (!leerCampos()) return;
            recalcularReal();

            LectorVideoEnsayo.Evaluacion ev =
                    LectorVideoEnsayo.evaluarNorma(sRotura, sigRotReal, D, absReal);

            ResultadoFlexion r = new ResultadoFlexion();
            r.probeta = nombreProbeta;
            r.largo = L; r.luz = LUZ; r.ancho = B; r.espesor = D;
            r.fMaxN = FRot;
            r.sN = sRotura;
            r.rMPa = sigRotReal;
            r.velKNs = velKNs;
            r.velMPas = velMPas;
            r.cumple = ev.cumple;
            r.textoNorma = ev.texto;
            resultado = r;

            detenerVideo();
            if (alContinuar != null) alContinuar.run();
        }

        private static JTextField campo(String v) {
            JTextField t = new JTextField(v, 6);
            t.setFont(Tema.ui(Font.BOLD, 12));
            t.setHorizontalAlignment(SwingConstants.RIGHT);
            return t;
        }

        private static JButton boton(String txt, Color c) {
            JButton b = new JButton(txt);
            b.setFont(Tema.ui(Font.BOLD, 12));
            b.setForeground(Color.WHITE);
            b.setBackground(c);

            b.setUI(new javax.swing.plaf.basic.BasicButtonUI());
            b.setOpaque(true);
            b.setFocusPainted(false);
            b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(c.darker()),
                BorderFactory.createEmptyBorder(5, 12, 5, 12)));
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            return b;
        }

        private JPanel crearSinVideo() {
            JPanel p = new JPanel(new GridBagLayout());
            p.setBackground(new Color(28, 30, 34));
            p.setMinimumSize(new Dimension(300, 160));
            JPanel caja = new JPanel();
            caja.setOpaque(false);
            caja.setLayout(new BoxLayout(caja, BoxLayout.Y_AXIS));
            JLabel l1 = new JLabel("VIDEO DEL ENSAYO REAL");
            l1.setFont(Tema.ui(Font.BOLD, 14));
            l1.setForeground(Color.WHITE);
            JLabel l2 = new JLabel("Cargue el video de la pantalla de la prensa: se verá aquí y la curva se dibujará con él.");
            l2.setFont(Tema.ui(Font.PLAIN, 11));
            l2.setForeground(new Color(190, 192, 200));
            JButton b = boton("Cargar video", Tema.ACENTO);
            b.addActionListener(e -> cargarVideo());
            l1.setAlignmentX(Component.CENTER_ALIGNMENT);
            l2.setAlignmentX(Component.CENTER_ALIGNMENT);
            b.setAlignmentX(Component.CENTER_ALIGNMENT);
            caja.add(l1);
            caja.add(Box.createVerticalStrut(6));
            caja.add(l2);
            caja.add(Box.createVerticalStrut(12));
            caja.add(b);
            p.add(caja);
            return p;
        }

        private void error(String msg) {
            aviso.setForeground(ROJO);
            aviso.setText("⚠ " + msg);
            JOptionPane.showMessageDialog(this, msg, "Video del ensayo", JOptionPane.ERROR_MESSAGE);
        }

        private static String fmt(double v) { return String.format("%.1f", v); }

        private static double num(JTextField tf, double min, double max) {
            try {
                double v = Double.parseDouble(tf.getText().trim().replace(',', '.'));
                return (v < min || v > max) ? Double.NaN : v;
            } catch (Exception e) { return Double.NaN; }
        }

        private boolean leerCampos() {
            double l = num(tfL, 20, 2000), b = num(tfB, 5, 1000), d = num(tfD, 1, 200);
            if (Double.isNaN(l) || Double.isNaN(b) || Double.isNaN(d)) {
                aviso.setForeground(ROJO);
                aviso.setText("⚠ Revise el largo, el ancho y el espesor.");
                return false;
            }
            if (aviso.getForeground().equals(ROJO)) aviso.setText(" ");
            L = l; B = b; D = d;

            double luz = num(tfLuz, 5, 2000);
            LUZ = (Double.isNaN(luz) || luz >= L) ? L * 0.85 : luz;

            Kprob = 4 * P.E * B * Math.pow(D, 3) / Math.pow(LUZ, 3);
            Keq = 1 / (1 / Kprob + 1 / K_MAQUINA);
            FRotPrev = 2 * P.sigma * B * D * D / (3 * LUZ);
            dProbRot = FRotPrev / Kprob;
            dTotRot = FRotPrev / Keq;
            tRotPrev = dTotRot / (VEL / 60.0);
            amplif = (D * 0.70) / Math.max(1e-4, dProbRot);
            prepararReferencia();
            return true;
        }

        private void recalcularReal() {
            if (!modoReal || datosVideo == null) return;
            // Modulo de rotura: R = 3*F*L / (2*b*d^2)   (b = ancho, d = espesor)
            sigRotReal = 3 * FRot * LUZ / (2 * B * D * D);
            // Fuerza de rotura: S = F*L / b
            sRotura = FRot * LUZ / B;
            velMPas = Double.isNaN(velKNs) ? Double.NaN
                    : 3 * (velKNs * 1000) * LUZ / (2 * B * D * D);
            dProbRot2 = FRot / Kprob;
            amplif = (D * 0.70) / Math.max(1e-4, dProbRot2);
        }

        private void prepararReferencia() {
            double fk = FRotPrev / 9.80665;
            for (int i = 0; i < 4; i++) {
                ref[i][0] = tRotPrev * (0.84 + 0.11 * i);
                ref[i][1] = fk * (0.82 + 0.12 * i);
                ref[i][2] = rnd.nextDouble() * 6.28;
            }
        }

        private void iniciar() {
            if (timer != null && timer.isRunning()) return;
            if (!leerCampos()) return;
            quitarVideo();
            reiniciarEstado();
            dtSim = (tRotPrev * 1.18) / 450.0;
            timer = new Timer(20, e -> paso());
            timer.start();
        }

        private void reiniciarEstado() {
            t = delta = dProb = F = sigma = energia = 0;
            roto = terminado = debOn = false;
            tRot = FRot = sigRotReal = dProbRot2 = energiaRot = angRot = gapRot = 0;
            sRotura = 0;
            modoReal = false;
            velKNs = velMPas = Double.NaN;
            curva.clear();
        }

        private void cargarVideo() {
            if (timer != null) timer.stop();
            if (repro != null) repro.pausar();
            if (!leerCampos()) return;

            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter("Video (mp4, mov, avi, mkv)",
                                                         "mp4", "mov", "avi", "mkv"));
            if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
            final File video = fc.getSelectedFile();

            if (!absConocida) {
                String abs = JOptionPane.showInputDialog(this,
                        "Absorción de agua medida (%) para clasificar según la NTC 919:",
                        String.format("%.1f", absReal));
                if (abs == null) return;
                try { absReal = Double.parseDouble(abs.trim().replace(',', '.')); }
                catch (NumberFormatException ex) { absReal = P.absorcion; }
            }

            BufferedImage muestra;
            try {
                muestra = LectorVideoEnsayo.fotograma(video, 0.5);
            } catch (Exception ex) {
                error("No se pudo abrir el video: " + ex);
                return;
            }
            final BufferedImage primerFotograma = muestra;

            File datosOcr = new File(RUTA_TESSDATA, "eng.traineddata");
            if (!datosOcr.isFile()) {
                error("Falta el archivo del OCR. Copie eng.traineddata en esta carpeta:\n"
                      + new File(RUTA_TESSDATA).getAbsolutePath());
                return;
            }

            Rectangle z = null;
            for (double seg : new double[]{0.5, 3, 6, 10}) {
                try {
                    BufferedImage fr = seg == 0.5 ? muestra : LectorVideoEnsayo.fotograma(video, seg);
                    Rectangle cand = LectorVideoEnsayo.detectarZona(fr);
                    if (cand != null && LectorVideoEnsayo.zonaValida(fr, cand, RUTA_TESSDATA)) { z = cand; break; }
                } catch (Exception ignorar) { }
            }
            if (z == null) {
                JOptionPane.showMessageDialog(this,
                        "No se encontró automáticamente el número de la carga.\n"
                        + "Márquelo con el ratón en la siguiente ventana.",
                        "Video del ensayo", JOptionPane.INFORMATION_MESSAGE);
                z = LectorVideoEnsayo.elegirZona(this, muestra);
            }
            if (z == null) return;
            final Rectangle zona = z;

            aviso.setForeground(AZUL);
            aviso.setText("Leyendo la carga del video…");
            final ProgressMonitor pm = new ProgressMonitor(this, "Leyendo la carga del video…", "", 0, 100);
            pm.setMillisToDecideToPopup(0);
            pm.setMillisToPopup(0);
            SwingWorker<LectorVideoEnsayo.Resultado, Void> w =
                    new SwingWorker<LectorVideoEnsayo.Resultado, Void>() {
                @Override protected LectorVideoEnsayo.Resultado doInBackground() throws Exception {
                    return LectorVideoEnsayo.procesar(video, zona, RUTA_TESSDATA, 10, p -> setProgress(p));
                }
                @Override protected void done() {
                    pm.close();
                    try {
                        aplicarResultado(get(), video, zona, primerFotograma);
                    } catch (Exception ex) {
                        Throwable c = ex.getCause() != null ? ex.getCause() : ex;
                        error("Error al leer el video: " + c);
                    }
                }
            };
            w.addPropertyChangeListener(ev -> {
                if ("progress".equals(ev.getPropertyName())) pm.setProgress((Integer) ev.getNewValue());
            });
            w.execute();
        }

        private void aplicarResultado(LectorVideoEnsayo.Resultado r, File video,
                                      Rectangle zona, BufferedImage primerFotograma) {
            if (r.puntos.size() < 10) {
                StringBuilder ej = new StringBuilder();
                int k = 0;
                for (String s : r.textosOcr) {
                    if (s.trim().isEmpty()) continue;
                    ej.append("  \"").append(s.trim()).append("\"\n");
                    if (++k == 6) break;
                }
                error("Solo se leyeron " + r.puntos.size() + " valores de carga en el video ("
                      + r.textosOcr.size() + " fotogramas revisados).\n\n"
                      + (k == 0 ? "El lector no reconoció ningún número en la zona.\n"
                                : "Ejemplos de lo que leyó:\n" + ej)
                      + "\nVuelva a cargarlo; si se pide, encierre SOLO los dígitos del número.");
                return;
            }
            quitarVideo();
            datosVideo = r;
            prepararReproduccion();

            repro = new ReproductorVideo(video, zona, primerFotograma,
                    this::alAvanzarVideo,
                    this::prepararReproduccion,
                    () -> {
                        terminado = true;
                        if (modoReal) habilitarContinuar(true);
                        repaintTodo();
                    });
            repro.setZonaPorTiempo(r::zonaEn);

            int div = vertical.getDividerLocation();
            vertical.setBottomComponent(repro);
            vertical.setDividerLocation(div);
            izquierda.revalidate();
            izquierda.repaint();

            aviso.setForeground(VERDE);
            aviso.setText(String.format("Video listo · carga máx. %.3f kN · pulse ▶ Reproducir; "
                                        + "al romperse la probeta se habilita Continuar.", r.fMaxKN));
            JOptionPane.showMessageDialog(this, String.format(
                    "Lectura del video terminada.\n\n"
                    + "Lecturas válidas: %d   (descartadas: %d)\n"
                    + "Carga máxima: %.3f kN, en t = %.1f s\n\n"
                    + "Pulse ▶ Reproducir debajo del mapa de esfuerzos.\n"
                    + "Cuando la probeta se rompa se calculan la resistencia y el módulo de rotura\n"
                    + "y se habilita «Continuar» para volver a los ensayos.\n\n"
                    + "Las lecturas quedaron guardadas en: %s",
                    r.leidos, r.descartados, r.fMaxKN, r.tMax + r.t0,
                    new File(video.getParentFile(),
                             video.getName().replaceAll("\\.[^.]+$", "") + "_lecturas.csv").getAbsolutePath()),
                    "Video del ensayo", JOptionPane.INFORMATION_MESSAGE);
        }

        private void prepararReproduccion() {
            LectorVideoEnsayo.Resultado r = datosVideo;
            if (r == null) return;
            reiniciarEstado();
            modoReal = true;

            FRot = r.fMaxKN * 1000;
            tRot = r.tMax + r.t0;
            velKNs = r.velocidadKNs;
            recalcularReal();
            tRotPrev = Math.max(tRot, r.duracion) / 1.25;
            idxVideo = 0;
            repaintTodo();
        }

        private void alAvanzarVideo(double tVideo) {
            LectorVideoEnsayo.Resultado r = datosVideo;
            if (r == null) return;
            double tc = tVideo;

            while (idxVideo < r.puntosVideo.size() && r.puntosVideo.get(idxVideo)[0] <= tc) {
                double[] p = r.puntosVideo.get(idxVideo++);
                curva.add(new double[]{p[0], p[1]});
                if (!roto) F = p[1] * 1000;
            }
            t = Math.max(0, tc);

            if (!roto) {

                dProb = F / Kprob;
                sigma = 3 * F * LUZ / (2 * B * D * D);
                if (tc >= tRot && idxVideo > 0) {
                    roto = true;
                    sigma = 0;
                    iniciarDebris();

                    habilitarContinuar(true);
                    aviso.setForeground(VERDE);
                    aviso.setText(String.format("Probeta fracturada · S = %.0f N · R = %.2f MPa  →  pulse Continuar",
                                                sRotura, sigRotReal));
                }
            } else {

                double u = (t - tRot) / Math.max(1e-6, tRot * 0.18);
                double ease = 1 - Math.pow(Math.max(0, 1 - Math.min(1, u)), 2.2);
                angRot = ease * 7.5;
                gapRot = ease * (L * 0.012);
                if (u <= 1.05) avanzarDebris();
            }
            repaintTodo();
        }

        private void quitarVideo() {
            habilitarContinuar(false);
            if (repro == null && datosVideo == null) return;
            if (repro != null) { repro.detener(); repro = null; }
            datosVideo = null;
            int div = vertical.getDividerLocation();
            vertical.setBottomComponent(sinVideo);
            vertical.setDividerLocation(div);
            izquierda.revalidate();
            izquierda.repaint();
        }

        void detenerVideo() {
            if (timer != null) timer.stop();
            if (repro != null) repro.detener();
        }

        private void paso() {
            t += dtSim;
            if (!roto) {
                double dd = (VEL / 60.0) * dtSim;
                delta += dd;
                F = Keq * delta;
                dProb = F / Kprob;
                sigma = 3 * F * LUZ / (2 * B * D * D);
                energia += F * dd / 1000.0;
                if (sigma >= P.sigma) {
                    roto = true;
                    tRot = t; FRot = F; sigRotReal = sigma; dProbRot2 = dProb; energiaRot = energia;
                    sRotura = FRot * LUZ / B;
                    iniciarDebris();
                }
            } else {
                double u = (t - tRot) / Math.max(1e-6, tRot * 0.18);
                F = FRot * Math.max(0, 1 - u * 1.4);
                sigma = 0;
                double ease = 1 - Math.pow(Math.max(0, 1 - Math.min(1, u)), 2.2);
                angRot = ease * 7.5;
                gapRot = ease * (L * 0.012);
                avanzarDebris();
                if (u >= 1.05) { terminado = true; timer.stop(); }
            }
            double ruido = 1 + 0.015 * Math.sin(t * 9.3) + 0.008 * Math.sin(t * 23.7);
            curva.add(new double[]{t, Math.max(0, F * ruido) / 9.80665});
            repaintTodo();
        }

        private void repaintTodo() { fem.repaint(); grafica.repaint(); }

        private void iniciarDebris() {
            for (int i = 0; i < debX.length; i++) {
                debX[i] = debY[i] = 0;
                debVX[i] = (rnd.nextDouble() - 0.5) * 5.0;
                debVY[i] = -0.8 - rnd.nextDouble() * 2.6;
                debR[i] = 1.3 + rnd.nextDouble() * 2.6;
            }
            debOn = true;
        }

        private void avanzarDebris() {
            if (!debOn) return;
            for (int i = 0; i < debX.length; i++) {
                debX[i] += debVX[i]; debY[i] += debVY[i];
                debVY[i] += 0.45; debVX[i] *= 0.99;
            }
        }

        final class FemPanel extends JPanel {
            private static final double UX = 1.00, UY = 0.11;
            private static final double VX = 0.60, VY = -0.45;
            private double s = 1, ox = 0, oy = 0, rRod = 3;
            private double x0Sop, x1Sop;

            private JComponent controles;

            private final JPanel cLargo = cotaCampo("Largo",   tfL);
            private final JPanel cLuz   = cotaCampo("Luz",     tfLuz);
            private final JPanel cAncho = cotaCampo("Ancho",   tfB);
            private final JPanel cEsp   = cotaCampo("Espesor", tfD);

            private int mx0v = 190, my0v = 96;
            private int[] cotaLuz, cotaLargo, cotaAncho, cotaEsp;

            FemPanel() {
                setBackground(FONDO);
                setLayout(null);
                add(cLargo); add(cLuz); add(cAncho); add(cEsp);
            }

            void ponerControles(JComponent c) {
                controles = c;
                add(c);
            }

            private void calcularVista(int W, int H) {
                x0Sop = (L - LUZ) / 2;
                x1Sop = x0Sop + LUZ;
                rRod = Math.max(2.5, Math.min(D * 0.85, L * 0.06));

                int alturaCtl = controles != null ? controles.getPreferredSize().height : 0;
                my0v = Math.max(70, 46 + alturaCtl + 14);
                mx0v = 190;
                int mw = Math.max(110, W - mx0v - 40);
                int mh = Math.max(90, H - my0v - 165);
                double wU = L * UX + B * VX + 2 * rRod;
                double hU = L * UY + B * (-VY) + D + 4 * rRod + D * 0.8;
                s = Math.max(0.05, Math.min(mw / wU, mh / hU));
                ox = mx0v + 14;
                oy = my0v + s * (B * (-VY) + D + 2.4 * rRod) + 24;

                int rPx = Math.max(4, (int) Math.round(s * rRod));

                double usado = s * hU + 24 + 2 * rPx + 20 + 48 + 34;
                double libre = H - my0v - 70;
                if (libre > usado) oy += (libre - usado) / 2;

                int[] a = spRod(x0Sop, 0, -rRod), b = spRod(x1Sop, 0, -rRod);
                int dLuz = rPx + 20;
                cotaLuz = new int[]{a[0], a[1] + dLuz, b[0], b[1] + dLuz};

                int[] c0 = spRod(0, 0, 0), c1 = spRod(L, 0, 0);
                int[] base = spRod(x0Sop, 0, 0);
                int dLargo = (a[1] + dLuz - base[1]) + 48;
                cotaLargo = new int[]{c0[0], c0[1] + dLargo, c1[0], c1[1] + dLargo};

                int[] e0 = spRod(0, 0, D), e1 = spRod(0, B, D);
                cotaAncho = new int[]{e0[0] - 26, e0[1], e1[0] - 26, e1[1]};

                int[] f0 = spRod(L, 0, 0), f1 = spRod(L, 0, D);
                cotaEsp = new int[]{f0[0] + 18, f0[1], f1[0] + 18, f1[1]};
            }

            @Override public void doLayout() {
                int W = getWidth(), H = getHeight();
                if (W <= 0 || H <= 0) return;
                if (controles != null) {
                    Dimension d = controles.getPreferredSize();
                    int x = Math.max(160, W - d.width - 14);
                    controles.setBounds(x, 46, Math.min(d.width, W - x - 8), d.height);
                }
                calcularVista(W, H);

                ponerBajo(cLuz, cotaLuz, W);
                ponerBajo(cLargo, cotaLargo, W);

                Dimension da = cAncho.getPreferredSize();
                int[] m = medio(cotaAncho);
                cAncho.setBounds(Math.max(98, m[0] - 10 - da.width), m[1] - da.height / 2, da.width, da.height);

                Dimension de = cEsp.getPreferredSize();
                m = medio(cotaEsp);
                cEsp.setBounds(Math.min(W - de.width - 6, m[0] + 10), m[1] - de.height / 2 + 12,
                               de.width, de.height);
            }

            private void ponerBajo(JComponent c, int[] cota, int W) {
                Dimension d = c.getPreferredSize();
                int[] m = medio(cota);
                int x = Math.max(4, Math.min(W - d.width - 4, m[0] - d.width / 2));
                c.setBounds(x, m[1] + 6, d.width, d.height);
            }

            private int[] medio(int[] p) { return new int[]{(p[0] + p[2]) / 2, (p[1] + p[3]) / 2}; }

            private void dibujarCotas(Graphics2D g) {
                if (cotaLuz == null) return;
                Stroke punteada = new BasicStroke(0.9f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                                                  10f, new float[]{3f, 3f}, 0f);
                g.setStroke(punteada);
                g.setColor(new Color(COTA.getRed(), COTA.getGreen(), COTA.getBlue(), 140));

                int[] a = spRod(x0Sop, 0, -rRod), b = spRod(x1Sop, 0, -rRod);
                g.drawLine(a[0], a[1], cotaLuz[0], cotaLuz[1]);
                g.drawLine(b[0], b[1], cotaLuz[2], cotaLuz[3]);

                int[] c0 = spRod(0, 0, 0), c1 = spRod(L, 0, 0);
                g.drawLine(c0[0], c0[1], cotaLargo[0], cotaLargo[1]);
                g.drawLine(c1[0], c1[1], cotaLargo[2], cotaLargo[3]);

                int[] e0 = spRod(0, 0, D), e1 = spRod(0, B, D);
                g.drawLine(e0[0], e0[1], cotaAncho[0], cotaAncho[1]);
                g.drawLine(e1[0], e1[1], cotaAncho[2], cotaAncho[3]);

                int[] f0 = spRod(L, 0, 0), f1 = spRod(L, 0, D);
                g.drawLine(f0[0], f0[1], cotaEsp[0], cotaEsp[1]);
                g.drawLine(f1[0], f1[1], cotaEsp[2], cotaEsp[3]);

                g.setColor(COTA);
                g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int[] p : new int[][]{cotaLuz, cotaLargo, cotaAncho, cotaEsp}) lineaCota(g, p);
            }

            private void lineaCota(Graphics2D g, int[] p) {
                g.drawLine(p[0], p[1], p[2], p[3]);
                double dx = p[2] - p[0], dy = p[3] - p[1], len = Math.hypot(dx, dy);
                if (len < 1) return;
                int nx = (int) Math.round(-dy / len * 6), ny = (int) Math.round(dx / len * 6);
                g.drawLine(p[0] - nx, p[1] - ny, p[0] + nx, p[1] + ny);
                g.drawLine(p[2] - nx, p[3] - ny, p[2] + nx, p[3] + ny);

                int ux = (int) Math.round(dx / len * 8), uy = (int) Math.round(dy / len * 8);
                int px = (int) Math.round(-dy / len * 3), py = (int) Math.round(dx / len * 3);
                g.fillPolygon(new int[]{p[0], p[0] + ux + px, p[0] + ux - px},
                              new int[]{p[1], p[1] + uy + py, p[1] + uy - py}, 3);
                g.fillPolygon(new int[]{p[2], p[2] - ux + px, p[2] - ux - px},
                              new int[]{p[3], p[3] - uy + py, p[3] - uy - py}, 3);
            }

            private double fac() {
                if (roto) return 0;
                double sRef = (modoReal && sigRotReal > 0) ? sigRotReal : P.sigma;
                return Math.min(1, sigma / sRef);
            }

            private double sigmaN(double x, double z) {
                double m;
                if (x <= x0Sop || x >= x1Sop) m = 0;
                else {
                    double u = (x - x0Sop) / (x1Sop - x0Sop);
                    m = 2 * Math.min(u, 1 - u);
                }
                return fac() * m * (2 * (z / D) - 1);
            }

            private double flecha(double x) {
                if (roto) return 0;
                double u = (x - x0Sop) / (x1Sop - x0Sop);
                double gf;
                if (u < 0)      gf = 3 * u;
                else if (u > 1) gf = 3 * (1 - u);
                else {
                    double uu = u <= 0.5 ? u : 1 - u;
                    gf = 3 * uu - 4 * uu * uu * uu;
                }
                return dProb * amplif * gf;
            }

            private int[] sp(double x, double y, double z) {
                double zz = z - flecha(x);
                return new int[]{
                    (int) Math.round(ox + s * (x * UX + y * VX)),
                    (int) Math.round(oy + s * (x * UY + y * VY - zz))
                };
            }

            private void celda(Graphics2D g, double[][] p, Color c) {
                int[] xs = new int[4], ys = new int[4];
                for (int i = 0; i < 4; i++) {
                    int[] q = sp(p[i][0], p[i][1], p[i][2]);
                    xs[i] = q[0]; ys[i] = q[1];
                }
                g.setColor(c);
                g.fillPolygon(xs, ys, 4);
                g.setColor(new Color(0, 0, 0, 130));
                g.setStroke(new BasicStroke(0.7f));
                g.drawPolygon(xs, ys, 4);
            }

            @Override protected void paintComponent(Graphics g0) {
                super.paintComponent(g0);
                Graphics2D g = (Graphics2D) g0;
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int W = getWidth(), H = getHeight();
                g.setColor(FONDO); g.fillRect(0, 0, W, H);

                calcularVista(W, H);

                g.setColor(TINTA);
                g.setFont(Tema.fuente(Font.BOLD, 14));
                g.drawString("MAPA DE ESFUERZOS · ELEMENTOS FINITOS", 22, 24);
                g.setFont(Tema.fuente(Font.PLAIN, 11));
                g.setColor(roto ? ROJO : SUAVE);
                g.drawString(roto
                    ? "Probeta FRACTURADA — rotura frágil a media luz"
                    : "En carga — tracción abajo, compresión arriba", 22, 41);

                int lgX = 26, lgW = 22, lgY = 84;
                int lgH = Math.max(110, H - 178);
                leyenda(g, lgX, lgY, lgW, lgH);

                modelo(g, mx0v, my0v);
                dibujarCotas(g);

                ejes(g, 88, H - 52);

                g.setFont(Tema.fuente(Font.PLAIN, 10));
                g.setColor(SUAVE);
                String nota = String.format("%.0f × %.0f × %.0f mm · luz %.0f mm · deformación ×%.0f",
                        L, B, D, LUZ, amplif);
                g.drawString(nota, Math.max(150, W - 24 - g.getFontMetrics().stringWidth(nota)), H - 14);
            }

            private void leyenda(Graphics2D g, int x, int y, int w, int h) {
                g.setColor(TINTA);
                g.setFont(Tema.fuente(Font.PLAIN, 12));
                g.drawString("σ [MPa]", x, y - 20);
                g.setFont(Tema.fuente(Font.PLAIN, 10));
                g.setColor(SUAVE);
                g.drawString("(+) compresión", x, y - 7);

                for (int i = 0; i < h; i++) {
                    g.setColor(jet(1.0 - (double) i / h));
                    g.fillRect(x, y + i, w, 1);
                }
                g.setColor(TINTA);
                g.setStroke(new BasicStroke(1.2f));
                g.drawRect(x, y, w, h);

                double sEsc = (modoReal && sigRotReal > 0) ? sigRotReal : P.sigma;
                g.setFont(Tema.fuente(Font.PLAIN, 10));
                int n = 8;
                for (int i = 0; i <= n; i++) {
                    double v = sEsc * (1 - 2.0 * i / n);
                    int yy = y + (int) Math.round((double) i / n * h);
                    g.setColor(TINTA);
                    g.drawLine(x + w, yy, x + w + 4, yy);
                    g.drawString(String.format("%+.1f", v), x + w + 7, yy + 4);
                }
                g.setFont(Tema.fuente(Font.PLAIN, 10));
                g.setColor(SUAVE);
                g.drawString("(−) tracción", x, y + h + 15);
            }

            private void modelo(Graphics2D g, int mx0, int my0) {

                rodillo(g, x0Sop, -rRod);
                rodillo(g, x1Sop, -rRod);

                if (!roto) {
                    tramo(g, 0, L, true, false);
                } else {
                    int[] pL = sp(x0Sop, B / 2, 0);
                    java.awt.geom.AffineTransform at = g.getTransform();
                    g.rotate(Math.toRadians(angRot), pL[0], pL[1]);
                    tramo(g, 0, L / 2 - gapRot, true, true);
                    g.setTransform(at);

                    int[] pR = sp(x1Sop, B / 2, 0);
                    g.rotate(Math.toRadians(-angRot), pR[0], pR[1]);
                    tramo(g, L / 2 + gapRot, L, true, false);
                    filoRotura(g, L / 2 + gapRot);
                    g.setTransform(at);

                    polvoYesquirlas(g);
                }

                double desc = roto ? Math.sin(Math.toRadians(angRot)) * (LUZ / 2) : flecha(L / 2);
                int[] mid = rodillo(g, L / 2, D + rRod - desc);
                int ax = mid[0], ay = mid[1] - (int) (s * rRod) - 10;
                int top = Math.max(my0 + 4, ay - 72);
                g.setColor(TINTA);
                g.setStroke(new BasicStroke(3f));
                g.drawLine(ax, top, ax, ay - 12);
                g.fillPolygon(new int[]{ax - 8, ax + 8, ax}, new int[]{ay - 14, ay - 14, ay}, 3);
                g.setFont(Tema.fuente(Font.BOLD, 13));
                boolean finalReal = modoReal && roto;
                double fMostrar = finalReal ? FRot : Math.max(0, F);
                g.drawString(String.format(finalReal ? "Carga máx.  %.0f N" : "Carga  %.0f N", fMostrar),
                             ax + 13, top + 12);
                g.setFont(Tema.fuente(Font.PLAIN, 11));
                g.setColor(SUAVE);
                if (finalReal)
                    g.drawString(String.format("R = %.2f MPa   S = %.0f N", sigRotReal, sRotura), ax + 13, top + 27);
                else
                    g.drawString(String.format("σ = %.2f MPa   δ = %.2f mm", sigma, dProb), ax + 13, top + 27);
            }

            private void tramo(Graphics2D g, double xa, double xb, boolean tapaDer, boolean fractura) {
                if (xb <= xa) return;
                int nx = Math.max(6, (int) Math.round(40 * (xb - xa) / L));
                int nz = 10;
                int ny = Math.min(26, Math.max(4, (int) Math.round(40 * B / L)));

                for (int i = 0; i < nx; i++) {
                    double xA = xa + (xb - xa) * i / nx, xB = xa + (xb - xa) * (i + 1) / nx;
                    Color c = jet(0.5 + 0.5 * sigmaN((xA + xB) / 2, D));
                    for (int j = 0; j < ny; j++) {
                        double yA = B * j / ny, yB = B * (j + 1) / ny;
                        celda(g, new double[][]{{xA, yA, D}, {xB, yA, D}, {xB, yB, D}, {xA, yB, D}}, c);
                    }
                }
                if (tapaDer) {
                    for (int j = 0; j < ny; j++) {
                        double yA = B * j / ny, yB = B * (j + 1) / ny;
                        for (int k = 0; k < nz; k++) {
                            double zA = D * k / nz, zB = D * (k + 1) / nz;
                            Color c = fractura ? granito(k / (double) nz)
                                               : jet(0.5 + 0.5 * sigmaN(xb, (zA + zB) / 2));
                            celda(g, new double[][]{{xb, yA, zA}, {xb, yB, zA}, {xb, yB, zB}, {xb, yA, zB}}, c);
                        }
                    }
                }
                for (int i = 0; i < nx; i++) {
                    double xA = xa + (xb - xa) * i / nx, xB = xa + (xb - xa) * (i + 1) / nx;
                    for (int k = 0; k < nz; k++) {
                        double zA = D * k / nz, zB = D * (k + 1) / nz;
                        Color c = jet(0.5 + 0.5 * sigmaN((xA + xB) / 2, (zA + zB) / 2));
                        celda(g, new double[][]{{xA, 0, zA}, {xB, 0, zA}, {xB, 0, zB}, {xA, 0, zB}}, c);
                    }
                }

                if (!roto && fac() > 0.72) {
                    double gr = Math.min(1, (fac() - 0.72) / 0.28);
                    int[] p0 = sp(L / 2, 0, 0), p1 = sp(L / 2, 0, D * gr * 0.8);
                    g.setColor(new Color(20, 20, 20, (int) (120 + 130 * gr)));
                    g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    Path2D cr = new Path2D.Double();
                    cr.moveTo(p0[0], p0[1]);
                    for (int i = 1; i <= 4; i++) {
                        double u = i / 4.0;
                        cr.lineTo(p0[0] + ((i % 2 == 0) ? 2 : -2) * (1 - u) * 2, p0[1] + (p1[1] - p0[1]) * u);
                    }
                    g.draw(cr);
                }

                if (fractura) {
                    Random rr = new Random(31);
                    int[] a = sp(xb, 0, D), b = sp(xb, B, D);
                    g.setColor(new Color(90, 55, 30, 220));
                    g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    Path2D e = new Path2D.Double();
                    e.moveTo(a[0], a[1]);
                    for (int i = 1; i <= 6; i++) {
                        double u = i / 6.0;
                        e.lineTo(a[0] + (b[0] - a[0]) * u, a[1] + (b[1] - a[1]) * u + (rr.nextDouble() - 0.5) * 5);
                    }
                    g.draw(e);
                }
            }

            private void filoRotura(Graphics2D g, double x) {
                Random rr = new Random(53);
                int[] a = sp(x, 0, D), b = sp(x, 0, 0);
                g.setColor(new Color(240, 216, 190, 220));
                g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                Path2D p = new Path2D.Double();
                p.moveTo(a[0], a[1]);
                for (int i = 1; i <= 5; i++) {
                    double u = i / 5.0;
                    p.lineTo(a[0] + (b[0] - a[0]) * u + (rr.nextDouble() - 0.5) * 4, a[1] + (b[1] - a[1]) * u);
                }
                g.draw(p);
            }

            private void polvoYesquirlas(Graphics2D g) {
                int[] c = sp(L / 2, 0, D / 2);
                double u = Math.min(1, (t - tRot) / Math.max(1e-6, tRot * 0.18));
                if (u < 0.6) {
                    int al = (int) (120 * (1 - u / 0.6));
                    int rad = (int) (s * D * (0.4 + u * 2.2));
                    g.setColor(new Color(196, 160, 124, Math.max(0, al)));
                    g.fillOval(c[0] - rad, c[1] - rad / 2, rad * 2, rad);
                }
                if (debOn) {
                    for (int i = 0; i < debX.length; i++) {
                        int px = c[0] + (int) debX[i], py = c[1] + (int) debY[i];
                        int rr = (int) Math.max(1, debR[i]);
                        g.setColor(new Color(172, 110, 66, 220));
                        g.fillOval(px - rr, py - rr, rr * 2, rr * 2);
                        g.setColor(new Color(96, 56, 32, 170));
                        g.drawOval(px - rr, py - rr, rr * 2, rr * 2);
                    }
                }
            }

            private int[] rodillo(Graphics2D g, double x, double z) {
                double over = 0.16 * B;
                int[] fr = spRod(x, -over, z), bk = spRod(x, B + over, z);
                int r = Math.max(4, (int) Math.round(s * rRod));
                int dx = bk[0] - fr[0], dy = bk[1] - fr[1];

                g.setColor(new Color(0, 0, 0, 38));
                g.fillOval(fr[0] - r + 4, fr[1] + r - 2, r * 2 + 10, 8);

                g.setColor(new Color(205, 208, 214));
                g.fillOval(bk[0] - r, bk[1] - r, r * 2, r * 2);
                g.setColor(new Color(120, 124, 134));
                g.setStroke(new BasicStroke(1f));
                g.drawOval(bk[0] - r, bk[1] - r, r * 2, r * 2);

                double len = Math.hypot(dx, dy);
                double px = len > 0.01 ? (-dy / len) * r : 0;
                double py = len > 0.01 ? (dx / len) * r : -r;
                int[] xs = {(int) (fr[0] + px), (int) (bk[0] + px), (int) (bk[0] - px), (int) (fr[0] - px)};
                int[] ys = {(int) (fr[1] + py), (int) (bk[1] + py), (int) (bk[1] - py), (int) (fr[1] - py)};
                g.setPaint(new GradientPaint(fr[0], fr[1] - r, new Color(212, 216, 222),
                                             fr[0], fr[1] + r, new Color(118, 122, 132)));
                g.fillPolygon(xs, ys, 4);

                g.setPaint(new GradientPaint(fr[0] - r, fr[1] - r, new Color(216, 220, 226),
                                             fr[0] + r, fr[1] + r, new Color(112, 116, 128)));
                g.fillOval(fr[0] - r, fr[1] - r, r * 2, r * 2);
                g.setPaint(null);
                g.setColor(new Color(80, 84, 96));
                g.setStroke(new BasicStroke(1.3f));
                g.drawOval(fr[0] - r, fr[1] - r, r * 2, r * 2);
                g.setColor(new Color(255, 255, 255, 110));
                g.fillOval(fr[0] - r + 3, fr[1] - r + 3, Math.max(2, r), Math.max(1, r / 2));
                return new int[]{(fr[0] + bk[0]) / 2, (fr[1] + bk[1]) / 2};
            }

            private int[] spRod(double x, double y, double z) {
                return new int[]{
                    (int) Math.round(ox + s * (x * UX + y * VX)),
                    (int) Math.round(oy + s * (x * UY + y * VY - z))
                };
            }

            private void ejes(Graphics2D g, int x, int y) {
                double n = 34;
                double lx = Math.hypot(UX, UY), ly = Math.hypot(VX, VY);
                flechaEje(g, x, y, x + (int) (n * UX / lx), y + (int) (n * UY / lx), new Color(214, 40, 40), "X");
                flechaEje(g, x, y, x + (int) (n * VX / ly), y + (int) (n * VY / ly), new Color(28, 132, 58), "Y");
                flechaEje(g, x, y, x, y - (int) n, new Color(38, 70, 210), "Z");
            }

            private void flechaEje(Graphics2D g, int x1, int y1, int x2, int y2, Color c, String lbl) {
                g.setColor(c);
                g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawLine(x1, y1, x2, y2);
                double a = Math.atan2(y2 - y1, x2 - x1);
                int h = 8;
                g.fillPolygon(new int[]{x2, (int) (x2 - h * Math.cos(a - 0.42)), (int) (x2 - h * Math.cos(a + 0.42))},
                              new int[]{y2, (int) (y2 - h * Math.sin(a - 0.42)), (int) (y2 - h * Math.sin(a + 0.42))}, 3);
                g.setFont(Tema.fuente(Font.PLAIN, 11));
                g.setColor(TINTA);
                g.drawString(lbl, x2 + (int) (9 * Math.cos(a)) - 3, y2 + (int) (9 * Math.sin(a)) + 4);
            }

            private void escala(Graphics2D g, int y) {
                double paso = pasoBonito(L / 5);
                int nSeg = Math.max(2, (int) Math.round(L / paso));
                int wPx = Math.max(70, (int) Math.round(paso * nSeg * s * UX));
                int x = (int) Math.max(150, ox);
                if (x + wPx > getWidth() - 20) wPx = Math.max(70, getWidth() - 20 - x);
                int h = 8;
                for (int i = 0; i < nSeg; i++) {
                    int xa = x + (int) Math.round((double) i / nSeg * wPx);
                    int xb = x + (int) Math.round((double) (i + 1) / nSeg * wPx);
                    g.setColor(i % 2 == 0 ? TINTA : Color.WHITE);
                    g.fillRect(xa, y, xb - xa, h);
                }
                g.setColor(TINTA);
                g.setStroke(new BasicStroke(1.2f));
                g.drawRect(x, y, wPx, h);
                g.setFont(Tema.fuente(Font.PLAIN, 10));
                for (int i = 0; i <= nSeg; i++) {
                    int xa = x + (int) Math.round((double) i / nSeg * wPx);
                    String s2 = (i == nSeg) ? String.format("%.0f mm", paso * i) : String.format("%.0f", paso * i);
                    g.drawString(s2, xa - 5, y - 4);
                }
            }
        }

        final class CurvaPanel extends JPanel {
            CurvaPanel() { setBackground(Color.WHITE); }

            @Override protected void paintComponent(Graphics g0) {
                super.paintComponent(g0);
                Graphics2D g = (Graphics2D) g0;
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int W = getWidth(), H = getHeight();
                g.setColor(Color.WHITE); g.fillRect(0, 0, W, H);
                g.setColor(new Color(212, 213, 218));
                g.drawLine(0, 0, 0, H);

                g.setColor(TINTA);
                g.setFont(Tema.fuente(Font.BOLD, 14));
                g.drawString("CURVA CARGA – TIEMPO", 70, 26);
                g.setFont(Tema.fuente(Font.PLAIN, 11));
                g.setColor(SUAVE);
                g.drawString(modoReal
                        ? "video del ensayo real · " + (terminado ? "finalizado"
                            : (roto ? "rotura" : (curva.isEmpty() ? "pulse ▶ Reproducir" : "reproduciendo")))
                        : "vista previa simulada · " + (terminado ? "finalizado"
                        : (roto ? "descarga tras la rotura" : "en carga")), 70, 43);

                int px0 = 70, py0 = 58, pw = W - px0 - 34, ph = H - py0 - 52;
                if (pw < 60 || ph < 40) return;

                double xMax = Math.max(tRotPrev * 1.25, Math.max(t * 1.08, 1));

                double yMax;
                if (modoReal) {
                    yMax = Math.max(0.5, FRot / 1000.0 * 1.15);
                    double paso = yMax <= 2 ? 0.5 : (yMax <= 5 ? 1 : (yMax <= 12 ? 2 : 5));
                    yMax = Math.ceil(yMax / paso) * paso;
                } else {
                    yMax = 10;
                    for (double[] r : ref) yMax = Math.max(yMax, r[1] * 1.25);
                    for (double[] c : curva) yMax = Math.max(yMax, c[1] * 1.15);
                    yMax = Math.ceil(yMax / 10) * 10;
                }
                final double div = modoReal ? 1000.0 : 9.80665;

                g.setFont(Tema.fuente(Font.PLAIN, 11));
                for (int i = 0; i <= 5; i++) {
                    double v = yMax * i / 5;
                    int y = (int) (py0 + ph - (double) i / 5 * ph);
                    g.setColor(new Color(238, 238, 240));
                    g.drawLine(px0, y, px0 + pw, y);
                    g.setColor(TINTA);
                    String s2 = String.format(modoReal ? "%.1f" : "%.0f", v);
                    g.drawString(s2, px0 - 8 - g.getFontMetrics().stringWidth(s2), y + 4);
                }
                for (int i = 0; i <= 6; i++) {
                    double v = xMax * i / 6;
                    int x = (int) (px0 + (double) i / 6 * pw);
                    g.setColor(new Color(243, 243, 245));
                    g.drawLine(x, py0, x, py0 + ph);
                    g.setColor(TINTA);
                    String s2 = String.format("%.1f", v);
                    g.drawString(s2, x - g.getFontMetrics().stringWidth(s2) / 2, py0 + ph + 16);
                }

                g.setColor(TINTA);
                g.setStroke(new BasicStroke(1.3f));
                g.drawLine(px0, py0, px0, py0 + ph);
                g.drawLine(px0, py0 + ph, px0 + pw, py0 + ph);
                g.setFont(Tema.fuente(Font.PLAIN, 12));
                g.drawString("Tiempo (s)", px0 + pw / 2 - 28, py0 + ph + 36);
                java.awt.geom.AffineTransform at = g.getTransform();
                g.rotate(-Math.PI / 2, 18, py0 + ph / 2);
                g.drawString(modoReal ? "Carga (kN)" : "Carga (kgF)", 18 - 34, py0 + ph / 2);
                g.setTransform(at);

                if (!modoReal) {
                    g.setStroke(new BasicStroke(1.3f));
                    for (int m = 0; m < ref.length; m++) {
                        g.setColor(new Color(188 - m * 16, 188 - m * 16, 192 - m * 16));
                        int prevX = -1, prevY = 0;
                        double tEnd = ref[m][0] * 1.16;
                        for (double tt = 0; tt <= tEnd; tt += tEnd / 160) {
                            double ff = (tt <= ref[m][0])
                                ? ref[m][1] * (tt / ref[m][0])
                                : ref[m][1] * Math.max(0, 1 - (tt - ref[m][0]) / (ref[m][0] * 0.16) * 0.62);
                            ff *= 1 + 0.03 * Math.sin(tt * 7 + ref[m][2]);
                            int x = (int) (px0 + tt / xMax * pw);
                            int y = (int) (py0 + ph - ff / yMax * ph);
                            if (prevX >= 0) g.drawLine(prevX, prevY, x, y);
                            prevX = x; prevY = y;
                        }
                    }
                }

                g.setColor(new Color(15, 15, 18));
                g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int prevX = -1, prevY = 0, curX = -1, curY = 0;
                for (double[] c : curva) {
                    int x = (int) (px0 + c[0] / xMax * pw);
                    int y = (int) (py0 + ph - c[1] / yMax * ph);
                    if (prevX >= 0) g.drawLine(prevX, prevY, x, y);
                    prevX = x; prevY = y; curX = x; curY = y;
                }
                if (curX >= 0 && !terminado) {
                    g.setColor(AZUL);
                    g.fillOval(curX - 4, curY - 4, 8, 8);
                }

                if (roto) {
                    int x = (int) (px0 + tRot / xMax * pw);
                    int y = (int) (py0 + ph - (FRot / div) / yMax * ph);
                    g.setColor(ROJO);
                    g.setStroke(new BasicStroke(1.2f));
                    g.drawLine(x, py0, x, py0 + ph);
                    g.fillOval(x - 5, y - 5, 10, 10);

                    double absEval = modoReal ? absReal : P.absorcion;
                    LectorVideoEnsayo.Evaluacion ev =
                            LectorVideoEnsayo.evaluarNorma(sRotura, sigRotReal, D, absEval);
                    boolean cumple = ev.cumple;

                    String[] txt;
                    if (modoReal) {
                        txt = new String[]{
                            String.format("ENSAYO REAL (video) · rotura en t = %.1f s", tRot),
                            String.format("Carga máx. F = %.3f kN   (%.0f N)", FRot / 1000, FRot),
                            String.format("Fuerza de rotura S = %.0f N", sRotura),
                            String.format("Módulo de rotura R = %.2f MPa", sigRotReal),
                            Double.isNaN(velKNs) ? "Velocidad de carga: sin datos suficientes"
                                : String.format("Vel. carga = %.3f kN/s  ·  %.2f MPa/s (norma: 1 ± 0,2)",
                                                velKNs, velMPas),
                            ev.texto
                        };
                    } else {
                        txt = new String[]{
                            String.format("SIMULACIÓN · rotura en t = %.1f s", tRot),
                            String.format("F = %.0f N   (%.1f kgF)   S = %.0f N", FRot, FRot / 9.80665, sRotura),
                            String.format("σ = %.2f MPa   δ = %.2f mm", sigRotReal, dProbRot2),
                            String.format("Energía absorbida = %.2f J", energiaRot),
                            String.format("Rigidez K = %.0f N/mm", FRot / Math.max(1e-6, dProbRot2)),
                            ev.texto
                        };
                    }

                    g.setFont(Tema.fuente(Font.BOLD, 11));
                    int bw = 214;
                    for (String s3 : txt) bw = Math.max(bw, g.getFontMetrics().stringWidth(s3) + 22);
                    int bh = 16 * txt.length + 12;
                    int bxr = px0 + 14, byr = py0 + 10;
                    g.setColor(new Color(255, 255, 255, 235));
                    g.fillRect(bxr, byr, bw, bh);
                    g.setColor(new Color(206, 207, 212));
                    g.setStroke(new BasicStroke(1f));
                    g.drawRect(bxr, byr, bw, bh);
                    for (int i = 0; i < txt.length; i++) {
                        boolean ult = i == txt.length - 1;
                        g.setFont(Tema.fuente(i == 0 || ult ? Font.BOLD : Font.PLAIN, 11));
                        g.setColor(i == 0 ? ROJO : (ult ? (cumple ? VERDE : ROJO) : TINTA));
                        g.drawString(txt[i], bxr + 10, byr + 18 + i * 16);
                    }
                }

                if (pw > 330 && !modoReal) {
                    g.setFont(Tema.fuente(Font.PLAIN, 11));
                    g.setColor(new Color(150, 150, 155));
                    g.setStroke(new BasicStroke(1.3f));
                    g.drawLine(px0 + pw - 240, py0 + 12, px0 + pw - 218, py0 + 12);
                    g.setColor(SUAVE);
                    g.drawString("dispersión del lote", px0 + pw - 212, py0 + 16);
                    g.setColor(new Color(15, 15, 18));
                    g.setStroke(new BasicStroke(2.4f));
                    g.drawLine(px0 + pw - 96, py0 + 12, px0 + pw - 74, py0 + 12);
                    g.drawString("probeta", px0 + pw - 68, py0 + 16);
                }
            }
        }
    }

    static Color granito(double u) {
        return new Color(Math.max(0, (int) (214 - u * 52)),
                         Math.max(0, (int) (168 - u * 56)),
                         Math.max(0, (int) (126 - u * 48)));
    }

    static Color jet(double t) {
        t = Math.max(0, Math.min(1, t));
        double r, g, b;
        if (t < 0.125)      { r = 0;                  g = 0;                      b = 0.5 + t / 0.125 * 0.5; }
        else if (t < 0.375) { r = 0;                  g = (t - 0.125) / 0.25;     b = 1; }
        else if (t < 0.625) { r = (t - 0.375) / 0.25; g = 1;                      b = 1 - (t - 0.375) / 0.25; }
        else if (t < 0.875) { r = 1;                  g = 1 - (t - 0.625) / 0.25; b = 0; }
        else                { r = 1 - (t - 0.875) / 0.125 * 0.5; g = 0;           b = 0; }
        return new Color((int) (r * 255), (int) (g * 255), (int) (b * 255));
    }

    static double pasoBonito(double v) {
        double[] c = {1, 2, 5, 10, 20, 25, 50, 100, 200, 250, 500};
        for (double x : c) if (x >= v) return x;
        return 1000;
    }

    static double clamp(double v, double a, double b) { return Math.max(a, Math.min(b, v)); }
}