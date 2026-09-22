package jp.neonward;
import java.util.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import com.mojang.math.Axis;

/** Camera-relative chain meshes; max 32 casts, 40 links each, lifetime under one second. */
public final class ChainPullClient implements ClientModInitializer {
 record Animation(ChainPull.Visual data,ClientLevel level,long received){}
 static final Map<Integer,Animation> ACTIVE=new HashMap<>();
 static ModelPart link,hook;
 public void onInitializeClient(){
  ClientPlayNetworking.registerGlobalReceiver(ChainPull.Visual.TYPE,(v,c)->c.client().execute(()->receive(v)));
  ClientPlayConnectionEvents.DISCONNECT.register((h,m)->ACTIVE.clear());
  ClientTickEvents.END_CLIENT_TICK.register(mc->ACTIVE.values().removeIf(a->mc.level!=a.level||a.level.getGameTime()-a.received>=ChainPull.DURATION||a.level.getEntity(a.data.owner())==null));
  LevelRenderEvents.COLLECT_SUBMITS.register(ChainPullClient::draw);
 }
 static void receive(ChainPull.Visual v){var mc=Minecraft.getInstance();if(v.cancel()){ACTIVE.remove(v.owner());return;}if(mc.level==null||ACTIVE.size()>=ChainPull.LIMIT&&!ACTIVE.containsKey(v.owner()))return;ACTIVE.put(v.owner(),new Animation(v,mc.level,mc.level.getGameTime()));}
 public static float phase(int owner,float delta){var a=ACTIVE.get(owner);return a==null?-1:(a.level.getGameTime()-a.received+delta)/ChainPull.DURATION;}
 static ModelPart mesh(boolean head){
  var mesh=new MeshDefinition();var b=CubeListBuilder.create().texOffs(0,0);
  if(head){b.addBox(-1.5f,-2,-1.5f,3,3,3);b.addBox(1,-4,-.6f,1,4,1.2f);b.addBox(-1,-4,-.6f,3,1,1.2f);}
  else {b.addBox(-.9f,-1.8f,-.25f,.4f,3.6f,.5f);b.addBox(.5f,-1.8f,-.25f,.4f,3.6f,.5f);b.addBox(-.5f,-1.8f,-.25f,1,.4f,.5f);b.addBox(-.5f,1.4f,-.25f,1,.4f,.5f);}
  mesh.getRoot().addOrReplaceChild("chain",b,PartPose.ZERO);return LayerDefinition.create(mesh,32,32).bakeRoot();
 }
 static void draw(LevelRenderContext context){
  var mc=Minecraft.getInstance();if(mc.level==null||ACTIVE.isEmpty())return;
  if(link==null){link=mesh(false);hook=mesh(true);}
  float delta=mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);var camera=context.levelState().cameraRenderState.pos;
  var nodes=context.submitNodeCollector();var pose=context.poseStack();
  for(var a:ACTIVE.values()){
   if(a.level!=mc.level)continue;var v=a.data;var entity=mc.level.getEntity(v.owner());if(!(entity instanceof LivingEntity owner)||!owner.isAlive()||owner.distanceToSqr(camera)>48*48)continue;
   float age=mc.level.getGameTime()-a.received+delta;if(age<0||age>=ChainPull.DURATION)continue;
   int side=owner.getMainArm()==HumanoidArm.RIGHT?1:-1;Vec3 look=owner.getViewVector(delta);Vec3 lateral=new Vec3(-look.z,0,look.x).normalize().scale(side*.30);
   Vec3 start=owner.getEyePosition(delta).add(0,-.35,0).add(lateral).add(look.scale(.22));
   var target=mc.level.getEntity(v.target());Vec3 end=target!=null&&target.isAlive()?target.getPosition(delta).add(0,target.getBbHeight()*.55,0):new Vec3(v.x(),v.y(),v.z());
   float amount=age<ChainPull.FLIGHT?age/ChainPull.FLIGHT:age<12?1:(ChainPull.DURATION-age)/6f;
   end=start.lerp(end,Math.max(0,Math.min(1,amount)));double length=end.distanceTo(start);if(length<.05||length>12)continue;
   int count=Math.min(40,Math.max(2,(int)Math.ceil(length/.20)));Vec3 direction=end.subtract(start).normalize();
   var rotation=new Quaternionf().rotationTo(new Vector3f(0,1,0),new Vector3f((float)direction.x,(float)direction.y,(float)direction.z));
   for(int i=0;i<=count;i++){
    double t=(double)i/count;var point=start.lerp(end,t);double sag=age<ChainPull.FLIGHT?Math.sin(t*Math.PI)*.08:0;
    pose.pushPose();pose.translate(point.x-camera.x,point.y-camera.y-sag,point.z-camera.z);pose.mulPose(rotation);
    if(i<count){pose.mulPose(Axis.YP.rotationDegrees(i%2*90));pose.scale(1,(float)(length/count/.20),1);}
    nodes.submitModelPart(i==count?hook:link,pose,RenderTypes.entityCutout(NeonWard.id("textures/item/"+(i==count?"arsenal_cyan":"arsenal_edge")+".png")),15728880,OverlayTexture.NO_OVERLAY,null);pose.popPose();
   }
  }
 }
}
