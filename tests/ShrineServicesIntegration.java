package jp.neonward;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Parent isolated-world runner calls run(visitor) AFTER SakuraTown.ready and staff entity-ticking.
 * No registration/build/launch side effects. Temporarily replaces one anchor and restores it in finally.
 * Uses real ledger atomic-save failure injection, registered blocks/items and production request methods.
 */
public final class ShrineServicesIntegration {
 static int checks;
 static Pending pending;static boolean registered;
 static final class Pending {
  boolean started;final ServerPlayer player;final int deadline;final Set<net.minecraft.world.level.ChunkPos> added=new HashSet<>();
  final java.util.concurrent.CompletableFuture<Integer> result=new java.util.concurrent.CompletableFuture<>();
  Pending(ServerPlayer p){player=p;deadline=p.level().getServer().getTickCount()+200;}
 }
 /** Parent must keep visitor alive and delay its COMPLETE marker until this future completes.
  * Pins only authored fixture/staff chunks (deduplicated), waits <=200 ticks, releases its own tickets.
  * Call on server thread; never join/get there. No production chunks are permanently forced.
  */
 public static java.util.concurrent.CompletableFuture<Integer> runWhenReady(ServerPlayer p){
  var server=p.level().getServer();check(server.isSameThread()&&pending==null,"one server-thread shrine integration at a time");
  if(!registered){registered=true;
   net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(ShrineServicesIntegration::continuePending);
   net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPING.register(s->{if(pending!=null&&pending.player.level().getServer()==s)finishPending(new AssertionError("server stopped before shrine integration"));});
  }
  var test=new Pending(p);pending=test;
  try{var l=server.overworld();for(var at:fixturePositions()){
   var chunk=new net.minecraft.world.level.ChunkPos(at.getX()>>4,at.getZ()>>4);if(!l.getForceLoadedChunks().contains(chunk.pack())){test.added.add(chunk);l.setChunkForced(chunk.x(),chunk.z(),true);}l.getChunk(chunk.x(),chunk.z());
  }}catch(Throwable failure){finishPending(failure);}return test.result;
 }
 static void continuePending(net.minecraft.server.MinecraftServer server){
  var test=pending;if(test==null||test.started||test.player.level().getServer()!=server)return;
  if(server.getTickCount()>test.deadline){finishPending(new AssertionError("shrine readiness exceeded 200 ticks: "+SakuraTown.status()));return;}
  if(!SakuraTown.ready(server)||!fixturePositions().stream().allMatch(server.overworld()::isPositionEntityTicking))return;
  try{test.started=true;run(test.player);ShrineRitualIntegration.run(server).whenComplete((count,failure)->{if(failure==null)checks+=count;finishPending(failure);});}catch(Throwable failure){finishPending(failure);}
 }
 static void finishPending(Throwable failure){var test=pending;if(test==null)return;pending=null;
  for(var chunk:test.added)test.player.level().getServer().overworld().setChunkForced(chunk.x(),chunk.z(),false);
  if(failure==null)test.result.complete(checks);else test.result.completeExceptionally(failure);
 }
 static void check(boolean ok,String reason){checks++;if(!ok)throw new AssertionError("SHRINE: "+reason);}
 static MarketLedger.Account account(ServerPlayer p){return StockMarket.ledger.account(p.getStringUUID());}
 static Set<BlockPos> fixturePositions(){var positions=new HashSet<BlockPos>();positions.add(SakuraTownPlan.STAFF);for(int kind=0;kind<3;kind++)positions.addAll(ShrineServices.fixtureParts(kind).keySet());return positions;}
 static void empty(ServerPlayer p){for(int i=0;i<36;i++)p.getInventory().setItem(i,ItemStack.EMPTY);}
 static int count(ServerPlayer p){int n=0;for(int i=0;i<36;i++)n+=p.getInventory().getItem(i).getCount();return n;}
 static ShrineServices.Limits limits(ServerPlayer p){return ShrineServices.LIMITS.computeIfAbsent(p.getUUID(),id->new ShrineServices.Limits());}
 static int open(ServerPlayer p,int kind){
  // Separate scenarios execute in one tick; waive only UI-open throttling, never service cooldowns.
  limits(p).open=0;var at=ShrineServices.anchor(kind);p.setPos(Vec3.atBottomCenterOf(at).add(2,0,0));check(ShrineServices.open(p,kind)==1,"open exact service "+kind);return ShrineServices.SESSIONS.get(p.getUUID()).token();
 }
 static void fixtureChecks(ServerPlayer p){
  var l=p.level();long cash=account(p).cash;int[] sizes={3,6,1};
  for(int kind=0;kind<3;kind++){
   var parts=ShrineServices.fixtureParts(kind);check(parts.size()==sizes[kind],"exact authored fixture cell count "+kind);
   p.setPos(Vec3.atBottomCenterOf(ShrineServices.anchor(kind)).add(2,0,0));
   for(var part:parts.entrySet()){
    var at=part.getKey();var expected=part.getValue();check(l.getBlockState(at).equals(expected)&&l.getBlockEntity(at)==null,"expected exact part/facing installed "+at);
    limits(p).open=0;ShrineServices.SESSIONS.remove(p.getUUID());
    check(ShrineServices.serviceAt(l,at)==kind&&ShrineServices.useBlock(p,at)==1,"every authored part routes to service "+kind+" "+at);
    var session=ShrineServices.SESSIONS.get(p.getUUID());
    if(kind==1){check(ShrineRituals.active(p)&&ShrineRituals.kind(p)==1&&account(p).cash==cash,"basin part directly starts wash without charge");ShrineRituals.cancel(p);}
    else check(session!=null&&session.kind()==kind&&session.anchor().equals(ShrineServices.anchor(kind))&&account(p).cash==cash,"part opens same center-bound quote without charge");
    try{
     var wrong=expected.setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING,expected.getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING).getClockWise());
     l.setBlock(at,wrong,3);limits(p).open=0;
     check(ShrineServices.serviceAt(l,at)==-1&&ShrineServices.useBlock(p,at)==0&&!ShrineServices.near(p,kind),"rotated authored part fails closed");
     check(ShrineServices.request(p,kind==0?"offer":kind==1?"wash":"buy",(session==null?-1:session.token()),0)==0&&account(p).cash==cash,"confirm rejects changed non-center part");
     l.setBlock(at,Blocks.AIR.defaultBlockState(),3);check(ShrineServices.useBlock(p,at)==0&&!ShrineServices.near(p,kind),"missing part invalidates complete fixture");
    }finally{l.setBlock(at,expected,3);}
    // A nearby copy is within service reach, but outside the exact authored footprint.
    var copy=ShrineServices.anchor(kind).east(3);var old=l.getBlockState(copy);
    check(old.isAir()&&l.getBlockEntity(copy)==null,"copy-rejection fixture requires vacant cell");
    try{l.setBlock(copy,expected,3);limits(p).open=0;ShrineServices.SESSIONS.remove(p.getUUID());
     check(ShrineServices.serviceAt(l,copy)==-1&&ShrineServices.useBlock(p,copy)==0&&!ShrineServices.SESSIONS.containsKey(p.getUUID()),"nearby copied part cannot open a service");
    }finally{if(l.getBlockState(copy).equals(expected))l.setBlock(copy,old,3);}
   }
   var first=parts.keySet().iterator().next();p.setPos(Vec3.atBottomCenterOf(ShrineServices.anchor(kind)).add(20,0,0));limits(p).open=0;
   check(ShrineServices.useBlock(p,first)==0,"authored part still enforces player distance");
  }
  check(account(p).cash==cash&&p.getActiveEffects().isEmpty(),"part clicks never charge or apply effects");
  ShrineServices.SESSIONS.remove(p.getUUID());System.out.println("SHRINE_MULTIPART_PASS: all ten authored cells, facing, copied blocks, missing parts, center-bound quotes, distance and confirmation revalidation");
 }
 public static void run(ServerPlayer p)throws Exception{
  var other=WestLandIntegration.visitor(p.level().getServer().overworld());
  try{run(p,other);}finally{ShrineServices.SESSIONS.remove(other.getUUID());ShrineServices.LIMITS.remove(other.getUUID());other.level().removePlayerImmediately(other,Entity.RemovalReason.DISCARDED);}
 }
 public static void run(ServerPlayer p,ServerPlayer other)throws Exception{
  checks=0;var level=p.level();var pos=p.position();float yaw=p.getYRot(),pitch=p.getXRot();var inventory=Cyberware.inventory(p);var mode=p.gameMode.getGameModeForPlayer();
  var effects=p.getActiveEffects().stream().map(MobEffectInstance::new).toList();var file=StockMarket.file;var session=ShrineServices.SESSIONS.get(p.getUUID());var oldLimits=ShrineServices.LIMITS.remove(p.getUUID());
  check(StockMarket.ledger!=null,"ledger loaded");String savedAccount=StockMarket.JSON.toJson(account(p));
  try{
   check(SakuraTown.ready(level.getServer()),"town ready before service test");check(p.teleportTo(level.getServer().overworld(),SakuraTownPlan.COUNTER.getX()+2.5,SakuraTownPlan.COUNTER.getY(),SakuraTownPlan.COUNTER.getZ()+.5,Set.of(),0,0,true),"overworld visitor");
   p.setGameMode(GameType.SURVIVAL);p.removeAllEffects();empty(p);account(p).cash=10000;
   fixtureChecks(p);
   for(int kind=0;kind<3;kind++)check(p.level().getBlockState(ShrineServices.anchor(kind)).is(ShrineServices.block(kind)),"actual matching shrine block "+kind);
   check(Arrays.stream(ShrineServices.AMULETS).allMatch(Objects::nonNull),"three registered charms");
   check(p.level().getServer().getCommands().getDispatcher().getRoot().getChild("neonshrine")!=null,"dedicated command root registered");
   long cash=account(p).cash;int token=open(p,0);check(account(p).cash==cash&&p.getActiveEffects().isEmpty(),"opening offering never charges or blesses");
   check(ShrineServices.open(p,0)==0,"opening spam rate limited");
   check(ShrineServices.request(other,"offer",token,0)==0,"token bound to player");
   check(ShrineServices.request(p,"offer",token^1,0)==0,"wrong token rejected");
   check(ShrineServices.request(p,"buy",token,0)==0&&account(p).cash==cash,"wrong service action cannot buy");
   check(ShrineServices.SESSIONS.get(p.getUUID()).token()!=token&&ShrineServices.request(p,"offer",token,0)==0,"failed attempt consumes quote");
   token=open(p,0);var quote=ShrineServices.SESSIONS.get(p.getUUID());ShrineServices.SESSIONS.put(p.getUUID(),new ShrineServices.Session(0,token,ShrineServices.now(p)-1,quote.anchor()));
   check(ShrineServices.request(p,"offer",token,0)==0,"expired token rejected");
   token=open(p,0);p.setPos(p.getX()+30,p.getY(),p.getZ());check(ShrineServices.request(p,"offer",token,0)==0,"confirm rechecks distance");
   token=open(p,0);quote=ShrineServices.SESSIONS.get(p.getUUID());ShrineServices.SESSIONS.put(p.getUUID(),new ShrineServices.Session(0,token,quote.expires(),quote.anchor().east()));
   check(ShrineServices.request(p,"offer",token,0)==0,"quote bound to exact anchor");
   token=open(p,0);var at=SakuraTownPlan.SAISEN;var state=p.level().getBlockState(at);
   try{p.level().setBlock(at,Blocks.STONE.defaultBlockState(),3);check(!ShrineServices.near(p,0)&&ShrineServices.request(p,"offer",token,0)==0,"removed/replaced service block fails closed");}finally{p.level().setBlock(at,state,3);}
   p.setGameMode(GameType.SPECTATOR);check(ShrineServices.open(p,0)==0,"spectator denied");p.setGameMode(GameType.SURVIVAL);
   account(p).cash=99;token=open(p,0);check(ShrineServices.request(p,"offer",token,0)==0&&account(p).cash==99&&!ShrineRituals.active(p),"insufficient offering funds");
   // Ritual rewards/rollback/cooldowns are exercised across real ticks by ShrineRitualIntegration.
   for(int kind=0;kind<3;kind++){
    empty(p);account(p).cash=10000;token=open(p,2);check(ShrineServices.request(p,"buy",token,kind)==1,"purchase charm "+kind);
    check(account(p).cash==10000-ShrineServices.PRICES[kind]&&count(p)==1&&p.getInventory().getItem(0).is(ShrineServices.AMULETS[kind]),"exact price and charm delivery "+kind);
    check(ShrineServices.request(p,"buy",token,kind)==0&&count(p)==1,"charm replay "+kind);
   }
   empty(p);account(p).cash=10000;token=open(p,2);try{StockMarket.file=file.getParent();check(ShrineServices.request(p,"buy",token,0)==0,"charm injected save failure");}finally{StockMarket.file=file;}
   check(account(p).cash==10000&&count(p)==0,"charm cash/item rollback");
   for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));token=open(p,2);check(ShrineServices.request(p,"buy",token,0)==0&&account(p).cash==10000&&count(p)==2304,"full inventory no charge");empty(p);
   token=open(p,2);check(ShrineServices.request(p,"buy",token,3)==0&&account(p).cash==10000,"out-of-range catalog denied");
   var ledger=StockMarket.ledger;token=open(p,2);try{StockMarket.ledger=null;check(ShrineServices.request(p,"buy",token,0)==0,"missing ledger fails closed");}finally{StockMarket.ledger=ledger;}
   // Use the disposable second visitor: accessory state must not leak into the parent's visitor.
   other.setGameMode(GameType.SURVIVAL);
   for(int kind=0;kind<3;kind++){
    other.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ShrineServices.AMULETS[kind],2));
    check(!AccessoryEquipment.equipped(other,kind),"kind initially unequipped");
    check(ShrineServices.AMULETS[kind].use(other.level(),other,InteractionHand.MAIN_HAND)==InteractionResult.SUCCESS,"registered amulet hand use equips "+kind);
    check(AccessoryEquipment.equipped(other,kind)&&other.getMainHandItem().getCount()==1,"moves exactly one charm to persistent accessory slot "+kind);
    ShrineServices.AMULETS[kind].use(other.level(),other,InteractionHand.MAIN_HAND);
    check(other.getMainHandItem().getCount()==1,"duplicate kind use neither consumes nor stacks "+kind);
   }
   int accessories=0;for(int slot=0;slot<AccessoryEquipment.SLOTS;slot++)accessories+=AccessoryEquipment.get(other,slot).getCount();
   check(accessories==3,"all three registered amulets coexist, one per slot");
   check(p.level().isPositionEntityTicking(SakuraTownPlan.STAFF),"parent warmed staff chunk before integration");ShrineServices.spawn(p.level().getServer());
   check(p.level().getEntity(ShrineServices.STAFF_UUID) instanceof CityResident,"one real miko present");var npc=(CityResident)p.level().getEntity(ShrineServices.STAFF_UUID);
   check(npc.job.equals(ShrineServices.JOB)&&npc.home.equals(SakuraTownPlan.STAFF)&&npc.isNoAi()&&npc.isNoGravity()&&npc.isPersistenceRequired(),"fixed persistent miko metadata");
   ShrineServices.spawn(p.level().getServer());check(p.level().getEntity(ShrineServices.STAFF_UUID)==npc,"repeat spawn preserves same entity");
   npc.setPos(npc.position().add(1,1,1));npc.tick();check(npc.position().equals(Vec3.atBottomCenterOf(SakuraTownPlan.STAFF)),"parent tick hook resets miko to exact home");
   open(p,2);ShrineServices.SESSIONS.remove(p.getUUID());limits(p).open=0;check(ShrineServices.use(p,npc)&&ShrineServices.SESSIONS.get(p.getUUID()).kind()==2,"real miko opens shrine-specific shop");
   ShrineServices.SESSIONS.remove(p.getUUID());limits(p).open=0;npc.mobInteract(p,InteractionHand.MAIN_HAND);check(ShrineServices.SESSIONS.containsKey(p.getUUID())&&ShrineServices.SESSIONS.get(p.getUUID()).kind()==2,"parent interaction hook opens shrine screen");
   var fake=new CityResident(CityResidents.TYPES.get(3),p.level());fake.home=SakuraTownPlan.STAFF;fake.job=ShrineServices.JOB;fake.shopStaff=true;
   check(!ShrineServices.use(p,fake)&&!ShrineServices.freeze(fake),"miko impostor UUID rejected");fake.discard();
   String job=npc.job;try{npc.job="bar";check(!ShrineServices.use(p,npc),"wrong NPC job rejected");}finally{npc.job=job;}
   var savedSession=ShrineServices.SESSIONS.get(p.getUUID());ShrineServices.SESSIONS.put(p.getUUID(),new ShrineServices.Session(2,savedSession.token(),ShrineServices.now(p)-1,SakuraTownPlan.COUNTER));
   check(ShrineServices.request(p,"buy",savedSession.token(),0)==0,"expired shop cannot charge");
   System.out.println("SHRINE_SERVICES_SYNC_PASS checks="+checks+" exact anchors, quotes, purchase rollback, replay/proximity, three equippable charms, fixed authenticated miko");
  }finally{
   StockMarket.file=file;StockMarket.ledger.accounts.put(p.getStringUUID(),StockMarket.JSON.fromJson(savedAccount,MarketLedger.Account.class));StockMarket.save();
   Cyberware.restore(p,inventory);p.removeAllEffects();for(var effect:effects)p.addEffect(effect);p.setGameMode(mode);
   p.teleportTo(level,pos.x,pos.y,pos.z,Set.of(),yaw,pitch,true);if(session==null)ShrineServices.SESSIONS.remove(p.getUUID());else ShrineServices.SESSIONS.put(p.getUUID(),session);
   if(oldLimits==null)ShrineServices.LIMITS.remove(p.getUUID());else ShrineServices.LIMITS.put(p.getUUID(),oldLimits);
  }
 }
}
