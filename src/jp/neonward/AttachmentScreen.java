package jp.neonward;
import com.google.gson.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
public final class AttachmentScreen extends Screen {
 String mode="",message="",gun="";int token,x,y,w,h,page,perPage=4;long cash,reveal,sentAt;boolean pending;
 JsonArray rows=new JsonArray(),installed=new JsonArray();PhoneScreen.NeonButton one,ten;
 public AttachmentScreen(){super(Component.literal("NEON MOD / アタッチメント"));}
 public boolean isPauseScreen(){return false;}
 void receive(JsonObject o){String next=o.get("mode").getAsString();if(!next.equals(mode)){page=0;rows=new JsonArray();}mode=next;token=o.get("token").getAsInt();message=o.get("message").getAsString();gun=o.get("gun").getAsString();cash=o.get("cash").getAsLong();installed=o.getAsJsonArray("installed");pending=false;
  if(!mode.equals("gacha"))rows=o.getAsJsonArray("rows");else if(!o.getAsJsonArray("prizes").isEmpty()){rows=o.getAsJsonArray("prizes");page=0;}
  reveal=System.currentTimeMillis()+o.get("remaining").getAsLong()*50;rebuildWidgets();
 }
 void action(String action,int value){if(pending||minecraft.player==null)return;pending=true;sentAt=System.currentTimeMillis();minecraft.player.connection.sendCommand("neonattach "+action+" "+token+" "+value);rebuildWidgets();}
 int start(){return mode.equals("equip")?155:62;}
 protected void init(){w=Math.min(470,width-16);h=Math.min(355,height-16);x=(width-w)/2;y=(height-h)/2;perPage=Math.max(1,(h-start()-(mode.equals("gacha")?130:70))/30);page=Math.min(page,Math.max(0,(rows.size()-1)/perPage));
  if(mode.equals("equip"))for(int i=0;i<4;i++){final int mount=i;var b=addRenderableWidget(new PhoneScreen.NeonButton(x+w-72,y+53+i*23,60,19,"取り外す",z->action("remove",mount)));b.active=!pending&&installed.size()==4&&installed.get(i).getAsInt()>=0;}
  for(int j=0;j<perPage&&page*perPage+j<rows.size();j++){var row=rows.get(page*perPage+j).getAsJsonObject();int code=row.get("code").getAsInt();if(mode.equals("gacha"))continue;boolean shop=mode.equals("shop");var b=addRenderableWidget(new PhoneScreen.NeonButton(x+w-103,y+start()+j*30,91,24,shop?row.get("price").getAsInt()+" Cr":"装着",z->action(shop?"buy":"install",shop?code:row.get("slot").getAsInt())));b.active=!pending&&(shop?cash>=row.get("price").getAsInt():row.get("compatible").getAsBoolean());}
  int foot=y+h-(mode.equals("gacha")?116:53);
  addRenderableWidget(new PhoneScreen.NeonButton(x+12,foot,42,18,"＜",z->{page=Math.max(0,page-1);rebuildWidgets();}));
  addRenderableWidget(new PhoneScreen.NeonButton(x+60,foot,42,18,"＞",z->{page=Math.min(Math.max(0,(rows.size()-1)/perPage),page+1);rebuildWidgets();}));
  if(mode.equals("gacha")){int half=(w-28)/2;one=addRenderableWidget(new PhoneScreen.NeonButton(x+12,y+h-52,half,21,"1回 / 1,000 Cr",z->action("roll",1)));ten=addRenderableWidget(new PhoneScreen.NeonButton(x+16+half,y+h-52,half,21,"10連 / 10,000 Cr",z->action("roll",10)));}
  addRenderableWidget(new PhoneScreen.NeonButton(x+w-92,y+h-26,80,19,"閉じる",z->onClose()));
 }
 public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){boolean anim=mode.equals("gacha")&&System.currentTimeMillis()<reveal;
  if(pending&&System.currentTimeMillis()-sentAt>3000){pending=false;message="応答待ちです。再試行するか画面を開き直してください";rebuildWidgets();}
  if(one!=null){one.active=!pending&&!anim&&cash>=1000;ten.active=!pending&&!anim&&cash>=10000;}
  g.fill(x-3,y-3,x+w+3,y+h+3,0xff08111e);g.outline(x-3,y-3,w+6,h+6,0xffb9ff76);
  g.text(font,mode.equals("gacha")?"MOD CAPSULE / 専用ガチャ":mode.equals("shop")?"BLACK STEEL / パーツ販売":"WEAPON MOD / 装着",x+12,y+10,0xffb9ff76);
  g.text(font,font.plainSubstrByWidth(mode.equals("equip")?gun:"残高 "+cash+" Cr",w-24),x+12,y+28,0xffffd684);
  if(mode.equals("equip")&&installed.size()==4)for(int i=0;i<4;i++){int c=installed.get(i).getAsInt();g.text(font,font.plainSubstrByWidth(GunAttachments.SLOTS[i]+": "+(c<0?"未装着":GunAttachments.name(c)),w-100),x+12,y+59+i*23,c<0?0xff7696a8:0xff000000|CyberwareCatalog.COLORS[c%5]);}
  if(anim){g.centeredText(font,"◇ パーツ解析中… ◇",x+w/2,y+85,0xffb9ff76);}
  else for(int j=0;j<perPage&&page*perPage+j<rows.size();j++){var row=rows.get(page*perPage+j).getAsJsonObject();int code=row.get("code").getAsInt(),yy=y+start()+j*30,ww=w-(mode.equals("gacha")?55:145);g.item(GunAttachments.stack(code),x+12,yy+3);g.text(font,font.plainSubstrByWidth(row.get("name").getAsString(),ww),x+34,yy,0xff000000|CyberwareCatalog.COLORS[code%5]);g.text(font,font.plainSubstrByWidth(row.get("effect").getAsString(),ww),x+34,yy+12,0xffbacdd7);}
  if(rows.isEmpty()&&!anim)g.text(font,mode.equals("equip")?"持ち物にパーツがありません":"8種類 × 5レア度 / 重複あり",x+12,y+start()+8,0xffbacdd7);
  int foot=y+h-(mode.equals("gacha")?112:49);g.text(font,(page+1)+" / "+Math.max(1,(rows.size()+perPage-1)/perPage),x+115,foot,0xffbacdd7);
  if(mode.equals("gacha")){g.text(font,"コモン41.9% / アンコモン30% / レア20%",x+12,y+h-90,0xffbacdd7);g.text(font,"エピック8% / レジェンダリー0.1%（10連も同率）",x+12,y+h-77,0xffbacdd7);}
  g.text(font,font.plainSubstrByWidth(pending?"処理中…":message,w-120),x+12,y+h-20,0xffffd684);
  super.extractRenderState(g,mx,my,dt);
 }
}
