package jp.neonward;
public final class LandTestAddon implements net.fabricmc.api.ModInitializer {
 boolean done;
 public void onInitialize(){net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(s->{if(done||s.getTickCount()<40)return;done=true;var p=WestLandIntegration.visitor(s.overworld());try{WestLandIntegration.run(p);System.out.println("LAND_TEST_COMPLETE");}catch(Throwable t){System.out.println("LAND_TEST_FAILED");t.printStackTrace();}finally{s.overworld().removePlayerImmediately(p,net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);}});}
}
