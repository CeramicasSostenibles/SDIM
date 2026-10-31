package geoceramiclab.modelo3d;

import geoceramiclab.Tema;
import geoceramiclab.model.SueloData;
import geoceramiclab.model.BaldosinCatalan;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.AmbientLight;
import javafx.scene.PointLight;
import javafx.scene.Node;
import javafx.scene.DepthTest;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.paint.Color;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Interpolator;
import javafx.util.Duration;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.geometry.Bounds;
import javafx.geometry.VPos;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.transform.Scale;
import javafx.stage.FileChooser;
import javafx.embed.swing.SwingFXUtils;

import javax.imageio.ImageIO;
import java.io.File;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Cursor;

public class Ceramica3D extends JPanel {

    private static final java.awt.Color S_FONDO   = Tema.FONDO;
    private static final java.awt.Color S_PANEL   = Tema.SUPERFICIE;
    private static final java.awt.Color S_BORDE   = Tema.BORDE;
    private static final java.awt.Color S_TEXTO   = Tema.TEXTO;
    private static final java.awt.Color S_MUTED   = Tema.TEXTO_2;

    private static String fxHex(java.awt.Color c) { return Tema.hex(c); }
    private static Color  fxCol(java.awt.Color c) { return Color.rgb(c.getRed(), c.getGreen(), c.getBlue()); }

    private static final String FX_FONDO   = fxHex(Tema.FONDO);
    private static final String FX_PANEL   = fxHex(Tema.SUPERFICIE);
    private static final String FX_PANEL2  = fxHex(Tema.SUPERFICIE_2);
    private static final String FX_BORDE   = fxHex(Tema.BORDE);
    private static final String FX_TEXTO   = fxHex(Tema.TEXTO);
    private static final String FX_MUTED   = fxHex(Tema.TEXTO_2);
    private static final String FX_DIM     = fxHex(Tema.TEXTO_3);
    private static final String FX_AZUL    = fxHex(Tema.INFO);
    private static final String FX_VERDE   = fxHex(Tema.OK);
    private static final String FX_ACENTO  = fxHex(Tema.ACENTO);
    private static final String FX_TEAL    = "#3ea6b0";

    private static final String FX_HUD =
        "rgba(" + Tema.SUPERFICIE.getRed() + "," + Tema.SUPERFICIE.getGreen()
        + "," + Tema.SUPERFICIE.getBlue() + ",0.85)";

    private static final Color C_FONDO3D  = fxCol(Tema.GRAFICA_FONDO);
    private static final Color C_TEXTO_FX = fxCol(Tema.TEXTO_2);

    private static final Color C_GRID_MIN = fxCol(Tema.GRAFICA_REJILLA);
    private static final Color C_GRID_MAY = fxCol(Tema.BORDE);

    private static final String ESTILO_PANEL =
        "-fx-background-color:" + FX_PANEL + ";-fx-border-color:" + FX_BORDE +
        ";-fx-border-width:1;-fx-background-radius:8;-fx-border-radius:8;";

    private final SueloData datos;
    private final Runnable volverCallback;
    private final JFXPanel jfxPanel;
    private final VentanaMezclas ventanaMezclas;

    private Group root3D;
    private Group mundo3D;
    private Group baldosaGroup;
    private PerspectiveCamera camera3D;
    private double anchorX, anchorY;
    private Rotate rx, ry;

    private Group ejesGizmo;
    private Rotate rxEje, ryEje;

    private Group cuboVistas;
    private Rotate rxCubo, ryCubo;
    private Timeline animVista;

    private Group gridGroup;
    private double refCamZ = -700;
    private double refEjeLargo = 36;
    private double alturaPiso = -10;

    private double distHome = 700;

    private Group mundoFicha;
    private Group fichaGroup;
    private SubScene subFicha;
    private PerspectiveCamera cameraFicha;

    private boolean modoExport = false;

    private Label lblClasif, lblDetalle;
    private BaldosinCatalan bc = new BaldosinCatalan();

    private double sub3dW = 560;
    private double sub3dH = 580;

    private static final double HOME_RX = -145;
    private static final double HOME_RY =  -25;

