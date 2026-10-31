package geoceramiclab.ensayos;

import geoceramiclab.Tema;
import geoceramiclab.model.SueloData;
import geoceramiclab.model.ClasificadorSUCS;
import geoceramiclab.model.ClasificadorSUCS.Nivel;
import geoceramiclab.model.ClasificadorSUCS.Resultado;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class DialogoClasificacion extends JDialog {

    private static final Color BG     = Tema.FONDO;
    private static final Color BG2    = Tema.SUPERFICIE;
    private static final Color BG3    = Tema.SUPERFICIE_2;
    private static final Color BORD   = Tema.BORDE;
    private static final Color GREEN  = Tema.OK;
    private static final Color AMBER  = Tema.AVISO;
    private static final Color RED    = Tema.ERROR;
    private static final Color TEXT   = Tema.TEXTO;
    private static final Color MUTED  = Tema.TEXTO_2;
    private static final Color DIM    = Tema.TEXTO_3;

    private boolean continuar = false;

    public static boolean mostrar(Component padre, SueloData datos, Resultado r) {
        Window w = SwingUtilities.getWindowAncestor(padre);
        DialogoClasificacion d = new DialogoClasificacion(
                w instanceof Frame ? (Frame) w : null, datos, r);
        d.setVisible(true);
        return d.continuar;
    }

    private DialogoClasificacion(Frame padre, SueloData datos, Resultado r) {
        super(padre, "Clasificación del suelo — SUCS (ASTM D2487)", true);

        Color c = colorDe(r.nivel);

        JPanel raiz = new JPanel(new BorderLayout(0,0));
        raiz.setBackground(BG);

        JPanel head = new JPanel(new BorderLayout(16,0));
        head.setBackground(BG2);
        head.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0,0,1,0,BORD),
            BorderFactory.createEmptyBorder(16,20,16,20)));

        JLabel lblSim = new JLabel(r.simbolo);
        lblSim.setFont(Tema.fuente(Font.BOLD, r.simbolo.length() > 8 ? 22 : 40));
        lblSim.setForeground(c);
        head.add(lblSim, BorderLayout.WEST);

        JPanel textos = new JPanel();
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        textos.setBackground(BG2);

        JLabel lblNom = new JLabel(r.nombre);
        lblNom.setFont(Tema.ui(Font.BOLD, 16));
        lblNom.setForeground(TEXT);
        lblNom.setAlignmentX(Component.LEFT_ALIGNMENT);
        textos.add(lblNom);

        JLabel lblNivel = new JLabel(etiquetaNivel(r.nivel));
        lblNivel.setFont(Tema.ui(Font.PLAIN, 11));
        lblNivel.setForeground(c);
        lblNivel.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblNivel.setBorder(BorderFactory.createEmptyBorder(4,0,0,0));
        textos.add(lblNivel);

        JLabel lblEnsayos = new JLabel(ensayosUsados(datos));
        lblEnsayos.setFont(Tema.ui(Font.PLAIN, 11));
        lblEnsayos.setForeground(DIM);
        lblEnsayos.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblEnsayos.setBorder(BorderFactory.createEmptyBorder(2,0,0,0));
        textos.add(lblEnsayos);

        head.add(textos, BorderLayout.CENTER);
        raiz.add(head, BorderLayout.NORTH);

        JEditorPane ep = new JEditorPane("text/html", cuerpoHtml(r, datos));
        ep.setEditable(false);
        ep.setBackground(BG);
        ep.setBorder(BorderFactory.createEmptyBorder());
        ep.setCaretPosition(0);

        JScrollPane sp = new JScrollPane(ep);
        sp.setPreferredSize(new Dimension(720, 400));
        sp.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        sp.getViewport().setBackground(BG);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        raiz.add(sp, BorderLayout.CENTER);

        JPanel pie = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        pie.setBackground(Tema.BARRA);
        pie.setBorder(BorderFactory.createMatteBorder(1,0,0,0,BORD));

        JButton btnCerrar = boton("Volver a los ensayos", BG3, MUTED);
        btnCerrar.addActionListener(e -> { continuar = false; dispose(); });
        pie.add(btnCerrar);

        JButton btnSeguir = boton("  Continuar al Modelo 3D  →  ", GREEN, new Color(10,20,15));
        btnSeguir.addActionListener(e -> { continuar = true; dispose(); });
        pie.add(btnSeguir);

        raiz.add(pie, BorderLayout.SOUTH);

        setContentPane(raiz);
        getRootPane().setDefaultButton(btnSeguir);

        getRootPane().registerKeyboardAction(
            e -> { continuar = false; dispose(); },
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW);

        pack();
        setLocationRelativeTo(padre);
    }

    /** Mezcla el color a sobre el fondo b con opacidad t (0..1). */
    static Color mezcla(Color a, Color b, double t) {
        return new Color(
            (int) Math.round(a.getRed()   * t + b.getRed()   * (1 - t)),
            (int) Math.round(a.getGreen() * t + b.getGreen() * (1 - t)),
            (int) Math.round(a.getBlue()  * t + b.getBlue()  * (1 - t)));
    }

    static Color colorDe(Nivel n) {
        if (n == Nivel.COMPLETA) return GREEN;
        if (n == Nivel.PARCIAL)  return AMBER;
        return RED;
    }

    static String etiquetaNivel(Nivel n) {
        if (n == Nivel.COMPLETA) return "● CLASIFICACIÓN COMPLETA";
        if (n == Nivel.PARCIAL)  return "◐ CLASIFICACIÓN PARCIAL — faltan datos para cerrar el símbolo";
        return "○ NO CLASIFICABLE con los ensayos disponibles";
    }

    static String ensayosUsados(SueloData d) {
        StringBuilder sb = new StringBuilder("Ensayos usados: ");
        boolean algo = false;
        if (d.granCalculada)  { sb.append("Granulometría");            algo = true; }
        if (d.hidroCalculada) { sb.append(algo?" · ":"").append("Hidrómetro");  algo = true; }
        if (d.atterCalculada) { sb.append(algo?" · ":"").append("Atterberg");   algo = true; }
        if (!algo) sb.append("ninguno");
        return sb.toString();
    }

    static String cuerpoHtml(Resultado r, SueloData d) {
        StringBuilder b = new StringBuilder();
        b.append("<html><body style='background:#10161d;color:#e6ebf0;"
               + "font-family:sans-serif;font-size:12px;margin:14px;'>");

        if (!r.avisos.isEmpty()) {
            b.append(titulo("Advertencias", "#e05a5a"));
            for (String a : r.avisos)
                b.append("<p style='margin:4px 0;padding:8px;background:#2a1216;"
                       + "border-left:3px solid #e05a5a;'>").append(esc(a)).append("</p>");
        }

        if (!r.alternativas.isEmpty()) {
            b.append(titulo("Resultados posibles", "#e0a33a"));
            b.append("<table cellpadding='6' cellspacing='0' width='100%' "
                   + "style='border:1px solid #2a3a4a;'>");
            for (String[] alt : r.alternativas) {
                b.append("<tr>")
                 .append("<td width='14%' style='color:#7fa3c2;'><b>").append(esc(alt[0])).append("</b></td>")
                 .append("<td width='40%'>").append(esc(alt[1])).append("</td>")
                 .append("<td style='color:#a9b4bf;'>").append(esc(alt[2])).append("</td>")
                 .append("</tr>");
            }
            b.append("</table>");
        }

        if (!r.faltantes.isEmpty()) {
            b.append(titulo(r.nivel == Nivel.COMPLETA
                    ? "Datos opcionales que enriquecerían el resultado"
                    : "Para completar la clasificación falta", "#7fa3c2"));
            b.append("<ul style='margin:4px 0 0 18px;color:#e6ebf0;'>");
            for (String f : r.faltantes) b.append("<li style='margin:3px 0;'>").append(esc(f)).append("</li>");
            b.append("</ul>");
        }

        if (!r.pasos.isEmpty()) {
            b.append(titulo("Cómo se llegó a este resultado", "#a9b4bf"));
            b.append("<table cellpadding='4' cellspacing='0' width='100%'>");
            int i = 1;
            for (String p : r.pasos) {
                b.append("<tr>")
                 .append("<td width='26' valign='top' style='color:#7d8a97;'>").append(i++).append(".</td>")
                 .append("<td style='color:#e6ebf0;'>").append(esc(p)).append("</td>")
                 .append("</tr>");
            }
            b.append("</table>");
        }

        b.append(titulo("Datos usados", "#7d8a97"));
        b.append("<table cellpadding='5' cellspacing='0' width='100%' style='border:1px solid #2a3a4a;'>");
        if (d.granCalculada) {
            b.append(dato("Granulometría", String.format(
                "Grava %.1f %% · Arena %.1f %% · Finos %.1f %%%s",
                d.pctGrava, d.pctArena, d.pctFinos,
                d.Cu != null && d.Cc != null
                    ? String.format(" &nbsp;·&nbsp; Cu = %.2f · Cc = %.2f", d.Cu, d.Cc)
                    : " &nbsp;·&nbsp; Cu y Cc no disponibles")));
        }
        if (d.atterCalculada) {
            b.append(dato("Atterberg", String.format(
                "LL = %.1f %% · LP = %.1f %% · IP = %.1f %%", d.LL, d.LP, d.IP)));
        }
        if (d.hidroCalculada) {
            b.append(dato("Hidrómetro", String.format(
                "Arcilla %.1f %% · Limo %.1f %% · Arena fina %.1f %%%s",
                d.pctArcilla, d.pctLimo, d.pctArenaFina,
                r.texturalNombre != null ? " &nbsp;·&nbsp; " + r.texturalNombre : "")));
        }
        b.append("</table>");

        b.append("<p style='margin:14px 0 0 0;color:#7d8a97;font-size:11px;'>"
               + "Clasificación según ASTM D2487 (SUCS). Línea A: IP = 0.73·(LL−20) &nbsp;·&nbsp; "
               + "Línea U: IP = 0.9·(LL−8). Los límites se ensayan sobre la fracción que pasa el tamiz N.° 40."
               + "</p>");

        b.append("</body></html>");
        return b.toString();
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String titulo(String t, String color) {
        return "<p style='margin:16px 0 6px 0;color:" + color + ";font-size:13px;'><b>" + t + "</b></p>";
    }

    private static String dato(String k, String v) {
        return "<tr><td width='22%' style='color:#a9b4bf;'>" + k + "</td>"
             + "<td style='color:#e6ebf0;'>" + v + "</td></tr>";
    }

    private static JButton boton(String txt, Color bg, Color fg) {
        return Tema.boton(txt, bg);
    }
}