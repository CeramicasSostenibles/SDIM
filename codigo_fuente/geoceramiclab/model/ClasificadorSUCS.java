package geoceramiclab.model;

import java.util.ArrayList;
import java.util.List;

public final class ClasificadorSUCS {

    private ClasificadorSUCS(){}

    private static final double FINOS_LIMPIO      =  5.0;
    private static final double FINOS_DOBLE       = 12.0;
    private static final double FINOS_SUELO_FINO  = 50.0;
    private static final double MODIF_CON         = 15.0;
    private static final double MODIF_ADJ         = 30.0;

    public enum Nivel {

        COMPLETA,

        PARCIAL,

        NO_CLASIFICABLE
    }

    public static class Resultado {
        public String  simbolo = "—";
        public String  nombre  = "Sin clasificar";
        public Nivel   nivel   = Nivel.NO_CLASIFICABLE;

        public final List<String[]> alternativas = new ArrayList<>();

        public final List<String>   pasos        = new ArrayList<>();

        public final List<String>   faltantes    = new ArrayList<>();

        public final List<String>   avisos       = new ArrayList<>();

        public String texturalNombre = null;

        public String nombreGenerico = null;

        public boolean esCompleta(){ return nivel == Nivel.COMPLETA; }

        public String nombreParaModelo() {
            if (nivel == Nivel.COMPLETA) return nombre;
            return nombreGenerico;
        }

        void paso(String s){ pasos.add(s); }
        void alt(String sim, String nom, String cond){ alternativas.add(new String[]{sim,nom,cond}); }
    }

    public static Resultado clasificar(SueloData d) {
        Resultado r = new Resultado();

        boolean gran  = d.granCalculada;
        boolean atter = d.atterCalculada && !Double.isNaN(d.LL) && !Double.isNaN(d.IP);
        boolean hidro = d.hidroCalculada;

        if (hidro && d.claseTextural != null && !d.claseTextural.isBlank()) {
            r.texturalNombre = d.claseTextural;
        }

        if (!gran && !atter && !hidro) {
            r.nombre = "Calcule al menos un ensayo";
            r.faltantes.add("Granulometría, límites de Atterberg o hidrómetro");
            return r;
        }

        if (!gran) {
            if (atter) return soloAtterberg(d, r, hidro);
            return soloHidrometro(d, r);
        }

        double finos = d.pctFinos;
        double grava = d.pctGrava;
        double arena = d.pctArena;

        r.paso(String.format("Granulometría: grava %.1f %%, arena %.1f %%, finos %.1f %%",
                grava, arena, finos));

        coherencia(d, r, atter, hidro);

        if (finos >= FINOS_SUELO_FINO) {
            r.paso(String.format("Finos %.1f %% ≥ 50 %%  →  SUELO FINO", finos));
            return suelosFinos(d, r, atter, finos, grava, arena);
        } else {
            r.paso(String.format("Finos %.1f %% < 50 %%  →  SUELO GRUESO", finos));
            return suelosGruesos(d, r, atter, finos, grava, arena);
        }
    }

