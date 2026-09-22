package jp.neonward;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
/** Matches the south/slum tunnel's 33-wide opening and Y97 roof, connecting the existing west gate. */
public final class LandTunnel {
 static void put(ServerLevel l,int x,int y,int z,Block block){var p=new BlockPos(x,y,z);if(l.getBlockEntity(p)==null&&LandConstruction.vegetation(l.getBlockState(p)))l.setBlock(p,block.defaultBlockState(),3);}
 static void column(MinecraftServer server){var ledger=StockMarket.ledger;if(ledger==null||ledger.westLandTunnelColumns>=40)return;var l=server.overworld();int x=-67+ledger.westLandTunnelColumns;
  try{var concrete=BuiltInRegistries.BLOCK.getValue(NeonWard.id("grimy_concrete"));if(concrete==null||concrete==Blocks.AIR)throw new IllegalStateException("Missing tunnel material");boolean rib=(x+67)%8==0||x==-28;
   for(int z=134;z<=174;z++){
    for(int y=65;y<=100;y++)if(z<=137||z>=171||y>=97)put(l,x,y,z,rib?Blocks.POLISHED_DEEPSLATE:concrete);
    if(z>=138&&z<=170){var floor=new BlockPos(x,64,z);var state=l.getBlockState(floor);if(state.is(Blocks.GRASS_BLOCK)||state.is(Blocks.DIRT)||LandConstruction.vegetation(state))l.setBlock(floor,Blocks.SMOOTH_STONE.defaultBlockState(),3);}
   }
   // Ceiling and low side lights illuminate the passage without blocking either direction.
   if(rib)for(int z:new int[]{139,154,169})put(l,x,96,z,Blocks.SEA_LANTERN);
   if(rib)for(int z:new int[]{138,170})put(l,x,68,z,Blocks.SEA_LANTERN);
   ledger.westLandTunnelColumns++;StockMarket.save();if(ledger.westLandTunnelColumns==40)System.out.println("[WestLand] West gate tunnel connected; existing road and gate preserved");
  }catch(Exception ex){LandConstruction.failed=true;System.err.println("[WestLand] Tunnel construction paused safely: "+ex);}
 }
}
