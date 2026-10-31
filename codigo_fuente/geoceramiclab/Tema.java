package geoceramiclab;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatLaf;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

public final class Tema {

    private Tema() {}

    private static final Preferences PREFS = Preferences.userNodeForPackage(Tema.class);

    public static final boolean OSCURO = PREFS.getBoolean("temaOscuro", true);

    private static Color dl(int oscuro, int claro) {
        return new Color(OSCURO ? oscuro : claro);
    }

    public static boolean esOscuro() { return OSCURO; }

    public static void guardarTema(boolean oscuro) {
        PREFS.putBoolean("temaOscuro", oscuro);
    }

    public static final Color FONDO        = dl(0x10161D, 0xEEF1F5);
    public static final Color BARRA        = dl(0x0B1015, 0xE3E8EE);
    public static final Color SUPERFICIE   = dl(0x16212C, 0xFFFFFF);
    public static final Color SUPERFICIE_2 = dl(0x1C2A37, 0xF3F6F9);
    public static final Color SUPERFICIE_3 = dl(0x18202F, 0xEAEFF4);
    public static final Color CONTROL      = dl(0x22303E, 0xE8EDF2);
    public static final Color BORDE        = dl(0x2A3A4A, 0xCFD8E0);
    public static final Color SELECCION    = dl(0x2C4A63, 0xD3E4F6);

    public static final Color TEXTO        = dl(0xE6EBF0, 0x1E2833);
    public static final Color TEXTO_2      = dl(0xA9B4BF, 0x55626E);
    public static final Color TEXTO_3      = dl(0x7D8A97, 0x8793A0);

    public static final Color ACENTO       = new Color(0x3E, 0x7C, 0xC4);
    public static final Color ACENTO_CLARO = new Color(0x5E, 0x97, 0xD8);

    public static final Color INFO         = dl(0x7FA3C2, 0x3E7CC4);

    public static final Color OK           = new Color(0x4C, 0xB7, 0x82);
    public static final Color AVISO        = new Color(0xE0, 0xA3, 0x3A);
    public static final Color ERROR        = new Color(0xE0, 0x5A, 0x5A);

    public static final Color GRAFICA_FONDO   = dl(0x0F131F, 0xFBFCFE);
    public static final Color GRAFICA_REJILLA = dl(0x242D44, 0xE3E8F0);

    public static String hex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    public static final String FAMILIA = elegirFamilia("Segoe UI", "Inter", "Helvetica Neue", "Arial");

    public static final String FAMILIA_MONO = elegirFamilia("Consolas", "Cascadia Mono", "Menlo", "Monospaced");

    public static final int T_PEQUENO = 11;
    public static final int T_TEXTO   = 12;
    public static final int T_BOTON   = 13;
    public static final int T_SECCION = 15;
    public static final int T_TITULO  = 20;

    public static Font ui(int estilo, int tamano) {
        return compuesta(FAMILIA, estilo, escala(tamano));
    }

    public static Font fuente(int estilo, float tamano) {
        Font f = compuesta(FAMILIA, estilo, Math.max(1, Math.round(tamano)));
        return f.getSize2D() == tamano ? f : f.deriveFont(tamano);
    }

    public static Font mono(int estilo, int tamano) {
        return compuesta(FAMILIA_MONO, estilo, tamano);
    }

    private static Font compuesta(String familia, int estilo, int tamano) {
        return javax.swing.text.StyleContext.getDefaultStyleContext().getFont(familia, estilo, tamano);
    }

    static int escala(int t) {
        if (t <= 10) return T_PEQUENO;
        if (t <= 12) return T_TEXTO;
        if (t == 13) return T_BOTON;
        if (t <= 16) return T_SECCION;
        return T_TITULO;
    }

