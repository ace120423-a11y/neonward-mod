package jp.neonward;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.resources.Identifier;

public class CyberEnemyRenderer extends ZombieRenderer {
 private final Identifier texture;
 CyberEnemyRenderer(EntityRendererProvider.Context context,ModelLayerLocation layer,String id){super(context,layer,layer,ModelLayers.ZOMBIE_ARMOR,ModelLayers.ZOMBIE_BABY_ARMOR);texture=NeonWard.id("textures/entity/"+id+".png");addLayer(new net.minecraft.client.renderer.entity.layers.EyesLayer<ZombieRenderState,net.minecraft.client.model.monster.zombie.ZombieModel<ZombieRenderState>>(this){public net.minecraft.client.renderer.rendertype.RenderType renderType(){return net.minecraft.client.renderer.rendertype.RenderTypes.eyes(NeonWard.id("textures/entity/"+id+"_glow.png"));}});}
 @Override public Identifier getTextureLocation(ZombieRenderState state){return texture;}
 @Override protected HumanoidModel.ArmPose getArmPose(net.minecraft.world.entity.monster.zombie.Zombie mob,net.minecraft.world.entity.HumanoidArm arm){if(mob instanceof CyberEnemy enemy&&enemy.kind().interval()>0&&mob.isAggressive())return HumanoidModel.ArmPose.CROSSBOW_HOLD;return super.getArmPose(mob,arm);}
 public static void init(){
  for(var k:HostileRoster.ALL){var layer=new ModelLayerLocation(NeonWard.id(k.id()),"main");ModelLayerRegistry.registerModelLayer(layer,()->mesh(k));String skin=switch(k.id()){case "neon_bomber"->"pile_breaker";case "mirage_stalker"->"ghost_sniper";case "signal_hacker"->"hex_netrunner";default->k.id();};EntityRendererRegistry.register(NeonHostiles.TYPES.get(k.id()),ctx->new CyberEnemyRenderer(ctx,layer,skin));}
 }
 static void box(PartDefinition parent,String name,int u,int v,float x,float y,float z,float w,float h,float d){parent.addOrReplaceChild(name,CubeListBuilder.create().texOffs(u,v).addBox(x,y,z,w,h,d),PartPose.ZERO);}
 static LayerDefinition mesh(HostileRoster.Kind k){
  var mesh=HumanoidModel.createMesh(CubeDeformation.NONE,0);var root=mesh.getRoot();var head=root.getChild("head");var body=root.getChild("body");var ra=root.getChild("right_arm");var la=root.getChild("left_arm");
  box(head,"optic_frame",64,0,-4.3f,-6.5f,-4.6f,8.6f,2.5f,1);
  box(head,"respirator",64,16,-2.5f,-3.5f,-5.3f,5,2.8f,2);
  box(body,"chest_plate",64,32,-4.4f,1,-3,8.8f,6,1.5f);
  box(body,"utility_belt",64,48,-4.6f,9,-2.8f,9.2f,2,5.6f);
  box(body,"pouch",96,48,-4,7,-4.5f,2.7f,3,2);
  box(root.getChild("right_leg"),"knee",96,64,-2.2f,4,-2.8f,4.4f,3,1.5f);
  box(root.getChild("left_leg"),"knee",96,64,-2.2f,4,-2.8f,4.4f,3,1.5f);
  if(k.armor()>=5){box(ra,"pauldron",64,64,-3.8f,-2.6f,-2.8f,5.6f,4,5.6f);box(la,"pauldron",64,64,-1.8f,-2.6f,-2.8f,5.6f,4,5.6f);}
  if(k.armor()>=10){box(body,"power_pack",64,82,-4,-1,2,8,10,4);box(ra,"gauntlet",96,80,-3.5f,5,-2.6f,5,5,5.2f);box(la,"gauntlet",96,80,-1.5f,5,-2.6f,5,5,5.2f);}
  if(k.id().equals("chrome_ronin")){box(head,"crest",96,0,-.7f,-11,-3,1.4f,4,6);box(body,"coat_left",0,80,-4.4f,10,-2.5f,3.5f,6,5);box(body,"coat_right",0,80,.9f,10,-2.5f,3.5f,6,5);}
  if(k.id().equals("scrap_raider")){box(head,"mohawk",96,0,-1,-11,-3,2,4,6);box(la,"scrap_guard",64,64,-1.5f,3,-3,5,5,1);}
  if(k.id().equals("neon_runner")){box(head,"ear_fin",96,0,4,-7,0,1,6,5);box(body,"spine",96,80,-1,-1,2,2,12,2);}
  if(k.id().equals("ghost_sniper")){box(head,"scope",96,16,-4,-6,-7,3,3,3);box(body,"cloak",0,80,-5,1,2.4f,10,17,1);}
  if(k.id().equals("belt_gunner")){box(body,"ammo_box",64,82,-4,1,2,8,9,5);for(int i=0;i<5;i++)box(body,"round"+i,96,0,-3.5f+i*1.4f,7,-3.5f,.8f,2.5f,1);}
  if(k.id().equals("arc_trooper")){box(body,"coil_l",64,82,-5,-2,2,3,12,3);box(body,"coil_r",64,82,2,-2,2,3,12,3);box(head,"rod",96,0,3,-12,1,.8f,5,.8f);}
  if(k.id().equals("hex_netrunner")){box(head,"antenna",96,0,-4.8f,-12,0,1,8,1);box(la,"terminal",64,32,-1.5f,4,-3.6f,5,4,1.5f);}
  if(k.id().equals("patch_medic")){box(body,"medical_pack",64,82,-4,1,2,8,9,4);box(head,"beacon",96,0,-1.5f,-10,-1.5f,3,2,3);}
  if(k.id().equals("pile_breaker")){box(body,"exhaust",96,80,2,-4,2,2,13,3);box(la,"piston",96,80,3,0,-1,1.5f,9,2);}
  if(k.id().equals("iron_colossus")){box(head,"crown_l",96,0,-5,-12,0,2,6,3);box(head,"crown_r",96,0,3,-12,0,2,6,3);box(body,"reactor",64,16,-2,2,-4,4,4,2);}
  EnemyDetailModels.decorate(root,k.id());
  return LayerDefinition.create(mesh,512,512);
 }
}
