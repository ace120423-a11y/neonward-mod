package jp.neonward;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class HomeScreen extends Screen {
 boolean owned,confirm;long cash;String message="物件を確認中";int x,y,w;
 public HomeScreen(){super(Component.literal("HIDEOUT / 不動産"));}
 @Override public boolean isPauseScreen(){return false;}
 @Override protected void init(){w=Math.min(380,width-16);x=(width-w)/2;y=Math.max(8,(height-190)/2);
  addRenderableWidget(new PhoneScreen.NeonButton(x+14,y+115,w-28,24,owned?"自分の部屋に入る":confirm?"10,000 Cr で購入を確定":"購入する / 10,000 Cr",b->{if(owned){send("enter");onClose();}else if(!confirm){confirm=true;rebuildWidgets();}else{confirm=false;send("buy");}}));
  addRenderableWidget(new PhoneScreen.NeonButton(x+14,y+151,w-28,22,"閉じる",b->onClose()));
 }
 void send(String verb){if(minecraft.player!=null)minecraft.player.connection.sendCommand("neonhome "+verb);}
 void receive(JsonObject o){owned=o.get("owned").getAsBoolean();cash=o.get("cash").getAsLong();message=o.get("message").getAsString();rebuildWidgets();}
 @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float d){g.fill(x-3,y-3,x+w+3,y+187,0xff08131b);g.outline(x-3,y-3,w+6,190,0xff55ffe0);g.text(font,"HIDEOUT / スラムの隠れ家",x+14,y+12,0xff66ffdf);g.text(font,"残高 "+cash+" Cr   "+(owned?"購入済み":"販売中"),x+14,y+34,0xffffce78);g.text(font,"寝室・リビング・バスルーム・武器庫",x+14,y+55,0xffeeeeee);g.text(font,"室内と収納はあなた専用 / 名前入り表札付き",x+14,y+73,0xffabcbd4);g.text(font,font.plainSubstrByWidth(message,w-28),x+14,y+95,0xffffc58a);super.extractRenderState(g,mx,my,d);}
}
