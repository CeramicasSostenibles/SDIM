package geoceramiclab.ensayos;

import geoceramiclab.Tema;
import geoceramiclab.model.SueloData;
import geoceramiclab.graficas.CartaPlasticidadPanel;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.List;
import java.util.ArrayList;
import javax.swing.filechooser.FileNameExtensionFilter;

public class AtterbergPanel extends JPanel {

    private static final Color BG    = Tema.SUPERFICIE;
    private static final Color BG2   = Tema.SUPERFICIE_2;
    private static final Color BG3   = Tema.CONTROL;
    private static final Color BORD  = Tema.BORDE;
    private static final Color ACCENT= Tema.INFO;
    private static final Color GREEN = Tema.OK;
    private static final Color AMBER = Tema.AVISO;
    private static final Color RED   = Tema.ERROR;
    private static final Color TEXT  = Tema.TEXTO;
    private static final Color MUTED = Tema.TEXTO_2;
    private static final Color DIM   = Tema.TEXTO_3;

    private static final double FINOS_SIN_ATTERBERG = 5.0;

    private static final double FINOS_DOBLE_SIMBOLO = 12.0;

    private static final double FINOS_SUELO_FINO    = 50.0;

    private final SueloData datos;
    private final DefaultTableModel llModel;
    private final JTextField campoLL, campoLP, campoIP;
    private final JLabel lblResultado;
    private CartaPlasticidadPanel cartaPanel;

    private JTable tablaLL;
    private JTable tablaLP;
    private DefaultTableModel lpModel;

    private BotonAyuda btnAyuda;
    private JCheckBox chkForzar;
    private JButton btnCalc;
    private boolean aplicaAtterberg = true;

    private static final int CASO_SIN_GRAN      = 0;
    private static final int CASO_GRUESO_LIMPIO = 1;
    private static final int CASO_DOBLE_SIMBOLO = 2;
    private static final int CASO_GRUESO_FINOS  = 3;
    private static final int CASO_SUELO_FINO    = 4;
    private int casoActual = CASO_SIN_GRAN;

