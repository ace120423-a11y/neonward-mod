package jp.neonward;

import java.util.*;
import java.nio.file.*;
import com.google.gson.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;

public final class NightSpire {
 static class Progress {int built;int exterior;int layout;int run;Map<Integer,String> bosses=new HashMap<>();Set<Integer> resetPending=new HashSet<>();Map<Integer,ClockworkPuzzles.State> puzzles=new HashMap<>();Map<Integer,Set<Integer>> patrols=new HashMap<>();Map<String,Integer> cleared=new HashMap<>();Map<String,Integer> checkpoint=new HashMap<>();}
 static Progress progress;static Path file;static int cursor;static boolean[][] plan;static final Map<Integer,Long> active=new HashMap<>();static final Map<UUID,Boolean> waiting=new HashMap<>();static String fault="";
 static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
 static final Block[] TRIM={net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("cyan_concrete")),net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("purple_concrete")),net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("orange_concrete")),net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("light_blue_concrete")),net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("red_concrete")),net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("yellow_concrete"))};
 static final Block[] GLASS={net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("cyan_stained_glass")),net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("magenta_stained_glass")),net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("orange_stained_glass")),net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("light_blue_stained_glass")),net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("red_stained_glass")),net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("yellow_stained_glass"))};
 public static void init(){
  ServerLifecycleEvents.SERVER_STARTED.register(s->{file=s.getWorldPath(LevelResource.ROOT).resolve("neonward/night_spire.json");cursor=0;plan=null;ClockworkFeedback.OPENING.clear();ClockworkFeedback.ACTIVITY.clear();ClockworkDisplays.reset();ExteriorShell.reset();active.clear();waiting.clear();fault="";try{progress=Files.exists(file)?JSON.fromJson(Files.readString(file),Progress.class):new Progress();if(progress==null||progress.cleared==null||progress.checkpoint==null||progress.built<0||progress.built>30)throw new IllegalStateException("Invalid tower progress");if(progress.layout!=3){progress.layout=3;progress.built=0;}}catch(Exception ex){progress=null;fault="塔の進行データを読み込めません";System.err.println("[Night Spire] "+ex);}});
  ServerLifecycleEvents.SERVER_STOPPED.register(s->{progress=null;file=null;active.clear();waiting.clear();});
  ServerTickEvents.END_SERVER_TICK.register(s->{var l=s.getLevel(NeonZones.TOWER);if(l==null||progress==null)return;
   build(l);
   if(s.getTickCount()%20!=0)return;
   SpireBosses.migrate(l);ClockworkPuzzles.tick(l);ClockworkPatrols.tick(l);
   for(var id:new ArrayList<>(waiting.keySet())){var p=s.getPlayerList().getPlayer(id);if(p==null){waiting.remove(id);continue;}int f=waiting.get(id)?1:checkpoint(p);if(progress.built>=f){waiting.remove(id);arrive(p,f);}}
   var floors=new HashSet<Integer>();for(var p:new ArrayList<>(l.players()))if(!p.isSpectator()&&p.isAlive()&&SpireSite.contains(l,p.blockPosition())){
    int f=DungeonLayout.floor(p.getY());floors.add(f);active.put(f,(long)s.getTickCount());
    
    if(cleared(p)>=f&&enemies(l,f).stream().noneMatch(e->e.entityTags().contains("nw_spire_boss")&&e.isAlive())){gate(l,f);double gx=SpireSite.X+34.5,gz=SpireSite.Z+35;if(p.distanceToSqr(gx,DungeonLayout.base(f)+3,gz)<=12.25){ascend(p);continue;}}
    p.sendOverlayMessage(Component.literal("NIGHT SPIRE  "+f+" / 30階  "+DungeonLayout.THEMES[DungeonLayout.theme(f)]+(!ClockworkPuzzles.solved(f)?"  機関復旧 "+ClockworkPuzzles.state(f).stage+" / 3 ・通路を探索":cleared(p)>=f?"  ボス撃破済み・光るゲートへ":"  奥の階層ボスを撃破")));
   }
   for(int f:floors){if(l.getDifficulty()==Difficulty.PEACEFUL)continue;boolean needs=l.players().stream().anyMatch(p->SpireSite.contains(l,p.blockPosition())&&!p.isSpectator()&&DungeonLayout.floor(p.getY())==f&&cleared(p)<f);if(needs&&ClockworkPuzzles.solved(f)&&enemies(l,f).stream().noneMatch(e->e.entityTags().contains("nw_spire_boss")&&e.isAlive())){populate(l,f);}}
   for(var f:new ArrayList<>(active.keySet()))if(!floors.contains(f)&&s.getTickCount()-active.get(f)>200){boolean removed=false;for(var e:enemies(l,f))if(e.entityTags().contains("nw_spire_boss")){e.discard();removed=true;}if(removed){progress.bosses.remove(f);try{save();}catch(Exception ex){System.err.println("[Night Spire] Cleanup save failed: "+ex);}}active.remove(f);}
  });
  ServerLivingEntityEvents.AFTER_DEATH.register((e,source)->{
   if(!(e instanceof net.minecraft.world.entity.LivingEntity boss)||!SpireSite.contains(e.level(),e.blockPosition())||!e.entityTags().contains("nw_spire_boss")||progress==null)return;
   int f=DungeonLayout.floor(e.getY());var l=(ServerLevel)e.level();progress.bosses.remove(f,e.getStringUUID());var before=new HashMap<>(progress.cleared);var recipients=new ArrayList<ServerPlayer>();
   for(var p:l.players())if(SpireSite.contains(l,p.blockPosition())&&!p.isSpectator()&&p.isAlive()&&DungeonLayout.floor(p.getY())==f){progress.cleared.put(p.getStringUUID(),Math.max(cleared(p),f));recipients.add(p);}
   gate(l,f);try{save();for(var p:recipients)p.sendSystemMessage(Component.literal(f==30?"NIGHT SPIRE / 30階攻略達成！ 出現した帰還ゲートで街へ戻れます。":f+"階ボス撃破！ 最奥に次の階層へのゲートが出現しました。"));}catch(Exception ex){progress.cleared=before;for(var p:recipients)p.sendSystemMessage(Component.literal("攻略記録を保存できませんでした。再挑戦できるようボスを復旧します。"));}
  });
 }
 static int cleared(ServerPlayer p){return progress==null?0:Math.max(0,Math.min(30,progress.cleared.getOrDefault(p.getStringUUID(),0)));}
 static int checkpoint(ServerPlayer p){return progress==null?1:Math.max(1,Math.min(30,progress.checkpoint.getOrDefault(p.getStringUUID(),1)));}
 static void save()throws Exception{Files.createDirectories(file.getParent());var tmp=file.resolveSibling("night_spire.json.tmp");Files.writeString(tmp,JSON.toJson(progress));for(int attempt=0;;attempt++){try{Files.move(tmp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);break;}catch(java.nio.file.AccessDeniedException locked){if(attempt>=4)throw locked;Thread.sleep(15L*(attempt+1));}}}
 public static void enter(ServerPlayer p,boolean first){
  if(progress==null){p.sendSystemMessage(Component.literal(fault.isEmpty()?"塔を準備中です":fault));return;}
  var l=p.level().getServer().getLevel(NeonZones.TOWER);if(l==null){p.sendSystemMessage(Component.literal("塔の反映にはゲームの再起動が必要です"));return;}
  if(l.getDifficulty()==Difficulty.PEACEFUL){p.sendSystemMessage(Component.literal("ピースフルではボスが出ないため、イージー以上に変更してね"));return;}
  if(first&&!restart(p,l))return;
  int f=first?1:checkpoint(p); // A normal entry always respects this player's saved floor.
  if(progress.built<f){waiting.put(p.getUUID(),first);p.sendSystemMessage(Component.literal("塔を建設中です（"+progress.built+" / 30階）。準備できたら移動します。"));return;}arrive(p,f);
 }
 static void arrive(ServerPlayer p,int f){var l=p.level().getServer().getLevel(NeonZones.TOWER);if(l==null)return;resetFloor(l,f);NeonZones.move(p,l,new Vec3(40.5,DungeonLayout.base(f)+1,9.5),-30);p.sendSystemMessage(Component.literal("NIGHT SPIRE / "+f+"階。最奥のボス撃破でゲートが出現します。3つの機関を復旧して進もう。入口の端末で塔の外へ帰還できます。"));}
 static boolean restart(ServerPlayer p,ServerLevel l){
  if(l.players().stream().anyMatch(q->q!=p&&!q.isSpectator()&&q.isAlive()&&SpireSite.contains(l,q.blockPosition()))||waiting.keySet().stream().anyMatch(id->!id.equals(p.getUUID()))){p.sendSystemMessage(Component.literal("他のプレイヤーが攻略中のためリセットできません。全員が塔の外に出てから再挑戦してください。"));return false;}
  String before=JSON.toJson(progress);
  progress.run++;progress.bosses.clear();progress.puzzles.clear();progress.patrols.clear();
  progress.cleared.put(p.getStringUUID(),0);progress.checkpoint.put(p.getStringUUID(),1);
  for(int f=1;f<=30;f++)progress.resetPending.add(f);
  try{save();}catch(Exception ex){progress=JSON.fromJson(before,Progress.class);p.sendSystemMessage(Component.literal("保存できなかったためリセットを中止しました。"));return false;}
  active.clear();ClockworkFeedback.OPENING.clear();ClockworkFeedback.ACTIVITY.clear();ClockworkDisplays.reset();
  for(var e:new ArrayList<>(enemiesAll(l)))e.discard();
  p.sendSystemMessage(Component.literal("新しい挑戦を開始します。機関・敵・ゲートを初期状態に戻しました。"));return true;
 }
 static void resetFloor(ServerLevel l,int f){
  if(!progress.resetPending.contains(f))return;
  int y=DungeonLayout.base(f)+1;
  for(int z:ClockworkPuzzles.DOOR)for(int x=54;x<=64;x++)for(int dy=0;dy<7;dy++)l.setBlock(new BlockPos(x,y+dy,z),ClockworkInterior.COPPER.defaultBlockState(),2);
  for(var e:l.getAllEntities())if(e.entityTags().stream().anyMatch(t->t.startsWith("nw_feedback_"+f+"_lift_door_")))e.discard();
  for(int x=32;x<=37;x++)for(int dy=0;dy<=5;dy++)l.setBlock(SpireSite.pos(x,y+dy,35),Blocks.AIR.defaultBlockState(),2);
  var layout=DungeonLayout.plan(f);for(int x=33;x<=36;x++)l.setBlock(SpireSite.pos(x,y-1,35),ClockworkInterior.state(layout,SpireSite.X+x,0,SpireSite.Z+35,f),2);
  ClockworkPuzzles.state(f);progress.resetPending.remove(f);
  try{save();}catch(Exception ex){progress.resetPending.add(f);System.err.println("[Night Spire] Floor reset will retry: "+ex);}
 }
 public static void ascend(ServerPlayer p){if(progress==null||!SpireSite.contains(p.level(),p.blockPosition()))return;int f=DungeonLayout.floor(p.getY());if(!ClockworkPuzzles.solved(f)){p.sendSystemMessage(Component.literal("この階の3つの機関を復旧してから進もう"));return;}if(cleared(p)<f){p.sendSystemMessage(Component.literal("この階のボスを倒すとゲートが解放されます"));return;}if(f==30){leave(p);return;}if(progress.built<f+1){p.sendSystemMessage(Component.literal("次の階を準備しています"));return;}int before=checkpoint(p);progress.checkpoint.put(p.getStringUUID(),Math.max(before,f+1));try{save();}catch(Exception ex){progress.checkpoint.put(p.getStringUUID(),before);p.sendSystemMessage(Component.literal("進行を保存できなかったため移動を中止しました"));return;}arrive(p,f+1);p.sendSystemMessage(Component.literal("進行セーブ：第"+(f+1)+"階に到達。次回はこの階から再開できます。"));}
 public static void leave(ServerPlayer p){NeonZones.move(p,p.level().getServer().overworld(),new Vec3(SpireSite.OUTER_X+3.5,65,SpireSite.OUTER_Z-8.5),180);p.sendSystemMessage(Component.literal("NIGHT SPIRE / 塔の玄関前へ帰還"));}
 static void gate(ServerLevel l,int f){
  int y=DungeonLayout.base(f)+1;if(l.getGameTime()%40==0)for(int i=0;i<4;i++){double x=33+l.getRandom().nextDouble()*3,yy=y+.2+l.getRandom().nextDouble()*3.5;l.sendParticles(new net.minecraft.core.particles.DustParticleOptions(0x89ffff,1),SpireSite.X+x,yy,SpireSite.Z+35.5,1,0,0,0,0);}if(l.getBlockState(SpireSite.pos(32,y,35)).is(Blocks.SEA_LANTERN))return;
  for(int dy=0;dy<5;dy++)for(int x=32;x<=37;x++)if(x==32||x==37||dy==4)l.setBlock(SpireSite.pos(x,y+dy,35),Blocks.SEA_LANTERN.defaultBlockState(),3);
  for(int x=33;x<=36;x++)l.setBlock(SpireSite.pos(x,y-1,35),Blocks.SEA_LANTERN.defaultBlockState(),3);
  for(int x=33;x<=36;x++)for(int dy=1;dy<=3;dy++)l.setBlock(SpireSite.pos(x,y+dy,35),Blocks.AIR.defaultBlockState(),3);
  NeonZones.sign(l,SpireSite.pos(32,y+5,35),f==30?"帰還ゲート":"第 "+(f+1)+" 階層へ","ボス撃破・解放済み","光る枠をくぐる","移動時に進行セーブ");
 }
 static List<CyberEnemy> enemies(ServerLevel l,int f){
  return enemiesAll(l).stream().filter(e->e.entityTags().contains("nw_spire_floor_"+f)).toList();
 }
 static List<CyberEnemy> enemiesAll(ServerLevel l){var found=new ArrayList<CyberEnemy>();for(var e:l.getAllEntities())if(e instanceof CyberEnemy c&&e.entityTags().stream().anyMatch(t->t.startsWith("nw_spire_floor_")))found.add(c);return found;}
 static void populate(ServerLevel l,int f){
  if(progress.bosses.containsKey(f)||!l.isPositionEntityTicking(SpireSite.pos(32,DungeonLayout.base(f)+1,32)))return;
  int gy=DungeonLayout.base(f)+1;for(int x=32;x<=37;x++)for(int dy=0;dy<=5;dy++)l.setBlock(SpireSite.pos(x,gy+dy,35),Blocks.AIR.defaultBlockState(),2);
  var boss=SpireBosses.spawn(l,f,32.5,32.5);if(boss!=null){progress.bosses.put(f,boss.getStringUUID());try{save();}catch(Exception ex){System.err.println("[Night Spire] Boss record save failed: "+ex);}}
 }
 static CyberEnemy spawn(ServerLevel l,int floor,double x,double z,HostileRoster.Kind k,boolean boss){
  var e=new CyberEnemy(NeonHostiles.TYPES.get(k.id()),l);e.setPos(SpireSite.X+x,DungeonLayout.base(floor)+1,SpireSite.Z+z);e.setPersistenceRequired();e.addTag("nw_spire_floor_"+floor);
  e.addTag("nw_spire_run_"+progress.run);
  double factor=1+(floor-1)*.09;e.getAttribute(Attributes.MAX_HEALTH).setBaseValue(k.hp()*factor*(boss?4.5:1));e.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(k.damage()*(1+(floor-1)*.035)*(boss?1.35:1));e.setHealth(e.getMaxHealth());
  if(boss){e.addTag("nw_spire_boss");e.getAttribute(Attributes.SCALE).setBaseValue(k.id().equals("iron_colossus")?1.65:1.25);e.setCustomName(Component.literal("第"+floor+"階 守護者 / "+k.name()).withColor(0xff537f));e.setCustomNameVisible(true);}
  l.addFreshEntity(e);return e;
 }
 static void build(ServerLevel l){
  if(progress.built>=30)return;int f=progress.built+1,size=DungeonLayout.SIZE,total=size*DungeonLayout.DEPTH*DungeonLayout.STEP;
  if(plan==null)plan=DungeonLayout.plan(f);
  for(int budget=0;budget<5000&&cursor<total;budget++,cursor++){
   int x=cursor%size,z=(cursor/size)%DungeonLayout.DEPTH,dy=cursor/(size*DungeonLayout.DEPTH);var p=new BlockPos(x,DungeonLayout.base(f)+dy,z);BlockState state=ClockworkInterior.state(plan,x,dy,z,f);
   if(!l.getBlockState(p).equals(state))l.setBlock(p,state,2);
  }
  if(cursor>=total){
   int y=DungeonLayout.base(f)+1;l.setBlock(new BlockPos(7,y,9),NeonZones.EXIT.defaultBlockState(),3);NeonZones.sign(l,new BlockPos(7,y+1,9),"CLOCKWORK SPIRE",f+" / 30階・拡張空間","帰還端末","右クリック：塔の外へ");
   NeonZones.sign(l,SpireSite.pos(36,y,37),f==30?"最終階":"階層ゲート出現地点","ボス撃破で出現","光るゲートをくぐる","進行は自動保存");
   ClockworkPuzzles.install(l,f);
   progress.built=f;try{save();}catch(Exception ex){progress.built=f-1;System.err.println("[Night Spire] Build checkpoint save failed: "+ex);}cursor=0;plan=null;
  }
 }
}
