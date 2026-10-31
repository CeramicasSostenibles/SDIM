package geoceramiclab.modelo3d;

import geoceramiclab.Tema;

import java.awt.*;
import java.awt.geom.Path2D;
import java.util.Locale;

import static geoceramiclab.modelo3d.AnimUtil.*;

/**
 * Paso 5 de la ayuda: pantalla Secado y Cocción. No se llena nada: solo se
 * dibujan las curvas (secado de M1, M2, M3 y la curva de cocción) en bucle.
 */
final class SimulacionSecado {

    static final long DUR = 9000;
    private static final Locale ES = new Locale("es", "CO");

    private static final String[] NOM = {"M1 — Relave alto", "M2 — Intermedia", "M3 — Fundente alto"};
    private static final Color[] COL = {new Color(0x7F, 0xA3, 0xC2), new Color(0x4C, 0xB7, 0x82), new Color(0xE0, 0xA3, 0x3A)};
    // masa (g) por dia D0..D7
    private static final double[][] MASA = {
        {735.0, 734.6, 733.2, 710.0, 696.0, 686.0, 678.0, 668.0},
        {754.0, 753.4, 751.7, 726.0, 711.0, 699.0, 690.0, 672.0},
        {773.0, 772.3, 770.2, 742.0, 724.0, 709.0, 698.0, 677.0},
    };
    private static final String[] HUM = {"4,3 % H", "4,9 % H", "5,6 % H"};

    private static final Color C_AZUL = new Color(0x6C, 0x9C, 0xF0);
    private static final Color C_VERDE = new Color(0x5F, 0xD0, 0x9A);
    private static final Color C_CAL = new Color(0xE8, 0xA6, 0x3A);
    private static final Color C_ENF = new Color(0xF0, 0x70, 0x78);

    void pintar(Graphics2D g, long ms, int w, int h) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        double s = (ms % DUR) / (double) DUR;

        g.setFont(Tema.ui(Font.BOLD, 11));
        g.setColor(Tema.AVISO);
        g.drawString("Secado y Cocción — baldosín (dimensiones + suelo)", 8, 13);
        g.setFont(Tema.ui(Font.PLAIN, 9));
        g.setColor(Tema.TEXTO_3);
        String inf = "Espesor 15,0 mm · volumen 255 cm³ · arcilla 9,4 %";
        g.drawString(inf, w - 8 - g.getFontMetrics().stringWidth(inf), 13);

        // ---------------- SECADO
        int lx = 8, lw = 330, top = 22;
        g.setFont(Tema.ui(Font.BOLD, 10));
        g.setColor(Tema.INFO);
        g.drawString("SECADO (post-desmolde)", lx, top + 10);
        g.setFont(Tema.ui(Font.PLAIN, 9));
        g.setColor(C_AZUL); g.fillRect(lx + 170, top + 3, 7, 7);
        g.setColor(Tema.TEXTO_2); g.drawString("plástico 2 d", lx + 180, top + 10);
        g.setColor(C_VERDE); g.fillRect(lx + 246, top + 3, 7, 7);
        g.setColor(Tema.TEXTO_2); g.drawString("aire libre", lx + 256, top + 10);

        int ch = (h - top - 16 - 26) / 3;
        for (int m = 0; m < 3; m++) {
            int cy = top + 16 + m * ch;
            double k = tramo(s, 0.03 + m * 0.08, 0.40 + m * 0.08);
            secado(g, lx, cy, lw, ch - 4, m, k, s);
        }

        // ---------------- COCCION
        int rx = 348, rw = w - rx - 8;
        g.setFont(Tema.ui(Font.BOLD, 10));
        g.setColor(Tema.ERROR);
        g.drawString("COCCIÓN (horno)", rx, top + 10);
        g.setFont(Tema.ui(Font.PLAIN, 9));
        g.setColor(C_CAL); g.fillRect(rx + 120, top + 3, 7, 7);
        g.setColor(Tema.TEXTO_2); g.drawString("calentamiento", rx + 130, top + 10);
        g.setColor(C_ENF); g.fillRect(rx + 200, top + 3, 7, 7);
        g.setColor(Tema.TEXTO_2); g.drawString("enfriamiento natural", rx + 210, top + 10);

