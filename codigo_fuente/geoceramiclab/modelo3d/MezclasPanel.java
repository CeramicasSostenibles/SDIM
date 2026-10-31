package geoceramiclab.modelo3d;

import geoceramiclab.Tema;
import geoceramiclab.graficas.CopyImageMenu;

import geoceramiclab.model.BaldosinCatalan;
import geoceramiclab.model.BaldosinCatalan.MezclaBaldosin;
import geoceramiclab.model.SueloData;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.geom.*;
import javax.swing.Timer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MezclasPanel extends JPanel {

    private static final Color BG    = Tema.FONDO;
    private static final Color BG2   = Tema.SUPERFICIE;
    private static final Color BG3   = Tema.SUPERFICIE_2;
    private static final Color BORD  = Tema.BORDE;
    private static final Color ACCENT= Tema.INFO;
    private static final Color GREEN = Tema.OK;
    private static final Color AMBER = Tema.AVISO;
    private static final Color RED   = Tema.ERROR;
    private static final Color TEXT  = Tema.TEXTO;
    private static final Color MUTED = Tema.TEXTO_2;
    private static final Color DIM   = Tema.TEXTO_3;
    private static final Color RELAVE= new Color(160,140,120);
    private static final Color C1    = Tema.INFO;
    private static final Color C2    = Tema.OK;
    private static final Color C3    = Tema.AVISO;
    private static final Color CAOLIN= new Color(185,215,235);
    private static final Color FELD  = new Color(210,195,175);

    private static final String[] NOMS = {"M1 — Relave alto","M2 — Intermedia","M3 — Fundente alto"};

    private static final double[][] DOSIS_DEF = {
        {75, 20,  5, 10},
        {65, 25, 10, 13},
        {55, 30, 15, 16}
    };

    private final SueloData datos;
    private EnsayosNTC ensayosNTC;
    private SecadoCoccion secadoCoccion;

    private final JTextField fAncho, fLargo, fEspesor, fNervios, fDensidad;
    private final JLabel     lblVolCalc;

    private final JTextField[] fDosRel  = new JTextField[3];
    private final JTextField[] fDosCao  = new JTextField[3];
    private final JTextField[] fDosFeld = new JTextField[3];
    private final JTextField[] fDosAgua = new JTextField[3];
    private final JLabel[]     lDosSum  = new JLabel[3];
    private final JLabel       lblFormula;
    private final JLabel       lblRegla;

    private final JToggleButton btnM1, btnM2, btnM3;
    private int mezclaActiva = 0;
    private String firmaSuelo = null;

    private final JTextField fRelave, fCaolin, fFeld, fAgua;
    private final JLabel lFracRelave, lFracCaolin, lFracFeld, lRatioAgua;

    private final AnimPanel animPanel;

    private double[][] masas       = new double[3][3];
    private double[]   aguaM       = new double[3];
    private double[]   humM        = new double[3];
    private double[]   presionCalc = new double[3];

    public MezclasPanel(SueloData datos) {
        this.datos = datos;
        setLayout(new BorderLayout(0,0));
        setBackground(BG);

        JPanel top = new JPanel(new BorderLayout(12,0));
        top.setBackground(BG2);
        top.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0,0,1,0,BORD),
            BorderFactory.createEmptyBorder(5,14,5,14)));

        JPanel paramRow = new JPanel(new GridBagLayout());
        paramRow.setBackground(BG2);
        GridBagConstraints gp = new GridBagConstraints();
        gp.insets=new Insets(2,6,2,6); gp.anchor=GridBagConstraints.WEST; gp.fill=GridBagConstraints.HORIZONTAL;
        fAncho   = fld("20.0",70);
        fLargo   = fld("20.0",70);
        fEspesor = fld("1.2", 70);
        fNervios = fld("4",   70);
        // Las dimensiones se definen en la pieza 3D (fuente unica): aqui solo se muestran
        for (JTextField f : new JTextField[]{fAncho, fLargo, fEspesor, fNervios}) {
            f.setEditable(false);
            f.setFocusable(false);
            f.setToolTipText("Se define en la pestana de la pieza 3D (dimensiones)");
        }
        lblVolCalc = vl("—", ACCENT);
        fDensidad = fld("1.85",70); fDensidad.setForeground(AMBER);
        lblFormula = vl("m\u209b\u00f3l = \u03c1\u00b7V = —", GREEN);
        int fila=0;
        fila=filaParam(paramRow,gp,fila,"Ancho (cm):",   fAncho);
        fila=filaParam(paramRow,gp,fila,"Largo (cm):",   fLargo);
        fila=filaParam(paramRow,gp,fila,"Espesor (cm):", fEspesor);
        fila=filaParam(paramRow,gp,fila,"Nervios:",      fNervios);
        fila=filaParam(paramRow,gp,fila,"Vol. calc.:",   lblVolCalc);
        fila=filaParam(paramRow,gp,fila,"Densidad estimada (g/cm\u00b3):", fDensidad);
        fila=filaParam(paramRow,gp,fila,"Masa s\u00f3lidos:", lblFormula);

        JPanel dosPanel = new JPanel(new GridBagLayout());
        dosPanel.setBackground(BG2);
        dosPanel.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        GridBagConstraints gd = new GridBagConstraints();
        gd.insets=new Insets(1,4,1,4); gd.fill=GridBagConstraints.HORIZONTAL;
        String[] hd={"Dosificaci\u00f3n","Relave %","Caol\u00edn %","Feldespato %","Agua % (s/s\u00f3lidos)","\u03a3 s\u00f3lidos"};
        for(int i=0;i<hd.length;i++){
            gd.gridx=i; gd.gridy=0;
            JLabel h=new JLabel(hd[i]);
            h.setFont(Tema.ui(Font.BOLD, 10)); h.setForeground(DIM);
            dosPanel.add(h,gd);
        }

        Regla.Propuesta prop0=propuestaDesdeSuelo();
        double[][] dosisIni=(prop0!=null)?prop0.dosis:DOSIS_DEF;
        Color[] cM={C1,C2,C3};
        for(int m=0;m<3;m++){
            gd.gridy=m+1;
            gd.gridx=0; JLabel ln=new JLabel(NOMS[m]);
            ln.setFont(Tema.ui(Font.BOLD, 10)); ln.setForeground(cM[m]);
            dosPanel.add(ln,gd);
            gd.gridx=1; fDosRel [m]=fld(fmt1(dosisIni[m][0]),55); fDosRel [m].setForeground(RELAVE); dosPanel.add(fDosRel [m],gd);
            gd.gridx=2; fDosCao [m]=fld(fmt1(dosisIni[m][1]),55); fDosCao [m].setForeground(CAOLIN); dosPanel.add(fDosCao [m],gd);
            gd.gridx=3; fDosFeld[m]=fld(fmt1(dosisIni[m][2]),55); fDosFeld[m].setForeground(FELD);   dosPanel.add(fDosFeld[m],gd);
            gd.gridx=4; fDosAgua[m]=fld(fmt1(dosisIni[m][3]),55); fDosAgua[m].setForeground(ACCENT); dosPanel.add(fDosAgua[m],gd);
            gd.gridx=5; lDosSum [m]=vl("—",GREEN); dosPanel.add(lDosSum[m],gd);
        }
        lblRegla=new JLabel(prop0!=null?"Propuesta autom\u00e1tica: "+prop0.resumen:"Sin datos de suelo: dosis por defecto");
        lblRegla.setFont(Tema.ui(Font.PLAIN, 10)); lblRegla.setForeground(MUTED);
        JPanel propRow=new JPanel(new FlowLayout(FlowLayout.LEFT,6,2));
        propRow.setBackground(BG2);
        propRow.add(lblRegla);
        gd.gridx=0; gd.gridy=4; gd.gridwidth=hd.length;
        dosPanel.add(propRow,gd);
        gd.gridwidth=1;

        DocumentListener dosL=new DocumentListener(){
            public void changedUpdate(DocumentEvent e){refrescarSuma();}
            public void insertUpdate (DocumentEvent e){refrescarSuma();}
            public void removeUpdate (DocumentEvent e){refrescarSuma();}
        };
        for(int m=0;m<3;m++){
            fDosRel [m].getDocument().addDocumentListener(dosL);
            fDosCao [m].getDocument().addDocumentListener(dosL);
            fDosFeld[m].getDocument().addDocumentListener(dosL);
        }
        refrescarSuma();

        ButtonGroup bg = new ButtonGroup();
        btnM1 = tab(NOMS[0], C1); bg.add(btnM1); btnM1.setSelected(true);
        btnM2 = tab(NOMS[1], C2); bg.add(btnM2);
        btnM3 = tab(NOMS[2], C3); bg.add(btnM3);
        JPanel tabRow = new JPanel(new FlowLayout(FlowLayout.LEFT,4,0));
        tabRow.setBackground(BG2);
        tabRow.add(btnM1); tabRow.add(btnM2); tabRow.add(btnM3);
        btnM1.addActionListener(e->{mezclaActiva=0; mostrarMezclaActiva(); enviarAEnsayos();});
        btnM2.addActionListener(e->{mezclaActiva=1; mostrarMezclaActiva(); enviarAEnsayos();});
        btnM3.addActionListener(e->{mezclaActiva=2; mostrarMezclaActiva(); enviarAEnsayos();});

        JButton btnCalc = Tema.boton("Calcular 3 mezclas", Tema.Tipo.PRIMARIO);
        btnCalc.addActionListener(e->calcular(false));

        top.add(tabRow,  BorderLayout.CENTER);
        tabRow.add(Box.createHorizontalStrut(14));
        tabRow.add(btnCalc);
        add(top, BorderLayout.NORTH);

        animPanel = new AnimPanel();
        animPanel.setPreferredSize(new Dimension(300,100));
        animPanel.setMinimumSize(new Dimension(280,100));

        JPanel datosPanel = new JPanel(new GridBagLayout());
        datosPanel.setBackground(BG);
        datosPanel.setBorder(BorderFactory.createEmptyBorder(4,10,6,14));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill=GridBagConstraints.BOTH; gc.insets=new Insets(0,0,5,0);

        gc.gridx=0; gc.gridy=0; gc.weightx=1; gc.weighty=0.5;
        JPanel rellenoSup=new JPanel(); rellenoSup.setBackground(BG);
        datosPanel.add(rellenoSup, gc);

        gc.gridy=1; gc.weighty=0;
        JPanel dimSec = seccion("Dimensiones de la baldosa y densidad estimada — m\u209b\u00f3l = \u03c1 \u00b7 V");
        dimSec.setLayout(new FlowLayout(FlowLayout.LEFT,0,0));
        dimSec.add(paramRow);
        datosPanel.add(dimSec, gc);

        gc.gridy=2; gc.weighty=0;
        JPanel dosSec = seccion("Dosificaci\u00f3n de las 3 mezclas — Relave / Caol\u00edn / Feldespato / Agua");
        dosSec.setLayout(new FlowLayout(FlowLayout.LEFT,0,0));
        dosSec.add(dosPanel);
        datosPanel.add(dosSec, gc);

        gc.gridy=3; gc.weighty=0;
        JPanel cantPanel = seccion("Cantidades por baldosa — Mezcla activa (editables)");
        cantPanel.setLayout(new GridBagLayout());
        GridBagConstraints gc2=new GridBagConstraints();
        gc2.fill=GridBagConstraints.HORIZONTAL; gc2.insets=new Insets(2,4,2,4);
        String[] cabs={"Material","Cantidad","Unid.","% / Rol"};
        for(int i=0;i<cabs.length;i++){
            gc2.gridx=i; gc2.gridy=0; gc2.weightx=i==3?1:0;
            JLabel h=new JLabel(cabs[i]);
            h.setFont(Tema.ui(Font.BOLD, 10)); h.setForeground(DIM);
            h.setBorder(BorderFactory.createEmptyBorder(2,4,4,4)); cantPanel.add(h,gc2);
        }
        fRelave = fldEdit("—"); lFracRelave = vl("—",RELAVE);
        fCaolin = fldEdit("—"); lFracCaolin = vl("—",CAOLIN);
        fFeld   = fldEdit("—"); lFracFeld   = vl("—",FELD);
        fAgua   = fldEdit("—"); fAgua.setForeground(ACCENT);
        lRatioAgua = vl("—",ACCENT);

        addFilaCant(cantPanel,gc2,1,"Relave",       RELAVE, fRelave,"g", lFracRelave);
        addFilaCant(cantPanel,gc2,2,"Caol\u00edn",  CAOLIN, fCaolin,"g", lFracCaolin);
        addFilaCant(cantPanel,gc2,3,"Feldespato K", FELD,   fFeld,  "g", lFracFeld);
        addFilaCant(cantPanel,gc2,4,"Agua",         ACCENT, fAgua,  "ml",lRatioAgua);

        gc2.gridx=0; gc2.gridy=5; gc2.gridwidth=4; gc2.weightx=1;
        JPanel totRow=new JPanel(new FlowLayout(FlowLayout.LEFT,14,2));
        totRow.setBackground(BG2);
        totRow.add(lbl("Total s\u00f3lidos:")); JLabel lTS=vl("—",GREEN); totRow.add(lTS);
        totRow.add(Box.createHorizontalStrut(12));
        totRow.add(lbl("Total verde:"));  JLabel lTV=vl("—",AMBER); totRow.add(lTV);
        totRow.add(Box.createHorizontalStrut(12));
        totRow.add(lbl("W/S:"));          JLabel lWS=vl("—",ACCENT); totRow.add(lWS);
        cantPanel.add(totRow,gc2); gc2.gridwidth=1;
        datosPanel.add(cantPanel, gc);

        gc.gridy=4; gc.weighty=0.5;
        JPanel rellenoInf=new JPanel(); rellenoInf.setBackground(BG);
        datosPanel.add(rellenoInf, gc);

        DocumentListener dl=new DocumentListener(){
            public void changedUpdate(DocumentEvent e){recalcTotales(lTS,lTV,lWS);}
            public void insertUpdate (DocumentEvent e){recalcTotales(lTS,lTV,lWS);}
            public void removeUpdate (DocumentEvent e){recalcTotales(lTS,lTV,lWS);}
        };
        for(JTextField f:new JTextField[]{fRelave,fCaolin,fFeld,fAgua})
            f.getDocument().addDocumentListener(dl);

        DocumentListener dimL=new DocumentListener(){
            public void changedUpdate(DocumentEvent e){onDimCambia();}
            public void insertUpdate (DocumentEvent e){onDimCambia();}
            public void removeUpdate (DocumentEvent e){onDimCambia();}
        };
        for(JTextField f:new JTextField[]{fAncho,fLargo,fEspesor,fNervios})
            f.getDocument().addDocumentListener(dimL);

        JSplitPane split=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,animPanel,datosPanel);
        split.setDividerLocation(300); split.setDividerSize(4);
        split.setBackground(BG); split.setBorder(BorderFactory.createEmptyBorder());
        split.setResizeWeight(0.25);
        add(split,BorderLayout.CENTER);

        firmaSuelo=(prop0!=null)?calcularFirmaSuelo():null;
        Timer vigilante=new Timer(800,e->{ if(isShowing()) actualizarAutomatico(); });
        vigilante.start();
        addHierarchyListener(e->{
            if((e.getChangeFlags()&java.awt.event.HierarchyEvent.SHOWING_CHANGED)!=0 && isShowing())
                actualizarAutomatico();
        });
    }

    private void actualizarAutomatico(){
        String f=calcularFirmaSuelo();
        boolean cambio=(f!=null && !f.equals(firmaSuelo));
        if(cambio){
            aplicarPropuesta();
            firmaSuelo=f;
        }
        if(cambio || masas[0][0]==0) calcular(true);
    }

    private String calcularFirmaSuelo(){
        if(datos==null) return null;
        return String.valueOf(datos.clasificacionUSCS)+"|"+datos.LL+"|"
             +leerCampo(datos,"LP","lp","limitePlastico","PL")+"|"
             +leerCampo(datos,"IP","ip","indicePlasticidad","PI")+"|"
             +datos.pctArcilla+"|"
             +leerCampo(datos,"pctFinos","pctFino","finos","porcentajeFinos","pasa200","pctPasa200");
    }

    private Regla.Propuesta propuestaDesdeSuelo(){
        if(datos==null) return null;
        try{
            double ll=datos.LL;
            double lp=leerCampo(datos,"LP","lp","limitePlastico","PL");
            double ip=leerCampo(datos,"IP","ip","indicePlasticidad","PI");
            if(Double.isNaN(ip)&&!Double.isNaN(ll)&&!Double.isNaN(lp)) ip=ll-lp;
            double arc=datos.pctArcilla;
            double finos=leerCampo(datos,"pctFinos","pctFino","finos","porcentajeFinos","pasa200","pctPasa200");
            return Regla.proponer(datos.clasificacionUSCS,ip,lp,arc,finos);
        }catch(Exception ex){
            ex.printStackTrace();
            return null;
        }
    }

    private void aplicarPropuesta(){
        Regla.Propuesta p=propuestaDesdeSuelo();
        if(p==null){
            lblRegla.setText("Sin datos de suelo: dosis por defecto");
            return;
        }
        for(int m=0;m<3;m++) setDosis(m,p.dosis[m][0],p.dosis[m][1],p.dosis[m][2],p.dosis[m][3]);
        lblRegla.setText("Propuesta autom\u00e1tica: "+p.resumen);
    }

    private static double leerCampo(Object o,String... nombres){
        for(String n:nombres){
            try{
                Object v=o.getClass().getField(n).get(o);
                if(v instanceof Number) return ((Number)v).doubleValue();
            }catch(Exception ignored){}
        }
        return Double.NaN;
    }

    private void refrescarSuma(){
        for(int m=0;m<3;m++){
            double s=po(fDosRel[m])+po(fDosCao[m])+po(fDosFeld[m]);
            boolean ok=Math.abs(s-100.0)<0.05;
            lDosSum[m].setText(fmt1(s)+" %"+(ok?"":" \u2192 se normaliza"));
            lDosSum[m].setForeground(ok?GREEN:AMBER);
        }
    }

    private double[] leerDosis(int m){
        double r=Math.max(0,pd(fDosRel [m]));
        double c=Math.max(0,pd(fDosCao [m]));
        double f=Math.max(0,pd(fDosFeld[m]));
        double a=Math.max(0,pd(fDosAgua[m]));
        double tot=r+c+f;
        if(tot<=0) throw new IllegalArgumentException(NOMS[m]+": los s\u00f3lidos suman 0 %.");
        double k=100.0/tot;
        return new double[]{r*k,c*k,f*k,a};
    }

    private double calcularVolumen(){
        try{
            double a=pd(fAncho),l=pd(fLargo),e=pd(fEspesor);
            int nn=(int)pd(fNervios);
            double vol=l*a*e + nn*(a/30.0)*l*(e*0.35);
            lblVolCalc.setText(String.format("%.1f cm\u00b3",vol));
            return vol;
        }catch(Exception ex){lblVolCalc.setText("Error"); return 200;}
    }

    private BaldosinCatalan construirBaldosin(){
        double a, l, e; int n;
        try{ a = pd(fAncho);   }catch(Exception ex){ a = 20.0; }
        try{ l = pd(fLargo);   }catch(Exception ex){ l = 20.0; }
        try{ e = pd(fEspesor); }catch(Exception ex){ e = 1.2;  }
        try{ n = (int) pd(fNervios); }catch(Exception ex){ n = 4; }
        if(a <= 0) a = 20.0;
        if(l <= 0) l = 20.0;
        if(e <= 0) e = 1.2;
        if(n <  0) n = 0;
        BaldosinCatalan b = new BaldosinCatalan();
        b.ancho         = a;
        b.largo         = l;
        b.numNerviosInt = n;
        b.espesorNominal = e;          // e = espesor total escrito en la pieza
        b.calcularGeometria();         // deriva base, alto de nervio, nervios y ranura
        b.volNeto       = calcularVolumen();
        return b;
    }

    private void onDimCambia(){
        calcularVolumen();
        if(masas[0][0] != 0) enviarAEnsayos();
    }

    private double presionSegunRelave(String uscs){
        if(uscs==null) return 22;
        if     (uscs.startsWith("CH")||uscs.startsWith("MH")) return 35;
        else if(uscs.startsWith("CI"))                         return 30;
        else if(uscs.startsWith("CL")||uscs.startsWith("ML")) return 25;
        else if(uscs.startsWith("SC")||uscs.startsWith("SM")) return 18;
        return 22;
    }

    private void calcular(boolean silencioso){
        try{
            double volBald=calcularVolumen();
            double dens=pd(fDensidad);
            double presion=presionSegunRelave(datos.clasificacionUSCS);
            if(dens<=0||volBald<=0) throw new IllegalArgumentException("Densidad y volumen deben ser > 0.");

            // Masa de solidos por pieza: m_sol = densidad * volumen
            double mSol=dens*volBald;
            lblFormula.setText(String.format("m\u209b\u00f3l = %.2f \u00d7 %.1f = %.1f g",dens,volBald,mSol));

            for(int m=0;m<3;m++){
                double[] d=leerDosis(m);
                masas[m][0]=mSol*(d[0]/100.0);
                masas[m][1]=mSol*(d[1]/100.0);
                masas[m][2]=mSol*(d[2]/100.0);
                humM[m]    =d[3];
                aguaM[m]   =mSol*(humM[m]/100.0);
                presionCalc[m]=presion;
            }
            refrescarSuma();
            mostrarMezclaActiva();
            enviarAEnsayos();

        }catch(Exception ex){
            if(silencioso) ex.printStackTrace();
            else JOptionPane.showMessageDialog(this,"Error: "+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
        }
    }

    private void mostrarMezclaActiva(){
        int m=mezclaActiva;
        if(masas[m][0]==0) return;
        double mSol=masas[m][0]+masas[m][1]+masas[m][2];
        double relPct=masas[m][0]/mSol*100;
        double caoPct=masas[m][1]/mSol*100;
        double fldPct=masas[m][2]/mSol*100;
        double LL=Double.isNaN(datos.LL)?38:datos.LL;

        fRelave.setText(fmt1(masas[m][0]));
        fCaolin.setText(masas[m][1]>0.05?fmt1(masas[m][1]):"0.0");
        fFeld  .setText(masas[m][2]>0.05?fmt1(masas[m][2]):"0.0");
        fAgua  .setText(fmt1(aguaM[m]));

        lFracRelave.setText(fmt1(relPct)+"% \u00d7 "+fmt1(mSol)+" g — Materia prima (relave)");
        lFracCaolin.setText(masas[m][1]>0.05
            ? fmt1(caoPct)+"% \u00d7 "+fmt1(mSol)+" g — Plasticidad / cohesi\u00f3n en verde"
            : "No activo en esta mezcla");
        lFracFeld  .setText(masas[m][2]>0.05
            ? fmt1(fldPct)+"% \u00d7 "+fmt1(mSol)+" g — Fundente (LL relave="+fmt1(LL)+")"
            : "No activo en esta mezcla");
        lRatioAgua.setText(String.format(
            "%.1f%% de %.1f g s\u00f3lidos  W/S=%.2f  — Semiseco",
            humM[m], mSol, aguaM[m]/mSol));

        animPanel.setMezcla(
            new double[]{masas[m][0],masas[m][1],masas[m][2],aguaM[m]},
            aguaM[m],
            new String[]{"Relave","Caol\u00edn","Feldespato","Agua"},
            new Color[]{RELAVE,CAOLIN,FELD,ACCENT});
    }

    private void recalcTotales(JLabel lTS,JLabel lTV,JLabel lWS){
        try{
            double r=po(fRelave),ca=po(fCaolin),fe=po(fFeld),ag=po(fAgua);
            double tot=r+ca+fe;
            if(tot>0){
                lFracRelave.setText(fmt1(r/tot*100)+"% — Relave");
                lFracCaolin.setText(ca>0?fmt1(ca/tot*100)+"% — Caol\u00edn":"No activo");
                lFracFeld  .setText(fe>0?fmt1(fe/tot*100)+"% — Feldespato":"No activo");
                lRatioAgua .setText(String.format("H=%.1f%%  W/S=%.2f",ag/tot*100,ag/tot));
                lTS.setText(fmt1(tot)+" g");
                lTV.setText(fmt1(tot+ag)+" g");
                lWS.setText(fmt1(ag/tot));
            }
        }catch(Exception ignored){}
    }

    private void enviarAEnsayos(){
        if((ensayosNTC==null&&secadoCoccion==null)||masas[0][0]==0) return;
        try{
            MezclaBaldosin[] ms=new MezclaBaldosin[3];
            double dens; try{dens=pd(fDensidad);}catch(Exception e){dens=1.85;}
            double volBald; try{volBald=calcularVolumen();}catch(Exception e){volBald=200;}

            for(int m=0;m<3;m++){
                if(masas[m][0]==0) continue;
                MezclaBaldosin mz=new MezclaBaldosin();
                double mSol=masas[m][0]+masas[m][1]+masas[m][2];

                mz.fracSuelo     =masas[m][0]/mSol;
                mz.fracCal       =masas[m][1]/mSol;
                mz.fracFeldespato=masas[m][2]/mSol;
                mz.fracSilice    =0;
                // Relacion agua/solidos: w = agua / solidos
                mz.relacionAguaSolidos=aguaM[m]/mSol;
                mz.masaSuelo      =masas[m][0];
                mz.masaCal        =masas[m][1];
                mz.masaFeldespato =masas[m][2];
                mz.masaAgua       =aguaM[m];
                mz.masaTotalSolidos=mSol;
                mz.masaTotalVerde  =mSol+aguaM[m];
                mz.densidadAparente=dens;
                mz.densidadVerde   =(mSol+aguaM[m])/(volBald>0?volBald:200);

                double arc=Double.isNaN(datos.pctArcilla)?20:datos.pctArcilla;
                // Contraccion estimada: sube con arcilla, caolin y humedad; baja con feldespato
                double contr=clamp(6.0+arc*0.15+mz.fracCal*8+(humM[m]-12)*0.25
                                   -mz.fracFeldespato*40, 4.0, 14.0);
                mz.contraccionLineal=contr;
                double fc=Math.pow(1-contr/100.0,3);
                mz.densidadCocida  =clamp(dens/fc*0.92,1.55,2.20);
                mz.absorcionAgua   =clamp(28-dens*12-mz.fracFeldespato*30,4.0,25.0);
                mz.resistenciaFlexion=clamp(4.5+dens*3.8+mz.fracFeldespato*28,4,22);
                mz.cargaRotura     =clamp(mz.resistenciaFlexion*200,200,2500);
                mz.grupoISO        =mz.absorcionAgua>10?"AIII":"AIIb";
                ms[m]=mz;
            }

            if(ensayosNTC!=null){
                BaldosinCatalan bald = construirBaldosin();

                ensayosNTC.setMezclas3(ms, NOMS.clone(), bald, datos);
            }
            if(secadoCoccion!=null){
                double espesorMm;
                try{ espesorMm = pd(fEspesor)*10.0; }catch(Exception e){ espesorMm = 12; }
                secadoCoccion.setMezclas3(ms, NOMS.clone(), espesorMm, volBald, datos);
            }
        }catch(Exception ex){ ex.printStackTrace(); }
    }
    public void setEnsayosPanel(EnsayosNTC e){this.ensayosNTC=e;}
    public void setSecadoCoccionPanel(SecadoCoccion s){this.secadoCoccion=s;}

    public void setDimensiones(double a,double l,double e,int n){
        if(a>0) fAncho  .setText(fmt1(a));
        if(l>0) fLargo  .setText(fmt1(l));
        if(e>0) fEspesor.setText(fmt1(e));
        if(n>=0) fNervios.setText(String.valueOf(n));
        calcularVolumen();
        // Las dimensiones vienen de la pieza 3D: recalcular la dosificacion y los ensayos
        if(masas[0][0]!=0) calcular(true);
    }

    public void setDosis(int m,double relavePct,double caolinPct,double feldPct,double aguaPct){
        if(m<0||m>2) return;
        fDosRel [m].setText(fmt1(relavePct));
        fDosCao [m].setText(fmt1(caolinPct));
        fDosFeld[m].setText(fmt1(feldPct));
        fDosAgua[m].setText(fmt1(aguaPct));
        refrescarSuma();
    }

    private static final class Regla {

        public static final double   C_CONSTANTE   = 35.0;
        public static final double   C_MIN         = 15.0;
        public static final double   C_MAX         = 40.0;
        public static final double   PASO_CAOLIN   = 5.0;
        public static final double[] FELDESPATO    = {5, 10, 15};
        public static final double[] FACTOR_LP     = {0.45, 0.585, 0.72};
        public static final double   ACTIVIDAD     = 0.75;
        public static final double   H_BASE        = 8.0;
        public static final double   H_PEND_FINOS  = 5.0;
        public static final double   H_PASO        = 3.0;
        public static final double[] HUMEDAD_DEF   = {10, 13, 16};
        public static final double   AJUSTE_HUMEDAD= 5.0;

        private static final Pattern SIMBOLO =
            Pattern.compile("\\b(GW|GP|GM|GC|SW|SP|SM|SC|ML|CL|CI|OL|MH|CH|OH|PT)\\b");

        private Regla(){}

        public static final class Propuesta {

            public final double[][] dosis;

            public final double caolinBase;

            public final double ajusteUSCS;

            public final String simboloUSCS;

            public final boolean conLimites;

            public final String resumen;

            Propuesta(double[][] dosis,double caolinBase,double ajusteUSCS,String simbolo,
                      boolean conLimites,String resumen){
                this.dosis=dosis; this.caolinBase=caolinBase; this.ajusteUSCS=ajusteUSCS;
                this.simboloUSCS=simbolo; this.conLimites=conLimites; this.resumen=resumen;
            }
        }

        public static Propuesta proponer(String uscs,double ip,double lp){
            return proponer(uscs,ip,lp,Double.NaN,Double.NaN);
        }

        public static Propuesta proponer(String uscs,double ip,double lp,double pctArcilla,double pctFinos){
            String simbolo=simboloUSCS(uscs);
            double delta=ajustePorTipo(simbolo);

            boolean ipValido=valido(ip);
            boolean arcValida=valido(pctArcilla);
            double ipUsado;
            String fuenteIP;
            if(ipValido){
                ipUsado=ip;
                fuenteIP=String.format(Locale.US,"IP %.1f",ip);
            }else if(arcValida){
                ipUsado=ACTIVIDAD*pctArcilla;
                fuenteIP=String.format(Locale.US,"sin límites: IPeq = %.2f·%.1f %% arcilla = %.1f",
                                       ACTIVIDAD,pctArcilla,ipUsado);
            }else{
                ipUsado=0;
                fuenteIP="sin límites ni % arcilla: IP = 0";
            }
            double c0=clamp(C_CONSTANTE-ipUsado+delta,C_MIN,C_MAX);

            boolean lpValido=valido(lp);
            boolean finosValidos=valido(pctFinos);
            String fuenteH=lpValido?String.format(Locale.US,"H = f·LP (LP %.1f)",lp)
                          :finosValidos?String.format(Locale.US,"H por finos (%.1f %%)",pctFinos)
                          :"H por defecto";

            double[][] d=new double[3][4];
            for(int m=0;m<3;m++){
                double c=Math.round(c0+(m-1)*PASO_CAOLIN);
                double f=FELDESPATO[m];
                double r=100-c-f;
                double h;
                if(lpValido){
                    h=Math.round(FACTOR_LP[m]*lp);
                }else if(finosValidos){
                    double fMix=(r*pctFinos+(c+f)*100.0)/100.0;
                    h=Math.round(H_BASE+H_PEND_FINOS*fMix/100.0+H_PASO*(m-1));
                }else{
                    h=HUMEDAD_DEF[m];
                }
                h+=AJUSTE_HUMEDAD;
                d[m]=new double[]{r,c,f,h};
            }

            String resumen=String.format(Locale.US,
                "USCS %s (Δ=%+.0f) · %s → caolín base %.1f %% · %s + 5",
                simbolo,delta,fuenteIP,c0,fuenteH);
            return new Propuesta(d,c0,delta,simbolo,ipValido,resumen);
        }

        public static String simboloUSCS(String uscs){
            if(uscs==null) return "?";
            Matcher mt=SIMBOLO.matcher(uscs.toUpperCase(Locale.ROOT));
            return mt.find()?mt.group(1):"?";
        }

        public static double ajustePorTipo(String simbolo){
            switch(simbolo){
                case "CL": case "CI": case "CH":                   return 0;
                case "ML": case "MH": case "OL": case "OH":         return 3;
                case "SC": case "GC":                              return 5;
                case "SM": case "GM":                              return 8;
                case "SW": case "SP": case "GW": case "GP":        return 10;
                default:                                           return 0;
            }
        }

        private static boolean valido(double v){return !Double.isNaN(v)&&v>0;}
        private static double clamp(double v,double mn,double mx){return Math.max(mn,Math.min(mx,v));}
    }

    static class AnimPanel extends JPanel {
        private static final Color CA  = Tema.INFO;
        private static final Color CA2 = new Color(120,190,255);
        private static final Color BGA = Tema.FONDO;
        private static final Color BG2A= Tema.SUPERFICIE;

        private double[] mAnim=new double[4], mTgt=new double[4];
        private double   aguaAnim=0, aguaTgt=0, wave=0;
        private double   masaAguaTot=0;
        private Color[]  cols=new Color[]{RELAVE,CAOLIN,FELD,CA};
        private String[] noms={"Relave","Caol\u00edn","Feldespato","Agua"};
        private Timer    timer, waveTimer;

        AnimPanel(){
            setBackground(BGA);
            CopyImageMenu.instalar(this, "Básculas de Mezcla");
            waveTimer=new Timer(30,e->{wave+=0.10; repaint();}); waveTimer.start();
        }

        void setMezcla(double[] masas4,double aguaTot,String[] nombres,Color[] colores){
            noms=nombres; cols=colores; masaAguaTot=aguaTot;
            for(int i=0;i<4;i++) mTgt[i]=masas4[i];
            aguaTgt=1.0; mAnim=new double[4]; aguaAnim=0;
            if(timer!=null) timer.stop();
            timer=new Timer(20,e->{
                boolean done=true;
                for(int i=0;i<4;i++){
                    mAnim[i]+=(mTgt[i]-mAnim[i])*0.06;
                    if(Math.abs(mAnim[i]-mTgt[i])>0.8) done=false;
                }
                aguaAnim+=(aguaTgt-aguaAnim)*0.04;
                if(Math.abs(aguaAnim-aguaTgt)>0.005) done=false;
                repaint(); if(done) timer.stop();
            }); timer.start();
        }

        @Override protected void paintComponent(Graphics g0){
            super.paintComponent(g0);
            Graphics2D g=(Graphics2D)g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int W=getWidth(),H=getHeight();
            g.setColor(BG2A); g.fillRoundRect(2,2,W-4,H-4,10,10);
            if(mTgt[0]==0){
                g.setColor(Tema.TEXTO_3);
                g.setFont(Tema.fuente(Font.PLAIN, 11));
                g.drawString("Calcule las mezclas",12,H/2-6);
                g.drawString("para ver animaci\u00f3n",12,H/2+10);
                return;
            }
            g.setFont(Tema.fuente(Font.BOLD, 10));
            g.setColor(Tema.TEXTO_2);
            g.drawString("Materiales / baldosa — Relave + Caol\u00edn + Feld.",8,16);
            int bW=(W-12)/2, bH=(int)((H-50)*0.48);
            int[][] pos={{6,22},{6+bW+2,22},{6,22+bH+4},{6+bW+2,22+bH+4}};
            for(int i=0;i<4;i++)
                drawBascula(g,pos[i][0],pos[i][1],bW,bH,noms[i],cols[i],mAnim[i],mTgt[i]);
            int probY=22+bH*2+10; int probH=H-probY-10;
            if(probH>40) drawProbeta(g,6,probY,W-12,probH);
        }

        private void drawBascula(Graphics2D g,int x,int y,int w,int h,
                                  String nom,Color col,double anim,double tgt){
            g.setColor(BGA); g.fillRoundRect(x,y,w,h,6,6);
            g.setColor(Tema.BORDE); g.setStroke(new BasicStroke(1f));
            g.drawRoundRect(x,y,w,h,6,6);
            int cx=x+w/2;
            g.setColor(new Color(45,58,88));
            g.setStroke(new BasicStroke(2f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
            g.drawLine(cx-12,y+h-4,cx-16,y+h); g.drawLine(cx+12,y+h-4,cx+16,y+h);
            g.setColor(new Color(50,65,95)); g.setStroke(new BasicStroke(1.5f));
            g.fillRoundRect(cx-w/3,y+h-8,w*2/3,6,3,3);
            double frac=tgt>0?anim/tgt:0;
            int platY=y+h-8-(int)(frac*12)-22;
            int platW=(int)(w*0.70);
            g.setColor(new Color(70,90,130)); g.fillRoundRect(cx-platW/2,platY,platW,7,4,4);
            g.setColor(new Color(95,120,170)); g.setStroke(new BasicStroke(1f));
            g.drawRoundRect(cx-platW/2,platY,platW,7,4,4);
            g.setColor(new Color(50,65,95)); g.setStroke(new BasicStroke(2f));
            g.drawLine(cx,y+h-8,cx,platY+7);
            if(anim>0.5){
                int mH=Math.max(3,(int)(frac*18)); int mW=(int)(platW*0.80);
                if(col.equals(CA)||col.equals(CA2)){
                    g.setColor(new Color(50,100,200,180));
                    g.fillRoundRect(cx-mW/2,platY-mH,mW,mH,3,3);
                    g.setColor(new Color(120,180,255,140)); g.setStroke(new BasicStroke(1.2f));
                    GeneralPath wv=new GeneralPath();
                    for(int i=0;i<=8;i++){
                        double wx=cx-mW/2+mW*i/8.0,wy=platY-mH+Math.sin(wave+i)*1.2;
                        if(i==0) wv.moveTo(wx,wy); else wv.lineTo(wx,wy);
                    }
                    g.draw(wv);
                } else {
                    GeneralPath pile=new GeneralPath();
                    pile.moveTo(cx-mW/2,platY);
                    pile.curveTo(cx-mW/2,platY-mH*0.3,cx-mW*0.3,platY-mH,cx,platY-mH);
                    pile.curveTo(cx+mW*0.3,platY-mH,cx+mW/2,platY-mH*0.3,cx+mW/2,platY);
                    pile.closePath();
                    g.setPaint(new GradientPaint(cx-mW/2,platY-mH,col.brighter(),cx,platY,col.darker()));
                    g.fill(pile);
                    g.setColor(col.darker()); g.setStroke(new BasicStroke(0.8f)); g.draw(pile);
                }
            }
            g.setColor(new Color(8,20,12)); g.fillRoundRect(x+3,y+h-15,w-6,12,3,3);
            g.setFont(Tema.fuente(Font.BOLD, 9));
            String dsp=anim>0.5?String.format("%.0f g",anim):"--- g";
            FontMetrics fm=g.getFontMetrics();
            g.setColor(new Color(40,220,80));
            g.drawString(dsp,cx-fm.stringWidth(dsp)/2,y+h-5);
            g.setFont(Tema.fuente(Font.BOLD, 9)); g.setColor(col);
            g.drawString(nom,x+3,y+11);
            if(tgt>0){
                g.setFont(Tema.fuente(Font.PLAIN, 8)); g.setColor(Tema.TEXTO_3);
                String ob=String.format("%.0fg",tgt);
                g.drawString(ob,x+w-g.getFontMetrics().stringWidth(ob)-3,y+11);
            }
        }

        private void drawProbeta(Graphics2D g,int x,int y,int w,int h){
            int pw=34,px=x+w/2-pw/2,ph=h-20;
            g.setFont(Tema.fuente(Font.PLAIN, 8));
            double tot=masaAguaTot>0?masaAguaTot:aguaTgt*100;
            for(int i=0;i<=4;i++){
                int gy=(int)(y+ph*(1-i/4.0));
                g.setColor(Tema.BORDE); g.setStroke(new BasicStroke(0.7f));
                g.drawLine(px-10,gy,px-2,gy);
                g.setColor(Tema.TEXTO_3);
                String lb=String.format("%.0f",tot*i/4.0);
                g.drawString(lb,px-10-g.getFontMetrics().stringWidth(lb)-2,gy+4);
            }
            g.setColor(CA); g.setFont(Tema.fuente(Font.PLAIN, 8));
            g.drawString("ml",px+pw+3,y+ph/2+4);
            g.setColor(new Color(180,210,240,25)); g.fillRect(px,y,pw,ph);
            g.setColor(new Color(140,180,220,150)); g.setStroke(new BasicStroke(1.5f));
            g.drawLine(px,y,px,y+ph); g.drawLine(px+pw,y,px+pw,y+ph);
            g.drawLine(px,y+ph,px+pw,y+ph); g.drawLine(px-4,y,px+pw+4,y);
            if(aguaAnim>0.005){
                int nY=(int)(y+ph*(1-aguaAnim*0.88));
                GradientPaint gp=new GradientPaint(px,nY,new Color(70,130,210,200),px,y+ph,new Color(40,100,180,230));
                g.setPaint(gp); g.fillRect(px+1,nY,pw-2,y+ph-nY);
                GeneralPath wv=new GeneralPath();
                for(int i=0;i<=14;i++){
                    double wx=px+1+(pw-2)*i/14.0,wy=nY+Math.sin(wave+i*1.2)*1.8;
                    if(i==0) wv.moveTo(wx,wy); else wv.lineTo(wx,wy);
                }
                g.setColor(new Color(150,200,255,130)); g.setStroke(new BasicStroke(1.5f)); g.draw(wv);
                g.setColor(new Color(180,220,255,90)); g.drawArc(px+1,nY-3,pw-2,7,0,180);
                double mlNow=tot*aguaAnim;
                String ms=String.format("%.0f ml",mlNow);
                g.setFont(Tema.fuente(Font.BOLD, 12));
                FontMetrics fm=g.getFontMetrics();
                int txc=px+pw/2-fm.stringWidth(ms)/2,tyc=(int)(nY+(y+ph-nY)/2+5);
                g.setColor(new Color(0,0,0,100)); g.drawString(ms,txc+1,tyc+1);
                g.setColor(Color.WHITE); g.drawString(ms,txc,tyc);
                String tt=String.format("/ %.0f ml",tot);
                g.setFont(Tema.fuente(Font.PLAIN, 8)); fm=g.getFontMetrics();
                g.setColor(CA2); g.drawString(tt,px+pw/2-fm.stringWidth(tt)/2,y+ph+12);
            }
            g.setFont(Tema.fuente(Font.BOLD, 10));
            g.setColor(CA); FontMetrics fm=g.getFontMetrics();
            g.drawString("AGUA",px+pw/2-fm.stringWidth("AGUA")/2,y-4);
        }
    }

    private void addFilaCant(JPanel p,GridBagConstraints gc,int row,String nombre,
                              Color col,JTextField campo,String unid,JLabel lblFrac){
        gc.gridy=row; gc.weightx=0; Color bgR=row%2==0?BG2:BG3;
        gc.gridx=0; JLabel ln=new JLabel(nombre);
        ln.setFont(Tema.ui(Font.BOLD, 11)); ln.setForeground(col);
        ln.setBorder(BorderFactory.createEmptyBorder(0,4,0,4));
        ln.setOpaque(true); ln.setBackground(bgR); p.add(ln,gc);
        gc.gridx=1; campo.setBackground(Tema.SUPERFICIE_2);
        campo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60,85,130)),
            BorderFactory.createEmptyBorder(3,6,3,6))); p.add(campo,gc);
        gc.gridx=2; JLabel lu=new JLabel(unid);
        lu.setFont(Tema.ui(Font.PLAIN, 11)); lu.setForeground(DIM);
        lu.setOpaque(true); lu.setBackground(bgR);
        lu.setBorder(BorderFactory.createEmptyBorder(0,4,0,4)); p.add(lu,gc);
        gc.gridx=3; gc.weightx=1; lblFrac.setOpaque(true); lblFrac.setBackground(bgR);
        lblFrac.setBorder(BorderFactory.createEmptyBorder(0,6,0,4)); p.add(lblFrac,gc);
    }

    private static int filaParam(JPanel p,GridBagConstraints gp,int fila,String etiqueta,JComponent comp){
        gp.gridy=fila;
        gp.gridx=0; gp.weightx=0; p.add(lbl(etiqueta),gp);
        gp.gridx=1; gp.weightx=1; p.add(comp,gp);
        return fila+1;
    }
    private JPanel seccion(String titulo){
        JPanel wrap=new JPanel(new BorderLayout(0,4)); wrap.setBackground(BG2);
        wrap.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0,3,0,0,AMBER),
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORD),
                BorderFactory.createEmptyBorder(6,10,8,10))));
        JLabel t=new JLabel(titulo);
        t.setFont(Tema.ui(Font.BOLD, 9)); t.setForeground(DIM);
        t.setBorder(BorderFactory.createEmptyBorder(0,0,5,0));
        JPanel inner=new JPanel(); inner.setBackground(BG2);
        wrap.add(t,BorderLayout.NORTH); wrap.add(inner,BorderLayout.CENTER); return inner;
    }
    private static JLabel lbl(String t){JLabel l=new JLabel(t);l.setFont(Tema.ui(Font.PLAIN, 11));l.setForeground(MUTED);return l;}
    private static JLabel vl(String t,Color c){JLabel l=new JLabel(t);l.setFont(Tema.ui(Font.BOLD, 12));l.setForeground(c);return l;}
    private static JTextField fld(String v,int w){JTextField f=new JTextField(v);f.setPreferredSize(new Dimension(w,26));f.setBackground(BG3);f.setForeground(TEXT);f.setFont(Tema.ui(Font.PLAIN, 12));f.setCaretColor(ACCENT);f.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORD),BorderFactory.createEmptyBorder(3,5,3,5)));return f;}
    private static JTextField fldEdit(String v){JTextField f=new JTextField(v,8);f.setBackground(Tema.SUPERFICIE_2);f.setForeground(TEXT);f.setFont(Tema.ui(Font.PLAIN, 12));f.setCaretColor(ACCENT);f.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(55,80,120)),BorderFactory.createEmptyBorder(3,5,3,5)));return f;}
    private JToggleButton tab(String t,Color c){JToggleButton b=new JToggleButton(t);b.setFont(Tema.ui(Font.BOLD, 10));b.setBackground(BG3);b.setForeground(c);b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(c),BorderFactory.createEmptyBorder(4,10,4,10)));b.setFocusPainted(false);return b;}
    private static double clamp(double v,double mn,double mx){return Math.max(mn,Math.min(mx,v));}
    private static double pd(JTextField f){return Double.parseDouble(f.getText().trim().replace(",","."));}
    private static double po(JTextField f){try{return Double.parseDouble(f.getText().trim().replace(",","."));}catch(Exception e){return 0;}}
    private static String fmt1(double v){return String.format("%.1f",v);}
    private static Color blend(Color a,Color b,float t){return new Color(Math.min(255,(int)(a.getRed()*(1-t)+b.getRed()*t)),Math.min(255,(int)(a.getGreen()*(1-t)+b.getGreen()*t)),Math.min(255,(int)(a.getBlue()*(1-t)+b.getBlue()*t)));}
}