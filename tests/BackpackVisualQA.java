package jp.neonward;

import java.nio.file.*;
import java.util.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;

/** Actual registered screens, C2S container clicks and server shop replies in a disposable copy. */
public final class BackpackVisualQA implements ClientModInitializer {
 int warm,stage,age,wait,shots;boolean started,done;volatile boolean ready;volatile Throwable failure;
 static void check(boolean ok,String why){if(!ok)throw new AssertionError("BACKPACK_VISUAL: "+why);}
 static void guard(net.minecraft.server.MinecraftServer server){
  var root=Path.of(System.getProperty("neonward.backpack.visual.root","__missing__")).toAbsolutePath().normalize();
  check(Files.isRegularFile(root.resolve("BACKPACK_VISUAL_ONLY"))&&server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().equals(root.resolve("saves/Backpack-QA")),"isolated copied world sentinel");
 }
 static ServerPlayer player(Minecraft mc){return mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();}
 void server(Minecraft mc,Runnable task){mc.getSingleplayerServer().execute(()->{try{guard(mc.getSingleplayerServer());task.run();}catch(Throwable e){failure=e;}});}
 void capture(Minecraft mc,String name){Screenshot.grab(mc,false);shots++;System.out.println("BACKPACK_SCREENSHOT "+name);}
 void move(Minecraft mc,int slot){mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId,slot,0,ContainerInput.QUICK_MOVE,mc.player);}
 void validate(Minecraft mc,int capacity,int cargo){
  check(mc.gui.screen() instanceof BackpackScreen&&mc.player.containerMenu instanceof BackpackMenu,"real registered screen/menu");
  var menu=mc.player.containerMenu;check(menu.slots.size()==64&&menu.getCarried().isEmpty(),"native menu slots and cursor");
  check(BackpackEquipment.capacity(menu.getSlot(0).getItem())==capacity,"synchronized equipment capacity");
  int total=0;for(int i=1;i<=27;i++){check(menu.getSlot(i).isActive()==(i<=capacity),"slot availability "+i);total+=menu.getSlot(i).getItem().getCount();}
  check(total==cargo,"synchronized cargo count");
  // Acknowledge each server snapshot before the next C2S move/close can change it.
  try{mc.getSingleplayerServer().submit(()->{
   guard(mc.getSingleplayerServer());
   var p=player(mc);check(p.containerMenu instanceof BackpackMenu,"server native menu");
   check(BackpackEquipment.capacity(BackpackEquipment.get(p))==capacity,"server equipped state");
   var all=new ArrayList<ItemStack>();all.add(BackpackEquipment.get(p));for(int i=0;i<36;i++)all.add(p.getInventory().getItem(i));
   int bags=0,apples=0,bread=0;
   for(var item:all){
    if(BackpackEquipment.capacity(item)>0)bags+=item.getCount();
    var contents=new ArrayList<ItemStack>();contents.add(item);item.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).allItemsCopyStream().forEach(contents::add);
    for(var value:contents){if(value.is(Items.APPLE))apples+=value.getCount();if(value.is(Items.BREAD))bread+=value.getCount();}
   }
   check(bags==3&&apples==12&&bread==8,"server no loss/duplication including unequipped bags");
  }).get(5,java.util.concurrent.TimeUnit.SECONDS);}catch(Exception e){throw new AssertionError("Server inventory validation did not complete",e);}
 }
 @Override public void onInitializeClient(){
  ServerPlayConnectionEvents.JOIN.register((h,s,server)->{guard(server);WelcomeTutorial.pending.remove(h.player.getUUID());if(StockMarket.ledger!=null)StockMarket.ledger.account(h.player.getStringUUID()).tutorialDone=true;});
  ClientTickEvents.END_CLIENT_TICK.register(mc->{
   if(done||mc.player==null||mc.level==null||mc.getSingleplayerServer()==null)return;
   try{
    if(failure!=null)throw new AssertionError("Backpack server validation",failure);
    ObjectiveHud.visible=false;if(++warm<80){mc.gui.setScreen(null);return;}
    check(++wait<1600,"bounded visual QA");
    if(!started){
     started=true;if(mc.gui.hud.isHidden())mc.gui.hud.toggle();mc.gui.setScreen(null);mc.options.setCameraType(CameraType.FIRST_PERSON);
     server(mc,()->{var p=player(mc);p.setGameMode(GameType.SURVIVAL);p.setInvulnerable(true);p.setNoGravity(true);p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);p.removeAllEffects();check(BackpackEquipment.get(p).isEmpty(),"fresh account bag slot");
      for(int i=0;i<36;i++)p.getInventory().setItem(i,ItemStack.EMPTY);
      p.getInventory().setItem(0,new ItemStack(BackpackEquipment.ITEMS[1]));
      p.getInventory().setItem(9,new ItemStack(BackpackEquipment.ITEMS[0]));
      p.getInventory().setItem(10,new ItemStack(BackpackEquipment.ITEMS[2]));
      p.getInventory().setItem(11,new ItemStack(Items.APPLE,12));p.getInventory().setItem(12,new ItemStack(Items.BREAD,8));
      p.getInventory().setChanged();p.containerMenu.broadcastChanges();ready=true;
     });return;
    }
    if(!ready)return;age++;
    if(stage==12&&(age==10||age==40||age==200)){
     System.out.println("BACKPACK_OPEN_DIAG client wait="+age+" screen="+(mc.gui.screen()==null?"null":mc.gui.screen().getClass().getSimpleName())+" pos="+mc.player.position()+" dimension="+mc.player.level().dimension());
     server(mc,()->{var p=player(mc);System.out.println("BACKPACK_OPEN_DIAG server pos="+p.position()+" room="+CompactShops.room(p.level(),p.blockPosition())+" near="+BackpackShop.near(p)+" session="+BackpackShop.SESSIONS.containsKey(p.getUUID())+" canReply="+net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(p,BackpackShop.Reply.TYPE));});
    }
    if(stage==0&&age==30){mc.gui.setScreen(new InventoryScreen(mc.player));stage=1;age=0;}
    else if(stage==1&&age==30){
     check(mc.gui.screen() instanceof InventoryScreen,"native inventory");
     check(Screens.getWidgets(mc.gui.screen()).stream().anyMatch(w->w.getMessage().getString().equals("バッグ")),"inventory bag button exists");
     capture(mc,"01-inventory-bag-button");mc.gui.setScreen(null);mc.player.connection.sendCommand("neonbackpack");stage=2;age=0;
    }else if(stage==2&&age>=30&&mc.gui.screen() instanceof BackpackScreen){
     validate(mc,0,0);capture(mc,"02-empty-locked-storage");move(mc,28);stage=3;age=0;
    }else if(stage==3&&age==30){validate(mc,9,0);move(mc,30);stage=4;age=0;
    }else if(stage==4&&age==35){validate(mc,9,12);capture(mc,"03-small-capacity-nine");move(mc,0);stage=5;age=0;
    }else if(stage==5&&age==30){validate(mc,0,0);move(mc,29);stage=6;age=0;
    }else if(stage==6&&age==30){validate(mc,27,0);move(mc,31);stage=7;age=0;
    }else if(stage==7&&age==35){validate(mc,27,8);capture(mc,"04-large-capacity-twenty-seven");move(mc,0);stage=8;age=0;
    }else if(stage==8&&age==30){validate(mc,0,0);mc.player.closeContainer();stage=9;age=0;
    }else if(stage==9&&age==40){
     check(mc.gui.screen()==null&&!mc.gui.hud.isHidden()&&mc.options.getCameraType()==CameraType.FIRST_PERSON,"visible first-person HUD and hands");
     check(mc.player.getMainHandItem().is(BackpackEquipment.ITEMS[1]),"actual synchronized held medium bag");
     capture(mc,"05-native-hand-model");ready=false;
     server(mc,()->{var p=player(mc);check(CompactShops.enter(p,4),"normal market entry");var level=p.level();
      var counter=CompactShops.counter(4);check(p.teleportTo(level,counter.getX()+.5,counter.getY(),counter.getZ()+2.5,Set.of(),180,20,true),"market counter approach");
      StockMarket.ledger.account(p.getStringUUID()).cash=50000;ready=true;
     });stage=10;age=0;
    }else if(stage==10&&age==60){
     ready=false;server(mc,()->{var p=player(mc);
      var staff=p.level().getEntitiesOfClass(CityResident.class,p.getBoundingBox().inflate(6),n->n.job.equals("compact_market")&&CompactShops.room(p.level(),n.blockPosition())==4);
      check(staff.size()==1,"one actual ensured market clerk within interaction range");
      check(CompactShops.staffUse(p,staff.getFirst()),"production market staff interaction sends Open packet");ready=true;
     });stage=11;age=0;
    }else if(stage==11&&age>=40&&mc.gui.screen() instanceof StockScreen){
     var buttons=Screens.getWidgets(mc.gui.screen()).stream().filter(w->w.getMessage().getString().equals("バッグ販売")).toList();
     check(buttons.size()==1&&buttons.getFirst().active&&buttons.getFirst().visible,"StockScreen has one enabled bag-sales entry");
     check(buttons.getFirst() instanceof PhoneScreen.NeonButton,"production bag-sales button type");
     System.out.println("BACKPACK_OPEN_DIAG press stockAge="+((StockScreen)mc.gui.screen()).age+" pollPhase="+(((StockScreen)mc.gui.screen()).age%40)+" pos="+mc.player.position());
     ((PhoneScreen.NeonButton)buttons.getFirst()).onPress(new net.minecraft.client.input.MouseButtonInfo(0,0));
     System.out.println("BACKPACK_MARKET_ENTRY_PASS staffUse/Open -> StockScreen -> バッグ販売.onPress");stage=12;age=0;
    }else if(stage==12&&age>=40&&mc.gui.screen() instanceof BackpackShopScreen shop){
     check(shop.rows.size()==3,"three real server shop rows");int[] prices={3000,8000,20000};
     for(int i=0;i<3;i++){var row=shop.rows.get(i).getAsJsonObject();check(row.get("price").getAsInt()==prices[i]&&row.get("capacity").getAsInt()==9*(i+1),"server tier quote "+i);}
     capture(mc,"06-market-three-prices");stage=13;age=0;
    }else if(stage==13&&age==30){check(shots==6,"six captures");done=true;System.out.println("BACKPACK_CLIENT_QA_COMPLETE captures=6 native clicks/server packets, capacity9/27/empty, preserved cargo, market entry, hand model");mc.stop();}
   }catch(Throwable e){done=true;e.printStackTrace();System.err.println("BACKPACK_CLIENT_QA_FAILED stage="+stage);mc.stop();}
  });
 }
}
