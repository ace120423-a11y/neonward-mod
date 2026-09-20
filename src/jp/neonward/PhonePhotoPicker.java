package jp.neonward;
import java.nio.file.*;
import java.util.*;
import java.io.*;
import java.awt.image.BufferedImage;
import javax.imageio.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Sharing is explicit: only a reduced copy of the selected camera-roll photo is uploaded. */
public final class PhonePhotoPicker extends Screen {
 final PhoneAppScreen back;List<Path> files=List.of();PhotoStore.Picture picture;int index,x,y,w,h;String message="写真を選んで添付 / 投稿ボタンを押すと公開";
 PhonePhotoPicker(PhoneAppScreen back){super(Component.literal("PULSE / 写真を添付"));this.back=back;}
 @Override public boolean isPauseScreen(){return false;}
 @Override protected void init(){w=Math.min(330,width-16);h=Math.min(310,height-12);x=(width-w)/2;y=(height-h)/2;try{files=PhotoStore.photos();}catch(Exception e){message="写真を読み込めません";}index=Math.min(index,Math.max(0,files.size()-1));load();
  addRenderableWidget(new PhoneScreen.NeonButton(x+12,y+h-56,42,19,"<",b->{index=Math.max(0,index-1);load();}));addRenderableWidget(new PhoneScreen.NeonButton(x+60,y+h-56,42,19,">",b->{index=Math.min(Math.max(0,files.size()-1),index+1);load();}));addRenderableWidget(new PhoneScreen.NeonButton(x+110,y+h-56,w-122,19,"この写真を添付",b->{if(files.isEmpty())return;try{String encoded=encode(files.get(index));if(encoded.isEmpty())throw new IOException();back.photo=encoded;back.notice="写真を添付しました。投稿ボタンで公開できます";minecraft.gui.setScreen(back);back.refresh();}catch(Exception e){message="この写真は添付できません";}}));addRenderableWidget(new PhoneScreen.NeonButton(x+12,y+h-29,w-24,19,"戻る",b->onClose()));}
 void load(){if(picture!=null)picture.close();picture=null;if(!files.isEmpty())try{picture=new PhotoStore.Picture(files.get(index));}catch(Exception e){message="写真を読み込めません";}}
 static String encode(Path p)throws Exception {BufferedImage original=ImageIO.read(p.toFile());if(original==null)throw new IOException();for(int size:new int[]{96,80,64,48}){double s=Math.min(1,Math.min((double)size/original.getWidth(),(double)size/original.getHeight()));var image=new BufferedImage(Math.max(1,(int)(original.getWidth()*s)),Math.max(1,(int)(original.getHeight()*s)),BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();g.drawImage(original,0,0,image.getWidth(),image.getHeight(),null);g.dispose();var bytes=new ByteArrayOutputStream();ImageIO.write(image,"jpg",bytes);if(bytes.size()<=4000)return Base64.getEncoder().encodeToString(bytes.toByteArray());}throw new IOException("photo size");}
 @Override public void onClose(){minecraft.gui.setScreen(back);back.refresh();}
 @Override public void removed(){if(picture!=null)picture.close();}
 @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float d){g.fill(x-3,y-3,x+w+3,y+h+3,0xff0a1421);g.outline(x-3,y-3,w+6,h+6,0xff66f5e3);g.text(font,"PULSE / CAMERA ROLL",x+12,y+12,0xff7bffeb);if(picture!=null)picture.draw(g,x+12,y+35,w-24,h-115);else g.text(font,"カメラで写真を撮るとここに表示されます",x+12,y+60,0xffc0d7e3);g.text(font,font.plainSubstrByWidth(message,w-24),x+12,y+h-74,0xffffbcdd);super.extractRenderState(g,mx,my,d);}
}
