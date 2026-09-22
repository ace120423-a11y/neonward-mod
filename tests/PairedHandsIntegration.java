package jp.neonward;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
public final class PairedHandsIntegration {
 static void check(boolean value,String message){if(!value)throw new AssertionError("PAIRED: "+message);}
 static int count(ServerPlayer p,Item item){int n=0;for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).is(item))n+=p.getInventory().getItem(i).getCount();return n;}
 static void clear(ServerPlayer p){for(int i=0;i<36;i++)p.getInventory().setItem(i,ItemStack.EMPTY);p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);}
 public static void run(ServerPlayer p){
  var backup=Cyberware.inventory(p);
  try{
   for(String id:new String[]{"neon_dualblades","impact_gauntlet"}){
    clear(p);var weapon=new ItemStack(NeonArsenal.ITEMS.get(id));var shield=new ItemStack(Items.SHIELD);shield.setDamageValue(17);shield.set(DataComponents.CUSTOM_NAME,Component.literal("Keep me"));
    p.setItemSlot(EquipmentSlot.MAINHAND,weapon);p.setItemSlot(EquipmentSlot.OFFHAND,shield);
    check(PairedHands.enforce(p)&&p.getOffhandItem().isEmpty(),"offhand relocated for "+id);
    check(count(p,Items.SHIELD)==1&&count(p,weapon.getItem())==1,"one shield and one pair");
    check(shield.getDamageValue()==17&&shield.getHoverName().getString().equals("Keep me"),"components preserved");
    for(int i=0;i<20;i++)PairedHands.enforce(p);
    check(count(p,weapon.getItem())==1&&count(p,Items.SHIELD)==1,"no tick duplication");
    p.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.APPLE,32));PairedHands.enforce(p);
    check(count(p,Items.APPLE)==32&&p.getOffhandItem().isEmpty(),"non-shield offhand preserved");
    for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.STONE,64));
    p.setItemSlot(EquipmentSlot.MAINHAND,weapon);p.setItemSlot(EquipmentSlot.OFFHAND,shield);PairedHands.enforce(p);
    check(p.getMainHandItem()==shield&&p.getOffhandItem()==weapon,"full inventory safely refuses pair");
    check(!PairedHands.enforce(p),"full inventory stable, no swap loop");
    p.setItemSlot(EquipmentSlot.MAINHAND,weapon);p.setItemSlot(EquipmentSlot.OFFHAND,weapon.copy());
    check(!PairedHands.enforce(p)&&count(p,weapon.getItem())==2,"two real pairs with full inventory do not swap forever");
    clear(p);p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_SWORD));p.setItemSlot(EquipmentSlot.OFFHAND,shield);
    check(!PairedHands.enforce(p)&&p.getOffhandItem()==shield,"ordinary weapons unchanged");
   }
   clear(p);var glove=new ItemStack(NeonArsenal.ITEMS.get("impact_gauntlet"));p.setItemSlot(EquipmentSlot.MAINHAND,glove);
   check(MeleeGuard.multiplier(p,p.damageSources().generic())==.5f,"gauntlet passive 50 percent reduction");
   p.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
   check(MeleeGuard.multiplier(p,p.damageSources().generic())==.5f,"charging stays 50 percent, no double reduction");p.stopUsingItem();
   check(MeleeGuard.multiplier(p,p.damageSources().fellOutOfWorld())==1f,"void damage exception preserved");
   p.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);p.setItemSlot(EquipmentSlot.OFFHAND,glove);
   check(MeleeGuard.multiplier(p,p.damageSources().generic())==1f,"offhand alone does not grant protection");
   p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);p.getInventory().setItem(10,glove);
   check(MeleeGuard.multiplier(p,p.damageSources().generic())==1f,"inventory alone does not grant protection");
   p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(NeonArsenal.ITEMS.get("neon_dualblades")));
   check(MeleeGuard.multiplier(p,p.damageSources().generic())==.5f,"dual blades passive 50 percent reduction");
   var dual=p.getMainHandItem();p.setItemSlot(EquipmentSlot.OFFHAND,dual.copy());
   check(MeleeGuard.multiplier(p,p.damageSources().generic())==.5f,"two real dual blade sets cannot stack protection");
   p.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
   check(MeleeGuard.multiplier(p,p.damageSources().generic())==1f,"dual blades offhand alone do not protect");
   p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);p.getInventory().setItem(11,dual);
   check(MeleeGuard.multiplier(p,p.damageSources().generic())==1f,"dual blades inventory alone do not protect");
   p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(NeonArsenal.ITEMS.get("coil_hammer")));
   check(MeleeGuard.multiplier(p,p.damageSources().generic())==.55f,"hammer passive unchanged");
   p.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
   check(MeleeGuard.multiplier(p,p.damageSources().generic())==.5f,"hammer guard unchanged");p.stopUsingItem();
   System.out.println("PAIRED_DEFENSE_QA_PASS: gauntlet and dual blades passive 50%, charging, no stacking, void exclusion, offhand/inventory exclusion, hammer unchanged");
   System.out.println("PAIRED_HANDS_QA_PASS: both weapon types, relocation, metadata, no duplication, non-shield, full inventory, ordinary weapon");
  }finally{Cyberware.restore(p,backup);}
 }
}
