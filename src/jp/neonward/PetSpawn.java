package jp.neonward;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;

/** Bounded loaded-chunk search; never requests tickets or changes terrain. */
final class PetSpawn {
 static Vec3 find(ServerPlayer owner,PetEntity pet){
  var level=owner.level();var origin=owner.blockPosition();
  for(int radius=1;radius<=3;radius++)for(int dy:new int[]{0,1,-1})for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
   if(Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;
   var pos=origin.offset(dx,dy,dz);var point=Vec3.atBottomCenterOf(pos);
   if(safe(level,pet,point)&&owner.distanceToSqr(point)>1.0)return point;
  }
  return null;
 }
 static boolean safe(ServerLevel level,PetEntity pet,Vec3 point){
  var pos=BlockPos.containing(point);var box=pet.getDimensions(pet.getPose()).makeBoundingBox(point);
  if(!level.getWorldBorder().isWithinBounds(box)||pos.getY()<=level.getMinY()||box.maxY>=level.getMaxY())return false;
  if(!level.hasChunksAt(pos.offset(-1,-1,-1),pos.offset(1,2,1)))return false;
  if(WalkNodeEvaluator.getPathTypeStatic(pet,pos)!=PathType.WALKABLE)return false;
  if(!level.getBlockState(pos.below()).isCollisionShapeFullBlock(level,pos.below()))return false;
  if(!level.noCollision(pet,box)||level.containsAnyLiquid(box))return false;
  return level.getEntities(pet,box,e->e.isAlive()&&!(e instanceof PetEntity)).isEmpty();
 }
 private PetSpawn(){}
}
