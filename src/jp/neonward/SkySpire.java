package jp.neonward;
import java.util.*;
import java.nio.file.*;
import com.google.gson.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.commands.Commands;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

/** Independent dimension, progress, gates and reward eligibility for the athletic tower. */
public final class SkySpire {
 public static final ResourceKey<Level> DIM=ResourceKey.create(Registries.DIMENSION,NeonWard.id("sky_spire"));
 public static final BlockPos ENTRANCE=new BlockPos(912,65,216);
 public static final BlockPos CITY_RETURN=new BlockPos(914,65,214);
 public static final BlockPos CITY_GUIDE=new BlockPos(272,65,11);
 public static Block ENTRY,EXIT;
 public static net.minecraft.world.item.Item SEAL;
 static class Progress {int built,snowFloors;boolean snowExterior;Map<String,Integer> checkpoint=new HashMap<>(),cleared=new HashMap<>();}
 static Progress progress;static Path file;static int cursor,exteriorCursor;
 static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
 static final Map<UUID,Integer> steps=new HashMap<>(),waiting=new HashMap<>();
 static final Map<Integer,Long> emptySince=new HashMap<>();
 static int padX(int floor,int index){return 59+(int)Math.round(Math.sin((index+floor)*.7)*2);}
 static int padZ(int index){return 10+index*4;}
 static int floor(ServerPlayer p){return DungeonLayout.floor(p.getY());}
 static String key(ServerPlayer p){return p.getStringUUID();}
 static int cleared(ServerPlayer p){return progress.cleared.getOrDefault(key(p),0);}
 public static boolean exterior(Level l,BlockPos p){return l.dimension()==Level.OVERWORLD&&p.getX()>=898&&p.getX()<=926&&p.getZ()>=209&&p.getZ()<=244;}
 static void save()throws Exception{Files.createDirectories(file.getParent());Path tmp=file.resolveSibling("sky_spire.json.tmp");Files.writeString(tmp,JSON.toJson(progress));Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
 static void message(ServerPlayer p,String text){p.sendSystemMessage(Component.literal("FROST CITADEL / 雪城 / "+text));}
 // Arrival is outside the castle shell. Build its own approach BEFORE teleporting.
 static boolean prepareLanding(ServerLevel l){
  for(int x=910;x<=914;x++)for(int z=211;z<=218;z++){
   var floor=new BlockPos(x,64,z);l.getChunkAt(floor);
   if(l.isEmptyBlock(floor))l.setBlock(floor,Blocks.QUARTZ_BRICKS.defaultBlockState(),3);
  }
  var feet=new BlockPos(912,65,213);
  if(!l.getBlockState(feet.below()).isFaceSturdy(l,feet.below(),net.minecraft.core.Direction.UP)||!l.isEmptyBlock(feet)||!l.isEmptyBlock(feet.above()))return false;
  if(l.isEmptyBlock(ENTRANCE))l.setBlock(ENTRANCE,ENTRY.defaultBlockState(),3);
  if(!l.getBlockState(ENTRANCE).is(ENTRY))return false;
  NeonZones.sign(l,ENTRANCE.above(),"FROST CITADEL / 雪城","30階・アスレチック","右クリックで挑戦","Shiftで1階から再挑戦");
  if(l.isEmptyBlock(CITY_RETURN)&&l.isEmptyBlock(CITY_RETURN.above()))l.setBlock(CITY_RETURN,EXIT.defaultBlockState(),3);
  if(l.getBlockState(CITY_RETURN).is(EXIT))NeonZones.sign(l,CITY_RETURN.above(),"街へ戻る / NEON WARD","企業ビルの受付へ","右クリックで帰還","攻略記録はそのまま");
  return true;
 }
 static void returnToCity(ServerPlayer p){
  if(p.isPassenger())p.stopRiding();
  if(CompactShops.enter(p,2)){waiting.remove(p.getUUID());steps.remove(p.getUUID());p.fallDistance=0;p.setDeltaMovement(Vec3.ZERO);message(p,"企業ビルの受付へ帰還しました。攻略記録は保持されています。");}
 }
 static boolean visitFront(ServerPlayer p){
  var l=p.level().getServer().overworld();
  if(!prepareLanding(l)){message(p,"転送先に障害物があります。安全を確認できないため移動を中止しました。");return false;}
  p.fallDistance=0;p.setDeltaMovement(Vec3.ZERO);NeonZones.move(p,l,new Vec3(912.5,65,213.5),0);p.fallDistance=0;p.setDeltaMovement(Vec3.ZERO);return true;
 }
 public static void init(){
  CommandRegistrationCallback.EVENT.register((d,c,e)->d.register(Commands.literal("skyspire").then(Commands.literal("repair").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(ctx->prepareLanding(ctx.getSource().getServer().overworld())?1:0))));
  CommandRegistrationCallback.EVENT.register((d,c,e)->d.register(Commands.literal("skyspire").then(Commands.literal("town").executes(ctx->{var p=ctx.getSource().getPlayerOrException();if(p.level().dimension()==DIM||exterior(p.level(),p.blockPosition())){returnToCity(p);return 1;}return 0;}))));
  ENTRY=NeonZones.terminal("sky_spire_entry");EXIT=NeonZones.terminal("sky_spire_exit");
  var sealKey=net.minecraft.resources.ResourceKey.create(Registries.ITEM,NeonWard.id("sky_spire_seal"));
  SEAL=net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.ITEM,sealKey,new net.minecraft.world.item.Item(new net.minecraft.world.item.Item.Properties().setId(sealKey)));
  ServerLifecycleEvents.SERVER_STARTED.register(server->{cursor=0;exteriorCursor=0;steps.clear();waiting.clear();emptySince.clear();file=server.getWorldPath(LevelResource.ROOT).resolve("neonward/sky_spire.json");try{progress=Files.exists(file)?JSON.fromJson(Files.readString(file),Progress.class):new Progress();if(progress==null||progress.checkpoint==null||progress.cleared==null||progress.built<0||progress.built>30)throw new IllegalStateException("Invalid SKY SPIRE progress");}catch(Exception e){progress=null;System.err.println("[Sky Spire] Progress unavailable: "+e);}});
  ServerLifecycleEvents.SERVER_STOPPED.register(server->{progress=null;file=null;steps.clear();waiting.clear();emptySince.clear();});
  CommandRegistrationCallback.EVENT.register((d,c,e)->d.register(Commands.literal("skyspire").executes(ctx->{enter(ctx.getSource().getPlayerOrException(),false);return 1;}).then(Commands.literal("restart").executes(ctx->{enter(ctx.getSource().getPlayerOrException(),true);return 1;})).then(Commands.literal("leave").executes(ctx->{var p=ctx.getSource().getPlayerOrException();if(p.level().dimension()==DIM)leave(p);return 1;}))));
  UseBlockCallback.EVENT.register((p,l,h,hit)->{var b=l.getBlockState(hit.getBlockPos()).getBlock();if(b!=ENTRY&&b!=EXIT)return InteractionResult.PASS;if(h==InteractionHand.MAIN_HAND&&p instanceof ServerPlayer sp&&!p.isSpectator()){if(b==ENTRY){if(l.dimension()==CompactShops.DIM&&hit.getBlockPos().equals(CITY_GUIDE)){if(visitFront(sp))message(sp,"雪と氷の30階城です。正面の入口からアスレチックに挑戦できます。");}else enter(sp,p.isShiftKeyDown());}else if(l.dimension()==DIM)leave(sp);else if(l.dimension()==Level.OVERWORLD&&hit.getBlockPos().equals(CITY_RETURN))returnToCity(sp);}return InteractionResult.SUCCESS;});
  ServerTickEvents.END_SERVER_TICK.register(server->{
   if(server.getTickCount()%20==0&&server.overworld().hasChunkAt(CITY_RETURN)&&!server.overworld().getBlockState(CITY_RETURN).is(EXIT)&&server.overworld().players().stream().anyMatch(p->exterior(p.level(),p.blockPosition())))prepareLanding(server.overworld());
   if(server.getTickCount()%10==0)for(var p:new ArrayList<>(server.overworld().players()))if(p.isAlive()&&!p.isSpectator()&&p.getX()>=898&&p.getX()<=926&&p.getZ()>=209&&p.getZ()<=244&&p.getY()<63){if(!visitFront(p)){p.fallDistance=0;p.setDeltaMovement(Vec3.ZERO);NeonZones.city(p);}}
   if(progress==null)return;var l=server.getLevel(DIM);if(l==null)return;
   if(progress.snowFloors<progress.built||!waiting.isEmpty()||!l.players().isEmpty())build(l);
   for(var id:new ArrayList<>(waiting.keySet())){var p=server.getPlayerList().getPlayer(id);if(p==null){waiting.remove(id);continue;}int f=waiting.get(id);if(progress.built>=f){waiting.remove(id);arrive(p,f);}}
   Set<Integer> occupied=new HashSet<>();
   for(var p:new ArrayList<>(l.players())){
    if(p.isSpectator()||!p.isAlive())continue;
    int f=floor(p),base=DungeonLayout.base(f);occupied.add(f);emptySince.remove(f);
    int step=steps.getOrDefault(p.getUUID(),0);
    if(p.getY()<base+2&&p.getZ()<151||p.getY()<base-1){reset(p,f,Math.min(step,32));continue;}
    if(step<34){int next=Math.min(33,step);double x=padX(f,next)+.5,z=padZ(next)+.5;
     if(p.distanceToSqr(x,base+6,z)<3.5&&p.onGround())steps.put(p.getUUID(),++step);
     if(p.getZ()>=151&&step<34){reset(p,f,Math.min(step,32));continue;}
    }
    if(step>=34&&p.getZ()>=152){
     if(cleared(p)<f&&l.getDifficulty()!=Difficulty.PEACEFUL&&bosses(l,f).isEmpty())SkyBosses.spawn(l,f,32.5,32.5);
     if(cleared(p)>=f&&bosses(l,f).isEmpty()){gate(l,f);if(p.distanceToSqr(98.5,base+1,173.5)<9){ascend(p,f);continue;}}
    }
    if(server.getTickCount()%20==0)p.sendOverlayMessage(Component.literal("雪城 "+f+"/30階 / "+(step<34?"アスレチック "+step+"/34":cleared(p)>=f?"撃破済み・奥の光へ":"ボス戦")));
   }
   if(server.getTickCount()%100==0)for(int f=1;f<=progress.built;f++){if(occupied.contains(f))continue;long since=emptySince.computeIfAbsent(f,k->l.getGameTime());if(l.getGameTime()-since>200)for(var b:bosses(l,f))b.discard();}
   // The exterior is installed only in already loaded chunks, without replacing existing blocks.
   if(server.getTickCount()%5==0)exterior(server.overworld());
  });
  ServerLivingEntityEvents.AFTER_DEATH.register((entity,source)->{
   if(!(entity instanceof SkyBoss boss)||entity.level().dimension()!=DIM||!entity.entityTags().contains("nw_sky_boss")||progress==null)return;
   int f=boss.spec().floor();var l=(ServerLevel)entity.level();var before=new HashMap<>(progress.cleared);var recipients=new ArrayList<ServerPlayer>();
   for(var p:l.players())if(p.isAlive()&&!p.isSpectator()&&floor(p)==f&&steps.getOrDefault(p.getUUID(),0)>=34&&p.getZ()>=152&&cleared(p)<f){progress.cleared.put(key(p),f);recipients.add(p);}
   try{save();}catch(Exception ex){progress.cleared=before;for(var p:recipients)message(p,"保存できませんでした。ボスを再出現させます。");return;}
   gate(l,f);for(var p:recipients){var reward=new net.minecraft.world.item.ItemStack(SEAL);if(!p.getInventory().add(reward))p.drop(reward,false);message(p,f==30?"30階踏破！ 氷冠の証を獲得。光るゲートで帰還できます。":f+"階撃破！ 氷冠の証を獲得。奥の光るゲートへ。");}
  });
 }
 static List<SkyBoss> bosses(ServerLevel l,int f){int y=DungeonLayout.base(f);return l.getEntitiesOfClass(SkyBoss.class,new AABB(0,y,150,117,y+24,181),e->e.isAlive()&&e.spec().floor()==f);}
 static void enter(ServerPlayer p,boolean restart){
  if(progress==null){message(p,"攻略データを読み込めていません。");return;}
  var l=p.level().getServer().getLevel(DIM);if(l==null){message(p,"追加された塔を読み込むため再起動してください。");return;}
  if(p.isSpectator()||l.getDifficulty()==Difficulty.PEACEFUL){message(p,"イージー以上で挑戦してください。");return;}
  int f=progress.checkpoint.getOrDefault(key(p),1);
  if(restart){int old=cleared(p),cp=f;progress.cleared.put(key(p),0);progress.checkpoint.put(key(p),1);try{save();f=1;}catch(Exception e){progress.cleared.put(key(p),old);progress.checkpoint.put(key(p),cp);message(p,"保存に失敗しました。");return;}}
  // Other players' current floors must not override this player's checkpoint.
  f=Math.max(1,Math.min(30,f));if(progress.built<f){waiting.put(p.getUUID(),f);message(p,"階層を建設しています。完成後に移動します。");}else arrive(p,f);
 }
 static void arrive(ServerPlayer p,int f){var l=p.level().getServer().getLevel(DIM);steps.put(p.getUUID(),0);NeonZones.move(p,l,new Vec3(padX(f,0)+.5,DungeonLayout.base(f)+6,10.5),0);message(p,f+"階 / 光る足場を順に渡り、奥の専用ボスを倒そう。");}
 static void reset(ServerPlayer p,int f,int step){int checkpoint=(Math.max(0,step-1)/8)*8;steps.put(p.getUUID(),checkpoint);p.fallDistance=0;NeonZones.move(p,p.level(),new Vec3(padX(f,checkpoint)+.5,DungeonLayout.base(f)+6,padZ(checkpoint)+.5),0);}
 static void ascend(ServerPlayer p,int f){if(f==30){leave(p);return;}int old=progress.checkpoint.getOrDefault(key(p),1);progress.checkpoint.put(key(p),Math.max(old,f+1));try{save();if(progress.built<f+1)waiting.put(p.getUUID(),f+1);else arrive(p,f+1);}catch(Exception e){progress.checkpoint.put(key(p),old);message(p,"保存に失敗したため移動を中止しました。");}}
 static void leave(ServerPlayer p){if(visitFront(p)){waiting.remove(p.getUUID());steps.remove(p.getUUID());}}
 static void gate(ServerLevel l,int f){int y=DungeonLayout.base(f);for(int x=96;x<=101;x++)for(int dy=0;dy<5;dy++)if(x==96||x==101||dy==0||dy==4)l.setBlock(new BlockPos(x,y+dy,174),Blocks.SEA_LANTERN.defaultBlockState(),3);}
 static void build(ServerLevel l){
  boolean renovate=progress.snowFloors<progress.built;
  if(!renovate&&progress.built>=30)return;int f=renovate?progress.snowFloors+1:progress.built+1,base=DungeonLayout.base(f),total=117*181*12;
  for(int budget=0;budget<4000&&cursor<total;cursor++,budget++){
   int x=cursor%117,z=cursor/117%181,y=cursor/(117*181);Block b=null;
   b=SnowCastle.interior(x,y,z,f);
   if(y==5)for(int i=0;i<34&&b==null;i++)if(Math.abs(x-padX(f,i))<=((i%8==0)?1:0)&&z>=padZ(i)&&z<=padZ(i)+1)b=i%8==0?Blocks.SEA_LANTERN:(f%3==0?Blocks.PACKED_ICE:Blocks.QUARTZ_BLOCK);
   if(z>=144&&z<=153&&x>=57&&x<=63&&y==5)b=Blocks.POLISHED_DIORITE;
   if(b!=null)l.setBlock(new BlockPos(x,base+y,z),b.defaultBlockState(),2);
  }
  if(cursor>=total){l.setBlock(new BlockPos(58,base+6,8),EXIT.defaultBlockState(),3);l.setBlock(new BlockPos(58,base+5,8),Blocks.QUARTZ_BLOCK.defaultBlockState(),3);int old=progress.built,decor=progress.snowFloors;progress.built=Math.max(old,f);progress.snowFloors=f;try{save();cursor=0;}catch(Exception e){progress.built=old;progress.snowFloors=decor;System.err.println("[Sky Spire] Build save failed: "+e);}}
 }
 static void exterior(ServerLevel l){
  if(!l.hasChunkAt(ENTRANCE)||!l.hasChunkAt(new BlockPos(926,65,244)))return;
  if(progress.snowExterior)return;
  int total=25*25*121;
  for(int budget=0;budget<2000&&exteriorCursor<total;budget++,exteriorCursor++){
   int x=900+exteriorCursor%25,z=218+exteriorCursor/25%25,y=64+exteriorCursor/625;
   var pos=new BlockPos(x,y,z);if(!l.hasChunkAt(pos)){return;}
   var old=l.getBlockState(pos);boolean oldShell=(y==64||y==184||x==900||x==924||z==218||z==242)&&(old.is(Blocks.TINTED_GLASS)||old.is(Blocks.SEA_LANTERN));
   Block block=SnowCastle.exterior(x-900,y-64,z-218);
   // Only replace recognized legacy shell blocks; preserve unrelated builds/terrain.
   if(block!=null&&(old.isAir()||oldShell))l.setBlock(pos,block.defaultBlockState(),2);
   else if(block==null&&oldShell)l.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
  }
  if(exteriorCursor<total)return;
  if(l.isEmptyBlock(ENTRANCE.below()))l.setBlock(ENTRANCE.below(),Blocks.QUARTZ_BLOCK.defaultBlockState(),3);
  if(l.isEmptyBlock(ENTRANCE))l.setBlock(ENTRANCE,ENTRY.defaultBlockState(),3);
  NeonZones.sign(l,ENTRANCE.above(),"FROST CITADEL / 雪城","30階・アスレチック","右クリックで挑戦","Shiftで1階から再挑戦");
  progress.snowExterior=true;try{save();}catch(Exception e){progress.snowExterior=false;}
 }
}

