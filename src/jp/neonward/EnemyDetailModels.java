package jp.neonward;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

/** Parts are children of animated limbs, so equipment follows the existing walk and aim poses. */
final class EnemyDetailModels {
 // Atlas materials: plate, rubber, brass, emissive, cloth, glass, vent, hazard.
 static void p(PartDefinition parent,String name,int material,float x,float y,float z,float w,float h,float d){
  parent.addOrReplaceChild("detail_"+name,CubeListBuilder.create().texOffs(128+(material%4)*64,(material/4)*64).addBox(x,y,z,w,h,d),PartPose.ZERO);
 }
 static void decorate(PartDefinition root,String id){
  var h=root.getChild("head");var b=root.getChild("body");var r=root.getChild("right_arm");var l=root.getChild("left_arm");
  var rl=root.getChild("right_leg");var ll=root.getChild("left_leg");
  // Shared construction details: layered soles, elbow bearings and stitched straps.
  for(var leg:new PartDefinition[]{rl,ll}){
   p(leg,"sole",1,-2.25f,10.8f,-2.8f,4.5f,1.3f,5.3f);
   p(leg,"toe",0,-2.15f,9.3f,-3,4.3f,1.5f,2);
  }
  p(r,"elbow",2,-3.5f,3.5f,-.8f,1,2,1.6f);p(l,"elbow",2,2.5f,3.5f,-.8f,1,2,1.6f);
  p(b,"strap",1,2.7f,0,-3.3f,1,9,.5f);
  if(id.equals("pile_breaker")||id.equals("belt_gunner")||id.equals("iron_colossus")||id.equals("patch_medic")){
   p(b,"gauge_bezel",2,-3.8f,2,-4.6f,2.8f,2.8f,.8f);
   p(b,"gauge_face",8,-3.5f,2.3f,-4.8f,2.2f,2.2f,.3f);
   p(b,"gauge_needle",1,-2.5f,2.5f,-5,.2f,1.4f,.2f);
   p(b,"steam_pipe_up",2,4.5f,1,1.8f,.8f,8,.8f);
   p(b,"steam_pipe_elbow",2,3,1,1.8f,2.3f,.8f,.8f);
   p(b,"valve_hub",2,4.4f,5,1.1f,1,1,.8f);
   for(int i=0;i<8;i++){
    float angle=(float)(i*Math.PI/4),x=4.9f+(float)Math.cos(angle)*1.4f,y=5.5f+(float)Math.sin(angle)*1.4f;
    p(b,"valve_tooth"+i,2,x-.45f,y-.45f,1,.9f,.9f,.5f);
   }
  }
  switch(id){
   case "scrap_raider" -> {
    p(h,"jaw_filter",6,-3.6f,-2.8f,-5.5f,2,2.5f,2);p(h,"ear_radio",2,-5,-6,-1,1.5f,3,3);
    p(r,"tire_shoulder",1,-4.5f,-2.8f,-3,6,4,6);
    for(int i=0;i<3;i++)p(r,"scrap_spike"+i,2,-4+i*1.8f,-5,-.6f,.7f,3,1.2f);
    p(b,"salvage_frame",2,-4.7f,0,2.2f,1,11,1);p(b,"tank",7,1,1,2.4f,3,7,3);
    p(ll,"patched_shin",7,-2.4f,5,-2.7f,4.8f,5,1);p(b,"tool",2,-5.5f,8,-1,1,6,1);
   }
   case "chrome_ronin" -> {
    p(h,"hat_brim",0,-7,-8.8f,-6,14,1,12);p(h,"hat_tier",0,-5.5f,-10,-4.5f,11,1.3f,9);
    p(h,"hat_top",2,-3.5f,-11.1f,-3,7,1.2f,6);
    for(int i=0;i<3;i++){
     p(b,"lamellar_l"+i,0,-5.2f,9+i*1.8f,-3.2f,4.5f,1.6f,1);p(b,"lamellar_r"+i,0,.7f,9+i*1.8f,-3.2f,4.5f,1.6f,1);
     p(l,"sleeve_plate"+i,0,-1.7f,i*1.8f,-2.8f,5,1.5f,5.6f);
    }
    p(b,"obi",4,-4.7f,7,-3.3f,9.4f,2,6.6f);p(b,"knot",2,-1,7,3.3f,2,2,1);
    p(b,"scabbard",1,-5.3f,5,2,1.4f,14,1.4f);p(b,"sword_wrap",3,-5.4f,2,1.9f,1.6f,3,1.6f);
    p(h,"menpo",0,-3,-3.5f,-5.8f,6,3,1);p(h,"nose",2,-.6f,-4.5f,-6.2f,1.2f,2,1);
    for(int i=0;i<4;i++)p(h,"mask_vent"+i,1,-2.5f+i*1.5f,-2.5f,-6,.7f,.5f,.3f);
    for(int i=0;i<3;i++){
     p(b,"armor_lacing_l"+i,2,-3.5f,10+i*1.8f,-3.4f,.5f,.8f,.3f);
     p(b,"armor_lacing_r"+i,2,2.5f,10+i*1.8f,-3.4f,.5f,.8f,.3f);
    }
    p(h,"hat_tie_l",4,-3.8f,-4,-3.8f,.6f,5,.6f);p(h,"hat_tie_r",4,3.2f,-4,-3.8f,.6f,5,.6f);
   }
   case "neon_runner" -> {
    p(h,"fin_right",0,-5,-7,0,1,3,7);p(h,"fin_light",3,4.9f,-6.5f,1,.3f,1,5);
    for(var leg:new PartDefinition[]{rl,ll}){
     p(leg,"shin_blade",0,-1.6f,3,1.8f,3.2f,7,1);p(leg,"tendon",3,-.4f,2,2.7f,.8f,7,.5f);
     p(leg,"heel_spring",2,-1.5f,8,2.3f,3,2,2);
    }
    p(b,"compact_drive",0,-2.5f,1,2.4f,5,5,2);p(b,"drive_light",3,-1.8f,2,4.4f,3.6f,.8f,.3f);
    p(l,"forearm_blade",0,3,4,-1,1,7,2);
    p(h,"headband",4,-4.5f,-7.5f,-4.8f,9,1.5f,1);p(h,"knot",4,2,-6,4,2,2,1);
    p(h,"ribbon_l",4,2,-4,4.5f,1,5,.5f);p(h,"ribbon_r",4,3.5f,-4,4.5f,1,3.5f,.5f);
    p(b,"waist_wrap",4,-4.5f,8,-2.9f,9,2,5.8f);
    for(int i=0;i<3;i++)p(b,"kunai"+i,2,-3.5f+i*1.2f,8,-3.4f,.5f,3,.5f);
   }
   case "street_enforcer" -> {
    p(h,"helmet",0,-4.6f,-8.7f,-4.5f,9.2f,2,9);p(h,"visor_glass",5,-4.5f,-6.2f,-5.1f,9,2.8f,.4f);
    p(h,"radio",1,4,-5,0,1.6f,3,3);p(h,"aerial",2,4.5f,-10,1,.5f,5,.5f);
    p(b,"badge",3,-3.5f,2,-3.5f,1.3f,1.5f,.3f);
    for(int i=0;i<3;i++)p(b,"magazine"+i,1,-3.5f+i*2.2f,6,-4,1.7f,3,1.3f);
    p(rl,"holster",1,-3.3f,1,-1,1.2f,5,2.5f);p(l,"cuff",0,-1.5f,6,-2.5f,5,3,5);
   }
   case "belt_gunner" -> {
    p(h,"ear_protection_l",1,-5.3f,-7,-1,1.5f,4,3);p(h,"ear_protection_r",1,3.8f,-7,-1,1.5f,4,3);
    p(b,"ammo_drum",7,-5,0,2.5f,10,10,5);
    for(int i=0;i<7;i++)p(b,"feed"+i,2,-4+i*.8f,1+i*.8f,-4.1f,.6f,2,.8f);
    p(r,"brace",0,-4,2,-3,1,7,6);p(l,"brace",0,3,2,-3,1,7,6);
    p(b,"heat_sink",6,-3,1,7.5f,6,7,.7f);
   }
   case "ghost_sniper" -> {
    p(h,"hood_top",4,-4.9f,-9,-4.6f,9.8f,1.5f,9.5f);p(h,"hood_l",4,-5,-8,-4.6f,1.1f,8,9.5f);p(h,"hood_r",4,3.9f,-8,-4.6f,1.1f,8,9.5f);
    p(h,"lens",3,-3.5f,-5.6f,-7.3f,2,1.8f,.4f);
    for(int i=0;i<5;i++)p(b,"cloak_tail"+i,4,-5+i*2,8,3.6f,1.8f,8+i%3,1);
    p(b,"rangefinder",6,3,1,3,2,7,2);p(l,"wrist_scope",5,-1,4,-3.8f,3.7f,3,.7f);
   }
   case "riot_bulwark" -> {
    p(h,"brow",0,-5,-8.3f,-5,10,2,10);p(h,"jaw",0,-4,-2.5f,-5.5f,8,3,3);
    p(b,"gorget",0,-5,-1,-4,10,2,8);
    p(l,"shield_back",1,-2,0,-5,11,14,1);p(l,"shield",0,-2.5f,-1,-6,12,15,1);
    p(l,"shield_window",5,-1,1,-6.2f,8,2,.3f);p(l,"shield_mark",7,.5f,7,-6.2f,5,4,.3f);
    for(var leg:new PartDefinition[]{rl,ll})p(leg,"greave",0,-2.7f,3,-3.5f,5.4f,7,2);
   }
   case "pile_breaker" -> {
    p(h,"hardhat",7,-5,-9,-4.5f,10,2.5f,9);p(h,"brim",2,-5.5f,-7,-6,11,.7f,11);
    p(b,"harness",7,-5,0,-3.8f,10,3,1);p(b,"engine",6,-4,0,2,8,8,4);
    for(var arm:new PartDefinition[]{r,l}){
     p(arm,"hydraulic_barrel",0,-1,0,2,2.5f,5,2.5f);p(arm,"hydraulic_rod",2,-.4f,5,2.6f,1.3f,4,1.3f);
    }
    p(l,"clamp",7,-2,7,-3.2f,6,4,6.4f);p(b,"stack_rim",1,1.6f,-4.5f,1.6f,2.8f,1,3.8f);
   }
   case "arc_trooper" -> {
    for(int i=0;i<4;i++){
     p(b,"coil_ring_l"+i,2,-5.5f,-1+i*2.5f,1.5f,4,.8f,4);p(b,"coil_ring_r"+i,2,1.5f,-1+i*2.5f,1.5f,4,.8f,4);
    }
    p(b,"coil_core_l",3,-4.2f,-1,5.5f,1.4f,9,.4f);p(b,"coil_core_r",3,2.8f,-1,5.5f,1.4f,9,.4f);
    p(h,"insulator",1,2.2f,-10,0,2.4f,1,2.4f);p(h,"electrode",3,2.7f,-12.5f,.5f,1.4f,1,1.4f);
    p(l,"conductor",2,-2,5,-3,6,1,6);
   }
   case "hex_netrunner" -> {
    p(h,"vr_shell",0,-4.7f,-7,-5.5f,9.4f,4,1.5f);p(h,"vr_screen",5,-4,-6.3f,-5.7f,8,2.5f,.3f);
    p(b,"server",0,-3.5f,0,2,7,9,3);
    for(int i=0;i<4;i++){p(b,"server_slot"+i,6,-3,1+i*1.8f,5,6,1,.5f);p(b,"led"+i,3,2,1+i*1.8f,5.5f,.5f,.5f,.2f);}
    p(l,"keyboard",1,-2,3,-4,6,5,1);p(l,"screen",5,-1.5f,3.5f,-4.3f,5,3,.3f);
    for(int i=0;i<3;i++)p(h,"cable"+i,1,-2+i*2,-2,4,1,5,1);
   }
   case "patch_medic" -> {
    p(b,"case_latch",2,-1,4,6,2,2,.7f);p(b,"cross_v",3,-.6f,1.5f,6.2f,1.2f,6,.4f);p(b,"cross_h",3,-2.4f,3.8f,6.2f,4.8f,1.3f,.4f);
    for(int i=0;i<3;i++){p(b,"vial"+i,5,-3.5f+i*2.5f,5,-4.4f,1.3f,3,1.3f);p(b,"cap"+i,2,-3.5f+i*2.5f,4.5f,-4.4f,1.3f,.6f,1.3f);}
    p(l,"injector",0,3,4,-2,1.5f,5,2);p(l,"needle",2,3.5f,9,-1.5f,.5f,2,.5f);
    p(h,"lamp",3,-1,-7.5f,-5,2,1.5f,.6f);
   }
   case "iron_colossus" -> {
    p(b,"thorax",0,-6,0,-4,12,8,2);p(b,"reactor_casing",2,-3,1,-5,6,6,1.5f);p(b,"reactor_core",3,-1.6f,2.3f,-5.3f,3.2f,3.2f,.4f);
    p(r,"siege_shoulder",0,-6,-4,-4,8,5,8);p(l,"siege_shoulder",0,-2,-4,-4,8,5,8);
    for(var leg:new PartDefinition[]{rl,ll}){p(leg,"piston",2,2,0,1,1.3f,9,1.3f);p(leg,"leg_shell",0,-3,3,-3.5f,6,7,2);}
    for(int i=0;i<3;i++)p(b,"radiator"+i,6,-4,1+i*2.5f,6,8,1.5f,1);
    p(b,"exhaust_l",1,-5,-5,3,2,7,2);p(b,"exhaust_r",1,3,-5,3,2,7,2);
   }
   case "neon_bomber" -> {
    for(int i=0;i<3;i++)p(b,"charge"+i,7,-4+i*3,1,2.5f,2.5f,9,4);
    p(b,"timer",3,-2,2,-4,4,3,1);p(h,"warning",3,-1,-10,-1,2,2,2);
   }
   case "mirage_stalker" -> {
    p(h,"hood",4,-5,-9,-4.5f,10,2,9);
    for(int i=0;i<4;i++)p(b,"cloak"+i,4,-4+i*2,1,2.5f,1.8f,15-i%2,1);
    p(b,"phase_core",3,-2,3,3.5f,4,4,1);
   }
   case "signal_hacker" -> {
    p(b,"router",0,-4,0,2,8,9,4);p(h,"antenna_l",2,-5,-13,1,1,10,1);p(h,"antenna_r",2,4,-11,1,1,8,1);
    p(l,"terminal",5,-2,3,-4,6,5,1);p(h,"visor",3,-4,-6,-5,8,2,1);
   }
   default -> throw new IllegalArgumentException(id);
  }
 }
}
