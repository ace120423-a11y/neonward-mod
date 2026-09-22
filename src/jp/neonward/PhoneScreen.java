package jp.neonward;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class PhoneScreen extends Screen {
 @Override public void onClose(){if(minecraft!=null)minecraft.gui.setScreen(null);}
 static final String[] APPS={"友達","電話","トーク","PULSE","マップ","カメラ","録音","送金","土地・住宅","クエスト","称号","ガレージ"};
 private int page=-1,l,t,w,h;
 public PhoneScreen(){super(Component.literal("NEON LINK"));}
 @Override public boolean isPauseScreen(){return false;}
 void open(int app){if(app==0){minecraft.gui.setScreen(new FriendsScreen());return;}if(app==11){minecraft.gui.setScreen(new GarageScreen());return;}if(app==4){minecraft.gui.setScreen(new CityMapScreen());return;}if(app==5){NeonClient.photo();return;}if(app>=0){minecraft.gui.setScreen(new PhoneAppScreen(app));return;}page=app;clearWidgets();init();}
 @Override protected void init(){
  h=Math.min(370,height-12);w=Math.min(250,Math.min(width-24,(int)(h*.72)));l=(width-w)/2;t=(height-h)/2;
  if(page==-1){
   int gap=(w-20)/4,step=(h-70)/3,size=Math.min(42,gap-8);
   for(int i=0;i<APPS.length;i++){final int app=i;int x=l+10+(i%4)*gap+(gap-size)/2,y=t+42+(i/4)*step;
    addRenderableWidget(new AppButton(x,y,size,app,b->open(app)));
   }
  }
  if(page==11){
   addRenderableWidget(new NeonButton(l+15,t+87,w-30,24,"車を呼び出す",b->callVehicle("car")));
   addRenderableWidget(new NeonButton(l+15,t+125,w-30,24,"バイクを呼び出す",b->callVehicle("bike")));
  }
  addRenderableWidget(new NeonButton(l+15,t+h-25,w-30,18,page==-1?"閉じる":"ホームに戻る",b->{if(page==-1)onClose();else open(-1);}));
 }
 @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
  g.fill(l-7,t-5,l+w+7,t+h+5,0xff080b12);g.outline(l-5,t-3,w+10,h+6,0xff334653);
  g.fill(l,t,l+w,t+h,0xff09121e);
  for(int y=t+29;y<t+h-29;y+=4)g.fill(l+2,y,l+w-2,y+1,0xff0c1928);
  for(int x=l+8;x<l+w-3;x+=18)g.fill(x,t+29,x+1,t+h-30,0xff102033);
  g.fill(l-2,t+8,l,t+h/2,0xff59f8e9);g.fill(l+w,t+h/2,l+w+2,t+h-8,0xffff4eac);
  g.fill(l+w/2-22,t-1,l+w/2+22,t+3,0xff03060a);
  g.text(font,LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),l+10,t+11,0xffaafff5);
  g.text(font,"NEON LINK",l+w-64,t+11,0xff60f8e7);
  g.fill(l+10,t+26,l+w-10,t+27,0xff235064);
  int sweep=(int)((System.currentTimeMillis()/90) % Math.max(1,w-20));
  g.fill(l+10+sweep,t+26,l+Math.min(w-10,18+sweep),t+27,0xffff51b3);
  if(page>=0){
   g.centeredText(font,APPS[page],l+w/2,t+36,0xffa7dbe5);
   if(page==4)map(g);
   else if(page==11){
    g.centeredText(font,"NEON MOTOR / 配車",l+w/2,t+61,0xff70fff0);
    g.centeredText(font,"近くの空きスペースへ呼び出す",l+w/2,t+165,0xffa7dbe5);
    g.centeredText(font,"近くの自分の車両は再利用",l+w/2,t+181,0xffa7dbe5);
   }
   else {
    String[] lines={};
    for(int i=0;i<lines.length;i++)g.centeredText(font,lines[i],l+w/2,t+68+i*17,0xffa7dbe5);
   }
  }
  super.extractRenderState(g,mx,my,delta);
 }
 void callVehicle(String type){
  if(minecraft.player!=null)minecraft.player.connection.sendCommand("neongarage "+type);
  onClose();
 }
 void map(GuiGraphicsExtractor g){
  int x=l+12,y=t+55,mw=w-24,mh=h-110;g.fill(x,y,x+mw,y+mh,0xff142737);g.outline(x,y,mw,mh,0xfff052bd);
  int a=x+(int)(mw*176.0/608),c=y+(int)(mh*170.0/736);g.fill(a-2,y,a+2,y+mh,0xff4d677a);g.fill(x,c-2,x+mw,c+2,0xff4d677a);
  mark(g,x,y,mw,mh,80,237,"HOME",0xffff66ac);mark(g,x,y,mw,mh,160,154,"PLAZA",0xffa1ffff);mark(g,x,y,mw,mh,80,450,"ARMS",0xffffb751);mark(g,x,y,mw,mh,244,451,"ARMOR",0xffffb751);
  for(double[] p:new double[][]{{160,-16},{160,688},{-16,154},{560,154}})mark(g,x,y,mw,mh,p[0],p[1],"GATE",0xff80ffff);
  if(minecraft.player!=null){var p=minecraft.player;mark(g,x,y,mw,mh,p.getX(),p.getZ(),"YOU",0xffffff40);g.centeredText(font,String.format("X %.0f  Y %.0f  Z %.0f",p.getX(),p.getY(),p.getZ()),l+w/2,t+h-43,0xffa7dbe5);}
 }
 void mark(GuiGraphicsExtractor g,int x,int y,int w,int h,double px,double pz,String name,int color){int xx=x+(int)Math.max(2,Math.min(w-3,(px+32)/608*w)),yy=y+(int)Math.max(2,Math.min(h-3,(pz+32)/736*h));g.fill(xx-2,yy-2,xx+2,yy+2,color);g.text(font,name,Math.min(x+w-font.width(name),xx+3),Math.max(y,yy-6),color);}
 static class AppButton extends Button {
  final int app,size;AppButton(int x,int y,int s,int a,OnPress press){super(x,y,s,s+15,Component.literal(APPS[a]),press,DEFAULT_NARRATION);app=a;size=s;}
  @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta){
   int x=getX(),y=getY();int accent=app==3||app==2||app==6?0xffff5caf:0xff57efed;
   g.fill(x,y,x+size,y+size+15,0xff09121e);
   g.fill(x,y,x+size,y+size,isHoveredOrFocused()?0xff193a4d:0xff112537);
   g.outline(x,y,size,size,isHoveredOrFocused()?accent:0xff34536a);
   g.fill(x,y,x+8,y+2,accent);g.fill(x,y,x+2,y+8,accent);
   g.fill(x+size-8,y+size-2,x+size,y+size,accent);g.fill(x+size-2,y+size-8,x+size,y+size,accent);

   g.pose().pushMatrix();g.pose().translate(x+size*.15f,y+size*.15f);g.pose().scale(size*.7f/32,size*.7f/32);icon(g,app);g.pose().popMatrix();
   g.centeredText(Minecraft.getInstance().font,APPS[app],x+size/2,y+size+4,0xffd0eaf0);
  }
 }
 static class NeonButton extends Button {
  NeonButton(int x,int y,int w,int h,String text,OnPress press){super(x,y,w,h,Component.literal(text),press,DEFAULT_NARRATION);}
  @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta){
   int x=getX(),y=getY(),w=getWidth(),h=getHeight();
   g.fill(x,y,x+w,y+h,isHoveredOrFocused()?0xff243e53:0xff122333);
   g.outline(x,y,w,h,isHoveredOrFocused()?0xffff5caf:0xff3b9fae);
   g.fill(x,y,x+3,y+h,0xff55f4e4);
   g.centeredText(Minecraft.getInstance().font,getMessage(),x+w/2,y+(h-8)/2,0xffd5fff9);
  }
 }
 static final int INK=0xff70fff0;
 static void line(GuiGraphicsExtractor g,int x,int y,int X,int Y){int n=Math.max(Math.abs(X-x),Math.abs(Y-y));for(int i=0;i<=n;i++){int a=x+(X-x)*i/Math.max(1,n),b=y+(Y-y)*i/Math.max(1,n);g.fill(a-1,b-1,a+2,b+2,INK);}}
 static void ring(GuiGraphicsExtractor g,int x,int y,int r){for(int i=0;i<36;i++){double a=i*Math.PI/18,b=(i+1)*Math.PI/18;line(g,x+(int)(r*Math.cos(a)),y+(int)(r*Math.sin(a)),x+(int)(r*Math.cos(b)),y+(int)(r*Math.sin(b)));}}
 static void rect(GuiGraphicsExtractor g,int x,int y,int X,int Y){line(g,x,y,X,y);line(g,X,y,X,Y);line(g,X,Y,x,Y);line(g,x,Y,x,y);}
 static void icon(GuiGraphicsExtractor g,int app){switch(app){
  case 0->{ring(g,16,9,6);rect(g,6,20,26,28);}
  case 1->{line(g,5,3,3,12);line(g,3,12,12,24);line(g,12,24,24,29);rect(g,3,2,10,9);rect(g,23,22,29,29);}
  case 2->{rect(g,3,5,29,24);line(g,8,24,6,30);line(g,6,30,16,24);for(int x:new int[]{9,16,23})g.fill(x-1,13,x+2,16,INK);}
  case 3->{line(g,2,17,8,17);line(g,8,17,12,6);line(g,12,6,19,27);line(g,19,27,24,13);line(g,24,13,29,13);}
  case 4->{ring(g,16,16,13);line(g,16,4,11,21);line(g,11,21,21,11);line(g,21,11,16,28);}
  case 5->{rect(g,3,8,29,27);ring(g,16,18,7);rect(g,7,4,14,8);}
  case 6->{rect(g,11,2,21,21);line(g,6,15,6,23);line(g,6,23,16,28);line(g,16,28,26,23);line(g,26,23,26,15);line(g,16,28,16,31);}
  case 7->{ring(g,16,16,13);g.centeredText(Minecraft.getInstance().font,"$",16,11,INK);}
  case 8->{line(g,2,14,16,2);line(g,16,2,30,14);rect(g,7,14,25,29);rect(g,14,21,19,29);}
  case 9->{rect(g,6,2,27,30);for(int y:new int[]{9,16,23}){g.fill(10,y,13,y+3,INK);line(g,17,y+1,23,y+1);}}
  case 11->{rect(g,3,12,29,24);line(g,7,12,11,5);line(g,11,5,23,5);line(g,23,5,27,12);ring(g,9,25,4);ring(g,24,25,4);}
  case 10->{ring(g,16,12,9);line(g,10,21,6,30);line(g,22,21,26,30);line(g,6,30,15,25);line(g,26,30,18,25);line(g,16,6,16,17);line(g,11,10,21,13);line(g,21,10,11,16);}
 }}
}

