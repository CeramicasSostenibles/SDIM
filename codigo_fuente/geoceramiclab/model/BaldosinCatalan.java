package geoceramiclab.model;

public class BaldosinCatalan {

    public enum TipoCara {
        NERVADA_UNA("Nervada por una cara"),
        LISA_AMBAS ("Lisa por ambas caras");

        private final String etiqueta;
        TipoCara(String e){ this.etiqueta = e; }
        @Override public String toString(){ return etiqueta; }
    }

    private static TipoCara TIPO_GLOBAL = TipoCara.NERVADA_UNA;

    public static TipoCara getTipoGlobal() { return TIPO_GLOBAL; }

    public static void setTipoGlobal(TipoCara t) {
        if (t != null) TIPO_GLOBAL = t;
    }

    public TipoCara tipo = TIPO_GLOBAL;

    public BaldosinCatalan() {
        this.tipo = TIPO_GLOBAL;
    }

    public BaldosinCatalan(TipoCara tipo) {
        this.tipo = (tipo != null) ? tipo : TIPO_GLOBAL;
    }

    public boolean esNervada(){ return tipo == TipoCara.NERVADA_UNA; }

    public boolean tieneNervios(){ return esNervada() && numNerviosInt > 0 && altoNervio > 0; }

    public int numRanuras(){ return tieneNervios() ? numNerviosInt + 1 : 0; }

    public int numNerviosTotales(){ return tieneNervios() ? numNerviosInt + 2 : 0; }

    private static BaldosinCatalan INSTANCIA;

    public static BaldosinCatalan actual() {
        if (INSTANCIA == null) {
            INSTANCIA = new BaldosinCatalan();
            INSTANCIA.calcularGeometria();
        }
        return INSTANCIA;
    }

    public static void setActual(BaldosinCatalan b) {
        INSTANCIA = b;
        if (b != null) TIPO_GLOBAL = b.tipo;
    }

    public double ancho   = 10.0;
    public double largo   = 20.0;
    public double espesor =  1.5;
    // Espesor nominal = espesor total que escribe el usuario (base + nervio).
    // El espesor de la base y el alto del nervio se derivan de el (2/3 y 1/3).
    public double espesorNominal = 1.5;
    public double ranura  =  0.30;

    public int    numNerviosInt   = 0;
    public double nervCentral     = 0;
    public double nervBorde       = 0;
    public double altoNervio      = 0;
    public double altoNervioAlto  = 0;
    public double anguloDesm      = 3.0;

    public double volBase         = 0;
    public double volNervios      = 0;
    public double volHuecos       = 0;
    public double volNeto         = 0;

    public int    numTornillos    = 4;
    public double fuerzaPorTorn   = 0;
    public double presionReal     = 0;

    public static final double TORQUE_NM  = 120.0;
    public static final double K_FRICTION = 0.20;
    public static final double D_TORN_M   = 0.012;
    public static final double PRES_OBJET = 5.0;

    public static final double DENSIDAD_COCIDA = 1.85;
    public static final double ABSORCION_AIIb_MIN = 6.0;
    public static final double ABSORCION_AIIb_MAX = 10.0;

    public static class MezclaBaldosin {
        public double masaSuelo, masaAgua, masaCal, masaCemento;
        public double masaFeldespato, masaSilice;
        public double fracSuelo, fracCal, fracCemento;
        public double fracFeldespato, fracSilice;
        public double relacionAguaSolidos;
        public double densidadAparente, densidadVerde, densidadCocida;
        public double masaTotalSolidos, masaTotalVerde;
        public double absorcionAgua, contraccionLineal;
        public String grupoISO, grupoDensidad;
        public String[] aditivos, razones;
        public String advertencia = "";
        public double resistenciaFlexion, cargaRotura;
        public boolean cumpleFlexion;
        public String clasificacionNTC;
    }

    public static final double NB_FIJO  = 0.80;
    public static final double NC_FIJO  = 0.30;
    public static final double NCC_FIJO = 0.40;
    public static final double R_FIJO   = 0.30;
    public static final double H_TOTAL  = 0.80;

    public int nLado    = 0;
    public int nCentral = 0;

    public double anchoNervLateral  = NC_FIJO;
    public double anchoNervCentral  = NCC_FIJO;

