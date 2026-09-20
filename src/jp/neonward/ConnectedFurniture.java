package jp.neonward;
import net.minecraft.core.*;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.util.RandomSource;

public final class ConnectedFurniture {
 public static final BooleanProperty NORTH=BooleanProperty.create("join_north"),EAST=BooleanProperty.create("join_east"),SOUTH=BooleanProperty.create("join_south"),WEST=BooleanProperty.create("join_west");
 public static final BooleanProperty[] FLAGS={NORTH,EAST,SOUTH,WEST};
 public static final Direction[] DIRS={Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST};
 public static void define(StateDefinition.Builder<Block,BlockState> b){b.add(NORTH,EAST,SOUTH,WEST);}
 public static BlockState empty(BlockState s){for(var p:FLAGS)s=s.setValue(p,false);return s;}
 public static int group(Block b){for(int i=0;i<InteriorCatalog.PRODUCTS.length;i++){var p=InteriorCatalog.PRODUCTS[i];if(NeonFurniture.BLOCKS.get(p.id())==b&&p.id().startsWith("shop_sofa_"))return 1;}for(String n:new String[]{"neon_sofa","neon_bed","neon_table","hacker_desk","work_desk","terminal_desk"})if(NeonFurniture.BLOCKS.get(n)==b)return n.equals("neon_sofa")?1:n.equals("neon_bed")?2:n.equals("neon_table")?3:4;return 0;}
 public static BlockState update(BlockState s,BlockGetter l,BlockPos p){
  if(!s.hasProperty(NORTH))return s;int group=group(s.getBlock());Direction face=s.getValue(HorizontalDirectionalBlock.FACING);
  for(int i=0;i<4;i++){var other=l.getBlockState(p.relative(DIRS[i]));boolean join=group!=0&&group(other.getBlock())==group;
   if(group==1)join=join&&s.getBlock()==other.getBlock();if(group==1||group==2)join=join&&DIRS[i].getAxis()!=face.getAxis()&&other.getValue(HorizontalDirectionalBlock.FACING)==face;
   if(group==2&&join)join=other.getValue(BedBlock.PART)==s.getValue(BedBlock.PART);
   s=s.setValue(FLAGS[i],join);
  }return s;
 }
 public static class Bed extends BedBlock {
  public Bed(BlockBehaviour.Properties p){super(DyeColor.CYAN,p);registerDefaultState(empty(defaultBlockState()));}
  @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);define(b);}
  @Override public BlockState getStateForPlacement(BlockPlaceContext c){var s=super.getStateForPlacement(c);return s==null?null:update(s,c.getLevel(),c.getClickedPos());}
  @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess ticks,BlockPos p,Direction d,BlockPos q,BlockState o,RandomSource r){return update(super.updateShape(s,l,ticks,p,d,q,o,r),l,p);}
 }
}
