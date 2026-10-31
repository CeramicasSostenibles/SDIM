package geoceramiclab.modelo3d;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.*;

public class PlanoPythonPanel extends JPanel {

    private static final String PYTHON_CMD   = detectPython();

    private static final String SCRIPT_PATH  = "generar_plano.py";

    private static final String SVG_OUTPUT   = "plano.svg";

    private static final int    TIMEOUT_SECS = 30;

    private static final Color BG      = new Color(12, 14, 22);
    private static final Color BG2     = new Color(18, 22, 36);
    private static final Color BORDER  = new Color(42, 51, 80);
    private static final Color ACCENT  = new Color(79, 156, 249);
    private static final Color GREEN   = new Color(79, 217, 168);
    private static final Color AMBER   = new Color(249, 168, 79);
    private static final Color RED     = new Color(249, 95, 95);
    private static final Color MUTED   = new Color(143, 163, 200);

    private final JTextField  fldAncho;
    private final JTextField  fldAlto;
    private final JTextField  fldEspesor;
    private final JButton     btnGenerar;
    private final JLabel      lblEstado;
    private final JEditorPane svgViewer;
    private final JProgressBar progressBar;

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public PlanoPythonPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(BG);

        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
        controls.setBackground(BG2);
        controls.setPreferredSize(new Dimension(220, 0));
        controls.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER),
            BorderFactory.createEmptyBorder(16, 14, 16, 14)));

        JLabel title = monoLabel("⚙  Generador de Plano", new Color(180, 200, 220), 13, true);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        controls.add(title);
        controls.add(Box.createVerticalStrut(4));
        controls.add(sepLine());

        controls.add(Box.createVerticalStrut(10));
        controls.add(sectionLabel("Parámetros del Molde"));
        controls.add(Box.createVerticalStrut(8));

        fldAncho   = fieldRow(controls, "Ancho (mm)",    "200");
        fldAlto    = fieldRow(controls, "Alto (mm)",     "150");
        fldEspesor = fieldRow(controls, "Espesor (mm)",  "30");

        controls.add(Box.createVerticalStrut(14));
        controls.add(sepLine());
        controls.add(Box.createVerticalStrut(10));

        btnGenerar = mkButton("  Generar Plano SVG  ", BG, AMBER);
        btnGenerar.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnGenerar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        btnGenerar.addActionListener(e -> lanzarGeneracion());
        controls.add(btnGenerar);

        controls.add(Box.createVerticalStrut(10));

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        progressBar.setBackground(BG);
        progressBar.setForeground(ACCENT);
        progressBar.setBorderPainted(false);
        progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 6));
        controls.add(progressBar);

        controls.add(Box.createVerticalStrut(8));

        lblEstado = monoLabel("Listo.", MUTED, 9, false);
        lblEstado.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblEstado.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        controls.add(lblEstado);

        controls.add(Box.createVerticalStrut(10));
        controls.add(sepLine());
        controls.add(Box.createVerticalStrut(8));
        controls.add(sectionLabel("Notas"));
        controls.add(Box.createVerticalStrut(4));

        JTextArea notes = new JTextArea(
            "• Python 3 requerido\n" +
            "• generar_plano.py en\n" +
            "  el directorio de trabajo\n\n" +
            "Plano incluye:\n" +
            "  – Vista superior\n" +
            "  – Sección A-A\n" +
            "  – Cotas en mm\n" +
            "  – Cajetín técnico\n" +
            "  – Cavidad y nervios");
        notes.setFont(new Font("Monospaced", Font.PLAIN, 9));
        notes.setForeground(new Color(100, 120, 160));
        notes.setBackground(BG2);
        notes.setEditable(false);
        notes.setAlignmentX(Component.LEFT_ALIGNMENT);
        controls.add(notes);

        controls.add(Box.createVerticalGlue());

        JButton btnSave = mkButton("  💾  Guardar SVG  ", BG, new Color(100, 180, 100));
        btnSave.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnSave.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        btnSave.addActionListener(e -> guardarSVG());
        controls.add(btnSave);

        add(controls, BorderLayout.WEST);

        svgViewer = new JEditorPane();
        svgViewer.setEditable(false);
        svgViewer.setContentType("text/html");
        svgViewer.setBackground(new Color(30, 34, 50));
        svgViewer.setText(placeholderHtml());

        JScrollPane scroll = new JScrollPane(svgViewer);
        scroll.setBackground(BG);
        scroll.getViewport().setBackground(new Color(30, 34, 50));
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        add(scroll, BorderLayout.CENTER);
    }

    private void lanzarGeneracion() {
        double ancho, alto, espesor;
        try {
            ancho   = Double.parseDouble(fldAncho.getText().trim());
            alto    = Double.parseDouble(fldAlto.getText().trim());
            espesor = Double.parseDouble(fldEspesor.getText().trim());
            if (ancho <= 0 || alto <= 0 || espesor <= 0)
                throw new NumberFormatException("Valores deben ser positivos");
        } catch (NumberFormatException ex) {
            estado("❌ Error: ingrese números positivos válidos.", RED);
            JOptionPane.showMessageDialog(this,
                "Los valores deben ser números positivos.\n" + ex.getMessage(),
                "Parámetros inválidos", JOptionPane.ERROR_MESSAGE);
            return;
        }

        btnGenerar.setEnabled(false);
        progressBar.setIndeterminate(true);
        estado("⏳ Ejecutando Python…", AMBER);
        svgViewer.setText(loadingHtml());

        final double a = ancho, h = alto, e = espesor;

        executor.submit(() -> {
            String resultado = ejecutarPython(a, h, e);
            SwingUtilities.invokeLater(() -> {
                progressBar.setIndeterminate(false);
                btnGenerar.setEnabled(true);
                if (resultado == null) {

                    cargarSVGEnVisor();
                } else {
                    estado("❌ " + resultado, RED);
                    svgViewer.setText(errorHtml(resultado));
                }
            });
        });
    }

    private String ejecutarPython(double ancho, double alto, double espesor) {
        ProcessBuilder pb = new ProcessBuilder(
            PYTHON_CMD,
            SCRIPT_PATH,
            String.valueOf(ancho),
            String.valueOf(alto),
            String.valueOf(espesor)
        );
        pb.redirectErrorStream(true);

        pb.directory(resolveScriptDir());

        try {
            Process proc = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    final String log = line;
                    SwingUtilities.invokeLater(() ->
                        estado("Python: " + log, MUTED));
                }
            }

            boolean finished = proc.waitFor(TIMEOUT_SECS, TimeUnit.SECONDS);
            if (!finished) {
                proc.destroyForcibly();
                return "Timeout: Python tardó más de " + TIMEOUT_SECS + " segundos.";
            }

            int exitCode = proc.exitValue();
            if (exitCode != 0) {
                return "Python terminó con código " + exitCode + ":\n" + output;
            }

            File svgFile = resolveSVGFile();
            if (!svgFile.exists() || svgFile.length() == 0) {
                return "El archivo " + SVG_OUTPUT + " no fue creado por Python.";
            }

            return null;

        } catch (IOException ex) {
            return "No se pudo ejecutar Python.\n" +
                   "Verifique que '" + PYTHON_CMD + "' esté en el PATH.\n" +
                   "Detalle: " + ex.getMessage();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return "Operación interrumpida.";
        }
    }

    private void cargarSVGEnVisor() {
        try {
            File svgFile = resolveSVGFile();
            String svgContent = Files.readString(svgFile.toPath(), StandardCharsets.UTF_8);

            String html = "<html><body style='margin:0;padding:0;background:#1E2232;'>"
                        + svgContent
                        + "</body></html>";

            svgViewer.setText(html);
            svgViewer.setCaretPosition(0);
            estado("✅ Plano generado: " + svgFile.getAbsolutePath(), GREEN);

        } catch (IOException ex) {
            estado("❌ No se pudo leer el SVG: " + ex.getMessage(), RED);
            svgViewer.setText(errorHtml(ex.getMessage()));
        }
    }

    private void guardarSVG() {
        File src = resolveSVGFile();
        if (!src.exists()) {
            JOptionPane.showMessageDialog(this,
                "Primero genera el plano.", "Sin archivo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("plano_molde.svg"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                Files.copy(src.toPath(), fc.getSelectedFile().toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
                estado("💾 SVG guardado en: " + fc.getSelectedFile(), GREEN);
            } catch (IOException ex) {
                estado("❌ Error al guardar: " + ex.getMessage(), RED);
            }
        }
    }

    private static String detectPython() {
        for (String cmd : new String[]{"python3", "python", "py"}) {
            try {
                Process p = new ProcessBuilder(cmd, "--version").start();
                if (p.waitFor(3, TimeUnit.SECONDS) && p.exitValue() == 0)
                    return cmd;
            } catch (Exception ignored) {}
        }
        return "python";
    }

    private static File resolveScriptDir() {
        File script = new File(SCRIPT_PATH);
        if (script.exists()) return script.getAbsoluteFile().getParentFile();

        try {
            File jarDir = new File(
                PlanoPythonPanel.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).getParentFile();
            File inJarDir = new File(jarDir, SCRIPT_PATH);
            if (inJarDir.exists()) return jarDir;
        } catch (Exception ignored) {}
        return new File(".");
    }

    private static File resolveSVGFile() {

        return new File(resolveScriptDir(), SVG_OUTPUT);
    }

    private void estado(String msg, Color color) {
        lblEstado.setText("<html><font color='" + hexColor(color) + "'>" +
            msg.replace("\n", "<br>") + "</font></html>");
    }

    private static String hexColor(Color c) {
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }

    private static JLabel monoLabel(String text, Color fg, int size, boolean bold) {
        JLabel lbl = new JLabel(text);
        lbl.setForeground(fg);
        lbl.setFont(new Font("Monospaced", bold ? Font.BOLD : Font.PLAIN, size));
        return lbl;
    }

    private static JLabel sectionLabel(String text) {
        JLabel lbl = monoLabel(text.toUpperCase(), new Color(90, 111, 148), 8, true);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private static JSeparator sepLine() {
        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER);
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return sep;
    }

    private static JTextField fieldRow(JPanel parent, String label, String defVal) {
        JLabel lbl = monoLabel(label, MUTED, 9, false);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        parent.add(lbl);

        JTextField tf = new JTextField(defVal);
        tf.setFont(new Font("Monospaced", Font.BOLD, 11));
        tf.setForeground(new Color(200, 220, 255));
        tf.setBackground(new Color(22, 28, 46));
        tf.setCaretColor(ACCENT);
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            BorderFactory.createEmptyBorder(3, 6, 3, 6)));
        tf.setAlignmentX(Component.LEFT_ALIGNMENT);
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        parent.add(tf);
        parent.add(Box.createVerticalStrut(6));
        return tf;
    }

    private static JButton mkButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Monospaced", Font.BOLD, 11));
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(fg, 1),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                btn.setBackground(fg);
                btn.setForeground(bg);
            }
            @Override public void mouseExited(MouseEvent e) {
                btn.setBackground(bg);
                btn.setForeground(fg);
            }
        });
        return btn;
    }

    private static String placeholderHtml() {
        return "<html><body style='background:#1E2232;text-align:center;font-family:monospace;'>"
             + "<br><br><br>"
             + "<p style='color:#3A4A6E;font-size:18px;'>◈ SDIM</p>"
             + "<p style='color:#2A3860;font-size:13px;'>Ingrese los parámetros y haga clic en<br>"
             + "<b style='color:#F9A84F;'>Generar Plano SVG</b></p>"
             + "</body></html>";
    }

    private static String loadingHtml() {
        return "<html><body style='background:#1E2232;text-align:center;font-family:monospace;'>"
             + "<br><br><br>"
             + "<p style='color:#F9A84F;font-size:14px;'>⏳ Generando plano técnico…</p>"
             + "<p style='color:#5A6B8A;font-size:11px;'>Ejecutando Python</p>"
             + "</body></html>";
    }

    private static String errorHtml(String msg) {
        return "<html><body style='background:#1E2232;font-family:monospace;padding:20px;'>"
             + "<p style='color:#F95F5F;font-size:13px;'>❌ Error al generar el plano</p>"
             + "<pre style='color:#8090B0;font-size:10px;'>" + msg + "</pre>"
             + "<p style='color:#5A6B8A;font-size:10px;'>Verifique que Python esté instalado<br>"
             + "y que generar_plano.py esté en el directorio de trabajo.</p>"
             + "</body></html>";
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("SDIM — Plano Python SVG");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1100, 750);
            frame.setLocationByPlatform(true);
            frame.getContentPane().setBackground(new Color(12, 14, 22));
            frame.add(new PlanoPythonPanel());
            frame.setVisible(true);
        });
    }
}
