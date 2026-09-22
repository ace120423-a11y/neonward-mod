package jp.neonward;
import com.google.gson.JsonObject;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Screenshot;
public class HousingSalesQA implements ClientModInitializer {
 int ticks;
 public void onInitializeClient(){ClientTickEvents.END_CLIENT_TICK.register(mc->{if(mc.player==null||mc.getSingleplayerServer()==null)return;ticks++;
  if(ticks==10)mc.getSingleplayerServer().execute(()->StockMarket.ledger.account(mc.player.getStringUUID()).tutorialDone=true);
  if(ticks==50||ticks==80){var s=new PhoneAppScreen(8);s.requested=true;s.page=ticks==50?0:1;s.data=new JsonObject();s.data.addProperty("city",3);s.data.addProperty("home",2);s.data.addProperty("cash",30000);s.data.addProperty("housing_revision",0);s.sellConfirm=true;s.notice="家具・収納は事前回収 / 自動回収はありません";mc.gui.setScreen(s);}
  if(ticks==110||ticks==140){var s=new HomeScreen();s.owned=true;s.cash=30000;s.sellConfirm=ticks==140;s.message=s.sellConfirm?"家具・収納は事前回収 / 残した物はそのまま":"購入済み / 売却して買い直せます";mc.gui.setScreen(s);}
  if(ticks==60||ticks==90||ticks==120||ticks==150){if(!(mc.gui.screen() instanceof PhoneAppScreen)&&!(mc.gui.screen() instanceof HomeScreen))throw new AssertionError("Wrong housing screen");Screenshot.grab(mc,false);}
  if(ticks==165){System.out.println("PAIRED_CLIENT_QA_COMPLETE HOUSING_UI_PASS");mc.stop();}
 });}
}