    public Ceramica3D(SueloData datos, Runnable volverCallback) {
        this.datos = datos;
        this.volverCallback = volverCallback;
        setLayout(new BorderLayout());
        setBackground(S_FONDO);

        datos.addOyente(this::actualizarDesdeModelo);

        ventanaMezclas = new VentanaMezclas(datos);

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Tema.BARRA);
        top.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.BORDE),
                javax.swing.BorderFactory.createEmptyBorder(8, 16, 8, 16)));

        JPanel titulos = new JPanel(new java.awt.GridLayout(2, 1, 0, 1));
        titulos.setOpaque(false);
        titulos.add(Tema.etiqueta("Pieza: baldosín", Tema.TEXTO, java.awt.Font.BOLD, Tema.T_SECCION));
        titulos.add(Tema.etiqueta("Modelo 3D interactivo y secciones acotadas", Tema.TEXTO_3,
                java.awt.Font.PLAIN, Tema.T_PEQUENO));
        top.add(titulos, BorderLayout.WEST);

        JButton btnMezclas = Tema.boton("Siguiente: diseño de mezcla  →", Tema.Tipo.PRIMARIO);
        btnMezclas.addActionListener(e -> {
            ventanaMezclas.setDimensiones(bc.ancho, bc.largo, bc.espesorNominal, bc.numNerviosInt);
            ventanaMezclas.mostrar();
        });
        JPanel der = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        der.setOpaque(false);
        der.add(btnMezclas);
        top.add(der, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);

        jfxPanel = new JFXPanel();
        add(jfxPanel, BorderLayout.CENTER);

        Platform.runLater(this::initJFX);
    }

    private void initJFX() {
        Label lA = fx("Ancho cm:");  TextField fA = fxF("18");
        Label lL = fx("Largo cm:");  TextField fL = fxF("9");
        Label lE = fx("Esp. cm:");   TextField fE = fxF("1.5");
        Label lN = fx("Nervios:");   TextField fN = fxF("14");

        Button btnGen = new Button("Generar");
        btnGen.setStyle("-fx-background-color:" + FX_ACENTO + ";-fx-text-fill:#ffffff;"
                      + "-fx-font-weight:bold;-fx-cursor:hand;-fx-padding:6 14;-fx-background-radius:5;");

        Button btnCocer = new Button("Cocción 365°");
        btnCocer.setStyle("-fx-background-color:" + FX_TEAL + ";-fx-text-fill:#ffffff;"
                        + "-fx-font-weight:bold;-fx-cursor:hand;-fx-padding:6 14;-fx-background-radius:5;");

        HBox toolbar = new HBox(10, lA, fA, lL, fL, lE, fE, lN, fN, btnGen, btnCocer);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(8, 14, 8, 14));
        toolbar.setStyle(ESTILO_PANEL);

        lblClasif = new Label(tituloPieza());
        lblClasif.setStyle("-fx-text-fill:" + FX_ACENTO + ";-fx-font-family:'Segoe UI';-fx-font-size:15;-fx-font-weight:bold;");
        lblDetalle = new Label(" ");
        lblDetalle.setStyle("-fx-text-fill:" + FX_VERDE + ";-fx-font-family:'Segoe UI';-fx-font-size:11;");
        VBox info = new VBox(4, lblClasif, lblDetalle);
        info.setPadding(new Insets(8, 14, 8, 14));
        info.setStyle(ESTILO_PANEL);

        root3D = new Group();
        mundo3D = new Group();
        root3D.getChildren().add(mundo3D);

        SubScene sub3D = new SubScene(root3D, sub3dW, sub3dH, true, SceneAntialiasing.BALANCED);
        sub3D.setFill(C_FONDO3D);

        camera3D = new PerspectiveCamera(true);
        camera3D.setTranslateZ(-700);
        camera3D.setNearClip(0.1);
        camera3D.setFarClip(50000);
        sub3D.setCamera(camera3D);

        PointLight pl1 = new PointLight(Color.rgb(255, 250, 240));
        pl1.setTranslateX(400); pl1.setTranslateY(-400); pl1.setTranslateZ(-500);
        PointLight pl2 = new PointLight(Color.rgb(150, 170, 210));
        pl2.setTranslateX(-350); pl2.setTranslateY(300); pl2.setTranslateZ(-200);
        AmbientLight amb3D = new AmbientLight(Color.rgb(120, 124, 136));
        mundo3D.getChildren().addAll(pl1, pl2, amb3D);

        ry = new Rotate(HOME_RY, Rotate.Y_AXIS);
        rx = new Rotate(HOME_RX, Rotate.X_AXIS);
        mundo3D.getTransforms().addAll(ry, rx);

        ejesGizmo = crearEjesXYZ(refEjeLargo);
        ryEje = new Rotate(HOME_RY, Rotate.Y_AXIS);
        rxEje = new Rotate(HOME_RX, Rotate.X_AXIS);

        rxEje.angleProperty().bind(rx.angleProperty());
        ryEje.angleProperty().bind(ry.angleProperty());
        ejesGizmo.getTransforms().addAll(ryEje, rxEje);
        root3D.getChildren().add(ejesGizmo);

        sub3D.setOnMouseEntered(e -> sub3D.setCursor(javafx.scene.Cursor.OPEN_HAND));
        sub3D.setOnMouseExited(e -> sub3D.setCursor(javafx.scene.Cursor.DEFAULT));

        sub3D.setOnMousePressed(e -> {
            anchorX = e.getSceneX();
            anchorY = e.getSceneY();
            if (e.getButton() == MouseButton.PRIMARY
                    || e.getButton() == MouseButton.MIDDLE
                    || e.getButton() == MouseButton.SECONDARY) {
                sub3D.setCursor(javafx.scene.Cursor.CLOSED_HAND);
            }
        });

        sub3D.setOnMouseReleased(e -> {
            if (sub3D.isHover()) sub3D.setCursor(javafx.scene.Cursor.OPEN_HAND);
            else                 sub3D.setCursor(javafx.scene.Cursor.DEFAULT);
        });

        sub3D.setOnMouseDragged(e -> {
            double dx = e.getSceneX() - anchorX;
            double dy = e.getSceneY() - anchorY;
            if (e.getButton() == MouseButton.PRIMARY) {
                detenerAnimacionVista();
                setAngulos(rx.getAngle() - dy * 0.45, ry.getAngle() + dx * 0.45);
            } else if (e.getButton() == MouseButton.MIDDLE
                    || e.getButton() == MouseButton.SECONDARY) {
                double factor = Math.abs(camera3D.getTranslateZ()) * 0.0015;
                camera3D.setTranslateX(camera3D.getTranslateX() - dx * factor);
                camera3D.setTranslateY(camera3D.getTranslateY() - dy * factor);
                actualizarEjesEnPantalla();
            }
            anchorX = e.getSceneX();
            anchorY = e.getSceneY();
        });

        sub3D.setOnScroll(e -> {
            camera3D.setTranslateZ(camera3D.getTranslateZ() + e.getDeltaY() * 3);
            actualizarGridYEjesConZoom();
        });

        Label tit3D = new Label(" 3D · izq=rotar · der/centro=pan · scroll=zoom");
        tit3D.setStyle("-fx-text-fill:" + FX_AZUL + ";-fx-font-family:'Segoe UI';-fx-font-size:11;"
                     + "-fx-padding:4 8;-fx-background-color:" + FX_HUD + ";-fx-background-radius:4;");

        StackPane hudVistas = crearHudVistas();

        StackPane pane3D = new StackPane(sub3D, tit3D, hudVistas);
        StackPane.setAlignment(tit3D, Pos.TOP_LEFT);
        StackPane.setAlignment(hudVistas, Pos.TOP_RIGHT);
        StackPane.setMargin(hudVistas, new Insets(8, 10, 0, 0));
        pane3D.setStyle(ESTILO_PANEL);

        sub3D.setManaged(false);
        sub3D.widthProperty().bind(pane3D.widthProperty());
        sub3D.heightProperty().bind(pane3D.heightProperty());
        sub3D.widthProperty().addListener((o, a, b) -> {
            sub3dW = b.doubleValue();
            actualizarEjesEnPantalla();
        });
        sub3D.heightProperty().addListener((o, a, b) -> {
            sub3dH = b.doubleValue();
            actualizarEjesEnPantalla();
        });

        mundoFicha = new Group();
        subFicha = new SubScene(mundoFicha, 560, 580, true, SceneAntialiasing.BALANCED);
        subFicha.setFill(C_FONDO3D);

        cameraFicha = new PerspectiveCamera(true);
        cameraFicha.setVerticalFieldOfView(true);
        cameraFicha.setTranslateZ(-900);
        cameraFicha.setNearClip(0.1);
        cameraFicha.setFarClip(12000);
        subFicha.setCamera(cameraFicha);

        AmbientLight ambF = new AmbientLight(Color.WHITE);
        mundoFicha.getChildren().add(ambF);

        Label titFicha = new Label(" Secciones acotadas · cara lisa | cara nervada | corte | detalle de nervios · clic derecho = copiar PNG");
        titFicha.setStyle("-fx-text-fill:" + FX_MUTED + ";-fx-font-family:'Segoe UI';-fx-font-size:11;"
                        + "-fx-padding:4 8;-fx-background-color:" + FX_HUD + ";-fx-background-radius:4;");

        StackPane paneFicha = new StackPane(subFicha, titFicha);
        StackPane.setAlignment(titFicha, Pos.TOP_LEFT);
        paneFicha.setStyle(ESTILO_PANEL);

        subFicha.setManaged(false);
        subFicha.widthProperty().bind(paneFicha.widthProperty());
        subFicha.heightProperty().bind(paneFicha.heightProperty());

        subFicha.widthProperty().addListener((o, x, y) -> ajustarCamaraFicha());
        subFicha.heightProperty().addListener((o, x, y) -> ajustarCamaraFicha());

        MenuItem miCopiar = new MenuItem("Copiar imagen (PNG)");
        miCopiar.setOnAction(e -> copiarFichaPNG());
        MenuItem miGuardar = new MenuItem("Guardar como PNG…");
        miGuardar.setOnAction(e -> guardarFichaPNG());
        ContextMenu menuFicha = new ContextMenu(miCopiar, miGuardar);
        paneFicha.setOnContextMenuRequested(e -> {
            menuFicha.show(paneFicha, e.getScreenX(), e.getScreenY());
            e.consume();
        });
        paneFicha.setOnMousePressed(e -> { if (menuFicha.isShowing()) menuFicha.hide(); });

        HBox split = new HBox(16, pane3D, paneFicha);
        HBox.setHgrow(pane3D, Priority.ALWAYS);
        HBox.setHgrow(paneFicha, Priority.ALWAYS);
        split.setPadding(new Insets(4, 0, 4, 0));
        split.setStyle("-fx-background-color:" + FX_FONDO + ";");

        Label hint = new Label(" ViewCube arriba a la derecha: clic en una cara = alzado · ⌂ = vista inicial");
        hint.setStyle("-fx-text-fill:" + FX_DIM + ";-fx-font-family:'Segoe UI';-fx-font-size:11;"
                    + "-fx-padding:6 14;" + ESTILO_PANEL);

        btnGen.setOnAction(ev -> {
            try {
                bc.ancho = Double.parseDouble(fA.getText().replace(",", "."));
                bc.largo = Double.parseDouble(fL.getText().replace(",", "."));
                bc.espesorNominal = Double.parseDouble(fE.getText().replace(",", "."));
                bc.numNerviosInt = Integer.parseInt(fN.getText().trim());
                bc.calcularGeometria();
            } catch (Exception ex) {  }
            crearPieza3D();
            crearFichaEstatica();
            SwingUtilities.invokeLater(() -> {
                ventanaMezclas.setDimensiones(bc.ancho, bc.largo, bc.espesorNominal, bc.numNerviosInt);
            });
        });
        btnCocer.setOnAction(ev -> animar365());

        VBox root = new VBox(10, toolbar, info, split, hint);
        VBox.setVgrow(split, Priority.ALWAYS);
        root.setPadding(new Insets(10, 10, 10, 10));
        root.setStyle("-fx-background-color:" + FX_FONDO + ";");

        jfxPanel.setScene(new Scene(root, Color.web(FX_FONDO)));

        crearPieza3D();
        crearFichaEstatica();
        SwingUtilities.invokeLater(() ->
                ventanaMezclas.setDimensiones(bc.ancho, bc.largo, bc.espesorNominal, bc.numNerviosInt));
    }

    private static final double[] V_FRONTAL   = { 180,    0 };
    private static final double[] V_POSTERIOR = { 180,  180 };
    private static final double[] V_DERECHA   = { 180,   90 };
    private static final double[] V_IZQUIERDA = { 180,  -90 };
    private static final double[] V_SUPERIOR  = { -90,    0 };
    private static final double[] V_INFERIOR  = {  90,    0 };

    private static final double LADO_CUBO = 54;
    private static final double SUBCUBO   = 130;

    private StackPane crearHudVistas() {
        cuboVistas = new Group();

        agregarCara(cuboVistas, 'Z', +1, "FRENTE", V_FRONTAL);
        agregarCara(cuboVistas, 'Z', -1, "ATRÁS",  V_POSTERIOR);
        agregarCara(cuboVistas, 'X', +1, "DER",    V_DERECHA);
        agregarCara(cuboVistas, 'X', -1, "IZQ",    V_IZQUIERDA);
        agregarCara(cuboVistas, 'Y', +1, "SUP",    V_SUPERIOR);
        agregarCara(cuboVistas, 'Y', -1, "INF",    V_INFERIOR);

        ryCubo = new Rotate(HOME_RY, Rotate.Y_AXIS);
        rxCubo = new Rotate(HOME_RX, Rotate.X_AXIS);
        if (rx != null) rxCubo.angleProperty().bind(rx.angleProperty());
        if (ry != null) ryCubo.angleProperty().bind(ry.angleProperty());
        cuboVistas.getTransforms().addAll(ryCubo, rxCubo);

        Group rootCubo = new Group(cuboVistas);

        SubScene subCubo = new SubScene(rootCubo, SUBCUBO, SUBCUBO, true, SceneAntialiasing.BALANCED);
        subCubo.setFill(Color.TRANSPARENT);

        subCubo.setPickOnBounds(false);

        PerspectiveCamera camCubo = new PerspectiveCamera(true);
        camCubo.setTranslateZ(-230);
        camCubo.setNearClip(0.1);
        camCubo.setFarClip(2000);
        subCubo.setCamera(camCubo);

        AmbientLight ambCubo = new AmbientLight(Color.WHITE);
        rootCubo.getChildren().add(ambCubo);

        Button btnCasa = new Button("⌂");
        btnCasa.setStyle(estiloBotonHud(false));
        btnCasa.setTooltip(new Tooltip("Vista inicial (isométrica) y encuadre por defecto"));
        btnCasa.setOnMouseEntered(e -> btnCasa.setStyle(estiloBotonHud(true)));
        btnCasa.setOnMouseExited(e  -> btnCasa.setStyle(estiloBotonHud(false)));
        btnCasa.setOnAction(e -> irAVistaInicial());

        Button bLeft  = botonFlecha("◂", "Girar a la izquierda",  () -> rotarVista(  0, -90));
        Button bUp    = botonFlecha("▴", "Girar hacia arriba",    () -> rotarVista(+90,   0));
        Button bDown  = botonFlecha("▾", "Girar hacia abajo",     () -> rotarVista(-90,   0));
        Button bRight = botonFlecha("▸", "Girar a la derecha",    () -> rotarVista(  0, +90));

        double lado = SUBCUBO + 30;
        StackPane hud = new StackPane();
        hud.setMinSize(lado, lado);
        hud.setPrefSize(lado, lado);
        hud.setMaxSize(lado, lado);
        hud.setPickOnBounds(false);
        hud.getChildren().addAll(subCubo, bUp, bDown, bLeft, bRight, btnCasa);
        StackPane.setAlignment(subCubo, Pos.CENTER);
        StackPane.setAlignment(bUp,     Pos.TOP_CENTER);
        StackPane.setAlignment(bDown,   Pos.BOTTOM_CENTER);
        StackPane.setAlignment(bLeft,   Pos.CENTER_LEFT);
        StackPane.setAlignment(bRight,  Pos.CENTER_RIGHT);
        StackPane.setAlignment(btnCasa, Pos.TOP_LEFT);
        return hud;
    }

    private Button botonFlecha(String glifo, String tip, Runnable accion) {
        Button b = new Button(glifo);
        b.setStyle(estiloFlecha(false));
        b.setTooltip(new Tooltip(tip));
        b.setFocusTraversable(false);
        b.setOnMouseEntered(e -> b.setStyle(estiloFlecha(true)));
        b.setOnMouseExited(e  -> b.setStyle(estiloFlecha(false)));
        b.setOnAction(e -> accion.run());
        return b;
    }

    private static String estiloFlecha(boolean hover) {
        return "-fx-background-color:" + (hover ? FX_ACENTO : FX_HUD) + ";"
             + "-fx-text-fill:" + (hover ? "#ffffff" : FX_AZUL) + ";"
             + "-fx-font-size:12;-fx-font-weight:bold;-fx-cursor:hand;"
             + "-fx-min-width:22;-fx-min-height:22;-fx-max-width:22;-fx-max-height:22;-fx-padding:0;"
             + "-fx-background-radius:11;-fx-border-radius:11;"
             + "-fx-border-color:" + FX_BORDE + ";-fx-border-width:1;";
    }

    private Button botonHud(String txt, String tip, Runnable accion) {
        Button b = new Button(txt);
        b.setStyle(estiloBotonHud(false));
        b.setTooltip(new Tooltip(tip));
        b.setOnMouseEntered(e -> b.setStyle(estiloBotonHud(true)));
        b.setOnMouseExited(e  -> b.setStyle(estiloBotonHud(false)));
        b.setOnAction(e -> accion.run());
        return b;
    }

    // Gira la vista 90 grados por clic (flechas del HUD)
    private void rotarVista(double dRx, double dRy) {
        if (rx == null || ry == null) return;
        animarHaciaVista(rx.getAngle() + dRx, ry.getAngle() + dRy);
    }

    private static String estiloBotonHud(boolean hover) {
        return "-fx-background-color:" + (hover ? FX_PANEL2 : "" + FX_HUD + "") + ";"
             + "-fx-text-fill:" + (hover ? FX_AZUL : FX_MUTED) + ";"
             + "-fx-font-size:15;-fx-font-weight:bold;-fx-cursor:hand;"
             + "-fx-min-width:28;-fx-min-height:26;-fx-padding:0;"
             + "-fx-border-color:" + FX_BORDE + ";-fx-border-radius:4;-fx-background-radius:4;";
    }

    private void agregarCara(Group padre, char eje, int signo, String etiqueta, double[] vista) {
        double L = LADO_CUBO;
        double t = 1.6;
        double off = signo * L / 2.0;

        Box cara;
        switch (eje) {
            case 'X': cara = new Box(t, L, L); cara.setTranslateX(off); break;
            case 'Y': cara = new Box(L, t, L); cara.setTranslateY(off); break;
            default:  cara = new Box(L, L, t); cara.setTranslateZ(off); break;
        }

        PhongMaterial normal = new PhongMaterial();
        normal.setDiffuseMap(texturaCara(etiqueta, false, eje, signo));
        normal.setSpecularColor(Color.TRANSPARENT);

        PhongMaterial resaltada = new PhongMaterial();
        resaltada.setDiffuseMap(texturaCara(etiqueta, true, eje, signo));
        resaltada.setSpecularColor(Color.TRANSPARENT);

        cara.setMaterial(normal);
        cara.setCursor(javafx.scene.Cursor.HAND);
        cara.setOnMouseEntered(e -> cara.setMaterial(resaltada));
        cara.setOnMouseExited(e  -> cara.setMaterial(normal));
        cara.setOnMouseClicked(e -> {
            animarHaciaVista(vista[0], vista[1]);
            e.consume();
        });
        Tooltip.install(cara, new Tooltip("Vista " + etiqueta.toLowerCase()));

        padre.getChildren().add(cara);
    }

    private Image texturaCara(String texto, boolean resaltada, char eje, int signo) {
        final int S = 160;
        Canvas c = new Canvas(S, S);
        GraphicsContext g = c.getGraphicsContext2D();

        Color base1  = resaltada ? Color.web("#4d8ad6") : Color.web("#2d3a4d");
        Color base2  = resaltada ? Color.web("#2f63a0") : Color.web("#1f2836");
        Color borde  = resaltada ? Color.web("#a9ccf0") : Color.web("#3c4e64");
        Color colTxt = resaltada ? Color.web("#ffffff") : Color.web("#cfdae8");

        g.clearRect(0, 0, S, S);

        double pad = 9, arc = 30;
        javafx.scene.paint.LinearGradient grad = new javafx.scene.paint.LinearGradient(
                0, 0, 0, 1, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0, base1),
                new javafx.scene.paint.Stop(1, base2));
        g.setFill(grad);
        g.fillRoundRect(pad, pad, S - 2 * pad, S - 2 * pad, arc, arc);

        g.setStroke(Color.web("#ffffff", 0.08));
        g.setLineWidth(1.5);
        g.strokeRoundRect(pad + 2, pad + 2, S - 2 * pad - 4, S - 2 * pad - 4, arc - 4, arc - 4);
        g.setStroke(borde);
        g.setLineWidth(resaltada ? 4 : 2.5);
        g.strokeRoundRect(pad, pad, S - 2 * pad, S - 2 * pad, arc, arc);

        g.save();
        g.translate(S / 2.0, S / 2.0);
        g.rotate(rotEtiqueta(eje, signo));
        if (flipEtiqueta(eje, signo)) g.scale(-1, 1);
        g.setFill(colTxt);
        g.setFont(Font.font("SansSerif", FontWeight.BOLD, texto.length() > 4 ? 28 : 36));
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);
        g.fillText(texto, 0, 0);
        g.restore();

        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);
        return c.snapshot(sp, null);
    }

    // El Box de JavaFX mapea la textura girada 180 grados en sus caras:
    // se contrarresta rotando el rotulo 180 grados para que se lea derecho.
    private static double rotEtiqueta(char eje, int signo) { return 180; }

    private static boolean flipEtiqueta(char eje, int signo) { return false; }

    private void setAngulos(double ax, double ay) {
        if (rx != null) rx.setAngle(ax);
        if (ry != null) ry.setAngle(ay);
    }

    private void detenerAnimacionVista() {
        if (animVista != null) {
            animVista.stop();
            animVista = null;
        }
    }

    private static double masCercano(double actual, double destino) {
        double d = destino;
        while (d - actual >  180) d -= 360;
        while (d - actual < -180) d += 360;
        return d;
    }

    private void animarHaciaVista(double destinoRx, double destinoRy) {
        if (rx == null || ry == null) return;
        detenerAnimacionVista();

        double dRx = masCercano(rx.getAngle(), destinoRx);
        double dRy = masCercano(ry.getAngle(), destinoRy);

        animVista = new Timeline(new KeyFrame(Duration.millis(480),
                new KeyValue(rx.angleProperty(), dRx, Interpolator.EASE_BOTH),
                new KeyValue(ry.angleProperty(), dRy, Interpolator.EASE_BOTH)));

        animVista.setOnFinished(e -> animVista = null);
        animVista.play();
    }

    private void irAVistaInicial() {
        if (camera3D == null) return;
        detenerAnimacionVista();

        double dRx = masCercano(rx.getAngle(), HOME_RX);
        double dRy = masCercano(ry.getAngle(), HOME_RY);

        animVista = new Timeline(new KeyFrame(Duration.millis(520),
                new KeyValue(rx.angleProperty(), dRx, Interpolator.EASE_BOTH),
                new KeyValue(ry.angleProperty(), dRy, Interpolator.EASE_BOTH),
                new KeyValue(camera3D.translateXProperty(), 0.0, Interpolator.EASE_BOTH),
                new KeyValue(camera3D.translateYProperty(), 0.0, Interpolator.EASE_BOTH),
                new KeyValue(camera3D.translateZProperty(), -distHome, Interpolator.EASE_BOTH)));

        animVista.currentTimeProperty().addListener((o, a, b) -> actualizarEjesEnPantalla());
        animVista.setOnFinished(e -> {
            animVista = null;
            actualizarGridYEjesConZoom();
        });
        animVista.play();
    }

    private void crearPieza3D() {
        if (mundo3D == null) return;

        mundo3D.getChildren().removeIf(n -> n instanceof Group);

        bc.calcularGeometria();

        double sc = 12.0;
        double W = bc.ancho * sc;
        double L = bc.largo * sc;
        double Hb = bc.espesor * sc;
        double Hn = bc.altoNervio * sc;
        double ns = bc.nervCentral * sc;
        double rs = bc.ranura * sc;
        double nb = bc.nervBorde * sc;
        int N = Math.max(0, bc.numNerviosInt);

        Color col = colorRealista(colorDesdeHex(datos.colorSuelo));
        PhongMaterial matLiso = matSolido(col);
        PhongMaterial matNerv = matSolido(col.darker());
        PhongMaterial matBorde = matSolido(col.darker().darker());

        baldosaGroup = construirPiezaOrientada(W, L, Hb, Hn, ns, rs, nb, N,
                matLiso, matNerv, matBorde, true);

        alturaPiso = -(Hb / 2.0) - 1.5;

        gridGroup = new Group();
        mundo3D.getChildren().add(gridGroup);

        mundo3D.getChildren().add(baldosaGroup);

        setAngulos(HOME_RX, HOME_RY);

        double dist = Math.max(W, Math.max(L, Hb + Hn)) * 2.6;
        camera3D.setTranslateZ(-dist);
        camera3D.setTranslateX(0);
        camera3D.setTranslateY(0);
        refCamZ = -dist;
        distHome = dist;

        actualizarGridYEjesConZoom();

        if (lblDetalle != null) {
            lblDetalle.setText(String.format(
                    "%.0f×%.0f cm | base=%.1f mm | nervios=%.1f mm | %d nerv.int. | Vol.neto=%.1f cm³",
                    bc.largo, bc.ancho, bc.espesor * 10, bc.altoNervio * 10, N, bc.volNeto));
        }
    }

    private void actualizarGridYEjesConZoom() {
        if (camera3D == null) return;
        double dist = Math.abs(camera3D.getTranslateZ());
        if (dist < 50) dist = 50;

        if (gridGroup != null) {
            gridGroup.getChildren().clear();
            double tamanoGrid = dist * 4.0;
            double pasoIdeal = dist * 0.08;
            double paso = redondearPasoCAD(pasoIdeal);
            int divisiones = (int) Math.round(tamanoGrid / paso);
            if (divisiones < 8) divisiones = 8;
            if (divisiones > 60) divisiones = 60;
            tamanoGrid = divisiones * paso;
            construirCuadriculaEn(gridGroup, tamanoGrid, divisiones, alturaPiso);
        }

        if (baldosaGroup != null && gridGroup != null) {
            mundo3D.getChildren().remove(gridGroup);
            mundo3D.getChildren().remove(baldosaGroup);
            mundo3D.getChildren().add(gridGroup);
            mundo3D.getChildren().add(baldosaGroup);
        }

        actualizarEjesEnPantalla();
    }

    private void actualizarEjesEnPantalla() {
        if (ejesGizmo == null || camera3D == null) return;

        double dist = Math.abs(camera3D.getTranslateZ());
        if (dist < 50) dist = 50;

        double fovRad = Math.toRadians(camera3D.getFieldOfView());
        double halfH = Math.tan(fovRad / 2.0) * dist;
        double aspect = (sub3dH > 0 ? sub3dW / sub3dH : 1.0);
        double halfW = halfH * aspect;

        double largoPantalla = halfH * 0.18;
        double escala = largoPantalla / refEjeLargo;
        escala = Math.max(0.15, Math.min(escala, 8.0));
        ejesGizmo.setScaleX(escala);
        ejesGizmo.setScaleY(escala);
        ejesGizmo.setScaleZ(escala);

        double margin = 0.22;
        double ox = camera3D.getTranslateX() - halfW * (1.0 - margin);
        double oy = camera3D.getTranslateY() + halfH * (1.0 - margin);
        double oz = camera3D.getTranslateZ() + dist * 0.55;

        ejesGizmo.setTranslateX(ox);
        ejesGizmo.setTranslateY(oy);
        ejesGizmo.setTranslateZ(oz);

        if (root3D != null) {
            root3D.getChildren().remove(ejesGizmo);
            root3D.getChildren().add(ejesGizmo);
        }
    }

    private double redondearPasoCAD(double v) {
        if (v <= 0.01) return 0.01;
        double exp = Math.floor(Math.log10(v));
        double base = Math.pow(10, exp);
        double n = v / base;
        double nice;
        if (n < 1.5) nice = 1;
        else if (n < 3.5) nice = 2;
        else if (n < 7.5) nice = 5;
        else nice = 10;
        return nice * base;
    }

    private void construirCuadriculaEn(Group g, double tamano, int divisiones, double alturaY) {
        PhongMaterial matLinea = matPlano(C_GRID_MIN);
        PhongMaterial matEjeMayor = matPlano(C_GRID_MAY);

        double mitad = tamano / 2.0;
        double paso = tamano / divisiones;
        double esp = Math.max(0.10, paso * 0.006);
        double espMayor = esp * 1.5;

        for (int i = 0; i <= divisiones; i++) {
            double pos = -mitad + i * paso;
            boolean esMayor = (i % 5 == 0);
            double e = esMayor ? espMayor : esp;
            PhongMaterial mat = esMayor ? matEjeMayor : matLinea;

            Box lx = new Box(tamano, e, e);
            lx.setTranslateZ(pos);
            lx.setTranslateY(alturaY);
            lx.setMaterial(mat);
            g.getChildren().add(lx);

            Box lz = new Box(e, e, tamano);
            lz.setTranslateX(pos);
            lz.setTranslateY(alturaY);
            lz.setMaterial(mat);
            g.getChildren().add(lz);
        }
    }

    private Group crearEjesXYZ(double largo) {
        Group g = new Group();
        g.getChildren().add(crearEjeDelgado('X', +1, largo, Color.rgb(226, 84, 84), "X"));
        g.getChildren().add(crearEjeDelgado('Z', +1, largo, Color.rgb(88, 200, 110), "Y"));
        g.getChildren().add(crearEjeDelgado('Y', +1, largo, Color.rgb(92, 140, 245), "Z"));
        return g;
    }

    private Group crearEjeDelgado(char eje, int signo, double largo, Color color, String etiqueta) {
        Group g = new Group();
        PhongMaterial mat = matPlano(color);

        double grosor = Math.max(0.30, largo * 0.025);
        double largoAsta = largo * 0.82;
        double largoPunta = largo - largoAsta;

        Box asta = cajaEnEje(eje, largoAsta, grosor);
        asta.setMaterial(mat);
        moverEnEje(asta, eje, signo * largoAsta / 2.0);
        g.getChildren().add(asta);

        Box puntaBase = cajaEnEje(eje, largoPunta * 0.50, grosor * 1.7);
        puntaBase.setMaterial(mat);
        moverEnEje(puntaBase, eje, signo * (largoAsta + largoPunta * 0.25));
        g.getChildren().add(puntaBase);

        Box puntaFin = cajaEnEje(eje, largoPunta * 0.50, grosor * 0.50);
        puntaFin.setMaterial(mat);
        moverEnEje(puntaFin, eje, signo * (largoAsta + largoPunta * 0.50 + largoPunta * 0.25));
        g.getChildren().add(puntaFin);

        Text lbl = txtPlano(etiqueta, 11, color);
        moverEnEje(lbl, eje, signo * (largo + 9));
        g.getChildren().add(lbl);

        return g;
    }

    private Box cajaEnEje(char eje, double longitud, double grosor) {
        switch (eje) {
            case 'X': return new Box(longitud, grosor, grosor);
            case 'Y': return new Box(grosor, longitud, grosor);
            default:  return new Box(grosor, grosor, longitud);
        }
    }

    private void moverEnEje(Node n, char eje, double valor) {
        switch (eje) {
            case 'X': n.setTranslateX(valor); break;
            case 'Y': n.setTranslateY(valor); break;
            default:  n.setTranslateZ(valor); break;
        }
    }

    private void crearFichaEstatica() {
        if (mundoFicha == null) return;
        mundoFicha.getChildren().removeIf(n -> n instanceof Group);

        bc.calcularGeometria();

        final double sc = 9.0;
        double anchoCm = Math.max(0.5, bc.ancho);
        double largoCm = Math.max(0.5, bc.largo);
        double espCm   = Math.max(0.2, bc.espesor);

        double LX = anchoCm * sc;
        double LY = largoCm * sc;
        double Hb = espCm * sc;

        // Los nervios se reparten sobre el lado menor y corren paralelos al mayor
        double spanCm = Math.min(anchoCm, largoCm);
        double span   = Math.min(LX, LY);
        double[] nv = patronNervios(span, bc, sc);
        double nb = nv[0], ns = nv[1], rs = nv[2], Hn = nv[3];
        int N = Math.max(0, bc.numNerviosInt);

        Color col = masTerracota(colorRealista(colorDesdeHex(datos.colorSuelo)), 0.40);

        PhongMaterial matLiso  = matPlano(col);
        PhongMaterial matNerv  = matPlano(col.darker());
        PhongMaterial matBorde = matPlano(col.darker().darker());
        Color colSombra = modoExport ? Color.rgb(0, 0, 0, 0.18) : Color.rgb(0, 0, 0, 0.55);
        Color colCota   = modoExport ? Color.rgb(35, 40, 55) : C_TEXTO_FX;

        Group bloqueLisa = new Group();
        Box sombraLisa = crearSombraPlana(LX * 0.94, LY * 0.94, Hb / 2.0 + 4, colSombra);
        sombraLisa.setTranslateX(6); sombraLisa.setTranslateY(7);
        Box caraLisa = new Box(LX, LY, Hb);
        caraLisa.setMaterial(matLiso);
        bloqueLisa.getChildren().addAll(sombraLisa, caraLisa,
                textoCentrado("Cara lisa", 11, colCota, 0, -LY / 2.0 - 14));

        Group bloqueNerv = new Group();
        Box sombraNerv = crearSombraPlana(LX * 0.94, LY * 0.94, Hb / 2.0 + 4, colSombra);
        sombraNerv.setTranslateX(6); sombraNerv.setTranslateY(7);
        Group nervada = construirPiezaFrontal(LX, LY, Hb, Hn, ns, rs, nb, N,
                matLiso, matNerv, matBorde);
        bloqueNerv.getChildren().addAll(sombraNerv, nervada,
                cotaHorizontal(-LX / 2.0, LX / 2.0, LY / 2.0 + 18,
                        String.format("b = %.1f cm", anchoCm), colCota),
                cotaVertical(-LY / 2.0, LY / 2.0, LX / 2.0 + 20,
                        String.format("h = %.1f cm", largoCm), colCota, false),
                textoCentrado("Cara nervada", 11, colCota, 0, -LY / 2.0 - 14));

        double escCorte = 2.2;
        Group corteGeo = construirCorteFrontal(span, Hb, Hn, ns, rs, nb, N,
                matLiso, matNerv, matBorde, 10);
        corteGeo.setScaleX(escCorte);
        corteGeo.setScaleY(escCorte);
        corteGeo.setScaleZ(escCorte);

        double medioAltoCorte = (Hb / 2.0 + Hn) * escCorte;
        Group bloqueCorte = new Group(corteGeo,
                cotaHorizontal(-span * escCorte / 2.0, span * escCorte / 2.0,
                        (Hb / 2.0) * escCorte + 20,
                        String.format("%.1f cm", spanCm), colCota),
                textoCentrado("Corte lateral", 11, colCota, 0, -medioAltoCorte - 16));

        Group bloqueDet = construirDetalleNervios(Hb, Hn, ns, rs, nb, span,
                matLiso, matNerv, matBorde, colCota);

        double w1 = anchoDe(bloqueLisa),  h1 = altoDe(bloqueLisa);
        double w2 = anchoDe(bloqueNerv),  h2 = altoDe(bloqueNerv);
        double w3 = anchoDe(bloqueCorte), h3 = altoDe(bloqueCorte);
        double w4 = anchoDe(bloqueDet),   h4 = altoDe(bloqueDet);

        double colIzq = Math.max(w1, w3);
        double colDer = Math.max(w2, w4);
        double filaSup = Math.max(h1, h2);
        double filaInf = Math.max(h3, h4);

        double gapCol = Math.max(70, (colIzq + colDer) * 0.10);
        double gapFila = Math.max(60, (filaSup + filaInf) * 0.14);

        double xIzq = -(gapCol / 2.0 + colIzq / 2.0);
        double xDer = +(gapCol / 2.0 + colDer / 2.0);
        double ySup = -(gapFila / 2.0 + filaSup / 2.0);
        double yInf = +(gapFila / 2.0 + filaInf / 2.0);

        colocar(bloqueLisa,  xIzq, ySup);
        colocar(bloqueNerv,  xDer, ySup);
        colocar(bloqueCorte, xIzq, yInf);
        colocar(bloqueDet,   xDer, yInf);

        fichaGroup = new Group(bloqueLisa, bloqueNerv, bloqueCorte, bloqueDet);
        mundoFicha.getChildren().add(fichaGroup);

        ajustarCamaraFicha();
    }

    private static double anchoDe(Group g) { return g.getBoundsInParent().getWidth(); }
    private static double altoDe (Group g) { return g.getBoundsInParent().getHeight(); }

    private static void colocar(Group g, double cxDestino, double cyDestino) {
        Bounds b = g.getBoundsInParent();
        double cx = b.getMinX() + b.getWidth() / 2.0;
        double cy = b.getMinY() + b.getHeight() / 2.0;
        g.setTranslateX(g.getTranslateX() + (cxDestino - cx));
        g.setTranslateY(g.getTranslateY() + (cyDestino - cy));
    }

    private double[] patronNervios(double anchoPx, BaldosinCatalan bc, double sc) {
        int N = Math.max(0, bc.numNerviosInt);
        double nb = Math.max(0.02, bc.nervBorde)   * sc;
        double ns = Math.max(0.02, bc.nervCentral) * sc;
        double rs = Math.max(0.01, bc.ranura)      * sc;
        double Hn = Math.max(0.05, bc.altoNervio)  * sc;

        double total = 2 * nb + N * ns + (N + 1) * rs;
        if (total > 0.001 && Math.abs(total - anchoPx) / anchoPx > 0.001) {
            double k = anchoPx / total;
            nb *= k; ns *= k; rs *= k;
        }
        return new double[]{nb, ns, rs, Hn};
    }

    private void ajustarCamaraFicha() {
        if (fichaGroup == null || cameraFicha == null || subFicha == null) return;

        Bounds b = fichaGroup.getBoundsInParent();
        double w = b.getWidth(), h = b.getHeight();
        if (w <= 0 || h <= 0) return;

        double vw = subFicha.getWidth(), vh = subFicha.getHeight();
        if (vw <= 0 || vh <= 0) { vw = 560; vh = 580; }
        double aspect = vw / vh;

        double tan = Math.tan(Math.toRadians(cameraFicha.getFieldOfView() / 2.0));
        double distV = (h / 2.0) / tan;
        double distH = (w / 2.0) / (tan * aspect);
        double dist = Math.max(distV, distH) * 1.12;

        cameraFicha.setTranslateX(b.getMinX() + w / 2.0);
        cameraFicha.setTranslateY(b.getMinY() + h / 2.0);
        cameraFicha.setTranslateZ(-dist);
    }

    private Group construirPiezaOrientada(double ladoX, double ladoZ,
                                          double Hb, double Hn,
                                          double ns, double rs, double nb, int N,
                                          PhongMaterial matLiso, PhongMaterial matNerv,
                                          PhongMaterial matBorde, boolean nerviosEnX) {
        Group g = new Group();
        Box base = new Box(ladoX, Hb, ladoZ);
        base.setMaterial(matLiso);
        g.getChildren().add(base);

        double yN = Hb / 2.0 + Hn / 2.0;

        // Los nervios corren paralelos al lado mas largo y se reparten sobre el
        // lado mas corto llenandolo por completo: ranura = (lado - 2 bordes - N ns)/(N+1)
        boolean corrEnZ  = ladoZ >= ladoX;
        double largoNerv = corrEnZ ? ladoZ : ladoX;
        double anchoRep  = corrEnZ ? ladoX : ladoZ;

        double nbEf = Math.min(Math.max(nb, 0), anchoRep * 0.22);
        boolean hayBorde = nbEf > 0.12;
        double libre = anchoRep - (hayBorde ? 2.0 * nbEf : 0.0);
        double nsEf = ns, rsEf = 0;
        if (N > 0) {
            rsEf = (libre - N * nsEf) / (N + 1);
            double rsMin = Math.max(0.1, nsEf * 0.25);
            if (rsEf < rsMin) {
                nsEf = Math.max(0.1, (libre - (N + 1) * rsMin) / N);
                rsEf = (libre - N * nsEf) / (N + 1);
            }
        }

        double c = -anchoRep / 2.0;
        if (hayBorde) { g.getChildren().add(nerviBox(corrEnZ, c, nbEf, largoNerv, Hn, yN, matBorde)); c += nbEf; }
        for (int i = 0; i < N; i++) {
            c += rsEf;
            g.getChildren().add(nerviBox(corrEnZ, c, nsEf, largoNerv, Hn, yN, matNerv));
            c += nsEf;
        }
        c += rsEf;
        if (hayBorde) { g.getChildren().add(nerviBox(corrEnZ, c, nbEf, largoNerv, Hn, yN, matBorde)); }
        return g;
    }

    private static Box nerviBox(boolean corrEnZ, double c, double w,
                                double largoNerv, double Hn, double yN, PhongMaterial mat) {
        Box box = corrEnZ ? new Box(w, Hn, largoNerv) : new Box(largoNerv, Hn, w);
        if (corrEnZ) box.setTranslateX(c + w / 2.0);
        else         box.setTranslateZ(c + w / 2.0);
        box.setTranslateY(yN);
        box.setMaterial(mat);
        return box;
    }

    private Group construirPiezaFrontal(double ladoX, double ladoY, double Hb, double Hn,
                                        double ns, double rs, double nb, int N,
                                        PhongMaterial matLiso, PhongMaterial matNerv,
                                        PhongMaterial matBorde) {
        Group g = new Group();
        Box base = new Box(ladoX, ladoY, Hb);
        base.setMaterial(matLiso);
        g.getChildren().add(base);

        double zN = -(Hb / 2.0 + Hn / 2.0);
        // Nervios paralelos al lado mayor; se reparten sobre el menor (ns,rs,nb ya ajustados a ese lado)
        boolean corrEnY  = ladoY >= ladoX;
        double largoNerv = corrEnY ? ladoY : ladoX;
        double c = -(corrEnY ? ladoX : ladoY) / 2.0;

        if (nb > 0.12) { g.getChildren().add(nervFrontal(corrEnY, c, nb, largoNerv, Hn, zN, matBorde)); }
        c += nb;
        for (int i = 0; i < N; i++) {
            c += rs;
            g.getChildren().add(nervFrontal(corrEnY, c, ns, largoNerv, Hn, zN, matNerv));
            c += ns;
        }
        c += rs;
        if (nb > 0.12) { g.getChildren().add(nervFrontal(corrEnY, c, nb, largoNerv, Hn, zN, matBorde)); }
        return g;
    }

    private static Box nervFrontal(boolean corrEnY, double c, double w,
                                   double largoNerv, double Hn, double zN, PhongMaterial mat) {
        Box box = corrEnY ? new Box(w, largoNerv, Hn) : new Box(largoNerv, w, Hn);
        if (corrEnY) box.setTranslateX(c + w / 2.0);
        else         box.setTranslateY(c + w / 2.0);
        box.setTranslateZ(zN);
        box.setMaterial(mat);
        return box;
    }

    private Group construirCorteFrontal(double anchoCorte, double Hb, double Hn,
                                        double ns, double rs, double nb, int N,
                                        PhongMaterial matLiso, PhongMaterial matNerv,
                                        PhongMaterial matBorde, double prof) {
        Group g = new Group();
        Box cuerpo = new Box(anchoCorte, Hb, prof);
        cuerpo.setMaterial(matLiso);
        g.getChildren().add(cuerpo);

        double yN = -(Hb / 2.0 + Hn / 2.0);
        double xC = -anchoCorte / 2.0;

        if (nb > 0.12) {
            Box b1 = new Box(nb, Hn, prof);
            b1.setTranslateX(xC + nb / 2.0);
            b1.setTranslateY(yN);
            b1.setMaterial(matBorde);
            g.getChildren().add(b1);
        }
        xC += nb;
        for (int i = 0; i < N; i++) {
            xC += rs;
            Box n = new Box(ns, Hn, prof);
            n.setTranslateX(xC + ns / 2.0);
            n.setTranslateY(yN);
            n.setMaterial(matNerv);
            g.getChildren().add(n);
            xC += ns;
        }
        xC += rs;
        if (nb > 0.12) {
            Box b2 = new Box(nb, Hn, prof);
            b2.setTranslateX(xC + nb / 2.0);
            b2.setTranslateY(yN);
            b2.setMaterial(matBorde);
            g.getChildren().add(b2);
        }
        return g;
    }

    private WritableImage capturarFicha() {
        if (subFicha == null) return null;

        modoExport = true;
        subFicha.setFill(Color.WHITE);
        crearFichaEstatica();

        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.WHITE);
        sp.setTransform(new Scale(2, 2));
        WritableImage img = subFicha.snapshot(sp, null);

        modoExport = false;
        subFicha.setFill(C_FONDO3D);
        crearFichaEstatica();

        return img;
    }

    private void copiarFichaPNG() {
        WritableImage img = capturarFicha();
        if (img == null) return;
        ClipboardContent cc = new ClipboardContent();
        cc.putImage(img);
        Clipboard.getSystemClipboard().setContent(cc);
    }

    private void guardarFichaPNG() {
        WritableImage img = capturarFicha();
        if (img == null) return;

        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar ficha como PNG");
        fc.setInitialFileName("ficha_baldosin.png");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagen PNG", "*.png"));
        File f = fc.showSaveDialog(null);
        if (f == null) return;
        if (!f.getName().toLowerCase().endsWith(".png"))
            f = new File(f.getParentFile(), f.getName() + ".png");

        try {
            ImageIO.write(SwingFXUtils.fromFXImage(img, null), "png", f);
        } catch (Exception ex) {
            final String msg = ex.getMessage();
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                    "No se pudo guardar la imagen:\n" + msg, "Error", JOptionPane.ERROR_MESSAGE));
        }
    }

    private Text txtPlano(String s, double size, Color c) {
        Text t = new Text(s);
        t.setFont(Font.font("Segoe UI", FontWeight.BOLD, size));
        t.setFill(c);

        t.setTranslateZ(-5);
        return t;
    }

    private Box crearSombraPlana(double ancho, double alto, double zDetras, Color colorSombra) {
        Box s = new Box(ancho, alto, 0.6);
        s.setTranslateZ(zDetras);
        PhongMaterial m = new PhongMaterial();
        m.setDiffuseColor(colorSombra);
        m.setSpecularColor(Color.TRANSPARENT);
        s.setMaterial(m);
        return s;
    }

    private void animar365() {
        if (baldosaGroup == null) return;
        RotateTransition rt = new RotateTransition(Duration.seconds(3.5), baldosaGroup);
        rt.setAxis(Rotate.Y_AXIS);
        rt.setByAngle(365);
        rt.setCycleCount(1);
        rt.play();
    }

    private PhongMaterial matSolido(Color c) {
        PhongMaterial m = new PhongMaterial();
        m.setDiffuseColor(c);
        m.setSpecularColor(Color.color(1, 1, 1, 0.06));
        m.setSpecularPower(18);
        return m;
    }

    private PhongMaterial matPlano(Color c) {
        PhongMaterial m = new PhongMaterial();
        m.setDiffuseColor(c);
        m.setSpecularColor(Color.TRANSPARENT);
        return m;
    }

    private Color colorRealista(Color base) {
        double hue = base.getHue();
        double sat = Math.min(1.0, base.getSaturation() * 1.15 + 0.06);
        double bri = Math.max(0.38, base.getBrightness() * 0.88);
        double hueTerracota = 28.0;
        double hueAjustado = hue + (hueTerracota - hue) * 0.12;
        return Color.hsb(hueAjustado, sat, bri);
    }

    private Color masTerracota(Color base, double factor) {
        double hueTerracota = 22.0;
        double hue = base.getHue() + (hueTerracota - base.getHue()) * factor;
        double sat = Math.min(1.0, base.getSaturation() + 0.12);
        double bri = Math.max(0.30, base.getBrightness() * 0.82);
        return Color.hsb(hue, sat, bri);
    }

    private Group construirDetalleNervios(double Hb, double Hn,
                                          double ns, double rs, double nb,
                                          double anchoRef,
                                          PhongMaterial matLiso, PhongMaterial matNerv,
                                          PhongMaterial matBorde, Color colCota) {
        Group g = new Group();
        final double prof = 8;

        double patron = nb + rs + ns + rs + ns * 0.5;
        if (patron <= 0.01) return g;

        double k = (anchoRef * 0.70) / patron;
        k = Math.max(1.5, Math.min(k, 30.0));

        double nbE = nb * k, nsE = ns * k, rsE = rs * k, HnE = Hn * k, HbE = Hb * k;
        double anchoTotal = nbE + rsE + nsE + rsE + nsE * 0.5;
        double x0 = -anchoTotal / 2.0;

        Box base = new Box(anchoTotal, HbE, prof);
        base.setMaterial(matLiso);
        g.getChildren().add(base);

        double yN = -(HbE / 2.0 + HnE / 2.0);

        Box borde = new Box(nbE, HnE, prof);
        borde.setTranslateX(x0 + nbE / 2.0);
        borde.setTranslateY(yN);
        borde.setMaterial(matBorde);
        g.getChildren().add(borde);

        double xN1 = x0 + nbE + rsE;
        Box n1 = new Box(nsE, HnE, prof);
        n1.setTranslateX(xN1 + nsE / 2.0);
        n1.setTranslateY(yN);
        n1.setMaterial(matNerv);
        g.getChildren().add(n1);

        double xN2 = xN1 + nsE + rsE;
        Box n2 = new Box(nsE * 0.5, HnE, prof);
        n2.setTranslateX(xN2 + nsE * 0.25);
        n2.setTranslateY(yN);
        n2.setMaterial(matNerv);
        g.getChildren().add(n2);

        double yArriba = yN - HnE / 2.0;

        g.getChildren().add(cotaVertical(yArriba, yN + HnE / 2.0, x0 - 22,
                String.format("hn = %.1f mm", bc.altoNervio * 10), colCota, true));

        g.getChildren().add(cotaVertical(-HbE / 2.0, HbE / 2.0, x0 - 22,
                String.format("e = %.1f mm", bc.espesor * 10), colCota, true));

        g.getChildren().add(cotaHorizontal(xN1, xN1 + nsE, yArriba - 18,
                String.format("bn = %.1f mm", bc.nervCentral * 10), colCota));

        g.getChildren().add(textoCentrado("Detalle de nervios", 11, colCota,
                0, yArriba - 46));

        return g;
    }

    private Group cotaHorizontal(double x1, double x2, double y, String texto, Color col) {
        Group g = new Group();
        PhongMaterial m = matPlano(col);
        double largo = Math.abs(x2 - x1);
        double xm = (x1 + x2) / 2.0;
        double gr = 0.7, tope = 8;

        Box linea = new Box(largo, gr, gr);
        linea.setTranslateX(xm);
        linea.setTranslateY(y);
        linea.setMaterial(m);
        g.getChildren().add(linea);

        for (double x : new double[]{x1, x2}) {
            Box t = new Box(gr, tope, gr);
            t.setTranslateX(x);
            t.setTranslateY(y);
            t.setMaterial(m);
            g.getChildren().add(t);
        }

        g.getChildren().add(textoCentrado(texto, 9, col, xm, y - 6));

        g.setDepthTest(DepthTest.DISABLE);
        return g;
    }

    private Group cotaVertical(double y1, double y2, double x, String texto,
                               Color col, boolean textoALaIzquierda) {
        Group g = new Group();
        PhongMaterial m = matPlano(col);
        double largo = Math.abs(y2 - y1);
        double ym = (y1 + y2) / 2.0;
        double gr = 0.7, tope = 8;

        Box linea = new Box(gr, largo, gr);
        linea.setTranslateX(x);
        linea.setTranslateY(ym);
        linea.setMaterial(m);
        g.getChildren().add(linea);

        for (double y : new double[]{y1, y2}) {
            Box t = new Box(tope, gr, gr);
            t.setTranslateX(x);
            t.setTranslateY(y);
            t.setMaterial(m);
            g.getChildren().add(t);
        }

        Text t = txtPlano(texto, 9, col);
        double w = t.getLayoutBounds().getWidth();
        t.setTranslateX(textoALaIzquierda ? (x - 8 - w) : (x + 8));
        t.setTranslateY(ym + 3);
        g.getChildren().add(t);

        g.setDepthTest(DepthTest.DISABLE);
        return g;
    }

    private Group textoCentrado(String s, double size, Color col, double cx, double y) {
        Text t = txtPlano(s, size, col);
        double w = t.getLayoutBounds().getWidth();
        t.setTranslateX(cx - w / 2.0);
        t.setTranslateY(y);
        Group g = new Group(t);
        g.setDepthTest(DepthTest.DISABLE);
        return g;
    }

    private Color colorDesdeHex(String hex) {
        try { return Color.web(hex != null ? hex : "#C1622A"); }
        catch (Exception e) { return Color.web("#C1622A"); }
    }

    private void actualizarDesdeModelo() {
        Platform.runLater(() -> {
            if (lblClasif != null) lblClasif.setText(tituloPieza());
            crearPieza3D();
            crearFichaEstatica();
        });
    }

    private String tituloPieza() {
        String n = datos.nombreFinal;
        if (n == null || n.isBlank() || n.startsWith("Sin")) return "Baldosín";
        return "Baldosín — " + n;
    }

    public VentanaMezclas getMezclas() { return ventanaMezclas; }

    private JButton mkBtn(String t, java.awt.Color bg, java.awt.Color fg) {
        return Tema.boton(t, Tema.Tipo.SECUNDARIO);
    }

    private static Label fx(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-text-fill:" + FX_MUTED + ";-fx-font-family:'Segoe UI';-fx-font-size:11;");
        return l;
    }

    private static TextField fxF(String v) {
        TextField f = new TextField(v);
        f.setPrefWidth(52);
        f.setStyle("-fx-background-color:" + FX_PANEL2 + ";-fx-text-fill:" + FX_TEXTO
                 + ";-fx-font-family:'Segoe UI';-fx-font-size:11;-fx-border-color:" + FX_BORDE
                 + ";-fx-border-radius:3;-fx-background-radius:3;");
        return f;
    }
}