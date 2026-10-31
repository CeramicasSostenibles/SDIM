package geoceramiclab.ensayos;

import geoceramiclab.Tema;
import geoceramiclab.model.SueloData;
import geoceramiclab.model.ClasificadorSUCS;
import geoceramiclab.ExportarPDFDialog;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

public class EnsayosPanel extends JPanel {

    private static final Color C_BG     = Tema.FONDO;
    private static final Color C_BG2    = Tema.SUPERFICIE;
    private static final Color C_BORDER = Tema.BORDE;
    private static final Color C_ACCENT = Tema.INFO;
    private static final Color C_GREEN  = Tema.OK;
    private static final Color C_AMBER  = Tema.AVISO;
    private static final Color C_RED    = Tema.ERROR;
    private static final Color C_TEXT   = Tema.TEXTO;
    private static final Color C_MUTED  = Tema.TEXTO_2;

    private final JLabel lblResumen;
    private final JLabel lblClasif;

    public EnsayosPanel(SueloData datos, Runnable irAModelo) {
        setLayout(new BorderLayout(0, 0));
        setBackground(C_BG);

        JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
        tabs.setBackground(C_BG2);
        tabs.setForeground(C_MUTED);
        tabs.setFont(Tema.ui(Font.BOLD, 13));
        tabs.setBorder(BorderFactory.createEmptyBorder());

        tabs.addTab("① Granulometría",  new GranulometriaPanel(datos));
        tabs.addTab("② Hidrómetro",     new HidrometroPanel(datos));
        tabs.addTab("③ Atterberg",      new AtterbergPanel(datos));

        add(tabs, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(16, 0));
        footer.setBackground(Tema.BARRA);
        footer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(2, 0, 0, 0, C_BORDER),
            BorderFactory.createEmptyBorder(8, 20, 8, 20)
        ));

        JPanel resumenPanel = new JPanel(new GridLayout(3, 1, 0, 2));
        resumenPanel.setBackground(Tema.BARRA);

        lblResumen = new JLabel("Complete los ensayos para obtener la clasificación del suelo");
        lblResumen.setFont(Tema.ui(Font.PLAIN, 12));
        lblResumen.setForeground(C_MUTED);

        lblClasif = new JLabel(" ");
        lblClasif.setFont(Tema.ui(Font.BOLD, 12));
        lblClasif.setForeground(C_GREEN);

        JLabel lblHint = new JLabel("① Calcule Granulometría  ·  ② Calcule Hidrómetro  ·  ③ Calcule Atterberg  →  Clasificar");
        lblHint.setFont(Tema.ui(Font.PLAIN, 11));
        lblHint.setForeground(Tema.TEXTO_3);

        JButton btnClasificar = Tema.boton("Clasificar suelo  →", Tema.Tipo.PRIMARIO);
        btnClasificar.setToolTipText("Clasifica el suelo (SUCS) y pasa a la pieza 3D.");
        btnClasificar.addActionListener(e -> {
            if (!datos.granCalculada && !datos.hidroCalculada && !datos.atterCalculada) {
                JOptionPane.showMessageDialog(this,
                    "Calcule al menos un ensayo antes de clasificar.",
                    "Sin datos", JOptionPane.WARNING_MESSAGE);
                return;
            }

            ClasificadorSUCS.Resultado r = ClasificadorSUCS.clasificar(datos);

            datos.clasificacionUSCS = r.simbolo;

            String nombre = r.nombreParaModelo();
            if (nombre != null) datos.nombreFinal = nombre;

            datos.notificar();

            boolean seguir = DialogoClasificacion.mostrar(this, datos, r);
            if (seguir) irAModelo.run();
        });

        JPanel botonesPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        botonesPanel.setBackground(Tema.BARRA);
        botonesPanel.add(btnClasificar);

        footer.add(botonesPanel,  BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);

    }

    private void actualizarResumen(SueloData datos) {
        StringBuilder sb = new StringBuilder("<html><font color='#e6ebf0'>");
        boolean algo = false;

        if (datos.granCalculada) {
            sb.append(String.format("Grava %.0f%%  Arena %.0f%%  Finos %.0f%%",
                datos.pctGrava, datos.pctArena, datos.pctFinos));
            if (datos.Cu != null)
                sb.append(String.format("  &nbsp;&nbsp; Cu=%.2f  Cc=%.2f", datos.Cu, datos.Cc));
            algo = true;
        }
        if (datos.hidroCalculada) {
            if (algo) sb.append("  &nbsp;|&nbsp;  ");
            sb.append(String.format("<font color='#4cb782'>Arcilla %.0f%%  Limo %.0f%%  Arena %.0f%%  →  %s</font>",
                datos.pctArcilla, datos.pctLimo, datos.pctArenaFina, datos.claseTextural));
            algo = true;
        }
        if (datos.atterCalculada) {
            if (algo) sb.append("  &nbsp;|&nbsp;  ");
            sb.append(String.format("<font color='#e0a33a'>LL=%.0f  LP=%.0f  IP=%.0f</font>",
                datos.LL, datos.LP, datos.IP));
            algo = true;
        }
        if (!algo) sb.append("Sin datos calculados aún");
        sb.append("</font></html>");
        lblResumen.setText(sb.toString());

        actualizarClasificacion(datos);
    }

    private void actualizarClasificacion(SueloData datos) {
        if (!datos.granCalculada && !datos.hidroCalculada && !datos.atterCalculada) {
            lblClasif.setText(" ");
            return;
        }

        ClasificadorSUCS.Resultado r = ClasificadorSUCS.clasificar(datos);

        String hex;
        String marca;
        switch (r.nivel) {
            case COMPLETA: hex = "#4cb782"; marca = "●"; break;
            case PARCIAL:  hex = "#e0a33a"; marca = "◐"; break;
            default:       hex = "#e05a5a"; marca = "○"; break;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<html><font color='").append(hex).append("'>")
          .append(marca).append("  SUCS: <b>").append(r.simbolo).append("</b>")
          .append("  —  ").append(r.nombre);

        if (r.nivel != ClasificadorSUCS.Nivel.COMPLETA && !r.faltantes.isEmpty()) {
            sb.append("  <font color='#7d8a97'>(falta: ").append(r.faltantes.get(0)).append(")</font>");
        }
        if (!r.avisos.isEmpty()) {
            sb.append("  <font color='#e05a5a'>⚠ ").append(r.avisos.size())
              .append(r.avisos.size() == 1 ? " advertencia" : " advertencias").append("</font>");
        }
        sb.append("</font></html>");

        lblClasif.setText(sb.toString());
    }
}