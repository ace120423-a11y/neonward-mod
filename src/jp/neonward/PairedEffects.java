package jp.neonward;
import java.util.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.particles.*;
import net.minecraft.sounds.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/** Enemy-only, bounded effects. Explosion visuals never invoke world.explode(). */
public final class PairedEffects {
 static final ResourceKey<DamageType> BLEED_TYPE=ResourceKey.create(Registries.DAMAGE_TYPE,NeonWard.id("paired_bleed"));
 record Bleed(ServerPlayer owner,long end,long next){}
 static final Map<LivingEntity,Bleed> BLEEDS=new IdentityHashMap<>();
 static final int LIMIT=128,DURATION=100,INTERVAL=20;
 static final float DAMAGE=2f;
 static void init(){
  ServerTickEvents.END_SERVER_TICK.register(s->tick());
  ServerLifecycleEvents.SERVER_STOPPED.register(s->BLEEDS.clear());
  ServerPlayConnectionEvents.DISCONNECT.register((h,s)->BLEEDS.entrySet().removeIf(e->e.getValue().owner()==h.player));
 }
 static void bleed(ServerPlayer p,LivingEntity target){
  if(!ArsenalExpansion.enemy(p,target))return;
  var previous=BLEEDS.get(target);if(previous==null&&BLEEDS.size()>=LIMIT)return;
  long now=p.level().getGameTime();
  // Refresh duration, never stack damage or postpone the next scheduled tick.
  BLEEDS.put(target,new Bleed(p,now+DURATION,previous==null?now+INTERVAL:previous.next()));
  var at=target.position().add(0,target.getBbHeight()*.6,0);
  ElementVfx.emit(p,target,ElementVfx.BLEED,6,false);
  ElementVfx.emit(p,target,ElementVfx.BLEED,DURATION,true);
 }
 static DamageSource bleedSource(ServerPlayer p){return new DamageSource(p.level().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(BLEED_TYPE),p);}
 static void tick(){
  for(var entry:new ArrayList<>(BLEEDS.entrySet())){
   var target=entry.getKey();var dot=entry.getValue();var p=dot.owner();long now=target.level().getGameTime();
   if(!p.isAlive()||p.hasDisconnected()||target.isRemoved()||!ArsenalExpansion.enemy(p,target)||p.distanceToSqr(target)>64*64||now>dot.end()){BLEEDS.remove(target);continue;}
   if(now>=dot.next()){
    target.hurtServer(p.level(),bleedSource(p),DAMAGE);
    if(now>=dot.end()||!target.isAlive())BLEEDS.remove(target);else BLEEDS.put(target,new Bleed(p,dot.end(),now+INTERVAL));
   }
  }
 }
 static void blast(ServerPlayer p,LivingEntity primary,float splash,double knockback){
  if(!(primary instanceof Enemy)||primary.level()!=p.level())return;
  var l=p.level();var at=primary.position().add(0,Math.max(.35,primary.getBbHeight()*.5),0);
  ElementVfx.emit(p,primary,ElementVfx.BLAST,12,false);
  l.playSound(null,primary.blockPosition(),SoundEvents.GENERIC_EXPLODE.value(),SoundSource.PLAYERS,.55f,1.2f);
  if(primary.isAlive())ArsenalExpansion.push(p,primary,knockback);
  var targets=l.getEntitiesOfClass(LivingEntity.class,new AABB(at,at).inflate(2.5),e->e!=primary&&ArsenalExpansion.enemy(p,e)&&e.position().add(0,e.getBbHeight()*.5,0).distanceToSqr(at)<=6.25);
  targets.sort(Comparator.comparingDouble(primary::distanceToSqr));
  int hit=0;
  for(var e:targets){
   if(hit>=8)break;
   if(l.clip(new ClipContext(at,e.getEyePosition(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p)).getType()!=HitResult.Type.MISS)continue;
   if(ArsenalExpansion.hurt(p,e,splash)){ArsenalExpansion.push(p,e,knockback);hit++;}
  }
 }
}
