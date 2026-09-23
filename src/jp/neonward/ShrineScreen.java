package jp.neonward;

import com.google.gson.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

/** Shrine-only quote UI. No transactions on open; every action names its single-use server token. */
public final class ShrineScreen extends Screen {
 JsonArray rows=new JsonArray();String heading="桜宮",message="";int kind=-1,token,x,y,w,h;long cash,sent;boolean pending;
 public ShrineScreen(){super(Component.literal("桜宮 / 参拝"));}
 @Override public boolean isPauseScreen(){return false;}
 void receive(JsonObject data){kind=data.get("kind").getAsInt();token=data.get("token").getAsInt();heading=data.get("title").getAsString();message=data.get("message").getAsString();cash=data.get("cash").getAsLong();rows=data.getAsJsonArray("rows");pending=false;rebuildWidgets();}
 void send(String action,int value){if(pending||minecraft.player==null)return;pending=true;sent=System.currentTimeMillis();minecraft.player.connection.sendCommand("neonshrine "+action+" "+token+" "+value);rebuildWidgets();}
 @Override protected void init(){
  w=Math.min(490,width-16);h=Math.min(292,height-16);x=(width-w)/2;y=(height-h)/2;
  for(int i=0;i<rows.size();i++){int index=i;var row=rows.get(i).getAsJsonObject();String action=row.get("action").getAsString();long price=row.get("price").getAsLong();
   String label=action.equals("offer")?"お参り開始":action.equals("wash")?"手水を始める":price+" Crで授与";
   var button=addRenderableWidget(new PhoneScreen.NeonButton(x+w-124,y+58+i*48,110,32,label,b->send(action,index)));button.active=!pending&&cash>=price;
  }
  addRenderableWidget(new PhoneScreen.NeonButton(x+w-108,y+h-50,94,20,"閉じる",b->onClose()));
 }
 @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){
  if(pending&&System.currentTimeMillis()-sent>4000){pending=false;message="応答を確認できません。参拝場所で開き直してください";rebuildWidgets();}
  g.fill(x-3,y-3,x+w+3,y+h+3,0xff21151d);g.outline(x-3,y-3,w+6,h+6,0xffffb2c5);g.fill(x,y,x+3,y+h,0xffcb3f58);
  g.text(font,heading,x+14,y+12,0xffffceda);g.text(font,"所持金 "+cash+" Cr",x+14,y+32,0xffffd684);
  for(int i=0;i<rows.size();i++){var row=rows.get(i).getAsJsonObject();int yy=y+58+i*48,icon=row.get("icon").getAsInt();
   g.fill(x+10,yy-3,x+w-10,yy+38,0xff382930);g.item(new ItemStack(icon>=0?ShrineServices.AMULETS[icon]:icon==-1?Items.GOLD_NUGGET:Items.WATER_BUCKET),x+17,yy+7);
   g.text(font,font.plainSubstrByWidth(row.get("name").getAsString(),w-177),x+42,yy+3,0xffffe8d6);g.text(font,font.plainSubstrByWidth(row.get("effect").getAsString(),w-177),x+42,yy+20,0xffd6babc);
  }
  if(kind==0)g.text(font,font.plainSubstrByWidth("攻撃+10% または被ダメージ-10% / 5分 / ランダム1種",w-28),x+14,y+h-94,0xffffd684);
  g.text(font,font.plainSubstrByWidth(kind==2?"右手で使用して装備 / 装飾品3枠 / 同種重複不可":kind==0?"10秒で完了 / 完了時のみ100 Cr / 加護中は再抽選不可":"8秒で完了 / 無料 / 移動・被ダメージで中断",w-28),x+14,y+h-72,0xffedc18e);
  g.text(font,font.plainSubstrByWidth(pending?"確認中…":message,w-28),x+14,y+h-20,0xffffd7a5);super.extractRenderState(g,mx,my,dt);
 }
 public static void initClient(){
  ClientPlayNetworking.registerGlobalReceiver(ShrineServices.Reply.TYPE,(packet,ctx)->ctx.client().execute(()->{
   var mc=ctx.client();var data=JsonParser.parseString(packet.json()).getAsJsonObject();
   if(data.has("ritualStarted")&&data.get("ritualStarted").getAsBoolean()){
    if(mc.gui.screen() instanceof ShrineScreen screen&&screen.kind==data.get("kind").getAsInt()&&(data.get("token").getAsInt()<0||screen.token==data.get("token").getAsInt()))screen.onClose();return;
   }
   if(!(mc.gui.screen() instanceof ShrineScreen))mc.gui.setScreen(new ShrineScreen());((ShrineScreen)mc.gui.screen()).receive(data);
  }));
  net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback.EVENT.register((stack,c,t,lines)->{for(int i=0;i<ShrineServices.AMULETS.length;i++)if(stack.is(ShrineServices.AMULETS[i])){lines.add(Component.literal(ShrineServices.EFFECTS[i]).withColor(0xffb2c5));lines.add(Component.literal("右手で使用して装備 / インベントリの装飾品3枠").withColor(0xffd684));lines.add(Component.literal("使い切りではありません / 同種のお守りは重複装備不可").withColor(0xffd684));}});
 }
}
