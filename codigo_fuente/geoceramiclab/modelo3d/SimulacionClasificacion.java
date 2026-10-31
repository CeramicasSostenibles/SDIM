package geoceramiclab.modelo3d;

import geoceramiclab.Tema;
import geoceramiclab.graficas.CartaPlasticidadPanel;
import geoceramiclab.graficas.CurvaGranPanel;
import geoceramiclab.graficas.TrianguloTextural;
import geoceramiclab.model.ClasificadorSUCS;
import geoceramiclab.model.SueloData;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Paso 2 de la ayuda: animacion "tipo video" de la clasificacion del suelo.
 *
 * Cuatro escenas en bucle: (1) Granulometria, (2) Hidrometro, (3) Atterberg y
 * (4) Clasificacion final. En cada ensayo se simula que el usuario escribe los
 * datos en la tabla, lleva el cursor a «Calcular», hace clic y aparecen la
 * grafica (la REAL del programa, pre-renderizada) y el resultado.
 *
 * Todos los datos son de UNA misma muestra (limo-arcilloso de Vetas), asi que
 * los tres ensayos son coherentes y la clasificacion final es CL.
 */
final class SimulacionClasificacion {

    /** Duracion de cada escena (ms). */
    static final long DUR = 7000;
    static final int ESCENAS = 4;

    // --------------------------------------------------------- datos de la muestra
    // Granulometria (masa 500 g): tamiz, mm, peso retenido, % pasa
    private static final String[][] GRAN = {
        {"N° 4",   "4.750", "0.0",  "100.0"},
        {"N° 10",  "2.000", "5.0",  "99.0"},
        {"N° 40",  "0.425", "10.0", "97.0"},
        {"N° 100", "0.150", "15.0", "94.0"},
        {"N° 200", "0.075", "17.5", "90.5"},
    };
    private static final double[][] GRAN_PTS = {
        {4.75, 100}, {2.0, 99}, {0.425, 97}, {0.15, 94}, {0.075, 90.5}
    };
    // Hidrometro: t (min), lectura, % finos, D (mm)
    private static final String[][] HIDRO = {
        {"1",   "1.025", "75.0", "0.0400"},
        {"4",   "1.019", "55.0", "0.0200"},
        {"60",  "1.007", "20.0", "0.0050"},
        {"240", "1.004", "9.4",  "0.0020"},
    };
    private static final double ARCILLA = 9.4, LIMO = 73.3, ARENA_F = 17.3;
    private static final String TEXTURAL = "Franco limoso";
    // Atterberg: LL (golpes, humedad) y LP (humedad)
    private static final String[][] LL_TAB = {{"15", "35.1"}, {"22", "33.4"}, {"30", "31.9"}};
    private static final String[][] LP_TAB = {{"1", "22.3"}, {"2", "22.1"}};
    private static final double LL = 32.7, LP = 22.2, IP = 10.5;

    // --------------------------------------------------------- imagenes
    private static final int IW = 600, IH = 330;     // tamano de render de las graficas grandes
    private final BufferedImage[] curva;             // 0 = vacia, k = k puntos
    private final BufferedImage triVacio, triPunto, cartaVacia, cartaPunto;

    private final String simbolo;
    private final String nombre;

    SimulacionClasificacion() {
        curva = new BufferedImage[GRAN_PTS.length + 1];
        for (int k = 0; k <= GRAN_PTS.length; k += GRAN_PTS.length) {
            CurvaGranPanel c = new CurvaGranPanel();
            c.setEscalaAuto(false);
            List<double[]> d = new ArrayList<>();
            for (int j = 0; j < k; j++) d.add(GRAN_PTS[j]);
            c.setData(d);
            curva[k] = render(c, IW, IH);
        }
        TrianguloTextural t = new TrianguloTextural();
        triVacio = render(t, IW, IH);
        t.setPuntoTextural(ARCILLA, LIMO, ARENA_F, TEXTURAL);
        triPunto = render(t, IW, IH);

        CartaPlasticidadPanel cp = new CartaPlasticidadPanel();
        cartaVacia = render(cp, IW, IH);
        cp.setPunto(LL, IP, "CL");
        cartaPunto = render(cp, IW, IH);

        List<double[]> todo = new ArrayList<>();
        for (double[] p : GRAN_PTS) todo.add(p);

        // Clasificacion con el mismo motor del programa (cae a CL si algo falla)
        String sim = "CL", nom = "Arcilla de baja plasticidad";
        try {
            SueloData s = new SueloData();
            s.pctGrava = 0; s.pctArena = 9.5; s.pctFinos = 90.5;
            s.puntosCurvaGran = todo; s.granCalculada = true;
            s.pctArcilla = ARCILLA; s.pctLimo = LIMO; s.pctArenaFina = ARENA_F;
            s.claseTextural = TEXTURAL; s.hidroCalculada = true;
            s.LL = LL; s.LP = LP; s.IP = IP; s.atterCalculada = true;
            ClasificadorSUCS.Resultado r = ClasificadorSUCS.clasificar(s);
            if (r.simbolo != null && !r.simbolo.isBlank()) { sim = r.simbolo; nom = r.nombre; }
        } catch (RuntimeException ignorar) { }
        simbolo = sim;
        nombre = nom;
    }

