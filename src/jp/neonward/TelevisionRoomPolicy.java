package jp.neonward;
/** Geometry-only policy, independently testable without Minecraft or Chromium. */
public final class TelevisionRoomPolicy {
 static boolean inside(double x,double y,double z){return x>=66&&x<104&&z>=217&&z<260&&y>=72&&y<226;}
 public static boolean apartment(double tx,double ty,double tz,double x,double y,double z){return inside(tx,ty,tz)&&inside(x,y,z)&&!(x>=91&&x<97&&z>=225&&z<231)&&(int)Math.floor((ty-65)/8)==(int)Math.floor((y-65)/8);}
}
