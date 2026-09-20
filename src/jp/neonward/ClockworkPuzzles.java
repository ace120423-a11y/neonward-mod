package jp.neonward;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
/** Server-authoritative cooperative mechanisms. Restored mechanisms and boss progress survive restarts. */
public final class ClockworkPuzzles {
 public static Block PANEL;
 static final int[] STATION={16,66,116},DOOR={24,74,124};
 static final String[] SYMBOL={"月","太陽","星","歯車"};
 public static class State {int stage;int[] valves=new int[3];int sequence;int power;int machineryVersion;int[][] devices;}
 static State state(int f){if(NightSpire.progress.puzzles==null)NightSpire.progress.puzzles=new HashMap<>();return NightSpire.progress.puzzles.computeIfAbsent(f,k->{var s=new State();s.machineryVersion=1;s.devices=new int[3][];for(int i=0;i<3;i++)s.devices[i]=MechanismRules.initial(MechanismRules.kind(f,i),f,i);return s;});}
 static int kind(int f,int stage){return state(f).machineryVersion<1?-1:MechanismRules.kind(f,stage);}
 static boolean solved(int f){return NightSpire.progress!=null&&state(f).stage>=3;}
 static int target(int f,int stage){return stage==0?8+f%12:3+f%12;}
 static int[] code(int f){return new int[]{f%4,(f+2)%4,(f+1)%4,(f+3)%4};}
 public static void init(){PANEL=NeonZones.terminal("clockwork_puzzle_panel");UseBlockCallback.EVENT.register((p,l,h,hit)->{
  if(!l.getBlockState(hit.getBlockPos()).is(PANEL))return InteractionResult.PASS;
  if(p instanceof ServerPlayer sp&&!p.isSpectator()&&SpireSite.contains(l,p.blockPosition())&&p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(hit.getBlockPos()))<=49)press(sp,hit.getBlockPos());
  return InteractionResult.SUCCESS;
 });}
 static void press(ServerPlayer p,BlockPos pos){
  if(NightSpire.progress==null)return;int f=DungeonLayout.floor(p.getY()),base=DungeonLayout.base(f)+1;
  int index=(pos.getX()-46)/6;if(pos.getY()!=base||index<0||index>4||pos.getX()!=46+index*6)return;
  int stage=-1;for(int i=0;i<3;i++)if(pos.getZ()==STATION[i])stage=i;if(stage<0)return;
  var s=state(f);if(s.stage>stage){p.sendOverlayMessage(Component.literal("この機関は復旧済み"));return;}if(s.stage!=stage){p.sendOverlayMessage(Component.literal("手前の機関から順番に復旧しよう"));return;}
  var before=NightSpire.JSON.toJson(s);boolean success=false;String message;
  int kind=kind(f,stage);
  if(kind>=0){
   MechanismRules.operate(kind,s.devices[stage],index);
   success=index==4&&MechanismRules.solved(kind,s.devices[stage],f,stage);
   message=MechanismRules.title(kind)+" ｜ "+Arrays.toString(s.devices[stage])+(success?"":" ｜ 装置のつながりを確認して確定");
  }else if(stage==0){
   if(index<3)s.valves[index]=(s.valves[index]+1)%5;
   int pressure=s.valves[0]+s.valves[1]*2+s.valves[2]*3;
   success=index==4&&pressure==target(f,0);message="蒸気圧 "+pressure+" / 目標 "+target(f,0)+" ｜ 弁 "+Arrays.toString(s.valves)+"（左から ×1 / ×2 / ×3）";
  }else if(stage==1){
   if(index<4){if(index==code(f)[s.sequence])s.sequence++;else s.sequence=0;}else s.sequence=0;
   success=s.sequence==4;message="共鳴装置 "+s.sequence+" / 4 ｜ 設計図："+String.join(" → ",Arrays.stream(code(f)).mapToObj(i->SYMBOL[i]).toList());
  }else{
   if(index<4)s.power^=1<<index;
   success=index==4&&s.power==target(f,2);message="動力 "+s.power+" / 必要量 "+target(f,2)+" ｜ 回路 1・2・4・8："+Integer.toBinaryString(16+s.power).substring(1);
  }
  if(success)s.stage++;
  try{NightSpire.save();}catch(Exception e){System.err.println("[Clockwork] Save rollback: "+e);NightSpire.progress.puzzles.put(f,NightSpire.JSON.fromJson(before,State.class));p.sendSystemMessage(Component.literal("保存できなかったため操作を戻しました"));return;}
  p.sendSystemMessage(Component.literal(success?(stage==2?"すべての機関を復旧！ 最奥の守護者への扉が開いた。":"機関復旧！ 次の大広間への扉が開いた。"):message).withColor(success?0xffd478:0xdfc49b));
  if(!before.equals(NightSpire.JSON.toJson(s)))ClockworkFeedback.activity(p.level(),f);
  ClockworkFeedback.tick(p.level(),f);
  if(success){ClockworkFeedback.begin(p.level(),f,stage);p.level().playSound(null,pos,net.minecraft.sounds.SoundEvents.ANVIL_USE,net.minecraft.sounds.SoundSource.BLOCKS,.65f,1.25f);}
 }
 static void apply(ServerLevel l,int f,int stage){int y=DungeonLayout.base(f)+1;for(int i=0;i<stage;i++)if(!ClockworkFeedback.opening(f,i))for(int x=54;x<=64;x++)for(int dy=0;dy<7;dy++){var pos=new BlockPos(x,y+dy,DOOR[i]);if(!l.getBlockState(pos).isAir())l.setBlock(pos,Blocks.AIR.defaultBlockState(),3);}}
 static void tick(ServerLevel l){if(NightSpire.progress==null)return;Set<Integer> touched=new HashSet<>();for(var p:l.players())if(SpireSite.contains(l,p.blockPosition())){int f=DungeonLayout.floor(p.getY());if(touched.add(f))apply(l,f,state(f).stage);}}
 static void install(ServerLevel l,int f){
  int y=DungeonLayout.base(f)+1;for(int stage=0;stage<3;stage++)for(int i=0;i<5;i++){
   int x=46+i*6,z=STATION[stage];l.setBlock(new BlockPos(x,y,z),PANEL.defaultBlockState(),2);
   String label=stage==0?(i<3?"圧力弁 ×"+(i+1):i==3?"計器":"圧力を確定"):stage==1?(i<4?SYMBOL[i]:"最初から"):(i<4?"動力 "+(1<<i):"動力を確定");
   NeonZones.sign(l,new BlockPos(x,y+1,z),label,"右クリックで操作",stage==0?"目標圧力 "+target(f,0):stage==2?"必要動力 "+target(f,2):"図面の順に押す","機関 "+(stage+1)+" / 3");
  }
  for(int z:new int[]{12,62,112})l.setBlock(new BlockPos(59,y,z),Blocks.POLISHED_BLACKSTONE.defaultBlockState(),2);
  NeonZones.sign(l,new BlockPos(59,y+1,12),"蒸気圧の調整","弁の値を0〜4で調整","左から ×1・×2・×3","合計を目標圧力へ");
  NeonZones.sign(l,new BlockPos(59,y+1,62),"歯車の共鳴",SYMBOL[code(f)[0]]+" → "+SYMBOL[code(f)[1]],SYMBOL[code(f)[2]]+" → "+SYMBOL[code(f)[3]],"この順で起動する");
  NeonZones.sign(l,new BlockPos(59,y+1,112),"動力の配分","1・2・4・8の回路","必要な回路だけON","合計 "+target(f,2)+" に調整");
  for(int i=0;i<3;i++)NeonZones.sign(l,new BlockPos(52,y+1,DOOR[i]-2),"機関通路・警戒","曲がり角に敵影あり","奥の大広間を目指す","復旧で扉が開く");
 }
}
