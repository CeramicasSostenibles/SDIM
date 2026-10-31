package geoceramiclab.graficas;

import geoceramiclab.Tema;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

public class CartaPlasticidadPanel extends JPanel {
    private Double pLL = null, pIP = null;
    private String pUSCS = "";

    private static final Color BG    = Color.WHITE;
    private static final Color BG2   = new Color(248,249,252);
    private static final Color GRID  = new Color(200,205,215,160);
    private static final Color NEGRO = new Color(30,30,30);
    private static final Color TEXT2 = new Color(110,120,140);
    private static final int ML=60, MR=35, MT=45, MB=55;

    public CartaPlasticidadPanel() {
        CopyImageMenu.instalar(this,"Carta de Plasticidad");
        setBackground(BG);
        setPreferredSize(new Dimension(560,440));
    }

    public void setPunto(double ll,double ip,String uscs){
        this.pLL=ll; this.pIP=ip; this.pUSCS=uscs; repaint();
    }

    @Override
    protected void paintComponent(Graphics g0){
        super.paintComponent(g0);
        Graphics2D g=(Graphics2D)g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,    RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,       RenderingHints.VALUE_RENDER_QUALITY);

        int W=getWidth(), H=getHeight();
        int pw=W-ML-MR, ph=H-MT-MB;
        double xMin=0,xMax=100,yMin=0,yMax=60;

        g.setColor(BG);  g.fillRect(0,0,W,H);
        g.setColor(BG2); g.fillRect(ML,MT,pw,ph);

        GeneralPath superior=new GeneralPath();
        superior.moveTo(toX(0,xMin,xMax,pw)+ML, toY(0,yMin,yMax,ph)+MT);
        superior.lineTo(toX(60,xMin,xMax,pw)+ML,toY(60,yMin,yMax,ph)+MT);
        superior.lineTo(toX(0,xMin,xMax,pw)+ML, toY(60,yMin,yMax,ph)+MT);
        superior.closePath();
        g.setColor(new Color(245, 245, 245)); g.fill(superior);

        GeneralPath inferior=new GeneralPath();
        inferior.moveTo(toX(0,xMin,xMax,pw)+ML,  toY(0,yMin,yMax,ph)+MT);
        inferior.lineTo(toX(100,xMin,xMax,pw)+ML, toY(0,yMin,yMax,ph)+MT);
        inferior.lineTo(toX(100,xMin,xMax,pw)+ML, toY(60,yMin,yMax,ph)+MT);
        inferior.lineTo(toX(60,xMin,xMax,pw)+ML,  toY(60,yMin,yMax,ph)+MT);
        inferior.closePath();
        g.setColor(new Color(220, 220, 220)); g.fill(inferior);

        g.setFont(Tema.fuente(Font.PLAIN, 10));
        g.setStroke(new BasicStroke(0.5f));
        for(int v=0;v<=100;v+=10){
            int x=toX(v,xMin,xMax,pw)+ML;
            g.setColor(v%20==0?new Color(180,185,200,180):GRID);
            g.drawLine(x,MT,x,MT+ph);
            g.setColor(TEXT2);
            g.drawString(String.valueOf(v),x-6,MT+ph+18);
        }
        for(int v=0;v<=60;v+=10){
            int y=toY(v,yMin,yMax,ph)+MT;
            g.setColor(v%20==0?new Color(180,185,200,180):GRID);
            g.drawLine(ML,y,ML+pw,y);
            g.setColor(TEXT2);
            g.drawString(String.valueOf(v),ML-22,y+4);
        }

