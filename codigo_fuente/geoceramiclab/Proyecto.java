package geoceramiclab;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;

public final class Proyecto {

    private Proyecto() { }

    public static File archivo;

    public static final String EXT = "gcl";
    private static final Preferences PREFS =
        Preferences.userNodeForPackage(geoceramiclab.modelo3d.DialogoInicio.class);

    public static File carpeta() {
        return new File(PREFS.get("carpetaProyecto",
                new File(System.getProperty("user.home"), "SDIM").getAbsolutePath()));
    }

    public static void anotarReciente(File f) {
        List<String> rs = new ArrayList<>();
        for (String s : PREFS.get("archivosRecientes", "").split("\n")) if (!s.isBlank()) rs.add(s);
        rs.remove(f.getAbsolutePath());
        rs.add(0, f.getAbsolutePath());
        while (rs.size() > 6) rs.remove(rs.size() - 1);
        PREFS.put("archivosRecientes", String.join("\n", rs));
    }

    public static Properties leer(File f) throws IOException {
        Properties p = new Properties();
        if (f != null && f.isFile()) try (InputStream in = new FileInputStream(f)) { p.load(in); }
        return p;
    }

    public static void escribir(File f, Properties p) throws IOException {
        if (f.getParentFile() != null) f.getParentFile().mkdirs();

        Properties ord = new Properties() {
            @Override public synchronized Set<Map.Entry<Object, Object>> entrySet() {
                TreeMap<Object, Object> t = new TreeMap<>((a, b) -> a.toString().compareTo(b.toString()));
                t.putAll(p);
                return Collections.synchronizedSet(t.entrySet());
            }
        };
        ord.putAll(p);
        try (OutputStream o = new FileOutputStream(f)) { ord.store(o, "SDIM - estudio"); }
    }

    private static final class Cuenta { int tabla, texto, lista, marca, giro; }

    public static void capturar(Component raiz, String pre, Properties p) {
        recorrer(raiz, pre, p, new Cuenta(), true);
    }

    public static void restaurar(Component raiz, String pre, Properties p) {
        recorrer(raiz, pre, p, new Cuenta(), false);
    }

    private static void recorrer(Component c, String pre, Properties p, Cuenta n, boolean guardar) {
        if (c instanceof JTable) {
            tabla((JTable) c, pre + "tabla." + (n.tabla++) + ".", p, guardar);
            return;
        }
        if (c instanceof JSpinner) {
            String k = pre + "giro." + (n.giro++);
            JSpinner s = (JSpinner) c;
            if (guardar) p.setProperty(k, String.valueOf(s.getValue()));
            else if (p.containsKey(k)) {
                try {
                    Object v = s.getValue();
                    String t = p.getProperty(k);
                    if (v instanceof Integer) s.setValue(Integer.parseInt(t));
                    else if (v instanceof Double) s.setValue(Double.parseDouble(t));
                } catch (Exception ignorar) { }
            }
            return;
        }
        if (c instanceof JComboBox) {
            String k = pre + "lista." + (n.lista++);
            JComboBox<?> cb = (JComboBox<?>) c;
            if (guardar) p.setProperty(k, String.valueOf(cb.getSelectedIndex()));
            else if (p.containsKey(k)) {
                try {
                    int i = Integer.parseInt(p.getProperty(k));
                    if (i >= 0 && i < cb.getItemCount() && i != cb.getSelectedIndex()) cb.setSelectedIndex(i);
                } catch (Exception ignorar) { }
            }
            return;
        }
        if (c instanceof JTextField) {

            String k = pre + "texto." + (n.texto++);
            JTextField f = (JTextField) c;
            if (!f.isEditable()) return;
            if (guardar) p.setProperty(k, f.getText());
            else if (p.containsKey(k) && !p.getProperty(k).equals(f.getText())) f.setText(p.getProperty(k));
            return;
        }
        if (c instanceof JToggleButton) {
            String k = pre + "marca." + (n.marca++);
            JToggleButton b = (JToggleButton) c;
            if (guardar) p.setProperty(k, String.valueOf(b.isSelected()));
            else if (p.containsKey(k)) {
                boolean v = Boolean.parseBoolean(p.getProperty(k));
                if (v != b.isSelected() && (v || b instanceof JCheckBox)) b.doClick(0);
            }
            return;
        }
        if (c instanceof Container) {
            for (Component h : ((Container) c).getComponents()) recorrer(h, pre, p, n, guardar);
        }
    }

    private static void tabla(JTable t, String k, Properties p, boolean guardar) {
        TableModel m = t.getModel();
        if (guardar) {
            if (t.isEditing()) t.getCellEditor().stopCellEditing();
            p.setProperty(k + "filas", String.valueOf(m.getRowCount()));
            for (int r = 0; r < m.getRowCount(); r++)
                for (int c = 0; c < m.getColumnCount(); c++) {
                    Object v = m.getValueAt(r, c);
                    if (v != null) p.setProperty(k + r + "." + c, codificar(v));
                }
            return;
        }
        if (!p.containsKey(k + "filas")) return;
        int filas;
        try { filas = Integer.parseInt(p.getProperty(k + "filas")); } catch (Exception e) { return; }
        if (m instanceof DefaultTableModel) ((DefaultTableModel) m).setRowCount(filas);
        filas = Math.min(filas, m.getRowCount());
        for (int r = 0; r < filas; r++)
            for (int c = 0; c < m.getColumnCount(); c++) {
                String v = p.getProperty(k + r + "." + c);
                if (v != null) {
                    try { m.setValueAt(decodificar(v), r, c); } catch (Exception ignorar) { }
                }
            }
    }

    private static String codificar(Object v) {
        if (v instanceof Boolean) return "b:" + v;
        if (v instanceof Integer) return "i:" + v;
        if (v instanceof Double)  return "d:" + v;
        return "s:" + v;
    }

    private static Object decodificar(String s) {
        if (s.length() < 2 || s.charAt(1) != ':') return s;
        String v = s.substring(2);
        switch (s.charAt(0)) {
            case 'b': return Boolean.parseBoolean(v);
            case 'i': try { return Integer.parseInt(v); } catch (Exception e) { return v; }
            case 'd': try { return Double.parseDouble(v); } catch (Exception e) { return v; }
            default:  return v;
        }
    }

    public static boolean pulsar(Component raiz, String texto) {
        if (raiz instanceof AbstractButton && texto.equals(((AbstractButton) raiz).getText())) {
            ((AbstractButton) raiz).doClick(0);
            return true;
        }
        if (raiz instanceof Container)
            for (Component h : ((Container) raiz).getComponents()) if (pulsar(h, texto)) return true;
        return false;
    }
}
