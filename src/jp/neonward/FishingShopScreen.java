package jp.neonward;
import com.google.gson.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
public final class FishingShopScreen extends Screen {
 JsonObject data;int x,y,w,page;boolean buyConfirm;long nextSend;
 FishingShopScreen(){super(Component.literal("AQUA PIER / 釣り堀"));}
 public boolean isPauseScreen(){return false;}
 void receive(JsonObject o){data=o;rebuildWidgets();}
 void send(String action){if(minecraft.player==null||System.currentTimeMillis()<nextSend)return;nextSend=System.currentTimeMillis()+200;minecraft.player.connection.sendCommand("neonfish "+action);}
 protected void init(){w=Math.min(430,width-16);x=(width-w)/2;y=Math.max(4,(height-230)/2);if(data==null)return;
  if(data.get("rods").getAsBoolean()){addRenderableWidget(new PhoneScreen.NeonButton(x+12,y+72,w-24,24,buyConfirm?"250 Crで釣り竿を1本購入する":"釣り竿 / 250 Cr",b->{if(buyConfirm){send("buy");buyConfirm=false;}else buyConfirm=true;rebuildWidgets();}));}
  else if(data.get("quote").getAsInt()>0){addRenderableWidget(new PhoneScreen.NeonButton(x+12,y+82,w-24,24,"1個売却を確定 / +"+data.get("quote").getAsInt()+" Cr",b->send("confirm")));addRenderableWidget(new PhoneScreen.NeonButton(x+12,y+112,w-24,22,"キャンセル",b->send("cancel")));}
  else{var rows=data.getAsJsonArray("rows");page=Math.max(0,Math.min(page,Math.max(0,(rows.size()-1)/4)));for(int j=0;j<4&&page*4+j<rows.size();j++){var row=rows.get(page*4+j).getAsJsonObject();String label="["+(row.get("slot").getAsInt()+1)+"] "+row.get("name").getAsString()+" ×"+row.get("count").getAsInt()+" / "+row.get("price").getAsInt()+" Cr";addRenderableWidget(new PhoneScreen.NeonButton(x+12,y+67+j*25,w-24,22,font.plainSubstrByWidth(label,w-40),b->send("select "+row.get("slot").getAsInt())));}addRenderableWidget(new PhoneScreen.NeonButton(x+12,y+170,80,20,"前へ",b->{page--;rebuildWidgets();}));addRenderableWidget(new PhoneScreen.NeonButton(x+w-92,y+170,80,20,"次へ",b->{page++;rebuildWidgets();}));}
  addRenderableWidget(new PhoneScreen.NeonButton(x+12,y+198,w-24,22,"閉じる",b->onClose()));
 }
 public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float d){g.fill(x-3,y-3,x+w+3,y+227,0xff08131b);g.outline(x-3,y-3,w+6,230,0xff55ffe0);if(data!=null){g.text(font,data.get("rods").getAsBoolean()?"AQUA PIER / 釣り竿販売":"AQUA PIER / 釣果買取",x+12,y+10,0xff66ffdf);g.text(font,"残高 "+data.get("cash").getAsLong()+" Cr",x+12,y+28,0xffffce78);g.text(font,font.plainSubstrByWidth(data.get("message").getAsString(),w-24),x+12,y+46,0xffffffff);}super.extractRenderState(g,mx,my,d);}
}
