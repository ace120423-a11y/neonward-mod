package jp.neonward;
import java.util.*;
import com.google.gson.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.server.level.ServerPlayer;
public final class ObjectiveTracker {
 static final Map<UUID,String> sent=new HashMap<>();
 static String snapshot(ServerPlayer p){var a=StockMarket.ledger.account(p.getStringUUID());var out=new JsonObject();out.addProperty("objectives_ui",true);var rows=new JsonArray();
  for(var id:new TreeSet<>(a.guildQuests.keySet())){var q=GuildRanks.accepted(a,id);int progress=Math.min(q.target(),a.guildQuests.get(id));var row=new JsonObject();row.addProperty("title",q.title());row.addProperty("progress",progress+" / "+q.target()+(progress>=q.target()?" 報告可":""));rows.add(row);}
  if(a.guildExamActive){var row=new JsonObject();row.addProperty("title","昇級試験");row.addProperty("progress",GuildRanks.description(a));rows.add(row);}
  if(a.street.mission>=0){var row=new JsonObject();row.addProperty("title","裏稼業 / BLACKLINE");row.addProperty("progress",Underworld.objective(a.street));rows.add(row);}
  out.add("rows",rows);return out.toString();
 }
 static void init(){ServerPlayConnectionEvents.DISCONNECT.register((h,s)->sent.remove(h.player.getUUID()));ServerLifecycleEvents.SERVER_STOPPED.register(s->sent.clear());ServerTickEvents.END_SERVER_TICK.register(s->{if(s.getTickCount()%20!=0||StockMarket.ledger==null)return;for(var p:s.getPlayerList().getPlayers()){String json=snapshot(p);if(!json.equals(sent.get(p.getUUID()))){ServerPlayNetworking.send(p,new StockMarket.Snapshot(json));sent.put(p.getUUID(),json);}}});}
}
