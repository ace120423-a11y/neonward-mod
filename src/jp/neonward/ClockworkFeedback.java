package jp.neonward;
import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.nbt.TagParser;
import net.minecraft.util.ProblemReporter;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.*;
import net.minecraft.sounds.*;
import com.mojang.math.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import jp.neonward.mixin.*;
/** The visible instruments read exactly the same server state as each puzzle's solution. */
public final class ClockworkFeedback {
 static final Map<Integer,Long> OPENING=new HashMap<>(),ACTIVITY=new HashMap<>();
 static void activity(ServerLevel l,int f){ACTIVITY.put(f,l.getGameTime());}
 static int key(int f,int stage){return f*3+stage;}
 static boolean opening(int f,int stage){return OPENING.containsKey(key(f,stage));}
 static int pressure(ClockworkPuzzles.State s){return s.valves[0]+s.valves[1]*2+s.valves[2]*3;}
 static void begin(ServerLevel l,int f,int stage){OPENING.put(key(f,stage),l.getGameTime());door(l,f,stage,0);l.playSound(null,59,DungeonLayout.base(f)+1,ClockworkPuzzles.DOOR[stage],SoundEvents.PISTON_EXTEND,SoundSource.BLOCKS,.45f,.8f);}
 static void tick(ServerLevel l,int f){
  var s=ClockworkPuzzles.state(f);int y=DungeonLayout.base(f);
  for(int stage=0;stage<3;stage++){
   int z=ClockworkPuzzles.DOOR[stage];if(l.players().stream().noneMatch(p->p.distanceToSqr(59,y+1,z-8)<64*64)&&!opening(f,stage))continue;boolean done=s.stage>stage,enabled=s.stage>=stage;
   int value=stage==0?pressure(s):stage==1?s.sequence:s.power;
   int target=stage==1?4:ClockworkPuzzles.target(f,stage);
   boolean ready=enabled&&value==target;
   ClockworkHintBoards.tick(l,f,stage);
   for(int i=0;i<5;i++)console(l,f,stage,i,y);
   if(ClockworkPuzzles.kind(f,stage)>=0){
    ClockworkVariants.render(l,f,stage,enabled,done);
    var start=OPENING.get(key(f,stage));if(start!=null)door(l,f,stage,(int)(l.getGameTime()-start));
    else if(done){String stale=tag("lift_door_"+stage,f);for(var d:l.getEntitiesOfClass(Display.BlockDisplay.class,new AABB(53,y,z-2,66,y+11,z+2),e->e.entityTags().contains(stale)))d.discard();}
    continue;
   }
   float input=enabled?(stage==0?Math.min(.01f,value*.0006f):value>0?.007f:0f):0f;
   ClockworkMachinery.gear(l,43.5,y+13,z-.7,9,0,1,input);
   ClockworkMachinery.gear(l,50.7,y+13,z-.7,6.6f,0,-1,ready||done?.007f:0f);
   // Three pressure risers / four electric branches display each input separately.
   int count=stage==0?3:4;
   for(int i=0;i<count;i++){
    int amount=stage==0?s.valves[i]:stage==1?s.sequence>i?1:0:(s.power&(1<<i))!=0?1:0;
    if(!enabled)amount=0;
    int x=46+6*i;
    if(stage==0)ClockworkMachinery.handwheel(l,x+.5,y+2.05,ClockworkPuzzles.STATION[stage]+.08,amount);
    for(int pz=ClockworkPuzzles.STATION[stage]+1;pz<z;pz++)block(l,x,y,pz,amount>0?Blocks.OCHRE_FROGLIGHT:ClockworkInterior.COPPER);
    for(int h=1;h<=4;h++)block(l,x,y+h,z-1,amount>=(stage==0?h:1)?Blocks.OCHRE_FROGLIGHT:ClockworkInterior.COPPER);
    String label=stage==0?"弁 "+(i+1)+"  "+amount+" ×"+(i+1):stage==1?ClockworkPuzzles.SYMBOL[ClockworkPuzzles.code(f)[i]]+"  "+(amount>0?"接続":"停止"):(1<<i)+"  "+(amount>0?"通電":"OFF");
    text(l,"input_"+stage+"_"+i,f,x+.5,y+5.2,z-1.3,label,amount>0?0xffd478:0x829095,.65f);
    if(stage==1){
     ClockworkMachinery.gear(l,75+6*i,y+7,z-.8,6,0,i%2==0?1:-1,amount>0?.007f:0f);
     text(l,"coupling_"+i,f,75+6*i,y+11,z-1.2,(i+1)+". "+ClockworkPuzzles.SYMBOL[ClockworkPuzzles.code(f)[i]],amount>0?0x80ffae:0xffd478,.8f);
     if(i<3)for(int xx=77+6*i;xx<=79+6*i;xx++)block(l,xx,y+7,z-1,s.sequence>i+1?Blocks.OCHRE_FROGLIGHT:ClockworkInterior.OLD);
    }
   }
   if(stage!=1){
    // Interpolated liquid/energy level and a fixed target marker, both visible from the controls.
    cube(l,"meter_back_"+stage,f,75,y+9,z-1.1,"polished_blackstone",24,1.7f,.4f,0);
    cube(l,"meter_fill_"+stage,f,75,y+9.3,z-1.4,"ochre_froglight",Math.max(.02f,enabled?value:0),1,.22f,0);
    cube(l,"meter_target_"+stage,f,75+target,y+8.5,z-1.65,"red_concrete",.16f,2.7f,.2f,0);
   }
   String title=stage==0?"蒸気圧":stage==1?"動力伝達":"供給電力";
   String status=!enabled?"動力未接続":done?"扉へ動力供給中":ready?"一致：確定で扉へ送る":stage==1?"点灯した接続の先へ動力が伝わる":value>target?"過剰：入力を減らす":"不足：入力を増やす";
   text(l,"meter_title_"+stage,f,85,y+15,z-1.5,title+"  "+(enabled?value:0)+" / "+target+"\n"+status,ready||done?0x80ffae:0xffd478,1.1f);
   // Main shaft to the door lights only when sufficient power reaches the output.
   for(int xx=51;xx<=59;xx++)block(l,xx,y+9,z-1,ready||done?Blocks.OCHRE_FROGLIGHT:ClockworkInterior.COPPER);
   if(enabled&&value>0&&stage==0&&l.getGameTime()-ACTIVITY.getOrDefault(f,-1000L)<24)for(int x:new int[]{15,101})ClockworkMachinery.steamPulse(l,x+.5,y+8,15.5,value>=target,(int)(l.getGameTime()-ACTIVITY.getOrDefault(f,-1000L)));
   var started=OPENING.get(key(f,stage));if(started!=null)door(l,f,stage,(int)(l.getGameTime()-started));
   else if(done){final String stale=tag("lift_door_"+stage,f);for(var d:l.getEntitiesOfClass(Display.BlockDisplay.class,new AABB(53,y,z-2,66,y+11,z+2),e->e.entityTags().contains(stale)))d.discard();}
  }
 }
 static void console(ServerLevel l,int f,int stage,int i,int y){
  int x=46+6*i,z=ClockworkPuzzles.STATION[stage];var pos=new BlockPos(x,y+2,z);
  if(!l.isPositionEntityTicking(pos))return;
  // Replace only the former console's standing sign; retain the interactive block and saved puzzle state.
  if(l.getBlockState(pos).getBlock() instanceof StandingSignBlock)l.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
  if(stage==0&&i<3){String old="nw_gear_at_"+(x+.5)+"_"+(y+1.3)+"_"+(z-.65);
   for(var e:l.getEntitiesOfClass(Display.BlockDisplay.class,new AABB(x-1,y,z-2,x+2,y+3,z+1),e->e.entityTags().contains(old)))e.discard();}
  String label=stage==0?(i<3?"圧力弁 ×"+(i+1):i==3?"圧力計":"圧力を確定"):stage==1?(i<4?ClockworkPuzzles.SYMBOL[i]:"最初から"):(i<4?"動力 "+(1<<i):"動力を確定");
  String detail=stage==0?"目標圧力 "+ClockworkPuzzles.target(f,0):stage==2?"必要動力 "+ClockworkPuzzles.target(f,2):"図面の順に操作";
  int kind=ClockworkPuzzles.kind(f,stage);if(kind>=0){label=i==4?"動力を確定":new String[]{"回転管 ","連動ラック ","圧力移送 ","反射鏡 "}[kind]+(i+1);detail=i==4?"接続を確認する":"設定 "+ClockworkPuzzles.state(f).devices[stage][i];}
  String id=tag("console_v2_"+stage+"_"+i,f);var d=ClockworkDisplays.find(l,id,new Vec3(x+.5,y+2.55,z+.145),Display.TextDisplay.class);if(d==null)return;
  if(!ClockworkDisplays.initialized(d)){
   load(d,l,"{text:{text:''},billboard:'fixed',Rotation:[180f,0f],line_width:180,background:0,brightness:{block:15,sky:15},Invulnerable:1b,Tags:['"+id+"']}");
   d.setPos(x+.5,y+2.55,z+.145);ClockworkDisplays.transform(d,new Transformation(new Vector3f(),new Quaternionf(),new Vector3f(.22f),new Quaternionf()));
  }
  var next=Component.literal(label+"\n"+detail+"\n右クリックで操作").withColor(0xffd478);var a=(MeterTextAccess)d;if(!a.neonGetText().equals(next))a.neonSetText(next);ClockworkDisplays.publish(l,d);
 }
 static void block(ServerLevel l,int x,int y,int z,Block b){var pos=new BlockPos(x,y,z);if(!l.getBlockState(pos).is(b))l.setBlock(pos,b.defaultBlockState(),3);}
 static void door(ServerLevel l,int f,int stage,int elapsed){
  int y=DungeonLayout.base(f)+1,z=ClockworkPuzzles.DOOR[stage];float rise=Math.max(0,Math.min(7.2f,(elapsed-20)*7.2f/40));
  if(elapsed<20){double x=51+8*Math.max(0,elapsed)/20.0;l.sendParticles(new DustParticleOptions(0xffd478,1.3f),x,y+8,z-1.5,3,.1,.1,.1,0);}
  for(int x=54;x<=64;x++)for(int dy=0;dy<7;dy++)block(l,x,y+dy,z,dy+1<=rise?Blocks.AIR:Blocks.BARRIER);
  cube(l,"lift_door_"+stage,f,54,y,z-.12,"waxed_cut_copper",11,7,1,rise);

  if(elapsed>=60){
   for(int x=54;x<=64;x++)for(int dy=0;dy<7;dy++)block(l,x,y+dy,z,Blocks.AIR);
   for(var d:l.getEntitiesOfClass(Display.BlockDisplay.class,new AABB(53,y-1,z-2,66,y+9,z+2),e->e.entityTags().contains(tag("lift_door_"+stage,f))))d.discard();
   OPENING.remove(key(f,stage));
  }
 }
 static String tag(String id,int f){return "nw_feedback_"+f+"_"+id;}
 static void load(Entity e,ServerLevel l,String nbt){try{e.load(TagValueInput.create(ProblemReporter.DISCARDING,l.registryAccess(),TagParser.parseCompoundFully(nbt)));}catch(Exception ex){throw new IllegalStateException(ex);}}
 static Display.BlockDisplay cube(ServerLevel l,String id,int f,double x,double y,double z,String block,float sx,float sy,float sz,float rise){
  var at=new Vec3(x,y,z);String tag=tag(id,f);
  var d=ClockworkDisplays.find(l,tag,at,Display.BlockDisplay.class);if(d==null)return null;
  if(!ClockworkDisplays.initialized(d)){load(d,l,"{block_state:{Name:'minecraft:"+block+"'},Invulnerable:1b,width:30f,height:20f,view_range:2f,interpolation_duration:4,Tags:['"+tag+"']}");d.setPos(at);}
  ClockworkDisplays.transform(d,new Transformation(new Vector3f(0,rise,0),new Quaternionf(),new Vector3f(sx,sy,sz),new Quaternionf()));ClockworkDisplays.publish(l,d);return d;
 }
 static void text(ServerLevel l,String id,int f,double x,double y,double z,String value,int color,float scale){
  var at=new Vec3(x,y,z);String tag=tag(id,f);var d=ClockworkDisplays.find(l,tag,at,Display.TextDisplay.class);if(d==null)return;
  if(!ClockworkDisplays.initialized(d)){load(d,l,"{text:{text:''},billboard:'center',line_width:300,background:0,brightness:{block:15,sky:15},Invulnerable:1b,Tags:['"+tag+"']}");d.setPos(at);ClockworkDisplays.transform(d,new Transformation(new Vector3f(),new Quaternionf(),new Vector3f(scale),new Quaternionf()));}
  var access=(MeterTextAccess)d;var next=Component.literal(value).withColor(color);if(!access.neonGetText().equals(next))access.neonSetText(next);ClockworkDisplays.publish(l,d);
 }
}
