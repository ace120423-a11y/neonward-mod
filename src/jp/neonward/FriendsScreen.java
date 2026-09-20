package jp.neonward;
import com.google.gson.*;
import java.util.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.core.component.DataComponents;
public final class FriendsScreen extends Screen {
 JsonObject data=new JsonObject();int x,y,w,tab,page,ticks;boolean requested,editing,confirmRemove;EditBox status;String selected="";ItemStack avatar=ItemStack.EMPTY;
 FriendsScreen(){super(Component.literal("NEON LINK / FRIENDS"));}
 @Override public boolean isPauseScreen(){return false;}
 @Override public void onClose(){minecraft.gui.setScreen(new PhoneScreen());}
 void send(String action,String value){if(minecraft.player!=null)minecraft.player.connection.sendCommand("neonfriends "+action+(value.isEmpty()?"":" "+value));}
 String text(String key){return data.has(key)?data.get(key).getAsString():"";}
 JsonArray rows(){return data.has(tab==0?"friends":tab==1?"pending":"online")?data.getAsJsonArray(tab==0?"friends":tab==1?"pending":"online"):new JsonArray();}
 void receive(JsonObject o){data=o;selected=o.has("profile")?o.getAsJsonObject("profile").get("id").getAsString():"";if(!selected.isEmpty()){avatar=new ItemStack(Items.PLAYER_HEAD);avatar.set(DataComponents.PROFILE,ResolvableProfile.createUnresolved(UUID.fromString(selected)));}page=Math.min(page,Math.max(0,(rows().size()-1)/4));rebuildWidgets();}
 void button(int xx,int yy,int width,String title,Runnable action){addRenderableWidget(new PhoneScreen.NeonButton(xx,yy,width,19,title,b->action.run()));}
 @Override protected void init(){w=Math.min(420,width-14);x=(width-w)/2;y=Math.max(3,(height-234)/2);if(!requested){requested=true;send("view","");}
 if(!selected.isEmpty()){
  button(x+10,y+205,74,"一覧へ",()->{editing=false;confirmRemove=false;send("view","");});
  if(selected.equals(text("self"))){button(x+w-122,y+205,110,editing?"保存":"ひとことを編集",()->{if(editing){String s=status.getValue().strip();send("status",s.isEmpty()?"—":s);editing=false;}else{editing=true;status=null;rebuildWidgets();}});if(editing){String draft=status==null?text("status"):status.getValue();status=new EditBox(font,x+137,y+152,w-151,22,Component.literal("ひとこと"));status.setMaxLength(48);status.setValue(draft);addRenderableWidget(status);setInitialFocus(status);}}
  else button(x+w-122,y+205,110,confirmRemove?"解除を確定":"フレンド解除",()->{if(confirmRemove){send("remove",selected);confirmRemove=false;}else{confirmRemove=true;rebuildWidgets();}});
 }else{
  String[] tabs={"フレンド","申請 "+(data.has("pending")?data.getAsJsonArray("pending").size():0),"追加"};int bw=(w-26)/3;for(int i=0;i<3;i++){final int n=i;button(x+10+i*(bw+3),y+32,bw,tabs[i],()->{tab=n;page=0;rebuildWidgets();});}
  var rows=rows();for(int j=0;j<4;j++){int i=page*4+j;if(i>=rows.size())break;var a=rows.get(i).getAsJsonObject();String id=a.get("id").getAsString();int yy=y+59+j*31;
   if(tab==0)button(x+w-90,yy,78,"プロフィール",()->send("profile",id));
   else if(tab==1){button(x+w-120,yy,51,"承認",()->send("accept",id));button(x+w-64,yy,51,"辞退",()->send("decline",id));}
   else {var b=new PhoneScreen.NeonButton(x+w-90,yy,78,19,a.get("sent").getAsBoolean()?"申請済み":"申請する",bt->send("request",id));b.active=!a.get("sent").getAsBoolean();addRenderableWidget(b);}}
  button(x+10,y+186,26,"<",()->{page=Math.max(0,page-1);rebuildWidgets();});button(x+40,y+186,26,">",()->{page=Math.min(Math.max(0,(rows().size()-1)/4),page+1);rebuildWidgets();});button(x+75,y+186,60,"更新",()->send("view",""));button(x+w-133,y+186,121,"自分のプロフィール",()->send("profile",text("self")));button(x+10,y+209,w-20,"スマホへ戻る",()->onClose());
 }}
 @Override public void tick(){super.tick();if(++ticks%100==0&&!editing&&!confirmRemove)send(selected.isEmpty()?"view":"profile",selected);}
 @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){g.fill(x-4,y-3,x+w+4,y+234,0xff060b13);g.outline(x-4,y-3,w+8,237,0xff324653);g.fill(x,y,x+w,y+230,0xff0b1422);for(int yy=y+27;yy<y+202;yy+=5)g.fill(x+2,yy,x+w-2,yy+1,0xff101e2d);g.fill(x,y+5,x+3,y+55,0xff54f5e6);g.fill(x+w-3,y+158,x+w,y+227,0xffff53ac);g.text(font,"NEON LINK  /  "+(selected.isEmpty()?"FRIENDS":"CITIZEN PROFILE"),x+12,y+11,0xff84ffeb);
 if(selected.isEmpty()){
  var rows=rows();if(rows.isEmpty())g.centeredText(font,tab==0?"追加タブから、一緒に遊ぶ人を登録":tab==1?"届いている申請はありません":"追加できるオンラインプレイヤーはいません",x+w/2,y+102,0xff97b7c9);
  for(int j=0;j<4;j++){int i=page*4+j;if(i>=rows.size())break;var a=rows.get(i).getAsJsonObject();int yy=y+57+j*31;g.fill(x+9,yy,x+w-9,yy+27,0xff14293a);g.text(font,font.plainSubstrByWidth(a.get("name").getAsString(),w-160),x+20,yy+4,0xffe0fff7);String sub=tab==0?(a.get("online").getAsBoolean()?"● ONLINE":"○ OFFLINE")+" / RANK "+GuildRanks.NAMES[a.get("rank").getAsInt()]:tab==1?"フレンド申請":"同じサーバーでプレイ中";g.text(font,sub,x+20,yy+16,0xff70b9ba);}g.text(font,font.plainSubstrByWidth(text("message"),w-20),x+10,y+174,0xffffc483);
 }else {var p=data.getAsJsonObject("profile");int rank=p.get("rank").getAsInt();int accent=new int[]{0xff8eafbd,0xff72daba,0xff61e4ed,0xff5eaaff,0xffac89ff,0xffff70b3,0xffffcf70}[rank];g.fill(x+11,y+35,x+127,y+195,0xff152c3c);g.outline(x+11,y+35,116,160,accent);g.fill(x+14,y+179,x+124,y+191,0xff0c1725);g.centeredText(font,p.get("online").getAsBoolean()?"ONLINE":"OFFLINE",x+69,y+181,p.get("online").getAsBoolean()?0xff68ffb4:0xff94a9ba);
  var entity=minecraft.level==null?null:minecraft.level.getPlayerByUUID(UUID.fromString(selected));if(entity!=null)InventoryScreen.extractEntityInInventoryFollowsMouse(g,x+13,y+40,x+125,y+177,52,.0625f,mx,my,entity);else {g.pose().pushMatrix();g.pose().translate(x+34,y+83);g.pose().scale(4,4);g.item(avatar,0,0);g.pose().popMatrix();}
  int xx=x+139;g.text(font,font.plainSubstrByWidth(p.get("name").getAsString(),w-152),xx,y+41,0xffeafff9);g.text(font,p.has("title")?p.get("title").getAsString():"NEON GUILD / 認定ランク",xx,y+59,0xff86aabd);g.fill(xx,y+75,x+w-13,y+116,0xff1b3141);g.outline(xx,y+75,w-152,41,accent);g.pose().pushMatrix();g.pose().translate(xx+12,y+79);g.pose().scale(3,3);g.text(font,GuildRanks.NAMES[rank],0,0,accent);g.pose().popMatrix();g.text(font,new String[]{"見習い","探索者","実働員","精鋭","熟練者","上級精鋭","最高位"}[rank],xx+49,y+87,accent);
  int done=Math.min(5,p.get("reports").getAsInt());g.text(font,rank==6?"最高ランク到達":p.get("exam").getAsBoolean()?"昇級試験に挑戦中":"次の試験まで  "+done+" / 5 件",xx,y+125,0xffb8d4df);g.fill(xx,y+138,x+w-14,y+142,0xff263a4d);g.fill(xx,y+138,xx+(w-153)*(rank==6?5:done)/5,y+142,accent);
  if(!editing){String status=p.get("status").getAsString();int max=w-154;int line=0;while(!status.isEmpty()&&line<3){String part=font.plainSubstrByWidth(status,max);if(part.isEmpty())break;g.text(font,part,xx,y+154+line*12,0xffcbdde4);status=status.substring(part.length());line++;}}}
 super.extractRenderState(g,mx,my,dt);}
}
