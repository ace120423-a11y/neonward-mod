package jp.neonward;

import java.util.*;
import com.google.gson.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/** --leisure only: 18 scene captures plus four-item catalog pages in the runner's copied world. Never auto-launched.
 * UI quotes are synthetic visual fixtures; independent LeisureIntegration covers transactions.
 * Pets, display block entity and range targets use real registered server entities/renderers.
 * PetRenderer compatibility (including Sodium) remains owned by the pet implementation agent.
 */
public final class LeisureQA implements ClientModInitializer {
 static final int ICON_START=18,ICON_PAGE_SIZE=4;
 static final String[] CAPTURES=captures();
 static String[] captures(){
  var names=new ArrayList<>(List.of("01-food-menu","02-pet-shop-menu","03-black-market-menu","04-training-menu","05-display-shop-menu","06-phone-pets","07-pet-dog","08-pet-cat","09-pet-snake","10-pet-crow","11-pet-robot","12-weapon-display","13-display-details","14-training-overview","15-training-hit-dps","16-training-scope","17-existing-eatery-exterior","18-existing-eatery-counter"));
  for(int first=0;first<StreetMeals.NAMES.length;first+=ICON_PAGE_SIZE)names.add(String.format(Locale.ROOT,"%02d-food-icons-%02d-%02d",names.size()+1,first+1,Math.min(first+ICON_PAGE_SIZE,StreetMeals.NAMES.length)));
  return names.toArray(String[]::new);
 }
 final BitSet capturedIcons=new BitSet();
 volatile ExistingEateries.Site visualEatery;volatile BlockPos visualSign;
 int warmup,stage,age,shots,wait;boolean pending,finished;volatile boolean ready;volatile Throwable failure;
 LeisureShopScreen expectedMenu;
 String expectedMenuTitle;int expectedMenuRows;
 static final BlockPos DISPLAY=new BlockPos(3050,65,11);
 void check(boolean condition,String reason){if(!condition)throw new AssertionError("LEISURE_QA: "+reason);}
 static String read(java.nio.file.Path file){try{return java.nio.file.Files.readString(file);}catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}}
 void server(Minecraft mc,Runnable work){mc.getSingleplayerServer().execute(()->{try{work.run();}catch(Throwable e){failure=e;}});}
 ServerPlayer player(Minecraft mc){return mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();}
 PetEntity trackPet(Minecraft mc,float partial){
  if(finished||!ready||stage<6||stage>10||mc.player==null||mc.level==null)return null;
  var pet=mc.level.getEntitiesOfClass(PetEntity.class,mc.player.getBoundingBox().inflate(24),e->e.kind()==stage-6&&mc.player.getUUID().equals(e.owner)).stream().findFirst().orElse(null);
  // owner is server-only on some pet versions; each QA stage has exactly one pet
  // of this registered type, so the synchronized type is the stable fallback.
  if(pet==null)pet=mc.level.getEntitiesOfClass(PetEntity.class,mc.player.getBoundingBox().inflate(24),e->e.kind()==stage-6).stream().findFirst().orElse(null);
  if(pet==null)return null;
  var center=pet.getPosition(partial).add(0,pet.getBbHeight()*.5,0);
  var delta=center.subtract(mc.player.getEyePosition(partial));
  float yaw=(float)Math.toDegrees(Math.atan2(-delta.x,delta.z));
  float pitch=(float)-Math.toDegrees(Math.atan2(delta.y,Math.hypot(delta.x,delta.z)));
  mc.player.setYRot(yaw);mc.player.setXRot(pitch);mc.player.yRotO=yaw;mc.player.xRotO=pitch;
  return pet;
 }
 JsonObject quote(ServerPlayer p,int kind,boolean remote){
  var data=new JsonObject();data.addProperty("kind",kind);data.addProperty("token",12345);data.addProperty("title",remote?"PHONE / ペット呼び出し・帰還":kind==0?LeisureShop.foodSite(p).name():LeisureShop.NAMES[kind]);
  data.addProperty("cash",200000);data.addProperty("message",remote?"購入済みペットを呼び出せます / 未購入は店舗で購入":"価格と内容を確認して選択してください");
  var rows=new JsonArray();int index=0;
  for(var offer:LeisureShop.offers(p,kind,0)){
   var row=new JsonObject();boolean owned=kind==1&&index<2;
   row.addProperty("name",offer.name());row.addProperty("effect",owned?"購入済み / 連れ歩きは1人1匹":kind==1?"永久購入 / 呼び出し・帰還可能":offer.effect());row.addProperty("price",offer.price());
   row.addProperty("action",kind==3?"enter":owned?"summon":"buy");row.addProperty("enabled",!remote||owned);rows.add(row);index++;
  }
  data.add("rows",rows);return data;
 }
 void prepare(Minecraft mc,int index){
  expectedMenu=null;mc.options.keyUse.setDown(false);mc.gui.setScreen(null);mc.player.stopUsingItem();
  server(mc,()->{
   var p=player(mc);var l=mc.getSingleplayerServer().overworld();p.stopUsingItem();p.setGameMode(GameType.CREATIVE);PetCompanions.recall(p);
   check(StockMarket.ledger!=null,"QA ledger loaded");StockMarket.ledger.account(p.getStringUUID()).tutorialDone=true;WelcomeTutorial.pending.remove(p.getUUID());
   if(index>=16){
    if(TrainingRange.contains(p.level(),p.blockPosition()))check(TrainingRange.leave(p),"range exit before existing terrain QA");
    p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
    if(index==16){
     visualEatery=ExistingEateries.ALL.stream().filter(s->s.name().startsWith("焼鳥")).findFirst().orElseThrow();var home=visualEatery.staff();
     // Load the copied original terrain only; never manufacture a restaurant for visual QA.
     for(int x=home.getX()-16;x<=home.getX()+16;x+=8)for(int z=home.getZ()-18;z<=home.getZ()+18;z+=8)l.getChunk(x>>4,z>>4);
     EateryDecor.start(l.getServer());
     var file=l.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("neonward/eatery_decor.json");
     var records=JsonParser.parseString(read(file)).getAsJsonObject().getAsJsonObject("sites");
     check(records.has(Integer.toString(visualEatery.id()))&&records.getAsJsonObject(Integer.toString(visualEatery.id())).get("status").getAsString().equals("installed"),"actual restaurant decor installed");
     visualSign=null;
     for(var at:BlockPos.betweenClosed(home.offset(-8,1,-1),home.offset(8,1,1)))if(l.getBlockEntity(at) instanceof net.minecraft.world.level.block.entity.SignBlockEntity sign&&sign.isWaxed())visualSign=at.immutable();
     check(visualSign!=null,"actual decorated menu sign");
    }
    if(index<18){
     var home=visualEatery.staff();int direction=visualEatery.yaw()==0?1:-1;
     double x=home.getX()+.5,z=index==16?visualEatery.arrival().z()+.5+direction*5:home.getZ()+.5+direction*4;
     check(p.teleportTo(l,x,65,z,Set.of(),direction==1?180:0,index==16?-8:8,true),"existing restaurant camera");
     if(index==17){
      check(visualSign.getX()!=home.getX(),"menu sign does not cover staff face");
      var ray=l.clip(new net.minecraft.world.level.ClipContext(p.getEyePosition(),net.minecraft.world.phys.Vec3.atBottomCenterOf(home).add(0,1.62,0),net.minecraft.world.level.ClipContext.Block.OUTLINE,net.minecraft.world.level.ClipContext.Fluid.NONE,p));
      check(ray.getType()==net.minecraft.world.phys.HitResult.Type.MISS,"center staff interaction ray unobstructed by terrain/decor");
     }
     check(l.noCollision(p),"actual terrain camera clear");ready=true;return;
    }
    var data=quote(p,0,false);data.addProperty("title","FOOD ICON QA / "+(index-ICON_START+1)+" / "+(CAPTURES.length-ICON_START));var rows=new JsonArray();
    int first=(index-ICON_START)*ICON_PAGE_SIZE,end=Math.min(first+ICON_PAGE_SIZE,StreetMeals.NAMES.length);
    for(int meal=first;meal<end;meal++){var row=new JsonObject();row.addProperty("name",StreetMeals.NAMES[meal]);row.addProperty("effect",StreetMeals.EFFECTS[meal]);row.addProperty("price",StreetMeals.PRICES[meal]);row.addProperty("action","buy");row.addProperty("enabled",true);rows.add(row);}
    data.add("rows",rows);mc.execute(()->{try{var screen=new LeisureShopScreen();mc.gui.setScreen(screen);screen.receive(data);expectedMenu=screen;expectedMenuTitle=data.get("title").getAsString();expectedMenuRows=end-first;ready=true;}catch(Throwable e){failure=e;}});return;
   }
   if(index==0){
    check(StreetMeals.NAMES.length==18&&StreetMeals.ITEMS.length==18&&StreetMeals.PRICES.length==18,"final eighteen-item food/drink catalog present");
    for(int x=3044;x<=3056;x++)for(int z=3;z<=17;z++)for(int y=64;y<=70;y++)l.setBlock(new BlockPos(x,y,z),(y==64?Blocks.SEA_LANTERN:Blocks.AIR).defaultBlockState(),2);
    for(int i=0;i<36;i++)p.getInventory().setItem(i,ItemStack.EMPTY);p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);
   }
   if(index<13){check(p.teleportTo(l,3050.5,65,8.5,Set.of(),0,20,true),"gallery teleport");p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);}
   if(index<6){
    if(index==0){
     check(LeisureSites.at(0)==null,"food has no kiosk");var site=ExistingEateries.ALL.stream().filter(s->s.name().startsWith("焼鳥")).findFirst().orElseThrow();
     // Menu fixture uses an exact existing vendor; server integration tests real NPC authorization.
     LeisureShop.SESSIONS.put(p.getUUID(),new LeisureShop.Session(0,12345,p.level().getGameTime()+1200,0,false,site.id()));
    }
    var data=quote(p,index==5?1:index,index==5);
    mc.execute(()->{try{var screen=new LeisureShopScreen();mc.gui.setScreen(screen);screen.receive(data);check(screen.kind==(index==5?1:index),"service screen kind");expectedMenu=screen;expectedMenuTitle=data.get("title").getAsString();expectedMenuRows=data.getAsJsonArray("rows").size();ready=true;}catch(Throwable e){failure=e;}});return;
   }
   if(index<=10){
    int kind=index-6;var account=StockMarket.ledger.account(p.getStringUUID());if(account.petOwned==null)account.petOwned=new HashSet<>();account.petOwned.add(kind);
    check(PetCompanions.summon(p,kind)==PetCompanions.OK,"real pet summon "+kind);
    var pet=l.getEntitiesOfClass(PetEntity.class,p.getBoundingBox().inflate(8),e->p.getUUID().equals(e.owner)).getFirst();
    pet.setPos(3050.5,65,10.6);pet.setNoAi(true);pet.setYRot(180);pet.setYHeadRot(180);pet.setYBodyRot(180);
   }else if(index==11){
    l.setBlock(DISPLAY,WeaponDisplay.BLOCK.defaultBlockState(),3);var display=(WeaponDisplayEntity)l.getBlockEntity(DISPLAY);check(display!=null,"display block entity");
    display.assignOwner(p.getUUID());var weapon=RolledWeapons.create(NeonArsenal.ITEMS.get("longwatch_sniper"),new WeaponLoot.Quality(3,175));
    GunAttachments.install(weapon,0,14);check(display.deposit(p.getUUID(),"Leisure QA",weapon),"display deposit");
   }else if(index==12){((WeaponDisplayEntity)l.getBlockEntity(DISPLAY)).interact(p,InteractionHand.MAIN_HAND);}
   else {
    if(index==13){
     // The copied city may not have generated pavement here. Only the isolated QA
     // copy is changed: a seven-square landing pad with two blocks of headroom.
     var arrival=TrainingRange.returnPoint();
     for(int x=arrival.x()-3;x<=arrival.x()+3;x++)for(int z=arrival.z()-3;z<=arrival.z()+3;z++){
      l.getChunk(x>>4,z>>4);l.setBlock(new BlockPos(x,64,z),Blocks.SMOOTH_STONE.defaultBlockState(),3);
      for(int y=65;y<=66;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),3);
     }
     check(p.teleportTo(l,arrival.x()+.5,65,arrival.z()+.5,Set.of(),0,0,true),"range street setup");check(TrainingRange.enter(p),"real range entry");
    }
    check(TrainingRange.contains(p.level(),p.blockPosition()),"range visit persists across shop ticks");
    check(p.teleportTo(p.level(),TrainingRange.X+8.5,65,index==13?27.5:index==15?24.5:6.5,Set.of(),-90,index==13?8:0,true),"range camera");
    if(index>=14){var item=index==15?NeonArsenal.ITEMS.get("longwatch_sniper"):NeonArsenal.RIFLE;check(item instanceof NeonArsenal.Rifle,"registered range QA gun");var gun=new ItemStack(item);if(index==15)GunAttachments.install(gun,0,14);p.setItemInHand(InteractionHand.MAIN_HAND,gun);}
   }
   p.getInventory().setChanged();p.containerMenu.broadcastChanges();ready=true;
  });
 }
 public void onInitializeClient(){
  System.out.println("LEISURE_CAPTURE_PLAN captures="+CAPTURES.length+" catalog="+StreetMeals.NAMES.length);
  ObjectiveHud.visible=false;
  // Extraction runs once per rendered world frame; refresh the view for the next
  // camera extraction as well as at ticks so moving/flying pets remain centered.
  net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents.END_EXTRACTION.register(ctx->
   trackPet(Minecraft.getInstance(),ctx.camera().getCameraEntityPartialTicks(ctx.deltaTracker())));
  // This test addon loads before its isolated integrated server starts. Cancel the
  // normal join+60-tick welcome popup before it can race with the first menu fixture.
  net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->{
   WelcomeTutorial.pending.remove(handler.player.getUUID());
   if(StockMarket.ledger!=null)StockMarket.ledger.account(handler.player.getStringUUID()).tutorialDone=true;
  });
  ClientTickEvents.END_CLIENT_TICK.register(mc->{
  if(finished||mc.player==null||mc.level==null||mc.getSingleplayerServer()==null)return;
  try{
   if(failure!=null)throw new AssertionError("Server/client setup failed",failure);
   ObjectiveHud.visible=false; // QA process only; do not alter the user's HUD settings file.
   if(++warmup<40){mc.gui.setScreen(null);return;}
   if(stage==CAPTURES.length){
    if(!pending){pending=true;ready=false;server(mc,()->{var p=player(mc);PetCompanions.recall(p);if(TrainingRange.contains(p.level(),p.blockPosition()))check(TrainingRange.leave(p),"final range exit");ready=true;});}
    if(!ready){check(++wait<300,"cleanup timeout");return;}
    check(shots==CAPTURES.length&&capturedIcons.cardinality()==StreetMeals.NAMES.length,"exact capture count and every catalog icon reviewed");finished=true;System.out.println("PAIRED_CLIENT_QA_COMPLETE LEISURE_CLIENT_PASS: "+shots+" captures; "+capturedIcons.cardinality()+" real catalog icons, existing decorated restaurant, pets/display/range");mc.stop();return;
   }
   if(!pending){pending=true;ready=false;age=wait=0;prepare(mc,stage);return;}
   if(!ready){check(++wait<600,"stage setup timeout "+stage);return;}
   age++;
   trackPet(mc,1);
   if(stage>=6&&stage<18)mc.gui.setScreen(null);
   if(stage!=12)mc.gui.hud.getChat().clearMessages(false);
   if(stage==15&&age==20){mc.options.keyUse.setDown(true);mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);}
   if(stage==14&&age==35)server(mc,()->{var p=player(mc);var gun=(NeonArsenal.Rifle)p.getMainHandItem().getItem();gun.fire(p.level(),p,InteractionHand.MAIN_HAND);check(TrainingRange.STATS.get(p.getUUID()).total()>0,"range gun hit and meter");});
   if(age==55){
    if(stage<6||stage>=18){
     check(expectedMenu!=null&&mc.gui.screen()==expectedMenu,"expected menu replaced before capture "+CAPTURES[stage]+" by "+(mc.gui.screen()==null?"null":mc.gui.screen().getClass().getSimpleName()));
     check(expectedMenu.kind==(stage>=18?0:stage==5?1:stage),"service kind at capture");
     check(expectedMenu.title.equals(expectedMenuTitle),"service title at capture");
     check(expectedMenu.rows.size()==expectedMenuRows&&!expectedMenu.pending,"menu rows loaded at capture");
     if(stage==0)check(expectedMenu.title.startsWith("焼鳥")&&expectedMenuRows==ExistingEateries.ALL.stream().filter(s->s.name().startsWith("焼鳥")).findFirst().orElseThrow().meals().length,"existing skewer restaurant title and current menu");
     if(stage>=ICON_START){check(expectedMenu.per>=expectedMenuRows,"all catalog page icons visible");for(int i=0;i<expectedMenuRows;i++){int meal=(stage-ICON_START)*ICON_PAGE_SIZE+i;check(expectedMenu.rows.get(i).getAsJsonObject().get("name").getAsString().equals(StreetMeals.NAMES[meal]),"exact food icon catalog key");check(!capturedIcons.get(meal),"catalog icon captured exactly once");capturedIcons.set(meal);}}
    }
    if(stage>=6&&stage<=10){var pet=trackPet(mc,1);check(pet!=null,"synchronized pet model");var sight=pet.position().add(0,pet.getBbHeight()*.5,0).subtract(mc.player.getEyePosition()).normalize();check(mc.player.getLookAngle().dot(sight)>.98,"pet centered at capture");}
    if(stage==11)check(mc.level.getBlockEntity(DISPLAY) instanceof WeaponDisplayEntity e&&!e.displayedStack().isEmpty(),"synchronized display weapon");
    if(stage>=13&&stage<16)check(TrainingRange.contains(mc.level,mc.player.blockPosition())&&!mc.level.getEntitiesOfClass(TrainingDummy.class,mc.player.getBoundingBox().inflate(120)).isEmpty(),"visible training targets");
    if(stage>=13&&stage<16)check(TrainingClient.last.startsWith("TRAINING  Hit ")&&TrainingClient.last.contains("DPS/5s"),"dedicated training HUD payload received");
    if(stage==16||stage==17)check(mc.level.getBlockEntity(visualSign) instanceof net.minecraft.world.level.block.entity.SignBlockEntity,"actual decorated restaurant synchronized");
    if(stage==15)check(GunControls.aiming()&&GunControls.zoom()<1,"real scope active");
    Screenshot.grab(mc,false);shots++;System.out.println("LEISURE_SCREENSHOT "+CAPTURES[stage]);
   }
   if(age==75){mc.options.keyUse.setDown(false);stage++;pending=false;ready=false;wait=0;}
  }catch(Throwable e){finished=true;e.printStackTrace();System.err.println("LEISURE_CLIENT_QA_FAILED stage="+stage);mc.options.keyUse.setDown(false);mc.stop();}
 });}
}
