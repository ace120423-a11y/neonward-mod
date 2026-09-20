package jp.neonward;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.level.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.*;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.ItemStack;

/** Dedicated entity types with server-authoritative attacks and no terrain damage. */
public class CyberEnemy extends Zombie implements RangedAttackMob {
 @Override public boolean hurtServer(net.minecraft.server.level.ServerLevel l,net.minecraft.world.damagesource.DamageSource source,float amount){float before=getHealth();boolean result=super.hurtServer(l,source,amount);float lost=Math.max(0,before-getHealth());if(source.getEntity() instanceof net.minecraft.server.level.ServerPlayer&&lost>0)CombatFeedback.hit(this,lost);return result;}

 private Vec3 aim;private int charge;private LivingEntity aimedTarget;private int ability;
 public HostileRoster.Kind kind(){return NeonHostiles.KINDS.get(getType());}
 public CyberEnemy(EntityType<? extends Zombie> type,Level level){
  super(type,level);setCanPickUpLoot(false);setCanBreakDoors(false);
  var weapon=NeonArsenal.ITEMS.get(kind().weapon());if(weapon!=null)setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(weapon));
  for(var s:EquipmentSlot.values())setDropChance(s,0);xpReward=kind().id().equals("iron_colossus")?40:8;
 }
 @Override protected void registerGoals(){
  goalSelector.addGoal(0,new FloatGoal(this));
  var k=kind();
  if(k.interval()>0)goalSelector.addGoal(2,new RangedAttackGoal(this,1,k.interval(),k.range()));
  else goalSelector.addGoal(2,new MeleeAttackGoal(this,1.1,false));
  goalSelector.addGoal(5,new WaterAvoidingRandomStrollGoal(this,.7));
  goalSelector.addGoal(6,new LookAtPlayerGoal(this,Player.class,12));goalSelector.addGoal(7,new RandomLookAroundGoal(this));
  targetSelector.addGoal(1,new NearestAttackableTargetGoal<>(this,Player.class,true,(e,l)->!CityProtection.contains(e.level(),e.blockPosition())));
 }
 @Override protected boolean isSunSensitive(){return false;}
 @Override protected boolean convertsInWater(){return false;}
 @Override public void setBaby(boolean baby){super.setBaby(false);}
 @Override public boolean canAttack(LivingEntity target){return (!Underworld.hunter(this)||entityTags().contains("nw_target_"+target.getStringUUID()))&&target instanceof Player&&!CityProtection.contains(target.level(),target.blockPosition())&&!NeonZones.safeOutpost(target.level(),target.blockPosition())&&super.canAttack(target);}
 @Override protected void populateDefaultEquipmentSlots(net.minecraft.util.RandomSource r,net.minecraft.world.DifficultyInstance d){}
 @Override public void performRangedAttack(LivingEntity target,float distance){
  if(charge>0||!canAttack(target))return;
  aim=target.getEyePosition();aimedTarget=target;charge=kind().id().equals("ghost_sniper")?26:kind().id().equals("belt_gunner")?4:10;
 }
 private void beam(ServerLevel l,Vec3 from,Vec3 to,int color,double spacing){
  var delta=to.subtract(from);int n=Math.min(100,(int)(delta.length()/spacing)+1);
  for(int i=0;i<=n;i++){var p=from.add(delta.scale((double)i/n));l.sendParticles(new DustParticleOptions(color,.65f),p.x,p.y,p.z,1,0,0,0,0);}
 }
 @Override public void tick(){
  super.tick();if(!(level() instanceof ServerLevel l)||!isAlive())return;
  if(CityProtection.contains(l,blockPosition())||NeonZones.safeOutpost(l,blockPosition())){discard();return;}
  var target=getTarget();setAggressive(target!=null);if(target!=null&&!canAttack(target)){setTarget(null);getNavigation().stop();charge=0;aim=null;aimedTarget=null;}
  if(charge>0){
   if(aimedTarget==null||!aimedTarget.isAlive()||!canAttack(aimedTarget)){charge=0;aim=null;}
   else {
    var from=getEyePosition();var end=l.clip(new ClipContext(from,aim,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this)).getLocation();
    if(charge%4==0)beam(l,from,end,kind().color(),1.4);
    if(--charge==0){
     beam(l,from,end,kind().color(),.4);playSound(net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_BLAST,.5f,1.5f);
     if(aimedTarget.getBoundingBox().inflate(.12).clip(from,end).isPresent()){
      aimedTarget.hurtServer(l,damageSources().mobAttack(this),(float)getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE));
      if(kind().id().equals("arc_trooper")||kind().id().equals("hex_netrunner"))aimedTarget.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,kind().id().equals("hex_netrunner")?60:25,0),this);
     }
     aim=null;aimedTarget=null;
    }
   }
  }
  if(++ability>=100){
   ability=0;
   if(kind().id().equals("patch_medic")){
    CyberEnemy injured=null;
    for(var ally:l.getEntitiesOfClass(CyberEnemy.class,getBoundingBox().inflate(10)))if(ally!=this&&ally.isAlive()&&ally.getHealth()<ally.getMaxHealth()&&getSensing().hasLineOfSight(ally)&&(injured==null||ally.getHealth()/ally.getMaxHealth()<injured.getHealth()/injured.getMaxHealth()))injured=ally;
    if(injured!=null){injured.heal(4);beam(l,getEyePosition(),injured.getEyePosition(),0x65ff94,.35);}
   }
   if(kind().id().equals("neon_runner")&&target!=null&&canAttack(target)&&distanceToSqr(target)<100&&getSensing().hasLineOfSight(target)){var d=target.position().subtract(position()).normalize();setDeltaMovement(d.x*.65,.15,d.z*.65);}
  }
  if(kind().id().equals("iron_colossus")&&target!=null&&canAttack(target)){
   if(ability>=75&&ability%5==0){for(int i=0;i<24;i++){double a=i*Math.PI/12;l.sendParticles(new DustParticleOptions(0xff4242,1),getX()+Math.cos(a)*4,getY()+.2,getZ()+Math.sin(a)*4,1,0,0,0,0);}}
   if(ability==99)for(var p:l.getEntitiesOfClass(Player.class,getBoundingBox().inflate(4)))if(canAttack(p)&&distanceToSqr(p)<=16&&getSensing().hasLineOfSight(p)){p.hurtServer(l,damageSources().mobAttack(this),8);p.knockback(.8,getX()-p.getX(),getZ()-p.getZ(),damageSources().mobAttack(this),8);}
  }
 }
 @Override public boolean doHurtTarget(ServerLevel l,Entity target){boolean hit=super.doHurtTarget(l,target);if(hit&&kind().id().equals("pile_breaker")&&target instanceof LivingEntity living)living.knockback(.9,getX()-target.getX(),getZ()-target.getZ(),damageSources().mobAttack(this),(float)getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE));return hit;}
}
