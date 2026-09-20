package jp.neonward;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
public class VendingScreen extends Screen {
 final BlockPos pos;int x,y,w,h;
 public VendingScreen(BlockPos p){super(Component.literal("PULSE MART"));pos=p;}
 @Override public boolean isPauseScreen(){return false;}
 @Override protected void init(){w=Math.min(270,width-20);h=190;x=(width-w)/2;y=(height-h)/2;
  for(int i=0;i<3;i++){final int product=i;addRenderableWidget(Button.builder(Component.literal(VendingMachines.NAMES[i]+"  /  "+VendingMachines.PRICES[i]+" Crで購入"),b->{if(minecraft.player!=null)minecraft.player.connection.sendCommand("neonvend "+pos.getX()+" "+pos.getY()+" "+pos.getZ()+" "+product);onClose();}).bounds(x+12,y+56+i*32,w-24,25).build());}
  addRenderableWidget(Button.builder(Component.literal("閉じる"),b->onClose()).bounds(x+12,y+h-28,w-24,20).build());
 }
 @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float d){g.fill(x-3,y-3,x+w+3,y+h+3,0xff08131e);g.outline(x-3,y-3,w+6,h+6,0xff43ffe4);g.centeredText(font,"PULSE MART / 自動販売機",x+w/2,y+12,0xffff65b6);g.centeredText(font,"街通貨 Cr で購入",x+w/2,y+32,0xff81fff0);super.extractRenderState(g,mx,my,d);}
}
