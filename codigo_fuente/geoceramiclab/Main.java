package geoceramiclab;

import geoceramiclab.model.BaldosinCatalan;
import geoceramiclab.model.ContornosMunicipales;
import geoceramiclab.model.MunicipiosCO;
import geoceramiclab.modelo3d.DialogoInicio;
import javafx.application.Platform;

import javax.swing.*;
import java.io.PrintWriter;
import java.io.StringWriter;

public class Main {

    private static volatile boolean abierta = false;

    public static void main(String[] args) {

        Thread.setDefaultUncaughtExceptionHandler((h, e) -> reportar("Error inesperado", e));

        try {
            Platform.startup(() -> {});
            Platform.setImplicitExit(false);
        } catch (Throwable t) {
            System.err.println("[SDIM] Aviso al iniciar JavaFX: " + t);
        }

        final Splash[] splash = new Splash[1];
        try {
            SwingUtilities.invokeAndWait(() -> {

                Tema.instalar();

                if (!Boolean.getBoolean("sdim.sinsplash")) splash[0] = Splash.mostrar();
            });
        } catch (Throwable t) {
            System.err.println("[SDIM] Aviso al crear el splash: " + t);
        }

        Timer vigilante = new Timer(15000, e -> {
            if (!abierta) {
                System.err.println("[SDIM] El splash tardó demasiado; se abre la ventana inicial.");
                if (splash[0] != null) splash[0].cerrarYa();
                abrirPrograma();
            }
        });
        vigilante.setRepeats(false);
        vigilante.start();

        Thread carga = new Thread(() -> {
            Splash s = splash[0];
            try {
                paso(s, "Cargando municipios…", 0.15);
                MunicipiosCO.todos();
                MunicipiosCO.departamentos();

                paso(s, "Cargando el mapa de Colombia…", 0.45);
                MunicipiosCO.contornos();

                paso(s, "Cargando contornos municipales…", 0.72);
                ContornosMunicipales.cuantos();

                paso(s, "Preparando la geometría de la pieza…", 0.90);
                BaldosinCatalan.actual();

                paso(s, "Listo", 1.0);
            } catch (Throwable t) {
                t.printStackTrace();
                paso(s, "Se cargó con advertencias", 1.0);
            }

            SwingUtilities.invokeLater(() -> {
                vigilante.stop();
                if (s != null) s.cerrar(Main::abrirPrograma);
                else abrirPrograma();
            });
        }, "carga-inicial");
        carga.setDaemon(false);
        carga.start();
    }

    private static void paso(Splash s, String txt, double avance) {
        if (s != null) s.paso(txt, avance);
        System.out.println("[SDIM] " + txt);
    }

    private static void abrirPrograma() {
        if (abierta) return;
        abierta = true;
        try {
            BaldosinCatalan baldosin = DialogoInicio.pedir(null);

            if (baldosin == null) {
                try { Platform.exit(); } catch (Throwable ignorada) { }
                System.exit(0);
                return;
            }
            BaldosinCatalan.setActual(baldosin);

            MainWindow ventana = new MainWindow();
            try { if (!Marca.iconos().isEmpty()) ventana.setIconImages(Marca.iconos()); }
            catch (Throwable ignorada) { }
            ventana.setVisible(true);

        } catch (Throwable t) {
            reportar("No se pudo abrir la ventana inicial", t);
        }
    }

    private static void reportar(String titulo, Throwable t) {
        t.printStackTrace();
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        String detalle = sw.toString();
        if (detalle.length() > 2500) detalle = detalle.substring(0, 2500) + "\n…";

        final String texto = detalle;
        Runnable mostrar = () -> {
            JTextArea ta = new JTextArea(titulo + "\n\n" + texto, 18, 74);
            ta.setEditable(false);
            ta.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 11));
            JOptionPane.showMessageDialog(null, new JScrollPane(ta),
                    "SDIM — " + titulo, JOptionPane.ERROR_MESSAGE);
        };
        if (SwingUtilities.isEventDispatchThread()) mostrar.run();
        else SwingUtilities.invokeLater(mostrar);
    }
}