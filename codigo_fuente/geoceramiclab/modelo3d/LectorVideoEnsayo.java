package geoceramiclab.modelo3d;

import net.sourceforge.tess4j.Tesseract;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntConsumer;

public final class LectorVideoEnsayo {

    private LectorVideoEnsayo() { }

    public static final class Resultado {
        public final List<double[]> puntos = new ArrayList<>();

        public final List<double[]> puntosVideo = new ArrayList<>();
        public double duracion;
        public double fMaxKN;
        public double tMax;
        public double t0;
        public double velocidadKNs;
        public int leidos, descartados;
        public int decimales;
        public final List<String> textosOcr = new ArrayList<>();

        public final List<double[]> zonas = new ArrayList<>();

        public Rectangle zonaEn(double t) {
            if (zonas.isEmpty()) return null;
            double[] mejor = zonas.get(0);
            for (double[] z : zonas) if (Math.abs(z[0] - t) < Math.abs(mejor[0] - t)) mejor = z;
            return new Rectangle((int) mejor[1], (int) mejor[2], (int) mejor[3], (int) mejor[4]);
        }
    }

    public static final class Evaluacion {
        public final String texto;
        public final boolean cumple;
        Evaluacion(String texto, boolean cumple) { this.texto = texto; this.cumple = cumple; }
    }

    public static BufferedImage fotograma(File video, double segundo) throws Exception {
        FFmpegFrameGrabber g = new FFmpegFrameGrabber(video);
        try {
            g.start();
            if (segundo > 0) g.setTimestamp((long) (segundo * 1_000_000));
            Frame f = g.grabImage();
            if (f == null) {
                g.setTimestamp(0);
                f = g.grabImage();
            }
            if (f == null) throw new IllegalStateException("El video no tiene imágenes legibles.");
            return copiar(new Java2DFrameConverter().convert(f));
        } finally {
            g.stop();
            g.release();
        }
    }

