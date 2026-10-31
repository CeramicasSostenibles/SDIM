package geoceramiclab.ensayos;

import geoceramiclab.Tema;
import geoceramiclab.model.SueloData;
import geoceramiclab.graficas.CurvaGranPanel;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.io.*;
import javax.swing.filechooser.FileNameExtensionFilter;

public class GranulometriaPanel extends JPanel {

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

    private static final double TAMIZ_N4   = 4.750;

    private static final double TAMIZ_N200 = 0.075;

    private static final double TOL_MM     = 1e-4;

    private static final Object[][] CATALOGO = {

        {"6\"",        "152 mm",    152.000},
        {"4\"",        "100 mm",    100.000},
        {"3\"",         "75 mm",     75.000},
        {"2\"",         "50 mm",     50.000},
        {"1½\"",        "37 mm",     37.000},
        {"1\"",         "25 mm",     25.000},
        {"¾\"",         "19 mm",     19.000},
        {"½\"",       "12.5 mm",     12.500},
        {"⅜\"",        "9.5 mm",      9.500},

        {"N° 3½",     "5.600 mm",     5.600},
        {"N° 4",      "4.750 mm",     4.750},
        {"N° 5",      "4.000 mm",     4.000},
        {"N° 6",      "3.350 mm",     3.350},
        {"N° 7",      "2.800 mm",     2.800},
        {"N° 8",      "2.360 mm",     2.360},
        {"N° 10",     "2.000 mm",     2.000},
        {"N° 12",     "1.700 mm",     1.700},
        {"N° 14",     "1.400 mm",     1.400},
        {"N° 16",     "1.180 mm",     1.180},
        {"N° 18",     "1.000 mm",     1.000},
        {"N° 20",     "0.850 mm",     0.850},
        {"N° 25",     "0.710 mm",     0.710},
        {"N° 30",     "0.600 mm",     0.600},
        {"N° 35",     "0.500 mm",     0.500},
        {"N° 40",     "0.425 mm",     0.425},
        {"N° 45",     "0.355 mm",     0.355},
        {"N° 50",     "0.300 mm",     0.300},

        {"N° 60",     "250 µm",       0.250},
        {"N° 70",     "212 µm",       0.212},
        {"N° 80",     "180 µm",       0.180},
        {"N° 100",    "150 µm",       0.150},
        {"N° 120",    "125 µm",       0.125},
        {"N° 140",    "106 µm",       0.106},
        {"N° 170",     "90 µm",       0.090},
        {"N° 200",     "75 µm",       0.075},
        {"N° 230",     "63 µm",       0.063},
        {"N° 270",     "53 µm",       0.053},
        {"N° 325",     "45 µm",       0.045},

        {"N° 400",     "38 µm",       0.038},
        {"N° 450",     "32 µm",       0.032},
        {"N° 500",     "25 µm",       0.025},
        {"N° 635",     "20 µm",       0.020},

        {"Fondo",      "< 20 µm",     0.000},
    };

    static class FilaTamiz {
        String  ingles;
        String  intl;
        double  mm;
        double  pesoRet = 0;

        double  pctRet  = 0;
        double  pctAcum = 0;
        double  pctPasa = 0;

        FilaTamiz(String ing, String intl, double mm) {
            this.ingles=ing; this.intl=intl; this.mm=mm;
        }
        FilaTamiz(String ing, String intl, double mm, double peso) {
            this(ing,intl,mm); this.pesoRet=peso;
        }
    }

    private final SueloData datos;
    private final ArrayList<FilaTamiz> filas = new ArrayList<>();

    private final DefaultTableModel tablaModel;
    private       JTable            tabla;

    private final JTextField campoMasa, campoFinos;
    private final JLabel     lblResultado;
    private final CurvaGranPanel graficaPanel;
    private final JComboBox<String> cmbSistema;
    private final JComboBox<String> cmbAgregar;

    private int sistemaActivo = 0;

    private boolean calculado = false;

