package jp.neonward;
import java.util.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.level.block.Blocks;

/** Local, bounded visual effects. No entities, packets, or enchantment logic changes. */
public final class NeonEnchantEffects {
 static ClientLevel current;static final List<BlockPos> tables=new ArrayList<>();static int ticks;
 public static void init(){ClientTickEvents.END_CLIENT_TICK.register(mc->{
  if(mc.level==null||mc.player==null){current=null;tables.clear();return;}
  if(current!=mc.level){current=mc.level;tables.clear();ticks=0;}
  if(mc.isPaused())return;
  if(ticks++%20==0){tables.clear();var at=mc.player.blockPosition();for(var p:BlockPos.betweenClosed(at.offset(-10,-4,-10),at.offset(10,4,10)))if(mc.level.getBlockState(p).is(Blocks.ENCHANTING_TABLE))tables.add(p.immutable());tables.sort(Comparator.comparingDouble(p->p.distSqr(at)));if(tables.size()>4)tables.subList(4,tables.size()).clear();}
  if(ticks%2!=0)return;
  double time=mc.level.getGameTime()*.065;
  for(var p:tables){if(!mc.level.getBlockState(p).is(Blocks.ENCHANTING_TABLE))continue;
   for(int ring=0;ring<2;ring++)for(int i=0;i<12;i++){
    double a=time*(ring==0?1:-1)+i*Math.PI/6,r=ring==0?.60:.43;
    double y=p.getY()+1.05+ring*.23+Math.sin(a*2+time)*.055;
    mc.level.addParticle(new DustParticleOptions(ring==0?0x38f5e5:0xe55cff,.55f),p.getX()+.5+Math.cos(a)*r,y,p.getZ()+.5+Math.sin(a)*r,0,.006,0);
   }
  }
 });}
}