        {
            int xa=toX(7,  xMin,xMax,pw)+ML;
            int xb=toX(29.7, xMin,xMax,pw)+ML;
            int xc=toX(25.7, xMin,xMax,pw)+ML;
            int xd=toX(4,  xMin,xMax,pw)+ML;
            int ya=toY(7,  yMin,yMax,ph)+MT;
            int yb=toY(4,  yMin,yMax,ph)+MT;
            int[] xs={xa,xb,xc,xd}, ys={ya,ya,yb,yb};
            g.setColor(new Color(200,220,255,180)); g.fillPolygon(xs,ys,4);
            g.setColor(NEGRO); g.setStroke(new BasicStroke(1.2f)); g.drawPolygon(xs,ys,4);
            g.setStroke(new BasicStroke(1f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,10,new float[]{3f,3f},0));
            g.drawLine(xd,yb,xc,yb);
            g.setStroke(new BasicStroke(1f));
            g.setFont(Tema.fuente(Font.BOLD, 11));
            g.setColor(new Color(30,50,100));
            g.drawString("CL-ML",xa+8,ya+13);
        }

        g.setColor(NEGRO); g.setStroke(new BasicStroke(2.6f));
        drawLineClipped(g,0,0,100,100,xMin,xMax,yMin,yMax,pw,ph);

        g.setColor(NEGRO); g.setStroke(new BasicStroke(2.6f));
        drawLineClipped(g,20,0,100,0.73*(100-20),xMin,xMax,yMin,yMax,pw,ph);

        g.setColor(NEGRO); g.setStroke(new BasicStroke(1.7f));
        drawLineClipped(g,50,0,50,60,xMin,xMax,yMin,yMax,pw,ph);

        g.setColor(new Color(200,25,25)); g.setStroke(new BasicStroke(2.0f));
        drawLineClipped(g,15,7,100,0.9*(100-8),xMin,xMax,yMin,yMax,pw,ph);

        g.setFont(Tema.fuente(Font.BOLD, 20));
        g.setColor(new Color(20,40,85));
        label(g,"CL",toX(40,xMin,xMax,pw)+ML,toY(23,yMin,yMax,ph)+MT);
        label(g,"CH",toX(67,xMin,xMax,pw)+ML,toY(40,yMin,yMax,ph)+MT);
        g.setFont(Tema.fuente(Font.BOLD, 18));
        label(g,"ML",toX(40,xMin,xMax,pw)+ML,toY(7, yMin,yMax,ph)+MT);
        label(g,"MH",toX(67,xMin,xMax,pw)+ML,toY(11,yMin,yMax,ph)+MT);

        g.setFont(Tema.fuente(Font.BOLD, 11));
        g.setColor(new Color(180,20,20));
        {
    Graphics2D gl=(Graphics2D)g.create();
    gl.setFont(Tema.ui(Font.BOLD, 11));
    gl.setColor(new Color(50,55,75));
    int lx=toX(65,xMin,xMax,pw)+ML+5, ly=toY(48,yMin,yMax,ph)+MT-4;
    gl.translate(lx,ly);
    gl.rotate(Math.atan2(-0.9*(ph/60.0), pw/100.0));
    gl.drawString("Ipu = 0,9(LL \u2212 8)",5,-4);
    gl.dispose();
}
g.setColor(new Color(255, 0, 0));
{
    Graphics2D gl=(Graphics2D)g.create();
    gl.setFont(Tema.ui(Font.BOLD, 11));
    gl.setColor(new Color(255, 0, 0));
    int lx=toX(70,xMin,xMax,pw)+ML, ly=toY(33,yMin,yMax,ph)+MT;
    gl.translate(lx,ly);
    gl.rotate(Math.atan2(-0.73*(ph/60.0), pw/100.0));
    gl.drawString("IpA = 0,73(LL \u2212 20)",5,-4);
    gl.dispose();
}

        g.setFont(Tema.fuente(Font.BOLD, 15));
        g.setColor(new Color(15,35,80));
        String tit="CARTA DE PLASTICIDAD - USCS";
        g.drawString(tit,ML+pw/2-g.getFontMetrics().stringWidth(tit)/2,28);

        g.setColor(NEGRO); g.setStroke(new BasicStroke(2.4f));
        g.drawLine(ML,MT+ph,ML+pw+18,MT+ph); fillArrow(g,ML+pw+18,MT+ph,true);
        g.drawLine(ML,MT+ph,ML,MT-15);       fillArrow(g,ML,MT-15,false);

