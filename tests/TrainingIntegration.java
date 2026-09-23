package jp.neonward;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Installed hooks: NeonWard TrainingRange.init(), NeonClient TrainingRange.initClient(),
 * PhoneTravel.source range inclusion, UiCommandLimiter neonrange root, and CompactShops
 * END_SERVER_TICK range skip before its room lookup/ejection/hostile cleanup.
 * CompactShops.room()/ALL remain unchanged; the kiosk calls TrainingRange.enter(p).
 *
 * Call run(visitor) from parent's isolated server test runner after normal initialization.
 * Run in a disposable city copy with safe weapon-shop pavement. No live-world test/install.
 * Epicurus / LeisureIntegration: after synchronous run(visitor), call runDelayed(visitor)
 * and emit LEISURE_TEST_COMPLETE / clean up the visitor ONLY in its whenComplete callback.
 * Never join/get on the server thread. The fixture pins only the room's 16 chunks,
 * waits at most 200 ticks for entity ticking, then releases its added tickets on completion.
 * The returned future completes after 105 measured real ticks,
 * checking the visitor and exact original targets across a 100-tick shop-cleanup boundary.
 * The visitor must be in the server player list (LeisureIntegration.visitor already is).
 * Render/scope smoke check still needs an actual client with the initClient hook.
 *
 * Meter semantics: damage after vanilla armor/effects/absorption, before immortal health
 * replacement (not actual lost HP); default target has zero armor. Normal immunity windows
 * remain; standard guns already clear them. Native fire belongs to the last successful
 * attacker at ignition; explicit elemental/bleed/echo sources retain their own player.
 * Targets stay fixed, so stun/slowness/fire VFX and damage are testable but physical
 * knockback/pull displacement and kill-triggered effects cannot be demonstrated here.
 * No damage hooks are needed in NeonArsenal, CyberEnemy, MeleeElements or the mixins.
 */
