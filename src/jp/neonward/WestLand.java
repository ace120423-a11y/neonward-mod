package jp.neonward;
import java.util.*;
import com.google.gson.*;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
/** A separate, server-owned land registry. No apartment/farm ownership is consulted. */
public final class WestLand {
 public record State(String json) implements CustomPacketPayload {
  public static final Type<State> TYPE=new Type<>(NeonWard.id("west_land"));
  public static final StreamCodec<RegistryFriendlyByteBuf,State> CODEC=StreamCodec.composite(ByteBufCodecs.STRING_UTF8,State::json,State::new);
  public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 public static final Map<Integer,String> CLIENT_OWNERS=new HashMap<>();
 record Session(int plot,int terminal,int token,long expires){}
 static final Map<UUID,Session> SESSIONS=new HashMap<>();static final java.security.SecureRandom RNG=new java.security.SecureRandom();
 public static boolean area(Level l,BlockPos p){return l.dimension()==Level.OVERWORLD&&LandLayout.area(p.getX(),p.getZ());}
 public static boolean owned(Player p,int id){if(p==null||id<0)return false;if(p.level().isClientSide())return p.getStringUUID().equals(CLIENT_OWNERS.get(id));if(StockMarket.ledger==null)return false;var o=StockMarket.ledger.westLand.get(id);return o!=null&&p.getStringUUID().equals(o.uuid);}
 public static boolean edit(Level l,Player p,BlockPos pos){int id=LandLayout.plot(pos.getX(),pos.getZ());return area(l,pos)&&LandLayout.editable(id,pos.getX(),pos.getY(),pos.getZ())&&owned(p,id)&&!p.isSpectator();}
 public static boolean transfer(Level l,BlockPos from,BlockPos to){if(!area(l,from)&&!area(l,to))return true;int id=LandLayout.plot(from.getX(),from.getZ());return LandLayout.samePlot(from.getX(),from.getY(),from.getZ(),to.getX(),to.getY(),to.getZ())&&StockMarket.ledger!=null&&StockMarket.ledger.westLand.containsKey(id);}
 public static BlockPos terminal(int id,int type){return new BlockPos(LandLayout.x(id)+5+type*4,65,id<4?138:165);}
 static int terminalAt(BlockPos p){for(int i=0;i<8;i++)for(int t=0;t<4;t++)if(terminal(i,t).equals(p))return i*4+t;return -1;}
 static boolean near(ServerPlayer p,int id,int t){return p.isAlive()&&!p.isSpectator()&&p.level().dimension()==Level.OVERWORLD&&p.distanceToSqr(Vec3.atCenterOf(terminal(id,t)))<=49&&p.level().getBlockState(terminal(id,t)).is(block(t));}
 static Block block(int t){return BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(switch(t){case 0->"emerald_block";case 1->"cyan_glazed_terracotta";case 2->"orange_glazed_terracotta";default->"lime_glazed_terracotta";}));}
 static String title(int t){return switch(t){case 0->"土地購入";case 1->"建材";case 2->"動物";default->"畑・水";};}
 public static void init(){
  PayloadTypeRegistry.clientboundPlay().register(State.TYPE,State.CODEC);
  ServerLifecycleEvents.SERVER_STARTED.register(s->{LandCatalog.init();LandConstruction.failed=false;});
  ServerPlayConnectionEvents.JOIN.register((h,sender,s)->sync(h.player));
  ServerPlayConnectionEvents.DISCONNECT.register((h,s)->SESSIONS.remove(h.player.getUUID()));
  ServerLifecycleEvents.SERVER_STOPPED.register(s->{SESSIONS.clear();LandOwnerSigns.CACHE.clear();});
  UseBlockCallback.EVENT.register((p,l,h,hit)->{var pos=hit.getBlockPos();if(!area(l,pos))return InteractionResult.PASS;int n=terminalAt(pos);if(n>=0&&l.getBlockState(pos).is(block(n%4))){if(p instanceof ServerPlayer sp&&h==InteractionHand.MAIN_HAND)open(sp,n/4,n%4,"種類・数量を選んで金額を確認してください");return InteractionResult.SUCCESS;}if(!edit(l,p,pos))return InteractionResult.FAIL;var stack=p.getItemInHand(h);if(HomeBuildingRules.altersWorld(stack)&&!edit(l,p,pos.relative(hit.getDirection())))return InteractionResult.FAIL;return InteractionResult.PASS;});
  UseItemCallback.EVENT.register((p,l,h)->{if(!area(l,p.blockPosition())||!HomeBuildingRules.altersWorld(p.getItemInHand(h)))return InteractionResult.PASS;var hit=p.pick(5,0,true);return hit instanceof BlockHitResult b&&edit(l,p,b.getBlockPos())&&edit(l,p,b.getBlockPos().relative(b.getDirection()))?InteractionResult.PASS:InteractionResult.FAIL;});
  UseEntityCallback.EVENT.register((p,l,h,e,hit)->area(l,e.blockPosition())&&!edit(l,p,e.blockPosition())?InteractionResult.FAIL:InteractionResult.PASS);
  AttackEntityCallback.EVENT.register((p,l,h,e,hit)->area(l,e.blockPosition())&&!edit(l,p,e.blockPosition())?InteractionResult.FAIL:InteractionResult.PASS);
  CommandRegistrationCallback.EVENT.register((d,c,e)->{
   var count=Commands.argument("count",IntegerArgumentType.integer(1,64)).executes(ctx->buy(ctx.getSource().getPlayerOrException(),IntegerArgumentType.getInteger(ctx,"token"),IntegerArgumentType.getInteger(ctx,"product"),IntegerArgumentType.getInteger(ctx,"count")));
   d.register(Commands.literal("neonland").then(Commands.literal("buy").then(Commands.argument("token",IntegerArgumentType.integer(0)).then(Commands.argument("product",IntegerArgumentType.integer(-1,2000)).then(count)))));
  });
  ServerTickEvents.END_SERVER_TICK.register(s->{LandConstruction.tick(s);LandOwnerSigns.tick(s);if(s.getTickCount()%100==0)for(var p:s.getPlayerList().getPlayers())if(area(p.level(),p.blockPosition()))for(var e:p.level().getEntities(p,p.getBoundingBox().inflate(48),e->e instanceof net.minecraft.world.entity.monster.Enemy))if(area(p.level(),e.blockPosition()))e.discard();});
 }
 static String purchase(MarketLedger ledger,String uuid,String name,int plot){
  if(plot<0||plot>=8)throw new IllegalArgumentException("土地を選んでください");
  if(ledger.westLand.containsKey(plot))throw new IllegalArgumentException("この土地は購入済みです");
  if(ledger.westLandColumns<216)throw new IllegalArgumentException("土地の整備中です。お待ちください");
  var a=ledger.account(uuid);if(a.cash<LandLayout.PRICE)throw new IllegalArgumentException("20,000 Cr必要です");
  ledger.westLand.put(plot,new MarketLedger.LandOwner(uuid,name));a.cash-=LandLayout.PRICE;
  return "土地"+(plot+1)+"を購入しました / 20,000 Cr";
 }
 static void phoneSnapshot(MarketLedger ledger,String uuid,JsonObject out){
  var plots=new JsonArray();int owned=0;
  for(int i=0;i<8;i++){var owner=ledger.westLand.get(i);boolean mine=owner!=null&&uuid.equals(owner.uuid);if(mine)owned++;
   var row=new JsonObject();row.addProperty("plot",i);row.addProperty("vacant",owner==null);row.addProperty("mine",mine);row.addProperty("owner",owner==null?"未購入":owner.name);plots.add(row);
  }out.add("land_plots",plots);out.addProperty("land",owned);
 }
 static JsonObject state(){var out=new JsonObject();var owners=new JsonObject();if(StockMarket.ledger!=null)for(var e:StockMarket.ledger.westLand.entrySet())owners.addProperty(""+e.getKey(),e.getValue().uuid);out.add("owners",owners);return out;}
 static void sync(ServerPlayer p){ServerPlayNetworking.send(p,new State(state().toString()));}
 static void open(ServerPlayer p,int id,int t,String message){if(!near(p,id,t))return;var session=new Session(id,t,RNG.nextInt(Integer.MAX_VALUE),p.level().getGameTime()+1200);SESSIONS.put(p.getUUID(),session);var out=state();out.addProperty("plot",id);out.addProperty("terminal",t);out.addProperty("token",session.token());out.addProperty("message",message);out.addProperty("cash",StockMarket.ledger==null?0:StockMarket.ledger.account(p.getStringUUID()).cash);var o=StockMarket.ledger==null?null:StockMarket.ledger.westLand.get(id);out.addProperty("owner",o==null?"未購入":o.name);out.addProperty("mine",owned(p,id));ServerPlayNetworking.send(p,new State(out.toString()));}
 static int animals(ServerPlayer p,int id){return p.level().getEntitiesOfClass(net.minecraft.world.entity.animal.Animal.class,new AABB(LandLayout.x(id),48,LandLayout.z(id),LandLayout.x(id)+32,128,LandLayout.z(id)+32)).size();}
 static int buy(ServerPlayer p,int token,int product,int count){var s=SESSIONS.get(p.getUUID());if(s==null||s.token()!=token)return 0;SESSIONS.remove(p.getUUID());if(!near(p,s.plot(),s.terminal())||p.level().getGameTime()>s.expires())return 0;String msg="購入できません";var ledger=StockMarket.ledger;if(ledger==null){open(p,s.plot(),s.terminal(),"台帳を読み込めないため休止中");return 0;}
  var a=ledger.account(p.getStringUUID());long cash=a.cash;var inv=Cyberware.inventory(p);Entity animal=null;boolean land=false;
  try{
   if(s.terminal()==0){if(product!=-1||count!=1)throw new IllegalArgumentException("土地を選んでください");msg=purchase(ledger,p.getStringUUID(),p.getName().getString(),s.plot());land=true;}
   else{if(!owned(p,s.plot()))throw new IllegalArgumentException("この区画の所有者だけ購入できます");if(product<0||product>=LandCatalog.PRODUCTS.size())throw new IllegalArgumentException("商品が不正です");var item=LandCatalog.PRODUCTS.get(product);if(!LandCatalog.page(item,s.terminal()))throw new IllegalArgumentException("別の端末の商品です");long total=(long)item.price()*count;if(cash<total)throw new IllegalArgumentException("残高が足りません");
    if(s.terminal()==2){if(count!=1||animals(p,s.plot())>=LandLayout.ANIMAL_LIMIT)throw new IllegalArgumentException("動物は1頭ずつ、区画内12頭までです");var type=switch(item.id()){case "cow"->EntityTypes.COW;case "pig"->EntityTypes.PIG;case "chicken"->EntityTypes.CHICKEN;default->throw new IllegalArgumentException();};animal=type.create(p.level(),EntitySpawnReason.COMMAND);boolean placed=false;for(int dx=2;dx<30&&!placed;dx++)for(int dz=2;dz<30&&!placed;dz++){var pos=new BlockPos(LandLayout.x(s.plot())+dx,65,LandLayout.z(s.plot())+dz);if(!p.level().getBlockState(pos.below()).isSolidRender())continue;animal.setPos(pos.getX()+.5,65,pos.getZ()+.5);if(p.level().noCollision(animal)){placed=true;}}if(!placed)throw new IllegalArgumentException("区画の地上に動物用の空間を空けてください");LandAnimals.mark(animal,s.plot());if(!p.level().addFreshEntity(animal))throw new IllegalStateException();}
    else {var stack=new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(item.id())),count);if(!p.getInventory().add(stack)||!stack.isEmpty())throw new IllegalArgumentException("持ち物に十分な空きがありません（課金なし）");}a.cash-=total;msg="購入しました / −"+total+" Cr";
   }StockMarket.save();
  }catch(Exception ex){a.cash=cash;Cyberware.restore(p,inv);if(animal!=null)animal.discard();if(land)ledger.westLand.remove(s.plot());land=false;msg=ex instanceof IllegalArgumentException&&ex.getMessage()!=null?ex.getMessage():"保存できなかったため購入を取り消しました";}
  p.getInventory().setChanged();p.containerMenu.broadcastChanges();
  if(land){for(var other:p.level().getServer().getPlayerList().getPlayers())sync(other);if(p.level().getBlockEntity(terminal(s.plot(),0).above()) instanceof net.minecraft.world.level.block.entity.SignBlockEntity sign){var text=sign.getFrontText().setMessage(2,Component.literal("所有者: "+p.getName().getString())).setMessage(3,Component.literal("購入済み / UUID管理"));sign.setText(text,true);sign.setText(text,false);sign.setChanged();var at=terminal(s.plot(),0).above();p.level().sendBlockUpdated(at,p.level().getBlockState(at),p.level().getBlockState(at),3);}}
  open(p,s.plot(),s.terminal(),msg);return 1;
 }
}
