package jp.neonward;

import java.util.Set;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Screenshot;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** Runs each real companion renderer in the production Sodium/Iris client. */
public final class PetClientQA implements ClientModInitializer {
 int ticks,images;volatile Throwable failure;volatile boolean sawFlight;
 @Override public void onInitializeClient(){
  if(!FabricLoader.getInstance().isModLoaded("sodium"))throw new IllegalStateException("Production Sodium required");
  ClientTickEvents.END_CLIENT_TICK.register(mc->{
   if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null)return;
   if(failure!=null){System.out.println("PET_CLIENT_QA_FAILED");failure.printStackTrace();mc.stop();return;}
   ticks++;mc.gui.setScreen(null);if(ticks<40)return;
   int kind=(ticks-40)/120,phase=(ticks-40)%120;
   if(kind>=5){if(images!=10||!sawFlight){failure=new AssertionError("Missing screenshots or synchronized crow flight");return;}System.out.println("PET_CLIENT_QA_COMPLETE screenshots="+images+" sodium=true crowFlight=true");mc.stop();return;}
   if(phase==0)mc.getSingleplayerServer().execute(()->{try{
    var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();var level=p.level();p.setGameMode(GameType.CREATIVE);
    p.teleportTo(level,190.5,65,154.5,Set.of(),0,15,true);p.setDeltaMovement(Vec3.ZERO);
    StockMarket.ledger.account(p.getStringUUID()).petOwned.add(kind);
    if(PetCompanions.summon(p,kind)!=1)throw new AssertionError("Summon "+kind);
    var pet=level.getEntitiesOfClass(PetEntity.class,p.getBoundingBox().inflate(12)).stream().filter(e->p.getUUID().equals(e.owner)).findFirst().orElseThrow();
    pet.setPos(190.5,65,156.5);pet.setYRot(180);pet.setYBodyRot(180);pet.getNavigation().stop();
   }catch(Throwable t){failure=t;}});
   var visible=mc.level.getEntitiesOfClass(PetEntity.class,mc.player.getBoundingBox().inflate(15));
   if(kind==3&&visible.stream().anyMatch(PetEntity::flying))sawFlight=true;
   if(phase==35||phase==80){
    if(visible.stream().noneMatch(p->p.kind()==kind)){failure=new AssertionError("No synchronized pet "+kind);return;}
    Screenshot.grab(mc,false);images++;System.out.println("PET_SCREENSHOT kind="+kind+" frame="+phase+" flying="+visible.stream().anyMatch(PetEntity::flying));
   }
  });
 }
}
