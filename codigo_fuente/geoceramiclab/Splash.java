package geoceramiclab;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class Splash extends JWindow {

    private static final int ANCHO = 640, ALTO = 460;

    private static final long MINIMO_MS = 5000;
    private static final int  RED_MS    = 700;

    private final Lienzo lienzo = new Lienzo();
    private Timer animador;
    private final long nacido = System.currentTimeMillis();

    private String estado = "Iniciando…";
    private double progreso = 0, progresoMostrado = 0, brillo = 0;
    private float  opacidad = 1f;
    private boolean cerrando = false, terminado = false;
    private boolean efectos = true;
    private Runnable despues;

    public static Splash mostrar() {
        try {
            Splash s = new Splash();
            s.setVisible(true);
            return s;
        } catch (Throwable t) {
            System.err.println("[SDIM] No se pudo mostrar el splash: " + t);
            return null;
        }
    }

    private Splash() {
        setSize(ANCHO, ALTO);
        setLocationRelativeTo(null);
        setContentPane(lienzo);

        if (Boolean.getBoolean("sdim.sinefectos")) efectos = false;

        try {
            if (!efectos) throw new UnsupportedOperationException("desactivado");
            setBackground(new Color(0, 0, 0, 0));
        } catch (Throwable t) {
            efectos = false;
            try { setBackground(Color.BLACK); } catch (Throwable ignorada) { }
        }
        if (efectos) {
            try { setShape(new RoundRectangle2D.Double(0, 0, ANCHO, ALTO, 22, 22)); }
            catch (Throwable t) {  }
            try { setOpacity(0f); opacidad = 0f; }
            catch (Throwable t) { efectos = false; opacidad = 1f; }
        }

        try { if (!Marca.iconos().isEmpty()) setIconImages(Marca.iconos()); }
        catch (Throwable ignorada) { }

        animador = new Timer(16, e -> latido());
        animador.start();
    }

    public void paso(String texto, double avance) {
        SwingUtilities.invokeLater(() -> {
            if (texto != null) estado = texto;
            progreso = Math.max(0, Math.min(1, avance));
        });
    }

    public void cerrar(Runnable alTerminar) {
        SwingUtilities.invokeLater(() -> {
            if (cerrando) return;
            cerrando = true;
            despues = alTerminar;
            progreso = 1;

            long falta = Math.max(0, MINIMO_MS - (System.currentTimeMillis() - nacido));
            Timer arranque = new Timer((int) falta + 1, e -> {
                desvaneciendo = true;
                Timer red = new Timer(RED_MS, x -> cerrarYa());
                red.setRepeats(false);
                red.start();
            });
            arranque.setRepeats(false);
            arranque.start();
        });
    }

    private boolean desvaneciendo = false;

    public void cerrarYa() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::cerrarYa);
            return;
        }
        if (terminado) return;
        terminado = true;
        if (animador != null) animador.stop();
        setVisible(false);
        dispose();
        if (despues != null) {
            Runnable r = despues;
            despues = null;
            SwingUtilities.invokeLater(r);
        }
    }

    private void latido() {
        if (terminado) return;

        if (!desvaneciendo) {
            if (efectos && opacidad < 1f) {
                opacidad = Math.min(1f, opacidad + 0.12f);
                aplicarOpacidad(opacidad);
            }
        } else {
            if (!efectos) { cerrarYa(); return; }
            opacidad -= 0.10f;
            if (opacidad <= 0f) { cerrarYa(); return; }
            aplicarOpacidad(opacidad);
        }

        double porReloj = Math.min(1.0,
                (System.currentTimeMillis() - nacido) / (MINIMO_MS * 0.85));
        double objetivo = desvaneciendo ? 1.0 : Math.min(progreso, porReloj);
        progresoMostrado += (objetivo - progresoMostrado) * (desvaneciendo ? 0.35 : 0.13);
        if (progresoMostrado > 0.999) progresoMostrado = 1.0;

        brillo += 0.016;
        if (brillo > 1.35) brillo = -0.35;
        lienzo.repaint();
    }

    private void aplicarOpacidad(float v) {
        try { setOpacity(Math.max(0f, Math.min(1f, v))); }
        catch (Throwable t) { efectos = false; }
    }

    private class Lienzo extends JPanel {

        Lienzo() { setOpaque(false); }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                               RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                               RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            fondo(g, w, h);
            int abajoDelLogo = logo(g, w, h);
            barra(g, w, h);
            textos(g, w, h, abajoDelLogo);
            marco(g, w, h);
            g.dispose();
        }

        private void fondo(Graphics2D g, int w, int h) {

            g.setColor(Marca.NEGRO);
            g.fillRoundRect(0, 0, w, h, efectos ? 22 : 0, efectos ? 22 : 0);
        }

        private int logo(Graphics2D g, int w, int h) {
            Image l = null;
            try { l = Marca.logo(); } catch (Throwable ignorada) { }
            int techo = 26, disponible = (int) (h * 0.50);

            if (l != null) {
                int iw = l.getWidth(null), ih = l.getHeight(null);
                if (iw > 0 && ih > 0) {
                    double k = Math.min((w - 80) / (double) iw, disponible / (double) ih);
                    int nw = (int) Math.round(iw * k), nh = (int) Math.round(ih * k);
                    g.drawImage(l, (w - nw) / 2, techo, nw, nh, null);
                    return techo + nh;
                }
            }
            return logoTipografico(g, w, techo, disponible);
        }

        private int logoTipografico(Graphics2D g, int w, int techo, int disponible) {
            int y = techo + disponible / 2;
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 76));
            FontMetrics fm = g.getFontMetrics();
            int sw = fm.stringWidth(Marca.SIGLA), x = (w - sw) / 2;
            g.setPaint(new GradientPaint(x, 0, Marca.ACERO_CLARO, x + sw, 0, Marca.PLATA_CLARA));
            g.drawString(Marca.SIGLA, x, y);

            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            g.setColor(Marca.GRIS_TEXTO);
            centrar(g, Marca.LEMA, w, y + 26);
            return y + 44;
        }

        private void barra(Graphics2D g, int w, int h) {
            int bw = w - 140, bx = (w - bw) / 2, by = h - 104, bh = 6;

            g.setColor(new Color(0x1C, 0x20, 0x25));
            g.fillRoundRect(bx, by, bw, bh, bh, bh);

            int llenado = (int) Math.round(bw * progresoMostrado);
            if (llenado > 2) {
                g.setPaint(new GradientPaint(bx, 0, Marca.ACERO, bx + bw, 0, Marca.PLATA_CLARA));
                g.fillRoundRect(bx, by, llenado, bh, bh, bh);
            }
            if (llenado > 24) {
                Shape antes = g.getClip();
                g.clipRect(bx, by, llenado, bh);
                int cx = bx + (int) (brillo * llenado);
                g.setPaint(new GradientPaint(cx - 55, 0, new Color(255, 255, 255, 0),
                                             cx,      0, new Color(255, 255, 255, 110), true));
                g.fillRect(cx - 55, by, 110, bh);
                g.setClip(antes);
            }
        }

        private void textos(Graphics2D g, int w, int h, int abajoDelLogo) {
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 17));
            g.setColor(new Color(0xE4, 0xE4, 0xE4));
            centrar(g, Marca.SUBAPP, w, Math.min(abajoDelLogo + 34, h - 172));

            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            g.setColor(new Color(0x86, 0x8E, 0x96));
            centrar(g, "versión " + Marca.VERSION, w, Math.min(abajoDelLogo + 56, h - 150));

            int pct = (int) Math.round(progresoMostrado * 100);
            String txt = pct + " %";
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
            FontMetrics fm = g.getFontMetrics();
            int tw = fm.stringWidth(txt), tx = (w - tw) / 2;
            g.setPaint(new GradientPaint(tx, 0, Marca.ACERO_CLARO,
                                         tx + tw, 0, Marca.PLATA_CLARA));
            g.drawString(txt, tx, h - 70);

            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            g.setColor(Marca.GRIS_TEXTO);
            centrar(g, estado, w, h - 46);

            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
            g.setColor(new Color(0x5A, 0x5A, 0x5A));
            centrar(g, "versión " + Marca.VERSION, w, h - 22);
        }

        private void marco(Graphics2D g, int w, int h) {
            g.setStroke(new BasicStroke(1.2f));
            g.setPaint(new GradientPaint(0, 0, new Color(0x3C, 0x5A, 0x73, 150),
                                         w, h, new Color(0xB4, 0xC0, 0xCC, 110)));
            g.drawRoundRect(0, 0, w - 1, h - 1, efectos ? 22 : 0, efectos ? 22 : 0);
        }

        private void centrar(Graphics2D g, String s, int w, int y) {
            if (s == null) return;
            g.drawString(s, (w - g.getFontMetrics().stringWidth(s)) / 2, y);
        }
    }
}