    private static Resultado suelosGruesos(SueloData d, Resultado r, boolean atter,
                                           double finos, double grava, double arena) {

        boolean esGrava = grava > arena;
        String  g = esGrava ? "G" : "S";
        r.paso(String.format("Fracción gruesa: %s (grava %.1f %% %s arena %.1f %%)  →  símbolo empieza en %s",
                esGrava ? "grava" : "arena", grava, esGrava ? ">" : "≤", arena, g));

        String gradacion = null;
        if (d.Cu != null && d.Cc != null) {
            double cuMin = esGrava ? 4.0 : 6.0;
            // Gradacion de gruesos: bien gradado si Cu >= limite y 1 <= Cc <= 3
            //   Cu = D60/D10 (uniformidad),  Cc = D30^2/(D10*D60) (curvatura)
            boolean bien = d.Cu >= cuMin && d.Cc >= 1.0 && d.Cc <= 3.0;
            gradacion = bien ? "W" : "P";
            r.paso(String.format("Cu = %.2f, Cc = %.2f  →  %s  (criterio: Cu ≥ %.0f y 1 ≤ Cc ≤ 3)",
                    d.Cu, d.Cc, bien ? "bien gradada (W)" : "mal gradada (P)", cuMin));
        } else if (finos <= FINOS_DOBLE) {

            r.faltantes.add("D₁₀/D₃₀/D₆₀ para calcular Cu y Cc (amplíe la curva granulométrica)");
            r.paso("No se pudieron calcular Cu y Cc: la curva no cubre los diámetros necesarios");
        } else {
            r.paso("Cu y Cc no disponibles, pero con más de 12 % de finos no intervienen en el símbolo");
        }

        if (finos < FINOS_LIMPIO) {
            r.paso(String.format("Finos %.1f %% < 5 %%  →  grueso limpio: NO se requieren límites de Atterberg", finos));

            if (gradacion == null) {
                r.nivel = Nivel.PARCIAL;
                r.simbolo = g + "W  o  " + g + "P";
                r.nombre  = "Falta la gradación (Cu y Cc)";
                r.nombreGenerico = esGrava ? "Grava limpia" : "Arena limpia";
                r.alt(g+"W", nombreGrueso(g+"W", grava, arena), "Cu ≥ "+(esGrava?"4":"6")+" y 1 ≤ Cc ≤ 3");
                r.alt(g+"P", nombreGrueso(g+"P", grava, arena), "no cumple lo anterior");
                return r;
            }
            return cerrar(r, g + gradacion, grava, arena);
        }

        String finoMC = null;
        if (atter) {
            finoMC = letraFinos(d.LL, d.IP, r);
        } else {
            r.faltantes.add("Límites de Atterberg (LL y LP) para saber si los finos son limosos o arcillosos");
        }

        if (finos <= FINOS_DOBLE) {
            r.paso(String.format("5 %% ≤ finos %.1f %% ≤ 12 %%  →  SÍMBOLO DOBLE", finos));

            if (gradacion == null || finoMC == null) {
                r.nivel = Nivel.PARCIAL;
                r.simbolo = g + "?-" + g + "?";
                r.nombre  = "Símbolo doble incompleto";
                r.nombreGenerico = esGrava ? "Grava con pocos finos" : "Arena con pocos finos";
                for (String grad : (gradacion == null ? new String[]{"W","P"} : new String[]{gradacion}))
                    for (String mc : (finoMC == null ? new String[]{"M","C"} : new String[]{finoMC.equals("C-M")?"C":finoMC})) {
                        String sim = g+grad+"-"+g+mc;
                        r.alt(sim, nombreGrueso(sim, grava, arena), condicion(grad, mc, esGrava));
                    }
                return r;
            }
            String mc = finoMC.equals("C-M") ? "C" : finoMC;
            return cerrar(r, g + gradacion + "-" + g + mc, grava, arena);
        }

        r.paso(String.format("Finos %.1f %% > 12 %%  →  los finos gobiernan la segunda letra", finos));

        if (finoMC == null) {
            r.nivel = Nivel.PARCIAL;
            r.simbolo = g + "M  o  " + g + "C";
            r.nombre  = "Faltan los límites de Atterberg";
            r.nombreGenerico = esGrava ? "Grava con finos" : "Arena con finos";
            r.alt(g+"M",       nombreGrueso(g+"M",       grava, arena), "IP < 4  o  bajo la línea A");
            r.alt(g+"C",       nombreGrueso(g+"C",       grava, arena), "IP > 7  y  sobre la línea A");
            r.alt(g+"C-"+g+"M",nombreGrueso(g+"C-"+g+"M",grava, arena), "4 ≤ IP ≤ 7  y  sobre la línea A");
            return r;
        }

        String sim = finoMC.equals("C-M") ? (g+"C-"+g+"M") : (g + finoMC);
        return cerrar(r, sim, grava, arena);
    }

