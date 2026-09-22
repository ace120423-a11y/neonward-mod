package jp.neonward;
import java.util.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

/** Cosmetic S2C events only. No damage, world edits, entities or chunk loading. */
public final class ElementVfx {
 public static final int THUNDER=0,FIRE=1,POISON=2,WAVE=3,BLAST=4,BLEED=5,RANGE=32;
 public record Visual(int kind,int target,double x,double y,double z,float yaw,int duration,boolean aura) implements CustomPacketPayload {
  public static final Type<Visual> TYPE=new Type<>(NeonWard.id("element_vfx"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Visual> CODEC=new StreamCodec<>(){
   public Visual decode(RegistryFriendlyByteBuf b){return new Visual(b.readVarInt(),b.readVarInt(),b.readDouble(),b.readDouble(),b.readDouble(),b.readFloat(),b.readVarInt(),b.readBoolean());}
   public void encode(RegistryFriendlyByteBuf b,Visual v){b.writeVarInt(v.kind);b.writeVarInt(v.target);b.writeDouble(v.x);b.writeDouble(v.y);b.writeDouble(v.z);b.writeFloat(v.yaw);b.writeVarInt(v.duration);b.writeBoolean(v.aura);}
  };
  public Type<? extends CustomPacketPayload> type(){return TYPE;}
  public boolean valid(){return kind>=0&&kind<=BLEED&&duration>0&&duration<=100&&Double.isFinite(x)&&Double.isFinite(y)&&Double.isFinite(z)&&Float.isFinite(yaw);}
 }
 static final Map<ServerLevel,long[]> BUDGET=new WeakHashMap<>();
 public static void init(){PayloadTypeRegistry.clientboundPlay().register(Visual.TYPE,Visual.CODEC);ServerLifecycleEvents.SERVER_STOPPED.register(s->BUDGET.clear());}
 public static void emit(ServerPlayer p,LivingEntity target,int kind,int duration,boolean aura){
  Vec3 at=target.position().add(0,kind==WAVE?.12:Math.max(.35,target.getBbHeight()*.55),0);
  send(p.level(),new Visual(kind,target.getId(),at.x,at.y,at.z,p.getYRot(),duration,aura));
 }
 static void send(ServerLevel level,Visual v){
  if(!v.valid())return;long now=level.getGameTime();var budget=BUDGET.computeIfAbsent(level,l->new long[]{now,0});
  if(budget[0]!=now){budget[0]=now;budget[1]=0;}if(budget[1]>=24)return;budget[1]++;
  for(var viewer:level.players())if(viewer.distanceToSqr(v.x,v.y,v.z)<=RANGE*RANGE&&ServerPlayNetworking.canSend(viewer,Visual.TYPE))ServerPlayNetworking.send(viewer,v);
 }
 private ElementVfx(){}
}
