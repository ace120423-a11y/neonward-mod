package jp.neonward;

import java.util.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.commands.Commands;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;

/** Isolated room in CompactShops.DIM; deliberately absent from CompactShops room indexes. */
public final class TrainingRange {
 public record Meter(String line) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
  public static final Type<Meter> TYPE=new Type<>(NeonWard.id("training_meter"));
  public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf,Meter> CODEC=new net.minecraft.network.codec.StreamCodec<>(){
   public Meter decode(net.minecraft.network.RegistryFriendlyByteBuf b){return new Meter(b.readUtf(512));}
   public void encode(net.minecraft.network.RegistryFriendlyByteBuf b,Meter value){b.writeUtf(value.line(),512);}
  };
  public Meter{Objects.requireNonNull(line);if(line.length()>512)throw new IllegalArgumentException("Training meter exceeds 512 characters");}
  public Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type(){return TYPE;}
 }
 public static final int X=2048,MAX_X=X+112,MAX_Z=30,FLOOR=64,CEILING=72;
 public static final BlockPos RETURN_TERMINAL=new BlockPos(X+2,65,27);
 public static final BlockPos RESET_TERMINAL=new BlockPos(X+5,65,27);
 static final int[] DISTANCES={10,25,50,100};
 static final AABB BOUNDS=new AABB(X,64,0,MAX_X+1,73,MAX_Z+1);
 static final Map<UUID,TrainingStats> STATS=new HashMap<>();
 private static final Map<UUID,Long> NEXT=new HashMap<>();
 private static final Map<Integer,TrainingDummy> targets=new HashMap<>();
 private static final Map<Vec3,Display.TextDisplay> labels=new HashMap<>();
 private static ServerLevel prepared;
 private static boolean initialized;
 public static EntityType<TrainingDummy> DUMMY;
 private TrainingRange(){}
 /** Call only from the client initializer, after init() registered the entity type. */
 public static void initClient(){TrainingClient.initClient();}
 public static boolean contains(Level level,BlockPos at){return level.dimension()==CompactShops.DIM&&at.getX()>=X&&at.getX()<=MAX_X&&at.getY()>=64&&at.getY()<=72&&at.getZ()>=0&&at.getZ()<=MAX_Z;}
 static boolean active(ServerLevel l){return l.players().stream().anyMatch(p->contains(l,p.blockPosition()));}
 static Vec3 targetPosition(int i){return i<4?new Vec3(X+8.5+DISTANCES[i],65,6.5+i*6):new Vec3(X+4.5,65,6.5+(i-4)*2);}
 static void record(ServerPlayer p,double damage){STATS.computeIfAbsent(p.getUUID(),id->new TrainingStats()).hit(p.level().getGameTime(),damage);}
 static int message(ServerPlayer p,String value){p.sendSystemMessage(Component.literal("TRAINING / "+value));return 0;}
 static boolean throttle(ServerPlayer p){long now=p.level().getServer().getTickCount();if(now<NEXT.getOrDefault(p.getUUID(),0L))return false;NEXT.put(p.getUUID(),now+10);return true;}
 static Block concrete(String color){return BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(color+"_concrete"));}
 static void put(ServerLevel l,int x,int y,int z,Block b){l.setBlock(new BlockPos(x,y,z),b.defaultBlockState(),2);}
 static void build(ServerLevel l){
  for(int x=X;x<=MAX_X;x++)for(int z=0;z<=MAX_Z;z++)for(int y=FLOOR;y<=CEILING;y++){
   boolean shell=x==X||x==MAX_X||z==0||z==MAX_Z||y==CEILING;
   Block b=y==FLOOR?Blocks.POLISHED_DEEPSLATE:shell?concrete("gray"):Blocks.AIR;
   if(y==CEILING&&x%4==0&&z%4==2)b=Blocks.SEA_LANTERN;
   if(y==FLOOR&&x==X+8)b=concrete("yellow");
   if(y==FLOOR&&z>=3&&z<=27&&z%6==3&&x>X+8)b=concrete("cyan");
   put(l,x,y,z,b);
  }
  // Low lane dividers cannot intercept muzzle/eye rays or obstruct the return walkway.
  for(int z=3;z<=27;z+=6)for(int x=X+9;x<=MAX_X-1;x++)put(l,x,65,z,Blocks.SMOOTH_STONE_SLAB);
  for(int i=0;i<4;i++){var at=BlockPos.containing(targetPosition(i));for(int y=65;y<=68;y++)for(int z=at.getZ()-1;z<=at.getZ()+1;z++)put(l,at.getX()+2,y,z,concrete("white"));}
  l.setBlock(RETURN_TERMINAL,Blocks.LODESTONE.defaultBlockState(),2);
  l.setBlock(RESET_TERMINAL,Blocks.EMERALD_BLOCK.defaultBlockState(),2);
  put(l,X,63,0,Blocks.LODESTONE);
 }
 static class TrainingLabel extends Display.TextDisplay {
  TrainingLabel(ServerLevel l){super(EntityTypes.TEXT_DISPLAY,l);}
  @Override public boolean shouldBeSaved(){return false;}
 }
 static void label(ServerLevel l,double x,double y,double z,String text){
  var key=new Vec3(x,y,z);var previous=labels.get(key);
  if(previous!=null&&!previous.isRemoved())return;
  labels.remove(key);if(!l.hasChunkAt(BlockPos.containing(key)))return;
  var e=new TrainingLabel(l);
  ClockworkFeedback.load(e,l,"{billboard:'center',background:-1274542030,brightness:{block:15,sky:15},line_width:320,Invulnerable:1b}");
  ((jp.neonward.mixin.MeterTextAccess)e).neonSetText(Component.literal(text).withColor(0x83fff0));
  e.setPos(x,y,z);if(l.addFreshEntity(e))labels.put(key,e);
 }
 static void populate(ServerLevel l){
  // Repair each fixed slot independently. An unloaded distant lane or label must
  // never reset a nearby target, its identity, or its in-progress elemental effects.
  for(int i=0;i<7;i++){
   var previous=targets.get(i);if(previous!=null&&!previous.isRemoved())continue;
   if(previous!=null){previous.resetTarget();targets.remove(i);}
   if(!l.hasChunkAt(BlockPos.containing(targetPosition(i))))continue;
   var e=new TrainingDummy(DUMMY,l);e.anchor(targetPosition(i));
   e.setCustomName(Component.literal(i<4?DISTANCES[i]+" m / TARGET":"MELEE / COMBO "+(i-3)));e.setCustomNameVisible(true);
   if(l.addFreshEntity(e))targets.put(i,e);
  }
  for(int i=0;i<4;i++){var at=targetPosition(i);label(l,X+8.5,68,at.z,"LANE "+(i+1)+" / "+DISTANCES[i]+" m →");label(l,at.x,68.3,at.z,DISTANCES[i]+" m");}
  label(l,X+3,68,8.5,"MELEE / ELEMENT / COMBO\nFixed targets • no kill rewards");
  label(l,X+2.5,67.5,27.5,"RETURN / 街へ戻る\nRight-click • /neonrange leave");
  label(l,X+5.5,67.5,27.5,"RESET / 自分の計測をリセット\nRight-click • /neonrange reset");
  label(l,X+8.5,69.5,29,"NEON TRAINING\nYellow line: 10 / 25 / 50 / 100 m\nLast hit • Total • DPS (rolling 5 s)\n/neonrange melee — melee bay\n/neonrange targets — clear target effects (shared)");
 }
 static void ensure(ServerLevel l){
  if(prepared!=l){
   for(int x=X>>4;x<=MAX_X>>4;x++)for(int z=0;z<=MAX_Z>>4;z++)l.getChunk(x,z);
   if(!l.getBlockState(new BlockPos(X,63,0)).is(Blocks.LODESTONE))build(l);
   prepared=l;
  }
  populate(l);
 }
 /** Entry accepts the dedicated placed kiosk, weapon shop, or its street frontage. */
 public static boolean enter(ServerPlayer p){
  if(!p.isAlive()||p.isSpectator()||p.isPassenger()||!throttle(p))return false;
  if(contains(p.level(),p.blockPosition())){ensure(p.level());return true;}
  var shop=CompactShops.ALL[0];
  boolean street=p.level().dimension()==Level.OVERWORLD&&p.getY()>=60&&p.getY()<=72&&Math.hypot(p.getX()-shop.x(),p.getZ()-shop.z())<=24;
  if(!street&&!LeisureShop.near(p,3)&&!(p.level().dimension()==CompactShops.DIM&&CompactShops.room(p.level(),p.blockPosition())==0)){message(p,"武器屋横の訓練端末から入場してください");return false;}
  var l=p.level().getServer().getLevel(CompactShops.DIM);if(l==null){message(p,"Range dimension unavailable; restart after installation.");return false;}
  // Validate the standard street destination before admitting anyone.
  if(PhoneTravel.landing(p.level().getServer().overworld(),p,returnPoint())==null){message(p,"武器屋前の帰還先が塞がっています");return false;}
  ensure(l);
  if(!p.teleportTo(l,X+8.5,65,28.5,Set.of(),180,0,true))return false;
  p.fallDistance=0;p.setDeltaMovement(Vec3.ZERO);STATS.put(p.getUUID(),new TrainingStats());
  message(p,"黄色線から射撃 / 左の近接標的 / 帰還端末または /neonrange leave");return true;
 }
 static PhoneTravel.Point returnPoint(){var shop=CompactShops.ALL[0];return new PhoneTravel.Point("武器屋前",shop.x()+1,shop.z()+4);}
 /** Uses the same collision/hazard/door-aware street search as phone travel; no phone required. */
 public static boolean leave(ServerPlayer p){
  if(!contains(p.level(),p.blockPosition()))return false;
  var l=p.level().getServer().overworld();var at=PhoneTravel.landing(l,p,returnPoint());
  if(at==null)for(var fallback:PhoneTravel.POINTS){at=PhoneTravel.landing(l,p,fallback);if(at!=null){message(p,"武器屋前が塞がっているため "+fallback.name()+" へ帰還します");break;}}
  if(at==null){message(p,"街の安全な帰還先がすべて塞がっています。通路の復旧が必要です");return false;}
  if(p.isPassenger())p.stopRiding();
  if(!p.teleportTo(l,at.x,at.y,at.z,Set.of(),0,0,true))return false;
  p.fallDistance=0;p.setDeltaMovement(Vec3.ZERO);CompactShops.cooldown.remove(p.getUUID());STATS.remove(p.getUUID());
  if(prepared!=null&&!active(prepared))cleanupEntities();return true;
 }
 public static int reset(ServerPlayer p){if(!contains(p.level(),p.blockPosition())||!throttle(p))return 0;STATS.put(p.getUUID(),new TrainingStats());message(p,"自分の計測をリセットしました (継続中の効果は計測を続けます)");return 1;}
 static int resetTargets(ServerPlayer p){if(!contains(p.level(),p.blockPosition())||!throttle(p))return 0;for(var e:targets.values())e.resetTarget();clearShots();STATS.put(p.getUUID(),new TrainingStats());message(p,"共有標的の効果と自分の計測をリセットしました");return 1;}
 static int melee(ServerPlayer p){if(!contains(p.level(),p.blockPosition())||!p.isAlive()||p.isSpectator()||p.isPassenger()||!throttle(p))return 0;return p.teleportTo(p.level(),X+2.5,65,8.5,Set.of(),-90,0,true)?1:0;}
 static void clearShots(){ArsenalExpansion.SHOTS.removeIf(s->{if(s.level()!=prepared||!contains(s.level(),BlockPos.containing(s.position())))return false;VisibleBolts.remove(s);return true;});}
 static void cleanupEntities(){for(var e:targets.values()){e.resetTarget();e.discard();}targets.clear();for(var e:labels.values())e.discard();labels.clear();clearShots();}
 static void tick(net.minecraft.server.MinecraftServer server){
  if(server.getTickCount()%10!=0)return;
  STATS.keySet().removeIf(id->{var p=server.getPlayerList().getPlayer(id);return p==null||!contains(p.level(),p.blockPosition());});
  if(prepared==null){var l=server.getLevel(CompactShops.DIM);if(l==null||!active(l))return;ensure(l);}
  if(!active(prepared)){cleanupEntities();return;}
  if(server.getTickCount()%100==0)populate(prepared);
  for(var p:prepared.players())if(contains(prepared,p.blockPosition())){
   var s=STATS.computeIfAbsent(p.getUUID(),id->new TrainingStats());
   if(ServerPlayNetworking.canSend(p,Meter.TYPE))ServerPlayNetworking.send(p,new Meter(String.format(Locale.ROOT,"TRAINING  Hit %.1f  Total %.1f  DPS/5s %.1f  Hits %d",s.last(),s.total(),s.dps(prepared.getGameTime()),s.hits())));
  }
 }
 public static void init(){
  if(initialized)return;initialized=true;
  PayloadTypeRegistry.clientboundPlay().register(Meter.TYPE,Meter.CODEC);
  var key=ResourceKey.create(Registries.ENTITY_TYPE,NeonWard.id("training_dummy"));
  DUMMY=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<TrainingDummy>of(TrainingDummy::new,MobCategory.MISC).sized(.6f,1.95f).clientTrackingRange(12).noLootTable().build(key));
  FabricDefaultAttributeRegistry.register(DUMMY,TrainingDummy.createAttributes().add(Attributes.MAX_HEALTH,100).add(Attributes.ARMOR,0).add(Attributes.MOVEMENT_SPEED,0).add(Attributes.ATTACK_DAMAGE,0).add(Attributes.SPAWN_REINFORCEMENTS_CHANCE,0).add(Attributes.KNOCKBACK_RESISTANCE,1));
  CommandRegistrationCallback.EVENT.register((d,c,e)->d.register(Commands.literal("neonrange")
   .executes(ctx->{message(ctx.getSource().getPlayerOrException(),"enter | leave | reset | melee | targets (shared effect reset)");return 1;})
   .then(Commands.literal("enter").executes(ctx->enter(ctx.getSource().getPlayerOrException())?1:0))
   .then(Commands.literal("leave").executes(ctx->leave(ctx.getSource().getPlayerOrException())?1:0))
   .then(Commands.literal("reset").executes(ctx->reset(ctx.getSource().getPlayerOrException())))
   .then(Commands.literal("targets").executes(ctx->resetTargets(ctx.getSource().getPlayerOrException())))
   .then(Commands.literal("melee").executes(ctx->melee(ctx.getSource().getPlayerOrException())))));
  UseBlockCallback.EVENT.register((p,l,hand,hit)->{
   if(!contains(l,hit.getBlockPos())||!contains(l,p.blockPosition()))return InteractionResult.PASS;
   if(hit.getBlockPos().equals(RETURN_TERMINAL)||hit.getBlockPos().equals(RESET_TERMINAL)){
    if(hand==InteractionHand.MAIN_HAND&&p instanceof ServerPlayer sp){if(hit.getBlockPos().equals(RETURN_TERMINAL))leave(sp);else reset(sp);}return InteractionResult.SUCCESS;
   }
   return p.getItemInHand(hand).getItem() instanceof BlockItem||p.getItemInHand(hand).getItem() instanceof BucketItem?InteractionResult.FAIL:InteractionResult.PASS;
  });
  PlayerBlockBreakEvents.BEFORE.register((l,p,at,state,entity)->!contains(l,at));
  ServerTickEvents.END_SERVER_TICK.register(TrainingRange::tick);
  ServerPlayConnectionEvents.DISCONNECT.register((h,s)->{STATS.remove(h.player.getUUID());NEXT.remove(h.player.getUUID());});
  ServerLifecycleEvents.SERVER_STOPPED.register(s->{cleanupEntities();STATS.clear();NEXT.clear();prepared=null;});
 }
}
