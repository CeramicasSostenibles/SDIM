package geoceramiclab;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

public class VisorPdf extends JDialog {

    private static final String CARPETA = "normas";

    public static File buscar(String nombreArchivo) {
        if (nombreArchivo == null || nombreArchivo.trim().isEmpty()) return null;
        String n = nombreArchivo.trim();

        File f = new File(CARPETA, n);
        if (f.isFile()) return f;

        f = new File(new File(carpetaDelPrograma(), CARPETA), n);
        if (f.isFile()) return f;

        f = new File(new File(System.getProperty("user.home"), "SDIM"), CARPETA + "/" + n);
        if (f.isFile()) return f;

        try (InputStream in = VisorPdf.class.getResourceAsStream(
                "/geoceramiclab/" + CARPETA + "/" + n)) {
            if (in != null) {
                File tmp = new File(System.getProperty("java.io.tmpdir"),
                                    "sdim-" + n.replace('/', '-'));
                if (!tmp.isFile() || tmp.length() == 0) {
                    try (OutputStream os = Files.newOutputStream(tmp.toPath())) {
                        byte[] buf = new byte[8192];
                        int r;
                        while ((r = in.read(buf)) > 0) os.write(buf, 0, r);
                    }
                    tmp.deleteOnExit();
                }
                return tmp;
            }
        } catch (Exception ignorada) { }

        return porPatron(n);
    }

    private static File porPatron(String patron) {
        String[] tokens = normalizar(patron).split("[\\s/_-]+");
        if (tokens.length == 0) return null;

        File[] raices = {
            new File(CARPETA),
            new File(carpetaDelPrograma(), CARPETA),
            new File(new File(System.getProperty("user.home"), "SDIM"), CARPETA),
            new File(System.getProperty("user.home"), "SDIM")
        };
        for (File r : raices) {
            File f = recorrer(r, tokens, 0);
            if (f != null) return f;
        }
        return null;
    }

    private static File recorrer(File carpeta, String[] tokens, int nivel) {
        if (carpeta == null || !carpeta.isDirectory() || nivel > 4) return null;
        File[] hijos = carpeta.listFiles();
        if (hijos == null) return null;

        for (File h : hijos) {

            if (h.isDirectory()) continue;
            String nom = normalizar(h.getName());
            if (!nom.endsWith(".pdf")) continue;
            boolean todos = true;
            for (String t : tokens) if (!t.isEmpty() && !nom.contains(t)) { todos = false; break; }
            if (todos) return h;
        }
        for (File h : hijos) {
            if (!h.isDirectory()) continue;
            File f = recorrer(h, tokens, nivel + 1);
            if (f != null) return f;
        }
        return null;
    }

    static File carpetaDelPrograma() {
        try {
            String ruta = System.getProperty("jpackage.app-path");
            if (ruta != null && !ruta.isEmpty()) {
                File exe = new File(ruta);
                File dir = exe.getParentFile();
                if (dir != null) {
                    File app = new File(dir, "app");
                    if (app.isDirectory()) return app;
                    return dir;
                }
            }
        } catch (Throwable ignorada) { }
        return new File(".");
    }

    static String normalizar(String s) {
        if (s == null) return "";
        String n = java.text.Normalizer.normalize(s.trim().toLowerCase(),
                java.text.Normalizer.Form.NFD);
        return n.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    static String compacto(String s) {
        return normalizar(s).replaceAll("[^a-z0-9]", "");
    }

    public static boolean hayMotor() { return PanelPdf.hayMotor(); }

    public static String carpetaSugerida() {
        return new File(new File(System.getProperty("user.home"), "SDIM"), CARPETA)
                .getAbsolutePath();
    }

    public static void mostrar(Window padre, String titulo, File pdf) {
        mostrar(padre, titulo, pdf, (String) null);
    }

    public static void mostrar(Window padre, String titulo, File pdf, String destino) {
        if (pdf == null) {
            JOptionPane.showMessageDialog(padre,
                    "No se encontró el archivo del documento.",
                    "Documento", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            new VisorPdf(padre, titulo, pdf, destino).setVisible(true);
            return;
        } catch (Throwable t) {
            System.err.println("[SDIM] Visor interno no disponible: " + t);
        }
        abrirConElSistema(padre, pdf);
    }

    static void abrirConElSistema(Window padre, File pdf) {
        try {
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(pdf);
                return;
            }
        } catch (Exception ignorada) { }
        JOptionPane.showMessageDialog(padre,
                "No se pudo abrir el documento automáticamente.\n\nEstá en:\n"
                        + pdf.getAbsolutePath(),
                "Documento", JOptionPane.INFORMATION_MESSAGE);
    }

    private VisorPdf(Window padre, String titulo, File pdf, String destino) throws Exception {
        super(padre, titulo, ModalityType.APPLICATION_MODAL);
        final PanelPdf panel = new PanelPdf(pdf, destino);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) { panel.cerrar(); }
        });

        Rectangle scr = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setSize(Math.min(940, scr.width - 120), Math.min(780, scr.height - 120));
        setLocationRelativeTo(padre);
    }
}