package jp.neonward;

import net.minecraft.world.phys.Vec3;
/** Shared analytic geometry: the warning and hit test describe the same footprint. */
public record BossHazard(int shape,Vec3 a,Vec3 b,double radius,double angle,int start,int live,float power,boolean jump,int effect) {
 public static final int DISC=0,RING=1,LINE=2,CONE=3;
 public boolean contains(double x,double z,double padding){
  double dx=x-a.x,dz=z-a.z,d=Math.sqrt(dx*dx+dz*dz);
  return switch(shape){
   case DISC -> d<=radius+padding;
   case RING -> Math.abs(d-radius)<=.3+padding;
   case LINE -> {double vx=b.x-a.x,vz=b.z-a.z,len=vx*vx+vz*vz,t=len==0?0:Math.max(0,Math.min(1,(dx*vx+dz*vz)/len));yield Math.hypot(dx-t*vx,dz-t*vz)<=radius+padding;}
   case CONE -> {double delta=Math.atan2(dz,dx)-angle;delta=Math.atan2(Math.sin(delta),Math.cos(delta));yield d<=radius+padding&&Math.abs(delta)<=b.x+Math.asin(Math.min(1,padding/Math.max(.1,d)));}
   default -> false;
  };
 }
}