    public static Rectangle detectarZona(BufferedImage img) {
        int W = img.getWidth(), H = img.getHeight();
        int[] rgb = img.getRGB(0, 0, W, H, null, 0, W);

        int[] xs = new int[W], ys = new int[H];
        int nAzul = 0;
        for (int y = 0; y < H; y++)
            for (int x = 0; x < W; x++) {
                int c = rgb[y * W + x], r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
                if (b > r + 50 && b > g + 40 && b > 120) { xs[x]++; ys[y]++; nAzul++; }
            }
        if (nAzul < W * H / 200) return null;
        int x0 = percentil(xs, nAzul, 0.02), x1 = percentil(xs, nAzul, 0.98);
        int y0 = percentil(ys, nAzul, 0.02), y1 = percentil(ys, nAzul, 0.98);
        int lw = x1 - x0, lh = y1 - y0;
        if (lw < 40 || lh < 30) return null;

        int[] gris = new int[lw * lh];
        int[] hist = new int[256];
        for (int y = 0; y < lh; y++)
            for (int x = 0; x < lw; x++) {
                int c = rgb[(y0 + y) * W + x0 + x];
                int v = (int) (0.299 * ((c >> 16) & 255) + 0.587 * ((c >> 8) & 255) + 0.114 * (c & 255));
                gris[y * lw + x] = v;
                hist[v]++;
            }
        int umbral = otsu(hist, gris.length);

        boolean[] visto = new boolean[gris.length];
        List<int[]> manchas = new ArrayList<>();
        int[] pila = new int[gris.length];
        for (int i = 0; i < gris.length; i++) {
            if (visto[i] || gris[i] > umbral) continue;
            int top = 0, mx0 = lw, my0 = lh, mx1 = -1, my1 = -1;
            pila[top++] = i; visto[i] = true;
            while (top > 0) {
                int p = pila[--top], px = p % lw, py = p / lw;
                if (px < mx0) mx0 = px; if (px > mx1) mx1 = px;
                if (py < my0) my0 = py; if (py > my1) my1 = py;
                int[] vec = {p - 1, p + 1, p - lw, p + lw};
                for (int k = 0; k < 4; k++) {
                    int q = vec[k];
                    if (q < 0 || q >= gris.length) continue;
                    if ((k == 0 && px == 0) || (k == 1 && px == lw - 1)) continue;
                    if (!visto[q] && gris[q] <= umbral) { visto[q] = true; pila[top++] = q; }
                }
            }
            int h = my1 - my0 + 1, w = mx1 - mx0 + 1;
            boolean tocaBorde = mx0 == 0 || my0 == 0 || mx1 == lw - 1 || my1 == lh - 1;
            if (!tocaBorde && h >= 0.12 * lh && h <= 0.45 * lh && w <= 0.16 * lw)
                manchas.add(new int[]{mx0, my0, mx1 + 1, my1 + 1});
        }
        manchas.sort((a, b) -> Integer.compare(a[0], b[0]));

        List<int[]> mejor = new ArrayList<>();
        int altoMejor = 0;
        for (int i = 0; i < manchas.size(); i++) {
            List<int[]> fila = new ArrayList<>();
            fila.add(manchas.get(i));
            for (int j = i + 1; j < manchas.size(); j++) {
                int[] u = fila.get(fila.size() - 1), d = manchas.get(j);
                int hh = u[3] - u[1];
                int solape = Math.min(u[3], d[3]) - Math.max(u[1], d[1]);
                if (solape > 0.6 * hh && d[0] - u[2] < 1.2 * hh
                        && Math.abs((d[3] - d[1]) - hh) < 0.25 * hh) fila.add(d);
            }
            if (fila.size() < 4 || fila.size() > 9) continue;
            int alto = 0;
            for (int[] m : fila) alto += m[3] - m[1];
            alto /= fila.size();

            boolean masAlta = alto > altoMejor * 1.12;
            boolean parecida = alto >= altoMejor * 0.88;
            if (masAlta || (parecida && fila.size() > mejor.size())) {
                mejor = fila;
                altoMejor = alto;
            }
        }
        if (mejor.size() < 4) return null;

        int bx0 = Integer.MAX_VALUE, by0 = Integer.MAX_VALUE, bx1 = 0, by1 = 0;
        for (int[] m : mejor) {
            bx0 = Math.min(bx0, m[0]); by0 = Math.min(by0, m[1]);
            bx1 = Math.max(bx1, m[2]); by1 = Math.max(by1, m[3]);
        }
        int mh = by1 - by0, mw = bx1 - bx0;
        Rectangle z = new Rectangle(x0 + bx0 - (int) (0.15 * mh), y0 + by0 - (int) (0.15 * mh),
                                    mw + (int) (0.30 * mh), mh + (int) (0.30 * mh));
        return z.intersection(new Rectangle(0, 0, W, H));
    }

