package geoceramiclab.modelo3d;

import geoceramiclab.Tema;

import java.awt.*;
import java.awt.geom.Path2D;
import java.util.Locale;

import static geoceramiclab.modelo3d.AnimUtil.*;

/**
 * Paso 6 de la ayuda: animacion "tipo video" de Ensayos NTC 919 / ISO 13006.
 *   ① M1 → «Prensado» → probetas 1 → 3 → «Aplicar»
 *   ② Absorcion: se llenan las masas y «Calcular absorción»
 *   ③ Contraccion: se llenan las longitudes y «Calcular contracción»
 *   ④ Flexion: «Ensayo a flexión» abre la ventana del ensayo; se escribe la LUZ
 *      (ancho, espesor y largo ya vienen llenos), «Cargar video», el video de la
 *      prensa dibuja la curva carga–tiempo a la derecha y se pulsa «Continuar».
 */
final class SimulacionEnsayos {

    static final long DUR = 30000;
    private static final Locale ES = new Locale("es", "CO");

    // ---------------- datos de ejemplo (M1, prensado, 3 probetas)
    private static final String[][] ABS = {{"452,6", "405,1"}, {"448,9", "402,0"}, {"455,3", "407,8"}};
    private static final String[] ABS_R = {"11,73", "11,67", "11,65"};
    private static final String[][] CON = {{"200,0", "193,4"}, {"200,2", "193,7"}, {"199,8", "193,1"}};
    private static final String[] CON_R = {"3,30", "3,25", "3,35"};
    private static final double F_MAX_KGF = 41.0, T_ROT = 7.6, T_VID = 8.7;
    private static final double LUZ = 170, ANCHO = 83.3, ESP = 8.3, LARGO = 193.4;

    // ---------------- colores
    private static final Color C_AZ = new Color(0x3E, 0x7C, 0xC4);
    private static final Color C_TEAL = new Color(0x2F, 0xA8, 0xB0);
    private static final Color C_GRIS = new Color(0x8F, 0x9A, 0xC0);
    private static final Color C_MOR = new Color(0xA8, 0x8C, 0xE0);
    private static final Color C_ORO = new Color(0xE0, 0xB4, 0x50);

    // ---------------- tiempos
    private static final double T_M1 = 0.04, T_PRENS = 0.07, T_UP1 = 0.10, T_UP2 = 0.12, T_APL = 0.14;
    private static final double T_ABS0 = 0.16, T_ABS1 = 0.25, T_CABS = 0.27;
    private static final double T_SCR0 = 0.30, T_SCR1 = 0.33;
    private static final double T_CON0 = 0.35, T_CON1 = 0.43, T_CCON = 0.45;
    private static final double T_COMBO = 0.48, T_BFLEX = 0.51, T_VEN0 = 0.52, T_VEN1 = 0.54;
    private static final double T_LUZ_CLIC = 0.56, T_LUZ0 = 0.57, T_LUZ1 = 0.60;
    private static final double T_CARGAR = 0.63, T_VID0 = 0.65, T_VID1 = 0.85, T_CONT = 0.89, T_CIERRA = 0.91;

    // ---------------- secciones (coordenadas virtuales, antes del scroll)
    private static final int SEC_CONF = 0, SEC_ABS = 118, SEC_CON = 240, SEC_FLEX = 346;
    private static final int H_CONF = 110, H_ABS = 114, H_CON = 98, H_FLEX = 104;
    private static final int SCROLL = 236;

    // rectangulos calculados en cada cuadro (para mover el cursor)
    private Rectangle rM1, rPrens, rUp, rApl, rCAbs, rCCon, rCombo, rBFlex, rLuz, rCargar, rCont;
    private final Rectangle[][] cAbs = new Rectangle[3][2], cCon = new Rectangle[3][2];

    void pintar(Graphics2D g, long ms, int w, int h) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        double s = (ms % DUR) / (double) DUR;

        // ---- vista principal
        g.setFont(Tema.ui(Font.BOLD, 11));
        g.setColor(Tema.TEXTO);
        g.drawString("Ensayos NTC 919 / ISO 13006", 8, 13);
        g.setFont(Tema.ui(Font.PLAIN, 9));
        g.setColor(Tema.TEXTO_3);
        String sub = "Siga el mapa de arriba hacia abajo · cada paso se habilita al calcular el anterior";
        g.drawString(sub, w - 8 - g.getFontMetrics().stringWidth(sub), 13);

        int vy = 20, vh = h - vy - 27;
        int off = (int) Math.round(SCROLL * tramo(s, T_SCR0, T_SCR1));
        Shape clip = g.getClip();
        g.clipRect(0, vy, w, vh);
        int oy = vy + 2 - off;
        mapa(g, oy, s);
        int sx = 124, sw = w - sx - 8;
        conformado(g, sx, oy + SEC_CONF, sw, s);
        absorcion(g, sx, oy + SEC_ABS, sw, s);
        contraccion(g, sx, oy + SEC_CON, sw, s);
        flexion(g, sx, oy + SEC_FLEX, sw, s);
        g.setClip(clip);

