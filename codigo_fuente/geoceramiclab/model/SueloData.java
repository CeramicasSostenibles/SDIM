package geoceramiclab.model;

import java.util.*;

public class SueloData {

    public double pctGrava   = 0;
    public double pctArena   = 0;
    public double pctFinos   = 0;
    public Double D10 = null, D30 = null, D60 = null;
    public Double Cu  = null, Cc  = null;
    public List<double[]> puntosCurvaGran = new ArrayList<>();
    public boolean granCalculada = false;

    public List<String[]> tablaGran = new ArrayList<>();
    public double masaTotalGran = 0;

    public double pctArcilla  = 0;
    public double pctLimo     = 0;
    public double pctArenaFina= 0;
    public String claseTextural = "—";
    public List<double[]> puntosHidro = new ArrayList<>();
    public boolean hidroCalculada = false;

    public List<String[]> tablaHidro = new ArrayList<>();
    public double masaSecaHidro = 0;
    public double GsHidro = 2.65;

    public double LL = Double.NaN;
    public double LP = Double.NaN;
    public double IP = Double.NaN;
    public String clasificacionUSCS = "—";
    public boolean atterCalculada = false;

    public List<String[]> tablaLL = new ArrayList<>();

    public double lpTara = Double.NaN, lpHum = Double.NaN, lpSec = Double.NaN;

    public List<String[]> tablaLP = new ArrayList<>();

    public String nombreFinal = "Sin clasificar";
    public String colorSuelo  = "#C1622A";

    public double pctSiO2  = 0;
    public double pctAl2O3 = 0;
    public double pctCaO   = 0;

    public boolean quimicaCalculada = false;

    public String fasePrimaria = "—";
    public String subfaseTernaria = "—";

    private final List<Runnable> oyentes = new ArrayList<>();

    public void addOyente(Runnable r) { oyentes.add(r); }

    public void notificar() {
        actualizarClasificacion();
        actualizarColor();
        oyentes.forEach(Runnable::run);
    }

    private void actualizarClasificacion() {
        if (!granCalculada && !hidroCalculada && !atterCalculada) {
            nombreFinal = "Sin datos";
            return;
        }

        String dom = "";
        if (granCalculada) {
            if (pctGrava > 50)       dom = "Grava";
            else if (pctArena > 50)  dom = "Arena";
            else                     dom = "Suelo fino";
        }

        String mod = "";
        if (hidroCalculada) {
            double tot = pctArcilla + pctLimo + pctArenaFina;
            if (tot > 0) {
                double pc = pctArcilla/tot*100;
                double ps = pctLimo/tot*100;
                double pa = pctArenaFina/tot*100;
                if      (pc >= 40)            mod = "Arcilloso";
                else if (pc >= 27 && ps >= 20) mod = "Arcillo-Limoso";
                else if (pc >= 27)             mod = "Arcillo-Arenoso";
                else if (ps >= 50)             mod = "Limoso";
                else if (pa >= 70)             mod = "Arenoso";
                else if (pc >= 12)             mod = "Franco";
                else                           mod = "Franco";
            }
        }

        clasificacionUSCS = clasificarUSCS_Completo();
        String uscsTag = clasificacionUSCS.equals("—") ? "" : " [" + clasificacionUSCS.split(" ")[0] + "]";

        if (dom.isEmpty() && mod.isEmpty()) nombreFinal = "Sin clasificar";
        else if (dom.isEmpty())             nombreFinal = mod + uscsTag;
        else if (mod.isEmpty())             nombreFinal = dom + uscsTag;
        else                                nombreFinal = dom + " " + mod + uscsTag;
    }

    private void actualizarColor() {
        if (!hidroCalculada && !granCalculada) { colorSuelo = "#C1622A"; return; }

        if (pctArcilla > 40) {
            colorSuelo = "#8B3A0F";
        } else if (pctArcilla > 20) {
            colorSuelo = "#C1622A";
        } else if (pctLimo > 40) {
            colorSuelo = "#C8A96E";
        } else if (pctArena > 60 || pctArenaFina > 60) {
            colorSuelo = "#D4B483";
        } else if (pctGrava > 40) {
            colorSuelo = "#9E9E9E";
        } else {
            colorSuelo = "#B5703C";
        }
    }

    public static String clasificarTextural(double clay, double silt, double sand) {
        double tot = clay + silt + sand;
        if (tot <= 0) return "Sin datos";
        double c = clay/tot*100, s = silt/tot*100, a = sand/tot*100;

        if (c >= 40)                              return "Arcilla";
        if (c >= 27 && c < 40 && s < 20)         return "Arcillo arenoso";
        if (c >= 27 && c < 40 && s >= 20)        return "Arcillo limoso";
        if (c >= 20 && c < 35 && s < 15 && a>=45) return "Franco arcillo arenoso";
        if (c >= 27 && c < 40 && s>=15 && s<53)  return "Franco arcilloso";
        if (c >= 27 && c < 40 && s >= 40)        return "Franco arcillo limoso";
        if (c >= 7  && c < 27 && s>=28 && a<52)  return "Franco";
        if (s >= 80 && c < 12)                   return "Limo";
        if (s >= 50 && c < 27)                   return "Franco limoso";
        if (a >= 70 && c < 15)                   return "Franco arenoso";
        if (a >= 85 && c < 10)                   return "Arenoso franco";
        if (a >= 90)                             return "Arena";
        return "Franco";
    }

