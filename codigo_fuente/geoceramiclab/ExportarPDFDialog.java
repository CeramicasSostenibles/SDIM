package geoceramiclab;

import geoceramiclab.model.SueloData;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

public class ExportarPDFDialog extends JDialog {

    private static final Color C_BG     = Tema.FONDO;
    private static final Color C_BG2    = Tema.SUPERFICIE_2;
    private static final Color C_BG3    = new Color(25, 32, 52);
    private static final Color C_BORDER = new Color(45, 55, 85);
    private static final Color C_ACCENT = Tema.INFO;
    private static final Color C_GREEN  = Tema.OK;
    private static final Color C_AMBER  = new Color(245, 166, 35);
    private static final Color C_RED    = new Color(231, 76,  60);
    private static final Color C_TEXT   = new Color(220, 228, 248);
    private static final Color C_MUTED  = new Color(130, 150, 190);

    private final SueloData datos;
    private JCheckBox chkGran, chkAtter, chkHidro, chkTernario, chkResumen;
    private JTextField txtProyecto, txtMuestra, txtOperador, txtRuta;
    private JLabel     lblEstado;
    private JButton    btnGenerar;
    private JProgressBar progressBar;

    public ExportarPDFDialog(JFrame parent, SueloData datos) {
        super(parent, "Exportar Informe PDF \u2014 SDIM", true);
        this.datos = datos;
        buildUI();
        pack();
        setMinimumSize(new Dimension(640, 590));
        setLocationRelativeTo(parent);
    }