    private static String elegirFamilia(String... candidatas) {
        try {
            java.util.Set<String> hay = new java.util.HashSet<>(java.util.Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
            for (String c : candidatas) if (hay.contains(c)) return c;
        } catch (Throwable ignorada) { }
        return Font.DIALOG;
    }

    public static void instalar() {
        try {
            Map<String, String> v = new HashMap<>();
            v.put("@background",          hex(SUPERFICIE));
            v.put("@foreground",          hex(TEXTO));
            v.put("@accentColor",         hex(ACENTO));
            v.put("@selectionBackground", hex(SELECCION));
            v.put("@selectionForeground", hex(TEXTO));
            v.put("@disabledForeground",  hex(TEXTO_3));
            FlatLaf.setGlobalExtraDefaults(v);
            if (OSCURO) FlatDarkLaf.setup(); else FlatLightLaf.setup();

            UIManager.put("defaultFont", new FontUIResource(compuesta(FAMILIA, Font.PLAIN, T_TEXTO)));

            UIManager.put("Component.arc", 8);
            UIManager.put("Button.arc", 8);
            UIManager.put("TextComponent.arc", 6);
            UIManager.put("Component.focusWidth", 1);
            UIManager.put("Component.borderColor", BORDE);
            UIManager.put("Component.focusedBorderColor", INFO);
            UIManager.put("Component.focusColor", new Color(INFO.getRed(), INFO.getGreen(), INFO.getBlue(), 90));

            UIManager.put("Panel.background", SUPERFICIE);
            UIManager.put("OptionPane.background", SUPERFICIE);
            UIManager.put("TextField.background", SUPERFICIE_2);
            UIManager.put("FormattedTextField.background", SUPERFICIE_2);
            UIManager.put("Spinner.background", SUPERFICIE_2);
            UIManager.put("ComboBox.background", SUPERFICIE_2);

            UIManager.put("ScrollBar.showButtons", false);
            UIManager.put("ScrollBar.width", 10);
            UIManager.put("ScrollBar.thumbArc", 999);
            UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
            UIManager.put("ScrollBar.track", FONDO);
            UIManager.put("ScrollPane.border", BorderFactory.createLineBorder(BORDE));

            UIManager.put("TabbedPane.tabHeight", 38);
            UIManager.put("TabbedPane.selectedBackground", SUPERFICIE);
            UIManager.put("TabbedPane.underlineColor", ACENTO);
            UIManager.put("TabbedPane.inactiveUnderlineColor", BORDE);
            UIManager.put("TabbedPane.hoverColor", SUPERFICIE_2);
            UIManager.put("TabbedPane.font", new FontUIResource(compuesta(FAMILIA, Font.BOLD, T_BOTON)));
            UIManager.put("TabbedPane.contentAreaColor", BORDE);

            UIManager.put("Table.background", SUPERFICIE);
            UIManager.put("Table.alternateRowColor", SUPERFICIE_2);
            UIManager.put("Table.gridColor", BORDE);
            UIManager.put("Table.showHorizontalLines", true);
            UIManager.put("Table.showVerticalLines", false);
            UIManager.put("Table.rowHeight", 24);
            UIManager.put("Table.selectionBackground", SELECCION);
            UIManager.put("TableHeader.background", BARRA);
            UIManager.put("TableHeader.foreground", TEXTO_2);
            UIManager.put("TableHeader.separatorColor", BORDE);
            UIManager.put("TableHeader.bottomSeparatorColor", BORDE);

            UIManager.put("SplitPane.background", FONDO);
            UIManager.put("SplitPaneDivider.draggingColor", ACENTO);
            UIManager.put("ToolTip.background", SUPERFICIE_2);
            UIManager.put("ToolTip.foreground", TEXTO);
            UIManager.put("ProgressBar.foreground", ACENTO);

            UIManager.put("TitlePane.unifiedBackground", true);
            UIManager.put("TitlePane.background", BARRA);
            UIManager.put("TitlePane.inactiveBackground", BARRA);

            FlatLaf.updateUI();
        } catch (Throwable t) {

            System.err.println("[SDIM] No se pudo instalar el tema: " + t);
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Throwable ignorada) { }
        }
    }

    public enum Tipo {

        PRIMARIO,

        SECUNDARIO,

        PELIGRO
    }

    public static JButton boton(String texto, Tipo tipo) {
        JButton b = new JButton(texto);
        estilizar(b, tipo);
        return b;
    }

