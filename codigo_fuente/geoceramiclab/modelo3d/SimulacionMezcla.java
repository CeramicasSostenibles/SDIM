package geoceramiclab.modelo3d;

import geoceramiclab.Tema;
import geoceramiclab.model.BaldosinCatalan;
import geoceramiclab.model.BaldosinCatalan.TipoCara;

import java.awt.*;
import java.util.Locale;

import static geoceramiclab.modelo3d.AnimUtil.*;

/**
 * Paso 4 de la ayuda: animacion "tipo video" del Diseño de mezcla.
 *   1) Señala que la DENSIDAD se puede cambiar y la cambia (1.85 → 1.90).
 *   2) Señala que la DOSIFICACION es SUGERIDA por SDIM y que se puede cambiar:
 *      edita M1 (relave 65 → 60, caolín 30 → 35) y el Σ pasa de 95 % a 100 %.
 *   3) Pulsa «Calcular 3 mezclas»: las balanzas se llenan y salen los gramos y el agua.
 */
final class SimulacionMezcla {

    static final long DUR = 14000;
    private static final Locale ES = new Locale("es", "CO");

    private static final Color C_M1 = new Color(0x7F, 0xA3, 0xC2);
    private static final Color C_M2 = new Color(0x4C, 0xB7, 0x82);
    private static final Color C_M3 = new Color(0xE0, 0xA3, 0x3A);
    private static final Color[] C_MAT = {
        new Color(0xA8, 0x86, 0x66),   // relave
        new Color(0xC9, 0xD3, 0xDB),   // caolin
        new Color(0xD8, 0xCF, 0xC0),   // feldespato
        new Color(0x2F, 0x6F, 0xD0)    // agua
    };

    // Dosificacion sugerida {relave, caolin, feldespato, agua}
    private static final double[][] SUG = {{65, 30, 5, 15}, {55, 35, 10, 18}, {45, 40, 15, 21}};
    private static final double D_SUG = 1.85, D_USR = 1.90;

    private final double vol;

    SimulacionMezcla() {
        BaldosinCatalan b = new BaldosinCatalan(TipoCara.NERVADA_UNA);
        b.ancho = 10; b.largo = 20; b.espesorNominal = 1.5; b.numNerviosInt = 13;
        b.calcularGeometria();
        vol = b.volNeto > 0 ? b.volNeto : 255.0;
    }

    // Tiempos (fraccion de la escena)
    private static final double T_G1 = 0.04, T_CLIC_D = 0.15, T_TIPO_D0 = 0.17, T_TIPO_D1 = 0.22, T_G1_FIN = 0.30;
    private static final double T_G2 = 0.31, T_CLIC_R = 0.42, T_TIPO_R0 = 0.43, T_TIPO_R1 = 0.46;
    private static final double T_CLIC_C = 0.51, T_TIPO_C0 = 0.52, T_TIPO_C1 = 0.55, T_G2_FIN = 0.62;
    private static final double T_CLIC_CALC = 0.68, T_RES = 0.71;

    void pintar(Graphics2D g, long ms, int w, int h) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        double s = (ms % DUR) / (double) DUR;

        // ---- valores segun el momento
        boolean dCambiada = s >= T_TIPO_D1;
        double dens = dCambiada ? D_USR : D_SUG;
        String txtDens;
        if (s < T_CLIC_D)       txtDens = "1.85";
        else if (s < T_TIPO_D0) txtDens = "1.85";                 // seleccionado
        else                    txtDens = escribiendo("1.90", s, T_TIPO_D0, T_TIPO_D1);
        double masa = dens * vol;

        double rel = s >= T_TIPO_R1 ? 60 : 65;
        double cao = s >= T_TIPO_C1 ? 35 : 30;
        String txtRel = s < T_CLIC_R ? "65,0" : (s < T_TIPO_R0 ? "65,0" : escribiendo("60,0", s, T_TIPO_R0, T_TIPO_R1));
        String txtCao = s < T_CLIC_C ? "30,0" : (s < T_TIPO_C0 ? "30,0" : escribiendo("35,0", s, T_TIPO_C0, T_TIPO_C1));
        double[][] dos = {{rel, cao, SUG[0][2], SUG[0][3]}, SUG[1], SUG[2]};