    public GranulometriaPanel(SueloData datos) {
        this.datos = datos;
        setLayout(new BorderLayout(0,0));
        setBackground(BG);

        JPanel izq = new JPanel(new BorderLayout(0,6));
        izq.setBackground(BG);
        izq.setBorder(BorderFactory.createEmptyBorder(8,10,8,6));

        JPanel fila1 = new JPanel(new FlowLayout(FlowLayout.LEFT,8,4));
        fila1.setBackground(BG2);
        fila1.setBorder(BorderFactory.createMatteBorder(0,0,1,0,BORD));
        fila1.add(lbl("Masa total (g):")); campoMasa = fld("500",58); fila1.add(campoMasa);
        fila1.add(lbl("% Finos manual (opcional):")); campoFinos = fld("",52);
        campoFinos.setToolTipText("Vacío = automático desde tamiz N°200 (0.075 mm)");
        fila1.add(campoFinos);
        izq.add(fila1, BorderLayout.NORTH);

        String[] cols = {"Inglés","Internacional","mm","Peso ret. (g)","% Ret.","% Ret. Acum.","% Pasa","  "};
        tablaModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) {
                return c == 3;
            }
            @Override public Class<?> getColumnClass(int c) { return String.class; }
        };

        tabla = new JTable(tablaModel) {

            @Override public boolean isCellEditable(int r, int c) { return c==3||c==7; }
        };
        estilizarTabla(tabla);

        tabla.getColumnModel().getColumn(0).setPreferredWidth(62);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(90);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(62);
        tabla.getColumnModel().getColumn(3).setPreferredWidth(90);
        tabla.getColumnModel().getColumn(4).setPreferredWidth(58);
        tabla.getColumnModel().getColumn(5).setPreferredWidth(88);
        tabla.getColumnModel().getColumn(6).setPreferredWidth(58);
        tabla.getColumnModel().getColumn(7).setPreferredWidth(30);

        tabla.getColumnModel().getColumn(3).setCellRenderer(rendPeso());
        tabla.getColumnModel().getColumn(3).setCellEditor(new DefaultCellEditor(fldTabla()));

        DefaultTableCellRenderer rv = rendVerde();
        for(int c=4;c<=6;c++) tabla.getColumnModel().getColumn(c).setCellRenderer(rv);

        tabla.getColumnModel().getColumn(7).setCellRenderer(new BtnEliminarRenderer());
        tabla.getColumnModel().getColumn(7).setCellEditor(new BtnEliminarEditor());

        tabla.addPropertyChangeListener("tableCellEditor", e -> {
            if(!tabla.isEditing()) guardarPesos();
        });

        JScrollPane scrollTabla = scrollOscuro(tabla);
        izq.add(scrollTabla, BorderLayout.CENTER);

        JPanel filaAgregar = new JPanel();
        filaAgregar.setLayout(new BoxLayout(filaAgregar, BoxLayout.Y_AXIS));
        filaAgregar.setBackground(BG4);
        JPanel filaSis = new JPanel(new FlowLayout(FlowLayout.LEFT,6,3));
        filaSis.setOpaque(false);
        JPanel filaTam = new JPanel(new FlowLayout(FlowLayout.LEFT,6,3));
        filaTam.setOpaque(false);
        filaAgregar.add(filaSis);
        filaAgregar.add(filaTam);
        filaAgregar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1,0,0,0,BORD),
            BorderFactory.createEmptyBorder(4,8,4,8)));

        JLabel lSis = new JLabel("Sistema:");
        lSis.setFont(Tema.ui(Font.BOLD, 11)); lSis.setForeground(Tema.TEXTO_2);
        filaSis.add(lSis);

        cmbSistema = new JComboBox<>(new String[]{"Sistema Inglés","Sistema Internacional"});
        estilizarCombo(cmbSistema);
        cmbSistema.setPreferredSize(new Dimension(168,26));
        cmbSistema.addActionListener(e -> {
            sistemaActivo = cmbSistema.getSelectedIndex();
            actualizarCmbAgregar();
        });
        filaSis.add(cmbSistema);

        JLabel lAgregar = new JLabel("Agregar tamiz:");
        lAgregar.setFont(Tema.ui(Font.BOLD, 11)); lAgregar.setForeground(Tema.TEXTO_2);
        filaTam.add(lAgregar);

        cmbAgregar = new JComboBox<>();
        estilizarCombo(cmbAgregar);
        cmbAgregar.setPreferredSize(new Dimension(138,26));
        actualizarCmbAgregar();
        filaTam.add(cmbAgregar);

        JButton btnAgregar = Tema.boton("+ Agregar", Tema.Tipo.SECUNDARIO);
        btnAgregar.addActionListener(e -> agregarTamizSeleccionado());
        filaTam.add(btnAgregar);

        filaTam.add(Box.createHorizontalStrut(4));

        JButton btnManual = Tema.boton("+ Manual", Tema.Tipo.SECUNDARIO);
        btnManual.setToolTipText("Ingresar un tamiz personalizado (mm)");
        btnManual.addActionListener(e -> agregarManual());
        filaTam.add(btnManual);

        izq.add(filaAgregar, BorderLayout.SOUTH);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT,6,6));
        acciones.setBackground(BG);

        JButton btnCargarTodos = Tema.boton("Cargar todos", Tema.Tipo.SECUNDARIO);
        btnCargarTodos.setToolTipText("Carga todos los tamices del sistema seleccionado");
        btnCargarTodos.addActionListener(e -> cargarTodosLosTamices());
        acciones.add(btnCargarTodos);

        JButton btnEjemplo = mkBtn("Ejemplo",BG3,MUTED,11);
        btnEjemplo.addActionListener(e -> cargarEjemplo());
        acciones.add(btnEjemplo);

        acciones.add(btnCSV());

        JButton btnLimpiar = Tema.boton("Limpiar", Tema.Tipo.PELIGRO);
        btnLimpiar.addActionListener(e -> limpiar());
        acciones.add(btnLimpiar);

        acciones.add(Box.createHorizontalStrut(12));

        JButton btnCalc = Tema.boton("Calcular granulometría", Tema.Tipo.PRIMARIO);
        btnCalc.addActionListener(e -> calcular());
        acciones.add(btnCalc);

        JPanel izqConAcciones = new JPanel(new BorderLayout(0,0));
        izqConAcciones.setBackground(BG);
        izqConAcciones.add(izq,     BorderLayout.CENTER);
        izqConAcciones.add(acciones,BorderLayout.SOUTH);

        JPanel der = new JPanel(new BorderLayout(0,6));
        der.setBackground(BG);
        der.setBorder(BorderFactory.createEmptyBorder(8,6,8,10));

        graficaPanel = new CurvaGranPanel();
        graficaPanel.setBackground(BG2);
        der.add(graficaPanel, BorderLayout.CENTER);

        lblResultado = new JLabel("Presione Calcular para ver D₁₀ D₃₀ D₆₀ Cu Cc");
        lblResultado.setFont(Tema.ui(Font.PLAIN, 12));
        lblResultado.setForeground(DIM);
        lblResultado.setBorder(BorderFactory.createEmptyBorder(6,6,2,4));
        der.add(lblResultado, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,izqConAcciones,der);
        split.setDividerSize(6);
        split.setContinuousLayout(true);
        split.setBackground(BG);
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setResizeWeight(0.42);
        izqConAcciones.setMinimumSize(new Dimension(380, 0));
        der.setMinimumSize(new Dimension(360, 0));
        add(split, BorderLayout.CENTER);

        addComponentListener(new ComponentAdapter() {
            private boolean colocado = false;
            @Override public void componentResized(ComponentEvent e) {
                if (!colocado && getWidth() > 0) {
                    split.setDividerLocation(0.42);
                    colocado = true;
                }
            }
        });

        cargarEjemplo();
    }

    private static Object[] buscarEnCatalogo(double mm) {
        for(Object[] cat : CATALOGO)
            if(Math.abs((double)cat[2] - mm) < TOL_MM) return cat;
        return null;
    }

    private void actualizarCmbAgregar() {
        cmbAgregar.removeAllItems();
        for(Object[] cat : CATALOGO) {
            String nombre = sistemaActivo==0 ? (String)cat[0] : (String)cat[1];
            cmbAgregar.addItem(nombre);
        }
    }

    private void agregarTamizSeleccionado() {
        int idx = cmbAgregar.getSelectedIndex();
        if(idx<0) return;
        Object[] cat = CATALOGO[idx];
        double mm = (double)cat[2];

        for(FilaTamiz f : filas) if(Math.abs(f.mm-mm)<TOL_MM) {
            JOptionPane.showMessageDialog(this,
                "El tamiz "+cat[0]+" / "+cat[1]+" ya está en la tabla.",
                "Duplicado", JOptionPane.WARNING_MESSAGE);
            return;
        }

        FilaTamiz nueva = new FilaTamiz((String)cat[0],(String)cat[1],mm,0);
        filas.add(nueva);
        ordenarFilas();
        reconstruirTabla();
    }

    private void agregarManual() {
        JPanel dlgPanel = new JPanel(new GridLayout(0,2,8,6));
        dlgPanel.setBackground(BG2);
        JTextField fIng  = fld("",100); JTextField fIntl = fld("",100);
        JTextField fMm   = fld("",80);
        addDlgFila(dlgPanel,"Nombre inglés (ej: N° 4):",fIng);
        addDlgFila(dlgPanel,"Nombre SI (ej: 4.750 mm):",fIntl);
        addDlgFila(dlgPanel,"Apertura (mm):",fMm);
        dlgPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        int res = JOptionPane.showConfirmDialog(this,dlgPanel,
            "Agregar tamiz personalizado",JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE);
        if(res!=JOptionPane.OK_OPTION) return;

        try{
            double mm = Double.parseDouble(fMm.getText().trim().replace(",","."));
            String ing  = fIng.getText().trim().isEmpty()  ? String.format("%.3f mm",mm) : fIng.getText().trim();
            String intl = fIntl.getText().trim().isEmpty() ? String.format("%.3f mm",mm) : fIntl.getText().trim();

            for(FilaTamiz f:filas) if(Math.abs(f.mm-mm)<TOL_MM){
                JOptionPane.showMessageDialog(this,"Ya existe un tamiz de "+mm+" mm.","Duplicado",JOptionPane.WARNING_MESSAGE);
                return;
            }
            filas.add(new FilaTamiz(ing,intl,mm,0));
            ordenarFilas();
            reconstruirTabla();
        }catch(NumberFormatException ex){
            JOptionPane.showMessageDialog(this,"Apertura (mm) inválida.","Error",JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addDlgFila(JPanel p, String lbl, JTextField f){
        JLabel l=new JLabel(lbl); l.setFont(Tema.ui(Font.PLAIN, 12));
        l.setForeground(MUTED); p.add(l); p.add(f);
    }

    private void eliminarFila(int idx) {
        if(idx>=0 && idx<filas.size()){
            filas.remove(idx);
            reconstruirTabla();
        }
    }

    private void ordenarFilas() {
        filas.sort((a,b) -> {
            if (a.mm == 0 && b.mm != 0) return 1;
            if (b.mm == 0 && a.mm != 0) return -1;
            return Double.compare(b.mm, a.mm);
        });
    }

    private void guardarPesos() {
        for(int i=0;i<Math.min(filas.size(),tablaModel.getRowCount());i++){
            Object val = tablaModel.getValueAt(i,3);
            if(val==null||val.toString().trim().isEmpty()) { filas.get(i).pesoRet=0; continue; }
            try{ filas.get(i).pesoRet=Double.parseDouble(val.toString().trim().replace(",",".")); }
            catch(NumberFormatException ignored){}
        }
    }

    private void reconstruirTabla() {
        tablaModel.setRowCount(0);
        for(int i=0;i<filas.size();i++){
            FilaTamiz f = filas.get(i);
            boolean esFondo = (f.mm == 0.000);

            String mmStr = esFondo ? "—" :
                           (f.mm >= 1 ? String.format("%.2f",f.mm)
                                      : String.format("%.4f",f.mm));

            String sRet  = calculado ? String.format("%.1f%%", f.pctRet)  : "";
            String sAcum = calculado ? String.format("%.1f%%", f.pctAcum) : "";
            String sPasa = !calculado ? "" : (esFondo ? "0.0%" : String.format("%.1f%%", f.pctPasa));

            tablaModel.addRow(new Object[]{
                f.ingles, f.intl, mmStr,
                f.pesoRet > 0 ? String.format("%.1f",f.pesoRet) : "",
                sRet, sAcum, sPasa,
                "✕"
            });
        }
    }

    private void cargarTodosLosTamices() {
        filas.clear();
        calculado = false;
        for(Object[] cat : CATALOGO)
            filas.add(new FilaTamiz((String)cat[0],(String)cat[1],(double)cat[2],0));
        reconstruirTabla();
    }

    private void cargarEjemplo() {
        filas.clear();
        calculado = false;

        double[][] ej={
            {152,0},{100,0},{75,0},{50,12},{37,25},{25,18},
            {12.5,30},{9.5,22},{4.750,48},{2.360,60},{1.180,55},
            {0.600,80},{0.300,55},{0.150,40},{0.075,25}
        };
        for(double[] e:ej){
            Object[] cat = buscarEnCatalogo(e[0]);
            if(cat!=null) filas.add(new FilaTamiz((String)cat[0],(String)cat[1],(double)cat[2],e[1]));
        }

        filas.add(new FilaTamiz("Fondo","< 20 µm",0.000,18));
        campoMasa.setText("500");
        reconstruirTabla();
        lblResultado.setText("Datos de ejemplo cargados (incluye Fondo) — presione Calcular");
        lblResultado.setForeground(MUTED);
    }

    private void limpiar(){
        filas.clear();
        calculado = false;
        tablaModel.setRowCount(0);
        graficaPanel.setData(Collections.emptyList());

        datos.granCalculada = false;
        datos.notificar();
        lblResultado.setText("Tabla limpiada");
        lblResultado.setForeground(DIM);
    }

    private JButton btnCSV(){
        JButton b = mkBtn("CSV",BG3,MUTED,11);
        b.addActionListener(e->{
            JFileChooser fc=new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter("CSV","csv","txt"));
            if(fc.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION) return;
            try(BufferedReader br=new BufferedReader(new FileReader(fc.getSelectedFile()))){
                filas.clear();
                calculado = false;
                String line;
                while((line=br.readLine())!=null){
                    String[] p=line.split("[,;\\t]");
                    if(p.length<2) continue;
                    try{
                        double mm=Double.parseDouble(p[0].trim().replace(",","."));
                        double peso=Double.parseDouble(p[1].trim().replace(",","."));

                        String ing=String.format("%.3f mm",mm), intl=ing;
                        Object[] cat = buscarEnCatalogo(mm);
                        if(cat!=null){ ing=(String)cat[0]; intl=(String)cat[1]; }
                        filas.add(new FilaTamiz(ing,intl,mm,peso));
                    }catch(NumberFormatException ignored){}
                }
                ordenarFilas();
                reconstruirTabla();
            }catch(Exception ex){
                JOptionPane.showMessageDialog(this,"Error CSV: "+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
            }
        });
        return b;
    }

    private void calcular(){
        guardarPesos();
        if(filas.size()<2){ error("Ingrese al menos 2 tamices"); return; }

        double masaTotal;
        try{ masaTotal=Double.parseDouble(campoMasa.getText().trim().replace(",",".")); }
        catch(Exception e){ error("Masa total inválida"); return; }
        if(masaTotal<=0){ error("La masa total debe ser mayor que cero"); return; }

        ordenarFilas();

        double acum = 0;
        for(FilaTamiz f : filas){
            f.pctRet  = (f.pesoRet / masaTotal) * 100.0;
            acum     += f.pctRet;
            f.pctAcum = acum;
            f.pctPasa = Math.max(0, 100.0 - acum);
        }

        double sumaPesos = filas.stream().mapToDouble(f->f.pesoRet).sum();
        if(sumaPesos > masaTotal * 1.05){
            error(String.format("Suma de pesos retenidos (%.1f g) supera la masa total (%.1f g).\nRevise los datos.", sumaPesos, masaTotal));
            return;
        }

        double pctFinos;
        String finosTxt = campoFinos.getText().trim();
        if(finosTxt.isEmpty()){
            pctFinos = Math.max(0, getPasa(TAMIZ_N200));
        } else {
            try{ pctFinos = Double.parseDouble(finosTxt.replace(",",".")); }
            catch(NumberFormatException ex){ error("% Finos manual inválido"); return; }
            if(pctFinos<0 || pctFinos>100){ error("% Finos manual debe estar entre 0 y 100"); return; }
        }

        if(finosTxt.isEmpty() && buscarEnFilas(TAMIZ_N200)==null){
            JOptionPane.showMessageDialog(this,
                "No hay tamiz N° 200 (0.075 mm) en la tabla.\n"
              + "El % de finos se estimó por interpolación y puede ser impreciso.\n\n"
              + "Ese valor es el que decide si aplican los límites de Atterberg.",
                "Aviso", JOptionPane.WARNING_MESSAGE);
        }

        Double D10=getDiam(10), D30=getDiam(30), D60=getDiam(60);
        Double Cu = D10!=null&&D60!=null&&D10>0 ? D60/D10 : null;
        Double Cc = D10!=null&&D30!=null&&D60!=null&&D10>0&&D60>0 ? (D30*D30)/(D10*D60) : null;

        double pctGrava = Math.max(0, 100 - getPasa(TAMIZ_N4));
        double pctArena = Math.max(0, 100 - pctGrava - pctFinos);

        datos.D10=D10; datos.D30=D30; datos.D60=D60;
        datos.Cu=Cu; datos.Cc=Cc;
        datos.pctGrava=pctGrava; datos.pctArena=pctArena; datos.pctFinos=pctFinos;
        datos.puntosCurvaGran = new ArrayList<>();
        datos.tablaGran = new ArrayList<>();
        datos.masaTotalGran = masaTotal;

        calculado = true;

        for(FilaTamiz f : filas) {
            if(f.mm > 0) datos.puntosCurvaGran.add(new double[]{f.mm, f.pctPasa});
            boolean esFondo = (f.mm == 0.000);
            String mmStr = esFondo ? "—" : (f.mm >= 1 ? String.format("%.2f",f.mm) : String.format("%.4f",f.mm));
            String pPasa = esFondo ? "0.0%" : String.format("%.1f%%",f.pctPasa);
            datos.tablaGran.add(new String[]{
                f.ingles, f.intl, mmStr,
                f.pesoRet > 0 ? String.format("%.1f",f.pesoRet) : "",
                String.format("%.1f%%",f.pctRet),
                String.format("%.1f%%",f.pctAcum),
                pPasa
            });
        }

        datos.granCalculada = true;
        datos.notificar();

        reconstruirTabla();
        graficaPanel.setData(datos.puntosCurvaGran);

        String notaAtt;
        if(pctFinos < 5)       notaAtt = "<font color='#e05a5a'>Atterberg NO aplica (grueso limpio)</font>";
        else if(pctFinos < 12) notaAtt = "<font color='#e0a33a'>Atterberg aplica (símbolo doble)</font>";
        else if(pctFinos < 50) notaAtt = "<font color='#e0a33a'>Atterberg aplica (SM/SC · GM/GC)</font>";
        else                   notaAtt = "<font color='#4cb782'>Suelo fino — Atterberg aplica</font>";

        lblResultado.setText(String.format(
            "<html><font color='#e6ebf0'>Grava <b>%.1f%%</b>  Arena <b>%.1f%%</b>  Finos <b>%.1f%%</b>" +
            "&nbsp;&nbsp;|&nbsp;&nbsp;" +
            "<font color='#4cb782'>D₁₀=%s  D₃₀=%s  D₆₀=%s</font>" +
            "&nbsp;&nbsp;<font color='#e0a33a'>Cu=%s  Cc=%s</font>" +
            "<br>%s</font></html>",
            pctGrava,pctArena,pctFinos,
            D10!=null?String.format("%.3f",D10):"—",
            D30!=null?String.format("%.3f",D30):"—",
            D60!=null?String.format("%.3f",D60):"—",
            Cu!=null?String.format("%.2f",Cu):"—",
            Cc!=null?String.format("%.2f",Cc):"—",
            notaAtt
        ));
        lblResultado.setForeground(GREEN);
    }

    private FilaTamiz buscarEnFilas(double mm){
        for(FilaTamiz f : filas) if(Math.abs(f.mm-mm)<TOL_MM) return f;
        return null;
    }

    private List<FilaTamiz> filasReales() {
        List<FilaTamiz> r = new ArrayList<>();
        for(FilaTamiz f : filas) if(f.mm > 0) r.add(f);
        return r;
    }

    private double getPasa(double mm){
        List<FilaTamiz> fr = filasReales();
        if(fr.isEmpty()) return 0;
        if(mm >= fr.get(0).mm) return 100;
        if(mm <= fr.get(fr.size()-1).mm) return fr.get(fr.size()-1).pctPasa;
        for(int i=0;i<fr.size()-1;i++){
            FilaTamiz hi=fr.get(i), lo=fr.get(i+1);
            if(hi.mm>=mm && lo.mm<=mm && lo.mm>0){
                double t=(Math.log10(mm)-Math.log10(lo.mm))/(Math.log10(hi.mm)-Math.log10(lo.mm));
                return lo.pctPasa + t*(hi.pctPasa-lo.pctPasa);
            }
        }
        return 0;
    }

    private Double getDiam(double pct){
        List<FilaTamiz> fr = filasReales();
        for(int i=0;i<fr.size()-1;i++){
            FilaTamiz hi=fr.get(i), lo=fr.get(i+1);
            if(hi.pctPasa>=pct && lo.pctPasa<=pct && lo.mm>0){
                double den = hi.pctPasa-lo.pctPasa;
                if(Math.abs(den) < 1e-9) continue;
                double t=(pct-lo.pctPasa)/den;
                double logD=Math.log10(lo.mm)+t*(Math.log10(hi.mm)-Math.log10(lo.mm));
                return Math.pow(10,logD);
            }
        }
        return null;
    }

    class BtnEliminarRenderer implements TableCellRenderer {
        private final JButton btn;
        BtnEliminarRenderer(){
            btn=new JButton("✕");
            btn.putClientProperty("JButton.buttonType", "borderless");
            btn.setFont(Tema.ui(Font.BOLD, 12));
            btn.setForeground(Tema.ERROR);
            btn.setFocusPainted(false);
            btn.setToolTipText("Quitar este tamiz");
        }
        @Override
        public Component getTableCellRendererComponent(JTable t,Object v,boolean sel,boolean foc,int r,int c){
            return btn;
        }
    }

    class BtnEliminarEditor extends AbstractCellEditor implements TableCellEditor {
        private final JButton btn;
        private int fila;
        BtnEliminarEditor(){
            btn=new JButton("✕");
            btn.putClientProperty("JButton.buttonType", "borderless");
            btn.setFont(Tema.ui(Font.BOLD, 12));
            btn.setForeground(Tema.ERROR);
            btn.setFocusPainted(false);
            btn.addActionListener(e->{
                fireEditingStopped();
                SwingUtilities.invokeLater(()->eliminarFila(fila));
            });
        }
        @Override
        public Component getTableCellEditorComponent(JTable t,Object v,boolean sel,int r,int c){
            fila=r; return btn;
        }
        @Override public Object getCellEditorValue(){ return "✕"; }
    }

    private static void estilizarTabla(JTable t){
        t.setBackground(BG2); t.setForeground(TEXT);
        t.setFont(Tema.ui(Font.PLAIN, 12));
        t.setRowHeight(26); t.setGridColor(BORD);
        t.setSelectionBackground(Tema.CONTROL);
        t.setSelectionForeground(ACCENT);
        t.setShowGrid(true);
        t.getTableHeader().setBackground(BG4);
        t.getTableHeader().setForeground(DIM);
        t.getTableHeader().setFont(Tema.ui(Font.BOLD, 11));
        t.getTableHeader().setReorderingAllowed(false);
    }

    private static TableCellRenderer rendPeso(){
        return new DefaultTableCellRenderer(){{
            setBackground(Tema.SUPERFICIE_2); setForeground(ACCENT);
            setFont(Tema.ui(Font.PLAIN, 12));
        }};
    }

    private static DefaultTableCellRenderer rendVerde(){
        return new DefaultTableCellRenderer(){{
            setBackground(BG2); setForeground(GREEN);
            setFont(Tema.ui(Font.PLAIN, 12));
        }};
    }

    private static JScrollPane scrollOscuro(JTable t){
        JScrollPane s=new JScrollPane(t);
        s.setBackground(BG2); s.getViewport().setBackground(BG2);
        s.setBorder(BorderFactory.createLineBorder(BORD));
        return s;
    }

    private static JTextField fldTabla(){
        JTextField f=new JTextField();
        f.setBackground(Tema.SUPERFICIE_2); f.setForeground(ACCENT);
        f.setFont(Tema.ui(Font.PLAIN, 12));
        f.setCaretColor(ACCENT); f.setHorizontalAlignment(JTextField.RIGHT);
        return f;
    }

    private static JLabel lbl(String t){
        JLabel l=new JLabel(t); l.setFont(Tema.ui(Font.PLAIN, 12));
        l.setForeground(MUTED); return l;
    }

    private static JTextField fld(String v,int w){
        JTextField f=new JTextField(v);
        f.setPreferredSize(new Dimension(w,26));
        f.setBackground(BG3); f.setForeground(TEXT);
        f.setFont(Tema.ui(Font.PLAIN, 12)); f.setCaretColor(ACCENT);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORD),BorderFactory.createEmptyBorder(3,5,3,5)));
        return f;
    }

    private static JButton mkBtn(String t,Color bg,Color fg,int sz){
        return Tema.boton(t, bg);
    }

    private static void estilizarCombo(JComboBox<?> c){
        c.setBackground(BG3); c.setForeground(TEXT);
        c.setFont(Tema.ui(Font.PLAIN, 12));
        ((JLabel)c.getRenderer()).setHorizontalAlignment(SwingConstants.LEFT);
    }

    private void error(String m){ JOptionPane.showMessageDialog(this,m,"Error",JOptionPane.ERROR_MESSAGE); }
}