    public void calcularGeometria() {

        if (espesorNominal <= 0) espesorNominal = espesor;

        if (tipo == TipoCara.LISA_AMBAS) {
            espesor        = espesorNominal;
            altoNervio     = 0.0;
            altoNervioAlto = 0.0;

            nLado = 0;  nCentral = 0;  numNerviosInt = 0;
            nervBorde = 0.0;  nervCentral = 0.0;  ranura = 0.0;
            anchoNervLateral = 0.0;  anchoNervCentral = 0.0;

            volBase    = ancho * largo * espesor;
            volNervios = 0.0;
            volHuecos  = 0.0;
            volNeto    = volBase;

            calcularTornillos();
            return;
        }

        // El espesor escrito es el TOTAL (base + nervio): base = 2/3, nervio = 1/3
        espesor        = espesorNominal * 2.0 / 3.0;
        altoNervio     = espesorNominal * 1.0 / 3.0;
        altoNervioAlto = altoNervio;

        double nb = NB_FIJO;
        double nc = NC_FIJO;
        nervBorde        = nb;
        nervCentral      = nc;
        anchoNervLateral = nc;
        anchoNervCentral = nc;

        double espacioInt = ancho - 2.0 * nb;

        // Se respeta el numero de nervios escrito por el usuario; si no hay, se estima
        int N = numNerviosInt;
        if (N <= 0) N = Math.max(1, (int) Math.round(espacioInt / (nc + R_FIJO)) - 1);

        // Ranura que reparte los N nervios llenando el ancho: r = (libre - N*nc)/(N+1)
        double rFill = (espacioInt - N * nc) / (N + 1);
        double rMin  = Math.max(0.03, nc * 0.20);
        if (rFill < rMin) {                 // no caben: se adelgazan los nervios
            nc = Math.max(0.03, (espacioInt - (N + 1) * rMin) / N);
            nervCentral = nc; anchoNervLateral = nc; anchoNervCentral = nc;
            rFill = (espacioInt - N * nc) / (N + 1);
        }
        ranura = rFill;

        nLado         = 0;
        nCentral      = N;
        numNerviosInt = N;

        volBase = ancho * largo * espesor;
        double hN = altoNervio;
        volNervios = (N * nc + 2.0 * nb) * hN * largo;
        int numRanuras = N + 1;
        volHuecos  = numRanuras * ranura * hN * largo;
        volNeto    = volBase + volNervios;

        calcularTornillos();
    }

    private void calcularTornillos() {
        double areaM2 = (ancho/100.0) * (largo/100.0);
        fuerzaPorTorn = TORQUE_NM / (K_FRICTION * D_TORN_M);
        double nMin   = PRES_OBJET * 1e6 * areaM2 / fuerzaPorTorn;
        numTornillos  = Math.max(4, (int)(Math.ceil(nMin/2.0))*2);
        presionReal   = (numTornillos * fuerzaPorTorn) / areaM2 / 1e6;
    }

    public double espesorTotal() {
        return esNervada() ? espesor + altoNervio : espesor;
    }

    public double espesorMaximo() {
        return esNervada() ? espesor + altoNervio : espesor;
    }

    public double superficieExpuesta() {
        if (!esNervada()) {
            return 2.0*ancho*largo + 2.0*(ancho + largo)*espesor;
        }
        double hN = altoNervio;
        double anchoNerviosTotal = 2.0*nervBorde
                                 + 2.0*nLado*anchoNervLateral
                                 + nCentral*anchoNervCentral;
        double dorsoDescubierto = ancho*largo - anchoNerviosTotal*largo;
        int numRanuras = numNerviosInt + 1;
        return  ancho*largo
              + dorsoDescubierto
              + anchoNerviosTotal*largo
              + 2.0*numRanuras*hN*largo
              + 2.0*(ancho + largo)*espesor
              + 2.0*anchoNerviosTotal*hN;
    }

    public double espesorEquivalente() {
        double s = superficieExpuesta();
        return s > 0 ? 2.0 * volNeto / s : espesor;
    }

    public boolean esNervioAlto(int i) {
        if (numNerviosInt <= 0 || i < 0) return false;
        return i >= nLado && i < nLado + nCentral;
    }

    public int numNerviosAltos() { return nCentral; }

    public double anchoNervio(int i) {
        if (!esNervada()) return 0.0;
        return esNervioAlto(i) ? NCC_FIJO : NC_FIJO;
    }

