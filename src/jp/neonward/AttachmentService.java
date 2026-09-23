package jp.neonward;
import java.util.*;
import com.google.gson.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.commands.Commands;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Dedicated sessions cannot be replayed across purchase, gacha, or equipment screens. */
public final class AttachmentService {
 record Session(String mode,int token,long expires,InteractionHand hand,ItemStack expected){}
 static final Map<UUID,Session> SESSIONS=new HashMap<>();
 static final java.security.SecureRandom RNG=new java.security.SecureRandom();
 public record Reply(String json) implements CustomPacketPayload {
  public static final Type<Reply> TYPE=new Type<>(NeonWard.id("attachments_v1"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Reply> CODEC=StreamCodec.composite(ByteBufCodecs.STRING_UTF8,Reply::json,Reply::new);
  public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 static boolean shop(ServerPlayer p){return CompactShops.room(p.level(),p.blockPosition())==0&&p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(CompactShops.counter(0)))<=36;}
 static boolean shopSells(int code){return GunAttachments.valid(code)&&code%5==0;}
 static JsonArray shopRows(){var rows=new JsonArray();for(int code=0;code<40;code++)if(shopSells(code)){var row=row(code,-1);row.addProperty("price",GunAttachments.price(code));rows.add(row);}return rows;}
 static boolean allowed(ServerPlayer p,String mode){return p.isAlive()&&!p.isSpectator()&&switch(mode){case "shop"->shop(p);case "gacha"->AttachmentGacha.near(p);case "equip"->NeonArsenal.isGun(p.getItemInHand(NeonArsenal.gunHand(p)));default->false;};}
 static Session session(ServerPlayer p,String mode){var old=SESSIONS.get(p.getUUID());int token;do{token=RNG.nextInt(Integer.MAX_VALUE);}while(old!=null&&token==old.token());var hand=NeonArsenal.gunHand(p);return new Session(mode,token,p.level().getGameTime()+1200,hand,p.getItemInHand(hand).copy());}
 static int open(ServerPlayer p,String mode){if(!allowed(p,mode))return 0;p.stopUsingItem();SESSIONS.put(p.getUUID(),session(p,mode));reply(p,"",new JsonArray());return 1;}
 static int request(ServerPlayer p,String action,int token,int value){
  var s=SESSIONS.get(p.getUUID());if(s==null||s.token()!=token||p.level().getGameTime()>s.expires()||!allowed(p,s.mode()))return 0;
  boolean equip=action.equals("install")||action.equals("remove");String needed=equip?"equip":action.equals("buy")?"shop":action.equals("roll")?"gacha":"";
  if(!s.mode().equals(needed))return 0;
  var results=new JsonArray();String message;int done=0;
  if(equip){var gun=p.getItemInHand(s.hand());
   if(!ItemStack.matches(gun,s.expected()))message="銃の状態が変わりました。Bキーで開き直してください";
   else if(GunReload.reloading(gun))message="リロード完了後に付け替えてください";
   else {
    int code=action.equals("install")&&value>=0&&value<36?GunAttachments.code(p.getInventory().getItem(value)):-1;
    int mount=action.equals("install")&&code>=0?GunAttachments.slot(code/5):value;
    if(mount<0||mount>=4||action.equals("install")&&!GunAttachments.compatible(gun,code))message="この銃には取り付けられません";
    else {int previous=GunAttachments.installed(gun,mount);var before=Cyberware.inventory(p);var gunCopy=gun.copy();
     try{
      if(action.equals("install")){p.getInventory().getItem(value).shrink(1);}
      else if(previous<0)throw new IllegalArgumentException("empty");
      if(previous>=0&&!p.getInventory().add(GunAttachments.stack(previous)))throw new IllegalStateException("full");
      int remaining=GunReload.remaining(gun);
      GunAttachments.install(gun,mount,action.equals("install")?code:-1);
      int cap=GunReload.profile(gun).capacity();
      net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,gun,t->t.putInt(GunReload.USED,Math.max(0,cap-Math.min(cap,remaining))));
      done=1;message=action.equals("install")?"取り付けました / 元のパーツは持ち物へ返却":"取り外して持ち物へ返却しました";
     }catch(Exception ex){Cyberware.restore(p,before);p.setItemInHand(s.hand(),gunCopy);message="持ち物の空きが足りないか、パーツがありません（変更なし）";}
    }
   }
  }else if(StockMarket.ledger==null)message="台帳を読み込めないため休止中です";
  else {
   int count=action.equals("roll")?value:1;
   if(action.equals("roll")&&count!=1&&count!=10||action.equals("buy")&&!shopSells(value))return 0;
   long price=action.equals("roll")?1000L*count:GunAttachments.price(value);
   var account=StockMarket.ledger.account(p.getStringUUID());int free=0;for(int i=0;i<36;i++)if(p.getInventory().getItem(i).isEmpty())free++;
   if(action.equals("roll")&&p.level().getGameTime()<AttachmentGacha.ends)message="抽選演出が終わるまでお待ちください";
   else if(account.cash<price)message="残高が足りません / "+price+" Cr必要";
   else if(free<count)message="持ち物に空きを"+count+"枠作ってください（課金なし）";
   else {var inventory=Cyberware.inventory(p);long cash=account.cash;try{
    int highest=0;for(int i=0;i<count;i++){var item=action.equals("roll")?GunAttachments.roll(RNG):GunAttachments.stack(value);int c=GunAttachments.code(item);highest=Math.max(highest,c%5);results.add(row(c,-1));if(!p.getInventory().add(item))throw new IllegalStateException();}
    account.cash-=price;StockMarket.save();done=count;message=count+"個を持ち物へ / −"+price+" Cr";
    if(action.equals("roll")){AttachmentGacha.ends=p.level().getGameTime()+60;AttachmentGacha.tier=highest;}
   }catch(Exception ex){account.cash=cash;Cyberware.restore(p,inventory);results=new JsonArray();message="保存に失敗したため全て取り消しました";}}
  }
  p.getInventory().setChanged();p.containerMenu.broadcastChanges();
  // Successful actions consume the nonce. Failed actions remain retryable without charging.
  if(done>0)SESSIONS.put(p.getUUID(),session(p,s.mode()));
  reply(p,message,results);return done;
 }
 static JsonObject row(int code,int slot){var o=new JsonObject();o.addProperty("code",code);o.addProperty("slot",slot);o.addProperty("name",GunAttachments.name(code));o.addProperty("effect",GunAttachments.effect(code));return o;}
 static void reply(ServerPlayer p,String message,JsonArray prizes){
  var s=SESSIONS.get(p.getUUID());if(s==null)return;
  var out=new JsonObject();out.addProperty("mode",s.mode());out.addProperty("token",s.token());out.addProperty("message",message);out.addProperty("cash",StockMarket.ledger==null?0:StockMarket.ledger.account(p.getStringUUID()).cash);out.add("prizes",prizes);out.addProperty("remaining",Math.max(0,AttachmentGacha.ends-p.level().getGameTime()));
  var gun=p.getItemInHand(s.hand());out.addProperty("gun",NeonArsenal.isGun(gun)?gun.getHoverName().getString():"");var installed=new JsonArray();for(int i=0;i<4;i++)installed.add(GunAttachments.installed(gun,i));out.add("installed",installed);
  var rows=new JsonArray();if(s.mode().equals("shop"))rows=shopRows();
  else if(s.mode().equals("equip"))for(int i=0;i<36;i++){int c=GunAttachments.code(p.getInventory().getItem(i));if(c>=0){var row=row(c,i);row.addProperty("compatible",GunAttachments.compatible(gun,c));rows.add(row);}}
  out.add("rows",rows);
  if(ServerPlayNetworking.canSend(p,Reply.TYPE))ServerPlayNetworking.send(p,new Reply(out.toString()));
 }
 static void init(){PayloadTypeRegistry.clientboundPlay().register(Reply.TYPE,Reply.CODEC);
  CommandRegistrationCallback.EVENT.register((d,c,e)->{var root=Commands.literal("neonattach");for(String mode:new String[]{"equip","shop","gacha"})root.then(Commands.literal(mode).executes(ctx->open(ctx.getSource().getPlayerOrException(),mode)));
   for(String action:new String[]{"install","remove","buy","roll"})root.then(Commands.literal(action).then(Commands.argument("token",IntegerArgumentType.integer(0)).then(Commands.argument("value",IntegerArgumentType.integer(0,39)).executes(ctx->request(ctx.getSource().getPlayerOrException(),action,IntegerArgumentType.getInteger(ctx,"token"),IntegerArgumentType.getInteger(ctx,"value"))))));
   d.register(root);
  });
  ServerPlayConnectionEvents.DISCONNECT.register((h,s)->SESSIONS.remove(h.player.getUUID()));
  net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(s->SESSIONS.clear());
 }
}