    static Rectangle detectarRapido(BufferedImage img) {
        double k = Math.min(1.0, 960.0 / img.getWidth());
        if (k >= 1.0) return detectarZona(img);
        int w = (int) (img.getWidth() * k), h = (int) (img.getHeight() * k);
        BufferedImage peq = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = peq.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img, 0, 0, w, h, null);
        g.dispose();
        Rectangle z = detectarZona(peq);
        if (z == null) return null;
        Rectangle r = new Rectangle((int) (z.x / k), (int) (z.y / k), (int) (z.width / k), (int) (z.height / k));
        return r.intersection(new Rectangle(0, 0, img.getWidth(), img.getHeight()));
    }

    public static boolean zonaValida(BufferedImage img, Rectangle z, String rutaTessdata) {
        try {
            Tesseract ocr = new Tesseract();
            ocr.setDatapath(rutaTessdata);
            ocr.setLanguage("eng");
            ocr.setPageSegMode(7);
            ocr.setVariable("tessedit_char_whitelist", "0123456789.");
            String s = ocr.doOCR(preparar(img.getSubimage(z.x, z.y, z.width, z.height)))
                          .replaceAll("[^0-9.]", "");
            int nd = s.replace(".", "").length();
            return s.contains(".") && nd >= 4 && nd <= 8;
        } catch (Throwable e) {
            return false;
        }
    }

    private static int percentil(int[] cuenta, int total, double q) {
        long objetivo = Math.round(q * total), acum = 0;
        for (int i = 0; i < cuenta.length; i++) { acum += cuenta[i]; if (acum >= objetivo) return i; }
        return cuenta.length - 1;
    }

    private static int otsu(int[] hist, int total) {
        long suma = 0;
        for (int i = 0; i < 256; i++) suma += (long) i * hist[i];
        long sumB = 0, wB = 0;
        double maxVar = -1;
        int umbral = 128;
        for (int i = 0; i < 256; i++) {
            wB += hist[i];
            if (wB == 0) continue;
            long wF = total - wB;
            if (wF == 0) break;
            sumB += (long) i * hist[i];
            double mB = (double) sumB / wB, mF = (double) (suma - sumB) / wF;
            double var = (double) wB * wF * (mB - mF) * (mB - mF);
            if (var > maxVar) { maxVar = var; umbral = i; }
        }
        return umbral;
    }

    public static Rectangle elegirZona(Component padre, BufferedImage img) {
        Window w = SwingUtilities.getWindowAncestor(padre);
        JDialog dlg = new JDialog(w, "Marque el número de la carga (kN)",
                                  Dialog.ModalityType.APPLICATION_MODAL);
        SelectorZona sel = new SelectorZona(img);
        final Rectangle[] res = {null};

        JButton ok = new JButton("Aceptar");
        JButton cancelar = new JButton("Cancelar");
        ok.addActionListener(e -> { res[0] = sel.zonaEnImagen(); dlg.dispose(); });
        cancelar.addActionListener(e -> dlg.dispose());

        JPanel sur = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        sur.add(new JLabel("Arrastre un rectángulo que encierre SOLO los dígitos de la carga (ej. 008.838)."));
        sur.add(cancelar);
        sur.add(ok);

        dlg.setLayout(new BorderLayout());
        dlg.add(sel, BorderLayout.CENTER);
        dlg.add(sur, BorderLayout.SOUTH);
        dlg.setSize(1000, 700);
        dlg.setLocationRelativeTo(padre);
        dlg.setVisible(true);

        Rectangle z = res[0];
        return (z != null && z.width > 5 && z.height > 5) ? z : null;
    }

    private static final class SelectorZona extends JPanel {
        private final BufferedImage img;
        private Point a, b;
        private double esc = 1;
        private int ox, oy;

        SelectorZona(BufferedImage img) {
            this.img = img;
            setBackground(Color.DARK_GRAY);
            MouseAdapter m = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) { a = e.getPoint(); b = a; repaint(); }
                @Override public void mouseDragged(MouseEvent e) { b = e.getPoint(); repaint(); }
            };
            addMouseListener(m);
            addMouseMotionListener(m);
        }

        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            esc = Math.min((double) getWidth() / img.getWidth(), (double) getHeight() / img.getHeight());
            int w = (int) (img.getWidth() * esc), h = (int) (img.getHeight() * esc);
            ox = (getWidth() - w) / 2;
            oy = (getHeight() - h) / 2;
            g.drawImage(img, ox, oy, w, h, null);
            if (a != null && b != null) {
                Rectangle r = rect();
                g.setColor(new Color(255, 0, 0, 60));
                g.fill(r);
                g.setColor(Color.RED);
                g.setStroke(new BasicStroke(2f));
                g.draw(r);
            }
        }

        private Rectangle rect() {
            return new Rectangle(Math.min(a.x, b.x), Math.min(a.y, b.y),
                                 Math.abs(a.x - b.x), Math.abs(a.y - b.y));
        }

        Rectangle zonaEnImagen() {
            if (a == null || b == null) return null;
            Rectangle r = rect();
            Rectangle z = new Rectangle((int) ((r.x - ox) / esc), (int) ((r.y - oy) / esc),
                                        (int) (r.width / esc), (int) (r.height / esc));
            return z.intersection(new Rectangle(0, 0, img.getWidth(), img.getHeight()));
        }
    }

    public static Resultado procesar(File video, Rectangle zona, String rutaTessdata,
                                     double lecturasPorSegundo, IntConsumer progreso) throws Exception {
        Tesseract ocr = new Tesseract();
        ocr.setDatapath(rutaTessdata);
        ocr.setLanguage("eng");
        ocr.setPageSegMode(7);
        ocr.setVariable("tessedit_char_whitelist", "0123456789.");

        Resultado r = new Resultado();
        List<double[]> crudos = new ArrayList<>();
        List<Double> tiempos = new ArrayList<>();
        List<String> textos = new ArrayList<>();
        Rectangle actual = new Rectangle(zona);

        FFmpegFrameGrabber g = new FFmpegFrameGrabber(video);
        Java2DFrameConverter conv = new Java2DFrameConverter();
        try {
            g.start();
            double fps = g.getFrameRate() > 0 ? g.getFrameRate() : 30;
            int salto = Math.max(1, (int) Math.round(fps / lecturasPorSegundo));
            int total = Math.max(1, g.getLengthInVideoFrames());
            int n = 0;
            Frame f;
            while ((f = g.grabImage()) != null) {
                if (n++ % salto != 0) continue;
                if (progreso != null) progreso.accept(Math.min(99, n * 100 / total));

                BufferedImage img = conv.convert(f);
                double ts = g.getTimestamp() / 1e6;

                Rectangle det = detectarRapido(img);
                if (det != null && Math.abs(det.width - zona.width) < 0.4 * zona.width
                        && Math.abs(det.height - zona.height) < 0.4 * zona.height) {
                    actual = det;
                }
                Rectangle z = actual.intersection(new Rectangle(0, 0, img.getWidth(), img.getHeight()));
                if (z.isEmpty()) continue;

                String txt = ocr.doOCR(preparar(img.getSubimage(z.x, z.y, z.width, z.height)));
                tiempos.add(ts);
                textos.add(txt == null ? "" : txt);
                r.zonas.add(new double[]{ts, z.x, z.y, z.width, z.height});
            }
        } finally {
            g.stop();
            g.release();
        }

        int dec = decimalesPantalla(textos);

        int[] cuentaDig = new int[16];
        for (String t : textos) {
            int nd = t.replaceAll("[^0-9]", "").length();
            if (nd >= 3 && nd < cuentaDig.length) cuentaDig[nd]++;
        }
        int digitos = 0;
        for (int d = 3; d < cuentaDig.length; d++) if (cuentaDig[d] > cuentaDig[digitos]) digitos = d;

        for (int i = 0; i < textos.size(); i++) {
            int nd = textos.get(i).replaceAll("[^0-9]", "").length();
            if (digitos > 0 && nd != digitos) { r.descartados++; continue; }
            Double v = numero(textos.get(i), dec);
            if (v == null || v < 0 || v > 2000) { r.descartados++; continue; }
            crudos.add(new double[]{tiempos.get(i), v});
        }
        r.decimales = dec;
        r.textosOcr.addAll(textos);
        guardarLecturas(video, tiempos, textos, dec);

        filtrar(crudos, r);
        analizar(r);
        if (progreso != null) progreso.accept(100);
        return r;
    }

    static void guardarLecturas(File video, List<Double> tiempos, List<String> textos, int dec) {
        String nombre = video.getName().replaceAll("\\.[^.]+$", "") + "_lecturas.csv";
        File csv = new File(video.getParentFile(), nombre);
        try (java.io.PrintWriter w = new java.io.PrintWriter(csv, "UTF-8")) {
            w.println("tiempo_s;texto_ocr;carga_kN");
            for (int i = 0; i < textos.size(); i++) {
                Double v = numero(textos.get(i), dec);
                w.printf(java.util.Locale.ROOT, "%s;%s;%s%n",
                         String.format(java.util.Locale.ROOT, "%.3f", tiempos.get(i)).replace('.', ','),
                         textos.get(i).trim().replace(";", ","), v == null ? "" : String.valueOf(v).replace('.', ','));
            }
        } catch (Exception ignorar) { }
    }

    static int decimalesPantalla(List<String> textos) {
        int[] cuenta = new int[8];
        for (String t : textos) {
            String s = t.replaceAll("[^0-9.]", "");
            int p = s.indexOf('.');
            if (p < 0) continue;
            int d = s.substring(p + 1).replace(".", "").length();
            if (d >= 1 && d < cuenta.length) cuenta[d]++;
        }
        int mejor = 3;
        for (int d = 1; d < cuenta.length; d++) if (cuenta[d] > cuenta[mejor]) mejor = d;
        return mejor;
    }

    static Double numero(String txt, int decimales) {
        if (txt == null) return null;
        String s = txt.replaceAll("[^0-9.]", "");
        int p = s.indexOf('.');
        if (p >= 0) {
            s = s.substring(0, p + 1) + s.substring(p + 1).replace(".", "");
        } else if (s.length() > decimales) {
            s = s.substring(0, s.length() - decimales) + "." + s.substring(s.length() - decimales);
        }
        if (s.isEmpty() || s.equals(".")) return null;
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return null; }
    }

    static void filtrar(List<double[]> crudos, Resultado r) {
        if (crudos.isEmpty()) { r.leidos = 0; return; }

        int n0 = Math.min(7, crudos.size());
        double[] ini = new double[n0];
        for (int i = 0; i < n0; i++) ini[i] = crudos.get(i)[1];
        Arrays.sort(ini);
        double ultimo = ini[n0 / 2];

        for (int i = 0; i < crudos.size(); i++) {
            double v = crudos.get(i)[1];
            double tol = Math.max(0.25, 0.08 * Math.max(ultimo, v));
            boolean acepta;
            if (Math.abs(v - ultimo) <= tol) {
                acepta = true;
            } else {

                int conf = 0, vistos = 0;
                for (int j = i + 1; j < crudos.size() && vistos < 2; j++, vistos++) {
                    double w = crudos.get(j)[1];
                    if (Math.abs(w - v) <= Math.max(0.25, 0.08 * Math.max(w, v))) conf++;
                }
                acepta = vistos > 0 && conf == vistos;
            }
            if (acepta) { r.puntos.add(crudos.get(i)); ultimo = v; }
            else r.descartados++;
        }
        r.leidos = r.puntos.size();
    }

    static void analizar(Resultado r) {
        if (r.puntos.isEmpty()) return;

        int iMax = 0;
        for (int i = 1; i < r.puntos.size(); i++)
            if (r.puntos.get(i)[1] > r.puntos.get(iMax)[1]) iMax = i;
        r.fMaxKN = r.puntos.get(iMax)[1];
        double tPico = r.puntos.get(iMax)[0];

        double t0 = r.puntos.get(0)[0];
        for (int i = 0; i <= iMax; i++) {
            if (r.puntos.get(i)[1] >= 0.02 * r.fMaxKN) {
                t0 = r.puntos.get(Math.max(0, i - 1))[0];
                break;
            }
        }
        List<double[]> desplazados = new ArrayList<>();
        r.puntosVideo.clear();
        for (double[] p : r.puntos) r.puntosVideo.add(new double[]{p[0], p[1]});
        r.duracion = r.puntos.get(r.puntos.size() - 1)[0];
        for (double[] p : r.puntos)
            if (p[0] >= t0) desplazados.add(new double[]{p[0] - t0, p[1]});
        r.puntos.clear();
        r.puntos.addAll(desplazados);
        r.tMax = tPico - t0;
        r.t0 = t0;
        r.leidos = r.puntos.size();

        double sx = 0, sy = 0, sxx = 0, sxy = 0;
        int n = 0;
        for (double[] p : r.puntos) {
            if (p[0] > r.tMax) break;
            double u = p[1] / r.fMaxKN;
            if (u >= 0.2 && u <= 0.8) {
                sx += p[0]; sy += p[1]; sxx += p[0] * p[0]; sxy += p[0] * p[1]; n++;
            }
        }
        double den = n * sxx - sx * sx;
        r.velocidadKNs = (n >= 2 && den != 0) ? (n * sxy - sx * sy) / den : Double.NaN;
    }

    public static volatile boolean EXTRUIDO = false;

    // NTC 919 / ISO 13006: grupo por absorcion y minimos de S (fuerza de rotura, N)
    // y R (modulo de rotura, MPa). Piezas prensadas = grupos B.
    public static Evaluacion evaluarNorma(double S, double R, double espesorMm, double absorcion) {
        if (EXTRUIDO) return evaluarExtruida(S, R, absorcion);
        String grupo;
        double sMinGrueso, sMinDelgado, rMin;
        if (absorcion <= 0.5)     { grupo = "BIa";  sMinGrueso = 1300; sMinDelgado = 700; rMin = 35; }
        else if (absorcion <= 3)  { grupo = "BIb";  sMinGrueso = 1100; sMinDelgado = 700; rMin = 30; }
        else if (absorcion <= 6)  { grupo = "BIIa"; sMinGrueso = 1000; sMinDelgado = 600; rMin = 22; }
        else if (absorcion <= 10) { grupo = "BIIb"; sMinGrueso = 800;  sMinDelgado = 500; rMin = 18; }
        else                      { grupo = "BIII"; sMinGrueso = 600;  sMinDelgado = 200;
                                    rMin = espesorMm >= 7.5 ? 15 : 12; }

        double sMin = espesorMm >= 7.5 ? sMinGrueso : sMinDelgado;
        boolean okS = S >= sMin;
        boolean rAplica = S < 3000;
        boolean okR = !rAplica || R >= rMin;
        boolean cumple = okS && okR;

        String texto = String.format("NTC 919 %s: %s  (S ≥ %.0f N; R %s)",
                grupo, cumple ? "CUMPLE" : "NO CUMPLE", sMin,
                rAplica ? String.format("≥ %.0f MPa", rMin) : "no aplica");
        return new Evaluacion(texto, cumple);
    }

    // Piezas extruidas = grupos A, con sus propios minimos de S y R.
    private static Evaluacion evaluarExtruida(double S, double R, double absorcion) {
        String grupo; double sMin, rMin;
        if (absorcion <= 0.5)     { grupo = "AIa";  sMin = 1300; rMin = 28;   }
        else if (absorcion <= 3)  { grupo = "AIb";  sMin = 1100; rMin = 23;   }
        else if (absorcion <= 6)  { grupo = "AIIa"; sMin = 950;  rMin = 20;   }
        else if (absorcion <= 10) { grupo = "AIIb"; sMin = 900;  rMin = 17.5; }
        else                      { grupo = "AIII"; sMin = 600;  rMin = 8;    }
        boolean okS = S >= sMin;
        boolean rAplica = S < 3000;
        boolean okR = !rAplica || R >= rMin;
        boolean cumple = okS && okR;
        String texto = String.format("NTC 919 %s: %s  (S ≥ %.0f N; R %s)",
                grupo, cumple ? "CUMPLE" : "NO CUMPLE", sMin,
                rAplica ? String.format("≥ %.1f MPa", rMin).replace(".0 ", " ") : "no aplica");
        return new Evaluacion(texto, cumple);
    }

    private static BufferedImage copiar(BufferedImage src) {
        BufferedImage c = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = c.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return c;
    }

    static BufferedImage preparar(BufferedImage src) {
        double k = Math.max(2.0, 60.0 / Math.max(1, src.getHeight()));
        int w = (int) (src.getWidth() * k), h = (int) (src.getHeight() * k);
        BufferedImage gris = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = gris.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();

        int[] hist = new int[256];
        java.awt.image.Raster ras = gris.getRaster();
        int[] px = ras.getPixels(0, 0, w, h, (int[]) null);
        for (int v : px) hist[v]++;
        long total = px.length, suma = 0;
        for (int i = 0; i < 256; i++) suma += (long) i * hist[i];
        long sumB = 0, wB = 0;
        double maxVar = -1;
        int umbral = 128;
        for (int i = 0; i < 256; i++) {
            wB += hist[i];
            if (wB == 0) continue;
            long wF = total - wB;
            if (wF == 0) break;
            sumB += (long) i * hist[i];
            double mB = (double) sumB / wB, mF = (double) (suma - sumB) / wF;
            double var = (double) wB * wF * (mB - mF) * (mB - mF);
            if (var > maxVar) { maxVar = var; umbral = i; }
        }
        int oscuros = 0;
        for (int v : px) if (v <= umbral) oscuros++;
        boolean invertir = oscuros > px.length / 2;

        int m = 12;
        BufferedImage out = new BufferedImage(w + 2 * m, h + 2 * m, BufferedImage.TYPE_BYTE_GRAY);
        java.awt.image.WritableRaster wr = out.getRaster();
        for (int y = 0; y < out.getHeight(); y++)
            for (int x = 0; x < out.getWidth(); x++) wr.setSample(x, y, 0, 255);
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                boolean tinta = px[y * w + x] <= umbral;
                if (invertir) tinta = !tinta;
                wr.setSample(x + m, y + m, 0, tinta ? 0 : 255);
            }
        return out;
    }
}