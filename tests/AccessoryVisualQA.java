package jp.neonward;

import java.nio.file.*;
import java.util.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.client.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;

/** Four real screen captures and C2S vanilla quick-moves in a disposable completed-world copy. */
public final class AccessoryVisualQA implements ClientModInitializer {
    int warm,stage,age,wait,shots;boolean started,done;volatile boolean ready;volatile Throwable failure;
    static void check(boolean ok,String why){if(!ok)throw new AssertionError("ACCESSORY_VISUAL: "+why);}
    static void guard(net.minecraft.server.MinecraftServer s){var root=Path.of(System.getProperty("neonward.accessory.visual.root","__missing__")).toAbsolutePath().normalize();check(Files.isRegularFile(root.resolve("ACCESSORY_VISUAL_ONLY"))&&s.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().equals(root.resolve("saves/Sakura-QA")),"isolated copied world sentinel");}
    static ServerPlayer player(Minecraft mc){return mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();}
    void server(Minecraft mc,Runnable task){mc.getSingleplayerServer().execute(()->{try{task.run();}catch(Throwable e){failure=e;}});}
    void capture(Minecraft mc,String name){check(mc.gui.screen() instanceof AccessoryScreen,"real registered accessory screen rendered");Screenshot.grab(mc,false);shots++;System.out.println("ACCESSORY_SCREENSHOT "+name);}
    void validate(Minecraft mc,int expected){
        var menu=mc.player.containerMenu;check(menu instanceof AccessoryMenu&&menu.slots.size()==39,"client menu synchronization");int count=0;
        for(int i=0;i<3;i++){
            var item=menu.getSlot(i).getItem();if(!item.isEmpty()){count++;check(item.getCount()==1&&item.has(DataComponents.CUSTOM_NAME),"menu slot item components/count");}
            check(ItemStack.matches(item,AccessoryEquipment.get(mc.player,i)),"attachment and menu agree on client");
        }
        check(count==expected&&menu.getCarried().isEmpty(),"client occupied slots/cursor");
        server(mc,()->{var p=player(mc);check(p.containerMenu instanceof AccessoryMenu,"server menu stays open");int n=0,total=0;for(int i=0;i<3;i++){var item=AccessoryEquipment.get(p,i);if(!item.isEmpty())n++;total+=item.getCount();}for(int i=0;i<36;i++)total+=p.getInventory().getItem(i).getCount();check(n==expected&&total==6,"server attachment/total count no loss or duplication");});
    }
    @Override public void onInitializeClient(){
        ServerPlayConnectionEvents.JOIN.register((h,s,server)->{guard(server);WelcomeTutorial.pending.remove(h.player.getUUID());if(StockMarket.ledger!=null)StockMarket.ledger.account(h.player.getStringUUID()).tutorialDone=true;});
        ClientTickEvents.END_CLIENT_TICK.register(mc->{
            if(done||mc.player==null||mc.level==null||mc.getSingleplayerServer()==null)return;
            try{
                if(failure!=null)throw new AssertionError("Accessory server assertion failed",failure);
                ObjectiveHud.visible=false;if(++warm<60){mc.gui.setScreen(null);return;}
                check(++wait<800,"bounded accessory visual QA");
                if(!started){started=true;if(mc.gui.hud.isHidden())mc.gui.hud.toggle();mc.gui.setScreen(null);
                    server(mc,()->{var p=player(mc);guard(p.level().getServer());check(SakuraTown.ready(p.level().getServer()),"completed copied town");p.setGameMode(GameType.SURVIVAL);p.setInvulnerable(true);p.removeAllEffects();
                        for(int i=0;i<36;i++)p.getInventory().setItem(i,ItemStack.EMPTY);
                        for(int i=0;i<3;i++){check(AccessoryEquipment.get(p,i).isEmpty(),"fresh visual account accessory slot empty");var item=new ItemStack(ShrineServices.AMULETS[i],2);item.set(DataComponents.CUSTOM_NAME,Component.literal("QA お守り "+i));p.getInventory().setItem(9+i,item);}
                        p.getInventory().setChanged();p.containerMenu.broadcastChanges();ready=true;});return;}
                if(!ready)return;age++;
                if(stage==0&&age==20){mc.player.connection.sendCommand("neonaccessories");stage=1;age=0;}
                else if(stage==1&&age>=30&&mc.gui.screen() instanceof AccessoryScreen){validate(mc,0);capture(mc,"01-empty-accessory-screen");for(int slot=3;slot<=5;slot++)mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId,slot,0,ContainerInput.QUICK_MOVE,mc.player);stage=2;age=0;}
                else if(stage==2&&age==40){validate(mc,3);capture(mc,"02-three-equipped");mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId,0,0,ContainerInput.QUICK_MOVE,mc.player);stage=3;age=0;}
                else if(stage==3&&age==40){validate(mc,2);capture(mc,"03-travel-unequipped");mc.player.closeContainer();stage=4;age=0;}
                else if(stage==4&&age==20){mc.player.connection.sendCommand("neonaccessories");stage=5;age=0;}
                else if(stage==5&&age>=30&&mc.gui.screen() instanceof AccessoryScreen){validate(mc,2);capture(mc,"04-reopened-persistent-slots");stage=6;age=0;}
                else if(stage==6&&age==20){check(shots==4,"four native screen captures");done=true;System.out.println("ACCESSORY_CLIENT_QA_COMPLETE captures=4 real registered screen, C2S quickMove, synchronized attachment, reopen persistence, total6 unchanged");mc.stop();}
            }catch(Throwable e){done=true;e.printStackTrace();System.err.println("ACCESSORY_CLIENT_QA_FAILED stage="+stage);mc.stop();}
        });
    }
}
