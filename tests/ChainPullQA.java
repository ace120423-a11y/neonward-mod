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
public class ChainPullQA implements ClientModInitializer {
 int ticks,shot;volatile Throwable failure;Mob enemy;
 public void onInitializeClient(){ClientTickEvents.END_CLIENT_TICK.register(mc->{
  if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null)return;
  ticks++;mc.gui.setScreen(null);if(failure!=null){failure.printStackTrace();mc.stop();return;}
  if(ticks==30||ticks==130){boolean second=ticks==130;shot=0;mc.options.setCameraType(second?CameraType.THIRD_PERSON_BACK:CameraType.FIRST_PERSON);
   mc.getSingleplayerServer().execute(()->{try{
    var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();var l=p.level();p.setGameMode(GameType.CREATIVE);
    for(int x=3044;x<=3056;x++)for(int z=0;z<=14;z++){l.setBlock(new BlockPos(x,69,z),Blocks.QUARTZ_BLOCK.defaultBlockState(),3);for(int y=70;y<76;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),3);}
    p.teleportTo(l,3050.5,70,2.5,Set.of(),0,0,true);p.stopUsingItem();p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(NeonArsenal.ITEMS.get("chain_kusarigama")));p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);p.containerMenu.broadcastChanges();
    if(enemy!=null)enemy.discard();enemy=NeonHostiles.TYPES.get("neon_runner").create(l,EntitySpawnReason.COMMAND);enemy.setNoAi(false);enemy.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);enemy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);enemy.getAttribute(Attributes.ARMOR).setBaseValue(0);enemy.setHealth(100);enemy.setPos(3050.5,70,8.5);l.addFreshEntity(enemy);
   }catch(Throwable t){failure=t;}});
  }
  if(ticks==90||ticks==180)mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();ArsenalExpansion.skill(p,p.getMainHandItem(),1,1);});
  float phase=ChainPullClient.phase(mc.player.getId(),0);
  if(phase>=0&&((shot==0&&phase>.06f)||(shot==1&&phase>.32f)||(shot==2&&phase>.70f))){shot++;Screenshot.grab(mc,false);System.out.println("CHAIN_SHOT "+ticks+" phase="+phase);}
  if(ticks==119||ticks==209){if(shot!=3||phase>=0){failure=new AssertionError("Missing animation or chain not cleaned");return;}
   mc.getSingleplayerServer().execute(()->{try{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();if(enemy.getHealth()>=100||enemy.getHealth()<85||p.distanceTo(enemy)>=6||!ChainPull.ACTIVE.isEmpty())throw new AssertionError("Damage/pull/cleanup failed: "+enemy.getHealth()+" distance "+p.distanceTo(enemy));System.out.println("CHAIN_SERVER_PASS health="+enemy.getHealth()+" distance="+p.distanceTo(enemy));}catch(Throwable t){failure=t;}});
  }
  if(ticks==235){System.out.println("PAIRED_CLIENT_QA_COMPLETE CHAIN_PASS");mc.stop();}
 });}
}
