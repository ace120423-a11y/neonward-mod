package jp.neonward;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;

/** A pair is one real item. The second hand is rendered, never minted as loot. */
public final class PairedHands {
 public static boolean paired(ItemStack s){return !s.isEmpty()&&(s.is(NeonArsenal.ITEMS.get("neon_dualblades"))||gauntlet(s));}
 public static boolean gauntlet(ItemStack s){return !s.isEmpty()&&s.is(NeonArsenal.ITEMS.get("impact_gauntlet"));}
 public static boolean active(Player p){return !p.isSpectator()&&paired(p.getMainHandItem());}
 static boolean enforce(ServerPlayer p){
  if(!active(p)||p.getOffhandItem().isEmpty())return false;
  var off=p.getOffhandItem();var inv=p.getInventory();int free=-1;
  for(int i=0;i<36;i++)if(inv.getItem(i).isEmpty()){free=i;break;}
  if(free<0&&paired(off))return false; // Two real pairs cannot enter a full-inventory swap loop.
  p.stopUsingItem();
  if(free>=0){inv.setItem(free,off);p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);}
  else{
   // No dropping, partial inserts or hidden storage: leave the pair unequipped.
   var main=p.getMainHandItem();p.setItemSlot(EquipmentSlot.MAINHAND,off);p.setItemSlot(EquipmentSlot.OFFHAND,main);
   p.sendOverlayMessage(Component.literal("両手装備には持ち物の空きが1枠必要です（アイテムは保持）"));
  }
  inv.setChanged();p.inventoryMenu.broadcastChanges();if(p.containerMenu!=p.inventoryMenu)p.containerMenu.broadcastChanges();return true;
 }
 public static void init(){
  ServerTickEvents.END_SERVER_TICK.register(s->{for(var p:s.getPlayerList().getPlayers())enforce(p);});
  UseItemCallback.EVENT.register((p,l,h)->h==InteractionHand.OFF_HAND&&active(p)?InteractionResult.FAIL:InteractionResult.PASS);
 }
}
