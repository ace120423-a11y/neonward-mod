package jp.neonward;

import java.util.*;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.entity.event.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.phys.Vec3;

/** Server tick authority; clients render Motion only. No pose forcing, inventory props or charge on start. */
public final class ShrineRituals {
 public static final int PRAYER=0,WASH=1,PRAYER_TICKS=200,WASH_TICKS=160;
 public record Motion(String json) implements CustomPacketPayload {
  public static final Type<Motion> TYPE=new Type<>(NeonWard.id("shrine_ritual_motion_v1"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Motion> CODEC=StreamCodec.composite(ByteBufCodecs.STRING_UTF8,Motion::json,Motion::new);
  public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 static final class Ritual {
  final ServerPlayer player;final ServerLevel level;final Vec3 start;final int kind,started,duration;
  final Set<UUID> viewers=new HashSet<>();
  Ritual(ServerPlayer p,int kind){player=p;level=p.level();start=p.position();this.kind=kind;started=level.getServer().getTickCount();duration=kind==PRAYER?PRAYER_TICKS:WASH_TICKS;}
  int elapsed(){return Math.clamp((long)level.getServer().getTickCount()-started,0,duration);}
 }
 static final Map<UUID,Ritual> ACTIVE=new HashMap<>();private static boolean initialized;
 private ShrineRituals(){}
 public static boolean active(ServerPlayer p){return ACTIVE.containsKey(p.getUUID());}
 public static int elapsed(ServerPlayer p){var r=ACTIVE.get(p.getUUID());return r==null?0:r.elapsed();}
 public static int kind(ServerPlayer p){var r=ACTIVE.get(p.getUUID());return r==null?-1:r.kind;}
 public static int start(ServerPlayer p,int kind){
  if(kind!=PRAYER&&kind!=WASH||active(p)||p.hasDisconnected()||!ShrineServices.near(p,kind)||p.isPassenger())return 0;
  // Never reserve/debit money. Revalidate again when the ritual finishes.
  if(!ShrineServices.ritualAvailable(p,kind))return 0;
  var session=ShrineServices.SESSIONS.remove(p.getUUID());var ritual=new Ritual(p,kind);ACTIVE.put(p.getUUID(),ritual);
  broadcast(ritual,true);ShrineServices.acknowledgeStart(p,kind,session!=null&&session.kind()==kind?session.token():-1);
  p.sendSystemMessage(Component.literal(kind==PRAYER?"お参りを始めます / 10秒間その場で / 完了時のみ100 Cr":"手水を始めます / 8秒間その場で / 無料"));return 1;
 }
 static Motion packet(Ritual r,boolean active){var json=new JsonObject();json.addProperty("uuid",r.player.getStringUUID());json.addProperty("kind",r.kind);json.addProperty("elapsed",r.elapsed());json.addProperty("duration",r.duration);json.addProperty("active",active);return new Motion(json.toString());}
 static void send(Ritual r,ServerPlayer viewer,boolean active){if(ServerPlayNetworking.canSend(viewer,Motion.TYPE)){ServerPlayNetworking.send(viewer,packet(r,active));if(active)r.viewers.add(viewer.getUUID());}}
 static void broadcast(Ritual r,boolean active){
  var viewers=new HashSet<>(PlayerLookup.tracking(r.player));viewers.addAll(PlayerLookup.around(r.level,r.start,48));viewers.add(r.player);
  if(!active)for(var id:r.viewers){var p=r.level.getServer().getPlayerList().getPlayer(id);if(p!=null)viewers.add(p);}
  for(var viewer:viewers)send(r,viewer,active);
 }
 public static void cancel(ServerPlayer p){finish(p.getUUID(),false,"中断しました（課金・効果なし）");}
 static void finish(UUID id,boolean complete,String reason){
  var r=ACTIVE.remove(id);if(r==null)return;boolean success=false;
  if(complete)success=ShrineServices.completeRitual(r.player,r.kind);
  broadcast(r,false);ShrineServices.SESSIONS.remove(id);
  if(!r.player.hasDisconnected())r.player.sendSystemMessage(Component.literal(complete?(success?(r.kind==PRAYER?"お参り完了 / 100 Crを奉納・加護を授かりました（5分）":"手水完了 / 毒を解除・再生 I・5秒"):"完了できませんでした（課金・効果なし）/ 残高・保存状態・加護を確認してください"):reason));
 }
 static void tick(MinecraftServer server){
  for(var r:List.copyOf(ACTIVE.values())){
   var p=r.player;if(r.level.getServer()!=server)continue;
   if(p.hasDisconnected()||!p.isAlive()||p.isSpectator()||p.isPassenger()||p.level()!=r.level||p.position().distanceToSqr(r.start)>.75*.75||!ShrineServices.near(p,r.kind)){
    finish(p.getUUID(),false,"動作を中断しました（課金・効果なし）");continue;
   }
   if(r.elapsed()>=r.duration){finish(p.getUUID(),true,"");continue;}
   if(r.elapsed()%20==0)broadcast(r,true); // Bounded correction/nearby late-viewer snapshots, not every frame.
  }
 }
 public static void init(){
  if(initialized)return;initialized=true;PayloadTypeRegistry.clientboundPlay().register(Motion.TYPE,Motion.CODEC);
  ServerTickEvents.END_SERVER_TICK.register(ShrineRituals::tick);
  ServerLivingEntityEvents.AFTER_DAMAGE.register((entity,source,base,taken,blocked)->{if(entity instanceof ServerPlayer p&&(taken>0||base>0&&!blocked))cancel(p);});
  ServerLivingEntityEvents.AFTER_DEATH.register((entity,source)->{if(entity instanceof ServerPlayer p)cancel(p);});
  ServerPlayConnectionEvents.DISCONNECT.register((h,s)->cancel(h.player));
  ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((p,origin,destination)->cancel(p));
  EntityTrackingEvents.START_TRACKING.register((entity,viewer)->{var r=ACTIVE.get(entity.getUUID());if(r!=null)send(r,viewer,true);});
  EntityTrackingEvents.STOP_TRACKING.register((entity,viewer)->{var r=ACTIVE.get(entity.getUUID());if(r!=null){send(r,viewer,false);r.viewers.remove(viewer.getUUID());}});
  ServerLifecycleEvents.SERVER_STOPPING.register(s->{for(var r:List.copyOf(ACTIVE.values()))if(r.level.getServer()==s)finish(r.player.getUUID(),false,"終了のため中断しました（課金・効果なし）");});
  ServerLifecycleEvents.SERVER_STOPPED.register(s->ACTIVE.entrySet().removeIf(e->e.getValue().level.getServer()==s));
 }
}
