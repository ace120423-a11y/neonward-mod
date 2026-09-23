package jp.neonward;
import com.google.gson.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class StockScreen extends Screen {
 final BlockPos pos;int l,t,w,h,selected,quantity=1,age;long bagOpenAt;JsonObject data;String status="接続しています…";
 public StockScreen(BlockPos p){super(Component.literal("PULSE EXCHANGE"));pos=p.immutable();}
 @Override public boolean isPauseScreen(){return false;}
 @Override protected void init(){
  w=Math.min(510,width-16);h=Math.min(300,height-16);l=(width-w)/2;t=(height-h)/2;
  for(int i=0;i<6;i++){final int s=i;addRenderableWidget(new PhoneScreen.NeonButton(l+10,t+47+i*21,98,19,MarketLedger.SYMBOLS[i],b->selected=s));}
  addRenderableWidget(new PhoneScreen.NeonButton(l+120,t+h-52,24,19,"-",b->quantity=Math.max(1,quantity/10)));
  addRenderableWidget(new PhoneScreen.NeonButton(l+202,t+h-52,24,19,"+",b->quantity=Math.min(1000,quantity*10)));
  addRenderableWidget(new PhoneScreen.NeonButton(l+234,t+h-52,70,19,"購入",b->send("buy")));
  addRenderableWidget(new PhoneScreen.NeonButton(l+310,t+h-52,70,19,"売却",b->send("sell")));
  if(minecraft.player!=null&&CompactShops.room(minecraft.player.level(),minecraft.player.blockPosition())==4)
   addRenderableWidget(new PhoneScreen.NeonButton(l+10,t+h-52,98,19,"バッグ販売",b->{if(bagOpenAt==0){bagOpenAt=System.currentTimeMillis()+250;b.active=false;status="バッグ売場を開いています…";}}));
  addRenderableWidget(new PhoneScreen.NeonButton(l+w-75,t+h-25,65,18,"閉じる",b->onClose()));send("view");
 }
 void send(String action){if(minecraft.player==null)return;if(!action.equals("view")&&(data==null||!data.has("prices")))return;int quote=data!=null&&data.has("prices")?data.getAsJsonArray("prices").get(selected).getAsInt():0;minecraft.player.connection.sendCommand("neonmarket "+action+" "+pos.getX()+" "+pos.getY()+" "+pos.getZ()+" "+selected+" "+quantity+" "+quote);}
 void receive(JsonObject d){if(d.get("x").getAsInt()!=pos.getX()||d.get("y").getAsInt()!=pos.getY()||d.get("z").getAsInt()!=pos.getZ())return;data=d;String m=d.get("message").getAsString();if(!m.isEmpty())status=m;else if(status.startsWith("接続"))status="銘柄と数量を選んで売買";}
 @Override public void tick(){
  // Give the last stock refresh time to clear the shared non-kicking UI limiter.
  if(bagOpenAt!=0){if(System.currentTimeMillis()>=bagOpenAt&&minecraft.player!=null){minecraft.player.connection.sendCommand("neonbagshop open");minecraft.gui.setScreen(null);}return;}
  if(++age%40==0)send("view");
 }
 @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
  g.fill(l-3,t-3,l+w+3,t+h+3,0xff07101a);g.outline(l-3,t-3,w+6,h+6,0xff55e5d7);
  g.text(font,"PULSE EXCHANGE / 街の株式市場",l+12,t+10,0xff7cfff0);g.text(font,"ゲーム内通貨 Cr / 30秒ごとに更新",l+12,t+25,0xff819fac);
  if(data!=null&&data.has("prices")){
   var prices=data.getAsJsonArray("prices");var account=data.getAsJsonObject("account");var held=account.getAsJsonArray("shares");long cash=account.get("cash").getAsLong(),assets=cash;
   for(int i=0;i<6;i++)assets+=(long)prices.get(i).getAsInt()*held.get(i).getAsInt();
   int x=l+120,y=t+48,cw=w-134,ch=Math.max(35,h-176);int price=prices.get(selected).getAsInt();
   g.text(font,MarketLedger.NAMES[selected]+" / "+price+" Cr",x,y,0xfff8d785);
   var history=data.getAsJsonArray("history").get(selected).getAsJsonArray();int lo=price,hi=price;for(var p:history){lo=Math.min(lo,p.getAsInt());hi=Math.max(hi,p.getAsInt());}lo-=2;hi+=2;
   int gy=y+19;g.fill(x,gy,x+cw,gy+ch,0xff10202c);for(int k=1;k<4;k++)g.fill(x,gy+k*ch/4,x+cw,gy+k*ch/4+1,0xff213745);
   for(int k=1;k<history.size();k++){int a=x+(k-1)*cw/Math.max(1,history.size()-1),b=x+k*cw/Math.max(1,history.size()-1),v=gy+ch-(history.get(k-1).getAsInt()-lo)*ch/(hi-lo),u=gy+ch-(history.get(k).getAsInt()-lo)*ch/(hi-lo);line(g,a,v,b,u);}
   if(history.size()==1)g.fill(x,gy+ch/2,x+cw,gy+ch/2+1,0xff62f6d7);
   g.text(font,"保有 "+held.get(selected).getAsInt()+" 株 / 評価 "+((long)held.get(selected).getAsInt()*price)+" Cr",x,gy+ch+6,0xffb8d4df);
   g.text(font,"残高 "+cash+" / 総資産 "+assets+" Cr",x,gy+ch+18,0xfff1da87);
   g.text(font,"実現損益 "+account.get("realized").getAsLong()+" Cr",x,gy+ch+30,0xff88d9c8);
   g.text(font,font.plainSubstrByWidth(data.get("news").getAsString(),w-22),l+10,t+h-70,0xffff77b7);
  }
  g.centeredText(font,quantity+" 株",l+173,t+h-46,0xffe7ffef);
  g.text(font,font.plainSubstrByWidth(status,w-94),l+10,t+h-19,0xffbdd8e2);
  super.extractRenderState(g,mx,my,delta);
 }
 static void line(GuiGraphicsExtractor g,int x,int y,int a,int b){int n=Math.max(Math.abs(a-x),Math.abs(b-y));for(int k=0;k<=n;k++){int px=x+(a-x)*k/Math.max(1,n),py=y+(b-y)*k/Math.max(1,n);g.fill(px,py,px+1,py+1,0xff62f6d7);}}
}
