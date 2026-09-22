package jp.neonward;
import java.util.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.*;
import net.minecraft.world.level.GameType;
/** Visits the saved integration world, using real transfer/return code. */
public final class VolcanoTourQA implements ClientModInitializer {
 int ticks,shots;volatile Throwable failure;
 public void onInitializeClient(){ClientTickEvents.END_CLIENT_TICK.register(mc->{
  if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null)return;
  ticks++;mc.gui.setScreen(null);mc.gui.hud.getChat().clearMessages(false);
  if(failure!=null){failure.printStackTrace();mc.stop();return;}
  if(ticks<40)return;int index=(ticks-40)/120,step=(ticks-40)%120;
  if(index>=4){if(shots!=4)throw new AssertionError("screenshots");System.out.println("PAIRED_CLIENT_QA_COMPLETE VOLCANO_TOUR_PASS");mc.stop();return;}
  if(step==0)mc.getSingleplayerServer().execute(()->{try{
   var s=mc.getSingleplayerServer();var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.CREATIVE);
   if(index==0){VolcanicSpire.visit(p);}
   if(index==1){p.teleportTo(s.getLevel(VolcanicSpire.DIM),-111,76,4,Set.of(),32,15,true);p.getAbilities().flying=true;p.onUpdateAbilities();}
   if(index==2){VolcanicSpire.NEXT.clear();VolcanicSpire.enter(p,true);}
   if(index==3){VolcanicSpire.town(p);}
  }catch(Throwable e){failure=e;}});
  if(step==100){if(index!=3&&mc.level.dimension()!=VolcanicSpire.DIM)throw new AssertionError("volcano transfer");if(index==3&&mc.level.dimension()!=CompactShops.DIM)throw new AssertionError("guild return");Screenshot.grab(mc,false);shots++;}
 });}
}
