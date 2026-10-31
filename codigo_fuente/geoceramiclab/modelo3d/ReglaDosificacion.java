package geoceramiclab.modelo3d;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ReglaDosificacion {

    public static final double   K_CONSTANTE   = 35.0;
    public static final double   K_MIN         = 15.0;
    public static final double   K_MAX         = 40.0;
    public static final double   PASO_CAOLIN   = 5.0;
    public static final double[] FELDESPATO    = {5, 10, 15};
    public static final double[] FACTOR_LP     = {0.45, 0.585, 0.72};
    public static final double[] HUMEDAD_NP    = {10, 13, 16};

    private static final Pattern SIMBOLO =
        Pattern.compile("\\b(GW|GP|GM|GC|SW|SP|SM|SC|ML|CL|CI|OL|MH|CH|OH|PT)\\b");

    private ReglaDosificacion(){}

    public static final class Propuesta {

        public final double[][] dosis;

        public final double caolinBase;

        public final double ajusteUSCS;

        public final String simboloUSCS;

        public final String resumen;

        Propuesta(double[][] dosis,double caolinBase,double ajusteUSCS,String simbolo,String resumen){
            this.dosis=dosis; this.caolinBase=caolinBase; this.ajusteUSCS=ajusteUSCS;
            this.simboloUSCS=simbolo; this.resumen=resumen;
        }
    }

    public static Propuesta proponer(String uscs,double ip,double lp){
        String simbolo=simboloUSCS(uscs);
        double delta=ajustePorTipo(simbolo);
        boolean np=Double.isNaN(ip)||ip<=0;
        double ipUsado=np?0:ip;

        double k0=clamp(K_CONSTANTE-ipUsado+delta,K_MIN,K_MAX);
        boolean lpValido=!Double.isNaN(lp)&&lp>0;

        double[][] d=new double[3][4];
        for(int m=0;m<3;m++){
            double k=Math.round(k0+(m-1)*PASO_CAOLIN);
            double f=FELDESPATO[m];
            double r=100-k-f;
            double h=lpValido?Math.round(FACTOR_LP[m]*lp):HUMEDAD_NP[m];
            d[m]=new double[]{r,k,f,h};
        }

        String resumen=String.format(Locale.US,
            "USCS %s (Δ=%+.0f) · IP %s · LP %s → caolín base %.1f %%%s",
            simbolo,delta,
            np?"NP":String.format(Locale.US,"%.1f",ip),
            lpValido?String.format(Locale.US,"%.1f",lp):"NP",
            k0,
            lpValido?"":" (humedad por defecto: suelo NP)");
        return new Propuesta(d,k0,delta,simbolo,resumen);
    }

    public static String simboloUSCS(String uscs){
        if(uscs==null) return "?";
        Matcher mt=SIMBOLO.matcher(uscs.toUpperCase(Locale.ROOT));
        return mt.find()?mt.group(1):"?";
    }

    public static double ajustePorTipo(String simbolo){
        switch(simbolo){
            case "CL": case "CI": case "CH":                   return 0;
            case "ML": case "MH": case "OL": case "OH":         return 3;
            case "SC": case "GC":                              return 5;
            case "SM": case "GM":                              return 8;
            case "SW": case "SP": case "GW": case "GP":        return 10;
            default:                                           return 0;
        }
    }

    private static double clamp(double v,double mn,double mx){return Math.max(mn,Math.min(mx,v));}
}