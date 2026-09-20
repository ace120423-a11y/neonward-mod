package jp.neonward;
import com.google.gson.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
public class GuildScreen extends Screen {
 static final int[] ORDER={0,6,1,3,2,4,5};final BlockPos pos;final boolean exchange;int x,y,w,page,rank,reports;long cash;int[] counts=new int[8],quests=new int[GuildCatalog.QUESTS.length];String message="読み込み中",examText="";boolean requested,examActive,examReady;JsonArray specs=new JsonArray();
 public GuildScreen(BlockPos p,boolean e){super(Component.literal("NEON GUILD"));pos=p;exchange=e;java.util.Arrays.fill(quests,-1);}
 int pages(){return exchange?2:(quests.length+1)/2;}int target(int i){return specs.size()>i?specs.get(i).getAsJsonObject().get("target").getAsInt():GuildCatalog.QUESTS[i].target();}boolean locked(int i){return specs.size()>i&&specs.get(i).getAsJsonObject().get("locked").getAsBoolean();}
 @Override public boolean isPauseScreen(){return false;}
 @Override protected void init(){w=Math.min(460,width-12);x=(width-w)/2;y=Math.max(3,(height-236)/2);int rows=exchange?4:2;
 for(int row=0;row<rows;row++){final int raw=page*rows+row;if(!exchange&&raw>=quests.length)break;final int i=exchange?raw:ORDER[raw];int yy=y+(exchange?47:59)+row*(exchange?31:43);String label=exchange?"売却":locked(i)?"未解放":quests[i]<0?"受注":quests[i]>=target(i)?"報酬受取":"受注中";var b=new PhoneScreen.NeonButton(x+w-88,yy,76,22,label,bt->send(exchange?"sell":quests[i]<0?"accept":"claim",i));if(!exchange&&(locked(i)||quests[i]>=0&&quests[i]<target(i)))b.active=false;addRenderableWidget(b);}
 if(!exchange){var b=new PhoneScreen.NeonButton(x+12,y+151,w-24,22,rank==6?"Sランク / 依頼は何度でも受注可能":examActive?examReady?"昇級試験：合格報告":examText:"昇級試験 / 依頼報告 "+reports+" / 5",bt->send(examActive?"promote":"exam",-1));b.active=rank<6&&(examActive?examReady:reports>=5);addRenderableWidget(b);}
 addRenderableWidget(new PhoneScreen.NeonButton(x+12,y+176,35,18,"<",b->{page=(page+pages()-1)%pages();rebuildWidgets();}));addRenderableWidget(new PhoneScreen.NeonButton(x+52,y+176,35,18,">",b->{page=(page+1)%pages();rebuildWidgets();}));
 if(exchange){long total=0;for(int i=0;i<8;i++)total+=(long)counts[i]*GuildCatalog.PRICES[i];addRenderableWidget(new PhoneScreen.NeonButton(x+100,y+176,w-112,18,"素材をすべて売る（"+total+" Cr）",b->send("sell",-1)));}
 addRenderableWidget(new PhoneScreen.NeonButton(x+12,y+212,w-24,18,"閉じる",b->onClose()));if(!requested){requested=true;send("view",-1);}}
 void send(String a,int i){if(minecraft.player!=null)minecraft.player.connection.sendCommand("neonguild "+a+" "+pos.getX()+" "+pos.getY()+" "+pos.getZ()+" "+i);}
 void receive(JsonObject o){cash=o.get("cash").getAsLong();message=o.get("message").getAsString();for(int i=0;i<8;i++)counts[i]=o.getAsJsonArray("counts").get(i).getAsInt();var q=o.getAsJsonObject("quests");for(int i=0;i<quests.length;i++)quests[i]=q.has(""+i)?q.get(""+i).getAsInt():-1;rank=o.get("rank").getAsInt();reports=o.get("reports").getAsInt();examActive=o.get("exam_active").getAsBoolean();examReady=o.get("exam_ready").getAsBoolean();examText=o.get("exam_text").getAsString();specs=o.getAsJsonArray("specs");rebuildWidgets();}
 @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){g.fill(x-3,y-3,x+w+3,y+235,0xff08121d);g.outline(x-3,y-3,w+6,238,0xff5ff5ec);g.text(font,"NEON GUILD / "+(exchange?"素材換金所":"ランク "+GuildRanks.NAMES[rank]),x+12,y+11,0xff73fff0);g.text(font,"残高 "+cash+" Cr",x+12,y+28,0xffffd071);if(!exchange)g.text(font,rank==6?"回数制限なし / 同時受注3件":"現ランクの報告 "+reports+"/5件 / 同時受注3件",x+12,y+42,0xff97bbcf);
 for(int row=0;row<(exchange?4:2);row++){int raw=page*(exchange?4:2)+row;if(!exchange&&raw>=quests.length)break;int i=exchange?raw:ORDER[raw];int yy=y+(exchange?47:59)+row*(exchange?31:43);if(exchange){g.item(new ItemStack(GuildServices.MATERIALS.get(i)),x+12,yy+3);g.text(font,GuildCatalog.MATERIAL_NAMES[i],x+35,yy+1,0xffeef5ff);g.text(font,counts[i]+"個 / 単価 "+GuildCatalog.PRICES[i]+" Cr",x+35,yy+14,0xff8cbaca);}else{var q=specs.size()>i?specs.get(i).getAsJsonObject():null;g.text(font,GuildCatalog.QUESTS[i].title()+" "+Math.max(0,quests[i])+"/"+target(i),x+12,yy,locked(i)?0xff7f8b9a:0xffeef5ff);g.text(font,font.plainSubstrByWidth(q==null?"読み込み中":q.get("condition").getAsString(),w-112),x+12,yy+13,0xff8cbaca);g.text(font,"報酬 "+(q==null?0:q.get("reward").getAsInt())+" Cr",x+12,yy+26,0xffffd071);}}
 g.text(font,(page+1)+" / "+pages(),x+w-70,y+28,0xff91adb9);g.text(font,font.plainSubstrByWidth(message,w-24),x+12,y+199,0xffffc67d);super.extractRenderState(g,mx,my,dt);}
}
