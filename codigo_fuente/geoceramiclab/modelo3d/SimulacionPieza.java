package geoceramiclab.modelo3d;

import geoceramiclab.Tema;
import geoceramiclab.model.BaldosinCatalan;
import geoceramiclab.model.BaldosinCatalan.TipoCara;

import java.awt.*;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static geoceramiclab.modelo3d.AnimUtil.*;

/**
 * Paso 3 de la ayuda: animacion "tipo video" de la pantalla Pieza.
 * Se escriben ancho, largo, espesor y nervios, se pulsa «Generar» y aparecen
 * el modelo 3D (girando suavemente) y las secciones acotadas.
 */
final class SimulacionPieza {

    static final long DUR = 11000;
    private static final Locale ES = new Locale("es", "CO");

    private static final String ANCHO = "10", LARGO = "20", ESP = "1.5", NERV = "13";

    private static final Color C_TOP  = new Color(0xB4, 0x4E, 0x16);
    private static final Color C_MID  = new Color(0x93, 0x3D, 0x0E);
    private static final Color C_DARK = new Color(0x6A, 0x2A, 0x08);
    private static final Color C_LINEA = new Color(0x4A, 0x1C, 0x05);

    private final BaldosinCatalan b;
    private final double[][] nervios;   // {x0, ancho} de cada nervio, en cm

    SimulacionPieza() {
        b = new BaldosinCatalan(TipoCara.NERVADA_UNA);
        b.ancho = Double.parseDouble(ANCHO);
        b.largo = Double.parseDouble(LARGO);
        b.espesorNominal = Double.parseDouble(ESP);
        b.numNerviosInt = Integer.parseInt(NERV);
        b.calcularGeometria();

        // Nervio de borde + N nervios interiores repartidos con ranuras iguales
        int n = Math.max(1, b.numNerviosInt);
        double nb = b.nervBorde > 0 ? b.nervBorde : 0.3;
        double nc = b.nervCentral > 0 ? b.nervCentral : 0.3;
        double r = (b.ancho - 2 * nb - n * nc) / (n + 1);
        if (r <= 0) { r = b.ancho / (2.0 * n + 3); nb = r; nc = r; }
        nervios = new double[n + 2][];
        nervios[0] = new double[]{0, nb};
        double x = nb + r;
        for (int i = 0; i < n; i++) { nervios[i + 1] = new double[]{x, nc}; x += nc + r; }
        nervios[n + 1] = new double[]{b.ancho - nb, nb};
    }

    void pintar(Graphics2D g, long ms, int w, int h) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        double s = (ms % DUR) / (double) DUR;

        // ---- barra de datos
        double[] tA = {0.05, 0.10}, tL = {0.13, 0.18}, tE = {0.21, 0.27}, tN = {0.30, 0.35};
        int y0 = 4;
        Rectangle fA = campo(g, 8,   y0, "Ancho cm", escribiendo(ANCHO, s, tA[0], tA[1]), 50, 38, en(s, tA), caret(s, tA), null);
        Rectangle fL = campo(g, 108, y0, "Largo cm", escribiendo(LARGO, s, tL[0], tL[1]), 48, 38, en(s, tL), caret(s, tL), null);
        Rectangle fE = campo(g, 206, y0, "Esp. cm",  escribiendo(ESP,   s, tE[0], tE[1]), 42, 40, en(s, tE), caret(s, tE), null);
        Rectangle fN = campo(g, 300, y0, "Nervios",  escribiendo(NERV,  s, tN[0], tN[1]), 42, 36, en(s, tN), caret(s, tN), null);
        boolean clic = s >= 0.43 && s < 0.46;
        Rectangle bG = boton(g, 392, y0 + 1, "Generar", Tema.ACENTO, clic);
        boton(g, bG.x + bG.width + 8, y0 + 1, "Cocción 365°", new Color(0x2A, 0x9D, 0x9F), false);

        // ---- franja de resumen
        boolean gen = s >= 0.46;
        g.setColor(Tema.SUPERFICIE_2);
        g.fillRoundRect(8, 30, w - 16, 18, 6, 6);
        g.setFont(Tema.ui(Font.BOLD, 11));
        g.setColor(Tema.ACENTO_CLARO);
        g.drawString("Baldosín", 16, 43);
        if (gen) {
            String info = String.format(ES,
                    "%.0f×%.0f cm  |  base = %.1f mm  |  nervios = %.1f mm  |  %d nerv. int.  |  Vol. neto = %.1f cm³",
                    b.largo, b.ancho, b.espesor * 10, b.altoNervio * 10, b.numNerviosInt, b.volNeto);
            g.setFont(Tema.ui(Font.PLAIN, 10));
            g.setColor(Tema.OK);
            g.drawString(escribiendo(info, s, 0.46, 0.53), 78, 43);
        }

