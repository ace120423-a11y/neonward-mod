package jp.neonward;
import java.util.*;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.core.registries.BuiltInRegistries;

/** Separate meshes per garment and slot; children follow their owner's animated limb. */
public final class TailoredModels {
 private static final Map<String,HumanoidModel<HumanoidRenderState>> CACHE=new HashMap<>();
 public static HumanoidModel<HumanoidRenderState> get(HumanoidRenderState state,EquipmentSlot slot){
  if(state.isBaby)return null;
  var item=switch(slot){case HEAD->state.headEquipment;case CHEST->state.chestEquipment;case LEGS->state.legsEquipment;case FEET->state.feetEquipment;default->null;};
  if(item==null||item.isEmpty())return null;var id=BuiltInRegistries.ITEM.getKey(item.getItem());if(!id.getNamespace().equals("neonward"))return null;
  for(String style:StreetFashion.IDS)if(id.getPath().startsWith(style+"_"))return CACHE.computeIfAbsent(style+slot,k->build(style,slot));return null;
 }
 static void cube(PartDefinition parent,String name,int u,int v,float x,float y,float z,float w,float h,float d,PartPose pose){parent.addOrReplaceChild(name,CubeListBuilder.create().texOffs(u,v).addBox(x,y,z,w,h,d),pose);}
 static HumanoidModel<HumanoidRenderState> build(String style,EquipmentSlot slot){
  boolean kimono=style.equals("kimono")||style.equals("ronin"),leather=style.equals("leather")||style.equals("nomad"),denim=style.equals("denim"),suit=style.equals("executive")||style.equals("velvet");
  var mesh=HumanoidModel.createMesh(new CubeDeformation(slot==EquipmentSlot.LEGS?.16f:kimono?.4f:.3f),0);var root=mesh.getRoot();var body=root.getChild("body");
  if(slot==EquipmentSlot.FEET){root.addOrReplaceChild("right_leg",kimono?CubeListBuilder.create():CubeListBuilder.create().texOffs(0,16).addBox(-2,leather?4:7,-2,4,leather?7:4,4,new CubeDeformation(.35f)),PartPose.offset(-1.9f,12,0));root.addOrReplaceChild("left_leg",kimono?CubeListBuilder.create():CubeListBuilder.create().texOffs(0,16).mirror().addBox(-2,leather?4:7,-2,4,leather?7:4,4,new CubeDeformation(.35f)),PartPose.offset(1.9f,12,0));}
  // Raised collar sections frame the neck instead of painting a collar onto the chest.
  cube(body,"collar_back",0,32,-3,-1,1.8f,6,2.5f,1,PartPose.ZERO);
  cube(body,"collar_left",0,32,0,0,0,2.2f,3.8f,.65f,PartPose.offsetAndRotation(-3,-.8f,-2.55f,.08f,0,-.35f));
  cube(body,"collar_right",0,32,0,0,0,2.2f,3.8f,.65f,PartPose.offsetAndRotation(.8f,-1.4f,-2.58f,.08f,0,.35f));
  if(kimono){
   cube(body,"overlap",0,40,0,0,0,1.2f,10,.55f,PartPose.offsetAndRotation(2,-.1f,-2.55f,0,0,.5f));
   cube(body,"obi",16,32,-4.65f,8,-2.9f,9.3f,3,5.8f,PartPose.ZERO);
   cube(body,"obi_knot",34,32,-2,8.2f,2.9f,4,2.6f,1.7f,PartPose.ZERO);
   cube(body,"hem_left",0,48,-4.5f,11,-2.7f,4.4f,8,5.4f,PartPose.ZERO);
   cube(body,"hem_right",0,48,.1f,11,-2.7f,4.4f,8,5.4f,PartPose.ZERO);
   for(String arm:List.of("right_arm","left_arm")){boolean left=arm.startsWith("left");cube(root.getChild(arm),"wide_sleeve",20,46,left?-1.5f:-3.5f,2,-2.7f,5,8,5.4f,PartPose.ZERO);}
  }else{
   for(int side:new int[]{-1,1}){
    cube(body,"pocket_"+side,32,40,side<0?-3.7f:1.0f,denim?3:6,-2.8f,2.7f,2.5f,.65f,PartPose.ZERO);
    cube(body,"flap_"+side,44,40,side<0?-3.8f:.9f,denim?2.7f:5.7f,-3,2.9f,.65f,.8f,PartPose.ZERO);
   }
   cube(body,"waistband",0,56,-4.35f,10.6f,-2.55f,8.7f,1.1f,5.1f,PartPose.ZERO);
   if(leather){
    cube(body,"diagonal_zip",52,32,0,0,0,.45f,10,.4f,PartPose.offsetAndRotation(2.7f,.8f,-2.85f,0,0,.37f));
    cube(body,"buckle",48,42,-.6f,10.3f,-3.1f,2.2f,1.5f,.55f,PartPose.ZERO);
    cube(body,"belt_tail",52,45,1.4f,11.2f,-2.8f,.8f,3,.3f,PartPose.ZERO);
   }
   if(suit)cube(body,"lapel",0,40,0,0,0,1.5f,7,.5f,PartPose.offsetAndRotation(-3,.5f,-2.65f,0,0,-.3f));
   for(String arm:List.of("right_arm","left_arm")){boolean left=arm.startsWith("left");cube(root.getChild(arm),"cuff",0,56,left?-1.3f:-3.3f,8.5f,-2.35f,4.6f,1.5f,4.7f,PartPose.ZERO);}
  }
  for(String leg:List.of("right_leg","left_leg")){
   if(slot==EquipmentSlot.LEGS&&kimono)cube(root.getChild(leg),"hakama",0,16,-2.45f,0,-2.45f,4.9f,11.4f,4.9f,PartPose.ZERO);
   if(slot==EquipmentSlot.FEET){
    if(kimono){
     cube(root.getChild(leg),"setta_sole",40,56,-2.25f,11.45f,-3,4.5f,.65f,5.6f,PartPose.ZERO);
     cube(root.getChild(leg),"thong_left",52,32,0,0,0,.55f,.65f,3,PartPose.offsetAndRotation(-1.7f,10.9f,-.4f,0,.45f,0));
     cube(root.getChild(leg),"thong_right",52,32,0,0,0,.55f,.65f,3,PartPose.offsetAndRotation(1.2f,10.9f,-.4f,0,-.45f,0));
    }else{
     cube(root.getChild(leg),"boot_toe",20,56,-2.35f,9.2f,-3.1f,4.7f,2.5f,5.5f,PartPose.ZERO);
     cube(root.getChild(leg),"sole",40,56,-2.4f,11.5f,-3.2f,4.8f,leather?.35f:.8f,leather?3.2f:5.7f,PartPose.ZERO);
     if(leather)cube(root.getChild(leg),"heel",40,56,-1.5f,11.3f,.6f,3,1.1f,2,PartPose.ZERO);
     if(denim)for(int lace=0;lace<3;lace++)cube(root.getChild(leg),"lace_"+lace,52,32,-1.8f,7.5f+lace*.7f,-2.5f,3.6f,.22f,.28f,PartPose.ZERO);
    }
   }
  }
  var model=new HumanoidModel<HumanoidRenderState>(LayerDefinition.create(mesh,64,64).bakeRoot());
  model.head.visible=slot==EquipmentSlot.HEAD;model.hat.visible=false;
  model.body.visible=slot==EquipmentSlot.CHEST||slot==EquipmentSlot.LEGS;
  // Torso details belong exclusively to the jacket, so mixed outfits do not overlap.
  if(slot!=EquipmentSlot.CHEST)for(var part:model.body.getAllParts())if(part!=model.body)part.visible=false;
  model.leftArm.visible=model.rightArm.visible=slot==EquipmentSlot.CHEST;
  model.leftLeg.visible=model.rightLeg.visible=slot==EquipmentSlot.LEGS||slot==EquipmentSlot.FEET;
  return model;
 }
}
