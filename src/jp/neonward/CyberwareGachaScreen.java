package jp.neonward;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import com.google.gson.*;
public final class CyberwareGachaScreen extends Screen {
 int x,y,w,h,token,page,perPage;long cash,reveal;boolean pending,weapon;String message="";
 JsonArray prizes=new JsonArray();PhoneScreen.NeonButton spin,ten,previous,next;
 public CyberwareGachaScreen(){super(Component.literal("CAPSULE GACHA"));}
 public boolean isPauseScreen(){return false;}
 void receive(JsonObject o){
  boolean mode=o.get("weapon").getAsBoolean();if(mode!=weapon){prizes=new JsonArray();page=0;}weapon=mode;
  token=o.get("token").getAsInt();cash=o.get("cash").getAsLong();message=o.get("message").getAsString();pending=false;
  if(!o.getAsJsonArray("prizes").isEmpty()){prizes=o.getAsJsonArray("prizes");page=0;}
  reveal=System.currentTimeMillis()+o.get("remaining").getAsLong()*50;
 }
 void roll(int count){if(pending||System.currentTimeMillis()<reveal||minecraft.player==null)return;pending=true;spin.active=false;ten.active=false;minecraft.player.connection.sendCommand((weapon?"neonweapongacha":"neongacha")+" roll "+token+" "+count);}
 protected void init(){w=Math.min(450,width-16);h=Math.min(350,height-16);x=(width-w)/2;y=(height-h)/2;perPage=Math.max(1,Math.min(5,(h-180)/24));page=Math.min(page,Math.max(0,(prizes.size()-1)/perPage));
  previous=addRenderableWidget(new PhoneScreen.NeonButton(x+12,y+h-124,52,18,"＜",b->{page=Math.max(0,page-1);}));
  next=addRenderableWidget(new PhoneScreen.NeonButton(x+w-64,y+h-124,52,18,"＞",b->{page=Math.min(Math.max(0,(prizes.size()-1)/perPage),page+1);}));
  int half=(w-28)/2;
  spin=addRenderableWidget(new PhoneScreen.NeonButton(x+12,y+h-50,half,20,"1回 / 1,000 Cr",b->roll(1)));
  ten=addRenderableWidget(new PhoneScreen.NeonButton(x+16+half,y+h-50,half,20,"10連 / 10,000 Cr",b->roll(10)));
  addRenderableWidget(new PhoneScreen.NeonButton(x+12,y+h-26,w-24,19,"閉じる",b->onClose()));
 }
 public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){boolean anim=System.currentTimeMillis()<reveal;
  spin.active=!pending&&!anim&&cash>=1000;ten.active=!pending&&!anim&&cash>=10000;
  previous.active=!anim&&page>0;next.active=!anim&&(page+1)*perPage<prizes.size();
  int accent=weapon?0xffff71ca:0xff70fff0;
  g.fill(x-3,y-3,x+w+3,y+h+3,0xff08111e);g.outline(x-3,y-3,w+6,h+6,accent);
  g.centeredText(font,weapon?"WEAPON CAPSULE":"CYBER CAPSULE",x+w/2,y+10,accent);
  g.centeredText(font,String.format("%,d Cr / 1回につき必ず1個",cash),x+w/2,y+26,0xfff3dc94);
  if(anim||prizes.isEmpty())g.centeredText(font,anim?"◇ カプセル解析中… ◇":weapon?"近接・銃24種 / 種類は均等":"部位・系統は均等",x+w/2,y+68,accent);
  else for(int i=0;i<perPage&&page*perPage+i<prizes.size();i++){
   int index=page*perPage+i;var prize=prizes.get(index).getAsJsonObject();int tier=prize.get("tier").getAsInt(),color=0xff000000|CyberwareCatalog.COLORS[tier],row=y+49+i*24;
   g.text(font,font.plainSubstrByWidth((index+1)+". "+prize.get("name").getAsString(),w-24),x+12,row,color);
   g.text(font,font.plainSubstrByWidth(CyberwareCatalog.RARITIES[tier]+" / "+prize.get("detail").getAsString(),w-30),x+18,row+11,0xffa8c7d8);
  }
  g.centeredText(font,prizes.isEmpty()?"結果一覧":(page+1)+" / "+Math.max(1,(prizes.size()+perPage-1)/perPage)+"ページ",x+w/2,y+h-120,0xffa8c7d8);
  String[] lines={"コモン41.9% / アンコモン30% / レア20%","エピック8% / レジェンダリー0.1%","10連も同じ確率 / 重複あり / 空き10枠必要",anim?"結果は持ち物へ受け取り済み":message};
  for(int i=0;i<lines.length;i++)g.centeredText(font,font.plainSubstrByWidth(lines[i],w-16),x+w/2,y+h-102+i*12,i==3?0xffffd684:0xffa8c7d8);
  super.extractRenderState(g,mx,my,delta);
 }
}