        // ---- vista 3D
        Rectangle v3 = new Rectangle(8, 54, 280, h - 54 - 28);
        g.setColor(Tema.GRAFICA_FONDO);
        g.fillRect(v3.x, v3.y, v3.width, v3.height);
        Shape clip = g.getClip();
        g.clipRect(v3.x, v3.y, v3.width, v3.height);
        g.setColor(Tema.GRAFICA_REJILLA);
        for (int k = -v3.height; k < v3.width; k += 22) {
            g.drawLine(v3.x + k, v3.y + v3.height, v3.x + k + v3.height, v3.y);
            g.drawLine(v3.x + k, v3.y, v3.x + k + v3.height, v3.y + v3.height);
        }
        if (gen) {
            double esc = tramo(s, 0.46, 0.54);
            double th = -0.55 + 0.30 * Math.sin((s - 0.46) * Math.PI * 2 * 1.3);
            Graphics2D g3 = (Graphics2D) g.create();
            g3.translate(v3.x + v3.width / 2.0, v3.y + v3.height / 2.0);
            g3.scale(0.4 + 0.6 * esc, 0.4 + 0.6 * esc);
            g3.translate(-v3.width / 2.0, -v3.height / 2.0);
            dibujar3D(g3, v3.width, v3.height, th);
            g3.dispose();
        } else {
            g.setFont(Tema.ui(Font.ITALIC, 11));
            g.setColor(Tema.TEXTO_3);
            String t = "Pulsa «Generar» para ver la pieza";
            g.drawString(t, v3.x + (v3.width - g.getFontMetrics().stringWidth(t)) / 2, v3.y + v3.height / 2);
        }
        g.setClip(clip);
        g.setFont(Tema.ui(Font.PLAIN, 9));
        g.setColor(Tema.TEXTO_3);
        g.drawString("3D · izq = rotar · scroll = zoom", v3.x + 6, v3.y + 12);
        g.setColor(Tema.BORDE);
        g.drawRect(v3.x, v3.y, v3.width, v3.height);

        // ---- secciones acotadas
        Rectangle sc = new Rectangle(296, 54, w - 296 - 8, h - 54 - 28);
        g.setColor(Tema.FONDO);
        g.fillRect(sc.x, sc.y, sc.width, sc.height);
        g.setColor(Tema.BORDE);
        g.drawRect(sc.x, sc.y, sc.width, sc.height);
        int cw = sc.width / 2, ch1 = (int) (sc.height * 0.62);
        if (s >= 0.56) caraLisa(g, sc.x, sc.y, cw, ch1, tramo(s, 0.56, 0.60));
        if (s >= 0.62) caraNervada(g, sc.x + cw, sc.y, cw, ch1, tramo(s, 0.62, 0.66));
        if (s >= 0.68) corte(g, sc.x, sc.y + ch1, cw, sc.height - ch1, tramo(s, 0.68, 0.72));
        if (s >= 0.74) detalle(g, sc.x + cw, sc.y + ch1, cw, sc.height - ch1, tramo(s, 0.74, 0.78));

        // ---- cursor
        if (s < 0.50) {
            double[][] par = {
                {0.02, fA.x + 12, fA.y + 10}, {0.045, fA.x + 12, fA.y + 10},
                {0.12, fL.x + 12, fL.y + 10}, {0.20, fE.x + 12, fE.y + 10},
                {0.29, fN.x + 12, fN.y + 10}, {0.36, fN.x + 12, fN.y + 10},
                {0.43, bG.x + bG.width * 0.6, bG.y + 12}};
            double[] p = trayecto(s, par);
            cursor(g, p[0], p[1], clic);
        }

        // ---- aviso final
        if (s >= 0.80) {
            double a = tramo(s, 0.80, 0.84) * (1 - tramo(s, 0.97, 1.0));
            globo(g, 24, 70, 150, 150, "Estas medidas son la única fuente",
                  "las usan la dosificación y los ensayos", Tema.AVISO, a);
        }

        // ---- leyenda
        String ley;
        if (s < 0.38)      ley = "1 · Escribe ancho, largo, espesor TOTAL y número de nervios";
        else if (s < 0.46) ley = "2 · Pulsa «Generar»";
        else               ley = "3 · Modelo 3D y secciones acotadas · el espesor se reparte en base (2/3) y nervio (1/3)";
        leyenda(g, 8, h - 23, w - 16, ley);

