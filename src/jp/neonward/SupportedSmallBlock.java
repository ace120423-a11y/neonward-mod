package jp.neonward;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.shapes.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;

public class SupportedSmallBlock extends NeonFurniture.FurnitureBlock {
 public static final BooleanProperty LOW=BooleanProperty.create("low");
 public SupportedSmallBlock(BlockBehaviour.Properties p){super(p,"small");registerDefaultState(defaultBlockState().setValue(LOW,false));}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState>b){super.createBlockStateDefinition(b);b.add(LOW);}
 static BlockState fit(BlockState s,BlockGetter l,BlockPos p){return s.setValue(LOW,l.getBlockState(p.below()).is(NeonFurniture.BLOCKS.get("neon_table")));}
 @Override public BlockState getStateForPlacement(BlockPlaceContext c){return fit(super.getStateForPlacement(c),c.getLevel(),c.getClickedPos());}
 @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess ticks,BlockPos p,Direction d,BlockPos q,BlockState other,RandomSource r){return fit(super.updateShape(s,l,ticks,p,d,q,other,r),l,p);}
 @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return super.getShape(s,l,p,c).move(0,s.getValue(LOW)?-.5:0,0);}
 @Override protected void onPlace(BlockState s,Level l,BlockPos p,BlockState old,boolean moved){super.onPlace(s,l,p,old,moved);if(!l.isClientSide())l.scheduleTick(p,this,1);}
 @Override protected void tick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){var fitted=fit(s,l,p);if(fitted!=s)l.setBlock(p,fitted,3);}
 public static void init(){ServerChunkEvents.CHUNK_LOAD.register((l,c,generated)->{
  var sections=c.getSections();for(int i=0;i<sections.length;i++){var section=sections[i];if(!section.maybeHas(s->s.getBlock() instanceof SupportedSmallBlock))continue;int y0=c.getMinY()+i*16;
   for(int y=0;y<16;y++)for(int z=0;z<16;z++)for(int x=0;x<16;x++){var s=section.getBlockState(x,y,z);if(s.getBlock() instanceof SupportedSmallBlock)l.scheduleTick(new BlockPos(c.getPos().getMinBlockX()+x,y0+y,c.getPos().getMinBlockZ()+z),s.getBlock(),1);}
  }
 });}
}
