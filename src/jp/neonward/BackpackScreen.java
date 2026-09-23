package jp.neonward;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

/** Server-owned equipment/storage slots; no client attachment or custom item-moving logic. */
public final class BackpackScreen extends AbstractContainerScreen<BackpackMenu> {
 private static boolean registered;
 public BackpackScreen(BackpackMenu menu,Inventory inventory,Component title){
  super(menu,inventory,title,176,210);inventoryLabelY=116;
 }
 /** Same private vanilla screen-factory bridge as AccessoryScreen (un-widened 26.2 jar). */
 public static void initClient(){
  if(registered)return;
  try{
   var type=Class.forName("net.minecraft.client.gui.screens.MenuScreens$ScreenConstructor");
   var lookup=MethodHandles.privateLookupIn(type,MethodHandles.lookup());
   var factory=Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(proxy,method,args)->{
    if(method.getName().equals("create"))return new BackpackScreen((BackpackMenu)args[0],(Inventory)args[1],(Component)args[2]);
    if(method.isDefault())return lookup.unreflectSpecial(method,type).bindTo(proxy).invokeWithArguments(args);
    return switch(method.getName()){
     case "toString"->"NeonWard backpack screen factory";
     case "hashCode"->System.identityHashCode(proxy);
     case "equals"->proxy==args[0];
     default->throw new UnsupportedOperationException(method.toString());
    };
   });
   var register=MenuScreens.class.getDeclaredMethod("register",MenuType.class,type);
   register.setAccessible(true);register.invoke(null,BackpackEquipment.MENU,factory);registered=true;
  }catch(ReflectiveOperationException ex){throw new IllegalStateException("Backpack screen registration failed",ex);}
 }
 private int capacity(){return Math.clamp(BackpackEquipment.capacity(menu.getSlot(0).getItem()),0,27);}
 private int used(){int count=0;for(int i=1;i<=27;i++)if(!menu.getSlot(i).getItem().isEmpty())count++;return count;}
 @Override public void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta){
  int x=leftPos,y=topPos,capacity=capacity();
  g.fill(x-2,y-2,x+178,y+212,0xff080d12);
  g.fill(x,y,x+176,y+210,0xff192832);g.outline(x,y,176,210,0xff607c86);
  g.fill(x+1,y+1,x+175,y+3,0xffa8c894);
  g.fill(x+6,y+20,x+170,y+45,0xff293d43);
  // Paint the actual menu coordinates, so slot visuals never drift from hit targets.
  for(int i=0;i<menu.slots.size();i++){
   var slot=menu.getSlot(i);int sx=x+slot.x,sy=y+slot.y;
   boolean inactive=i>=1&&i<=27&&i>capacity;
   g.fill(sx-1,sy-1,sx+17,sy+17,i==0?0xffb6d394:inactive?0xff42494e:0xff5b707a);
   g.fill(sx,sy,sx+16,sy+16,inactive?0xff30373c:0xff0c171e);
   if(inactive){
    g.fill(sx+5,sy+7,sx+11,sy+12,0xff5a6268);
    g.outline(sx+6,sy+4,4,5,0xff5a6268);
   }
  }
  super.extractContents(g,mx,my,delta);
 }
 @Override protected void extractLabels(GuiGraphicsExtractor g,int mx,int my){
  g.text(font,"バックパック",8,7,0xffe4eed6,false);
  g.text(font,"装備バッグ",32,29,0xffdce5dd,false);
  g.text(font,"容量 "+capacity()+" 枠",104,29,0xffadc7b7,false);
  g.text(font,"バッグの中身",8,48,0xffd7e2e6,false);
  g.text(font,"使用 "+used()+" 枠",116,48,0xffadc7b7,false);
  g.text(font,playerInventoryTitle,8,inventoryLabelY,0xffd7e2e6,false);
 }
 @Override protected List<Component> getTooltipFromContainerItem(ItemStack stack){
  var lines=new ArrayList<>(super.getTooltipFromContainerItem(stack));int size=BackpackEquipment.capacity(stack);
  if(size>0){
   lines.add(Component.literal("追加収納: "+size+" 枠").withColor(0xa8d8ad));
   lines.add(Component.literal("上の専用枠へ装備して使用").withColor(0xc3d1db));
  }
  return lines;
 }
 @Override protected void extractTooltip(GuiGraphicsExtractor g,int mx,int my){
  super.extractTooltip(g,mx,my);
  if(!menu.getCarried().isEmpty())return;
  for(int i=0;i<=27;i++){
   var slot=menu.getSlot(i);
   if(!slot.getItem().isEmpty()||!isHovering(slot.x,slot.y,16,16,mx,my))continue;
   if(i==0)g.setComponentTooltipForNextFrame(font,List.of(Component.literal("バッグ専用装備枠"),Component.literal("バッグを置くと 9 / 18 / 27 枠の収納を追加")),mx,my);
   else if(i>capacity())g.setComponentTooltipForNextFrame(font,List.of(Component.literal("未開放の収納枠"),Component.literal(capacity()==0?"バッグを装備してください":"より大きいバッグが必要です")),mx,my);
  }
 }
}
