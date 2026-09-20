package jp.neonward;
import java.nio.file.Path;
import java.util.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class CameraRollScreen extends Screen {
 List<Path> files=List.of();Map<Integer,PhotoStore.Picture> pictures=new HashMap<>();int page,selected=-1,l,t,w,h;String error="";
 public CameraRollScreen(){super(Component.literal("カメラロール"));try{files=PhotoStore.photos();}catch(Exception e){error="写真を読み込めませんでした";}}
 @Override public boolean isPauseScreen(){return false;}
 void change(int chosen){selected=chosen;clearWidgets();init();}
 @Override protected void init(){
  release();h=Math.min(370,height-12);w=Math.min(250,Math.min(width-24,(int)(h*.72)));l=(width-w)/2;t=(height-h)/2;
  if(selected>=0){load(selected);addRenderableWidget(new PhoneScreen.NeonButton(l+8,t+h-48,w-16,18,"一覧へ",b->change(-1)));}
  else {
   int cw=(w-24)/2,ch=(h-104)/3;
   for(int k=0;k<6;k++){int i=page*6+k;if(i>=files.size())break;load(i);addRenderableWidget(new PhoneScreen.NeonButton(l+8+(k%2)*(cw+8),t+38+(k/2)*ch,cw,ch-5,"",b->change(i)));}
   addRenderableWidget(new PhoneScreen.NeonButton(l+8,t+h-49,35,18,"前",b->{if(page>0){page--;change(-1);}}));
   addRenderableWidget(new PhoneScreen.NeonButton(l+w-43,t+h-49,35,18,"次",b->{if((page+1)*6<files.size()){page++;change(-1);}}));
  }
  addRenderableWidget(new PhoneScreen.NeonButton(l+8,t+h-24,w-16,18,"カメラに戻る",b->onClose()));
 }
 void load(int i){try{pictures.put(i,new PhotoStore.Picture(files.get(i)));}catch(Exception e){error="読み込めない写真があります";}}
 void release(){for(var p:pictures.values())p.close();pictures.clear();}
 @Override public void removed(){release();}
 @Override public void onClose(){minecraft.gui.setScreen(new CameraScreen());}
 @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
  g.fill(l-4,t-4,l+w+4,t+h+4,0xff09121e);g.outline(l-4,t-4,w+8,h+8,0xff58eddf);
  g.centeredText(font,"カメラロール / "+files.size()+"枚",l+w/2,t+12,0xff78fff0);
  super.extractRenderState(g,mx,my,delta);
  if(selected>=0){var photo=pictures.get(selected);if(photo!=null)photo.draw(g,l+8,t+34,w-16,h-91);}
  else {int cw=(w-24)/2,ch=(h-104)/3;for(var e:pictures.entrySet()){int k=e.getKey()-page*6;e.getValue().draw(g,l+10+(k%2)*(cw+8),t+40+(k/2)*ch,cw-4,ch-9);}
   g.centeredText(font,(page+1)+" / "+Math.max(1,(files.size()+5)/6),l+w/2,t+h-43,0xffc0e7ef);
   if(files.isEmpty())g.centeredText(font,"撮影した写真がここに並びます",l+w/2,t+76,0xffc0e7ef);
  }
  if(!error.isEmpty())g.centeredText(font,error,l+w/2,t+26,0xffff70b5);
 }
}
