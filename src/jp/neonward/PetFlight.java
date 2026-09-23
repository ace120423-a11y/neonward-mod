package jp.neonward;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

/** Short parrot-like flights between verified ground perches. Never loads chunks. */
final class PetFlight {
 private Vec3 start,end;private int step,duration,cooldown;
 boolean active(){return start!=null;}
 static Vec3 arc(Vec3 a,Vec3 b,double t){return a.lerp(b,t).add(0,Math.sin(Math.PI*t)*1.4,0);}
 static boolean clear(ServerLevel level,PetEntity pet,Vec3 from,Vec3 to){
  var box=pet.getDimensions(pet.getPose()).makeBoundingBox(from).expandTowards(to.subtract(from));
  if(!level.getWorldBorder().isWithinBounds(box)||box.minY<=level.getMinY()||box.maxY>=level.getMaxY())return false;
  if(!level.hasChunksAt(BlockPos.containing(box.minX,box.minY,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ)))return false;
  return level.noCollision(pet,box)&&!level.containsAnyLiquid(box)&&level.getEntities(pet,box,e->e.isAlive()).isEmpty();
 }
 boolean begin(ServerPlayer owner,PetEntity pet){
  if(active()||cooldown-- >0)return false;
  ServerLevel level=owner.level();Vec3 origin=pet.position();
  if(!PetSpawn.safe(level,pet,origin))return false;
  // Rotate the search so an idle bird occasionally changes perch as well as following.
  int offset=pet.tickCount/20;
  for(int i=0;i<24;i++){
   double angle=(i+offset)*Math.PI/12;var pos=owner.blockPosition().offset((int)Math.round(Math.cos(angle)*2),0,(int)Math.round(Math.sin(angle)*2));
   for(int dy:new int[]{0,1,-1}){
    Vec3 target=Vec3.atBottomCenterOf(pos.offset(0,dy,0));double d=target.distanceToSqr(origin);
    if(d<2.25||d>64||!PetSpawn.safe(level,pet,target))continue;
    int ticks=Math.max(24,(int)Math.ceil(Math.sqrt(d)/.14));boolean open=true;Vec3 previous=origin;
    for(int n=1;n<=ticks;n++){Vec3 next=arc(origin,target,(double)n/ticks);if(!clear(level,pet,previous,next)){open=false;break;}previous=next;}
    if(!open)continue;
    start=origin;end=target;step=0;duration=ticks;pet.getNavigation().stop();pet.setDeltaMovement(Vec3.ZERO);pet.setNoGravity(true);pet.flying(true);return true;
   }
  }
  cooldown=20;return false;
 }
 void tick(ServerPlayer owner,PetEntity pet){
  if(!active())return;var level=owner.level();Vec3 next=arc(start,end,(double)(step+1)/duration);
  if(owner.distanceToSqr(end)>144||!PetSpawn.safe(level,pet,end)||!clear(level,pet,pet.position(),next)){recover(owner,pet);return;}
  Vec3 delta=next.subtract(pet.position());pet.setDeltaMovement(Vec3.ZERO);pet.move(MoverType.SELF,delta);
  if(pet.position().distanceToSqr(next)>.0025){recover(owner,pet);return;}
  if(delta.horizontalDistanceSqr()>.0001){float yaw=(float)(Math.toDegrees(Math.atan2(delta.z,delta.x))-90);pet.setYRot(yaw);pet.setYBodyRot(yaw);pet.setYHeadRot(yaw);}
  if(++step>=duration){stop(pet);pet.setOnGround(true);}
 }
 private void recover(ServerPlayer owner,PetEntity pet){
  Vec3 landing=PetSpawn.safe(owner.level(),pet,start)?start:PetSpawn.find(owner,pet);
  stop(pet);if(landing==null)pet.discard();else {pet.teleportTo(landing.x,landing.y,landing.z);pet.setOnGround(true);}
 }
 void stop(PetEntity pet){start=end=null;cooldown=60;pet.flying(false);pet.setNoGravity(false);pet.setDeltaMovement(Vec3.ZERO);}
}
