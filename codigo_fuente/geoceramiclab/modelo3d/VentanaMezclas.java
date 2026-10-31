package geoceramiclab.modelo3d;

import geoceramiclab.model.SueloData;

public class VentanaMezclas {

    private final MezclasPanel   mezclasPanel;
    private final SecadoCoccion  secadoPanel;
    private final EnsayosNTC     ensayosPanel;

    private Runnable alMostrar;

    public VentanaMezclas(SueloData datos) {
        ensayosPanel = new EnsayosNTC();
        secadoPanel  = new SecadoCoccion();
        mezclasPanel = new MezclasPanel(datos);
        mezclasPanel.setEnsayosPanel(ensayosPanel);
        mezclasPanel.setSecadoCoccionPanel(secadoPanel);
    }

    public MezclasPanel  getMezclasPanel() { return mezclasPanel; }
    public SecadoCoccion getSecadoPanel()  { return secadoPanel; }
    public EnsayosNTC    getEnsayosPanel() { return ensayosPanel; }

    public void setAlMostrar(Runnable r) { this.alMostrar = r; }

    public void setDimensiones(double a, double l, double e, int n) {
        mezclasPanel.setDimensiones(a, l, e, n);
    }

    public void mostrar() {
        if (alMostrar != null) alMostrar.run();
    }
}
