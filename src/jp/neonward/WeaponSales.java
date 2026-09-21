package jp.neonward;
import java.util.*;
import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
public final class WeaponSales {
 record Quote(BlockPos pos,int slot,ItemStack stack,long price,int token,long expires){}
 static final Map<UUID,Quote> QUOTES=new HashMap<>();
 static final java.security.SecureRandom RANDOM=new java.security.SecureRandom();
 public record Reply(String json) implements CustomPacketPayload {
  public static final Type<Reply> TYPE=new Type<>(NeonWard.id("weapon_sale"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Reply> CODEC=StreamCodec.composite(ByteBufCodecs.STRING_UTF8,Reply::json,Reply::new);
  public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 static long price(ItemStack stack){int index=NeonArsenal.DROPS.indexOf(stack.getItem());return index<0?0:Math.max(1,(long)Math.floor(CompactShops.price(index)*.25*Math.min(2,RolledWeapons.multiplier(stack))));}
 static void init(){PayloadTypeRegistry.clientboundPlay().register(Reply.TYPE,Reply.CODEC);
  CommandRegistrationCallback.EVENT.register((d,c,e)->{
   var quote=Commands.literal("quote").then(Commands.argument("x",IntegerArgumentType.integer()).then(Commands.argument("y",IntegerArgumentType.integer()).then(Commands.argument("z",IntegerArgumentType.integer()).then(Commands.argument("slot",IntegerArgumentType.integer(0,35)).executes(ctx->quote(ctx.getSource().getPlayerOrException(),new BlockPos(IntegerArgumentType.getInteger(ctx,"x"),IntegerArgumentType.getInteger(ctx,"y"),IntegerArgumentType.getInteger(ctx,"z")),IntegerArgumentType.getInteger(ctx,"slot")))))));
   d.register(Commands.literal("neonweaponsale").then(quote).then(Commands.literal("confirm").then(Commands.argument("token",IntegerArgumentType.integer(0)).executes(ctx->confirm(ctx.getSource().getPlayerOrException(),IntegerArgumentType.getInteger(ctx,"token"))))));
  });
  ServerPlayConnectionEvents.DISCONNECT.register((h,s)->QUOTES.remove(h.player.getUUID()));ServerLifecycleEvents.SERVER_STOPPED.register(s->QUOTES.clear());
 }
 static int quote(ServerPlayer p,BlockPos pos,int slot){QUOTES.remove(p.getUUID());if(slot<0||slot>=36||!p.isAlive()||!Cyberware.canSell(p,pos))return reply(p,"換金所の近くで操作してください",null);
  var stack=p.getInventory().getItem(slot);long price=price(stack);if(stack.isEmpty()||price==0)return reply(p,"売却できるNeonWard武器を選んでください",null);
  if(stack==p.getMainHandItem())return reply(p,"手に持っている武器は別の枠へ移してから選んでください",null);
  var q=new Quote(pos,slot,stack.copy(),price,RANDOM.nextInt(Integer.MAX_VALUE),p.level().getGameTime()+200);QUOTES.put(p.getUUID(),q);return reply(p,"この個体を1本売却します（確認は10秒間有効）",q);
 }
 static int confirm(ServerPlayer p,int token){var q=QUOTES.get(p.getUUID());if(q==null||q.token()!=token)return 0;QUOTES.remove(p.getUUID());
  if(!p.isAlive()||!Cyberware.canSell(p,q.pos())||p.level().getGameTime()>q.expires())return reply(p,"確認期限切れ、または換金所から離れました。選び直してください",null);
  var stack=p.getInventory().getItem(q.slot());if(stack==p.getMainHandItem()||!ItemStack.matches(stack,q.stack()))return reply(p,"持ち物が変わったため中止しました。選び直してください",null);
  if(StockMarket.ledger==null)return reply(p,"台帳を読み込めないため売却できません",null);
  var a=StockMarket.ledger.account(p.getStringUUID());long cash=a.cash;var inventory=Cyberware.inventory(p);
  try{a.cash=Math.addExact(cash,q.price());stack.shrink(1);StockMarket.save();}
  catch(Exception ex){a.cash=cash;Cyberware.restore(p,inventory);return reply(p,"保存に失敗したため売却を取り消しました",null);}
  p.getInventory().setChanged();p.containerMenu.broadcastChanges();return reply(p,"1本売却しました / +"+q.price()+" Cr",null);
 }
 static int reply(ServerPlayer p,String message,Quote q){var o=new JsonObject();o.addProperty("message",message);o.addProperty("token",q==null?-1:q.token());o.addProperty("price",q==null?0:q.price());o.addProperty("name",q==null?"":q.stack().getHoverName().getString());ServerPlayNetworking.send(p,new Reply(o.toString()));return 1;}
}
