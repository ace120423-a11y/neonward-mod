package jp.neonward;
import net.fabricmc.fabric.api.client.rendering.v1.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;

public class CityResidentRenderer extends HumanoidMobRenderer<CityResident,HumanoidRenderState,HumanoidModel<HumanoidRenderState>> {
 final Identifier skin;
 CityResidentRenderer(EntityRendererProvider.Context c,ModelLayerLocation layer,String id){super(c,new HumanoidModel<>(c.bakeLayer(layer)),.45f);skin=NeonWard.id("textures/entity/"+id+".png");}
 @Override public HumanoidRenderState createRenderState(){return new HumanoidRenderState();}
 @Override public Identifier getTextureLocation(HumanoidRenderState s){return skin;}
 static void part(PartDefinition p,String n,int mat,float x,float y,float z,float w,float h,float d){p.addOrReplaceChild(n,CubeListBuilder.create().texOffs(64+(mat%3)*64,(mat/3)*64).addBox(x,y,z,w,h,d),PartPose.ZERO);}
 static LayerDefinition mesh(int role){var m=HumanoidModel.createMesh(CubeDeformation.NONE,0);var r=m.getRoot();var head=r.getChild("head");var body=r.getChild("body");
  part(head,"hair_cap",0,-4.15f,-8.2f,-4.1f,8.3f,2.2f,8.2f);part(head,"fringe",0,-4.1f,-6.4f,-4.3f,role%2==0?3:6,1.6f,.6f);
  part(body,"belt",2,-4.2f,9.5f,-2.3f,8.4f,1.1f,4.6f);part(body,"buckle",3,-.7f,9.6f,-2.65f,1.4f,.8f,.4f);
  for(var leg:new PartDefinition[]{r.getChild("right_leg"),r.getChild("left_leg")}){part(leg,"sole",2,-2.1f,11,-2.5f,4.2f,1,4.8f);part(leg,"toe",2,-2.1f,9.6f,-2.6f,4.2f,1.5f,1.8f);}
  if(role==0||role==8||role==10||role==11){
   part(body,"lapel_l",1,-3.8f,.1f,-2.5f,2.2f,6,.6f);part(body,"lapel_r",1,1.6f,.1f,-2.5f,2.2f,6,.6f);part(body,"tie",role==8?3:4,-.45f,1,-2.8f,.9f,5.6f,.5f);
   part(body,"pocket",1,-3.4f,6.5f,-2.8f,2.1f,2,.7f);part(body,"pin",3,2.8f,1.3f,-2.8f,.7f,.7f,.3f);
  }
  if(role==1||role==7){part(body,"backpack",1,-4,0,2.1f,8,9,3.5f);part(body,"pack_pocket",2,-3,4,5.5f,6,4,1);for(float x:new float[]{-3,2})part(body,"strap"+x,2,x,0,-2.5f,1,9,.6f);part(head,"headset",3,4,-5,-1,1.3f,3,3);}
  if(role==2||role==10){part(head,"hair_back",0,-4,-6,3.5f,8,8,1.4f);part(head,"bun",0,1,-9,2.5f,3,3,3);part(body,"skirt",1,-4.4f,10,-2.8f,8.8f,4,5.6f);part(head,"earring",3,4,-2,-.5f,.45f,1.5f,.45f);}
  if(role==3){part(body,"kimono_left",1,-4.3f,0,-2.8f,4.4f,10,.8f);part(body,"obi",4,-4.5f,6.8f,-3,9,3,6);part(body,"hem_l",1,-4.3f,10,-2.6f,4.2f,6,5.2f);part(body,"hem_r",1,.1f,10,-2.6f,4.2f,6,5.2f);part(head,"hair_knot",0,-2,-10,-1,4,3,3);}
  if(role==4){part(head,"cap",1,-4.6f,-8.6f,-4.6f,9.2f,2,9);part(head,"visor",2,-4.4f,-7,-6.5f,8.8f,.7f,3);part(body,"toolbelt",2,-4.5f,8.8f,-3.2f,9,2.2f,6);for(int i=0;i<3;i++)part(body,"wrench"+i,3,-3+i*2,7.5f,-3.9f,.6f,4,.5f);}
  if(role==5){part(body,"apron",1,-4.15f,4,-2.8f,8.3f,11,.7f);part(body,"apron_pocket",2,-2.5f,9,-3.4f,5,3,.6f);part(body,"bowtie",3,-1.5f,1,-2.5f,3,1,.6f);}
  if(role==6||role==9){part(body,"jacket",1,-4.4f,.2f,-2.5f,8.8f,9,5);part(body,"zip",3,-.2f,.5f,-2.7f,.4f,8,.3f);part(head,"glasses",2,-4.2f,-5.3f,-4.4f,8.4f,1.8f,.7f);for(var arm:new PartDefinition[]{r.getChild("right_arm"),r.getChild("left_arm")})part(arm,"cuff",2,-2,7.5f,-2.2f,4,2,4.4f);}
  if(role==7||role==9){part(body,"coat_l",1,-4.6f,8,-2.8f,3.8f,10,5.6f);part(body,"coat_r",1,.8f,8,-2.8f,3.8f,10,5.6f);part(body,"patched_pocket",2,-4.7f,11,-3.2f,3,3,.6f);}
  if(role==8||role==9){for(int i=0;i<5;i++)part(body,"chain"+i,3,-2+i,1.8f+Math.abs(i-2)*-.3f,-3,1,.4f,.4f);part(body,"holster",2,3.5f,8,0,2,4,2.8f);part(head,"temple_implant",3,-4.4f,-3,-1,.5f,2,2);}
  if(role==10||role==11){part(head,"mic_ear",2,4,-5,-1,1,2,2);part(head,"mic_boom",3,4,-3,-4,.4f,.4f,4);part(body,"badge",4,2,4,-3,1.5f,2,.35f);}
  return LayerDefinition.create(m,256,256);
 }
 public static void init(){for(var k:CityResidents.ALL){var layer=new ModelLayerLocation(NeonWard.id(k.id()),"main");ModelLayerRegistry.registerModelLayer(layer,()->mesh(k.role()));EntityRendererRegistry.register(CityResidents.TYPES.get(k.role()),ctx->new CityResidentRenderer(ctx,layer,k.id()));}}
}
