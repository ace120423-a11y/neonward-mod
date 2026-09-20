package jp.neonward;
import java.util.*;
import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.commands.Commands;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.*;
import net.minecraft.util.RandomSource;

public class StreetLights {
 public static final List<Block> LIGHTS=new ArrayList<>();
 public static void init(){
  for(String color:List.of("cyan","pink","amber")){
   var id=NeonWard.id("streetlight_"+color);var block=new Lamp(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK,id)).strength(2,6).sound(SoundType.METAL).noOcclusion().lightLevel(s->s.getValue(Lamp.PART)==3?15:7));
   Registry.register(BuiltInRegistries.BLOCK,id,block);LIGHTS.add(block);
   Registry.register(BuiltInRegistries.ITEM,id,new BlockItem(block,new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id)).useBlockDescriptionPrefix()));
  }
  CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB,net.minecraft.resources.Identifier.withDefaultNamespace("functional_blocks"))).register(e->LIGHTS.forEach(e::accept));
  CommandRegistrationCallback.EVENT.register((d,c,e)->d.register(Commands.literal("neon").then(Commands.literal("lights").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(ctx->{var p=ctx.getSource().getPlayerOrException();for(var b:LIGHTS){var stack=new ItemStack(b,16);if(!p.getInventory().add(stack))p.drop(stack,false);}p.sendSystemMessage(net.minecraft.network.chat.Component.literal("街灯3色：上に4ブロック分の空間を空けて地面に設置。1回で支柱から照明まで組み上がります。"));return 1;}))));
 }
 public static class Lamp extends HorizontalDirectionalBlock {
  public static final IntegerProperty PART=IntegerProperty.create("part",0,3);
  public static final MapCodec<Lamp> CODEC=simpleCodec(Lamp::new);
  Lamp(BlockBehaviour.Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(PART,0));}
  @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec(){return CODEC;}
  @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,PART);}
  @Override public BlockState getStateForPlacement(BlockPlaceContext c){
   var pos=c.getClickedPos();if(pos.getY()+3>=c.getLevel().getMaxY())return null;
   for(int i=1;i<4;i++)if(!c.getLevel().isEmptyBlock(pos.above(i)))return null;
   if(!c.getLevel().getBlockState(pos.below()).isFaceSturdy(c.getLevel(),pos.below(),Direction.UP))return null;
   return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());
  }
  @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity placer,ItemStack stack){for(int i=1;i<4;i++)l.setBlock(p.above(i),s.setValue(PART,i),3);}
  @Override protected boolean canSurvive(BlockState s,LevelReader l,BlockPos p){int part=s.getValue(PART);var below=l.getBlockState(p.below());return part==0?below.isFaceSturdy(l,p.below(),Direction.UP):below.is(this)&&below.getValue(PART)==part-1&&below.getValue(FACING)==s.getValue(FACING);}
  @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess ticks,BlockPos p,Direction d,BlockPos q,BlockState other,RandomSource r){return d==Direction.DOWN&&!canSurvive(s,l,p)?Blocks.AIR.defaultBlockState():super.updateShape(s,l,ticks,p,d,q,other,r);}
  @Override public BlockState playerWillDestroy(Level l,BlockPos p,BlockState s,Player player){if(!l.isClientSide()&&s.getValue(PART)>0){var base=p.below(s.getValue(PART));if(l.getBlockState(base).is(this))l.destroyBlock(base,!player.isCreative());}return super.playerWillDestroy(l,p,s,player);}
  @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return s.getValue(PART)==0?Block.box(4,0,4,12,16,12):s.getValue(PART)==3?Shapes.or(Block.box(6,0,6,10,13,10),Block.box(1,10,1,15,16,15)):Block.box(6,0,6,10,16,10);}
 }
}
