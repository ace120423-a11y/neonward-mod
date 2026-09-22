package jp.neonward;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import com.google.gson.JsonObject;
public class ArmsShopScreen extends Screen {
 int page,x,y,w;long cash;String message="";public ArmsShopScreen(){super(Component.literal("BLACK STEEL"));}public boolean isPauseScreen(){return false;}
 void request(int i){if(minecraft.player!=null)minecraft.player.connection.sendCommand("neonarms "+i);}
 protected void init(){w=Math.min(380,width-12);x=(width-w)/2;y=Math.max(4,(height-216)/2);for(int row=0;row<4;row++){int i=page*4+row;if(i>=NeonArsenal.DROPS.size())break;addRenderableWidget(new PhoneScreen.NeonButton(x+w-100,y+46+row*31,88,22,CompactShops.price(i)+" Cr 購入",b->request(i)));}addRenderableWidget(new PhoneScreen.NeonButton(x+110,y+176,112,18,"アタッチメント",b->{if(minecraft.player!=null)minecraft.player.connection.sendCommand("neonattach shop");}));int pages=(NeonArsenal.DROPS.size()+3)/4;addRenderableWidget(new PhoneScreen.NeonButton(x+12,y+176,40,18,"<",b->{page=(page+pages-1)%pages;rebuildWidgets();}));addRenderableWidget(new PhoneScreen.NeonButton(x+58,y+176,40,18,">",b->{page=(page+1)%pages;rebuildWidgets();}));addRenderableWidget(new PhoneScreen.NeonButton(x+w-100,y+176,88,18,"閉じる",b->onClose()));request(-1);}
 void receive(JsonObject o){cash=o.get("cash").getAsLong();message=o.get("message").getAsString();}
 public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){g.fill(x-3,y-3,x+w+3,y+216,0xff111720);g.outline(x-3,y-3,w+6,219,0xffff556f);g.text(font,"BLACK STEEL / 武器販売",x+12,y+10,0xffff8498);g.text(font,"残高 "+cash+" Cr",x+12,y+27,0xffffd080);for(int row=0;row<4;row++){int i=page*4+row;if(i>=NeonArsenal.DROPS.size())break;var item=new ItemStack(NeonArsenal.DROPS.get(i));g.item(item,x+12,y+49+row*31);g.text(font,font.plainSubstrByWidth(item.getHoverName().getString(),w-142),x+34,y+53+row*31,0xffe5eef5);}g.text(font,font.plainSubstrByWidth(message,w-24),x+12,y+201,0xffa8c2cf);super.extractRenderState(g,mx,my,dt);}
}
