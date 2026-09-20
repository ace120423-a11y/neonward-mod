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
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.*;

/** Server-owned, telegraphed arena combat. No block destruction and no unmarked contact damage. */
public class SpireBoss extends CyberEnemy {
 static final EntityDataAccessor<Integer> CAST=SynchedEntityData.defineId(SpireBoss.class,EntityDataSerializers.INT);
 static final EntityDataAccessor<Integer> PHASE=SynchedEntityData.defineId(SpireBoss.class,EntityDataSerializers.INT);
 final List<BossHazard> hazards=new ArrayList<>();
 private final Map<UUID,Integer> lastHit=new HashMap<>();
 private ServerBossEvent bar;
 int combatTicks,cycle,casts;boolean engaged;Vec3 dashFrom,dashTo;int dashAt;
 public SpireBoss(EntityType<? extends Zombie> type,Level level){super(type,level);setPersistenceRequired();xpReward=70+spec().floor()*4;}
 public BossRoster.Kind spec(){return SpireBosses.KINDS.get(getType());}
 @Override public HostileRoster.Kind kind(){return spec().legacy();}
 @Override protected void registerGoals(){} // Movement and targeting belong to the arena controller.
 @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(CAST,0);b.define(PHASE,0);}
 public int castVisual(){return entityData.get(CAST);}
 public int phase(){return entityData.get(PHASE);}
 @Override public boolean causeFallDamage(double d,float multiplier,DamageSource source){return false;}
 @Override public boolean canAttack(LivingEntity p){return p instanceof Player player&&!player.isCreative()&&!player.isSpectator()&&p.isAlive()&&p.level()==level()&&p.getX()>=SpireSite.X-63&&p.getX()<=SpireSite.X+51&&p.getZ()>=SpireSite.Z+24&&p.getZ()<=SpireSite.Z+51&&DungeonLayout.floor(p.getY())==spec().floor()&&super.canAttack(p);}
 static final String[] VOICES={"block.note_block.bit","block.note_block.bell","block.note_block.chime","block.note_block.xylophone","block.note_block.iron_xylophone","block.note_block.pling","block.note_block.basedrum","block.note_block.bass","block.note_block.hat","block.note_block.flute"};
 net.minecraft.sounds.SoundEvent voice(){return net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(VOICES[(spec().floor()-1)%VOICES.length]));}
 @Override protected net.minecraft.sounds.SoundEvent getAmbientSound(){return null;}
 @Override protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source){return voice();}
 @Override protected net.minecraft.sounds.SoundEvent getDeathSound(){return net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value();}
 @Override public float getVoicePitch(){return .65f+(spec().floor()-1)/10*.35f;}
 @Override public void tick(){
  super.tick();if(!(level() instanceof ServerLevel l)||!isAlive()||isRemoved())return;
  int f=spec().floor(),base=DungeonLayout.base(f)+1;var max=getAttribute(Attributes.MAX_HEALTH);if(max!=null&&max.getBaseValue()<spec().hp()*3){max.setBaseValue(spec().hp()*3);setHealth(getMaxHealth());}
  // Bosses stay in their own chamber; players may retreat into the maze safely.
  if(l.dimension()!=NeonZones.TOWER||!entityTags().contains("nw_spire_boss"))return;
  if(getY()<base-.5||getY()>base+4||getX()<SpireSite.X-63||getX()>SpireSite.X+51||getZ()<SpireSite.Z+24||getZ()>SpireSite.Z+51)setPos(SpireSite.X+32.5,base,SpireSite.Z+32.5);
  if(bar==null)bar=new ServerBossEvent(getUUID(),Component.literal(spec().name()),BossEvent.BossBarColor.PURPLE,BossEvent.BossBarOverlay.PROGRESS);
  var audience=l.players().stream().filter(p->SpireSite.contains(l,p.blockPosition())&&!p.isSpectator()&&p.isAlive()&&DungeonLayout.floor(p.getY())==f&&p.distanceToSqr(this)<24*24).toList();
  for(var p:new ArrayList<>(bar.getPlayers()))if(!audience.contains(p))bar.removePlayer(p);
  for(var p:audience)bar.addPlayer(p);
  int phase=getHealth()<=getMaxHealth()*.5?1:0;entityData.set(PHASE,phase);bar.setProgress(getHealth()/getMaxHealth());
  var target=l.players().stream().filter(p->canAttack(p)&&p.getX()>=SpireSite.X-63&&p.getX()<=SpireSite.X+51&&p.getZ()>=SpireSite.Z+24&&p.getZ()<=SpireSite.Z+51&&getSensing().hasLineOfSight(p)).min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
  if(target==null){if(engaged){hazards.clear();dashTo=null;cycle=0;entityData.set(CAST,0);}engaged=false;setTarget(null);setDeltaMovement(0,getDeltaMovement().y,0);bar.setName(Component.literal(f+"F  "+spec().name()+" / "+spec().form()));return;}
  if(!engaged){engaged=true;cycle=55;target.sendSystemMessage(Component.literal(spec().name()+"："+spec().hint()).withColor(spec().color()));}
  setTarget(target);combatTicks++;
  var dir=target.position().subtract(position());float yaw=(float)(Math.toDegrees(Math.atan2(dir.z,dir.x))-90);setYRot(yaw);setYHeadRot(yaw);setYBodyRot(yaw);
  boolean casting=!hazards.isEmpty();entityData.set(CAST,casting?1:0);
  bar.setName(Component.literal(f+"F  "+spec().name()+" / "+(phase==1?"OVERDRIVE / ":"")+(casting?spec().attack():"冷却中")));
  if(--cycle<=0){planAttack(target.position());casts++;cycle=phase==1?165:185;}
  tickHazards(l);
  if(dashTo!=null&&combatTicks>=dashAt){move(MoverType.SELF,dashTo.subtract(position()).multiply(1,0,1).scale(.24));if(combatTicks>dashAt+14)dashTo=null;}
  else if(!casting&&cycle>35)moveAround(target);
  if(tickCount%100==0)lastHit.entrySet().removeIf(e->combatTicks-e.getValue()>100);
 }
 void moveAround(Player p){
  Vec3 toward=p.position().subtract(position()).multiply(1,0,1);double d=toward.length();if(d<.01)return;var unit=toward.scale(1/d);double speed=.075+spec().floor()*.0015;var motion=spec().motion();Vec3 delta=Vec3.ZERO;
  switch(motion){
   case "stalk" -> delta=unit.scale(d>3?speed:-speed*.5);
   case "rush" -> delta=unit.scale(d>4?speed*1.35:-speed*.7);
   case "orbit","float" -> delta=new Vec3(-unit.z,0,unit.x).scale(speed).add(unit.scale((d-4)*.01));
   case "strafe" -> delta=new Vec3(-unit.z,0,unit.x).scale(Math.sin(combatTicks*.018)*speed*1.4);
   case "slither" -> delta=unit.scale(speed*.7).add(new Vec3(-unit.z,0,unit.x).scale(Math.sin(combatTicks*.07)*speed));
   case "patrol" -> delta=new Vec3(Math.cos(combatTicks*.025)*speed,0,Math.sin(combatTicks*.025)*speed);
  }
  double pad=spec().width()/2+.15;
  if(getX()+delta.x<SpireSite.X-63+pad||getX()+delta.x>SpireSite.X+51-pad)delta=new Vec3(0,0,delta.z);
  if(getZ()+delta.z<SpireSite.Z+24+pad||getZ()+delta.z>SpireSite.Z+51-pad)delta=new Vec3(delta.x,0,0);
  move(MoverType.SELF,delta);
 }
 Vec3 floor(Vec3 p){return new Vec3(p.x,DungeonLayout.base(spec().floor())+1.1,p.z);}
 double localX(double x){return 32.5+(x-SpireSite.X-32.5)/2.4;}
 double localZ(double z){return 32.5+(z-SpireSite.Z-32.5)/2.4;}
 Vec3 point(double x,double z){return floor(new Vec3(SpireSite.X+32.5+(x-32.5)*2.4,0,SpireSite.Z+32.5+(z-32.5)*2.4));}
 void disc(Vec3 p,double r,int delay,float power){hazards.add(new BossHazard(0,floor(p),Vec3.ZERO,r*6,0,combatTicks+delay,6,power,false,0));}
 void ring(Vec3 p,double r,int delay){hazards.add(new BossHazard(1,floor(p),Vec3.ZERO,r*6,0,combatTicks+delay,6,1,true,0));}
 void line(Vec3 a,Vec3 b,double r,int delay){hazards.add(new BossHazard(2,floor(a),floor(b),r*4.5,0,combatTicks+delay,7,1,false,0));}
 void cone(Vec3 a,double angle,double half,double radius,int delay){hazards.add(new BossHazard(3,floor(a),new Vec3(half,0,0),radius*6,angle,combatTicks+delay,7,1.1f,false,0));}
 Vec3 radial(Vec3 center,double angle,double distance){return center.add(Math.cos(angle)*distance,0,Math.sin(angle)*distance);}
 void charge(Vec3 a,Vec3 target,int delay){Vec3 end=target;double pad=spec().width()/2+.2;end=new Vec3(Math.max(SpireSite.X-63+pad,Math.min(SpireSite.X+51-pad,end.x)),getY(),Math.max(SpireSite.Z+24+pad,Math.min(SpireSite.Z+51-pad,end.z)));line(a,end,.8,delay);dashFrom=a;dashTo=end;dashAt=combatTicks+delay;}
 /** The thirty cases are intentionally distinct encounter programs, not random recolors. */
 void planAttack(Vec3 target){
  playSound(voice(),.8f,getVoicePitch());
  hazards.clear();var o=floor(position());var t=floor(target);double a=Math.atan2(t.z-o.z,t.x-o.x);int n=spec().floor();int w=32;var center=point(32.5,32.5);
  switch(n){
   case 1 -> {cone(o,a-.6,.4,4,w);cone(o,a+.6,.4,4,w);disc(radial(o,a,2),1.3,w+22,1.25f);}
   case 2 -> {line(point(28,localZ(t.z)),point(37,localZ(t.z)),.35,w);line(point(localX(t.x),28),point(localX(t.x),37),.35,w);}
   case 3 -> {disc(t,1.2,w,1);line(o,radial(o,a,8),.3,w+24);}
   case 4 -> {charge(o,t,w);line(radial(t,a+1.3,3),t,.55,w+26);line(radial(t,a-1.3,3),t,.55,w+52);}
   case 5 -> {for(int i=0;i<5;i++)disc(i==0?t:radial(t,i*Math.PI/2,2.2),1,w+i*9,1.1f);}
   case 6 -> {for(int i=1;i<=5;i++)ring(o,i*.9,w+i*8);}
   case 7 -> {line(point(localX(t.x)-3,localZ(t.z)-3),point(localX(t.x)+3,localZ(t.z)+3),.35,w);line(point(localX(t.x)-3,localZ(t.z)+3),point(localX(t.x)+3,localZ(t.z)-3),.35,w+20);cone(o,a,.55,5,w+42);}
   case 8 -> {for(int i=0;i<9;i++)disc(point(28.5+i,32.5+Math.sin(i*.9+casts)*2.6),.7,w+i*6,1);}
   case 9 -> {charge(o,t,w);ring(t,2.2,w+30);}
   case 10 -> {var p1=radial(t,0,2.7);var p2=radial(t,2.094,2.7);var p3=radial(t,4.188,2.7);line(p1,p2,.3,w);line(p2,p3,.3,w+15);line(p3,p1,.3,w+30);line(o,t,.23,w+45);}
   case 11 -> {disc(o,2.2,w,1.2f);charge(o,t,w+35);}
   case 12 -> {cone(o,a+Math.PI/2,.65,6,w);cone(o,a-Math.PI/2,.65,6,w);ring(o,2.5,w+30);}
   case 13 -> {for(int i=0;i<9;i++)disc(radial(center,i*.8+casts,4-i*.32),.75,w+i*6,1);}
   case 14 -> {for(int i=0;i<8;i++)line(o,radial(o,i*Math.PI/4+casts*.15,7),.24,w+(i%2)*18);}
   case 15 -> {for(int i=0;i<3;i++)line(point(29+i*3,28),point(29+i*3,37),.65,w+i*20);disc(o,2,w+65,1.4f);}
   case 16 -> {line(point(localX(t.x)-2,28),point(localX(t.x)-2,37),.35,w);line(point(localX(t.x)+2,28),point(localX(t.x)+2,37),.35,w);line(point(localX(t.x),28),point(localX(t.x),37),.45,w+30);}
   case 17 -> {for(int i=0;i<3;i++){line(point(29+i*3,28),point(29+i*3,37),.25,w);line(point(28,29+i*3),point(37,29+i*3),.25,w+24);}}
   case 18 -> {disc(t,1.45,w,1);charge(o,t,w+35);disc(radial(t,a,2),1.25,w+55,1.1f);}
   case 19 -> {line(point(30,28),point(30,37),.6,w);line(point(35,28),point(35,37),.6,w+18);line(point(28,localZ(t.z)),point(37,localZ(t.z)),.4,w+48);}
   case 20 -> {for(int i=0;i<8;i++)line(center,radial(center,casts*.4+i*Math.PI/4,6),.23,w+i*7);}
   case 21 -> {disc(t,1.55,w,1.2f);for(int i=-1;i<=1;i++)line(o,radial(o,a+i*.4,8),.25,w+25);}
   case 22 -> {for(int i=0;i<6;i++)disc(radial(o,i*Math.PI/3+casts*.3,3),1.1,w,1);disc(o,1.25,w+35,1.2f);}
   case 23 -> {for(int i=0;i<8;i++)disc(radial(o,a,i),.75,w+i*7,1);charge(o,t,w+60);}
   case 24 -> {ring(o,2,w);ring(o,3.5,w+25);ring(o,5,w+50);}
   case 25 -> {cone(o,a,1,5,w);line(point(localX(t.x)-3,localZ(t.z)),point(localX(t.x)+3,localZ(t.z)),.35,w+27);line(point(localX(t.x),localZ(t.z)-3),point(localX(t.x),localZ(t.z)+3),.35,w+47);}
   case 26 -> {for(int i=0;i<10;i++)disc(radial(center,i*Math.PI/5,4),1.05,w,1);disc(center,2.1,w+42,1.2f);}
   case 27 -> {line(point(29,28),point(29,37),.65,w);line(point(36,28),point(36,37),.65,w);line(point(32.5,28),point(32.5,37),.55,w+32);}
   case 28 -> {for(int i=-1;i<=1;i++)line(o.add(i*.8,0,0),radial(o,a+i*.23,8),.3,w+(i+1)*22);disc(t,1,w+65,1);}
   case 29 -> {for(int i=0;i<6;i++)line(o,radial(o,i*Math.PI/3+casts*.3,7),.23,w);disc(t,1.1,w+26,1);ring(o,3.2,w+50);}
   case 30 -> {cone(o,a,.65,6,w);for(int i=0;i<4;i++)disc(radial(t,i*Math.PI/2,1.8),.9,w+25+i*8,1.1f);ring(o,3.5,w+72);}
  }
  // The second phase repeats this boss's own opening hazard at a new angle/location.
  if(phase()==1&&!hazards.isEmpty()){
   var h=hazards.getFirst();var rotatedA=rotate(h.a(),center,Math.PI/2);var rotatedB=h.shape()==BossHazard.CONE?h.b():rotate(h.b(),center,Math.PI/2);
   hazards.add(new BossHazard(h.shape(),rotatedA,rotatedB,h.radius(),h.angle()+Math.PI/2,combatTicks+125,h.live(),h.power(),h.jump(),h.effect()));
  }
 }
 static Vec3 rotate(Vec3 p,Vec3 center,double a){double x=p.x-center.x,z=p.z-center.z;return new Vec3(center.x+x*Math.cos(a)-z*Math.sin(a),p.y,center.z+x*Math.sin(a)+z*Math.cos(a));}
 void tickHazards(ServerLevel l){
  for(var h:hazards){int remaining=h.start()-combatTicks;if(remaining>0&&combatTicks%5==0)draw(l,h,0xffb34c);if(remaining<=0&&remaining>-h.live()){
    if(combatTicks%3==0)draw(l,h,spec().color());
    for(var p:l.players())if(canAttack(p)&&Math.abs(p.getY()-h.a().y)<4&&combatTicks-lastHit.getOrDefault(p.getUUID(),-100)>11&&h.contains(p.getX(),p.getZ(),p.getBbWidth()*.35)&&(!h.jump()||p.getY()<h.a().y+.7)&&getSensing().hasLineOfSight(p)){
     float damage=(float)getAttributeValue(Attributes.ATTACK_DAMAGE)*h.power();if(p.hurtServer(l,damageSources().mobAttack(this),damage)){
      lastHit.put(p.getUUID(),combatTicks);
      if(spec().floor()==18||spec().floor()==8)p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,35,0),this);
      if(spec().floor()==22)heal(damage*.35f);
     }
    }
  }}
  hazards.removeIf(h->combatTicks>=h.start()+h.live());
 }
 void dot(ServerLevel l,Vec3 p,int color){if(p.x<SpireSite.X-63||p.x>SpireSite.X+51||p.z<SpireSite.Z+24||p.z>SpireSite.Z+51)return;l.sendParticles(new DustParticleOptions(color,.7f),p.x,p.y,p.z,1,0,0,0,0);}
 void segment(ServerLevel l,Vec3 a,Vec3 b,int color){int count=Math.max(1,Math.min(26,(int)(a.distanceTo(b)*2)));for(int i=0;i<=count;i++)dot(l,a.lerp(b,(double)i/count),color);}
 void draw(ServerLevel l,BossHazard h,int color){
  if(h.shape()==BossHazard.LINE){var v=h.b().subtract(h.a()).multiply(1,0,1).normalize();var side=new Vec3(-v.z,0,v.x).scale(h.radius());segment(l,h.a().add(side),h.b().add(side),color);segment(l,h.a().subtract(side),h.b().subtract(side),color);}
  else if(h.shape()==BossHazard.CONE){double half=h.b().x;segment(l,h.a(),radial(h.a(),h.angle()-half,h.radius()),color);segment(l,h.a(),radial(h.a(),h.angle()+half,h.radius()),color);for(int i=0;i<=20;i++)dot(l,radial(h.a(),h.angle()-half+half*2*i/20,h.radius()),color);}
  else{for(int i=0;i<28;i++)dot(l,radial(h.a(),i*Math.PI/14,h.radius()),color);if(h.shape()==BossHazard.DISC){segment(l,h.a().add(-h.radius()*.4,0,0),h.a().add(h.radius()*.4,0,0),color);segment(l,h.a().add(0,0,-h.radius()*.4),h.a().add(0,0,h.radius()*.4),color);}}
 }
 @Override public void remove(Entity.RemovalReason reason){if(bar!=null)bar.removeAllPlayers();hazards.clear();super.remove(reason);}
 @Override public void stopSeenByPlayer(ServerPlayer p){super.stopSeenByPlayer(p);if(bar!=null)bar.removePlayer(p);}
 @Override public void onRemoval(Entity.RemovalReason reason){if(bar!=null)bar.removeAllPlayers();super.onRemoval(reason);}
}
