package jp.neonward;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import com.google.gson.JsonObject;

public class FashionScreen extends Screen {
 final BlockPos pos;int x,y,w,page;long cash;String message="服は外見専用・防御力なし";
 public FashionScreen(BlockPos p){super(Component.literal("NEON THREADS"));pos=p;}
 @Override public boolean isPauseScreen(){return false;}
 @Override protected void init(){w=Math.min(380,width-12);x=(width-w)/2;y=(height-222)/2;
  for(int i=0;i<4;i++){final int n=i;addRenderableWidget(new PhoneScreen.NeonButton(x+w-103,y+58+i*30,91,23,StreetFashion.price(page*4+i)+" Cr / 購入",b->request(page*4+n)));}
  addRenderableWidget(new PhoneScreen.NeonButton(x+12,y+32,30,18,"<",b->{page=(page+StreetFashion.IDS.length-1)%StreetFashion.IDS.length;rebuildWidgets();}));
  addRenderableWidget(new PhoneScreen.NeonButton(x+w-42,y+32,30,18,">",b->{page=(page+1)%StreetFashion.IDS.length;rebuildWidgets();}));
  addRenderableWidget(new PhoneScreen.NeonButton(x+12,y+196,w-24,18,"閉じる",b->onClose()));request(-1);
 }
 void request(int product){if(minecraft.player!=null)minecraft.player.connection.sendCommand("neonfashion "+pos.getX()+" "+pos.getY()+" "+pos.getZ()+" "+product);}
 void receive(JsonObject o){if(o.has("fashion_cash")){cash=o.get("fashion_cash").getAsLong();message=o.get("message").getAsString();}}
 @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
  g.fill(x-3,y-3,x+w+3,y+225,0xff09101c);g.outline(x-3,y-3,w+6,228,0xfffa65b2);
  g.text(font,"NEON THREADS",x+12,y+12,0xff72ffee);g.text(font,"残高 "+cash+" Cr",x+w-120,y+12,0xffffcd79);
  g.centeredText(font,(page+1)+"/"+StreetFashion.IDS.length+"  "+StreetFashion.NAMES[page],x+w/2,y+36,0xffffffff);
  for(int i=0;i<4;i++){g.item(new ItemStack(StreetFashion.ITEMS.get(page*4+i)),x+15,y+61+i*30);g.text(font,StreetFashion.PARTS[i],x+41,y+66+i*30,0xffd5e6f1);}
  g.text(font,message,x+12,y+182,0xffffc777);super.extractRenderState(g,mx,my,delta);
 }
}
