package jp.neonward;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Blocks;
/** One column per tick. Never rebuilds purchased land; no chunks or underground data are erased. */
public final class LandConstruction {
 static boolean failed;
 public static void tick(MinecraftServer server){var ledger=StockMarket.ledger;if(failed||ledger==null||ledger.westLandColumns>=216||!ledger.westLand.isEmpty())return;
  if(server.getPlayerList().getPlayers().stream().noneMatch(p->p.level()==server.overworld()&&p.getX()<0&&p.getZ()>60&&p.getZ()<240))return;
  column(server);
 }
 static void column(MinecraftServer server){var ledger=StockMarket.ledger;if(ledger==null||ledger.westLandColumns>=216||!ledger.westLand.isEmpty())return;
  int x=-248+ledger.westLandColumns;var l=server.overworld();
  try{for(int z=88;z<=216;z++){
   int plot=LandLayout.plot(x,z);boolean avenue=z>=144&&z<=159;boolean cross=x%44>=-17&&x%44<=-9; // fixed shared access paths
   var pos=new BlockPos(x,64,z);var old=l.getBlockState(pos);
   // Only replace surveyed terrain/old roadway. Unknown blocks and containers are left intact.
   if(l.getBlockEntity(pos)==null&&(old.isAir()||old.is(Blocks.GRASS_BLOCK)||old.is(Blocks.DIRT)||java.util.Set.of("black_concrete","gray_concrete","light_gray_concrete").contains(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(old.getBlock()).getPath())||old.is(Blocks.SMOOTH_STONE)||old.is(Blocks.STONE))){
    var ground=avenue||cross?Blocks.SMOOTH_STONE:Blocks.GRASS_BLOCK;
    if(plot<0&&(avenue||cross)&&(x+248)%16==0&&z%8==0)ground=Blocks.SEA_LANTERN;
    if(plot>=0){boolean edge=x==LandLayout.x(plot)||x==LandLayout.x(plot)+31||z==LandLayout.z(plot)||z==LandLayout.z(plot)+31;ground=edge?Blocks.POLISHED_ANDESITE:Blocks.GRASS_BLOCK;}
    l.setBlock(pos,ground.defaultBlockState(),3);
   }
   var above=pos.above();var b=l.getBlockState(above);if(b.is(Blocks.IRON_BARS)||b.is(Blocks.SEA_LANTERN)||b.is(Blocks.SHORT_GRASS)||b.is(Blocks.TALL_GRASS))l.setBlock(above,Blocks.AIR.defaultBlockState(),3);
   if(x==-34&&z==152)NeonZones.sign(l,above,"WEST LAND","西の土地エリア","緑色の端末で購入","32×32 / 20,000 Cr");
   for(int id=0;id<8;id++)for(int t=0;t<4;t++)if(WestLand.terminal(id,t).equals(above)){
    if(l.getBlockEntity(pos)!=null||!l.getBlockState(above).isAir()&&!l.getBlockState(above).is(WestLand.block(t)))throw new IllegalStateException("Existing object at land terminal "+above);
    l.setBlock(pos,Blocks.SMOOTH_STONE.defaultBlockState(),3);l.setBlock(above,WestLand.block(t).defaultBlockState(),3);
    NeonZones.sign(l,above.above(),"WEST LAND "+(id+1),WestLand.title(t),t==0?"32×32 / 20,000 Cr":"所有者専用", "右クリック");
   }
  }ledger.westLandColumns++;StockMarket.save();if(ledger.westLandColumns==216)System.out.println("[WestLand] 8 plots and independent terminals ready");}
  catch(Exception ex){failed=true;System.err.println("[WestLand] Construction paused safely: "+ex);}
 }
}