        g.setFont(Tema.fuente(Font.BOLD, 13)); g.setColor(NEGRO);
        g.drawString("L\u00edmite L\u00edquido LL (%)",ML+pw/2-75,H-12);
        Graphics2D g2=(Graphics2D)g.create();
        g2.rotate(-Math.PI/2,26,MT+ph/2);
        g2.setFont(Tema.fuente(Font.BOLD, 13)); g2.setColor(NEGRO);
        g2.drawString("\u00cdndice de Plasticidad Ip (%)",8,MT+ph/2+8);
        g2.dispose();

        if(pLL!=null&&pIP!=null){
            int px=toX(pLL,xMin,xMax,pw)+ML, py=toY(pIP,yMin,yMax,ph)+MT;
            g.setColor(new Color(249,168,79,80));  g.fillOval(px-15,py-15,30,30);
            g.setColor(new Color(230,110,20));      g.fillOval(px-6, py-6, 12,12);
            String tag=String.format("LL = %.1f   IP = %.1f   \u2192 %s",pLL,pIP,pUSCS);
            FontMetrics fm=g.getFontMetrics(Tema.fuente(Font.BOLD, 11));
            int tw=fm.stringWidth(tag), lx=px+20, ly=py-18;
            if(lx+tw>ML+pw-25) lx=px-tw-25;
            g.setColor(new Color(255,255,255,240));
            g.fillRoundRect(lx-6,ly-14,tw+12,22,8,8);
            g.setColor(new Color(190,65,15));
            g.setFont(Tema.fuente(Font.BOLD, 11));
            g.drawString(tag,lx,ly);
        } else {
            g.setColor(new Color(140,150,170));
            g.setFont(Tema.fuente(Font.ITALIC, 14));
            g.drawString("Sin datos \u2014 presione Calcular",ML+pw/2-115,MT+ph/2);
        }
    }

    private void drawLineLabel(Graphics2D g,String text,double ll,double ip,double angleDeg,Color color,float fontSize){
        Graphics2D g2=(Graphics2D)g.create();
        g2.setFont(Tema.fuente(Font.BOLD,(int)fontSize));
        g2.setColor(color);
        int x=toX(ll,0,100,getWidth()-ML-MR)+ML;
        int y=toY(ip,0,60, getHeight()-MT-MB)+MT;
        g2.translate(x,y); g2.rotate(Math.toRadians(angleDeg));
        g2.drawString(text,10,-3); g2.dispose();
    }

    private void drawLineClipped(Graphics2D g,double x1,double y1,double x2,double y2,
                                 double xMin,double xMax,double yMin,double yMax,int pw,int ph){
        if(y1<yMin){double t=(yMin-y1)/(y2-y1);x1+=t*(x2-x1);y1=yMin;}
        if(y2>yMax){double t=(yMax-y1)/(y2-y1);x2=x1+t*(x2-x1);y2=yMax;}
        g.drawLine(toX(x1,xMin,xMax,pw)+ML,toY(y1,yMin,yMax,ph)+MT,
                   toX(x2,xMin,xMax,pw)+ML,toY(y2,yMin,yMax,ph)+MT);
    }

    private void fillArrow(Graphics2D g,int x,int y,boolean horizontal){
        int s=10;
        if(horizontal) g.fillPolygon(new int[]{x,x-s,x-s},new int[]{y,y-s/2,y+s/2},3);
        else           g.fillPolygon(new int[]{x,x-s/2,x+s/2},new int[]{y,y+s,y+s},3);
    }

    private void label(Graphics2D g,String txt,int x,int y){
        FontMetrics fm=g.getFontMetrics();
        g.drawString(txt,x-fm.stringWidth(txt)/2,y+fm.getAscent()/2-1);
    }

    private int toX(double v,double min,double max,int pw){return(int)((v-min)/(max-min)*pw);}
    private int toY(double v,double min,double max,int ph){return(int)((1-(v-min)/(max-min))*ph);}
}