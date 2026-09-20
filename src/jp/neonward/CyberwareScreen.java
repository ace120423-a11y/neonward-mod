package jp.neonward;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import com.google.gson.JsonObject;

public final class CyberwareScreen extends Screen {
 final BlockPos pos;int x,y,w,h,selected,family=0,tier=0;int[] counts=new int[CyberwareCatalog.PARTS.length],rolls=new int[CyberwareCatalog.PARTS.length];boolean requested,editable;MarketLedger.Account account=new MarketLedger.Account();String message="接続中…";
 public CyberwareScreen(BlockPos p){super(Component.literal("NEURAL / CYBERWARE"));pos=p;account.cash=0;}
 @Override public boolean isPauseScreen(){return false;}
 @Override protected void init(){
  w=Math.min(510,width-8);h=Math.min(300,height-8);x=(width-w)/2;y=(height-h)/2;int side=(w-112)/2,row=Math.min(23,(h-163)/5);
  for(int i=0;i<10;i++){final int slot=i;int bx=i<5?x+12:x+w-side-12,by=y+39+(i%5)*row;Integer installed=account.cyberSlots.get(i);String label=(selected==i?"> ":"")+CyberwareCatalog.SLOTS[i]+"  "+(installed==null?"[ + ]":new String[]{"攻","HP","防","速"}[CyberwareCatalog.PARTS[installed].family()]+CyberwareCatalog.percent(account.cyberRolls.getOrDefault(i,0))+"%");addRenderableWidget(new PhoneScreen.NeonButton(bx,by,side,row-1,label,b->{selected=slot;if(installed!=null){family=CyberwareCatalog.PARTS[installed].family();tier=CyberwareCatalog.PARTS[installed].tier();}rebuildWidgets();}));}
  int tabs=(w-20)/5;for(int i=0;i<5;i++){final int t=i;addRenderableWidget(new PhoneScreen.NeonButton(x+10+i*tabs,y+23,tabs-2,14,(tier==i?">":"")+CyberwareCatalog.RARITIES[i],b->{tier=t;rebuildWidgets();}));}
  int fw=(w-20)/4;for(int f=0;f<4;f++){final int f0=f;addRenderableWidget(new PhoneScreen.NeonButton(x+10+f*fw,y+h-119,fw-2,16,(family==f?">":"")+CyberwareCatalog.FAMILIES[f],b->{family=f0;rebuildWidgets();}));}
  int id=CyberwareCatalog.index(selected,family,tier);boolean installed=java.util.Objects.equals(account.cyberSlots.get(selected),id)&&account.cyberRolls.getOrDefault(selected,0)>=rolls[id];var install=new PhoneScreen.NeonButton(x+10,y+h-64,w-20,18,installed?"装着中":counts[id]>0?"所持中の最高値を装着 / 所持 "+counts[id]+" 個":"未所持 / 敵からランダムドロップ",b->request("install",id));install.active=editable&&!installed&&counts[id]>0;addRenderableWidget(install);
  var remove=new PhoneScreen.NeonButton(x+10,y+h-25,95,18,"取り外す",b->request("remove",selected));remove.active=editable&&account.cyberSlots.containsKey(selected);addRenderableWidget(remove);
  var sell=new PhoneScreen.NeonButton(x+112,y+h-25,95,18,"売却",b->request("sell",id));sell.active=editable&&counts[id]>0;addRenderableWidget(sell);
  addRenderableWidget(new PhoneScreen.NeonButton(x+w-80,y+h-25,70,18,"閉じる",b->onClose()));
  if(!requested){requested=true;request("view",-1);}
 }
 void request(String action,int id){if(minecraft.player!=null)minecraft.player.connection.sendCommand("neoncyber "+action+" "+pos.getX()+" "+pos.getY()+" "+pos.getZ()+" "+id);}
 void receive(JsonObject o){if(!o.has("cyberware")||o.get("x").getAsInt()!=pos.getX()||o.get("y").getAsInt()!=pos.getY()||o.get("z").getAsInt()!=pos.getZ())return;editable=o.get("editable").getAsBoolean();if(o.has("rolls"))rolls=StockMarket.JSON.fromJson(o.get("rolls"),int[].class);if(o.has("inventory"))counts=StockMarket.JSON.fromJson(o.get("inventory"),int[].class);message=o.get("message").getAsString();if(o.has("account")){account=StockMarket.JSON.fromJson(o.get("account"),MarketLedger.Account.class);CyberwareCatalog.normalize(account);}rebuildWidgets();}
 @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
  g.fill(x,y,x+w,y+h,0xf5090e17);g.outline(x,y,w,h,0xffb64759);for(int j=3;j<h;j+=5)g.fill(x+2,y+j,x+w-2,y+j+1,0x14265d69);
  g.text(font,"NEURAL / CYBERWARE",x+12,y+9,0xff62f9ef);String cash="容量制限なし";g.text(font,cash,x+w-font.width(cash)-12,y+9,0xffffc863);
  int cx=x+w/2;segment(g,cx-12,0,24,22,selected==0||selected==6,0);segment(g,cx-17,25,34,44,selected==2||selected==4||selected==5||selected==8,4);segment(g,cx-30,25,10,48,selected==1||selected==7,1);segment(g,cx+20,25,10,48,selected==1||selected==7,1);segment(g,cx-17,73,15,39,selected==3||selected==9,9);segment(g,cx+2,73,15,39,selected==3||selected==9,9);
  g.fill(cx-1,fy(29),cx+1,fy(67),0xfff16a81);g.fill(cx-10,fy(9),cx+10,fy(12),0xff65fff1);for(int j=0;j<5;j++)g.fill(cx-12,fy(33+j*6),cx+12,fy(33+j*6)+1,0xff567785);
  double[] b=CyberwareCatalog.bonuses(account);int bottom=y+h-101,card=(w-30)/2;

