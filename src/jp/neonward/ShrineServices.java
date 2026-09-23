package jp.neonward;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import com.google.gson.*;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.commands.Commands;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Fixed, fail-closed shrine services. Opening a quote never spends money or grants effects. */
public final class ShrineServices {
 public static final UUID STAFF_UUID=UUID.nameUUIDFromBytes("neonward/resident/sakura_shrine".getBytes(StandardCharsets.UTF_8));
 public static final String JOB="sakura_shrine";
 public static final Item[] AMULETS=new Item[3];
 static final String[] IDS={"shrine_amulet_travel","shrine_amulet_guard","shrine_amulet_fortune"};
 static final String[] NAMES={"旅路のお守り","厄除けのお守り","福運のお守り"};
 static final String[] EFFECTS={"装備中：移動速度+20%","装備中：被ダメージ-20%","装備中：幸運+1"};
 static final int[] PRICES={800,1000,1200};
 static final int WASH_COOLDOWN=600,OFFER_COOLDOWN=6000;
 static final String[] TITLES={"桜宮 / お賽銭","桜宮 / 手水舎","桜宮 / お守り授与所"};
 record Session(int kind,int token,long expires,BlockPos anchor){}
 static final Map<UUID,Session> SESSIONS=new HashMap<>();
 // One compact state per recent visitor, expired entries swept every five seconds.
 static final class Limits {long open,wash,offer;long last(){return Math.max(Math.max(open,wash),offer);}}
 static final Map<UUID,Limits> LIMITS=new HashMap<>();
 private static final SecureRandom RANDOM=new SecureRandom();private static boolean initialized;
 private ShrineServices(){}
 public record Reply(String json) implements CustomPacketPayload {
  public static final Type<Reply> TYPE=new Type<>(NeonWard.id("shrine_services_v1"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Reply> CODEC=StreamCodec.composite(ByteBufCodecs.STRING_UTF8,Reply::json,Reply::new);
  public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 static long now(ServerPlayer p){return p.level().getServer().overworld().getGameTime();}
 static BlockPos anchor(int kind){return switch(kind){case 0->SakuraTownPlan.SAISEN;case 1->SakuraTownPlan.CHOZU;case 2->SakuraTownPlan.COUNTER;default->null;};}
 static Block block(int kind){return switch(kind){case 0->SakuraMaterials.get("sakura_saisen_box");case 1->SakuraMaterials.get("sakura_chozu_basin");case 2->SakuraMaterials.get("sakura_omamori_counter");default->null;};}
 static boolean validPlayer(ServerPlayer p){return p.isAlive()&&!p.isSpectator();}
 /** Shared authored placement contract: same footprint, block identities and facing as the blueprint. */
 public static Map<BlockPos,BlockState> fixtureParts(int kind){return switch(kind){
  case 0->SakuraMaterials.footprint("sakura_saisen_box",SakuraTownPlan.SAISEN,Direction.EAST);
  case 1->SakuraMaterials.footprint("sakura_chozu_basin",SakuraTownPlan.CHOZU,Direction.EAST);
  case 2->Map.of(SakuraTownPlan.COUNTER,SakuraMaterials.facing("sakura_omamori_counter",SakuraTownPlan.STAFF_FACING));
  default->Map.of();
 };}
 static boolean intact(Level level,int kind){var parts=fixtureParts(kind);return !parts.isEmpty()&&parts.entrySet().stream().allMatch(e->level.getBlockState(e.getKey()).equals(e.getValue()));}
 public static int serviceAt(Level level,BlockPos clicked){
  if(level.dimension()!=Level.OVERWORLD)return -1;
  for(int kind=0;kind<3;kind++){var expected=fixtureParts(kind).get(clicked);if(expected!=null&&level.getBlockState(clicked).equals(expected))return kind;}return -1;
 }
 /** Block callback and tests share this path. A part never becomes an independent/relocatable terminal. */
 public static int useBlock(ServerPlayer p,BlockPos clicked){
  int kind=serviceAt(p.level(),clicked);if(kind<0||p.distanceToSqr(Vec3.atCenterOf(clicked))>36||ShrineRituals.active(p))return 0;return kind==1?ShrineRituals.start(p,ShrineRituals.WASH):open(p,kind);
 }
 public static boolean near(ServerPlayer p,int kind){
  var at=anchor(kind);var b=block(kind);
  return validPlayer(p)&&at!=null&&b!=null&&p.level().dimension()==Level.OVERWORLD&&SakuraTown.ready(p.level().getServer())
   &&p.distanceToSqr(Vec3.atCenterOf(at))<=36&&intact(p.level(),kind);
 }
 static Session quote(ServerPlayer p,int kind,int previous){int token;do{token=RANDOM.nextInt(Integer.MAX_VALUE);}while(token==previous);return new Session(kind,token,now(p)+600,anchor(kind));}
 public static int open(ServerPlayer p,int kind){
  if(!near(p,kind)||ShrineRituals.active(p))return 0;var limits=LIMITS.computeIfAbsent(p.getUUID(),id->new Limits());long time=now(p);if(time<limits.open)return 0;
  limits.open=time+10;var old=SESSIONS.get(p.getUUID());SESSIONS.put(p.getUUID(),quote(p,kind,old==null?-1:old.token()));
  reply(p,kind==0?"10秒のお参り完了時だけ100 Cr / 移動・被ダメージで中断（無料）":kind==1?"8秒の手水完了で毒解除・再生 I 5秒 / 完了後30秒待機":"右手で使用して装備 / インベントリの装飾品3枠 / 同種の重複不可");return 1;
 }
 static boolean purchase(ServerPlayer p,int kind){
  if(kind<0||kind>=AMULETS.length||StockMarket.ledger==null)return false;
  int slot=p.getInventory().getFreeSlot();if(slot<0)return false;var account=StockMarket.ledger.account(p.getStringUUID());long before=account.cash;if(before<PRICES[kind])return false;
  var saved=p.getInventory().getItem(slot).copy();
  try{account.cash-=PRICES[kind];p.getInventory().setItem(slot,new ItemStack(AMULETS[kind]));StockMarket.save();return true;}
  catch(Exception failure){account.cash=before;p.getInventory().setItem(slot,saved);return false;}
 }
 static boolean ritualAvailable(ServerPlayer p,int kind){
  var limits=LIMITS.computeIfAbsent(p.getUUID(),id->new Limits());
  if(kind==1)return now(p)>=limits.wash;
  return kind==0&&StockMarket.ledger!=null&&now(p)>=limits.offer&&!ShrineBlessings.active(p)&&StockMarket.ledger.account(p.getStringUUID()).cash>=100;
 }
 private static boolean offer(ServerPlayer p,Limits limits){
  if(!ritualAvailable(p,0))return false;
  var account=StockMarket.ledger.account(p.getStringUUID());long before=account.cash;if(before<100)return false;
  // One completion transaction on the server thread. Failed grants never debit;
  // failed ledger commits undo both the debit and this newly granted custom effect.
  try{
   if(!ShrineBlessings.grant(p))return false;account.cash-=100;StockMarket.save();
  }catch(Exception failure){account.cash=before;p.removeEffect(ShrineBlessings.ATTACK);p.removeEffect(ShrineBlessings.DEFENSE);return false;}
  limits.offer=now(p)+OFFER_COOLDOWN;return true;
 }
 static boolean wash(ServerPlayer p,Limits limits){
  if(now(p)<limits.wash)return false;p.removeEffect(MobEffects.POISON);p.addEffect(new MobEffectInstance(MobEffects.REGENERATION,100,0));limits.wash=now(p)+WASH_COOLDOWN;return true;
 }
 static boolean completeRitual(ServerPlayer p,int kind){
  if(!near(p,kind)||!ritualAvailable(p,kind))return false;var limits=LIMITS.computeIfAbsent(p.getUUID(),id->new Limits());
  return kind==0?offer(p,limits):kind==1&&wash(p,limits);
 }
 static void acknowledgeStart(ServerPlayer p,int kind,int token){
  if(!ServerPlayNetworking.canSend(p,Reply.TYPE))return;var json=new JsonObject();json.addProperty("ritualStarted",true);json.addProperty("kind",kind);json.addProperty("token",token);ServerPlayNetworking.send(p,new Reply(json.toString()));
 }
 public static int request(ServerPlayer p,String action,int token,int value){
  var s=SESSIONS.get(p.getUUID());
  if(s==null||s.token()!=token||ShrineRituals.active(p)||now(p)>s.expires()||!Objects.equals(s.anchor(),anchor(s.kind()))||!near(p,s.kind()))return 0;
  // Invalidate before any mutation: even rejected valid-quote attempts cannot be replayed.
  SESSIONS.remove(p.getUUID());boolean ok=false;
  if((s.kind()==0&&action.equals("offer")||s.kind()==1&&action.equals("wash"))&&value==0){
   if(ShrineRituals.start(p,s.kind())==1)return 1; // Start acknowledgement only; no quote reopening or inventory mutation.
  }else if(s.kind()==2&&action.equals("buy"))ok=purchase(p,value);
  SESSIONS.put(p.getUUID(),quote(p,s.kind(),token));p.getInventory().setChanged();p.containerMenu.broadcastChanges();
  reply(p,ok?"お守りを授与しました / 右手に持って右クリックで装備":
   "実行できません（課金なし）/ 残高・空き枠・効果・待機時間を確認してください");return ok?1:0;
 }
 static int activate(ServerPlayer p,InteractionHand hand,int kind){
  if(!validPlayer(p)||kind<0||kind>=AMULETS.length||hand!=InteractionHand.MAIN_HAND||!p.getItemInHand(hand).is(AMULETS[kind]))return 0;
  // The equipment backend alone transfers one item and owns slot/duplicate validation.
  if(ShrineRituals.active(p)||!AccessoryEquipment.equip(p,p.getItemInHand(hand)))return 0;
  p.sendSystemMessage(Component.literal(NAMES[kind]+"を装備 / "+EFFECTS[kind]+" / 装飾品3枠（同種重複不可）"));return 1;
 }
 static final class Amulet extends Item {
  final int kind;Amulet(Properties properties,int kind){super(properties);this.kind=kind;}
  @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
   if(hand!=InteractionHand.MAIN_HAND||player.isSpectator())return InteractionResult.PASS;
   if(player instanceof ServerPlayer p)return activate(p,hand,kind)==1?InteractionResult.SUCCESS:InteractionResult.FAIL;return InteractionResult.SUCCESS;
  }
 }
 static void reply(ServerPlayer p,String message){
  var s=SESSIONS.get(p.getUUID());if(s==null||!ServerPlayNetworking.canSend(p,Reply.TYPE))return;
  var data=new JsonObject();data.addProperty("kind",s.kind());data.addProperty("token",s.token());data.addProperty("title",TITLES[s.kind()]);data.addProperty("message",message);
  data.addProperty("cash",StockMarket.ledger==null?0:StockMarket.ledger.account(p.getStringUUID()).cash);var rows=new JsonArray();
  int count=s.kind()==2?3:1;for(int i=0;i<count;i++){var row=new JsonObject();row.addProperty("name",s.kind()==0?"お賽銭を奉納する":s.kind()==1?"手を清める":NAMES[i]);
   row.addProperty("effect",s.kind()==0?"攻撃+10% または被ダメージ-10% / 5分 / ランダム1種":s.kind()==1?"8秒間の手水 / 完了で毒解除・再生 I 5秒":EFFECTS[i]);
   row.addProperty("price",s.kind()==0?100:s.kind()==1?0:PRICES[i]);row.addProperty("action",s.kind()==0?"offer":s.kind()==1?"wash":"buy");row.addProperty("icon",s.kind()==2?i:s.kind()==0?-1:-2);rows.add(row);
  }data.add("rows",rows);ServerPlayNetworking.send(p,new Reply(data.toString()));
 }
 static boolean authentic(CityResident npc){return STAFF_UUID.equals(npc.getUUID())&&JOB.equals(npc.job)&&npc.shopStaff&&SakuraTownPlan.STAFF.equals(npc.home)&&npc.level().dimension()==Level.OVERWORLD;}
 public static boolean use(ServerPlayer p,CityResident npc){
  if(!authentic(npc))return false;
  if(npc.isAlive()&&p.level()==npc.level()&&p.level().getEntity(STAFF_UUID)==npc&&npc.position().distanceToSqr(Vec3.atBottomCenterOf(SakuraTownPlan.STAFF))<1&&p.distanceToSqr(npc)<=36)open(p,2);
  return true;
 }
 public static boolean freeze(CityResident npc){
  if(npc.level().isClientSide()||!authentic(npc)||!(npc.level() instanceof ServerLevel l)||!SakuraTown.ready(l.getServer()))return false;
  npc.setNoAi(true);npc.setNoGravity(true);npc.setPersistenceRequired();npc.getNavigation().stop();npc.setDeltaMovement(Vec3.ZERO);npc.setPos(Vec3.atBottomCenterOf(SakuraTownPlan.STAFF));
  var delta=Vec3.atCenterOf(SakuraTownPlan.COUNTER).subtract(npc.position());float yaw=(float)Math.toDegrees(Math.atan2(-delta.x,delta.z));npc.setYRot(yaw);npc.setYHeadRot(yaw);npc.setYBodyRot(yaw);
  npc.setCustomName(Component.literal("桜宮の巫女 / お守り授与"));return true;
 }
 static void spawn(MinecraftServer server){
  if(!SakuraTown.ready(server)||CityResidents.TYPES.size()<4)return;var level=server.overworld();var at=SakuraTownPlan.STAFF;
  if(!level.isPositionEntityTicking(at)||!intact(level,2)||level.getEntity(STAFF_UUID)!=null)return;
  if(!level.getBlockState(at.below()).isCollisionShapeFullBlock(level,at.below())||!level.getBlockState(at).isAir()||!level.getBlockState(at.above()).isAir())return;
  var npc=new CityResident(CityResidents.TYPES.get(3),level);npc.setUUID(STAFF_UUID);npc.home=at;npc.shopStaff=true;npc.job=JOB;freeze(npc);
  if(level.noCollision(npc))level.addFreshEntity(npc);
 }
 static void tick(MinecraftServer server){
  if(server.getTickCount()%100!=0)return;long time=server.overworld().getGameTime();SESSIONS.entrySet().removeIf(e->e.getValue().expires()<time);
  LIMITS.entrySet().removeIf(e->e.getValue().last()<=time);spawn(server);
 }
 public static void init(){
  if(initialized)return;initialized=true;
  for(int i=0;i<AMULETS.length;i++){var id=NeonWard.id(IDS[i]);AMULETS[i]=Registry.register(BuiltInRegistries.ITEM,id,new Amulet(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id)).stacksTo(16).component(DataComponents.ITEM_NAME,Component.literal(NAMES[i])),i));}
  PayloadTypeRegistry.clientboundPlay().register(Reply.TYPE,Reply.CODEC);
  UseBlockCallback.EVENT.register((p,l,hand,hit)->{
   if(hand!=InteractionHand.MAIN_HAND||l.dimension()!=Level.OVERWORLD)return InteractionResult.PASS;
   if(serviceAt(l,hit.getBlockPos())<0)return InteractionResult.PASS;if(p instanceof ServerPlayer sp)useBlock(sp,hit.getBlockPos());return InteractionResult.SUCCESS;
  });
  CommandRegistrationCallback.EVENT.register((d,c,e)->{
   var root=Commands.literal("neonshrine").then(Commands.literal("open").then(Commands.argument("service",IntegerArgumentType.integer(0,2)).executes(ctx->open(ctx.getSource().getPlayerOrException(),IntegerArgumentType.getInteger(ctx,"service")))));
   for(var action:List.of("offer","wash","buy"))root.then(Commands.literal(action).then(Commands.argument("token",IntegerArgumentType.integer(0)).then(Commands.argument("value",IntegerArgumentType.integer(0,2)).executes(ctx->request(ctx.getSource().getPlayerOrException(),action,IntegerArgumentType.getInteger(ctx,"token"),IntegerArgumentType.getInteger(ctx,"value"))))));d.register(root);
  });
  ServerTickEvents.END_SERVER_TICK.register(ShrineServices::tick);
  ServerPlayConnectionEvents.DISCONNECT.register((h,s)->SESSIONS.remove(h.player.getUUID()));
  ServerLifecycleEvents.SERVER_STOPPED.register(s->{SESSIONS.clear();LIMITS.clear();});
 }
}
