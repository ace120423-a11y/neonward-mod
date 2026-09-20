package jp.neonward;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.nbt.TagParser;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.phys.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.*;
import com.mojang.math.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import jp.neonward.mixin.LiftDisplayAccess;
/** Native rotating display meshes; only machinery near occupied floors is animated. */
public final class ClockworkMachinery {
 static final Map<Display.BlockDisplay,Float> ANGLES=new WeakHashMap<>();
 static final Map<Display.BlockDisplay,Long> WHEEL_TICKS=new WeakHashMap<>();
 static final String TAG="nw_clockwork_gear";
 public static void init(){
  var id=NeonWard.id("clockwork_gear");Registry.register(BuiltInRegistries.BLOCK,id,new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK,id)).noCollision().noOcclusion().strength(-1,3600000)));
  var wheelId=NeonWard.id("clockwork_handwheel");Registry.register(BuiltInRegistries.BLOCK,wheelId,new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK,wheelId)).noCollision().noOcclusion().strength(-1,3600000)));
  var terminalId=NeonWard.id("clockwork_hint_terminal");Registry.register(BuiltInRegistries.BLOCK,terminalId,new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK,terminalId)).noCollision().noOcclusion().strength(-1,3600000)));
  for(String name:new String[]{"clockwork_pipe_straight","clockwork_pipe_elbow","clockwork_mirror"}){var meshId=NeonWard.id(name);Registry.register(BuiltInRegistries.BLOCK,meshId,new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK,meshId)).noCollision().noOcclusion().strength(-1,3600000)));}
  ServerTickEvents.END_SERVER_TICK.register(s->{if(s.getTickCount()%8!=0)return;var l=s.getLevel(NeonZones.TOWER);
   if(l!=null&&NightSpire.progress!=null){var floors=new HashSet<Integer>();for(var p:l.players())if(SpireSite.contains(l,p.blockPosition()))floors.add(DungeonLayout.floor(p.getY()));
    for(int f:floors){if(NightSpire.progress.built<f)continue;int y=DungeonLayout.base(f);
     ClockworkFeedback.tick(l,f);
    }
   }
   var outside=s.overworld();if(outside.players().stream().anyMatch(p->p.distanceToSqr(755,180,243)<280*280)){
    for(boolean west:new boolean[]{false,true})for(int i=0;i<2;i++){
     double x=west?728.8:755.5,z=west?243.5:216.8;gear(outside,x,i==0?180.5:139.5,z,i==0?22:15,west?(float)(Math.PI/2):0,i==0?1:-1,true);
    }
    steam(outside,741.5,317,229.5,true);steam(outside,769.5,317,257.5,true);
   }
  });
 }
 static Display.BlockDisplay gear(ServerLevel l,double x,double y,double z,float size,float facing,int direction,boolean restored){
  return gear(l,x,y,z,size,facing,direction,restored?.007f:0f);
 }
 static Display.BlockDisplay gear(ServerLevel l,double x,double y,double z,float size,float facing,int direction,float rate){
  var center=new Vec3(x,y,z);if(l.players().stream().noneMatch(p->p.distanceToSqr(center)<180*180))return null;
  String id="nw_gear_at_"+x+"_"+y+"_"+z;
  var d=ClockworkDisplays.find(l,id,center,Display.BlockDisplay.class);if(d==null)return null;
  if(!ClockworkDisplays.initialized(d)){
   String nbt="{block_state:{Name:'neonward:clockwork_gear'},view_range:3f,width:32f,height:32f,shadow_radius:0f,interpolation_duration:4,Invulnerable:1b,Tags:['"+TAG+"','"+id+"']}";
   try{d.load(TagValueInput.create(ProblemReporter.DISCARDING,l.registryAccess(),TagParser.parseCompoundFully(nbt)));}catch(Exception e){throw new IllegalStateException(e);}d.setPos(center);
  }
  // Integrate rotation rather than switching the absolute rate, so restored gears don't jump.
  float angle=ANGLES.getOrDefault(d,0f)+direction*rate*4*(9/size);ANGLES.put(d,angle);
  // Entity yaw remains zero visually; BlockDisplay transformation carries the rotation.
  Quaternionf rotation=new Quaternionf().rotationY(facing).rotateZ(angle);
  Vector3f offset=rotation.transform(new Vector3f(-size*.5f,-size*.5f,-size*.5f));
  ClockworkDisplays.transform(d,new Transformation(offset,rotation,new Vector3f(size),new Quaternionf()));ClockworkDisplays.publish(l,d);
  return d;
 }
 static void handwheel(ServerLevel l,double x,double y,double z,int setting){
  var at=new Vec3(x,y,z);String id="nw_handwheel_v2_"+x+"_"+y+"_"+z;
  var d=ClockworkDisplays.find(l,id,at,Display.BlockDisplay.class);if(d==null)return;
  if(!ClockworkDisplays.initialized(d)){
   ClockworkFeedback.load(d,l,"{block_state:{Name:'neonward:clockwork_handwheel'},view_range:2f,width:2f,height:2f,shadow_radius:0f,interpolation_duration:4,Invulnerable:1b,Tags:['"+TAG+"','"+id+"']}");d.setPos(at);
  }
  long now=l.getGameTime(),previous=WHEEL_TICKS.getOrDefault(d,now);
  float angle=HandwheelMotion.advance(ANGLES.getOrDefault(d,setting*HandwheelMotion.STEP),setting,now-previous);
  ANGLES.put(d,angle);WHEEL_TICKS.put(d,now);
  // The mesh itself is centered at zero: interpolating rotation cannot move its shaft.
  ClockworkDisplays.transform(d,new Transformation(new Vector3f(),new Quaternionf().rotationZ(angle),new Vector3f(.62f),new Quaternionf()));ClockworkDisplays.publish(l,d);
  String old="nw_gear_at_"+x+"_"+y+"_"+z;
  for(var e:l.getEntitiesOfClass(Display.BlockDisplay.class,new AABB(x-1,y-1,z-1,x+1,y+1,z+1),e->e.entityTags().contains(old)))e.discard();
 }
 static void steamPulse(ServerLevel l,double x,double y,double z,boolean strong,int age){
  if(age<0||age>=24||age%8>=4||l.players().stream().noneMatch(p->p.distanceToSqr(x,y,z)<64*64))return;
  l.sendParticles(ParticleTypes.CLOUD,x,y,z,strong?2:1,.1,.2,.1,.02);
 }
 static void steam(ServerLevel l,double x,double y,double z,boolean strong){
  if(l.players().stream().noneMatch(p->p.distanceToSqr(x,y,z)<80*80))return;
  long t=l.getGameTime();boolean burst=t%100<(strong?36:12);if(!burst)return;
  l.sendParticles(ParticleTypes.CLOUD,x,y,z,strong?2:1,.12,.3,.12,strong?.055:.02);
  if(t%8==0)l.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,x,y,z,1,.08,.3,.08,.015);
  if(t%100<4)l.playSound(null,x,y,z,SoundEvents.FIRE_EXTINGUISH,SoundSource.BLOCKS,strong?.08f:.04f,1.6f);
 }
}
