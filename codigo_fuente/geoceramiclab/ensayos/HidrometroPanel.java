package geoceramiclab.ensayos;

import geoceramiclab.Tema;
import geoceramiclab.model.SueloData;
import geoceramiclab.graficas.TrianguloTextural;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.List;
import java.util.ArrayList;
import javax.swing.filechooser.FileNameExtensionFilter;

public class HidrometroPanel extends JPanel {

    private static final Color BG    = Tema.FONDO;
    private static final Color BG2   = Tema.SUPERFICIE;
    private static final Color BG3   = Tema.SUPERFICIE_2;
    private static final Color BG4   = Tema.BARRA;
    private static final Color BORD  = Tema.BORDE;
    private static final Color ACCENT= Tema.INFO;
    private static final Color GREEN = Tema.OK;
    private static final Color AMBER = Tema.AVISO;
    private static final Color RED   = Tema.ERROR;
    private static final Color TEXT  = Tema.TEXTO;
    private static final Color MUTED = Tema.TEXTO_2;
    private static final Color DIM   = Tema.TEXTO_3;
    private static final Color EDIT_BG = Tema.SUPERFICIE_2;

    private static final Color C_ARENA   = new Color(120,190,90);
    private static final Color C_LIMO    = new Color(90,160,240);
    private static final Color C_ARCILLA = new Color(235,100,100);

    private static final Object[][] TAMICES = {
        {"6\"",   "152 mm",  152.0},
        {"4\"",   "100 mm",  100.0},
        {"3\"",   "75 mm",    75.0},
        {"2\"",   "50 mm",    50.0},
        {"1½\"",  "37 mm",    37.0},
        {"1\"",   "25 mm",    25.0},
        {"¾\"",   "19 mm",    19.0},
        {"½\"",   "12.5 mm",  12.5},
        {"⅜\"",   "9.5 mm",    9.5},
        {"N° 4",  "4.75 mm",   4.75},
        {"N° 8",  "2.36 mm",   2.36},
        {"N° 16", "1.18 mm",   1.18},
        {"N° 30", "600 µm",    0.600},
        {"N° 60", "250 µm",    0.250},
        {"N° 100","150 µm",    0.150},
        {"N° 200","75 µm",     0.075},
    };

    private static final double[][] TEMP_CORR = {
        {20,0.0},{21,0.2},{22,0.4},{23,0.7},{24,1.0},
        {25,1.2},{26,1.65},{27,2.0},{28,2.5},{29,3.05},{30,3.8}
    };
    private static double factorTemp(double T){
        for(int i=0;i<TEMP_CORR.length-1;i++){
            if(T>=TEMP_CORR[i][0]&&T<=TEMP_CORR[i+1][0]){
                double t0=TEMP_CORR[i][0],t1=TEMP_CORR[i+1][0];
                double f0=TEMP_CORR[i][1],f1=TEMP_CORR[i+1][1];
                return f0+(f1-f0)*(T-t0)/(t1-t0);
            }
        }
        return T<20?TEMP_CORR[0][1]:TEMP_CORR[TEMP_CORR.length-1][1];
    }

    private final SueloData datos;
    private final DefaultTableModel tablaModel;
    private final JTable tabla;
    private final JTextField campoMasa, campoGs, campoBoy0, campoTemp0;
    private final JLabel lblResultado, lblClase;
    private final JLabel valArena, valLimo, valArcilla;
    private final JComboBox<String> cmbSistema;
    private final JComboBox<String> cmbTamiz;
    private final JTextField campoTamizMm;
    private final TrianguloTextural triangulo;

