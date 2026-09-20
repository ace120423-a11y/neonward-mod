package jp.neonward;
import java.util.*;
import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.network.chat.Component;
public final class Underworld {
 public static boolean hunter(Entity e){return e.entityTags().contains("nw_pursuer");}
 static boolean contact(ServerPlayer p){return CityProtection.contains(p.level(),p.blockPosition())&&p.level().getEntitiesOfClass(CityResident.class,p.getBoundingBox().inflate(8)).stream().anyMatch(n->n.kind().role()==8||n.kind().role()==9);}
 static StreetProgress data(ServerPlayer p){return StockMarket.ledger.account(p.getStringUUID()).street;}
 static void init(){
  CommandRegistrationCallback.EVENT.register((d,c,e)->d.register(Commands.literal("underworld").executes(ctx->request(ctx.getSource().getPlayerOrException(),"view",0)).then(Commands.literal("view").executes(ctx->request(ctx.getSource().getPlayerOrException(),"view",0))).then(Commands.literal("accept").then(Commands.argument("type",IntegerArgumentType.integer(0,2)).executes(ctx->request(ctx.getSource().getPlayerOrException(),"accept",IntegerArgumentType.getInteger(ctx,"type"))))).then(Commands.literal("claim").executes(ctx->request(ctx.getSource().getPlayerOrException(),"claim",0))).then(Commands.literal("abandon").executes(ctx->request(ctx.getSource().getPlayerOrException(),"abandon",0)))));
  ServerTickEvents.END_SERVER_TICK.register(Underworld::tick);
  ServerLivingEntityEvents.AFTER_DEATH.register((e,s)->{if(e instanceof ServerPlayer p&&StockMarket.ledger!=null){data(p).clear();data(p).heat=0;persist();}});
 }
 static void persist(){try{StockMarket.save();}catch(Exception ex){System.err.println("BLACKLINE persistence: "+ex);}}
 static int request(ServerPlayer p,String action,int type){if(p.isSpectator()||StockMarket.ledger==null)return 0;String msg="追跡者は街の外だけ。指名手配中は外で隠れて解除。";
  if(!action.equals("view")){String before=StockMarket.JSON.toJson(StockMarket.ledger);try{var a=StockMarket.ledger.account(p.getStringUUID());var s=a.street;long now=System.currentTimeMillis();
   if(action.equals("accept")){if(!contact(p))throw new IllegalArgumentException("マフィアの幹部か用心棒の近くで受注してください");s.accept(type,now);int[][] destinations={{-140,200},{650,570},{210,1060}};var dest=destinations[p.getRandom().nextInt(destinations.length)];s.targetX=dest[0];s.targetZ=dest[1];msg="受注 / "+objective(s);}
   else if(action.equals("claim")){if(!contact(p))throw new IllegalArgumentException("マフィアの近くで報告してください");a.cash=Math.addExact(a.cash,s.claim(now));msg="報酬を入金。裏社会の評判が上がりました";}
   else if(action.equals("abandon")){s.clear();s.heat=0;s.cooldown=now+30_000;msg="依頼を中止しました。報酬はありません";}
   StockMarket.save();
  }catch(Exception ex){StockMarket.ledger=StockMarket.JSON.fromJson(before,MarketLedger.class);msg=ex instanceof IllegalArgumentException?ex.getMessage():"保存に失敗したため操作を取り消しました";}}
  var s=data(p);var o=new JsonObject();o.addProperty("street_ui","underworld");o.addProperty("summary","裏ランク "+s.rank()+" / 評判 "+s.reputation+" / 手配 "+s.heat+" / "+(s.mission<0?"依頼なし":"報酬 "+s.reward()+" Cr"));o.addProperty("objective",objective(s));boolean nav=s.mission>=0&&((s.stage==0&&(s.mission==0||s.mission==1))||(s.stage==1&&s.mission==1)||(s.stage==2&&s.heat==0));int nx=s.targetX,nz=s.targetZ;String navName=s.mission==1&&s.stage==1?"南の外壁・回収車返却地点":s.stage==2?"地下・マフィア事務所":"裏仕事の目的地";if(s.stage==1&&s.mission==1){nx=160;nz=1000;}if(s.stage==2){nx=128;nz=877;}o.addProperty("nav",nav);o.addProperty("nav_x",nx);o.addProperty("nav_z",nz);o.addProperty("nav_name",navName);o.addProperty("message",msg);ServerPlayNetworking.send(p,new StockMarket.Snapshot(o.toString()));return 1;
 }
 static String objective(StreetProgress s){if(s.mission<0)return "3種類の仕事から選べます";if(s.stage==2)return s.heat==0?"完了：マフィアへ報告":"追跡解除まで隠れる：残り "+s.heat+" 秒";if(s.stage==1&&s.mission==1)return "回収車を南の外壁前 X160 Z1000 へ";if(s.mission==2)return "外で自分の車を運転："+s.escapeSeconds+" / 90秒";return (s.mission==0?"届け先":"回収車の場所")+" X"+s.targetX+" Z"+s.targetZ;}
 static boolean near(ServerPlayer p,int x,int z,int radius){return p.level().dimension()==Level.OVERWORLD&&Math.abs(p.getX()-x)<radius&&Math.abs(p.getZ()-z)<radius;}
 static void tick(MinecraftServer server){if(server.getTickCount()%20!=0||StockMarket.ledger==null)return;long now=System.currentTimeMillis();
  for(var level:server.getAllLevels())for(var e:level.getAllEntities()){
   if(hunter(e)){String target=e.entityTags().stream().filter(t->t.startsWith("nw_target_")).findFirst().orElse("");ServerPlayer p=null;try{p=server.getPlayerList().getPlayer(UUID.fromString(target.substring(10)));}catch(Exception ignored){}if(p==null||!p.isAlive()||data(p).heat==0||!NeonZones.isField(p.level(),p.blockPosition())||e.distanceToSqr(p)>160*160)e.discard();}
   if(e instanceof StreetVehicle v&&v.entityTags().contains("nw_recovery")){var p=v.owner==null?null:server.getPlayerList().getPlayer(v.owner);var job=v.owner==null?null:StockMarket.ledger.account(v.owner.toString()).street;if(job==null||job.mission!=1||now>job.deadline||(p!=null&&!p.isAlive())||!job.recovery.equals(v.getStringUUID())){for(var part:List.copyOf(v.getPassengers())){if(part instanceof Display)part.discard();else part.stopRiding();}v.discard();}}
  }
  for(var p:server.getPlayerList().getPlayers()){var s=data(p);if(s.mission<0)continue;
   if(now>s.deadline||!p.isAlive()){s.clear();s.heat=0;p.sendSystemMessage(Component.literal("BLACKLINE / 依頼失敗：制限時間または死亡"));persist();continue;}
   if(p.isSpectator())continue;boolean field=NeonZones.isField(p.level(),p.blockPosition());
   if(s.mission==0&&s.stage==0&&field&&near(p,s.targetX,s.targetZ,10)){s.stage=2;s.heat=60;p.sendSystemMessage(Component.literal("荷物を引き渡した！ 追跡を振り切って報告しろ。"));persist();}
   if(s.mission==1&&s.stage==0&&field&&near(p,s.targetX,s.targetZ,45)){
    var level=(ServerLevel)p.level();var v=new StreetVehicle(NeonWard.CAR,level);v.variant("hauler");var spot=PhoneGarage.findSpace(level,p,v);if(spot!=null){v.owner=p.getUUID();v.addTag("nw_recovery");v.setPos(spot);if(level.addFreshEntity(v)){s.recovery=v.getStringUUID();s.stage=1;s.heat=75;p.sendSystemMessage(Component.literal("回収車が到着。これに乗って南の外壁前 X160 Z1000 へ。"));persist();}}}
   if(s.mission==1&&s.stage==1&&near(p,160,1000,16)&&p.getVehicle() instanceof StreetVehicle v&&s.recovery.equals(v.getStringUUID())){p.stopRiding();for(var part:List.copyOf(v.getPassengers()))if(part instanceof Display)part.discard();v.discard();s.recovery="";s.stage=2;persist();}
   if(s.mission==2&&s.stage==0&&field&&p.getVehicle() instanceof StreetVehicle v&&p.getUUID().equals(v.owner)&&v.speedKmh()>10){s.escapeSeconds++;s.heat=60;if(s.escapeSeconds>=90){s.stage=2;p.sendSystemMessage(Component.literal("囮の仕事は完了！ 追跡を振り切れ。"));persist();}}
   boolean seen=field&&p.level().getEntitiesOfClass(CyberEnemy.class,p.getBoundingBox().inflate(40)).stream().anyMatch(e->hunter(e)&&e.entityTags().contains("nw_target_"+p.getStringUUID())&&e.getSensing().hasLineOfSight(p));
   if(s.heat>0&&field&&!seen)s.heat--; // Entering a safe district never clears a pursuit.
   if(s.heat>0&&field&&server.getTickCount()%300==0)spawn(p,s);
   p.sendOverlayMessage(Component.literal("BLACKLINE / "+objective(s)+" / 残り"+Math.max(0,(s.deadline-now)/1000)+"秒"));
  }
  if(server.getTickCount()%200==0&&StockMarket.ledger.accounts.values().stream().anyMatch(a->a.street.mission>=0))persist();
 }
 static void spawn(ServerPlayer p,StreetProgress s){var level=(ServerLevel)p.level();if(level.getEntitiesOfClass(CyberEnemy.class,p.getBoundingBox().inflate(160)).stream().filter(Underworld::hunter).count()>=4)return;
  for(int tries=0;tries<8;tries++){double angle=p.getRandom().nextDouble()*Math.PI*2;int x=(int)(p.getX()+Math.cos(angle)*30),z=(int)(p.getZ()+Math.sin(angle)*30);if(!level.hasChunk(x>>4,z>>4))continue;int y=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);var pos=new BlockPos(x,y,z);if(!NeonZones.isField(level,pos)||!level.getFluidState(pos).isEmpty())continue;var e=new CyberEnemy(NeonHostiles.TYPES.get(s.rank()>2?"arc_trooper":"street_enforcer"),level);e.addTag("nw_pursuer");e.addTag("nw_target_"+p.getStringUUID());e.setCustomName(Component.literal("企業警備・追跡部隊"));e.setPos(x+.5,y,z+.5);e.setTarget(p);if(level.noCollision(e)&&level.addFreshEntity(e))break;}
 }
}
