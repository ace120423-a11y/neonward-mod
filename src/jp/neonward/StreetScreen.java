package jp.neonward;
import com.google.gson.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
/** Server snapshots drive both workshops and the underworld board. */
public final class StreetScreen extends Screen {
 JsonObject data=new JsonObject();String page,id="car",message="";int x,y,w,h;
 public StreetScreen(String page){super(Component.literal(page.equals("motor")?"MOTOR WORKS":"BLACKLINE"));this.page=page;}
 public boolean isPauseScreen(){return false;}
 void receive(JsonObject o){data=o;if(o.has("vehicle"))id=o.get("vehicle").getAsString();message=o.get("message").getAsString();rebuildWidgets();}
 void cmd(String s){if(minecraft.player!=null)minecraft.player.connection.sendCommand(s);}
 int offset(int v){return v*h/290;}
 void button(int yy,String text,String cmd){addRenderableWidget(new PhoneScreen.NeonButton(x+12,y+offset(yy),w-24,h<270?16:20,text,b->cmd(cmd)));}
 protected void init(){w=Math.min(430,width-12);h=Math.min(290,height-8);x=(width-w)/2;y=Math.max(3,(height-h)/2);
  if(page.equals("motor")){if(data.has("tune")){var t=data.getAsJsonObject("tune");String[] keys={"engine","handling","cargo","paint","neon"};String[] labels={"エンジン","足回り・ブレーキ","荷室拡張","塗装を切替","ネオンを切替"};var tune=StockMarket.JSON.fromJson(t,StreetProgress.Tune.class);for(int i=0;i<5;i++){int level=t.get(keys[i]).getAsInt(),cost=StreetProgress.price(tune,keys[i]);if(i==2&&VehicleCatalog.get(id).rows()+level>=6)cost=0;button(90+i*25,labels[i]+(i<3?" Lv."+level:"")+" / "+(cost==0?"上限":cost+" Cr"),"neonmotor "+id+" "+keys[i]);}}}
  else{String[] names={"運び屋 / 外の投下地点へ届ける","車両回収 / 指定車を南の外壁まで運ぶ","囮運転 / 自分の車で外を走って逃げ切る"};for(int i=0;i<3;i++)button(90+i*25,names[i],"underworld accept "+i);button(170,"完了した仕事を報告","underworld claim");button(195,"依頼を中止（報酬なし）","underworld abandon");}
  button(225,"情報を更新",page.equals("motor")?"neonmotor "+id+" view":"underworld view");
  addRenderableWidget(new PhoneScreen.NeonButton(x+12,y+offset(260),w-24,h<270?16:20,"閉じる",b->onClose()));
 }
 public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){g.fill(x-3,y-3,x+w+3,y+h+3,0xff0b1722);g.outline(x-3,y-3,w+6,h+6,0xff56dfd1);g.text(font,page.equals("motor")?"MOTOR WORKS / "+VehicleCatalog.get(id).name():"BLACKLINE / 裏稼業",x+12,y+offset(12),0xff72fff0);
  String[] lines;
  if(page.equals("motor")){var t=data.has("tune")?StockMarket.JSON.fromJson(data.get("tune"),StreetProgress.Tune.class):new StreetProgress.Tune();var s=VehicleCatalog.get(id);lines=new String[]{"残高 "+(data.has("cash")?data.get("cash").getAsLong():0)+" Cr","最高速度 "+Math.round(s.top()*(1+.1*t.engine)*72)+" km/h / 荷室 "+Math.min(6,s.rows()+t.cargo)*9+"枠","塗装："+new String[]{"標準","青","赤","白","紫"}[t.paint]+" / ネオン："+new String[]{"OFF","シアン","ピンク","ライム"}[t.neon]};}
  else lines=new String[]{data.has("summary")?data.get("summary").getAsString():"読込中",data.has("objective")?data.get("objective").getAsString():"","受注・報告：マフィアの近く / 制限15分"};
  for(int i=0;i<lines.length;i++)g.text(font,font.plainSubstrByWidth(lines[i],w-24),x+12,y+offset(34+i*16),0xffd4e7ed);g.text(font,font.plainSubstrByWidth(message,w-24),x+12,y+offset(248),0xffffc888);super.extractRenderState(g,mx,my,dt);
 }
}
