package jp.neonward;
import java.nio.file.*;
import com.google.gson.*;
import com.mojang.brigadier.arguments.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;

public final class StockMarket {
 static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();static MarketLedger ledger;static Path file;static int ticks;static String fault="";
 public record Snapshot(String json) implements CustomPacketPayload {
  public static final Type<Snapshot> TYPE=new Type<>(NeonWard.id("market"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Snapshot> CODEC=StreamCodec.composite(ByteBufCodecs.STRING_UTF8,Snapshot::json,Snapshot::new);
  public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 public static boolean isPC(net.minecraft.world.level.block.state.BlockState s){return s.is(NeonFurniture.BLOCKS.get("terminal_desk"))||s.is(NeonFurniture.BLOCKS.get("hacker_desk"))||s.is(NeonFurniture.BLOCKS.get("work_desk"));}
 public static void init(){
  PayloadTypeRegistry.clientboundPlay().register(Snapshot.TYPE,Snapshot.CODEC);
  ServerLifecycleEvents.SERVER_STARTED.register(server->{file=server.getWorldPath(LevelResource.ROOT).resolve("neonward/market.json");ticks=0;fault="";try{ledger=Files.exists(file)?JSON.fromJson(Files.readString(file),MarketLedger.class):new MarketLedger();ledger.validate();}catch(Exception e){ledger=null;fault="市場データを読み込めません。取引を停止しています";System.err.println("[Neon market] "+e);}});
  ServerLifecycleEvents.SERVER_STOPPED.register(server->{ledger=null;file=null;});
  ServerTickEvents.END_SERVER_TICK.register(server->{if(ledger!=null&&++ticks>=600){ticks=0;String before=JSON.toJson(ledger);ledger.advance();try{save();}catch(Exception e){ledger=JSON.fromJson(before,MarketLedger.class);fault="市場の保存に失敗しました";}}});
  CommandRegistrationCallback.EVENT.register((d,c,e)->{
   var root=Commands.literal("neonmarket");
   for(String action:new String[]{"view","buy","sell"})root.then(Commands.literal(action).then(Commands.argument("x",IntegerArgumentType.integer()).then(Commands.argument("y",IntegerArgumentType.integer()).then(Commands.argument("z",IntegerArgumentType.integer()).then(Commands.argument("stock",IntegerArgumentType.integer(0,5)).then(Commands.argument("count",IntegerArgumentType.integer(1,1000)).then(Commands.argument("quote",IntegerArgumentType.integer(0,10000)).executes(ctx->request(ctx.getSource().getPlayerOrException(),new BlockPos(IntegerArgumentType.getInteger(ctx,"x"),IntegerArgumentType.getInteger(ctx,"y"),IntegerArgumentType.getInteger(ctx,"z")),action,IntegerArgumentType.getInteger(ctx,"stock"),IntegerArgumentType.getInteger(ctx,"count"),IntegerArgumentType.getInteger(ctx,"quote"))))))))));
   d.register(root);
  });
 }
 static void save() throws Exception {Files.createDirectories(file.getParent());Path tmp=file.resolveSibling("market.json.tmp");Files.writeString(tmp,JSON.toJson(ledger));Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);fault="";}
 static int request(ServerPlayer p,BlockPos pos,String action,int stock,int count,int quote){
  if(p.isSpectator()||p.distanceToSqr(Vec3.atCenterOf(pos))>49||!isPC(p.level().getBlockState(pos)))return 0;
  JsonObject out=new JsonObject();String msg=fault;
  if(ledger!=null){String before=JSON.toJson(ledger);boolean fresh=!ledger.accounts.containsKey(p.getStringUUID());var a=ledger.account(p.getStringUUID());
   if(!action.equals("view"))msg=ledger.trade(p.getStringUUID(),stock,count,action.equals("buy"),quote);
   if(fresh||!action.equals("view")){try{save();}catch(Exception ex){ledger=JSON.fromJson(before,MarketLedger.class);msg="保存できなかったため注文を取り消しました";}}
   out.add("account",JSON.toJsonTree(ledger.account(p.getStringUUID())));out.add("prices",JSON.toJsonTree(ledger.prices));out.add("history",JSON.toJsonTree(ledger.history));out.addProperty("news",ledger.news);
  }
  out.addProperty("message",msg);out.addProperty("x",pos.getX());out.addProperty("y",pos.getY());out.addProperty("z",pos.getZ());ServerPlayNetworking.send(p,new Snapshot(out.toString()));return 1;
 }
}
