package jp.neonward;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import com.google.gson.JsonObject;
public final class CyberwareGachaScreen extends Screen {
 int x,y,w,h,token,prize=-1,value;long cash,reveal;boolean pending;String message="";PhoneScreen.NeonButton spin;
 public CyberwareGachaScreen(){super(Component.literal("CYBER CAPSULE"));}
 public boolean isPauseScreen(){return false;}
 void receive(JsonObject o){token=o.get("token").getAsInt();cash=o.get("cash").getAsLong();message=o.get("message").getAsString();pending=false;if(o.get("prize").getAsInt()>=0){prize=o.get("prize").getAsInt();value=o.get("value").getAsInt();}reveal=System.currentTimeMillis()+o.get("remaining").getAsLong()*50;}
 protected void init(){w=Math.min(410,width-16);h=Math.min(310,height-16);x=(width-w)/2;y=(height-h)/2;
  spin=addRenderableWidget(new PhoneScreen.NeonButton(x+12,y+h-50,w-24,20,"1回まわす / 1,000 Cr",b->{if(pending||System.currentTimeMillis()<reveal||minecraft.player==null)return;pending=true;spin.active=false;minecraft.player.connection.sendCommand("neongacha roll "+token);}));
  addRenderableWidget(new PhoneScreen.NeonButton(x+12,y+h-26,w-24,19,"閉じる",b->onClose()));
 }
 public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){long now=System.currentTimeMillis();boolean anim=now<reveal;spin.active=!pending&&!anim&&cash>=1000;
  g.fill(x-3,y-3,x+w+3,y+h+3,0xff08111e);g.outline(x-3,y-3,w+6,h+6,0xff5ffff0);g.centeredText(font,"CYBER CAPSULE",x+w/2,y+10,0xff70fff0);g.centeredText(font,cash+" Cr / 必ずサイバーウェア1個",x+w/2,y+25,0xfff3dc94);
  int color=0xff000000|(prize<0?0x66ffee:CyberwareCatalog.COLORS[CyberwareCatalog.PARTS[prize].tier()]);int cy=y+Math.max(55,(h-175)/2);
  g.fill(x+w/2-28,cy,x+w/2+28,cy+42,0xff142d3b);g.outline(x+w/2-28,cy,56,42,anim?0xff66ffee:color);g.centeredText(font,anim?"◇ "+"・".repeat((int)(now/180%4)):prize>=0?"GET!":"?",x+w/2,cy+17,anim?0xff66ffee:color);
  String name=prize>=0&&!anim?CyberwareCatalog.PARTS[prize].name():anim?"カプセルを解析中…":"CYBERWARE COLLECTION";g.centeredText(font,name,x+w/2,cy+47,color);
  if(prize>=0&&!anim)g.centeredText(font,CyberwareCatalog.RARITIES[CyberwareCatalog.PARTS[prize].tier()]+" / "+CyberwareCatalog.effect(prize,value),x+w/2,cy+60,color);
  String[] lines={"コモン41.5% / アンコモン30% / レア20%","エピック8% / レジェンダリー0.5%","部位・系統は均等 / 重複あり / 空き1枠必要",anim?"結果のアイテムはすでに持ち物へ受け取り済み":message};for(int i=0;i<lines.length;i++)g.centeredText(font,lines[i],x+w/2,y+h-104+i*12,i==3?0xffffd684:0xffa8c7d8);
  super.extractRenderState(g,mx,my,delta);
 }
}
