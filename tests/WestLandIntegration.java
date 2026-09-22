package jp.neonward;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
public final class WestLandIntegration {
 static void check(boolean b,String why){if(!b)throw new AssertionError("LAND: "+why);}
 static int product(String id){for(int i=0;i<LandCatalog.PRODUCTS.size();i++)if(LandCatalog.PRODUCTS.get(i).id().equals(id))return i;throw new AssertionError(id);}
 static ServerPlayer visitor(ServerLevel l){var profile=new com.mojang.authlib.GameProfile(UUID.randomUUID(),"LandQA");var p=new ServerPlayer(l.getServer(),l,profile,ClientInformation.createDefault()){@Override public boolean hasDisconnected(){return false;}};p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(l.getServer(),new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p,net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false)){@Override public boolean hasClientLoaded(){return true;}};p.setPos(40,65,9);l.addNewPlayer(p);return p;}
 static void phoneChecks(ServerPlayer p)throws Exception {
  String saved=StockMarket.JSON.toJson(StockMarket.ledger);
  var phone=PhoneEquipment.get(p).copy();
  var saveFile=StockMarket.file;
  try{
   var ledger=StockMarket.ledger;ledger.westLand.clear();ledger.westLandColumns=216;
   ledger.account(p.getStringUUID()).cash=50000;
   ((net.fabricmc.fabric.api.attachment.v1.AttachmentTarget)p).setAttached(PhoneEquipment.SLOT,new ItemStack(NeonWard.PHONE));
   var req=new com.google.gson.JsonObject();req.addProperty("app","8");req.addProperty("action","land_buy");req.addProperty("plot",2);
   PhoneServices.last.clear();PhoneServices.request(p,req);
   check(StockMarket.ledger.account(p.getStringUUID()).cash==30000&&WestLand.owned(p,2),"phone purchase charges 20k and grants exact selected plot");
   check(StockMarket.ledger.westLand.get(2).name.equals(p.getName().getString()),"phone uses actual player name");
   PhoneServices.last.clear();PhoneServices.request(p,req);
   check(StockMarket.ledger.account(p.getStringUUID()).cash==30000,"duplicate phone purchase no charge");
   var snapshot=PhoneServices.snapshot(StockMarket.ledger,p.getStringUUID(),req);
   check(snapshot.getAsJsonArray("land_plots").size()==8&&snapshot.get("land").getAsInt()==1&&!snapshot.has("farm"),"phone shows eight lots and no old farm");
   req.addProperty("plot",3);StockMarket.file=saveFile.getParent();PhoneServices.last.clear();PhoneServices.request(p,req);StockMarket.file=saveFile;
   check(StockMarket.ledger.account(p.getStringUUID()).cash==30000&&!WestLand.owned(p,3),"phone save failure rolls back cash and land");
   StockMarket.ledger.account(p.getStringUUID()).cash=19999;PhoneServices.last.clear();PhoneServices.request(p,req);
   check(StockMarket.ledger.account(p.getStringUUID()).cash==19999&&!WestLand.owned(p,3),"insufficient funds no land or debit");
   req.addProperty("plot",8);PhoneServices.last.clear();PhoneServices.request(p,req);
   check(!StockMarket.ledger.westLand.containsKey(8),"invalid plot rejected");
   req.addProperty("action","farm_buy");PhoneServices.last.clear();PhoneServices.request(p,req);
   check(StockMarket.ledger.account(p.getStringUUID()).cash==19999,"retired phone purchase cannot charge");
   p.setPos(327.5,65,634.5);PrivateFarms.request(p,"buy");PrivateFarms.request(p,"enter");
   check(p.level().dimension()==net.minecraft.world.level.Level.OVERWORLD&&StockMarket.ledger.account(p.getStringUUID()).cash==19999,"old counter cannot charge or enter retired dimension");
   check(!PrivateFarms.edit(p.level(),p,p.blockPosition()),"retired farm has no edit permission");
   System.out.println("LAND_PHONE_QA_PASS: selected plot, actual owner name, duplicate, eight plots, rollback, insufficient funds, invalid plot, retired routes");
  }finally{StockMarket.file=saveFile;StockMarket.ledger=StockMarket.JSON.fromJson(saved,MarketLedger.class);PhoneServices.last.clear();((net.fabricmc.fabric.api.attachment.v1.AttachmentTarget)p).setAttached(PhoneEquipment.SLOT,phone);}
 }
 public static void run(ServerPlayer p)throws Exception{phoneChecks(p);var l=p.level();var ledger=StockMarket.ledger;var saved=StockMarket.JSON.toJson(ledger);var inv=Cyberware.inventory(p);var old=p.position();var visitor=visitor(l);
  try{ledger.westLand.clear();ledger.westLandColumns=216;var a=ledger.account(p.getStringUUID());a.cash=200000;
   var pos=new BlockPos(LandLayout.x(0)+10,65,LandLayout.z(0)+10);p.setPos(Vec3.atCenterOf(pos));check(!HomeBuildingRules.canBreak(l,p,pos),"unbought is protected");
   for(int i=0;i<8;i++){ledger.westLand.put(i,new MarketLedger.LandOwner(p.getStringUUID(),"QA owner"));for(var test:List.of(new BlockPos(LandLayout.x(i),48,LandLayout.z(i)),new BlockPos(LandLayout.x(i)+31,127,LandLayout.z(i)+31))){check(HomeBuildingRules.canBreak(l,p,test),"owner can break exact edges");check(!HomeBuildingRules.canBreak(l,visitor,test),"visitor denied");}check(!WestLand.edit(l,p,new BlockPos(LandLayout.x(i),47,LandLayout.z(i))),"bottom protected");}
   ledger.westLand.clear();var tp=WestLand.terminal(0,0);l.setBlock(tp,WestLand.block(0).defaultBlockState(),3);p.setPos(Vec3.atCenterOf(tp));WestLand.open(p,0,0,"test");int token=WestLand.SESSIONS.get(p.getUUID()).token();WestLand.buy(p,token,-1,1);check(a.cash==180000&&WestLand.owned(p,0),"purchase debits and immediate ownership");WestLand.buy(p,token,-1,1);check(a.cash==180000,"replayed token cannot purchase twice");
   StockMarket.save();StockMarket.ledger=StockMarket.JSON.fromJson(java.nio.file.Files.readString(StockMarket.file),MarketLedger.class);StockMarket.ledger.validate();check(WestLand.edit(l,p,pos)&&!WestLand.edit(l,visitor,pos),"UUID permissions survive saved ledger reload");ledger=StockMarket.ledger;a=ledger.account(p.getStringUUID());
   var context=new BlockPlaceContext(l,p,InteractionHand.MAIN_HAND,new ItemStack(Items.STONE),new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false)){};check(HomeBuildingRules.canPlace(context,Blocks.STONE),"owner builds ordinary blocks despite city protection");
   check(HomeBuildingRules.editablePosition(l,p,pos)&&!HomeBuildingRules.editablePosition(l,visitor,pos),"furniture TV aquarium share parcel ownership");
   l.setBlock(pos,Blocks.BARREL.defaultBlockState(),3);var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);check(net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.invoker().interact(visitor,l,InteractionHand.MAIN_HAND,hit)==InteractionResult.FAIL,"visitor cannot open barrel");check(net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.invoker().interact(p,l,InteractionHand.MAIN_HAND,hit)==InteractionResult.PASS,"owner barrel not blocked by other protection callbacks");
   check(WestLand.transfer(l,pos,pos.east()),"water can move within purchased lot");check(!WestLand.transfer(l,new BlockPos(LandLayout.x(0)+31,65,LandLayout.z(0)),new BlockPos(LandLayout.x(0)+32,65,LandLayout.z(0))),"water cannot cross edge");check(!l.setBlock(pos,Blocks.TNT.defaultBlockState(),3),"TNT rejected");check(!l.setBlock(pos,Blocks.LAVA.defaultBlockState(),3),"lava rejected");
   check(!net.minecraft.world.level.block.piston.PistonBaseBlock.isPushable(Blocks.STONE.defaultBlockState(),l,pos,Direction.EAST,true,Direction.EAST),"piston cannot move protected block");
   var farmPos=pos.below();l.setBlock(farmPos,Blocks.FARMLAND.defaultBlockState(),3);Blocks.FARMLAND.fallOn(l,l.getBlockState(farmPos),farmPos,visitor,5);check(l.getBlockState(farmPos).is(Blocks.FARMLAND),"visitor cannot trample crops");
   tp=WestLand.terminal(0,1);l.setBlock(tp,WestLand.block(1).defaultBlockState(),3);p.setPos(Vec3.atCenterOf(tp));long before=a.cash;for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));WestLand.open(p,0,1,"test");WestLand.buy(p,WestLand.SESSIONS.get(p.getUUID()).token(),product("glass"),64);check(a.cash==before,"full inventory does not charge");p.getInventory().setItem(10,ItemStack.EMPTY);WestLand.open(p,0,1,"test");WestLand.buy(p,WestLand.SESSIONS.get(p.getUUID()).token(),product("glass"),16);check(a.cash==before-24*16&&p.getInventory().getItem(10).is(Items.GLASS)&&p.getInventory().getItem(10).getCount()==16,"selected count and price delivered");
   before=a.cash;WestLand.open(p,0,1,"test");WestLand.buy(p,WestLand.SESSIONS.get(p.getUUID()).token(),product("cow"),1);check(a.cash==before,"building terminal cannot buy animals");visitor.setPos(Vec3.atCenterOf(tp));var b=ledger.account(visitor.getStringUUID());b.cash=100000;WestLand.open(visitor,0,1,"test");WestLand.buy(visitor,WestLand.SESSIONS.get(visitor.getUUID()).token(),product("glass"),1);check(b.cash==100000,"visitor cannot shop for another lot");
   check(LandCatalog.PRODUCTS.size()>200,"large curated block catalog");check(LandCatalog.PRODUCTS.stream().noneMatch(v->v.id().contains("sword")||v.id().contains("spawn_egg")||v.id().contains("command_block")),"no weapons eggs admin blocks");
   // A construction restart never touches owned plots.
   ledger.westLandColumns=0;LandConstruction.column(l.getServer());check(ledger.westLandColumns==0,"construction blocked if any owner exists");
   ledger.westLand.clear();ledger.westLandColumns=21; // x=-227: first plot's purchase terminal
   LandConstruction.column(l.getServer());check(ledger.westLandColumns==22&&!LandConstruction.failed,"one generation column committed");check(l.getBlockState(WestLand.terminal(0,0)).is(WestLand.block(0)),"dedicated terminal generated");
   ledger.westLandColumns=0;while(ledger.westLandColumns<216&&!LandConstruction.failed)LandConstruction.column(l.getServer());check(!LandConstruction.failed&&ledger.westLandColumns==216,"all columns generated");for(int i=0;i<8;i++)for(int t=0;t<4;t++){check(l.getBlockState(WestLand.terminal(i,t)).is(WestLand.block(t)),"all 32 terminals present");check(l.getBlockEntity(WestLand.terminal(i,t).above()) instanceof net.minecraft.world.level.block.entity.SignBlockEntity,"all terminal signs present");}
   ledger.westLand.put(0,new MarketLedger.LandOwner(p.getStringUUID(),"QA"));tp=WestLand.terminal(0,2);p.setPos(Vec3.atCenterOf(tp));before=a.cash;for(String kind:List.of("cow","pig","chicken")){WestLand.open(p,0,2,"test");WestLand.buy(p,WestLand.SESSIONS.get(p.getUUID()).token(),product(kind),1);}check(a.cash==before-2300&&WestLand.animals(p,0)==3,"cow pig chicken delivery and exact prices");
   var animal=l.getEntitiesOfClass(net.minecraft.world.entity.animal.Animal.class,new AABB(LandLayout.x(0),48,LandLayout.z(0),LandLayout.x(0)+32,128,LandLayout.z(0)+32)).getFirst();animal.setPos(LandLayout.x(0)+40,65,LandLayout.z(0)+10);LandAnimals.contain(animal);check(LandLayout.plot(animal.blockPosition().getX(),animal.blockPosition().getZ())==0,"purchased animal remains inside original plot");
   tp=WestLand.terminal(0,3);p.setPos(Vec3.atCenterOf(tp));p.getInventory().setItem(11,ItemStack.EMPTY);before=a.cash;WestLand.open(p,0,3,"test");WestLand.buy(p,WestLand.SESSIONS.get(p.getUUID()).token(),product("water_bucket"),1);check(a.cash==before-100&&p.getInventory().getItem(11).is(Items.WATER_BUCKET),"farming terminal water purchase");
   p.getInventory().setItem(12,ItemStack.EMPTY);before=a.cash;WestLand.open(p,0,3,"test");WestLand.buy(p,WestLand.SESSIONS.get(p.getUUID()).token(),product("wheat_seeds"),16);check(a.cash==before-320&&p.getInventory().getItem(12).is(Items.WHEAT_SEEDS),"seeds from farming terminal");
   var saveFile=StockMarket.file;before=a.cash;try{StockMarket.file=saveFile.getParent();WestLand.open(p,0,3,"test");WestLand.buy(p,WestLand.SESSIONS.get(p.getUUID()).token(),product("wheat_seeds"),16);check(a.cash==before&&p.getInventory().getItem(12).getCount()==16,"save failure rolls back cash and inventory");}finally{StockMarket.file=saveFile;}
   check(!animal.hurtServer(l,l.damageSources().playerAttack(visitor),1),"visitor damage to purchased animal denied");
   for(int n=0;n<9;n++){var extra=EntityTypes.COW.create(l,EntitySpawnReason.COMMAND);extra.setPos(LandLayout.x(0)+10,65,LandLayout.z(0)+10);LandAnimals.mark(extra,0);l.addFreshEntity(extra);}check(WestLand.animals(p,0)==12,"12 animal cap fixture");tp=WestLand.terminal(0,2);p.setPos(Vec3.atCenterOf(tp));before=a.cash;WestLand.open(p,0,2,"test");WestLand.buy(p,WestLand.SESSIONS.get(p.getUUID()).token(),product("chicken"),1);check(a.cash==before&&WestLand.animals(p,0)==12,"animal cap purchase does not charge");
   var herbHole=new BlockPos(LandLayout.x(3)+6,64,LandLayout.z(3)+6);l.setBlock(herbHole.below(),Blocks.DIRT.defaultBlockState(),3);l.setBlock(herbHole,Medicine.HERB.defaultBlockState(),3);
   var deliberateHole=herbHole.east();l.setBlock(deliberateHole,Blocks.AIR.defaultBlockState(),3);var preserved=herbHole.east(2);l.setBlock(preserved,Blocks.BARREL.defaultBlockState(),3);
   var occupiedWall=new BlockPos(LandLayout.x(3)-1,65,LandLayout.z(3)+4);l.setBlock(occupiedWall,Blocks.BARREL.defaultBlockState(),3);
   ledger.westLandRepairColumns=0;while(ledger.westLandRepairColumns<216&&!LandConstruction.failed)LandConstruction.repairColumn(l.getServer());check(!LandConstruction.failed,"migration succeeds with purchased land");
   check(l.getBlockState(herbHole).is(Blocks.GRASS_BLOCK),"herb at floor level becomes real ground");check(l.getBlockState(deliberateHole).isAir(),"owner excavation is not refilled");check(l.getBlockState(preserved).is(Blocks.BARREL)&&l.getBlockState(occupiedWall).is(Blocks.BARREL),"existing construction and containers preserved");check(l.getBlockState(occupiedWall.above()).isAir(),"no wall stacked over existing container");
   for(int id=0;id<8;id++){int X=LandLayout.x(id),Z=LandLayout.z(id);check(!LandLayout.wall(X,Z)&&!LandLayout.wall(X+31,Z+31),"32x32 footprint remains free");for(int dx=14;dx<=17;dx++){var gate=new BlockPos(X+dx,65,id<4?Z+32:Z-1);check(!LandLayout.wall(gate.getX(),gate.getZ())&&l.getBlockState(gate).isAir(),"four-block entrance remains open");}check(l.getBlockState(new BlockPos(X-1,65,Z-1)).is(Blocks.STONE_BRICKS),"outside corner wall installed");}
   l.setBlock(herbHole,Blocks.AIR.defaultBlockState(),3);LandConstruction.repairColumn(l.getServer());check(l.getBlockState(herbHole).isAir(),"completed migration never rebuilds player changes");
   var roadMark=new BlockPos(-50,64,154);l.setBlock(roadMark,Blocks.GOLD_BLOCK.defaultBlockState(),3);ledger.westLandTunnelColumns=0;while(ledger.westLandTunnelColumns<40&&!LandConstruction.failed)LandTunnel.column(l.getServer());check(!LandConstruction.failed&&ledger.westLandTunnelColumns==40,"tunnel built");check(l.getBlockState(roadMark).is(Blocks.GOLD_BLOCK),"existing road markings preserved");
   for(int xx=-67;xx<=-28;xx++){check(!l.getBlockState(new BlockPos(xx,97,154)).isAir(),"continuous roof");check(!l.getBlockState(new BlockPos(xx,65,137)).isAir()&&!l.getBlockState(new BlockPos(xx,65,171)).isAir(),"continuous side walls");for(int zz=148;zz<=160;zz++)for(int yy=65;yy<=70;yy++)check(l.getBlockState(new BlockPos(xx,yy,zz)).isAir()||l.getBlockState(new BlockPos(xx,yy,zz)).is(Blocks.OAK_SIGN),"main passage has no barrier");}
   check(LandLayout.plot(-67,154)==-1,"tunnel does not consume purchased land");
   ledger.westLandPerimeterColumns=0;while(ledger.westLandPerimeterColumns<216&&!LandConstruction.failed)LandPerimeter.column(l.getServer());check(!LandConstruction.failed,"outer perimeter built");int perimeter=0;
   for(int xx=-248;xx<=-33;xx++)for(int zz=88;zz<=216;zz++)if(LandLayout.perimeter(xx,zz)){perimeter++;check(LandLayout.plot(xx,zz)==-1,"outer wall stays outside purchased plots");for(int yy=65;yy<=69;yy++)check(!l.getBlockState(new BlockPos(xx,yy,zz)).isAir(),"continuous five-block outer wall");}
   check(perimeter==653,"full exterior boundary except 33-wide tunnel");for(int zz=148;zz<=160;zz++)for(int yy=65;yy<=70;yy++)check(l.getBlockState(new BlockPos(-33,yy,zz)).isAir()||l.getBlockState(new BlockPos(-33,yy,zz)).is(Blocks.OAK_SIGN),"perimeter cannot obstruct tunnel");
   var removedWall=new BlockPos(-248,65,90);l.setBlock(removedWall,Blocks.AIR.defaultBlockState(),3);LandPerimeter.column(l.getServer());check(l.getBlockState(removedWall).isAir(),"completed outer wall migration not replayed");
   var chestWall=new BlockPos(-248,65,92);l.setBlock(chestWall,Blocks.BARREL.defaultBlockState(),3);l.setBlock(chestWall.above(),Blocks.AIR.defaultBlockState(),3);ledger.westLandPerimeterColumns=0;LandPerimeter.column(l.getServer());check(l.getBlockState(chestWall).is(Blocks.BARREL)&&l.getBlockState(chestWall.above()).isAir(),"outer wall preserves occupied container column");
   ledger.westLand.put(0,new MarketLedger.LandOwner(p.getStringUUID(),"kurotoufu_LHP"));LandOwnerSigns.refresh(l,0);var signArea=new AABB(LandOwnerSigns.center(0),LandOwnerSigns.center(0)).inflate(8);var signs=l.getEntitiesOfClass(Display.class,signArea,e->e.entityTags().stream().anyMatch(t->t.startsWith("nw_land_owner_0_")));check(signs.size()==9,"nine neon sign parts for existing owner");
   for(var e:signs)if(e instanceof Display.TextDisplay text)check(((jp.neonward.mixin.MeterTextAccess)text).neonGetText().getString().contains("kurotoufu_LHP"),"owner on both sign faces");
   int poseUpdates=ClockworkDisplays.updates;for(int n=0;n<10;n++)LandOwnerSigns.refresh(l,0);check(ClockworkDisplays.updates==poseUpdates,"static sign poses not resent");LandOwnerSigns.CACHE.clear();LandOwnerSigns.refresh(l,0);check(l.getEntitiesOfClass(Display.class,signArea,e->e.entityTags().stream().anyMatch(t->t.startsWith("nw_land_owner_0_"))).size()==9,"cache reset does not duplicate signs");
   ledger.westLand.get(0).name="UpdatedOwner";LandOwnerSigns.refresh(l,0);for(var e:signs)if(e instanceof Display.TextDisplay text)check(((jp.neonward.mixin.MeterTextAccess)text).neonGetText().getString().contains("UpdatedOwner"),"sign follows authoritative owner record");
   System.out.println("LAND_OWNER_SIGN_QA_PASS: existing owner, both faces, nine parts, static poses, no duplicate on cache reset, name update");
   System.out.println("LAND_PERIMETER_QA_PASS: 653 wall columns, height 5, protected plots, open tunnel, one-time generation and container preservation");
   System.out.println("LAND_TUNNEL_QA_PASS: 40 columns, connected roof/walls, open passage, existing road markings preserved");
   System.out.println("LAND_REPAIR_QA_PASS: herb holes, 8 exterior walls, four-wide entrances, purchased plots, container preservation and one-time migration");
   System.out.println("WEST_LAND_QA_PASS: 8 borders, owner/visitor, saved UUID reload, purchase replay, full inventory, separated shops, water/piston/TNT/lava/trample protection, complete generation, 32 signs, 3 animal purchases, containment, farming supplies");
  }finally{StockMarket.ledger=StockMarket.JSON.fromJson(saved,MarketLedger.class);StockMarket.save();Cyberware.restore(p,inv);p.setPos(old);l.removePlayerImmediately(visitor,Entity.RemovalReason.DISCARDED);WestLand.SESSIONS.clear();}
 }
}
