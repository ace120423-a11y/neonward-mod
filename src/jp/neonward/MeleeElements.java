package jp.neonward;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
public final class MeleeElements {
 public enum Kind {THUNDER,FIRE,POISON,WAVE}
 public static Kind kind(ItemStack stack){var id=BuiltInRegistries.ITEM.getKey(stack.getItem());if(!id.getNamespace().equals("neonward"))return null;return switch(id.getPath()){case "neon_blade","raikiri_odachi"->Kind.THUNDER;case "akatsuki_wakizashi"->Kind.FIRE;case "kurosame_katana"->Kind.POISON;case "shock_bat","riot_bat","coil_hammer","pile_maul"->Kind.WAVE;default->null;};}
 public static String description(ItemStack s){var k=kind(s);return k==null?"":switch(k){case THUNDER->"雷属性 / 短時間スタン";case FIRE->"炎属性 / 4秒間の継続ダメージ";case POISON->"毒属性 / 5秒間の継続ダメージ（不死系にも有効）";case WAVE->"衝撃波 / 周囲の敵をノックバック";};}
 static final Map<UUID,Long> NEXT=new HashMap<>();
 static final Map<LivingEntity,Long> STUN=new IdentityHashMap<>(),RESIST=new IdentityHashMap<>();
 record Dot(ServerPlayer owner,Kind kind,long end,long next){}
 static final Map<LivingEntity,Dot> DOT=new IdentityHashMap<>();
 public static boolean stunned(LivingEntity e){return e.level() instanceof ServerLevel l&&STUN.getOrDefault(e,0L)>l.getGameTime();}
 static boolean enemy(ServerPlayer p,LivingEntity e){return e instanceof Enemy&&e.isAlive()&&!e.isSpectator()&&!e.isAlliedTo(p)&&e.level()==p.level();}
 public static void impact(ServerPlayer p,LivingEntity target,ItemStack weapon,float damage){Kind kind=kind(weapon);if(kind==null||!(target instanceof Enemy))return;var l=p.level();long now=l.getGameTime();var shape=MeleeReach.profile(weapon);boolean shortBlade=shape!=null&&shape.shape()==MeleeShape.Shape.WAKIZASHI;if(now<NEXT.getOrDefault(p.getUUID(),0L))return;NEXT.put(p.getUUID(),now+(shortBlade?6:12));var center=target.position().add(0,Math.min(1,target.getBbHeight()*.5),0);
  if(kind==Kind.WAVE){boolean hammer=weapon.is(NeonArsenal.ITEMS.get("coil_hammer"))||weapon.is(NeonArsenal.ITEMS.get("pile_maul"));double radius=hammer?3.5:2.5,force=hammer?1.5:1.0;int color=hammer?0xffc15c:0x66dcff;
   ElementVfx.emit(p,target,ElementVfx.WAVE,14,false);
   var candidates=l.getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(7,3,7),e->enemy(p,e)&&(e==target||MeleeReach.inShape(p,e,target,shape)));candidates.sort(Comparator.comparingDouble(target::distanceToSqr));int hit=0;
   for(var e:candidates){if(hit++>=12)break;var dir=e.position().subtract(p.position()).multiply(1,0,1);if(dir.lengthSqr()<.001)dir=p.getLookAngle();boolean affected=e==target||e.hurtServer(l,p.damageSources().playerAttack(p),Math.max(1,damage*.25f));if(affected)e.knockback(force,-dir.x,-dir.z,p.damageSources().playerAttack(p),damage);}
   l.playSound(null,target.blockPosition(),SoundEvents.GENERIC_EXPLODE.value(),SoundSource.PLAYERS,.45f,hammer?.7f:1.2f);return;
  }
  // Narrow forward cuts; secondary hits do not recurse through Player.attack.
  if(shape!=null&&!shortBlade){var candidates=l.getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(shape.reach(),2,shape.reach()),e->e!=target&&enemy(p,e)&&MeleeReach.inShape(p,e,target,shape));candidates.sort(Comparator.comparingDouble(p::distanceToSqr));int hits=0;for(var e:candidates){if(hits++>=4)break;if(e.hurtServer(l,p.damageSources().playerAttack(p),Math.max(1,damage*.25f)))applyElement(p,e,kind,now);}}
  applyElement(p,target,kind,now);
 }
 static void applyElement(ServerPlayer p,LivingEntity target,Kind kind,long now){var l=p.level();var center=target.position().add(0,Math.min(1,target.getBbHeight()*.5),0);
  int color=kind==Kind.THUNDER?0x6cecff:kind==Kind.FIRE?0xff702c:0x83ed49;
  ElementVfx.emit(p,target,kind.ordinal(),10,false);
  if(kind==Kind.THUNDER){l.playSound(null,target.blockPosition(),SoundEvents.LIGHTNING_BOLT_IMPACT,SoundSource.PLAYERS,.28f,1.7f);if(enemy(p,target)&&now>=RESIST.getOrDefault(target,0L)){boolean boss=target instanceof SpireBoss||target instanceof SkyBoss;STUN.put(target,now+(boss?8:20));ElementVfx.emit(p,target,ElementVfx.THUNDER,boss?8:20,true);RESIST.put(target,now+60);if(target instanceof Mob mob)mob.getNavigation().stop();target.setDeltaMovement(0,target.getDeltaMovement().y,0);}}
  else if(enemy(p,target)){if(DOT.size()<256||DOT.containsKey(target)){var previous=DOT.get(target);DOT.put(target,new Dot(p,kind,now+(kind==Kind.FIRE?80:100),previous==null?now+20:previous.next()));ElementVfx.emit(p,target,kind.ordinal(),kind==Kind.FIRE?80:100,true);}if(kind==Kind.FIRE)target.igniteForSeconds(4);l.playSound(null,target.blockPosition(),kind==Kind.FIRE?SoundEvents.FIRECHARGE_USE:SoundEvents.SLIME_SQUISH,SoundSource.PLAYERS,.35f,1.3f);}
 }
 static void particle(ServerLevel l,Vec3 at,int color,float size){l.sendParticles(new DustParticleOptions(color,size),at.x,at.y,at.z,1,0,0,0,0);}
 static void tick(net.minecraft.server.MinecraftServer server){STUN.entrySet().removeIf(e->!e.getKey().isAlive()||e.getKey().isRemoved()||e.getKey().level().getGameTime()>=e.getValue());RESIST.entrySet().removeIf(e->!e.getKey().isAlive()||e.getKey().isRemoved()||e.getKey().level().getGameTime()>=e.getValue());
  for(var entry:new ArrayList<>(DOT.entrySet())){var e=entry.getKey();var d=entry.getValue();long now=e.level().getGameTime();if(!e.isAlive()||e.isRemoved()||!d.owner().isAlive()||d.owner().hasDisconnected()||!enemy(d.owner(),e)||now>d.end()){DOT.remove(e);continue;}if(now>=d.next()){e.hurtServer((ServerLevel)e.level(),d.owner().damageSources().playerAttack(d.owner()),2f);DOT.put(e,new Dot(d.owner(),d.kind(),d.end(),now+20));}}
 }
 public static void init(){ElementVfx.init();ServerTickEvents.END_SERVER_TICK.register(MeleeElements::tick);ServerTickEvents.END_SERVER_TICK.register(s->s.getPlayerList().getPlayers().forEach(MeleeGuard::updateSpeed));ServerPlayConnectionEvents.DISCONNECT.register((h,s)->NEXT.remove(h.player.getUUID()));ServerLifecycleEvents.SERVER_STOPPED.register(s->{NEXT.clear();STUN.clear();RESIST.clear();DOT.clear();});}
}
