package jp.neonward;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Display;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import com.mojang.math.Transformation;
import org.joml.Vector3f;
import org.joml.Quaternionf;
import jp.neonward.mixin.MeterTextAccess;
/** Visual results are published only after the corresponding ledger transaction succeeds. */
public final class CasinoProps {
 static final String[] SYMBOLS={"7","BAR","★","♦","♣","♥"};
 static final int[] COLORS={0xff4768,0x61fff0,0xffd46a,0x68beff,0x8bff9d,0xff71ca};
 static final Map<Display,String> MATERIALS=new WeakHashMap<>();
 static final Map<String,net.minecraft.world.entity.Interaction> TARGETS=new HashMap<>();
 static long slotStart=-1000,slotEnd,cardStart;
 static String tableOwner="",pc="",dc="",status="右クリックでブラックジャック";
 static int playerTotal,dealerTotal;static boolean hidden;
 static boolean busy(long now){return now<slotEnd;}
 static void spin(long now){slotStart=now;slotEnd=now+80;}
 static void hand(ServerPlayer p,CasinoGames.Hand h){if(h==null)return;if(tableOwner.equals(p.getName().getString())&&pc.equals(CasinoGames.cards(h.player,false))&&dc.equals(CasinoGames.cards(h.dealer,h.active)))return;tableOwner=p.getName().getString();pc=CasinoGames.cards(h.player,false);dc=CasinoGames.cards(h.dealer,h.active);hidden=h.active;playerTotal=CasinoGames.total(h.player);dealerTotal=h.active?CasinoGames.value(h.dealer.getFirst()):CasinoGames.total(h.dealer);status=h.active?"HIT / STAND":h.result+" / "+h.payout+" Cr";cardStart=p.level().getGameTime();}
 public static void init(){net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.register((p,l,h,e,hit)->{if(e.entityTags().contains("nw_casino_touch_slot")||e.entityTags().contains("nw_casino_touch_cards")){if(p instanceof ServerPlayer sp&&h==net.minecraft.world.InteractionHand.MAIN_HAND)NeonCasino.request(sp,"view",e.entityTags().contains("nw_casino_touch_cards")?1:0);return net.minecraft.world.InteractionResult.SUCCESS;}return net.minecraft.world.InteractionResult.PASS;});ServerLifecycleEvents.SERVER_STARTED.register(s->{TARGETS.clear();slotStart=-1000;slotEnd=0;tableOwner="";pc="";dc="";status="右クリックでブラックジャック";});ServerTickEvents.END_SERVER_TICK.register(s->{var l=s.overworld();long now=l.getGameTime();if(now%2!=0)return;
  if(l.isPositionEntityTicking(NeonCasino.DESK)&&l.getBlockState(NeonCasino.DESK).is(NeonFurniture.BLOCKS.get("neon_counter"))&&(CasinoSlots.animating(now)||now%20==0))machine(l,now);
  if(l.isPositionEntityTicking(NeonCasino.BLACKJACK)&&l.getBlockState(NeonCasino.BLACKJACK).is(NeonFurniture.BLOCKS.get("neon_counter"))&&(now-cardStart<60||now%20==0))table(l,now);
 });}
 static void machine(ServerLevel l,long now){CasinoSlots.render(l,now);}
 static List<String> tokens(String s){var out=new ArrayList<String>();var m=java.util.regex.Pattern.compile("\\[([^\\]]+)\\]").matcher(s);while(m.find())out.add(m.group(1));return out;}
 static void table(ServerLevel l,long now){
  touch(l,"cards",498.5,65,464.5,4.8f,1.2f);
  box(l,"felt",498.5,66.064,464.5,4.7f,.015f,.94f,"green_concrete");
  text(l,"table_name",498.5,67.05,464.6,tableOwner.isEmpty()?"BLACKJACK":tableOwner+" / BLACKJACK",0xffd46a,.45f,false);
  text(l,"table_result",498.5,66.75,464.68,status,0x76ffce,.33f,false);
  for(int row=0;row<2;row++){
   var cards=tokens(row==0?dc:pc);int count=cards.size();float width=Math.min(.38f,3.8f/Math.max(1,count));
   for(int i=0;i<22;i++){String key="card_"+row+"_"+i;boolean visible=i<count;String v=visible?cards.get(i):"";boolean back=v.equals("??");
    double progress=Math.clamp((now-cardStart-i*3)/10.0,0,1);double cx=498.5+(i-(count-1)/2.0)*width,cz=464.25+row*.48;double xx=499.9+(cx-499.9)*progress,zz=464.10+(cz-464.10)*progress;
    box(l,key,xx,66.09+i*.0004,zz,visible?width-.025f:0,.014f,visible?.39f:0,back?"cyan_concrete":"white_concrete");
    String face=back?"◆":face(v);text(l,key+"_ink",xx,66.113+i*.0004,zz,face,v.endsWith("H")||v.endsWith("D")?0xbb2452:back?0x163854:0x15202c,visible?.27f:0,true);
   }
  }
  text(l,"totals",498.5,66.45,465.04,tableOwner.isEmpty()?"100 Cr / HIT:追加  STAND:勝負":"自分 "+playerTotal+"   親 "+dealerTotal+(hidden?" + ?":""),0xffecd1,.30f,false);
  for(int i=0;i<6;i++)box(l,"chips"+i,500.45+(i%2)*.14,66.09+(i/2)*.04,464.5,.12f,.035f,.12f,i%2==0?"red_concrete":"blue_concrete");
 }
 static void touch(ServerLevel l,String id,double x,double y,double z,float width,float height){String tag="nw_casino_touch_"+id;var target=TARGETS.get(id);if(target!=null&&!target.isRemoved())return;for(var e:l.getAllEntities())if(e instanceof net.minecraft.world.entity.Interaction in&&e.entityTags().contains(tag)){if(target==null||target.isRemoved())target=in;else e.discard();}if(target==null||target.isRemoved()){target=new net.minecraft.world.entity.Interaction(net.minecraft.world.entity.EntityTypes.INTERACTION,l);ClockworkFeedback.load(target,l,"{width:"+width+"f,height:"+height+"f,response:1b,Invulnerable:1b,Tags:['"+tag+"']}");target.setPos(x,y,z);l.addFreshEntity(target);}TARGETS.put(id,target);}
 static String face(String v){if(v.isEmpty())return "";String suit=switch(v.charAt(v.length()-1)){case 'S'->"♠";case 'H'->"♥";case 'D'->"♦";case 'C'->"♣";default->"";};return v.substring(0,v.length()-1)+suit;}
 static void box(ServerLevel l,String key,double x,double y,double z,float w,float h,float d,String block){var at=new Vec3(x,y,z);String tag="nw_casino_prop_"+key;var e=ClockworkDisplays.find(l,tag,at,Display.BlockDisplay.class);if(e==null)return;
  // Card backs change material when revealed; load the block state only when it changes.
  if(!block.equals(MATERIALS.get(e))){MATERIALS.put(e,block);ClockworkDisplays.POSES.remove(e);ClockworkFeedback.load(e,l,"{block_state:{Name:'minecraft:"+block+"'},Invulnerable:1b,brightness:{block:15,sky:15},Tags:['"+tag+"']}");}
  e.setPos(at);ClockworkDisplays.transform(e,new Transformation(new Vector3f(-w/2,0,-d/2),new Quaternionf(),new Vector3f(w,h,d),new Quaternionf()));ClockworkDisplays.publish(l,e);
 }
 static void text(ServerLevel l,String key,double x,double y,double z,String value,int color,float scale,boolean flat){var at=new Vec3(x,y,z);String tag="nw_casino_prop_"+key;var e=ClockworkDisplays.find(l,tag,at,Display.TextDisplay.class);if(e==null)return;if(!ClockworkDisplays.initialized(e))ClockworkFeedback.load(e,l,"{text:{text:''},billboard:'fixed',background:0,brightness:{block:15,sky:15},Invulnerable:1b,line_width:400,Tags:['"+tag+"']}");e.setPos(at);var a=(MeterTextAccess)e;var c=Component.literal(value).withColor(color);if(key.startsWith("japan_mj_ink"))c.withStyle(style->style.withFont(new net.minecraft.network.chat.FontDescription.Resource(NeonWard.id("mahjong"))));if(!a.neonGetText().equals(c))a.neonSetText(c);ClockworkDisplays.transform(e,new Transformation(new Vector3f(),flat?new Quaternionf().rotationX(-(float)Math.PI/2):new Quaternionf(),new Vector3f(scale),new Quaternionf()));ClockworkDisplays.publish(l,e);}
}