    private static Resultado suelosFinos(SueloData d, Resultado r, boolean atter,
                                         double finos, double grava, double arena) {

        if (!atter) {
            r.nivel = Nivel.PARCIAL;
            r.simbolo = "Suelo fino (CL · ML · CH · MH)";
            r.nombre  = "La clasificación de un suelo fino sale de la carta de plasticidad";
            r.nombreGenerico = "Suelo fino";
            r.faltantes.add("Límites de Atterberg (LL y LP) — son indispensables aquí");
            r.alt("CL",    "Arcilla de baja plasticidad", "LL < 50 · sobre línea A · IP > 7");
            r.alt("ML",    "Limo",                        "LL < 50 · bajo línea A o IP < 4");
            r.alt("CL-ML", "Limo arcilloso",              "LL < 50 · sobre línea A · 4 ≤ IP ≤ 7");
            r.alt("CH",    "Arcilla de alta plasticidad", "LL ≥ 50 · sobre línea A");
            r.alt("MH",    "Limo elástico",               "LL ≥ 50 · bajo línea A");
            return r;
        }

        String sim = simboloFino(d.LL, d.IP, r);
        return cerrarFino(r, sim, finos, grava, arena);
    }

    // Linea A de la carta de plasticidad: IP = 0.73*(LL - 20)
    private static double lineaA(double LL){ return 0.73 * (LL - 20.0); }
    private static double lineaU(double LL){ return 0.90 * (LL -  8.0); }

    private static String letraFinos(double LL, double IP, Resultado r) {
        double a = lineaA(LL);
        chequearLineaU(LL, IP, r);
        r.paso(String.format("Atterberg: LL = %.1f, IP = %.1f  ·  línea A = 0.73·(LL−20) = %.1f", LL, IP, a));

        if (IP < 4 || IP < a) {
            r.paso("IP < 4 o bajo la línea A  →  finos LIMOSOS (M)");
            return "M";
        }
        if (IP > 7 && IP >= a) {
            r.paso("IP > 7 y sobre la línea A  →  finos ARCILLOSOS (C)");
            return "C";
        }
        r.paso("4 ≤ IP ≤ 7 y sobre la línea A  →  zona de frontera (C-M)");
        return "C-M";
    }

    private static String simboloFino(double LL, double IP, Resultado r) {
        double a = lineaA(LL);
        chequearLineaU(LL, IP, r);
        r.paso(String.format("Atterberg: LL = %.1f, IP = %.1f  ·  línea A = 0.73·(LL−20) = %.1f", LL, IP, a));

        if (LL < 50) {
            if (IP < 4 || IP < a) { r.paso("LL < 50, bajo la línea A o IP < 4  →  ML"); return "ML"; }
            if (IP > 7)           { r.paso("LL < 50, sobre la línea A, IP > 7  →  CL");  return "CL"; }
            r.paso("LL < 50, sobre la línea A, 4 ≤ IP ≤ 7  →  CL-ML (zona rayada)");
            return "CL-ML";
        }
        if (IP >= a) { r.paso("LL ≥ 50, sobre la línea A  →  CH"); return "CH"; }
        r.paso("LL ≥ 50, bajo la línea A  →  MH");
        return "MH";
    }

    private static void chequearLineaU(double LL, double IP, Resultado r) {
        if (IP > lineaU(LL) + 0.5) {
            r.avisos.add(String.format(
                "El punto (LL=%.1f, IP=%.1f) queda POR ENCIMA de la línea U (IP = 0.9·(LL−8) = %.1f). "
              + "Ningún suelo natural debería estar ahí: revise el ensayo antes de reportar.",
                LL, IP, lineaU(LL)));
        }
        if (LL < 0 || IP < 0) {
            r.avisos.add("LL o IP negativos: revise las masas del ensayo.");
        }
    }

