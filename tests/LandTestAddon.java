package jp.neonward;
public final class LandTestAddon implements net.fabricmc.api.ModInitializer {
 public LandTestAddon(){net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(s->{var l=s.overworld();for(int x=-14;x<=-13;x++){l.setChunkForced(x,8,true);l.getChunk(x,8);}l.setChunkForced(190,0,true);l.getChunk(190,0);});}
 boolean done;
 public void onInitialize(){net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(s->{if(done||s.getTickCount()<40)return;done=true;var p=WestLandIntegration.visitor(s.overworld());try{PairedHandsIntegration.run(p);PairedEffectsIntegration.run(p);ChainPullIntegration.run(p);CasinoGachaIntegration.run(p);HousingSalesIntegration.run();WestLandIntegration.run(p);System.out.println("LAND_TEST_COMPLETE");}catch(Throwable t){System.out.println("LAND_TEST_FAILED");t.printStackTrace();}finally{s.overworld().removePlayerImmediately(p,net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);}});}
}
