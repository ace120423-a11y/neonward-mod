package jp.neonward;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Standard server-authoritative inventory clicks, including drag, swap and quick-move. */
public final class AccessoryMenu extends AbstractContainerMenu {
 final Container equipment;final Player owner;
 public AccessoryMenu(int id,Inventory inv){this(id,inv,new SimpleContainer(AccessoryEquipment.SLOTS));}
 public AccessoryMenu(int id,Inventory inv,Container equipment){
  super(AccessoryEquipment.MENU,id);checkContainerSize(equipment,AccessoryEquipment.SLOTS);this.equipment=equipment;owner=inv.player;
  for(int i=0;i<AccessoryEquipment.SLOTS;i++){
   final int target=i;
   addSlot(new Slot(equipment,i,62+i*18,25){
    @Override public int getMaxStackSize(){return 1;}
    @Override public boolean mayPlace(ItemStack stack){
     int kind=AccessoryEquipment.kind(stack);if(kind<0)return false;
     for(int j=0;j<AccessoryEquipment.SLOTS;j++)if(j!=target&&AccessoryEquipment.kind(equipment.getItem(j))==kind)return false;
     return true;
    }
   });
  }
  for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inv,9+row*9+col,8+col*18,75+row*18));
  for(int col=0;col<9;col++)addSlot(new Slot(inv,col,8+col*18,133));
 }
 @Override public boolean stillValid(Player p){return owner==p&&p.isAlive()&&!p.isSpectator()&&equipment.stillValid(p);}
 @Override public ItemStack quickMoveStack(Player p,int index){
  if(!stillValid(p)||index<0||index>=slots.size())return ItemStack.EMPTY;
  var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;
  var stack=slot.getItem();var original=stack.copy();
  if(index<AccessoryEquipment.SLOTS){if(!moveItemStackTo(stack,3,39,true))return ItemStack.EMPTY;}
  else if(!moveItemStackTo(stack,0,3,false))return ItemStack.EMPTY;
  if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();
  slot.onTake(p,stack);equipment.setChanged();return original;
 }
 // Only a cursor item is returned by vanilla close handling; equipped stacks stay saved on the owner.
}