        // ---- barra superior: mezclas y boton
        int x = 8, y = 4;
        String[] mz = {"M1 — Relave alto", "M2 — Intermedia", "M3 — Fundente alto"};
        Color[] cm = {C_M1, C_M2, C_M3};
        g.setFont(Tema.ui(Font.BOLD, 10));
        for (int k = 0; k < 3; k++) {
            int tw = g.getFontMetrics().stringWidth(mz[k]) + 16;
            g.setColor(k == 0 ? mezcla(cm[k], Tema.SUPERFICIE, 0.18) : Tema.SUPERFICIE_2);
            g.fillRect(x, y, tw, 20);
            g.setColor(cm[k]);
            g.drawRect(x, y, tw, 20);
            g.drawString(mz[k], x + 8, y + 14);
            x += tw + 6;
        }
        boolean clicCalc = s >= T_CLIC_CALC && s < T_CLIC_CALC + 0.03;
        Rectangle bCalc = boton(g, x + 10, y, "Calcular 3 mezclas", Tema.ACENTO, clicCalc);

        // ---- balanzas (izquierda)
        double kRes = tramo(s, T_RES, T_RES + 0.12);
        double[] gramos = {
            dos[0][0] / 100 * masa, dos[0][1] / 100 * masa, dos[0][2] / 100 * masa, dos[0][3] / 100 * masa};
        String[] nom = {"Relave", "Caolín", "Feldespato", "Agua"};
        int bx = 8, by = 30, bw = 82, bh = (h - 30 - 30) / 2 - 3;
        for (int k = 0; k < 4; k++) {
            int cx = bx + (k % 2) * (bw + 4), cy = by + (k / 2) * (bh + 6);
            balanza(g, cx, cy, bw, bh, nom[k], C_MAT[k], gramos[k] * kRes, k == 3, kRes);
        }

        // ---- derecha: volumen, densidad, masa
        int rx = 184, ry = 32, rw = w - rx - 8;
        g.setColor(Tema.SUPERFICIE);
        g.fillRect(rx, ry - 2, rw, 26);
        g.setFont(Tema.ui(Font.PLAIN, 10));
        g.setColor(Tema.TEXTO_2);
        g.drawString("Vol. calc.", rx + 6, ry + 14);
        g.setColor(Tema.INFO);
        g.setFont(Tema.ui(Font.BOLD, 11));
        g.drawString(String.format(ES, "%.1f cm³", vol), rx + 58, ry + 14);
        boolean dActivo = s >= T_CLIC_D && s < T_TIPO_D1 + 0.03;
        boolean selec = s >= T_CLIC_D && s < T_TIPO_D0;
        Rectangle fD = campo(g, rx + 132, ry - 1, "Densidad (g/cm³)", selec ? "" : txtDens, 92, 44,
                dActivo, dActivo && !selec && ((long) (s * DUR / 280)) % 2 == 0,
                dCambiada ? Tema.AVISO : Tema.AVISO);
        if (selec) {   // texto seleccionado (como al hacer doble clic)
            g.setColor(Tema.SELECCION);
            g.fillRect(fD.x + 4, fD.y + 3, 30, 13);
            g.setColor(Tema.TEXTO);
            g.setFont(Tema.ui(Font.BOLD, 11));
            g.drawString("1.85", fD.x + 6, fD.y + 13);
        }
        g.setFont(Tema.ui(Font.PLAIN, 10));
        g.setColor(Tema.TEXTO_2);
        g.drawString("Masa sólidos", fD.x + fD.width + 12, ry + 14);
        g.setFont(Tema.ui(Font.BOLD, 11));
        g.setColor(s >= T_TIPO_D1 && s < T_TIPO_D1 + 0.05 ? Tema.AVISO : Tema.OK);
        g.drawString(String.format(ES, "= %.2f × %.1f = %.1f g", dens, vol, masa), fD.x + fD.width + 84, ry + 14);

