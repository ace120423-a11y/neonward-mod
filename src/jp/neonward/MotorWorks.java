package jp.neonward;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.google.gson.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
public final class MotorWorks {
 static StreetProgress data(ServerPlayer p){return StockMarket.ledger.account(p.getStringUUID()).street;}
 static StreetProgress.Tune tune(StreetVehicle v){if(StockMarket.ledger==null||v.owner==null||v.showroom)return new StreetProgress.Tune();return StockMarket.ledger.account(v.owner.toString()).street.tune(v.spec().id());}
 static boolean workshop(ServerPlayer p){return p.level().dimension()==net.minecraft.world.level.Level.OVERWORLD&&p.level().getEntitiesOfClass(CityResident.class,p.getBoundingBox().inflate(8)).stream().anyMatch(n->n.job.equals("dealer")||n.kind().role()==4);}
 static void init(){CommandRegistrationCallback.EVENT.register((d,c,e)->d.register(Commands.literal("neonmotor").then(Commands.argument("vehicle",StringArgumentType.word()).then(Commands.argument("action",StringArgumentType.word()).executes(ctx->request(ctx.getSource().getPlayerOrException(),StringArgumentType.getString(ctx,"vehicle"),StringArgumentType.getString(ctx,"action")))))));}
 static int request(ServerPlayer p,String id,String action){if(p.isSpectator()||StockMarket.ledger==null)return 0;var spec=VehicleCatalog.get(id);if(spec==null)return 0;String msg="車屋の店員・横丁の整備工の近くで改造できます";
  if(!action.equals("view")){String before=StockMarket.JSON.toJson(StockMarket.ledger);try{
   if(!workshop(p))throw new IllegalArgumentException(msg);
   var a=StockMarket.ledger.account(p.getStringUUID());if(!CasinoGames.unlocked(a,id))throw new IllegalArgumentException("未入手の景品車両は改造できません");
   var t=a.street.tune(id);a.cash=StreetProgress.upgrade(t,action,a.cash,spec.rows()+t.cargo>=6);StockMarket.save();
   for(var level:p.level().getServer().getAllLevels())for(var e:level.getAllEntities())if(e instanceof StreetVehicle v&&p.getUUID().equals(v.owner)&&v.spec().id().equals(id))v.refreshModel();
   msg="改造完了 / 再配車・再接続後も引き継ぎます";
  }catch(Exception ex){StockMarket.ledger=StockMarket.JSON.fromJson(before,MarketLedger.class);msg=ex instanceof IllegalArgumentException?ex.getMessage():"保存に失敗したため代金と改造を取り消しました";}}
  var a=StockMarket.ledger.account(p.getStringUUID());var t=a.street.tune(id);var o=new JsonObject();o.addProperty("street_ui","motor");o.addProperty("vehicle",id);o.addProperty("cash",a.cash);o.addProperty("message",msg);o.add("tune",StockMarket.JSON.toJsonTree(t));o.addProperty("near",workshop(p));ServerPlayNetworking.send(p,new StockMarket.Snapshot(o.toString()));return 1;
 }
 static VehicleCatalog.Spec performance(StreetVehicle v){var s=v.spec();var t=tune(v);return new VehicleCatalog.Spec(s.id(),s.name(),s.bike(),s.top()*(1+.10*t.engine),s.acceleration()*(1+.15*t.engine),s.seats(),Math.min(6,s.rows()+t.cargo),s.style());}
 static java.util.List<StreetVehicle.Part> model(StreetVehicle v){var t=tune(v);var out=new java.util.ArrayList<StreetVehicle.Part>();String[] paint={"","blue_concrete","red_concrete","white_concrete","purple_concrete"};for(var part:FleetModels.model(v.spec())){
  String block=part.block();if(t.paint>0&&part.y()>.65&&part.h()>.12&&(block.endsWith("_concrete")||block.endsWith("_terracotta"))&&!block.equals("redstone_block"))block=paint[t.paint];out.add(new StreetVehicle.Part(part.x(),part.y(),part.z(),part.w(),part.h(),part.d(),block));}
  if(t.neon>0){String glow=new String[]{"","sea_lantern","magenta_concrete","lime_concrete"}[t.neon];double half=v.bike()?.4:1.5;for(double x:new double[]{-half,half})out.add(new StreetVehicle.Part(x,.35,v.bike()?-1:-2,.05,.07,v.bike()?2:4,glow));}return out;
 }
}