    public double[][] trazadoNervios() {
        if (!tieneNervios()) return new double[0][];

        double[][] t = new double[numNerviosInt + 2][3];
        double x = 0;
        int k = 0;

        t[k][0] = x; t[k][1] = nervBorde; t[k][2] = 1; k++;
        x += nervBorde + ranura;

        for (int i = 0; i < numNerviosInt; i++) {
            double a = anchoNervio(i);
            t[k][0] = x; t[k][1] = a; t[k][2] = esNervioAlto(i) ? 1 : 0; k++;
            x += a + ranura;
        }

        t[k][0] = x; t[k][1] = nervBorde; t[k][2] = 1;
        return t;
    }

    public double[] posicionesTornillosX() {
        int n = numTornillos / 2;
        double[] pos = new double[n];
        double margen = (esNervada() ? nervBorde * 0.5 : 0.40) + 0.3;
        double espacioDisp = ancho - 2 * margen;
        for (int i = 0; i < n; i++) {
            pos[i] = margen + espacioDisp * i / Math.max(1, n - 1);
        }
        return pos;
    }

    public MezclaBaldosin calcularMezcla(SueloData datos, double densidadObj) {
        calcularGeometria();
        MezclaBaldosin m = new MezclaBaldosin();

        double pctArc = datos.hidroCalculada ? datos.pctArcilla : 20;
        double IP     = datos.atterCalculada ? datos.IP         : 15;
        double LL     = datos.atterCalculada ? datos.LL         : 35;
        double pctFin = datos.granCalculada  ? datos.pctFinos   : 55;

        double fSuelo = clamp(0.60 + (pctArc/100.0)*0.18, 0.55, 0.80);
        double fCal=0, fFeld=0, fSil=0;
        java.util.List<String> adits = new java.util.ArrayList<>();
        java.util.List<String> razon = new java.util.ArrayList<>();

        if (IP>7 || pctArc>15) {
            fCal = clamp(0.04+(IP-7)*0.002, 0.04, 0.15);
            adits.add("Cal hidratada Ca(OH)₂");
            razon.add(String.format("IP=%.0f → reduce plasticidad", IP));
        }
        if (densidadObj>1.70 || pctArc>20) {
            fFeld = clamp(0.06+(densidadObj-1.70)*0.04, 0.06, 0.18);
            adits.add("Feldespato potásico");
            razon.add("Fundente para control absorción AIIb-AIII");
        }
        if (pctArc>30 || LL>45) {
            fSil = clamp(0.04+(pctArc-30)*0.002, 0.04, 0.12);
            adits.add("Cuarzo/Sílice SiO₂");
            razon.add("Reduce contracción en cocción");
        }

        double sumAdit = fCal+fFeld+fSil;
        fSuelo = clamp(1.0-sumAdit, 0.50, 0.85);

        // Agua de amasado: w = 0.20 + 0.10*(%arcilla) - 0.15*(fraccion de cal)
        double wRel   = clamp(0.20+(pctArc/100.0)*0.10-fCal*0.15, 0.18, 0.30);
        // Masa de solidos = densidad * volumen neto
        double masaSol= densidadObj*volNeto;
        double masaAg = masaSol*wRel;

        m.masaSuelo=masaSol*fSuelo; m.masaCal=masaSol*fCal;
        m.masaFeldespato=masaSol*fFeld; m.masaSilice=masaSol*fSil;
        m.masaAgua=masaAg;
        m.fracSuelo=fSuelo; m.fracCal=fCal;
        m.fracFeldespato=fFeld; m.fracSilice=fSil;
        m.relacionAguaSolidos=wRel;
        m.masaTotalSolidos=masaSol; m.masaTotalVerde=masaSol+masaAg;
        m.densidadAparente=densidadObj;
        m.densidadVerde=m.masaTotalVerde/volNeto;
        m.contraccionLineal=clamp(6.0+pctArc*0.15-fFeld*40, 4.0, 14.0);
        double fc=Math.pow(1-m.contraccionLineal/100.0, 3);
        m.densidadCocida=clamp(densidadObj/fc*0.92, 1.55, 2.20);
        m.absorcionAgua=clamp(28-densidadObj*12-fFeld*30, 4.0, 25.0);
        m.grupoISO = m.absorcionAgua>10 ? "AIII" : "AIIb";
        m.aditivos=adits.toArray(new String[0]);
        m.razones =razon.toArray(new String[0]);
        return m;
    }

    private static double clamp(double v,double mn,double mx){ return Math.max(mn,Math.min(mx,v)); }
}