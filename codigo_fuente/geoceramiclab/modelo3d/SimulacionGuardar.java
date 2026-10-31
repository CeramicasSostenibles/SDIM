package geoceramiclab.modelo3d;

import geoceramiclab.Tema;

import java.awt.*;

import static geoceramiclab.modelo3d.AnimUtil.*;

/**
 * Paso 8 de la ayuda: Guardar. Se señala el botón «Guardar» (disquete) de la
 * barra superior, se pulsa, se escribe el nombre del archivo .gcl y aparece
 * el aviso de estudio guardado.
 */
final class SimulacionGuardar {

    static final long DUR = 9000;

    private static final double T_GLOBO = 0.05, T_CLIC = 0.26, T_DLG = 0.29, T_NOM0 = 0.33, T_NOM1 = 0.47;
    private static final double T_OK = 0.53, T_TOAST = 0.56;
    private static final String NOMBRE = "estudio_leticia";

    void pintar(Graphics2D g, long ms, int w, int h) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        double s = (ms % DUR) / (double) DUR;

        // ---- barra superior del programa
        int bh = 40;
        g.setColor(Tema.BARRA);
        g.fillRect(0, 0, w, bh);
        g.setColor(Tema.BORDE);
        g.drawLine(0, bh, w, bh);
        g.setFont(Tema.ui(Font.BOLD, 13));
        g.setColor(Tema.TEXTO);
        g.drawString("SDIM", 12, 25);
        g.setFont(Tema.ui(Font.PLAIN, 10));
        g.setColor(Tema.TEXTO_3);
        g.drawString("Suelo → Pieza → Mezcla → Secado → Ensayos → Informe", 56, 25);

        boolean clic = s >= T_CLIC && s < T_CLIC + 0.015;
        Rectangle btn = new Rectangle(w - 252, 9, 92, 22);
        g.setColor(clic ? Tema.SELECCION : Tema.CONTROL);
        g.fillRoundRect(btn.x, btn.y + (clic ? 1 : 0), btn.width, btn.height, 22, 22);
        g.setColor(Tema.BORDE);
        g.drawRoundRect(btn.x, btn.y + (clic ? 1 : 0), btn.width, btn.height, 22, 22);
        disquete(g, btn.x + 14, btn.y + 4 + (clic ? 1 : 0), 14, Tema.TEXTO);
        g.setFont(Tema.ui(Font.BOLD, 11));
        g.setColor(Tema.TEXTO);
        g.drawString("Guardar", btn.x + 36, btn.y + 15 + (clic ? 1 : 0));

        g.setFont(Tema.ui(Font.BOLD, 9));
        g.setColor(Tema.TEXTO_3);
        g.drawString("LUGAR DE ESTUDIO", w - 140, 18);
        g.setFont(Tema.ui(Font.BOLD, 11));
        g.setColor(Tema.TEXTO);
        g.drawString("Leticia, Amazonas", w - 140, 32);

        // ---- contenido del programa (atenuado)
        g.setColor(Tema.FONDO);
        g.fillRect(0, bh + 1, w, h - bh - 27);
        g.setColor(Tema.SUPERFICIE);
        for (int i = 0; i < 3; i++) g.fillRoundRect(12 + i * 226, bh + 14, 214, 70, 8, 8);
        g.setColor(Tema.SUPERFICIE_2);
        g.fillRoundRect(12, bh + 96, w - 24, 100, 8, 8);
        g.setFont(Tema.ui(Font.PLAIN, 10));
        g.setColor(Tema.TEXTO_3);
        String[] tarj = {"Clasificación · CL", "Mezclas · M1, M2, M3", "Ensayos · M1 cumple NTC 919"};
        for (int i = 0; i < 3; i++) g.drawString(tarj[i], 24 + i * 226, bh + 34);
        g.drawString("Informe final listo", 24, bh + 116);

        // ---- globo sobre el boton
        double a = tramo(s, T_GLOBO, T_GLOBO + 0.03) * (1 - tramo(s, T_CLIC - 0.02, T_CLIC));
        if (a > 0) {
            resaltar(g, btn, Tema.AVISO, s);
            globo(g, btn.x - 150, btn.y + 40, btn.x + btn.width / 2, btn.y + btn.height + 4,
                  "Guarda tu estudio con el disquete", "o con Ctrl + S · archivo .gcl", Tema.AVISO, a);
        }

