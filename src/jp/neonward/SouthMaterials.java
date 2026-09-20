package jp.neonward;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;

public final class SouthMaterials {
 static final java.util.List<Block> ALL=new java.util.ArrayList<>();
 static void add(String name,boolean panel,int light){
  var id=NeonWard.id(name);var props=BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK,id)).strength(2,8).sound(SoundType.METAL).lightLevel(s->light);
  Block b=panel?new Panel(props.noCollision().noOcclusion()):new Block(name.equals("red_paper_lantern")?props.noOcclusion():props);
  Registry.register(BuiltInRegistries.BLOCK,id,b);Registry.register(BuiltInRegistries.ITEM,id,new BlockItem(b,new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id)).useBlockDescriptionPrefix()));ALL.add(b);
 }
 public static void init(){
  for(String n:new String[]{"grimy_concrete","patched_brick","rusted_corrugated","peeling_plaster","oily_floor"})add(n,false,0);
  add("alley_vent",true,0);add("red_paper_lantern",false,13);
  for(int i=0;i<4;i++)add("street_graffiti_"+i,true,0);
  net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB,net.minecraft.resources.Identifier.withDefaultNamespace("building_blocks"))).register(e->ALL.forEach(e::accept));
 }
 static final class Panel extends HorizontalDirectionalBlock {
  static final MapCodec<Panel> CODEC=simpleCodec(Panel::new);
  Panel(BlockBehaviour.Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
  @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec(){return CODEC;}
  @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING);}
  @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
 }
}
