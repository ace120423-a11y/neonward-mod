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
 public static void impact(ServerPlayer p,LivingEntity target,ItemStack weapon,float damage){Kind kind=kind(weapon);if(kind==null||!(target instanceof Enemy))return;var l=p.level();long now=l.getGameTime();if(now<NEXT.getOrDefault(p.getUUID(),0L))return;NEXT.put(p.getUUID(),now+12);var center=target.position().add(0,Math.min(1,target.getBbHeight()*.5),0);
  if(kind==Kind.WAVE){boolean hammer=weapon.is(NeonArsenal.ITEMS.get("coil_hammer"))||weapon.is(NeonArsenal.ITEMS.get("pile_maul"));double radius=hammer?3.5:2.5,force=hammer?1.5:1.0;int color=hammer?0xffc15c:0x66dcff;
   for(int ring=1;ring<=3;ring++)for(int i=0;i<20;i++){double a=i*Math.PI/10,r=radius*ring/3;particle(l,center.add(Math.cos(a)*r,-.4,Math.sin(a)*r),color,.8f);}
   var candidates=l.getEntitiesOfClass(LivingEntity.class,target.getBoundingBox().inflate(radius),e->enemy(p,e)&&e.distanceToSqr(target)<=radius*radius&&p.hasLineOfSight(e)&&target.hasLineOfSight(e));candidates.sort(Comparator.comparingDouble(target::distanceToSqr));int hit=0;
   for(var e:candidates){if(hit++>=12)break;var dir=e.position().subtract(p.position()).multiply(1,0,1);if(dir.lengthSqr()<.001)dir=p.getLookAngle();boolean affected=e==target||e.hurtServer(l,p.damageSources().playerAttack(p),Math.max(1,damage*.25f));if(affected)e.knockback(force,-dir.x,-dir.z,p.damageSources().playerAttack(p),damage);}
   l.playSound(null,target.blockPosition(),SoundEvents.GENERIC_EXPLODE.value(),SoundSource.PLAYERS,.45f,hammer?.7f:1.2f);return;
  }
  int color=kind==Kind.THUNDER?0x6cecff:kind==Kind.FIRE?0xff702c:0x83ed49;
  for(int i=0;i<22;i++){double a=-1.2+i*.11;particle(l,center.add(Math.cos(a)*.8,Math.sin(a)*.8,(i%2==0?.10:-.10)),color,1f);}
  if(kind==Kind.THUNDER){l.sendParticles(ParticleTypes.ELECTRIC_SPARK,center.x,center.y,center.z,14,.35,.5,.35,.1);l.playSound(null,target.blockPosition(),SoundEvents.LIGHTNING_BOLT_IMPACT,SoundSource.PLAYERS,.28f,1.7f);if(enemy(p,target)&&now>=RESIST.getOrDefault(target,0L)){boolean boss=target instanceof SpireBoss||target instanceof SkyBoss;STUN.put(target,now+(boss?8:20));RESIST.put(target,now+60);if(target instanceof Mob mob)mob.getNavigation().stop();target.setDeltaMovement(0,target.getDeltaMovement().y,0);}}
  else if(enemy(p,target)){if(DOT.size()<256||DOT.containsKey(target)){var previous=DOT.get(target);DOT.put(target,new Dot(p,kind,now+(kind==Kind.FIRE?80:100),previous==null?now+20:previous.next()));}if(kind==Kind.FIRE)target.igniteForSeconds(4);l.sendParticles(kind==Kind.FIRE?ParticleTypes.FLAME:ParticleTypes.WITCH,center.x,center.y,center.z,12,.3,.45,.3,.02);l.playSound(null,target.blockPosition(),kind==Kind.FIRE?SoundEvents.FIRECHARGE_USE:SoundEvents.SLIME_SQUISH,SoundSource.PLAYERS,.35f,1.3f);}
 }
 static void particle(ServerLevel l,Vec3 at,int color,float size){l.sendParticles(new DustParticleOptions(color,size),at.x,at.y,at.z,1,0,0,0,0);}
 static void tick(net.minecraft.server.MinecraftServer server){STUN.entrySet().removeIf(e->!e.getKey().isAlive()||e.getKey().isRemoved()||e.getKey().level().getGameTime()>=e.getValue());RESIST.entrySet().removeIf(e->!e.getKey().isAlive()||e.getKey().isRemoved()||e.getKey().level().getGameTime()>=e.getValue());
  for(var entry:new ArrayList<>(DOT.entrySet())){var e=entry.getKey();var d=entry.getValue();long now=e.level().getGameTime();if(!e.isAlive()||e.isRemoved()||!d.owner().isAlive()||d.owner().hasDisconnected()||!enemy(d.owner(),e)||now>d.end()){DOT.remove(e);continue;}if(now>=d.next()){e.hurtServer((ServerLevel)e.level(),d.owner().damageSources().playerAttack(d.owner()),2f);DOT.put(e,new Dot(d.owner(),d.kind(),d.end(),now+20));}if(now%10==0)particle((ServerLevel)e.level(),e.position().add(0,e.getBbHeight()*.6,0),d.kind()==Kind.FIRE?0xff702c:0x83ed49,.9f);}
 }
 public static void init(){ServerTickEvents.END_SERVER_TICK.register(MeleeElements::tick);ServerPlayConnectionEvents.DISCONNECT.register((h,s)->NEXT.remove(h.player.getUUID()));ServerLifecycleEvents.SERVER_STOPPED.register(s->{NEXT.clear();STUN.clear();RESIST.clear();DOT.clear();});}
}