    public HidrometroPanel(SueloData datos) {
        this.datos = datos;
        setLayout(new BorderLayout());
        setBackground(BG);

        JPanel izq = new JPanel(new BorderLayout(0,8));
        izq.setBackground(BG);
        izq.setBorder(BorderFactory.createEmptyBorder(10,12,10,6));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(BG2);
        form.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORD),
            BorderFactory.createEmptyBorder(8,10,8,10)));

        campoMasa  = field("50");
        campoGs    = field("2.65");
        campoBoy0  = field("1.040");
        campoTemp0 = field("25");
        cmbSistema = new JComboBox<>(new String[]{"Sistema Inglés","Sistema Internacional"});
        cmbTamiz   = new JComboBox<>();
        estilizarCombo(cmbSistema);
        estilizarCombo(cmbTamiz);
        campoTamizMm = field("");
        campoTamizMm.setForeground(AMBER);

        int r = 0;
        addPar(form, r,   0, "Masa seca (g)", campoMasa);
        addPar(form, r++, 2, "Gs",            campoGs);
        addPar(form, r,   0, "Boyucos t=0",   campoBoy0);
        addPar(form, r++, 2, "Temp₀ (°C)",    campoTemp0);

        GridBagConstraints gs = gbc(0, r++, 4, 0);
        gs.insets = new Insets(6,0,6,0);
        JSeparator sep = new JSeparator();
        sep.setForeground(BORD); sep.setBackground(BG2);
        form.add(sep, gs);

        JLabel lTam = label("Tamiz de referencia");
        lTam.setForeground(AMBER);
        lTam.setFont(Tema.ui(Font.BOLD, 11));
        form.add(lTam, gbc(0, r++, 4, 0));
        addPar(form, r++, 0, "Sistema", cmbSistema, 3);
        addPar(form, r,   0, "Tamiz",   cmbTamiz);
        addPar(form, r++, 2, "mm",      campoTamizMm);

        izq.add(form, BorderLayout.NORTH);

        cmbSistema.addActionListener(e -> llenarComboTamiz());
        cmbTamiz.addActionListener(e -> actualizarMm());
        llenarComboTamiz();

        String[] cols = {"t (min)","Lectura","T (°C)","% Finos","D (mm)"};
        tablaModel = new DefaultTableModel(cols,0){
            @Override public boolean isCellEditable(int row,int c){ return c<3; }
        };
        tabla = new JTable(tablaModel);
        estilizarTabla(tabla);
        String[] tips = {"Tiempo transcurrido (min)","Lectura del hidrómetro (boyucos)",
                         "Temperatura de la suspensión (°C)","% de finos en suspensión","Diámetro equivalente (mm)"};
        tabla.getTableHeader().setDefaultRenderer(new HeaderRenderer(tips));
        for(int c=0;c<3;c++) tabla.getColumnModel().getColumn(c).setCellRenderer(celda(ACCENT, EDIT_BG));
        tabla.getColumnModel().getColumn(3).setCellRenderer(celda(GREEN, BG2));
        tabla.getColumnModel().getColumn(4).setCellRenderer(celda(MUTED, BG2));
        tabla.getColumnModel().getColumn(4).setMinWidth(72);
        JTextField ed = field("");
        ed.setBackground(EDIT_BG); ed.setForeground(ACCENT); ed.setHorizontalAlignment(JTextField.RIGHT);
        ed.setBorder(BorderFactory.createLineBorder(ACCENT));
        DefaultCellEditor editor = new DefaultCellEditor(ed);
        editor.setClickCountToStart(1);
        for(int c=0;c<3;c++) tabla.getColumnModel().getColumn(c).setCellEditor(editor);

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.getViewport().setBackground(BG2);
        scroll.setBorder(BorderFactory.createLineBorder(BORD));
        izq.add(scroll, BorderLayout.CENTER);

        JPanel botones = new JPanel(new WrapLayout(FlowLayout.LEFT,6,4));
        botones.setBackground(BG);
        botones.add(btnAccion("+ Lectura",   MUTED, e -> { agregarFila("","",""); enfocarUltima(); }));
        botones.add(btnAccion("− Quitar fila", MUTED, e -> quitarSeleccionadas()));
        botones.add(btnAccion("Ejemplo",     MUTED,  e -> cargarEjemplo()));
        botones.add(btnAccion("CSV",      MUTED,  e -> cargarCSV()));
        botones.add(btnAccion("Limpiar",     RED,    e -> limpiar()));

        JButton btnCalc = Tema.boton("Calcular hidrómetro", Tema.Tipo.PRIMARIO);
        btnCalc.addActionListener(e -> calcular());

        JPanel acciones = new JPanel(new BorderLayout(0,6));
        acciones.setBackground(BG);
        acciones.add(botones, BorderLayout.CENTER);
        acciones.add(btnCalc, BorderLayout.SOUTH);
        izq.add(acciones, BorderLayout.SOUTH);

        JPanel der = new JPanel(new BorderLayout(0,8));
        der.setBackground(BG);
        der.setBorder(BorderFactory.createEmptyBorder(10,6,10,12));

        triangulo = new TrianguloTextural();
        triangulo.setMinimumSize(new Dimension(280,280));
        JPanel marcoTri = new JPanel(new BorderLayout());
        marcoTri.setBackground(BG2);
        marcoTri.setBorder(BorderFactory.createLineBorder(BORD));
        marcoTri.add(triangulo, BorderLayout.CENTER);
        der.add(marcoTri, BorderLayout.CENTER);

        valArena   = new JLabel("—", SwingConstants.CENTER);
        valLimo    = new JLabel("—", SwingConstants.CENTER);
        valArcilla = new JLabel("—", SwingConstants.CENTER);
        JPanel tarjetas = new JPanel(new GridLayout(1,3,8,0));
        tarjetas.setBackground(BG);
        tarjetas.add(tarjeta("ARENA",   valArena,   C_ARENA));
        tarjetas.add(tarjeta("LIMO",    valLimo,    C_LIMO));
        tarjetas.add(tarjeta("ARCILLA", valArcilla, C_ARCILLA));

        lblClase = new JLabel(" ", SwingConstants.CENTER);
        lblClase.setFont(Tema.ui(Font.BOLD, 14));
        lblClase.setForeground(AMBER);

        lblResultado = new JLabel("Ingrese lecturas de boyucos y temperatura", SwingConstants.CENTER);
        lblResultado.setFont(Tema.ui(Font.PLAIN, 11));
        lblResultado.setForeground(DIM);

        JPanel resultados = new JPanel(new BorderLayout(0,6));
        resultados.setBackground(BG);
        resultados.add(tarjetas, BorderLayout.NORTH);
        resultados.add(lblClase, BorderLayout.CENTER);
        resultados.add(lblResultado, BorderLayout.SOUTH);
        der.add(resultados, BorderLayout.SOUTH);

        izq.setMinimumSize(new Dimension(380,0));
        der.setMinimumSize(new Dimension(320,0));
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, izq, der);
        split.setResizeWeight(0.45);
        split.setDividerSize(6);
        split.setContinuousLayout(true);
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setBackground(BG);
        add(split, BorderLayout.CENTER);
        addComponentListener(new ComponentAdapter(){
            private boolean colocado=false;
            @Override public void componentResized(ComponentEvent e){
                if(!colocado && getWidth()>0){ split.setDividerLocation(0.45); colocado=true; }
            }
        });

        cargarEjemplo();
    }

    private void llenarComboTamiz() {
        int sel = Math.max(0, cmbTamiz.getSelectedIndex());
        cmbTamiz.removeAllItems();
        boolean ingles = cmbSistema.getSelectedIndex() == 0;
        for (Object[] t : TAMICES) cmbTamiz.addItem(ingles ? (String)t[0] : (String)t[1]);
        if (sel < cmbTamiz.getItemCount()) cmbTamiz.setSelectedIndex(sel);
        actualizarMm();
    }

    private void actualizarMm() {
        int idx = cmbTamiz.getSelectedIndex();
        if (idx >= 0 && idx < TAMICES.length) {
            double mm = (double) TAMICES[idx][2];
            campoTamizMm.setText(mm < 1 ? String.format("%.3f", mm) : String.format("%.2f", mm));
        }
    }

    private void calcular() {
        if (tabla.isEditing()) tabla.getCellEditor().stopCellEditing();
        double Ws, Gs;
        try {
            Ws = Double.parseDouble(campoMasa.getText().trim().replace(",","."));
            Gs = Double.parseDouble(campoGs.getText().trim().replace(",","."));
        } catch (Exception e) { showError("Verifique masa seca y Gs"); return; }

        int n = tablaModel.getRowCount();
        if (n < 2) { showError("Ingrese al menos 2 lecturas"); return; }

        List<double[]> puntos = new ArrayList<>();
        double lectCorr40s = Double.NaN;
        double lectCorr2h  = Double.NaN;
        double masaMuestra = Ws;
        double diff40s = Double.MAX_VALUE;
        double diff2h  = Double.MAX_VALUE;

        for (int i = 0; i < n; i++) {
            try {
                double t    = Double.parseDouble(tablaModel.getValueAt(i,0).toString().trim().replace(",","."));
                double boy  = Double.parseDouble(tablaModel.getValueAt(i,1).toString().trim().replace(",","."));
                double temp = Double.parseDouble(tablaModel.getValueAt(i,2).toString().trim().replace(",","."));

                double Fc  = factorTemp(temp);
                double boyVal = (boy >= 1.0 && boy < 2.0) ? (boy - 1.0)*1000.0 : boy;
                double Rc  = boyVal + Fc;
                double vis  = 1.002 - 0.0281*(temp-20) + 0.000236*(temp-20)*(temp-20);
                double eta  = vis * 0.001;
                double pct  = Math.min(100, Math.max(0, (Gs/(Gs-1.0))*(Rc/Ws)*100.0));
                double tSec = t * 60.0;
                double rhoS = Gs*1000, rhoW=1000, g=9.806;
                double D_m  = Math.sqrt((18.0*eta*0.10)/((rhoS-rhoW)*g*tSec));
                double D_mm = D_m * 1000.0;

                tablaModel.setValueAt(String.format("%.1f%%", pct), i, 3);
                tablaModel.setValueAt(String.format("%.5f", D_mm),  i, 4);
                puntos.add(new double[]{D_mm, pct});

                double d40 = Math.abs(t - 40.0/60.0);
                if (d40 < diff40s) { diff40s = d40; lectCorr40s = Rc; }
                double d2h = Math.abs(t - 120.0);
                if (d2h < diff2h)  { diff2h  = d2h;  lectCorr2h  = Rc; }
            } catch (Exception ex) {}
        }

        if (puntos.isEmpty()) { showError("No se pudieron calcular datos"); return; }

        double pctArenaFina, pctArcilla, pctLimo;
        if(!Double.isNaN(lectCorr40s) && !Double.isNaN(lectCorr2h)){
            pctArenaFina = Math.max(0, 100.0 - (lectCorr40s/masaMuestra)*100.0);
            pctArcilla   = Math.min(100, Math.max(0,(lectCorr2h*100.0)/masaMuestra));
            pctLimo      = Math.max(0, 100.0 - pctArenaFina - pctArcilla);
        } else {
            double pctFinos2h  = interpolarPorTiempo(120);
            double pctFinos24h = interpolarPorTiempo(1440);
            pctArcilla   = Double.isNaN(pctFinos24h) ? pctFinos2h*0.55 : pctFinos24h;
            pctLimo      = Math.max(0, pctFinos2h - pctArcilla);
            pctArenaFina = Math.max(0, 100.0 - pctFinos2h);
        }

        String textural = SueloData.clasificarTextural(pctArcilla, pctLimo, pctArenaFina);
        datos.tablaHidro = new ArrayList<>();
        for(int i=0;i<n;i++){
            try {
                datos.tablaHidro.add(new String[]{
                    String.valueOf(tablaModel.getValueAt(i,0)),
                    String.valueOf(tablaModel.getValueAt(i,1)),
                    String.valueOf(tablaModel.getValueAt(i,2)),
                    String.valueOf(tablaModel.getValueAt(i,3)),
                    String.valueOf(tablaModel.getValueAt(i,4))
                });
            } catch(Exception ex){}
        }
        datos.masaSecaHidro = Ws;
        datos.GsHidro       = Gs;
        datos.pctArcilla    = pctArcilla;
        datos.pctLimo       = pctLimo;
        datos.pctArenaFina  = pctArenaFina;
        datos.claseTextural = textural;
        datos.puntosHidro   = puntos;
        datos.hidroCalculada = true;
        datos.notificar();

        triangulo.setPuntoTextural(pctArcilla, pctLimo, pctArenaFina, textural);
        valArena.setText(String.format("%.1f%%", pctArenaFina));
        valLimo.setText(String.format("%.1f%%", pctLimo));
        valArcilla.setText(String.format("%.1f%%", pctArcilla));
        lblClase.setText("Clase textural: " + textural);
        lblResultado.setText("Cálculo completado — " + puntos.size() + " lecturas");
        lblResultado.setForeground(GREEN);
    }

    private double interpolarPorTiempo(double tMin) {
        double mejor = Double.NaN, diff = Double.MAX_VALUE;
        for (int i=0; i<tablaModel.getRowCount(); i++) {
            try {
                double t = Double.parseDouble(tablaModel.getValueAt(i,0).toString().trim().replace(",","."));
                if (Math.abs(t-tMin) < diff) {
                    diff = Math.abs(t-tMin);
                    Object pctObj = tablaModel.getValueAt(i,3);
                    if (pctObj!=null && !pctObj.toString().isEmpty()) {
                        mejor = Double.parseDouble(pctObj.toString().replace("%","").trim());
                    }
                }
            } catch (Exception ignored) {}
        }
        return mejor;
    }

    private void cargarEjemplo() {
        tablaModel.setRowCount(0);
        double[][] ej = {
            {0.5,1.038,25},{1,1.035,25},{2,1.032,24},{4,1.028,24},
            {8,1.022,24},{15,1.018,23},{30,1.014,23},{60,1.010,23},
            {120,1.007,22},{480,1.004,22},{1440,1.002,21}
        };
        for (double[] row : ej) agregarFila(fmt(row[0]),fmt(row[1]),fmt(row[2]));
        campoMasa.setText("50"); campoGs.setText("2.65");
        campoBoy0.setText("1.040"); campoTemp0.setText("25");
        lblResultado.setText("Datos de ejemplo cargados — presione Calcular");
        lblResultado.setForeground(MUTED);
    }

    private void cargarCSV() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("CSV","csv","txt"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try (BufferedReader br = new BufferedReader(new FileReader(fc.getSelectedFile()))) {
            tablaModel.setRowCount(0);
            String line;
            while ((line=br.readLine())!=null) {
                String[] c = line.split("[,;\\t]");
                if (c.length>=2 && c[0].trim().matches(".*\\d.*"))
                    agregarFila(c[0].trim(),c[1].trim(),c.length>2?c[2].trim():"25");
            }
        } catch (Exception ex) { showError("Error CSV: "+ex.getMessage()); }
    }

    private void agregarFila(String t,String b,String te){
        tablaModel.addRow(new Object[]{t,b,te.isEmpty()?"25":te,"",""});
    }

    private void enfocarUltima(){
        int last = tablaModel.getRowCount()-1;
        if(last<0) return;
        tabla.scrollRectToVisible(tabla.getCellRect(last,0,true));
        tabla.editCellAt(last,0);
        Component c = tabla.getEditorComponent();
        if(c!=null) c.requestFocusInWindow();
    }

    private void quitarSeleccionadas(){
        if (tabla.isEditing()) tabla.getCellEditor().cancelCellEditing();
        int[] sel = tabla.getSelectedRows();
        if(sel.length==0){ showError("Seleccione la(s) fila(s) a quitar"); return; }
        for(int i=sel.length-1;i>=0;i--) tablaModel.removeRow(sel[i]);
    }

    private void limpiar(){
        if (tabla.isEditing()) tabla.getCellEditor().cancelCellEditing();
        tablaModel.setRowCount(0);
        triangulo.limpiar();
        valArena.setText("—"); valLimo.setText("—"); valArcilla.setText("—");
        lblClase.setText(" ");
        lblResultado.setText("Datos limpiados");
        lblResultado.setForeground(MUTED);
        datos.hidroCalculada = false;
        datos.notificar();
    }

    private static GridBagConstraints gbc(int x,int y,int w,double wx){
        GridBagConstraints c = new GridBagConstraints();
        c.gridx=x; c.gridy=y; c.gridwidth=w; c.weightx=wx;
        c.fill=GridBagConstraints.HORIZONTAL; c.anchor=GridBagConstraints.WEST;
        c.insets=new Insets(3,3,3,3);
        return c;
    }
    private static void addPar(JPanel p,int fila,int col,String txt,JComponent comp){ addPar(p,fila,col,txt,comp,1); }
    private static void addPar(JPanel p,int fila,int col,String txt,JComponent comp,int span){
        p.add(label(txt), gbc(col,fila,1,0));
        p.add(comp, gbc(col+1,fila,span,1));
    }

    private JPanel tarjeta(String titulo, JLabel valor, Color c){
        JPanel t = new JPanel(new BorderLayout(0,2));
        t.setBackground(BG2);
        t.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(3,0,0,0,c),
            BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0,1,1,1,BORD),
                BorderFactory.createEmptyBorder(6,8,8,8))));
        JLabel l = new JLabel(titulo, SwingConstants.CENTER);
        l.setFont(Tema.ui(Font.BOLD, 11)); l.setForeground(c);
        valor.setFont(Tema.ui(Font.BOLD, 20)); valor.setForeground(TEXT);
        t.add(l, BorderLayout.NORTH);
        t.add(valor, BorderLayout.CENTER);
        return t;
    }

    private static void estilizarCombo(JComboBox<?> c){
        c.setUI(new BasicComboBoxUI(){
            @Override protected JButton createArrowButton(){
                JButton b = new JButton("▾");
                b.setFont(Tema.ui(Font.PLAIN, 11));
                b.setBackground(BG3); b.setForeground(MUTED);
                b.setBorder(BorderFactory.createEmptyBorder(0,6,0,6));
                b.setFocusPainted(false);
                return b;
            }
            @Override public void paintCurrentValueBackground(Graphics g,Rectangle b,boolean f){
                g.setColor(BG3); g.fillRect(b.x,b.y,b.width,b.height);
            }
        });
        c.setBackground(BG3); c.setForeground(TEXT);
        c.setFont(Tema.ui(Font.PLAIN, 12));
        c.setBorder(BorderFactory.createLineBorder(BORD));
        c.setMinimumSize(new Dimension(80,26));
        c.setRenderer(new DefaultListCellRenderer(){
            @Override public Component getListCellRendererComponent(JList<?> l,Object v,int i,boolean sel,boolean foc){
                super.getListCellRendererComponent(l,v,i,sel,foc);
                setBackground(sel ? new Color(35,50,80) : BG3);
                setForeground(sel ? ACCENT : TEXT);
                setBorder(BorderFactory.createEmptyBorder(3,6,3,6));
                return this;
            }
        });
    }

    private static void estilizarTabla(JTable t){
        t.setBackground(BG2); t.setForeground(TEXT);
        t.setFont(Tema.ui(Font.PLAIN, 12)); t.setRowHeight(26);
        t.setGridColor(BORD); t.setShowGrid(true);
        t.setSelectionBackground(new Color(35,50,80)); t.setSelectionForeground(TEXT);
        t.setFillsViewportHeight(true);
        t.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        t.getTableHeader().setReorderingAllowed(false);
        t.getTableHeader().setPreferredSize(new Dimension(0,28));
    }

    private static class HeaderRenderer extends DefaultTableCellRenderer {
        private final String[] tips;
        HeaderRenderer(String[] tips){ this.tips=tips; setHorizontalAlignment(CENTER); }
        @Override public Component getTableCellRendererComponent(JTable t,Object v,boolean s,boolean f,int r,int c){
            super.getTableCellRendererComponent(t,v,false,false,r,c);
            setBackground(BG4); setForeground(MUTED);
            setFont(Tema.ui(Font.BOLD, 11));
            setBorder(BorderFactory.createMatteBorder(0,0,1,1,BORD));
            setToolTipText(c<tips.length?tips[c]:null);
            return this;
        }
    }

    private static TableCellRenderer celda(Color fg, Color bg){
        return new DefaultTableCellRenderer(){
            { setHorizontalAlignment(RIGHT); }
            @Override public Component getTableCellRendererComponent(JTable t,Object v,boolean s,boolean f,int r,int c){
                super.getTableCellRendererComponent(t,v,s,false,r,c);
                setBackground(s ? new Color(35,50,80) : bg);
                setForeground(fg);
                setFont(Tema.ui(Font.PLAIN, 12));
                setBorder(BorderFactory.createEmptyBorder(0,6,0,8));
                return this;
            }
        };
    }

    private static JLabel label(String t){
        JLabel l=new JLabel(t); l.setForeground(MUTED);
        l.setFont(Tema.ui(Font.PLAIN, 12)); return l;
    }
    private static JTextField field(String v){
        JTextField f=new JTextField(v,5);
        f.setMinimumSize(new Dimension(56,26));
        f.setBackground(BG3); f.setForeground(TEXT);
        f.setFont(Tema.ui(Font.PLAIN, 12));
        f.setCaretColor(ACCENT);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORD),BorderFactory.createEmptyBorder(4,6,4,6)));
        return f;
    }
    private static JButton btnAccion(String t, Color fg, ActionListener al){
        JButton b = Tema.boton(t, fg);
        b.addActionListener(al);
        return b;
    }
    private static String fmt(double v){ return v==(long)v?String.valueOf((long)v):String.valueOf(v); }
    private void showError(String msg){ JOptionPane.showMessageDialog(this,msg,"Error",JOptionPane.ERROR_MESSAGE); }

    static class WrapLayout extends FlowLayout {
        WrapLayout(int align,int h,int v){ super(align,h,v); }
        @Override public Dimension preferredLayoutSize(Container t){ return medir(t,true); }
        @Override public Dimension minimumLayoutSize(Container t){
            Dimension d = medir(t,false); d.width -= getHgap()+1; return d;
        }
        private Dimension medir(Container target, boolean pref){
            synchronized (target.getTreeLock()){
                int anchoDestino = target.getSize().width;
                Container c = target;
                while (c.getSize().width==0 && c.getParent()!=null) c = c.getParent();
                if (anchoDestino==0) anchoDestino = c.getSize().width;
                if (anchoDestino==0) anchoDestino = Integer.MAX_VALUE;
                Insets in = target.getInsets();
                int maxAncho = anchoDestino - (in.left+in.right+getHgap()*2);
                Dimension dim = new Dimension(0,0);
                int filaW=0, filaH=0;
                for (Component m : target.getComponents()){
                    if(!m.isVisible()) continue;
                    Dimension d = pref ? m.getPreferredSize() : m.getMinimumSize();
                    if (filaW + d.width > maxAncho){
                        dim.width = Math.max(dim.width,filaW);
                        if(dim.height>0) dim.height += getVgap();
                        dim.height += filaH;
                        filaW=0; filaH=0;
                    }
                    if (filaW!=0) filaW += getHgap();
                    filaW += d.width;
                    filaH = Math.max(filaH,d.height);
                }
                dim.width = Math.max(dim.width,filaW);
                if(dim.height>0) dim.height += getVgap();
                dim.height += filaH;
                dim.width  += in.left+in.right+getHgap()*2;
                dim.height += in.top+in.bottom+getVgap()*2;
                Container sp = SwingUtilities.getAncestorOfClass(JScrollPane.class,target);
                if (sp!=null && target.isValid()) dim.width -= getHgap()+1;
                return dim;
            }
        }
    }
}