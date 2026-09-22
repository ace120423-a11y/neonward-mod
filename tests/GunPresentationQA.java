package jp.neonward;
import java.util.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
public class GunPresentationQA implements ClientModInitializer {
 int ticks,shots;volatile Throwable failure;
 public void onInitializeClient(){ClientTickEvents.END_CLIENT_TICK.register(mc->{if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null)return;ticks++;mc.gui.setScreen(null);
  if(failure!=null){failure.printStackTrace();mc.stop();return;}
  if(GunVfxClient.lastParticles>128||GunVfxClient.ACTIVE.size()>64)throw new AssertionError("Unbounded gun visuals");
  if(ticks<40)return;int index=(ticks-40)/60,step=(ticks-40)%60;
  if(index>=11){GunVfxClient.clear();if(!GunVfxClient.LOOPS.isEmpty()||!GunVfxClient.ACTIVE.isEmpty()||shots!=12)throw new AssertionError("Cleanup/shots");System.out.println("PAIRED_CLIENT_QA_COMPLETE GUN_PRESENTATION_CLIENT_PASS");mc.stop();return;}
  if(step==0){mc.options.keyAttack.setDown(false);mc.options.keyUse.setDown(false);mc.player.stopUsingItem();mc.getSingleplayerServer().execute(()->{try{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();var l=p.level();p.setGameMode(GameType.CREATIVE);p.stopUsingItem();
   if(index==0){for(int x=3046;x<=3054;x++)for(int z=0;z<=16;z++){l.setBlock(new BlockPos(x,69,z),Blocks.STONE.defaultBlockState(),3);for(int y=70;y<76;y++)l.setBlock(new BlockPos(x,y,z),z==16?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);}}
   p.teleportTo(l,3050.5,70,3.5,Set.of(),0,0,true);p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(NeonArsenal.ITEMS.get(GunVfx.IDS[index])));p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);p.containerMenu.broadcastChanges();
  }catch(Throwable t){failure=t;}});}
  if(step>=10&&step<45)mc.options.keyUse.setDown(true);
  if(step==10)mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);
  if(index==9&&step>=30&&step<45)mc.options.keyAttack.setDown(true);
  if(step==35&&index!=9)mc.getSingleplayerServer().execute(()->{try{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();var gun=(NeonArsenal.Rifle)p.getMainHandItem().getItem();if(gun.fire(p.level(),p,InteractionHand.MAIN_HAND)==net.minecraft.world.InteractionResult.FAIL)throw new AssertionError("Shot rejected "+index);}catch(Throwable t){failure=t;}});
  if(step==38){if(index==9&&(!GunVfxClient.CRYO.containsKey(mc.player.getId())||GunVfxClient.LOOPS.isEmpty()))throw new AssertionError("No continuous cryo");if(index==10&&mc.level.getEntitiesOfClass(net.minecraft.world.entity.projectile.arrow.Arrow.class,mc.player.getBoundingBox().inflate(20)).isEmpty())throw new AssertionError("Arrow not synchronized");Screenshot.grab(mc,false);shots++;System.out.println("GUN_SCREENSHOT "+GunVfx.IDS[index]+" particles="+GunVfxClient.lastParticles);}
  if(index==10&&step==43)mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();p.teleportTo(p.level(),3054.5,70,14,Set.of(),63.4f,0,true);});
  if(index==10&&step==48){var arrows=mc.level.getEntitiesOfClass(net.minecraft.world.entity.projectile.arrow.Arrow.class,mc.player.getBoundingBox().inflate(20));if(arrows.isEmpty()||arrows.stream().noneMatch(a->a.getZ()>15&&a.getZ()<16))throw new AssertionError("Arrow did not reach wall on client");Screenshot.grab(mc,false);shots++;}
  if(step==45){mc.options.keyUse.setDown(false);mc.options.keyAttack.setDown(false);mc.player.stopUsingItem();mc.getSingleplayerServer().execute(()->mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst().stopUsingItem());}
  if(step==50&&GunVfxClient.LOOPS.containsKey(mc.player.getId()))throw new AssertionError("Loop survives release");
 });}
}
