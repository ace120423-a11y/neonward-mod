package jp.neonward;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
public class GarageScreen extends Screen {
 int selected,l,t,w,h,page;java.util.Set<String> owned=new java.util.HashSet<>();
 void receive(com.google.gson.JsonObject o){owned.clear();if(o.has("casinoVehicles")&&o.get("casinoVehicles").isJsonArray())for(var v:o.getAsJsonArray("casinoVehicles"))owned.add(v.getAsString());}
 public GarageScreen(){super(Component.literal("NEON MOTOR"));}
 @Override public boolean isPauseScreen(){return false;}
 @Override public void onClose(){minecraft.gui.setScreen(new PhoneScreen());}
 void command(String s){if(minecraft.player!=null)minecraft.player.connection.sendCommand("neongarage "+s);minecraft.gui.setScreen(null);}
 @Override protected void init(){
  w=Math.min(450,width-16);h=Math.min(360,height-12);l=(width-w)/2;t=(height-h)/2;
  int step=Math.min(22,(h-107)/8),left=Math.min(162,w/2-10);
  for(int i=0;i<8&&page*8+i<VehicleCatalog.ALL.size();i++){final int n=page*8+i;var s=VehicleCatalog.ALL.get(n);addRenderableWidget(new PhoneScreen.NeonButton(l+10,t+34+i*step,left,step-2,s.name(),b->selected=n));}
  addRenderableWidget(new PhoneScreen.NeonButton(l+10,t+h-53,72,18,"車種切替",b->{page=1-page;clearWidgets();init();}));
  addRenderableWidget(new PhoneScreen.NeonButton(l+10,t+h-77,left,18,"選んだ車種を改造",b->{if(minecraft.player!=null)minecraft.player.connection.sendCommand("neonmotor "+VehicleCatalog.ALL.get(selected).id()+" view");}));
  if(minecraft.player!=null)minecraft.player.connection.sendCommand("neongarage view");
  addRenderableWidget(new PhoneScreen.NeonButton(l+left+20,t+h-76,w-left-30,22,"選んだ車両を呼ぶ",b->command(VehicleCatalog.ALL.get(selected).id())));
  addRenderableWidget(new PhoneScreen.NeonButton(l+left+20,t+h-49,w-left-30,19,"近くの空の車両を回収",b->command("recall")));
  addRenderableWidget(new PhoneScreen.NeonButton(l+10,t+h-25,w-20,18,"ホームに戻る",b->onClose()));
 }
 @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
  g.fill(l-4,t-4,l+w+4,t+h+4,0xff080c15);g.fill(l,t,l+w,t+h,0xff0b1926);g.outline(l,t,w,h,0xff4de7da);g.centeredText(font,"NEON MOTOR / 車種選択",l+w/2,t+12,0xff64fff0);
  var s=VehicleCatalog.ALL.get(selected);int x=l+Math.min(162,w/2-10)+20,y=t+45;
  String[] lines={s.name(),s.style(),"最高速度 約"+Math.round(s.top()*72)+" km/h","加速 "+String.format(java.util.Locale.ROOT,"%.1f",s.acceleration()*1440)+" km/h毎秒",s.seats()+"人乗り / 荷室 "+s.rows()*9+"枠",CasinoGames.PRIZES.contains(s.id())?(owned.contains(s.id())?"景品：入手済み":"景品：未入手（カジノで当選）"):"","右クリック：乗車","Shift＋右クリック：荷室","荷室は所有者専用","荷物・乗員がいる車は回収不可"};
  for(int i=0;i<lines.length;i++)g.text(font,lines[i],x,y+i*Math.min(17,Math.max(8,(h-127)/10)),i==0?0xfffd71b6:0xffbbdfeb);
  super.extractRenderState(g,mx,my,delta);
 }
}
