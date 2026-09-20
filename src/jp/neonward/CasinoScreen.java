package jp.neonward;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import com.google.gson.JsonObject;
public class CasinoScreen extends Screen {
 boolean manual,slotActive,slotOwner;boolean[] stopped={true,true,true};int[] offsets={0,0,0};long slotTick,slotReceived;int machine=-1;int l,t,w,h,page,number;long cash;String reels="? | ? | ?",message="",playerCards="",dealerCards="";int pt,dt,last;boolean active;long next;java.util.Set<String> owned=new java.util.HashSet<>();
 public CasinoScreen(){this(0);}
 long spinStarted=-10000,cardDealt;int[] reelValues={0,0,0};
 public CasinoScreen(int page){this(page,-1);}
 public CasinoScreen(int page,int machine){super(Component.literal("CHROME CASINO"));this.machine=machine;this.page=machine>=0?0:page;}
 @Override public boolean isPauseScreen(){return false;}
 void send(String command){if(minecraft.player!=null)minecraft.player.connection.sendCommand(machine>=0?"neonslot "+machine+" "+command:"neoncasino "+command);}
 void act(String command){if(System.currentTimeMillis()<next||command.equals("spin")&&(manual?slotActive:System.currentTimeMillis()-spinStarted<4000))return;next=System.currentTimeMillis()+350;send(command.equals("number")?"number "+number:command);}
 void receive(JsonObject o){if(o.has("manualSlots")){manual=true;slotActive=o.get("slotActive").getAsBoolean();slotOwner=o.get("slotOwner").getAsBoolean();slotTick=o.get("slotElapsed").getAsLong();slotReceived=System.currentTimeMillis();for(int i=0;i<3;i++){offsets[i]=o.getAsJsonArray("slotOffsets").get(i).getAsInt();stopped[i]=o.getAsJsonArray("slotStopped").get(i).getAsBoolean();}}if(o.has("slotRemaining"))spinStarted=System.currentTimeMillis()-4000+o.get("slotRemaining").getAsLong()*50;if(o.has("lastReels")){var a=o.getAsJsonArray("lastReels");for(int i=0;i<3;i++)reelValues[i]=Math.floorMod(a.get(i).getAsInt(),6);}if(o.has("playerCards")&&!playerCards.equals(o.get("playerCards").getAsString()))cardDealt=System.currentTimeMillis();if(o.has("cash"))cash=o.get("cash").getAsLong();if(o.has("reels"))reels=o.get("reels").getAsString();if(o.has("lastRoulette"))last=o.get("lastRoulette").getAsInt();if(o.has("playerCards")){playerCards=o.get("playerCards").getAsString();dealerCards=o.get("dealerCards").getAsString();pt=o.get("playerTotal").getAsInt();dt=o.get("dealerTotal").getAsInt();active=o.get("handActive").getAsBoolean();}if(o.has("casinoVehicles")&&o.get("casinoVehicles").isJsonArray()){owned.clear();for(var v:o.getAsJsonArray("casinoVehicles"))owned.add(v.getAsString());}message=o.get("message").getAsString();}
 void button(int x,int y,int width,String name,String cmd){addRenderableWidget(new PhoneScreen.NeonButton(x,y,width,19,name,b->act(cmd)));}
 @Override protected void init(){w=Math.min(520,width-16);h=Math.min(330,height-12);l=(width-w)/2;t=(height-h)/2;
  String[] names={"スロット案内","ブラックジャック","大ルーレット","ルール・景品"};int bw=(w-20)/4;
  if(machine<0)for(int i=0;i<4;i++){final int n=i;addRenderableWidget(new PhoneScreen.NeonButton(l+10+i*bw,t+28,bw-3,18,names[i],b->{page=n;clearWidgets();init();}));}
  if(page==0&&machine>=0){int sw=(w-30)/4;button(l+15,t+h-55,sw-3,"START 100 Cr","spin");String[] stops={"左 STOP","中 STOP","右 STOP"};for(int i=0;i<3;i++)button(l+15+sw*(i+1),t+h-55,sw-3,stops[i],"stop"+i);}
  if(page==1){int step=(w-30)/3;button(l+15,t+h-55,step-4,"配る：100 Cr","deal");button(l+15+step,t+h-55,step-4,"ヒット","hit");button(l+15+step*2,t+h-55,step-4,"スタンド","stand");}
  if(page==2){int step=(w-30)/4;String[] bets={"red","black","odd","even"},label={"赤","黒","奇数","偶数"};for(int i=0;i<4;i++)button(l+15+i*step,t+h-83,step-3,label[i]+" 100 Cr",bets[i]);
   addRenderableWidget(new PhoneScreen.NeonButton(l+15,t+h-55,30,19,"-",b->number=(number+36)%37));addRenderableWidget(new PhoneScreen.NeonButton(l+92,t+h-55,30,19,"+",b->number=(number+1)%37));button(l+130,t+h-55,w-145,"選んだ数字に100 Cr","number");
  }
  addRenderableWidget(new PhoneScreen.NeonButton(l+15,t+h-26,w-30,18,"閉じる",b->onClose()));send("view");
 }
 @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float d){g.fill(l,t,l+w,t+h,0xff150c20);g.outline(l,t,w,h,0xfffa57ae);g.centeredText(font,(machine>=0?"CHROME SLOTS #"+(machine+1):"CHROME CASINO")+" / "+cash+" Cr",l+w/2,t+12,0xffffbf58);
  if(machine>=0)line(g,"SLOT ONLY / 1回 100 Cr",35);if(page==0){if(machine>=0)reelPanel(g);else {line(g,"スロットは北側に並ぶ6台の筐体で遊べます",77);line(g,"台を右クリック / 各台スロット専用",101);}}
  if(page==1){g.fill(l+10,t+55,l+w-10,t+h-93,0xff123e37);g.outline(l+10,t+55,w-20,h-148,0xffc4a45d);cards(g,"親 "+dt+(active?" + ?":""),dealerCards,61);cards(g,"自分 "+pt,playerCards,99);}
  if(page==2){line(g,"直近の当たり："+last+" / 大きな盤面も回転します",68);line(g,"赤黒・奇偶：2倍 / 数字0～36：36倍",87);g.centeredText(font,""+number,l+68,t+h-49,0xffffdf74);}
  if(page==3){String[] rules={"ブラックジャック：Aは1か11、絵札は10","ディーラーはソフト17を含め17で止まる","スプリット・ダブルなし / 中断しても手札を保存","ルーレット：0～36が各1/37。0は赤黒・奇偶とも外れ","1回終了ごと景品1% / 4種類それぞれ0.25%","獲得済みの景品は1,000 Crに交換"};int step=Math.min(18,(h-113)/10);for(int i=0;i<rules.length;i++)line(g,rules[i],61+i*step);for(int i=0;i<4;i++){String id=CasinoGames.PRIZES.get(i);line(g,(owned.contains(id)?"入手済み ":"未入手 ")+VehicleCatalog.get(id).name(),61+(i+6)*step);}}
  else {String msg=font.plainSubstrByWidth(page==0&&(manual?slotActive:System.currentTimeMillis()-spinStarted<4000)?"左・中・右 STOPで各リールを止めてください":message,w-28);line(g,msg,h-(page==2?108:85));}
  super.extractRenderState(g,mx,my,d);
 }
 void reelPanel(GuiGraphicsExtractor g){int rw=Math.min(70,(w-95)/3),left=l+w/2-rw*3/2;long elapsed=System.currentTimeMillis()-spinStarted;int rh=Math.min(62,h-174);for(int i=0;i<3;i++){int x=left+i*rw;g.fill(x-2,t+59,x+rw-3,t+65+rh,0xff60eadb);g.fill(x,t+61,x+rw-5,t+63+rh,0xffeee9da);boolean rolling=manual?slotActive&&!stopped[i]:elapsed<2000+i*900;int n=rolling?(manual?Math.floorMod((int)((slotTick+(System.currentTimeMillis()-slotReceived)/50)/3)+offsets[i],6):(int)((System.currentTimeMillis()/90+i*3)%6)):reelValues[i];int yy=t+63+rh/2;g.centeredText(font,CasinoProps.SYMBOLS[n],x+(rw-5)/2,yy-4,0xff000000|CasinoProps.COLORS[n]);g.centeredText(font,rolling?CasinoProps.SYMBOLS[(n+1)%6]:"◆",x+(rw-5)/2,yy+13,0xff657884);g.centeredText(font,rolling?CasinoProps.SYMBOLS[(n+5)%6]:"◆",x+(rw-5)/2,yy-20,0xff657884);g.fill(x,yy+7,x+rw-5,yy+8,0xffeb5597);}line(g,"3つ一致 2,000 Cr / 2つ一致 100 Cr",h-101);}
 void cards(GuiGraphicsExtractor g,String label,String text,int y){g.text(font,label,l+17,t+y+15,0xffffe3a2);var tokens=CasinoProps.tokens(text);int step=Math.min(42,Math.max(13,(w-110)/Math.max(1,tokens.size())));int cw=Math.min(37,step+7);for(int i=0;i<tokens.size();i++){int slide=(int)(14*Math.max(0,1-(System.currentTimeMillis()-cardDealt-i*80)/250.0));int x=l+91+i*step+slide;String v=tokens.get(i);boolean back=v.equals("??");int ink=v.endsWith("H")||v.endsWith("D")?0xffb52e52:0xff15202b;g.fill(x+2,t+y+2,x+cw+2,t+y+35,0x88000000);g.fill(x,t+y,x+cw,t+y+33,back?0xff173f57:0xfff4efdf);g.outline(x,t+y,cw,33,0xffdeb96b);if(back){g.outline(x+3,t+y+3,cw-6,27,0xff68efe3);g.centeredText(font,"◆",x+cw/2,t+y+15,0xff69ede2);}else{String rank=v.substring(0,v.length()-1),face=CasinoProps.face(v);g.text(font,rank,x+3,t+y+3,ink);g.centeredText(font,face.substring(face.length()-1),x+cw/2,t+y+13,ink);g.text(font,rank,x+cw-font.width(rank)-3,t+y+23,ink);}}}
 void line(GuiGraphicsExtractor g,String s,int y){g.centeredText(font,font.plainSubstrByWidth(s,w-22),l+w/2,t+y,0xffdcd5e1);}
}