    public AtterbergPanel(SueloData datos) {
        this.datos = datos;
        setLayout(new BorderLayout(0,0));
        setBackground(BG);

        JPanel izq = new JPanel();
        izq.setLayout(new BoxLayout(izq,BoxLayout.Y_AXIS));
        izq.setBackground(BG);
        izq.setBorder(BorderFactory.createEmptyBorder(10,10,10,6));

        String[] colsLLInit={"N° golpes","Tara (g)","T+húmedo (g)","T+seco (g)","Humedad %"};
        llModel = new DefaultTableModel(colsLLInit,0){
            @Override public boolean isCellEditable(int r,int c){ return c<4 && aplicaAtterberg; }
        };
        campoLL  = field("",65);
        campoLP  = field("",60);
        campoIP  = field("",60);
        campoIP.setEditable(false); campoIP.setForeground(ACCENT);

        JPanel panelAviso = new JPanel(new WrapLayout(FlowLayout.LEFT,8,4));
        panelAviso.setBackground(BG2);
        panelAviso.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORD),
            BorderFactory.createEmptyBorder(2,8,2,8)));
        panelAviso.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelAviso.setMaximumSize(new Dimension(Integer.MAX_VALUE,60));

        JLabel lblAplic = new JLabel("Aplicabilidad del ensayo:");
        lblAplic.setFont(Tema.ui(Font.PLAIN, 11));
        lblAplic.setForeground(DIM);
        panelAviso.add(lblAplic);

        btnAyuda = new BotonAyuda();
        btnAyuda.addActionListener(e -> mostrarDialogoAplicabilidad());
        panelAviso.add(btnAyuda);

        chkForzar = new JCheckBox("Forzar ensayo");
        chkForzar.setBackground(BG2);
        chkForzar.setForeground(DIM);
        chkForzar.setFont(Tema.ui(Font.PLAIN, 10));
        chkForzar.setFocusPainted(false);
        chkForzar.setToolTipText("Habilita el ensayo aunque la granulometría indique suelo grueso limpio (<5% de finos)");
        chkForzar.setVisible(false);
        chkForzar.addActionListener(e -> setControlesHabilitados(aplicaAtterberg || chkForzar.isSelected()));
        panelAviso.add(chkForzar);

        izq.add(panelAviso);
        izq.add(Box.createVerticalStrut(6));

        izq.add(secTitle("Límite Líquido (ASTM D4318) — Casagrande"));

        JPanel barraLL = new JPanel(new WrapLayout(FlowLayout.LEFT,6,4));
        barraLL.setBackground(BG);
        barraLL.add(btnAccion("+ Punto",  e -> agregarFilaLL("","","","")));
        barraLL.add(btnAccion("Ejemplo",  e -> cargarEjemplo()));
        barraLL.add(btnAccion("CSV",      e -> cargarCSV()));

        JButton btnLimpiarLL = btnAccion("Limpiar tabla", e -> {
            llModel.setRowCount(0);
            campoLL.setText("");
        });
        btnLimpiarLL.setForeground(RED);
        barraLL.add(btnLimpiarLL);
        izq.add(barraLL);

        tablaLL = new JTable(llModel);
        estilizarTabla(tablaLL);
        tablaLL.getColumnModel().getColumn(4).setCellRenderer(celdaColor(ACCENT));
        JScrollPane scrollLL = new JScrollPane(tablaLL);
        scrollLL.setBackground(BG); scrollLL.getViewport().setBackground(BG);
        scrollLL.setBorder(BorderFactory.createLineBorder(BORD));
        scrollLL.setPreferredSize(new Dimension(400,140));
        scrollLL.setMaximumSize(new Dimension(Integer.MAX_VALUE,140));
        izq.add(scrollLL);
        izq.add(Box.createVerticalStrut(4));

        JPanel barraLimpCol = new JPanel(new WrapLayout(FlowLayout.LEFT,5,2));
        barraLimpCol.setBackground(BG);
        JLabel limpLbl = new JLabel("  Limpiar columna:");
        limpLbl.setFont(Tema.ui(Font.PLAIN, 10)); limpLbl.setForeground(DIM);
        barraLimpCol.add(limpLbl);
        String[] colNames={"N° golpes","Tara","T+húm","T+seco","Humedad"};
        for (int col=0; col<5; col++) {
            final int c = col;
            JButton b = btnColLimpiar(colNames[col], e -> limpiarColumnaLL(c));
            barraLimpCol.add(b);
        }
        barraLimpCol.setAlignmentX(Component.LEFT_ALIGNMENT);
        izq.add(barraLimpCol);
        izq.add(Box.createVerticalStrut(8));

        JPanel filaLL = new JPanel(new WrapLayout(FlowLayout.LEFT,10,4));
        filaLL.setBackground(BG);
        filaLL.add(label("LL calculado (%):"));
        campoLL.setToolTipText("Se calcula automáticamente a 25 golpes por regresión. O ingrese manual.");
        filaLL.add(campoLL);
        JButton btnLimpLL = btnColLimpiar("✕ LL", e -> campoLL.setText(""));
        filaLL.add(btnLimpLL);
        izq.add(filaLL);
        izq.add(Box.createVerticalStrut(12));

        izq.add(secTitle("Límite Plástico (ASTM D4318) — promedio de ensayos"));

        String[] colsLP={"Tara (g)","T+húmedo (g)","T+seco (g)","Humedad %"};
        lpModel = new DefaultTableModel(colsLP,0){
            @Override public boolean isCellEditable(int r,int c){ return c<3 && aplicaAtterberg; }
        };

        JPanel barraLP = new JPanel(new WrapLayout(FlowLayout.LEFT,6,4));
        barraLP.setBackground(BG);
        barraLP.add(btnAccion("+ Punto",  e -> agregarFilaLP("","","")));
        JButton btnLimpiarLP = btnAccion("Limpiar tabla", e -> lpModel.setRowCount(0));
        btnLimpiarLP.setForeground(RED);
        barraLP.add(btnLimpiarLP);
        barraLP.setAlignmentX(Component.LEFT_ALIGNMENT);
        izq.add(barraLP);

        tablaLP = new JTable(lpModel);
        estilizarTabla(tablaLP);
        tablaLP.getColumnModel().getColumn(3).setCellRenderer(celdaColor(ACCENT));
        JScrollPane scrollLP = new JScrollPane(tablaLP);
        scrollLP.setBackground(BG); scrollLP.getViewport().setBackground(BG);
        scrollLP.setBorder(BorderFactory.createLineBorder(BORD));
        scrollLP.setPreferredSize(new Dimension(400,110));
        scrollLP.setMaximumSize(new Dimension(Integer.MAX_VALUE,110));
        scrollLP.setAlignmentX(Component.LEFT_ALIGNMENT);
        izq.add(scrollLP);
        izq.add(Box.createVerticalStrut(6));

        JPanel filaRes = new JPanel(new WrapLayout(FlowLayout.LEFT,10,4));
        filaRes.setBackground(BG);
        filaRes.add(label("LP (%):"));
        filaRes.add(campoLP);
        filaRes.add(label("IP (%):"));
        filaRes.add(campoIP);

        JButton btnLimpLP = btnAccion("LP", e -> {
            lpModel.setRowCount(0);
            campoLP.setText(""); campoIP.setText("");
        });
        btnLimpLP.setForeground(RED);
        btnLimpLP.setToolTipText("Limpiar las masas y los resultados del límite plástico");
        filaRes.add(btnLimpLP);

        JButton btnLimpTodo = btnAccion("Todo", e -> limpiarTodo());
        btnLimpTodo.setToolTipText("Limpiar la tabla de LL y todos los resultados");
        btnLimpTodo.setForeground(RED);
        filaRes.add(btnLimpTodo);
        izq.add(filaRes);
        izq.add(Box.createVerticalStrut(14));

        btnCalc = Tema.boton("Calcular Atterberg", Tema.Tipo.PRIMARIO);
        btnCalc.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnCalc.addActionListener(e -> calcular());
        izq.add(btnCalc);
        izq.add(Box.createVerticalGlue());

        cartaPanel = new CartaPlasticidadPanel();
        cartaPanel.setPreferredSize(new Dimension(460,360));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,izq,cartaPanel);
        split.setDividerLocation(430);
        split.setBackground(BG);
        split.setBorder(BorderFactory.createEmptyBorder());
        add(split, BorderLayout.CENTER);

        lblResultado = new JLabel("  Ingrese datos de límite líquido y plástico");
        lblResultado.setForeground(MUTED);
        lblResultado.setFont(Tema.ui(Font.PLAIN, 12));
        lblResultado.setOpaque(true); lblResultado.setBackground(BG2);
        lblResultado.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1,0,0,0,BORD),
            BorderFactory.createEmptyBorder(6,12,6,12)));
        add(lblResultado, BorderLayout.SOUTH);

        cargarEjemplo();

        addComponentListener(new ComponentAdapter(){
            @Override public void componentShown(ComponentEvent e){ actualizarEstadoSegunGranulometria(); }
        });
        actualizarEstadoSegunGranulometria();
    }

    private Double obtenerPasa200() {
        if (!datos.granCalculada) return null;
        double p = datos.pctFinos;
        if (Double.isNaN(p) || p < 0) return null;
        return p;
    }

    private void actualizarEstadoSegunGranulometria() {
        Double pasa200 = obtenerPasa200();

        if (pasa200 == null) {
            casoActual = CASO_SIN_GRAN;
            aplicaAtterberg = true;
            chkForzar.setVisible(false);
            btnAyuda.setEstado(AMBER, "Granulometría sin calcular — clic para más información");
            setControlesHabilitados(true);
            return;
        }

        double p = pasa200;

        if (p < FINOS_SIN_ATTERBERG) {

            casoActual = CASO_GRUESO_LIMPIO;
            aplicaAtterberg = false;
            chkForzar.setVisible(true);
            btnAyuda.setEstado(RED, String.format(
                "NO aplica (finos = %.1f%%) — clic para ver por qué y cómo clasificar", p));
            setControlesHabilitados(chkForzar.isSelected());

            datos.LL = Double.NaN;
            datos.LP = Double.NaN;
            datos.IP = Double.NaN;
            datos.atterCalculada = false;
            cartaPanel.setPunto(Double.NaN, Double.NaN, "N.A.");
            lblResultado.setText("  Ensayo no aplicable: suelo grueso con menos de 5% de finos");
            lblResultado.setForeground(RED);

        } else if (p < FINOS_DOBLE_SIMBOLO) {

            casoActual = CASO_DOBLE_SIMBOLO;
            aplicaAtterberg = true;
            chkForzar.setVisible(false);
            btnAyuda.setEstado(GREEN, String.format(
                "Sí aplica (finos = %.1f%%, símbolo doble) — clic para ver la clasificación", p));
            setControlesHabilitados(true);

        } else if (p < FINOS_SUELO_FINO) {

            casoActual = CASO_GRUESO_FINOS;
            aplicaAtterberg = true;
            chkForzar.setVisible(false);
            btnAyuda.setEstado(GREEN, String.format(
                "Sí aplica (finos = %.1f%%) — clic para ver la clasificación", p));
            setControlesHabilitados(true);

        } else {

            casoActual = CASO_SUELO_FINO;
            aplicaAtterberg = true;
            chkForzar.setVisible(false);
            btnAyuda.setEstado(GREEN, String.format(
                "Sí aplica — suelo fino (finos = %.1f%%) — clic para ver la clasificación", p));
            setControlesHabilitados(true);
        }
    }

    private boolean esGrava() {
        return datos.granCalculada && datos.pctGrava > datos.pctArena;
    }

    private void mostrarDialogoAplicabilidad() {
        Double p = obtenerPasa200();
        String g = esGrava() ? "G" : "S";
        String nombreFraccion = esGrava() ? "grava" : "arena";
        String cuerpo, titulo;
        Color  color;

        switch (casoActual) {

            case CASO_GRUESO_LIMPIO:
                titulo = "Los límites de Atterberg NO aplican";
                color  = RED;
                cuerpo =
                    fila("% que pasa el tamiz N.° 200", String.format("%.1f %%", p))
                  + fila("Criterio SUCS", "finos &lt; 5 %  →  suelo grueso limpio")
                  + sep()
                  + parr("Los límites de Atterberg miden la plasticidad de la <b>fracción fina</b>. "
                       + "Con menos de 5 % de finos no hay material suficiente para ejecutarlos, "
                       + "y la norma no los exige.")
                  + h("Clasifique solo con la curva granulométrica:")
                  + tabla(new String[][]{
                        {"<b>"+g+"W</b>", "Bien gradada", esGrava()
                            ? "Cu &ge; 4  <b>y</b>  1 &le; Cc &le; 3"
                            : "Cu &ge; 6  <b>y</b>  1 &le; Cc &le; 3"},
                        {"<b>"+g+"P</b>", "Mal gradada", "no cumple lo anterior"}
                    })
                  + nota("La fracción gruesa de esta muestra es <b>" + nombreFraccion + "</b> "
                       + String.format("(grava %.1f %% vs. arena %.1f %%), por eso el símbolo empieza en <b>%s</b>.",
                            datos.pctGrava, datos.pctArena, g))
                  + nota("Si aun así necesita registrar el ensayo (muestra de control, verificación), "
                       + "marque <b>Forzar ensayo</b> y el panel se habilita.");
                break;

            case CASO_DOBLE_SIMBOLO:
                titulo = "Sí aplican — suelo grueso con símbolo doble";
                color  = GREEN;
                cuerpo =
                    fila("% que pasa el tamiz N.° 200", String.format("%.1f %%", p))
                  + fila("Criterio SUCS", "5 % &le; finos &lt; 12 %  →  <b>símbolo doble</b>")
                  + sep()
                  + parr("Hay finos suficientes para influir en el comportamiento, pero no para dominarlo. "
                       + "El SUCS pide un símbolo compuesto: la primera parte viene de la gradación "
                       + "(Cu y Cc) y la segunda de la plasticidad, que es lo que usted va a medir aquí.")
                  + h("Resultados posibles para esta muestra:")
                  + tabla(new String[][]{
                        {"<b>"+g+"W-"+g+"M</b>", "bien gradada, finos limosos",   "IP &lt; 4  o bajo la línea A"},
                        {"<b>"+g+"W-"+g+"C</b>", "bien gradada, finos arcillosos","IP &gt; 7  y sobre la línea A"},
                        {"<b>"+g+"P-"+g+"M</b>", "mal gradada, finos limosos",    "IP &lt; 4  o bajo la línea A"},
                        {"<b>"+g+"P-"+g+"C</b>", "mal gradada, finos arcillosos", "IP &gt; 7  y sobre la línea A"}
                    })
                  + nota("Línea A:  <b>IP = 0.73 · (LL &minus; 20)</b>")
                  + nota("Ensaye sobre la fracción que <b>pasa el tamiz N.° 40</b>, no sobre la muestra total.");
                break;

            case CASO_GRUESO_FINOS:
                titulo = "Sí aplican — suelo grueso con finos";
                color  = GREEN;
                cuerpo =
                    fila("% que pasa el tamiz N.° 200", String.format("%.1f %%", p))
                  + fila("Criterio SUCS", "12 % &le; finos &lt; 50 %  →  grueso con finos")
                  + sep()
                  + parr("El suelo sigue siendo grueso (menos del 50 % pasa el N.° 200), pero los finos "
                       + "ya gobiernan su comportamiento. Los límites definen la <b>segunda letra</b> del símbolo.")
                  + h("Resultados posibles para esta muestra:")
                  + tabla(new String[][]{
                        {"<b>"+g+"M</b>",     esGrava()?"Grava limosa":"Arena limosa",
                         "IP &lt; 4  o bajo la línea A"},
                        {"<b>"+g+"C</b>",     esGrava()?"Grava arcillosa":"Arena arcillosa",
                         "IP &gt; 7  y sobre la línea A"},
                        {"<b>"+g+"C-"+g+"M</b>", "caso de frontera",
                         "4 &le; IP &le; 7  y sobre la línea A"}
                    })
                  + nota("Línea A:  <b>IP = 0.73 · (LL &minus; 20)</b>")
                  + nota("Ensaye sobre la fracción que <b>pasa el tamiz N.° 40</b>, no sobre la muestra total.");
                break;

            case CASO_SUELO_FINO:
                titulo = "Sí aplican — suelo fino";
                color  = GREEN;
                cuerpo =
                    fila("% que pasa el tamiz N.° 200", String.format("%.1f %%", p))
                  + fila("Criterio SUCS", "finos &ge; 50 %  →  suelo fino")
                  + sep()
                  + parr("Este es el caso principal de los límites de Atterberg: la clasificación sale "
                       + "<b>completa</b> de la carta de plasticidad, ubicando el punto (LL, IP).")
                  + h("Resultados posibles:")
                  + tabla(new String[][]{
                        {"<b>CL</b>",    "Arcilla de baja plasticidad", "LL &lt; 50  ·  sobre línea A  ·  IP &gt; 7"},
                        {"<b>ML</b>",    "Limo de baja plasticidad",    "LL &lt; 50  ·  bajo línea A  o  IP &lt; 4"},
                        {"<b>CL-ML</b>", "Frontera limo-arcilla",       "LL &lt; 50  ·  sobre línea A  ·  4 &le; IP &le; 7"},
                        {"<b>CH</b>",    "Arcilla de alta plasticidad", "LL &ge; 50  ·  sobre línea A"},
                        {"<b>MH</b>",    "Limo de alta plasticidad",    "LL &ge; 50  ·  bajo línea A"},
                        {"<b>OL / OH</b>","Suelo orgánico",             "LL(seco al horno) / LL(sin secar) &lt; 0.75"}
                    })
                  + nota("Línea A:  <b>IP = 0.73 · (LL &minus; 20)</b>"
                       + " &nbsp;&nbsp;·&nbsp;&nbsp; Línea U:  <b>IP = 0.9 · (LL &minus; 8)</b>")
                  + nota("Ningún punto real debe quedar por <b>encima de la línea U</b>. Si eso pasa, "
                       + "revise el ensayo antes de reportar.")
                  + nota("Ensaye sobre la fracción que <b>pasa el tamiz N.° 40</b>.");
                break;

            default:
                titulo = "Falta la granulometría";
                color  = AMBER;
                cuerpo =
                    parr("Todavía no se ha calculado la granulometría, así que no se puede verificar "
                       + "si este suelo requiere límites de Atterberg. El panel queda habilitado, "
                       + "pero el resultado podría no ser aplicable.")
                  + h("Vaya a la pestaña Granulometría, calcule, y vuelva aquí. El criterio será:")
                  + tabla(new String[][]{
                        {"<b>finos &lt; 5 %</b>",        "NO aplican",  "grueso limpio: solo Cu y Cc"},
                        {"<b>5 – 12 %</b>",              "Sí aplican",  "símbolo doble (SW-SC, GP-GM…)"},
                        {"<b>12 – 50 %</b>",             "Sí aplican",  "SM/SC · GM/GC"},
                        {"<b>&ge; 50 %</b>",             "Sí aplican",  "suelo fino: carta de plasticidad"}
                    })
                  + nota("«Finos» = porcentaje que pasa el tamiz N.° 200 (0.075 mm).");
                break;
        }

        mostrarPanelHtml(titulo, cuerpo, color);
    }

    private static String parr(String s){
        return "<p style='margin:6px 0 8px 0;'>"+s+"</p>";
    }
    private static String h(String s){
        return "<p style='margin:12px 0 4px 0;color:#e0a33a;'><b>"+s+"</b></p>";
    }
    private static String sep(){
        return "<hr style='height:1px;border:0;background:#2a3a4a;'>";
    }
    private static String fila(String k, String v){
        return "<table cellpadding='2' cellspacing='0'><tr>"
             + "<td style='color:#a9b4bf;'>"+k+":&nbsp;&nbsp;</td>"
             + "<td style='color:#e6ebf0;'><b>"+v+"</b></td></tr></table>";
    }
    private static String nota(String s){
        return "<p style='margin:6px 0 0 0;color:#a9b4bf;font-size:11px;'>› "+s+"</p>";
    }
    private static String tabla(String[][] filas){
        StringBuilder sb = new StringBuilder(
            "<table cellpadding='5' cellspacing='0' width='100%' style='border:1px solid #2a3a4a;'>");
        for (String[] f : filas) {
            sb.append("<tr>")
              .append("<td width='13%' style='color:#7fa3c2;'>").append(f[0]).append("</td>")
              .append("<td width='31%' style='color:#e6ebf0;'>").append(f[1]).append("</td>")
              .append("<td style='color:#a9b4bf;'>").append(f[2]).append("</td>")
              .append("</tr>");
        }
        return sb.append("</table>").toString();
    }

    private void mostrarPanelHtml(String titulo, String cuerpo, Color color) {
        String hex = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());

        String html =
            "<html><body style='background:#16212c;color:#e6ebf0;"
          + "font-family:sans-serif;font-size:12px;margin:10px;'>"
          + "<p style='margin:0 0 10px 0;font-size:15px;color:"+hex+";'><b>"+titulo+"</b></p>"
          + cuerpo
          + "</body></html>";

        JEditorPane ep = new JEditorPane("text/html", html);
        ep.setEditable(false);
        ep.setBackground(Tema.SUPERFICIE_2);
        ep.setBorder(BorderFactory.createEmptyBorder());

        JScrollPane sp = new JScrollPane(ep);
        sp.setPreferredSize(new Dimension(660, 440));
        sp.setBorder(BorderFactory.createLineBorder(BORD));
        sp.getViewport().setBackground(Tema.SUPERFICIE_2);
        sp.getVerticalScrollBar().setUnitIncrement(16);

        SwingUtilities.invokeLater(() -> sp.getVerticalScrollBar().setValue(0));

        JOptionPane.showMessageDialog(this, sp,
            "Aplicabilidad de los límites de Atterberg", JOptionPane.PLAIN_MESSAGE);
    }

    private static class BotonAyuda extends JButton {
        private Color color = MUTED;

        BotonAyuda() {
            super("?");
            setPreferredSize(new Dimension(24,24));
            setFont(Tema.ui(Font.BOLD, 13));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        void setEstado(Color c, String tooltip) {
            this.color = c;
            setToolTipText(tooltip);
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
            int d = Math.min(getWidth(), getHeight()) - 2;
            int x = (getWidth()-d)/2, y = (getHeight()-d)/2;

            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(),
                                  getModel().isRollover() ? 70 : 40));
            g2.fillOval(x, y, d, d);

            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.6f));
            g2.drawOval(x, y, d, d);

            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            String s = "?";
            g2.drawString(s,
                (getWidth() - fm.stringWidth(s)) / 2f,
                (getHeight() - fm.getHeight()) / 2f + fm.getAscent());
            g2.dispose();
        }
    }

    private void setControlesHabilitados(boolean on) {
        tablaLL.setEnabled(on);
        tablaLL.setForeground(on ? TEXT : DIM);
        tablaLL.setBackground(on ? BG : BG2);

        tablaLP.setEnabled(on);
        tablaLP.setForeground(on ? TEXT : DIM);
        tablaLP.setBackground(on ? BG : BG2);

        for (JTextField f : new JTextField[]{campoLL,campoLP,campoIP}) {
            f.setEnabled(on);
            f.setDisabledTextColor(DIM);
            f.setBackground(on ? BG3 : BG2);
        }
        campoIP.setEditable(false);

        btnCalc.setEnabled(on);
        btnCalc.setBackground(on ? AMBER : BG3);
        btnCalc.setForeground(on ? Tema.BARRA : DIM);
        btnCalc.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(on ? AMBER : BORD),
            BorderFactory.createEmptyBorder(8,16,8,16)));
        btnCalc.setCursor(Cursor.getPredefinedCursor(
            on ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));

        tablaLL.repaint();
    }

    public void refrescarAplicabilidad() {
        actualizarEstadoSegunGranulometria();
    }

    private void limpiarColumnaLL(int col) {
        for (int r=0; r<llModel.getRowCount(); r++) {
            llModel.setValueAt("", r, col);
        }
    }

    private void limpiarTodo() {
        llModel.setRowCount(0);
        lpModel.setRowCount(0);
        campoLL.setText(""); campoLP.setText(""); campoIP.setText("");
        cartaPanel.setPunto(Double.NaN, Double.NaN, "—");
        lblResultado.setText("  Datos limpiados");
        lblResultado.setForeground(MUTED);
    }

    private void calcular() {

        if (!aplicaAtterberg && !chkForzar.isSelected()) {
            mostrarDialogoAplicabilidad();
            return;
        }

        int n = llModel.getRowCount();
        List<double[]> pts = new ArrayList<>();
        for (int i=0; i<n; i++) {
            try {
                double g   = Double.parseDouble(llModel.getValueAt(i,0).toString().trim());
                double tar = Double.parseDouble(llModel.getValueAt(i,1).toString().trim().replace(",","."));
                double hum = Double.parseDouble(llModel.getValueAt(i,2).toString().trim().replace(",","."));
                double sec = Double.parseDouble(llModel.getValueAt(i,3).toString().trim().replace(",","."));
                double w   = ((hum-sec)/(sec-tar))*100.0;
                llModel.setValueAt(String.format("%.1f%%",w), i, 4);
                pts.add(new double[]{g, w});
            } catch (Exception ex) {}
        }

        double LL;
        String llTxt = campoLL.getText().trim();
        if (!llTxt.isEmpty()) {
            try { LL=Double.parseDouble(llTxt); }
            catch (Exception e){ showError("LL inválido"); return; }
        } else if (pts.size()>=2) {
            int np=pts.size(); double sx=0,sy=0,sxy=0,sx2=0;
            for (double[] p:pts){ double lx=Math.log10(p[0]); sx+=lx;sy+=p[1];sxy+=lx*p[1];sx2+=lx*lx; }
            double m=(np*sxy-sx*sy)/(np*sx2-sx*sx);
            double b=(sy-m*sx)/np;
            LL=m*Math.log10(25)+b;
            campoLL.setText(String.format("%.1f",LL));
        } else { showError("Ingrese ≥2 puntos de LL o ingrese LL manual"); return; }

        double LP;
        String lpTxt = campoLP.getText().trim();
        double sumaLP = 0; int nLP = 0;
        for (int i=0; i<lpModel.getRowCount(); i++) {
            try {
                double tar=Double.parseDouble(lpModel.getValueAt(i,0).toString().trim().replace(",","."));
                double hum=Double.parseDouble(lpModel.getValueAt(i,1).toString().trim().replace(",","."));
                double sec=Double.parseDouble(lpModel.getValueAt(i,2).toString().trim().replace(",","."));
                double w=((hum-sec)/(sec-tar))*100.0;
                lpModel.setValueAt(String.format("%.1f%%",w), i, 3);
                sumaLP += w; nLP++;
            } catch (Exception ex) {}
        }
        if (!lpTxt.isEmpty()) {
            try { LP=Double.parseDouble(lpTxt); }
            catch (Exception e){ showError("LP inválido"); return; }
        } else if (nLP > 0) {
            // LP = promedio de los ensayos (ASTM D4318)
            LP = sumaLP / nLP;
            campoLP.setText(String.format("%.1f",LP));
        } else { showError("Ingrese ≥1 ensayo de LP o valor LP manual"); return; }

        double IP = LL-LP;
        campoIP.setText(String.format("%.1f",IP));

        String uscs = SueloData.clasificarUSCS(LL,IP);

        datos.tablaLL = new ArrayList<>();
        for(int i=0;i<llModel.getRowCount();i++){
            datos.tablaLL.add(new String[]{
                String.valueOf(llModel.getValueAt(i,0)),
                String.valueOf(llModel.getValueAt(i,1)),
                String.valueOf(llModel.getValueAt(i,2)),
                String.valueOf(llModel.getValueAt(i,3)),
                String.valueOf(llModel.getValueAt(i,4))
            });
        }

        datos.tablaLP = new ArrayList<>();
        for(int i=0;i<lpModel.getRowCount();i++){
            datos.tablaLP.add(new String[]{
                String.valueOf(lpModel.getValueAt(i,0)),
                String.valueOf(lpModel.getValueAt(i,1)),
                String.valueOf(lpModel.getValueAt(i,2)),
                String.valueOf(lpModel.getValueAt(i,3))
            });
        }
        if (lpModel.getRowCount() > 0) {
            try { datos.lpTara = Double.parseDouble(lpModel.getValueAt(0,0).toString().trim().replace(",",".")); } catch(Exception ex){}
            try { datos.lpHum  = Double.parseDouble(lpModel.getValueAt(0,1).toString().trim().replace(",",".")); } catch(Exception ex){}
            try { datos.lpSec  = Double.parseDouble(lpModel.getValueAt(0,2).toString().trim().replace(",",".")); } catch(Exception ex){}
        }

        datos.LL=LL; datos.LP=LP; datos.IP=IP;
        datos.clasificacionUSCS=uscs;
        datos.atterCalculada=true;
        datos.notificar();

        cartaPanel.setPunto(LL,IP,uscs);

        lblResultado.setText(String.format(
            "  LL=%.1f%%  |  LP=%.1f%%  |  IP=%.1f%%  →  %s", LL,LP,IP,uscs));
        lblResultado.setForeground(ACCENT);
    }

    private void cargarEjemplo() {
        llModel.setRowCount(0);
        double[][] ej={{15,10.2,30.5,24.8},{20,10.5,29.8,24.2},{25,10.1,31.2,25.3},{30,10.3,30.1,24.4},{35,10.4,29.5,24.0}};
        for (double[] r:ej) agregarFilaLL(fmt(r[0]),fmt(r[1]),fmt(r[2]),fmt(r[3]));
        lpModel.setRowCount(0);
        double[][] ejLP={{10.2,22.5,20.1},{10.4,23.1,20.6}};
        for (double[] r:ejLP) agregarFilaLP(fmt(r[0]),fmt(r[1]),fmt(r[2]));
        campoLL.setText(""); campoLP.setText(""); campoIP.setText("");
    }

    private void cargarCSV() {
        JFileChooser fc=new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("CSV","csv","txt"));
        if (fc.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION) return;
        try (BufferedReader br=new BufferedReader(new FileReader(fc.getSelectedFile()))) {
            llModel.setRowCount(0);
            String line;
            while ((line=br.readLine())!=null){
                String[] cols=line.split("[,;\\t]");
                if (cols.length>=4 && cols[0].trim().matches(".*\\d.*"))
                    agregarFilaLL(cols[0].trim(),cols[1].trim(),cols[2].trim(),cols[3].trim());
            }
        } catch (Exception ex){ showError("Error CSV: "+ex.getMessage()); }
    }

    private void agregarFilaLL(String g,String t,String h,String s){
        llModel.addRow(new Object[]{g,t,h,s,""});
    }

    private void agregarFilaLP(String t,String h,String s){
        lpModel.addRow(new Object[]{t,h,s,""});
    }

    private static JLabel secTitle(String t){
        JLabel l=new JLabel(t); l.setForeground(DIM);
        l.setFont(Tema.ui(Font.BOLD, 11));
        l.setBorder(BorderFactory.createEmptyBorder(6,0,4,0));
        l.setAlignmentX(Component.LEFT_ALIGNMENT); return l;
    }

    private static void estilizarTabla(JTable t){
        t.setBackground(BG); t.setForeground(TEXT);
        t.setFont(Tema.ui(Font.PLAIN, 12)); t.setRowHeight(24);
        t.setGridColor(BORD); t.setSelectionBackground(BG3);
        t.getTableHeader().setBackground(BG2);
        t.getTableHeader().setForeground(DIM);
        t.getTableHeader().setFont(Tema.ui(Font.BOLD, 11));
    }

    private static TableCellRenderer celdaColor(Color c){
        return new DefaultTableCellRenderer(){{
            setForeground(c); setBackground(BG); setFont(Tema.fuente(Font.PLAIN, 12));
        }};
    }

    private static JLabel label(String t){
        JLabel l=new JLabel(t); l.setForeground(MUTED);
        l.setFont(Tema.ui(Font.PLAIN, 12)); return l;
    }

    private static JTextField field(String v,int w){
        JTextField f=new JTextField(v,w/8);
        f.setBackground(BG3); f.setForeground(TEXT);
        f.setFont(Tema.ui(Font.PLAIN, 12)); f.setCaretColor(ACCENT);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORD),BorderFactory.createEmptyBorder(3,6,3,6)));
        return f;
    }

    private static JButton btnAccion(String t, ActionListener al){
        JButton b = Tema.boton(t, Tema.Tipo.SECUNDARIO);
        b.addActionListener(al);
        return b;
    }

    private static JButton btnColLimpiar(String col, ActionListener al){
        JButton b = Tema.boton(col, Tema.Tipo.PELIGRO);
        b.setToolTipText("Limpiar columna " + col);
        b.addActionListener(al);
        return b;
    }

    private static String fmt(double v){ return v==(long)v?String.valueOf((long)v):String.valueOf(v); }

    private void showError(String msg){ JOptionPane.showMessageDialog(this,msg,"Aviso",JOptionPane.WARNING_MESSAGE); }

    private static class WrapLayout extends FlowLayout {
        WrapLayout(int align, int hgap, int vgap){ super(align, hgap, vgap); }

        @Override public Dimension preferredLayoutSize(Container t){ return layoutSize(t, true); }
        @Override public Dimension minimumLayoutSize(Container t){
            Dimension d = layoutSize(t, false); d.width -= (getHgap()+1); return d;
        }

        private Dimension layoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int targetWidth = target.getSize().width;
                if (targetWidth == 0) targetWidth = Integer.MAX_VALUE;

                Insets insets = target.getInsets();
                int horizInsets = insets.left + insets.right + getHgap()*2;
                int maxWidth = targetWidth - horizInsets;

                Dimension dim = new Dimension(0,0);
                int rowWidth = 0, rowHeight = 0;

                for (int i = 0; i < target.getComponentCount(); i++) {
                    Component m = target.getComponent(i);
                    if (!m.isVisible()) continue;
                    Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                    if (rowWidth + d.width > maxWidth && rowWidth > 0) {
                        dim.width  = Math.max(dim.width, rowWidth);
                        dim.height += rowHeight + getVgap();
                        rowWidth = 0; rowHeight = 0;
                    }
                    if (rowWidth != 0) rowWidth += getHgap();
                    rowWidth += d.width;
                    rowHeight = Math.max(rowHeight, d.height);
                }
                dim.width  = Math.max(dim.width, rowWidth);
                dim.height += rowHeight;

                dim.width  += horizInsets;
                dim.height += insets.top + insets.bottom + getVgap()*2;
                return dim;
            }
        }
    }

}