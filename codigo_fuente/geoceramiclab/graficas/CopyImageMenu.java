package geoceramiclab.graficas;

import geoceramiclab.Tema;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class CopyImageMenu {

    public static void instalar(JPanel panel, String nombre) {
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e)  { check(e); }
            @Override
            public void mouseReleased(MouseEvent e) { check(e); }

            void check(MouseEvent e) {
                if (e.isPopupTrigger()) mostrarMenu(e, panel, nombre);
            }
        });
    }

    public static void instalar(JPanel panel) {
        instalar(panel, panel.getClass().getSimpleName());
    }

    static BufferedImage renderPanel(JPanel panel) {
        int w = Math.max(1, panel.getWidth());
        int h = Math.max(1, panel.getHeight());

        int scale = 2;
        BufferedImage img = new BufferedImage(w * scale, h * scale,
                                              BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.scale(scale, scale);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,     RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        panel.paint(g);
        g.dispose();
        return img;
    }

    static void mostrarMenu(MouseEvent e, JPanel panel, String nombre) {
        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(new Color(22, 28, 44));
        menu.setBorder(BorderFactory.createLineBorder(new Color(60, 80, 130)));

        JMenuItem itemCopiar = menuItem("Copiar imagen (PNG)");
        itemCopiar.addActionListener(ae -> {
            BufferedImage img = renderPanel(panel);
            Toolkit.getDefaultToolkit().getSystemClipboard()
                   .setContents(new ImageSelection(img), null);
            mostrarToast(panel, "Imagen copiada al portapapeles");
        });
        menu.add(itemCopiar);

        menu.addSeparator();

        JMenuItem itemGuardar = menuItem("Guardar como PNG...");
        itemGuardar.addActionListener(ae -> {
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Guardar imagen — " + nombre);
            String fn = nombre.replaceAll("[^a-zA-Z0-9\\-_]", "_") + ".png";
            fc.setSelectedFile(new File(fn));
            fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "PNG (*.png)", "png"));
            if (fc.showSaveDialog(panel) == JFileChooser.APPROVE_OPTION) {
                File out = fc.getSelectedFile();
                if (!out.getName().toLowerCase().endsWith(".png"))
                    out = new File(out.getPath() + ".png");
                try {
                    ImageIO.write(renderPanel(panel), "PNG", out);
                    mostrarToast(panel, "Guardado: " + out.getName());
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(panel,
                        "Error al guardar:\n" + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        menu.add(itemGuardar);

        menu.show(panel, e.getX(), e.getY());
    }

    static void mostrarToast(JPanel parent, String msg) {
        JWindow toast = new JWindow(SwingUtilities.getWindowAncestor(parent));
        JLabel lbl = new JLabel("  " + msg + "  ");
        lbl.setFont(Tema.ui(Font.BOLD, 12));
        lbl.setForeground(Color.WHITE);
        lbl.setBackground(new Color(30, 60, 140, 220));
        lbl.setOpaque(true);
        lbl.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(80, 140, 240)),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)));
        toast.add(lbl);
        toast.pack();

        Point p = parent.getLocationOnScreen();
        toast.setLocation(p.x + parent.getWidth()/2 - toast.getWidth()/2,
                          p.y + parent.getHeight()/2 - toast.getHeight()/2);
        toast.setVisible(true);

        Timer t = new Timer(1800, ae -> toast.dispose());
        t.setRepeats(false); t.start();
    }

    static JMenuItem menuItem(String txt) {
        JMenuItem item = new JMenuItem(txt);
        item.setFont(Tema.ui(Font.PLAIN, 12));
        item.setBackground(new Color(22, 28, 44));
        item.setForeground(new Color(200, 220, 255));
        item.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 16));
        item.setOpaque(true);
        item.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                item.setBackground(new Color(40, 65, 140)); }
            public void mouseExited(MouseEvent e) {
                item.setBackground(new Color(22, 28, 44)); }
        });
        return item;
    }

    static class ImageSelection implements Transferable {
        private final BufferedImage img;
        ImageSelection(BufferedImage img) { this.img = img; }
        @Override public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{ DataFlavor.imageFlavor }; }
        @Override public boolean isDataFlavorSupported(DataFlavor f) {
            return DataFlavor.imageFlavor.equals(f); }
        @Override public Object getTransferData(DataFlavor f)
                throws UnsupportedFlavorException {
            if (!DataFlavor.imageFlavor.equals(f)) throw new UnsupportedFlavorException(f);
            return img;
        }
    }
}
