package jp.neonward;
import java.util.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
public class ElementVfxQA implements ClientModInitializer {
 int ticks,shots;volatile Throwable failure;Mob target;long before;
 public void onInitializeClient(){ClientTickEvents.END_CLIENT_TICK.register(mc->{
  if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null)return;
  ticks++;if(mc.gui.screen()!=null)mc.gui.setScreen(null);
  if(failure!=null){failure.printStackTrace();mc.stop();return;}
  if(ElementVfxClient.lastEmitted>96||ElementVfxClient.ACTIVE.size()>48)throw new AssertionError("Unbounded visuals");
  if(ticks==30)mc.getSingleplayerServer().execute(()->{try{
   var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();var l=p.level();p.setGameMode(GameType.CREATIVE);
   for(int x=3044;x<=3056;x++)for(int z=0;z<=14;z++){l.setBlock(new BlockPos(x,69,z),Blocks.STONE.defaultBlockState(),3);for(int y=70;y<76;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),3);}
   p.teleportTo(l,3050.5,70,3.5,Set.of(),0,0,true);p.stopUsingItem();p.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);
   target=NeonHostiles.TYPES.get("neon_runner").create(l,EntitySpawnReason.COMMAND);target.setNoAi(true);target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);target.setHealth(100);target.setPos(3050.5,70,7);l.addFreshEntity(target);
  }catch(Throwable t){failure=t;}});
  if(ticks>=60&&ticks<420){int kind=(ticks-60)/60,step=(ticks-60)%60;
   if(step==0){ElementVfxClient.ACTIVE.clear();before=ElementVfxClient.totalEmitted;mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();ElementVfx.emit(p,target,kind,14,false);if(kind!=3&&kind!=4)ElementVfx.emit(p,target,kind,40,true);});}
   if(step==6){if(ElementVfxClient.totalEmitted<=before)throw new AssertionError("No network visuals for "+kind);Screenshot.grab(mc,false);shots++;System.out.println("ELEMENT_VFX_SHOT kind="+kind+" active="+ElementVfxClient.ACTIVE.size()+" emitted="+ElementVfxClient.lastEmitted);}
   if(step==50&&!ElementVfxClient.ACTIVE.isEmpty())throw new AssertionError("Expired effect retained");
  }
  if(ticks==425){ElementVfxClient.ACTIVE.clear();var c=mc.player.position().add(0,1,2);
   ElementVfxClient.receive(new ElementVfx.Visual(0,0,c.x+100,c.y,c.z,0,10,false));if(!ElementVfxClient.ACTIVE.isEmpty())throw new AssertionError("Far visual accepted");
   for(int i=0;i<100;i++)ElementVfxClient.receive(new ElementVfx.Visual(0,i,c.x,c.y,c.z,0,10,false));if(ElementVfxClient.ACTIVE.size()!=48)throw new AssertionError("Animation limit");
  }
  if(ticks==440&&(!ElementVfxClient.ACTIVE.isEmpty()||shots!=6))throw new AssertionError("Vfx cleanup/screenshots");
  if(ticks==450||ticks==520||ticks==560){String id=ticks==450?"neon_dualblades":ticks==520?"impact_gauntlet":"akatsuki_wakizashi";mc.options.keyUse.setDown(false);mc.player.stopUsingItem();mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();p.stopUsingItem();p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(NeonArsenal.ITEMS.get(id)));p.containerMenu.broadcastChanges();});}
  if(ticks==480||ticks==500||ticks==570){before=ElementVfxClient.totalEmitted;mc.player.swing(ticks==500?net.minecraft.world.InteractionHand.OFF_HAND:net.minecraft.world.InteractionHand.MAIN_HAND);}
  if(ticks>=479&&ticks<=485)System.out.println("VFX_WEAPON_PROBE tick="+ticks+" item="+mc.player.getMainHandItem()+" swing="+mc.player.getAttackAnim(0)+" swingTime="+mc.player.swingTime+" using="+mc.player.isUsingItem()+" players="+mc.level.players().size()+" count="+ElementVfxClient.lastEmitted);
  if(ticks>=530&&ticks<=550){mc.options.keyUse.setDown(true);if(ticks==530){before=ElementVfxClient.totalEmitted;mc.gameMode.useItem(mc.player,net.minecraft.world.InteractionHand.MAIN_HAND);}}
  if(ticks==485||ticks==505||ticks==540||ticks==575){if(ElementVfxClient.totalEmitted<=before)throw new AssertionError("Missing weapon trail/charge at "+ticks);Screenshot.grab(mc,false);shots++;System.out.println("ELEMENT_WEAPON_SHOT "+ticks);}
  if(ticks==600){if(shots!=10)throw new AssertionError("Weapon screenshot count");System.out.println("PAIRED_CLIENT_QA_COMPLETE ELEMENT_VFX_PASS: six styles, S2C, expiry, distance, 48 animations, 96 particles/tick, dual trails, fist charge, fire trail");mc.stop();}
 });}
}
