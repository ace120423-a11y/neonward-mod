package jp.neonward;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
public class TelevisionScreen extends Screen {
 String command="neontv ";
 public TelevisionScreen(net.minecraft.core.BlockPos pos){this();command="interiortv "+pos.getX()+" "+pos.getY()+" "+pos.getZ()+" ";}
 int x,y,w;EditBox url;String message="";
 public TelevisionScreen(){super(Component.literal("PULSE TV"));}
 @Override public boolean isPauseScreen(){return false;}
 @Override protected void init(){w=Math.min(360,width-20);x=(width-w)/2;y=(height-212)/2;
  String previous=url==null?"":url.getValue();
  url=new EditBox(font,x+12,y+45,w-24,22,Component.literal("YouTube URL"));url.setMaxLength(TelevisionUrl.MAX_LENGTH);url.setValue(previous);url.setHint(Component.literal("https://youtu.be/..."));addRenderableWidget(url);setInitialFocus(url);
  addRenderableWidget(new PhoneScreen.NeonButton(x+12,y+73,130,20,"コピーしたURLを全文貼る",b->{String s=minecraft.keyboardHandler.getClipboard().trim();if(s.length()>TelevisionUrl.MAX_LENGTH){message="長すぎます。YouTubeの「共有」からコピーしてね";return;}url.setValue(s);message="全文を貼り付けました（"+s.length()+"文字）";setFocused(url);}));
  addRenderableWidget(new PhoneScreen.NeonButton(x+12,y+119,w-24,22,"この動画をテレビで開く",b->send(url.getValue())));
  addRenderableWidget(new PhoneScreen.NeonButton(x+12,y+149,(w-30)/2,20,"YouTubeホーム",b->send("home")));
  addRenderableWidget(new PhoneScreen.NeonButton(x+18+(w-30)/2,y+149,(w-30)/2,20,"電源OFF",b->send("off")));
  addRenderableWidget(new PhoneScreen.NeonButton(x+12,y+180,w-24,19,"閉じる",b->onClose()));
 }
 void send(String s){if(!MediaBridge.available()){message="テレビ用MODが読み込まれていません";return;}try{String normalized=TelevisionUrl.normalize(s);if(minecraft.player!=null){minecraft.player.connection.sendCommand(command+(s.equals("off")?"off":s.equals("home")?"home":normalized));onClose();}}catch(IllegalArgumentException ex){message=ex.getMessage();}}
 @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){g.fill(x-3,y-3,x+w+3,y+212,0xff08131f);g.outline(x-3,y-3,w+6,215,0xff61efe0);g.text(font,"PULSE TV / YouTube",x+12,y+12,0xff82fff0);g.text(font,"YouTubeの「共有」→「コピー」で動画のURLを取得",x+12,y+29,0xffb9d4de);g.text(font,url.getValue().length()+" / "+TelevisionUrl.MAX_LENGTH+"文字",x+151,y+79,0xffb9d4de);g.text(font,message,x+12,y+102,0xffffc777);super.extractRenderState(g,mx,my,delta);}
}
