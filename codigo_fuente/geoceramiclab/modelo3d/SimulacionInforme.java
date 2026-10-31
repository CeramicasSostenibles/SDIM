package geoceramiclab.modelo3d;

import geoceramiclab.Tema;

import java.awt.*;
import java.awt.geom.Path2D;

import static geoceramiclab.modelo3d.AnimUtil.*;

/**
 * Paso 7 de la ayuda: Informe final. Se señala «Descargar informe final (PDF)»,
 * se pulsa y se hojea rapido el informe de 4 paginas (como un video corto).
 */
final class SimulacionInforme {

    static final long DUR = 13000;

    private static final Color C_RES = new Color(0x5A, 0x60, 0xC0);
    private static final Color AZUL = new Color(0x2A, 0x5C, 0xB0);
    private static final Color TINTA = new Color(0x1C, 0x22, 0x2A);
    private static final Color GRIS = new Color(0x8A, 0x92, 0x9C);
    private static final Color LINEA = new Color(0xD8, 0xDD, 0xE3);
    private static final Color LADRILLO = new Color(0xC8, 0x6A, 0x3C);

    private static final double T_GLOBO = 0.06, T_CLIC = 0.24, T_BAJA0 = 0.26, T_BAJA1 = 0.34, T_PDF = 0.36;
    private static final String[] PAGINAS = {
        "1 · Procedencia y clasificación del suelo",
        "2 · Baldosín: dosificación y secciones",
        "3 · Curvas de secado y cocción",
        "4 · Resumen de ensayos y uso recomendado"
    };

    // Las 4 paginas se dibujan una vez a buen tamaño y luego se muestran reducidas
    private static final int PW = 420, PH = (int) Math.round(420 * 842 / 595.0);
    private final java.awt.image.BufferedImage[] paginas = new java.awt.image.BufferedImage[4];

    SimulacionInforme() {
        for (int i = 0; i < 4; i++) {
            java.awt.image.BufferedImage bi = new java.awt.image.BufferedImage(PW, PH, java.awt.image.BufferedImage.TYPE_INT_RGB);
            Graphics2D g = bi.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            dibujarPagina(g, i, 0, 0, PW, PH);
            g.dispose();
            paginas[i] = bi;
        }
    }

