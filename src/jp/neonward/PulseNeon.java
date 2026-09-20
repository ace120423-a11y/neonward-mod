package jp.neonward;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.*;

/** Brief, staggered flicker; no constant global scan or permanently forced chunks. */
public final class PulseNeon extends HorizontalDirectionalBlock {
 static final BooleanProperty LIT=BooleanProperty.create("lit");
 static final MapCodec<PulseNeon> CODEC=simpleCodec(PulseNeon::new);
 PulseNeon(BlockBehaviour.Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(LIT,true));}
 public static void init(){for(String c:new String[]{"cyan","pink","amber"}){
  var id=NeonWard.id("pulse_neon_"+c);var b=new PulseNeon(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK,id)).strength(2,6).noOcclusion().lightLevel(s->s.getValue(LIT)?15:5).sound(SoundType.GLASS));
  Registry.register(BuiltInRegistries.BLOCK,id,b);Registry.register(BuiltInRegistries.ITEM,id,new BlockItem(b,new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id))));
 }}
 @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec(){return CODEC;}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState>b){b.add(FACING,LIT);}
 @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
 @Override protected void onPlace(BlockState s,Level l,BlockPos p,BlockState old,boolean moving){if(!l.isClientSide())l.scheduleTick(p,this,1);}
 @Override protected void tick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){
  long phase=Math.floorMod(l.getGameTime()+p.asLong(),240);boolean lit=phase>=12;
  if(lit!=s.getValue(LIT))l.setBlock(p,s.setValue(LIT,lit),3);
  l.scheduleTick(p,this,12);
 }
 @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return switch(s.getValue(FACING)){
  case NORTH->Block.box(0,5,13,16,11,16);case SOUTH->Block.box(0,5,0,16,11,3);case EAST->Block.box(0,5,0,3,11,16);default->Block.box(13,5,0,16,11,16);
 };}
}