    private static BufferedImage render(JComponent comp, int w, int h) {
        comp.setSize(w, h);
        comp.doLayout();
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = bi.createGraphics();
        g.setColor(Tema.SUPERFICIE);
        g.fillRect(0, 0, w, h);
        comp.printAll(g);
        g.dispose();
        return bi;
    }

    // --------------------------------------------------------- dibujo

    // Tiempos dentro de cada escena (fraccion 0..1)
    private static final double T_ESCRIBE_0 = 0.04, T_ESCRIBE_1 = 0.52;
    private static final double T_CURSOR = 0.54, T_CLIC = 0.66, T_GRAF = 0.70, T_RES = 0.76;

    private static final String[] PESTANAS = {"① Granulometría", "② Hidrómetro", "③ Atterberg", "④ Clasificación"};

    void pintar(Graphics2D g, long ms, int w, int h) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        long ciclo = ms % (DUR * ESCENAS);
        int esc = (int) (ciclo / DUR);
        double s = (ciclo % DUR) / (double) DUR;

        pestanas(g, esc, w);

        Rectangle izq = new Rectangle(8, 34, 270, h - 34 - 32);
        Rectangle der = new Rectangle(286, 34, w - 286 - 8, h - 34 - 32);
        Rectangle res = new Rectangle(8, h - 26, w - 16, 22);

        switch (esc) {
            case 0:  escenaGran(g, s, izq, der, res);  break;
            case 1:  escenaHidro(g, s, izq, der, res); break;
            case 2:  escenaAtter(g, s, izq, der, res); break;
            default: escenaFinal(g, s, w, h);
        }

