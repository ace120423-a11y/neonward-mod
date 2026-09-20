package jp.neonward;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
public class LiftScreen extends Screen {
 final VerticalLift lift;int page=0,x,y,w,h;
 public LiftScreen(VerticalLift lift){super(Component.literal("NEON LIFT"));this.lift=lift;}
 @Override public boolean isPauseScreen(){return false;}
 @Override protected void init(){w=Math.min(280,width-20);h=Math.min(245,height-12);x=(width-w)/2;y=(height-h)/2;int step=Math.max(22,(h-95)/5);
  for(int j=0;j<20;j++){int f=page*20+j;if(f>=lift.plan().floors().length)break;addRenderableWidget(Button.builder(Component.literal((f+1)+"階"),b->{if(minecraft.player!=null)minecraft.player.connection.sendCommand("neonlift select "+lift.liftId()+" "+(f+1));onClose();}).bounds(x+12+(j%4)*(w-20)/4,y+51+(j/4)*step,(w-28)/4-3,step-3).build());}
  if(lift.plan().floors().length>20)addRenderableWidget(Button.builder(Component.literal(page==0?"21階〜 →":"← 1〜20階"),b->{page=1-page;clearWidgets();init();}).bounds(x+12,y+h-28,w/2-18,20).build());
  addRenderableWidget(Button.builder(Component.literal("閉じる"),b->onClose()).bounds(x+w/2+3,y+h-28,w/2-15,20).build());
 }
 @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float d){g.fill(x-3,y-3,x+w+3,y+h+3,0xff09111c);g.outline(x-3,y-3,w+6,h+6,0xff48fff0);g.centeredText(font,"NEON LIFT / 階数選択",x+w/2,y+10,0xff70fff0);g.centeredText(font,lift.status(),x+w/2,y+29,0xffff70c5);super.extractRenderState(g,mx,my,d);}
}
