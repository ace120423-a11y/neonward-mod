package jp.neonward;
import java.util.*;
import jp.neonward.StreetVehicle.Part;
/** Shared detailed chassis and distinct silhouettes; all seats keep clear headroom. */
public final class FleetModels {
 static final Map<String,List<Part>> CACHE=new HashMap<>();
 static void p(List<Part>a,double x,double y,double z,double w,double h,double d,String b){a.add(new Part(x,y,z,w,h,d,b));}
 static void wheel(List<Part>a,double x,double z,double w,double r){ReferenceModels.wheel(a,x,z,w,r);}
 static List<Part> model(VehicleCatalog.Spec s){return CACHE.computeIfAbsent(s.id(),k->build(s));}
 static List<Part> build(VehicleCatalog.Spec s){
  if(CasinoGames.PRIZES.contains(s.id()))return PrizeVehicleModels.build(s.id());
  if(s.id().equals("car"))return Models.CAR;if(s.id().equals("bike"))return Models.BIKE;
  var a=new ArrayList<Part>();String id=s.id();
  if(s.bike()){
   boolean american=id.equals("chopper")||id.equals("cruiser"),tour=id.equals("cruiser"),race=id.equals("volt"),cargo=id.equals("courier");
   String paint=race?"red_concrete":american?"black_concrete":cargo?"yellow_terracotta":"green_terracotta";
   double front=american?1.8:1.1;wheel(a,-.29,-1.28,.58,.62);wheel(a,-.20,front,.40,american?.67:.57);
   p(a,-.28,.53,-1.0,.56,.16,2.0,"polished_blackstone");p(a,-.28,.72,-.10,.56,.38,.65,paint);
   p(a,-.26,.86,-.79,.52,.11,.61,"black_concrete");
   for(double x:new double[]{-.39,.21})for(int i=0;i<5;i++)p(a,x,.48+i*.065,-.13,.18,.035,.47,"iron_block");
   for(double x:new double[]{-.32,.24})for(int i=0;i<10;i++)p(a,x,.58+i*.072,front-.12-i*(american?.12:.052),.08,.12,.18,"iron_block");
   p(a,-.62,american?1.5:1.22,.50,1.24,.075,.085,"iron_block");
   for(double x:new double[]{-.64,.47})p(a,x,american?1.5:1.22,.46,.18,.095,.16,"black_concrete");
   p(a,-.23,1.15,.91,.46,.16,.08,"sea_lantern");p(a,-.19,.90,-1.70,.38,.075,.07,"redstone_block");
   for(double x:new double[]{-.48,.34})p(a,x,.33,-1.49,.14,.13,1.45,"iron_block");
   p(a,-.12,1.29,.54,.24,.03,.16,"cyan_concrete");
   if(race){for(int i=0;i<5;i++)p(a,-.42+i*.035,.78+i*.08,.48,.84-i*.07,.10,.67-i*.07,paint);p(a,-.28,1.27,.74,.56,.19,.03,"cyan_stained_glass");}
   if(american){p(a,-.32,.73,-1.47,.64,.12,.70,paint);p(a,-.23,1.04,-1.41,.46,.38,.09,"red_terracotta");}
   if(tour){p(a,-.25,.89,-1.49,.5,.12,.65,"black_concrete");for(double x:new double[]{-.85,.38}){p(a,x,.47,-1.49,.47,.51,.87,"brown_terracotta");p(a,x+.03,.73,-1.51,.4,.035,.035,"iron_block");}p(a,-.33,1.29,.8,.66,.37,.04,"light_gray_stained_glass");}
   if(cargo){p(a,-.6,.93,-1.55,1.2,.85,.85,"yellow_terracotta");for(double x:new double[]{-.46,.31})p(a,x,.96,-1.57,.10,.78,.025,"iron_block");p(a,-.48,1.30,-1.59,.96,.09,.03,"sea_lantern");}
   if(id.equals("scrambler")){p(a,-.28,1.12,-1.40,.56,.12,.48,paint);p(a,-.3,1.25,front-.4,.6,.08,.6,paint);p(a,-.16,.29,-.4,.32,.12,.8,"iron_block");}
  }else{
   boolean sport=id.equals("razor"),truck=id.equals("hauler"),armor=id.equals("bulwark"),wagon=id.equals("nomad");
   String paint=sport?"red_concrete":truck?"orange_terracotta":armor?"gray_concrete":wagon?"green_terracotta":"black_concrete";
   p(a,-1.55,.50,-3.05,3.1,.33,6.1,"polished_blackstone");
   for(double x:new double[]{-1.72,1.35})for(double z:new double[]{-2.06,2.02})wheel(a,x,z,.37,.67);
   double back=s.seats()==4?-2.8:-1.6;
   p(a,-1.48,.83,.5,2.96,.43,2.42,paint);p(a,-1.45,.83,-3.0,2.9,.4,.30,paint);
   for(double x:new double[]{-1.5,1.35}){
    p(a,x,.83,back,.15,.60,.55-back,paint);
    for(double z:new double[]{back,.38})p(a,x,1.38,z,.15,1.28,.13,paint);
    p(a,x,1.45,back+.14,.035,1.0,.23-back,"cyan_stained_glass");
    p(a,x,1.05,back+.4,.17,.065,.43,"iron_block");
    p(a,x,1.28,.27,.34,.18,.30,"black_concrete");
   }
   if(sport){for(int n=0;n<8;n++)p(a,-1.35,1.42+n*.137,.39-n*.065,2.7,.14,.04,"cyan_stained_glass");}
   else p(a,-1.36,1.42,.39,2.72,1.1,.035,"cyan_stained_glass");
   p(a,-1.35,1.42,back,2.7,1.1,.035,"cyan_stained_glass");
   p(a,-1.55,2.62,back-.07,3.1,.13,.70-back,paint);
   // Seats, headrests, door trim, driver wheel and illuminated dashboard.
   for(int seat=0;seat<s.seats();seat++){double x=seat%2==0?-1.04:.20,z=seat<2?-.9:-2.13;
    p(a,x,.90,z-.22,.82,.15,.80,"black_concrete");p(a,x+.07,1.03,z-.28,.68,.77,.14,"red_terracotta");p(a,x+.15,1.80,z-.29,.50,.20,.15,"black_concrete");
    for(int n=0;n<5;n++)p(a,x+.1,1.1+n*.12,z-.13,.6,.02,.02,"gray_concrete");}
   p(a,-1.28,1.51,.05,2.56,.27,.30,"black_concrete");p(a,-1.03,1.89,.017,.85,.12,.024,"cyan_concrete");
   for(int n=0;n<12;n++){double r=n*Math.PI/6;p(a,-.65+Math.cos(r)*.23,1.73+Math.sin(r)*.23,-.30,.065,.065,.05,"black_concrete");}
   p(a,-.85,1.73,-.30,.46,.045,.05,"iron_block");p(a,-.13,1.07,-.74,.26,.15,.73,"polished_blackstone");p(a,-.04,1.23,-.4,.08,.21,.08,"iron_block");
   for(double x:new double[]{-1.39,1.34})p(a,x,1.32,back+.2,.05,.045,.1-back,"cyan_concrete");
   for(double x:new double[]{-1.32,.60})p(a,x,1.03,2.96,.72,.14,.055,"sea_lantern");
   p(a,-1.35,.98,-3.08,2.7,.11,.04,"redstone_block");p(a,-1.54,.69,2.97,3.08,.19,.12,"iron_block");
   for(int n=0;n<6;n++)p(a,-.48+n*.16,.9,3.0,.075,.25,.055,"black_concrete");
   for(double x:new double[]{-1.73,1.35})for(double z:new double[]{-2.06,2.02}){p(a,x,1.29,z-.5,.38,.09,1.0,paint);for(double dz:new double[]{-.62,.5})p(a,x,.83,z+dz,.38,.48,.12,paint);}
   for(double x:new double[]{-1.54,1.51}){
    p(a,x,.70,-1.38,.03,.04,2.62,"sea_lantern");
    if(s.seats()==4){p(a,x,1.38,-1.22,.075,1.22,.12,paint);p(a,x,1.07,-2.25,.09,.055,.32,"iron_block");}
   }
   if(sport){p(a,-1.6,1.45,-2.98,3.2,.10,.52,paint);for(double x:new double[]{-1.2,1.1})p(a,x,1.13,-2.8,.1,.32,.15,"iron_block");for(int n=0;n<6;n++)p(a,-.54,1.29,.76+n*.25,1.08,.04,.10,"black_concrete");}
   if(truck){for(double x:new double[]{-1.5,1.35})p(a,x,.83,-2.95,.15,.9,1.28,paint);p(a,-1.45,.82,-2.98,2.9,.12,1.36,"iron_block");for(int n=0;n<4;n++)p(a,-1.24+n*.64,.95,-2.8,.5,.52,.9,"brown_terracotta");}
   if(armor){for(double x:new double[]{-1.6,1.46})for(int n=0;n<4;n++)p(a,x,1.0,-2.5+n*.7,.14,.47,.6,"iron_block");p(a,-1.6,1.0,3.0,3.2,.18,.22,"polished_blackstone");p(a,-.9,2.77,-1.4,1.8,.15,1.1,"gray_concrete");}
   if(wagon){for(double x:new double[]{-1.37,1.25})p(a,x,2.77,-2.6,.12,.14,2.75,"iron_block");for(int n=0;n<5;n++)p(a,-1.3,2.78,-2.6+n*.6,2.6,.10,.1,"iron_block");p(a,-.95,2.91,-2.4,1.9,.28,1.35,"brown_terracotta");}
  }
  return List.copyOf(a);
 }
}