    private void pagina(Graphics2D g, int n, int x, int y, int w, int h) {
        g.setColor(new Color(0, 0, 0, 70));
        g.fillRect(x + 3, y + 3, w, h);
        Object q = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(paginas[n], x, y, w, h, null);
        if (q != null) g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, q);
    }

    void pintar(Graphics2D g, long ms, int w, int h) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        double s = (ms % DUR) / (double) DUR;

        if (s < T_PDF) resumen(g, s, w, h);
        else visor(g, s, w, h);

        String ley;
        if (s < T_CLIC)       ley = "Al final de los ensayos aparece el resumen con el USO RECOMENDADO de la baldosa";
        else if (s < T_PDF)   ley = "«Descargar informe final (PDF)» guarda el informe completo del estudio";
        else                  ley = "El informe reúne lugar, clasificación, dosificación, secado, cocción, ensayos y uso recomendado";
        leyenda(g, 8, h - 23, w - 16, ley);

        if (s < 0.015 || (s > T_PDF - 0.01 && s < T_PDF + 0.01)) {
            double a = s < 0.015 ? 1 - s / 0.015 : 1 - Math.abs(s - T_PDF) / 0.01;
            g.setColor(alfa(Tema.SUPERFICIE, (int) (255 * clamp01(a))));
            g.fillRect(0, 0, w, h - 26);
        }
    }

    // ------------------------------------------------------------ pantalla Resumen
    private void resumen(Graphics2D g, double s, int w, int h) {
        // mapa: nodo 5
        int nx = 8, ny = 92, nw = 112, nh = 34;
        g.setColor(C_RES);
        g.setStroke(new BasicStroke(1.6f));
        g.drawLine(nx + nw / 2, 4, nx + nw / 2, ny);
        g.drawLine(nx + nw, ny + nh / 2, 136, ny + nh / 2);
        g.fillPolygon(new int[]{136, 130, 130}, new int[]{ny + nh / 2, ny + nh / 2 - 3, ny + nh / 2 + 3}, 3);
        g.setStroke(new BasicStroke(1f));
        g.setColor(mezcla(C_RES, Tema.SUPERFICIE, 0.35));
        g.fillRoundRect(nx, ny, nw, nh, 10, 10);
        g.setColor(mezcla(C_RES, Color.WHITE, 0.6));
        g.drawRoundRect(nx, ny, nw, nh, 10, 10);
        g.setColor(C_RES);
        g.fillOval(nx + 6, ny + 9, 16, 16);
        g.setColor(Color.WHITE);
        g.setFont(Tema.ui(Font.BOLD, 10));
        g.drawString("5", nx + 11, ny + 21);
        g.drawString("Resumen", nx + 44, ny + 15);
        g.setFont(Tema.ui(Font.PLAIN, 8));
        g.drawString("vs. NTC 919", nx + 44, ny + 27);

        // seccion
        int x = 140, y = 6, sw = w - x - 8;
        g.setColor(Tema.SUPERFICIE);
        g.fillRect(x, y, sw, h - 26 - 10);
        g.setColor(C_RES);
        g.fillRect(x, y, 3, h - 26 - 10);
        g.setFont(Tema.ui(Font.BOLD, 10));
        g.setColor(mezcla(C_RES, Color.WHITE, 0.3));
        g.drawString("USO RECOMENDADO", x + 12, y + 16);
        g.setFont(Tema.ui(Font.BOLD, 11));
        g.setColor(Tema.OK);
        g.drawString("Pisos interiores de tránsito residencial", x + 12, y + 32);
        g.setFont(Tema.ui(Font.PLAIN, 9));
        g.setColor(Tema.TEXTO_2);
        g.drawString("Absorción alta (grupo BIII) con flexión conforme: ideal en salas, comedores, cocinas y corredores.", x + 12, y + 45);

        Rectangle img = new Rectangle(x + 12, y + 52, 250, 150);
        habitacion(g, img, s);
        g.setFont(Tema.ui(Font.BOLD, 9));
        g.setColor(Color.WHITE);
        g.drawString("Interior · pisos residenciales", img.x + 8, img.y + img.height - 8);

        boolean clic = s >= T_CLIC && s < T_CLIC + 0.015;
        Rectangle btn = new Rectangle(img.x + img.width + 20, img.y + 100, 176, 26);
        g.setColor(clic ? mezcla(C_RES, Color.WHITE, 0.75) : C_RES);
        g.fillRect(btn.x, btn.y + (clic ? 1 : 0), btn.width, btn.height);
        g.setFont(Tema.ui(Font.BOLD, 10));
        g.setColor(new Color(0x10, 0x12, 0x20));
        g.drawString("Descargar informe final (PDF)", btn.x + 12, btn.y + 17 + (clic ? 1 : 0));

        double a = tramo(s, T_GLOBO, T_GLOBO + 0.03) * (1 - tramo(s, T_CLIC - 0.02, T_CLIC));
        if (a > 0) {
            resaltar(g, btn, Tema.AVISO, s);
            globo(g, btn.x - 6, btn.y - 52, btn.x + btn.width / 2, btn.y - 4,
                  "Descarga el informe", "completo del estudio en PDF", Tema.AVISO, a);
        }

        // descarga
        if (s >= T_BAJA0) {
            double k = tramo(s, T_BAJA0, T_BAJA1);
            Rectangle t = new Rectangle(btn.x - 10, btn.y + 36, 196, 34);
            g.setColor(Tema.SUPERFICIE_2);
            g.fillRoundRect(t.x, t.y, t.width, t.height, 8, 8);
            g.setColor(Tema.BORDE);
            g.drawRoundRect(t.x, t.y, t.width, t.height, 8, 8);
            g.setColor(Tema.ERROR);
            g.fillRoundRect(t.x + 8, t.y + 7, 18, 20, 3, 3);
            g.setFont(Tema.ui(Font.BOLD, 6));
            g.setColor(Color.WHITE);
            g.drawString("PDF", t.x + 10, t.y + 21);
            g.setFont(Tema.ui(Font.PLAIN, 8));
            g.setColor(Tema.TEXTO);
            g.drawString("Informe_final_M1_Relave_alto.pdf", t.x + 32, t.y + 14);
            g.setColor(Tema.BORDE);
            g.fillRect(t.x + 32, t.y + 20, 150, 4);
            g.setColor(k >= 1 ? Tema.OK : Tema.ACENTO);
            g.fillRect(t.x + 32, t.y + 20, (int) (150 * k), 4);
            g.setFont(Tema.ui(Font.PLAIN, 7));
            g.setColor(k >= 1 ? Tema.OK : Tema.TEXTO_3);
            g.drawString(k >= 1 ? "✓ Guardado · 4 páginas" : "Generando…", t.x + 32, t.y + 32);
        }

        if (s < T_CLIC + 0.02) {
            double[][] par = {{0.02, x + 200, y + 200}, {T_CLIC, btn.x + 110, btn.y + 15}};
            double[] p = trayecto(s, par);
            cursor(g, p[0], p[1], clic);
        }
    }

    private static void habitacion(Graphics2D g, Rectangle r, double s) {
        Shape clip = g.getClip();
        g.clipRect(r.x, r.y, r.width, r.height);
        int horiz = r.y + (int) (r.height * 0.42);
        g.setPaint(new GradientPaint(r.x, r.y, new Color(0xDC, 0xD2, 0xC4), r.x, horiz, new Color(0xC4, 0xB8, 0xA6)));
        g.fillRect(r.x, r.y, r.width, horiz - r.y);
        g.setColor(new Color(0xF0, 0xF2, 0xF4));
        g.fillRect(r.x + 24, r.y + 14, 60, 36);
        g.setColor(new Color(0xB0, 0xB6, 0xBC));
        g.drawRect(r.x + 24, r.y + 14, 60, 36);
        g.drawLine(r.x + 54, r.y + 14, r.x + 54, r.y + 50);
        g.setColor(new Color(0x5A, 0x30, 0x1E));
        g.fillRect(r.x + r.width - 70, r.y + 18, 28, horiz - r.y - 18);
        g.setColor(new Color(0xE8, 0xE0, 0xD0));
        g.drawRect(r.x + r.width - 70, r.y + 18, 28, horiz - r.y - 18);
        // piso en perspectiva
        g.setPaint(new GradientPaint(r.x, horiz, new Color(0x9A, 0x50, 0x30), r.x, r.y + r.height, new Color(0x6A, 0x30, 0x1A)));
        g.fillRect(r.x, horiz, r.width, r.y + r.height - horiz);
        g.setPaint(null);
        double vx = r.x + r.width / 2.0, vy = horiz - 60;
        g.setColor(new Color(0x4A, 0x22, 0x10, 150));
        for (int i = -10; i <= 10; i++) {
            double xb = vx + i * 34;
            double t0 = (horiz - vy) / (r.y + r.height - vy);
            g.drawLine((int) (vx + (xb - vx) * t0), horiz, (int) xb, r.y + r.height);
        }
        for (int j = 1; j < 9; j++) {
            double t = Math.pow(j / 9.0, 1.7);
            int yy = (int) (horiz + (r.y + r.height - horiz) * t);
            g.drawLine(r.x, yy, r.x + r.width, yy);
        }
        // brillo que recorre el piso
        double bx = r.x + ((s * 3) % 1.0) * (r.width + 80) - 40;
        g.setPaint(new GradientPaint((float) bx - 30, 0, new Color(255, 255, 255, 0), (float) bx, 0,
                new Color(255, 255, 255, 40), true));
        g.fillRect(r.x, horiz, r.width, r.y + r.height - horiz);
        g.setPaint(null);
        g.setPaint(new GradientPaint(r.x, r.y + r.height - 30, new Color(0, 0, 0, 0), r.x, r.y + r.height, new Color(0, 0, 0, 160)));
        g.fillRect(r.x, r.y + r.height - 30, r.width, 30);
        g.setPaint(null);
        g.setClip(clip);
        g.setColor(Tema.BORDE);
        g.drawRoundRect(r.x, r.y, r.width, r.height, 6, 6);
    }

    // ------------------------------------------------------------ visor del PDF
    private void visor(Graphics2D g, double s, int w, int h) {
        g.setColor(new Color(0x2B, 0x2F, 0x36));
        g.fillRect(0, 0, w, h - 26);
        g.setColor(new Color(0x1E, 0x22, 0x28));
        g.fillRect(0, 0, w, 16);
        g.setFont(Tema.ui(Font.PLAIN, 9));
        g.setColor(new Color(0xD0, 0xD6, 0xDE));
        g.drawString("Informe_final_M1_Relave_alto.pdf", 8, 12);

        double k = (s - T_PDF) / (0.92 - T_PDF);   // 0..1 para hojear las 4 paginas
        int ph = h - 26 - 26, pw = (int) (ph * 595 / 842.0);
        int px = 40, py = 20;

        if (s < 0.92) {
            double pos = clamp01(k) * 4;
            int pag = Math.min(3, (int) pos);
            double f = pos - pag;
            double desl = f > 0.82 && pag < 3 ? tramo(f, 0.82, 1.0) : 0;
            // pagina actual deslizandose a la izquierda y la siguiente entrando
            pagina(g, pag, px - (int) (desl * (pw + 30)), py, pw, ph);
            if (desl > 0) pagina(g, pag + 1, px + pw + 30 - (int) (desl * (pw + 30)), py, pw, ph);
            g.setColor(new Color(0x2B, 0x2F, 0x36));
            g.fillRect(px + pw + 8, py - 4, 4, ph + 8);
            indice(g, px + pw + 26, py + 10, w - (px + pw + 26) - 10, pag);
        } else {
            // las 4 paginas juntas
            double a = tramo(s, 0.92, 0.95);
            int tw = (int) (pw * 0.78), th = (int) (ph * 0.78);
            int gap = 14, tot = 4 * tw + 3 * gap, x0 = (w - tot) / 2;
            for (int i = 0; i < 4; i++) {
                int xx = x0 + i * (tw + gap);
                int yy = py + 6 + (int) ((1 - a) * 20);
                pagina(g, i, xx, yy, tw, th);
            }
            g.setFont(Tema.ui(Font.BOLD, 10));
            g.setColor(new Color(0xD0, 0xD6, 0xDE));
            String t = "4 páginas · listo para imprimir o compartir";
            g.drawString(t, (w - g.getFontMetrics().stringWidth(t)) / 2, py + th + 22);
        }
    }

    private static void indice(Graphics2D g, int x, int y, int w, int act) {
        g.setFont(Tema.ui(Font.BOLD, 11));
        g.setColor(Color.WHITE);
        g.drawString("Informe final de laboratorio", x, y + 6);
        g.setFont(Tema.ui(Font.PLAIN, 9));
        g.setColor(new Color(0xA8, 0xB2, 0xBE));
        g.drawString("Baldosín a partir del suelo · M1 — Relave alto · NTC 919 / ISO 13006", x, y + 20);
        for (int i = 0; i < 4; i++) {
            int yy = y + 44 + i * 26;
            boolean a = i == act;
            if (a) {
                g.setColor(alfa(Tema.ACENTO, 70));
                g.fillRoundRect(x - 6, yy - 14, w, 22, 8, 8);
            }
            g.setFont(Tema.ui(a ? Font.BOLD : Font.PLAIN, 11));
            g.setColor(a ? Color.WHITE : new Color(0x8A, 0x94, 0xA0));
            g.drawString(PAGINAS[i], x, yy);
        }
        g.setFont(Tema.ui(Font.PLAIN, 9));
        g.setColor(new Color(0x8A, 0x94, 0xA0));
        g.drawString("Página " + (act + 1) + " de 4", x, y + 160);
    }

    // ------------------------------------------------------------ paginas del informe (miniatura)
    private static void dibujarPagina(Graphics2D g0, int n, int x, int y, int w, int h) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setColor(Color.WHITE);
        g.fillRect(x, y, w, h);
        g.clipRect(x, y, w, h);
        double e = w / 595.0;   // escala de puntos PDF a pixeles
        // cabecera
        g.setColor(AZUL);
        g.fillRect(x, y, w, Math.max(2, (int) (4 * e)));
        g.setFont(Tema.ui(Font.BOLD, Math.max(3, (int) (13 * e))));
        g.setColor(TINTA);
        g.drawString("SDIM · Informe final", x + (int) (40 * e), y + (int) (24 * e));
        g.setColor(LINEA);
        g.drawLine(x + (int) (40 * e), y + (int) (32 * e), x + w - (int) (40 * e), y + (int) (32 * e));
        g.setFont(Tema.ui(Font.PLAIN, Math.max(3, (int) (10 * e))));
        g.setColor(GRIS);
        g.drawString("Página " + (n + 1) + " de 4", x + w - (int) (90 * e), y + h - (int) (14 * e));

        int mx = x + (int) (40 * e), cw = w - (int) (80 * e);
        switch (n) {
            case 0: pag1(g, mx, y, cw, e); break;
            case 1: pag2(g, mx, y, cw, e); break;
            case 2: pag3(g, mx, y, cw, e); break;
            default: pag4(g, mx, y, cw, e);
        }
        g.dispose();
    }

    private static int py(int y, double e, double pt) { return y + (int) (pt * e); }

    private static void titulo(Graphics2D g, String t, int x, int y, double e, float pt) {
        g.setFont(Tema.ui(Font.BOLD, Math.max(3, (int) (pt * e))));
        g.setColor(TINTA);
        g.drawString(t, x, y);
    }

    private static void filas(Graphics2D g, int x, int y, int w, double e, int n, double alto, boolean cabecera) {
        int rh = Math.max(2, (int) (alto * e));
        for (int i = 0; i < n; i++) {
            int yy = y + i * rh;
            if (cabecera && i == 0) { g.setColor(TINTA); g.fillRect(x, yy, w, rh); continue; }
            g.setColor(i % 2 == 0 ? new Color(0xF2, 0xF4, 0xF7) : Color.WHITE);
            g.fillRect(x, yy, w, rh);
            g.setColor(new Color(0xB8, 0xC0, 0xCA));
            g.fillRect(x + 3, yy + rh / 2 - 1, w / 4, Math.max(1, rh / 4));
            g.fillRect(x + w / 2, yy + rh / 2 - 1, w / 5, Math.max(1, rh / 4));
        }
    }

    private static void pag1(Graphics2D g, int x, int y, int w, double e) {
        titulo(g, "Informe final de laboratorio", x, py(y, e, 72), e, 22);
        g.setFont(Tema.ui(Font.PLAIN, Math.max(3, (int) (10 * e))));
        g.setColor(GRIS);
        g.drawString("Baldosín a partir del suelo · M1 — Relave alto", x, py(y, e, 90));
        titulo(g, "1. Procedencia del suelo", x, py(y, e, 122), e, 13);
        filas(g, x, py(y, e, 132), (int) (w * 0.55), e, 6, 15, false);
        // mapa de Colombia simplificado con el punto en Leticia
        int mx = x + (int) (w * 0.62), my = py(y, e, 120);
        int mw = (int) (w * 0.36), mh = (int) (150 * e);
        Path2D.Double co = new Path2D.Double();
        double[][] pts = {{0.40, 0.00}, {0.62, 0.10}, {0.72, 0.30}, {0.95, 0.38}, {0.88, 0.62}, {0.70, 0.72},
                          {0.72, 0.98}, {0.60, 0.95}, {0.45, 0.70}, {0.20, 0.62}, {0.05, 0.45}, {0.18, 0.20}};
        for (int i = 0; i < pts.length; i++) {
            double X = mx + pts[i][0] * mw, Y = my + pts[i][1] * mh;
            if (i == 0) co.moveTo(X, Y); else co.lineTo(X, Y);
        }
        co.closePath();
        g.setColor(new Color(0xE4, 0xE8, 0xEE));
        g.fill(co);
        g.setColor(new Color(0xA0, 0xAA, 0xB6));
        g.draw(co);
        g.setColor(new Color(0xE0, 0x50, 0x50));
        int lx = (int) (mx + 0.68 * mw), ly = (int) (my + 0.93 * mh);
        g.fillOval(lx - 3, ly - 3, 6, 6);

        titulo(g, "2. Clasificación del suelo (SUCS · ASTM D2487)", x, py(y, e, 300), e, 13);
        g.setColor(new Color(0x2E, 0x8B, 0x57));
        g.fillRect(x, py(y, e, 312), (int) (110 * e), (int) (50 * e));
        g.setFont(Tema.ui(Font.BOLD, Math.max(3, (int) (26 * e))));
        g.setColor(Color.WHITE);
        g.drawString("CL", x + (int) (36 * e), py(y, e, 346));
        g.setFont(Tema.ui(Font.BOLD, Math.max(3, (int) (13 * e))));
        g.setColor(TINTA);
        g.drawString("Arcilla de baja plasticidad", x + (int) (124 * e), py(y, e, 334));
        int cw3 = w / 3;
        for (int c = 0; c < 3; c++) filas(g, x + c * cw3, py(y, e, 380), cw3 - (int) (8 * e), e, 3, 14, false);
    }

    private static void pag2(Graphics2D g, int x, int y, int w, double e) {
        titulo(g, "3. Baldosín", x, py(y, e, 66), e, 15);
        filas(g, x, py(y, e, 76), w / 2 - 4, e, 7, 13, false);
        filas(g, x + w / 2 + 4, py(y, e, 76), w / 2 - 4, e, 4, 13, false);
        titulo(g, "Dosificación de las mezclas (por pieza)", x, py(y, e, 190), e, 13);
        filas(g, x, py(y, e, 198), w, e, 4, 16, true);
        titulo(g, "Secciones acotadas", x, py(y, e, 290), e, 13);
        int ry = py(y, e, 310), rh = (int) (200 * e), rw = (int) (100 * e);
        g.setColor(LADRILLO);
        g.fillRect(x + (int) (w * 0.12), ry, rw, rh);
        g.fillRect(x + (int) (w * 0.62), ry, rw, rh);
        g.setColor(LADRILLO.darker());
        for (int i = 0; i < 13; i++) g.fillRect(x + (int) (w * 0.62) + i * rw / 13, ry, Math.max(1, rw / 30), rh);
        int cy = py(y, e, 560);
        g.setColor(LADRILLO);
        g.fillRect(x + (int) (w * 0.06), cy, (int) (w * 0.42), (int) (22 * e));
        for (int i = 0; i < 14; i++) g.fillRect(x + (int) (w * 0.06) + i * (int) (w * 0.42) / 14, cy - (int) (8 * e), (int) (8 * e), (int) (8 * e));
        g.fillRect(x + (int) (w * 0.70), cy - (int) (10 * e), (int) (60 * e), (int) (34 * e));
    }

    private static void curvaMini(Graphics2D g, int x, int y, int w, int h, boolean coccion) {
        g.setColor(LINEA);
        for (int i = 0; i <= 4; i++) g.drawLine(x, y + h * i / 4, x + w, y + h * i / 4);
        g.setColor(GRIS);
        g.drawLine(x, y, x, y + h);
        g.drawLine(x, y + h, x + w, y + h);
        g.setColor(AZUL);
        Path2D.Double p = new Path2D.Double();
        for (int i = 0; i <= 60; i++) {
            double t = i / 60.0, v;
            if (coccion) v = t < 0.55 ? t / 0.55 * 0.85 : (t < 0.62 ? 0.85 : 0.85 * Math.max(0, 1 - (t - 0.62) / 0.38));
            else v = t < 0.72 ? 0.85 * Math.exp(-t * 1.6) + 0.2 : 0.2 * Math.max(0, 1 - (t - 0.72) / 0.15);
            double X = x + w * t, Y = y + h * (1 - v);
            if (i == 0) p.moveTo(X, Y); else p.lineTo(X, Y);
        }
        g.draw(p);
        if (!coccion) {
            g.setColor(new Color(0x2A, 0x5C, 0xB0, 40));
            g.fillRect(x + (int) (w * 0.72), y, (int) (w * 0.28), h);
        }
    }

    private static void pag3(Graphics2D g, int x, int y, int w, double e) {
        titulo(g, "4. Curva de secado", x, py(y, e, 66), e, 15);
        curvaMini(g, x + (int) (20 * e), py(y, e, 80), w - (int) (30 * e), (int) (150 * e), false);
        titulo(g, "5. Curva de cocción", x, py(y, e, 300), e, 15);
        curvaMini(g, x + (int) (20 * e), py(y, e, 314), w - (int) (30 * e), (int) (190 * e), true);
        filas(g, x, py(y, e, 560), w, e, 7, 16, true);
    }

    private static void pag4(Graphics2D g, int x, int y, int w, double e) {
        titulo(g, "6. Resumen de ensayos · M1 — Relave alto", x, py(y, e, 66), e, 15);
        filas(g, x, py(y, e, 76), w, e, 5, 15, true);
        titulo(g, "Flexión en 3 puntos por probeta", x, py(y, e, 168), e, 11);
        filas(g, x, py(y, e, 174), w, e, 2, 15, true);
        g.setColor(new Color(0xDD, 0xF3, 0xE6));
        g.fillRect(x, py(y, e, 214), w, (int) (34 * e));
        g.setFont(Tema.ui(Font.BOLD, Math.max(3, (int) (10 * e))));
        g.setColor(new Color(0x1E, 0x7A, 0x46));
        g.drawString("Conclusión: la mezcla M1 CUMPLE la NTC 919", x + (int) (8 * e), py(y, e, 234));
        titulo(g, "7. Uso recomendado", x, py(y, e, 282), e, 15);
        g.setFont(Tema.ui(Font.BOLD, Math.max(3, (int) (12 * e))));
        g.setColor(AZUL);
        g.drawString("Pisos interiores de tránsito residencial", x, py(y, e, 304));
        habitacion(g, new Rectangle(x, py(y, e, 330), (int) (w * 0.5), (int) (150 * e)), 0.3);
    }
}