public final class TrainingIntegration {
 private static boolean delayedRegistered;
 private static Delayed delayed;
 private static final class Delayed {
  final ServerPlayer player;final net.minecraft.server.level.ServerLevel original,range;final Vec3 position;final float yaw,pitch;
  final Set<net.minecraft.world.level.ChunkPos> added=new HashSet<>();
  final java.util.concurrent.CompletableFuture<Void> result=new java.util.concurrent.CompletableFuture<>();
  final int deadline;int due;Set<UUID> targets;
  Delayed(ServerPlayer p,net.minecraft.server.level.ServerLevel l){player=p;original=p.level();position=p.position();yaw=p.getYRot();pitch=p.getXRot();range=l;deadline=l.getServer().getTickCount()+200;}
 }
 static void check(boolean ok,String why){if(!ok)throw new AssertionError("TRAINING: "+why);}
 static void close(double expected,double actual,String why){check(Math.abs(expected-actual)<.01,why+": "+expected+" != "+actual);}
 public static void assertStillInside(ServerPlayer p){check(TrainingRange.contains(p.level(),p.blockPosition()),"CompactShops tick must not eject the range visitor");}
 /** Nonblocking test continuation; one bounded pending visitor, one registered listener. */
 public static java.util.concurrent.CompletableFuture<Void> runDelayed(ServerPlayer p){
  var server=p.level().getServer();check(server.isSameThread(),"delayed setup on server thread");
  check(delayed==null,"only one delayed regression at a time");
  check(server.getPlayerList().getPlayers().contains(p),"visitor must participate in CompactShops player loop");
  if(!delayedRegistered){
   delayedRegistered=true;
   net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(TrainingIntegration::delayedTick);
   net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPING.register(s->{if(delayed!=null&&delayed.range.getServer()==s)finishDelayed(new AssertionError("server stopped before delayed regression completed"));});
  }
  var range=server.getLevel(CompactShops.DIM);check(range!=null,"range dimension");var test=new Delayed(p,range);delayed=test;
  try{
   for(int x=TrainingRange.X>>4;x<=TrainingRange.MAX_X>>4;x++)for(int z=0;z<=TrainingRange.MAX_Z>>4;z++){
    var chunk=new net.minecraft.world.level.ChunkPos(x,z);
    if(!range.getForceLoadedChunks().contains(chunk.pack())){test.added.add(chunk);range.setChunkForced(x,z,true);}range.getChunk(x,z);
   }
   TrainingRange.ensure(range);
   check(p.teleportTo(range,TrainingRange.X+8.5,65,6.5,Set.of(),-90,0,true),"delayed range setup");
   CompactShops.cooldown.remove(p.getUUID()); // A shop cooldown must not hide the regression.
  }catch(Throwable failure){finishDelayed(failure);}
  return test.result;
 }
 private static void delayedTick(net.minecraft.server.MinecraftServer server){
  var test=delayed;if(test==null||test.range.getServer()!=server)return;
  try{
   assertStillInside(test.player);
   if(test.targets==null){
    check(server.getTickCount()<=test.deadline,"range chunks did not become entity-ticking within 200 ticks");
    for(int x=TrainingRange.X>>4;x<=TrainingRange.MAX_X>>4;x++)for(int z=0;z<=TrainingRange.MAX_Z>>4;z++)
     if(!test.range.areEntitiesActuallyLoadedAndTicking(new net.minecraft.world.level.ChunkPos(x,z)))return;
    TrainingRange.populate(test.range);var ids=new HashSet<UUID>();for(var e:test.range.getEntitiesOfClass(TrainingDummy.class,TrainingRange.BOUNDS))ids.add(e.getUUID());
    check(ids.size()==7,"fully loaded range has seven targets");test.targets=Set.copyOf(ids);test.due=server.getTickCount()+105;
    System.out.println("TRAINING_DELAYED_READY: all 16 room chunks entity-ticking; strict 105-tick identity check starts");return;
   }
   var ids=new HashSet<UUID>();for(var e:test.range.getEntitiesOfClass(TrainingDummy.class,TrainingRange.BOUNDS))ids.add(e.getUUID());
   var missing=new HashSet<>(test.targets);missing.removeAll(ids);var added=new HashSet<>(ids);added.removeAll(test.targets);
   check(ids.equals(test.targets),"shop tick must not remove or respawn original training targets; missing="+missing+", added="+added+", tick="+server.getTickCount());
   if(server.getTickCount()>=test.due)finishDelayed(null);
  }catch(Throwable failure){finishDelayed(failure);}
 }
 private static void finishDelayed(Throwable failure){
  var test=delayed;if(test==null)return;delayed=null;
  try{
   check(test.player.teleportTo(test.original,test.position.x,test.position.y,test.position.z,Set.of(),test.yaw,test.pitch,true),"restore delayed visitor");
   TrainingRange.STATS.remove(test.player.getUUID());if(!TrainingRange.active(test.range))TrainingRange.cleanupEntities();
  }catch(Throwable cleanup){if(failure==null)failure=cleanup;else failure.addSuppressed(cleanup);}
  finally{for(var chunk:test.added)test.range.setChunkForced(chunk.x(),chunk.z(),false);}
  if(failure==null){System.out.println("TRAINING_DELAYED_PASS: 105 real ticks, no ejection, original seven targets survive shop cleanup boundary, fixture tickets released");test.result.complete(null);}
  else test.result.completeExceptionally(failure);
 }
 // Disposable-world fixture: the kiosk is within the allowed anchor search radius,
 // while a player within kiosk reach is beyond the legacy 24-block door radius.
 static void enterOffsetKiosk(ServerPlayer p){
  var l=p.level().getServer().overworld();var fixture=new BlockPos(97,65,484);l.getChunkAt(fixture);
  var state=l.getBlockState(fixture);BlockPos[] anchors;
  try{var field=LeisureSites.class.getDeclaredField("ACTIVE");field.setAccessible(true);anchors=(BlockPos[])field.get(null);}catch(ReflectiveOperationException e){throw new AssertionError(e);}
  var saved=anchors[3];
  try{
   check(LeisureSites.COUNTERS[3]!=null,"leisure initialization");anchors[3]=fixture;l.setBlock(fixture,LeisureSites.COUNTERS[3].defaultBlockState(),3);
   check(p.teleportTo(l,103.5,65,484.5,Set.of(),0,0,true),"offset kiosk setup");
   check(Math.hypot(p.getX()-CompactShops.ALL[0].x(),p.getZ()-CompactShops.ALL[0].z())>24,"fixture exceeds legacy door radius");
   check(LeisureShop.near(p,3),"dedicated kiosk validates reach and actual counter");
   check(TrainingRange.enter(p),"entry at dedicated kiosk beyond legacy door radius");
  }finally{anchors[3]=saved;l.setBlock(fixture,state,3);}
 }
 public static void run(ServerPlayer p){
  var originalLevel=p.level();var original=p.position();float yaw=p.getYRot(),pitch=p.getXRot();var hand=p.getMainHandItem().copy();
  try{
   check(TrainingRange.DUMMY!=null,"init hook registered dedicated type");
   enterOffsetKiosk(p);
   assertStillInside(p);var l=p.level();
   check(CompactShops.room(l,p.blockPosition())==-1,"shop room index unchanged");
   check(PhoneTravel.source(p),"parent added PhoneTravel source hook");
   check(!TrainingRange.contains(l,new BlockPos(TrainingRange.X-1,65,1)),"west boundary");
   check(!TrainingRange.contains(l,new BlockPos(TrainingRange.X,73,1)),"upper boundary");
   check(!TrainingRange.contains(l.getServer().overworld(),p.blockPosition()),"dimension boundary");
   var dummies=l.getEntitiesOfClass(TrainingDummy.class,TrainingRange.BOUNDS);
   check(dummies.size()==7,"exactly four ranged and three melee targets");
   TrainingRange.ensure(l);check(l.getEntitiesOfClass(TrainingDummy.class,TrainingRange.BOUNDS).size()==7,"ensure idempotent");
   check(l.getEntitiesOfClass(Display.TextDisplay.class,TrainingRange.BOUNDS).size()==12,"bounded labels");
   var removed=dummies.getLast();var survivors=new HashSet<UUID>();for(var e:dummies)if(e!=removed)survivors.add(e.getUUID());removed.discard();
   TrainingRange.populate(l);dummies=l.getEntitiesOfClass(TrainingDummy.class,TrainingRange.BOUNDS);
   check(dummies.size()==7&&dummies.stream().filter(e->survivors.contains(e.getUUID())).count()==6,"one missing slot preserves six existing target identities");
   var stableIds=new HashSet<UUID>();for(var e:dummies)stableIds.add(e.getUUID());
   l.getEntitiesOfClass(Display.TextDisplay.class,TrainingRange.BOUNDS).getLast().discard();TrainingRange.populate(l);
   check(l.getEntitiesOfClass(TrainingDummy.class,TrainingRange.BOUNDS).stream().allMatch(e->stableIds.contains(e.getUUID()))&&l.getEntitiesOfClass(Display.TextDisplay.class,TrainingRange.BOUNDS).size()==12,"missing label repaired without target churn");
   var target=dummies.stream().min(Comparator.comparingDouble(e->e.position().distanceToSqr(TrainingRange.targetPosition(0)))).orElseThrow();
   check(!VanillaEnemyFilter.blocked(target)&&ArsenalExpansion.enemy(p,target),"native-filter safe and expansion-target compatible");
   check(!target.shouldBeSaved()&&!target.shouldDropExperience(),"no saved mobs or XP");
   var stats=TrainingRange.STATS.get(p.getUUID());stats.reset();
   check(target.hurtServer(l,p.damageSources().playerAttack(p),7),"normal damage accepted");
   close(7,stats.last(),"post-mitigation hit");close(100,target.getHealth(),"health remains full");
   check(!target.hurtServer(l,p.damageSources().playerAttack(p),7),"normal immunity window preserved");
   var absorptionCap=target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_ABSORPTION);
   check(absorptionCap!=null,"absorption capacity attribute exists");double originalCap=absorptionCap.getBaseValue();
   try{
    // Vanilla clamps setAbsorptionAmount to MAX_ABSORPTION (zero on a plain dummy).
    absorptionCap.setBaseValue(3);target.invulnerableTime=0;target.setAbsorptionAmount(3);
    close(3,target.getAbsorptionAmount(),"fixture really has three absorption points");
    check(target.hurtServer(l,p.damageSources().playerAttack(p),7),"absorbed hit accepted");
    close(4,stats.last(),"meter excludes actual absorption");close(0,target.getAbsorptionAmount(),"absorption consumed");
   }finally{target.setAbsorptionAmount(0);absorptionCap.setBaseValue(originalCap);}
   target.invulnerableTime=0;check(target.hurtServer(l,p.damageSources().playerAttack(p),10000),"overkill accepted");
   close(10000,stats.last(),"overkill not clipped to target HP");check(target.isAlive(),"immortal");
   check(!target.hurtServer(l,p.damageSources().playerAttack(p),Float.NaN),"reject malformed damage");
   target.die(p.damageSources().playerAttack(p));check(target.isAlive(),"explicit die does not enter death/loot path");
   check(l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,TrainingRange.BOUNDS).isEmpty(),"no enemy loot");
   check(l.getEntitiesOfClass(ExperienceOrb.class,TrainingRange.BOUNDS).isEmpty(),"no XP");
   var anchor=target.position();target.setDeltaMovement(new Vec3(3,1,0));target.move(MoverType.SELF,new Vec3(3,1,0));close(0,anchor.distanceTo(target.position()),"fixed target");
   target.resetTarget();stats.reset();
   p.setPos(TrainingRange.X+8.5,65,6.5);p.setYRot(-90);p.setXRot(0);
   var gun=new ItemStack(NeonArsenal.RIFLE);p.setItemInHand(InteractionHand.MAIN_HAND,gun);
   ((NeonArsenal.Rifle)gun.getItem()).fire(l,p,InteractionHand.MAIN_HAND);
   check(stats.total()>0&&GunReload.used(gun)==1,"production hitscan selects dummy and consumes normal magazine");
   target.resetTarget();stats.reset();target.invulnerableTime=0;
   check(target.hurtServer(l,PairedEffects.bleedSource(p),2),"production bleed source accepted");close(2,stats.last(),"bleed recorded");
   MeleeElements.applyElement(p,target,MeleeElements.Kind.POISON,l.getGameTime());check(MeleeElements.DOT.containsKey(target),"poison applied to enemy dummy");
   MeleeElements.applyElement(p,target,MeleeElements.Kind.THUNDER,l.getGameTime());check(MeleeElements.stunned(target),"stun applied");
   PairedEffects.bleed(p,target);check(PairedEffects.BLEEDS.containsKey(target),"bleed scheduled");
   ArsenalExpansion.impact(p,target,new ItemStack(NeonArsenal.ITEMS.get("neon_dualblades")),10);
   check(ArsenalExpansion.ECHOES.stream().anyMatch(e->e.target()==target),"combo follow-up scheduled");
   target.resetTarget();check(!MeleeElements.DOT.containsKey(target)&&!MeleeElements.STUN.containsKey(target)&&!PairedEffects.BLEEDS.containsKey(target)&&ArsenalExpansion.ECHOES.stream().noneMatch(e->e.target()==target),"target reset clears delayed effects");
   stats.reset();close(0,stats.total(),"personal meter reset");
   check(TrainingRange.leave(p),"no-phone exit");check(p.level().dimension()==Level.OVERWORLD,"street return dimension");
   check(PhoneTravel.safeBody(p.level(),p,p.position())&&!PhoneTravel.entrance(p.position()),"safe return outside automatic doors");
   check(Math.hypot(p.getX()-79,p.getZ()-481)<=18,"return near weapon shop");
   check(!TrainingRange.STATS.containsKey(p.getUUID()),"exit removes player meter");
   check(l.getEntitiesOfClass(TrainingDummy.class,TrainingRange.BOUNDS).isEmpty(),"empty room discards targets");
   check(l.getEntitiesOfClass(Display.TextDisplay.class,TrainingRange.BOUNDS).isEmpty(),"empty room discards labels");
   System.out.println("TRAINING_INTEGRATION_PASS: layout, bounds, targeting, immortality, no loot/XP, gun/reload, bleed, poison, stun, combo, reset, safe no-phone return, cleanup");
  }finally{
   TrainingRange.cleanupEntities();TrainingRange.STATS.remove(p.getUUID());p.setItemInHand(InteractionHand.MAIN_HAND,hand);
   p.teleportTo(originalLevel,original.x,original.y,original.z,Set.of(),yaw,pitch,true);
  }
 }
}
