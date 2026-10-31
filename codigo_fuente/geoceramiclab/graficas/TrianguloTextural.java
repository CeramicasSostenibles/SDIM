package geoceramiclab.graficas;

import geoceramiclab.Tema;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

public class TrianguloTextural extends JPanel {
    private double pArc = 0, pLim = 0, pAre = 0;
    private String clase = "";
    private boolean tieneDatos = false;

    public TrianguloTextural() {
        CopyImageMenu.instalar(this, "Tri\u00e1ngulo Textural USDA");
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(920, 880));
    }

    public void setPuntoTextural(double arc, double lim, double are, String cls) {
        double tot = arc + lim + are;
        if (tot <= 0) return;
        pArc = arc / tot * 100;
        pLim = lim / tot * 100;
        pAre = are / tot * 100;
        clase = cls;
        tieneDatos = true;
        repaint();
    }

    public void limpiar() { tieneDatos = false; clase = ""; repaint(); }

    private double[] px(double arc, double are, double lim,
                        double Ax, double Ay, double Bx, double By, double Cx, double Cy) {
        double tot = arc + are + lim;
        if (tot <= 0) tot = 100;
        return new double[]{
            (arc/tot)*Ax + (are/tot)*Bx + (lim/tot)*Cx,
            (arc/tot)*Ay + (are/tot)*By + (lim/tot)*Cy
        };
    }

    private void drawLine(Graphics2D g, double a1, double s1, double l1,
                          double a2, double s2, double l2,
                          double Ax, double Ay, double Bx, double By, double Cx, double Cy) {
        double[] p1 = px(a1,s1,l1,Ax,Ay,Bx,By,Cx,Cy);
        double[] p2 = px(a2,s2,l2,Ax,Ay,Bx,By,Cx,Cy);
        g.drawLine((int)Math.round(p1[0]),(int)Math.round(p1[1]),
                   (int)Math.round(p2[0]),(int)Math.round(p2[1]));
    }

    private void fillPoly(Graphics2D g, double[][] v,
                          double Ax, double Ay, double Bx, double By, double Cx, double Cy) {
        int n = v.length; int[] xs = new int[n], ys = new int[n];
        for (int i = 0; i < n; i++) {
            double[] p = px(v[i][0],v[i][1],v[i][2],Ax,Ay,Bx,By,Cx,Cy);
            xs[i]=(int)Math.round(p[0]); ys[i]=(int)Math.round(p[1]);
        }
        g.fillPolygon(xs,ys,n);
        Color c = g.getColor();
        g.setColor(Color.BLACK); g.setStroke(new BasicStroke(1.2f));
        g.drawPolygon(xs,ys,n); g.setColor(c);
    }

    private void drawArrowHead(Graphics2D g, int x1, int y1, int x2, int y2){
        double dx=x2-x1, dy=y2-y1, len=Math.sqrt(dx*dx+dy*dy);
        if(len==0) return; dx/=len; dy/=len;
        int sz=10;
        int px1=(int)(x2-dx*sz-dy*sz*0.5), py1=(int)(y2-dy*sz+dx*sz*0.5);
        int px2=(int)(x2-dx*sz+dy*sz*0.5), py2=(int)(y2-dy*sz-dx*sz*0.5);
        g.fillPolygon(new int[]{x2,px1,px2}, new int[]{y2,py1,py2}, 3);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D)g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,    RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,       RenderingHints.VALUE_RENDER_QUALITY);

        int W = getWidth(), H = getHeight();
        g.setColor(Color.WHITE); g.fillRect(0,0,W,H);

        g.setFont(Tema.fuente(Font.BOLD, 13));
        g.setColor(new Color(0,60,120));
        String tit = "CLASES TEXTURALES DEL SUELO (USDA)";
        g.drawString(tit,(W-g.getFontMetrics().stringWidth(tit))/2,28);

        int padT=48, padB=100, padL=80, padR=80;
        int aW=W-padL-padR, aH=H-padT-padB;
        double side = Math.min(aW, aH/0.8660254);
        double Ax=padL+aW/2.0, Ay=padT;
        double Bx=Ax-side/2.0, By=padT+side*0.8660254;
        double Cx=Ax+side/2.0, Cy=By;

        Polygon clip = new Polygon(new int[]{(int)Ax,(int)Bx,(int)Cx},
                                   new int[]{(int)Ay,(int)By,(int)Cy},3);
        Shape oldClip = g.getClip();
        g.setClip(clip);

        g.setColor(new Color(220,20,20));  fillPoly(g,new double[][]{{100,0,0},{55,45,0},{40,45,15},{40,20,40},{60,0,40}},Ax,Ay,Bx,By,Cx,Cy);
        g.setColor(new Color(190,60,20));  fillPoly(g,new double[][]{{55,45,0},{35,65,0},{35,45,20}},Ax,Ay,Bx,By,Cx,Cy);
        g.setColor(new Color(210,40,20));  fillPoly(g,new double[][]{{40,20,40},{40,0,60},{60,0,40}},Ax,Ay,Bx,By,Cx,Cy);
        g.setColor(new Color(195,100,20)); fillPoly(g,new double[][]{{35,65,0},{20,80,0},{20,55,27},{27,45,27},{35,45,20}},Ax,Ay,Bx,By,Cx,Cy);
        g.setColor(new Color(190,120,30)); fillPoly(g,new double[][]{{40,45,15},{40,20,40},{27,20,53},{27,45,27}},Ax,Ay,Bx,By,Cx,Cy);
        g.setColor(new Color(220,160,40)); fillPoly(g,new double[][]{{40,20,40},{27,20,53},{27,0,73},{40,0,60}},Ax,Ay,Bx,By,Cx,Cy);
        g.setColor(new Color(237,189,81)); fillPoly(g,new double[][]{{27,45,27},{27,23,50},{7,43,50},{7,55,40},{20,55,27}},Ax,Ay,Bx,By,Cx,Cy);
        g.setColor(new Color(166,189,88)); fillPoly(g,new double[][]{{0,50,50},{0,70,30},{10,90,0},{20,80,0},{20,53,27},{7,55,40},{7,43,50}},Ax,Ay,Bx,By,Cx,Cy);
        g.setColor(new Color(230,160,50)); fillPoly(g,new double[][]{{15,0,85},{15,5,80},{0,20,80},{0,50,50},{27,23,50},{28,0,73}},Ax,Ay,Bx,By,Cx,Cy);
        g.setColor(new Color(200,210,100));fillPoly(g,new double[][]{{15,0,85},{0,0,100},{0,20,80},{15,5,80}},Ax,Ay,Bx,By,Cx,Cy);
        g.setColor(new Color(30,130,150)); fillPoly(g,new double[][]{{10,90,0},{0,85,15},{0,70,30},{20,80,0}},Ax,Ay,Bx,By,Cx,Cy);
        g.setColor(new Color(20,100,170)); fillPoly(g,new double[][]{{0,100,0},{10,90,0},{0,85,15}},Ax,Ay,Bx,By,Cx,Cy);

        g.setColor(new Color(60,60,60,120)); g.setStroke(new BasicStroke(0.7f));
        for(int p=10;p<100;p+=10){
            double f=p/100.0, fi=1-f;
            drawLine(g,f,fi,0,f,0,fi,Ax,Ay,Bx,By,Cx,Cy);
            drawLine(g,fi,f,0,0,f,fi,Ax,Ay,Bx,By,Cx,Cy);
            drawLine(g,fi,0,f,0,fi,f,Ax,Ay,Bx,By,Cx,Cy);
        }

        if(tieneDatos){
            double[] pt=px(pArc,pAre,pLim,Ax,Ay,Bx,By,Cx,Cy);
            int ptx=(int)pt[0], pty=(int)pt[1];

            g.setColor(new Color(0,0,0,80));
            g.setStroke(new BasicStroke(1.2f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,1,new float[]{6,4},0));
            drawLine(g,pArc,100-pArc,0,pArc,0,100-pArc,Ax,Ay,Bx,By,Cx,Cy);
            drawLine(g,100-pAre,pAre,0,0,pAre,100-pAre,Ax,Ay,Bx,By,Cx,Cy);
            drawLine(g,100-pLim,0,pLim,0,100-pLim,pLim,Ax,Ay,Bx,By,Cx,Cy);
        }

        g.setClip(oldClip);

        g.setColor(Color.BLACK); g.setStroke(new BasicStroke(3f));
        g.drawPolygon(new int[]{(int)Ax,(int)Bx,(int)Cx},
                      new int[]{(int)Ay,(int)By,(int)Cy},3);

        g.setFont(Tema.fuente(Font.PLAIN, 9));
        g.setColor(new Color(30,30,30));
        FontMetrics fm = g.getFontMetrics();
        for(int v=10;v<=90;v+=10){
            double f=v/100.0, fi=1-f;
            double[] pL=px(f,fi,0,Ax,Ay,Bx,By,Cx,Cy);
            g.drawString(""+v,(int)pL[0]-fm.stringWidth(""+v)-11,(int)pL[1]+3);
            double[] pR=px(f,0,fi,Ax,Ay,Bx,By,Cx,Cy);
            g.drawString(""+(100-v),(int)pR[0]+10,(int)pR[1]+3);
            double[] pB=px(0,f,fi,Ax,Ay,Bx,By,Cx,Cy);
            g.drawString(""+v,(int)pB[0]-fm.stringWidth(""+v)/2,(int)pB[1]+17);
        }
        g.setFont(Tema.fuente(Font.BOLD, 9));
        g.drawString("100",(int)Ax-14,(int)Ay-8);
        g.drawString("100",(int)Bx-27,(int)By+17);
        g.drawString("100",(int)Cx+8, (int)Cy+17);

        g.setFont(Tema.fuente(Font.BOLD, 10));
        drawAxisLabel(g,"% Arcilla",Ax,Ay,Bx,By,Cx,Cy,-1);
        drawAxisLabel(g,"% Limo",   Ax,Ay,Cx,Cy,Bx,By,+1);
        g.setColor(new Color(30,30,30));
        g.drawString("% Arena",(int)((Bx+Cx)/2)-33,(int)By+50);

        g.setColor(new Color(20,20,20)); g.setStroke(new BasicStroke(2f));

        {
            double dx=Ax-Bx, dy=Ay-By, len=Math.sqrt(dx*dx+dy*dy); dx/=len; dy/=len;
            double nx=dy, ny=-dx;
            double[] p1=px(25,75,0,Ax,Ay,Bx,By,Cx,Cy), p2=px(75,25,0,Ax,Ay,Bx,By,Cx,Cy);
            double off=55;
            int ax1=(int)(p1[0]+nx*off),ay1=(int)(p1[1]+ny*off);
            int ax2=(int)(p2[0]+nx*off),ay2=(int)(p2[1]+ny*off);
            g.drawLine(ax1,ay1,ax2,ay2); drawArrowHead(g,ax1,ay1,ax2,ay2);
        }

        {
            double dx=Cx-Ax, dy=Cy-Ay, len=Math.sqrt(dx*dx+dy*dy); dx/=len; dy/=len;
            double nx=dy, ny=-dx;
            double cx3=(Ax+Bx+Cx)/3, cy3=(Ay+By+Cy)/3;
            double[] p1=px(75,0,25,Ax,Ay,Bx,By,Cx,Cy), p2=px(25,0,75,Ax,Ay,Bx,By,Cx,Cy);
            double mx2=(p1[0]+p2[0])/2, my2=(p1[1]+p2[1])/2;
            if(nx*(mx2-cx3)+ny*(my2-cy3)<0){nx=-nx;ny=-ny;}
            double off=55;
            int ax1=(int)(p1[0]+nx*off),ay1=(int)(p1[1]+ny*off);
            int ax2=(int)(p2[0]+nx*off),ay2=(int)(p2[1]+ny*off);
            g.drawLine(ax1,ay1,ax2,ay2); drawArrowHead(g,ax1,ay1,ax2,ay2);
        }

        {
            double[] p1=px(0,25,75,Ax,Ay,Bx,By,Cx,Cy), p2=px(0,75,25,Ax,Ay,Bx,By,Cx,Cy);
            double off=60;
            int ax1=(int)p1[0],ay1=(int)(p1[1]+off);
            int ax2=(int)p2[0],ay2=(int)(p2[1]+off);
            g.drawLine(ax1,ay1,ax2,ay2); drawArrowHead(g,ax1,ay1,ax2,ay2);
        }

        labelZona(g,"Arcillosa",            59,22,19,Ax,Ay,Bx,By,Cx,Cy);
        labelZona(g,"Arcillo\narenosa",     42,52, 7,Ax,Ay,Bx,By,Cx,Cy);
        labelZona(g,"Arcillo\nlimosa",      47, 7,47,Ax,Ay,Bx,By,Cx,Cy);
        labelZona(g,"Franco\narcillo\narenosa",27,58,15,Ax,Ay,Bx,By,Cx,Cy);
        labelZona(g,"Franco\narcillosa",    34,32,34,Ax,Ay,Bx,By,Cx,Cy);
        labelZona(g,"Franco\narcillo\nlimosa",34,10,56,Ax,Ay,Bx,By,Cx,Cy);
        labelZona(g,"Franca",               18,44,39,Ax,Ay,Bx,By,Cx,Cy);
        labelZona(g,"Franco\narenosa",       9,65,27,Ax,Ay,Bx,By,Cx,Cy);
        labelZona(g,"Franco\nlimosa",       14,16,70,Ax,Ay,Bx,By,Cx,Cy);
        labelZona(g,"Limosa",                8, 6,86,Ax,Ay,Bx,By,Cx,Cy);
        labelZona(g,"Areno\nfranca",         8,81,11,Ax,Ay,Bx,By,Cx,Cy);
        labelZona(g,"Arenosa",               3,92, 5,Ax,Ay,Bx,By,Cx,Cy);

        if(tieneDatos){
            double[] pt=px(pArc,pAre,pLim,Ax,Ay,Bx,By,Cx,Cy);
            int ptx=(int)pt[0], pty=(int)pt[1];
            g.setStroke(new BasicStroke(1.5f));
            g.setColor(new Color(180,20,20)); g.fillOval(ptx-9,pty-9,18,18);
            g.setColor(Color.WHITE);          g.fillOval(ptx-4,pty-4, 8, 8);
            g.setColor(new Color(20,20,20));  g.drawOval(ptx-9,pty-9,18,18);
        }
    }

    private void labelZona(Graphics2D g, String nombre,
                           double arc, double are, double lim,
                           double Ax, double Ay, double Bx, double By, double Cx, double Cy) {
        double[] pt = px(arc,are,lim,Ax,Ay,Bx,By,Cx,Cy);
        String[] lines = nombre.split("\n");
        g.setFont(Tema.fuente(Font.BOLD, 8));
        FontMetrics fm = g.getFontMetrics();
        int lh = fm.getHeight(), totalH = lines.length*lh;
        int ty = (int)pt[1]-totalH/2+fm.getAscent();
        for(String line:lines){
            int tx=(int)pt[0]-fm.stringWidth(line)/2;
            g.setColor(new Color(255,255,255,200));
            for(int dx=-1;dx<=1;dx++)
                for(int dy=-1;dy<=1;dy++)
                    if(dx!=0||dy!=0) g.drawString(line,tx+dx,ty+dy);
            g.setColor(new Color(15,15,15));
            g.drawString(line,tx,ty);
            ty+=lh;
        }
    }

    private void drawAxisLabel(Graphics2D g, String txt,
                               double x1, double y1, double x2, double y2,
                               double ox, double oy, int side) {
        double angle=Math.atan2(y2-y1,x2-x1);
        double mx=(x1+x2)/2, my=(y1+y2)/2;
        double nx=-(y2-y1), ny=(x2-x1);
        double len=Math.sqrt(nx*nx+ny*ny);
        if(len>0){nx/=len;ny/=len;}
        if(nx*(mx-ox)+ny*(my-oy)<0){nx=-nx;ny=-ny;}
        double lx=mx+nx*36, ly=my+ny*36;
        Graphics2D gt=(Graphics2D)g.create();
        gt.translate(lx,ly); gt.rotate(angle);
        gt.setFont(Tema.ui(Font.BOLD, 10));
        gt.setColor(new Color(30,30,30));
        gt.drawString(txt,-gt.getFontMetrics().stringWidth(txt)/2,3);
        gt.dispose();
    }
}
