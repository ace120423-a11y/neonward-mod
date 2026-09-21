package jp.neonward;

/** Local coordinates: forward follows the attacker's horizontal look direction. */
public final class MeleeShape {
 public enum Shape {HAMMER,BAT,BLADE,WAKIZASHI}
 public record Profile(Shape shape,double reach,double radius,double halfWidth){}
 public static Profile profile(String id){return switch(id){
  case "coil_hammer","pile_maul"->new Profile(Shape.HAMMER,3,3.5,0);
  case "shock_bat","riot_bat"->new Profile(Shape.BAT,3.2,0,0);
  case "neon_blade","kurosame_katana"->new Profile(Shape.BLADE,4,0,.65);
  case "raikiri_odachi"->new Profile(Shape.BLADE,4.5,0,.75);
  case "akatsuki_wakizashi"->new Profile(Shape.WAKIZASHI,2.25,0,.35);
  case "volt_spear"->new Profile(Shape.BLADE,4.5,0,.3);
  case "chain_kusarigama"->new Profile(Shape.BAT,3.5,0,0);
  case "neon_dualblades"->new Profile(Shape.WAKIZASHI,2.5,0,.35);
  case "reaper_scythe"->new Profile(Shape.BAT,3.6,0,0);
  case "impact_gauntlet"->new Profile(Shape.BLADE,2.1,0,.5);
  default->null;
 };}
 public static boolean contains(Profile p,double forward,double side,double impactDistance,double height){
  if(forward<0||Math.abs(height)>2)return false;
  return switch(p.shape()){
   case HAMMER->impactDistance<=p.radius();
   case BAT->forward*forward+side*side<=p.reach()*p.reach()&&Math.abs(side)<=forward*Math.tan(Math.toRadians(65));
   case BLADE,WAKIZASHI->forward<=p.reach()&&Math.abs(side)<=p.halfWidth();
  };
 }
}
