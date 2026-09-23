package jp.neonward;

import java.util.*;
import com.google.gson.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.commands.Commands;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Separate market bag purchases; never reuses stock-trading or equipment sessions. */
public final class BackpackShop {
 public static final String[] NAMES={"小型バッグ","中型バッグ","大型バッグ"};
 public static final int[] PRICES={3000,8000,20000};
 record Session(int token,long expires){}
 static final Map<UUID,Session> SESSIONS=new HashMap<>();
 static final java.security.SecureRandom RNG=new java.security.SecureRandom();
 public record Reply(String json) implements CustomPacketPayload {
  public static final Type<Reply> TYPE=new Type<>(NeonWard.id("backpack_shop_v1"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Reply> CODEC=StreamCodec.composite(ByteBufCodecs.STRING_UTF8,Reply::json,Reply::new);
  public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 static boolean near(ServerPlayer p){return p.isAlive()&&!p.isSpectator()&&CompactShops.room(p.level(),p.blockPosition())==4&&p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(CompactShops.counter(4)))<=36;}
 static void quote(ServerPlayer p){var old=SESSIONS.get(p.getUUID());int token;do{token=RNG.nextInt(Integer.MAX_VALUE);}while(old!=null&&old.token()==token);SESSIONS.put(p.getUUID(),new Session(token,p.level().getGameTime()+1200));}
 static int open(ServerPlayer p){if(!near(p))return 0;quote(p);reply(p,"専用枠へ装備すると追加の持ち物枠を使えます");return 1;}
 static int request(ServerPlayer p,int token,int tier){
  var session=SESSIONS.get(p.getUUID());
  if(session==null||session.token()!=token||p.level().getGameTime()>session.expires()||!near(p)||tier<0||tier>=3)return 0;
  int done=0;String message;
  if(StockMarket.ledger==null)message="台帳を読み込めないため販売を停止中です";
  else {
   var account=StockMarket.ledger.account(p.getStringUUID());int free=p.getInventory().getFreeSlot();
   if(free<0)message="持ち物に空きを1枠作ってください（課金なし）";
   else if(account.cash<PRICES[tier])message="残高が足りません（課金なし）";
   else {
    long cash=account.cash;var inventory=Cyberware.inventory(p);
    try{p.getInventory().setItem(free,new ItemStack(BackpackEquipment.ITEMS[tier]));account.cash-=PRICES[tier];StockMarket.save();done=1;message=NAMES[tier]+"を購入しました / インベントリの「バッグ」から装備";}
    catch(Exception e){account.cash=cash;Cyberware.restore(p,inventory);message="保存に失敗したため購入を取り消しました";}
   }
  }
  // Every processed attempt invalidates its quote; repeated clicks cannot double-charge.
  quote(p);p.getInventory().setChanged();p.containerMenu.broadcastChanges();reply(p,message);return done;
 }
 static void reply(ServerPlayer p,String message){
  var s=SESSIONS.get(p.getUUID());if(s==null)return;var out=new JsonObject();
  out.addProperty("token",s.token());out.addProperty("message",message);out.addProperty("cash",StockMarket.ledger==null?0:StockMarket.ledger.account(p.getStringUUID()).cash);
  var rows=new JsonArray();for(int i=0;i<3;i++){var row=new JsonObject();row.addProperty("tier",i);row.addProperty("name",NAMES[i]);row.addProperty("capacity",9*(i+1));row.addProperty("price",PRICES[i]);rows.add(row);}out.add("rows",rows);
  if(ServerPlayNetworking.canSend(p,Reply.TYPE))ServerPlayNetworking.send(p,new Reply(out.toString()));
 }
 static void init(){
  PayloadTypeRegistry.clientboundPlay().register(Reply.TYPE,Reply.CODEC);
  CommandRegistrationCallback.EVENT.register((d,c,e)->d.register(Commands.literal("neonbagshop")
   .then(Commands.literal("open").executes(ctx->open(ctx.getSource().getPlayerOrException())))
   .then(Commands.literal("buy").then(Commands.argument("token",IntegerArgumentType.integer(0)).then(Commands.argument("tier",IntegerArgumentType.integer(0,2)).executes(ctx->request(ctx.getSource().getPlayerOrException(),IntegerArgumentType.getInteger(ctx,"token"),IntegerArgumentType.getInteger(ctx,"tier"))))))));
  ServerPlayConnectionEvents.DISCONNECT.register((h,s)->SESSIONS.remove(h.player.getUUID()));
  ServerLifecycleEvents.SERVER_STOPPED.register(s->SESSIONS.clear());
 }
}
