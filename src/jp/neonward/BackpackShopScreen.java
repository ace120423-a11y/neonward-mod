package jp.neonward;

import com.google.gson.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class BackpackShopScreen extends Screen {
 int x,y,w,h,token;long cash,sentAt;boolean pending;String message="";JsonArray rows=new JsonArray();
 BackpackShopScreen(){super(Component.literal("マーケット / バッグ販売"));}
 public boolean isPauseScreen(){return false;}
 public static void initClient(){ClientPlayNetworking.registerGlobalReceiver(BackpackShop.Reply.TYPE,(payload,ctx)->ctx.client().execute(()->{var mc=ctx.client();if(!(mc.gui.screen() instanceof BackpackShopScreen))mc.gui.setScreen(new BackpackShopScreen());((BackpackShopScreen)mc.gui.screen()).receive(JsonParser.parseString(payload.json()).getAsJsonObject());}));}
 void receive(JsonObject data){token=data.get("token").getAsInt();cash=data.get("cash").getAsLong();message=data.get("message").getAsString();rows=data.getAsJsonArray("rows");pending=false;rebuildWidgets();}
 void buy(int tier){if(pending||minecraft.player==null)return;pending=true;sentAt=System.currentTimeMillis();minecraft.player.connection.sendCommand("neonbagshop buy "+token+" "+tier);rebuildWidgets();}
 @Override protected void init(){
  w=Math.min(420,width-16);h=Math.min(236,height-16);x=(width-w)/2;y=(height-h)/2;
  for(int i=0;i<rows.size();i++){var row=rows.get(i).getAsJsonObject();int tier=row.get("tier").getAsInt();var button=addRenderableWidget(new PhoneScreen.NeonButton(x+w-112,y+51+i*43,100,28,row.get("price").getAsInt()+" Cr",b->buy(tier)));button.active=!pending&&cash>=row.get("price").getAsInt();}
  addRenderableWidget(new PhoneScreen.NeonButton(x+w-82,y+h-26,70,18,"閉じる",b->onClose()));
 }
 @Override public void tick(){if(pending&&System.currentTimeMillis()-sentAt>4000){pending=false;message="応答待ちです。再度押すか、売場を開き直してください";rebuildWidgets();}}
 @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
  g.fill(x-3,y-3,x+w+3,y+h+3,0xff07121d);g.outline(x-3,y-3,w+6,h+6,0xff66e8c9);
  g.text(font,"CARRY / バッグ販売",x+12,y+10,0xff8cffe4);g.text(font,"残高 "+cash+" Cr",x+12,y+28,0xffffd889);
  for(int i=0;i<rows.size();i++){var row=rows.get(i).getAsJsonObject();int yy=y+50+i*43,tier=row.get("tier").getAsInt();g.item(new ItemStack(BackpackEquipment.ITEMS[tier]),x+12,yy+3);g.text(font,row.get("name").getAsString(),x+36,yy+3,0xffe1f4f4);g.text(font,"追加 +"+row.get("capacity").getAsInt()+"枠 / 装備は1個",x+36,yy+17,0xffa1c6c5);}
  g.text(font,"外しても中身はバッグに保存",x+12,y+h-48,0xffa1c6c5);g.text(font,font.plainSubstrByWidth(pending?"購入処理中…":message,w-100),x+12,y+h-20,0xffffd889);
  super.extractRenderState(g,mx,my,delta);
 }
}
