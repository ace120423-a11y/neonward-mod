package jp.neonward;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
public final class WeaponSellScreen extends Screen {
 final BlockPos pos;int x,y,token=-1;long price;String message="持ち物の武器を選んでください",name="";PhoneScreen.NeonButton confirm;
 WeaponSellScreen(BlockPos pos){super(Component.literal("武器売却"));this.pos=pos;}
 public boolean isPauseScreen(){return false;}
 public void onClose(){minecraft.gui.setScreen(new GuildScreen(pos,true));}
 protected void init(){x=(width-300)/2;y=(height-224)/2;
  for(int row=0;row<4;row++)for(int col=0;col<9;col++){int slot=row==3?col:9+row*9+col,xx=x+24+col*28,yy=y+42+row*26;addRenderableWidget(new PhoneScreen.NeonButton(xx,yy,24,24,"",b->{if(minecraft.player==null)return;token=-1;confirm.active=false;minecraft.player.connection.sendCommand("neonweaponsale quote "+pos.getX()+" "+pos.getY()+" "+pos.getZ()+" "+slot);}));}
  confirm=addRenderableWidget(new PhoneScreen.NeonButton(x+12,y+177,276,20,"選んだ1本を売却",b->{if(token<0||minecraft.player==null)return;int t=token;token=-1;confirm.active=false;minecraft.player.connection.sendCommand("neonweaponsale confirm "+t);}));confirm.active=token>=0;
  addRenderableWidget(new PhoneScreen.NeonButton(x+12,y+201,276,18,"換金所へ戻る",b->onClose()));
 }
 void receive(JsonObject o){token=o.get("token").getAsInt();price=o.get("price").getAsLong();name=o.get("name").getAsString();message=o.get("message").getAsString();confirm.active=token>=0;}
 public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){g.fill(x-3,y-3,x+303,y+225,0xff08121d);g.outline(x-3,y-3,306,228,0xff5ff5ec);g.text(font,"NEON GUILD / 武器売却",x+12,y+8,0xff73fff0);g.text(font,"持ち物から1本選択 → 金額確認 → 売却",x+12,y+24,0xffa8c2cf);super.extractRenderState(g,mx,my,dt);
  if(minecraft.player!=null)for(int row=0;row<4;row++)for(int col=0;col<9;col++){int slot=row==3?col:9+row*9+col,xx=x+24+col*28,yy=y+42+row*26;var stack=minecraft.player.getInventory().getItem(slot);if(!stack.isEmpty())g.item(stack,xx+4,yy+4);if(WeaponSales.price(stack)>0)g.outline(xx,yy,24,24,0xff6bf7ce);}
  g.text(font,font.plainSubstrByWidth(name.isEmpty()?message:name+" / "+price+" Cr",276),x+12,y+149,0xffffd071);if(!name.isEmpty())g.text(font,font.plainSubstrByWidth(message,276),x+12,y+162,0xffa8c2cf);
 }
}
