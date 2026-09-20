package jp.neonward;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import com.google.gson.JsonObject;
public final class InteriorShopScreen extends Screen {
 static int pages(){return (InteriorCatalog.PRODUCTS.length+3)/4;}
 int x,y,w,page;long cash,next;String message="";
 public InteriorShopScreen(){super(Component.literal("NEON NEST / 家具屋"));}
 @Override public boolean isPauseScreen(){return false;}
 @Override protected void init(){w=Math.min(410,width-12);x=(width-w)/2;y=(height-220)/2;
 for(int i=0;i<4&&page*4+i<InteriorCatalog.PRODUCTS.length;i++){final int n=page*4+i;var p=InteriorCatalog.PRODUCTS[n];addRenderableWidget(new PhoneScreen.NeonButton(x+w-102,y+57+i*30,90,23,p.price()+" Cr / 購入",b->{if(System.currentTimeMillis()>next){next=System.currentTimeMillis()+350;minecraft.player.connection.sendCommand("interiors "+n);}}));}
 addRenderableWidget(new PhoneScreen.NeonButton(x+12,y+30,35,18,"<",b->{page=(page+pages()-1)%pages();rebuildWidgets();}));addRenderableWidget(new PhoneScreen.NeonButton(x+w-47,y+30,35,18,">",b->{page=(page+1)%pages();rebuildWidgets();}));addRenderableWidget(new PhoneScreen.NeonButton(x+12,y+197,w-24,18,"閉じる",b->onClose()));}
 void receive(JsonObject o){cash=o.get("cash").getAsLong();message=o.get("message").getAsString();}
 @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){g.fill(x-3,y-3,x+w+3,y+223,0xff0b1920);g.outline(x-3,y-3,w+6,226,0xff6bdebb);g.text(font,"NEON NEST / 家具屋",x+12,y+11,0xff8affe5);g.text(font,cash+" Cr",x+w-110,y+11,0xffffcb77);g.centeredText(font,(page+1)+" / "+pages()+"  全"+InteriorCatalog.PRODUCTS.length+"種類",x+w/2,y+34,0xffdae9de);for(int i=0;i<4&&page*4+i<InteriorCatalog.PRODUCTS.length;i++){var p=InteriorCatalog.PRODUCTS[page*4+i];g.item(new ItemStack(NeonFurniture.BLOCKS.get(p.id())),x+13,y+61+i*30);g.text(font,font.plainSubstrByWidth(p.name(),w-150),x+38,y+65+i*30,0xffe1eee9);}g.text(font,font.plainSubstrByWidth(message,w-24),x+12,y+182,0xffffca7c);super.extractRenderState(g,mx,my,dt);}
}
