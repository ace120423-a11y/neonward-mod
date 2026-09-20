package jp.neonward;
import com.google.gson.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
public final class ParlorScreen extends Screen {
 final String kind;JsonObject data=new JsonObject();int l,t,w,h,selected=-1;boolean requested,rules;long next;
 ParlorScreen(String kind){super(Component.literal("遊戯の間"));this.kind=kind;}
 @Override public void onClose(){send("close",0);super.onClose();}
 @Override public boolean isPauseScreen(){return false;}
 void send(String action,int arg){if(minecraft.player!=null)minecraft.player.connection.sendCommand("parlor "+kind+" "+action+" "+arg);}
 void act(String action,int arg){if(System.currentTimeMillis()<next)return;next=System.currentTimeMillis()+200;send(action,arg);}
 String str(String k,String fallback){return data.has(k)?data.get(k).getAsString():fallback;}int num(String k,int fallback){return data.has(k)?data.get(k).getAsInt():fallback;}
 JsonArray arr(String k){return data.has(k)?data.getAsJsonArray(k):new JsonArray();}
 void receive(JsonObject o){data=o;clearWidgets();init();}
 void button(int x,int y,int width,String label,String action,int arg){addRenderableWidget(new PhoneScreen.NeonButton(x,y,width,18,label,b->act(action,arg)));}
 @Override protected void init(){w=Math.min(660,width-12);h=Math.min(400,height-12);l=(width-w)/2;t=(height-h)/2;if(!requested){requested=true;send("view",0);}
  addRenderableWidget(new PhoneScreen.NeonButton(l+w-56,t+6,48,17,"閉じる",b->onClose()));addRenderableWidget(new PhoneScreen.NeonButton(l+w-110,t+6,48,17,"ルール",b->{rules=!rules;clearWidgets();init();}));if(rules)return;
  String phase=str("phase","lobby");boolean lobby=phase.equals("lobby")||phase.equals("end");
  if(kind.equals("dice")){button(l+20,t+h-37,(w-50)/2,"丁（偶数）100 Cr","even",0);button(l+30+(w-50)/2,t+h-37,(w-50)/2,"半（奇数）100 Cr","odd",0);return;}
  if(lobby){int bw=(w-36)/3;button(l+10,t+h-30,bw,"着席","join",0);button(l+18+bw,t+h-30,bw,"開始・空席CPU","start",0);button(l+26+bw*2,t+h-30,bw,"離席","leave",0);return;}
  if(phase.equals("result")){button(l+15,t+h-29,w-30,"次の局へ（全員の準備を待ちます）","next",0);return;}
  var hand=arr("hand");if(kind.equals("mahjong")){int tw=Math.min(32,(w-24)/Math.max(14,hand.size()));for(int i=0;i<hand.size();i++){final int index=i;int tile=hand.get(i).getAsInt();addRenderableWidget(new TileButton(l+12+i*tw,t+h-105,tw-2,32,tile,false,i,b->{selected=index;clearWidgets();init();}));}
   int bw=(w-28)/6;String[] labels={"打牌","リーチ打牌","ツモ","暗槓","ロン","パス"},actions={"discard","reach","tsumo","ankan","ron","pass"};for(int i=0;i<6;i++){int arg=actions[i].equals("ankan")&&selected>=0&&selected<hand.size()?hand.get(selected).getAsInt():selected;button(l+10+i*bw,t+h-65,bw-3,labels[i],actions[i],arg);}
   int x=l+10;for(var o:arr("options")){String v=o.getAsString();if(v.equals("pon")||v.equals("kan")||v.startsWith("chi:")){String action=v.startsWith("chi:")?"chi":v;int arg=v.startsWith("chi:")?Integer.parseInt(v.substring(4)):0;String label=v.startsWith("chi:")?"チー "+MahjongRound.tile(arg):v.equals("pon")?"ポン":"明槓";button(x,t+h-43,76,label,action,arg);x+=80;}}
  }else {int cw=Math.min(46,(w-24)/Math.max(8,hand.size()));for(int i=0;i<hand.size();i++){int c=hand.get(i).getAsInt();addRenderableWidget(new TileButton(l+12+i*cw,t+h-99,cw-3,44,c,true,c,b->act("play",c)));}
   var field=arr("field");int fw=Math.min(37,(w-24)/12);for(int i=0;i<field.size();i++){int c=field.get(i).getAsInt();addRenderableWidget(new TileButton(l+12+(i%12)*fw,t+64+(i/12)*30,fw-3,29,c,true,c,b->act("match",c)));}
   button(l+15,t+h-43,(w-40)/2,"勝負（得点を確定）","stop",0);button(l+25+(w-40)/2,t+h-43,(w-40)/2,"こいこい（続行）","koi",0);
  }
 }
 @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){g.fill(l,t,l+w,t+h,0xff18261e);g.outline(l,t,w,h,0xffd0af6f);g.text(font,kind.equals("mahjong")?"四人麻雀・東風戦":kind.equals("koi")?"花札・こいこい":"丁半・壺振り",l+10,t+10,0xffffe7b7);
  if(rules){String[] lines=kind.equals("mahjong")?new String[]{"25000点持ち・東4局。親の和了／聴牌は連荘。","空席CPU。離席・切断中はCPUが代打します。","ツモ・ロン・リーチ・チー・ポン・明槓・暗槓。","喰いタンあり・赤牌なし・頭ハネ。","加槓・リーチ後の槓・途中流局・流し満貫なし。","自分の牌を選択 → 打牌。リーチも牌を選んで押す。","副露牌・捨て牌・ドラ表示牌は公開、他人の手牌は非公開。","点数は卓内専用。Cr消費なし。"}:kind.equals("koi")?new String[]{"2人・48枚・12回戦。空席はCPU。Cr消費なし。","自分の札を押す。同じ月が2枚なら場札を選ぶ。","札を出した後、山札を1枚めくります。","出来役で勝負、または1回だけこいこいを宣言。","こいこい後、役が増えた側が勝負を確定。","7点以上は倍。こいこいを返したらさらに倍。","五光10・四光8・雨四光7・三光5。","猪鹿蝶・赤短・青短・花見酒・月見酒は5。","タネ5枚・短冊5枚・カス10枚から1点、追加で+1。"}:new String[]{"丁＝2個のサイコロの合計が偶数、半＝奇数。","1回100 Cr。的中で200 Cr払い戻し。","最初の賭けから6秒で締め切り。1人1口。","空席はCPU客。壺振りの掛け声の後に出目を公開。"};for(int i=0;i<lines.length;i++)line(g,lines[i],42+i*17);super.extractRenderState(g,mx,my,delta);return;}
  if(kind.equals("dice")){line(g,"所持金 "+num("cash",0)+" Cr / 参加 "+num("bets",0)+"人 + CPU "+num("cpu",4)+"人",39);line(g,str("message","さあ、張った張った！"),67);int a=num("a",0),b=num("b",0);die(g,l+w/2-70,t+100,a);die(g,l+w/2+12,t+100,b);line(g,a==0?"壺の中の出目は勝負まで非公開":(a+b)%2==0?"丁（偶数）":"半（奇数）",h-64);}
  else {var players=arr("players");int me=num("seat",-1);String phase=str("phase","lobby");if(kind.equals("mahjong")){g.text(font,"東"+num("round",1)+"局 / "+num("honba",0)+"本場 / 残り"+num("remaining",0)+"枚 / 自席 "+(me<0?"観戦":me+1),l+10,t+31,0xffdccb9f);for(int i=0;i<players.size();i++){var p=players.get(i).getAsJsonObject();int y=t+45+i*14;String name=p.get("name").getAsString();g.text(font,font.plainSubstrByWidth((i==num("turn",0)?"> ":"")+name+" "+p.get("points").getAsInt()+(p.get("reach").getAsBoolean()?" R":""),120),l+10,y,0xffede8cf);var river=p.getAsJsonArray("river");int step=Math.max(9,Math.min(16,(w-145)/Math.max(1,river.size())));for(int j=0;j<river.size();j++){int x=l+138+j*step;g.blit(NeonWard.id("textures/gui/mahjong/"+river.get(j).getAsInt()+".png"),x,y-1,x+step-1,y+13,0,1,0,1);if(mx>=x&&mx<x+step&&my>=y-1&&my<y+13)g.text(font,MahjongRound.tile(river.get(j).getAsInt()),l+w-68,t+31,0xffffdd88);} }if(data.has("dora"))g.text(font,"ドラ表示 "+String.join(" ",arr("dora").asList().stream().map(v->MahjongRound.tile(v.getAsInt())).toList()),l+10,t+102,0xfff5d184);}
   else {for(int i=0;i<players.size();i++){var p=players.get(i).getAsJsonObject();g.text(font,p.get("name").getAsString()+" "+p.get("points").getAsInt()+"点 / 取り札"+p.getAsJsonArray("taken").size(),l+10+i*(w/2),t+32,0xffffdeb0);}g.text(font,(num("round",1))+"回戦 / "+(me==num("turn",0)?"あなたの番":"相手の番")+(num("pending",-1)>=0?" / "+KoiRound.label(num("pending",0))+"を合わせる":""),l+10,t+48,0xffd2dca6);}
   if(phase.equals("lobby")||phase.equals("end"))line(g,"着席してから開始。空席にはCPUが入ります。",h-65);line(g,str("message",""),h-18);
  }super.extractRenderState(g,mx,my,delta);
 }
 void line(GuiGraphicsExtractor g,String s,int y){g.centeredText(font,font.plainSubstrByWidth(s,w-20),l+w/2,t+y,0xfff3dfb1);}
 void die(GuiGraphicsExtractor g,int x,int y,int n){g.fill(x,y,x+56,y+56,0xfff0e8d7);g.outline(x,y,56,56,0xffaf956c);if(n==0){g.centeredText(font,"?",x+28,y+24,0xff77322c);return;}int[][] points={{-1,-1},{1,1},{-1,1},{1,-1},{-1,0},{1,0},{0,0}};for(int i=0;i<7;i++)if(n==1?i==6:n==2?i<2:n==3?i<2||i==6:n==4?i<4:n==5?i<4||i==6:i<6){int px=x+28+points[i][0]*16,py=y+28+points[i][1]*16;g.fill(px-3,py-3,px+4,py+4,n==1?0xffb52030:0xff17202a);}}
 class TileButton extends PhoneScreen.NeonButton {final int card,key;final boolean flower;TileButton(int x,int y,int w,int h,int card,boolean flower,int key,OnPress press){super(x,y,w,h,flower?KoiRound.label(card):MahjongRound.tile(card),press);this.card=card;this.flower=flower;this.key=key;}
  @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta){int x=getX(),y=getY(),w=getWidth(),h=getHeight();if(flower)g.blit(NeonWard.id("textures/gui/hanafuda/"+card+".png"),x,y,x+w,y+h,0,1,0,1);else {g.blit(NeonWard.id("textures/gui/mahjong/"+card+".png"),x,y,x+w,y+h,0,1,0,1);}g.outline(x,y,w,h,isHoveredOrFocused()||!flower&&selected==key?0xffffcb52:0xffa89a7d);if(isHoveredOrFocused())g.text(font,font.plainSubstrByWidth(getMessage().getString(),ParlorScreen.this.w-20),l+10,t+ParlorScreen.this.h-24,0xffffe5a2);}
 }
}