        if (s < 0.025) {
            g.setColor(alfa(Tema.SUPERFICIE, (int) (255 * (1 - s / 0.025))));
            g.fillRect(0, 0, w, h);
        }
    }

    private static boolean en(double s, double[] t) { return s >= t[0] - 0.02 && s < t[1] + 0.02; }

    private static boolean caret(double s, double[] t) {
        return en(s, t) && ((long) (s * DUR / 280)) % 2 == 0;
    }

    // ------------------------------------------------------------ 3D isometrico

    private static final double COS30 = 0.8660254, VE = 2.6;

    private static final class Cara implements Comparable<Cara> {
        final Path2D.Double p = new Path2D.Double();
        double prof; Color c;
        @Override public int compareTo(Cara o) { return Double.compare(prof, o.prof); }
    }

    private static double[] proy(double x, double y, double z, double th) {
        double c = Math.cos(th), sn = Math.sin(th);
        double xr = x * c - y * sn, yr = x * sn + y * c;
        return new double[]{(xr - yr) * COS30, (xr + yr) * 0.5 - z, xr + yr + z};
    }

    private void dibujar3D(Graphics2D g, int w, int h, double th) {
        double W = b.ancho, L = b.largo, t = b.espesor * VE, hn = b.altoNervio * VE;
        double R = Math.sqrt(W * W + L * L) / 2.0;
        double esc = Math.min(w / (2 * Math.sqrt(2) * R * COS30), h / (Math.sqrt(2) * R + t + hn)) * 0.92;
        double ox = w / 2.0, oy = h / 2.0 + (t + hn) * esc / 2.0;
        double x0 = -W / 2.0, y0 = -L / 2.0;

        List<Cara> caras = new ArrayList<>();
        caja(caras, th, ox, oy, esc, x0, y0, 0, W, L, t);
        for (double[] n : nervios) caja(caras, th, ox, oy, esc, x0 + n[0], y0, t, n[1], L, hn);
        Collections.sort(caras);
        g.setStroke(new BasicStroke(0.8f));
        for (Cara c : caras) {
            g.setColor(c.c);
            g.fill(c.p);
            g.setColor(C_LINEA);
            g.draw(c.p);
        }
    }

    private static void caja(List<Cara> lista, double th, double ox, double oy, double s,
                             double x0, double y0, double z0, double dx, double dy, double dz) {
        double[][] v = new double[8][];
        for (int iz = 0; iz < 2; iz++)
            for (int iy = 0; iy < 2; iy++)
                for (int ix = 0; ix < 2; ix++)
                    v[ix + 2 * iy + 4 * iz] = new double[]{x0 + ix * dx, y0 + iy * dy, z0 + iz * dz};
        int[][] idx = {{0, 1, 3, 2}, {4, 5, 7, 6}, {0, 1, 5, 4}, {2, 3, 7, 6}, {0, 2, 6, 4}, {1, 3, 7, 5}};
        Color[] col = {C_DARK, C_TOP, C_MID, C_MID, C_DARK, C_DARK};
        for (int f = 0; f < 6; f++) {
            Cara c = new Cara();
            double px = 0, py = 0, pz = 0;
            for (int k = 0; k < 4; k++) {
                double[] q = v[idx[f][k]];
                double[] pr = proy(q[0], q[1], q[2], th);
                double X = ox + pr[0] * s, Y = oy + pr[1] * s;
                if (k == 0) c.p.moveTo(X, Y); else c.p.lineTo(X, Y);
                px += q[0]; py += q[1]; pz += q[2];
            }
            c.p.closePath();
            c.prof = proy(px / 4, py / 4, pz / 4, th)[2];
            c.c = col[f];
            lista.add(c);
        }
    }

    // ------------------------------------------------------------ secciones 2D

    private static void titulo(Graphics2D g, String t, int cx, int y, double a) {
        g.setFont(Tema.ui(Font.BOLD, 10));
        g.setColor(mezcla(Tema.TEXTO, Tema.FONDO, a));
        g.drawString(t, cx - g.getFontMetrics().stringWidth(t) / 2, y);
    }

    private static void cota(Graphics2D g, String t, int x1, int y1, int x2, int y2, boolean vertical, double a) {
        g.setColor(mezcla(Tema.TEXTO_2, Tema.FONDO, a));
        g.drawLine(x1, y1, x2, y2);
        if (vertical) { g.drawLine(x1 - 3, y1, x1 + 3, y1); g.drawLine(x2 - 3, y2, x2 + 3, y2); }
        else          { g.drawLine(x1, y1 - 3, x1, y1 + 3); g.drawLine(x2, y2 - 3, x2, y2 + 3); }
        g.setFont(Tema.ui(Font.PLAIN, 9));
        FontMetrics fm = g.getFontMetrics();
        if (vertical) g.drawString(t, x1 + 5, (y1 + y2) / 2 + 4);
        else          g.drawString(t, (x1 + x2 - fm.stringWidth(t)) / 2, y1 + 11);
    }

    private void caraLisa(Graphics2D g, int x, int y, int w, int h, double a) {
        titulo(g, "Cara lisa", x + w / 2, y + 13, a);
        int rh = h - 42, rw = (int) Math.round(rh * b.ancho / b.largo);
        int rx = x + (w - rw) / 2, ry = y + 20;
        g.setColor(mezcla(C_MID, Tema.FONDO, a));
        g.fillRect(rx, ry, rw, rh);
    }

    private void caraNervada(Graphics2D g, int x, int y, int w, int h, double a) {
        titulo(g, "Cara nervada", x + w / 2, y + 13, a);
        int rh = h - 42, rw = (int) Math.round(rh * b.ancho / b.largo);
        int rx = x + (w - rw) / 2 - 14, ry = y + 20;
        g.setColor(mezcla(C_DARK, Tema.FONDO, a));
        g.fillRect(rx, ry, rw, rh);
        g.setColor(mezcla(C_TOP, Tema.FONDO, a));
        for (double[] n : nervios) {
            int nx = rx + (int) Math.round(n[0] / b.ancho * rw);
            int nw = Math.max(1, (int) Math.round(n[1] / b.ancho * rw));
            g.fillRect(nx, ry, nw, rh);
        }
        cota(g, String.format(ES, "h = %.1f cm", b.largo), rx + rw + 10, ry, rx + rw + 10, ry + rh, true, a);
        cota(g, String.format(ES, "b = %.1f cm", b.ancho), rx, ry + rh + 4, rx + rw, ry + rh + 4, false, a);
    }

    private void corte(Graphics2D g, int x, int y, int w, int h, double a) {
        titulo(g, "Corte lateral", x + w / 2, y + 12, a);
        int cw = w - 40, cx = x + 20;
        double esc = cw / b.ancho;
        int tb = Math.max(4, (int) Math.round(b.espesor * esc * 1.6));
        int tn = Math.max(3, (int) Math.round(b.altoNervio * esc * 1.6));
        int by = y + 20 + tn;
        g.setColor(mezcla(C_MID, Tema.FONDO, a));
        g.fillRect(cx, by, cw, tb);
        g.setColor(mezcla(C_TOP, Tema.FONDO, a));
        for (double[] n : nervios) {
            int nx = cx + (int) Math.round(n[0] * esc);
            g.fillRect(nx, by - tn, Math.max(1, (int) Math.round(n[1] * esc)), tn);
        }
        cota(g, String.format(ES, "%.1f cm", b.ancho), cx, by + tb + 5, cx + cw, by + tb + 5, false, a);
    }

    private void detalle(Graphics2D g, int x, int y, int w, int h, double a) {
        titulo(g, "Detalle de nervios", x + w / 2, y + 14, a);
        int dw = 50, tn = 10, tb = 20;
        int dx = x + w - dw - 22, dy = y + 24;
        g.setColor(mezcla(C_MID, Tema.FONDO, a));
        g.fillRect(dx, dy + tn, dw, tb);
        g.setColor(mezcla(C_TOP, Tema.FONDO, a));
        g.fillRect(dx, dy, 10, tn);
        g.fillRect(dx + 20, dy, 8, tn);
        g.fillRect(dx + 38, dy, 8, tn);
        // cotas a la izquierda del dibujo
        int lx = dx - 8;
        g.setColor(mezcla(Tema.TEXTO_2, Tema.FONDO, a));
        g.drawLine(lx, dy, lx, dy + tn + tb);
        g.drawLine(lx - 3, dy, lx + 3, dy);
        g.drawLine(lx - 3, dy + tn, lx + 3, dy + tn);
        g.drawLine(lx - 3, dy + tn + tb, lx + 3, dy + tn + tb);
        g.setFont(Tema.ui(Font.PLAIN, 9));
        FontMetrics fm = g.getFontMetrics();
        String hn = String.format(ES, "nervio = %.1f mm", b.altoNervio * 10);
        String eb = String.format(ES, "base = %.1f mm", b.espesor * 10);
        g.drawString(hn, lx - 6 - fm.stringWidth(hn), dy + tn / 2 + 4);
        g.drawString(eb, lx - 6 - fm.stringWidth(eb), dy + tn + tb / 2 + 4);
    }
}
