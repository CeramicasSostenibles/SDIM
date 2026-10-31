package geoceramiclab.modelo3d;

import geoceramiclab.Tema;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.function.DoubleConsumer;

public final class ReproductorVideo extends JPanel {

    private static final int ANCHO_MAX = 960;

    private final File video;
    private final DoubleConsumer alAvanzar;
    private final Runnable alReiniciar;
    private final Runnable alTerminar;

    private final PantallaVideo pantalla = new PantallaVideo();
    private final JButton bPlay = new JButton("▶  Reproducir");
    private final JButton bIni  = new JButton("⏮  Inicio");
    private final JComboBox<String> cbVel = new JComboBox<>(new String[]{"0.5×", "1×", "2×", "4×"});
    private final JLabel lbTiempo = new JLabel("0.0 s");

    private final Rectangle zonaOriginal;
    private final int anchoOriginal;

    private java.util.function.DoubleFunction<Rectangle> zonaPorTiempo;
    private volatile double tMostrado;

    private volatile int generacion;
    private volatile boolean reproduciendo;
    private volatile double posicion;
    private volatile boolean alFinal;

    public ReproductorVideo(File video, Rectangle zona, BufferedImage primerFotograma,
                            DoubleConsumer alAvanzar, Runnable alReiniciar, Runnable alTerminar) {
        this.video = video;
        this.zonaOriginal = zona;
        this.anchoOriginal = primerFotograma.getWidth();
        this.alAvanzar = alAvanzar;
        this.alReiniciar = alReiniciar;
        this.alTerminar = alTerminar;

        setLayout(new BorderLayout());
        setBackground(Color.BLACK);
        add(pantalla, BorderLayout.CENTER);
        add(controles(), BorderLayout.SOUTH);
        pantalla.setImagen(reducir(primerFotograma));
    }

    public void setZonaPorTiempo(java.util.function.DoubleFunction<Rectangle> f) {
        zonaPorTiempo = f;
        pantalla.repaint();
    }

    private JPanel controles() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        p.setBackground(new Color(231, 232, 236));
        estilo(bPlay, new Color(30, 96, 200));
        estilo(bIni, new Color(110, 114, 124));
        cbVel.setSelectedIndex(1);
        cbVel.setToolTipText("Velocidad de reproducción");
        lbTiempo.setFont(Tema.ui(Font.BOLD, 12));

        bPlay.addActionListener(e -> alternar());
        bIni.addActionListener(e -> volverAlInicio());

        JLabel tit = new JLabel("VIDEO DEL ENSAYO");
        tit.setFont(Tema.ui(Font.BOLD, 12));
        p.add(tit);
        p.add(bPlay);
        p.add(bIni);
        p.add(new JLabel("Velocidad:"));
        p.add(cbVel);
        p.add(lbTiempo);
        return p;
    }

    private static void estilo(JButton b, Color c) {
        b.setFont(Tema.ui(Font.BOLD, 12));
        b.setForeground(Color.WHITE);
        b.setBackground(c);
        b.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(c.darker()),
            BorderFactory.createEmptyBorder(4, 10, 4, 10)));
    }

    private void alternar() {
        if (reproduciendo) { pausar(); return; }
        if (alFinal) volverAlInicio();

        reproduciendo = true;
        bPlay.setText("⏸  Pausa");
        final int gen = ++generacion;
        final double desde = posicion;
        final double vel = velocidad();
        Thread h = new Thread(() -> bucle(gen, desde, vel), "reproductor-video");
        h.setDaemon(true);
        h.start();
    }

    public void pausar() {
        generacion++;
        reproduciendo = false;
        bPlay.setText("▶  Reproducir");
    }

    public void detener() { pausar(); }

    private void volverAlInicio() {
        pausar();
        posicion = 0;
        tMostrado = 0;
        alFinal = false;
        lbTiempo.setText("0.0 s");
        alReiniciar.run();
    }

    private double velocidad() {
        String s = (String) cbVel.getSelectedItem();
        try { return Double.parseDouble(s.replace("×", "")); } catch (Exception e) { return 1; }
    }

    private void bucle(int gen, double desde, double vel) {
        FFmpegFrameGrabber g = new FFmpegFrameGrabber(video);
        Java2DFrameConverter conv = new Java2DFrameConverter();
        try {
            g.start();
            if (desde > 0) g.setTimestamp((long) (desde * 1_000_000));

            long relojIni = System.nanoTime();
            double tsIni = -1;
            Frame f;
            while (gen == generacion && (f = g.grabImage()) != null) {
                double ts = g.getTimestamp() / 1e6;
                if (tsIni < 0) tsIni = ts;
                BufferedImage img = reducir(conv.convert(f));

                double objetivo = (ts - tsIni) / vel;
                double real = (System.nanoTime() - relojIni) / 1e9;
                if (objetivo > real) Thread.sleep((long) ((objetivo - real) * 1000));
                if (gen != generacion) break;

                posicion = ts;
                SwingUtilities.invokeLater(() -> {
                    if (gen != generacion) return;
                    tMostrado = ts;
                    pantalla.setImagen(img);
                    lbTiempo.setText(String.format("%.1f s", ts));
                    alAvanzar.accept(ts);
                });
            }
            if (gen == generacion) {
                reproduciendo = false;
                alFinal = true;
                SwingUtilities.invokeLater(() -> {
                    bPlay.setText("▶  Reproducir");
                    alTerminar.run();
                });
            }
        } catch (Exception ex) {
            reproduciendo = false;
            SwingUtilities.invokeLater(() -> {
                bPlay.setText("▶  Reproducir");
                lbTiempo.setText("Error: " + ex.getMessage());
            });
        } finally {
            try { g.stop(); g.release(); } catch (Exception ignorar) { }
        }
    }

    private static BufferedImage reducir(BufferedImage src) {
        double k = Math.min(1.0, (double) ANCHO_MAX / src.getWidth());
        int w = Math.max(1, (int) (src.getWidth() * k)), h = Math.max(1, (int) (src.getHeight() * k));
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    private final class PantallaVideo extends JPanel {
        private BufferedImage img;

        PantallaVideo() { setBackground(Color.BLACK); }

        void setImagen(BufferedImage i) { img = i; repaint(); }

        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            if (img == null) return;
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            double esc = Math.min((double) getWidth() / img.getWidth(), (double) getHeight() / img.getHeight());
            int w = (int) (img.getWidth() * esc), h = (int) (img.getHeight() * esc);
            int ox = (getWidth() - w) / 2, oy = (getHeight() - h) / 2;
            g.drawImage(img, ox, oy, w, h, null);

            Rectangle zr = zonaPorTiempo != null ? zonaPorTiempo.apply(tMostrado) : zonaOriginal;
            if (zr != null) {
                double k = esc * img.getWidth() / (double) anchoOriginal;
                g.setColor(new Color(220, 30, 30));
                g.setStroke(new BasicStroke(2f));
                g.drawRect(ox + (int) (zr.x * k), oy + (int) (zr.y * k),
                           (int) (zr.width * k), (int) (zr.height * k));
            }
        }
    }
}