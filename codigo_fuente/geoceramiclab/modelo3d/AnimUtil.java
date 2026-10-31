package geoceramiclab.modelo3d;

import geoceramiclab.Tema;

import java.awt.*;
import java.awt.geom.Path2D;

/** Piezas comunes de las animaciones "tipo video" de la ayuda. */
final class AnimUtil {

    private AnimUtil() { }

    static double clamp01(double v) { return Math.max(0, Math.min(1, v)); }

    /** Avance 0..1 de s entre a y b, suavizado. */
    static double tramo(double s, double a, double b) {
        double k = clamp01((s - a) / (b - a));
        return k * k * (3 - 2 * k);
    }

    static Color mezcla(Color a, Color b, double t) {
        t = clamp01(t);
        return new Color(
            (int) Math.round(a.getRed()   * t + b.getRed()   * (1 - t)),
            (int) Math.round(a.getGreen() * t + b.getGreen() * (1 - t)),
            (int) Math.round(a.getBlue()  * t + b.getBlue()  * (1 - t)));
    }

    static Color alfa(Color c, int a) { return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a))); }

    /** Texto parcialmente escrito entre los tiempos a y b. */
    static String escribiendo(String txt, double s, double a, double b) {
        if (s <= a) return "";
        if (s >= b) return txt;
        int n = (int) Math.floor((s - a) / (b - a) * txt.length());
        return txt.substring(0, Math.max(0, Math.min(txt.length(), n)));
    }

    /** Flecha de raton en (x, y). Si pulsando, dibuja un pequeno destello. */
    static void cursor(Graphics2D g, double x, double y, boolean pulsando) {
        int cx = (int) Math.round(x), cy = (int) Math.round(y);
        if (pulsando) {
            g.setColor(alfa(Tema.ACENTO_CLARO, 120));
            g.fillOval(cx - 9, cy - 9, 18, 18);
        }
        Path2D.Double p = new Path2D.Double();
        p.moveTo(cx, cy);
        p.lineTo(cx, cy + 15);
        p.lineTo(cx + 4, cy + 11);
        p.lineTo(cx + 7, cy + 17);
        p.lineTo(cx + 9, cy + 16);
        p.lineTo(cx + 6, cy + 10);
        p.lineTo(cx + 11, cy + 10);
        p.closePath();
        g.setStroke(new BasicStroke(1f));
        g.setColor(Color.WHITE);
        g.fill(p);
        g.setColor(Color.BLACK);
        g.draw(p);
    }

    /** Posicion del cursor que viaja por una lista de paradas {t, x, y} (t creciente). */
    static double[] trayecto(double s, double[][] paradas) {
        if (s <= paradas[0][0]) return new double[]{paradas[0][1], paradas[0][2]};
        for (int i = 1; i < paradas.length; i++) {
            if (s <= paradas[i][0]) {
                double k = tramo(s, paradas[i - 1][0], paradas[i][0]);
                return new double[]{
                    paradas[i - 1][1] + (paradas[i][1] - paradas[i - 1][1]) * k,
                    paradas[i - 1][2] + (paradas[i][2] - paradas[i - 1][2]) * k};
            }
        }
        double[] u = paradas[paradas.length - 1];
        return new double[]{u[1], u[2]};
    }

    /** Campo de texto con etiqueta; devuelve el rectangulo del campo. */
    static Rectangle campo(Graphics2D g, int x, int y, String etiqueta, String valor, int ancEtq, int ancVal,
                           boolean activo, boolean caret, Color colorValor) {
        g.setFont(Tema.ui(Font.PLAIN, 10));
        g.setColor(Tema.TEXTO_2);
        g.drawString(etiqueta, x, y + 14);
        int fx = x + ancEtq + 4;
        g.setColor(activo ? mezcla(Tema.ACENTO, Tema.FONDO, 0.22) : Tema.SUPERFICIE_2);
        g.fillRoundRect(fx, y + 2, ancVal, 18, 4, 4);
        g.setColor(activo ? Tema.ACENTO : Tema.BORDE);
        g.drawRoundRect(fx, y + 2, ancVal, 18, 4, 4);
        g.setColor(colorValor == null ? Tema.TEXTO : colorValor);
        g.setFont(Tema.ui(Font.BOLD, 11));
        g.drawString(valor, fx + 6, y + 15);
        if (caret) {
            int px = fx + 7 + g.getFontMetrics().stringWidth(valor);
            g.setColor(Tema.ACENTO_CLARO);
            g.fillRect(px, y + 5, 1, 12);
        }
        return new Rectangle(fx, y + 2, ancVal, 18);
    }

    static Rectangle boton(Graphics2D g, int x, int y, String txt, Color fondo, boolean pulsado) {
        g.setFont(Tema.ui(Font.BOLD, 11));
        FontMetrics fm = g.getFontMetrics();
        int bw = fm.stringWidth(txt) + 22, bh = 20;
        g.setColor(pulsado ? fondo.brighter() : fondo);
        g.fillRoundRect(x, y + (pulsado ? 1 : 0), bw, bh, 6, 6);
        g.setColor(Color.WHITE);
        g.drawString(txt, x + 11, y + 14 + (pulsado ? 1 : 0));
        return new Rectangle(x, y, bw, bh);
    }

    /** Recuadro que late alrededor de un elemento para llamar la atencion. */
    static void resaltar(Graphics2D g, Rectangle r, Color c, double s) {
        double p = 0.5 + 0.5 * Math.sin(s * 60);
        int m = 3 + (int) Math.round(2 * p);
        g.setStroke(new BasicStroke(2f));
        g.setColor(alfa(c, 140 + (int) (100 * p)));
        g.drawRoundRect(r.x - m, r.y - m, r.width + 2 * m, r.height + 2 * m, 8, 8);
        g.setStroke(new BasicStroke(1f));
    }

    /**
     * Globo de aviso con flecha que apunta a (ax, ay). El globo se coloca con su esquina
     * superior izquierda en (bx, by). a = opacidad 0..1.
     */
    static void globo(Graphics2D g, int bx, int by, int ax, int ay, String titulo, String texto, Color c, double a) {
        if (a <= 0) return;
        Font ft = Tema.ui(Font.BOLD, 11), fx = Tema.ui(Font.PLAIN, 10);
        g.setFont(ft);
        int w1 = g.getFontMetrics().stringWidth(titulo);
        g.setFont(fx);
        int w2 = texto == null ? 0 : g.getFontMetrics().stringWidth(texto);
        int bw = Math.max(w1, w2) + 20, bh = texto == null ? 22 : 36;
        Composite cmp = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) clamp01(a)));
        // flecha
        int cx = Math.max(bx + 10, Math.min(bx + bw - 10, ax));
        int cy = ay < by ? by : by + bh;
        Path2D.Double f = new Path2D.Double();
        f.moveTo(cx - 6, cy);
        f.lineTo(ax, ay);
        f.lineTo(cx + 6, cy);
        f.closePath();
        g.setColor(c);
        g.fill(f);
        g.fillRoundRect(bx, by, bw, bh, 10, 10);
        g.setColor(new Color(20, 24, 30));
        g.setFont(ft);
        g.drawString(titulo, bx + 10, by + 15);
        if (texto != null) {
            g.setFont(fx);
            g.drawString(texto, bx + 10, by + 29);
        }
        g.setComposite(cmp);
    }

    /** Barra inferior con la explicacion de lo que esta pasando. */
    static void leyenda(Graphics2D g, int x, int y, int w, String txt) {
        g.setColor(Tema.SUPERFICIE_2);
        g.fillRoundRect(x, y, w, 20, 8, 8);
        g.setColor(Tema.TEXTO_2);
        g.setFont(Tema.ui(Font.PLAIN, 11));
        g.drawString(txt, x + 10, y + 14);
    }
}