        Rectangle cc = new Rectangle(rx, top + 16, rw, 112);
        coccion(g, cc, s);
        tabla(g, rx, cc.y + cc.height + 6, rw, h - 26 - (cc.y + cc.height + 6), s);

        leyenda(g, 8, h - 23, w - 16,
                "Las curvas se generan solas para cada mezcla: secado 2 días en plástico + aire libre, y cocción con pico de 1100 °C");
    }

    private static void secado(Graphics2D g, int x, int y, int w, int h, int m, double k, double s) {
        g.setColor(Tema.SUPERFICIE);
        g.fillRect(x, y, w, h);
        g.setColor(Tema.BORDE);
        g.drawRect(x, y, w, h);
        g.setFont(Tema.ui(Font.BOLD, 10));
        g.setColor(COL[m]);
        g.drawString(NOM[m], x + 6, y + 12);
        g.setFont(Tema.ui(Font.PLAIN, 8));
        g.setColor(Tema.TEXTO_3);
        g.drawString("masa vs tiempo", x + 120, y + 12);
        double[] d = MASA[m];
        String res = String.format(ES, "7 días · %.0f g → %.0f g", d[0], d[7]);
        g.setFont(Tema.ui(Font.BOLD, 9));
        g.setColor(Tema.TEXTO);
        g.drawString(res, x + w - 6 - g.getFontMetrics().stringWidth(res), y + 12);

        int px = x + 30, py = y + 17, pw = w - 40, ph = h - 30;
        g.setColor(Tema.GRAFICA_FONDO);
        g.fillRect(px, py, pw, ph);
        // zona plastico
        int xd2 = px + pw * 2 / 7;
        g.setColor(alfa(C_AZUL, 30));
        g.fillRect(px, py, xd2 - px, ph);
        g.setColor(Tema.GRAFICA_REJILLA);
        for (int i = 0; i <= 7; i++) { int xx = px + pw * i / 7; g.drawLine(xx, py, xx, py + ph); }
        for (int i = 0; i <= 3; i++) { int yy = py + ph * i / 3; g.drawLine(px, yy, px + pw, yy); }
        double mn = d[7] - 30, mx = d[0] + 30;
        g.setFont(Tema.ui(Font.PLAIN, 8));
        g.setColor(Tema.TEXTO_3);
        g.drawString(String.format("%.0f", mx), x + 4, py + 7);
        g.drawString(String.format("%.0f", mn), x + 4, py + ph);
        for (int i = 0; i <= 7; i++) {
            String t = "D" + i;
            g.drawString(t, px + pw * i / 7 - 5, py + ph + 10);
        }

        // linea que se dibuja
        double total = 7 * k;
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 7; i++) {
            if (total <= i) break;
            double f = Math.min(1, total - i);
            double x1 = px + pw * i / 7.0, y1 = py + ph * (mx - d[i]) / (mx - mn);
            double x2 = px + pw * (i + 1) / 7.0, y2 = py + ph * (mx - d[i + 1]) / (mx - mn);
            g.setColor(i < 2 ? C_AZUL : C_VERDE);
            g.drawLine((int) x1, (int) y1, (int) (x1 + (x2 - x1) * f), (int) (y1 + (y2 - y1) * f));
        }
        g.setStroke(new BasicStroke(1f));
        for (int i = 0; i <= 7; i++) {
            if (total < i) break;
            int xx = px + pw * i / 7, yy = (int) (py + ph * (mx - d[i]) / (mx - mn));
            g.setColor(Tema.GRAFICA_FONDO);
            g.fillOval(xx - 3, yy - 3, 6, 6);
            g.setColor(i <= 2 ? C_AZUL : C_VERDE);
            g.drawOval(xx - 3, yy - 3, 6, 6);
        }
        if (k >= 1) {
            g.setFont(Tema.ui(Font.BOLD, 8));
            g.setColor(C_VERDE);
            g.drawString(HUM[m], px + pw - 34, py + ph - 4);
            g.setColor(C_AZUL);
            g.drawString(String.format(ES, "−%.2f g", d[0] - d[2]), xd2 - 18, py + 8);
        }
    }

    // T(t) de la curva de coccion (t en horas)
    private static double temp(double t) {
        if (t <= 1.35) return 25 + (200 - 25) * t / 1.35;
        if (t <= 3.57) return 200 + (600 - 200) * (t - 1.35) / (3.57 - 1.35);
        if (t <= 6.5)  return 600 + (1100 - 600) * (t - 3.57) / (6.5 - 3.57);
        if (t <= 7.0)  return 1100;
        return 30 + (1100 - 30) * Math.exp(-(t - 7.0) / 1.6);
    }

    private static void coccion(Graphics2D g, Rectangle r, double s) {
        g.setColor(Tema.SUPERFICIE);
        g.fillRect(r.x, r.y, r.width, r.height);
        g.setColor(Tema.BORDE);
        g.drawRect(r.x, r.y, r.width, r.height);
        g.setFont(Tema.ui(Font.BOLD, 10));
        g.setColor(Tema.AVISO);
        g.drawString("Todas las mezclas", r.x + 6, r.y + 12);
        g.setFont(Tema.ui(Font.BOLD, 9));
        g.setColor(Tema.TEXTO);
        String t = "1100 °C · 13 h 48 min";
        g.drawString(t, r.x + r.width - 6 - g.getFontMetrics().stringWidth(t), r.y + 12);

        int px = r.x + 34, py = r.y + 18, pw = r.width - 44, ph = r.height - 34;
        double tMax = 13.8, Tmax = 1300;
        g.setColor(Tema.GRAFICA_FONDO);
        g.fillRect(px, py, pw, ph);
        int xOff = px + (int) (pw * 7.0 / tMax);
        g.setColor(alfa(C_ENF, 22));
        g.fillRect(xOff, py, px + pw - xOff, ph);
        g.setColor(Tema.GRAFICA_REJILLA);
        for (int i = 0; i <= 6; i++) { int xx = px + (int) (pw * 2.0 * i / tMax); g.drawLine(xx, py, xx, py + ph); }
        for (int i = 0; i <= 4; i++) { int yy = py + ph * i / 4; g.drawLine(px, yy, px + pw, yy); }
        g.setFont(Tema.ui(Font.PLAIN, 8));
        g.setColor(Tema.TEXTO_3);
        for (int i = 0; i <= 4; i++) g.drawString(String.valueOf((int) (Tmax * (4 - i) / 4)), r.x + 4, py + ph * i / 4 + 3);
        for (int i = 0; i <= 6; i++) g.drawString((2 * i) + "h", px + (int) (pw * 2.0 * i / tMax) - 5, py + ph + 10);
        int y1100 = py + (int) (ph * (1 - 1100 / Tmax));
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{3, 3}, 0));
        g.setColor(Tema.TEXTO_3);
        g.drawLine(px, y1100, px + pw, y1100);
        g.setColor(C_ENF);
        g.drawLine(xOff, py, xOff, py + ph);
        g.setStroke(new BasicStroke(1f));

        double k = tramo(s, 0.05, 0.60);
        double tFin = tMax * k;
        Path2D.Double cal = new Path2D.Double(), enf = new Path2D.Double();
        boolean e0 = true;
        for (int i = 0; i <= 300; i++) {
            double tt = tMax * i / 300.0;
            if (tt > tFin) break;
            double X = px + pw * tt / tMax, Y = py + ph * (1 - temp(tt) / Tmax);
            if (tt <= 7.0) { if (i == 0) cal.moveTo(X, Y); else cal.lineTo(X, Y); }
            else { if (e0) { enf.moveTo(px + pw * 7.0 / tMax, py + ph * (1 - 1100 / Tmax)); e0 = false; } enf.lineTo(X, Y); }
        }
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(C_CAL);
        g.draw(cal);
        g.setColor(C_ENF);
        g.draw(enf);
        g.setStroke(new BasicStroke(1f));
        double[] hitos = {0, 1.35, 3.57, 6.5, 7.0};
        for (double hh : hitos) {
            if (hh > tFin) break;
            int X = px + (int) (pw * hh / tMax), Y = py + (int) (ph * (1 - temp(hh) / Tmax));
            g.setColor(Tema.GRAFICA_FONDO); g.fillOval(X - 3, Y - 3, 6, 6);
            g.setColor(C_CAL); g.drawOval(X - 3, Y - 3, 6, 6);
        }
        if (tFin >= 6.5) {
            g.setFont(Tema.ui(Font.BOLD, 9));
            g.setColor(Tema.TEXTO);
            g.drawString("1100 °C", px + (int) (pw * 6.4 / tMax), y1100 - 4);
        }
        if (tFin >= 9) {
            g.setFont(Tema.ui(Font.BOLD, 8));
            g.setColor(C_ENF);
            g.drawString("horno OFF · enfr. natural", xOff + 4, py + ph - 4);
        }
        // punto que recorre la curva una vez dibujada
        if (k >= 1) {
            double tt = tMax * ((s - 0.60) / 0.40);
            int X = px + (int) (pw * tt / tMax), Y = py + (int) (ph * (1 - temp(tt) / Tmax));
            g.setColor(alfa(tt <= 7 ? C_CAL : C_ENF, 90));
            g.fillOval(X - 7, Y - 7, 14, 14);
            g.setColor(tt <= 7 ? C_CAL : C_ENF);
            g.fillOval(X - 3, Y - 3, 6, 6);
        }
    }

    private static void tabla(Graphics2D g, int x, int y, int w, int h, double s) {
        String[][] f = {
            {"Temperatura de pico", "1100 °C (óptimo del feldespato)"},
            {"25 → 200 °C", "130 °C/h · 1 h 21 min"},
            {"200 → 600 °C", "180 °C/h · 2 h 13 min"},
            {"600 → pico", "170 °C/h · 2 h 56 min"},
            {"Permanencia en el pico", "30 min"},
            {"ENFRIAMIENTO", "horno apagado — no controlado"},
        };
        int rh = Math.min(15, (h - 14) / f.length);
        g.setColor(new Color(0xB0, 0x82, 0x2E));
        g.fillRect(x, y, w, 14);
        g.setFont(Tema.ui(Font.BOLD, 9));
        g.setColor(Color.WHITE);
        g.drawString("Etapa / parámetro", x + 6, y + 10);
        g.drawString("Valor", x + w / 2 + 6, y + 10);
        for (int i = 0; i < f.length; i++) {
            if (s < 0.10 + i * 0.07) break;
            int yy = y + 14 + i * rh;
            g.setColor(i % 2 == 0 ? Tema.SUPERFICIE : Tema.SUPERFICIE_2);
            g.fillRect(x, yy, w, rh);
            boolean ult = i == f.length - 1;
            g.setFont(Tema.ui(Font.PLAIN, 9));
            g.setColor(ult ? C_ENF : Tema.TEXTO_2);
            g.drawString(f[i][0], x + 6, yy + rh - 4);
            g.setFont(Tema.ui(Font.BOLD, 9));
            g.setColor(i == 0 ? Tema.AVISO : (ult ? C_ENF : Tema.TEXTO));
            g.drawString(f[i][1], x + w / 2 + 6, yy + rh - 4);
        }
    }
}
