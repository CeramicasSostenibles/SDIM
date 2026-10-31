package geoceramiclab.modelo3d;

import geoceramiclab.model.BaldosinCatalan;
import geoceramiclab.model.BaldosinCatalan.MezclaBaldosin;
import geoceramiclab.model.ClasificadorSUCS;
import geoceramiclab.model.MunicipiosCO;
import geoceramiclab.model.SueloData;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public final class InformeFinal {

    private InformeFinal() { }

    private static final float PW = 595.28f, PH = 841.89f;
    private static final float ESC = 2.6f;
    private static final float MX = 42;

    private static final Color TINTA   = new Color(30, 32, 38);
    private static final Color SUAVE   = new Color(108, 112, 122);
    private static final Color LINEA   = new Color(214, 216, 222);
    private static final Color FONDO_T = new Color(245, 247, 250);
    private static final Color TERRA   = new Color(39, 91, 166);
    private static final Color TERRA_C = new Color(62, 124, 196);
    private static final Color ARCILLA   = new Color(205, 118, 78);
    private static final Color ARCILLA_B = new Color(176, 84, 52);
    private static final Color AZUL    = new Color(30, 96, 200);
    private static final Color VERDE   = new Color(28, 128, 72);
    private static final Color ROJO    = new Color(198, 40, 40);
    private static final Color AMBAR   = new Color(196, 128, 20);

    public static final class Contenido {
        public SueloData suelo;
        public BaldosinCatalan baldosin;
        public MezclaBaldosin mezcla;
        public String nombreMezcla = "Mezcla 1";

        public int nProbetas;
        public double[] abs, cont, dens, poros;
        public String[] grupo;
        public List<DetalleFlexion.ResultadoFlexion> flexion = new ArrayList<>();
        public double sProm, sMin, rProm, rMin;
        public boolean flexCumple;
        public int flexIndivOk;
        public String textoNorma = "";

        public String usoTitulo = "", usoDetalle = "";
        public String[] usoImagenes = new String[0], usoPies = new String[0];

        public List<double[]> curvaSecado, curvaCoccion;

        public List<MezclaBaldosin> mezclasDiseno = new ArrayList<>();
        public List<String> nombresDiseno = new ArrayList<>();
        public boolean extruido;

        public boolean flexOkTotal() {
            return flexCumple && flexion != null && !flexion.isEmpty() && flexIndivOk == flexion.size();
        }
    }

    public static final class Comparacion {
        public final List<Contenido> mezclas;

        public int mejorAbs = -1, mejorCont = -1, mejorFlex = -1;

        public int recomendada = -1;

        public int[] puntaje;
        public boolean algunaCumple;

        Comparacion(List<Contenido> l) { mezclas = l; }
    }

    private static double val(double[] a) { return (a == null) ? Double.NaN : a[0]; }

    private static int[] posiciones(double[] v, boolean menorEsMejor) {
        int n = v.length;
        int[] pos = new int[n];
        for (int i = 0; i < n; i++) {
            int p = 1;
            for (int j = 0; j < n; j++) {
                if (j == i) continue;
                double a = v[i], b = v[j];
                if (Double.isNaN(a) && !Double.isNaN(b)) { p++; continue; }
                if (Double.isNaN(b)) continue;
                if (menorEsMejor ? b < a : b > a) p++;
            }
            pos[i] = p;
        }
        return pos;
    }

    private static int mejor(double[] v, boolean menorEsMejor) {
        int k = -1;
        for (int i = 0; i < v.length; i++) {
            if (Double.isNaN(v[i])) continue;
            if (k < 0 || (menorEsMejor ? v[i] < v[k] : v[i] > v[k])) k = i;
        }
        return k;
    }

    public static Comparacion comparar(List<Contenido> l) {
        Comparacion k = new Comparacion(l);
        int n = l.size();
        double[] ev = new double[n], ct = new double[n], rr = new double[n];
        for (int i = 0; i < n; i++) {
            Contenido c = l.get(i);
            ev[i] = val(c.abs);
            ct[i] = val(c.cont);
            rr[i] = (c.flexion != null && !c.flexion.isEmpty()) ? c.rProm : Double.NaN;
        }
        k.mejorAbs  = mejor(ev, true);
        k.mejorCont = mejor(ct, true);
        k.mejorFlex = mejor(rr, false);
        int[] pe = posiciones(ev, true), pc = posiciones(ct, true), pr = posiciones(rr, false);
        k.puntaje = new int[n];
        for (int i = 0; i < n; i++) k.puntaje[i] = pe[i] + pc[i] + pr[i];
        for (Contenido c : l) if (c.flexOkTotal()) k.algunaCumple = true;
        for (int i = 0; i < n; i++) {
            if (k.algunaCumple && !l.get(i).flexOkTotal()) continue;
            int b = k.recomendada;
            if (b < 0 || k.puntaje[i] < k.puntaje[b]
                    || (k.puntaje[i] == k.puntaje[b] && !Double.isNaN(rr[i])
                        && (Double.isNaN(rr[b]) || rr[i] > rr[b]))) k.recomendada = i;
        }
        return k;
    }

    public static void generar(File destino, Contenido c) throws IOException {
        List<Contenido> l = new ArrayList<>();
        l.add(c);
        generar(destino, l);
    }

    public static void generar(File destino, List<Contenido> lista) throws IOException {
        if (lista == null || lista.isEmpty()) throw new IOException("No hay mezclas ensayadas.");
        final Comparacion cmp = comparar(lista);
        final Contenido base = lista.get(cmp.recomendada >= 0 ? cmp.recomendada : 0);
        final boolean varias = lista.size() > 1;

        List<BufferedImage> paginas = new ArrayList<>();
        int total = 3 + lista.size() + (varias ? 1 : 0);
        int num = 1;
        paginas.add(pagina(num++, total, "Procedencia y clasificación del suelo", g -> pagina1(g, base)));
        paginas.add(pagina(num++, total, "Baldosín: dosificación y secciones", g -> pagina2(g, base)));
        paginas.add(pagina(num++, total, "Secado y cocción", g -> pagina3(g, base)));
        for (int i = 0; i < lista.size(); i++) {
            final Contenido c = lista.get(i);
            final String sec = varias ? "6." + (i + 1) : "6.";
            paginas.add(pagina(num++, total, "Resumen de ensayos y uso · " + c.nombreMezcla,
                               g -> pagina4(g, c, sec, varias)));
        }
        if (varias)
            paginas.add(pagina(num++, total, "Comparación de mezclas", g -> pagina5(g, cmp)));
        escribirPdf(destino, paginas);
    }

    private interface Dibujo { void en(Graphics2D g); }

    private static BufferedImage pagina(int num, int total, String seccion, Dibujo d) {
        BufferedImage im = new BufferedImage(Math.round(PW * ESC), Math.round(PH * ESC), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = im.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, im.getWidth(), im.getHeight());
        g.scale(ESC, ESC);

        g.setColor(TERRA);
        g.fill(new Rectangle2D.Float(0, 0, PW, 5));

        float xTxt = MX;
        try {
            java.awt.Image ico = geoceramiclab.Marca.icono();
            if (ico != null) { g.drawImage(ico, Math.round(MX), 11, 16, 16, null); xTxt = MX + 21; }
        } catch (Throwable ignorar) { }
        texto(g, "SDIM · Informe final", xTxt, 24, 8.5f, true, TERRA);
        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy",
                                                                          Locale.forLanguageTag("es-CO")));
        textoDer(g, fecha, PW - MX, 24, 8.5f, false, SUAVE);
        g.setColor(LINEA);
        g.setStroke(new BasicStroke(0.6f));
        g.draw(new Line2D.Float(MX, 31, PW - MX, 31));

        d.en(g);

        g.setColor(LINEA);
        g.draw(new Line2D.Float(MX, PH - 34, PW - MX, PH - 34));
        texto(g, seccion, MX, PH - 21, 7.5f, false, SUAVE);
        textoDer(g, "Página " + num + " de " + total, PW - MX, PH - 21, 7.5f, false, SUAVE);
        g.dispose();
        return im;
    }

    private static void pagina1(Graphics2D g, Contenido c) {
        float y = 66;
        texto(g, "Informe final de laboratorio", MX, y, 22, true, TINTA);
        y += 18;
        texto(g, "Baldosín a partir del suelo · " + c.nombreMezcla
                 + " · NTC 919 / ISO 13006", MX, y, 10, false, SUAVE);
        y += 30;

        y = titulo(g, "1. Procedencia del suelo", y);
        MunicipiosCO.Municipio m = MunicipiosCO.actual();
        float mapaW = 170, mapaH = 200;
        float xMapa = PW - MX - mapaW;
        String[][] filas;
        if (m != null) {
            filas = new String[][]{
                {"Municipio", m.nombre},
                {"Departamento", m.departamento},
                {"Código DANE", m.codigoDane},
                {"Coordenadas", m.coordenadas()},
                {"Latitud / longitud", String.format(Locale.US, "%.4f° / %.4f°", m.lat, m.lon)},
            };
        } else {
            filas = new String[][]{{"Ubicación", "No se registró el lugar de estudio"}};
        }
        float yTabla = tablaClave(g, filas, MX, y, xMapa - MX - 20);
        mapa(g, m, xMapa, y - 6, mapaW, mapaH);
        y = Math.max(yTabla, y + mapaH) + 16;

        y = titulo(g, "2. Clasificación del suelo (SUCS · ASTM D2487)", y);
        SueloData d = c.suelo;
        ClasificadorSUCS.Resultado r = d != null ? ClasificadorSUCS.clasificar(d) : null;

        float bw = 118, bh = 74;
        g.setColor(TERRA);
        g.fill(new RoundRectangle2D.Float(MX, y, bw, bh, 10, 10));
        String sim = r != null ? r.simbolo.trim() : "—";
        float tam = sim.length() > 6 ? 15 : (sim.length() > 3 ? 22 : 32);
        textoCentro(g, sim, MX + bw / 2, y + bh / 2 + tam * 0.35f, tam, true, Color.WHITE);

        float xn = MX + bw + 16, wn = PW - MX - xn;
        String nombre = r != null ? (r.esCompleta() ? r.nombre
                                    : (r.nombreGenerico != null ? r.nombreGenerico : r.nombre)) : "Sin datos";
        float yn = parrafo(g, nombre, xn, y + 16, wn, 14, true, TINTA);
        String nivel = r == null ? "" : (r.nivel == ClasificadorSUCS.Nivel.COMPLETA ? "Clasificación completa"
                      : r.nivel == ClasificadorSUCS.Nivel.PARCIAL ? "Clasificación parcial" : "No clasificable");
        yn = parrafo(g, nivel, xn, yn + 4, wn, 9.5f, false,
                     r != null && r.esCompleta() ? VERDE : AMBAR);
        if (r != null && r.texturalNombre != null)
            yn = parrafo(g, "Clase textural (hidrómetro): " + r.texturalNombre, xn, yn + 2, wn, 9.5f, false, SUAVE);
        y = Math.max(y + bh, yn) + 16;

        float colW = (PW - 2 * MX - 2 * 12) / 3f;
        float y0 = y;
        float y1 = tablaClaveTitulo(g, "Granulometría", d != null && d.granCalculada ? new String[][]{
                {"Grava", pct(d.pctGrava)}, {"Arena", pct(d.pctArena)}, {"Finos (#200)", pct(d.pctFinos)},
                {"Cu", d.Cu != null ? f2(d.Cu) : "—"}, {"Cc", d.Cc != null ? f2(d.Cc) : "—"}}
                : new String[][]{{"Estado", "No calculada"}}, MX, y0, colW);
        float y2 = tablaClaveTitulo(g, "Hidrómetro", d != null && d.hidroCalculada ? new String[][]{
                {"Arcilla", pct(d.pctArcilla)}, {"Limo", pct(d.pctLimo)}, {"Arena", pct(d.pctArenaFina)},
                {"Clase textural", d.claseTextural != null ? d.claseTextural : "—"}}
                : new String[][]{{"Estado", "No calculado"}}, MX + colW + 12, y0, colW);
        float y3 = tablaClaveTitulo(g, "Límites de Atterberg", d != null && d.atterCalculada ? new String[][]{
                {"Límite líquido LL", f1(d.LL)}, {"Límite plástico LP", f1(d.LP)}, {"Índice plástico IP", f1(d.IP)}}
                : new String[][]{{"Estado", "No calculados"}}, MX + 2 * (colW + 12), y0, colW);
        y = Math.max(y1, Math.max(y2, y3)) + 18;

        if (r != null && !r.pasos.isEmpty()) {
            texto(g, "Cómo se llegó al símbolo", MX, y, 10, true, TINTA);
            y += 14;
            int k = 0;
            for (String p : r.pasos) {
                if (y > PH - 70) break;
                y = parrafo(g, "•  " + p, MX + 4, y, PW - 2 * MX - 4, 8.8f, false, SUAVE) + 2;
                if (++k == 8) break;
            }
            for (String a : r.avisos) {
                if (y > PH - 60) break;
                y = parrafo(g, "⚠  " + a, MX + 4, y + 2, PW - 2 * MX - 4, 8.8f, false, AMBAR) + 2;
            }
        }
    }

    private static void mapa(Graphics2D g, MunicipiosCO.Municipio m, float x, float y, float w, float h) {
        List<double[][]> cs;
        List<String> nombres;
        try { cs = MunicipiosCO.contornos(); nombres = MunicipiosCO.nombresContorno(); }
        catch (Exception e) { return; }
        if (cs == null || cs.isEmpty()) return;

        double minLon = 1e9, maxLon = -1e9, minLat = 1e9, maxLat = -1e9;
        for (double[][] anillo : cs) {
            double lonMedio = 0;
            for (double[] p : anillo) lonMedio += p[0];
            lonMedio /= Math.max(1, anillo.length);
            if (lonMedio < -80) continue;
            for (double[] p : anillo) {
                minLon = Math.min(minLon, p[0]); maxLon = Math.max(maxLon, p[0]);
                minLat = Math.min(minLat, p[1]); maxLat = Math.max(maxLat, p[1]);
            }
        }
        double cosLat = Math.cos(Math.toRadians((minLat + maxLat) / 2));
        double esc = Math.min(w / ((maxLon - minLon) * cosLat), h / (maxLat - minLat));
        double ox = x + (w - (maxLon - minLon) * cosLat * esc) / 2;
        double oy = y + (h - (maxLat - minLat) * esc) / 2;

        String deptoNorm = m != null ? MunicipiosCO.normalizar(m.departamento) : "";
        for (int i = 0; i < cs.size(); i++) {
            double[][] anillo = cs.get(i);
            Path2D p = new Path2D.Double();
            boolean fuera = false;
            for (int k = 0; k < anillo.length; k++) {
                if (anillo[k][0] < -80) { fuera = true; break; }
                double px = ox + (anillo[k][0] - minLon) * cosLat * esc;
                double py = oy + (maxLat - anillo[k][1]) * esc;
                if (k == 0) p.moveTo(px, py); else p.lineTo(px, py);
            }
            if (fuera) continue;
            p.closePath();
            boolean suyo = !deptoNorm.isEmpty() && i < nombres.size()
                           && MunicipiosCO.normalizar(nombres.get(i)).equals(deptoNorm);
            g.setColor(suyo ? new Color(244, 214, 196) : new Color(238, 238, 240));
            g.fill(p);
            g.setColor(suyo ? TERRA_C : new Color(190, 192, 198));
            g.setStroke(new BasicStroke(suyo ? 0.8f : 0.4f));
            g.draw(p);
        }
        if (m != null) {
            float px = (float) (ox + (m.lon - minLon) * cosLat * esc);
            float py = (float) (oy + (maxLat - m.lat) * esc);
            g.setColor(new Color(198, 40, 40, 60));
            g.fill(new Ellipse2D.Float(px - 9, py - 9, 18, 18));
            g.setColor(ROJO);
            g.fill(new Ellipse2D.Float(px - 3.5f, py - 3.5f, 7, 7));
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(1f));
            g.draw(new Ellipse2D.Float(px - 3.5f, py - 3.5f, 7, 7));
            boolean izq = px > x + w * 0.6f;
            String et = m.nombre;
            if (izq) textoDer(g, et, px - 7, py - 6, 8, true, TINTA);
            else texto(g, et, px + 7, py - 6, 8, true, TINTA);
        }
        textoCentro(g, "Colombia", x + w / 2, y + h + 8, 7.5f, false, SUAVE);
    }

    private static void pagina2(Graphics2D g, Contenido c) {
        float y = 58;
        y = titulo(g, "3. Baldosín", y);
        BaldosinCatalan b = c.baldosin;
        if (b != null) {
            try { b.calcularGeometria(); } catch (Exception ignorar) { }
        }
        double fac = c.cont != null ? 1 - c.cont[0] / 100.0 : 1;

        float colW = (PW - 2 * MX - 16) / 2f;
        String[][] med = b == null ? new String[][]{{"Geometría", "Sin datos"}} : new String[][]{
            {"Largo × ancho (diseño)", mm(b.largo * 10) + " × " + mm(b.ancho * 10)},
            {"Largo × ancho (cocido)", c.cont != null ? mm(b.largo * 10 * fac) + " × " + mm(b.ancho * 10 * fac) : "—"},
            {"Espesor de la base", mm(b.espesor * 10)},
            {"Alto del nervio", mm(b.altoNervio * 10)},
            {"Nervios interiores", String.valueOf(b.numNerviosInt)},
            {"Ancho del nervio", mm(b.nervCentral * 10)},
            {"Volumen neto", String.format(Locale.US, "%.1f cm³", b.volNeto)},
        };
        float ya = tablaClaveTitulo(g, "Medidas", med, MX, y, colW);

        MezclaBaldosin m0 = !c.mezclasDiseno.isEmpty() ? c.mezclasDiseno.get(0) : c.mezcla;
        String[][] pieza = {
            {"Conformado", c.extruido ? "Extruido" : "Prensado"},
            {"Masa de sólidos por pieza", m0 != null && m0.masaTotalSolidos > 0 ? f1(m0.masaTotalSolidos) + " g" : "—"},
            {"Temperatura de cocción", String.format(Locale.US, "%.0f °C", temperaturaCoccion(c))},
        };
        float yb = tablaClaveTitulo(g, "Pieza", pieza, MX + colW + 16, y, colW);
        y = Math.max(ya, yb) + 20;

        y = titulo(g, "Dosificación de las mezclas (por pieza)", y);
        List<String[]> fd = new ArrayList<>();
        List<MezclaBaldosin> lm = c.mezclasDiseno;
        List<String> ln = c.nombresDiseno;
        if (lm.isEmpty() && c.mezcla != null) { lm = new ArrayList<>(); lm.add(c.mezcla); ln = new ArrayList<>(); ln.add(c.nombreMezcla); }
        for (int i = 0; i < lm.size(); i++) {
            MezclaBaldosin m = lm.get(i);
            fd.add(new String[]{ln.get(i),
                pct(m.fracSuelo * 100) + " · " + f1(m.masaSuelo) + " g",
                pct(m.fracCal * 100) + " · " + f1(m.masaCal) + " g",
                pct(m.fracFeldespato * 100) + " · " + f1(m.masaFeldespato) + " g",
                pct(m.relacionAguaSolidos * 100) + " · " + f1(m.masaAgua) + " ml",
                f1(m.masaTotalSolidos) + " g",
                f1(m.masaTotalVerde) + " g"});
        }
        float W = PW - 2 * MX;
        y = tabla(g, new String[]{"Mezcla", "Relave", "Caolín", "Feldespato K", "Agua (s/sólidos)",
                                  "Sólidos", "Total verde"},
                  fd.toArray(new String[0][]), MX, y,
                  new float[]{W * 0.20f, W * 0.15f, W * 0.14f, W * 0.15f, W * 0.15f, W * 0.10f, W * 0.11f}) + 22;

        y = titulo(g, "Secciones acotadas", y);
        if (b == null) { parrafo(g, "Sin geometría del baldosín.", MX, y, 300, 10, false, SUAVE); return; }

        double anchoMm = Math.max(5, b.ancho * 10), largoMm = Math.max(5, b.largo * 10);
        double eMm = Math.max(0.5, b.espesor * 10), hnMm = Math.max(0.3, b.altoNervio * 10);
        int N = Math.max(0, b.numNerviosInt);

        float zonaH = 250, zonaW = (PW - 2 * MX - 40) / 2f;
        double s = Math.min((zonaW - 60) / anchoMm, (zonaH - 40) / largoMm);
        float pw = (float) (anchoMm * s), ph = (float) (largoMm * s);

        float x1 = MX + (zonaW - pw) / 2 - 10, yPl = y + 16;
        textoCentro(g, "Cara lisa", x1 + pw / 2, y + 6, 9.5f, true, TINTA);
        g.setColor(new Color(0, 0, 0, 30));
        g.fill(new Rectangle2D.Float(x1 + 3, yPl + 4, pw, ph));
        g.setColor(ARCILLA);
        g.fill(new Rectangle2D.Float(x1, yPl, pw, ph));
        g.setColor(ARCILLA_B);
        g.setStroke(new BasicStroke(0.8f));
        g.draw(new Rectangle2D.Float(x1, yPl, pw, ph));
        cotaH(g, x1, x1 + pw, yPl + ph + 14, "b = " + mm(anchoMm));
        cotaV(g, yPl, yPl + ph, x1 - 12, "h = " + mm(largoMm), true);

        float x2 = MX + zonaW + 40 + (zonaW - pw) / 2 - 10;
        textoCentro(g, "Cara nervada", x2 + pw / 2, y + 6, 9.5f, true, TINTA);
        g.setColor(new Color(0, 0, 0, 30));
        g.fill(new Rectangle2D.Float(x2 + 3, yPl + 4, pw, ph));
        g.setColor(ARCILLA);
        g.fill(new Rectangle2D.Float(x2, yPl, pw, ph));
        double[] nv = patron(anchoMm, b);
        double xc = 0;
        g.setColor(new Color(150, 70, 42));
        xc += nv[0];
        g.fill(new Rectangle2D.Double(x2, yPl, nv[0] * s, ph));
        for (int i = 0; i < N; i++) {
            xc += nv[2];
            g.fill(new Rectangle2D.Double(x2 + xc * s, yPl, nv[1] * s, ph));
            xc += nv[1];
        }
        xc += nv[2];
        g.fill(new Rectangle2D.Double(x2 + xc * s, yPl, nv[0] * s, ph));
        g.setColor(ARCILLA_B);
        g.draw(new Rectangle2D.Float(x2, yPl, pw, ph));
        cotaH(g, x2, x2 + pw, yPl + ph + 14, "b = " + mm(anchoMm));
        cotaV(g, yPl, yPl + ph, x2 + pw + 12, "h = " + mm(largoMm), false);

        y = yPl + ph + 40;

        float cw = PW - 2 * MX - 235;
        double sc = cw / anchoMm;
        double altoTot = (eMm + hnMm) * sc;
        float xCorte = MX + 10, yCorte = (float) (y + 16 + hnMm * sc);
        textoCentro(g, "Corte transversal", xCorte + cw / 2, y + 4, 9.5f, true, TINTA);
        g.setColor(ARCILLA);
        g.fill(new Rectangle2D.Double(xCorte, yCorte, cw, eMm * sc));
        g.setColor(new Color(150, 70, 42));
        xc = 0;
        g.fill(new Rectangle2D.Double(xCorte, yCorte - hnMm * sc, nv[0] * sc, hnMm * sc));
        xc += nv[0];
        for (int i = 0; i < N; i++) {
            xc += nv[2];
            g.fill(new Rectangle2D.Double(xCorte + xc * sc, yCorte - hnMm * sc, nv[1] * sc, hnMm * sc));
            xc += nv[1];
        }
        xc += nv[2];
        g.fill(new Rectangle2D.Double(xCorte + xc * sc, yCorte - hnMm * sc, nv[0] * sc, hnMm * sc));
        g.setColor(ARCILLA_B);
        g.setStroke(new BasicStroke(0.6f));
        g.draw(new Rectangle2D.Double(xCorte, yCorte, cw, eMm * sc));
        cotaH(g, xCorte, xCorte + cw, (float) (yCorte + eMm * sc + 14), "b = " + mm(anchoMm));

        float xDet = PW - MX - 150, wDet = 140;
        double patronMm = nv[0] + nv[2] + nv[1] + nv[2] + nv[1] * 0.5;
        double k = wDet / patronMm;
        k = Math.min(k, 60 / (eMm + hnMm));
        double nbE = nv[0] * k, rsE = nv[2] * k, nsE = nv[1] * k, hnE = hnMm * k, eE = eMm * k;
        double anchoDet = nbE + rsE + nsE + rsE + nsE * 0.5;
        float xd0 = (float) (PW - MX - anchoDet - 4);
        float yBase = (float) (y + 28 + hnE);
        textoCentro(g, "Detalle de nervios", xDet + wDet / 2, y + 4, 9.5f, true, TINTA);
        g.setColor(ARCILLA);
        g.fill(new Rectangle2D.Double(xd0, yBase, anchoDet, eE));
        g.setColor(new Color(150, 70, 42));
        g.fill(new Rectangle2D.Double(xd0, yBase - hnE, nbE, hnE));
        double xN1 = xd0 + nbE + rsE;
        g.fill(new Rectangle2D.Double(xN1, yBase - hnE, nsE, hnE));
        double xN2 = xN1 + nsE + rsE;
        g.fill(new Rectangle2D.Double(xN2, yBase - hnE, nsE * 0.5, hnE));
        g.setColor(ARCILLA_B);
        g.draw(new Rectangle2D.Double(xd0, yBase, anchoDet, eE));
        cotaH(g, (float) xN1, (float) (xN1 + nsE), (float) (yBase - hnE - 6), "");
        textoDer(g, "bn = " + mm(b.nervCentral * 10), (float) (xN1 - 4), (float) (yBase - hnE - 4),
                 7.5f, false, AZUL);
        cotaV(g, (float) (yBase - hnE), yBase, xd0 - 8, "hn = " + mm(hnMm), true);
        cotaV(g, yBase, (float) (yBase + eE), xd0 - 8, "e = " + mm(eMm), true);

        y = (float) Math.max(yCorte + eMm * sc + 30, yBase + eE + 24) + 10;
    }

    private static double[] patron(double anchoMm, BaldosinCatalan b) {
        int N = Math.max(0, b.numNerviosInt);
        double nb = Math.max(0.2, b.nervBorde * 10), ns = Math.max(0.2, b.nervCentral * 10),
               rs = Math.max(0.1, b.ranura * 10);
        double total = 2 * nb + N * ns + (N + 1) * rs;
        if (total > 0.001) { double k = anchoMm / total; nb *= k; ns *= k; rs *= k; }
        return new double[]{nb, ns, rs};
    }

    private static void pagina3(Graphics2D g, Contenido c) {
        float y = 58;
        y = titulo(g, "4. Curva de secado", y);
        boolean secadoTipo = c.curvaSecado == null || c.curvaSecado.isEmpty();
        List<double[]> sec = secadoTipo ? secadoTipo(c) : c.curvaSecado;
        double tAmb = 72;
        double[] zonaSec = secadoTipo ? new double[]{tAmb, sec.get(sec.size() - 1)[0]} : null;
        y = grafica(g, MX, y + 4, PW - 2 * MX, 230, sec, "Tiempo (h)", "Humedad (%)",
                    new Color(30, 110, 190), zonaSec, "Estufa 110 °C", Double.NaN, null) + 8;
        y = parrafo(g, secadoTipo
                ? "Curva tipo estimada: secado lento a la sombra durante " + (int) tAmb + " h y final en estufa "
                + "a 110 °C hasta masa constante. La humedad inicial se estima a partir del límite plástico."
                : "Curva de secado registrada para la mezcla.", MX, y, PW - 2 * MX, 8.8f, false, SUAVE) + 22;

        y = titulo(g, "5. Curva de cocción", y);
        boolean coccionTipo = c.curvaCoccion == null || c.curvaCoccion.isEmpty();
        double tmax = temperaturaCoccion(c);
        List<double[]> coc = coccionTipo ? coccionTipo(tmax) : c.curvaCoccion;
        y = grafica(g, MX, y + 4, PW - 2 * MX, 250, coc, "Tiempo (h)", "Temperatura (°C)",
                    TERRA, null, null, 573, "Inversión del cuarzo · 573 °C") + 8;

        if (coccionTipo) {
            String[][] etapas = {
                {"Ambiente → 200 °C", "60 °C/h", "Salida del agua de humedad residual"},
                {"200 → 600 °C", "80 °C/h", "Combustión de orgánicos y deshidroxilación de arcillas"},
                {"Meseta a 600 °C", "30 min", "Igualar temperatura antes de la inversión del cuarzo"},
                {"600 → " + String.format(Locale.US, "%.0f", tmax) + " °C", "120 °C/h", "Sinterización"},
                {"Meseta a " + String.format(Locale.US, "%.0f", tmax) + " °C", "60 min", "Homogeneizar la pieza"},
                {"Enfriamiento hasta 500 °C", "150 → 50 °C/h", "Lento entre 600 y 500 °C (cuarzo)"},
            };
            y = tabla(g, new String[]{"Etapa", "Velocidad", "Objetivo"}, etapas, MX, y,
                      new float[]{150, 90, PW - 2 * MX - 240}) + 16;
            parrafo(g, "Programa tipo estimado para la mezcla; la temperatura máxima se calcula con la "
                     + "proporción de fundentes. Ajústelo al horno disponible.", MX, y, PW - 2 * MX, 8.8f, false, SUAVE);
        }
    }

    private static double temperaturaCoccion(Contenido c) {
        return 1100;
    }

    private static List<double[]> secadoTipo(Contenido c) {
        double w0 = 20;
        if (c.suelo != null && c.suelo.atterCalculada && c.suelo.LP > 0)
            w0 = Math.max(12, Math.min(35, c.suelo.LP + 3));
        List<double[]> p = new ArrayList<>();
        double we = 3, tau = 20, w72 = 0;
        for (int t = 0; t <= 72; t++) {
            double w = we + (w0 - we) * Math.exp(-t / tau);
            p.add(new double[]{t, w});
            w72 = w;
        }
        for (int t = 1; t <= 24; t++) p.add(new double[]{72 + t, w72 * Math.exp(-t / 4.0)});
        return p;
    }

    private static List<double[]> coccionTipo(double tmax) {
        List<double[]> p = new ArrayList<>();
        double[] st = {0, 25};
        p.add(st.clone());
        rampa(p, st, 200, 60);
        rampa(p, st, 600, 80);
        meseta(p, st, 0.5);
        rampa(p, st, tmax, 120);
        meseta(p, st, 1.0);
        rampa(p, st, 600, 150);
        rampa(p, st, 500, 50);
        rampa(p, st, 40, 100);
        return p;
    }

    private static void rampa(List<double[]> p, double[] st, double tFin, double vel) {
        st[0] += Math.abs(tFin - st[1]) / vel;
        st[1] = tFin;
        p.add(st.clone());
    }

    private static void meseta(List<double[]> p, double[] st, double horas) {
        st[0] += horas;
        p.add(st.clone());
    }

    private static float grafica(Graphics2D g, float x, float y, float w, float h, List<double[]> pts,
                                 String ejeX, String ejeY, Color col, double[] zona, String etZona,
                                 double refY, String etRef) {
        float px0 = x + 42, py0 = y + 8, pw = w - 54, ph = h - 40;
        double xMax = 0, yMax = 0;
        for (double[] q : pts) { xMax = Math.max(xMax, q[0]); yMax = Math.max(yMax, q[1]); }
        xMax = escalaBonita(xMax);
        yMax = escalaBonita(yMax * 1.08);

        g.setColor(FONDO_T);
        g.fill(new Rectangle2D.Float(px0, py0, pw, ph));
        if (zona != null) {
            float za = (float) (px0 + zona[0] / xMax * pw), zb = (float) (px0 + zona[1] / xMax * pw);
            g.setColor(new Color(30, 96, 200, 22));
            g.fill(new Rectangle2D.Float(za, py0, zb - za, ph));
            texto(g, etZona, za + 4, py0 + 12, 7.5f, true, AZUL);
        }
        g.setStroke(new BasicStroke(0.4f));
        for (int i = 0; i <= 5; i++) {
            float yy = py0 + ph - i / 5f * ph;
            g.setColor(LINEA);
            g.draw(new Line2D.Float(px0, yy, px0 + pw, yy));
            textoDer(g, numEje(yMax * i / 5), px0 - 4, yy + 3, 7.5f, false, SUAVE);
        }
        for (int i = 0; i <= 5; i++) {
            float xx = px0 + i / 5f * pw;
            g.setColor(LINEA);
            g.draw(new Line2D.Float(xx, py0, xx, py0 + ph));
            textoCentro(g, numEje(xMax * i / 5), xx, py0 + ph + 11, 7.5f, false, SUAVE);
        }
        if (!Double.isNaN(refY) && refY < yMax) {
            float yy = (float) (py0 + ph - refY / yMax * ph);
            g.setColor(AMBAR);
            g.setStroke(new BasicStroke(0.8f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10,
                                        new float[]{4, 3}, 0));
            g.draw(new Line2D.Float(px0, yy, px0 + pw, yy));
            texto(g, etRef, px0 + 6, yy - 4, 7.5f, true, AMBAR);
        }
        Path2D linea = new Path2D.Double();
        for (int i = 0; i < pts.size(); i++) {
            double xx = px0 + pts.get(i)[0] / xMax * pw, yy = py0 + ph - pts.get(i)[1] / yMax * ph;
            if (i == 0) linea.moveTo(xx, yy); else linea.lineTo(xx, yy);
        }
        g.setColor(col);
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(linea);
        g.setColor(TINTA);
        g.setStroke(new BasicStroke(0.8f));
        g.draw(new Line2D.Float(px0, py0, px0, py0 + ph));
        g.draw(new Line2D.Float(px0, py0 + ph, px0 + pw, py0 + ph));
        textoCentro(g, ejeX, px0 + pw / 2, py0 + ph + 24, 8.5f, false, TINTA);
        AffineTransform at = g.getTransform();
        g.rotate(-Math.PI / 2, x + 8, py0 + ph / 2);
        textoCentro(g, ejeY, x + 8, py0 + ph / 2, 8.5f, false, TINTA);
        g.setTransform(at);
        return y + h;
    }

    private static String numEje(double v) {
        return Math.abs(v - Math.rint(v)) < 1e-6 ? String.format(Locale.US, "%.0f", v)
                                                 : String.format(Locale.US, "%.1f", v);
    }

    private static double escalaBonita(double v) {
        if (v <= 0) return 1;
        double e = Math.pow(10, Math.floor(Math.log10(v)));
        double n = v / e;
        double[] pasos = {1, 1.2, 1.5, 2, 2.5, 3, 4, 5, 6, 8, 10};
        double b = 10;
        for (double q : pasos) if (n <= q + 1e-9) { b = q; break; }
        return b * e;
    }

    private static void pagina4(Graphics2D g, Contenido c, String sec, boolean varias) {
        float y = 58;
        y = titulo(g, sec + " Resumen de ensayos · " + c.nombreMezcla, y);

        List<String[]> filas = new ArrayList<>();
        if (c.abs != null)
            filas.add(new String[]{"Absorción de agua (Ev)", f2(c.abs[0]) + " %  (σ " + f2(c.abs[1]) + ")",
                    c.grupo != null ? "Grupo " + c.grupo[0] + " · " + c.grupo[1] : "—", "Clasifica"});
        if (c.cont != null)
            filas.add(new String[]{"Contracción lineal total", f2(c.cont[0]) + " %  (σ " + f2(c.cont[1]) + ")",
                    "Informativo", "—"});
        if (c.dens != null)
            filas.add(new String[]{"Densidad aparente", f3(c.dens[0]) + " g/cm³", "Informativo", "—"});
        if (c.poros != null)
            filas.add(new String[]{"Porosidad aparente", f2(c.poros[0]) + " %", "Informativo", "—"});
        filas.add(new String[]{"Fuerza de rotura S", f0(c.sProm) + " N  (mín " + f0(c.sMin) + ")",
                c.textoNorma, c.flexCumple ? "CUMPLE" : "NO CUMPLE"});
        filas.add(new String[]{"Módulo de rotura R", f2(c.rProm) + " MPa  (mín " + f2(c.rMin) + ")",
                c.textoNorma, c.flexCumple ? "CUMPLE" : "NO CUMPLE"});
        y = tabla(g, new String[]{"Ensayo", "Resultado", "Referencia NTC 919 / ISO 13006", "Estado"},
                  filas.toArray(new String[0][]), MX, y, new float[]{120, 120, PW - 2 * MX - 300, 60}) + 16;

        texto(g, "Flexión en 3 puntos por probeta", MX, y, 10, true, TINTA);
        y += 8;
        List<String[]> fx = new ArrayList<>();
        for (DetalleFlexion.ResultadoFlexion f : c.flexion) {
            fx.add(new String[]{f.probeta, f1(f.largo), f1(f.luz), f1(f.ancho), f2(f.espesor),
                    f0(f.fMaxN), f0(f.sN), f2(f.rMPa), f.cumple ? "Cumple" : "No cumple"});
        }
        float wcol = (PW - 2 * MX) / 9f;
        y = tabla(g, new String[]{"Probeta", "L cocida mm", "Luz mm", "Ancho mm", "Espesor mm",
                                  "F máx N", "S N", "R MPa", "NTC 919"},
                  fx.toArray(new String[0][]), MX, y,
                  new float[]{wcol, wcol, wcol, wcol, wcol, wcol, wcol, wcol, wcol}) + 10;

        boolean completo = c.flexion.size() == c.nProbetas;
        boolean ok = c.flexCumple && c.flexIndivOk == c.flexion.size();
        String concl = !completo
            ? "Resultado parcial: faltan probetas por ensayar a flexión."
            : "Conclusión: la mezcla «" + c.nombreMezcla + "» " + (ok ? "CUMPLE" : "NO CUMPLE")
              + " los requisitos de flexión de la NTC 919 para el grupo "
              + (c.grupo != null ? c.grupo[0] : "—") + ".";
        g.setColor(ok && completo ? new Color(232, 245, 236) : new Color(252, 238, 232));
        float hBox = alturaParrafo(g, concl, PW - 2 * MX - 20, 10) + 14;
        g.fill(new RoundRectangle2D.Float(MX, y, PW - 2 * MX, hBox, 8, 8));
        parrafo(g, concl, MX + 10, y + 15, PW - 2 * MX - 20, 10, true, ok && completo ? VERDE : ROJO);
        y += hBox + 20;

        y = titulo(g, varias ? "Uso recomendado · " + c.nombreMezcla : "7. Uso recomendado", y);
        y = parrafo(g, c.usoTitulo, MX, y + 2, PW - 2 * MX, 12, true, TERRA) + 2;
        y = parrafo(g, c.usoDetalle, MX, y, PW - 2 * MX, 9.2f, false, TINTA) + 10;

        int n = c.usoImagenes.length;
        if (n > 0) {
            float gap = 12;
            float iw = Math.min(250, (PW - 2 * MX - gap * (n - 1)) / n);
            float ih = iw * 0.66f;
            float disponible = PH - 60 - y;
            if (ih > disponible) { ih = Math.max(60, disponible); iw = ih / 0.66f; }
            for (int i = 0; i < n; i++) {
                BufferedImage im = imagenUso(c.usoImagenes[i], c.usoPies[i], Math.round(iw * 3), Math.round(ih * 3));
                g.drawImage(im, Math.round(MX + i * (iw + gap)), Math.round(y), Math.round(iw), Math.round(ih), null);
            }
            y += ih + 10;
        }
    }

    private static void pagina5(Graphics2D g, Comparacion k) {
        List<Contenido> l = k.mezclas;
        float y = 58;
        y = titulo(g, "7. Comparación de mezclas", y);

        List<String[]> filas = new ArrayList<>();
        for (Contenido c : l) {
            filas.add(new String[]{
                c.nombreMezcla,
                c.abs != null ? f2(c.abs[0]) + " %" : "—",
                c.cont != null ? f2(c.cont[0]) + " %" : "—",
                (c.flexion != null && !c.flexion.isEmpty()) ? f2(c.rProm) + " MPa" : "—",
                c.flexOkTotal() ? "CUMPLE" : "NO CUMPLE",
                c.usoTitulo == null || c.usoTitulo.isEmpty() ? "—" : c.usoTitulo});
        }
        float W = PW - 2 * MX;
        y = tabla(g, new String[]{"Mezcla", "Absorción", "Contracción", "Flexión R", "NTC 919", "Uso"},
                  filas.toArray(new String[0][]), MX, y,
                  new float[]{W * 0.20f, W * 0.12f, W * 0.13f, W * 0.13f, W * 0.12f, W * 0.30f}) + 18;

        String[][] mj = {
            {"Menor absorción (exteriores y zonas húmedas)", k.mejorAbs  >= 0 ? l.get(k.mejorAbs).nombreMezcla  : "—"},
            {"Menor contracción (medidas más exactas)",      k.mejorCont >= 0 ? l.get(k.mejorCont).nombreMezcla : "—"},
            {"Mayor resistencia (pisos con tránsito)",       k.mejorFlex >= 0 ? l.get(k.mejorFlex).nombreMezcla : "—"},
        };
        y = tablaClaveTitulo(g, "Mejor mezcla en cada ensayo", mj, MX, y, W) + 18;

        if (k.recomendada >= 0) {
            Contenido r = l.get(k.recomendada);
            String txt = "Mezcla recomendada: " + r.nombreMezcla
                + (r.usoTitulo == null || r.usoTitulo.isEmpty() ? "" : " — " + r.usoTitulo);
            g.setColor(k.algunaCumple ? new Color(232, 245, 236) : new Color(252, 238, 232));
            float hBox = alturaParrafo(g, txt, W - 20, 11) + 14;
            g.fill(new RoundRectangle2D.Float(MX, y, W, hBox, 8, 8));
            parrafo(g, txt, MX + 10, y + 16, W - 20, 11, true, k.algunaCumple ? VERDE : ROJO);
        }
    }

    private static String corto(String s) {
        if (s == null) return "—";
        int i = s.indexOf(' ');
        return i > 0 ? s.substring(0, i) : s;
    }

    private static void barras(Graphics2D g, float x, float y, float w, float h, String tit,
                               String[] nom, double[] v, int mejor, String fmtNum) {
        g.setColor(FONDO_T);
        g.fill(new RoundRectangle2D.Float(x, y, w, h, 6, 6));
        parrafo(g, tit, x + 6, y + 12, w - 12, 7.6f, true, TINTA);
        double max = 0;
        for (double d : v) if (!Double.isNaN(d)) max = Math.max(max, d);
        if (max <= 0) max = 1;
        int n = v.length;
        float top = y + 30, base = y + h - 16, alto = base - top;
        float slot = (w - 16) / Math.max(1, n), bar = Math.min(34, slot * 0.6f);
        for (int i = 0; i < n; i++) {
            float cx = x + 8 + slot * i + slot / 2f;
            if (!Double.isNaN(v[i])) {
                float hb = (float) (v[i] / max * alto);
                g.setColor(i == mejor ? VERDE : TERRA_C);
                g.fill(new Rectangle2D.Float(cx - bar / 2, base - hb, bar, hb));
                textoCentro(g, String.format(Locale.US, fmtNum, v[i]), cx, base - hb - 3, 7.4f, i == mejor, i == mejor ? VERDE : TINTA);
            } else {
                textoCentro(g, "—", cx, base - 3, 7.4f, false, SUAVE);
            }
            textoCentro(g, nom[i], cx, base + 10, 7.6f, true, TINTA);
        }
        g.setColor(LINEA);
        g.draw(new Line2D.Float(x + 6, base, x + w - 6, base));
    }

    private static BufferedImage imagenUso(String clave, String pie, int w, int h) {
        BufferedImage im = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = im.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        EnsayosNTC.ImagenUso p = new EnsayosNTC.ImagenUso(clave, pie);
        p.setSize(w / 2, h / 2);
        g.scale(2, 2);
        p.paint(g);
        g.dispose();
        return im;
    }

    private static Font fuente(float tam, boolean neg) {
        return new Font("SansSerif", neg ? Font.BOLD : Font.PLAIN, 10).deriveFont(tam);
    }

    private static void texto(Graphics2D g, String s, float x, float y, float tam, boolean neg, Color c) {
        g.setFont(fuente(tam, neg));
        g.setColor(c);
        g.drawString(s, x, y);
    }

    private static void textoDer(Graphics2D g, String s, float x, float y, float tam, boolean neg, Color c) {
        g.setFont(fuente(tam, neg));
        g.setColor(c);
        g.drawString(s, x - g.getFontMetrics().stringWidth(s), y);
    }

    private static void textoCentro(Graphics2D g, String s, float x, float y, float tam, boolean neg, Color c) {
        g.setFont(fuente(tam, neg));
        g.setColor(c);
        g.drawString(s, x - g.getFontMetrics().stringWidth(s) / 2f, y);
    }

    private static float parrafo(Graphics2D g, String s, float x, float y, float ancho,
                                 float tam, boolean neg, Color c) {
        g.setFont(fuente(tam, neg));
        g.setColor(c);
        FontMetrics fm = g.getFontMetrics();
        float lh = tam * 1.32f;
        for (String linea : partir(s == null ? "" : s, fm, ancho)) {
            g.drawString(linea, x, y);
            y += lh;
        }
        return y;
    }

    private static float alturaParrafo(Graphics2D g, String s, float ancho, float tam) {
        g.setFont(fuente(tam, true));
        return partir(s, g.getFontMetrics(), ancho).size() * tam * 1.32f;
    }

    private static List<String> partir(String s, FontMetrics fm, float ancho) {
        List<String> out = new ArrayList<>();
        StringBuilder l = new StringBuilder();
        for (String p : s.split(" ")) {
            String prueba = l.length() == 0 ? p : l + " " + p;
            if (fm.stringWidth(prueba) > ancho && l.length() > 0) {
                out.add(l.toString());
                l = new StringBuilder(p);
            } else {
                l = new StringBuilder(prueba);
            }
        }
        if (l.length() > 0) out.add(l.toString());
        return out;
    }

    private static float titulo(Graphics2D g, String s, float y) {
        texto(g, s, MX, y, 13, true, TINTA);
        g.setColor(TERRA);
        g.fill(new Rectangle2D.Float(MX, y + 5, 36, 2));
        return y + 22;
    }

    private static float tablaClave(Graphics2D g, String[][] filas, float x, float y, float w) {
        float lh = 17;
        for (int i = 0; i < filas.length; i++) {
            if (i % 2 == 0) {
                g.setColor(FONDO_T);
                g.fill(new Rectangle2D.Float(x, y, w, lh));
            }
            texto(g, filas[i][0], x + 6, y + 12, 9, false, SUAVE);
            texto(g, filas[i][1], x + w * 0.42f, y + 12, 9, true, TINTA);
            y += lh;
        }
        return y;
    }

    private static float tablaClaveTitulo(Graphics2D g, String titulo, String[][] filas, float x, float y, float w) {
        texto(g, titulo, x, y + 2, 10, true, TERRA);
        g.setColor(LINEA);
        g.setStroke(new BasicStroke(0.6f));
        g.draw(new Line2D.Float(x, y + 7, x + w, y + 7));
        float yy = y + 11;
        float lh = 16;
        for (int i = 0; i < filas.length; i++) {
            if (i % 2 == 0) {
                g.setColor(FONDO_T);
                g.fill(new Rectangle2D.Float(x, yy, w, lh));
            }
            texto(g, filas[i][0], x + 5, yy + 11.5f, 8.6f, false, SUAVE);
            textoDer(g, filas[i][1], x + w - 5, yy + 11.5f, 8.6f, true, TINTA);
            yy += lh;
        }
        return yy;
    }

    private static float tabla(Graphics2D g, String[] cab, String[][] filas, float x, float y, float[] anchos) {
        float total = 0;
        for (float a : anchos) total += a;
        g.setColor(TINTA);
        g.fill(new Rectangle2D.Float(x, y, total, 18));
        float cx = x;
        for (int j = 0; j < cab.length; j++) {
            texto(g, cab[j], cx + 5, y + 12.5f, 8.2f, true, Color.WHITE);
            cx += anchos[j];
        }
        y += 18;
        for (int i = 0; i < filas.length; i++) {
            g.setFont(fuente(8.4f, false));
            FontMetrics fm = g.getFontMetrics();
            int lineas = 1;
            for (int j = 0; j < cab.length && j < filas[i].length; j++)
                lineas = Math.max(lineas, partir(filas[i][j] == null ? "" : filas[i][j], fm, anchos[j] - 10).size());
            float h = 6 + lineas * 11;
            if (i % 2 == 1) {
                g.setColor(FONDO_T);
                g.fill(new Rectangle2D.Float(x, y, total, h));
            }
            cx = x;
            for (int j = 0; j < cab.length && j < filas[i].length; j++) {
                String v = filas[i][j] == null ? "" : filas[i][j];
                Color col = TINTA;
                boolean neg = false;
                if (v.equals("CUMPLE") || v.equals("Cumple")) { col = VERDE; neg = true; }
                else if (v.equals("NO CUMPLE") || v.equals("No cumple")) { col = ROJO; neg = true; }
                else if (v.equals("Clasifica")) { col = AMBAR; neg = true; }
                parrafo(g, v, cx + 5, y + 12, anchos[j] - 10, 8.4f, neg || j == 0, col);
                cx += anchos[j];
            }
            y += h;
        }
        g.setColor(LINEA);
        g.setStroke(new BasicStroke(0.6f));
        g.draw(new Line2D.Float(x, y, x + total, y));
        return y;
    }

    private static void cotaH(Graphics2D g, float x1, float x2, float y, String t) {
        g.setColor(AZUL);
        g.setStroke(new BasicStroke(0.6f));
        g.draw(new Line2D.Float(x1, y, x2, y));
        g.draw(new Line2D.Float(x1, y - 3, x1, y + 3));
        g.draw(new Line2D.Float(x2, y - 3, x2, y + 3));
        textoCentro(g, t, (x1 + x2) / 2, y - 3, 7.5f, false, AZUL);
    }

    private static void cotaV(Graphics2D g, float y1, float y2, float x, String t, boolean izquierda) {
        g.setColor(AZUL);
        g.setStroke(new BasicStroke(0.6f));
        g.draw(new Line2D.Float(x, y1, x, y2));
        g.draw(new Line2D.Float(x - 3, y1, x + 3, y1));
        g.draw(new Line2D.Float(x - 3, y2, x + 3, y2));
        float ym = (y1 + y2) / 2 + 2.5f;
        if (izquierda) textoDer(g, t, x - 4, ym, 7.5f, false, AZUL);
        else texto(g, t, x + 4, ym, 7.5f, false, AZUL);
    }

    private static String mm(double v)  { return String.format(Locale.US, "%.1f mm", v); }
    private static String pct(double v) { return String.format(Locale.US, "%.1f %%", v); }
    private static String f0(double v)  { return Double.isNaN(v) ? "—" : String.format(Locale.US, "%.0f", v); }
    private static String f1(double v)  { return Double.isNaN(v) ? "—" : String.format(Locale.US, "%.1f", v); }
    private static String f2(double v)  { return Double.isNaN(v) ? "—" : String.format(Locale.US, "%.2f", v); }
    private static String f3(double v)  { return Double.isNaN(v) ? "—" : String.format(Locale.US, "%.3f", v); }

    private static void escribirPdf(File f, List<BufferedImage> paginas) throws IOException {
        int n = paginas.size();
        int nObj = 3 + 3 * n;
        long[] off = new long[nObj + 1];

        try (OutputStream os = new FileOutputStream(f)) {
            Contador out = new Contador(os);
            out.escribir("%PDF-1.4\n");
            out.bytes(new byte[]{'%', (byte) 0xE2, (byte) 0xE3, (byte) 0xCF, (byte) 0xD3, '\n'});

            off[1] = out.n;
            out.escribir("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

            StringBuilder kids = new StringBuilder();
            for (int i = 0; i < n; i++) kids.append(4 + 3 * i).append(" 0 R ");
            off[2] = out.n;
            out.escribir("2 0 obj\n<< /Type /Pages /Kids [" + kids + "] /Count " + n + " >>\nendobj\n");

            off[3] = out.n;
            out.escribir("3 0 obj\n<< /Title (Informe final - Baldosin) /Producer (SDIM) >>\nendobj\n");

            String caja = String.format(Locale.US, "[0 0 %.2f %.2f]", PW, PH);
            for (int i = 0; i < n; i++) {
                int pag = 4 + 3 * i, cont = pag + 1, img = pag + 2;
                BufferedImage im = paginas.get(i);
                byte[] jpg = jpeg(im, 0.9f);

                off[pag] = out.n;
                out.escribir(pag + " 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox " + caja
                        + " /Resources << /XObject << /Im" + i + " " + img + " 0 R >> >> /Contents "
                        + cont + " 0 R >>\nendobj\n");

                String cs = String.format(Locale.US, "q %.2f 0 0 %.2f 0 0 cm /Im%d Do Q\n", PW, PH, i);
                off[cont] = out.n;
                out.escribir(cont + " 0 obj\n<< /Length " + cs.length() + " >>\nstream\n" + cs + "endstream\nendobj\n");

                off[img] = out.n;
                out.escribir(img + " 0 obj\n<< /Type /XObject /Subtype /Image /Width " + im.getWidth()
                        + " /Height " + im.getHeight() + " /ColorSpace /DeviceRGB /BitsPerComponent 8"
                        + " /Filter /DCTDecode /Length " + jpg.length + " >>\nstream\n");
                out.bytes(jpg);
                out.escribir("\nendstream\nendobj\n");
            }

            long xref = out.n;
            StringBuilder sb = new StringBuilder("xref\n0 " + (nObj + 1) + "\n0000000000 65535 f \n");
            for (int i = 1; i <= nObj; i++) sb.append(String.format("%010d 00000 n \n", off[i]));
            sb.append("trailer\n<< /Size ").append(nObj + 1).append(" /Root 1 0 R /Info 3 0 R >>\n")
              .append("startxref\n").append(xref).append("\n%%EOF\n");
            out.escribir(sb.toString());
        }
    }

    private static byte[] jpeg(BufferedImage im, float calidad) throws IOException {
        Iterator<ImageWriter> it = ImageIO.getImageWritersByFormatName("jpeg");
        ImageWriter w = it.next();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(bos)) {
            w.setOutput(ios);
            ImageWriteParam p = w.getDefaultWriteParam();
            p.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            p.setCompressionQuality(calidad);
            w.write(null, new IIOImage(im, null, null), p);
        } finally {
            w.dispose();
        }
        return bos.toByteArray();
    }

    private static final class Contador {
        final OutputStream os;
        long n;
        Contador(OutputStream os) { this.os = os; }
        void escribir(String s) throws IOException { bytes(s.getBytes(StandardCharsets.ISO_8859_1)); }
        void bytes(byte[] b) throws IOException { os.write(b); n += b.length; }
    }
}