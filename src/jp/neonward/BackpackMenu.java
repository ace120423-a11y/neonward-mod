package jp.neonward;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/** Fixed 64-slot native menu: equipment 0, cargo 1..27, inventory 28..54, hotbar 55..63.
 * Cargo is a live working set. It is written into the SAME equipped bag before that bag can leave.
 * Each completed mutation persists a defensive attachment snapshot; no detached extra inventory.
 */
public final class BackpackMenu extends AbstractContainerMenu {
 public static final int EQUIPMENT_SLOT=0,CARGO_START=1,CARGO_END=28,INVENTORY_START=28,HOTBAR_START=55,SLOT_END=64;
 final Player owner;final ServerPlayer authority;
 final BagContainer equipment;final CargoContainer cargo;
 ItemStack boundBag=ItemStack.EMPTY,lastSaved=ItemStack.EMPTY;
 int activeCapacity;boolean loading=true,closed,writing;
 public BackpackMenu(int id,Inventory inventory){this(id,inventory,null);}
 public BackpackMenu(int id,Inventory inventory,ServerPlayer serverOwner){
  super(BackpackEquipment.MENU,id);owner=inventory.player;authority=serverOwner;
  if(serverOwner!=null&&serverOwner!=owner)throw new IllegalArgumentException("Backpack owner mismatch");
  cargo=new CargoContainer();equipment=new BagContainer();
  if(authority!=null){lastSaved=BackpackEquipment.get(owner);equipment.setItem(0,lastSaved.copy());}
  loading=false;loadBag();
  addSlot(new Slot(equipment,0,8,25){
   @Override public int getMaxStackSize(){return 1;}
   @Override public boolean mayPlace(ItemStack stack){return BackpackEquipment.validBag(stack);}
   @Override public boolean mayPickup(Player p){return stillValid(p);}
  });
  for(int i=0;i<27;i++){
   final int cargoIndex=i;
   addSlot(new Slot(cargo,i,8+18*(i%9),58+18*(i/9)){
    @Override public boolean isActive(){return cargoIndex<activeCapacity;}
    @Override public boolean mayPlace(ItemStack stack){return isActive()&&BackpackEquipment.canStore(stack);}
    @Override public boolean mayPickup(Player p){return isActive()&&stillValid(p);}
   });
  }
  for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,9+row*9+col,8+18*col,128+18*row));
  for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,8+18*col,186));
 }
 public int capacity(){return activeCapacity;}
 /** Invalid external changes invalidate this menu instead of overwriting another session's bag. */
 private boolean current(){return authority==null||ItemStack.matches(BackpackEquipment.get(owner),lastSaved);}
 @Override public boolean stillValid(Player player){return !closed&&player==owner&&BackpackEquipment.permitted(player)
  &&(authority==null||authority.containerMenu==this)&&current();}
 private void loadBag(){
  loading=true;boundBag=equipment.getItem(0);
  activeCapacity=BackpackEquipment.validBag(boundBag)?BackpackEquipment.capacity(boundBag):0;
  var contents=NonNullList.withSize(27,ItemStack.EMPTY);
  if(activeCapacity>0)boundBag.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).copyInto(contents);
  for(int i=0;i<27;i++)cargo.setItem(i,contents.get(i));loading=false;
 }
 /** Called before removing/replacing equipment, as well as after all native cargo mutations. */
 private void flush(){
  if(loading||writing||closed||authority==null||authority.containerMenu!=this||!current()||!BackpackEquipment.permitted(owner))return;
  writing=true;
  try{
   var bag=equipment.getItem(0);
   if(bag==boundBag&&!bag.isEmpty()&&activeCapacity>0){
    var items=NonNullList.withSize(activeCapacity,ItemStack.EMPTY);
    for(int i=0;i<activeCapacity;i++)items.set(i,cargo.getItem(i).copy());
    bag.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(items));
   }
   BackpackEquipment.save(authority,bag);lastSaved=bag.copy();
  }finally{writing=false;}
 }
 private void equipmentChanged(){
  if(loading||closed)return;
  var bag=equipment.getItem(0);
  if(bag!=boundBag||bag.isEmpty()||BackpackEquipment.capacity(bag)!=activeCapacity)loadBag();
  flush();
 }
 final class BagContainer extends SimpleContainer {
  BagContainer(){super(1);}
  @Override public void setItem(int index,ItemStack stack){flush();super.setItem(index,stack);}
  @Override public ItemStack removeItem(int index,int amount){flush();return super.removeItem(index,amount);}
  @Override public ItemStack removeItemNoUpdate(int index){flush();var removed=super.removeItemNoUpdate(index);setChanged();return removed;}
  @Override public void setChanged(){equipmentChanged();}
 }
 final class CargoContainer extends SimpleContainer {
  CargoContainer(){super(27);}
  @Override public void setChanged(){flush();}
  @Override public ItemStack removeItemNoUpdate(int index){var removed=super.removeItemNoUpdate(index);setChanged();return removed;}
 }
 @Override public void clicked(int slot,int button,ContainerInput input,Player player){
  if(!stillValid(player))return;
  if(slot>=CARGO_START&&slot<CARGO_END&&!getSlot(slot).isActive())return;
  player.stopUsingItem();
  flush();super.clicked(slot,button,input,player);equipmentChanged();flush();
 }
 @Override public boolean canDragTo(Slot slot){return slot.isActive()&&super.canDragTo(slot);}
 @Override public boolean canTakeItemForPickAll(ItemStack stack,Slot slot){return slot.isActive()&&super.canTakeItemForPickAll(stack,slot);}
 @Override public ItemStack quickMoveStack(Player player,int index){
  if(!stillValid(player)||index<0||index>=slots.size())return ItemStack.EMPTY;
  var slot=getSlot(index);if(!slot.isActive()||!slot.hasItem()||!slot.mayPickup(player))return ItemStack.EMPTY;
  player.stopUsingItem();
  flush();var stack=slot.getItem();var original=stack.copy();boolean moved;
  if(index<INVENTORY_START)moved=moveItemStackTo(stack,INVENTORY_START,SLOT_END,true);
  else if(BackpackEquipment.validBag(stack))moved=equipment.getItem(0).isEmpty()&&moveItemStackTo(stack,0,1,false);
  else moved=activeCapacity>0&&BackpackEquipment.canStore(stack)&&moveItemStackTo(stack,CARGO_START,CARGO_START+activeCapacity,false);
  if(!moved)return ItemStack.EMPTY;
  if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();
  slot.onTake(player,stack);equipmentChanged();flush();return original;
 }
 @Override public void broadcastChanges(){flush();super.broadcastChanges();}
 @Override public void removed(Player player){flush();closed=true;super.removed(player);}
}
