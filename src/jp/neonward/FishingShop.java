package jp.neonward;
import java.util.*;
import com.google.gson.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.commands.Commands;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
public final class FishingShop {
 static final int ROD_PRICE=250;
 record Session(UUID npc,boolean rods){}
 record Quote(int slot,ItemStack stack,int price){}
 static final Map<UUID,Session> sessions=new HashMap<>();
 static final Map<UUID,Quote> quotes=new HashMap<>();
 static boolean use(ServerPlayer p,CityResident npc){boolean rods=npc.job.equals("fishing_rods");if(!rods&&!npc.job.equals("fishing_buyer"))return false;if(p.isSpectator()||p.level().dimension()!=Level.OVERWORLD||p.distanceToSqr(npc)>36)return true;sessions.put(p.getUUID(),new Session(npc.getUUID(),rods));quotes.remove(p.getUUID());reply(p,"売りたい持ち物を選んでください",true);return true;}
 static Session session(ServerPlayer p){var s=sessions.get(p.getUUID());if(s==null||p.isSpectator()||p.level().dimension()!=Level.OVERWORLD)return null;var e=p.level().getEntity(s.npc());return e instanceof CityResident n&&n.job.equals(s.rods()?"fishing_rods":"fishing_buyer")&&p.distanceToSqr(n)<=36?s:null;}
 static int price(ItemStack stack){if(stack.isEmpty())return 0;int k=OrnamentalFish.kind(stack);if(Aquariums.fish(stack)){int cm=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("neon_fish_cm",0);return (k>=0?OrnamentalFish.PRICE[k]:stack.is(Items.PUFFERFISH)?40:stack.is(Items.TROPICAL_FISH)?35:20)+Math.min(60,Math.max(0,cm))*2;}return stack.is(Items.BOWL)||stack.is(Items.LEATHER)||stack.is(Items.LEATHER_BOOTS)||stack.is(Items.ROTTEN_FLESH)||stack.is(Items.STICK)||stack.is(Items.STRING)||stack.is(Items.INK_SAC)||stack.is(Items.TRIPWIRE_HOOK)||stack.is(Items.LILY_PAD)||stack.is(Items.BONE)||stack.is(Items.POTION)?5:stack.is(Items.BOW)||stack.is(Items.FISHING_ROD)?25:stack.is(Items.NAME_TAG)||stack.is(Items.SADDLE)||stack.is(Items.NAUTILUS_SHELL)||stack.is(Items.ENCHANTED_BOOK)?120:0;}
 static int request(ServerPlayer p,String action,int slot){var s=session(p);if(s==null||StockMarket.ledger==null)return 0;String message="1個ずつ買取 / 観賞魚は水槽にも飾れます";var account=StockMarket.ledger.account(p.getStringUUID());
  if(action.equals("buy")&&s.rods()){if(account.cash<ROD_PRICE)message="残高が足りません";else if(p.getInventory().getFreeSlot()<0)message="持ち物に空きが必要です";else{long before=account.cash;account.cash-=ROD_PRICE;try{StockMarket.save();p.getInventory().add(new ItemStack(Items.FISHING_ROD));message="釣り竿を購入しました / 250 Cr";}catch(Exception ex){account.cash=before;message="保存できなかったため購入を中止しました";}}}
  if(action.equals("select")&&!s.rods()&&slot>=0&&slot<36){var stack=p.getInventory().getItem(slot);int value=price(stack);quotes.remove(p.getUUID());if(value>0){quotes.put(p.getUUID(),new Quote(slot,stack.copy(),value));message=stack.getHoverName().getString()+" を1個売却しますか？";}}
  if(action.equals("confirm")&&!s.rods()){var q=quotes.remove(p.getUUID());if(q==null)message="売るものを選び直してください";else{var stack=p.getInventory().getItem(q.slot());if(stack.isEmpty()||!ItemStack.isSameItemSameComponents(stack,q.stack())||price(stack)!=q.price())message="持ち物が変わりました。選び直してください";else{var before=stack.copy();long cash=account.cash;stack.shrink(1);account.cash+=q.price();try{StockMarket.save();message="1個売却しました / +"+q.price()+" Cr";}catch(Exception ex){account.cash=cash;p.getInventory().setItem(q.slot(),before);message="保存できなかったため売却を取り消しました";}}}}
  if(action.equals("cancel"))quotes.remove(p.getUUID());p.getInventory().setChanged();p.containerMenu.broadcastChanges();reply(p,message,false);return 1;
 }
 static void reply(ServerPlayer p,String message,boolean open){var s=session(p);if(s==null)return;var o=new JsonObject();o.addProperty("fishing_shop",true);o.addProperty("open",open);o.addProperty("rods",s.rods());o.addProperty("cash",StockMarket.ledger==null?0:StockMarket.ledger.account(p.getStringUUID()).cash);o.addProperty("message",message);var rows=new JsonArray();for(int i=0;i<36;i++){var stack=p.getInventory().getItem(i);int price=price(stack);if(price==0)continue;var row=new JsonObject();row.addProperty("slot",i);row.addProperty("name",stack.getHoverName().getString());row.addProperty("count",stack.getCount());row.addProperty("price",price);rows.add(row);}o.add("rows",rows);var q=quotes.get(p.getUUID());o.addProperty("quote",q==null?0:q.price());ServerPlayNetworking.send(p,new StockMarket.Snapshot(o.toString()));}
 static void init(){CommandRegistrationCallback.EVENT.register((d,c,e)->{var root=Commands.literal("neonfish");for(String action:List.of("view","buy","confirm","cancel"))root.then(Commands.literal(action).executes(ctx->request(ctx.getSource().getPlayerOrException(),action,-1)));root.then(Commands.literal("select").then(Commands.argument("slot",IntegerArgumentType.integer(0,35)).executes(ctx->request(ctx.getSource().getPlayerOrException(),"select",IntegerArgumentType.getInteger(ctx,"slot")))));d.register(root);});ServerPlayConnectionEvents.DISCONNECT.register((h,s)->{sessions.remove(h.player.getUUID());quotes.remove(h.player.getUUID());});ServerLifecycleEvents.SERVER_STOPPED.register(s->{sessions.clear();quotes.clear();});}
}
