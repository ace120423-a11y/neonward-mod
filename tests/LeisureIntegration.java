package jp.neonward;

import java.nio.file.*;
import java.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.*;

/** Test-only addon. Refuses to run without the runner's isolated-world sentinel. */
public final class LeisureIntegration implements ModInitializer {
 static int checks;boolean done;
 final Set<net.minecraft.world.level.ChunkPos> foodTickets=new HashSet<>();
 static final int[][] CENTERS={{358,620},{160,863},{79,484},{327,600},{190,154}};
 public LeisureIntegration(){ServerLifecycleEvents.SERVER_STARTED.register(server->{
  guard(server);var level=server.overworld();
  for(var c:CENTERS)platform(level,c[0],c[1],12);
  var food=ExistingEateries.destination();platform(level,food.x(),food.z(),4);
  // Chunk forcing is asynchronous: allow real server ticks before spawning/querying
  // the fifty vendors. Same-tick getChunk/addFreshEntity does not ensure UUID lookup visibility.
  for(var site:ExistingEateries.ALL){var chunk=new net.minecraft.world.level.ChunkPos(site.staff().getX()>>4,site.staff().getZ()>>4);
   if(!level.getForceLoadedChunks().contains(chunk.pack())){foodTickets.add(chunk);level.setChunkForced(chunk.x(),chunk.z(),true);}level.getChunk(chunk.x(),chunk.z());
  }
 });}
 void releaseFoodTickets(MinecraftServer server){for(var chunk:foodTickets)server.overworld().setChunkForced(chunk.x(),chunk.z(),false);foodTickets.clear();}
 static void guard(MinecraftServer server){
  Path expected=Path.of(System.getProperty("neonward.leisure.qa.root","__missing__")).toAbsolutePath().normalize();
  Path actual=server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
  check(actual.equals(expected.resolve("world"))&&Files.isRegularFile(expected.resolve("LEISURE_QA_ONLY")),"isolated world guard");
 }
 static void platform(ServerLevel level,int x,int z,int radius){
  for(int xx=x-radius-2;xx<=x+radius+2;xx+=8)for(int zz=z-radius-2;zz<=z+radius+2;zz+=8){level.getChunk(xx>>4,zz>>4);level.setChunkForced(xx>>4,zz>>4,true);}
  for(int xx=x-radius;xx<=x+radius;xx++)for(int zz=z-radius;zz<=z+radius;zz++)for(int y=64;y<=70;y++)
   level.setBlock(new BlockPos(xx,y,zz),(y==64?Blocks.SMOOTH_STONE:Blocks.AIR).defaultBlockState(),2);
 }
 @Override public void onInitialize(){ServerTickEvents.END_SERVER_TICK.register(server->{
  if(done||server.getTickCount()<120)return;
  boolean foodReady=ExistingEateries.ALL.stream().allMatch(site->server.overworld().isPositionEntityTicking(site.staff()));
  if(!foodReady&&server.getTickCount()<320)return;done=true;
  ServerPlayer p=null,other=null;
  try{guard(server);check(foodReady,"all food fixture chunks entity-ticking within bounded warmup");p=visitor(server);other=visitor(server);run(p,other);
   final var player=p;final var visitor=other;
   TrainingIntegration.runDelayed(p).whenComplete((result,failure)->{
    try{if(failure!=null){System.out.println("LEISURE_TEST_FAILED");failure.printStackTrace();}else System.out.println("LEISURE_TEST_COMPLETE checks="+checks+" trainingDelayed=105ticks");}
    finally{cleanup(player);cleanup(visitor);releaseFoodTickets(server);}
   });
  }
  catch(Throwable failure){System.out.println("LEISURE_TEST_FAILED");failure.printStackTrace();cleanup(p);cleanup(other);releaseFoodTickets(server);}
 });}
 static void check(boolean ok,String reason){checks++;if(!ok)throw new AssertionError("LEISURE: "+reason);}
 static ServerPlayer visitor(MinecraftServer server){var p=WestLandIntegration.visitor(server.overworld());server.getPlayerList().getPlayersByUUID().put(p.getUUID(),p);server.getPlayerList().getPlayers().add(p);return p;}
 static void cleanup(ServerPlayer p){if(p==null)return;PetCompanions.recall(p);LeisureShop.SESSIONS.remove(p.getUUID());var list=p.level().getServer().getPlayerList();list.getPlayersByUUID().remove(p.getUUID());list.getPlayers().remove(p);p.level().removePlayerImmediately(p,Entity.RemovalReason.DISCARDED);}
 static MarketLedger.Account account(ServerPlayer p){return StockMarket.ledger.account(p.getStringUUID());}
 static void empty(ServerPlayer p){for(int i=0;i<36;i++)p.getInventory().setItem(i,ItemStack.EMPTY);}
 static int open(ServerPlayer p,int kind){var at=kind==0?ExistingEateries.ALL.getFirst().staff():LeisureSites.at(kind);check(at!=null,"site ready "+kind);p.setPos(at.getX()+.5,at.getY(),at.getZ()-2.5);check(LeisureShop.open(p,kind,false)==1,"open service "+kind);return LeisureShop.SESSIONS.get(p.getUUID()).token();}
 static int count(ServerPlayer p){int n=0;for(int i=0;i<36;i++)n+=p.getInventory().getItem(i).getCount();return n;}
 static Map<BlockPos,net.minecraft.world.level.block.state.BlockState> decorSnapshot(ServerLevel l,BlockPos staff){
  var out=new HashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();for(var at:BlockPos.betweenClosed(staff.offset(-6,-1,-18),staff.offset(6,5,18)))out.put(at.immutable(),l.getBlockState(at));return out;
 }
 static void decorChecks(ServerPlayer p)throws Exception{
  var l=p.level();var site=ExistingEateries.ALL.stream().filter(s->s.arrival().x()==s.staff().getX()&&(s.yaw()==0||s.yaw()==180)&&Math.abs(s.arrival().z()-s.staff().getZ())>=5&&Math.abs(s.arrival().z()-s.staff().getZ())<=16).findFirst().orElseThrow();
  var home=site.staff();var facing=site.yaw()==0?net.minecraft.core.Direction.SOUTH:net.minecraft.core.Direction.NORTH;
  for(int x=home.getX()-16;x<=home.getX()+16;x+=8)for(int z=home.getZ()-20;z<=home.getZ()+20;z+=8)l.getChunk(x>>4,z>>4);
  for(var at:BlockPos.betweenClosed(home.offset(-6,-1,-18),home.offset(6,5,18))){l.getChunkAt(at);l.setBlock(at,(at.getY()==64?Blocks.DARK_OAK_PLANKS:Blocks.AIR).defaultBlockState(),2);}
  var wall=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.fromNamespaceAndPath("neonward","patched_brick"));check(wall!=Blocks.AIR,"original wall material registered");
  var counter=NeonFurniture.BLOCKS.get("neon_counter");check(counter!=null,"original counter registered");
  for(int dx=-4;dx<=4;dx++){
   for(int y=65;y<=69;y++)l.setBlock(new BlockPos(home.getX()+dx,y,home.getZ()-facing.getStepZ()),wall.defaultBlockState(),2);
   var at=home.relative(facing).offset(dx,0,0);l.setBlock(at,counter.defaultBlockState(),2);
   if((at.getX()&1)==0)l.setBlock(at.above(),Blocks.FLOWER_POT.defaultBlockState(),2);
  }
  var chest=home.relative(facing).offset(4,1,0);l.setBlock(chest,Blocks.CHEST.defaultBlockState(),2);
  ((net.minecraft.world.level.block.entity.ChestBlockEntity)l.getBlockEntity(chest)).setItem(0,new ItemStack(Items.DIAMOND,7));
  var door=new BlockPos(home.getX(),65,site.arrival().z());var doorState=AutoDoors.DOOR.defaultBlockState().setValue(net.minecraft.world.level.block.DoorBlock.FACING,facing);
  l.setBlock(door,doorState.setValue(net.minecraft.world.level.block.DoorBlock.HALF,net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER),2);
  l.setBlock(door.above(),doorState.setValue(net.minecraft.world.level.block.DoorBlock.HALF,net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER),2);
  l.setBlock(door.above(2),net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("black_concrete")).defaultBlockState(),2);
  var original=decorSnapshot(l,home);var plan=EateryDecor.plan(l,site);check(plan!=null&&plan.blocks().size()==4,"original restaurant geometry yields bounded four-block decor plan");
  check(plan.sign().getX()!=home.getX()&&plan.blocks().keySet().stream().allMatch(at->at.getX()!=home.getX()),"all decor stays off staff center interaction column");
  check(original.equals(decorSnapshot(l,home)),"decor dry run is read-only");
  var banner=plan.blocks().keySet().stream().filter(at->plan.blocks().get(at).getBlock() instanceof net.minecraft.world.level.block.WallBannerBlock).findFirst().orElseThrow();
  l.setBlock(banner,Blocks.GOLD_BLOCK.defaultBlockState(),2);var blocked=decorSnapshot(l,home);
  check(EateryDecor.plan(l,site)==null,"occupied accent target rejects whole site");EateryDecor.start(l.getServer());check(blocked.equals(decorSnapshot(l,home)),"occupied decor attempt preserves all existing terrain");
  l.setBlock(banner,Blocks.AIR.defaultBlockState(),2);int entities=l.getEntities(null,new AABB(home).inflate(20)).size();EateryDecor.start(l.getServer());
  for(var entry:original.entrySet())if(!plan.blocks().containsKey(entry.getKey()))check(l.getBlockState(entry.getKey()).equals(entry.getValue()),"decor preserves original cell "+entry.getKey());
  for(var entry:plan.blocks().entrySet())check(l.getBlockState(entry.getKey()).equals(entry.getValue()),"decor applies planned accent only");
  check(((net.minecraft.world.level.block.entity.ChestBlockEntity)l.getBlockEntity(chest)).getItem(0).is(Items.DIAMOND)&&((net.minecraft.world.level.block.entity.ChestBlockEntity)l.getBlockEntity(chest)).getItem(0).getCount()==7,"player container contents preserved");
  check(l.getEntities(null,new AABB(home).inflate(20)).size()==entities,"decor creates no entities");
  var sign=(net.minecraft.world.level.block.entity.SignBlockEntity)l.getBlockEntity(plan.sign());check(sign!=null&&sign.isWaxed()&&sign.getFrontText().getMessage(0,false).getString().equals(site.name().replaceFirst(" \\[\\d+\\]$","")),"waxed named menu sign");
  var after=decorSnapshot(l,home);var file=l.getServer().getWorldPath(LevelResource.ROOT).resolve("neonward/eatery_decor.json");String saved=Files.readString(file);EateryDecor.start(l.getServer());
  check(after.equals(decorSnapshot(l,home))&&saved.equals(Files.readString(file)),"decor restart idempotent including durable marker");
  l.setBlock(plan.sign(),Blocks.AIR.defaultBlockState(),2);l.setBlock(banner,Blocks.DIAMOND_BLOCK.defaultBlockState(),2);var edited=decorSnapshot(l,home);EateryDecor.start(l.getServer());
  check(edited.equals(decorSnapshot(l,home)),"player removals and replacements never regenerated");
  System.out.println("LEISURE_EATERY_DECOR_PASS: original geometry, read-only plan, occupied-site preservation, four accents, container preservation, no entities, restart idempotence, player edits preserved");
 }
 static CityResident staff(ServerLevel level,ExistingEateries.Site site){
  level.getChunkAt(site.staff());level.setChunkForced(site.staff().getX()>>4,site.staff().getZ()>>4,true);
  for(int dx=-2;dx<=2;dx++)for(int dz=-3;dz<=2;dz++){
   var at=site.staff().offset(dx,0,dz);level.setBlock(at.below(),Blocks.SMOOTH_STONE.defaultBlockState(),2);level.setBlock(at,Blocks.AIR.defaultBlockState(),2);level.setBlock(at.above(),Blocks.AIR.defaultBlockState(),2);
  }
  if(level.getEntity(site.uuid()) instanceof CityResident existing)return existing;
  var row=CityResidents.plan.asList().stream().map(v->v.getAsJsonObject()).filter(r->r.get("key").getAsString().equals("bar_staff_"+site.id())).findFirst().orElseThrow();
  var npc=new CityResident(CityResidents.TYPES.get(row.get("role").getAsInt()),level);npc.setUUID(site.uuid());npc.home=site.staff();npc.shopStaff=true;npc.job="bar";npc.setPos(Vec3.atBottomCenterOf(site.staff()));npc.setYRot(site.yaw());
  check(level.addFreshEntity(npc),"original staff UUID spawned "+site.id());return npc;
 }
 static void foodChecks(ServerPlayer p,ServerPlayer other){
  check(ExistingEateries.ALL.size()==50,"all 50 existing numbered restaurant staff");check(LeisureSites.at(0)==null,"no physical food kiosk");
  check(LeisureSites.layout(new BlockPos(310,65,620),0).isEmpty(),"food kiosk has no construction layout");
  Set<UUID> ids=new HashSet<>();
  for(var site:ExistingEateries.ALL){
   check(ids.add(site.uuid()),"unique original vendor UUID "+site.id());var npc=staff(p.level(),site);
   p.setPos(Vec3.atBottomCenterOf(site.staff()).add(0,0,-2));npc.tick();
   check(npc.home.equals(site.staff())&&npc.position().equals(Vec3.atBottomCenterOf(site.staff()))&&npc.isNoAi()&&npc.isPersistenceRequired(),"existing staff anchored "+site.id());
   var nearest=ExistingEateries.nearest(p);
   check(ExistingEateries.near(p,site.id())&&nearest!=null&&nearest.id()==site.id(),"nearest authenticated vendor "+site.id()+" tracked="+(p.level().getEntity(site.uuid())==npc)+" ticking="+p.level().isPositionEntityTicking(site.staff())+" alive="+npc.isAlive()+" staff="+npc.shopStaff+" distance="+p.distanceToSqr(Vec3.atBottomCenterOf(site.staff()))+" nearest="+(nearest==null?"none":nearest.id()));
   LeisureShop.SESSIONS.remove(p.getUUID());npc.mobInteract(p,InteractionHand.MAIN_HAND);
   var session=LeisureShop.SESSIONS.get(p.getUUID());check(session!=null&&session.kind()==0&&session.eatery()==site.id(),"NPC interaction binds exact restaurant "+site.id());
   var rows=LeisureShop.offers(p,0,LeisureShop.day(p));check(rows.size()==site.meals().length,"restaurant-specific menu count");
   for(int i=0;i<rows.size();i++){int meal=site.meals()[i];check(rows.get(i).stack().is(StreetMeals.ITEMS[meal])&&rows.get(i).price()==StreetMeals.PRICES[meal],"exact meal/price for "+site.name());}
  }
  var a=ExistingEateries.ALL.getFirst();var b=ExistingEateries.ALL.stream().filter(s->s.staff().distSqr(a.staff())>144&&!Arrays.equals(s.meals(),a.meals())).findFirst().orElseThrow();
  empty(p);account(p).cash=100000;int token=open(p,0);var quote=LeisureShop.SESSIONS.get(p.getUUID());long cash=account(p).cash;
  p.setPos(Vec3.atBottomCenterOf(b.staff()).add(0,0,-2));check(ExistingEateries.near(p,b.id())&&!ExistingEateries.near(p,a.id()),"move to a different real vendor");
  check(LeisureShop.request(p,"buy",token,0)==0&&account(p).cash==cash&&count(p)==0,"vendor A token rejected at vendor B");
  check(LeisureShop.offers(p,0,0).getFirst().stack().is(StreetMeals.ITEMS[a.meals()[0]]),"bound quote retains A menu while standing at B");
  check(LeisureShop.open(p,0,false)==1,"nearest resolves vendor B");var next=LeisureShop.SESSIONS.get(p.getUUID());check(next.eatery()==b.id()&&next.token()!=token,"opening B replaces A quote");
  check(LeisureShop.request(p,"buy",token,0)==0,"stale A token rejected after switching");
  check(LeisureShop.request(other,"buy",next.token(),0)==0,"food token cannot transfer to another player");
  check(LeisureShop.request(p,"buy",next.token(),0)==1&&account(p).cash==cash-StreetMeals.PRICES[b.meals()[0]],"B menu purchase exact debit");
  check(LeisureShop.SESSIONS.get(p.getUUID()).eatery()==b.id(),"token rotation retains exact vendor");
  check(LeisureShop.request(p,"buy",next.token(),0)==0&&count(p)==1,"food replay rejected");
  var npc=(CityResident)p.level().getEntity(b.uuid());LeisureShop.SESSIONS.remove(p.getUUID());
  var fake=new CityResident(CityResidents.TYPES.get(5),p.level());fake.home=b.staff();fake.shopStaff=true;fake.setPos(npc.position());fake.setCustomName(npc.getName());
  check(!ExistingEateries.use(p,fake)&&!LeisureShop.SESSIONS.containsKey(p.getUUID()),"lookalike vendor without original UUID rejected");fake.discard();
  npc.shopStaff=false;check(!ExistingEateries.near(p,b.id()),"original UUID without staff flag rejected");npc.shopStaff=true;
  npc.home=b.staff().east();check(!ExistingEateries.near(p,b.id()),"wrong staff home rejected");npc.home=b.staff();
  npc.setPos(npc.position().add(2,0,0));check(!ExistingEateries.near(p,b.id()),"displaced staff rejected");npc.setPos(Vec3.atBottomCenterOf(b.staff()));
  check(LeisureShop.open(p,0,true)==0,"food cannot open remotely");
  check(new LeisureShop.Session(0,1,1,1,false).eatery()==-1,"legacy constructor cannot impersonate an eatery");
  LeisureShop.SESSIONS.put(p.getUUID(),new LeisureShop.Session(0,1,p.level().getGameTime()+100,0,false));cash=account(p).cash;
  check(LeisureShop.request(p,"buy",1,0)==0&&account(p).cash==cash,"unbound legacy food session rejected");
  LeisureShop.SESSIONS.remove(p.getUUID());p.setPos(3050.5,65,8.5);check(LeisureShop.open(p,0,false)==0,"no nearby staff denies opening");
  check(LeisureShop.offers(p,0,0).getFirst().stack().is(StreetMeals.ITEMS[ExistingEateries.ALL.getFirst().meals()[0]]),"preview defaults to first existing eatery");
  System.out.println("LEISURE_EXISTING_EATERIES_PASS: 50 original vendors, exact menus/authentication, vendor-bound quotes, replay, no food kiosk");
 }
 static void shopChecks(ServerPlayer p,ServerPlayer other)throws Exception{
  for(int kind:new int[]{0,2,4}){
   account(p).cash=1000000;empty(p);int token=open(p,kind);long price=LeisureShop.offers(p,kind,LeisureShop.day(p)).getFirst().price();
   check(LeisureShop.request(other,"buy",token,0)==0,"quote bound to player");
   check(LeisureShop.request(p,"buy",token,0)==1&&account(p).cash==1000000-price&&count(p)==1,"exact item debit/delivery "+kind);
   check(LeisureShop.request(p,"buy",token,0)==0&&count(p)==1,"double buy rejected "+kind);
   token=open(p,kind);long cash=account(p).cash;p.setPos(p.getX()+30,p.getY(),p.getZ());
   check(LeisureShop.request(p,"buy",token,0)==0&&account(p).cash==cash,"distance checked on confirm");
   token=open(p,kind);for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));
   check(LeisureShop.request(p,"buy",token,0)==0&&account(p).cash==cash&&count(p)==2304,"full inventory no debit");
   empty(p);token=open(p,kind);Path file=StockMarket.file;String before=StockMarket.JSON.toJson(account(p));
   try{StockMarket.file=file.getParent();check(LeisureShop.request(p,"buy",token,0)==0,"injected save failure");}
   finally{StockMarket.file=file;}
   check(before.equals(StockMarket.JSON.toJson(account(p)))&&count(p)==0,"save rollback cash and inventory");
   token=open(p,kind);var session=LeisureShop.SESSIONS.get(p.getUUID());LeisureShop.SESSIONS.put(p.getUUID(),new LeisureShop.Session(kind,token,p.level().getGameTime()-1,session.day(),false,session.eatery()));
   check(LeisureShop.request(p,"buy",token,0)==0,"expired quote rejected");
   if(kind==0)continue; // All fifty original food NPCs are covered by foodChecks.
   open(p,kind);var npc=new CityResident(CityResidents.TYPES.get(2),p.level());npc.job="leisure_"+kind;npc.home=LeisureSites.at(kind).south();npc.setPos(Vec3.atBottomCenterOf(npc.home));npc.tick();
   check(npc.position().equals(Vec3.atBottomCenterOf(npc.home))&&npc.isNoAi()&&npc.isPersistenceRequired(),"service NPC stationary and persistent");
   npc.mobInteract(p,InteractionHand.MAIN_HAND);check(LeisureShop.SESSIONS.get(p.getUUID()).kind()==kind,"NPC routes own shop");npc.discard();
  }
  int token=open(p,2);var session=LeisureShop.SESSIONS.get(p.getUUID());long cash=account(p).cash;
  LeisureShop.SESSIONS.put(p.getUUID(),new LeisureShop.Session(2,token,session.expires(),session.day()-1,false));
  check(LeisureShop.request(p,"buy",token,0)==0&&cash==account(p).cash&&LeisureShop.SESSIONS.get(p.getUUID()).token()!=token,"daily rotation invalidates stale quote without charge");
  var today=LeisureShop.offers(p,2,0);var again=LeisureShop.offers(p,2,0);check(today.size()==5,"black market catalog");
  for(int i=0;i<5;i++)check(ItemStack.matches(today.get(i).stack(),again.get(i).stack()),"same-day offers deterministic");
  boolean changed=false;for(int day=1;day<=7;day++)if(!ItemStack.matches(today.getFirst().stack(),LeisureShop.offers(p,2,day).getFirst().stack()))changed=true;
  check(changed,"daily assortment rotates");
  check(StreetMeals.ITEMS.length==18&&StreetMeals.NAMES.length==18&&StreetMeals.EFFECT_KINDS.length==18,"complete eighteen-item catalog");
  int[] alcoholPrices={180,260,220,240,280,320};int[] durations={6000,1200,3600,12000};
  for(int i=0;i<StreetMeals.ITEMS.length;i++){
   p.removeAllEffects();var item=new ItemStack(StreetMeals.ITEMS[i]);boolean drink=i==3||i==9||i==10||i>=12;
   check(StreetMeals.drink(i)==drink,"food/drink classification "+i);
   check(Objects.equals(item.get(net.minecraft.core.component.DataComponents.CONSUMABLE),drink?net.minecraft.world.item.component.Consumables.DEFAULT_DRINK:net.minecraft.world.item.component.Consumables.DEFAULT_FOOD),"exact vanilla drink/eat consumable "+i);
   if(i>=12)check(StreetMeals.PRICES[i]==alcoholPrices[i-12],"stable alcohol price "+i);
   int family=StreetMeals.EFFECT_KINDS[i];check(family>=0&&family<durations.length,"bounded effect family "+i);
   var effect=switch(family){case 0->net.minecraft.world.effect.MobEffects.SPEED;case 1->net.minecraft.world.effect.MobEffects.REGENERATION;case 2->net.minecraft.world.effect.MobEffects.RESISTANCE;default->StreetMeals.FOCUS;};
   int n=item.getCount();StreetMeals.ITEMS[i].finishUsingItem(item,p.level(),p);check(item.getCount()==n-1,"exactly one consumable used "+i);
   var applied=p.getEffect(effect);check(p.getActiveEffects().size()==1&&applied!=null&&applied.getDuration()==durations[family]&&applied.getAmplifier()==0,"exact single bounded effect "+i);
   StreetMeals.ITEMS[i].finishUsingItem(new ItemStack(StreetMeals.ITEMS[i]),p.level(),p);applied=p.getEffect(effect);
   check(p.getActiveEffects().size()==1&&applied.getDuration()==durations[family]&&applied.getAmplifier()==0,"repeat refreshes without duration/amplifier stacking "+i);
  }
  p.removeAllEffects();System.out.println("LEISURE_SHOPS_PASS");
 }
 static PetEntity pet(ServerPlayer p){return p.level().getEntitiesOfClass(PetEntity.class,p.getBoundingBox().inflate(20)).stream().filter(e->p.getUUID().equals(e.owner)&&!e.isRemoved()).findFirst().orElseThrow();}
 static void petChecks(ServerPlayer p,ServerPlayer other)throws Exception{
  account(p).cash=100000;account(p).petOwned.clear();int token=open(p,1);long cash=account(p).cash;
  check(LeisureShop.request(p,"buy",token,0)==1&&account(p).cash==cash-PetCompanions.price(0),"pet shop purchase");
  check(LeisureShop.request(p,"buy",token,0)==0,"pet replay rejected");cash=account(p).cash;check(PetCompanions.purchase(p,0)==2&&account(p).cash==cash,"duplicate pet no charge");
  Path save=StockMarket.file;try{StockMarket.file=save.getParent();check(PetCompanions.purchase(p,1)==-4&&!PetCompanions.owned(p,1)&&account(p).cash==cash,"pet ownership+cash rollback");}finally{StockMarket.file=save;}
  for(int kind=1;kind<5;kind++)check(PetCompanions.purchase(p,kind)==1,"purchase catalog "+kind);
  StockMarket.save();var reopened=StockMarket.JSON.fromJson(Files.readString(save),MarketLedger.class);check(reopened.account(p.getStringUUID()).petOwned.size()==5,"pet ownership persisted");
  p.setPos(190.5,65,154.5);other.setPos(199.5,65,154.5);check(PetCompanions.summon(other,0)==-5,"other owner denied");
  for(int kind=0;kind<5;kind++){check(PetCompanions.summon(p,kind)==1,"safe summon "+kind);var e=pet(p);check(e.kind()==kind&&!e.shouldBeSaved()&&!e.canAttack(other)&&e.isIgnoringBlockTriggers(),"native passive ephemeral pet");check(p.level().getEntitiesOfClass(PetEntity.class,p.getBoundingBox().inflate(20)).stream().filter(v->p.getUUID().equals(v.owner)&&!v.isRemoved()).count()==1,"one active pet across kinds");}
  PetCompanions.summon(p,3);var crow=pet(p);check(crow.flight.begin(p,crow),"crow finds bounded flight");double ground=crow.getY(),maxY=ground;int ticks=0;
  while(crow.flight.active()&&ticks++<80){crow.flight.tick(p,crow);maxY=Math.max(maxY,crow.getY());check(p.level().noCollision(crow,crow.getBoundingBox()),"flight collision-free");}
  check(!crow.flight.active()&&!crow.flying()&&maxY>ground+.8&&PetSpawn.safe(p.level(),crow,crow.position()),"crow takes off and lands safely");
  var wall=BlockPos.containing(crow.position().add(0,1,0));p.level().setBlock(wall,Blocks.STONE.defaultBlockState(),2);
  check(!PetFlight.clear(p.level(),crow,crow.position(),crow.position().add(0,1.4,0)),"flight rejects blocked sweep");p.level().setBlock(wall,Blocks.AIR.defaultBlockState(),2);
  PetCompanions.recall(p);check(crow.isRemoved()&&PetCompanions.activeKind(p)==-1,"recall cleanup");
  PetCompanions.summon(p,0);var dog=pet(p);ServerPlayConnectionEvents.DISCONNECT.invoker().onPlayDisconnect(p.connection,p.level().getServer());check(dog.isRemoved()&&PetCompanions.activeKind(p)==-1,"logout event cleanup");
  check(PetCompanions.summon(p,0)==1,"resummon after logout preserves ownership");dog=pet(p);
  var otherLevel=p.level().getServer().getLevel(CompactShops.DIM);check(otherLevel!=null,"dimension fixture");
  check(p.teleportTo(otherLevel,4000.5,65,0.5,Set.of(),0,0,true),"dimension travel fixture");check(dog.isRemoved()&&PetCompanions.activeKind(p)==-1,"dimension event cleanup");
  p.teleportTo(p.level().getServer().overworld(),190.5,65,154.5,Set.of(),0,0,true);
  PetCompanions.summon(p,0);dog=pet(p);ServerEntityEvents.ENTITY_UNLOAD.invoker().onUnload(dog,p.level());check(dog.isRemoved()&&PetCompanions.activeKind(p)==-1,"chunk unload cleanup");
  check(LeisureShop.open(p,1,true)==1,"remote owned-pet manager");token=LeisureShop.SESSIONS.get(p.getUUID()).token();cash=account(p).cash;check(LeisureShop.request(p,"buy",token,1)==0&&account(p).cash==cash,"remote manager cannot purchase");
  System.out.println("LEISURE_PETS_PASS");
 }
 static void displayChecks(ServerPlayer p,ServerPlayer other){
  var at=new BlockPos(194,65,154);p.level().setBlock(at,WeaponDisplay.BLOCK.defaultBlockState(),3);var display=(WeaponDisplayEntity)p.level().getBlockEntity(at);check(display!=null,"display block entity");display.assignOwner(p.getUUID());
  var weapon=RolledWeapons.create(NeonArsenal.DROPS.stream().filter(w->w!=NeonShield.ITEM).findFirst().orElseThrow(),new WeaponLoot.Quality(3,175));var copy=weapon.copy();
  check(!display.deposit(other.getUUID(),"visitor",weapon),"visitor cannot deposit");check(display.deposit(p.getUUID(),"owner",weapon)&&weapon.isEmpty(),"display transfers weapon");
  check(display.take(other.getUUID()).isEmpty()&&ItemStack.matches(display.displayedStack(),copy),"visitor cannot steal");
  var loaded=new WeaponDisplayEntity(at,WeaponDisplay.BLOCK.defaultBlockState());loaded.loadCustomOnly(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,p.level().registryAccess(),display.getUpdateTag(p.level().registryAccess())));
  check(loaded.ownedBy(p.getUUID())&&ItemStack.matches(loaded.displayedStack(),copy),"display component persistence");check(ItemStack.matches(display.take(p.getUUID()),copy)&&display.take(p.getUUID()).isEmpty(),"display withdrawal exactly once");System.out.println("LEISURE_DISPLAY_PASS");
 }
 static void routeChecks(ServerPlayer p)throws Exception{
  ((net.fabricmc.fabric.api.attachment.v1.AttachmentTarget)p).setAttached(PhoneEquipment.SLOT,new ItemStack(NeonWard.PHONE));
  for(int kind=0;kind<5;kind++){
   var dest=LeisureSites.destination(kind);check(dest!=null,"FT site destination");PhoneTravel.NEXT.remove(p.getUUID());
   if(kind==0)check(dest.equals(ExistingEateries.destination())&&dest.name().startsWith("焼鳥")&&LeisureSites.at(0)==null,"FT17 uses original grilled-skewer restaurant without kiosk");
   check(PhoneTravel.travel(p,17+kind)==1&&p.level().dimension()==net.minecraft.world.level.Level.OVERWORLD&&Math.hypot(p.getX()-dest.x(),p.getZ()-dest.z())<18,"service FT "+kind);
  }
  TrainingIntegration.run(p);System.out.println("LEISURE_ROUTES_RANGE_PASS");
 }
 static void run(ServerPlayer p,ServerPlayer other)throws Exception{
  check(StockMarket.ledger!=null,"market loaded");other.setPos(205.5,65,154.5);
  check(LeisureSites.at(0)==null,"no initial food kiosk");var anchors=new BlockPos[5];for(int i=1;i<5;i++){anchors[i]=LeisureSites.at(i);check(anchors[i]!=null,"initial fixed site "+i);}
  LeisureSites.start(p.level().getServer());
  check(LeisureSites.at(0)==null,"saved anchors do not recreate disabled food kiosk");for(int i=1;i<5;i++)check(anchors[i].equals(LeisureSites.at(i)),"saved site never relocates "+i);
  decorChecks(p);foodChecks(p,other);shopChecks(p,other);petChecks(p,other);displayChecks(p,other);routeChecks(p);
 }
}
