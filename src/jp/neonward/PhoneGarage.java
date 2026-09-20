package jp.neonward;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import java.util.Comparator;

public final class PhoneGarage {
 static final AttachmentType<Long> NEXT=AttachmentRegistry.<Long>builder().buildAndRegister(NeonWard.id("garage_cooldown"));
 public static void init(){
  CommandRegistrationCallback.EVENT.register((d,c,e)->{
   var root=Commands.literal("neongarage");
   for(var spec:VehicleCatalog.ALL)root.then(Commands.literal(spec.id()).executes(ctx->call(ctx.getSource().getPlayerOrException(),spec)));
   root.then(Commands.literal("buy").then(Commands.argument("vehicle",StringArgumentType.word()).executes(ctx->buy(ctx.getSource().getPlayerOrException(),StringArgumentType.getString(ctx,"vehicle")))));
   root.then(Commands.literal("view").executes(ctx->{var p=ctx.getSource().getPlayerOrException();var a=StockMarket.ledger==null?null:StockMarket.ledger.account(p.getStringUUID());var o=new com.google.gson.JsonObject();o.addProperty("garage",true);o.addProperty("shop",sales(p));o.addProperty("cash",a==null?0:a.cash);o.add("casinoVehicles",StockMarket.JSON.toJsonTree(a==null?java.util.Set.of():a.casinoVehicles));var owned=new java.util.HashSet<String>();if(a!=null){if(a.casinoVehicles!=null)owned.addAll(a.casinoVehicles);if(a.street!=null&&a.street.vehicles!=null)owned.addAll(a.street.vehicles.keySet());}o.add("ownedVehicles",StockMarket.JSON.toJsonTree(owned));net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(p,new StockMarket.Snapshot(o.toString()));return 1;}));
   root.then(Commands.literal("recall").executes(ctx->recall(ctx.getSource().getPlayerOrException())));d.register(root);
  });
 }
 static int message(ServerPlayer p,String text){p.sendSystemMessage(Component.literal(text));p.sendOverlayMessage(Component.literal(text));return 0;}
 static boolean sales(ServerPlayer p){return p.level().dimension()==net.minecraft.world.level.Level.OVERWORLD&&p.level().getEntitiesOfClass(CityResident.class,p.getBoundingBox().inflate(8)).stream().anyMatch(n->n.job.equals("car_sales"));}
 static boolean ordinary(String id){return !CasinoGames.PRIZES.contains(id);}
 static int price(String id){return switch(id){case "car"->5000;case "razor"->12000;case "executive"->10000;case "bulwark"->15000;case "hauler"->9000;case "nomad"->11000;case "bike"->4000;case "volt"->14000;case "chopper"->7000;case "cruiser"->8500;case "scrambler"->7500;case "courier"->6500;default->0;};}
 static boolean owned(MarketLedger.Account a,String id){return a!=null&&(a.casinoVehicles!=null&&a.casinoVehicles.contains(id)||a.street!=null&&a.street.vehicles!=null&&a.street.vehicles.containsKey(id));}
 static int call(ServerPlayer p,boolean bike){return call(p,VehicleCatalog.get(bike?"bike":"car"));}
 static int buy(ServerPlayer p,String id){if(p.isSpectator()||StockMarket.ledger==null)return 0;var spec=VehicleCatalog.get(id);if(spec==null||!ordinary(id)||price(id)<=0)return message(p,"購入できない車種です");if(!sales(p))return message(p,"車屋の購入カウンターの近くで購入してください");var a=StockMarket.ledger.account(p.getStringUUID());if(owned(a,id))return message(p,"その車両はすでに所有しています");int cost=price(id);if(a.cash<cost)return message(p,"残高が足りません（必要額 "+cost+" Cr）");a.cash-=cost;a.casinoVehicles.add(id);try{StockMarket.save();}catch(Exception e){a.cash+=cost;a.casinoVehicles.remove(id);return message(p,"購入を保存できなかったため取り消しました");}return message(p,spec.name()+"を購入しました（"+cost+" Cr）。ガレージから呼び出せます");}
 static int call(ServerPlayer p,VehicleCatalog.Spec spec){boolean bike=spec.bike();
  if(!owned(StockMarket.ledger==null?null:StockMarket.ledger.account(p.getStringUUID()),spec.id()))return message(p,ordinary(spec.id())?"車屋で購入するとガレージから呼び出せます":"この限定車はカジノの景品で入手できます。");
  if(p.isSpectator()||PhoneEquipment.get(p).isEmpty())return message(p,"スマホを装備してください。");
  if(p.isPassenger())return message(p,"車両から降りてから呼び出してください。");
  var level=(ServerLevel)p.level();var target=(AttachmentTarget)p;long now=level.getGameTime();
  if(now<target.getAttachedOrElse(NEXT,0L))return message(p,"配車の操作は少し待ってからお願いします。");
  target.setAttached(NEXT,now+40);
  var owned=level.getEntitiesOfClass(StreetVehicle.class,p.getBoundingBox().inflate(64)).stream().filter(v->p.getUUID().equals(v.owner)&&!v.entityTags().contains("nw_recovery")).toList();
  var available=owned.stream().filter(v->v.spec().id().equals(spec.id())&&v.getPassengers().stream().noneMatch(r->r instanceof Player))
   .min(Comparator.comparingDouble(v->v.distanceToSqr(p))).orElse(null);
  if(available==null&&owned.stream().anyMatch(v->v.spec().id().equals(spec.id())))return message(p,"その車両は使用中です。乗っている人が降りるまで呼び出せません。");
  if(available==null&&owned.size()>=8)return message(p,"近くの車両が上限です。使わない自分の車両をガレージの回収ボタンで回収してください。");
  var v=available!=null?available:new StreetVehicle(bike?NeonWard.BIKE:NeonWard.CAR,level);
  if(available==null)v.variant(spec.id());
  Vec3 spot=findSpace(level,p,v);
  if(spot==null)return message(p,"呼び出せる空きスペースがありません。広い道路や広場で使ってください。");
  v.setPos(spot);v.setYRot(p.getYRot());v.owner=p.getUUID();v.stopForDelivery();
  if(available==null&&!level.addFreshEntity(v))return message(p,"呼び出しに失敗しました。もう一度お試しください。");
  target.setAttached(PhoneEquipment.HELD,false);
  message(p,spec.name()+"を近くに呼び出しました。右クリックで乗車できます。");return 1;
 }
 static boolean reclaimable(StreetVehicle v,ServerPlayer p){return !v.entityTags().contains("nw_recovery")&&p.getUUID().equals(v.owner)&&v.cargoEmpty()&&v.getPassengers().stream().noneMatch(e->e instanceof Player);}
 static int recall(ServerPlayer p){
  var all=((ServerLevel)p.level()).getEntitiesOfClass(StreetVehicle.class,p.getBoundingBox().inflate(64));int n=0;
  for(var v:all)if(reclaimable(v,p)){for(var part:java.util.List.copyOf(v.getPassengers()))part.discard();v.discard();n++;}
  return message(p,n+"台を回収しました。荷物や乗員がいる車両は残しています。");
 }
 static Vec3 findSpace(ServerLevel level,ServerPlayer p,StreetVehicle vehicle){
  // Check the whole visible model envelope, including its wheels and rear wing.
  for(int distance:new int[]{8,12,16})for(int angle:new int[]{0,45,-45,90,-90,180})for(int dy:new int[]{0,-1,1,-2,2}){
   Vec3 direction=Vec3.directionFromRotation(0,p.getYRot()+angle);
   double x=p.getX()+direction.x*distance,z=p.getZ()+direction.z*distance,y=Math.floor(p.getY())+dy;
   double radius=vehicle.bike()?2.8:4.6; AABB box=new AABB(x-radius,y+.05,z-radius,x+radius,y+4.5,z+radius);
   if(!level.getWorldBorder().isWithinBounds(box)||!level.hasChunkAt(BlockPos.containing(x-5,y,z-5))||!level.hasChunkAt(BlockPos.containing(x+5,y,z+5)))continue;
   if(!level.noCollision(vehicle,box)||level.containsAnyLiquid(box))continue;
   boolean supported=true;
   for(double[] offset:new double[][]{{0,0},{-2,-2},{2,-2},{-2,2},{2,2}}){
    var floor=BlockPos.containing(x+offset[0],y-1,z+offset[1]);
    if(!level.getBlockState(floor).isSolidRender()||!level.getFluidState(floor).isEmpty()){supported=false;break;}
   }
   if(supported)return new Vec3(x,y+.1,z);
  }
  return null;
 }
}
