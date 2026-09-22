package jp.neonward;
import java.util.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.server.level.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.core.particles.*;
import net.minecraft.world.BossEvent;
/** Authoritative attack timeline. Models and visible projectiles follow the same release frame. */
public class VolcanoBoss extends Zombie {
 static final EntityDataAccessor<Integer> ACTION=SynchedEntityData.defineId(VolcanoBoss.class,EntityDataSerializers.INT),AGE=SynchedEntityData.defineId(VolcanoBoss.class,EntityDataSerializers.INT),PHASE=SynchedEntityData.defineId(VolcanoBoss.class,EntityDataSerializers.INT);
 record Shot(Display.BlockDisplay visual,Vec3 from,Vec3 to,int start,int duration,double radius){}
 record Patch(Vec3 at,int end,double radius){}
 static final class ProjectileVisual extends Display.BlockDisplay {ProjectileVisual(ServerLevel l){super(EntityTypes.BLOCK_DISPLAY,l);}@Override public boolean shouldBeSaved(){return false;}}
 final List<Shot> shots=new ArrayList<>();final List<Patch> patches=new ArrayList<>();final Map<UUID,Integer> hits=new HashMap<>();
 ServerBossEvent bar;int cooldown=45,actionAge,action=-1,casts;Vec3 aim=Vec3.ZERO;float attackYaw;
 public VolcanoBoss(EntityType<? extends Zombie> type,Level l){super(type,l);setPersistenceRequired();setCanPickUpLoot(false);setCanBreakDoors(false);xpReward=70+floor()*5;}
 public int floor(){return VolcanoBosses.TYPES.indexOf(getType())+1;}
 @Override protected void registerGoals(){}
 @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(ACTION,0);b.define(AGE,0);b.define(PHASE,0);}
 int action(){return entityData.get(ACTION);}int actionAge(){return entityData.get(AGE);}int phase(){return entityData.get(PHASE);}
 @Override protected boolean isSunSensitive(){return false;}
 @Override protected boolean convertsInWater(){return false;}
 @Override public void setBaby(boolean b){super.setBaby(false);}
 @Override public boolean causeFallDamage(double d,float m,DamageSource s){return false;}
 @Override public void die(DamageSource source){clearAttacks();if(bar!=null)bar.removeAllPlayers();super.die(source);}
 @Override public boolean canAttack(LivingEntity e){return e instanceof Player p&&!p.isCreative()&&!p.isSpectator()&&e.isAlive()&&e.level()==level()&&VolcanicSpire.floor(e)==floor()&&e.getZ()>58&&e.getZ()<103;}
 @Override public boolean hurtServer(ServerLevel l,DamageSource s,float amount){float before=getHealth();boolean ok=super.hurtServer(l,s,amount);if(s.getEntity() instanceof ServerPlayer&&getHealth()<before)CombatFeedback.hit(this,before-getHealth());return ok;}
 void clearAttacks(){for(var s:shots)s.visual().discard();shots.clear();patches.clear();action=-1;entityData.set(ACTION,0);entityData.set(AGE,0);}
 void startAttack(Vec3 target){aim=new Vec3(Math.clamp(target.x,VolcanoLayout.x(floor())+5,VolcanoLayout.x(floor())+59),65,target.z);attackYaw=getYRot();action=VolcanoRoster.MODES[floor()-1];if(casts%3==2)action=floor()==23?1:(floor()+casts)%6;actionAge=0;casts++;entityData.set(ACTION,action+1);entityData.set(AGE,0);playSound(net.minecraft.sounds.SoundEvents.BLAZE_AMBIENT,.8f,.6f+floor()*.012f);}
 @Override public void tick(){super.tick();if(!(level() instanceof ServerLevel l)||!isAlive()||l.dimension()!=VolcanicSpire.DIM)return;
  int f=floor(),ox=VolcanoLayout.x(f);if(getY()<64||getY()>77||getX()<ox+4||getX()>ox+60||getZ()<60||getZ()>101)setPos(ox+32.5,65,83.5);
  if(bar==null)bar=new ServerBossEvent(getUUID(),Component.literal(f+"F / "+VolcanoRoster.NAMES[f-1]),BossEvent.BossBarColor.RED,BossEvent.BossBarOverlay.PROGRESS);
  var audience=l.players().stream().filter(this::canAttack).toList();for(var p:new ArrayList<>(bar.getPlayers()))if(!audience.contains(p))bar.removePlayer(p);for(var p:audience)bar.addPlayer(p);bar.setProgress(getHealth()/getMaxHealth());entityData.set(PHASE,getHealth()<getMaxHealth()*.35?2:getHealth()<getMaxHealth()*.65?1:0);
  var target=audience.stream().filter(this::hasLineOfSight).min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
  if(target==null){clearAttacks();setDeltaMovement(0,getDeltaMovement().y,0);cooldown=40;return;}
  if(action<0){var dir=target.position().subtract(position());float yaw=(float)(Math.toDegrees(Math.atan2(dir.z,dir.x))-90);setYRot(yaw);setYHeadRot(yaw);setYBodyRot(yaw);
   if(--cooldown<=0)startAttack(target.position());else{var delta=dir.multiply(1,0,1).normalize().scale(distanceToSqr(target)>100?.15:distanceToSqr(target)<25?-.1:0);move(MoverType.SELF,delta);}
  }else{
   actionAge++;if(actionAge%2==0)entityData.set(AGE,actionAge);setYRot(attackYaw);setYHeadRot(attackYaw);setYBodyRot(attackYaw);
   if(actionAge<32&&tickCount%5==0){if(action==5||action==3)circle(l,aim,action==5?3.5:2,0xffb44c);else if(action==1)circle(l,position(),4+f*.04,0xffb44c);else ray(l,position().add(0,.15,0),aim.add(0,.15,0),0xffb44c);}
   if(actionAge==32){switch(action){
    case 0 -> slash(l,5+f*.06);
    case 1 -> {burst(l,position(),4+f*.04,1.2f);for(int i=0;i<2+f%3;i++)patch(position().add(Math.cos(i*2.4)*3,0,Math.sin(i*2.4)*3),2);}
    case 3 -> {int count=3+f%3;for(int i=0;i<count;i++){double a=(i-(count-1)/2.0)*.24;Vec3 d=aim.subtract(position());var end=position().add(d.x*Math.cos(a)-d.z*Math.sin(a),0,d.x*Math.sin(a)+d.z*Math.cos(a));launch(l,end,1.8,22+i*2);}}
    case 5 -> launch(l,aim,3.5,34);
   }}
   if(action==2&&actionAge>=32&&actionAge<=64&&actionAge%4==0)breath(l);
   if(action==4&&actionAge>=32&&actionAge<48){var d=aim.subtract(position()).multiply(1,0,1).normalize().scale(.65);move(MoverType.SELF,d);for(var p:audience)if(distanceToSqr(p)<9)hit(l,p,1);if(actionAge%4==0)patch(position(),1);}
   if(actionAge>=76){action=-1;entityData.set(ACTION,0);entityData.set(AGE,0);cooldown=55+f%5*5-phase()*10;}
  }
  tickShots(l);tickPatches(l);if(tickCount%100==0)hits.entrySet().removeIf(e->tickCount-e.getValue()>100);
 }
 void hit(ServerLevel l,ServerPlayer p,float power){if(!canAttack(p)||tickCount-hits.getOrDefault(p.getUUID(),-100)<16)return;if(p.hurtServer(l,damageSources().mobAttack(this),(float)getAttributeValue(Attributes.ATTACK_DAMAGE)*power)){hits.put(p.getUUID(),tickCount);p.igniteForSeconds(4);}}
 void slash(ServerLevel l,double r){for(var p:l.players())if(canAttack(p)&&distanceToSqr(p)<r*r&&hasLineOfSight(p)){var d=p.position().subtract(position()).normalize();var facing=Vec3.directionFromRotation(0,attackYaw);if(d.dot(facing)>.1)hit(l,p,1.1f);}l.sendParticles(ParticleTypes.SWEEP_ATTACK,getX(),getY()+2,getZ(),3,1,.5,1,0);}
 void breath(ServerLevel l){var from=position().add(0,2.3,0);double sweep=Math.sin((actionAge-32)*Math.PI/32)*.45;var d=Vec3.directionFromRotation(0,attackYaw+(float)Math.toDegrees(sweep));double range=12+floor()*.15;var hit=l.clip(new ClipContext(from,from.add(d.scale(range)),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));var end=hit.getLocation();ray(l,from,end,0xff7e28);for(var p:l.players())if(canAttack(p)&&hasLineOfSight(p)){var v=p.getBoundingBox().getCenter().subtract(from);double projection=v.dot(d);if(projection>=0&&projection<from.distanceTo(end)&&v.subtract(d.scale(projection)).length()<1.6)hit(l,p,.65f);}}
 void launch(ServerLevel l,Vec3 dest,double radius,int duration){if(shots.size()>=8)return;var from=position().add(-Math.cos(Math.toRadians(attackYaw))*1.3,3.7,-Math.sin(Math.toRadians(attackYaw))*1.3);var v=new ProjectileVisual(l);String scale=radius>3?"1.2":"0.5";ClockworkFeedback.load(v,l,"{block_state:{Name:'minecraft:magma_block'},transformation:{scale:["+scale+"f,"+scale+"f,"+scale+"f]},brightness:{block:15,sky:15},teleport_duration:1,Invulnerable:1b,Tags:['nw_volcano_projectile']}");v.setPos(from);l.addFreshEntity(v);shots.add(new Shot(v,from,new Vec3(Math.clamp(dest.x,VolcanoLayout.x(floor())+4,VolcanoLayout.x(floor())+60),65,Math.clamp(dest.z,61,101)),tickCount,duration,radius));}
 void tickShots(ServerLevel l){for(var s:new ArrayList<>(shots)){double t=Math.min(1,(tickCount-s.start())/(double)s.duration());var next=s.from().lerp(s.to(),t).add(0,Math.sin(t*Math.PI)*(s.radius()>3?7:2),0);var collision=l.clip(new ClipContext(s.visual().position(),next,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));if(t>=1||collision.getType()!=HitResult.Type.MISS){var at=t>=1?s.to():collision.getLocation();burst(l,at,s.radius(),1);patch(at,s.radius()*.8);s.visual().discard();shots.remove(s);}else{s.visual().setPos(next);if(tickCount%3==0)l.sendParticles(ParticleTypes.FLAME,next.x,next.y,next.z,2,.12,.12,.12,.01);}}}
 void burst(ServerLevel l,Vec3 at,double r,float power){l.sendParticles(ParticleTypes.LAVA,at.x,at.y+.5,at.z,14,.6,.3,.6,.05);playSound(net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(),.7f,.7f);for(var p:l.players())if(canAttack(p)&&p.position().distanceToSqr(at)<r*r&&l.clip(new ClipContext(at.add(0,.5,0),p.getEyePosition(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this)).getType()==HitResult.Type.MISS)hit(l,p,power);}
 void patch(Vec3 at,double r){if(patches.size()>=8)patches.removeFirst();patches.add(new Patch(new Vec3(at.x,65.05,at.z),tickCount+80,r));}
 void tickPatches(ServerLevel l){patches.removeIf(p->p.end()<=tickCount);if(tickCount%8!=0)return;for(var patch:patches){circle(l,patch.at(),patch.radius(),0xff5e13);for(var p:l.players())if(canAttack(p)&&p.getY()<66.5&&p.position().distanceToSqr(patch.at())<patch.radius()*patch.radius())hit(l,p,.25f);}}
 static void circle(ServerLevel l,Vec3 p,double r,int color){for(int i=0;i<24;i++){double a=i*Math.PI/12;l.sendParticles(new DustParticleOptions(color,.9f),p.x+Math.cos(a)*r,65.15,p.z+Math.sin(a)*r,1,0,0,0,0);}}
 static void ray(ServerLevel l,Vec3 a,Vec3 b,int color){int n=Math.min(28,Math.max(2,(int)a.distanceTo(b)*2));for(int i=0;i<=n;i++){var p=a.lerp(b,(double)i/n);l.sendParticles(new DustParticleOptions(color,.85f),p.x,p.y,p.z,1,0,0,0,0);}}
 @Override public void remove(Entity.RemovalReason r){clearAttacks();if(bar!=null)bar.removeAllPlayers();super.remove(r);}
 @Override public void stopSeenByPlayer(ServerPlayer p){super.stopSeenByPlayer(p);if(bar!=null)bar.removePlayer(p);}
}
