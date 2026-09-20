import jp.neonward.VehiclePhysics;
public class VehiclePhysicsTest {
 public static void main(String[] args){
  double v=0;for(int i=0;i<1000;i++)v=VehiclePhysics.speed(v,1,false,true);
  if(v>.680001||v<.6)throw new AssertionError("Bike speed cap");
  for(int i=0;i<20;i++)v=VehiclePhysics.speed(v,0,true,true);
  if(v>.001)throw new AssertionError("Emergency braking");
  for(int i=0;i<1000;i++)v=VehiclePhysics.speed(v,-1,false,false);
  if(v<-.180001||v>-.1)throw new AssertionError("Reverse cap");
  for(int i=0;i<200;i++)v=VehiclePhysics.speed(v,0,false,false);
  if(Math.abs(v)>.00001)throw new AssertionError("Coasting deceleration");
  System.out.println("PASS acceleration caps, braking, reverse, coasting");
 }
}
