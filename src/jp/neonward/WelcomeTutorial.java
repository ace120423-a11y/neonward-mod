package jp.neonward;
import java.util.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
public final class WelcomeTutorial {
 static final Map<UUID,Integer> pending=new HashMap<>();
 static boolean needed(ServerPlayer p){return StockMarket.ledger!=null&&!StockMarket.ledger.account(p.getStringUUID()).tutorialDone;}
 static void open(ServerPlayer p){ServerPlayNetworking.send(p,new StockMarket.Snapshot("{\"welcome_ui\":true}"));}
 static int finish(ServerPlayer p){if(StockMarket.ledger==null)return 0;var a=StockMarket.ledger.account(p.getStringUUID());boolean old=a.tutorialDone;a.tutorialDone=true;try{StockMarket.save();pending.remove(p.getUUID());p.sendSystemMessage(Component.literal("NEON WARDへようこそ！ 案内の見直しは /tutorial"));return 1;}catch(Exception e){a.tutorialDone=old;p.sendSystemMessage(Component.literal("案内の保存に失敗しました。次回もう一度表示します。"));return 0;}}
 static void init(){
  ServerPlayConnectionEvents.JOIN.register((h,s,server)->pending.put(h.player.getUUID(),server.getTickCount()+60));
  ServerPlayConnectionEvents.DISCONNECT.register((h,server)->pending.remove(h.player.getUUID()));
  ServerLifecycleEvents.SERVER_STOPPED.register(server->pending.clear());
  ServerTickEvents.END_SERVER_TICK.register(server->{var it=pending.entrySet().iterator();while(it.hasNext()){var entry=it.next();if(server.getTickCount()<entry.getValue())continue;var p=server.getPlayerList().getPlayer(entry.getKey());if(p==null){it.remove();continue;}if(StockMarket.ledger==null)continue;if(needed(p))open(p);it.remove();}});
  CommandRegistrationCallback.EVENT.register((d,c,e)->d.register(Commands.literal("tutorial").executes(ctx->{open(ctx.getSource().getPlayerOrException());return 1;}).then(Commands.literal("done").executes(ctx->finish(ctx.getSource().getPlayerOrException())))));
 }
}
