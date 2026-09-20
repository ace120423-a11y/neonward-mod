package jp.neonward;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import com.mojang.math.Transformation;
import org.joml.Vector3f;
import org.joml.Quaternionf;
/** Four distinct mechanisms, with visible paths computed from the authoritative puzzle rules. */
final class ClockworkVariants {
 static final Map<Display,Float> HEIGHTS=new WeakHashMap<>(),ROTATIONS=new WeakHashMap<>();
 static final Map<Display,Long> TIMES=new WeakHashMap<>();
 static void render(ServerLevel l,int f,int stage,boolean enabled,boolean done){
  int k=ClockworkPuzzles.kind(f,stage),y=DungeonLayout.base(f),z=ClockworkPuzzles.DOOR[stage];int[] a=ClockworkPuzzles.state(f).devices[stage];
  boolean ready=MechanismRules.solved(k,a,f,stage);
  String root="variant_"+stage+"_";
  // These slots are assigned only to newly visited floors, so no existing solved room is replaced.
  ClockworkFeedback.text(l,root+"title",f,84,y+16,z-1.5,MechanismRules.title(k)+"\n"+(!enabled?"前の機関から動力を接続":done?"復旧済み":ready?"接続完了：操作盤で確定":"装置の動きを見て調整"),ready?0x80ffae:0xffd478,1.0f);
  if(k==MechanismRules.PIPE)pipes(l,f,stage,a,enabled);
  if(k==MechanismRules.RACK)racks(l,f,stage,a,enabled);
  if(k==MechanismRules.STEAM)lifts(l,f,stage,a,enabled);
  if(k==MechanismRules.LIGHT)optics(l,f,stage,a,enabled);
  // A separate output cable powers up only when the complete mechanism is connected.
  line(l,f,root+"out",new Vec3(73,y+13,z-1.3),new Vec3(59,y+13,z-1.3),"minecraft:cut_copper",.22f,true);
  line(l,f,root+"out_live",new Vec3(73,y+13,z-1.55),new Vec3(59,y+13,z-1.55),"minecraft:ochre_froglight",.10f,enabled&&(ready||done));
 }
 static void pipes(ServerLevel l,int f,int s,int[] a,boolean enabled){
  int y=DungeonLayout.base(f),z=ClockworkPuzzles.DOOR[s],seed=MechanismRules.seed(f,s),flow=enabled?MechanismRules.pipeFlow(a,f,s):0;String root="variant_"+s+"_pipe_";
  for(int i=0;i<4;i++){
   int[] p=MechanismRules.rotate(MechanismRules.PIPE_POS[i][0],MechanismRules.PIPE_POS[i][1],seed);var at=new Vec3(84+p[0]*2.5,y+9+p[1]*2.5,z-1.2);
   rotating(l,f,root+i,at,"neonward:clockwork_pipe_"+(i==0?"straight":"elbow"),5,-a[i]*(float)Math.PI/2);
   ClockworkFeedback.text(l,root+"label_"+i,f,at.x,at.y+.65,at.z-.5,(i+1)+"",0xffffff,.6f);
   int mask=MechanismRules.mask(i,a[i]);
   for(int d=0;d<4;d++){var end=at.add(new double[]{0,2.5,0,-2.5}[d],new double[]{2.5,0,-2.5,0}[d],-.3);line(l,f,root+"flow_"+i+"_"+d,at.add(0,0,-.3),end,"minecraft:sea_lantern",.16f,i<flow&&(mask&(1<<d))!=0);}
  }
  int[] in=MechanismRules.rotate(-3,1,seed),out=MechanismRules.rotate(-1,-3,seed),first=MechanismRules.rotate(-1,1,seed),last=MechanismRules.rotate(-1,-1,seed);
  var inlet=new Vec3(84+in[0]*2.5,y+9+in[1]*2.5,z-1.2);var outlet=new Vec3(84+out[0]*2.5,y+9+out[1]*2.5,z-1.2);
  line(l,f,root+"in",inlet,new Vec3(84+first[0]*2.5,y+9+first[1]*2.5,z-1.2),"minecraft:cut_copper",.5f,true);
  line(l,f,root+"end",outlet,new Vec3(84+last[0]*2.5,y+9+last[1]*2.5,z-1.2),"minecraft:cut_copper",.5f,true);
  ClockworkFeedback.text(l,root+"source",f,inlet.x,inlet.y+.5,inlet.z-.5,"入口",0x67fff3,.65f);
  ClockworkFeedback.text(l,root+"exit",f,outlet.x,outlet.y+.5,outlet.z-.5,"出口",flow==4?0x80ffae:0xffce72,.65f);
 }
 static void racks(ServerLevel l,int f,int s,int[] a,boolean enabled){
  int y=DungeonLayout.base(f),z=ClockworkPuzzles.DOOR[s],prefix=0;while(prefix<4&&a[prefix]==1)prefix++;String root="variant_"+s+"_rack_";
  line(l,f,root+"axis",new Vec3(72,y+8,z-1),new Vec3(98,y+8,z-1),"minecraft:gold_block",.12f,true);
  for(int i=0;i<4;i++){
   double x=77+5*i;line(l,f,root+"rail_"+i,new Vec3(x,y+4,z-.5),new Vec3(x,y+12,z-.5),"minecraft:polished_blackstone",.4f,true);
   var anchor=new Vec3(x,y+8,z-1.4);var d=display(l,f,root+i,anchor,"neonward:clockwork_handwheel");if(d==null)continue;
   long now=l.getGameTime(),dt=Math.max(0,Math.min(4,now-TIMES.getOrDefault(d,now)));float target=(a[i]-1)*3,old=HEIGHTS.getOrDefault(d,target),height=approach(old,target,.20f*dt);HEIGHTS.put(d,height);TIMES.put(d,now);
   float angle=ROTATIONS.getOrDefault(d,0f);if(enabled&&i<prefix&&Math.abs(height)<.01f)angle+=(i%2==0?1:-1)*.025f*dt;ROTATIONS.put(d,angle);
   pose(l,d,new Vector3f(0,height,0),new Quaternionf().rotationZ(angle),new Vector3f(5));
   ClockworkFeedback.text(l,root+"label_"+i,f,x,y+13,z-1.5,(i+1)+"："+(a[i]==1?"軸一致":"軸ずれ"),i<prefix?0x80ffae:0xffce72,.65f);
  }
 }
 static void lifts(ServerLevel l,int f,int s,int[] a,boolean enabled){
  int y=DungeonLayout.base(f),z=ClockworkPuzzles.DOOR[s];String root="variant_"+s+"_lift_";int[] targets=MechanismRules.stair(f,s);
  for(int i=0;i<4;i++){
   int x=75+4*i,pz=z-5;var d=display(l,f,root+i,new Vec3(x,y+.8,pz),"minecraft:cut_copper");if(d==null)continue;
   long now=l.getGameTime(),dt=Math.max(0,Math.min(4,now-TIMES.getOrDefault(d,now)));float old=HEIGHTS.getOrDefault(d,(float)a[i]),height=approach(old,a[i],.06f*dt);HEIGHTS.put(d,height);TIMES.put(d,now);
   boolean stopped=Math.abs(height-a[i])<.001f;
   for(int xx=x;xx<x+3;xx++)for(int zz=pz;zz<pz+3;zz++)for(int h=1;h<=3;h++){
    var bp=new BlockPos(xx,y+h,zz);var current=l.getBlockState(bp);if(stopped&&h==a[i]){if(current.isAir())l.setBlock(bp,Blocks.BARRIER.defaultBlockState(),3);}else if(current.is(Blocks.BARRIER))l.setBlock(bp,Blocks.AIR.defaultBlockState(),3);
   }
   if(height!=old)for(var p:l.players())if(!p.isSpectator()&&!p.getAbilities().flying&&p.getX()>x-.2&&p.getX()<x+3.2&&p.getZ()>pz-.2&&p.getZ()<pz+3.2&&Math.abs(p.getY()-(y+1+old))<.5){p.teleportTo(l,p.getX(),y+1+height,p.getZ(),Set.of(),p.getYRot(),p.getXRot(),true);p.setDeltaMovement(0,0,0);p.fallDistance=0;}
   pose(l,d,new Vector3f(0,height,0),new Quaternionf(),new Vector3f(3,.2f,3));
   line(l,f,root+"piston_"+i,new Vec3(x+1.5,y+1,pz+1.5),new Vec3(x+1.5,y+.8+height,pz+1.5),"minecraft:iron_block",.6f,height>.25);
   line(l,f,root+"target_"+i,new Vec3(x,y+1+targets[i],pz+3.3),new Vec3(x+3,y+1+targets[i],pz+3.3),"minecraft:sea_lantern",.12f,true);
   ClockworkFeedback.text(l,root+"label_"+i,f,x+1.5,y+6,pz+2,(i+1)+"："+a[i]+" / "+targets[i],a[i]==targets[i]?0x80ffae:0xffce72,.7f);
   if(enabled&&height!=old)ClockworkMachinery.steamPulse(l,x+1.5,y+1,pz+3,true,(int)(now%24));
  }
 }
 static void optics(ServerLevel l,int f,int s,int[] a,boolean enabled){
  int y=DungeonLayout.base(f),z=ClockworkPuzzles.DOOR[s],seed=MechanismRules.seed(f,s);String root="variant_"+s+"_light_";
  for(int i=0;i<4;i++){
   int[] p=MechanismRules.rotate(MechanismRules.LIGHT_POS[i][0],MechanismRules.LIGHT_POS[i][1],seed);var at=new Vec3(84+p[0]*3,y+9+p[1]*3,z-1.3);
   rotating(l,f,root+"mirror_"+i,at,"neonward:clockwork_mirror",2,a[i]==0?(float)Math.PI/4:-(float)Math.PI/4);
   ClockworkFeedback.text(l,root+"label_"+i,f,at.x,at.y+1.8,at.z-.2,"鏡 "+(i+1),0xffffff,.65f);
  }
  var beam=MechanismRules.light(a,f,s);
  for(int i=0;i<16;i++){
   boolean show=enabled&&i<beam.rays().size();var ray=show?beam.rays().get(i):new MechanismRules.Ray(0,0,0,0);
   line(l,f,root+"beam_"+i,new Vec3(84+ray.x1()*3,y+9+ray.y1()*3,z-1.65),new Vec3(84+ray.x2()*3,y+9+ray.y2()*3,z-1.65),"minecraft:ochre_froglight",.10f,show);
  }
  int[] source=MechanismRules.rotate(-3,-1,seed),receiver=MechanismRules.rotate(3,-1,seed);
  ClockworkFeedback.text(l,root+"source",f,84+source[0]*3,y+10+source[1]*3,z-1.5,"光源",0xffd478,.7f);
  var at=new Vec3(84+receiver[0]*3,y+9+receiver[1]*3,z-1.3);
  part(l,f,root+"receiver",at,"minecraft:gold_block",new Vector3f(-.6f,-.6f,0),new Quaternionf(),new Vector3f(1.2f,1.2f,.4f));
  ClockworkFeedback.text(l,root+"target",f,at.x,at.y+1,at.z-.5,beam.hit()&&enabled?"受光：接続完了":"受光器",beam.hit()&&enabled?0x80ffae:0xffce72,.7f);
 }
 static float approach(float a,float b,float step){return a+Math.max(-step,Math.min(step,b-a));}
 static Display.BlockDisplay display(ServerLevel l,int f,String id,Vec3 at,String block){
  String tag=ClockworkFeedback.tag(id,f);var d=ClockworkDisplays.find(l,tag,at,Display.BlockDisplay.class);if(d==null)return null;
  if(!ClockworkDisplays.initialized(d)){ClockworkFeedback.load(d,l,"{block_state:{Name:'"+block+"'},brightness:{block:15,sky:15},Invulnerable:1b,width:40f,height:24f,view_range:2f,interpolation_duration:4,Tags:['"+tag+"']}");d.setPos(at);}return d;
 }
 static void pose(ServerLevel l,Display.BlockDisplay d,Vector3f offset,Quaternionf rotation,Vector3f scale){ClockworkDisplays.transform(d,new Transformation(offset,rotation,scale,new Quaternionf()));ClockworkDisplays.publish(l,d);}
 static void part(ServerLevel l,int f,String id,Vec3 at,String block,Vector3f offset,Quaternionf rotation,Vector3f scale){var d=display(l,f,id,at,block);if(d!=null)pose(l,d,offset,rotation,scale);}
 static void rotating(ServerLevel l,int f,String id,Vec3 at,String block,float size,float target){
  var d=display(l,f,id,at,block);if(d==null)return;long now=l.getGameTime(),dt=Math.max(0,Math.min(4,now-TIMES.getOrDefault(d,now)));float a=ROTATIONS.getOrDefault(d,target),delta=target-a,turn=(float)(2*Math.PI);delta-=turn*(float)Math.floor((delta+Math.PI)/turn);if(Math.abs(delta)>.00001)a+=Math.max(-.12f*dt,Math.min(.12f*dt,delta));ROTATIONS.put(d,a);TIMES.put(d,now);pose(l,d,new Vector3f(),new Quaternionf().rotationZ(a),new Vector3f(size));
 }
 static void line(ServerLevel l,int f,String id,Vec3 from,Vec3 to,String block,float width,boolean visible){
  // Use a stable room origin: endpoints may change, but an existing display's position does not.
  var origin=new Vec3(84,DungeonLayout.base(f)+9,ClockworkPuzzles.DOOR[Integer.parseInt(id.split("_")[1])]-1.5);
  var delta=to.subtract(from);float length=(float)delta.length();var rotation=length>.0001?new Quaternionf().rotationTo(new Vector3f(1,0,0),new Vector3f((float)delta.x,(float)delta.y,(float)delta.z).normalize()):new Quaternionf();
  var offset=rotation.transform(new Vector3f(0,-width/2,-width/2)).add((float)(from.x-origin.x),(float)(from.y-origin.y),(float)(from.z-origin.z));
  part(l,f,id,origin,block,offset,rotation,new Vector3f(visible?length:0,width,width));
 }
}