        // Fundido corto al empezar cada escena
        if (s < 0.035) {
            float a = (float) (1 - s / 0.035);
            g.setColor(new Color(Tema.SUPERFICIE.getRed(), Tema.SUPERFICIE.getGreen(),
                                 Tema.SUPERFICIE.getBlue(), Math.round(a * 255)));
            g.fillRect(0, 30, w, h - 30);
        }
    }

    private void pestanas(Graphics2D g, int esc, int w) {
        Font f = Tema.ui(Font.BOLD, 11);
        g.setFont(f);
        FontMetrics fm = g.getFontMetrics();
        int x = 8, y = 6, hh = 20;
        for (int k = 0; k < PESTANAS.length; k++) {
            String t = (k < esc ? "✓ " : "") + PESTANAS[k];
            int tw = fm.stringWidth(t) + 18;
            if (k == esc) {
                g.setColor(Tema.ACENTO);
                g.fillRoundRect(x, y, tw, hh, 6, 6);
                g.setColor(Color.WHITE);
            } else {
                g.setColor(Tema.SUPERFICIE_2);
                g.fillRoundRect(x, y, tw, hh, 6, 6);
                g.setColor(k < esc ? Tema.OK : Tema.TEXTO_3);
            }
            g.drawString(t, x + 9, y + 14);
            x += tw + 4;
            if (k < PESTANAS.length - 1) {
                g.setColor(Tema.TEXTO_3);
                g.drawString("→", x, y + 14);
                x += fm.stringWidth("→") + 4;
            }
        }
        g.setColor(Tema.BORDE);
        g.drawLine(8, 30, w - 8, 30);
    }

    // ---------------- escena 1: granulometria
    private void escenaGran(Graphics2D g, double s, Rectangle izq, Rectangle der, Rectangle res) {
        int x = izq.x, y = izq.y;
        campo(g, x, y, "Masa total (g)", "500", 92, 60);

        String[] cab = {"Tamiz", "mm", "Peso ret. (g)", "% Pasa"};
        int[] anc = {52, 52, 92, 74};
        String[][] filas = new String[GRAN.length][];
        int total = 0;
        for (String[] r : GRAN) total += r[2].length();
        int esc = escritos(s, total);
        boolean calc = s >= T_GRAF;
        int[] cur = null;
        int usados = 0;
        for (int i = 0; i < GRAN.length; i++) {
            int n = Math.max(0, Math.min(GRAN[i][2].length(), esc - usados));
            usados += GRAN[i][2].length();
            if (cur == null && n < GRAN[i][2].length()) cur = new int[]{i, 2};
            filas[i] = new String[]{GRAN[i][0], GRAN[i][1], GRAN[i][2].substring(0, n), calc ? GRAN[i][3] : ""};
        }
        Rectangle tab = tabla(g, x, y + 28, cab, anc, filas, new boolean[]{false, false, true, false}, s, cur);

        Rectangle btn = boton(g, x, tab.y + tab.height + 8, "Calcular granulometría", s);
        cursor(g, s, tab, btn);

        // La curva "se dibuja" de izquierda a derecha sobre la grafica vacia
        grafica(g, curva[0], der, s);
        if (s >= T_GRAF) {
            double k = Math.min(1, (s - T_GRAF) / 0.10);
            Shape clip = g.getClip();
            g.clipRect(der.x, der.y, (int) Math.round(der.width * k), der.height);
            g.drawImage(curva[GRAN_PTS.length], der.x, der.y, der.width, der.height, null);
            g.setClip(clip);
        }

        resultado(g, s, res,
            "Grava 0.0 %  ·  Arena 9.5 %  ·  Finos 90.5 %",
            "→  Finos ≥ 50 %: suelo FINO · se necesitan los límites de Atterberg", Tema.AVISO);
    }

    // ---------------- escena 2: hidrometro
    private void escenaHidro(Graphics2D g, double s, Rectangle izq, Rectangle der, Rectangle res) {
        int x = izq.x, y = izq.y;
        campo(g, x, y, "Masa seca (g)", "50", 88, 46);
        campo(g, x + 142, y, "Gs", "2.65", 22, 46);

        String[] cab = {"t (min)", "Lectura", "% Finos", "D (mm)"};
        int[] anc = {58, 66, 70, 76};
        int total = 0;
        for (String[] r : HIDRO) total += r[0].length() + r[1].length();
        int esc = escritos(s, total);
        boolean calc = s >= T_GRAF;
        String[][] filas = new String[HIDRO.length][];
        int[] cur = null;
        int usados = 0;
        for (int i = 0; i < HIDRO.length; i++) {
            String[] cel = new String[4];
            for (int c = 0; c < 2; c++) {
                String v = HIDRO[i][c];
                int n = Math.max(0, Math.min(v.length(), esc - usados));
                usados += v.length();
                if (cur == null && n < v.length()) cur = new int[]{i, c};
                cel[c] = v.substring(0, n);
            }
            cel[2] = calc ? HIDRO[i][2] : "";
            cel[3] = calc ? HIDRO[i][3] : "";
            filas[i] = cel;
        }
        Rectangle tab = tabla(g, x, y + 28, cab, anc, filas, new boolean[]{true, true, false, false}, s, cur);

        Rectangle btn = boton(g, x, tab.y + tab.height + 8, "Calcular hidrómetro", s);
        cursor(g, s, tab, btn);

        grafica(g, s >= T_GRAF ? triPunto : triVacio, der, s);

        resultado(g, s, res,
            String.format("Arena %.1f %%  ·  Limo %.1f %%  ·  Arcilla %.1f %%", ARENA_F, LIMO, ARCILLA),
            "→  Clase textural: " + TEXTURAL, Tema.OK);
    }

    // ---------------- escena 3: Atterberg
    private void escenaAtter(Graphics2D g, double s, Rectangle izq, Rectangle der, Rectangle res) {
        int x = izq.x, y = izq.y;
        int total = 0;
        for (String[] r : LL_TAB) total += r[0].length() + r[1].length();
        for (String[] r : LP_TAB) total += r[1].length();
        int esc = escritos(s, total);
        int usados = 0;
        int[] curLL = null, curLP = null;

        g.setFont(Tema.ui(Font.BOLD, 10));
        g.setColor(Tema.TEXTO_2);
        g.drawString("Límite Líquido", x, y + 10);
        String[][] fll = new String[LL_TAB.length][];
        for (int i = 0; i < LL_TAB.length; i++) {
            String[] cel = new String[2];
            for (int c = 0; c < 2; c++) {
                String v = LL_TAB[i][c];
                int n = Math.max(0, Math.min(v.length(), esc - usados));
                usados += v.length();
                if (curLL == null && n < v.length()) curLL = new int[]{i, c};
                cel[c] = v.substring(0, n);
            }
            fll[i] = cel;
        }
        Rectangle t1 = tabla(g, x, y + 15, new String[]{"N° golpes", "Humedad %"}, new int[]{62, 70},
                             fll, new boolean[]{true, true}, s, curLL);

        g.setFont(Tema.ui(Font.BOLD, 10));
        g.setColor(Tema.TEXTO_2);
        g.drawString("Límite Plástico", x + 142, y + 10);
        String[][] flp = new String[LP_TAB.length][];
        for (int i = 0; i < LP_TAB.length; i++) {
            String v = LP_TAB[i][1];
            int n = Math.max(0, Math.min(v.length(), esc - usados));
            usados += v.length();
            if (curLL == null && curLP == null && n < v.length()) curLP = new int[]{i, 1};
            flp[i] = new String[]{LP_TAB[i][0], v.substring(0, n)};
        }
        Rectangle t2 = tabla(g, x + 142, y + 15, new String[]{"Ensayo", "Humedad %"}, new int[]{50, 78},
                             flp, new boolean[]{false, true}, s, curLP);

        // Resultados numericos LL / LP / IP
        int ry = Math.max(t1.y + t1.height, t2.y + t2.height) + 10;
        boolean calc = s >= T_GRAF;
        campo(g, x, ry, "LL", calc ? String.format("%.1f", LL) : "", 18, 46);
        campo(g, x + 90, ry, "LP", calc ? String.format("%.1f", LP) : "", 18, 46);
        campo(g, x + 180, ry, "IP", calc ? String.format("%.1f", IP) : "", 16, 46);

        Rectangle tabla = new Rectangle(x, t1.y, 270, t1.height);
        Rectangle btn = boton(g, x, ry + 30, "Calcular Atterberg", s);
        cursor(g, s, curLP != null ? t2 : tabla, btn);

        grafica(g, calc ? cartaPunto : cartaVacia, der, s);

        double lineaA = 0.73 * (LL - 20);
        resultado(g, s, res,
            String.format("Punto (LL %.1f ; IP %.1f)", LL, IP),
            String.format("→  sobre la línea A (%.1f) e IP > 7  →  zona CL", lineaA), Tema.INFO);
    }

    // ---------------- escena 4: clasificacion final
    private void escenaFinal(Graphics2D g, double s, int w, int h) {
        int x = 12, y = 40;

        // Insignia con el simbolo
        double a = Math.min(1, s / 0.12);
        int bw = 96, bh = 74;
        g.setColor(mezcla(Tema.OK, Tema.SUPERFICIE, 0.18 * a));
        g.fillRoundRect(x, y, bw, bh, 12, 12);
        g.setColor(mezcla(Tema.OK, Tema.SUPERFICIE, a));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(x, y, bw, bh, 12, 12);
        g.setStroke(new BasicStroke(1f));
        Font fs = Tema.fuente(Font.BOLD, simbolo.length() > 4 ? 24f : 36f);
        g.setFont(fs);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(simbolo, x + (bw - fm.stringWidth(simbolo)) / 2, y + bh / 2 + fm.getAscent() / 2 - 4);

        g.setColor(Tema.TEXTO);
        g.setFont(Tema.ui(Font.BOLD, 14));
        g.drawString(nombre, x + bw + 14, y + 22);
        g.setColor(Tema.OK);
        g.setFont(Tema.ui(Font.PLAIN, 10));
        g.drawString("● SUCS completa (ASTM D2487)", x + bw + 14, y + 40);
        g.setColor(Tema.TEXTO_3);
        g.drawString("Granulometría · Hidrómetro · Atterberg", x + bw + 14, y + 56);

        // Razonamiento, linea por linea
        double lineaA = 0.73 * (LL - 20);
        String[] pasos = {
            "1.  Finos 90.5 % ≥ 50 %  →  suelo FINO",
            String.format("2.  LL = %.1f < 50  →  baja plasticidad (L)", LL),
            String.format("3.  IP = %.1f > línea A = %.1f  e  IP > 7  →  arcilla (C)", IP, lineaA),
            "→  " + simbolo + " · " + nombre
        };
        g.setFont(Tema.ui(Font.PLAIN, 12));
        int py = y + bh + 24;
        for (int k = 0; k < pasos.length; k++) {
            double t = 0.15 + k * 0.12;
            if (s < t) break;
            boolean ult = k == pasos.length - 1;
            g.setFont(Tema.ui(ult ? Font.BOLD : Font.PLAIN, 12));
            g.setColor(ult ? Tema.OK : Tema.TEXTO);
            g.drawString(pasos[k], x, py + k * 22);
        }

    }

    // --------------------------------------------------------- piezas

    private static int escritos(double s, int total) {
        if (s <= T_ESCRIBE_0) return 0;
        if (s >= T_ESCRIBE_1) return total;
        return (int) Math.floor((s - T_ESCRIBE_0) / (T_ESCRIBE_1 - T_ESCRIBE_0) * total);
    }

    private static void campo(Graphics2D g, int x, int y, String etiqueta, String valor, int ancEtq, int ancVal) {
        g.setFont(Tema.ui(Font.PLAIN, 10));
        g.setColor(Tema.TEXTO_2);
        g.drawString(etiqueta, x, y + 14);
        int fx = x + ancEtq + 4;
        g.setColor(Tema.SUPERFICIE_2);
        g.fillRoundRect(fx, y + 2, ancVal, 18, 4, 4);
        g.setColor(Tema.BORDE);
        g.drawRoundRect(fx, y + 2, ancVal, 18, 4, 4);
        g.setColor(Tema.TEXTO);
        g.setFont(Tema.ui(Font.BOLD, 11));
        g.drawString(valor, fx + 6, y + 15);
    }

    /** Dibuja una tabla; devuelve su rectangulo. cur = {fila, columna} de la celda que se esta escribiendo. */
    private static Rectangle tabla(Graphics2D g, int x, int y, String[] cab, int[] anc, String[][] filas,
                                   boolean[] editable, double s, int[] cur) {
        int rh = 17, hh = 17, tw = 0;
        for (int a : anc) tw += a;
        g.setColor(Tema.SUPERFICIE_2);
        g.fillRect(x, y, tw, hh);
        g.setFont(Tema.ui(Font.BOLD, 10));
        int cx = x;
        for (int c = 0; c < cab.length; c++) {
            g.setColor(Tema.TEXTO_2);
            g.drawString(cab[c], cx + 4, y + 12);
            cx += anc[c];
        }
        g.setFont(Tema.ui(Font.PLAIN, 10));
        for (int r = 0; r < filas.length; r++) {
            int ry = y + hh + r * rh;
            cx = x;
            for (int c = 0; c < cab.length; c++) {
                boolean activa = cur != null && cur[0] == r && cur[1] == c
                        && s > T_ESCRIBE_0 && s < T_ESCRIBE_1;
                g.setColor(activa ? mezcla(Tema.ACENTO, Tema.FONDO, 0.25)
                                  : (editable[c] ? Tema.SUPERFICIE : Tema.FONDO));
                g.fillRect(cx, ry, anc[c], rh);
                if (activa) {
                    g.setColor(Tema.ACENTO);
                    g.drawRect(cx, ry, anc[c] - 1, rh - 1);
                }
                String v = filas[r][c] == null ? "" : filas[r][c];
                g.setColor(editable[c] ? Tema.INFO : (c == cab.length - 1 || c == 2 ? Tema.OK : Tema.TEXTO));
                if (c == 0 && !editable[c]) g.setColor(Tema.TEXTO);
                g.drawString(v, cx + 4, ry + 12);
                if (activa && ((long) (s * DUR / 300)) % 2 == 0) {
                    int px = cx + 5 + g.getFontMetrics().stringWidth(v);
                    g.setColor(Tema.ACENTO_CLARO);
                    g.fillRect(px, ry + 3, 1, rh - 6);
                }
                cx += anc[c];
            }
            g.setColor(Tema.BORDE);
            g.drawLine(x, ry + rh - 1, x + tw, ry + rh - 1);
        }
        int th = hh + filas.length * rh;
        g.setColor(Tema.BORDE);
        g.drawRect(x, y, tw, th);
        return new Rectangle(x, y, tw, th);
    }

    private static Rectangle boton(Graphics2D g, int x, int y, String txt, double s) {
        g.setFont(Tema.ui(Font.BOLD, 11));
        FontMetrics fm = g.getFontMetrics();
        int bw = fm.stringWidth(txt) + 24, bh = 22;
        boolean pulsado = s >= T_CLIC && s < T_CLIC + 0.04;
        boolean listo = s >= T_GRAF;
        Color fondo = pulsado ? Tema.ACENTO_CLARO : Tema.ACENTO;
        g.setColor(fondo);
        g.fillRoundRect(x, y + (pulsado ? 1 : 0), bw, bh, 6, 6);
        g.setColor(Color.WHITE);
        g.drawString(txt, x + 12, y + 15 + (pulsado ? 1 : 0));
        if (listo && s < T_GRAF + 0.08) {    // onda del clic
            double k = (s - T_GRAF) / 0.08;
            int r = (int) (6 + 22 * k);
            g.setColor(new Color(Tema.ACENTO_CLARO.getRed(), Tema.ACENTO_CLARO.getGreen(),
                                 Tema.ACENTO_CLARO.getBlue(), (int) (160 * (1 - k))));
            g.setStroke(new BasicStroke(2f));
            g.drawOval(x + bw / 2 - r, y + bh / 2 - r, 2 * r, 2 * r);
            g.setStroke(new BasicStroke(1f));
        }
        return new Rectangle(x, y, bw, bh);
    }

    /** Cursor que va de la tabla al boton «Calcular» y hace clic. */
    private static void cursor(Graphics2D g, double s, Rectangle desde, Rectangle boton) {
        if (s < T_CURSOR || s > T_RES + 0.10) return;
        double k = Math.min(1, (s - T_CURSOR) / (T_CLIC - T_CURSOR));
        k = k * k * (3 - 2 * k);    // suavizado
        double x0 = desde.x + desde.width - 20, y0 = desde.y + desde.height - 8;
        double x1 = boton.x + boton.width * 0.6, y1 = boton.y + boton.height * 0.6;
        int cx = (int) (x0 + (x1 - x0) * k), cy = (int) (y0 + (y1 - y0) * k);
        Path2D.Double p = new Path2D.Double();
        p.moveTo(cx, cy);
        p.lineTo(cx, cy + 15);
        p.lineTo(cx + 4, cy + 11);
        p.lineTo(cx + 7, cy + 17);
        p.lineTo(cx + 9, cy + 16);
        p.lineTo(cx + 6, cy + 10);
        p.lineTo(cx + 11, cy + 10);
        p.closePath();
        g.setColor(Color.WHITE);
        g.fill(p);
        g.setColor(Color.BLACK);
        g.draw(p);
    }

    private static void grafica(Graphics2D g, BufferedImage img, Rectangle r, double s) {
        g.drawImage(img, r.x, r.y, r.width, r.height, null);
        boolean destello = s >= T_GRAF && s < T_GRAF + 0.12;
        g.setColor(destello ? Tema.ACENTO_CLARO : Tema.BORDE);
        g.setStroke(new BasicStroke(destello ? 2f : 1f));
        g.drawRect(r.x - 1, r.y - 1, r.width + 1, r.height + 1);
        g.setStroke(new BasicStroke(1f));
    }

    private static void resultado(Graphics2D g, double s, Rectangle r, String datos, String concl, Color c) {
        if (s < T_RES) {
            g.setColor(Tema.TEXTO_3);
            g.setFont(Tema.ui(Font.ITALIC, 11));
            g.drawString(s < T_CURSOR ? "Escribiendo los datos del ensayo…" : "Pulsando «Calcular»…", r.x + 8, r.y + 15);
            return;
        }
        double a = Math.min(1, (s - T_RES) / 0.06);
        g.setColor(mezcla(c, Tema.SUPERFICIE, 0.14 * a));
        g.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
        g.setColor(mezcla(c, Tema.SUPERFICIE, 0.7 * a));
        g.drawRoundRect(r.x, r.y, r.width, r.height, 8, 8);
        g.setFont(Tema.ui(Font.PLAIN, 11));
        g.setColor(mezcla(Tema.TEXTO, Tema.SUPERFICIE, a));
        g.drawString(datos, r.x + 10, r.y + 15);
        int dx = r.x + 10 + g.getFontMetrics().stringWidth(datos) + 10;
        g.setFont(Tema.ui(Font.BOLD, 11));
        g.setColor(mezcla(c, Tema.SUPERFICIE, a));
        g.drawString(concl, dx, r.y + 15);
    }

    private static Color mezcla(Color a, Color b, double t) {
        t = Math.max(0, Math.min(1, t));
        return new Color(
            (int) Math.round(a.getRed()   * t + b.getRed()   * (1 - t)),
            (int) Math.round(a.getGreen() * t + b.getGreen() * (1 - t)),
            (int) Math.round(a.getBlue()  * t + b.getBlue()  * (1 - t)));
    }
}
