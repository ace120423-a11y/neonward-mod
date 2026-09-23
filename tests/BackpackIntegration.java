package jp.neonward;

import java.nio.file.*;
import java.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.*;

/** Native menu tests, loaded only by the disposable-world runner. */
public final class BackpackIntegration implements ModInitializer {
 static int checks;boolean done;
 static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError("BACKPACK: "+why);}
 static void guard(MinecraftServer server){
  var expected=Path.of(System.getProperty("neonward.backpack.qa.root","__missing__")).toAbsolutePath().normalize();
  check(server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().equals(expected.resolve("world"))&&Files.isRegularFile(expected.resolve("BACKPACK_QA_ONLY")),"isolated fixture world guard");
 }
 static final class Probe extends ServerPlayer {
  boolean offline;
  Probe(ServerLevel l,com.mojang.authlib.GameProfile profile){super(l.getServer(),l,profile,ClientInformation.createDefault());}
  @Override public boolean hasDisconnected(){return offline;}
 }
 static Probe probe(ServerLevel level){
  var profile=new com.mojang.authlib.GameProfile(UUID.randomUUID(),"BackpackQA");var p=new Probe(level,profile);
  p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p,net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false)){
   @Override public boolean hasClientLoaded(){return true;}
  };
  p.setGameMode(GameType.SURVIVAL);p.setPos(0.5,65,0.5);return p;
 }
 static void empty(ServerPlayer p){for(int i=0;i<36;i++)p.getInventory().setItem(i,ItemStack.EMPTY);}
 static ItemStack named(Item item,int count){var stack=new ItemStack(item,count);stack.set(DataComponents.CUSTOM_NAME,Component.literal("Backpack QA components"));return stack;}
 static void same(ItemStack a,ItemStack b,String why){check(ItemStack.matches(a,b),why);}
 static int inventorySlot(ServerPlayer p,AbstractContainerMenu menu,int index){
  for(int i=0;i<menu.slots.size();i++)if(menu.slots.get(i).container==p.getInventory()&&menu.slots.get(i).getContainerSlot()==index)return i;
  throw new AssertionError("No native inventory slot "+index);
 }
 static ItemStack codecRoundtrip(ServerPlayer p,ItemStack stack){
  var ops=net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE,p.registryAccess());
  var encoded=ItemStack.CODEC.encodeStart(ops,stack).getOrThrow();
  var restored=ItemStack.CODEC.parse(ops,encoded).getOrThrow();same(stack,restored,"item codec preserves all components and contents");return restored;
 }
 static ServerPlayer entityRoundtrip(ServerPlayer p){
  var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,p.registryAccess());p.saveWithoutId(output);
  var copy=probe(p.level());copy.load(TagValueInput.create(ProblemReporter.DISCARDING,p.registryAccess(),output.buildResult()));return copy;
 }
 static ServerPlayer deathCopy(ServerPlayer p){
  var copy=probe(p.level());copy.restoreFrom(p,false);
  net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.AFTER_RESPAWN.invoker().afterRespawn(p,copy,false);return copy;
 }
 static void drag(AbstractContainerMenu menu,ServerPlayer p,int button,int... slots){
  // Vanilla QUICK_CRAFT packs stage in bits0..1 and left/right distribution in bits2..3.
  menu.clicked(-999,button<<2,ContainerInput.QUICK_CRAFT,p);
  for(int slot:slots)menu.clicked(slot,1|(button<<2),ContainerInput.QUICK_CRAFT,p);
  menu.clicked(-999,2|(button<<2),ContainerInput.QUICK_CRAFT,p);
 }
 static BackpackMenu menu(ServerPlayer p){var m=new BackpackMenu(71,p.getInventory(),p);p.containerMenu=m;return m;}
 static void click(BackpackMenu m,ServerPlayer p,int slot,int button){m.clicked(slot,button,ContainerInput.PICKUP,p);}
 static BackpackMenu equip(ServerPlayer p,int tier){
  empty(p);var m=menu(p);p.getInventory().setItem(0,named(BackpackEquipment.ITEMS[tier],1));
  m.clicked(inventorySlot(p,m,0),0,ContainerInput.QUICK_MOVE,p);
  check(m.capacity()==9*(tier+1)&&BackpackEquipment.capacity(BackpackEquipment.get(p))==m.capacity(),"native equip tier "+tier);return m;
 }
 static void collect(ItemStack stack,Map<String,Integer> counts){
  if(stack.isEmpty())return;String key=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();counts.merge(key,stack.getCount(),Integer::sum);
  stack.getOrDefault(DataComponents.CONTAINER,net.minecraft.world.item.component.ItemContainerContents.EMPTY).allItemsCopyStream().forEach(s->collect(s,counts));
 }
 static Map<String,Integer> total(ServerPlayer p,AbstractContainerMenu m){
  var result=new TreeMap<String,Integer>();collect(BackpackEquipment.get(p),result);collect(m.getCarried(),result);
  for(int i=0;i<36;i++)collect(p.getInventory().getItem(i),result);return result;
 }
 static void conserved(ServerPlayer p,BackpackMenu m,Map<String,Integer> expected,String why){check(expected.equals(total(p,m)),why);}
 static void cargoTests(ServerLevel l,int tier){
  var p=probe(l);var m=equip(p,tier);int capacity=m.capacity();
  check(m.slots.size()==64,"fixed native menu size");
  for(int i=0;i<27;i++)check(m.getSlot(1+i).isActive()==(i<capacity),"active cargo capacity "+tier+":"+i);
  p.getInventory().setItem(0,named(Items.DIAMOND,12));var baseline=total(p,m);int inv=inventorySlot(p,m,0);
  click(m,p,inv,0);click(m,p,1,1);check(m.getSlot(1).getItem().getCount()==1&&m.getCarried().getCount()==11,"right click inserts one");
  click(m,p,2,0);check(m.getSlot(2).getItem().getCount()==11&&m.getCarried().isEmpty(),"left click inserts remainder");
  click(m,p,2,1);check(m.getCarried().getCount()==6&&m.getSlot(2).getItem().getCount()==5,"right click takes ceiling half");
  drag(m,p,0,3,4);check(m.getSlot(3).getItem().getCount()==3&&m.getSlot(4).getItem().getCount()==3&&m.getCarried().isEmpty(),"native left drag evenly distributes");
  click(m,p,3,0);drag(m,p,1,5,6);check(m.getSlot(5).getItem().getCount()==1&&m.getSlot(6).getItem().getCount()==1&&m.getCarried().getCount()==1,"native right drag distributes one each");click(m,p,inv,0);
  m.clicked(4,1,ContainerInput.SWAP,p);check(p.getInventory().getItem(1).getCount()==3&&m.getSlot(4).getItem().isEmpty(),"native hotbar swap retrieves cargo");
  m.clicked(inventorySlot(p,m,1),0,ContainerInput.QUICK_MOVE,p);check(p.getInventory().getItem(1).isEmpty(),"native inventory quick move stores cargo");
  m.clicked(2,0,ContainerInput.QUICK_MOVE,p);check(m.getSlot(2).getItem().isEmpty(),"native cargo quick move retrieves");conserved(p,m,baseline,"all click styles conserve items");
  if(capacity<27){
   p.getInventory().setItem(8,new ItemStack(Items.EMERALD,4));click(m,p,inventorySlot(p,m,8),0);var saved=total(p,m);
   click(m,p,capacity+1,0);drag(m,p,0,capacity+1);m.clicked(capacity+1,0,ContainerInput.SWAP,p);
   check(m.getSlot(capacity+1).getItem().isEmpty()&&m.getCarried().getCount()==4,"inactive slots reject pickup drag and swap");conserved(p,m,saved,"inactive slots lose nothing");click(m,p,inventorySlot(p,m,8),0);
  }
  var stored=BackpackEquipment.get(p);codecRoundtrip(p,stored);same(stored,BackpackEquipment.get(entityRoundtrip(p)),"equipped bag entity NBT roundtrip");
  var respawn=deathCopy(p);same(stored,BackpackEquipment.get(respawn),"copyOnDeath includes cargo");
  var rm=menu(respawn);click(rm,respawn,1,0);same(stored,BackpackEquipment.get(p),"respawn cargo mutation does not alias original");rm.removed(respawn);
  m.removed(p);p.containerMenu=p.inventoryMenu;var reopened=menu(p);same(stored,BackpackEquipment.get(p),"close/reopen keeps bag and cargo");
  var before=total(p,reopened);m.clicked(1,0,ContainerInput.PICKUP,p);m.quickMoveStack(p,0);m.removed(p);conserved(p,reopened,before,"closed stale menu cannot duplicate or overwrite");
  p.offline=true;check(!reopened.stillValid(p),"disconnect invalidates menu");reopened.clicked(1,0,ContainerInput.PICKUP,p);reopened.removed(p);same(stored,BackpackEquipment.get(p),"disconnect cannot lose saved cargo");
  p.offline=false;var deathMenu=menu(p);p.setHealth(0);check(!deathMenu.stillValid(p),"death invalidates native menu");deathMenu.clicked(1,0,ContainerInput.PICKUP,p);same(stored,BackpackEquipment.get(p),"dead-player click cannot mutate cargo");
  var closer=probe(l);var cm=equip(closer,tier);closer.getInventory().setItem(0,named(Items.EMERALD,9));var closeTotal=total(closer,cm);
  click(cm,closer,inventorySlot(closer,cm,0),0);click(cm,closer,1,1);cm.removed(closer);
  check(cm.getCarried().isEmpty(),"close returns nonempty native cursor");conserved(closer,cm,closeTotal,"close saves cargo and returns cursor without duplicate");
 }
 static void switchTests(ServerLevel l){
  var p=probe(l);var m=equip(p,2);p.getInventory().setItem(0,named(Items.DIAMOND,37));
  click(m,p,inventorySlot(p,m,0),0);click(m,p,27,0);var large=BackpackEquipment.get(p);
  p.getInventory().setItem(0,named(BackpackEquipment.ITEMS[0],1));var baseline=total(p,m);
  click(m,p,inventorySlot(p,m,0),0);click(m,p,0,0);
  check(m.capacity()==9&&m.getSlot(27).getItem().isEmpty(),"smaller replacement hides old capacity");same(large,m.getCarried(),"filled old bag retains last-slot cargo on cursor");conserved(p,m,baseline,"filled large-to-small switch loses nothing");
  click(m,p,inventorySlot(p,m,0),0);click(m,p,inventorySlot(p,m,0),0);click(m,p,0,0);check(m.capacity()==27&&m.getSlot(27).getItem().getCount()==37,"switching filled bag back restores cargo");click(m,p,inventorySlot(p,m,0),0);
  for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));baseline=total(p,m);
  m.clicked(0,0,ContainerInput.QUICK_MOVE,p);conserved(p,m,baseline,"full inventory prevents bag quick removal without loss");check(m.capacity()==27,"full inventory retains equipped bag");
  click(m,p,0,0);check(m.capacity()==0&&m.getCarried().is(BackpackEquipment.ITEMS[2]),"full inventory permits safe cursor removal");conserved(p,m,baseline,"cursor holds complete filled bag");click(m,p,0,0);
  var replacement=menu(p);var snapshot=total(p,replacement);m.clicked(27,0,ContainerInput.PICKUP,p);m.broadcastChanges();m.removed(p);conserved(p,replacement,snapshot,"superseded live menu cannot mutate new instance");
 }
 static void rejectionTests(ServerLevel l){
  var p=probe(l);var m=equip(p,0);
  var forbidden=new ArrayList<ItemStack>();for(var item:BackpackEquipment.ITEMS)forbidden.add(new ItemStack(item));
  for(var item:List.of(Items.SHULKER_BOX,Items.BUNDLE))forbidden.add(new ItemStack(item));
  for(var item:List.of(Items.CHEST,Items.BARREL)){
   var filled=new ItemStack(item);filled.set(DataComponents.CONTAINER,net.minecraft.world.item.component.ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND))));forbidden.add(filled);
  }
  for(var item:List.of(Items.STONE,Items.CHEST,Items.BARREL,Items.COD_BUCKET))check(BackpackEquipment.canStore(new ItemStack(item)),"ordinary item without nested inventory allowed "+item);
  var disguised=new ItemStack(Items.STONE);disguised.set(DataComponents.CONTAINER,net.minecraft.world.item.component.ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND))));forbidden.add(disguised);
  for(var stack:forbidden){
   check(!BackpackEquipment.canStore(stack),"nested/container rejected "+stack);empty(p);p.getInventory().setItem(0,stack.copy());var before=total(p,m);
   click(m,p,inventorySlot(p,m,0),0);click(m,p,1,0);drag(m,p,0,1,2);check(m.getSlot(1).getItem().isEmpty()&&m.getSlot(2).getItem().isEmpty(),"container cursor/drag rejected");conserved(p,m,before,"container cursor no loss");click(m,p,inventorySlot(p,m,0),0);
   m.clicked(1,0,ContainerInput.SWAP,p);check(m.getSlot(1).getItem().isEmpty(),"container hotbar swap rejected");conserved(p,m,before,"container swap no loss");
   m.clicked(inventorySlot(p,m,0),0,ContainerInput.QUICK_MOVE,p);conserved(p,m,before,"container quickmove no loss");check(m.getSlot(1).getItem().isEmpty(),"container quickmove rejected");
  }
  var malformed=new ItemStack(BackpackEquipment.ITEMS[0]);var contents=net.minecraft.core.NonNullList.withSize(27,ItemStack.EMPTY);contents.set(26,new ItemStack(Items.DIAMOND));malformed.set(DataComponents.CONTAINER,net.minecraft.world.item.component.ItemContainerContents.fromItems(contents));
  check(!BackpackEquipment.validBag(malformed),"over-capacity bag rejected without truncation");codecRoundtrip(p,malformed);
  // Keep the expected value separate: native pickup splits/mutates the inventory stack.
  empty(p);p.getInventory().setItem(0,malformed.copy());click(m,p,inventorySlot(p,m,0),0);var before=total(p,m);click(m,p,0,0);conserved(p,m,before,"malformed replacement does not lose cargo");same(malformed,m.getCarried(),"malformed replacement stays intact on cursor");
 }
 static int token(ServerPlayer p){return BackpackShop.SESSIONS.get(p.getUUID()).token();}
 static void shopTests(MinecraftServer server)throws Exception{
  var l=server.getLevel(CompactShops.DIM);check(l!=null,"compact shop dimension registered");var p=probe(l);p.setPos(net.minecraft.world.phys.Vec3.atCenterOf(CompactShops.counter(4)));
  check(BackpackShop.near(p),"real market counter accepted");
  var saved=StockMarket.JSON.toJson(StockMarket.ledger);var file=StockMarket.file;
  try{
   var account=StockMarket.ledger.account(p.getStringUUID());account.cash=100000;
   check(Arrays.equals(BackpackShop.PRICES,new int[]{3000,8000,20000}),"approved market prices");
   for(int tier=0;tier<3;tier++){
    empty(p);check(BackpackShop.open(p)==1,"shop opens valid counter");int t=token(p);long cash=account.cash;
    check(BackpackShop.request(p,t,tier)==1&&account.cash==cash-BackpackShop.PRICES[tier],"exact purchase debit tier "+tier);
    check(p.getInventory().getItem(0).is(BackpackEquipment.ITEMS[tier])&&p.getInventory().getItem(0).getCount()==1,"exact purchase item");check(token(p)!=t,"success rotates quote");
    check(BackpackShop.request(p,t,tier)==0&&account.cash==cash-BackpackShop.PRICES[tier]&&p.getInventory().getItem(0).getCount()==1,"purchase replay no duplicate or debit");
   }
   int t=token(p);long cash=account.cash;check(BackpackShop.request(p,t,-1)==0&&token(p)==t&&account.cash==cash,"invalid tier preserves quote");
   check(BackpackShop.request(p,t==0?1:0,0)==0&&token(p)==t&&account.cash==cash,"foreign equipment/session token rejected");
   p.setPos(net.minecraft.world.phys.Vec3.atCenterOf(CompactShops.counter(4)).add(10,0,0));check(BackpackShop.request(p,t,0)==0&&token(p)==t&&account.cash==cash,"wrong range cannot spend");
   p.setPos(net.minecraft.world.phys.Vec3.atCenterOf(CompactShops.counter(0)));check(BackpackShop.open(p)==0&&BackpackShop.request(p,t,0)==0&&token(p)==t,"wrong room rejects existing quote");
   p.setPos(net.minecraft.world.phys.Vec3.atCenterOf(CompactShops.counter(4)));
   BackpackShop.SESSIONS.put(p.getUUID(),new BackpackShop.Session(t,l.getGameTime()-1));check(BackpackShop.request(p,t,0)==0&&account.cash==cash,"expired token cannot spend");
   empty(p);BackpackShop.open(p);t=token(p);account.cash=2999;check(BackpackShop.request(p,t,0)==0&&account.cash==2999&&p.getInventory().getItem(0).isEmpty()&&token(p)!=t,"insufficient cash rotates token without loss");
   account.cash=100000;for(int i=0;i<36;i++)p.getInventory().setItem(i,named(Items.STONE,64));t=token(p);var inventory=Cyberware.inventory(p);
   check(BackpackShop.request(p,t,2)==0&&account.cash==100000&&token(p)!=t,"full inventory rotates token without charge");for(int i=0;i<36;i++)same(p.getInventory().getItem(i),inventory.get(i),"full inventory preserved "+i);
   empty(p);p.getInventory().setItem(4,named(Items.DIAMOND,7));t=token(p);var failedInventory=Cyberware.inventory(p);
   // A file as parent deterministically fails createDirectories before a temporary ledger write.
   var blocker=Files.createTempFile(server.getWorldPath(LevelResource.ROOT),"backpack-save-failure-",".blocked");
   try{StockMarket.file=blocker.resolve("market.json");
    check(BackpackShop.request(p,t,1)==0&&account.cash==100000&&token(p)!=t,"save failure rolls back charge and rotates quote");for(int i=0;i<36;i++)same(p.getInventory().getItem(i),failedInventory.get(i),"save rollback preserves components "+i);
   }finally{StockMarket.file=file;Files.delete(blocker);}
   check(server.getCommands().getDispatcher().getRoot().getChild("neonbagshop")!=null&&server.getCommands().getDispatcher().getRoot().getChild("neonbackpack")!=null,"separate shop and equipment command roots");
   net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.invoker().onPlayDisconnect(p.connection,server);check(!BackpackShop.SESSIONS.containsKey(p.getUUID()),"disconnect removes shop quote");
  }finally{StockMarket.file=file;StockMarket.ledger=StockMarket.JSON.fromJson(saved,MarketLedger.class);BackpackShop.SESSIONS.remove(p.getUUID());}
 }
 public static int run(ServerPlayer p)throws Exception{
  checks=0;var emptyMenu=menu(p);check(emptyMenu.capacity()==0,"no equipment means no cargo capacity");
  for(int slot=1;slot<=27;slot++)check(!emptyMenu.getSlot(slot).isActive(),"empty bag slot leaves cargo inactive "+slot);
  for(int tier=0;tier<3;tier++)cargoTests(p.level(),tier);switchTests(p.level());rejectionTests(p.level());shopTests(p.level().getServer());return checks;
 }
 @Override public void onInitialize(){ServerTickEvents.END_SERVER_TICK.register(server->{
  if(done||server.getTickCount()<40)return;done=true;
  try{guard(server);run(probe(server.overworld()));System.out.println("BACKPACK_TEST_COMPLETE checks="+checks);}
  catch(Throwable failure){System.out.println("BACKPACK_TEST_FAILED");failure.printStackTrace();}
 });}
}
