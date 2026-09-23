package jp.neonward;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Ordinary native cubes (Sodium-compatible), never entities or inventory items. */
public final class ShrineRitualMeshes {
 static final ModelPart[][][] ARMS=new ModelPart[2][2][3];
 static ModelPart ladle,handle;
 static ModelPart bake(CubeListBuilder cubes,int width,int height){var mesh=new MeshDefinition();mesh.getRoot().addOrReplaceChild("mesh",cubes,PartPose.ZERO);return LayerDefinition.create(mesh,width,height).bakeRoot();}
 static ModelPart arm(boolean left,boolean slim,int part){
  int side=left?1:0,size=slim?1:0;if(ARMS[side][size][part]!=null)return ARMS[side][size][part];
  float w=slim?3:4;var b=CubeListBuilder.create().texOffs(left?32:40,(left?48:16)+(part==1?6:0));
  if(part<2)b.addBox(-w/2,0,-2,w,6,4);else b.addBox(-w/2,-.7f,-2,w,1.4f,4);
  return ARMS[side][size][part]=bake(b,64,64);
 }
 static void submit(ModelPart part,PoseStack pose,SubmitNodeCollector nodes,int light,Identifier skin){nodes.submitModelPart(part,pose,RenderTypes.entityCutout(skin),light,OverlayTexture.NO_OVERLAY,null);}
 static void segment(PoseStack pose,SubmitNodeCollector nodes,int light,Identifier skin,ModelPart part,Vec3 from,Vec3 to){
  var d=to.subtract(from);float length=(float)d.length();if(length<.001)return;
  pose.pushPose();pose.translate(from.x,from.y,from.z);pose.mulPose(new Quaternionf().rotationTo(new Vector3f(0,1,0),new Vector3f((float)d.x,(float)d.y,(float)d.z).normalize()));pose.scale(1,length/(6f/16),1);submit(part,pose,nodes,light,skin);pose.popPose();
 }
 public static void first(PoseStack pose,SubmitNodeCollector nodes,AbstractClientPlayer player,int light,ShrineRitualClient.Frame f){
  var skin=player.getSkin();boolean slim=skin.model()==PlayerModelType.SLIM;
  float fade=Math.min(1,Math.min(f.age()/12,(f.kind()==0?200-f.age():160-f.age())/12));fade=Math.max(0,fade);fade=fade*fade*(3-2*fade);
  for(int side:new int[]{1,-1}){
   boolean left=side<0;float pitch=left?f.leftPitch():f.rightPitch();float roll=left?f.leftRoll():f.rightRoll();float raise=Math.max(0,Math.min(1,-pitch/1.5f));
   var shoulder=new Vec3(side*.44,-.70,-.08);
   var wrist=new Vec3(side*(.26-.205*f.join()),-.66+raise*.38-f.bow()*.12-(1-fade)*.25,-.43-raise*.27);
   var elbow=shoulder.lerp(wrist,.46).add(side*.035,-.10,.045);
   segment(pose,nodes,light,skin.body().texturePath(),arm(left,slim,0),shoulder,elbow);
   segment(pose,nodes,light,skin.body().texturePath(),arm(left,slim,1),elbow,wrist);
   pose.pushPose();pose.translate(wrist.x,wrist.y,wrist.z);pose.mulPose(Axis.ZP.rotation(f.kind()==0?side*(float)Math.PI/2:roll));
   submit(arm(left,slim,2),pose,nodes,light,skin.body().texturePath());pose.popPose();
   if(f.ladle()==side){
    pose.pushPose();pose.translate(wrist.x,wrist.y+.02,wrist.z);
    // Mesh opening is -Y (player-model space). Camera space is +Y up.
    // Rz(+side*pour) tips +Y toward -X for right, +X for left: inward.
    pose.mulPose(Axis.ZP.rotation(side*f.pour()*.85f));pose.mulPose(Axis.YP.rotationDegrees(side*10));
    pose.mulPose(Axis.ZP.rotationDegrees(180));drawLadle(pose,nodes,light);pose.popPose();
   }
  }
 }
 public static void drawLadle(PoseStack pose,SubmitNodeCollector nodes,int light){
  if(ladle==null){
   var b=CubeListBuilder.create().texOffs(0,0);
   // Hollow stepped octagonal bowl, 6px diameter; long wooden handle.
   b.addBox(-2,0,-16,4,.45f,6);b.addBox(-3,0,-15,1,.45f,4);b.addBox(2,0,-15,1,.45f,4);
   b.addBox(-2,-2,-16,4,2,.5f);b.addBox(-2,-2,-10.5f,4,2,.5f);
   b.addBox(-3,-2,-15,.5f,2,4);b.addBox(2.5f,-2,-15,.5f,2,4);
   for(float x:new float[]{-2.5f,2})for(float z:new float[]{-15.5f,-11})b.addBox(x,-2,z,.5f,2,.5f);
   ladle=bake(b,32,32);var h=CubeListBuilder.create().texOffs(0,0).addBox(-.35f,-.4f,-10,.7f,.7f,13);
   for(int z:new int[]{-8,-3,2})h.addBox(-.43f,-.48f,z,.86f,.86f,.18f);handle=bake(h,32,32);
  }
  submit(ladle,pose,nodes,light,NeonWard.id("textures/block/sakura_board.png"));submit(handle,pose,nodes,light,NeonWard.id("textures/block/sakura_bamboo.png"));
 }
 private ShrineRitualMeshes(){}
}
