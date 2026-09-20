package jp.neonward;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;
public class FurnitureSeat extends Entity {
 BlockPos anchor=BlockPos.ZERO;
 public FurnitureSeat(EntityType<? extends FurnitureSeat> t,Level l){super(t,l);setNoGravity(true);}
 @Override protected void defineSynchedData(SynchedEntityData.Builder b){}
 @Override protected void readAdditionalSaveData(ValueInput i){anchor=BlockPos.of(i.getLongOr("Anchor",0));}
 @Override protected void addAdditionalSaveData(ValueOutput o){o.putLong("Anchor",anchor.asLong());}
 @Override public boolean hurtServer(ServerLevel l,DamageSource d,float a){return false;}
 @Override public void tick(){super.tick();if(!level().isClientSide()&&(getPassengers().isEmpty()||!(level().getBlockState(anchor).getBlock() instanceof NeonFurniture.FurnitureBlock b)||!b.kind.equals("seat")))discard();}
 @Override protected void positionRider(Entity rider,MoveFunction move){move.accept(rider,getX(),getY(),getZ());}
 @Override public Vec3 getDismountLocationForPassenger(LivingEntity p){for(Vec3 v:new Vec3[]{new Vec3(1,0,0),new Vec3(-1,0,0),new Vec3(0,0,1),new Vec3(0,0,-1)}){var target=new Vec3(anchor.getX()+.5,anchor.getY(),anchor.getZ()+.5).add(v);if(level().noCollision(p,p.getBoundingBox().move(target.subtract(p.position()))))return target;}return position().add(0,1,0);}
}