    private static Resultado soloAtterberg(SueloData d, Resultado r, boolean hidro) {
        String sim = simboloFino(d.LL, d.IP, r);

        boolean respaldado = false;
        if (hidro) {
            double finosHidro = d.pctArcilla + d.pctLimo;
            r.paso(String.format("Hidrómetro: arcilla %.1f %% + limo %.1f %% = %.1f %% de finos",
                    d.pctArcilla, d.pctLimo, finosHidro));
            if (finosHidro >= FINOS_SUELO_FINO) {
                respaldado = true;
                r.paso("El hidrómetro confirma ≥ 50 % de finos  →  el suelo sí es fino");
            } else {
                r.avisos.add(String.format(
                    "El hidrómetro indica solo %.1f %% de finos (< 50 %%). Si eso se confirma con "
                  + "tamizado, el suelo sería GRUESO y este símbolo no aplicaría.", finosHidro));
            }
        }

        if (respaldado) {
            r.nivel   = Nivel.COMPLETA;
            r.simbolo = sim;
            r.nombre  = nombreFinoBase(sim);
            r.faltantes.add("Granulometría por tamizado, para agregar el modificador del nombre "
                          + "(«con arena», «arenosa», etc.)");
            return r;
        }

        r.nivel   = Nivel.PARCIAL;
        r.simbolo = sim + "  (provisional)";
        r.nombre  = nombreFinoBase(sim);
        r.nombreGenerico = nombreFinoBase(sim);
        r.faltantes.add("Granulometría: sin ella no se puede confirmar que el suelo sea fino (≥ 50 % pasa el #200)");
        r.paso("Sin granulometría se ASUME suelo fino. Si el tamizado da menos de 50 % de finos, "
             + "el símbolo correcto sería del tipo SM/SC/GM/GC.");
        return r;
    }

    private static Resultado soloHidrometro(SueloData d, Resultado r) {
        r.nivel   = Nivel.NO_CLASIFICABLE;
        r.simbolo = "—";
        r.nombre  = "El hidrómetro por sí solo no da un símbolo SUCS";
        r.paso(String.format("Hidrómetro: arcilla %.1f %%, limo %.1f %%, arena %.1f %%",
                d.pctArcilla, d.pctLimo, d.pctArenaFina));
        if (r.texturalNombre != null)
            r.paso("Clasificación textural (USDA): " + r.texturalNombre);
        r.faltantes.add("Límites de Atterberg — el SUCS clasifica los finos por plasticidad, no por tamaño");
        r.faltantes.add("Granulometría por tamizado — para separar grava, arena y finos");
        r.paso("El SUCS distingue limo de arcilla por PLASTICIDAD (carta de plasticidad), "
             + "mientras que el hidrómetro los separa por TAMAÑO. No son equivalentes.");
        return r;
    }

    private static void coherencia(SueloData d, Resultado r, boolean atter, boolean hidro) {
        double suma = d.pctGrava + d.pctArena + d.pctFinos;
        if (Math.abs(suma - 100.0) > 1.0) {
            r.avisos.add(String.format(
                "Grava + arena + finos = %.1f %% (debería dar 100 %%). Revise la masa total del tamizado.", suma));
        }
        if (hidro) {
            double finosHidro = d.pctArcilla + d.pctLimo;
            if (Math.abs(finosHidro - d.pctFinos) > 10.0) {
                r.avisos.add(String.format(
                    "El tamizado da %.1f %% de finos y el hidrómetro %.1f %%: difieren en más de 10 puntos. "
                  + "Revise si ambos ensayos son de la misma muestra.", d.pctFinos, finosHidro));
            }
        }
        if (atter && d.pctFinos < FINOS_LIMPIO) {
            r.avisos.add(String.format(
                "Hay límites de Atterberg registrados pero el suelo tiene solo %.1f %% de finos (< 5 %%). "
              + "El SUCS no los usa en este caso.", d.pctFinos));
        }
    }

