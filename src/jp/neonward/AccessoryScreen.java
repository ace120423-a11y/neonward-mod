package jp.neonward;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Proxy;
import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

/** Vanilla container interaction; only the background, labels and tooltips are custom. */
public final class AccessoryScreen extends AbstractContainerScreen<AccessoryMenu> {
 private static final String[] EFFECTS={"移動速度 +20%","受けるダメージ -20%","幸運 +1"};
 private static final String[] SHORT={"速 +20%","守 -20%","運 +1"};
 private static boolean registered;
 public AccessoryScreen(AccessoryMenu menu,Inventory inventory,Component title){super(menu,inventory,title,176,157);inventoryLabelY=65;}
 /** Plain javac uses the un-widened 26.2 jar. Invoke vanilla's private registry once. */
 public static void initClient(){
  if(registered)return;
  try{
   var type=Class.forName("net.minecraft.client.gui.screens.MenuScreens$ScreenConstructor");
   var lookup=MethodHandles.privateLookupIn(type,MethodHandles.lookup());
   var factory=Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(proxy,method,args)->{
    if(method.getName().equals("create"))return new AccessoryScreen((AccessoryMenu)args[0],(Inventory)args[1],(Component)args[2]);
    if(method.isDefault())return lookup.unreflectSpecial(method,type).bindTo(proxy).invokeWithArguments(args);
    return switch(method.getName()){case "toString"->"NeonWard accessory screen factory";case "hashCode"->System.identityHashCode(proxy);case "equals"->proxy==args[0];default->throw new UnsupportedOperationException(method.toString());};
   });
   var register=MenuScreens.class.getDeclaredMethod("register",MenuType.class,type);register.setAccessible(true);register.invoke(null,AccessoryEquipment.MENU,factory);registered=true;
  }catch(ReflectiveOperationException ex){throw new IllegalStateException("Accessory screen registration failed",ex);}
 }
 private static int kind(ItemStack stack){for(int i=0;i<ShrineServices.AMULETS.length;i++)if(stack.is(ShrineServices.AMULETS[i]))return i;return -1;}
 private boolean equipped(int kind){for(int slot=0;slot<AccessoryEquipment.SLOTS;slot++)if(kind(menu.getSlot(slot).getItem())==kind)return true;return false;}
 private void slot(GuiGraphicsExtractor g,int x,int y,boolean accessory){
  int border=accessory?0xffcbb17c:0xff536571;
  g.fill(x-1,y-1,x+17,y+17,border);g.fill(x,y,x+16,y+16,0xff0c141c);g.fill(x,y,x+16,y+1,0xff05090e);
 }
 @Override public void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta){
  int x=leftPos,y=topPos;
  g.fill(x-2,y-2,x+178,y+159,0xff060c12);g.fill(x,y,x+176,y+157,0xff172530);g.outline(x,y,176,157,0xff607c86);
  g.fill(x+1,y+1,x+175,y+3,0xffd4b374);g.fill(x+6,y+20,x+170,y+44,0xff223540);
  for(int i=0;i<AccessoryEquipment.SLOTS;i++)slot(g,x+62+18*i,y+25,true);
  for(int row=0;row<3;row++)for(int col=0;col<9;col++)slot(g,x+8+18*col,y+75+18*row,false);
  for(int col=0;col<9;col++)slot(g,x+8+18*col,y+133,false);
  super.extractContents(g,mx,my,delta);
 }
 @Override protected void extractLabels(GuiGraphicsExtractor g,int mx,int my){
  g.text(font,"装飾品",8,7,0xffffdfaa,false);g.text(font,"装備",15,29,0xffd2dedf,false);
  for(int i=0;i<AccessoryEquipment.SLOTS;i++)g.centeredText(font,Integer.toString(i+1),70+18*i,15,0xffb6c6cc);
  g.text(font,"装備中のみ有効",8,45,0xffb6c6cc,false);
  for(int i=0;i<3;i++)g.text(font,SHORT[i],8+i*55,55,equipped(i)?0xff9aefca:0xff8c9aa4,false);
  g.text(font,playerInventoryTitle,8,65,0xffd7e2e6,false);
 }
 @Override protected List<Component> getTooltipFromContainerItem(ItemStack stack){
  var lines=new ArrayList<>(super.getTooltipFromContainerItem(stack));int kind=kind(stack);
  if(kind>=0){lines.add(Component.literal(EFFECTS[kind]).withColor(0x9aefca));lines.add(Component.literal("装飾品スロットに装備中、継続して有効").withColor(0xd4b374));}
  return lines;
 }
 @Override protected void extractTooltip(GuiGraphicsExtractor g,int mx,int my){
  super.extractTooltip(g,mx,my);
  if(!menu.getCarried().isEmpty())return;
  for(int i=0;i<3;i++)if(isHovering(8+i*55,54,53,10,mx,my))g.setComponentTooltipForNextFrame(font,List.of(Component.literal(EFFECTS[i]),Component.literal(equipped(i)?"装備中 / 有効":"未装備 / 無効")),mx,my);
  for(int i=0;i<AccessoryEquipment.SLOTS;i++)if(menu.getSlot(i).getItem().isEmpty()&&isHovering(62+18*i,25,16,16,mx,my))g.setComponentTooltipForNextFrame(font,List.of(Component.literal("装飾品スロット "+(i+1)),Component.literal("お守りを置いて装備 / 取り出して解除")),mx,my);
 }
}
