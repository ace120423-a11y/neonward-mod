package jp.neonward;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
public class PhoneTravelScreen extends Screen {
 int page,x,y,w,h,rows;boolean sent;
 public PhoneTravelScreen(){super(Component.literal("NEON TRAVEL"));}
 @Override public boolean isPauseScreen(){return false;}
 @Override public void onClose(){minecraft.gui.setScreen(new CityMapScreen());}
 @Override protected void init(){w=Math.min(340,width-16);h=Math.min(360,height-16);x=(width-w)/2;y=(height-h)/2;rows=Math.max(1,(h-100)/23);int pages=(PhoneTravel.POINTS.size()+rows-1)/rows;page=Math.min(page,pages-1);
  for(int i=0;i<rows&&page*rows+i<PhoneTravel.POINTS.size();i++){int id=page*rows+i;addRenderableWidget(new PhoneScreen.NeonButton(x+12,y+44+i*23,w-24,20,PhoneTravel.POINTS.get(id).name(),b->{if(sent||minecraft.player==null)return;sent=true;minecraft.player.connection.sendCommand("neontravel "+id);minecraft.gui.setScreen(null);}));}
  addRenderableWidget(new PhoneScreen.NeonButton(x+12,y+h-49,50,19,"前へ",b->{page=(page+pages-1)%pages;clearWidgets();init();}));
  addRenderableWidget(new PhoneScreen.NeonButton(x+w-62,y+h-49,50,19,"次へ",b->{page=(page+1)%pages;clearWidgets();init();}));
  addRenderableWidget(new PhoneScreen.NeonButton(x+12,y+h-25,w-24,19,"マップへ戻る",b->onClose()));
 }
 @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){g.fill(x-3,y-3,x+w+3,y+h+3,0xff08131f);g.outline(x-3,y-3,w+6,h+6,0xff61efe0);g.text(font,"NEON TRAVEL / ファストトラベル",x+12,y+10,0xff82fff0);g.text(font,"街中・塔の入口から利用可能 / 無料",x+12,y+27,0xffb9d4de);g.centeredText(font,(page+1)+" / "+((PhoneTravel.POINTS.size()+rows-1)/rows),x+w/2,y+h-43,0xffb9d4de);super.extractRenderState(g,mx,my,delta);}
}
