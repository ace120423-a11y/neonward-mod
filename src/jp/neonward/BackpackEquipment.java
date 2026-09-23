package jp.neonward;

import java.util.*;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.menu.v1.*;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;

/** One saved equipment item, carrying its own cargo. Never enlarges the vanilla Inventory array. */
public final class BackpackEquipment {
 public static final String[] IDS={"backpack_small","backpack_medium","backpack_large"};
 public static final Item[] ITEMS=new Item[3];
 public static final int MAX_CAPACITY=27;
 public static final AttachmentType<ItemStack> STORAGE=AttachmentRegistry.<ItemStack>builder()
  .persistent(ItemStack.OPTIONAL_CODEC).copyOnDeath()
  .buildAndRegister(NeonWard.id("backpack_equipment"));
 public static final MenuType<BackpackMenu> MENU=Registry.register(BuiltInRegistries.MENU,NeonWard.id("backpack"),
  new ExtendedMenuType<BackpackMenu,Boolean>((id,inv,unused)->new BackpackMenu(id,inv),ByteBufCodecs.BOOL));
 private static boolean initialized;
 private BackpackEquipment(){}
 public static int capacity(ItemStack stack){
  if(stack==null||stack.isEmpty())return 0;
  for(int i=0;i<ITEMS.length;i++)if(ITEMS[i]!=null&&stack.is(ITEMS[i]))return (i+1)*9;
  return 0;
 }
 /** Defensive public snapshot. Menu containers themselves return live stacks for vanilla mutations. */
 public static ItemStack get(Player player){return ((AttachmentTarget)player).getAttachedOrElse(STORAGE,ItemStack.EMPTY).copy();}
 static void save(ServerPlayer player,ItemStack stack){
  var before=((AttachmentTarget)player).getAttachedOrElse(STORAGE,ItemStack.EMPTY);
  if(!ItemStack.matches(before,stack))((AttachmentTarget)player).setAttached(STORAGE,stack.copy());
 }
 /** Ordinary building items/fish buckets are allowed; actual storage and nested bags are not. */
 public static boolean canStore(ItemStack stack){return canStore(stack,0);}
 private static boolean canStore(ItemStack stack,int depth){
  if(stack.isEmpty())return true;
  if(depth>2||capacity(stack)>0||!stack.getItem().canFitInsideContainerItems()
   ||stack.has(DataComponents.BUNDLE_CONTENTS)
   ||stack.has(DataComponents.CONTAINER_LOOT)||stack.has(DataComponents.BLOCK_ENTITY_DATA)||stack.has(DataComponents.ENTITY_DATA))return false;
  // 26.2 gives ordinary empty chests/barrels a CONTAINER component too.
  var container=stack.get(DataComponents.CONTAINER);
  if(container!=null&&container.nonEmptyItems().iterator().hasNext())return false;
  // Permit ordinary loaded crossbows and consumables, but inspect their item-bearing payloads.
  var projectiles=stack.get(DataComponents.CHARGED_PROJECTILES);
  if(projectiles!=null){if(projectiles.items().size()>3)return false;for(var item:projectiles.itemCopies())if(!canStore(item,depth+1))return false;}
  var remainder=stack.get(DataComponents.USE_REMAINDER);
  if(remainder!=null&&!canStore(remainder.convertInto().create(),depth+1))return false;
  var sulfur=stack.get(DataComponents.SULFUR_CUBE_CONTENT);
  return sulfur==null||canStore(sulfur.absorbedBlockItemStack().create(),depth+1);
 }
 /** Reject malformed/over-capacity bags instead of truncating or silently deleting their contents. */
 public static boolean validBag(ItemStack stack){
  int count=capacity(stack);if(count==0||stack.getCount()!=1)return false;
  var items=stack.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).allItemsCopyStream().limit(MAX_CAPACITY+1L).toList();
  if(items.size()>count)return false;
  for(var item:items)if(!canStore(item)||!item.isEmpty()&&item.getCount()>item.getMaxStackSize())return false;
  return true;
 }
 public static boolean permitted(Player player){return player.isAlive()&&!player.isSpectator()&&(!(player instanceof ServerPlayer p)||!p.hasDisconnected()&&!ShrineRituals.active(p));}
 public static int open(ServerPlayer player){
  if(!permitted(player)||player.containerMenu instanceof BackpackMenu)return 0;
  player.stopUsingItem(); // Do not let a consumable/use-item reference survive an inventory transfer.
  player.openMenu(new ExtendedMenuProvider<Boolean>(){
   public Boolean getScreenOpeningData(ServerPlayer p){return true;}
   public Component getDisplayName(){return Component.literal("バックパック");}
   public AbstractContainerMenu createMenu(int id,Inventory inv,Player p){return new BackpackMenu(id,inv,player);}
  });return 1;
 }
 public static void init(){
  if(initialized)return;initialized=true;
  for(int i=0;i<ITEMS.length;i++){
   var id=NeonWard.id(IDS[i]);
   ITEMS[i]=Registry.register(BuiltInRegistries.ITEM,id,new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id)).stacksTo(1).component(DataComponents.CONTAINER,ItemContainerContents.EMPTY)){
    @Override public boolean canFitInsideContainerItems(){return false;}
   });
  }
  CommandRegistrationCallback.EVENT.register((d,c,e)->d.register(Commands.literal("neonbackpack").executes(ctx->open(ctx.getSource().getPlayerOrException()))));
 }
}
