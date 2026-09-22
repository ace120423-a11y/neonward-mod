package jp.neonward;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** A physical sales office sharing the same ownership ledger as the phone. */
public final class RealEstate {
 static final BlockPos COUNTER=new BlockPos(378,65,646);
 static final String AGENT="不動産屋・レイ";
 static boolean available(ServerPlayer p){return !p.isSpectator()&&StockMarket.ledger!=null&&p.level().dimension()==Level.OVERWORLD&&p.distanceToSqr(378.5,65,646.5)<=100&&p.level().getBlockState(COUNTER).is(NeonFurniture.BLOCKS.get("neon_counter"));}
 static int open(ServerPlayer p){if(!available(p))return 0;var req=new JsonObject();req.addProperty("app","8");req.addProperty("action","view");var o=PhoneServices.snapshot(StockMarket.ledger,p.getStringUUID(),req);o.addProperty("estate_ui",true);o.addProperty("action","view");o.addProperty("ok",true);o.addProperty("message","レイ：街の住宅は1フロア専有。個別住宅・西側の土地も購入できます");ServerPlayNetworking.send(p,new StockMarket.Snapshot(o.toString()));return 1;}
 static void init(){
  UseBlockCallback.EVENT.register((p,l,h,hit)->{if(h==InteractionHand.MAIN_HAND&&l.dimension()==Level.OVERWORLD&&hit.getBlockPos().equals(COUNTER)&&l.getBlockState(COUNTER).is(NeonFurniture.BLOCKS.get("neon_counter"))){if(p instanceof ServerPlayer sp)open(sp);return InteractionResult.SUCCESS;}return InteractionResult.PASS;});
  ServerTickEvents.END_SERVER_TICK.register(s->{if(s.getTickCount()%100!=0)return;var l=s.overworld();if(l.isPositionEntityTicking(COUNTER)&&l.getBlockState(COUNTER).is(NeonFurniture.BLOCKS.get("neon_counter")))JapaneseParlor.npc(l,"estate_agent",378.5,65,648.5,AGENT,true,2);});
 }
}
