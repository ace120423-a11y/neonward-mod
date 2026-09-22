package jp.neonward;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
/** Arm-local shells, fitted around vanilla fists rather than held by a handle. */
public final class PairedGloves {
 static final ModelPart[][] PARTS=new ModelPart[2][3];
 static ModelPart build(boolean left,int layer){
  float x=left?-1.3f:-3.3f;
  var mesh=new MeshDefinition();var cubes=CubeListBuilder.create().texOffs(0,0);
  if(layer==0){cubes.addBox(x,4,-2.3f,4.6f,2,4.6f);cubes.addBox(x-.15f,6,-2.45f,4.9f,4.6f,4.9f);}
  else if(layer==1){for(int i=0;i<4;i++)cubes.addBox(x+i*1.15f,9,-2.9f,1.05f,1.8f,1);cubes.addBox(left?x+4.2f:x-.7f,6.5f,-1.3f,1.1f,2.6f,2.6f);}
  else{cubes.addBox(x+.45f,6.7f,-2.65f,3.7f,.6f,.25f);cubes.addBox(x+.45f,4.5f,2.32f,3.7f,.45f,.2f);}
  mesh.getRoot().addOrReplaceChild("shell",cubes,PartPose.ZERO);
  return LayerDefinition.create(mesh,32,32).bakeRoot();
 }
 public static void draw(PoseStack pose,SubmitNodeCollector nodes,int light,boolean left){
  int side=left?1:0;String[] textures={"arsenal_dark","arsenal_armor","arsenal_cyan"};
  for(int i=0;i<3;i++){
   if(PARTS[side][i]==null)PARTS[side][i]=build(left,i);
   nodes.submitModelPart(PARTS[side][i],pose,RenderTypes.entityCutout(NeonWard.id("textures/item/"+textures[i]+".png")),i==2?15728880:light,OverlayTexture.NO_OVERLAY,null);
  }
 }
}
