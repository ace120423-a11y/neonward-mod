package jp.neonward;
import java.util.Set;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.GameType;
public class GunReloadQA implements ClientModInitializer {
 int ticks,shots,phase;float previous;volatile Throwable failure;
 public void onInitializeClient(){ClientTickEvents.END_CLIENT_TICK.register(mc->{
  if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null)return;
  ticks++;mc.gui.setScreen(null);
  if(failure!=null){failure.printStackTrace();mc.stop();return;}
  if(ticks<40)return;int index=(ticks-40)/130,step=(ticks-40)%130;
  if(index>=11){if(shots!=44)throw new AssertionError("screenshots");System.out.println("PAIRED_CLIENT_QA_COMPLETE GUN_RELOAD_CLIENT_PASS");mc.stop();return;}
  if(step==0)mc.getSingleplayerServer().execute(()->{try{
   var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.CREATIVE);p.stopUsingItem();
   if(index==0)for(int x=3046;x<=3054;x++)for(int z=0;z<=12;z++)p.level().setBlock(new net.minecraft.core.BlockPos(x,69,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
   p.teleportTo(p.level(),3050.5,70,3.5,Set.of(),0,0,true);
   p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(NeonArsenal.ITEMS.get(GunVfx.IDS[index])));p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);p.containerMenu.broadcastChanges();
  }catch(Throwable t){failure=t;}});
  if(step==5){phase=0;previous=0;}
  if(step==10)mc.getSingleplayerServer().execute(()->{try{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();var s=p.getMainHandItem();CustomData.update(DataComponents.CUSTOM_DATA,s,n->n.putInt(GunReload.USED,GunReload.profile(s).capacity()));if(!GunReload.start(p,s,System.currentTimeMillis()))throw new AssertionError("start");}catch(Throwable t){failure=t;}});
  if(step>=11&&step<120){
   var s=mc.player.getMainHandItem();if(!s.is(NeonArsenal.ITEMS.get(GunVfx.IDS[index])))throw new AssertionError("sync "+index);
   float t=GunReload.progress(s);
   if(GunReload.reloading(s)){if(t<previous)throw new AssertionError("reload jumps backward");previous=t;
    if(phase<4&&t>=new float[]{.2f,.43f,.65f,.86f}[phase]){Screenshot.grab(mc,false);shots++;phase++;System.out.println("RELOAD_SCREEN "+GunVfx.IDS[index]+" "+t);}
   }
   if(index>=1&&index<=3&&step==20){
    if(GunReloadMotion.slide(s,.4f)!=1||GunReloadMotion.slide(s,1)!=0)throw new AssertionError("slide lock/release");
    var partial=s.copy();CustomData.update(DataComponents.CUSTOM_DATA,partial,n->n.putInt(GunReload.USED,1));
    if(GunReloadMotion.slide(partial,.4f)!=0||GunReloadMotion.handle(partial,.85f)!=0)throw new AssertionError("unnecessary tactical cocking");
   }
  }
  if(step==120){var s=mc.player.getMainHandItem();if(phase!=4||GunReload.reloading(s)||GunReload.remaining(s)!=GunReload.profile(s).capacity())throw new AssertionError("completion "+index);}
 });}
}