        // ---- tabla de dosificacion
        int ty = ry + 32;
        String[] cab = {"Dosificación", "Relave %", "Caolín %", "Feld. %", "Agua %", "Σ sólidos"};
        int[] anc = {112, 60, 60, 56, 56, 66};
        int tx = rx;
        g.setColor(Tema.SUPERFICIE_2);
        g.fillRect(rx, ty, rw, 16);
        g.setFont(Tema.ui(Font.BOLD, 10));
        g.setColor(Tema.TEXTO_2);
        for (int c = 0; c < cab.length; c++) { g.drawString(cab[c], tx + 4, ty + 12); tx += anc[c]; }
        Rectangle celRel = null, celCao = null;
        Rectangle tabla = new Rectangle(rx, ty, rw, 16 + 3 * 19);
        for (int r = 0; r < 3; r++) {
            int yy = ty + 16 + r * 19;
            tx = rx;
            g.setColor(Tema.SUPERFICIE);
            g.fillRect(rx, yy, rw, 19);
            g.setFont(Tema.ui(Font.BOLD, 10));
            g.setColor(cm[r]);
            g.drawString(mz[r], tx + 4, yy + 13);
            tx += anc[0];
            for (int c = 0; c < 4; c++) {
                String v;
                boolean act = false, caretOn = false;
                if (r == 0 && c == 0) {
                    v = txtRel; act = s >= T_CLIC_R && s < T_TIPO_R1 + 0.02;
                    caretOn = act && s >= T_TIPO_R0;
                } else if (r == 0 && c == 1) {
                    v = txtCao; act = s >= T_CLIC_C && s < T_TIPO_C1 + 0.02;
                    caretOn = act && s >= T_TIPO_C0;
                } else v = String.format(ES, "%.1f", dos[r][c]);
                Rectangle cel = new Rectangle(tx + 2, yy + 2, anc[c + 1] - 6, 15);
                if (r == 0 && c == 0) celRel = cel;
                if (r == 0 && c == 1) celCao = cel;
                boolean editado = r == 0 && ((c == 0 && s >= T_TIPO_R1) || (c == 1 && s >= T_TIPO_C1));
                g.setColor(act ? mezcla(Tema.ACENTO, Tema.FONDO, 0.25) : Tema.SUPERFICIE_2);
                g.fillRect(cel.x, cel.y, cel.width, cel.height);
                g.setColor(act ? Tema.ACENTO : Tema.BORDE);
                g.drawRect(cel.x, cel.y, cel.width, cel.height);
                boolean selCel = (r == 0 && c == 0 && s >= T_CLIC_R && s < T_TIPO_R0)
                              || (r == 0 && c == 1 && s >= T_CLIC_C && s < T_TIPO_C0);
                if (selCel) { g.setColor(Tema.SELECCION); g.fillRect(cel.x + 3, cel.y + 2, 26, 11); }
                g.setFont(Tema.ui(editado ? Font.BOLD : Font.PLAIN, 10));
                g.setColor(editado ? Tema.AVISO : Tema.TEXTO_2);
                g.drawString(v, cel.x + 5, cel.y + 11);
                if (caretOn && ((long) (s * DUR / 280)) % 2 == 0) {
                    g.setColor(Tema.ACENTO_CLARO);
                    g.fillRect(cel.x + 6 + g.getFontMetrics().stringWidth(v), cel.y + 2, 1, 11);
                }
                tx += anc[c + 1];
            }
            double suma = dos[r][0] + dos[r][1] + dos[r][2];
            boolean ok = Math.abs(suma - 100) < 0.01;
            g.setFont(Tema.ui(Font.BOLD, 11));
            g.setColor(ok ? Tema.OK : Tema.AVISO);
            g.drawString(String.format(ES, "%.1f %%", suma) + (ok ? "" : "  ⚠"), tx + 4, yy + 14);
        }
        // Etiqueta de propuesta automatica
        int py = ty + 16 + 3 * 19 + 13;
        g.setFont(Tema.ui(Font.BOLD, 9));
        String et = "SUGERIDA";
        int ew = g.getFontMetrics().stringWidth(et) + 10;
        g.setColor(mezcla(Tema.INFO, Tema.SUPERFICIE, 0.25));
        g.fillRoundRect(rx, py - 10, ew, 13, 6, 6);
        g.setColor(Tema.INFO);
        g.drawString(et, rx + 5, py);
        g.setFont(Tema.ui(Font.PLAIN, 10));
        g.setColor(Tema.TEXTO_3);
        g.drawString("Propuesta automática según el suelo: CL · IP = 10,5 · LP = 22,2 · finos 90,5 %", rx + ew + 6, py);