    public String clasificarUSCS_Completo() {

        boolean tieneGran   = granCalculada;
        boolean tieneHidro  = hidroCalculada;
        boolean tieneAtter  = atterCalculada && !Double.isNaN(LL) && !Double.isNaN(IP);

        if (!tieneGran && !tieneHidro && !tieneAtter) return "—";

        double finos;
        if (tieneGran)       finos = pctFinos;
        else if (tieneHidro) finos = pctArcilla + pctLimo;
        else                 finos = 100.0;

        double arcilla = tieneHidro ? pctArcilla : 0;
        double limo    = tieneHidro ? pctLimo    : 0;

        // Linea A de la carta de plasticidad: IP = 0.73*(LL - 20)
        double lineaA = tieneAtter ? 0.73*(LL-20) : Double.NaN;

        if (finos <= 50 && tieneGran) {
            boolean esGrava = pctGrava > pctArena;

            double cu = (Cu  != null) ? Cu  : 0;
            double cc = (Cc  != null) ? Cc  : 0;

            if (finos < 5) {

                if (esGrava) {
                    boolean bienGrad = (cu >= 4 && cc >= 1 && cc <= 3);
                    return bienGrad
                        ? "GW (Grava bien gradada)"
                        : "GP (Grava mal gradada)";
                } else {
                    boolean bienGrad = (cu >= 6 && cc >= 1 && cc <= 3);
                    return bienGrad
                        ? "SW (Arena bien gradada)"
                        : "SP (Arena mal gradada)";
                }
            } else if (finos >= 5 && finos <= 12) {

                if (esGrava) {
                    if (!tieneAtter) return "GW-GP (Grava con finos, sin Atterberg)";
                    return (IP > lineaA && IP >= 4)
                        ? "GC (Grava arcillosa)"
                        : "GM (Grava limosa)";
                } else {
                    if (!tieneAtter) return "SW-SP (Arena con finos, sin Atterberg)";
                    return (IP > lineaA && IP >= 4)
                        ? "SC (Arena arcillosa)"
                        : "SM (Arena limosa)";
                }
            } else {

                if (esGrava) {
                    if (!tieneAtter) return "GM (Grava limosa — sin Atterberg)";
                    return (IP > lineaA && IP >= 4)
                        ? "GC (Grava arcillosa)"
                        : "GM (Grava limosa)";
                } else {
                    if (!tieneAtter) return "SM (Arena limosa — sin Atterberg)";
                    return (IP > lineaA && IP >= 4)
                        ? "SC (Arena arcillosa)"
                        : "SM (Arena limosa)";
                }
            }
        }

        if (!tieneAtter) {
            if (arcilla > 40) return "CH (Arcilla alta plasticidad — sin LL)";
            if (arcilla > 15) return "CL (Arcilla baja plasticidad — sin LL)";
            if (limo    > 50) return "ML (Limo baja plasticidad — sin LL)";
            return "ML (Limo — sin Atterberg)";
        }

        boolean sobreLineaA = (IP > lineaA);
        boolean allaPlastic = (LL >= 50);

        boolean confirmArcilla = tieneHidro && arcilla > 25;
        boolean confirmLimo    = tieneHidro && limo    > 40 && arcilla < 25;

        if (IP >= 4 && IP <= 7 && lineaA >= 4 && !allaPlastic) {

            if (confirmArcilla) return "CL (Arcilla baja — confirmada por hidrómetro)";
            if (confirmLimo)    return "ML (Limo bajo — confirmado por hidrómetro)";
            return "CL-ML (Zona transición — LL=" + fmt(LL) + ", IP=" + fmt(IP) + ")";
        }

        if (!allaPlastic) {

            if (sobreLineaA && IP >= 7) {
                if (confirmLimo) return "CL (Arcilla baja — hidrómetro indica limo, verificar)";
                return "CL (Arcilla baja plasticidad — LL=" + fmt(LL) + ", IP=" + fmt(IP) + ")";
            } else {
                if (confirmArcilla) return "ML (Limo — hidrómetro indica arcilla, revisar Atterberg)";
                return "ML (Limo baja plasticidad — LL=" + fmt(LL) + ", IP=" + fmt(IP) + ")";
            }
        } else {

            if (sobreLineaA) {
                return "CH (Arcilla alta plasticidad — LL=" + fmt(LL) + ", IP=" + fmt(IP) + ")";
            } else {
                return "MH (Limo alta plasticidad — LL=" + fmt(LL) + ", IP=" + fmt(IP) + ")";
            }
        }
    }

    private static String fmt(double v){ return String.format("%.0f", v); }

    public static String clasificarUSCS(double ll, double ip) {
        if (Double.isNaN(ll) || Double.isNaN(ip)) return "—";
        double lineaA = 0.73 * (ll - 20);
        boolean sobreA = ip > lineaA;
        if (ll < 50) {
            if (ip >= 4 && ip <= 7) return "CL-ML (Zona transición)";
            if (sobreA && ip >= 7)  return "CL (Arcilla baja plasticidad)";
            return "ML (Limo baja plasticidad)";
        } else {
            if (sobreA) return "CH (Arcilla alta plasticidad)";
            return "MH (Limo alta plasticidad)";
        }
    }
}
