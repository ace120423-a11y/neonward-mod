package jp.neonward;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.*;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import java.util.*;
public class WeaponMotionQA implements ClientModInitializer {
 int ticks;List<String> ids;volatile Throwable failure;
 public void onInitializeClient(){ClientTickEvents.END_CLIENT_TICK.register(mc->{
  if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null)return;
  ticks++;mc.gui.setScreen(null);
  if(failure!=null){failure.printStackTrace();mc.stop();return;}
  if(ids==null){ids=new ArrayList<>(WeaponMotion.PROFILES.keySet());Collections.sort(ids);if(ids.size()!=25)throw new AssertionError("Expected 25 profiles");String focus=System.getProperty("neonward.qa.focus","");if(!focus.isBlank())ids=Arrays.asList(focus.split(","));}
  if(ticks<30)return;int index=(ticks-30)/50,step=(ticks-30)%50;
  if(index>=ids.size()){System.out.println("PAIRED_CLIENT_QA_COMPLETE WEAPON_MOTION_PASS count="+ids.size());mc.stop();return;}
  String id=ids.get(index);
  if(step>=20&&step<36)mc.options.keyUse.setDown(true);
  if(step==0){mc.options.keyUse.setDown(false);mc.options.setCameraType(CameraType.FIRST_PERSON);mc.player.stopUsingItem();
   mc.getSingleplayerServer().execute(()->{try{
    var server=mc.getSingleplayerServer();var p=server.getPlayerList().getPlayers().getFirst();var level=server.overworld();
    if(index==0)for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++){level.setBlock(new BlockPos(x,63,z),Blocks.QUARTZ_BLOCK.defaultBlockState(),3);for(int y=64;y<70;y++)level.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),3);}
    p.stopUsingItem();p.setGameMode(GameType.CREATIVE);p.teleportTo(level,.5,64,.5,Set.of(),0,0,true);
    for(int i=0;i<36;i++)p.getInventory().setItem(i,ItemStack.EMPTY);
    p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(NeonArsenal.ITEMS.get(id)));p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);
    p.getInventory().setChanged();p.containerMenu.broadcastChanges();
   }catch(Throwable t){failure=t;}});
  }
  if(step==15)shot(mc,id+"-first-rest");
  if(step==20){mc.options.keyUse.setDown(true);mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);}
  if(step==25)shot(mc,id+"-first-use");
  if(step==28)mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
  if(step==33){if(id.equals("neon_dualblades")&&!DualGuard.guarding(mc.player))throw new AssertionError("Dual guard must remain active while key held");shot(mc,id+"-third-use");}
  if(step==36){mc.options.keyUse.setDown(false);mc.player.stopUsingItem();mc.getSingleplayerServer().execute(()->mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst().stopUsingItem());}
  if(step==39){mc.player.swing(InteractionHand.MAIN_HAND);}
  if(step==42)shot(mc,id+"-third-attack");
 });}
 void shot(Minecraft mc,String name){
  var renderer=mc.getEntityRenderDispatcher().getRenderer(mc.player);
  if(renderer instanceof net.minecraft.client.renderer.entity.LivingEntityRenderer<?,?,?> living&&living.getModel() instanceof net.minecraft.client.model.HumanoidModel<?> model)
   System.out.println("POSE_PROBE "+name+" right="+model.rightArm.xRot+" left="+model.leftArm.xRot+" off="+mc.player.getOffhandItem());
  Screenshot.grab(mc,false);System.out.println("WEAPON_MOTION_SHOT "+name);
 }
}
