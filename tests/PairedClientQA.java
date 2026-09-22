package jp.neonward;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.GameType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import java.util.Set;
public class PairedClientQA implements ClientModInitializer {
 int ticks;
 volatile Throwable failure;
 public void onInitializeClient(){ClientTickEvents.END_CLIENT_TICK.register(mc->{
  if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null)return;
  ticks++;mc.gui.setScreen(null);
  if(failure!=null){failure.printStackTrace();mc.stop();return;}
  if(ticks==30||ticks==180){
   boolean glove=ticks==30;
   mc.getSingleplayerServer().execute(()->{
    var server=mc.getSingleplayerServer();var p=server.getPlayerList().getPlayers().getFirst();var level=server.overworld();
    for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++){level.setBlock(new BlockPos(x,63,z),Blocks.QUARTZ_BLOCK.defaultBlockState(),3);for(int y=64;y<70;y++)level.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),3);}
    p.setGameMode(GameType.CREATIVE);p.teleportTo(level,.5,64,.5,Set.of(),0,0,true);
    for(int i=0;i<36;i++)p.getInventory().setItem(i,ItemStack.EMPTY);
    p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(NeonArsenal.ITEMS.get(glove?"impact_gauntlet":"neon_dualblades")));p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);
    p.getInventory().setChanged();p.containerMenu.broadcastChanges();
   });mc.options.setCameraType(CameraType.FIRST_PERSON);
  }
  if(ticks==88||ticks==118||ticks==143||ticks==158||ticks==238||ticks==268||ticks==293||ticks==308){
   boolean left=ticks==118||ticks==158||ticks==268||ticks==308;
   try{
    var attack=Minecraft.class.getDeclaredMethod("startAttack");attack.setAccessible(true);attack.invoke(mc);
    var expected=left?net.minecraft.world.InteractionHand.OFF_HAND:net.minecraft.world.InteractionHand.MAIN_HAND;
    if(mc.player.swingingArm!=expected)throw new AssertionError("Wrong alternating hand at "+ticks);
    // Check an ignored click without the vanilla negative swingTime restart window.
    mc.player.swingTime=0;attack.invoke(mc);
    if(mc.player.swingingArm!=expected)throw new AssertionError("Rapid click changed hand");
    final int at=ticks;
    mc.getSingleplayerServer().execute(()->{
     // The packet is queued before this task; verify separately after several ticks below.
     System.out.println("PAIRED_ATTACK_SENT "+at+" "+expected);
    });
   }catch(Throwable t){failure=t;}
  }
  if(ticks==92||ticks==122||ticks==147||ticks==162||ticks==242||ticks==272||ticks==297||ticks==312){
   var expected=(ticks==122||ticks==162||ticks==272||ticks==312)?net.minecraft.world.InteractionHand.OFF_HAND:net.minecraft.world.InteractionHand.MAIN_HAND;
   mc.getSingleplayerServer().execute(()->{try{
    var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
    if(p.swingingArm!=expected)throw new AssertionError("Server animation hand mismatch: "+p.swingingArm+" expected "+expected);
    System.out.println("PAIRED_SERVER_SWING_PASS "+expected);
   }catch(Throwable t){failure=t;}});
  }
  if(ticks==90||ticks==240)shot(mc,ticks==90?"gauntlet-first.png":"dual-first.png");
  if(ticks==105||ticks==255)mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
  if(ticks==145||ticks==295)shot(mc,ticks==145?"gauntlet-third.png":"dual-third.png");
  if(ticks==340){System.out.println("PAIRED_CLIENT_QA_COMPLETE");mc.stop();}
 });}
 void shot(Minecraft mc,String file){Screenshot.grab(mc,false);System.out.println("PAIRED_SCREENSHOT "+file);}
}
