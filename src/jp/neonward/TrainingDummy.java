package jp.neonward;

import java.util.UUID;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.phys.Vec3;

/** An actual Enemy/LivingEntity for every existing weapon predicate, never a loot source. */
public final class TrainingDummy extends Zombie {
 private Vec3 anchor;
 private ServerPlayer measuring;
 private UUID lastOwner,burnOwner;
 private long lastHit;
 private double measured;
 private net.minecraft.world.damagesource.CombatTracker trainingCombat;
 public TrainingDummy(EntityType<? extends Zombie> type,Level level){
  super(type,level);setNoAi(true);setNoGravity(true);setSilent(true);
  setCanPickUpLoot(false);setCanBreakDoors(false);setPersistenceRequired();xpReward=0;
  for(var slot:EquipmentSlot.values())setDropChance(slot,0);
 }
 void anchor(Vec3 at){anchor=at;setPos(at);setYRot(90);setYHeadRot(90);}
 @Override protected void registerGoals(){}
 @Override protected boolean isSunSensitive(){return false;}
 @Override protected boolean convertsInWater(){return false;}
 @Override public void setBaby(boolean ignored){super.setBaby(false);}
 @Override public boolean shouldBeSaved(){return false;}
 @Override public void checkDespawn(){}
 @Override public boolean isPushable(){return false;}
 @Override public void move(MoverType type,Vec3 movement){}
 @Override public void setDeltaMovement(Vec3 movement){super.setDeltaMovement(Vec3.ZERO);}
 @Override public boolean canAttack(LivingEntity target){return false;}
 @Override public boolean doHurtTarget(ServerLevel level,Entity target){return false;}
 @Override public boolean shouldDropExperience(){return false;}
 @Override protected boolean shouldDropLoot(ServerLevel level){return false;}
 @Override public net.minecraft.world.damagesource.CombatTracker getCombatTracker(){
  // An immortal, continuously attacked mob would otherwise retain an unbounded death
  // message history. Keep vanilla damage calculations, but omit that unused history.
  if(trainingCombat==null)trainingCombat=new net.minecraft.world.damagesource.CombatTracker(this){
   @Override public void recordDamage(DamageSource source,float amount){}
  };
  return trainingCombat;
 }
 // Also intercept explicit kill/death paths; do not invoke Fabric's normal death/loot hooks.
 @Override public void die(DamageSource source){setHealth(getMaxHealth());}
 @Override public void setHealth(float health){
  // actuallyHurt supplies the unclamped health subtraction, including lethal overkill.
  if(measuring!=null&&Float.isFinite(health))measured+=Math.max(0,(double)getHealth()-health);
  super.setHealth(getMaxHealth());
 }
 @Override public boolean hurtServer(ServerLevel level,DamageSource source,float amount){
  if(!Float.isFinite(amount)||amount<=0||!TrainingRange.contains(level,blockPosition()))return false;
  ServerPlayer owner=source.getEntity() instanceof ServerPlayer p?p:null;
  if(owner==null&&source.is(DamageTypeTags.IS_FIRE)&&burnOwner!=null)owner=level.getServer().getPlayerList().getPlayer(burnOwner);
  if(owner==null||!owner.isAlive()||owner.isSpectator()||owner.level()!=level||!TrainingRange.contains(level,owner.blockPosition()))return false;
  measuring=owner;measured=0;
  boolean hit;
  try{hit=super.hurtServer(level,source,amount);}finally{measuring=null;}
  if(hit&&measured>0){lastOwner=owner.getUUID();lastHit=level.getGameTime();TrainingRange.record(owner,measured);}
  return hit;
 }
 @Override public void igniteForTicks(int ticks){
  if(level() instanceof ServerLevel l&&lastOwner!=null&&l.getGameTime()-lastHit<=1)burnOwner=lastOwner;
  super.igniteForTicks(ticks);
 }
 void resetTarget(){
  removeAllEffects();clearFire();burnOwner=lastOwner=null;invulnerableTime=0;hurtTime=0;
  MeleeElements.DOT.remove(this);MeleeElements.STUN.remove(this);MeleeElements.RESIST.remove(this);
  PairedEffects.BLEEDS.remove(this);ArsenalExpansion.ECHOES.removeIf(e->e.target()==this);
  for(var it=ChainPull.ACTIVE.values().iterator();it.hasNext();){var cast=it.next();if(cast.target==this){ChainPull.send(cast,true);it.remove();}}
  setHealth(getMaxHealth());setAbsorptionAmount(0);setDeltaMovement(Vec3.ZERO);
  if(anchor!=null)setPos(anchor);
 }
 @Override public void tick(){
  if(level() instanceof ServerLevel l&&(!TrainingRange.active(l)||!TrainingRange.contains(l,blockPosition()))){resetTarget();discard();return;}
  super.tick();if(anchor!=null)setPos(anchor);setDeltaMovement(Vec3.ZERO);
 }
}
