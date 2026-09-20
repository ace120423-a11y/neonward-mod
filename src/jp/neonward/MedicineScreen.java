package jp.neonward;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
public final class MedicineScreen extends Screen {
 final BlockPos pos;final boolean shop;int x,y,w;long cash,next;int herbs,bottles;String message="";
 MedicineScreen(BlockPos p,boolean s){super(Component.literal("MED LAB"));pos=p;shop=s;}
 @Override public boolean isPauseScreen(){return false;}
 @Override protected void init(){w=Math.min(410,width-12);x=(width-w)/2;y=Math.max(3,(height-224)/2);for(int i=0;i<(shop?4:3);i++){final int n=i;String cost=shop?new int[]{120,300,260,12}[i]+" Cr":"薬草"+new int[]{2,5,4}[i]+"＋瓶1";addRenderableWidget(new PhoneScreen.NeonButton(x+w-130,y+54+i*32,118,23,cost+(shop?" 購入":" 調合"),b->{if(System.currentTimeMillis()>next){next=System.currentTimeMillis()+350;minecraft.player.connection.sendCommand("neonmed "+(shop?"buy":"mix")+" "+pos.getX()+" "+pos.getY()+" "+pos.getZ()+" "+n);}}));}addRenderableWidget(new PhoneScreen.NeonButton(x+12,y+197,w-24,20,"閉じる",b->onClose()));}
 void receive(JsonObject o){cash=o.get("cash").getAsLong();herbs=o.get("herbs").getAsInt();bottles=o.get("bottles").getAsInt();message=o.get("message").getAsString();}
 @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){g.fill(x-3,y-3,x+w+3,y+223,0xff0c1e23);g.outline(x-3,y-3,w+6,226,0xff5ce5b0);g.text(font,"MED LAB / "+(shop?"薬品販売":"回復アイテム調合"),x+12,y+11,0xff80ffd0);g.text(font,cash+" Cr / 薬草 "+herbs+" / 空瓶 "+bottles,x+12,y+32,0xffffd081);String[] effect={"HPを4回復","HPを8回復","45秒かけて徐々に回復","調合に使える空き瓶"};for(int i=0;i<(shop?4:3);i++){g.item(Medicine.product(i),x+12,y+56+i*32);g.text(font,font.plainSubstrByWidth(Medicine.NAMES[i],w-172),x+35,y+56+i*32,0xffeef7f5);g.text(font,effect[i],x+35,y+69+i*32,0xff87b7aa);}g.text(font,font.plainSubstrByWidth(message,w-24),x+12,y+183,0xffffcb80);super.extractRenderState(g,mx,my,dt);}
}
