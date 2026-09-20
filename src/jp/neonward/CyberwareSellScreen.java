package jp.neonward;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public final class CyberwareSellScreen extends CyberwareScreen {
 int x,y,w,h,page;int[] counts=new int[CyberwareCatalog.PARTS.length];String message="インベントリを確認中…";boolean requested;
 public CyberwareSellScreen(BlockPos p){super(p);}
 @Override public boolean isPauseScreen(){return false;}
 int[] available(){var a=new java.util.ArrayList<Integer>();for(int i=0;i<counts.length;i++)if(counts[i]>0)a.add(i);return a.stream().mapToInt(Integer::intValue).toArray();}
 int pages(){return Math.max(1,(available().length+4)/5);}
 @Override protected void init(){w=Math.min(520,width-12);h=Math.min(270,height-12);x=(width-w)/2;y=(height-h)/2;int[] list=available();int start=page*5;for(int row=0;row<5;row++){int n=start+row;if(n>=list.length)break;int id=list[n],yy=y+38+row*31;var b=new PhoneScreen.NeonButton(x+w-105,yy,92,22,"売却",bt->request(id));b.active=counts[id]>0;addRenderableWidget(b);}addRenderableWidget(new PhoneScreen.NeonButton(x+12,y+h-47,35,18,"<",b->{page=(page+pages()-1)%pages();rebuildWidgets();}));addRenderableWidget(new PhoneScreen.NeonButton(x+52,y+h-47,35,18,">",b->{page=(page+1)%pages();rebuildWidgets();}));addRenderableWidget(new PhoneScreen.NeonButton(x+w-82,y+h-47,70,18,"閉じる",b->onClose()));if(!requested){requested=true;request(-1);}}
 void request(int id){if(minecraft.player!=null)minecraft.player.connection.sendCommand("neoncyber sell "+pos.getX()+" "+pos.getY()+" "+pos.getZ()+" "+id);}
 void receive(JsonObject o){if(!o.has("cyberware")||o.get("x").getAsInt()!=pos.getX()||o.get("y").getAsInt()!=pos.getY()||o.get("z").getAsInt()!=pos.getZ())return;if(o.has("inventory"))counts=StockMarket.JSON.fromJson(o.get("inventory"),int[].class);message=o.get("message").getAsString();page=Math.min(page,pages()-1);rebuildWidgets();}
 @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){g.fill(x-3,y-3,x+w+3,y+h+3,0xff08121d);g.outline(x-3,y-3,w+6,h+6,0xff5ff5ec);g.text(font,"NEON GUILD / サイバーウェア換金",x+12,y+10,0xff73fff0);g.text(font,"インベントリから売るものを選択",x+12,y+25,0xff9ab8c4);int[] list=available();for(int row=0;row<5;row++){int n=page*5+row;if(n>=list.length)break;int id=list[n],yy=y+38+row*31;var part=CyberwareCatalog.PARTS[id];g.item(new net.minecraft.world.item.ItemStack(Cyberware.ITEMS.get(id)),x+12,yy+2);g.text(font,part.name()+" / "+CyberwareCatalog.RARITIES[part.tier()],x+38,yy+1,CyberwareCatalog.COLORS[part.tier()]);g.text(font,"所持 "+counts[id]+"個 / "+Cyberware.SELL_PRICES[part.tier()]+" Cr",x+38,yy+14,0xff8cbaca);}g.text(font,(page+1)+" / "+pages(),x+w-65,y+25,0xff91adb9);g.text(font,font.plainSubstrByWidth(message,w-24),x+12,y+h-20,0xffffc67d);super.extractRenderState(g,mx,my,dt);}
}
