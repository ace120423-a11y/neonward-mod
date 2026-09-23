package jp.neonward;

import java.util.List;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.menu.v1.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Three player-owned, saved accessory slots. Menu contents are never dropped on close/death. */
public final class AccessoryEquipment {
 public static final int SLOTS=3;
 public static final List<AttachmentType<ItemStack>> STORAGE=List.of(slot(0),slot(1),slot(2));
 public static final MenuType<AccessoryMenu> MENU=Registry.register(BuiltInRegistries.MENU,NeonWard.id("accessories"),
   new ExtendedMenuType<AccessoryMenu,Boolean>((id,inv,unused)->new AccessoryMenu(id,inv),ByteBufCodecs.BOOL));
 private static AttachmentType<ItemStack> slot(int n){return AttachmentRegistry.<ItemStack>builder()
   .persistent(ItemStack.OPTIONAL_CODEC).copyOnDeath().syncWith(ItemStack.OPTIONAL_STREAM_CODEC,AttachmentSyncPredicate.all())
   .buildAndRegister(NeonWard.id("accessory_slot_"+n));}
 public static ItemStack get(Player p,int slot){return slot<0||slot>=SLOTS?ItemStack.EMPTY:((AttachmentTarget)p).getAttachedOrElse(STORAGE.get(slot),ItemStack.EMPTY).copy();}
 public static int kind(ItemStack stack){if(stack.isEmpty())return -1;for(int k=0;k<ShrineServices.AMULETS.length;k++)if(ShrineServices.AMULETS[k]!=null&&stack.is(ShrineServices.AMULETS[k]))return k;return -1;}
 public static boolean equipped(Player p,int kind){if(kind<0)return false;for(int i=0;i<SLOTS;i++)if(kind(get(p,i))==kind)return true;return false;}
 public static boolean equip(ServerPlayer p,ItemStack held){
  if(!p.isAlive()||p.isSpectator()||ShrineRituals.active(p)||held!=p.getMainHandItem()||p.containerMenu instanceof AccessoryMenu)return false;
  int kind=kind(held);if(kind<0||equipped(p,kind))return false;
  for(int i=0;i<SLOTS;i++)if(get(p,i).isEmpty()){
   ((AttachmentTarget)p).setAttached(STORAGE.get(i),held.copyWithCount(1));held.shrink(1);refresh(p);
   p.getInventory().setChanged();p.containerMenu.broadcastChanges();
   p.sendOverlayMessage(Component.literal("お守りを装飾品枠に装備しました / インベントリの「装飾品」で取り外し"));return true;
  }
  return false;
 }
 public static float defenseMultiplier(ServerPlayer p){return equipped(p,1)?.8f:1f;}
 private static void modifier(ServerPlayer p,net.minecraft.core.Holder<Attribute> type,String name,double amount,AttributeModifier.Operation operation,boolean enabled){
  var attribute=p.getAttribute(type);if(attribute==null)return;var id=NeonWard.id(name);
  if(enabled){if(!attribute.hasModifier(id))attribute.addTransientModifier(new AttributeModifier(id,amount,operation));}
  else if(attribute.hasModifier(id))attribute.removeModifier(id);
 }
 public static void refresh(ServerPlayer p){
  modifier(p,Attributes.MOVEMENT_SPEED,"accessory_travel",.2,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,equipped(p,0));
  modifier(p,Attributes.LUCK,"accessory_fortune",1,AttributeModifier.Operation.ADD_VALUE,equipped(p,2));
 }
 public static void init(){
  CommandRegistrationCallback.EVENT.register((d,c,e)->d.register(Commands.literal("neonaccessories").executes(ctx->{
   var p=ctx.getSource().getPlayerOrException();if(!p.isAlive()||p.isSpectator()||ShrineRituals.active(p)||p.containerMenu instanceof AccessoryMenu)return 0;
   p.openMenu(new ExtendedMenuProvider<Boolean>(){
    public Boolean getScreenOpeningData(ServerPlayer ignored){return true;}
    public Component getDisplayName(){return Component.literal("装飾品 / お守り");}
    public AbstractContainerMenu createMenu(int id,Inventory inv,Player who){return new AccessoryMenu(id,inv,new EquippedContainer(p));}
   });return 1;
  })));
  ServerPlayConnectionEvents.JOIN.register((h,s,server)->refresh(h.player));
  ServerPlayerEvents.AFTER_RESPAWN.register((old,p,alive)->refresh(p));
  ServerTickEvents.END_SERVER_TICK.register(server->{if(server.getTickCount()%20==0)for(var p:server.getPlayerList().getPlayers())refresh(p);});
 }
 static final class EquippedContainer extends SimpleContainer {
  final ServerPlayer owner;boolean loading=true;
  EquippedContainer(ServerPlayer p){super(SLOTS);owner=p;for(int i=0;i<SLOTS;i++)super.setItem(i,get(p,i));loading=false;}
  @Override public void setChanged(){
   if(loading||owner==null)return;
   for(int i=0;i<SLOTS;i++){var value=getItem(i).copy();if(!ItemStack.matches(get(owner,i),value))((AttachmentTarget)owner).setAttached(STORAGE.get(i),value);}
   refresh(owner);
  }
  @Override public boolean stillValid(Player p){return p==owner&&p.isAlive()&&!p.isSpectator();}
 }
 private AccessoryEquipment(){}
}
