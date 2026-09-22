package jp.neonward;
import java.util.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
public class CasinoGachaQA implements ClientModInitializer {
 int ticks,shots;volatile Throwable failure;
 public void onInitializeClient(){ClientTickEvents.END_CLIENT_TICK.register(mc->{
  if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null)return;ticks++;
  if(failure!=null){failure.printStackTrace();mc.stop();return;}
  if(ticks==30){mc.gui.setScreen(null);mc.getSingleplayerServer().execute(()->{try{
   var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();var l=p.level();p.setGameMode(GameType.CREATIVE);
   for(int x=511;x<=521;x++)for(int z=446;z<=453;z++){l.setBlock(new BlockPos(x,64,z),Blocks.STONE.defaultBlockState(),3);for(int y=65;y<=70;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),3);}
   p.teleportTo(l,516,65,451,Set.of(),180,0,true);for(int i=0;i<36;i++)p.getInventory().setItem(i,ItemStack.EMPTY);p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);p.getInventory().setChanged();p.containerMenu.broadcastChanges();StockMarket.ledger.account(p.getStringUUID()).cash=50000;CyberwareGacha.ends=0;WeaponGacha.ends=0;CyberwareGacha.cabinet(l,l.getGameTime());WeaponGacha.cabinet(l,l.getGameTime());
  }catch(Throwable t){failure=t;}});}
  if(ticks<85&&mc.gui.screen()!=null)mc.gui.setScreen(null);
  if(ticks==80)shot(mc,"two separate cabinets");
  if(ticks==90||ticks==205){boolean weapon=ticks==90;mc.getSingleplayerServer().execute(()->CasinoGacha.open(mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst(),weapon));}
  if(ticks==100||ticks==215){boolean weapon=ticks==100;if(!(mc.gui.screen() instanceof CyberwareGachaScreen s)||s.weapon!=weapon||!s.ten.active)throw new AssertionError("Wrong machine or batch button");shot(mc,weapon?"weapon view":"cyber view");}
  if(ticks==105||ticks==220){var s=(CyberwareGachaScreen)mc.gui.screen();s.roll(10);}
  if(ticks==180||ticks==290){boolean weapon=ticks==180;var s=(CyberwareGachaScreen)mc.gui.screen();if(s.weapon!=weapon||s.prizes.size()!=10||s.cash!=(weapon?40000:30000)||s.pending||System.currentTimeMillis()<s.reveal)throw new AssertionError("Ten-roll result/cash mismatch");shot(mc,weapon?"weapon ten results":"cyber ten results");}
  if(ticks==190){((CyberwareGachaScreen)mc.gui.screen()).page=1;}
  if(ticks==195)shot(mc,"weapon result next page");
  if(ticks==200)mc.gui.setScreen(null);
  if(ticks==310){if(shots!=6)throw new AssertionError("Screenshot count");System.out.println("PAIRED_CLIENT_QA_COMPLETE CASINO_GACHA_CLIENT_PASS: independent terminals, both ten-roll buttons, S2C results, pagination, debits");mc.stop();}
 });}
 void shot(Minecraft mc,String label){Screenshot.grab(mc,false);shots++;System.out.println("GACHA_SCREENSHOT "+label);}
}
