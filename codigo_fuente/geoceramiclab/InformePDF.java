package geoceramiclab;

import geoceramiclab.model.SueloData;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class InformePDF {

    public static class Seleccion {
        public boolean granulometria = false;
        public boolean atterberg     = false;
        public boolean hidrometro    = false;
        public boolean ternario      = false;
        public boolean resumen       = true;
    }

    private static final int PW = 595, PH = 842;
    private static final int ML = 40, MR = 40, CW = PW - ML - MR;

    private static final Color HDR   = new Color(0x0D2137);
    private static final Color NARANJ= new Color(0xE8760A);
    private static final Color BSEC  = new Color(0x1A4785);
    private static final Color BLITE = new Color(0x2E6FC9);
    private static final Color BANLY = new Color(0xDCEBFF);
    private static final Color BATXT = new Color(0x1D3F6E);
    private static final Color TH    = new Color(0x2B4C73);
    private static final Color TO    = new Color(0xF0F5FC);
    private static final Color TB    = Color.WHITE;
    private static final Color TBD   = new Color(0xC5D8F0);
    private static final Color TSUM  = new Color(0xD0E8F8);
    private static final Color G1 = new Color(0x111111);
    private static final Color G2 = new Color(0x333333);
    private static final Color G3 = new Color(0x666666);
    private static final Color G4 = new Color(0x999999);
    private static final Color G5 = new Color(0xCCCCCC);
    private static final Color W  = Color.WHITE;

    private static final int HDR_H = 50;
    private static final int MBR_H = 34;
    private static final int FTR_H = 42;
    private static final int BODY  = PH - HDR_H - 8;
    private static final int BODYP = PH - HDR_H - MBR_H - 8;

    public static void generarInforme(SueloData d, File archivo,
            String proyecto, String muestra, String operador,
            Seleccion sel) throws IOException {

        String dt = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
        String[] m = {proyecto, muestra, operador, dt};

        PDFDoc pdf = new PDFDoc();

        boolean hayEn = (sel.granulometria && d.granCalculada)
                     || (sel.atterberg && d.atterCalculada)
                     || (sel.hidrometro && d.hidroCalculada);
        int nEn = 0;
        if (sel.granulometria && d.granCalculada) nEn++;
        if (sel.atterberg     && d.atterCalculada) nEn++;
        if (sel.hidrometro    && d.hidroCalculada) nEn++;

        int tot = 1;
        if (hayEn && nEn > 1) tot += nEn - 1;
        if (sel.ternario && d.quimicaCalculada) tot++;
        if (sel.resumen) tot++;

        int[] T = {tot}, N = {1};

        PDFPage p1 = pdf.newPage();
        hdr(p1, m, N[0], T[0], true);
        membrete(p1, d, m);
        int y = BODYP;
        boolean priIns = false;

        if (sel.granulometria && d.granCalculada && !priIns) {
            y = pgGran(p1, d, y, N[0]);  priIns = true;
        }
        if (sel.atterberg && d.atterCalculada && !priIns) {
            y = pgAtter(p1, d, y, N[0]); priIns = true;
        }
        if (sel.hidrometro && d.hidroCalculada && !priIns) {
            y = pgHidro(p1, d, y, N[0]); priIns = true;
        }
        ftr(p1, m, N[0], T[0]);
        pdf.addPage(p1); N[0]++;

        if (sel.atterberg && d.atterCalculada) {
            boolean f1 = priIns && (!sel.granulometria || !d.granCalculada) && (!sel.hidrometro || !d.hidroCalculada);
            if (!f1) {
                PDFPage p = pdf.newPage();
                hdr(p, m, N[0], T[0], false);
                pgAtter(p, d, BODY, N[0]);
                ftr(p, m, N[0], T[0]);
                pdf.addPage(p); N[0]++;
            }
        }
        if (sel.hidrometro && d.hidroCalculada) {
            boolean f1 = priIns && (!sel.granulometria || !d.granCalculada) && (!sel.atterberg || !d.atterCalculada);
            if (!f1) {
                PDFPage p = pdf.newPage();
                hdr(p, m, N[0], T[0], false);
                pgHidro(p, d, BODY, N[0]);
                ftr(p, m, N[0], T[0]);
                pdf.addPage(p); N[0]++;
            }
        }
        if (sel.ternario && d.quimicaCalculada) {
            PDFPage p = pdf.newPage();
            hdr(p, m, N[0], T[0], false);
            pgTern(p, d, BODY);
            ftr(p, m, N[0], T[0]);
            pdf.addPage(p); N[0]++;
        }
        if (sel.resumen) {
            PDFPage p = pdf.newPage();
            hdr(p, m, N[0], T[0], false);
            pgRes(p, d, m, BODY);
            ftr(p, m, N[0], T[0]);
            pdf.addPage(p);
        }
        try (FileOutputStream fos = new FileOutputStream(archivo)) { pdf.escribir(fos); }
    }

    private static void hdr(PDFPage pg, String[] m, int n, int tot, boolean portada) throws IOException {
        pg.fill(0, PH - HDR_H, PW, HDR_H, HDR);
        pg.fill(0, PH - HDR_H, PW, 3, NARANJ);
        pg.txt(ML, PH - 31, "SDIM", 13f, W, true);
        pg.line(ML + 130, PH - 13, ML + 130, PH - HDR_H + 5, new Color(255,255,255,50), 0.7f);
        pg.txt(ML + 140, PH - 25, "Laboratorio de Caracterización de Suelos y Minerales", 8f, new Color(155,200,245), false);
        if (!portada) {
            pg.txt(ML + 140, PH - 35, trunc(m[0], 72), 7f, new Color(120,170,220), false);
            pg.txt(ML + 140, PH - 45, "Muestra: " + m[1] + "   Operador: " + m[2] + "   " + m[3], 7f, new Color(100,155,210), false);
        }
        pg.txtR(PW - ML, PH - 29, "Pág. " + n + " / " + tot + "  ·  " + m[3], 8f, new Color(155,200,245), false);
    }

    private static void membrete(PDFPage pg, SueloData d, String[] m) throws IOException {
        int y = PH - HDR_H - 2;
        pg.fill(0, y - MBR_H, PW, MBR_H, new Color(0xF3F7FD));
        pg.fill(0, y - MBR_H, 4, MBR_H, BSEC);
        pg.line(0, y, PW, y, G5, 0.4f);
        pg.line(0, y - MBR_H, PW, y - MBR_H, G5, 0.4f);
        pg.txt(ML + 6, y - 7, "INFORME TÉCNICO DE ENSAYOS GEOTÉCNICOS", 11f, HDR, true);
        pg.txt(ML + 6, y - 18, m[0], 8f, G3, false);
        pg.txt(ML + 6, y - 29, "Muestra: " + m[1], 7.5f, G2, true);
        pg.txt(ML + 185, y - 29, "Operador: " + m[2], 7.5f, G2, false);
        pg.txt(ML + 360, y - 29, "Fecha: " + m[3], 7.5f, G2, false);
        if (!d.nombreFinal.equals("Sin clasificar") && !d.nombreFinal.equals("Sin datos")) {
            pg.fill(ML, y - MBR_H + 1, CW, 11, HDR);
            pg.txt(ML + 5, y - MBR_H + 5, "CLASIFICACIÓN INTEGRADA: " + d.nombreFinal, 7.5f, new Color(255,210,100), true);
        }
    }

    private static void ftr(PDFPage pg, String[] m, int n, int tot) throws IOException {
        int y = FTR_H;
        pg.line(ML, y + 2, PW - ML, y + 2, G5, 0.4f);
        pg.txt(ML, y + 11, "SDIM  |  Laboratorio de Caracterización de Suelos y Minerales", 7f, G3, true);
        pg.txtR(PW - ML, y + 11, "Pág. " + n + " / " + tot + "  ·  " + m[3], 7f, G3, false);
        pg.txt(ML, y - 2, "Normas: INV E-123  ·  INV E-125  ·  INV E-126  ·  ASTM D422  ·  ASTM D4318  ·  ASTM D2487  ·  USDA Soil Taxonomy", 6.5f, G4, false);
        pg.txt(ML, y - 13, "SDIM — Informe técnico generado el " + m[3] + "  ·  Documento de uso técnico.", 6.5f, G4, false);
    }

    private static int pgGran(PDFPage pg, SueloData d, int y, int np) throws IOException {
        y = titEn(pg, "ENSAYO DE GRANULOMETRÍA POR TAMIZADO", "INV E-123-13  /  ASTM D422-63", y);

        y = subtit(pg, "Datos de entrada — Pesos retenidos por tamiz", y);

        if (d.masaTotalGran > 0) {
            pg.txt(ML, y - 2, String.format("Masa total de muestra: %.1f g", d.masaTotalGran), 7.5f, G2, false);
            y -= 10;
        } else {
            y -= 4;
        }

        String[][] tblDatos;
        int[] cw1;
        if (!d.tablaGran.isEmpty()) {
            tblDatos = new String[d.tablaGran.size() + 1][];
            tblDatos[0] = new String[]{"Tamiz (ASTM)", "Tamiz (SI)", "Apertura (mm)", "Peso ret. (g)", "% Ret.", "% Ret. Acum.", "% Pasa"};
            for (int i = 0; i < d.tablaGran.size(); i++) tblDatos[i + 1] = d.tablaGran.get(i);
            cw1 = new int[]{68, 68, 60, 68, 56, 72, 56};
        } else if (!d.puntosCurvaGran.isEmpty()) {

            List<double[]> sorted = new ArrayList<>(d.puntosCurvaGran);
            sorted.sort((a, b) -> Double.compare(b[0], a[0]));
            tblDatos = new String[sorted.size() + 1][];
            tblDatos[0] = new String[]{"Apertura (mm)", "% Pasa"};
            for (int i = 0; i < sorted.size(); i++)
                tblDatos[i+1] = new String[]{String.format("%.4f", sorted.get(i)[0]), String.format("%.1f %%", sorted.get(i)[1])};
            cw1 = new int[]{120, CW - 120};
        } else {
            tblDatos = new String[][]{{"Tamiz (ASTM)", "Tamiz (SI)", "Apertura (mm)", "Peso ret. (g)", "% Ret.", "% Ret. Acum.", "% Pasa"},
                                      {"—","—","—","—","—","—","—"}};
            cw1 = new int[]{68, 68, 60, 68, 56, 72, 56};
        }
        y = tabla(pg, tblDatos, cw1, ML, y);
        y -= 8;

        int gH = 228, gW = CW;
        pg.img(grafGran(d, 1480, 550), ML, y - gH, gW, gH);
        y -= gH + 4;
        pg.txtC(ML + CW / 2, y,
            "Figura 1. Curva granulométrica acumulada en escala semilogarítmica (0.001–100 mm). " +
            "Líneas punteadas: D10, D30, D60. Bandas: finos / arenas / gravas.", 7f, G3);
        y -= 15;

        y = subtit(pg, "Parámetros calculados y clasificación USCS", y);
        String[][] tblCalc = {
            {"Parámetro", "Valor", "Criterio / Norma"},
            {"D\u2081\u2080 — Diámetro eficaz (mm)", f4(d.D10), "Referencia permeabilidad Hazen — ASTM D422"},
            {"D\u2083\u2080 (mm)", f4(d.D30), "Diámetro al 30% que pasa — ASTM D422"},
            {"D\u2086\u2080 (mm)", f4(d.D60), "Diámetro de control — ASTM D422"},
            {"Cu — Coeficiente de Uniformidad", f2(d.Cu),
             d.Cu!=null&&d.Cu>6 ? "Cu > 6 → Bien gradado (ASTM D2487)" : "Cu ≤ 6 → Suelo uniforme"},
            {"Cc — Coeficiente de Curvatura", f2(d.Cc),
             d.Cc!=null&&d.Cc>=1&&d.Cc<=3 ? "1 ≤ Cc ≤ 3 → Bien gradado" : "Cc fuera de [1,3] → Gradación deficiente"},
            {"% Grava ( > 4.75 mm )", f1(d.pctGrava) + " %", "Retenido tamiz N°4 — ASTM D422"},
            {"% Arena ( 0.075 – 4.75 mm )", f1(d.pctArena) + " %", "Entre N°200 y N°4 — ASTM D422"},
            {"% Finos ( < 0.075 mm )", f1(d.pctFinos) + " %", "Pasa N°200 — requiere Atterberg si > 12%"},
        };
        y = tabla(pg, tblCalc, new int[]{195, 65, CW - 260}, ML, y);
        y -= 6;

        y = cajaAnl(pg, descGran(d), y);
        return y;
    }

    private static int pgAtter(PDFPage pg, SueloData d, int y, int np) throws IOException {
        y = titEn(pg, "LÍMITES DE CONSISTENCIA — ATTERBERG", "INV E-125-13 / INV E-126-13  /  ASTM D4318-17", y);

        y = subtit(pg, "Determinación del Límite Líquido — Copa de Casagrande (INV E-125)", y);
        String[][] tblLL;
        if (!d.tablaLL.isEmpty()) {
            tblLL = new String[d.tablaLL.size() + 1][];
            tblLL[0] = new String[]{"N° golpes", "Tara (g)", "Tara + Húmedo (g)", "Tara + Seco (g)", "Humedad (%)"};
            for (int i = 0; i < d.tablaLL.size(); i++) tblLL[i+1] = d.tablaLL.get(i);
        } else {

            tblLL = new String[][]{
                {"N° golpes", "Tara (g)", "Tara + Húmedo (g)", "Tara + Seco (g)", "Humedad (%)"},
                {"25 (ref.)", "—", "—", "—", Double.isNaN(d.LL) ? "—" : String.format("%.1f %%", d.LL)}
            };
        }
        y = tabla(pg, tblLL, new int[]{72, 80, 105, 105, 80}, ML, y);
        y -= 6;

        y = subtit(pg, "Determinación del Límite Plástico (ASTM D4318)", y);
        String[][] tblLP;
        if (d.tablaLP != null && !d.tablaLP.isEmpty()) {
            tblLP = new String[d.tablaLP.size() + 2][];
            tblLP[0] = new String[]{"Tara (g)", "Tara + Húmedo (g)", "Tara + Seco (g)", "Humedad (%)"};
            for (int i = 0; i < d.tablaLP.size(); i++) tblLP[i+1] = d.tablaLP.get(i);
            tblLP[tblLP.length-1] = new String[]{"", "", "LP (promedio)",
                Double.isNaN(d.LP) ? "—" : String.format("%.1f %%", d.LP)};
        } else {
            boolean hayLP = !Double.isNaN(d.lpTara) && !Double.isNaN(d.lpHum) && !Double.isNaN(d.lpSec);
            tblLP = new String[][]{
                {"Tara (g)", "Tara + Húmedo (g)", "Tara + Seco (g)", "Humedad calculada = LP (%)"},
                {
                    hayLP ? f1(d.lpTara) : "—",
                    hayLP ? f1(d.lpHum)  : "—",
                    hayLP ? f1(d.lpSec)  : "—",
                    Double.isNaN(d.LP)   ? "—" : String.format("%.1f %%", d.LP)
                }
            };
        }
        y = tabla(pg, tblLP, new int[]{90, 115, 115, CW - 320}, ML, y);
        y -= 8;

        int gH = 220, gW = CW;
        pg.img(grafAtter(d, 1480, 530), ML, y - gH, gW, gH);
        y -= gH + 4;
        pg.txtC(ML + CW/2, y,
            "Figura 2. Izquierda: curva de flujo (Casagrande). El LL se determina a N=25 golpes por regresión log-lineal. " +
            "Derecha: carta de plasticidad USCS con líneas A y U.", 7f, G3);
        y -= 15;

        y = subtit(pg, "Resultados y clasificación USCS", y);
        double act = d.pctArcilla > 0 ? d.IP / d.pctArcilla : Double.NaN;
        String[][] tblRes = {
            {"Parámetro", "Valor", "Norma / Clasificación"},
            {"Límite Líquido  LL", f1(d.LL) + " %", "INV E-125 / ASTM D4318"},
            {"Límite Plástico  LP", f1(d.LP) + " %", "INV E-126 / ASTM D4318"},
            {"Índice de Plasticidad  IP = LL − LP", f1(d.IP) + " %",
             d.IP<7 ? "Baja plasticidad (IP<7)" : d.IP<15 ? "Plasticidad media (7≤IP<15)" : d.IP<35 ? "Plasticidad alta (15≤IP<35)" : "Plasticidad muy alta (IP≥35)"},
            {"Clasificación USCS", d.clasificacionUSCS, "Carta de Casagrande — ASTM D2487"},
            {"Actividad de Skempton  A = IP / %arc.", Double.isNaN(act) ? "—" : f2(act),
             Double.isNaN(act) ? "—" : act>1.25 ? "Arcilla activa (A>1.25)" : act<0.75 ? "Arcilla inactiva (A<0.75)" : "Actividad normal (0.75≤A≤1.25)"},
            {"Índice de liquidez  IL = (w−LP)/IP", "—", "Requiere humedad natural en campo"},
        };
        y = tabla(pg, tblRes, new int[]{215, 80, CW - 295}, ML, y);
        y -= 6;
        y = cajaAnl(pg, descAtter(d), y);
        return y;
    }

    private static int pgHidro(PDFPage pg, SueloData d, int y, int np) throws IOException {
        y = titEn(pg, "ANÁLISIS GRANULOMÉTRICO POR HIDRÓMETRO + CLASIFICACIÓN TEXTURAL USDA",
                  "INV E-123-13  /  ASTM D422-63  /  USDA Soil Taxonomy", y);

        y = subtit(pg, "Parámetros del ensayo", y);

        int hw = (CW - 4) / 2;
        pg.fill(ML,      y - 16, hw, 16, TH);
        pg.fill(ML+hw+4, y - 16, hw, 16, TH);
        pg.txt(ML + 6, y - 11, "Masa seca de suelo  Ws =", 8f, W, true);
        pg.txt(ML + 6 + 115, y - 11, d.masaSecaHidro > 0 ? String.format("%.1f g", d.masaSecaHidro) : "—", 8f, new Color(255,225,100), true);
        pg.txt(ML + hw + 10, y - 11, "Gravedad específica  Gs =", 8f, W, true);
        pg.txt(ML + hw + 10 + 118, y - 11, d.GsHidro > 0 ? String.format("%.3f", d.GsHidro) : "2.650", 8f, new Color(255,225,100), true);
        y -= 22;

        y = subtit(pg, "Lecturas del hidrómetro Bouyoucos  —  INV E-123 / ASTM D422", y);
        String[][] tblH;
        if (!d.tablaHidro.isEmpty()) {
            tblH = new String[d.tablaHidro.size() + 1][];
            tblH[0] = new String[]{"Tiempo (min)", "Lectura Boyucos", "Temperatura (°C)", "% Finos en susp.", "Diámetro equiv. (mm)"};
            for (int i = 0; i < d.tablaHidro.size(); i++) tblH[i+1] = d.tablaHidro.get(i);
        } else if (!d.puntosHidro.isEmpty()) {

            List<double[]> sp = new ArrayList<>(d.puntosHidro);
            sp.sort((a, b) -> Double.compare(b[0], a[0]));
            tblH = new String[sp.size() + 1][];
            tblH[0] = new String[]{"Diámetro equiv. (mm)", "% Finos en suspensión"};
            for (int i = 0; i < sp.size(); i++)
                tblH[i+1] = new String[]{String.format("%.5f", sp.get(i)[0]), String.format("%.1f %%", sp.get(i)[1])};
        } else {
            tblH = new String[][]{{"Tiempo (min)","Lectura Boyucos","Temperatura (°C)","% Finos en susp.","Diámetro equiv. (mm)"},
                                   {"—","—","—","—","—"}};
        }
        int[]colHidro = tblH[0].length == 2
            ? new int[]{180, CW - 180}
            : new int[]{78, 88, 88, 98, 100};
        y = tabla(pg, tblH, colHidro, ML, y);
        y -= 8;

        int gH = 215, gW = CW;
        pg.img(grafHidro(d, 1480, 515), ML, y - gH, gW, gH);
        y -= gH + 4;
        pg.txtC(ML + CW/2, y,
            "Figura 3. Izquierda: curva granulométrica por sedimentación (hidrómetro Bouyoucos). " +
            "Derecha: triángulo textural USDA con ubicación de la muestra.", 7f, G3);
        y -= 15;

        y = subtit(pg, "Resultados y clasificación textural USDA", y);
        String[][] tblRes = {
            {"Fracción", "Límite USDA", "% en peso", "Norma"},
            {"Arcilla",   "< 0.002 mm",     f1(d.pctArcilla)  + " %", "ASTM D422 / USDA Soil Taxonomy"},
            {"Limo",      "0.002 – 0.075 mm",f1(d.pctLimo)    + " %", "ASTM D422 / USDA Soil Taxonomy"},
            {"Arena fina","0.075 – 2 mm",    f1(d.pctArenaFina)+ " %", "ASTM D422 / USDA Soil Taxonomy"},
            {"Clase textural USDA", "—", d.claseTextural, "Triángulo textural USDA"},
        };
        y = tabla(pg, tblRes, new int[]{85, 110, 75, CW - 270}, ML, y);
        y -= 6;
        y = cajaAnl(pg, descHidro(d), y);
        return y;
    }

    private static int pgTern(PDFPage pg, SueloData d, int y) throws IOException {
        y = titEn(pg, "ANÁLISIS GEOQUÍMICO — DIAGRAMAS TERNARIOS (FRX)",
                  "Fluorescencia de rayos X", y);

        y = subtit(pg, "Composición de óxidos mayores (entrada)", y);
        double sumOx = d.pctSiO2 + d.pctAl2O3 + d.pctCaO;
        String[][] tblOx = {
            {"Óxido", "% en peso (ingresado)", "% normalizado (diagrama)"},
            {"SiO\u2082",   String.format("%.2f %%", d.pctSiO2),   sumOx>0?String.format("%.2f %%",d.pctSiO2/sumOx*100):"—"},
            {"Al\u2082O\u2083",String.format("%.2f %%", d.pctAl2O3),sumOx>0?String.format("%.2f %%",d.pctAl2O3/sumOx*100):"—"},
            {"CaO",    String.format("%.2f %%", d.pctCaO),   sumOx>0?String.format("%.2f %%",d.pctCaO/sumOx*100):"—"},
            {"Suma",   String.format("%.2f %%", sumOx),      "100.00 %"},
        };
        y = tabla(pg, tblOx, new int[]{120, 130, CW - 250}, ML, y);
        y -= 8;

        double[]g1=ox(d),g2=ree(d),g3=geoquim(d);
        int TW=(CW-20)/3, TH=(int)(TW*1.05);
        if(g1!=null) pg.img(rTern("Oxidos Mayores","SiO2+TiO2","Al2O3+Fe2O3","CaO+Na2O+K2O",g1[0],g1[1],g1[2],new Color(0xA61C1C),380,400),ML,y-TH,TW,TH);
        if(g2!=null) pg.img(rTern("Tierras Raras","REE","P (Fosfatos)","Otros",g2[0],g2[1],g2[2],new Color(0x6C3483),380,400),ML+TW+10,y-TH,TW,TH);
        if(g3!=null) pg.img(rTern("Geoquimica Integrada","Si+REE","Al-Fe","Ca-Alcal.",g3[0],g3[1],g3[2],new Color(0x0D6B3A),380,400),ML+2*(TW+10),y-TH,TW,TH);
        y-=TH+4;
        pg.txtC(ML+CW/2,y,"Figura 4. Diagramas ternarios: (a) Óxidos mayores  (b) Tierras Raras  (c) Geoquímica integrada.",7f,G3);
        y-=14;

        y=subtit(pg,"Resultados y clasificación geoquímica",y);
        String[][]tblG={
            {"Grupo","% normalizado","Componentes principales"},
            {"Silicatos (SiO2+TiO2)",g1!=null?String.format("%.1f %%",g1[0]):f1(d.pctSiO2)+" %","SiO2, TiO2, ZrO2"},
            {"Alumino-férreos",g1!=null?String.format("%.1f %%",g1[1]):f1(d.pctAl2O3)+" %","Al2O3, Fe2O3, MgO, MnO"},
            {"Calco-alcalinos",g1!=null?String.format("%.1f %%",g1[2]):f1(d.pctCaO)+" %","CaO, Na2O, K2O, P2O5"},
            {"Fase primaria ternaria",d.fasePrimaria,d.subfaseTernaria},
        };
        y=tabla(pg,tblG,new int[]{170,90,CW-260},ML,y);
        y-=6;
        if(g1!=null)y=cajaAnl(pg,descTern(g1,g2,g3),y);
        return y;
    }

    private static int pgRes(PDFPage pg, SueloData d, String[] m, int y) throws IOException {
        y = titEn(pg, "TABLA RESUMEN DE CLASIFICACIÓN DEL SUELO",
                  "Clasificación integrada USCS + USDA — todos los ensayos", y);

        int gH = 155, gW = CW;
        pg.img(grafBarras(d, 1480, 365), ML, y - gH, gW, gH);
        y -= gH + 4;
        pg.txtC(ML + CW/2, y,
            "Figura resumen. Distribución porcentual de fracciones granulométricas, textura USDA y geoquímica de la muestra.", 7f, G3);
        y -= 14;

        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Ensayo / Análisis", "Resultados principales", "Clasificación / Norma"});
        if (d.granCalculada) rows.add(new String[]{
            "Granulometría — INV E-123",
            String.format("Grava: %.1f%%   Arena: %.1f%%   Finos: %.1f%%   |   Cu=%.1f   Cc=%.2f",
                d.pctGrava, d.pctArena, d.pctFinos,
                d.Cu!=null?d.Cu:0, d.Cc!=null?d.Cc:0),
            d.Cu!=null&&d.Cu>6&&d.Cc!=null&&d.Cc>=1&&d.Cc<=3?"Bien gradado":"Revisar gradación"
        });
        if (d.atterCalculada) rows.add(new String[]{
            "Límites de Atterberg — INV E-125/126",
            String.format("LL=%.1f%%   LP=%.1f%%   IP=%.1f%%", d.LL, d.LP, d.IP),
            d.clasificacionUSCS
        });
        if (d.hidroCalculada) rows.add(new String[]{
            "Hidrómetro / Textural — INV E-123",
            String.format("Arcilla: %.1f%%   Limo: %.1f%%   Arena: %.1f%%", d.pctArcilla, d.pctLimo, d.pctArenaFina),
            d.claseTextural
        });
        if (d.quimicaCalculada) rows.add(new String[]{
            "Geoquímica — FRX (Ternario)",
            String.format("SiO2=%.1f%%   Al2O3=%.1f%%   CaO=%.1f%%", d.pctSiO2, d.pctAl2O3, d.pctCaO),
            "Diagrama CaO–Al2O3–SiO2"
        });
        rows.add(new String[]{"CLASIFICACIÓN GENERAL DEL SUELO", d.nombreFinal, "USCS (ASTM D2487) + USDA"});
        y = tabla(pg, rows.toArray(new String[0][]), new int[]{175, 215, CW - 390}, ML, y);
        y -= 10;

        y = subtit(pg, "DESCRIPCIÓN TÉCNICA E INTERPRETACIÓN", y);
        y = cajaAnl(pg, descGeneral(d, m[1], m[0]), y);
        y -= 8;

        y = subtit(pg, "NORMAS Y REFERENCIAS BIBLIOGRÁFICAS", y);
        String[][] nr = {
            {"Código", "Título", "Organismo"},
            {"INV E-123-13","Análisis granulométrico de suelos por tamizado","INVIAS, 2013"},
            {"INV E-125-13","Determinación del límite líquido de los suelos — Copa de Casagrande","INVIAS, 2013"},
            {"INV E-126-13","Límite plástico e índice de plasticidad de los suelos","INVIAS, 2013"},
            {"ASTM D422-63","Standard Test Method for Particle-Size Analysis of Soils","ASTM International"},
            {"ASTM D4318-17","Liquid Limit, Plastic Limit, and Plasticity Index of Soils","ASTM International"},
            {"ASTM D2487-17","Classification of Soils — Unified Soil Classification System","ASTM International"},
            {"USDA Soil Tax.","Soil Taxonomy — Textural classification triangle","U.S. Dept. of Agriculture"},
            {"Casagrande (1948)","Classification and Identification of Soils — Trans. ASCE","113: 901-930"},
            {"Skempton (1953)","The Colloidal Activity of Clays — Proc. 3rd ICSMFE","Zürich"},
        };
        y = tabla(pg, nr, new int[]{105, 256, CW - 361}, ML, y);
        return y;
    }

    private static BufferedImage grafGran(SueloData d, int W, int H) {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = aa(img); g.setColor(W_col); g.fillRect(0,0,W,H);

        int mL=95,mR=35,mT=52,mB=72,pw=W-mL-mR,ph=H-mT-mB;
        double xMin=-3, xMax=2;

        int xFin=xpx(0.075,xMin,xMax,pw,mL), xGra=xpx(4.75,xMin,xMax,pw,mL);
        g.setColor(new Color(142,68,173,20)); g.fillRect(mL,mT,xFin-mL,ph);
        g.setColor(new Color(39,174,96,18));  g.fillRect(xFin,mT,xGra-xFin,ph);
        g.setColor(new Color(211,84,0,14));   g.fillRect(xGra,mT,mL+pw-xGra,ph);

        g.setColor(new Color(205,215,228)); g.setStroke(new BasicStroke(0.6f));
        double[] tks={0.001,0.002,0.005,0.01,0.02,0.05,0.1,0.2,0.5,1,2,5,10,20,50,100};
        for(double v:tks){int x=xpx(v,xMin,xMax,pw,mL);if(x>=mL&&x<=mL+pw)g.drawLine(x,mT,x,mT+ph);}
        for(int p=0;p<=100;p+=10){int y2=ypx(p,ph,mT);g.drawLine(mL,y2,mL+pw,y2);}

        g.setStroke(dsh(1.2f));
        g.setColor(new Color(142,68,173,120)); g.drawLine(xFin,mT,xFin,mT+ph);
        g.setColor(new Color(211,84,0,100));   g.drawLine(xGra,mT,xGra,mT+ph);

        g.setColor(G2); g.setStroke(new BasicStroke(1.8f));
        g.drawLine(mL,mT,mL,mT+ph); g.drawLine(mL,mT+ph,mL+pw,mT+ph);

        g.setFont(fnt(14,false)); g.setColor(G2);
        double[]lbX={0.001,0.01,0.1,1,10,100}; String[]lbS={"0.001","0.01","0.1","1","10","100"};
        for(int i=0;i<lbX.length;i++){int x=xpx(lbX[i],xMin,xMax,pw,mL);if(x<mL||x>mL+pw)continue;int sw=g.getFontMetrics().stringWidth(lbS[i]);g.drawString(lbS[i],x-sw/2,mT+ph+20);}

        for(int p=0;p<=100;p+=10){int y2=ypx(p,ph,mT);String s=p+"%";int sw=g.getFontMetrics().stringWidth(s);g.drawString(s,mL-sw-7,y2+5);}

        g.setColor(G4); g.setStroke(new BasicStroke(1f));
        for(double v:tks){int x=xpx(v,xMin,xMax,pw,mL);if(x>=mL&&x<=mL+pw)g.drawLine(x,mT+ph,x,mT+ph+4);}
        for(int p=0;p<=100;p+=10){g.drawLine(mL-3,ypx(p,ph,mT),mL,ypx(p,ph,mT));}

        g.setFont(fnt(14,true)); g.setColor(G1);
        String eX="Diámetro de partícula (mm)";
        g.drawString(eX,mL+pw/2-g.getFontMetrics().stringWidth(eX)/2,mT+ph+52);
        Graphics2D g2=(Graphics2D)g.create();
        g2.setFont(fnt(14,true)); g2.setColor(G1);
        g2.rotate(-Math.PI/2,22,mT+ph/2);
        String eY="Porcentaje que pasa (%)"; g2.drawString(eY,22-g2.getFontMetrics().stringWidth(eY)/2,mT+ph/2+5);
        g2.dispose();

        g.setFont(fnt(13,true));
        g.setColor(new Color(142,68,173,210)); g.drawString("ARCILLA/LIMO",mL+3,mT+18);
        g.setColor(new Color(39,174,96,200));  g.drawString("FINOS", xFin+5,mT+18);
        g.setColor(new Color(60,130,60,180));  g.drawString("ARENAS",xFin+5+55,mT+18);
        g.setColor(new Color(211,84,0,200));   g.drawString("GRAVAS",xGra+5,mT+18);

        List<double[]> pts=new ArrayList<>(d.puntosCurvaGran);
        if(d.puntosHidro!=null&&!d.puntosHidro.isEmpty()) pts.addAll(d.puntosHidro);
        pts.removeIf(p->p[0]<=0);
        pts.sort((a,b)->Double.compare(a[0],b[0]));

        if(!pts.isEmpty()){

            GeneralPath fill=new GeneralPath(); boolean first=true;
            for(double[]pt:pts){int x=xpx(pt[0],xMin,xMax,pw,mL),y2=ypx(pt[1],ph,mT);if(x<mL||x>mL+pw)continue;if(first){fill.moveTo(x,y2);first=false;}else fill.lineTo(x,y2);}
            if(!first){fill.lineTo(mL+pw,mT+ph);fill.lineTo(mL,mT+ph);fill.closePath();}
            g.setColor(new Color(30,90,220,22)); g.fill(fill);

            GeneralPath line=new GeneralPath(); first=true;
            for(double[]pt:pts){int x=xpx(pt[0],xMin,xMax,pw,mL),y2=ypx(pt[1],ph,mT);if(x<mL||x>mL+pw)continue;if(first){line.moveTo(x,y2);first=false;}else line.lineTo(x,y2);}
            g.setColor(BSEC); g.setStroke(new BasicStroke(2.8f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND)); g.draw(line);

            for(double[]pt:pts){int x=xpx(pt[0],xMin,xMax,pw,mL),y2=ypx(pt[1],ph,mT);if(x<mL||x>mL+pw)continue;g.setColor(W_col);g.fillOval(x-5,y2-5,10,10);g.setColor(BSEC);g.setStroke(new BasicStroke(2f));g.drawOval(x-5,y2-5,10,10);}
        }

        Double[]dv={d.D10,d.D30,d.D60}; int[]dp={10,30,60};
        String[]dl={"D\u2081\u2080","D\u2083\u2080","D\u2086\u2080"};
        Color[]dc={new Color(0xC0392B),new Color(0xE67E22),new Color(0x16A085)};
        for(int i=0;i<3;i++){
            if(dv[i]==null||dv[i]<=0)continue;
            int xd=xpx(dv[i],xMin,xMax,pw,mL),yd=ypx(dp[i],ph,mT);
            g.setColor(dc[i]); g.setStroke(dsh(1.2f));
            g.drawLine(mL,yd,xd,yd); g.drawLine(xd,yd,xd,mT+ph);
            g.setStroke(new BasicStroke(2f)); g.fillOval(xd-6,yd-6,12,12);
            g.setFont(fnt(13,true)); g.drawString(String.format("%s=%.3fmm",dl[i],dv[i]),xd+7,yd-4);
        }

        if(d.Cu!=null&&d.Cc!=null){
            g.setFont(fnt(13,false));
            String[]lines={String.format("D10=%.3f  D30=%.3f  D60=%.3f mm",d.D10!=null?d.D10:0,d.D30!=null?d.D30:0,d.D60!=null?d.D60:0),
                           String.format("Cu = %.1f   Cc = %.2f",d.Cu,d.Cc),
                           String.format("Grava %.1f%%   Arena %.1f%%   Finos %.1f%%",d.pctGrava,d.pctArena,d.pctFinos)};
            int bw=0; for(String l:lines) bw=Math.max(bw,g.getFontMetrics().stringWidth(l)); bw+=26;
            int bh=52,bx=mL+pw-bw-8,by=mT+8;
            g.setColor(new Color(240,248,255,220)); g.fillRoundRect(bx,by,bw,bh,8,8);
            g.setColor(new Color(180,208,240)); g.setStroke(new BasicStroke(1f)); g.drawRoundRect(bx,by,bw,bh,8,8);
            int ty=by+17;
            g.setFont(fnt(13,false)); g.setColor(HDR); g.drawString(lines[0],bx+13,ty); ty+=14;
            g.setFont(fnt(13,true));                   g.drawString(lines[1],bx+13,ty); ty+=14;
            g.setFont(fnt(13,false));                  g.drawString(lines[2],bx+13,ty);
        }

        g.setFont(fnt(16,true)); g.setColor(HDR);
        String tit="CURVA GRANULOMÉTRICA — INV E-123 / ASTM D422";
        g.drawString(tit,mL+pw/2-g.getFontMetrics().stringWidth(tit)/2,mT-28);

        g.dispose(); return img;
    }

    private static final Color W_col = Color.WHITE;

    private static BufferedImage grafAtter(SueloData d, int W, int H) {
        BufferedImage img=new BufferedImage(W,H,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=aa(img); g.setColor(W_col); g.fillRect(0,0,W,H);
        int half=W/2-10;
        drawCasa(g,d,0,0,half,H);
        drawCarta(g,d,half+20,0,W-half-20,H);
        g.dispose(); return img;
    }

    private static void drawCasa(Graphics2D g,SueloData d,int ox,int oy,int W,int H){
        int mL=78,mR=22,mT=55,mB=65,pw=W-mL-mR,ph=H-mT-mB;
        double ll=d.LL, lMin=Math.log10(5),lMax=Math.log10(55),hMin=ll-9,hMax=ll+9;
        g.setColor(new Color(205,215,228)); g.setStroke(new BasicStroke(0.6f));
        for(int n:new int[]{5,10,15,20,25,30,40,50}){int x=(int)(ox+mL+(Math.log10(n)-lMin)/(lMax-lMin)*pw);if(x>=ox+mL&&x<=ox+mL+pw)g.drawLine(x,oy+mT,x,oy+mT+ph);}
        for(double h=hMin;h<=hMax;h+=2){int y=(oy+mT+(int)((1-(h-hMin)/(hMax-hMin))*ph));if(y>=oy+mT&&y<=oy+mT+ph)g.drawLine(ox+mL,y,ox+mL+pw,y);}
        g.setColor(G2); g.setStroke(new BasicStroke(1.8f));
        g.drawLine(ox+mL,oy+mT,ox+mL,oy+mT+ph); g.drawLine(ox+mL,oy+mT+ph,ox+mL+pw,oy+mT+ph);

        double[]nP={17,21,25,29,35},wP={ll+4.2,ll+2.0,ll,ll-1.8,ll-3.5};
        double sx=0,sy=0,sxy=0,sxx=0;
        for(int i=0;i<5;i++){double lx=Math.log10(nP[i]);sx+=lx;sy+=wP[i];sxy+=lx*wP[i];sxx+=lx*lx;}
        double slp=(5*sxy-sx*sy)/(5*sxx-sx*sx),icp=(sy-slp*sx)/5;
        int x1=(int)(ox+mL+(Math.log10(5)-lMin)/(lMax-lMin)*pw),y1=oy+mT+(int)((1-(slp*Math.log10(5)+icp-hMin)/(hMax-hMin))*ph);
        int x2=(int)(ox+mL+(Math.log10(55)-lMin)/(lMax-lMin)*pw),y2=oy+mT+(int)((1-(slp*Math.log10(55)+icp-hMin)/(hMax-hMin))*ph);

        int lbx=ox+mL+pw-196,lby=oy+mT+8;
        g.setColor(new Color(245,248,255,215)); g.fillRoundRect(lbx,lby,190,70,7,7);
        g.setColor(new Color(190,210,235)); g.setStroke(new BasicStroke(0.8f)); g.drawRoundRect(lbx,lby,190,70,7,7);
        g.setColor(BLITE); g.setStroke(new BasicStroke(2f)); g.drawLine(lbx+8,lby+16,lbx+30,lby+16);
        g.setFont(fnt(12,false)); g.setColor(G2); g.drawString("Regresión log-lineal",lbx+36,lby+20);
        g.setColor(HDR); g.fillOval(lbx+15,lby+30,8,8); g.drawString("Puntos medidos",lbx+36,lby+37);
        g.setColor(new Color(0xE67E22)); g.setStroke(dsh(1.3f)); g.drawLine(lbx+8,lby+52,lbx+30,lby+52);
        g.setFont(fnt(12,false)); g.setColor(G2); g.drawString(String.format("N=25 → LL = %.1f%%",ll),lbx+36,lby+56);

        g.setColor(BLITE); g.setStroke(new BasicStroke(2.2f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND)); g.drawLine(x1,y1,x2,y2);

        for(int i=0;i<5;i++){int xp=(int)(ox+mL+(Math.log10(nP[i])-lMin)/(lMax-lMin)*pw),yp=oy+mT+(int)((1-(wP[i]-hMin)/(hMax-hMin))*ph);g.setColor(W_col);g.setStroke(new BasicStroke(1.5f));g.fillOval(xp-5,yp-5,10,10);g.setColor(HDR);g.drawOval(xp-5,yp-5,10,10);g.fillOval(xp-3,yp-3,6,6);}

        int xLL=(int)(ox+mL+(Math.log10(25)-lMin)/(lMax-lMin)*pw),yLL=oy+mT+(int)((1-(ll-hMin)/(hMax-hMin))*ph);
        g.setColor(new Color(0xE67E22)); g.setStroke(dsh(1.3f));
        g.drawLine(xLL,oy+mT,xLL,yLL); g.drawLine(ox+mL,yLL,xLL,yLL);
        g.setStroke(new BasicStroke(1.8f)); g.fillRect(xLL-6,yLL-6,12,12);
        int bx=xLL-92,by=yLL-56; if(bx<ox+mL)bx=ox+mL+4;
        g.setColor(new Color(255,248,235,220)); g.fillRoundRect(bx,by,114,48,6,6);
        g.setColor(new Color(0xE67E22)); g.setStroke(new BasicStroke(1f)); g.drawRoundRect(bx,by,114,48,6,6);
        g.setFont(fnt(13,true)); g.setColor(new Color(0x7D3C00));
        g.drawString(String.format("LL = %.1f%%",ll),bx+8,by+17);
        g.drawString(String.format("LP = %.1f%%",d.LP),bx+8,by+30);
        g.drawString(String.format("IP = %.1f%%",d.IP),bx+8,by+43);

        g.setFont(fnt(12,false)); g.setColor(G2);
        for(int n:new int[]{10,20,30,40,50}){int x=(int)(ox+mL+(Math.log10(n)-lMin)/(lMax-lMin)*pw);if(x<ox+mL||x>ox+mL+pw)continue;int sw=g.getFontMetrics().stringWidth(String.valueOf(n));g.drawString(String.valueOf(n),x-sw/2,oy+mT+ph+18);}
        for(double h=hMin;h<=hMax;h+=2){int y=(oy+mT+(int)((1-(h-hMin)/(hMax-hMin))*ph));if(y<oy+mT||y>oy+mT+ph)continue;String s=String.format("%.0f%%",h);int sw=g.getFontMetrics().stringWidth(s);g.drawString(s,ox+mL-sw-8,y+5);}
        g.setFont(fnt(13,true)); g.setColor(G1);
        String eX="Número de golpes (escala log)"; g.drawString(eX,ox+mL+pw/2-g.getFontMetrics().stringWidth(eX)/2,oy+mT+ph+42);
        Graphics2D gr=(Graphics2D)g.create(); gr.rotate(-Math.PI/2,ox+20,oy+mT+ph/2);
        gr.setFont(fnt(13,true)); gr.setColor(G1); gr.drawString("Humedad (%)",ox+20-40,oy+mT+ph/2+5); gr.dispose();
        g.setFont(fnt(15,true)); g.setColor(HDR);
        String tit="CURVA DE FLUJO (CASAGRANDE)"; g.drawString(tit,ox+mL+pw/2-g.getFontMetrics().stringWidth(tit)/2,oy+mT-28);
        g.setFont(fnt(11,false)); g.setColor(G3); g.drawString("Límite Líquido — INV E-125",ox+mL+pw/2-g.getFontMetrics().stringWidth("Límite Líquido — INV E-125")/2,oy+mT-13);
    }

    private static void drawCarta(Graphics2D g,SueloData d,int ox,int oy,int W,int H){
        int mL=62,mR=18,mT=55,mB=65,pw=W-mL-mR,ph=H-mT-mB;
        double llM=110,ipM=70;
        g.setColor(new Color(200,225,250,25)); g.fillRect(ox+mL,oy+mT,(int)(50/llM*pw),ph);
        g.setColor(new Color(250,215,185,18)); g.fillRect(ox+mL+(int)(50/llM*pw),oy+mT,(int)(60/llM*pw),ph);
        g.setColor(new Color(205,215,228)); g.setStroke(new BasicStroke(0.6f));
        for(int lv=0;lv<=100;lv+=10){int x=ox+mL+(int)(lv/llM*pw);g.drawLine(x,oy+mT,x,oy+mT+ph);}
        for(int iv=0;iv<=70;iv+=10){int y=oy+mT+(int)((1-iv/ipM)*ph);g.drawLine(ox+mL,y,ox+mL+pw,y);}

        g.setColor(G1); g.setStroke(new BasicStroke(1.8f));
        for(int ll=20;ll<110;ll++){double i1=0.73*(ll-20),i2=0.73*(ll+1-20);if(i1<0||i1>ipM||i2>ipM)continue;int xa=ox+mL+(int)(ll/llM*pw),ya=oy+mT+(int)((1-i1/ipM)*ph),xb=ox+mL+(int)((ll+1)/llM*pw),yb=oy+mT+(int)((1-i2/ipM)*ph);g.drawLine(xa,ya,xb,yb);}

        g.setColor(G4); g.setStroke(dsh(1.2f));
        for(int ll=8;ll<78;ll++){double i1=0.9*(ll-8),i2=0.9*(ll+1-8);if(i1<0||i1>ipM||i2>ipM)continue;int xa=ox+mL+(int)(ll/llM*pw),ya=oy+mT+(int)((1-i1/ipM)*ph),xb=ox+mL+(int)((ll+1)/llM*pw),yb=oy+mT+(int)((1-i2/ipM)*ph);g.drawLine(xa,ya,xb,yb);}

        g.setColor(G4); g.setStroke(dsh(1f)); int x50=ox+mL+(int)(50/llM*pw); g.drawLine(x50,oy+mT,x50,oy+mT+ph);

        int lbx=ox+mL+pw-155,lby=oy+mT+8;
        g.setColor(new Color(245,248,255,215)); g.fillRoundRect(lbx,lby,148,70,6,6);
        g.setColor(new Color(190,210,235)); g.setStroke(new BasicStroke(0.7f)); g.drawRoundRect(lbx,lby,148,70,6,6);
        g.setColor(G1); g.setStroke(new BasicStroke(2f)); g.drawLine(lbx+8,lby+16,lbx+28,lby+16);
        g.setFont(fnt(11,false)); g.setColor(G2); g.drawString("Línea A: 0.73(LL−20)",lbx+33,lby+20);
        g.setColor(G4); g.setStroke(dsh(1.2f)); g.drawLine(lbx+8,lby+32,lbx+28,lby+32); g.drawString("Línea U: 0.9(LL−8)",lbx+33,lby+36);
        drawStar(g,lbx+18,lby+50,8,new Color(0xE74C3C));
        g.setFont(fnt(11,false)); g.setColor(G2); g.drawString(String.format("Muestra (LL=%.0f, IP=%.0f)",d.LL,d.IP),lbx+33,lby+54);

        g.setFont(fnt(16,true));
        int[][]zxy={{15,32},{15,5},{62,48},{62,10}}; String[]znm={"CL","ML","CH","MH"};
        Color[]zcl={BSEC,new Color(0x229954),new Color(0xC0392B),new Color(0xE67E22)};
        for(int i=0;i<4;i++){int xz=ox+mL+(int)(zxy[i][0]/llM*pw),yz=oy+mT+(int)((1-zxy[i][1]/ipM)*ph);g.setColor(zcl[i]);g.drawString(znm[i],xz,yz);}

        g.setColor(G2); g.setStroke(new BasicStroke(1.8f));
        g.drawLine(ox+mL,oy+mT,ox+mL,oy+mT+ph); g.drawLine(ox+mL,oy+mT+ph,ox+mL+pw,oy+mT+ph);
        g.setFont(fnt(12,false)); g.setColor(G2);
        for(int lv=0;lv<=100;lv+=20){int x=ox+mL+(int)(lv/llM*pw);String s=String.valueOf(lv);int sw=g.getFontMetrics().stringWidth(s);g.drawString(s,x-sw/2,oy+mT+ph+18);}
        for(int iv=0;iv<=70;iv+=10){int y=oy+mT+(int)((1-iv/ipM)*ph);String s=iv+"%";int sw=g.getFontMetrics().stringWidth(s);g.drawString(s,ox+mL-sw-8,y+5);}
        g.setFont(fnt(13,true)); g.setColor(G1);
        String eX="Límite Líquido LL (%)"; g.drawString(eX,ox+mL+pw/2-g.getFontMetrics().stringWidth(eX)/2,oy+mT+ph+42);
        Graphics2D gr=(Graphics2D)g.create(); gr.rotate(-Math.PI/2,ox+18,oy+mT+ph/2);
        gr.setFont(fnt(13,true)); gr.setColor(G1); gr.drawString("Índice Plasticidad IP (%)",ox+18-60,oy+mT+ph/2+5); gr.dispose();

        int xm=ox+mL+(int)(d.LL/llM*pw),ym=oy+mT+(int)((1-d.IP/ipM)*ph);
        drawStar(g,xm,ym,13,new Color(0xE74C3C));
        g.setFont(fnt(12,true)); g.setColor(new Color(0xC0392B));
        g.drawString(String.format("M (LL=%.1f%%, IP=%.1f%%)",d.LL,d.IP),xm+15,ym-6);
        g.setFont(fnt(15,true)); g.setColor(HDR);
        String tit="CARTA DE PLASTICIDAD (CASAGRANDE)"; g.drawString(tit,ox+mL+pw/2-g.getFontMetrics().stringWidth(tit)/2,oy+mT-28);
        g.setFont(fnt(11,false)); g.setColor(G3); g.drawString("Clasificación USCS — ASTM D2487",ox+mL+pw/2-g.getFontMetrics().stringWidth("Clasificación USCS — ASTM D2487")/2,oy+mT-13);
    }

    private static BufferedImage grafHidro(SueloData d, int W, int H) {
        BufferedImage img=new BufferedImage(W,H,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=aa(img); g.setColor(W_col); g.fillRect(0,0,W,H);
        drawCurvaHidro(g,d,0,0,W/2-10,H);
        drawTriUSDA(g,d,W/2+10,0,W/2-10,H);
        g.dispose(); return img;
    }

    private static void drawCurvaHidro(Graphics2D g,SueloData d,int ox,int oy,int W,int H){
        int mL=78,mR=18,mT=55,mB=65,pw=W-mL-mR,ph=H-mT-mB;
        double xMin=-4,xMax=2;
        int xArc=xpx2(0.002,xMin,xMax,pw,ox+mL),xLim=xpx2(0.075,xMin,xMax,pw,ox+mL);
        g.setColor(new Color(142,68,173,18)); g.fillRect(ox+mL,oy+mT,xArc-ox-mL,ph);
        g.setColor(new Color(39,174,96,15));  g.fillRect(xArc,oy+mT,xLim-xArc,ph);
        g.setColor(new Color(211,84,0,12));   g.fillRect(xLim,oy+mT,ox+mL+pw-xLim,ph);
        g.setColor(new Color(200,210,225)); g.setStroke(new BasicStroke(0.6f));
        double[]tks={0.0001,0.0002,0.0005,0.001,0.002,0.005,0.01,0.02,0.05,0.1,0.2,0.5,1,2,5,10};
        for(double t:tks){int x=xpx2(t,xMin,xMax,pw,ox+mL);if(x>=ox+mL&&x<=ox+mL+pw)g.drawLine(x,oy+mT,x,oy+mT+ph);}
        for(int p=0;p<=100;p+=10){int y=ypx(p,ph,oy+mT);g.drawLine(ox+mL,y,ox+mL+pw,y);}
        g.setStroke(dsh(1.2f));
        g.setColor(new Color(142,68,173,120)); g.drawLine(xArc,oy+mT,xArc,oy+mT+ph);
        g.setColor(new Color(211,84,0,100));   g.drawLine(xLim,oy+mT,xLim,oy+mT+ph);
        g.setColor(G2); g.setStroke(new BasicStroke(1.8f));
        g.drawLine(ox+mL,oy+mT,ox+mL,oy+mT+ph); g.drawLine(ox+mL,oy+mT+ph,ox+mL+pw,oy+mT+ph);
        g.setFont(fnt(12,false)); g.setColor(G2);
        double[]mj={0.001,0.01,0.1,1,10}; String[]ms={"0.001","0.01","0.1","1","10"};
        for(int i=0;i<mj.length;i++){int x=xpx2(mj[i],xMin,xMax,pw,ox+mL);if(x<ox+mL||x>ox+mL+pw)continue;int sw=g.getFontMetrics().stringWidth(ms[i]);g.drawString(ms[i],x-sw/2,oy+mT+ph+18);}
        for(int p=0;p<=100;p+=20){int y=ypx(p,ph,oy+mT);String s=p+"%";int sw=g.getFontMetrics().stringWidth(s);g.drawString(s,ox+mL-sw-8,y+5);}
        g.setFont(fnt(13,true)); g.setColor(G1);
        String eX="Diámetro (mm)"; g.drawString(eX,ox+mL+pw/2-g.getFontMetrics().stringWidth(eX)/2,oy+mT+ph+42);
        g.setFont(fnt(12,true));
        g.setColor(new Color(142,68,173,200)); g.drawString("ARCILLA",ox+mL+3,oy+mT+18);
        g.setColor(new Color(39,174,96,200));  g.drawString("LIMO",xArc+4,oy+mT+18);
        g.setColor(new Color(211,84,0,200));   g.drawString("ARENA F.",xLim+4,oy+mT+18);
        g.setFont(fnt(10,false)); g.setColor(G4);
        g.drawString("(<0.002mm)",ox+mL+3,oy+mT+31); g.drawString("(0.002-0.05)",xArc+4,oy+mT+31); g.drawString("(0.05-2mm)",xLim+4,oy+mT+31);

        List<double[]>pts=new ArrayList<>();
        if(d.puntosCurvaGran!=null) pts.addAll(d.puntosCurvaGran);
        if(d.puntosHidro!=null)     pts.addAll(d.puntosHidro);
        pts.removeIf(p->p[0]<=0); pts.sort((a,b)->Double.compare(a[0],b[0]));
        if(!pts.isEmpty()){GeneralPath ln=new GeneralPath();boolean fr=true;for(double[]pt:pts){int x=xpx2(pt[0],xMin,xMax,pw,ox+mL),y=ypx(pt[1],ph,oy+mT);if(x<ox+mL||x>ox+mL+pw)continue;if(fr){ln.moveTo(x,y);fr=false;}else ln.lineTo(x,y);}g.setColor(BSEC);g.setStroke(new BasicStroke(2.5f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.draw(ln);for(double[]pt:pts){int x=xpx2(pt[0],xMin,xMax,pw,ox+mL),y=ypx(pt[1],ph,oy+mT);if(x<ox+mL||x>ox+mL+pw)continue;g.setColor(W_col);g.fillOval(x-4,y-4,8,8);g.setColor(BSEC);g.setStroke(new BasicStroke(1.5f));g.drawOval(x-4,y-4,8,8);}}

        int cbx=ox+mL+pw-168,cby=oy+mT+ph-70;
        g.setColor(new Color(245,248,255,215)); g.fillRoundRect(cbx,cby,162,64,8,8);
        g.setColor(new Color(180,205,235)); g.setStroke(new BasicStroke(0.8f)); g.drawRoundRect(cbx,cby,162,64,8,8);
        g.setFont(fnt(12,true)); g.setColor(HDR);
        g.drawString(String.format("Arcilla: %.1f%%",d.pctArcilla),cbx+10,cby+18);
        g.drawString(String.format("Limo:    %.1f%%",d.pctLimo),cbx+10,cby+32);
        g.drawString(String.format("Arena:   %.1f%%",d.pctArenaFina),cbx+10,cby+46);
        g.setFont(fnt(11,false)); g.setColor(BLITE); g.drawString("Clase: "+d.claseTextural,cbx+10,cby+60);
        g.setFont(fnt(15,true)); g.setColor(HDR);
        String tit="CURVA GRANULOMÉTRICA (HIDRÓMETRO)"; g.drawString(tit,ox+mL+pw/2-g.getFontMetrics().stringWidth(tit)/2,oy+mT-28);
        g.setFont(fnt(11,false)); g.setColor(G3); g.drawString("INV E-123 / ASTM D422",ox+mL+pw/2-g.getFontMetrics().stringWidth("INV E-123 / ASTM D422")/2,oy+mT-13);
    }

    private static void drawTriUSDA(Graphics2D g,SueloData d,int ox,int oy,int W,int H){
        double H3=Math.sqrt(3)/2; int side=(int)(Math.min(W,H)*0.68),cx=ox+W/2,topY=oy+52;
        int Ax=cx,Ay=topY,Bx=cx-side/2,By=topY+(int)(side*H3),Cx=cx+side/2,Cy=By;
        g.setColor(new Color(200,212,228)); g.setStroke(new BasicStroke(0.7f));
        for(int v=10;v<=90;v+=10){double r=1-v/100.0;int[]p1=tri(Ax,Ay,Bx,By,Cx,Cy,v,r*100,0),p2=tri(Ax,Ay,Bx,By,Cx,Cy,v,0,r*100);g.drawLine(p1[0],p1[1],p2[0],p2[1]);p1=tri(Ax,Ay,Bx,By,Cx,Cy,r*100,v,0);p2=tri(Ax,Ay,Bx,By,Cx,Cy,0,v,r*100);g.drawLine(p1[0],p1[1],p2[0],p2[1]);p1=tri(Ax,Ay,Bx,By,Cx,Cy,r*100,0,v);p2=tri(Ax,Ay,Bx,By,Cx,Cy,0,r*100,v);g.drawLine(p1[0],p1[1],p2[0],p2[1]);}
        g.setColor(G2); g.setStroke(new BasicStroke(2.2f));
        g.drawLine(Ax,Ay,Bx,By); g.drawLine(Bx,By,Cx,Cy); g.drawLine(Cx,Cy,Ax,Ay);
        int[][]regs={{50,20,30},{35,50,15},{35,8,57},{33,33,34},{28,52,20},{28,8,64},{17,40,43},{10,65,25},{6,82,12},{8,15,77},{5,8,87},{3,5,92}};
        String[]rnm={"Arcilla","Arc.\nLim.","Arc.\nAren.","Fco.\nArc.","Fco.\nArc.Lim.","Fco.\nArc.Aren.","Franco","Fco.\nLim.","Limo","Fco.\nAren.","Are.\nFco.","Arena"};
        g.setFont(fnt(10,false)); g.setColor(G2);
        for(int i=0;i<regs.length;i++){int[]pm=tri(Ax,Ay,Bx,By,Cx,Cy,regs[i][0],regs[i][1],regs[i][2]);String[]ls=rnm[i].split("\n");for(int j=0;j<ls.length;j++){int sw=g.getFontMetrics().stringWidth(ls[j]);g.drawString(ls[j],pm[0]-sw/2,pm[1]+(j-ls.length/2)*13);}}
        g.setFont(fnt(10,false)); g.setColor(G4);
        for(int v=20;v<=80;v+=20){int[]pm=tri(Ax,Ay,Bx,By,Cx,Cy,v,(100-v),0);g.drawString(v+"%",pm[0]-14,pm[1]+4);}
        g.setFont(fnt(13,true));
        g.setColor(new Color(142,68,173)); g.drawString("ARCILLA",Ax-g.getFontMetrics().stringWidth("ARCILLA")/2,Ay-18);
        g.setFont(fnt(11,false)); g.drawString("100%",Ax-g.getFontMetrics().stringWidth("100%")/2,Ay-4);
        g.setFont(fnt(12,true));
        g.setColor(new Color(0x229954)); g.drawString("ARENA 100%",Bx-90,By+20);
        g.setColor(new Color(0x2980B9)); g.drawString("LIMO 100%",Cx+6,Cy+20);
        int[]pm=tri(Ax,Ay,Bx,By,Cx,Cy,d.pctArcilla,d.pctLimo,d.pctArenaFina);
        drawStar(g,pm[0],pm[1],13,new Color(0xE74C3C));
        int cbx=pm[0]+15,cby=pm[1]-30; if(cbx+158>ox+W) cbx=pm[0]-174;
        g.setColor(new Color(255,248,245,215)); g.fillRoundRect(cbx,cby,156,68,8,8);
        g.setColor(new Color(220,160,140)); g.setStroke(new BasicStroke(0.8f)); g.drawRoundRect(cbx,cby,156,68,8,8);
        g.setFont(fnt(11,true)); g.setColor(new Color(0xC0392B)); g.drawString("Muestra",cbx+8,cby+14);
        g.setFont(fnt(11,false)); g.setColor(G2);
        g.drawString(String.format("Arc=%.1f%%",d.pctArcilla),cbx+8,cby+27);
        g.drawString(String.format("Lim=%.1f%%",d.pctLimo),cbx+8,cby+39);
        g.drawString(String.format("Ar=%.1f%%",d.pctArenaFina),cbx+8,cby+51);
        g.setFont(fnt(12,true)); g.setColor(new Color(0x229954)); g.drawString("► "+d.claseTextural,cbx+8,cby+65);
        g.setFont(fnt(15,true)); g.setColor(HDR);
        String tit="TRIÁNGULO TEXTURAL USDA"; g.drawString(tit,ox+W/2-g.getFontMetrics().stringWidth(tit)/2,oy+24);
        g.setFont(fnt(11,false)); g.setColor(G3); g.drawString("Clasificación USDA Soil Taxonomy",ox+W/2-g.getFontMetrics().stringWidth("Clasificación USDA Soil Taxonomy")/2,oy+38);
    }

    private static BufferedImage grafBarras(SueloData d, int W, int H) {
        BufferedImage img=new BufferedImage(W,H,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=aa(img); g.setColor(W_col); g.fillRect(0,0,W,H);
        int mL=52,mR=52,mT=48,mB=68,pw=W-mL-mR,ph=H-mT-mB;
        g.setFont(fnt(16,true)); g.setColor(HDR);
        String tit="RESUMEN DE COMPOSICIÓN DEL SUELO";
        g.drawString(tit,W/2-g.getFontMetrics().stringWidth(tit)/2,32);
        g.setColor(new Color(248,250,253)); g.fillRect(mL,mT,pw,ph);
        g.setColor(new Color(210,220,233)); g.setStroke(new BasicStroke(0.7f));
        for(int p=0;p<=100;p+=20){int y=mT+(int)((1-p/100.0)*ph);g.drawLine(mL,y,mL+pw,y);}
        g.setFont(fnt(12,false)); g.setColor(G3);
        for(int p=0;p<=100;p+=20){int y=mT+(int)((1-p/100.0)*ph);String s=p+"%";g.drawString(s,mL-g.getFontMetrics().stringWidth(s)-4,y+5);}
        int gW3=pw/3;
        g.setColor(new Color(200,212,228)); g.setStroke(new BasicStroke(0.8f));
        g.drawLine(mL+gW3,mT,mL+gW3,mT+ph); g.drawLine(mL+2*gW3,mT,mL+2*gW3,mT+ph);
        String[]grpL={"GRANULOMETRÍA","TEXTURAL (USDA)","ÓXIDOS (FRX)"};
        g.setFont(fnt(14,true)); g.setColor(HDR);
        for(int gr=0;gr<3;gr++){int cx=mL+gr*gW3+gW3/2;g.drawString(grpL[gr],cx-g.getFontMetrics().stringWidth(grpL[gr])/2,mT-8);}
        double sumOx=d.pctSiO2+d.pctAl2O3+d.pctCaO;
        String[][]lbs={{"Grava","Arena","Finos"},{"Arcilla","Limo","Arena fina"},{"SiO2","Al2O3","CaO"}};
        double[][]vals={{d.pctGrava,d.pctArena,d.pctFinos},{d.pctArcilla,d.pctLimo,d.pctArenaFina},{sumOx>0?d.pctSiO2/sumOx*100:0,sumOx>0?d.pctAl2O3/sumOx*100:0,sumOx>0?d.pctCaO/sumOx*100:0}};
        Color[][]bc={{new Color(0x8E44AD),new Color(0x2980B9),new Color(0x27AE60)},{new Color(0x8E44AD),new Color(0xF39C12),new Color(0x27AE60)},{new Color(0x2980B9),new Color(0xE74C3C),new Color(0x27AE60)}};
        boolean[]show={d.granCalculada,d.hidroCalculada,d.quimicaCalculada};
        int bW=(int)(gW3*0.22),bGp=(gW3-3*bW)/4;
        for(int gr=0;gr<3;gr++){for(int b=0;b<3;b++){int bx=mL+gr*gW3+bGp+b*(bW+bGp);double val=show[gr]?vals[gr][b]:0;int bh=(int)(val/100.0*ph),by=mT+ph-bh;if(bh>0){g.setColor(new Color(0,0,0,18));g.fillRect(bx+3,by+3,bW,bh);g.setColor(bc[gr][b]);g.fillRect(bx,by,bW,bh);g.setColor(bc[gr][b].darker());g.setStroke(new BasicStroke(1f));g.drawRect(bx,by,bW,bh);if(val>1){g.setFont(fnt(12,true));g.setColor(HDR);String vs=String.format("%.1f%%",val);int sw=g.getFontMetrics().stringWidth(vs);g.drawString(vs,bx+bW/2-sw/2,by-4);}}g.setFont(fnt(11,false));g.setColor(G3);String lbl=lbs[gr][b];int sw=g.getFontMetrics().stringWidth(lbl);g.drawString(lbl,bx+bW/2-sw/2,mT+ph+16);}}
        g.setColor(new Color(180,195,220)); g.setStroke(new BasicStroke(1f)); g.drawRect(mL,mT,pw,ph);
        g.dispose(); return img;
    }

    private static BufferedImage rTern(String titulo,String lA,String lB,String lC,double pA,double pB,double pC,Color ptC,int W,int H){
        BufferedImage img=new BufferedImage(W,H,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=aa(img); g.setColor(W_col); g.fillRect(0,0,W,H);
        double H3=Math.sqrt(3)/2; int side=(int)(Math.min(W,H)*0.72),cx=W/2,topY=54;
        int Ax=cx,Ay=topY,Bx=cx-side/2,By=topY+(int)(side*H3),Cx=cx+side/2,Cy=By;
        g.setFont(fnt(14,true)); g.setColor(HDR); g.drawString(titulo,cx-g.getFontMetrics().stringWidth(titulo)/2,topY-22);
        g.setColor(new Color(200,212,228)); g.setStroke(new BasicStroke(0.8f));
        for(int v=20;v<=80;v+=20){double r=1-v/100.0;int[]p1=tri(Ax,Ay,Bx,By,Cx,Cy,v,r*100,0),p2=tri(Ax,Ay,Bx,By,Cx,Cy,v,0,r*100);g.drawLine(p1[0],p1[1],p2[0],p2[1]);p1=tri(Ax,Ay,Bx,By,Cx,Cy,r*100,v,0);p2=tri(Ax,Ay,Bx,By,Cx,Cy,0,v,r*100);g.drawLine(p1[0],p1[1],p2[0],p2[1]);p1=tri(Ax,Ay,Bx,By,Cx,Cy,r*100,0,v);p2=tri(Ax,Ay,Bx,By,Cx,Cy,0,r*100,v);g.drawLine(p1[0],p1[1],p2[0],p2[1]);}
        g.setColor(G2); g.setStroke(new BasicStroke(2f));
        g.drawLine(Ax,Ay,Bx,By); g.drawLine(Bx,By,Cx,Cy); g.drawLine(Cx,Cy,Ax,Ay);
        g.setFont(fnt(11,true)); vtxt(g,lA,Ax,Ay-10,0,-1,new Color(0xA61C1C)); vtxt(g,lB,Bx,By+13,-1,1,new Color(0x1A5276)); vtxt(g,lC,Cx,Cy+13,1,1,new Color(0x0D6B3A));
        g.setFont(fnt(9,false)); g.setColor(G4);
        for(int v=20;v<=80;v+=20){int[]pm=tri(Ax,Ay,Bx,By,Cx,Cy,v,(100-v),0);g.drawString(v+"%",pm[0]-13,pm[1]+4);}
        int[]pm=tri(Ax,Ay,Bx,By,Cx,Cy,pA,pB,pC);
        drawStar(g,pm[0],pm[1],10,ptC);
        g.setFont(fnt(10,true)); g.setColor(ptC);
        int ox2=pm[0]<cx?9:-78; g.drawString(String.format("%.1f/%.1f/%.1f%%",pA,pB,pC),pm[0]+ox2,pm[1]+13);
        g.dispose(); return img;
    }

    private static int titEn(PDFPage pg, String t, String n, int y) throws IOException {
        pg.fill(ML, y-20, CW, 20, HDR);
        pg.txt(ML+5, y-5, t, 9.5f, W, true);
        if(!n.isEmpty()) pg.txtR(ML+CW-4, y-5, n, 7.5f, new Color(155,200,245), false);
        return y-26;
    }
    private static int subtit(PDFPage pg, String t, int y) throws IOException {
        pg.fill(ML, y-15, CW, 15, BSEC);
        pg.txt(ML+5, y-3, t, 8f, W, true);
        return y-20;
    }
    private static int tabla(PDFPage pg, String[][] rows, int[] colW, int x, int y) throws IOException {
        int rH=14, totalW=0; for(int w:colW) totalW+=w;
        int totalH=rows.length*rH, startY=y-totalH;
        for(int r=0;r<rows.length;r++){
            int ry=y-(r+1)*rH; boolean head=r==0;
            boolean last=r==rows.length-1&&rows[r][0]!=null&&(rows[r][0].startsWith("CLAS")||rows[r][0].startsWith("Clase textural")||rows[r][0].startsWith("Suma"));
            Color bg=head?TH:last?TSUM:r%2==0?TO:TB;
            pg.fill(x,ry,totalW,rH,bg);
            pg.line(x,ry,x+totalW,ry,TBD,0.3f);
            int cx=x;
            for(int c=0;c<rows[r].length&&c<colW.length;c++){
                if(rows[r][c]!=null) pg.txt(cx+4,ry+4,rows[r][c],7.5f,head?W:G1,head||last);
                cx+=colW[c]; if(c<colW.length-1) pg.line(cx,startY,cx,y,TBD,0.25f);
            }
        }
        pg.stroke(x,startY,totalW,totalH,TBD,0.5f);
        return startY-4;
    }
    private static int cajaAnl(PDFPage pg, String txt, int y) throws IOException {
        int cpl=(int)(CW/3.95);
        String[]words=txt.split(" "); List<String>lines=new ArrayList<>();
        StringBuilder ln=new StringBuilder();
        for(String w:words){if(ln.length()+w.length()+1>cpl&&ln.length()>0){lines.add(ln.toString().trim());ln=new StringBuilder();}ln.append(w).append(" ");}
        if(ln.length()>0) lines.add(ln.toString().trim());
        int bH=lines.size()*12+8;
        pg.fill(ML,y-bH,CW,bH,BANLY);
        int ty=y-5; for(String l:lines){pg.txt(ML+8,ty,l,8f,BATXT,false);ty-=12;}
        return ty-4;
    }

    private static String descGran(SueloData d){
        StringBuilder s=new StringBuilder();
        s.append(String.format("La muestra presenta %.1f%% de material fino (pasa N\u00b0200), %.1f%% de arena y %.1f%% de grava. ",d.pctFinos,d.pctArena,d.pctGrava));
        if(d.Cu!=null)s.append(String.format("Cu=%.1f (%s) y Cc=%.2f (%s). ",d.Cu,d.Cu>6?"bien gradado":"uniforme",d.Cc,(d.Cc!=null&&d.Cc>=1&&d.Cc<=3)?"rango admisible":"fuera del rango 1–3"));
        if(d.pctFinos>12)s.append(String.format("La proporci\u00f3n de finos (%.1f%%) supera el 12%%; se requiere determinar l\u00edmites de Atterberg para clasificaci\u00f3n USCS definitiva.",d.pctFinos));
        else if(d.pctArena>50)s.append("La fracci\u00f3n dominante es arena; verifique plasticidad de los finos.");
        return s.toString().trim();
    }
    private static String descAtter(SueloData d){
        String pl=d.IP<7?"baja (IP<7)":d.IP<15?"media (7\u2264IP<15)":d.IP<35?"alta (15\u2264IP<35)":"muy alta (IP\u226535)";
        StringBuilder s=new StringBuilder();
        s.append(String.format("Con LL=%.1f%% y LP=%.1f%%, el IP=%.1f%% indica plasticidad %s. ",d.LL,d.LP,d.IP,pl));
        s.append(String.format("Clasificaci\u00f3n USCS: %s. ",d.clasificacionUSCS));
        if(d.IP>=15)s.append("Potencial expansivo significativo; evaluar estabilizaci\u00f3n en subrasante o cimentaci\u00f3n. ");
        if(d.pctArcilla>0){double a=d.IP/d.pctArcilla;s.append(String.format("Actividad de Skempton A=%.2f (%s).",a,a>1.25?"arcilla activa":a<0.75?"arcilla inactiva":"actividad normal"));}
        return s.toString().trim();
    }
    private static String descHidro(SueloData d){
        StringBuilder s=new StringBuilder();
        s.append(String.format("An\u00e1lisis hidrom\u00e9trico: arcilla %.1f%%, limo %.1f%%, arena fina %.1f%%. ",d.pctArcilla,d.pctLimo,d.pctArenaFina));
        s.append(String.format("Clase textural USDA: %s. ",d.claseTextural));
        if(d.pctArcilla>40)s.append("Contenido alto de arcilla (>40%): baja permeabilidad, alta plasticidad, potencial expansivo significativo.");
        else if(d.pctArcilla>18)s.append("Contenido moderado de arcilla: cohesin notable, sensibilidad media al agua.");
        else s.append("Bajo contenido de arcilla: mayor permeabilidad, menor expansividad.");
        if(!d.clasificacionUSCS.equals("—"))s.append(String.format(" Combinado con Atterberg (IP=%.1f%%): confirma %s.",d.IP,d.clasificacionUSCS.split(" ")[0]));
        return s.toString().trim();
    }
    private static String descTern(double[]g1,double[]g2,double[]g3){
        StringBuilder s=new StringBuilder();
        if(g1!=null){s.append(String.format("\u00d3xidos normalizados: SiO2+Ti=%.1f%%, Al2O3+Fe=%.1f%%, Ca+\u00c1lc=%.1f%%. ",g1[0],g1[1],g1[2]));if(g1[0]>60)s.append("Car\u00e1cter f\u00e9lsico dominante. ");else if(g1[1]>35)s.append("Fase alumino-f\u00e9rrica dominante (illita, caolinita o esmectita). ");else s.append("Composici\u00f3n geoqu\u00edmica mixta. ");}
        if(g2!=null)s.append(String.format("REE: %.2f%% (%s).",g2[0],g2[0]>5?"enriquecimiento notable":"contenido traza"));
        return s.toString().trim();
    }
    private static String descGeneral(SueloData d,String muestra,String proyecto){
        StringBuilder s=new StringBuilder();
        s.append(String.format("Muestra %s — Proyecto: %s. Clasificación: %s. ",muestra,proyecto,d.nombreFinal));
        if(d.granCalculada){String dom=d.pctGrava>50?"grava":d.pctArena>50?"arena":"finos";s.append(String.format("Fracción dominante: %s (%.1f%%). ",dom,Math.max(d.pctGrava,Math.max(d.pctArena,d.pctFinos))));}
        if(d.atterCalculada){String pl=d.IP<7?"baja":d.IP<15?"media":d.IP<35?"alta":"muy alta";s.append(String.format("Plasticidad %s (LL=%.1f%%, LP=%.1f%%, IP=%.1f%%), USCS: %s. ",pl,d.LL,d.LP,d.IP,d.clasificacionUSCS));}
        if(d.hidroCalculada)s.append(String.format("Textura USDA: %s (Arcilla=%.1f%%, Limo=%.1f%%). ",d.claseTextural,d.pctArcilla,d.pctLimo));
        if(d.quimicaCalculada)s.append(String.format("Geoquímica FRX: SiO2=%.1f%%, Al2O3=%.1f%%, CaO=%.1f%% — composici\u00f3n silicatada con aluminosilicatos.",d.pctSiO2,d.pctAl2O3,d.pctCaO));
        return s.toString().trim();
    }

    private static double[]ox(SueloData d){double a=d.pctSiO2,b=d.pctAl2O3,c=d.pctCaO,t=a+b+c;if(t<=0)return null;return new double[]{a/t*100,b/t*100,c/t*100};}
    private static double[]ree(SueloData d){double si=d.pctSiO2,al=d.pctAl2O3,ca=d.pctCaO,t=si+al+ca;if(t<=0)return null;double re=si*0.04,p=ca*0.03,ot=t-re-p,t2=re+p+ot;return new double[]{re/t2*100,p/t2*100,ot/t2*100};}
    private static double[]geoquim(SueloData d){return ox(d);}

    private static Graphics2D aa(BufferedImage i){Graphics2D g=i.createGraphics();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);g.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);return g;}
    private static Font fnt(int sz,boolean b){return new Font("SansSerif",b?Font.BOLD:Font.PLAIN,sz);}
    private static BasicStroke dsh(float w){return new BasicStroke(w,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,10,new float[]{7f,5f},0);}
    private static int xpx(double mm,double lMin,double lMax,int pw,int mL){return mL+(int)((Math.log10(mm)-lMin)/(lMax-lMin)*pw);}
    private static int xpx2(double mm,double lMin,double lMax,int pw,int base){return base+(int)((Math.log10(mm)-lMin)/(lMax-lMin)*pw);}
    private static int ypx(double pct,int ph,int mT){return mT+(int)((1-pct/100.0)*ph);}
    private static int[]tri(int Ax,int Ay,int Bx,int By,int Cx,int Cy,double a,double b,double c){double t=a+b+c;if(t==0)return new int[]{(Ax+Bx+Cx)/3,(Ay+By+Cy)/3};return new int[]{(int)(Ax*(a/t)+Bx*(b/t)+Cx*(c/t)),(int)(Ay*(a/t)+By*(b/t)+Cy*(c/t))};}
    private static void drawStar(Graphics2D g,int cx,int cy,int r,Color c){int n=5;int[]px=new int[n*2],py=new int[n*2];for(int i=0;i<n*2;i++){double a=-Math.PI/2+i*Math.PI/n;double rd=i%2==0?r:r*0.42;px[i]=(int)(cx+rd*Math.cos(a));py[i]=(int)(cy+rd*Math.sin(a));}g.setColor(c);g.fillPolygon(px,py,n*2);g.setColor(W_col);g.setStroke(new BasicStroke(1.5f));g.drawPolygon(px,py,n*2);}
    private static void vtxt(Graphics2D g,String t,int x,int y,int hA,int vA,Color c){g.setColor(c);String[]ls=t.split("\n");int lh=13;for(int i=0;i<ls.length;i++){int sw=g.getFontMetrics().stringWidth(ls[i]);int dx=hA<0?-sw:hA>0?0:-sw/2;int dy=vA<0?-lh*(ls.length-1)+i*lh:i*lh;g.drawString(ls[i],x+dx,y+dy);}}
    private static String f4(Double v){return v!=null?String.format("%.4f",v):"—";}
    private static String f2(Double v){return v!=null?String.format("%.2f",v):"—";}
    private static String f1(double v){return String.format("%.1f",v);}
    private static String trunc(String s,int max){return s!=null&&s.length()>max?s.substring(0,max-3)+"...":s!=null?s:"";}

    static class PDFDoc{
        private final List<byte[]>objs=new ArrayList<>();
        private final List<Integer>offs=new ArrayList<>();
        private final List<Integer>pgIds=new ArrayList<>();
        private int nxt=1;int catId,pgrsId;
        PDFDoc(){catId=nxt++;pgrsId=nxt++;}
        int alloc(){return nxt++;}
        void set(int id,byte[]d){while(objs.size()<=id)objs.add(null);objs.set(id,d);}
        PDFPage newPage(){return new PDFPage(this);}
        void addPage(PDFPage p)throws IOException{p.build(this);pgIds.add(p.pgId);}
        void escribir(OutputStream out)throws IOException{
            set(catId,(catId+" 0 obj\n<<\n/Type /Catalog\n/Pages "+pgrsId+" 0 R\n>>\nendobj\n").getBytes(StandardCharsets.ISO_8859_1));
            StringBuilder kids=new StringBuilder("[");for(int id:pgIds)kids.append(id).append(" 0 R ");
            set(pgrsId,(pgrsId+" 0 obj\n<<\n/Type /Pages\n/Kids "+kids+"]\n/Count "+pgIds.size()+"\n>>\nendobj\n").getBytes(StandardCharsets.ISO_8859_1));
            ByteArrayOutputStream buf=new ByteArrayOutputStream();
            buf.write("%PDF-1.4\n%\u00e2\u00e3\u00cf\u00d3\n".getBytes(StandardCharsets.ISO_8859_1));
            offs.clear();
            for(int i=1;i<objs.size();i++){byte[]ob=objs.get(i);if(ob==null){offs.add(-1);continue;}offs.add(buf.size());buf.write(ob);}
            int xOff=buf.size(),nObj=objs.size();
            StringBuilder xr=new StringBuilder("xref\n0 ").append(nObj).append("\n0000000000 65535 f \n");
            for(int i=1;i<nObj;i++){int idx=i-1;int off=(idx<offs.size()&&offs.get(idx)>=0)?offs.get(idx):0;xr.append(String.format("%010d 00000 n \n",off));}
            xr.append("trailer\n<<\n/Size ").append(nObj).append("\n/Root ").append(catId).append(" 0 R\n>>\nstartxref\n").append(xOff).append("\n%%EOF\n");
            buf.write(xr.toString().getBytes(StandardCharsets.ISO_8859_1));
            out.write(buf.toByteArray());
        }
    }
    static class PDFPage{
        static final int PW=595,PH=842;
        private final List<String>ops=new ArrayList<>();
        private final List<int[]>imgs=new ArrayList<>();
        private final List<byte[]>ib=new ArrayList<>();
        private final List<int[]>id=new ArrayList<>();
        int pgId;private final PDFDoc doc;
        PDFPage(PDFDoc d){this.doc=d;}
        void fill(int x,int y,int w,int h,Color c){ops.add(cf(c)+" rg "+x+" "+y+" "+w+" "+h+" re f");}
        void stroke(int x,int y,int w,int h,Color c,float lw){ops.add(cf(c)+" RG "+lw+" w "+x+" "+y+" "+w+" "+h+" re S");}
        void line(int x1,int y1,int x2,int y2,Color c,float w){ops.add(String.format("%.3f %.3f %.3f RG %.1f w %d %d m %d %d l S",c.getRed()/255f,c.getGreen()/255f,c.getBlue()/255f,w,x1,y1,x2,y2));}
        void txt(int x,int y,String t,float sz,Color c,boolean b){ops.add(String.format("%.3f %.3f %.3f rg BT %s %.1f Tf %d %d Td (%s) Tj ET",c.getRed()/255f,c.getGreen()/255f,c.getBlue()/255f,b?"/F2":"/F1",sz,x,y,esc(t)));}
        void txtR(int xR,int y,String t,float sz,Color c,boolean b){txt(xR-(int)(t.length()*sz*0.46),y,t,sz,c,b);}
        void txtC(int xC,int y,String t,float sz,Color c){txt(xC-(int)(t.length()*sz*0.38),y,t,sz,c,false);}
        void img(BufferedImage bi,int x,int y,int w,int h)throws IOException{ByteArrayOutputStream bos=new ByteArrayOutputStream();ImageIO.write(bi,"jpg",bos);int idx=ib.size();ib.add(bos.toByteArray());id.add(new int[]{bi.getWidth(),bi.getHeight()});imgs.add(new int[]{idx,x,y,w,h});}
        void build(PDFDoc pdf)throws IOException{
            int f1=pdf.alloc(),f2=pdf.alloc();
            pdf.set(f1,(f1+" 0 obj\n<<\n/Type /Font\n/Subtype /Type1\n/BaseFont /Helvetica\n/Encoding /WinAnsiEncoding\n>>\nendobj\n").getBytes(StandardCharsets.ISO_8859_1));
            pdf.set(f2,(f2+" 0 obj\n<<\n/Type /Font\n/Subtype /Type1\n/BaseFont /Helvetica-Bold\n/Encoding /WinAnsiEncoding\n>>\nendobj\n").getBytes(StandardCharsets.ISO_8859_1));
            List<Integer>xoIds=new ArrayList<>();
            for(int i=0;i<ib.size();i++){int xoId=pdf.alloc();xoIds.add(xoId);byte[]jpg=ib.get(i);int[]dim=id.get(i);String hdr=xoId+" 0 obj\n<<\n/Type /XObject\n/Subtype /Image\n/Width "+dim[0]+"\n/Height "+dim[1]+"\n/ColorSpace /DeviceRGB\n/BitsPerComponent 8\n/Filter /DCTDecode\n/Length "+jpg.length+"\n>>\nstream\n";ByteArrayOutputStream xb=new ByteArrayOutputStream();xb.write(hdr.getBytes(StandardCharsets.ISO_8859_1));xb.write(jpg);xb.write("\nendstream\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));pdf.set(xoId,xb.toByteArray());}
            StringBuilder cs=new StringBuilder();
            cs.append("1 1 1 rg 0 0 ").append(PW).append(" ").append(PH).append(" re f\n");
            for(String op:ops)cs.append(op).append("\n");

            for(int[]im:imgs){int i=im[0];if(i>=xoIds.size())continue;cs.append(String.format("q %d 0 0 %d %d %d cm /Im%d Do Q\n",im[3],im[4],im[1],im[2],i));}
            byte[]csb=cs.toString().getBytes(StandardCharsets.ISO_8859_1);
            int csId=pdf.alloc();
            StringBuilder xoD=new StringBuilder("/XObject <<");for(int i=0;i<xoIds.size();i++)xoD.append(" /Im").append(i).append(" ").append(xoIds.get(i)).append(" 0 R");xoD.append(" >>");
            ByteArrayOutputStream cb=new ByteArrayOutputStream();
            cb.write((csId+" 0 obj\n<<\n/Length "+csb.length+"\n>>\nstream\n").getBytes(StandardCharsets.ISO_8859_1));
            cb.write(csb); cb.write("\nendstream\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));
            pdf.set(csId,cb.toByteArray());
            pgId=pdf.alloc();
            pdf.set(pgId,(pgId+" 0 obj\n<<\n/Type /Page\n/Parent "+pdf.pgrsId+" 0 R\n/MediaBox [0 0 "+PW+" "+PH+"]\n/Contents "+csId+" 0 R\n/Resources <<\n/Font << /F1 "+f1+" 0 R /F2 "+f2+" 0 R >>\n"+xoD+"\n>>\n>>\nendobj\n").getBytes(StandardCharsets.ISO_8859_1));
        }
        private static String cf(Color c){return String.format("%.3f %.3f %.3f",c.getRed()/255f,c.getGreen()/255f,c.getBlue()/255f);}
        private static String esc(String s){if(s==null)return"";StringBuilder sb=new StringBuilder();for(char c:s.toCharArray()){if(c=='(')sb.append("\\(");else if(c==')')sb.append("\\)");else if(c=='\\')sb.append("\\\\");else if(c>=0x20&&c<=0xFF)sb.append(c);else sb.append(' ');}return sb.toString();}
    }
}
