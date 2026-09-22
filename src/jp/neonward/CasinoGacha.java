package jp.neonward;
import java.util.*;
import com.google.gson.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import com.mojang.brigadier.arguments.IntegerArgumentType;

/** Shared odds/transaction for two independently addressed machines. */
public final class CasinoGacha {
 record Session(boolean weapon,int token){}
 static final Map<UUID,Session> SESSIONS=new HashMap<>();
 static final java.security.SecureRandom RNG=new java.security.SecureRandom();
 static List<Item> weapons(){return NeonArsenal.DROPS.stream().filter(i->i!=NeonShield.ITEM&&(i instanceof MeleeGuard.Weapon||i instanceof ArsenalExpansion.Blade||i instanceof NeonArsenal.Rifle)).toList();}
 static ItemStack weapon(java.util.random.RandomGenerator rng){var pool=weapons();int tier=CyberwareGacha.rarity(rng.nextInt(1000));return RolledWeapons.create(pool.get(rng.nextInt(pool.size())),new WeaponLoot.Quality(tier,rng.nextInt(WeaponLoot.MIN[tier],WeaponLoot.MAX[tier]+1)));}
 static void init(){
  CommandRegistrationCallback.EVENT.register((d,c,e)->{for(boolean weapon:new boolean[]{false,true}){
   var root=Commands.literal(weapon?"neonweapongacha":"neongacha");
   root.then(Commands.literal("view").executes(ctx->open(ctx.getSource().getPlayerOrException(),weapon)));
   root.then(Commands.literal("roll").then(Commands.argument("token",IntegerArgumentType.integer(0)).executes(ctx->request(ctx.getSource().getPlayerOrException(),weapon,true,IntegerArgumentType.getInteger(ctx,"token"),1)).then(Commands.argument("count",IntegerArgumentType.integer(1,10)).executes(ctx->request(ctx.getSource().getPlayerOrException(),weapon,true,IntegerArgumentType.getInteger(ctx,"token"),IntegerArgumentType.getInteger(ctx,"count"))))));d.register(root);
  }});
  ServerPlayConnectionEvents.DISCONNECT.register((h,s)->SESSIONS.remove(h.player.getUUID()));ServerLifecycleEvents.SERVER_STOPPED.register(s->SESSIONS.clear());
 }
 static Session newSession(boolean weapon){return new Session(weapon,RNG.nextInt(Integer.MAX_VALUE));}
 static int open(ServerPlayer p,boolean weapon){
  if(!ServerPlayNetworking.canSend(p,CyberwareGacha.Snapshot.TYPE)){p.sendOverlayMessage(net.minecraft.network.chat.Component.literal("ガチャの利用には参加者用パックの更新が必要です"));return 0;}
  return request(p,weapon,false,0,1);
 }
 static int request(ServerPlayer p,boolean weapon,boolean buy,int token,int count){
  if(!(weapon?WeaponGacha.near(p):CyberwareGacha.near(p))||(count!=1&&count!=10))return 0;
  var old=SESSIONS.get(p.getUUID());if(buy&&(old==null||old.weapon()!=weapon||old.token()!=token))return 0;
  if(!buy)SESSIONS.put(p.getUUID(),newSession(weapon));
  var prizes=new JsonArray();String message=weapon?"近接・銃24種 / 種類は均等 / 重複あり":"部位・系統は均等 / 重複あり";
  long now=p.level().getGameTime(),end=weapon?WeaponGacha.ends:CyberwareGacha.ends;int highest=0;long price=(long)CyberwareGacha.PRICE*count;
  if(StockMarket.ledger==null)message="台帳を読み込めないため休止中です";
  else if(buy){var account=StockMarket.ledger.account(p.getStringUUID());int free=0;for(int i=0;i<36;i++)if(p.getInventory().getItem(i).isEmpty())free++;
   if(now<end)message="抽選演出が終わるまでお待ちください";
   else if(account.cash<price)message=String.format("%,d Cr必要です。残高が足りません",price);
   else if(free<count)message="持ち物に空きを"+count+"枠作ってください（課金なし）";
   else {var inventory=Cyberware.inventory(p);long cash=account.cash;
    try{
     for(int i=0;i<count;i++){
      ItemStack item;int tier;String detail;
      if(weapon){item=weapon(RNG);var tag=item.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag();tier=tag.getIntOr("neon_weapon_tier",0);detail="攻撃性能 "+tag.getIntOr("neon_weapon_power",100)+"%";}
      else{int id=CyberwareGacha.roll(RNG),value=CyberwareCatalog.rollValue(id,RNG);item=Cyberware.stack(id,value);tier=CyberwareCatalog.PARTS[id].tier();detail=CyberwareCatalog.effect(id,value);}
      var result=new JsonObject();result.addProperty("name",item.getHoverName().getString());result.addProperty("tier",tier);result.addProperty("detail",detail);prizes.add(result);highest=Math.max(highest,tier);
      if(!p.getInventory().add(item))throw new IllegalStateException("inventory full");
     }
     account.cash-=price;StockMarket.save();end=now+60;
     if(weapon){WeaponGacha.ends=end;WeaponGacha.tier=highest;WeaponGacha.result=count+"個 GET!";}else{CyberwareGacha.ends=end;CyberwareGacha.tier=highest;CyberwareGacha.result=count+"個 GET!";}
     var next=newSession(weapon);while(next.token()==token)next=newSession(weapon);SESSIONS.put(p.getUUID(),next);
     message=String.format("%d個を持ち物へ受け取りました / −%,d Cr",count,price);
    }catch(Exception ex){account.cash=cash;Cyberware.restore(p,inventory);prizes=new JsonArray();message="保存できなかったため全ての抽選・支払いを取り消しました";}
    p.getInventory().setChanged();p.containerMenu.broadcastChanges();
   }
  }
  var out=new JsonObject();out.addProperty("weapon",weapon);out.addProperty("token",SESSIONS.get(p.getUUID()).token());out.addProperty("cash",StockMarket.ledger==null?0:StockMarket.ledger.account(p.getStringUUID()).cash);out.addProperty("message",message);out.add("prizes",prizes);out.addProperty("remaining",Math.max(0,end-now));
  if(ServerPlayNetworking.canSend(p,CyberwareGacha.Snapshot.TYPE))ServerPlayNetworking.send(p,new CyberwareGacha.Snapshot(out.toString()));return prizes.size();
 }
}
