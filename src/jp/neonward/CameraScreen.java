package jp.neonward;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.NativeImage;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class CameraScreen extends Screen {
 int l,t,w,h,vx,vy,vw,vh,frames;boolean capturing,requested,closed;double zoom=1;int originalFov;
 CameraType originalCamera;PhotoStore.Picture latest;String status="移動キー＋画面ドラッグ / Enterで撮影";long flashUntil;boolean looking;
 public CameraScreen(){super(Component.literal("NEON CAM"));}
 public static boolean active(){return Minecraft.getInstance().gui.screen() instanceof CameraScreen;}
 @Override public boolean isPauseScreen(){return false;}
 @Override public void extractBackground(GuiGraphicsExtractor g,int x,int y,float d){}
 @Override protected void init(){
  if(originalCamera==null){originalCamera=minecraft.options.getCameraType();originalFov=minecraft.options.fov().get();minecraft.options.setCameraType(CameraType.FIRST_PERSON);}
  h=Math.min(370,height-12);w=Math.min(250,Math.min(width-24,(int)(h*.72)));l=(width-w)/2;t=(height-h)/2;
  vx=l+7;vy=t+37;vw=w-14;vh=h-112;
  addRenderableWidget(new PhoneScreen.NeonButton(l+8,t+h-68,38,27,"写真",b->roll()));
  addRenderableWidget(new Shutter(l+w/2-17,t+h-74,b->shoot()));
  addRenderableWidget(new PhoneScreen.NeonButton(l+w-46,t+h-68,38,27,"自撮り",b->{if(!capturing)minecraft.options.setCameraType(minecraft.options.getCameraType()==CameraType.THIRD_PERSON_FRONT?CameraType.FIRST_PERSON:CameraType.THIRD_PERSON_FRONT);}));
  addRenderableWidget(new PhoneScreen.NeonButton(l+w/2-20,vy+vh-19,40,16,String.format(java.util.Locale.ROOT,"%.1fx",zoom),b->{if(capturing)return;zoom=zoom==1?1.5:zoom==1.5?2:1;minecraft.options.fov().set((int)Math.max(30,originalFov/zoom));b.setMessage(Component.literal(String.format(java.util.Locale.ROOT,"%.1fx",zoom)));}));
  addRenderableWidget(new PhoneScreen.NeonButton(l+8,t+h-23,w-16,17,"ホームに戻る",b->onClose()));
  refreshLatest();
 }
 void refreshLatest(){if(latest!=null){latest.close();latest=null;}try{var files=PhotoStore.photos();if(!files.isEmpty())latest=new PhotoStore.Picture(files.getFirst());}catch(Exception e){status="写真を読み込めませんでした";}}
 void roll(){if(!capturing)minecraft.gui.setScreen(new CameraRollScreen());}
 void shoot(){if(capturing||minecraft.player==null)return;capturing=true;requested=false;frames=0;status="保存中…";}
 public void afterFrame(){
  if(!capturing||requested||closed||++frames<3)return;requested=true;
  final int left=vx,top=vy,wide=vw,high=vh,screenW=width,screenH=height;
  Screenshot.takeScreenshot(minecraft.gameRenderer.mainRenderTarget(),image->{
   String result="カメラロールに保存しました";
   try(image){
    int x=(int)((long)left*image.getWidth()/screenW),y=(int)((long)top*image.getHeight()/screenH);
    int cw=Math.max(1,(int)((long)wide*image.getWidth()/screenW)),ch=Math.max(1,(int)((long)high*image.getHeight()/screenH));
    cw=Math.min(cw,image.getWidth()-x);ch=Math.min(ch,image.getHeight()-y);
    try(var cropped=new NativeImage(cw,ch,false)){
     for(int row=0;row<ch;row++)for(int col=0;col<cw;col++)cropped.setPixel(col,row,image.getPixel(x+col,y+row));
     Files.createDirectories(PhotoStore.directory());
     Path file=PhotoStore.directory().resolve(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))+"_"+UUID.randomUUID().toString().substring(0,8)+".png");
     cropped.writeToFile(file);
    }
   }catch(Exception e){result="保存できませんでした：空き容量を確認";}
   final String message=result;minecraft.execute(()->{capturing=false;status=message;flashUntil=System.currentTimeMillis()+150;if(!closed)refreshLatest();});
  });
 }
 @Override public boolean keyPressed(KeyEvent key){
  if(key.key()==256){onClose();return true;}if(capturing)return true;
  if(key.key()==257){shoot();return true;}
  var o=minecraft.options;
  if(o.keyUp.matches(key)||o.keyDown.matches(key)||o.keyLeft.matches(key)||o.keyRight.matches(key)||o.keyJump.matches(key)||o.keyShift.matches(key)||o.keySprint.matches(key))return true;
  if(minecraft.player!=null){var p=minecraft.player;switch(key.key()){
   case 263:p.setYRot(p.getYRot()-4);return true;case 262:p.setYRot(p.getYRot()+4);return true;
   case 265:p.setXRot(Math.max(-90,p.getXRot()-4));return true;case 264:p.setXRot(Math.min(90,p.getXRot()+4));return true;
 }}return super.keyPressed(key);
 }
 @Override public boolean mouseClicked(MouseButtonEvent e,boolean twice){
  if(capturing)return true;
  if(super.mouseClicked(e,twice))return true;
  if(e.button()==0&&e.x()>=vx&&e.x()<vx+vw&&e.y()>=vy&&e.y()<vy+vh){looking=true;return true;}
  return false;
 }
 @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){
  if(looking){if(!capturing&&minecraft.player!=null){
   var p=minecraft.player;double s=minecraft.options.sensitivity().get();float gain=(float)((.25+s)*.65/zoom);
   p.setYRot(p.getYRot()+(float)dx*gain);p.setXRot(net.minecraft.util.Mth.clamp(p.getXRot()+(float)dy*gain,-90,90));
  }return true;}return super.mouseDragged(e,dx,dy);
 }
 @Override public boolean mouseReleased(MouseButtonEvent e){if(e.button()==0&&looking){looking=false;return true;}return super.mouseReleased(e);}
 @Override public void onClose(){if(!capturing)minecraft.gui.setScreen(new PhoneScreen());}
 @Override public void removed(){closed=true;if(originalCamera!=null){minecraft.options.setCameraType(originalCamera);minecraft.options.fov().set(originalFov);}if(latest!=null){latest.close();latest=null;}}
 @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
  if(capturing)return;
  g.fill(0,0,l,t+h+5,0xaa020710);g.fill(l+w,0,width,height,0xaa020710);g.fill(l,0,l+w,t,0xaa020710);g.fill(l,t+h,l+w,height,0xaa020710);
  g.fill(l-4,t-4,l+w+4,vy,0xff0a1420);g.fill(l-4,vy+vh,l+w+4,t+h+4,0xff0a1420);
  g.fill(l-4,vy,vx,vy+vh,0xff0a1420);g.fill(vx+vw,vy,l+w+4,vy+vh,0xff0a1420);
  g.outline(l-3,t-3,w+6,h+6,0xff57dfdf);g.text(font,"NEON CAM",l+9,t+13,0xff70fff0);
  g.text(font,java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),l+w-39,t+13,0xffff68b7);
  for(int xx:new int[]{vx+4,vx+vw-15})for(int yy:new int[]{vy+4,vy+vh-15}){g.fill(xx,yy,xx+11,yy+1,0xff70fff0);g.fill(xx,yy,xx+1,yy+11,0xff70fff0);}
  super.extractRenderState(g,mx,my,delta);
  if(latest!=null)latest.draw(g,l+10,t+h-66,34,23);
  g.centeredText(font,status,l+w/2,t+h-36,0xffbad9e3);
  if(System.currentTimeMillis()<flashUntil)g.fill(vx,vy,vx+vw,vy+vh,0x99ffffff);
 }
 static class Shutter extends Button {
  Shutter(int x,int y,OnPress press){super(x,y,34,34,Component.literal("撮影"),press,DEFAULT_NARRATION);}
  @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float d){int cx=getX()+17,cy=getY()+17;g.fill(getX(),getY(),getX()+34,getY()+34,0xff0a1420);for(int y=-16;y<=16;y++){int x=(int)Math.sqrt(256-y*y);g.fill(cx-x,cy+y,cx+x+1,cy+y+1,0xffaefff3);}for(int y=-12;y<=12;y++){int x=(int)Math.sqrt(144-y*y);g.fill(cx-x,cy+y,cx+x+1,cy+y+1,isHoveredOrFocused()?0xffffa0d4:0xffff549f);}}
 }
}
