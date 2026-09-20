package jp.neonward;
import java.util.*;
import jp.neonward.StreetVehicle.Part;
final class PrizeVehicleModels {
 static void p(List<Part>a,double x,double y,double z,double w,double h,double d,String b){a.add(new Part(x,y,z,w,h,d,b));}
 static List<Part> build(String id){var a=new ArrayList<Part>();
  if(id.equals("duck")||id.equals("teapot")){
   for(double x:new double[]{-1.65,1.3})for(double z:new double[]{-2,1.7})ReferenceModels.wheel(a,x,z,.35,.65);
   p(a,-1.45,.52,-2.8,2.9,.24,5.6,"polished_blackstone");String metal=id.equals("duck")?"yellow_concrete":"waxed_copper_block";
   for(int n=0;n<5;n++){double w=3.1-Math.abs(n-2)*.23;for(double side:new double[]{-w/2,w/2-.16})p(a,side,.72+n*.22,-2.65,.16,.23,4.85,metal);}
   for(int seat=0;seat<(id.equals("duck")?4:2);seat++){double x=seat%2==0?-1.0:.2,z=seat<2?-.9:-2.13;p(a,x,.92,z-.2,.8,.13,.75,"black_concrete");p(a,x+.08,1.05,z-.23,.64,.78,.14,"red_terracotta");}
   p(a,-1.22,1.55,.02,2.44,.23,.24,"black_concrete");p(a,-1.02,1.85,.015,.81,.12,.025,"cyan_concrete");
   for(int n=0;n<12;n++){double r=n*Math.PI/6;p(a,-.62+Math.cos(r)*.21,1.72+Math.sin(r)*.21,-.25,.065,.065,.05,"iron_block");}
   if(id.equals("duck")){
    p(a,-.82,1.5,1.0,1.64,1.40,1.54,metal);p(a,-.68,2.90,1.13,1.36,.20,1.28,metal);
    p(a,-.90,1.82,2.40,1.8,.35,.86,"orange_concrete");p(a,-.88,1.80,2.63,1.76,.035,.64,"red_terracotta");
    for(double x:new double[]{-.835,.8}){p(a,x,2.40,1.84,.045,.35,.4,"white_concrete");p(a,x-.006,2.45,2.02,.06,.24,.16,"black_concrete");}
    for(double x:new double[]{-1.71,1.48})for(int n=0;n<4;n++)p(a,x,.88+n*.12,-1.75+n*.18,.23,.13,1.55-n*.28,"orange_terracotta");
    for(int n=0;n<4;n++)p(a,-.65+n*.11,1.05+n*.18,-2.92-n*.07,1.3-n*.22,.20,.4,metal);
   }else{
    for(int n=0;n<8;n++){double t=n*Math.PI/4;p(a,-1.92+Math.cos(t)*.38,1.53+Math.sin(t)*.74,-1.7,.25,.24,.25,"iron_block");}
    for(int n=0;n<7;n++)p(a,1.2+n*.10,1.0+n*.16,1.0,.4,.25,.5,metal);
    p(a,-1.25,2.75,-2.55,2.5,.14,2.9,metal);p(a,-.3,2.9,-1.2,.6,.23,.6,"gold_block");
    p(a,-.28,1.5,1.18,.56,2.45,.56,"polished_blackstone");p(a,-.4,3.91,1.06,.8,.12,.8,"iron_block");
    for(int n=0;n<5;n++)p(a,-1.55,1.05+n*.19,.75,.1,.07,.35,"gold_block");
   }
   for(double x:new double[]{-1.32,1.06})p(a,x,.96,2.78,.26,.19,.06,"sea_lantern");p(a,-1.05,.89,-2.85,2.1,.08,.05,"redstone_block");
  }else if(id.equals("gyro")){
   ReferenceModels.wheel(a,-.48,.70,.96,1.10);for(double x:new double[]{-.63,.51}){p(a,x,.85,-.55,.12,.15,1.8,"waxed_copper_block");p(a,x,1.0,-.5,.12,.86,.15,"iron_block");}
   p(a,-.32,1.1,-.77,.64,.15,.6,"black_concrete");p(a,-.29,1.25,-.81,.58,.4,.1,"purple_concrete");p(a,-.58,1.6,-.16,1.16,.1,.12,"iron_block");
   for(int n=0;n<8;n++)for(double x:new double[]{-.51,.49}){double r=n*Math.PI/4;p(a,x,1.05+Math.cos(r)*.76,.70+Math.sin(r)*.76,.04,.14,.14,"sea_lantern");}
   p(a,-.34,2.2,.50,.68,.12,.5,"purple_concrete");p(a,-.21,1.76,-.11,.42,.07,.18,"cyan_concrete");
   for(double x:new double[]{-.87,.62}){p(a,x,.35,-.72,.25,.08,.42,"iron_block");ReferenceModels.wheel(a,x,-.7,.14,.20);}
  }else{
   ReferenceModels.wheel(a,-.28,-1.4,.56,.65);ReferenceModels.wheel(a,-.2,1.65,.4,.65);
   p(a,-.35,.57,-1.6,.7,.25,2.7,"polished_blackstone");p(a,-.32,.93,-.72,.64,.13,.6,"red_terracotta");
   for(int n=0;n<10;n++)for(double x:new double[]{-.3,.23})p(a,x,.62+n*.07,1.53-n*.11,.07,.13,.15,"iron_block");
   for(int n=0;n<5;n++){double w=.8-Math.abs(n-2)*.10;p(a,-w/2,.86+n*.065,.05,w,.065,1.15,"dark_oak_planks");}
   p(a,-.06,1.2,.22,.12,.035,.8,"gold_block");p(a,-.26,1.2,.43,.52,.035,.12,"gold_block");
   p(a,-.62,1.53,.50,1.24,.08,.12,"iron_block");p(a,-.27,1.06,-1.12,.54,.75,.12,"black_concrete");
   for(double x:new double[]{-.72,.4}){p(a,x,.5,-1.43,.32,.45,.8,"dark_oak_planks");p(a,x+.05,.67,-1.45,.22,.05,.04,"gold_block");}
   p(a,-.2,1.2,1.29,.4,.18,.07,"sea_lantern");
  }
  return List.copyOf(a);
 }
}
