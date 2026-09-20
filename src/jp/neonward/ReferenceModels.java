package jp.neonward;
import java.util.*;
import jp.neonward.StreetVehicle.Part;
class ReferenceModels {
 static void p(List<Part> a,double x,double y,double z,double w,double h,double d,String b){a.add(new Part(x,y,z,w,h,d,b));}
 static void wheel(List<Part> a,double x,double z,double width,double r){
  for(int i=0;i<8;i++){double yy=-r+(i+.5)*r/4,half=Math.sqrt(r*r-yy*yy);p(a,x,r+yy-r/8,z-half,width,r/4,half*2,"black_concrete");}
  for(double xx:new double[]{x-.015,x+width}){p(a,xx,r-.23,z-.23,.025,.46,.46,"iron_block");p(a,xx-.005,r-.13,z-.13,.035,.26,.26,"polished_blackstone");}
 }
 static void populate(List<Part> bike,List<Part> car){
  bike.clear();car.clear();
  wheel(bike,-.26,-1.22,.52,.61);wheel(bike,-.24,1.05,.48,.61);
  p(bike,-.35,.43,-.85,.7,.42,1.5,"gray_concrete");p(bike,-.3,.32,-.55,.6,.25,1,"iron_block");
  for(double x:new double[]{-.43,.28}){p(bike,x,.57,-.4,.15,.64,.67,"polished_blackstone");p(bike,x-.01,.74,-.21,.17,.27,.27,"iron_block");}
  p(bike,-.26,1.05,-.72,.52,.13,.65,"red_terracotta");
  for(int i=0;i<9;i++){double yy=1.12+i*.12,z=-.76-i*.035;p(bike,-.25,yy,z,.5,.1,.14,"red_terracotta");p(bike,-.26,yy,z-.035,.52,.025,.03,"iron_block");}
  p(bike,-.3,1.12,.04,.6,.36,.59,"gray_concrete");p(bike,-.36,1.01,.47,.72,.21,.45,"black_concrete");
  for(int i=0;i<5;i++)for(double x:new double[]{-.28,.195})p(bike,x,.6+i*.12,.95-i*.075,.085,.15,.17,"iron_block");
  p(bike,-.5,1.48,.17,1,.08,.09,"iron_block");p(bike,-.16,1.42,.3,.32,.09,.2,"black_concrete");
  p(bike,-.14,1.515,.32,.28,.018,.14,"sea_lantern");p(bike,-.27,1.12,.91,.54,.11,.06,"sea_lantern");
  p(bike,.34,.28,-1.18,.18,.18,1.17,"iron_block");p(bike,-.38,.44,-.65,.025,.06,.62,"cyan_concrete");p(bike,-.25,.9,-1.8,.5,.08,.06,"redstone_block");
  p(car,-1.03,.36,-2.1,2.06,.48,4.2,"gray_concrete");p(car,-.97,.3,-1.8,1.94,.1,3.7,"black_concrete");
  for(int i=0;i<5;i++)p(car,-.94,.74-i*.038,.55+i*.27,1.88,.17,.28,"gray_concrete");
  p(car,-.88,.76,-1.78,1.76,.21,.7,"gray_concrete");
  for(double x:new double[]{-.83,.77}){p(car,x,.88,-1.08,.06,.58,1.48,"gray_concrete");p(car,x,1,.02,.06,.42,.1,"black_concrete");}
  for(double x:new double[]{-1.13,.91})for(double z:new double[]{-1.32,1.24})wheel(car,x,z,.22,.53);
  for(double x:new double[]{-1.065,1.03}){p(car,x,.62,-1.1,.035,.045,2.2,"iron_block");p(car,x,.4,-.35,.035,.045,.45,"yellow_concrete");}
  p(car,-.95,.74,1.99,.61,.09,.05,"sea_lantern");p(car,.34,.74,1.99,.61,.09,.05,"sea_lantern");p(car,-.88,.74,-2.13,1.76,.09,.04,"redstone_block");
  for(double x:new double[]{-.87,.72})for(int i=0;i<5;i++)p(car,x,.87+i*.12,-1.96-i*.04,.15,.13,.58-i*.06,"black_concrete");
  p(car,-1,1.47,-2.14,2,.09,.46,"gray_concrete");for(double x:new double[]{-.7,.48})p(car,x,.27,-2.27,.22,.18,.32,"iron_block");
  // Raised cabin, solid door panels and sculpted wheel shoulders.
  p(car,-.80,1.08,.27,1.60,.52,.025,"cyan_stained_glass");
  p(car,-.80,1.08,-1.055,1.60,.52,.025,"cyan_stained_glass");
  for(double x:new double[]{-.815,.795})p(car,x,1.10,-1.02,.025,.48,1.27,"cyan_stained_glass");
  p(car,-.88,1.60,-1.10,1.76,.13,1.38,"black_concrete");
  for(double x:new double[]{-.88,.80}){
   p(car,x,.80,-1.13,.08,.85,.14,"gray_concrete");
   p(car,x,.80,.20,.08,.85,.14,"gray_concrete");
   p(car,x,.77,-1.02,.08,.30,1.24,"gray_concrete");
  }
  for(double x:new double[]{-1.15,.86})for(double z:new double[]{-1.32,1.24}){
   p(car,x,.96,z-.44,.29,.14,.88,"gray_concrete");
   p(car,x,.60,z-.62,.29,.37,.18,"gray_concrete");
   p(car,x,.60,z+.44,.29,.37,.18,"gray_concrete");
  }
  for(double x:new double[]{-1.23,1.03})p(car,x,1.12,.06,.20,.13,.26,"black_concrete");
  p(car,-.99,.42,1.99,1.98,.22,.16,"polished_blackstone");
  p(car,-.92,.55,2.10,1.84,.10,.035,"iron_block");

  // Two sculpted bucket seats with contrasting bolsters and headrests.
  p(car,-.77,.85,-1.03,1.54,.04,1.30,"black_concrete");
  for(double x:new double[]{-.68,.15}){
   p(car,x,.90,-.89,.53,.13,.58,"black_concrete");
   p(car,x+.07,1.03,-.92,.39,.43,.12,"red_terracotta");
   p(car,x+.10,1.46,-.91,.33,.13,.12,"black_concrete");
   for(double side:new double[]{x,x+.45})p(car,side,1.02,-.88,.08,.30,.38,"gray_concrete");
   for(int k=0;k<4;k++)p(car,x+.09,1.09+k*.09,-.79,.35,.018,.018,"black_concrete");
  }
  // Dashboard, recessed driver display, air vents and passenger glovebox.
  p(car,-.76,1.02,-.03,1.52,.20,.30,"black_concrete");
  p(car,-.71,1.23,.08,.60,.04,.14,"polished_blackstone");
  p(car,-.71,1.12,.015,.60,.11,.018,"cyan_concrete");
  for(double x:new double[]{-.73,.38})for(int k=0;k<4;k++)p(car,x+k*.045,1.15,-.04,.018,.05,.018,"iron_block");
  p(car,.35,1.06,-.045,.36,.018,.018,"iron_block");
  // Steering wheel, spokes and illuminated hub facing the driver.
  for(int k=0;k<16;k++){double a=k*Math.PI/8;p(car,-.41+Math.cos(a)*.17,1.16+Math.sin(a)*.17,-.25,.045,.045,.035,"black_concrete");}
  p(car,-.55,1.17,-.25,.31,.035,.045,"iron_block");
  p(car,-.42,1.02,-.25,.035,.17,.045,"iron_block");
  p(car,-.45,1.15,-.265,.10,.075,.025,"sea_lantern");
  // Console, navigation screen, gear selector and switches.
  p(car,-.09,.91,-.78,.18,.14,.72,"polished_blackstone");
  p(car,-.08,1.09,-.10,.16,.13,.028,"cyan_concrete");
  p(car,-.025,1.04,-.43,.045,.15,.045,"iron_block");
  p(car,-.045,1.17,-.45,.085,.045,.075,"black_concrete");
  for(int k=0;k<4;k++)p(car,-.065+k*.04,1.07,-.64,.022,.018,.035,k%2==0?"sea_lantern":"redstone_block");
  // Door cards, pulls, speaker grilles, ambient strips and roof console.
  for(double x:new double[]{-.76,.73}){
   p(car,x,1.04,-.77,.035,.075,.58,"black_concrete");
   p(car,x,1.10,-.50,.04,.022,.18,"iron_block");
   p(car,x,.96,-.78,.025,.025,.81,"cyan_concrete");
   p(car,x,.88,-.19,.035,.12,.15,"polished_blackstone");
  }
  p(car,-.07,1.56,-.06,.14,.035,.18,"black_concrete");
  p(car,-.035,1.55,-.02,.07,.015,.08,"sea_lantern");
  // Increase the complete coupe to 150%, keeping all details aligned.
  for(int k=0;k<car.size();k++){Part a=car.get(k);car.set(k,new Part(a.x()*1.5,a.y()*1.5,a.z()*1.5,a.w()*1.5,a.h()*1.5,a.d()*1.5,a.block()));}

 }
}
