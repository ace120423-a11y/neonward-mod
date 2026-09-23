package jp.neonward;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.pathfinder.PathType;

/** A passive companion, not a wolf/cat predator or farm animal. Exactly one native entity. */
public final class PetEntity extends PathfinderMob {
 private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> FLYING=net.minecraft.network.syncher.SynchedEntityData.defineId(PetEntity.class,net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
 final PetFlight flight=new PetFlight();
 UUID owner;
 private int stuckTicks;
 private Vec3 lastProgress=Vec3.ZERO;
 private Vec3 idleTarget;
 private int idleWait=60;
 public PetEntity(EntityType<? extends PetEntity> type,Level level){
  super(type,level);setPersistenceRequired();setInvulnerable(true);setCanPickUpLoot(false);xpReward=0;
  for(var path:PathType.values())if(path.getMalus()>0)setPathfindingMalus(path,-1);
  setPathfindingMalus(PathType.WATER,-1);setPathfindingMalus(PathType.WATER_BORDER,-1);
 }
 public int kind(){return PetCompanions.TYPES.indexOf(getType());}
 @Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(FLYING,false);}
 public boolean flying(){return entityData.get(FLYING);}
 void flying(boolean value){entityData.set(FLYING,value);}
 @Override public void travel(Vec3 input){if(flying()){setDeltaMovement(Vec3.ZERO);return;}super.travel(input);}
 @Override protected void registerGoals(){}
 @Override public boolean shouldBeSaved(){return false;}
 @Override public boolean canAttack(LivingEntity target){return false;}
 @Override public boolean doHurtTarget(ServerLevel level,Entity target){return false;}
 @Override public boolean hurtServer(ServerLevel level,DamageSource source,float amount){return false;}
 @Override public boolean isAttackable(){return false;}
 @Override public boolean isPushable(){return false;}
 @Override public void push(Entity other){}
 @Override protected void pushEntities(){}
 @Override public boolean canInteractWithLevel(){return false;}
 @Override public boolean isIgnoringBlockTriggers(){return true;}
 @Override public boolean isSteppingCarefully(){return true;}
 @Override public boolean canBeLeashed(){return false;}
 @Override protected boolean canDispenserEquipIntoSlot(EquipmentSlot slot){return false;}
 @Override public boolean canUsePortal(boolean passenger){return false;}
 @Override protected void checkFallDamage(double dy,boolean ground,BlockState state,BlockPos pos){resetFallDistance();}
 @Override protected MovementEmission getMovementEmission(){return MovementEmission.NONE;}
 @Override public InteractionResult interact(Player player,InteractionHand hand,Vec3 location){return InteractionResult.PASS;}
 @Override public void tick(){
  super.tick();if(!(level() instanceof ServerLevel level))return;
  var player=owner==null?null:level.getServer().getPlayerList().getPlayer(owner);
  if(!PetCompanions.active(this)||player==null||!player.isAlive()||player.isSpectator()||player.level()!=level){discard();return;}
  if(kind()==3){
   if(flight.active()){flight.tick(player,this);return;}
   if(distanceToSqr(player)<144&&flight.begin(player,this)){flight.tick(player,this);return;}
  }
  if(tickCount%10!=0)return;
  double distance=distanceToSqr(player);
  if(distance>144||getY()<level.getMinY()+2||isInWater()||isInLava()||stuckTicks>=80){
   var spot=PetSpawn.find(player,this);if(spot==null){discard();return;}
   getNavigation().stop();idleTarget=null;teleportTo(spot.x,spot.y,spot.z);setDeltaMovement(Vec3.ZERO);stuckTicks=0;lastProgress=spot;return;
  }
  if(kind()!=3&&wander(player,level,distance))return;
  if(distance>6.25){
   getNavigation().moveTo(player,kind()==2?.8:1.05);
   stuckTicks=position().distanceToSqr(lastProgress)<.025?stuckTicks+10:0;lastProgress=position();
  }else{getNavigation().stop();stuckTicks=0;lastProgress=position();}
  getLookControl().setLookAt(player,30,30);
 }
 /** Keep an idle path until arrival; do not cancel it at the close-follow radius. */
 private boolean wander(net.minecraft.server.level.ServerPlayer player,ServerLevel level,double distance){
  if(idleTarget!=null){
   if(player.distanceToSqr(idleTarget)>16||!PetSpawn.safe(level,this,idleTarget))getNavigation().stop();
   else if(!getNavigation().isDone()&&position().distanceToSqr(idleTarget)>.3)return true;
   idleTarget=null;idleWait=60+getRandom().nextInt(101);
  }
  idleWait-=10;if(distance>6.25||idleWait>0)return false;
  idleWait=60+getRandom().nextInt(101);
  for(int attempt=0;attempt<12;attempt++){
   Vec3 target=Vec3.atBottomCenterOf(player.blockPosition().offset(getRandom().nextInt(7)-3,0,getRandom().nextInt(7)-3));
   if(player.distanceToSqr(target)>16||position().distanceToSqr(target)<1||!PetSpawn.safe(level,this,target))continue;
   if(getNavigation().moveTo(target.x,target.y,target.z,kind()==2?.6:.75)){idleTarget=target;return true;}
  }
  return false;
 }
}
