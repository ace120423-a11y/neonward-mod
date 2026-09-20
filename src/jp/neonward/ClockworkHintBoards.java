package jp.neonward;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import com.mojang.math.Transformation;
import org.joml.*;
import jp.neonward.mixin.MeterTextAccess;
/** Rigid, double-sided information screens; puzzle clues never scroll or flicker. */
final class ClockworkHintBoards {
 static void tick(ServerLevel l,int f,int stage){
  int[] code=ClockworkPuzzles.code(f);String[] symbols=ClockworkPuzzles.SYMBOL;
  String title=stage==0?"蒸気圧の調整":stage==1?"歯車の共鳴":"動力の配分";
  String body=stage==0?"弁の値を 0〜4 で調整\n左から ×1・×2・×3\n合計を目標圧力へ":stage==1?symbols[code[0]]+" → "+symbols[code[1]]+"\n"+symbols[code[2]]+" → "+symbols[code[3]]+"\nこの順で起動する":"1・2・4・8 の回路\n必要な回路だけ ON\n合計 "+ClockworkPuzzles.target(f,2)+" に調整";
  int kind=ClockworkPuzzles.kind(f,stage);if(kind>=0){title=MechanismRules.title(kind);body=MechanismRules.hint(kind,f,stage);}
  board(l,f,59,ClockworkPuzzles.STATION[stage]-4,title,body,false);
  board(l,f,52,ClockworkPuzzles.DOOR[stage]-2,"機関通路・警戒","曲がり角に敵影あり\n奥の大広間を目指す\n復旧で扉が開く",true);
 }
 static void board(ServerLevel l,int f,int x,int z,String title,String body,boolean warning){
  int y=DungeonLayout.base(f);var pos=new BlockPos(x,y+2,z);if(!l.isPositionEntityTicking(pos))return;
  if(l.getBlockState(pos).getBlock() instanceof StandingSignBlock)l.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
  String id="hint_v1_"+x+"_"+z;
  // A single detailed cabinet supports the screen and conceals the old pedestal.
  String baseTag=ClockworkFeedback.tag(id+"_cabinet",f);var baseAt=new Vec3(x,y+1,z);
  var cabinet=ClockworkDisplays.find(l,baseTag,baseAt,Display.BlockDisplay.class);
  if(cabinet!=null){
   if(!ClockworkDisplays.initialized(cabinet)){
    ClockworkFeedback.load(cabinet,l,"{block_state:{Name:'neonward:clockwork_hint_terminal'},Invulnerable:1b,width:4f,height:4f,view_range:2f,Tags:['"+baseTag+"']}");cabinet.setPos(baseAt);
    ClockworkDisplays.transform(cabinet,new Transformation(new Vector3f(),new Quaternionf(),new Vector3f(1),new Quaternionf()));
   }
   ClockworkDisplays.publish(l,cabinet);
  }
  for(var old:l.getEntitiesOfClass(Display.BlockDisplay.class,new net.minecraft.world.phys.AABB(x-1,y,z-1,x+2,y+4,z+2),e->e.entityTags().contains(ClockworkFeedback.tag(id+"_stand",f))))old.discard();
  cube(l,f,id+"_case",x-.9,y+2.1,z+.25,"waxed_cut_copper",2.8f,1.5f,.5f);
  cube(l,f,id+"_front",x-.78,y+2.22,z+.20,"black_concrete",2.56f,1.26f,.08f);
  cube(l,f,id+"_back",x-.78,y+2.22,z+.72,"black_concrete",2.56f,1.26f,.08f);
  for(int i=0;i<2;i++)cube(l,f,id+"_lamp_"+i,x-.86+2.66*i,y+2.3,z+.16,warning?"ochre_froglight":"sea_lantern",.08f,1.1f,.68f);
  for(int side=0;side<2;side++){
   String tag=ClockworkFeedback.tag(id+"_text_"+side,f);var at=new Vec3(x+.5,y+2.48,z+(side==0?.12:.88));
   var d=ClockworkDisplays.find(l,tag,at,Display.TextDisplay.class);if(d==null)continue;
   if(!ClockworkDisplays.initialized(d)){
    ClockworkFeedback.load(d,l,"{text:{text:''},billboard:'fixed',Rotation:["+(side==0?180:0)+"f,0f],line_width:240,background:0,brightness:{block:15,sky:15},Invulnerable:1b,Tags:['"+tag+"']}");
    d.setPos(at);ClockworkDisplays.transform(d,new Transformation(new Vector3f(),new Quaternionf(),new Vector3f(.65f),new Quaternionf()));
   }
   var value=Component.literal(title+"\n").withColor(warning?0xffce72:0x67fff3).append(Component.literal(body).withColor(0xe6f7f4));
   var a=(MeterTextAccess)d;if(!a.neonGetText().equals(value))a.neonSetText(value);ClockworkDisplays.publish(l,d);
  }
 }
 static void cube(ServerLevel l,int f,String id,double x,double y,double z,String block,float sx,float sy,float sz){ClockworkFeedback.cube(l,id,f,x,y,z,block,sx,sy,sz,0);}
}
