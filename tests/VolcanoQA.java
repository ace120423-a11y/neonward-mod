package jp.neonward;
import java.util.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameType;
/** Isolated client: every production renderer, atlas and synchronized attack pose. */
public final class VolcanoQA implements ClientModInitializer {
 int ticks,shots;volatile Throwable failure;VolcanoBoss boss;
 void server(Minecraft mc,Runnable work){mc.getSingleplayerServer().execute(()->{try{work.run();}catch(Throwable e){failure=e;}});}
 public void onInitializeClient(){ClientTickEvents.END_CLIENT_TICK.register(mc->{
  if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null)return;
  ticks++;mc.gui.setScreen(null);mc.gui.hud.getChat().clearMessages(false);
  if(failure!=null){failure.printStackTrace();mc.stop();return;}
  if(ticks<40)return;int elapsed=ticks-40,index=elapsed<120?0:1+(elapsed-120)/50,step=elapsed<120?elapsed:(elapsed-120)%50;
  if(index>=33){if(shots!=33)throw new AssertionError("Screenshots "+shots);System.out.println("PAIRED_CLIENT_QA_COMPLETE VOLCANO_RENDER_PASS 30 models + 23F throw poses");mc.stop();return;}
  if(step==0)server(mc,()->{
   var s=mc.getSingleplayerServer();var l=s.overworld();var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.CREATIVE);
   if(index==0){for(int x=3030;x<3070;x++)for(int z=0;z<40;z++)l.setBlock(new BlockPos(x,69,z),Blocks.SMOOTH_STONE.defaultBlockState(),2);}
   p.teleportTo(l,3058,72,8,Set.of(),38,8,true);p.getAbilities().flying=true;p.onUpdateAbilities();
   if(boss!=null)boss.discard();int f=index<30?index+1:23;boss=new VolcanoBoss(VolcanoBosses.TYPES.get(f-1),l);boss.setNoAi(true);boss.setNoGravity(true);boss.setPos(3050,70,18);boss.setYRot(180);boss.setYHeadRot(180);boss.setYBodyRot(180);
   if(index>=30){boss.getEntityData().set(VolcanoBoss.ACTION,6);boss.getEntityData().set(VolcanoBoss.AGE,new int[]{16,28,34}[index-30]);}
   l.addFreshEntity(boss);System.out.println("VOLCANO_RENDER_MODEL "+f+" pose "+index);
  });
  if(step==(index==0?100:35)){if(mc.level.getEntitiesOfClass(VolcanoBoss.class,mc.player.getBoundingBox().inflate(35)).isEmpty())throw new AssertionError("Missing client boss "+index);Screenshot.grab(mc,false);shots++;}
 });}
}
