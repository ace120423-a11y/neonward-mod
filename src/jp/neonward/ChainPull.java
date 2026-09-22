package jp.neonward;
import java.util.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraft.sounds.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;

/** Short, server-authoritative casts. No persistent entities or forced chunks. */
public final class ChainPull {
 public static final int FLIGHT=5,DURATION=18,LIMIT=32;
 public record Visual(int owner,int target,double x,double y,double z,long start,boolean cancel) implements CustomPacketPayload {
  public static final Type<Visual> TYPE=new Type<>(NeonWard.id("chain_pull"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Visual> CODEC=new StreamCodec<>(){
   public Visual decode(RegistryFriendlyByteBuf b){return new Visual(b.readVarInt(),b.readVarInt(),b.readDouble(),b.readDouble(),b.readDouble(),b.readLong(),b.readBoolean());}
   public void encode(RegistryFriendlyByteBuf b,Visual v){b.writeVarInt(v.owner);b.writeVarInt(v.target);b.writeDouble(v.x);b.writeDouble(v.y);b.writeDouble(v.z);b.writeLong(v.start);b.writeBoolean(v.cancel);}
  };
  public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 static final class Cast {
  final ServerPlayer owner;final ServerLevel level;final LivingEntity target;final float damage;final long start;final Vec3 end;boolean impacted;
  Cast(ServerPlayer p,LivingEntity t,float d,Vec3 end){this(p,t,d,end,p.level().getGameTime());}
  Cast(ServerPlayer p,LivingEntity t,float d,Vec3 end,long start){owner=p;level=p.level();target=t;damage=d;this.end=end;this.start=start;}
 }
 static final Map<UUID,Cast> ACTIVE=new HashMap<>();
 static void init(){
  PayloadTypeRegistry.clientboundPlay().register(Visual.TYPE,Visual.CODEC);
  ServerTickEvents.END_SERVER_TICK.register(s->tick());
  ServerLifecycleEvents.SERVER_STOPPED.register(s->ACTIVE.clear());
  ServerPlayConnectionEvents.DISCONNECT.register((h,s)->{var c=ACTIVE.remove(h.player.getUUID());if(c!=null)send(c,true);});
 }
 static void launch(ServerPlayer p,LivingEntity target,float damage){
  if(ACTIVE.containsKey(p.getUUID())||ACTIVE.size()>=LIMIT)return;
  Vec3 end=target==null?p.level().clip(new ClipContext(p.getEyePosition(),p.getEyePosition().add(p.getLookAngle().scale(9)),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p)).getLocation():target.position().add(0,target.getBbHeight()*.55,0);
  var c=new Cast(p,target,damage,end);ACTIVE.put(p.getUUID(),c);send(c,false);p.swing(InteractionHand.MAIN_HAND,true);
  p.level().playSound(null,p.blockPosition(),SoundEvents.CHAIN_PLACE,SoundSource.PLAYERS,.55f,1.25f);
 }
 static void send(Cast c,boolean cancel){var v=new Visual(c.owner.getId(),c.target==null?-1:c.target.getId(),c.end.x,c.end.y,c.end.z,c.start,cancel);for(var p:c.level.players())if(p.distanceToSqr(c.owner)<48*48&&ServerPlayNetworking.canSend(p,Visual.TYPE))ServerPlayNetworking.send(p,v);}
 static boolean valid(Cast c){return c.owner.isAlive()&&!c.owner.isRemoved()&&!c.owner.isSpectator()&&c.owner.level()==c.level&&c.owner.getMainHandItem().is(NeonArsenal.ITEMS.get("chain_kusarigama"));}
 static void tick(){
  var it=ACTIVE.values().iterator();while(it.hasNext()){
   var c=it.next();long age=c.level.getGameTime()-c.start;
   if(!valid(c)||age>=DURATION){send(c,true);it.remove();continue;}
   if(!c.impacted&&age>=FLIGHT){
    c.impacted=true;var e=c.target;
    if(e!=null&&ArsenalExpansion.enemy(c.owner,e)&&c.owner.distanceToSqr(e)<=10*10&&c.owner.hasLineOfSight(e)){
     boolean hit=ArsenalExpansion.hurt(c.owner,e,c.damage);
     if(hit&&!ArsenalExpansion.boss(e)){var dir=c.owner.position().subtract(e.position()).normalize();e.setDeltaMovement(dir.scale(.7).add(0,.12,0));}
     c.level.playSound(null,e.blockPosition(),SoundEvents.CHAIN_HIT,SoundSource.PLAYERS,.65f,.8f);c.owner.swing(InteractionHand.MAIN_HAND,true);
    }
   }
  }
 }
}