    private void buildUI() {
        getContentPane().setBackground(C_BG);
        setLayout(new BorderLayout());
        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Tema.BARRA);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0,0,2,0,C_BORDER),
            BorderFactory.createEmptyBorder(16,20,16,20)));
        JLabel t = new JLabel("Informe de clasificación del suelo");
        t.setFont(Tema.ui(Font.BOLD, 16)); t.setForeground(C_ACCENT);
        JLabel s = new JLabel("Seleccione los ensayos a incluir, complete los datos y elija la ruta de destino.");
        s.setFont(Tema.ui(Font.PLAIN, 11)); s.setForeground(C_MUTED);
        p.add(t, BorderLayout.NORTH); p.add(s, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildCenter() {
        JPanel c = new JPanel();
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        c.setBackground(C_BG2);
        c.setBorder(BorderFactory.createEmptyBorder(18,22,12,22));

        c.add(secLabel("ENSAYOS A INCLUIR"));
        c.add(Box.createVerticalStrut(10));

        JPanel grid = new JPanel(new GridLayout(3,2,12,8));
        grid.setBackground(C_BG2);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE,170));

        chkGran     = mkCB("\u2460 Granulometr\u00eda",          datos.granCalculada,    "Curva granulom\u00e9trica, D10/D30/D60, Cu, Cc");
        chkHidro    = mkCB("\u2461 Hidr\u00f3metro / Textural",  datos.hidroCalculada,   "Curva completa + Tri\u00e1ngulo USDA");
        chkAtter    = mkCB("\u2462 L\u00edmites de Atterberg",   datos.atterCalculada,   "Casagrande + Carta de Plasticidad");
        chkTernario = mkCB("\u2463 Diagramas Ternarios",         datos.quimicaCalculada, "\u00d3xidos \u00b7 Tierras Raras \u00b7 Geoqu\u00edmica");
        chkResumen  = mkCB("\u2464 Tabla Resumen + Normas",      true,                   "Clasificaci\u00f3n integrada y normas INV/ASTM");

        grid.add(chkGran); grid.add(chkHidro);
        grid.add(chkAtter); grid.add(chkTernario);
        grid.add(chkResumen); grid.add(new JLabel());

        c.add(grid);
        c.add(Box.createVerticalStrut(18));
        c.add(secLabel("DATOS DEL INFORME"));
        c.add(Box.createVerticalStrut(10));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(C_BG2);
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(5,0,5,10);

        g.gridx=0; g.gridy=0; g.weightx=0;
        form.add(lbl("Nombre del Proyecto:"),g);
        g.gridx=1; g.weightx=1; g.gridwidth=3;
        txtProyecto = fld("Caracterizaci\u00f3n Geot\u00e9cnica");
        form.add(txtProyecto,g);

        g.gridwidth=1; g.gridy=1;
        g.gridx=0; g.weightx=0; form.add(lbl("Muestra:"),g);
        g.gridx=1; g.weightx=0.35; txtMuestra=fld("M-01"); form.add(txtMuestra,g);
        g.gridx=2; g.weightx=0;    form.add(lbl("Operador:"),g);
        g.gridx=3; g.weightx=0.65; txtOperador=fld("Lab. GeoCer\u00e1mica"); form.add(txtOperador,g);

        g.gridy=2; g.gridx=0; g.weightx=0; g.gridwidth=1;
        form.add(lbl("Guardar en:"),g);
        g.gridx=1; g.weightx=1; g.gridwidth=2;
        txtRuta=fld(System.getProperty("user.home")+File.separator+"informe_sdim.pdf");
        form.add(txtRuta,g);
        g.gridx=3; g.weightx=0; g.gridwidth=1;
        JButton bb=sBtn("\u2026"); bb.addActionListener(e->elegirRuta()); form.add(bb,g);

        c.add(form);
        return c;
    }

    private JPanel buildFooter() {
        JPanel f = new JPanel(new BorderLayout(0,8));
        f.setBackground(Tema.BARRA);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(2,0,0,0,C_BORDER),
            BorderFactory.createEmptyBorder(12,20,14,20)));

        lblEstado = new JLabel(" ");
        lblEstado.setFont(Tema.ui(Font.PLAIN, 10));
        lblEstado.setForeground(C_MUTED);

        progressBar = new JProgressBar(0,100);
        progressBar.setVisible(false);
        progressBar.setForeground(C_GREEN);
        progressBar.setBackground(C_BG2);
        progressBar.setBorder(BorderFactory.createLineBorder(C_BORDER));
        progressBar.setPreferredSize(new Dimension(0,6));

        JPanel st = new JPanel(new BorderLayout(0,4));
        st.setBackground(Tema.BARRA);
        st.add(lblEstado,  BorderLayout.NORTH);
        st.add(progressBar,BorderLayout.SOUTH);

        JPanel bRow = new JPanel(new FlowLayout(FlowLayout.RIGHT,10,0));
        bRow.setBackground(Tema.BARRA);

        JButton btnCancelar = Tema.boton("Cancelar", Tema.Tipo.SECUNDARIO);
        btnCancelar.addActionListener(e->dispose());

        btnGenerar = Tema.boton("Generar PDF", Tema.Tipo.PRIMARIO);
        btnGenerar.addActionListener(e->generarPDF());

        bRow.add(btnCancelar); bRow.add(btnGenerar);
        f.add(st,   BorderLayout.CENTER);
        f.add(bRow, BorderLayout.SOUTH);
        return f;
    }

    private void generarPDF() {
        String proyecto=txtProyecto.getText().trim();
        String muestra =txtMuestra.getText().trim();
        String operador=txtOperador.getText().trim();
        String ruta    =txtRuta.getText().trim();

        if(proyecto.isEmpty()){error("Complete el nombre del proyecto.");return;}
        if(muestra.isEmpty()) {error("Complete el identificador de muestra.");return;}
        if(ruta.isEmpty())    {error("Seleccione la ruta de destino.");return;}
        if(!chkGran.isSelected()&&!chkAtter.isSelected()&&!chkHidro.isSelected()
          &&!chkTernario.isSelected()&&!chkResumen.isSelected()){
            error("Seleccione al menos un ensayo."); return;
        }

        File archivo = new File(ruta.endsWith(".pdf")?ruta:ruta+".pdf");
        if(!archivo.getParentFile().exists()){error("La carpeta de destino no existe.");return;}

        InformePDF.Seleccion sel = new InformePDF.Seleccion();
        sel.granulometria = chkGran.isSelected();
        sel.atterberg     = chkAtter.isSelected();
        sel.hidrometro    = chkHidro.isSelected();
        sel.ternario      = chkTernario.isSelected();
        sel.resumen       = chkResumen.isSelected();

        btnGenerar.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        setEstado("Generando informe, por favor espere\u2026",C_AMBER);

        final String fP=proyecto,fM=muestra,fO=operador; final File fA=archivo;
        new SwingWorker<Void,Void>(){
            protected Void doInBackground() throws Exception{
                InformePDF.generarInforme(datos,fA,fP,fM,fO,sel); return null;
            }
            protected void done(){
                progressBar.setIndeterminate(false); progressBar.setValue(100);
                try{
                    get();
                    setEstado("\u2713  PDF generado: "+fA.getName(),C_GREEN);
                    btnGenerar.setEnabled(true);
                    int r=JOptionPane.showConfirmDialog(ExportarPDFDialog.this,
                        "<html><b>Informe generado correctamente.</b><br>"+
                        fA.getAbsolutePath()+"<br><br>\u00bfAbrir la carpeta?</html>",
                        "Listo",JOptionPane.YES_NO_OPTION,JOptionPane.INFORMATION_MESSAGE);
                    if(r==JOptionPane.YES_OPTION)
                        try{Desktop.getDesktop().open(fA.getParentFile());}catch(Exception ig){}
                    dispose();
                }catch(Exception ex){
                    Throwable c=ex.getCause()!=null?ex.getCause():ex;
                    setEstado("\u2717  Error: "+c.getMessage(),C_RED);
                    btnGenerar.setEnabled(true); progressBar.setValue(0);
                    JOptionPane.showMessageDialog(ExportarPDFDialog.this,
                        "Error al generar el PDF:\n"+c.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void elegirRuta(){
        JFileChooser fc=new JFileChooser(new File(txtRuta.getText()).getParent());
        fc.setDialogTitle("Guardar informe PDF como\u2026");
        fc.setSelectedFile(new File(txtRuta.getText()));
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PDF (*.pdf)","pdf"));
        if(fc.showSaveDialog(this)==JFileChooser.APPROVE_OPTION){
            String p=fc.getSelectedFile().getAbsolutePath();
            txtRuta.setText(p.endsWith(".pdf")?p:p+".pdf");
        }
    }
    private void setEstado(String m,Color c){lblEstado.setText(m);lblEstado.setForeground(c);}
    private void error(String m){setEstado("\u2717  "+m,C_RED);JOptionPane.showMessageDialog(this,m,"Atenci\u00f3n",JOptionPane.WARNING_MESSAGE);}

    private JLabel secLabel(String t){
        JLabel l=new JLabel(t); l.setFont(Tema.ui(Font.BOLD, 10)); l.setForeground(C_ACCENT);
        l.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,0,1,0,C_BORDER),BorderFactory.createEmptyBorder(0,0,4,0)));
        l.setAlignmentX(0f); return l;
    }
    private JCheckBox mkCB(String nombre, boolean disp, String tip){
        JCheckBox cb=new JCheckBox(nombre); cb.setSelected(disp);
        cb.setFont(Tema.ui(Font.BOLD, 11));
        cb.setForeground(disp?C_TEXT:C_MUTED);
        cb.setBackground(C_BG3); cb.setOpaque(true);
        cb.setToolTipText((disp?"":"⚠ No calculado — ")+tip);
        cb.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(disp?C_BORDER:new Color(35,40,60)),
            BorderFactory.createEmptyBorder(7,10,7,10)));
        return cb;
    }
    private JLabel lbl(String t){JLabel l=new JLabel(t);l.setFont(Tema.ui(Font.BOLD, 10));l.setForeground(C_MUTED);return l;}
    private JTextField fld(String v){
        JTextField f=new JTextField(v); f.setFont(Tema.ui(Font.PLAIN, 11));
        f.setBackground(Tema.SUPERFICIE_2); f.setForeground(C_TEXT); f.setCaretColor(C_ACCENT);
        f.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(C_BORDER),BorderFactory.createEmptyBorder(5,8,5,8)));
        return f;
    }
    private JButton sBtn(String t){
        return Tema.boton(t, Tema.Tipo.SECUNDARIO);
    }
    private JButton bBtn(String t,Color bg,Color fg){
        return Tema.boton(t, bg);
    }
}
