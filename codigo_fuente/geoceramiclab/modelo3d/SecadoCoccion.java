package geoceramiclab.modelo3d;

import geoceramiclab.Tema;
import geoceramiclab.model.BaldosinCatalan;
import geoceramiclab.model.BaldosinCatalan.MezclaBaldosin;
import geoceramiclab.model.SueloData;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SecadoCoccion extends JPanel {

    private static final Color BG     = Tema.FONDO;
    private static final Color BG2    = Tema.SUPERFICIE;
    private static final Color BG3    = Tema.SUPERFICIE_2;
    private static final Color PLOTBG = Tema.GRAFICA_FONDO;
    private static final Color BORD   = Tema.BORDE;
    private static final Color GRID   = Tema.GRAFICA_REJILLA;

    private static final Color ACCENT = Tema.INFO;
    private static final Color GREEN  = Tema.OK;
    private static final Color AMBER  = Tema.AVISO;
    private static final Color RED    = Tema.ERROR;
    private static final Color TEXT   = Tema.TEXTO;
    private static final Color MUTED  = Tema.TEXTO_2;
    private static final Color DIM    = Tema.TEXTO_3;

    private static final Color[] M_COLS = {ACCENT, GREEN, AMBER};

    private static final Color FASE1_COL = new Color(126, 176, 255);
    private static final Color FASE2_COL = new Color(120, 220, 180);
    private static final Color OFF_COL   = new Color(255, 142, 142);

    private static final int CHART_H = 210;

    private static final double FRAC_PLASTICO = 0.25;

    private static final int    DIAS_PLASTICO = 2;
    private static final double K1_A = 0.025, K1_HREF = 28.0, K1_B = 1.28;
    private static final double K2_A = 0.6,   K2_HREF = 16.4, K2_B = 3.473;
    private static final int    DIAS_MAX = 120;

    private static final double FRAC_ESTABLE = 0.36;

    private static final double REF_LARGO = 9.0, REF_ANCHO = 6.5, REF_ESP = 1.0;
    private static final double EXP_GEOM  = 2.0;

    static double longitudSecado(double volCm3, double espCm) {
        double e    = Math.max(espCm, 0.2);
        double aTop = volCm3 / e;
        double a    = Math.sqrt(aTop * REF_LARGO / REF_ANCHO);
        double b    = aTop / a;
        // Longitud caracteristica L = volumen / area de evaporacion (cara superior + cantos)
        return volCm3 / (aTop + 2.0 * (a + b) * e);
    }

    static double factorGeometria(double volCm3, double espCm) {
        double lRef = longitudSecado(REF_LARGO * REF_ANCHO * REF_ESP, REF_ESP);
        // Factor de tamano (difusion de Fick, Crank 1975): G = (L/Lref)^2,  L = volumen / area que evapora
        return Math.pow(longitudSecado(volCm3, espCm) / lRef, EXP_GEOM);
    }

    // Pico de coccion: 1100 C (optimo de fusion del feldespato)
    private static final double T_PICO = 1100.0;

    static double corrI(double h0) { return h0 / 100.0 - K1_HREF / 100.0; }

    static double corrJ(double h2) { return h2 / 100.0 - K2_HREF / 100.0; }

    static double kPlastico(double h0) {
        // Fase plastico (2 dias): k1 = 0.025*(H0/28)^(-1.28*I),  I = H0/100 - 0.28
        return K1_A * Math.pow(h0 / K1_HREF, -K1_B * corrI(h0));
    }

    static double kLibre(double h2) {
        // Fase aire libre: k2 = 0.6*(H2/16.4)^(-3.473*J),  J = H2/100 - 0.164
        return K2_A * Math.pow(h2 / K2_HREF, -K2_B * corrJ(h2));
    }

    private MezclaBaldosin[] mezclas;
    private String[] mezclasNombres = {"M1", "M2", "M3"};
    private SueloData datos;
    private boolean tieneData = false;

    private double espesorMmActual = 12;
    private double volCm3Actual    = 400;

    private static class Secado {
        double[] masaPorDia;
        double[] humedadPorDia;
        int dias1, dias2, diasTotal;
        double tempFase1, tempFase2;
        double contraccionSecado;
        double masaAguaIniG, masaSolidosG, masaTotalIniG;
        double humedadFinalPct;
        double perdidaPlasticoG;
        double perdidaDiaPlasticoG;
        double lSecado, fGeom;
        double hEstable;
        double h0, h2;
        double I, J, k1, k2;
        boolean estabilizado;
        double umbralG;
    }
    private Secado[] secados;

    private static class Coccion {
        double[][] etapas;
        double tMeseta;
        double tiempoTotalMin;
        double contraccion, perdidaFuego, densidadFinal;
        double velCal2, velEnfr;

        boolean enfrNatural = true;
        double kEnfr;
        double tAmb;
        double tDesenhorne;
        double durEnfr;
        double minA200;
        double velEnfrMedia;
    }
    private Coccion coccion;
    private double[] contrM, pafM, densM;

    private final CurvaSecadoMezclaPanel[]  curvaSecadoPaneles  = new CurvaSecadoMezclaPanel[3];
    private final CurvaCoccionMezclaPanel   curvaCoccionPanel;

    private final JPanel secadoResumenPanel;
    private final JPanel coccionResumenPanel;

    private JLabel infoHdr;

    public SecadoCoccion() {
        setLayout(new BorderLayout());
        setBackground(BG);

        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(Tema.BARRA);
        hdr.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORD));
        JLabel t = lbl(" Secado y Cocción — baldosín (dimensiones + suelo)", AMBER, 13, true);
        t.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 8));
        infoHdr = lbl("Calcule la mezcla primero   ", DIM, 11, false);
        hdr.add(t, BorderLayout.WEST);
        hdr.add(infoHdr, BorderLayout.EAST);
        add(hdr, BorderLayout.NORTH);

        JPanel colSecado = new JPanel();
        colSecado.setLayout(new BoxLayout(colSecado, BoxLayout.Y_AXIS));
        colSecado.setBackground(BG);
        colSecado.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 6));
        colSecado.add(tituloColumna(" SECADO (post-desmolde)", ACCENT,
                new Color[]{FASE1_COL, FASE2_COL},
                new String[]{"plástico 2 d", "aire libre"}));
        colSecado.add(Box.createVerticalStrut(8));
        for (int m = 0; m < 3; m++) {
            curvaSecadoPaneles[m] = new CurvaSecadoMezclaPanel(m);
            prepChart(curvaSecadoPaneles[m]);
            colSecado.add(curvaSecadoPaneles[m]);
            colSecado.add(Box.createVerticalStrut(8));
        }
        secadoResumenPanel = new JPanel();
        secadoResumenPanel.setLayout(new BoxLayout(secadoResumenPanel, BoxLayout.Y_AXIS));
        secadoResumenPanel.setBackground(BG);
        secadoResumenPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        colSecado.add(secadoResumenPanel);
        colSecado.add(Box.createVerticalGlue());
        JScrollPane scrollSecado = mkScroll(colSecado);

        JPanel colCoccion = new JPanel();
        colCoccion.setLayout(new BoxLayout(colCoccion, BoxLayout.Y_AXIS));
        colCoccion.setBackground(BG);
        colCoccion.setBorder(BorderFactory.createEmptyBorder(10, 6, 10, 10));
        colCoccion.add(tituloColumna(" COCCIÓN (horno)", RED,
                new Color[]{RED, OFF_COL},
                new String[]{"calentamiento", "enfriamiento natural"}));
        colCoccion.add(Box.createVerticalStrut(8));
        curvaCoccionPanel = new CurvaCoccionMezclaPanel(0);
        prepChart(curvaCoccionPanel);
        curvaCoccionPanel.setPreferredSize(new Dimension(420, CHART_H + 60));
        curvaCoccionPanel.setMinimumSize(new Dimension(260, CHART_H + 60));
        curvaCoccionPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, CHART_H + 60));
        colCoccion.add(curvaCoccionPanel);
        colCoccion.add(Box.createVerticalStrut(8));
        coccionResumenPanel = new JPanel();
        coccionResumenPanel.setLayout(new BoxLayout(coccionResumenPanel, BoxLayout.Y_AXIS));
        coccionResumenPanel.setBackground(BG);
        coccionResumenPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        colCoccion.add(coccionResumenPanel);
        colCoccion.add(Box.createVerticalGlue());
        JScrollPane scrollCoccion = mkScroll(colCoccion);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollSecado, scrollCoccion);
        split.setResizeWeight(0.5);
        split.setDividerSize(6);
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setBackground(BORD);
        split.setContinuousLayout(true);
        add(split, BorderLayout.CENTER);

        actualizarSecadoUI();
        actualizarCoccionUI();
    }

    private void prepChart(JPanel p) {
        p.setPreferredSize(new Dimension(420, CHART_H));
        p.setMinimumSize(new Dimension(260, CHART_H));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, CHART_H));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setBorder(BorderFactory.createLineBorder(BORD));
    }

    private JScrollPane mkScroll(JComponent c) {
        JScrollPane s = new JScrollPane(c);
        s.setBackground(BG);
        s.getViewport().setBackground(BG);
        s.setBorder(BorderFactory.createEmptyBorder());
        s.getVerticalScrollBar().setUnitIncrement(18);
        s.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        return s;
    }

    private JPanel tituloColumna(String titulo, Color col, Color[] cols, String[] txts) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        p.add(lbl(titulo, col, 13, true), BorderLayout.WEST);
        JPanel leg = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 2));
        leg.setBackground(BG);
        for (int i = 0; i < cols.length; i++) leg.add(chip(cols[i], txts[i]));
        p.add(leg, BorderLayout.EAST);
        return p;
    }

    private JLabel chip(Color c, String txt) {
        JLabel l = new JLabel("■ " + txt);
        l.setFont(Tema.ui(Font.PLAIN, 10));
        l.setForeground(c);
        return l;
    }

    public void setMezcla(MezclaBaldosin m, BaldosinCatalan b, SueloData d) {
        setMezclas3(m == null ? null : new MezclaBaldosin[]{m},
                    new String[]{"Mezcla actual"}, b, d);
    }

    public void setMezclas3(MezclaBaldosin[] ms, String[] nombres,
                            BaldosinCatalan b, SueloData d) {
        double eMm = espesorMmActual, vCm3 = volCm3Actual;
        if (b != null) {
            try { eMm = (b.espesor + b.altoNervio) * 10; } catch (Exception ignored) {}
            try { vCm3 = b.volNeto; } catch (Exception ignored) {}
        }
        setMezclas3(ms, nombres, eMm, vCm3, d);
    }

    public void setMezclas3(MezclaBaldosin[] ms, String[] nombres,
                            double espesorMm, double volCm3, SueloData d) {
        this.mezclas = ms;
        if (nombres != null) this.mezclasNombres = nombres;
        if (espesorMm > 0) this.espesorMmActual = espesorMm;
        if (volCm3 > 0)    this.volCm3Actual    = volCm3;
        if (d != null)     this.datos = d;

        tieneData = safeCalcular();

        if (infoHdr != null) {
            infoHdr.setText(tieneData
                ? String.format("Espesor %.1f mm  ·  volumen %.0f cm³  ·  arcilla %.0f %%   ",
                        espesorMmActual, volCm3Actual,
                        (datos != null && datos.hidroCalculada) ? datos.pctArcilla : 20.0)
                : "Calcule la mezcla primero   ");
            infoHdr.setForeground(tieneData ? MUTED : DIM);
        }

        actualizarSecadoUI();
        actualizarCoccionUI();
        for (CurvaSecadoMezclaPanel p : curvaSecadoPaneles)  p.repaint();
        curvaCoccionPanel.repaint();
    }

    private boolean safeCalcular() {
        try {
            calcularSecados();
            calcularCocciones();
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private void calcularSecados() {
        if (mezclas == null || mezclas.length == 0)
            throw new IllegalStateException("no hay mezclas");

        final double vol = volCm3Actual;
        secados = new Secado[mezclas.length];

        for (int m = 0; m < mezclas.length; m++) {
            MezclaBaldosin mz = mezclas[m];
            if (mz == null) { secados[m] = null; continue; }

            double dens    = mz.densidadAparente;
            double relAgua = mz.relacionAguaSolidos > 0 ? mz.relacionAguaSolidos : 0.22;

            double masaSolidos  = dens * vol;
            double masaAguaIni  = masaSolidos * relAgua;
            double masaTotalIni = masaSolidos + masaAguaIni;

            double h0 = relAgua / (1.0 + relAgua) * 100.0;

            final double fGeom = factorGeometria(vol, espesorMmActual / 10.0);

            List<Double> serieW = new ArrayList<>();
            double w = h0;
            serieW.add(w);
            int dia = 0;

            double I  = corrI(h0);
            double k1 = kPlastico(h0) / fGeom;
            for (int i = 0; i < DIAS_PLASTICO; i++) {
                // Humedad dia a dia: w(d+1) = w(d)*e^(-k1)   (k1 ya incluye el factor de tamano G)
                w *= Math.exp(-k1);
                serieW.add(w); dia++;
            }
            double h2 = w;

            double J  = corrJ(h2);
            double k2 = kLibre(h2) / fGeom;
            // Estable cuando la humedad baja al 36% de la del dia 2 (H2)
            final double hEst = FRAC_ESTABLE * h2;

            final int diasLibre = (int) Math.min(DIAS_MAX - DIAS_PLASTICO,
                    Math.ceil(Math.log(1.0 / FRAC_ESTABLE) / (K2_A / fGeom)));
            int dias2 = 0;
            for (int i = 0; i < diasLibre; i++) {
                w *= Math.exp(-k2);
                serieW.add(w); dia++; dias2++;
            }
            boolean estable = true;
            final double umbralG = 1.0;

            Secado s = new Secado();
            int n = serieW.size();
            s.masaPorDia    = new double[n];
            s.humedadPorDia = new double[n];
            for (int i = 0; i < n; i++) {
                double wi = serieW.get(i);
                s.humedadPorDia[i] = wi;
                s.masaPorDia[i]    = masaSolidos / (1.0 - wi / 100.0);
            }
            s.dias1 = DIAS_PLASTICO;
            s.dias2 = dias2;
            s.diasTotal = dia;
            s.tempFase1 = 18;
            s.tempFase2 = 20;
            s.contraccionSecado = clamp(mz.contraccionLineal * 0.32, 0.3, 4.5);
            s.masaAguaIniG    = masaAguaIni;
            s.masaSolidosG    = masaSolidos;
            s.masaTotalIniG   = masaTotalIni;
            s.humedadFinalPct = s.humedadPorDia[n - 1];
            s.perdidaPlasticoG    = s.masaPorDia[0] - s.masaPorDia[DIAS_PLASTICO];
            s.perdidaDiaPlasticoG = s.perdidaPlasticoG / DIAS_PLASTICO;
            s.h0 = h0; s.h2 = h2; s.I = I; s.J = J; s.k1 = k1; s.k2 = k2;
            s.estabilizado = estable;
            s.umbralG = umbralG;
            s.fGeom   = fGeom;
            s.hEstable = hEst;
            s.lSecado = longitudSecado(vol, espesorMmActual / 10.0);
            secados[m] = s;
        }
    }

    private void calcularCocciones() {
        if (mezclas == null || mezclas.length == 0)
            throw new IllegalStateException("no hay mezclas");

        final double espMm = espesorMmActual;
        final double espFactor = clamp(Math.pow(Math.max(espMm, 6.0) / 25.0, 0.85), 0.45, 2.8);
        final double pctArc = (datos != null && datos.hidroCalculada)
                              ? datos.pctArcilla : 20.0;

        double vel1 = clamp(100.0 / espFactor, 40, 130) / 60.0;
        double vel2 = clamp(150.0 / espFactor, 60, 180) / 60.0;
        double vel3 = clamp(150.0 / espFactor, 50, 170) / 60.0;
        double durMeseta = clamp(45.0 * espFactor, 30, 60);

        double t0 = 25, tA = 200, tB = 600, tPico = T_PICO;
        double dur1 = (tA - t0) / vel1;
        double dur2 = (tB - tA) / vel2;
        double dur3 = (tPico - tB) / vel3;

        double tAmb        = 22.0;
        double tDesenhorne = 50.0;
        double kEnfr       = 0.0058 / espFactor;
        double durEnfr = Math.log((tPico - tAmb) / (tDesenhorne - tAmb)) / kEnfr;
        double minA200 = Math.log((tPico - tAmb) / (200.0 - tAmb)) / kEnfr;
        double velEnfrMedia = (tPico - tDesenhorne) / (durEnfr / 60.0);

        Coccion c = new Coccion();
        c.etapas = new double[][]{
            {t0,    tA,          dur1,      vel1},
            {tA,    tB,          dur2,      vel2},
            {tB,    tPico,       dur3,      vel3},
            {tPico, tPico,       durMeseta, 0},
            {tPico, tDesenhorne, durEnfr,   0}
        };
        c.tMeseta        = tPico;
        c.tiempoTotalMin = dur1 + dur2 + dur3 + durMeseta + durEnfr;
        c.velCal2        = vel3;
        c.enfrNatural  = true;
        c.kEnfr        = kEnfr;
        c.tAmb         = tAmb;
        c.tDesenhorne  = tDesenhorne;
        c.durEnfr      = durEnfr;
        c.minA200      = minA200;
        c.velEnfrMedia = velEnfrMedia;
        c.velEnfr      = velEnfrMedia / 60.0;
        coccion = c;

        contrM = new double[mezclas.length];
        pafM   = new double[mezclas.length];
        densM  = new double[mezclas.length];
        for (int m = 0; m < mezclas.length; m++) {
            MezclaBaldosin mz = mezclas[m];
            if (mz == null) { contrM[m] = pafM[m] = densM[m] = Double.NaN; continue; }
            double relAgua = mz.relacionAguaSolidos > 0 ? mz.relacionAguaSolidos : 0.22;
            double humResidual = 1.0;
            if (secados != null && m < secados.length && secados[m] != null)
                humResidual = Math.max(0.5, secados[m].humedadFinalPct);
            contrM[m] = mz.contraccionLineal;
            pafM[m]   = clamp(pctArc * 0.22 + mz.fracCal * 18 + relAgua * 12 + humResidual * 0.3, 3, 16);
            densM[m]  = mz.densidadCocida > 0 ? mz.densidadCocida : mz.densidadAparente * 0.92;
        }
    }

    private class CurvaSecadoMezclaPanel extends JPanel {
        final int idx;
        CurvaSecadoMezclaPanel(int idx) { this.idx = idx; setBackground(BG2); }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = mk2D(g0);
            int W = getWidth(), H = getHeight();
            g.setColor(BG2); g.fillRect(0, 0, W, H);

            Color mCol = M_COLS[idx % M_COLS.length];
            String nm  = nombre(idx);

            boolean hay = tieneData && secados != null && idx < secados.length && secados[idx] != null;
            Secado s = hay ? secados[idx] : null;

            String der = hay ? String.format("%d días · %.0f g → %.0f g",
                    s.diasTotal, s.masaPorDia[0], s.masaPorDia[s.masaPorDia.length - 1]) : "";
            cabecera(g, W, mCol, nm, "secado — masa vs tiempo", der);
            if (!hay) { vacio(g, W, H); return; }

            int ML = 62, MR = 16, MT = 32, MB = 40;
            int pw = W - ML - MR, ph = H - MT - MB;
            if (pw < 60 || ph < 40) return;

            int n = s.masaPorDia.length;
            double maxM = s.masaPorDia[0] * 1.05;
            double minM = s.masaPorDia[n - 1] * 0.95;
            if (maxM <= minM) maxM = minM + 1;

            g.setColor(PLOTBG); g.fillRect(ML, MT, pw, ph);

            int dFin1 = Math.min(s.dias1, n - 1);

            banda(g, ML, MT, pw, ph, n, 0,     dFin1, FASE1_COL, "plástico", s.tempFase1);
            banda(g, ML, MT, pw, ph, n, dFin1, n - 1, FASE2_COL, "aire libre", s.tempFase2);

            g.setFont(Tema.fuente(Font.PLAIN, 10));
            for (int i = 0; i <= 4; i++) {
                int yy = MT + ph - (int) ((double) i / 4 * ph);
                double mv = minM + (maxM - minM) * i / 4;
                g.setColor(GRID); g.drawLine(ML, yy, ML + pw, yy);
                g.setColor(MUTED);
                String lb = String.format("%.0f", mv);
                g.drawString(lb, ML - 7 - g.getFontMetrics().stringWidth(lb), yy + 4);
            }

            int step = Math.max(1, (int) Math.ceil((n - 1) / 7.0));
            for (int d = 0; d < n; d += step) {
                int xx = xDia(ML, pw, d, n);
                g.setColor(GRID); g.drawLine(xx, MT, xx, MT + ph);
                g.setColor(MUTED);
                String lb = "D" + d;
                g.drawString(lb, xx - g.getFontMetrics().stringWidth(lb) / 2, MT + ph + 15);
            }

            Polygon area = new Polygon();
            area.addPoint(ML, MT + ph);
            for (int i = 0; i < n; i++) area.addPoint(xDia(ML, pw, i, n), yMasa(s, i, MT, ph, minM, maxM));
            area.addPoint(ML + pw, MT + ph);
            g.setColor(new Color(mCol.getRed(), mCol.getGreen(), mCol.getBlue(), 26));
            g.fillPolygon(area);

            g.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < n - 1; i++) {
                g.setColor(colFase(i, dFin1));
                g.drawLine(xDia(ML, pw, i, n),     yMasa(s, i, MT, ph, minM, maxM),
                           xDia(ML, pw, i + 1, n), yMasa(s, i + 1, MT, ph, minM, maxM));
            }
            int markStep = Math.max(1, (int) Math.ceil(n / 18.0));
            for (int i = 0; i < n; i++) {
                if (i % markStep != 0 && i != n - 1) continue;
                int xi = xDia(ML, pw, i, n), yi = yMasa(s, i, MT, ph, minM, maxM);
                g.setColor(BG2);               g.fillOval(xi - 3, yi - 3, 6, 6);
                g.setColor(colFase(i, dFin1)); g.drawOval(xi - 3, yi - 3, 6, 6);
            }
            g.setStroke(new BasicStroke(1f));

            if (dFin1 > 0 && dFin1 < n) {
                int xb = xDia(ML, pw, dFin1, n), yb = yMasa(s, dFin1, MT, ph, minM, maxM);
                g.setFont(Tema.fuente(Font.BOLD, 10));
                String pl = String.format("−%.2f g", s.perdidaPlasticoG);
                int wPl = g.getFontMetrics().stringWidth(pl);
                g.setColor(FASE1_COL);
                g.drawString(pl, clampI(xb - wPl / 2, ML + 2, ML + pw - wPl - 2),
                                 Math.max(MT + 12, yb - 8));
            }

            int xl = xDia(ML, pw, n - 1, n), yl = yMasa(s, n - 1, MT, ph, minM, maxM);
            g.setFont(Tema.fuente(Font.BOLD, 10));
            String hf = String.format("%.1f %% H", s.humedadFinalPct);
            int wHf = g.getFontMetrics().stringWidth(hf);
            g.setColor(TEXT);
            g.drawString(hf, Math.max(ML + 2, xl - wHf - 6), Math.min(MT + ph - 4, yl + 14));

            g.setColor(BORD); g.drawRect(ML, MT, pw, ph);
            ejes(g, "Masa (g)", "Días (lectura cada 24 h)", ML, MT, pw, ph, H);
        }

        private void banda(Graphics2D g, int ML, int MT, int pw, int ph, int n,
                           int d0, int d1, Color col, String txt, double temp) {
            if (d1 <= d0) return;
            int x0 = xDia(ML, pw, d0, n), x1 = xDia(ML, pw, d1, n);
            g.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 20));
            g.fillRect(x0, MT, x1 - x0, ph);
            g.setColor(new Color(255, 255, 255, 26));
            if (d0 > 0) g.drawLine(x0, MT, x0, MT + ph);
            g.setFont(Tema.fuente(Font.BOLD, 10));
            String et = txt + "  " + String.format("%.0f°C", temp);
            int w = g.getFontMetrics().stringWidth(et);
            if (w + 8 <= x1 - x0) {
                g.setColor(col);
                g.drawString(et, (x0 + x1) / 2 - w / 2, MT + ph - 6);
            }
        }

        private Color colFase(int i, int d1) {
            return (i < d1) ? FASE1_COL : FASE2_COL;
        }
        private int xDia(int ML, int pw, int d, int n) {
            return ML + (int) ((double) d / Math.max(1, n - 1) * pw);
        }
        private int yMasa(Secado s, int i, int MT, int ph, double minM, double maxM) {
            return MT + ph - (int) ((s.masaPorDia[i] - minM) / (maxM - minM) * ph);
        }
    }

    private class CurvaCoccionMezclaPanel extends JPanel {
        final int idx;
        CurvaCoccionMezclaPanel(int idx) { this.idx = idx; setBackground(BG2); }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = mk2D(g0);
            int W = getWidth(), H = getHeight();
            g.setColor(BG2); g.fillRect(0, 0, W, H);

            Color mCol = AMBER;
            String nm  = "Todas las mezclas";

            boolean hay = tieneData && coccion != null;
            Coccion c = hay ? coccion : null;

            String der = hay ? String.format("%.0f °C · %s", c.tMeseta, fmtTiempo(c.tiempoTotalMin)) : "";
            cabecera(g, W, mCol, nm, "curva única — °C vs tiempo", der);
            if (!hay) { vacio(g, W, H); return; }

            int ML = 58, MR = 16, MT = 32, MB = 40;
            int pw = W - ML - MR, ph = H - MT - MB;
            if (pw < 60 || ph < 40) return;

            List<double[]> pts = new ArrayList<>();
            pts.add(new double[]{0, c.etapas[0][0]});
            double tAcum = 0;
            int idxEnfr = c.etapas.length - 1;
            for (int i = 0; i < c.etapas.length; i++) {
                double ini = c.etapas[i][0], fin = c.etapas[i][1], dur = c.etapas[i][2];
                if (i == idxEnfr && c.enfrNatural) {
                    int N = 60;
                    for (int k = 1; k <= N; k++) {
                        double tt = dur * k / N;
                        // Enfriamiento natural (Newton): T(t) = Tamb + (Tpico - Tamb)*e^(-k*t)
                        double T  = c.tAmb + (ini - c.tAmb) * Math.exp(-c.kEnfr * tt);
                        pts.add(new double[]{tAcum + tt, T});
                    }
                } else {
                    pts.add(new double[]{tAcum + dur, fin});
                }
                tAcum += dur;
            }
            double maxDur = Math.max(1, tAcum);
            double maxT   = Math.ceil(c.tMeseta * 1.18 / 100.0) * 100.0;

            double xPlatIni = c.etapas[0][2] + c.etapas[1][2] + c.etapas[2][2];
            double xOff     = xPlatIni + c.etapas[3][2];
            int xOffPx      = xMin(ML, pw, xOff, maxDur);

            g.setColor(PLOTBG); g.fillRect(ML, MT, pw, ph);

            g.setColor(new Color(255, 142, 142, 20));
            g.fillRect(xOffPx, MT, ML + pw - xOffPx, ph);

            g.setFont(Tema.fuente(Font.PLAIN, 10));
            for (int i = 0; i <= 4; i++) {
                int yy = MT + ph - (int) ((double) i / 4 * ph);
                g.setColor(GRID); g.drawLine(ML, yy, ML + pw, yy);
                g.setColor(MUTED);
                String lb = String.format("%.0f", maxT * i / 4);
                g.drawString(lb, ML - 7 - g.getFontMetrics().stringWidth(lb), yy + 4);
            }

            double totH = maxDur / 60.0;
            int stepH = totH <= 6 ? 1 : totH <= 14 ? 2 : totH <= 30 ? 4 : 6;
            for (int h = 0; h <= (int) Math.floor(totH); h += stepH) {
                int xx = xMin(ML, pw, h * 60.0, maxDur);
                g.setColor(GRID); g.drawLine(xx, MT, xx, MT + ph);
                g.setColor(MUTED);
                String lb = h + "h";
                g.drawString(lb, xx - g.getFontMetrics().stringWidth(lb) / 2, MT + ph + 15);
            }

            g.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < pts.size() - 1; i++) {
                double[] a = pts.get(i), b = pts.get(i + 1);
                g.setColor(a[0] >= xOff ? OFF_COL : mCol);
                g.drawLine(xMin(ML, pw, a[0], maxDur), yT(a[1], MT, ph, maxT),
                           xMin(ML, pw, b[0], maxDur), yT(b[1], MT, ph, maxT));
            }
            g.setStroke(new BasicStroke(1f));

            double[][] hitos = {
                {0, c.etapas[0][0]},
                {c.etapas[0][2], 200},
                {c.etapas[0][2] + c.etapas[1][2], 600},
                {xPlatIni, c.tMeseta},
                {xOff, c.tMeseta},
                {maxDur, c.tDesenhorne}
            };
            for (double[] hp : hitos) {
                int hx = xMin(ML, pw, hp[0], maxDur), hy = yT(hp[1], MT, ph, maxT);
                Color hc = hp[0] >= xOff && hp[0] > 0 ? OFF_COL : mCol;
                g.setColor(BG2); g.fillOval(hx - 4, hy - 4, 8, 8);
                g.setColor(hc);  g.drawOval(hx - 4, hy - 4, 8, 8);
            }

            int yPico = yT(c.tMeseta, MT, ph, maxT);
            g.setStroke(punteada(2, 4));
            g.setColor(new Color(255, 255, 255, 45));
            g.drawLine(ML, yPico, ML + pw, yPico);
            g.setStroke(new BasicStroke(1f));
            g.setFont(Tema.fuente(Font.BOLD, 11));
            String pico = String.format("%.0f °C", c.tMeseta);
            int wp = g.getFontMetrics().stringWidth(pico);
            int xp = clampI((xMin(ML, pw, xPlatIni, maxDur) + xOffPx) / 2 - wp / 2, ML + 4, ML + pw - wp - 4);
            g.setColor(TEXT);
            g.drawString(pico, xp, Math.max(MT + 12, yPico - 7));

            g.setStroke(punteada(3, 3));
            g.setColor(new Color(255, 142, 142, 130));
            g.drawLine(xOffPx, MT, xOffPx, MT + ph);
            g.setStroke(new BasicStroke(1f));
            g.setFont(Tema.fuente(Font.BOLD, 10));
            String off = "horno OFF · enfriamiento natural";
            int wo = g.getFontMetrics().stringWidth(off);
            if (wo + 10 > ML + pw - xOffPx) { off = "horno OFF"; wo = g.getFontMetrics().stringWidth(off); }
            g.setColor(OFF_COL);
            g.drawString(off, Math.min(xOffPx + 6, ML + pw - wo - 4), MT + ph - 8);

            g.setColor(BORD); g.drawRect(ML, MT, pw, ph);
            ejes(g, "T (°C)", "Tiempo (horas)", ML, MT, pw, ph, H);
        }

        private int xMin(int ML, int pw, double min, double maxDur) {
            return ML + (int) (min / maxDur * pw);
        }
        private int yT(double t, int MT, int ph, double maxT) {
            return MT + ph - (int) (t / maxT * ph);
        }
    }

    private Graphics2D mk2D(Graphics g0) {
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g;
    }

    private void cabecera(Graphics2D g, int W, Color mCol, String nm, String sub, String der) {
        g.setFont(Tema.fuente(Font.BOLD, 12));
        g.setColor(mCol);
        g.drawString(nm, 10, 18);
        int x = 10 + g.getFontMetrics().stringWidth(nm) + 8;

        g.setFont(Tema.fuente(Font.BOLD, 10));
        FontMetrics fmD = g.getFontMetrics();
        int wDer = der.isEmpty() ? 0 : fmD.stringWidth(der);
        if (!der.isEmpty() && W - wDer - 10 > x + 20) {
            g.setColor(TEXT);
            g.drawString(der, W - wDer - 10, 18);
        }
        g.setFont(Tema.fuente(Font.PLAIN, 10));
        int wSub = g.getFontMetrics().stringWidth(sub);
        if (x + wSub < W - wDer - 18) {
            g.setColor(DIM);
            g.drawString(sub, x, 18);
        }
    }

    private void vacio(Graphics2D g, int W, int H) {
        g.setColor(DIM);
        g.setFont(Tema.fuente(Font.PLAIN, 11));
        String msg = (mezclas == null)
                     ? "Calcule la mezcla en la pestaña Diseño de Mezcla"
                     : "Sin datos para esta mezcla";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(msg, (W - fm.stringWidth(msg)) / 2, H / 2 + 6);
    }

    private void ejes(Graphics2D g, String yLab, String xLab,
                      int ML, int MT, int pw, int ph, int H) {
        g.setFont(Tema.fuente(Font.PLAIN, 10));
        g.setColor(DIM);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.rotate(-Math.PI / 2);
        int wy = g2.getFontMetrics().stringWidth(yLab);
        g2.drawString(yLab, -(MT + ph / 2) - wy / 2, 13);
        g2.dispose();
        g.drawString(xLab, ML + pw / 2 - g.getFontMetrics().stringWidth(xLab) / 2, H - 8);
    }

    private BasicStroke punteada(float a, float b) {
        return new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[]{a, b}, 0f);
    }

    private String nombre(int i) {
        return (mezclasNombres != null && i < mezclasNombres.length && mezclasNombres[i] != null)
               ? mezclasNombres[i] : "M" + (i + 1);
    }

    private void actualizarSecadoUI() {
        secadoResumenPanel.removeAll();

        if (!tieneData || secados == null) {
            JPanel card = mkCard("Secado — resumen", ACCENT);
            card.add(msgVacio("Sin datos de secado aún."), BorderLayout.CENTER);
            secadoResumenPanel.add(card);
            secadoResumenPanel.revalidate();
            secadoResumenPanel.repaint();
            return;
        }

        for (int m = 0; m < secados.length; m++) {
            Secado s = secados[m];
            if (s == null) continue;
            Color accent = M_COLS[m % M_COLS.length];

            JPanel card = mkCard("Secado · " + nombre(m) + "   —   " + fmtDias(s.diasTotal), accent);

            int nDias = s.masaPorDia.length;

            int paso = (nDias <= 16) ? 1 : (nDias <= 32 ? 2 : (nDias <= 60 ? 3 : (int) Math.ceil(nDias / 20.0)));
            List<Integer> filas = new ArrayList<>();
            for (int d = 0; d < nDias; d += paso) filas.add(d);
            if (filas.get(filas.size() - 1) != nDias - 1) filas.add(nDias - 1);

            String[] headers = {"Día", "Masa (g)",
                    paso == 1 ? "Δ 24 h (g)" : "Δ " + paso + " días (g)", "Humedad (%)", "Fase"};
            String[][] data = new String[filas.size()][5];
            Color[][] fg = new Color[filas.size()][5];
            int dFin1 = s.dias1;

            for (int r = 0; r < filas.size(); r++) {
                int d = filas.get(r);
                String fase = (d < dFin1) ? "plástico" : "aire libre";
                Color fc = (d < dFin1) ? FASE1_COL : FASE2_COL;
                data[r][0] = "D" + d;
                data[r][1] = String.format("%.1f", s.masaPorDia[d]);
                data[r][2] = (r == 0) ? "—"
                        : String.format("−%.1f", s.masaPorDia[filas.get(r - 1)] - s.masaPorDia[d]);
                data[r][3] = String.format("%.1f", s.humedadPorDia[d]);
                data[r][4] = fase;
                fg[r][0] = MUTED;
                fg[r][1] = TEXT;
                fg[r][2] = (d <= dFin1) ? FASE1_COL : DIM;
                fg[r][3] = (s.humedadPorDia[d] <= s.hEstable * 1.15) ? GREEN
                         : (s.humedadPorDia[d] <= 8 ? AMBER : MUTED);
                fg[r][4] = fc;
            }

            card.add(crearTablaExcel(headers, data, accent, 0.16, fg), BorderLayout.CENTER);

            secadoResumenPanel.add(card);
            secadoResumenPanel.add(Box.createVerticalStrut(12));
        }
        secadoResumenPanel.revalidate();
        secadoResumenPanel.repaint();
    }

    private void actualizarCoccionUI() {
        coccionResumenPanel.removeAll();

        if (!tieneData || coccion == null) {
            JPanel card = mkCard("Cocción — resumen", AMBER);
            card.add(msgVacio("Sin datos de cocción aún."), BorderLayout.CENTER);
            coccionResumenPanel.add(card);
            coccionResumenPanel.revalidate();
            coccionResumenPanel.repaint();
            return;
        }

        Coccion c = coccion;
        JPanel card = mkCard("Cocción · todas las mezclas   —   "
                + fmtTiempo(c.tiempoTotalMin) + " de ciclo", AMBER);

        String[] headers = {"Etapa / parámetro", "Valor"};
        String[][] data = {
            {"Temperatura de pico",     String.format("%.0f °C  (óptimo del feldespato)", c.tMeseta)},
            {"25 → 200 °C",             String.format("%.0f °C/h · %s", c.etapas[0][3] * 60, fmtTiempo(c.etapas[0][2]))},
            {"200 → 600 °C",            String.format("%.0f °C/h · %s", c.etapas[1][3] * 60, fmtTiempo(c.etapas[1][2]))},
            {"600 → pico",              String.format("%.0f °C/h · %s", c.etapas[2][3] * 60, fmtTiempo(c.etapas[2][2]))},
            {"Permanencia en el pico",  fmtTiempo(c.etapas[3][2])},
            {"ENFRIAMIENTO",            "horno apagado — no controlado"}
        };
        Color[][] fg = new Color[data.length][2];
        for (int r = 0; r < data.length; r++) {
            boolean enfr = r == 5;
            fg[r][0] = enfr ? OFF_COL : DIM;
            fg[r][1] = enfr ? OFF_COL : TEXT;
        }
        fg[0][1] = AMBER;
        card.add(crearTablaExcel(headers, data, AMBER, 0.44, fg), BorderLayout.CENTER);
        card.add(nota(String.format(
                "Una sola curva para todas las mezclas, con pico en %.0f °C: ahí el feldespato funde mejor "
              + "y forma la fase vítrea. Enfriamiento natural.",
                c.tMeseta)), BorderLayout.SOUTH);
        coccionResumenPanel.add(card);
        coccionResumenPanel.add(Box.createVerticalStrut(12));

        coccionResumenPanel.revalidate();
        coccionResumenPanel.repaint();
    }

    private String diaHumedad(Secado s, double umbral) {
        for (int i = 0; i < s.humedadPorDia.length; i++)
            if (s.humedadPorDia[i] <= umbral) return "D" + i;
        return "—";
    }

    private String fmtDias(int dias) {
        if (dias >= 10) return dias + " días  (≈ " + String.format("%.1f", dias / 7.0) + " semanas)";
        return dias + " días";
    }

    private String fmtTiempo(double minTotal) {
        int h = (int) (minTotal / 60);
        int mm = (int) Math.round(minTotal % 60);
        if (mm == 60) { h++; mm = 0; }
        if (h > 0) return h + " h " + String.format("%02d", mm) + " min";
        return mm + " min";
    }

    private JPanel crearTablaExcel(String[] headers, String[][] data, Color accent) {
        return crearTablaExcel(headers, data, accent, 0.35, null);
    }

    private JPanel crearTablaExcel(String[] headers, String[][] data, Color accent,
                                   double w0, Color[][] fgOverride) {
        JPanel table = new JPanel(new GridBagLayout());
        table.setBackground(BG2);
        table.setBorder(BorderFactory.createLineBorder(BORD));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 0;

        int nc = headers.length;
        double wOtras = (1.0 - w0) / Math.max(1, nc - 1);

        for (int c = 0; c < nc; c++) {
            gbc.gridx = c; gbc.gridy = 0;
            gbc.weightx = (c == 0) ? w0 : wOtras;
            JLabel h = new JLabel(headers[c], c == 0 ? SwingConstants.LEFT : SwingConstants.CENTER);
            h.setFont(Tema.ui(Font.BOLD, 11));
            h.setForeground(Color.WHITE);
            h.setOpaque(true);
            h.setBackground(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 190));
            h.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, (c < nc - 1 ? 1 : 0), BORD),
                    new EmptyBorder(6, 7, 6, 7)));
            table.add(h, gbc);
        }

        for (int r = 0; r < data.length; r++) {
            Color rowBg = (r % 2 == 0) ? BG2 : BG3;
            for (int c = 0; c < data[r].length; c++) {
                gbc.gridx = c; gbc.gridy = r + 1;
                gbc.weightx = (c == 0) ? w0 : wOtras;
                JLabel cell = new JLabel(data[r][c] == null ? "" : data[r][c],
                        c == 0 ? SwingConstants.LEFT : SwingConstants.CENTER);
                cell.setFont(Tema.ui(c == 0 ? Font.PLAIN : Font.BOLD, 11));
                Color fc = (fgOverride != null && r < fgOverride.length
                            && c < fgOverride[r].length && fgOverride[r][c] != null)
                           ? fgOverride[r][c] : (c == 0 ? DIM : TEXT);
                cell.setForeground(fc);
                cell.setOpaque(true);
                cell.setBackground(rowBg);
                cell.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1,
                                (c < data[r].length - 1 ? 1 : 0), new Color(38, 46, 70)),
                        new EmptyBorder(5, 7, 5, 7)));
                table.add(cell, gbc);
            }
        }
        return table;
    }

    private static class Nota extends JTextArea {
        private boolean midiendo = false;
        private int ultimoAncho = -1;

        Nota(String txt) {
            super(txt);
            setLineWrap(true);
            setWrapStyleWord(true);
            setEditable(false);
            setFocusable(false);
            setOpaque(false);
            setFont(Tema.ui(Font.PLAIN, 10));
            setForeground(DIM);
            setBorder(BorderFactory.createEmptyBorder(8, 2, 2, 2));
        }

        @Override public Dimension getPreferredSize() {
            if (midiendo) return super.getPreferredSize();
            int w = getWidth();
            if (w <= 0) {
                Container p = getParent();
                if (p != null) {
                    Insets in = p.getInsets();
                    w = p.getWidth() - in.left - in.right;
                }
            }
            if (w <= 0) w = 380;
            midiendo = true;
            setSize(w, Short.MAX_VALUE);
            Dimension d = super.getPreferredSize();
            midiendo = false;
            if (w != ultimoAncho) {
                ultimoAncho = w;
                SwingUtilities.invokeLater(() -> {
                    Container c = getParent();
                    if (c != null) { c.invalidate(); c.revalidate(); c.repaint(); }
                });
            }
            return new Dimension(w, d.height + 4);
        }
    }

    private JTextArea nota(String txt) { return new Nota(txt); }

    private JLabel msgVacio(String txt) {
        JLabel l = new JLabel(txt + "  Calcule la mezcla en la pestaña Diseño de Mezcla.");
        l.setFont(Tema.ui(Font.ITALIC, 11));
        l.setForeground(DIM);
        l.setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
        return l;
    }

    private JPanel mkCard(String t, Color accent) {
        JPanel w = new JPanel(new BorderLayout(0, 6)) {
            @Override public Dimension getMaximumSize() {

                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height + 2);
            }
            @Override public Dimension getMinimumSize() {

                return getPreferredSize();
            }
        };
        w.setBackground(BG2);
        w.setAlignmentX(Component.LEFT_ALIGNMENT);
        w.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, accent),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORD),
                        BorderFactory.createEmptyBorder(9, 11, 14, 11))));
        JLabel tl = new JLabel(t);
        tl.setFont(Tema.ui(Font.BOLD, 12));
        tl.setForeground(accent);
        tl.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        w.add(tl, BorderLayout.NORTH);
        return w;
    }

    private static JLabel lbl(String t, Color c, int sz, boolean b) {
        JLabel l = new JLabel(t);
        l.setFont(Tema.fuente(b ? Font.BOLD : Font.PLAIN, sz));
        l.setForeground(c);
        return l;
    }

    private static double clamp(double v, double mn, double mx) {
        return Math.max(mn, Math.min(mx, v));
    }

    private static int clampI(int v, int mn, int mx) {
        return Math.max(mn, Math.min(mx, v));
    }
}