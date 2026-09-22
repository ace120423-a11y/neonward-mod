package jp.neonward;
import java.util.*;
import net.minecraft.server.level.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
/** Cosmetic notifications. Authoritative collision and damage stay in the weapon code. */
public final class GunVfx {
 static final String[] IDS={"pulse_rifle","kestrel_pistol","oni_handcannon","wisp_compact","longwatch_sniper","storm_machinegun","ion_railgun","plasma_launcher","arc_caster","cryo_projector","tactical_crossbow"};
 public static int profile(ItemStack s){for(int i=0;i<IDS.length;i++)if(s.is(NeonArsenal.ITEMS.get(IDS[i])))return i;return 0;}
 public record Visual(int owner,int kind,int phase,int mode,Vec3 from,Vec3 to) implements CustomPacketPayload {
  public static final Type<Visual> TYPE=new Type<>(NeonWard.id("gun_vfx"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Visual> CODEC=new StreamCodec<>(){
   public Visual decode(RegistryFriendlyByteBuf b){return new Visual(b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()));}
   public void encode(RegistryFriendlyByteBuf b,Visual v){b.writeVarInt(v.owner);b.writeVarInt(v.kind);b.writeVarInt(v.phase);b.writeVarInt(v.mode);b.writeDouble(v.from.x);b.writeDouble(v.from.y);b.writeDouble(v.from.z);b.writeDouble(v.to.x);b.writeDouble(v.to.y);b.writeDouble(v.to.z);}
  };public Type<? extends CustomPacketPayload> type(){return TYPE;}
  boolean valid(){return kind>=0&&kind<11&&phase>=0&&phase<=3&&mode>=-1&&mode<=2&&Double.isFinite(from.lengthSqr())&&Double.isFinite(to.lengthSqr())&&from.distanceToSqr(to)<=170*170;}
 }
 static final Map<ServerLevel,long[]> BUDGET=new WeakHashMap<>();
 static void init(){PayloadTypeRegistry.clientboundPlay().register(Visual.TYPE,Visual.CODEC);ServerLifecycleEvents.SERVER_STOPPED.register(s->BUDGET.clear());}
 static void send(ServerPlayer p,ItemStack weapon,int phase,int mode,Vec3 from,Vec3 to){send(p.level(),new Visual(p.getId(),profile(weapon),phase,mode,from,to));}
 static void send(ServerLevel l,Visual v){if(!v.valid())return;long now=l.getGameTime();var b=BUDGET.computeIfAbsent(l,k->new long[]{now,0});if(b[0]!=now){b[0]=now;b[1]=0;}if(b[1]++>=32)return;
  for(var p:l.players())if((p.distanceToSqr(v.from)<32*32||p.distanceToSqr(v.to)<32*32)&&ServerPlayNetworking.canSend(p,Visual.TYPE))ServerPlayNetworking.send(p,v);
 }
}