        // ---- resultados de M1
        int ry2 = py + 8;
        if (s >= T_RES) {
            String[] mat = {"Relave", "Caolín", "Feldespato K", "Agua"};
            String[] uni = {"g", "g", "g", "ml"};
            String[] rol = {"Materia prima (relave)", "Plasticidad / cohesión en verde",
                            "Fundente", "W/S = " + String.format(ES, "%.2f", dos[0][3] / 100)};
            g.setColor(Tema.SUPERFICIE_2);
            g.fillRect(rx, ry2, rw, 15);
            g.setFont(Tema.ui(Font.BOLD, 10));
            g.setColor(Tema.TEXTO_2);
            g.drawString("Material (M1)", rx + 4, ry2 + 11);
            g.drawString("Cantidad", rx + 112, ry2 + 11);
            g.drawString("% / Rol", rx + 196, ry2 + 11);
            for (int k = 0; k < 4; k++) {
                int yy = ry2 + 15 + k * 15;
                double v = gramos[k] * kRes;
                g.setFont(Tema.ui(Font.BOLD, 10));
                g.setColor(C_MAT[k].darker().equals(C_MAT[k]) ? C_MAT[k] : mezcla(C_MAT[k], Tema.TEXTO, 0.6));
                g.drawString(mat[k], rx + 4, yy + 11);
                g.setColor(Tema.TEXTO);
                g.drawString(String.format(ES, "%.1f %s", v, uni[k]), rx + 112, yy + 11);
                g.setFont(Tema.ui(Font.PLAIN, 10));
                g.setColor(Tema.INFO);
                g.drawString(String.format(ES, "%.1f %% · %s", dos[0][k], rol[k]), rx + 196, yy + 11);
            }
            double tot = (gramos[0] + gramos[1] + gramos[2]) * kRes;
            g.setFont(Tema.ui(Font.BOLD, 10));
            g.setColor(Tema.OK);
            g.drawString(String.format(ES, "Total sólidos %.1f g", tot), rx + 4, ry2 + 15 + 4 * 15 + 11);
            g.setColor(Tema.AVISO);
            g.drawString(String.format(ES, "Total verde %.1f g", tot + gramos[3] * kRes), rx + 150, ry2 + 15 + 4 * 15 + 11);
        }

        // ---- globos de aviso
        double a1 = tramo(s, T_G1, T_G1 + 0.03) * (1 - tramo(s, T_G1_FIN - 0.02, T_G1_FIN));
        if (a1 > 0) {
            resaltar(g, fD, Tema.AVISO, s);
            globo(g, fD.x - 40, fD.y + 34, fD.x + fD.width / 2, fD.y + fD.height + 4,
                  "✎ La densidad se puede cambiar", "1,85 g/cm³ es el valor estimado; escribe la tuya", Tema.AVISO, a1);
        }
        double a2 = tramo(s, T_G2, T_G2 + 0.03) * (1 - tramo(s, T_G2_FIN - 0.02, T_G2_FIN));
        if (a2 > 0) {
            resaltar(g, tabla, Tema.INFO, s);
            int gy = tabla.y + tabla.height + 22;
            globo(g, rx + 120, gy, rx + 160, tabla.y + tabla.height + 4,
                  "La dosificación es SUGERIDA por SDIM", "puedes cambiar cualquier % · Σ sólidos debe dar 100 %",
                  Tema.INFO, a2);
        }

