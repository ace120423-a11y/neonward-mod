package jp.neonward;

import java.util.*;
import com.google.gson.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.phys.Vec3;

/** Server-authorized cosmetic timelines. No camera, item, position or gameplay mutations. */
public final class ShrineRitualClient {
 static final int LIMIT=64;
 static final Map<UUID,Motion> ACTIVE=new HashMap<>();
 static ClientLevel world;static boolean initialized;
 static final class Motion {
  final int kind,duration;final long start;int last;
  Motion(int kind,int elapsed,int duration,long now){this.kind=kind;this.duration=duration;start=now-elapsed;last=elapsed;}
 }
 public record Frame(int kind,float age,float bow,float rightPitch,float rightYaw,float rightRoll,float leftPitch,float leftYaw,float leftRoll,float join,float pour,int ladle){}
 // tick, bow, R pitch/yaw/roll, L pitch/yaw/roll, palms together, water pour.
 static final float[][] PRAYER={
  {0,0,0,0,0,0,0,0,0,0},{20,.65f,.1f,0,0,.1f,0,0,0,0},{40,0,0,0,0,0,0,0,0,0},
  {60,.65f,.1f,0,0,.1f,0,0,0,0},{80,0,0,0,0,0,0,0,0,0},
  {98,0,-1.2f,-.15f,.1f,-1.2f,.15f,-.1f,0,0},
  {108,0,-1.25f,-.55f,.05f,-1.25f,.55f,-.05f,1,0},
  {116,0,-1.2f,-.08f,.1f,-1.2f,.08f,-.1f,0,0},
  {124,0,-1.25f,-.55f,.05f,-1.25f,.55f,-.05f,1,0},
  {132,0,-1.3f,-.52f,.05f,-1.3f,.52f,-.05f,1,0},
  {164,0,-1.3f,-.52f,.05f,-1.3f,.52f,-.05f,1,0},
  {174,0,0,0,0,0,0,0,0,0},{188,.65f,.1f,0,0,.1f,0,0,0,0},{200,0,0,0,0,0,0,0,0,0}};
 static final float[][] WASH={
  {0,0,0,0,0,0,0,0,0,0},{18,.12f,-1.15f,-.1f,.1f,-.4f,.1f,0,0,0},
  {30,.22f,-.95f,-.05f,.1f,-.5f,.1f,-.15f,0,0},
  {42,0,-1.5f,-.3f,.1f,-1.1f,.3f,-1.1f,.3f,0},
  {52,0,-1.5f,-.3f,-.85f,-1.1f,.3f,-1.1f,.3f,1},
  {68,0,-1.5f,-.3f,-.85f,-1.1f,.3f,-1.1f,.3f,1},
  {78,0,-1.3f,-.5f,0,-1.3f,.5f,0,.8f,0},
  {88,0,-1.1f,-.3f,1.1f,-1.5f,.3f,-.1f,.3f,0},
  {98,0,-1.1f,-.3f,1.1f,-1.5f,.3f,.85f,.3f,1},
  {118,0,-1.1f,-.3f,1.1f,-1.5f,.3f,.85f,.3f,1},
  {128,0,-1.3f,-.5f,0,-1.3f,.5f,0,.8f,0},
  {140,.15f,-1.1f,-.1f,.1f,-.4f,.1f,0,0,0},
  {150,.05f,-.6f,0,0,-.2f,0,0,0,0},{160,0,0,0,0,0,0,0,0,0}};
 public static void initClient(){
  if(initialized)return;initialized=true;
  ClientPlayNetworking.registerGlobalReceiver(ShrineRituals.Motion.TYPE,(packet,ctx)->ctx.client().execute(()->receive(packet.json())));
  ClientPlayConnectionEvents.DISCONNECT.register((h,mc)->clear());
  ClientTickEvents.END_CLIENT_TICK.register(ShrineRitualClient::tick);
 }
 public static void clear(){ACTIVE.clear();world=null;}
 public static void receive(String json){
  var mc=Minecraft.getInstance();if(mc.level==null)return;
  if(world!=mc.level){clear();world=mc.level;}
  try{
   if(json.length()>512)return;var o=JsonParser.parseString(json).getAsJsonObject();var uuid=UUID.fromString(o.get("uuid").getAsString());
   if(!o.get("active").getAsBoolean()){ACTIVE.remove(uuid);return;}
   int kind=o.get("kind").getAsInt(),elapsed=o.get("elapsed").getAsInt(),duration=o.get("duration").getAsInt();
   if(kind<0||kind>1||duration!=(kind==0?200:160)||elapsed<0||elapsed>=duration)return;
   if(ACTIVE.size()>=LIMIT&&!ACTIVE.containsKey(uuid))return;
   ACTIVE.put(uuid,new Motion(kind,elapsed,duration,world.getGameTime()));
  }catch(RuntimeException ignored){/* Malformed cosmetic packet must not interrupt rendering. */}
 }
 public static Frame frame(UUID uuid,float delta){
  var mc=Minecraft.getInstance();if(mc.level==null||world!=mc.level)return null;
  var m=ACTIVE.get(uuid);if(m==null)return null;float age=world.getGameTime()-m.start+delta;
  return age<0||age>=m.duration?null:sample(m.kind,age);
 }
 public static boolean active(UUID uuid){return frame(uuid,0)!=null;}
 public static Frame sample(int kind,float age){
  var keys=kind==0?PRAYER:WASH;int i=0;while(i<keys.length-2&&age>keys[i+1][0])i++;
  var a=keys[i];var b=keys[i+1];float t=Math.max(0,Math.min(1,(age-a[0])/(b[0]-a[0])));t=t*t*(3-2*t);
  float[] v=new float[9];for(int j=0;j<9;j++)v[j]=a[j+1]+(b[j+1]-a[j+1])*t;
  int ladle=kind==1&&age>=18&&age<145?(age>=82&&age<130?-1:1):0;
  return new Frame(kind,age,v[0],v[1],v[2],v[3],v[4],v[5],v[6],v[7],v[8],ladle);
 }
 public static void extract(Avatar player,AvatarRenderState state,float delta){
  var f=player.isAlive()&&!player.isSpectator()?frame(player.getUUID(),delta):null;
  ((ShrineRitualPose)state).neonward$ritual(f);
  if(f!=null){
   state.leftArmPose=HumanoidModel.ArmPose.EMPTY;state.rightArmPose=HumanoidModel.ArmPose.EMPTY;state.attackTime=0;state.isUsingItem=false;
   // These are disposable render snapshots, not the player's real inventory.
   // Also makes weapon/glove pose checks fail closed regardless of their tail ordering.
   state.leftHandItemStack=net.minecraft.world.item.ItemStack.EMPTY;state.rightHandItemStack=net.minecraft.world.item.ItemStack.EMPTY;
  }
 }
 public static Frame state(ArmedEntityRenderState s){return s instanceof ShrineRitualPose carrier?carrier.neonward$ritual():null;}
 public static void pose(HumanoidModel<?> model,HumanoidRenderState state){
  var f=state(state);if(f==null)return;
  model.body.xRot=f.bow();model.body.yRot=0;model.body.zRot=0;
  model.body.y=12-12*(float)Math.cos(f.bow());model.body.z=-12*(float)Math.sin(f.bow());
  model.head.xRot=f.bow()*.7f;model.head.yRot=0;model.head.zRot=0;
  model.head.y=model.body.y;model.head.z=model.body.z;
  model.rightArm.xRot=f.rightPitch()+f.bow();model.rightArm.yRot=f.rightYaw();model.rightArm.zRot=f.rightRoll();
  model.leftArm.xRot=f.leftPitch()+f.bow();model.leftArm.yRot=f.leftYaw();model.leftArm.zRot=f.leftRoll();
  // Rotate shoulder pivots with the torso; vanilla resets every part next setupAnim.
  model.rightArm.y=model.leftArm.y=model.body.y+2*(float)Math.cos(f.bow());model.rightArm.z=model.leftArm.z=model.body.z+2*(float)Math.sin(f.bow());
  model.rightLeg.xRot=model.leftLeg.xRot=0;model.rightLeg.yRot=model.leftLeg.yRot=0;
  // 26.2 hat/sleeves/jacket are children, so they inherit these transforms directly.
 }
 public static boolean firstPerson(float delta,PoseStack pose,SubmitNodeCollector nodes,AbstractClientPlayer player,int light){
  var f=player.isAlive()&&!player.isSpectator()?frame(player.getUUID(),delta):null;if(f==null)return false;
  if(!CameraScreen.active()&&!player.isInvisible())ShrineRitualMeshes.first(pose,nodes,player,light,f);
  return true;
 }
 static void tick(Minecraft mc){
  if(mc.level!=world){clear();world=mc.level;}if(world==null||mc.player==null||mc.isPaused())return;
  long now=world.getGameTime();int particles=0,sounds=0;
  var players=new HashMap<UUID,AbstractClientPlayer>();for(var p:world.players())players.put(p.getUUID(),p);
  var it=ACTIVE.entrySet().iterator();while(it.hasNext()){
   var e=it.next();var m=e.getValue();int age=(int)(now-m.start);var p=players.get(e.getKey());
   if(age>=m.duration||age<0||p!=null&&(!p.isAlive()||p.isSpectator()||p.isRemoved())){it.remove();continue;}
   if(p!=null&&p.distanceToSqr(mc.player)<=32*32){
    var look=p.getLookAngle();var at=p.position().add(look.x*.55,1.15,look.z*.55);
    if(m.kind==0){for(int clap:new int[]{108,124})if(m.last<clap&&age>=clap&&sounds++<8)world.playLocalSound(at.x,at.y,at.z,SoundEvents.PLAYER_ATTACK_NODAMAGE,SoundSource.PLAYERS,.45f,1.65f,false);}
    else{
     for(int pour:new int[]{52,98})if(m.last<pour&&age>=pour&&sounds++<8)world.playLocalSound(at.x,at.y,at.z,SoundEvents.BUCKET_EMPTY,SoundSource.PLAYERS,.18f,1.3f,false);
     if(sample(1,age).pour()>.7f&&age%3==0&&age!=m.last&&particles<24){for(int j=0;j<2;j++){world.addParticle(ParticleTypes.SPLASH,at.x+j*.025,at.y-.12,at.z,0,-.035,0);particles++;}}
    }
   }
   m.last=age;
  }
 }
 private ShrineRitualClient(){}
}
