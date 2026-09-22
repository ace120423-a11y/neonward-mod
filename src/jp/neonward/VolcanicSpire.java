package jp.neonward;
import java.util.*;
import java.nio.file.*;
import com.google.gson.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.*;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
/** Volcano progress, generation and encounters never share state with either older tower. */
public final class VolcanicSpire {
 public static final ResourceKey<Level> DIM=ResourceKey.create(Registries.DIMENSION,NeonWard.id("volcanic_spire"));
 static final BlockPos GUIDE=new BlockPos(266,65,11),ENTRY_POS=new BlockPos(-128,65,26),RETURN_POS=new BlockPos(-124,65,14);
 static Block ENTRY,EXIT;static Path file;static Progress progress;
 static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
 static class Progress {Set<Integer> built=new HashSet<>();boolean front;Map<String,Integer> checkpoint=new HashMap<>(),cleared=new HashMap<>();}
 static class Run {final String token=UUID.randomUUID().toString();final Map<UUID,CyberEnemy> mobs=new HashMap<>();int kills;VolcanoBoss boss;boolean defeated;long empty=-1;}
 static final Map<Integer,Run> RUNS=new HashMap<>();static final Map<UUID,Integer> WAITING=new LinkedHashMap<>(),NEXT=new HashMap<>();
 static int building,cursor,frontCursor;
 static int floor(Entity p){if(p.level().dimension()!=DIM||p.getX()<0||p.getZ()<0||p.getZ()>105)return 0;int f=1+(int)Math.floor(p.getX()/128);return f>=1&&f<=30&&p.getX()-VolcanoLayout.x(f)<=65?f:0;}
 static boolean front(Entity p){return p.level().dimension()==DIM&&p.getX()>=-177&&p.getX()<=-79&&p.getZ()>=-1&&p.getZ()<=98;}
 static void message(ServerPlayer p,String s){p.sendSystemMessage(Component.literal("VOLCANIC TOWER / "+s).withColor(0xffae63));}
 static void save()throws Exception{Files.createDirectories(file.getParent());var tmp=file.resolveSibling("volcanic_spire.json.tmp");Files.writeString(tmp,JSON.toJson(progress));Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
 static boolean saveSafe(){try{save();return true;}catch(Exception ex){System.err.println("[Volcano] Progress save failed: "+ex);return false;}}
 static void visit(ServerPlayer p){if(progress==null)return;WAITING.put(p.getUUID(),0);message(p,"火山の入口を準備しています。安全な足場の完成後に転送します。");}
 static void enter(ServerPlayer p,boolean restart){
  if(progress==null||!p.isAlive()||p.isSpectator()||p.level().getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL)return;
  int now=p.level().getServer().getTickCount();if(NEXT.getOrDefault(p.getUUID(),0)>now)return;NEXT.put(p.getUUID(),now+20);
  String id=p.getStringUUID();int f=progress.checkpoint.getOrDefault(id,1);
  if(restart){int old=progress.cleared.getOrDefault(id,0);progress.checkpoint.put(id,1);progress.cleared.put(id,0);if(!saveSafe()){progress.checkpoint.put(id,f);progress.cleared.put(id,old);message(p,"保存に失敗したため再挑戦を中止しました");return;}f=1;
   // Do not erase another party's ongoing fight.
   var run=RUNS.get(1);var l=p.level().getServer().getLevel(DIM);if(run!=null&&l.players().stream().noneMatch(q->q!=p&&q.isAlive()&&floor(q)==1)){dispose(run);RUNS.remove(1);}
  }
  WAITING.put(p.getUUID(),Math.clamp(f,1,30));message(p,f+"階へ。雑魚30体 → ボス → 次の階");
 }
 static void move(ServerPlayer p,ServerLevel l,double x,double z){if(p.isPassenger())p.stopRiding();p.teleportTo(l,x,65,z,Set.of(),0,0,true);p.fallDistance=0;p.setDeltaMovement(Vec3.ZERO);}
 static void arrive(ServerPlayer p,int f){var l=p.level().getServer().getLevel(DIM);if(f==0){move(p,l,-128.5,15.5);return;}run(l,f);move(p,l,VolcanoLayout.x(f)+32.5,6.5);}
 static void town(ServerPlayer p){if(CompactShops.enter(p,2)){WAITING.remove(p.getUUID());p.fallDistance=0;p.setDeltaMovement(Vec3.ZERO);}}
 static Run run(ServerLevel l,int f){var r=RUNS.get(f);if(r==null){r=new Run();RUNS.put(f,r);door(l,f,false);gate(l,f,false);}return r;}
 static void dispose(Run r){for(var e:r.mobs.values())e.discard();r.mobs.clear();if(r.boss!=null)r.boss.discard();}
 static boolean member(Entity e,Run r){return e.entityTags().contains("nw_volcano_run_"+r.token);}
 static void tag(Entity e,int f,Run r){e.addTag("nw_volcano");e.addTag("nw_volcano_floor_"+f);e.addTag("nw_volcano_run_"+r.token);}
 static void door(ServerLevel l,int f,boolean open){for(int x=28;x<=36;x++)for(int y=65;y<=71;y++)l.setBlock(new BlockPos(VolcanoLayout.x(f)+x,y,56),(open?Blocks.AIR:Blocks.OBSIDIAN).defaultBlockState(),2);}
 static void gate(ServerLevel l,int f,boolean open){for(int x=29;x<=35;x++)for(int y=65;y<=71;y++)if(x==29||x==35||y==71)l.setBlock(new BlockPos(VolcanoLayout.x(f)+x,y,98),(open?Blocks.SHROOMLIGHT:Blocks.AIR).defaultBlockState(),2);}
 static void killed(CyberEnemy e){int f=floor(e);var r=RUNS.get(f);if(r!=null&&member(e,r)&&r.mobs.remove(e.getUUID())!=null){r.kills=Math.min(30,r.kills+1);if(r.kills==30)door((ServerLevel)e.level(),f,true);}}
 static void bossDefeated(VolcanoBoss b){int f=b.floor();var r=RUNS.get(f);if(r==null||r.defeated||r.boss!=b||!member(b,r)||progress==null)return;
  var l=(ServerLevel)b.level();var cp=new HashMap<>(progress.checkpoint);var cleared=new HashMap<>(progress.cleared);
  var winners=l.players().stream().filter(p->p.isAlive()&&!p.isSpectator()&&floor(p)==f).toList();
  for(var p:winners){String id=p.getStringUUID();progress.cleared.put(id,Math.max(f,progress.cleared.getOrDefault(id,0)));progress.checkpoint.put(id,Math.max(Math.min(30,f+1),progress.checkpoint.getOrDefault(id,1)));}
  if(!saveSafe()){progress.checkpoint=cp;progress.cleared=cleared;r.boss=null;for(var p:winners)message(p,"進行保存に失敗。ボスを再出現させます");return;}
  r.defeated=true;gate(l,f,true);for(var p:winners)message(p,f==30?"30階踏破！ 奥の光でギルドへ帰還できます":f+"階撃破！ 奥の光るゲートへ");
 }
 static void encounter(ServerLevel l,int f,Run r){
  if(r.defeated)return;
  for(var e:new ArrayList<>(r.mobs.values())){
   if(e.isRemoved()){if(e.getRemovalReason()==Entity.RemovalReason.KILLED)killed(e);else r.mobs.remove(e.getUUID());continue;}
   // No escape into another encounter or falls that leave the counter permanently stuck.
   if(e.getY()<62||e.getX()<VolcanoLayout.x(f)+2||e.getX()>VolcanoLayout.x(f)+62||e.getZ()<11||e.getZ()>53)e.teleportTo(VolcanoLayout.x(f)+32.5,65,32.5);
  }
  if(r.kills<30){int remaining=Math.min(6-r.mobs.size(),30-r.kills-r.mobs.size());for(int n=0;n<Math.min(2,remaining);n++){
   var k=HostileRoster.ALL[(f*3+r.kills+r.mobs.size())%HostileRoster.ALL.length];var e=new CyberEnemy(NeonHostiles.TYPES.get(k.id()),l);
   int x=VolcanoLayout.x(f)+12+l.getRandom().nextInt(40),z=18+l.getRandom().nextInt(29);l.getChunk(x>>4,z>>4);e.setPos(x+.5,65,z+.5);tag(e,f,r);e.setPersistenceRequired();
   if(!l.noCollision(e,e.getBoundingBox()))continue;r.mobs.put(e.getUUID(),e);if(!l.addFreshEntity(e))r.mobs.remove(e.getUUID());
  }return;}
  if(r.boss==null||r.boss.isRemoved()){
   if(l.players().stream().noneMatch(p->p.isAlive()&&!p.isSpectator()&&floor(p)==f&&p.getZ()>58))return;
   var e=new VolcanoBoss(VolcanoBosses.TYPES.get(f-1),l);tag(e,f,r);e.setPos(VolcanoLayout.x(f)+32.5,65,83.5);e.setCustomName(Component.literal(f+"F / "+VolcanoRoster.NAMES[f-1]));r.boss=e;if(!l.addFreshEntity(e))r.boss=null;
  }
 }
 static void build(ServerLevel l){
  // Progress JSON and region files can be saved at different times after a crash.
  // Validate the landing floor before trusting a cached "built" flag.
  if(WAITING.containsValue(0)&&progress.front&&(l.getBlockState(ENTRY_POS).getBlock()!=ENTRY||l.getBlockState(new BlockPos(-128,64,15)).isAir())){progress.front=false;frontCursor=0;}
  for(int f:new HashSet<>(WAITING.values()))if(f>0&&progress.built.contains(f)){
   int x=VolcanoLayout.x(f);if(!l.getBlockState(new BlockPos(x+27,65,6)).is(EXIT)||l.getBlockState(new BlockPos(x+32,64,6)).isAir()||l.getBlockState(new BlockPos(x+32,64,83)).isAir())progress.built.remove(f);
  }
  if(WAITING.containsValue(0)&&!progress.front){for(int budget=0;budget<3000&&frontCursor<VolcanoLayout.FRONT_TOTAL;budget++,frontCursor++){
   int x=frontCursor%97,z=frontCursor/97%97,y=frontCursor/(97*97);var b=VolcanoLayout.front(x,y,z);if(b!=null)l.setBlock(new BlockPos(-176+x,64+y,z),b.defaultBlockState(),2);
  }if(frontCursor==VolcanoLayout.FRONT_TOTAL){l.setBlock(ENTRY_POS,ENTRY.defaultBlockState(),3);l.setBlock(RETURN_POS,EXIT.defaultBlockState(),3);NeonZones.sign(l,ENTRY_POS.above(),"VOLCANIC TOWER","火山・30階の戦闘塔","右クリック：続きから","Shift：1階から再挑戦");NeonZones.sign(l,RETURN_POS.above(),"街へ戻る","企業ビル・ギルド","右クリックで帰還","");progress.front=true;if(!saveSafe())progress.front=false;}return;}
  if(building==0)for(int f:WAITING.values())if(f>0&&!progress.built.contains(f)){building=f;cursor=0;break;}
  if(building==0)return;int f=building;
  for(int budget=0;budget<1800&&cursor<VolcanoLayout.TOTAL;budget++,cursor++){int x=cursor%65,z=cursor/65%105,y=cursor/(65*105);var b=VolcanoLayout.block(x,y,z,f);if(b!=null){var state=b.defaultBlockState();if(b==Blocks.POINTED_DRIPSTONE)state=state.setValue(SpeleothemBlock.TIP_DIRECTION,Direction.DOWN);l.setBlock(new BlockPos(VolcanoLayout.x(f)+x,64+y,z),state,2);}}
  if(cursor==VolcanoLayout.TOTAL){l.setBlock(new BlockPos(VolcanoLayout.x(f)+27,65,6),EXIT.defaultBlockState(),3);progress.built.add(f);if(saveSafe()){building=0;cursor=0;}else progress.built.remove(f);}
 }
 static void tick(net.minecraft.server.MinecraftServer s){if(progress==null)return;var l=s.getLevel(DIM);if(l==null)return;build(l);
  for(var entry:new ArrayList<>(WAITING.entrySet())){var p=s.getPlayerList().getPlayer(entry.getKey());if(p==null||!p.isAlive()){WAITING.remove(entry.getKey());continue;}int f=entry.getValue();if(f==0?progress.front:progress.built.contains(f)){WAITING.remove(entry.getKey());arrive(p,f);}}
  if(s.getTickCount()%20!=0)return;Set<Integer> occupied=new HashSet<>();
  for(var p:new ArrayList<>(l.players())){if(!p.isAlive()||p.isSpectator())continue;int f=floor(p);
   if(f==0){if(p.getY()<63||!front(p))town(p);continue;}if(!progress.built.contains(f)){town(p);continue;}
   occupied.add(f);var r=run(l,f);r.empty=-1;
   if(p.getY()<63||p.getY()>81){move(p,l,VolcanoLayout.x(f)+32.5,6.5);continue;}
   if(r.kills<30&&p.getZ()>55){move(p,l,VolcanoLayout.x(f)+32.5,50.5);}
   if(r.defeated&&Math.abs(p.getX()-(VolcanoLayout.x(f)+32.5))<3&&p.getZ()>96){if(f==30)town(p);else{WAITING.put(p.getUUID(),f+1);message(p,"次の階を準備しています");}continue;}
   p.sendOverlayMessage(Component.literal("火山 "+f+"/30階 / "+(r.kills<30?"討伐 "+r.kills+"/30":r.defeated?"撃破！ 奥の光へ":"岩扉開放・ボスを倒せ")));
  }
  for(var entry:new ArrayList<>(RUNS.entrySet())){int f=entry.getKey();var r=entry.getValue();if(occupied.contains(f)){encounter(l,f,r);continue;}if(r.empty<0)r.empty=l.getGameTime();if(l.getGameTime()-r.empty>=200){dispose(r);RUNS.remove(f);}}
 }
 public static void init(){VolcanoBosses.init();ENTRY=NeonZones.terminal("volcano_entry");EXIT=NeonZones.terminal("volcano_exit");
  ServerLifecycleEvents.SERVER_STARTED.register(s->{file=s.getWorldPath(LevelResource.ROOT).resolve("neonward/volcanic_spire.json");try{progress=Files.exists(file)?JSON.fromJson(Files.readString(file),Progress.class):new Progress();if(progress==null||progress.built==null||progress.checkpoint==null||progress.cleared==null||progress.built.stream().anyMatch(f->f<1||f>30))throw new IllegalStateException("Invalid volcano progress");}catch(Exception e){progress=null;System.err.println("[Volcano] Progress unavailable: "+e);}building=cursor=frontCursor=0;RUNS.clear();WAITING.clear();NEXT.clear();});
  ServerLifecycleEvents.SERVER_STOPPED.register(s->{RUNS.clear();WAITING.clear();NEXT.clear();progress=null;file=null;});ServerPlayConnectionEvents.DISCONNECT.register((h,s)->{WAITING.remove(h.player.getUUID());NEXT.remove(h.player.getUUID());});
  ServerEntityEvents.ENTITY_LOAD.register((e,l)->{if(l.dimension()!=DIM||!e.entityTags().contains("nw_volcano"))return;var r=RUNS.get(floor(e));if(r==null||!member(e,r)){e.discard();return;}
   if(e instanceof VolcanoBoss b){if(r.defeated||r.boss==null||!r.boss.getUUID().equals(b.getUUID()))b.discard();else r.boss=b;}
   else if(e instanceof CyberEnemy m){if(!r.mobs.containsKey(m.getUUID()))m.discard();else r.mobs.put(m.getUUID(),m);}
  });
  ServerLivingEntityEvents.AFTER_DEATH.register((e,source)->{if(e.level().dimension()!=DIM)return;if(e instanceof VolcanoBoss b)bossDefeated(b);else if(e instanceof CyberEnemy mob)killed(mob);});
  UseBlockCallback.EVENT.register((p,l,h,hit)->{var b=l.getBlockState(hit.getBlockPos()).getBlock();if(b!=ENTRY&&b!=EXIT)return InteractionResult.PASS;if(h==InteractionHand.MAIN_HAND&&p instanceof ServerPlayer sp&&!p.isSpectator()){if(b==EXIT&&l.dimension()==DIM)town(sp);else if(b==ENTRY&&l.dimension()==CompactShops.DIM&&hit.getBlockPos().equals(GUIDE))visit(sp);else if(b==ENTRY&&l.dimension()==DIM&&hit.getBlockPos().equals(ENTRY_POS))enter(sp,p.isShiftKeyDown());}return InteractionResult.SUCCESS;});
  CommandRegistrationCallback.EVENT.register((d,c,e)->d.register(Commands.literal("volcano").then(Commands.literal("leave").executes(ctx->{var p=ctx.getSource().getPlayerOrException();if(p.level().dimension()==DIM){town(p);return 1;}return 0;}))));
  ServerTickEvents.END_SERVER_TICK.register(VolcanicSpire::tick);
 }
}
