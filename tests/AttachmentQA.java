package jp.neonward;
import java.util.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
public final class AttachmentQA implements ClientModInitializer {
 int ticks,shots;volatile Throwable failure;
 void shot(Minecraft mc,String label){Screenshot.grab(mc,false);shots++;System.out.println("ATTACH_SCREEN "+label);}
 void server(Minecraft mc,Runnable r){mc.getSingleplayerServer().execute(()->{try{r.run();}catch(Throwable t){failure=t;}});}
 public void onInitializeClient(){ClientTickEvents.END_CLIENT_TICK.register(mc->{if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null)return;ticks++;mc.gui.hud.getChat().clearMessages(false);
  if(failure!=null){failure.printStackTrace();mc.stop();return;}
  if(ticks<40){mc.gui.setScreen(null);return;}int index=(ticks-40)/110,step=(ticks-40)%110;
  if(index>=7){if(shots!=12)throw new AssertionError("shots "+shots);System.out.println("PAIRED_CLIENT_QA_COMPLETE ATTACHMENT_CLIENT_PASS");mc.stop();return;}
  if(index<4)mc.gui.setScreen(null);
  if(step==0){mc.options.keyUse.setDown(false);mc.player.stopUsingItem();mc.gui.setScreen(null);
   server(mc,()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();p.stopUsingItem();p.setGameMode(GameType.CREATIVE);
    for(int i=0;i<36;i++)p.getInventory().setItem(i,ItemStack.EMPTY);
    if(index<5){var l=mc.getSingleplayerServer().overworld();
     if(index==0){for(int x=3046;x<=3054;x++)for(int z=0;z<=10;z++)l.setBlock(new BlockPos(x,69,z),Blocks.STONE.defaultBlockState(),3);for(int x=3044;x<=3056;x++)for(int yy=69;yy<=77;yy++)l.setBlock(new BlockPos(x,yy,30),x<=3050?net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("white_concrete")).defaultBlockState():net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("black_concrete")).defaultBlockState(),3);}
     p.teleportTo(l,3050.5,70,3.5,Set.of(),0,0,true);var gun=new ItemStack(NeonArsenal.ITEMS.get("longwatch_sniper"));GunAttachments.install(gun,0,Math.min(index,3)*5+4);GunAttachments.install(gun,1,24);GunAttachments.install(gun,2,34);GunAttachments.install(gun,3,39);p.setItemSlot(EquipmentSlot.MAINHAND,gun);p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);for(int i=0;i<8;i++)p.getInventory().setItem(9+i,GunAttachments.stack(i*5+2));p.containerMenu.broadcastChanges();
    }else if(index==5){for(int xx=519;xx<=524;xx++)for(int zz=445;zz<=452;zz++)mc.getSingleplayerServer().overworld().setBlock(new BlockPos(xx,64,zz),Blocks.STONE.defaultBlockState(),3);p.teleportTo(mc.getSingleplayerServer().overworld(),522,65,449,Set.of(),180,0,true);AttachmentGacha.cabinet(p.level(),p.level().getGameTime());StockMarket.ledger.account(p.getStringUUID()).cash=50000;AttachmentGacha.ends=0;}
    else {var l=mc.getSingleplayerServer().getLevel(CompactShops.DIM);CompactShops.ensure(l,0);var at=CompactShops.counter(0);p.teleportTo(l,at.getX()+.5,65,at.getZ()+2,Set.of(),180,0,true);StockMarket.ledger.account(p.getStringUUID()).cash=100000;}
   });
  }
  if(index<4){
   if(step>=15&&step<45)mc.options.keyUse.setDown(true);
   if(step==15)mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);
   if(step==35){if(!GunControls.aiming())throw new AssertionError("aim");float expected=index<2?1:index==2?.25f:.125f;if(GunControls.zoom()!=expected)throw new AssertionError("zoom");shot(mc,"scope "+index);}
   if(step==45){mc.options.keyUse.setDown(false);mc.player.stopUsingItem();server(mc,()->mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst().stopUsingItem());}
   if(step==52)shot(mc,"hardware "+index);
  }else {
   if(step==15)server(mc,()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();if(AttachmentService.open(p,index==4?"equip":index==5?"gacha":"shop")!=1)throw new AssertionError("open "+index);});
   if(step==22){if(!(mc.gui.screen() instanceof AttachmentScreen))throw new AssertionError("screen");shot(mc,"screen "+index);}
   if(index==5&&step==30)server(mc,()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();if(AttachmentService.request(p,"roll",AttachmentService.SESSIONS.get(p.getUUID()).token(),10)!=10)throw new AssertionError("ten");});
   if(index==5&&step==100)shot(mc,"ten results");
  }
 });}
}