    public static JButton boton(String texto, Color pista) {
        return boton(texto, tipoDe(pista));
    }

    public static JButton botonSolido(String texto, Color fondo) {
        JButton b = new JButton(texto);
        b.setFont(ui(Font.BOLD, T_BOTON));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(null);
        b.setMargin(new Insets(8, 20, 8, 20));
        Color hover    = mezclar(fondo, Color.WHITE, 0.12f);
        Color pressed  = mezclar(fondo, Color.BLACK, 0.18f);
        b.putClientProperty(FlatClientProperties.STYLE,
            "arc:8; focusWidth:0; innerFocusWidth:0; margin:8,20,8,20; borderWidth:0;"
            + "background:" + hex(fondo) + "; foreground:#ffffff;"
            + "hoverBackground:" + hex(hover) + "; pressedBackground:" + hex(pressed) + ";"
            + "disabledBackground:" + hex(CONTROL) + "; disabledText:" + hex(TEXTO_3) + ";");
        b.setBackground(fondo);
        b.setForeground(Color.WHITE);
        return b;
    }

    private static Color mezclar(Color a, Color b, float t) {
        return new Color(
            Math.round(a.getRed()   + (b.getRed()   - a.getRed())   * t),
            Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
            Math.round(a.getBlue()  + (b.getBlue()  - a.getBlue())  * t));
    }

    public static Tipo tipoDe(Color c) {
        if (c == null) return Tipo.SECUNDARIO;
        if (c.equals(ERROR)) return Tipo.PELIGRO;
        if (c.equals(OK) || c.equals(INFO) || c.equals(AVISO) || c.equals(ACENTO)) return Tipo.PRIMARIO;
        return Tipo.SECUNDARIO;
    }

    public static void estilizar(AbstractButton b, Tipo tipo) {
        b.setFont(ui(Font.BOLD, T_BOTON));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(null);
        b.setMargin(new Insets(7, 16, 7, 16));
        String comun = "arc:8; focusWidth:0; innerFocusWidth:0; margin:7,16,7,16;";
        switch (tipo) {
            case PRIMARIO:
                b.putClientProperty(FlatClientProperties.STYLE, comun
                    + "borderWidth:0; background:" + hex(ACENTO) + "; foreground:#ffffff;"
                    + "hoverBackground:" + hex(ACENTO_CLARO) + "; pressedBackground:#2f63a0;"
                    + "disabledBackground:" + hex(CONTROL) + "; disabledText:" + hex(TEXTO_3) + ";");
                b.setBackground(ACENTO);
                b.setForeground(Color.WHITE);
                break;
            case PELIGRO:
                b.putClientProperty(FlatClientProperties.STYLE, comun
                    + "background:" + hex(CONTROL) + "; foreground:" + hex(ERROR) + ";"
                    + "borderColor:" + hex(BORDE) + "; hoverBackground:" + hex(BORDE) + ";");
                b.setBackground(CONTROL);
                b.setForeground(ERROR);
                break;
            default:
                b.putClientProperty(FlatClientProperties.STYLE, comun
                    + "background:" + hex(CONTROL) + "; foreground:" + hex(TEXTO) + ";"
                    + "borderColor:" + hex(BORDE) + "; hoverBackground:" + hex(BORDE) + ";");
                b.setBackground(CONTROL);
                b.setForeground(TEXTO);
        }
    }

    public static Border bordeTarjeta(int margen) {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDE),
            BorderFactory.createEmptyBorder(margen, margen, margen, margen));
    }

    public static JComponent marcoGrafica(JComponent grafica) {
        JPanel m = new JPanel(new BorderLayout());
        m.setBackground(FONDO);
        m.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(10, 10, 10, 10),
            BorderFactory.createLineBorder(BORDE)));
        m.add(grafica, BorderLayout.CENTER);
        return m;
    }

    public static JLabel etiqueta(String texto, Color color, int estilo, int tamano) {
        JLabel l = new JLabel(texto);
        l.setFont(ui(estilo, tamano));
        l.setForeground(color);
        return l;
    }
}