        // ---- ventana del ensayo a flexion
        double aVen = tramo(s, T_VEN0, T_VEN1) * (1 - tramo(s, T_CIERRA, T_CIERRA + 0.015));
        if (aVen > 0) {
            g.setColor(alfa(Color.BLACK, (int) (120 * aVen)));
            g.fillRect(0, 0, w, h);
            Graphics2D gv = (Graphics2D) g.create();
            double esc = 0.92 + 0.08 * aVen;
            gv.translate(w / 2.0, h / 2.0);
            gv.scale(esc, esc);
            gv.translate(-w / 2.0, -h / 2.0);
            gv.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) aVen));
            ventana(gv, new Rectangle(10, 3, w - 20, h - 30), s);
            gv.dispose();
        }

        // ---- cursor
        double[] p = posCursor(s);
        if (p != null) cursor(g, p[0], p[1], pulsando(s));

        // ---- leyenda
        leyenda(g, 8, h - 23, w - 16, leyendaDe(s));

        if (s < 0.015) {
            g.setColor(alfa(Tema.SUPERFICIE, (int) (255 * (1 - s / 0.015))));
            g.fillRect(0, 0, w, h);
        }
    }

    private static String leyendaDe(double s) {
        if (s < T_APL + 0.01)  return "① Elige la mezcla (M1), el conformado (prensado o extruido) y cuántas probetas tiene";
        if (s < T_SCR0)        return "② Absorción: escribe la masa saturada y la masa seca de cada probeta → «Calcular absorción»";
        if (s < T_CCON + 0.02) return "③ Contracción: longitud en verde y cocida de cada probeta → «Calcular contracción»";
        if (s < T_VEN0)        return "④ Flexión: elige la probeta y pulsa «Ensayo a flexión» (se abre otra ventana)";
        if (s < T_CARGAR)      return "Ancho, espesor y largo ya vienen llenos: solo escribe la LUZ (distancia entre apoyos)";
        if (s < T_VID1)        return "«Cargar video»: el video de la prensa dibuja la curva carga–tiempo a la derecha";
        if (s < T_CIERRA)      return "Cuando la probeta se rompe, pulsa «Continuar →»";
        return "Resultado de flexión: carga máxima, S de rotura, módulo R y comparación con la NTC 919";
    }

    private static boolean pulsando(double s) {
        double[] cl = {T_M1, T_PRENS, T_UP1, T_UP2, T_APL, T_CABS, T_CCON, T_COMBO, T_BFLEX, T_LUZ_CLIC, T_CARGAR, T_CONT};
        for (double c : cl) if (s >= c && s < c + 0.012) return true;
        return false;
    }

    // ================================================================ mapa izquierdo
    private static void mapa(Graphics2D g, int oy, double s) {
        int x = 8, nw = 104, nh = 28, lx = x + nw / 2;
        int[] ny = {oy + 2, oy + SEC_CONF + 66, oy + SEC_ABS + 40, oy + SEC_CON + 36, oy + SEC_FLEX + 38};
        int[] sy = {-1, oy + SEC_CONF + 80, oy + SEC_ABS + 54, oy + SEC_CON + 50, oy + SEC_FLEX + 52};
        Color[] c = {C_AZ, C_AZ, C_TEAL, C_GRIS, C_MOR};
        String[][] t = {{"Laboratorio", "NTC 919 · ISO 13006"}, {"Mezclas", "y probetas"},
                        {"Absorción", "de agua"}, {"Contracción", "lineal total"}, {"Flexión", "3 puntos"}};
        int act = s < T_APL + 0.01 ? 1 : s < T_SCR0 ? 2 : s < T_CCON + 0.02 ? 3 : 4;
        for (int i = 1; i < 5; i++) {
            g.setColor(c[i]);
            g.setStroke(new BasicStroke(1.5f));
            g.drawLine(lx, ny[i - 1] + nh, lx, ny[i]);
            g.drawLine(x + nw, sy[i], 122, sy[i]);
            g.fillPolygon(new int[]{122, 116, 116}, new int[]{sy[i], sy[i] - 3, sy[i] + 3}, 3);
        }
        g.setStroke(new BasicStroke(1f));
        for (int i = 0; i < 5; i++) {
            boolean a = i == act;
            g.setColor(i == 0 ? C_AZ : mezcla(c[i], Tema.SUPERFICIE, a ? 0.45 : 0.22));
            g.fillRoundRect(x, ny[i], nw, nh, 8, 8);
            g.setColor(a || i == 0 ? mezcla(c[i], Color.WHITE, 0.7) : c[i]);
            g.setStroke(new BasicStroke(a ? 2f : 1f));
            g.drawRoundRect(x, ny[i], nw, nh, 8, 8);
            g.setStroke(new BasicStroke(1f));
            if (i > 0) {
                g.setColor(c[i]);
                g.fillOval(x + 5, ny[i] + 7, 14, 14);
                g.setColor(Color.WHITE);
                g.setFont(Tema.ui(Font.BOLD, 9));
                g.drawString(String.valueOf(i), x + 10, ny[i] + 18);
            }
            g.setFont(Tema.ui(Font.BOLD, i == 0 ? 8 : 9));
            g.setColor(Color.WHITE);
            FontMetrics fm = g.getFontMetrics();
            int cx = x + (i > 0 ? 58 : nw / 2);
            g.drawString(t[i][0], cx - fm.stringWidth(t[i][0]) / 2, ny[i] + 12);
            g.setFont(Tema.ui(Font.PLAIN, 8));
            fm = g.getFontMetrics();
            g.setColor(mezcla(Color.WHITE, c[i], 0.8));
            g.drawString(t[i][1], cx - fm.stringWidth(t[i][1]) / 2, ny[i] + 23);
        }
    }

    // ================================================================ secciones
    private static void caja(Graphics2D g, int x, int y, int w, int h, Color c, String tit, String sub) {
        g.setColor(Tema.SUPERFICIE);
        g.fillRect(x, y, w, h);
        g.setColor(c);
        g.fillRect(x, y, 3, h);
        g.setFont(Tema.ui(Font.BOLD, 11));
        g.drawString(tit, x + 10, y + 14);
        if (sub != null) {
            g.setFont(Tema.ui(Font.PLAIN, 8));
            g.setColor(Tema.TEXTO_3);
            g.drawString(sub, x + 10, y + 25);
        }
    }

    private void conformado(Graphics2D g, int x, int y, int w, double s) {
        caja(g, x, y, w, H_CONF, C_AZ, "① Conformado, mezcla y probetas", "Los datos se guardan solos");
        g.setFont(Tema.ui(Font.BOLD, 9));
        g.setColor(Tema.TEXTO);
        g.drawString("Conformado de las piezas", x + 10, y + 38);
        boolean prens = s >= T_PRENS;
        rPrens = toggle(g, x + 10, y + 42, "Prensado", prens);
        toggle(g, rPrens.x + rPrens.width + 4, y + 42, "Extruido", false);

        String[][] m = {{"M1", "Relave alto"}, {"M2", "Intermedia"}, {"M3", "Fundente alto"}};
        Color[] cm = {new Color(0x7F, 0xA3, 0xC2), new Color(0x4C, 0xB7, 0x82), C_ORO};
        int cw = (w - 28) / 3;
        for (int i = 0; i < 3; i++) {
            int cx = x + 10 + i * (cw + 4), cy = y + 64;
            boolean sel = i == 0 && s >= T_M1;
            g.setColor(sel ? Tema.SUPERFICIE_2 : Tema.FONDO);
            g.fillRoundRect(cx, cy, cw, 26, 6, 6);
            g.setColor(sel ? Tema.ACENTO_CLARO : Tema.BORDE);
            g.setStroke(new BasicStroke(sel ? 1.6f : 1f));
            g.drawRoundRect(cx, cy, cw, 26, 6, 6);
            g.setStroke(new BasicStroke(1f));
            g.setColor(cm[i]);
            g.fillRect(cx + 5, cy + 5, 2, 16);
            g.setFont(Tema.ui(Font.BOLD, 9));
            g.setColor(Tema.TEXTO);
            g.drawString(m[i][0], cx + 12, cy + 12);
            g.setFont(Tema.ui(Font.PLAIN, 9));
            g.drawString(m[i][1], cx + 33, cy + 12);
            g.setColor(Tema.TEXTO_2);
            g.drawString(sel ? "En proceso" : "Sin datos", cx + 12, cy + 22);
            g.setColor(Tema.TEXTO_3);
            g.drawOval(cx + cw - 20, cy + 6, 13, 13);
            if (i == 0) rM1 = new Rectangle(cx, cy, cw, 26);
        }
        int py = y + 94;
        g.setFont(Tema.ui(Font.BOLD, 9));
        g.setColor(Tema.TEXTO);
        g.drawString("Probetas de esta mezcla:", x + 10, py + 11);
        int n = s >= T_UP2 ? 3 : s >= T_UP1 ? 2 : 1;
        int fx = x + 146;
        g.setColor(Tema.FONDO);
        g.fillRect(fx, py, 34, 15);
        g.setColor(s >= T_UP1 - 0.01 && s < T_APL ? Tema.ACENTO : Tema.BORDE);
        g.drawRect(fx, py, 34, 15);
        g.setFont(Tema.ui(Font.BOLD, 10));
        g.setColor(Tema.TEXTO);
        g.drawString(String.valueOf(n), fx + 14, py + 12);
        g.setFont(Tema.ui(Font.PLAIN, 7));
        g.setColor(Tema.TEXTO_2);
        g.drawString("▲", fx + 26, py + 7);
        g.drawString("▼", fx + 26, py + 14);
        rUp = new Rectangle(fx + 24, py, 10, 7);
        rApl = boton(g, fx + 42, py - 2, "Aplicar", Tema.ACENTO, s >= T_APL && s < T_APL + 0.012);
        g.setFont(Tema.ui(Font.PLAIN, 9));
        g.setColor(Tema.TEXTO_3);
        g.drawString("M1 — Relave alto", rApl.x + rApl.width + 10, py + 11);
    }

    private static Rectangle toggle(Graphics2D g, int x, int y, String t, boolean on) {
        g.setFont(Tema.ui(Font.PLAIN, 9));
        int tw = g.getFontMetrics().stringWidth(t) + 24;
        g.setColor(on ? C_ORO : Tema.FONDO);
        g.fillRoundRect(x, y, tw, 16, 16, 16);
        g.setColor(on ? C_ORO.darker() : Tema.BORDE);
        g.drawRoundRect(x, y, tw, 16, 16, 16);
        g.setColor(on ? new Color(30, 24, 10) : Tema.TEXTO_2);
        g.drawString(t, x + 12, y + 12);
        return new Rectangle(x, y, tw, 16);
    }

    /** Tabla con celdas que se van escribiendo; devuelve la altura usada. */
    private static int tablaDatos(Graphics2D g, int x, int y, String[] cab, int[] anc, String[][] val, String[] res,
                                  double s, double t0, double t1, boolean filas, boolean conRes, Rectangle[][] celdas) {
        int hh = 14, rh = 13;
        int tw = 0;
        for (int a : anc) tw += a;
        g.setColor(Tema.BARRA);
        g.fillRect(x, y, tw, hh);
        g.setFont(Tema.ui(Font.PLAIN, 8));
        g.setColor(Tema.TEXTO_2);
        int cx = x;
        for (int c = 0; c < cab.length; c++) {
            FontMetrics fm = g.getFontMetrics();
            g.drawString(cab[c], cx + (anc[c] - fm.stringWidth(cab[c])) / 2, y + 10);
            cx += anc[c];
        }
        if (!filas) return hh;
        int total = 0;
        for (String[] r : val) for (String v : r) total += v.length();
        int esc = s <= t0 ? 0 : s >= t1 ? total : (int) ((s - t0) / (t1 - t0) * total);
        int usados = 0;
        for (int r = 0; r < val.length; r++) {
            int ry = y + hh + r * rh;
            cx = x;
            g.setColor(Tema.SUPERFICIE_2);
            g.fillRect(cx, ry, anc[0], rh);
            g.setFont(Tema.ui(Font.BOLD, 9));
            g.setColor(Tema.TEXTO);
            g.drawString("P" + (r + 1), cx + anc[0] / 2 - 6, ry + 10);
            cx += anc[0];
            for (int c = 0; c < val[r].length; c++) {
                String v = val[r][c];
                int n = Math.max(0, Math.min(v.length(), esc - usados));
                boolean activa = n < v.length() && esc >= usados && s > t0;
                usados += v.length();
                g.setColor(activa ? mezcla(Tema.ACENTO, Tema.FONDO, 0.45) : mezcla(Tema.ACENTO, Tema.FONDO, 0.18));
                g.fillRect(cx, ry, anc[c + 1] - 1, rh - 1);
                g.setFont(Tema.ui(Font.PLAIN, 9));
                g.setColor(Tema.TEXTO);
                String vv = v.substring(0, n);
                FontMetrics fm = g.getFontMetrics();
                g.drawString(vv, cx + (anc[c + 1] - fm.stringWidth(v)) / 2, ry + 10);
                if (activa && ((long) (s * DUR / 280)) % 2 == 0) {
                    g.setColor(Tema.ACENTO_CLARO);
                    g.fillRect(cx + (anc[c + 1] - fm.stringWidth(v)) / 2 + fm.stringWidth(vv) + 1, ry + 2, 1, 9);
                }
                if (celdas != null) celdas[r][c] = new Rectangle(cx, ry, anc[c + 1], rh);
                cx += anc[c + 1];
            }
            g.setColor(mezcla(Tema.OK, Tema.FONDO, 0.12));
            g.fillRect(cx, ry, anc[anc.length - 1], rh - 1);
            if (conRes) {
                g.setFont(Tema.ui(Font.BOLD, 9));
                g.setColor(Tema.OK);
                FontMetrics fm = g.getFontMetrics();
                g.drawString(res[r], cx + (anc[anc.length - 1] - fm.stringWidth(res[r])) / 2, ry + 10);
            }
        }
        return hh + val.length * rh;
    }

    private void absorcion(Graphics2D g, int x, int y, int w, double s) {
        caja(g, x, y, w, H_ABS, C_TEAL, "② Absorción de agua (Ev)",
             "NTC 919 · ISO 10545-3 · Ev = (m saturada − m seca) / m seca × 100");
        imgAgua(g, x + 10, y + 30, 92, 62, s);
        int tx = x + 110;
        boolean filas = s >= T_APL;
        int th = tablaDatos(g, tx, y + 30, new String[]{"Probeta", "M. sat. (g)", "M. seca (g)", "Ev (%)"},
                new int[]{44, 100, 100, 84}, ABS, ABS_R, s, T_ABS0, T_ABS1, filas, s >= T_CABS + 0.01, cAbs);
        rCAbs = boton(g, tx, y + 30 + th + 5, "Calcular absorción", Tema.ACENTO, s >= T_CABS && s < T_CABS + 0.012);
        if (s >= T_CABS + 0.01) {
            int ax = rCAbs.x + rCAbs.width + 10;
            par(g, ax, rCAbs.y + 8, "Absorción promedio: ", "11,68 %", Tema.OK);
            par(g, ax, rCAbs.y + 20, "Grupo NTC 919 (prensadas): ", "BIII · Ev > 10 %", C_ORO);
        } else if (!filas) {
            g.setFont(Tema.ui(Font.PLAIN, 9));
            g.setColor(Tema.TEXTO_3);
            g.drawString("Las filas aparecen al aplicar el número de probetas", rCAbs.x + rCAbs.width + 10, rCAbs.y + 14);
        }
    }

    private void contraccion(Graphics2D g, int x, int y, int w, double s) {
        caja(g, x, y, w, H_CON, C_GRIS, "③ Contracción lineal total",
             "Ct = (L en verde − L cocida) / L en verde × 100 · misma marca de referencia en cada probeta");
        imgContraccion(g, x + 10, y + 30, 92, 58, s);
        int tx = x + 110;
        int th = tablaDatos(g, tx, y + 30, new String[]{"Probeta", "L. verde (mm)", "L. cocida (mm)", "Contracción (%)"},
                new int[]{44, 100, 100, 84}, CON, CON_R, s, T_CON0, T_CON1, true, s >= T_CCON + 0.01, cCon);
        rCCon = boton(g, tx, y + 30 + th + 5, "Calcular contracción", Tema.ACENTO, s >= T_CCON && s < T_CCON + 0.012);
        if (s >= T_CCON + 0.01) {
            par(g, rCCon.x + rCCon.width + 10, rCCon.y + 14, "Contracción promedio: ", "3,30 %", Tema.OK);
        }
    }

    private static void par(Graphics2D g, int x, int y, String et, String v, Color c) {
        g.setFont(Tema.ui(Font.PLAIN, 9));
        g.setColor(Tema.TEXTO_2);
        g.drawString(et, x, y);
        int dx = g.getFontMetrics().stringWidth(et);
        g.setFont(Tema.ui(Font.BOLD, 9));
        g.setColor(c);
        g.drawString(v, x + dx, y);
    }

    private void flexion(Graphics2D g, int x, int y, int w, double s) {
        caja(g, x, y, w, H_FLEX, C_MOR, "④ Resistencia a flexión (3 puntos)", "NTC 919 · ISO 10545-4");
        imgFlexion(g, x + 10, y + 30, 92, 64);
        int tx = x + 110;
        g.setFont(Tema.ui(Font.BOLD, 9));
        g.setColor(Tema.TEXTO);
        g.drawString("Probeta:", tx, y + 42);
        rCombo = new Rectangle(tx + 46, y + 31, 56, 16);
        g.setColor(Tema.FONDO);
        g.fillRect(rCombo.x, rCombo.y, rCombo.width, rCombo.height);
        g.setColor(s >= T_COMBO && s < T_BFLEX ? Tema.ACENTO : Tema.BORDE);
        g.drawRect(rCombo.x, rCombo.y, rCombo.width, rCombo.height);
        g.setFont(Tema.ui(Font.PLAIN, 9));
        g.setColor(Tema.TEXTO);
        g.drawString("P1", rCombo.x + 6, rCombo.y + 12);
        g.drawString("▾", rCombo.x + rCombo.width - 10, rCombo.y + 12);
        rBFlex = boton(g, rCombo.x + rCombo.width + 8, y + 29, "Ensayo a flexión", C_MOR, s >= T_BFLEX && s < T_BFLEX + 0.012);

        String[] cab = {"Prob.", "L cocida", "Luz", "Ancho", "Espesor", "Carga máx", "S rotura", "R (MPa)", "NTC 919"};
        int[] anc = {30, 48, 32, 40, 48, 60, 54, 50, 64};
        int ty = y + 54;
        tablaDatos(g, tx, ty, cab, anc, new String[0][], null, s, 0, 1, false, false, null);
        int ry = ty + 14, cx = tx;
        boolean listo = s >= T_CIERRA;
        double fN = F_MAX_KGF * 9.80665;
        double sN = fN * LUZ / ANCHO, rM = 3 * fN * LUZ / (2 * ANCHO * ESP * ESP);
        String[] v = {"P1", "193,4", listo ? "170" : "", "83,3", "8,3",
                      listo ? String.format(ES, "%.0f N", fN) : "", listo ? String.format(ES, "%.0f N", sN) : "",
                      listo ? String.format(ES, "%.2f", rM) : "", listo ? "Cumple" : ""};
        for (int c = 0; c < v.length; c++) {
            g.setColor(c == 0 ? Tema.SUPERFICIE_2 : (c >= 5 ? mezcla(Tema.OK, Tema.FONDO, 0.12) : Tema.FONDO));
            g.fillRect(cx, ry, anc[c] - 1, 14);
            g.setFont(Tema.ui(c == 0 || c == 8 ? Font.BOLD : Font.PLAIN, 9));
            g.setColor(c == 8 ? Tema.OK : (c >= 5 ? Tema.OK : Tema.TEXTO));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(v[c], cx + (anc[c] - fm.stringWidth(v[c])) / 2, ry + 10);
            cx += anc[c];
        }
        if (listo) {
            g.setFont(Tema.ui(Font.PLAIN, 8));
            g.setColor(Tema.TEXTO_2);
            g.drawString("NTC 919 BIII: CUMPLE  (S ≥ 600 N; R ≥ 15 MPa)", tx, ry + 26);
        }
    }

    // ---------------- ilustraciones pequenas
    private static void imgAgua(Graphics2D g, int x, int y, int w, int h, double s) {
        g.setColor(Tema.FONDO);
        g.fillRect(x, y, w, h);
        g.setColor(Tema.BORDE);
        g.drawRect(x, y, w, h);
        int wy = y + 12;
        g.setColor(new Color(0x1E, 0x4F, 0x9A));
        g.fillRect(x + 4, wy, w - 8, h - 16);
        g.setColor(new Color(0x6C, 0xA8, 0xF0));
        for (int i = x + 4; i < x + w - 4; i += 2)
            g.drawLine(i, wy + (int) (1.5 * Math.sin(i * 0.3 + s * 80)), i + 2, wy + (int) (1.5 * Math.sin((i + 2) * 0.3 + s * 80)));
        g.setColor(new Color(0x8A, 0x6E, 0x60));
        g.fillRoundRect(x + w / 2 - 22, y + h / 2 - 4, 44, 18, 4, 4);
        g.setColor(new Color(0xA8, 0xD0, 0xFF));
        for (int k = 0; k < 6; k++) {
            double ph = (s * 40 + k * 0.37) % 1.0;
            int bx = x + 12 + (k * 13) % (w - 24);
            int by = (int) (y + h - 6 - ph * (h - 20));
            g.drawOval(bx, by, 3, 3);
        }
    }

    private static void imgContraccion(Graphics2D g, int x, int y, int w, int h, double s) {
        g.setColor(new Color(0x5A, 0x24, 0x18));
        g.fillRect(x, y, w, h);
        g.setColor(new Color(0xC0, 0x60, 0x30));
        g.drawRect(x, y, w, h);
        double k = 0.5 + 0.5 * Math.sin(s * 70);
        int pw = (int) (44 - 6 * k);
        g.setColor(new Color(0xB8, 0x8A, 0x6A));
        g.fillRoundRect(x + (w - pw) / 2, y + h / 2 - 9, pw, 18, 3, 3);
        g.setColor(new Color(0x9A, 0xB0, 0xD0));
        g.drawLine(x + 8, y + h / 2, x + (w - pw) / 2 - 3, y + h / 2);
        g.drawLine(x + w - 8, y + h / 2, x + (w + pw) / 2 + 3, y + h / 2);
    }

    private static void imgFlexion(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(Tema.FONDO);
        g.fillRect(x, y, w, h);
        g.setColor(Tema.BORDE);
        g.drawRect(x, y, w, h);
        Path2D.Double p = new Path2D.Double();
        p.moveTo(x + 10, y + 44); p.lineTo(x + 66, y + 44); p.lineTo(x + 84, y + 30); p.lineTo(x + 28, y + 30); p.closePath();
        g.setColor(new Color(0xC0, 0x70, 0x40));
        g.fill(p);
        g.setColor(new Color(0x8A, 0x48, 0x22));
        g.fillRect(x + 10, y + 44, 56, 4);
        g.setColor(new Color(0xD8, 0xDC, 0xE6));
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(x + 38, y + 38, x + 56, y + 32);
        g.setStroke(new BasicStroke(1f));
        g.setColor(Tema.TEXTO_3);
        g.drawLine(x + 47, y + 8, x + 47, y + 30);
    }

    // ================================================================ ventana de flexion
    private void ventana(Graphics2D g, Rectangle r, double s) {
        // marco y barra de titulo
        g.setColor(new Color(0x0E, 0x13, 0x19));
        g.fillRect(r.x, r.y, r.width, 16);
        g.setFont(Tema.ui(Font.PLAIN, 9));
        g.setColor(new Color(0xDD, 0xE3, 0xEA));
        g.drawString("Ensayo de flexión en 3 puntos · probeta P1", r.x + 8, r.y + 11);
        g.drawString("✕", r.x + r.width - 14, r.y + 11);
        int lw = (int) (r.width * 0.58);
        Rectangle izq = new Rectangle(r.x, r.y + 16, lw, r.height - 16);
        Rectangle der = new Rectangle(r.x + lw, r.y + 16, r.width - lw, r.height - 16);
        Color blanco = new Color(0xF4, 0xF5, 0xF7), tinta = new Color(0x1C, 0x22, 0x2A), gris = new Color(0x6A, 0x74, 0x80);
        g.setColor(blanco);
        g.fillRect(izq.x, izq.y, izq.width, izq.height);
        g.setColor(Color.WHITE);
        g.fillRect(der.x, der.y, der.width, der.height);
        g.setColor(new Color(0xD0, 0xD6, 0xDE));
        g.drawLine(der.x, der.y, der.x, der.y + der.height);

        // tiempo del video
        double kv = tramo(s, T_VID0, T_VID1);
        boolean video = s >= T_VID0;
        double tv = video ? T_VID * clamp01((s - T_VID0) / (T_VID1 - T_VID0)) : 0;
        double F = carga(tv);
        boolean roto = tv > T_ROT + 0.05;

        // cabecera izquierda
        g.setFont(Tema.ui(Font.BOLD, 9));
        g.setColor(tinta);
        g.drawString("ESFUERZOS (MEF)", izq.x + 8, izq.y + 13);
        g.setFont(Tema.ui(Font.PLAIN, 8));
        g.setColor(gris);
        g.drawString("elementos finitos", izq.x + 8, izq.y + 23);
        g.setFont(Tema.ui(Font.BOLD, 11));
        FontMetrics fb = g.getFontMetrics();
        int anchoBotones = fb.stringWidth("Cargar video") + fb.stringWidth("Simular") + fb.stringWidth("Continuar →") + 3 * 22 + 8;
        int bx = izq.x + izq.width - 6 - anchoBotones, by = izq.y + 5;
        rCargar = boton(g, bx, by, "Cargar video", C_AZ, s >= T_CARGAR && s < T_CARGAR + 0.012);
        Rectangle rSim = boton(g, rCargar.x + rCargar.width + 4, by, "Simular", new Color(0x6A, 0x70, 0x78), false);
        boolean contOk = s >= T_VID1;
        rCont = boton(g, rSim.x + rSim.width + 4, by, "Continuar →",
                contOk ? Tema.OK : new Color(0xB8, 0xC8, 0xDC), s >= T_CONT && s < T_CONT + 0.012);
        if (contOk && s < T_CONT) resaltar(g, rCont, Tema.OK, s);

        // barra de colores
        int cbx = izq.x + 10, cby = izq.y + 44, cbh = 92;
        for (int i = 0; i < cbh; i++) {
            g.setColor(colormap(1 - 2.0 * i / cbh));
            g.drawLine(cbx, cby + i, cbx + 8, cby + i);
        }
        g.setFont(Tema.ui(Font.PLAIN, 7));
        g.setColor(tinta);
        double smax = roto ? 0 : 17.9 * F / F_MAX_KGF;
        g.drawString(String.format(ES, "+%.1f", smax), cbx + 11, cby + 6);
        g.drawString("0,0", cbx + 11, cby + cbh / 2 + 3);
        g.drawString(String.format(ES, "−%.1f", smax), cbx + 11, cby + cbh);
        g.drawString("σ [MPa]", cbx - 2, cby - 9);

        // probeta con malla
        losa(g, izq.x + 46, izq.y + 30, izq.width - 60, 112, roto ? 0 : F / F_MAX_KGF, roto, tinta, s);

        // video
        Rectangle vid = new Rectangle(izq.x, izq.y + izq.height - 62, izq.width, 62);
        g.setColor(new Color(0x1A, 0x20, 0x28));
        g.fillRect(vid.x, vid.y, vid.width, vid.height);
        if (!video) {
            g.setFont(Tema.ui(Font.BOLD, 10));
            g.setColor(Color.WHITE);
            String t = "VIDEO DEL ENSAYO REAL";
            g.drawString(t, vid.x + (vid.width - g.getFontMetrics().stringWidth(t)) / 2, vid.y + 18);
            g.setFont(Tema.ui(Font.PLAIN, 8));
            g.setColor(new Color(0xB0, 0xBA, 0xC6));
            t = "Cargue el video de la pantalla de la prensa: la curva se dibujará con él";
            g.drawString(t, vid.x + (vid.width - g.getFontMetrics().stringWidth(t)) / 2, vid.y + 32);
            boton(g, vid.x + vid.width / 2 - 38, vid.y + 38, "Cargar video", C_AZ, false);
        } else {
            pantallaPrensa(g, vid, tv, F, roto);
        }

        // ---- grafica carga - tiempo
        curva(g, der, tv, video, tinta, gris);

        // ---- campos de medidas (encima del dibujo)
        Color azul = new Color(0x1E, 0x5B, 0xC6);
        cajaMedida(g, izq.x + 46, izq.y + 50, "Ancho", "83,3", azul, false, false);
        cajaMedida(g, izq.x + izq.width - 96, izq.y + 100, "Espesor", "8,3", azul, false, false);
        boolean luzAct = s >= T_LUZ_CLIC && s < T_LUZ1 + 0.02;
        String luz = escribiendo("170", s, T_LUZ0, T_LUZ1);
        rLuz = cajaMedida(g, izq.x + 150, izq.y + 124, "Luz", luz, azul, luzAct,
                luzAct && s >= T_LUZ0 && ((long) (s * DUR / 280)) % 2 == 0);
        cajaMedida(g, izq.x + 50, izq.y + 124, "Largo", "193,4", azul, false, false);
        if (s >= T_VEN1 && s < T_LUZ_CLIC) {
            resaltar(g, rLuz, Tema.AVISO, s);
        }
        if (s >= T_VEN1 + 0.005 && s < T_LUZ1 + 0.01) {
            double a = tramo(s, T_VEN1, T_VEN1 + 0.01) * (1 - tramo(s, T_LUZ1, T_LUZ1 + 0.01));
            globo(g, izq.x + 140, izq.y + 56, rLuz.x + rLuz.width / 2, rLuz.y - 2,
                  "Escribe la LUZ", "distancia entre los ejes de los apoyos", Tema.AVISO, a);
        }
    }

    private static Rectangle cajaMedida(Graphics2D g, int x, int y, String et, String v, Color azul,
                                        boolean act, boolean caret) {
        int w = 88, h = 15;
        g.setColor(act ? new Color(0xFF, 0xF4, 0xD6) : Color.WHITE);
        g.fillRect(x, y, w, h);
        g.setColor(act ? Tema.AVISO : azul);
        g.drawRect(x, y, w, h);
        g.setFont(Tema.ui(Font.BOLD, 8));
        g.setColor(azul);
        g.drawString(et, x + 3, y + 11);
        g.setFont(Tema.ui(Font.BOLD, 9));
        g.setColor(new Color(0x1C, 0x22, 0x2A));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(v, x + w - 20 - fm.stringWidth(v), y + 11);
        if (caret) { g.fillRect(x + w - 19, y + 3, 1, 9); }
        g.setFont(Tema.ui(Font.PLAIN, 7));
        g.setColor(new Color(0x8A, 0x4F, 0xA0));
        g.drawString("mm", x + w - 15, y + 11);
        return new Rectangle(x, y, w, h);
    }

    /** Carga (kgf) en el instante tv del video. */
    private static double carga(double tv) {
        if (tv <= 0) return 0;
        if (tv <= T_ROT) {
            double k = tv / T_ROT;
            return F_MAX_KGF * (k + 0.012 * Math.sin(tv * 7) * k) - 0.6 * Math.sin(tv * 2.3) * k * (1 - k);
        }
        double k = clamp01((tv - T_ROT) / 0.45);
        return F_MAX_KGF * (1 - k) * (1 - k);
    }

    private static double cargaLote(double tv, double pico, double tPico) {
        if (tv <= tPico) return pico * tv / tPico + 0.5 * Math.sin(tv * 6.5) * tv / tPico;
        double k = clamp01((tv - tPico) / 0.9);
        return pico * (1 - k);
    }

    private static Color colormap(double v) {   // v en [-1, 1]: azul (traccion) → verde (0) → rojo (compresion)
        v = Math.max(-1, Math.min(1, v));
        float hue = (float) ((1 - (v + 1) / 2) * 0.66);
        return Color.getHSBColor(hue, 0.85f, 0.95f);
    }

    private static void losa(Graphics2D g, int x, int y, int w, int h, double carga, boolean roto, Color tinta, double s) {
        int nx = 22, ny = 7;
        double L = w * 0.78, Wx = w * 0.20, Wy = -h * 0.34, d = 9;
        double x0 = x + 6, y0 = y + h * 0.66;
        double flecha = 6 * carga;
        // apoyos
        g.setColor(new Color(0xB8, 0xBE, 0xC8));
        for (double u : new double[]{0.08, 0.92}) {
            int cx = (int) (x0 + u * L + 0.5 * Wx), cy = (int) (y0 + 0.5 * Wy + d + 7);
            g.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine((int) (x0 + u * L) + 2, (int) (y0 + d + 6), (int) (x0 + u * L + Wx) - 2, (int) (y0 + Wy + d + 6));
            g.setStroke(new BasicStroke(1f));
        }
        // cara frontal (espesor): compresion arriba, traccion abajo
        for (int i = 0; i < nx; i++) {
            double u0 = i / (double) nx, u1 = (i + 1) / (double) nx, um = (u0 + u1) / 2;
            double f = 1 - Math.abs(um - 0.5) * 2;
            double dy0 = flecha * Math.sin(Math.PI * u0), dy1 = flecha * Math.sin(Math.PI * u1);
            for (int k = 0; k < 2; k++) {
                Path2D.Double p = new Path2D.Double();
                double za = d * k / 2.0, zb = d * (k + 1) / 2.0;
                p.moveTo(x0 + u0 * L, y0 + za + dy0); p.lineTo(x0 + u1 * L, y0 + za + dy1);
                p.lineTo(x0 + u1 * L, y0 + zb + dy1); p.lineTo(x0 + u0 * L, y0 + zb + dy0); p.closePath();
                g.setColor(colormap((k == 0 ? 1 : -1) * carga * f).darker());
                g.fill(p);
                g.setColor(new Color(0, 0, 0, 60));
                g.draw(p);
            }
        }
        // cara superior con malla
        for (int i = 0; i < nx; i++) {
            for (int j = 0; j < ny; j++) {
                double u0 = i / (double) nx, u1 = (i + 1) / (double) nx;
                double v0 = j / (double) ny, v1 = (j + 1) / (double) ny;
                double um = (u0 + u1) / 2;
                double f = 1 - Math.abs(um - 0.5) * 2;
                double dy0 = flecha * Math.sin(Math.PI * u0), dy1 = flecha * Math.sin(Math.PI * u1);
                Path2D.Double p = new Path2D.Double();
                p.moveTo(x0 + u0 * L + v0 * Wx, y0 + v0 * Wy + dy0);
                p.lineTo(x0 + u1 * L + v0 * Wx, y0 + v0 * Wy + dy1);
                p.lineTo(x0 + u1 * L + v1 * Wx, y0 + v1 * Wy + dy1);
                p.lineTo(x0 + u0 * L + v1 * Wx, y0 + v1 * Wy + dy0);
                p.closePath();
                g.setColor(colormap(carga * f * f));
                g.fill(p);
                g.setColor(new Color(0, 0, 0, 70));
                g.draw(p);
            }
        }
        // grieta al romper
        if (roto) {
            g.setColor(new Color(0x20, 0x10, 0x08));
            g.setStroke(new BasicStroke(2f));
            double xm = x0 + 0.5 * L;
            Path2D.Double c = new Path2D.Double();
            c.moveTo(xm, y0 + d);
            c.lineTo(xm + 2, y0 + d / 2); c.lineTo(xm - 1, y0);
            c.lineTo(xm + 0.5 * Wx + 2, y0 + 0.5 * Wy); c.lineTo(xm + Wx, y0 + Wy);
            g.draw(c);
            g.setStroke(new BasicStroke(1f));
        }
        // rodillo de carga + flecha
        double xm = x0 + 0.5 * L;
        g.setColor(new Color(0xC8, 0xCD, 0xD6));
        g.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine((int) xm, (int) (y0 - 4 + flecha), (int) (xm + Wx), (int) (y0 + Wy - 4 + flecha));
        g.setStroke(new BasicStroke(1.6f));
        g.setColor(tinta);
        int ax = (int) (xm + 0.5 * Wx), ay = (int) (y0 + 0.5 * Wy - 8 + flecha);
        g.drawLine(ax, ay - 30, ax, ay);
        g.fillPolygon(new int[]{ax - 4, ax + 4, ax}, new int[]{ay - 7, ay - 7, ay}, 3);
        g.setStroke(new BasicStroke(1f));
        double fN = roto ? 0 : carga * F_MAX_KGF * 9.80665;
        g.setFont(Tema.ui(Font.BOLD, 9));
        g.drawString(String.format(ES, "Carga %.0f N", fN), ax + 6, ay - 26);
        g.setFont(Tema.ui(Font.PLAIN, 8));
        g.setColor(new Color(0x55, 0x5E, 0x6A));
        g.drawString(String.format(ES, "σ = %.2f MPa   δ = %.2f mm", roto ? 0 : 17.9 * carga, roto ? 0 : 0.42 * carga), ax + 6, ay - 16);
    }

    private static void pantallaPrensa(Graphics2D g, Rectangle v, double tv, double F, boolean roto) {
        // "video": pantalla de la prensa con lectura digital
        int sx = v.x + 10, sy = v.y + 6, sw = 150, sh = v.height - 18;
        g.setColor(new Color(0x05, 0x08, 0x06));
        g.fillRoundRect(sx, sy, sw, sh, 6, 6);
        g.setColor(new Color(0x3A, 0x44, 0x3C));
        g.drawRoundRect(sx, sy, sw, sh, 6, 6);
        g.setFont(Tema.ui(Font.PLAIN, 7));
        g.setColor(new Color(0x60, 0xA0, 0x70));
        g.drawString("PRENSA · CARGA (kgf)", sx + 6, sy + 10);
        g.setFont(Tema.mono(Font.BOLD, 20));
        g.setColor(roto ? new Color(0xFF, 0x70, 0x60) : new Color(0x7C, 0xFF, 0x9A));
        g.drawString(String.format(Locale.US, "%05.1f", Math.max(0, F)), sx + 14, sy + 32);
        g.setFont(Tema.ui(Font.BOLD, 7));
        if (roto) g.drawString("ROTURA", sx + sw - 40, sy + 32);
        // barra de reproduccion
        int px = v.x + 10, py = v.y + v.height - 8, pw = v.width - 20;
        g.setColor(new Color(0x40, 0x48, 0x54));
        g.fillRect(px, py, pw, 3);
        g.setColor(C_AZ);
        g.fillRect(px, py, (int) (pw * tv / T_VID), 3);
        g.setFont(Tema.ui(Font.PLAIN, 8));
        g.setColor(new Color(0xB0, 0xBA, 0xC6));
        g.drawString(String.format(Locale.US, "▶ 00:%02d / 00:09", (int) tv), sx + sw + 12, sy + 12);
        g.drawString("La curva se dibuja con el video →", sx + sw + 12, sy + 26);
    }

    private static void curva(Graphics2D g, Rectangle r, double tv, boolean video, Color tinta, Color gris) {
        g.setFont(Tema.ui(Font.BOLD, 10));
        g.setColor(tinta);
        g.drawString("CURVA CARGA – TIEMPO", r.x + 10, r.y + 13);
        g.setFont(Tema.ui(Font.PLAIN, 8));
        g.setColor(gris);
        g.drawString(video ? "datos del video · en carga" : "vista previa simulada", r.x + 10, r.y + 23);
        int px = r.x + 34, py = r.y + 32, pw = r.width - 44, ph = r.height - 58;
        double yMax = 48;
        g.setColor(new Color(0xE6, 0xE9, 0xEE));
        for (int i = 0; i <= 4; i++) { int yy = py + ph * i / 4; g.drawLine(px, yy, px + pw, yy); }
        for (int i = 0; i <= 6; i++) { int xx = px + pw * i / 6; g.drawLine(xx, py, xx, py + ph); }
        g.setColor(tinta);
        g.drawLine(px, py, px, py + ph);
        g.drawLine(px, py + ph, px + pw, py + ph);
        g.setFont(Tema.ui(Font.PLAIN, 7));
        for (int i = 0; i <= 4; i++) {
            String t = String.valueOf((int) (yMax * (4 - i) / 4));
            g.drawString(t, px - 4 - g.getFontMetrics().stringWidth(t), py + ph * i / 4 + 3);
        }
        for (int i = 0; i <= 6; i++) g.drawString(String.format(ES, "%.1f", T_VID * i / 6), px + pw * i / 6 - 6, py + ph + 9);
        g.drawString("Tiempo (s)", px + pw / 2 - 18, py + ph + 21);
        Graphics2D gr = (Graphics2D) g.create();
        gr.rotate(-Math.PI / 2, r.x + 9, py + ph / 2.0);
        gr.drawString("Carga (kgf)", r.x - 10, py + ph / 2 + 3);
        gr.dispose();
        // leyenda
        g.setColor(new Color(0xA0, 0xA6, 0xAE));
        g.drawLine(px + pw - 132, py + 6, px + pw - 116, py + 6);
        g.drawString("dispersión del lote", px + pw - 112, py + 9);
        g.setColor(tinta);
        g.setStroke(new BasicStroke(2f));
        g.drawLine(px + pw - 132, py + 15, px + pw - 116, py + 15);
        g.setStroke(new BasicStroke(1f));
        g.drawString("probeta", px + pw - 112, py + 18);

        // curvas del lote (grises)
        double[][] lote = {{34, 6.2}, {37.5, 6.9}, {44, 8.0}};
        g.setColor(new Color(0xA0, 0xA6, 0xAE));
        for (double[] l : lote) {
            Path2D.Double p = new Path2D.Double();
            for (int i = 0; i <= 200; i++) {
                double t = T_VID * i / 200.0;
                double X = px + pw * t / T_VID, Y = py + ph * (1 - cargaLote(t, l[0], l[1]) / yMax);
                if (i == 0) p.moveTo(X, Y); else p.lineTo(X, Y);
            }
            g.draw(p);
        }
        // curva de la probeta (negra) hasta el tiempo actual del video
        if (video && tv > 0) {
            Path2D.Double p = new Path2D.Double();
            int n = (int) (300 * tv / T_VID);
            for (int i = 0; i <= n; i++) {
                double t = T_VID * i / 300.0;
                double X = px + pw * t / T_VID, Y = py + ph * (1 - carga(t) / yMax);
                if (i == 0) p.moveTo(X, Y); else p.lineTo(X, Y);
            }
            g.setColor(tinta);
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(p);
            g.setStroke(new BasicStroke(1f));
            if (tv > T_ROT) {
                int X = px + (int) (pw * T_ROT / T_VID), Y = py + (int) (ph * (1 - F_MAX_KGF / yMax));
                g.setColor(Tema.ERROR);
                g.fillOval(X - 3, Y - 3, 6, 6);
                g.setFont(Tema.ui(Font.BOLD, 8));
                g.drawString(String.format(ES, "F máx %.1f kgf", F_MAX_KGF), X - 74, Y + 10);
            }
        }
    }

    // ================================================================ cursor
    private double[] posCursor(double s) {
        if (rM1 == null || s > T_CONT + 0.03) return null;
        java.util.List<double[]> par = new java.util.ArrayList<>();
        par.add(new double[]{0.015, rM1.x + 40, rM1.y + 40});
        par.add(new double[]{T_M1, rM1.x + 50, rM1.y + 14});
        par.add(new double[]{T_PRENS, rPrens.x + 30, rPrens.y + 9});
        par.add(new double[]{T_UP1, rUp.x + 4, rUp.y + 3});
        par.add(new double[]{T_UP2, rUp.x + 4, rUp.y + 3});
        par.add(new double[]{T_APL, rApl.x + 26, rApl.y + 11});
        // absorcion: el cursor salta a cada celda que se escribe
        agregarCeldas(par, cAbs, T_ABS0, T_ABS1, ABS);
        par.add(new double[]{T_CABS, rCAbs.x + 50, rCAbs.y + 11});
        par.add(new double[]{T_SCR1, rCAbs.x + 50, rCAbs.y + 11});
        agregarCeldas(par, cCon, T_CON0, T_CON1, CON);
        par.add(new double[]{T_CCON, rCCon.x + 50, rCCon.y + 11});
        par.add(new double[]{T_COMBO, rCombo.x + 30, rCombo.y + 9});
        par.add(new double[]{T_BFLEX, rBFlex.x + 50, rBFlex.y + 11});
        if (rLuz != null) {
            par.add(new double[]{T_LUZ_CLIC, rLuz.x + 40, rLuz.y + 8});
            par.add(new double[]{T_LUZ1 + 0.01, rLuz.x + 40, rLuz.y + 8});
            par.add(new double[]{T_CARGAR, rCargar.x + 40, rCargar.y + 11});
            par.add(new double[]{T_VID1, rCargar.x + 40, rCargar.y + 30});
            par.add(new double[]{T_CONT, rCont.x + 40, rCont.y + 11});
        }
        return trayecto(s, par.toArray(new double[0][]));
    }

    private static void agregarCeldas(java.util.List<double[]> par, Rectangle[][] cel, double t0, double t1, String[][] val) {
        int total = 0;
        for (String[] r : val) for (String v : r) total += v.length();
        int acum = 0;
        for (int r = 0; r < val.length; r++) {
            for (int c = 0; c < val[r].length; c++) {
                Rectangle q = cel[r][c];
                double ta = t0 + (t1 - t0) * acum / total;
                acum += val[r][c].length();
                if (q != null) par.add(new double[]{ta, q.x + q.width / 2.0 + 18, q.y + 8});
            }
        }
    }
}
