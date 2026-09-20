package jp.neonward;
import java.nio.file.*;
import java.util.*;
import java.io.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.NativeImage;

public final class PhotoStore {
 static Path directory(){return Minecraft.getInstance().gameDirectory.toPath().resolve("neonward/photos");}
 static List<Path> photos() throws IOException {
  Files.createDirectories(directory());
  try(var files=Files.list(directory())){return files.filter(p->Files.isRegularFile(p)&&p.getFileName().toString().endsWith(".png")).sorted(Comparator.reverseOrder()).toList();}
 }
 static class Picture implements AutoCloseable {
  final Identifier id;final int width,height;
  Picture(Path path) throws IOException {
   NativeImage im;try(var stream=Files.newInputStream(path)){im=NativeImage.read(stream);}
   width=im.getWidth();height=im.getHeight();id=NeonWard.id("photo/"+UUID.randomUUID().toString());
   Minecraft.getInstance().getTextureManager().register(id,new DynamicTexture(()->"Phone photo",im));
  }
  Picture(byte[] bytes) throws IOException {
   NativeImage im;try(var in=new ByteArrayInputStream(bytes)){im=NativeImage.read(in);}
   width=im.getWidth();height=im.getHeight();id=NeonWard.id("photo/"+UUID.randomUUID());
   Minecraft.getInstance().getTextureManager().register(id,new DynamicTexture(()->"PULSE photo",im));
  }
  void draw(GuiGraphicsExtractor g,int x,int y,int w,int h){
   double scale=Math.min((double)w/width,(double)h/height);int a=Math.max(1,(int)(width*scale)),b=Math.max(1,(int)(height*scale));
   int left=x+(w-a)/2,top=y+(h-b)/2;g.blit(id,left,top,left+a,top+b,0f,1f,0f,1f);
  }
  public void close(){Minecraft.getInstance().getTextureManager().release(id);}
 }
}
