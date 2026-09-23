package jp.neonward;
import com.google.gson.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
/** A service-specific server quote: no shared guild/clinic routing. */
public final class LeisureShopScreen extends Screen {
 JsonArray rows=new JsonArray();String title="",message="";int kind=-1,token,page,per=4,x,y,w,h;long cash,sent;boolean pending;
 public LeisureShopScreen(){super(Component.literal("NEON / SHOP"));}
 public boolean isPauseScreen(){return false;}
 void receive(JsonObject o){int k=o.get("kind").getAsInt();if(k!=kind)page=0;kind=k;token=o.get("token").getAsInt();title=o.get("title").getAsString();message=o.get("message").getAsString();cash=o.get("cash").getAsLong();rows=o.getAsJsonArray("rows");pending=false;rebuildWidgets();}
 void send(String action,int value){if(pending||minecraft.player==null)return;pending=true;sent=System.currentTimeMillis();minecraft.player.connection.sendCommand("neonleisure "+action+" "+token+" "+value);if(action.equals("enter"))onClose();else rebuildWidgets();}
 protected void init(){w=Math.min(490,width-16);h=Math.min(340,height-16);x=(width-w)/2;y=(height-h)/2;per=Math.max(1,(h-112)/42);page=Math.min(page,Math.max(0,(rows.size()-1)/per));
  for(int j=0;j<per&&page*per+j<rows.size();j++){int index=page*per+j;var row=rows.get(index).getAsJsonObject();String a=row.get("action").getAsString();String label=a.equals("summon")?"呼び出す":a.equals("enter")?"入る":row.get("price").getAsLong()+" Cr";var b=addRenderableWidget(new PhoneScreen.NeonButton(x+w-114,y+57+j*42,102,30,label,z->send(a,index)));b.active=!pending&&row.get("enabled").getAsBoolean()&&(!a.equals("buy")||cash>=row.get("price").getAsLong());}
  addRenderableWidget(new PhoneScreen.NeonButton(x+12,y+h-49,32,18,"＜",z->{page=Math.max(0,page-1);rebuildWidgets();}));
  addRenderableWidget(new PhoneScreen.NeonButton(x+50,y+h-49,32,18,"＞",z->{page=Math.min(Math.max(0,(rows.size()-1)/per),page+1);rebuildWidgets();}));
  if(kind==1){var b=addRenderableWidget(new PhoneScreen.NeonButton(x+w-220,y+h-49,102,18,"ペットを帰還",z->send("recall",0)));b.active=!pending;}
  addRenderableWidget(new PhoneScreen.NeonButton(x+w-108,y+h-49,96,18,"閉じる",z->onClose()));
 }
 public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){if(pending&&System.currentTimeMillis()-sent>4000){pending=false;message="応答待ちです。再試行するか店舗を開き直してください";rebuildWidgets();}
  int accent=kind==2?0xffff67ac:kind==0?0xffffbe66:0xff60f5df;
  g.fill(x-3,y-3,x+w+3,y+h+3,0xff08111c);g.outline(x-3,y-3,w+6,h+6,accent);g.fill(x,y,x+3,y+h,accent);
  g.text(font,font.plainSubstrByWidth(title,w-24),x+12,y+12,accent);g.text(font,"残高 "+cash+" Cr",x+12,y+32,0xffffd684);
  for(int j=0;j<per&&page*per+j<rows.size();j++){var r=rows.get(page*per+j).getAsJsonObject();int yy=y+57+j*42;g.fill(x+9,yy-3,x+w-9,yy+35,kind==0?0xff30251e:0xff132633);int inset=14;
   if(kind==0){String name=r.get("name").getAsString();for(int i=0;i<StreetMeals.NAMES.length;i++)if(StreetMeals.NAMES[i].equals(name)){g.fill(x+13,yy+2,x+37,yy+28,0xff17130f);g.item(new net.minecraft.world.item.ItemStack(StreetMeals.ITEMS[i]),x+17,yy+7);inset=43;break;}}
   g.text(font,font.plainSubstrByWidth(r.get("name").getAsString(),w-inset-124),x+inset,yy+3,0xffe4fff9);g.text(font,font.plainSubstrByWidth(r.get("effect").getAsString(),w-inset-124),x+inset,yy+18,0xffb3c8d3);}
  g.text(font,(page+1)+" / "+Math.max(1,(rows.size()+per-1)/per),x+90,y+h-44,0xff9ab4c4);g.text(font,font.plainSubstrByWidth(pending?"処理中…":message,w-24),x+12,y+h-19,0xffffbe66);super.extractRenderState(g,mx,my,dt);
 }
 static void initClient(){net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(LeisureShop.Reply.TYPE,(packet,ctx)->ctx.client().execute(()->{var mc=ctx.client();if(!(mc.gui.screen() instanceof LeisureShopScreen))mc.gui.setScreen(new LeisureShopScreen());((LeisureShopScreen)mc.gui.screen()).receive(JsonParser.parseString(packet.json()).getAsJsonObject());}));
  net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback.EVENT.register((s,c,t,lines)->{for(int i=0;i<StreetMeals.ITEMS.length;i++)if(s.is(StreetMeals.ITEMS[i]))lines.add(Component.literal(StreetMeals.EFFECTS[i]).withColor(0xffbe66));});
 }
}