        // ---- dialogo Guardar estudio
        double ad = tramo(s, T_DLG, T_DLG + 0.03) * (1 - tramo(s, T_OK + 0.01, T_OK + 0.03));
        Rectangle bOk = null;
        if (ad > 0) {
            Composite c = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) ad));
            g.setColor(alfa(Color.BLACK, 90));
            g.fillRect(0, 0, w, h - 26);
            Rectangle d = new Rectangle(w / 2 - 180, 62, 360, 132);
            g.setColor(Tema.SUPERFICIE);
            g.fillRoundRect(d.x, d.y, d.width, d.height, 10, 10);
            g.setColor(Tema.BORDE);
            g.drawRoundRect(d.x, d.y, d.width, d.height, 10, 10);
            g.setFont(Tema.ui(Font.BOLD, 11));
            g.setColor(Tema.TEXTO);
            g.drawString("Guardar estudio", d.x + 14, d.y + 20);
            g.setFont(Tema.ui(Font.PLAIN, 9));
            g.setColor(Tema.TEXTO_3);
            g.drawString("Carpeta:  Documentos\\SDIM", d.x + 14, d.y + 40);
            String nom = escribiendo(NOMBRE, s, T_NOM0, T_NOM1);
            boolean car = s >= T_NOM0 - 0.02 && s < T_OK && ((long) (s * DUR / 280)) % 2 == 0;
            campo(g, d.x + 14, d.y + 50, "Nombre", nom + (s >= T_NOM1 ? ".gcl" : ""), 44, 240, s < T_OK, car, Tema.TEXTO);
            g.setFont(Tema.ui(Font.PLAIN, 9));
            g.setColor(Tema.TEXTO_3);
            g.drawString("Tipo:  SDIM (*.gcl)", d.x + 14, d.y + 92);
            bOk = boton(g, d.x + d.width - 90, d.y + d.height - 30, "Guardar", Tema.ACENTO, s >= T_OK && s < T_OK + 0.015);
            boton(g, bOk.x - 84, bOk.y, "Cancelar", Tema.CONTROL, false);
            g.setComposite(c);
        }

        // ---- aviso guardado
        if (s >= T_TOAST) {
            double k = tramo(s, T_TOAST, T_TOAST + 0.05) * (1 - tramo(s, 0.96, 1.0));
            int tw = 300, th = 46;
            int tx = (int) (w - 12 - tw * k), ty = h - 26 - th - 12;
            g.setColor(alfa(Tema.OK, 240));
            g.fillRoundRect(tx, ty, tw, th, 10, 10);
            g.setColor(new Color(10, 30, 20));
            g.setFont(Tema.ui(Font.BOLD, 12));
            g.drawString("✓ Estudio guardado", tx + 14, ty + 19);
            g.setFont(Tema.ui(Font.PLAIN, 9));
            g.drawString(NOMBRE + ".gcl  ·  Documentos\\SDIM", tx + 14, ty + 35);
        }

        // ---- cursor
        if (s < T_OK + 0.03) {
            double ox = bOk != null ? bOk.x + 40 : w / 2 + 120, oy = bOk != null ? bOk.y + 11 : 170;
            double[][] par = {{0.02, w / 2.0, 160}, {T_CLIC, btn.x + 50, btn.y + 13},
                              {T_DLG + 0.02, btn.x + 50, btn.y + 13}, {T_NOM1 + 0.02, w / 2.0 + 60, 125},
                              {T_OK, ox, oy}};
            double[] p = trayecto(s, par);
            cursor(g, p[0], p[1], clic || (s >= T_OK && s < T_OK + 0.015));
        }

        String ley;
        if (s < T_CLIC)      ley = "Último paso: guarda el estudio con el botón «Guardar» (disquete) o Ctrl + S";
        else if (s < T_OK)   ley = "Escribe el nombre del archivo; se guarda como .gcl";
        else                 ley = "Al abrir el .gcl se restauran el lugar, la clasificación, las mezclas y los ensayos";
        leyenda(g, 8, h - 23, w - 16, ley);

        if (s < 0.015) {
            g.setColor(alfa(Tema.SUPERFICIE, (int) (255 * (1 - s / 0.015))));
            g.fillRect(0, 0, w, h);
        }
    }

    static void disquete(Graphics2D g, int x, int y, int t, Color c) {
        g.setColor(c);
        g.setStroke(new BasicStroke(1.4f));
        g.drawRoundRect(x, y, t, t, 3, 3);
        g.fillRect(x + 3, y, t - 6, t / 3);
        g.drawRect(x + 3, y + t / 2 + 1, t - 6, t / 2 - 2);
        g.setStroke(new BasicStroke(1f));
    }
}