    private static Resultado cerrar(Resultado r, String sim, double grava, double arena) {
        r.nivel   = Nivel.COMPLETA;
        r.simbolo = sim;
        r.nombre  = nombreGrueso(sim, grava, arena);
        return r;
    }

    private static Resultado cerrarFino(Resultado r, String sim, double finos, double grava, double arena) {
        r.nivel   = Nivel.COMPLETA;
        r.simbolo = sim;
        r.nombre  = nombreFino(sim, finos, grava, arena);
        return r;
    }

    public static String nombreGrueso(String sim, double grava, double arena) {
        String base;
        switch (sim) {
            case "GW":    base = "Grava bien gradada";               break;
            case "GP":    base = "Grava mal gradada";                break;
            case "GW-GM": base = "Grava bien gradada con limo";      break;
            case "GW-GC": base = "Grava bien gradada con arcilla";   break;
            case "GP-GM": base = "Grava mal gradada con limo";       break;
            case "GP-GC": base = "Grava mal gradada con arcilla";    break;
            case "GM":    base = "Grava limosa";                     break;
            case "GC":    base = "Grava arcillosa";                  break;
            case "GC-GM": base = "Grava arcillo-limosa";             break;
            case "SW":    base = "Arena bien gradada";               break;
            case "SP":    base = "Arena mal gradada";                break;
            case "SW-SM": base = "Arena bien gradada con limo";      break;
            case "SW-SC": base = "Arena bien gradada con arcilla";   break;
            case "SP-SM": base = "Arena mal gradada con limo";       break;
            case "SP-SC": base = "Arena mal gradada con arcilla";    break;
            case "SM":    base = "Arena limosa";                     break;
            case "SC":    base = "Arena arcillosa";                  break;
            case "SC-SM": base = "Arena arcillo-limosa";             break;
            default:      return sim;
        }

        boolean esGrava = sim.startsWith("G");
        double  otra    = esGrava ? arena : grava;
        if (otra >= MODIF_CON) {
            String conQue = esGrava ? "arena" : "grava";

            base += base.contains(" con ") ? (" y " + conQue) : (" con " + conQue);
        }
        return base;
    }

    public static String nombreFino(String sim, double finos, double grava, double arena) {
        double retenido = 100.0 - finos;
        boolean masArena = arena >= grava;

        if (retenido < MODIF_CON)  return nombreFinoBase(sim);
        if (retenido < MODIF_ADJ)  return nombreFinoBase(sim) + (masArena ? " con arena" : " con grava");
        return nombreFinoAdj(sim, masArena);
    }

    public static String nombreFinoBase(String sim) {
        switch (sim) {
            case "CL":    return "Arcilla de baja plasticidad";
            case "CH":    return "Arcilla de alta plasticidad";
            case "ML":    return "Limo";
            case "MH":    return "Limo elástico";
            case "CL-ML": return "Limo arcilloso";
            case "OL":    return "Suelo orgánico de baja plasticidad";
            case "OH":    return "Suelo orgánico de alta plasticidad";
            default:      return sim;
        }
    }

    private static String nombreFinoAdj(String sim, boolean masArena) {
        String a = masArena ? "arenosa" : "gravosa";
        String o = masArena ? "arenoso" : "gravoso";
        switch (sim) {
            case "CL":    return "Arcilla " + a + " de baja plasticidad";
            case "CH":    return "Arcilla " + a + " de alta plasticidad";
            case "ML":    return "Limo " + o;
            case "MH":    return "Limo elástico " + o;
            case "CL-ML": return "Limo arcilloso " + o;
            default:      return nombreFinoBase(sim);
        }
    }

    private static String condicion(String grad, String mc, boolean esGrava) {
        String cg = grad.equals("W")
            ? ("Cu ≥ " + (esGrava ? "4" : "6") + " y 1 ≤ Cc ≤ 3")
            : "no cumple la buena gradación";
        String cm = mc.equals("M") ? "IP < 4 o bajo la línea A" : "IP > 7 y sobre la línea A";
        return cg + "  ·  " + cm;
    }
}