package jp.neonward;
import java.util.*;
import jp.neonward.StreetVehicle.Part;
public class Models {
 static final List<Part> BIKE=new ArrayList<>(),CAR=new ArrayList<>();
 static void p(List<Part> a,double x,double y,double z,double w,double h,double d,String b){a.add(new Part(x,y,z,w,h,d,b));}
 static {
  p(BIKE,-.22,.4,-1,.44,.35,2.1,"polished_blackstone");p(BIKE,-.3,.66,-.2,.6,.5,.95,"cyan_terracotta");
  p(BIKE,-.25,.88,-.85,.5,.16,.7,"black_concrete");p(BIKE,-.31,.75,.1,.035,.12,.7,"sea_lantern");p(BIKE,.275,.75,.1,.035,.12,.7,"sea_lantern");
  for(double z:new double[]{-1.1,.8}){p(BIKE,-.18,.05,z,.36,.72,.72,"black_concrete");p(BIKE,-.19,.22,z+.17,.38,.36,.36,"iron_block");}
  p(BIKE,-.4,1.12,.45,.8,.09,.09,"iron_block");p(BIKE,-.06,.8,.5,.12,.35,.12,"iron_block");
  p(BIKE,-.24,.86,.83,.48,.18,.09,"sea_lantern");p(BIKE,-.19,.63,-1.16,.38,.1,.08,"redstone_block");
  p(BIKE,.25,.32,-.85,.15,.17,1,"iron_block");
  p(CAR,-1,.4,-1.8,2,.45,3.6,"black_concrete");p(CAR,-.92,.74,.45,1.84,.25,1.25,"cyan_terracotta");
  p(CAR,-.85,.76,-1.5,1.7,.25,.65,"cyan_terracotta");p(CAR,-.78,.84,-.75,1.56,.64,1.3,"cyan_stained_glass");
  p(CAR,-.82,1.48,-.8,1.64,.11,1.4,"black_concrete");
  for(double x:new double[]{-1.06,.96}){p(CAR,x,.57,-1.6,.1,.09,3.2,"sea_lantern");for(double z:new double[]{-1.45,.9}){p(CAR,x-.12,.06,z,.24,.75,.75,"black_concrete");p(CAR,x-.13,.24,z+.18,.26,.38,.38,"iron_block");}}
  p(CAR,-.86,.69,1.78,.63,.13,.07,"sea_lantern");p(CAR,.23,.69,1.78,.63,.13,.07,"sea_lantern");p(CAR,-.82,.66,-1.85,1.64,.12,.08,"redstone_block");
  p(CAR,-.9,1.05,-1.8,1.8,.1,.28,"polished_blackstone");p(CAR,-.62,.25,-1.95,.25,.2,.35,"iron_block");p(CAR,.37,.25,-1.95,.25,.2,.35,"iron_block");
  ReferenceModels.populate(BIKE,CAR);
 }
}
