package jp.neonward;
public final class VehiclePhysics {
 public static double speed(double old,int throttle,boolean brake,boolean bike){return speed(old,throttle,brake,VehicleCatalog.get(bike?"bike":"car"));}
 public static double speed(double old,int throttle,boolean brake,VehicleCatalog.Spec spec){
  if(brake)return old*.65;
  double next=(old+throttle*spec.acceleration())*(throttle==0?.94:.995);
  return Math.max(-.18,Math.min(spec.top(),next));
 }
}
