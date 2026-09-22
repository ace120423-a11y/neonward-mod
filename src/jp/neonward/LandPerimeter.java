package jp.neonward;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Blocks;
/** Independent one-time outer enclosure. The existing tunnel is the only open entrance. */
public final class LandPerimeter {
 static void column(MinecraftServer server){var ledger=StockMarket.ledger;if(ledger==null||ledger.westLandColumns<216||ledger.westLandTunnelColumns<40||ledger.westLandPerimeterColumns>=216)return;
  var l=server.overworld();int x=-248+ledger.westLandPerimeterColumns;
  try{for(int z=88;z<=216;z++){if(!LandLayout.perimeter(x,z))continue;var ground=new BlockPos(x,64,z);
   // Existing buildings/containers take priority; never demolish a column to make room.
   boolean occupied=false;for(int y=64;y<=69;y++)if(l.getBlockEntity(new BlockPos(x,y,z))!=null)occupied=true;if(occupied)continue;
   if(LandConstruction.vegetation(l.getBlockState(ground)))l.setBlock(ground,Blocks.POLISHED_ANDESITE.defaultBlockState(),3);
   boolean pillar=(x+248)%8==0&&(z==88||z==216)||(z-88)%8==0&&(x==-248||x==-33);
   for(int y=65;y<=69;y++){var at=new BlockPos(x,y,z);if(!LandConstruction.vegetation(l.getBlockState(at)))continue;
    var block=pillar?Blocks.POLISHED_DEEPSLATE:Blocks.STONE_BRICKS;if(pillar&&y==68)block=Blocks.SEA_LANTERN;
    l.setBlock(at,block.defaultBlockState(),3);
   }
  }ledger.westLandPerimeterColumns++;StockMarket.save();if(ledger.westLandPerimeterColumns==216)System.out.println("[WestLand] Entire area enclosed; west gate tunnel entrance kept open");}
  catch(Exception ex){LandConstruction.failed=true;System.err.println("[WestLand] Perimeter construction paused safely: "+ex);}
 }
}
