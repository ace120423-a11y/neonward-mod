package jp.neonward;
import com.google.gson.JsonObject;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.commands.Commands;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.*;

/** Retired farm compatibility shim: no generation, purchase or entry remains.
 * The dimension key remains only to rescue players saved inside the retired world. */
public final class PrivateFarms {
 public static final ResourceKey<Level> DIM=ResourceKey.create(Registries.DIMENSION,NeonWard.id("private_farms"));
 static final BlockPos COUNTER=new BlockPos(327,65,634);
 static boolean owns(Level l,Player p,BlockPos pos){return false;}
 static boolean edit(Level l,Player p,BlockPos pos){return false;}
 static boolean hoe(Level l,Player p,BlockPos pos,ItemStack item){return false;}
 static String buy(MarketLedger ledger,String uuid){throw new IllegalArgumentException("個別農地は廃止しました。更新後のスマホから西側の土地を選んでください（課金なし）");}
 static void init(){
  CommandRegistrationCallback.EVENT.register((d,c,e)->{var root=Commands.literal("neonfarm");for(String action:java.util.List.of("view","buy","enter","leave"))root.then(Commands.literal(action).executes(ctx->request(ctx.getSource().getPlayerOrException(),action)));d.register(root);});
  UseBlockCallback.EVENT.register((p,l,h,hit)->{
   if(l.dimension()==DIM){if(p instanceof ServerPlayer sp)leave(sp);return InteractionResult.FAIL;}
   if(h==InteractionHand.MAIN_HAND&&l.dimension()==Level.OVERWORLD&&hit.getBlockPos().equals(COUNTER)){if(p instanceof ServerPlayer sp)request(sp,"view");return InteractionResult.SUCCESS;}
   return InteractionResult.PASS;
  });
  ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->{if(handler.player.level().dimension()==DIM)leave(handler.player);});
  ServerTickEvents.END_SERVER_TICK.register(server->{
   if(server.getTickCount()%20!=0)return;
   for(var p:server.getPlayerList().getPlayers())if(p.level().dimension()==DIM)leave(p);
  });
 }
 static int request(ServerPlayer p,String action){
  if(p.level().dimension()==DIM){leave(p);return 1;}
  if(p.isSpectator()||StockMarket.ledger==null||p.level().dimension()!=Level.OVERWORLD||p.distanceToSqr(327.5,65,634.5)>64)return 0;
  var req=new JsonObject();req.addProperty("app","8");req.addProperty("action","view");
  var out=PhoneServices.snapshot(StockMarket.ledger,p.getStringUUID(),req);
  out.addProperty("estate_ui",true);out.addProperty("land_page",true);out.addProperty("action","view");out.addProperty("ok",true);
  out.addProperty("message","西門の先の土地に統一しました / 旧農地の購入・入場は廃止");
  ServerPlayNetworking.send(p,new StockMarket.Snapshot(out.toString()));return 1;
 }
 static void leave(ServerPlayer p){
  if(p.isPassenger())p.stopRiding();
  p.teleportTo(p.level().getServer().overworld(),327.5,65,636.5,Set.of(),0,0,true);
 }
}