  var part=CyberwareCatalog.PARTS[CyberwareCatalog.index(selected,family,tier)];g.fill(x+10,bottom,x+w-10,bottom+32,0xff172530);g.item(new net.minecraft.world.item.ItemStack(Cyberware.ITEMS.get(CyberwareCatalog.index(selected,family,tier))),x+15,bottom+7);g.text(font,part.name()+" / "+CyberwareCatalog.RARITIES[tier],x+39,bottom+4,CyberwareCatalog.COLORS[tier]);g.text(font,(counts[CyberwareCatalog.index(selected,family,tier)]>0?CyberwareCatalog.effect(CyberwareCatalog.index(selected,family,tier),rolls[CyberwareCatalog.index(selected,family,tier)]):part.effect())+" / 所持 "+counts[CyberwareCatalog.index(selected,family,tier)]+" 個",x+39,bottom+19,0xff8ef9ee);
  String status=font.plainSubstrByWidth(message,w-20);g.text(font,status,x+10,y+h-46,0xffff8b9e);g.centeredText(font,(editable?"CLINIC":"閲覧")+" / HP +"+Math.round(b[0]*100)+"% / 防御 +"+Math.round(b[1]*100)+"%",cx,y+h-19,0xff7bc5c8);
  super.extractRenderState(g,mx,my,delta);
 }
 int fy(int offset){return y+39+(int)(offset*Math.min(1.0,(h-165)/112.0));}
 void segment(GuiGraphicsExtractor g,int bx,int by,int bw,int bh,boolean selected,int slot){body(g,bx,fy(by),bw,Math.max(1,fy(by+bh)-fy(by)),selected,slot);}
 void body(GuiGraphicsExtractor g,int bx,int by,int bw,int bh,boolean selected,int slot){g.fill(bx,by,bx+bw,by+bh,selected?0xff553642:0xff1d303b);g.outline(bx,by,bw,bh,selected?0xffffbe65:account.cyberSlots.containsKey(slot)?0xff57f2df:0xff69818c);g.fill(bx+2,by+2,bx+4,by+bh-2,0xff507e8a);}
}