        // ---- cursor
        if (s < T_RES + 0.05) {
            double[][] par = {
                {0.06, rx + 40, ry + 60},
                {T_CLIC_D - 0.01, fD.x + 22, fD.y + 10}, {T_TIPO_D1 + 0.03, fD.x + 22, fD.y + 10},
                {T_CLIC_R - 0.01, celRel.x + 18, celRel.y + 8}, {T_TIPO_R1 + 0.02, celRel.x + 18, celRel.y + 8},
                {T_CLIC_C - 0.01, celCao.x + 18, celCao.y + 8}, {T_TIPO_C1 + 0.03, celCao.x + 18, celCao.y + 8},
                {T_CLIC_CALC, bCalc.x + bCalc.width * 0.6, bCalc.y + 12}};
            double[] p = trayecto(s, par);
            boolean pul = (s >= T_CLIC_D && s < T_CLIC_D + 0.02) || (s >= T_CLIC_R && s < T_CLIC_R + 0.02)
                       || (s >= T_CLIC_C && s < T_CLIC_C + 0.02) || clicCalc;
            cursor(g, p[0], p[1], pul);
        }

        // ---- leyenda
        String ley;
        if (s < T_G1_FIN)          ley = "1 · La densidad estimada (1,85 g/cm³) se puede cambiar por la medida en laboratorio";
        else if (s < T_G2_FIN)     ley = "2 · La dosificación es sugerida según el suelo; ajusta los % si quieres (Σ = 100 %)";
        else if (s < T_RES)        ley = "3 · Pulsa «Calcular 3 mezclas»";
        else                       ley = "4 · SDIM entrega los gramos de relave, caolín, feldespato y el agua de cada mezcla";
        leyenda(g, 8, h - 23, w - 16, ley);

        if (s < 0.025) {
            g.setColor(alfa(Tema.SUPERFICIE, (int) (255 * (1 - s / 0.025))));
            g.fillRect(0, 0, w, h);
        }
    }

    private static void balanza(Graphics2D g, int x, int y, int w, int h, String nom, Color c,
                                double gramos, boolean agua, double k) {
        g.setColor(Tema.FONDO);
        g.fillRect(x, y, w, h);
        g.setColor(Tema.BORDE);
        g.drawRect(x, y, w, h);
        g.setFont(Tema.ui(Font.BOLD, 9));
        g.setColor(Tema.TEXTO_2);
        g.drawString(nom, x + 4, y + 11);
        int py = y + h - 22, pw = w - 20, px = x + 10;
        // carga
        if (k > 0) {
            int mh = (int) Math.round((agua ? 12 : 20) * k);
            g.setColor(c);
            if (agua) g.fillRect(px + 6, py - mh, pw - 12, mh);
            else      g.fillArc(px + 4, py - mh, pw - 8, 2 * mh, 0, 180);
        }
        // plato y pie
        g.setColor(new Color(0x3A, 0x55, 0x80));
        g.fillRoundRect(px, py, pw, 4, 3, 3);
        g.drawLine(x + w / 2, py + 4, x + w / 2, py + 11);
        g.setFont(Tema.ui(Font.BOLD, 9));
        g.setColor(Tema.OK);
        String t = String.format(ES, "%.0f %s", gramos, agua ? "ml" : "g");
        g.drawString(t, x + (w - g.getFontMetrics().stringWidth(t)) / 2, y + h - 3);
    }
}
