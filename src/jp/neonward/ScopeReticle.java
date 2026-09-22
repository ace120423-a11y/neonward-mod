package jp.neonward;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
/** No moving crosshair, tinted center or rarity-dependent bloom: the aim point stays readable. */
public final class ScopeReticle {
 static void ring(GuiGraphicsExtractor g,int x,int y,int radius,int color){int lastX=radius,lastY=0;for(int a=1;a<=180;a++){double t=a*Math.PI/90;int dx=(int)Math.round(Math.cos(t)*radius),dy=(int)Math.round(Math.sin(t)*radius);line(g,x+lastX,y+lastY,x+dx,y+dy,color);lastX=dx;lastY=dy;}}
 static void line(GuiGraphicsExtractor g,int x,int y,int xx,int yy,int color){int steps=Math.max(Math.abs(xx-x),Math.abs(yy-y));for(int i=0;i<=steps;i++){int a=x+(xx-x)*i/Math.max(1,steps),b=y+(yy-y)*i/Math.max(1,steps);g.fill(a,b,a+1,b+1,color);}}
 public static boolean draw(GuiGraphicsExtractor g,ItemStack gun){
  int c=GunAttachments.installed(gun,0);if(c<0)return false;int kind=c/5,x=g.guiWidth()/2,y=g.guiHeight()/2,color=0xffdd554d;
  if(kind<2){
   int rim=kind==0?34:43;
   g.fill(x-rim-3,y-rim,x-rim,y+rim,0xff1a242c);g.fill(x+rim,y-rim,x+rim+3,y+rim,0xff1a242c);
   g.fill(x-rim,y-rim-3,x+rim,y-rim,0xff1a242c);g.fill(x-rim,y+rim,x+rim,y+rim+5,0xff1a242c);
   g.outline(x-rim-1,y-rim-1,rim*2+2,rim*2+3,0xff697a85);
   g.fill(x-22,y+rim+5,x+22,y+rim+11,0xff1a242c);
   if(kind==1){ring(g,x,y,18,0xbbdd554d);g.fill(x-1,y-26,x+1,y-21,0xbbdd554d);g.fill(x-1,y+22,x+1,y+27,0xbbdd554d);}
   g.fill(x-2,y-2,x+3,y+3,0x44300000);g.fill(x,y,x+1,y+1,color);
   return true;
  }
  int radius=Math.max(36,(int)(Math.min(g.guiWidth(),g.guiHeight())*.39));
  // Opaque eye cup, completely clear central lens; only one-pixel reticle strokes.
  for(int yy=0;yy<g.guiHeight();yy++){int dy=yy-y;if(Math.abs(dy)>=radius)g.fill(0,yy,g.guiWidth(),yy+1,0xff06090c);else{int dx=(int)Math.sqrt(radius*radius-dy*dy);g.fill(0,yy,x-dx,yy+1,0xff06090c);g.fill(x+dx+1,yy,g.guiWidth(),yy+1,0xff06090c);}}
  ring(g,x,y,radius,0xff78909b);ring(g,x,y,radius-2,0xff202b34);ring(g,x,y,radius+3,0xff151e25);
  int reach=radius-14;g.fill(x-reach,y-1,x-7,y+2,0x777f939b);g.fill(x+8,y-1,x+reach,y+2,0x777f939b);g.fill(x-1,y-reach,x+2,y-7,0x777f939b);g.fill(x-1,y+8,x+2,y+reach,0x777f939b);
  g.fill(x-reach,y,x-7,y+1,0xff192229);g.fill(x+8,y,x+reach,y+1,0xff192229);g.fill(x,y-reach,x+1,y-7,0xff192229);g.fill(x,y+8,x+1,y+reach,0xff192229);
  int step=kind==3?12:18;for(int d=step;d<reach-5;d+=step){int size=d%(step*2)==0?4:2;g.fill(x+d,y-size,x+d+1,y+size+1,0xff192229);g.fill(x-d,y-size,x-d+1,y+size+1,0xff192229);g.fill(x-size,y+d,x+size+1,y+d+1,0xff192229);}
  g.fill(x,y,x+1,y+1,0xffe95348);
  var font=net.minecraft.client.Minecraft.getInstance().font;g.centeredText(font,kind==3?"8×  PRECISION":"4×  MIL",x,y+radius-21,0xff50656e);
  return true;
 }
}
