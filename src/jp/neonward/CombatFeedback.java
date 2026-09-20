package jp.neonward;
import java.util.*;
import net.minecraft.world.entity.*;
import net.minecraft.server.level.*;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import jp.neonward.mixin.MeterTextAccess;

/** Cosmetic server-owned text: never changes damage, targeting or loot. */
public final class CombatFeedback {
 static final Map<CyberEnemy,Display.TextDisplay> bars=new HashMap<>();
 static final List<Popup> hits=new ArrayList<>();
 record Popup(Display.TextDisplay text,long born){}
 static class TransientText extends Display.TextDisplay {
  TransientText(ServerLevel l){super(EntityTypes.TEXT_DISPLAY,l);}
  @Override public boolean shouldBeSaved(){return false;}
 }
 static String number(float n){return Math.abs(n-Math.round(n))<.05?String.valueOf(Math.round(n)):String.format(Locale.ROOT,"%.1f",n);}
 static Display.TextDisplay text(ServerLevel l,double x,double y,double z,float scale){
  var e=new TransientText(l);ClockworkFeedback.load(e,l,"{text:{text:''},billboard:'center',background:-1274542030,brightness:{block:15,sky:15},Invulnerable:1b,line_width:240,teleport_duration:1,view_range:0.75f,Tags:['nw_combat_feedback'],transformation:{scale:["+scale+"f,"+scale+"f,"+scale+"f]}}");e.setPos(x,y,z);l.addFreshEntity(e);return e;
 }
 static void hit(CyberEnemy enemy,float damage){
  if(!(enemy.level() instanceof ServerLevel l)||!Float.isFinite(damage)||damage<=0)return;
  while(hits.size()>=256)hits.removeFirst().text().discard();
  double side=(hits.size()%3-1)*.3;var e=text(l,enemy.getX()+side,enemy.getY()+enemy.getBbHeight()+.5,enemy.getZ(),.9f);
  ((MeterTextAccess)e).neonSetText(Component.literal("−"+number(damage)).withColor(0xffd36b));hits.add(new Popup(e,l.getGameTime()));
 }
 static void bar(CyberEnemy enemy){
  var l=(ServerLevel)enemy.level();var e=bars.get(enemy);if(e==null||e.isRemoved()){e=text(l,enemy.getX(),enemy.getY()+enemy.getBbHeight()+.25,enemy.getZ(),.6f);bars.put(enemy,e);}
  e.setPos(enemy.getX(),enemy.getY()+enemy.getBbHeight()+.25,enemy.getZ());float hp=Math.max(0,enemy.getHealth()),max=enemy.getMaxHealth();int filled=Math.clamp((int)Math.ceil(16*hp/Math.max(1,max)),0,16);
  var value=Component.literal("▰".repeat(filled)).withColor(hp/max>.5?0x54efcc:hp/max>.25?0xffce55:0xff547b).append(Component.literal("▰".repeat(16-filled)).withColor(0x435365)).append(Component.literal("\n"+number(hp)+" / "+number(max)+" HP").withColor(0xe9fffb));
  var access=(MeterTextAccess)e;if(!access.neonGetText().equals(value))access.neonSetText(value);
 }
 static void tick(net.minecraft.server.MinecraftServer server){
  hits.removeIf(p->{if(p.text().isRemoved()||p.text().level().getGameTime()-p.born()>=22){p.text().discard();return true;}var e=p.text();e.setPos(e.getX(),e.getY()+.035,e.getZ());long age=e.level().getGameTime()-p.born();((MeterTextAccess)e).neonSetOpacity((byte)(age<13?255:Math.max(0,255-(age-13)*28)));return false;});
  if(server.getTickCount()%2!=0)return;Set<CyberEnemy> visible=new HashSet<>();
  for(var p:server.getPlayerList().getPlayers())if(!p.isSpectator())for(var e:p.level().getEntitiesOfClass(CyberEnemy.class,p.getBoundingBox().inflate(32),e->e.isAlive()&&e.distanceToSqr(p)<1024))if(!(e instanceof SpireBoss)&&p.hasLineOfSight(e))visible.add(e);
  bars.entrySet().removeIf(e->{if(e.getKey().isRemoved()||!visible.contains(e.getKey())){e.getValue().discard();return true;}return false;});for(var e:visible)bar(e);
 }
 static void init(){ServerTickEvents.END_SERVER_TICK.register(CombatFeedback::tick);ServerLifecycleEvents.SERVER_STOPPED.register(s->{bars.clear();hits.clear();});}
}
