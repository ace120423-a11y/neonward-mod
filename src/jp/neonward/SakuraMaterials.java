package jp.neonward;

import java.util.*;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.shapes.*;

/** Original Edo street materials. Pure native blocks/models: no entities or world generation. */
public final class SakuraMaterials {
 private static final Map<String,Block> BLOCKS=new LinkedHashMap<>();
 public static final String[] IDS={"sakura_plaster","sakura_dark_timber","sakura_board","sakura_stone_paving","sakura_kawara_tile","sakura_kawara_slope","sakura_kawara_ridge","sakura_lattice","sakura_shoji","sakura_paper_lantern","sakura_stone_lantern","sakura_saisen_box","sakura_chozu_basin","sakura_vermilion_timber","sakura_omamori_counter","sakura_shrine_roof_tile","sakura_shrine_roof_slope","sakura_shrine_roof_ridge","sakura_shrine_bracket","sakura_shrine_rope","sakura_saisen_left","sakura_saisen_right","sakura_chozu_front_left","sakura_chozu_front_right","sakura_chozu_back_left","sakura_chozu_back_center","sakura_chozu_back_right"};
 public static void init(){
  if(!BLOCKS.isEmpty())return;
  for(int kind=0;kind<IDS.length;kind++){
   String name=IDS[kind];var id=NeonWard.id(name);
   boolean stone=kind==3||kind>=4&&kind<=6||kind==10||kind==12||kind>=15&&kind<=17||kind>=22;
   var props=BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK,id)).strength(stone?2.5f:1.8f,stone?8:4).sound(stone?SoundType.STONE:kind==8||kind==9?SoundType.WOOL:SoundType.WOOD);
   if(kind==9||kind==10)props.lightLevel(state->12);
   boolean shaped=kind>=5&&kind<=12||kind==14||kind>=16;
   if(kind==19)props.noCollision();
   Block block=shaped?new Shaped(props.noOcclusion(),kind):new Block(props);
   Registry.register(BuiltInRegistries.BLOCK,id,block);
   Registry.register(BuiltInRegistries.ITEM,id,new BlockItem(block,new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id)).useBlockDescriptionPrefix()));
   BLOCKS.put(name,block);
  }
  net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB,net.minecraft.resources.Identifier.withDefaultNamespace("building_blocks"))).register(entries->BLOCKS.values().forEach(entries::accept));
 }
 public static Block get(String id){var block=BLOCKS.get(id);if(block==null)throw new IllegalArgumentException("Unregistered Sakura material: "+id);return block;}
 /** Models face north by default. Slope eave is at FACING; its high edge is opposite. */
 public static BlockState facing(String id,Direction direction){
  var state=get(id).defaultBlockState();
  if(!state.hasProperty(HorizontalDirectionalBlock.FACING))return state;
  if(direction==null||direction.getAxis()==Direction.Axis.Y)throw new IllegalArgumentException("Sakura facing must be horizontal");
  return state.setValue(HorizontalDirectionalBlock.FACING,direction);
 }
 private static VoxelShape box(double x,double y,double z,double X,double Y,double Z){return Block.box(x,y,z,X,Y,Z);}
 /** Pure placement plan. EAST spans Z=-1..1 and basin back row X=-1; no world writes. */
 public static Map<BlockPos,BlockState> footprint(String anchorId,BlockPos anchor,Direction facing){
  if(facing==null||facing.getAxis()==Direction.Axis.Y)throw new IllegalArgumentException("Horizontal facing required");
  String[][] rows=switch(anchorId){
   case "sakura_saisen_box"->new String[][]{{"sakura_saisen_left",anchorId,"sakura_saisen_right"}};
   case "sakura_chozu_basin"->new String[][]{{"sakura_chozu_front_left",anchorId,"sakura_chozu_front_right"},{"sakura_chozu_back_left","sakura_chozu_back_center","sakura_chozu_back_right"}};
   default->throw new IllegalArgumentException("Not a Sakura fixture: "+anchorId);
  };
  var result=new LinkedHashMap<BlockPos,BlockState>();var right=facing.getClockWise();
  for(int depth=0;depth<rows.length;depth++)for(int col=0;col<3;col++)result.put(anchor.relative(right,col-1).relative(facing.getOpposite(),depth),facing(rows[depth][col],facing));
  return Collections.unmodifiableMap(result);
 }
 /** Generated per-cell geometry: collision exactly follows solid mesh cuboids, not decorative water. */
 private static VoxelShape fixtureShape(int kind){
  String resource="/assets/neonward/models/block/"+IDS[kind]+".json";
  try(var in=SakuraMaterials.class.getResourceAsStream(resource)){
   if(in==null)throw new IllegalStateException("Missing fixture model: "+resource);
   var data=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
   VoxelShape result=Shapes.empty();
   for(var entry:data.getAsJsonArray("elements")){
    var e=entry.getAsJsonObject();String material=e.getAsJsonObject("faces").getAsJsonObject("up").get("texture").getAsString();
    if(material.equals("#water")||material.equals("#water_glint"))continue;
    var a=e.getAsJsonArray("from");var b=e.getAsJsonArray("to");
    result=Shapes.or(result,box(a.get(0).getAsDouble(),a.get(1).getAsDouble(),a.get(2).getAsDouble(),b.get(0).getAsDouble(),b.get(1).getAsDouble(),b.get(2).getAsDouble()));
   }
   return result.optimize();
  }catch(java.io.IOException e){throw new IllegalStateException("Cannot read fixture shape: "+resource,e);}
 }
 private static VoxelShape shape(int kind){
  if(kind==11||kind==12||kind>=20)return fixtureShape(kind);
  if(kind==16)return shape(5);
  if(kind==17)return shape(6);
  var pieces=new ArrayList<VoxelShape>();
  switch(kind){
   case 5->{for(int i=0;i<8;i++)pieces.add(box(0,0,i*2,16,2+i*2,(i+1)*2));}
   case 6->{pieces.add(box(0,0,2,16,2,14));pieces.add(box(0,2,4,16,5,12));pieces.add(box(0,5,6,16,8,10));}
   case 7->{for(double x:new double[]{0,14.8})pieces.add(box(x,0,7,x+1.2,16,9));for(double y:new double[]{0,14.8})pieces.add(box(1.2,y,7,14.8,y+1.2,9));for(int x:new int[]{3,6,9,12})pieces.add(box(x,1.2,7.2,x+.45,14.8,8.8));for(int y:new int[]{4,8,12})pieces.add(box(1.2,y,7.1,14.8,y+.45,8.9));pieces.add(box(0,14.3,5.7,16,15,6.3));for(double x:new double[]{1,5.8,10.6})pieces.add(box(x,10.5,5.8,x+4.4,14.3,6.05));}
   case 8->pieces.add(box(0,0,7,16,16,9));
   case 9->{pieces.add(box(4,3,4,12,13,12));pieces.add(box(6,1,6,10,3,10));pieces.add(box(6,13,6,10,16,10));}
   case 10->{pieces.add(box(2,0,2,14,2,14));pieces.add(box(6,2,6,10,7,10));pieces.add(box(4,7,4,12,11,12));pieces.add(box(2,11,2,14,13,14));pieces.add(box(4,13,4,12,14,12));pieces.add(box(7,14,7,9,16,9));}
   case 11->{pieces.add(box(1,0,2,15,2,14));pieces.add(box(2,2,3,14,10,13));pieces.add(box(0,10,1,16,12,15));}
   case 12->{pieces.add(box(1,0,1,15,3,15));pieces.add(box(1,3,1,3,9,15));pieces.add(box(13,3,1,15,9,15));pieces.add(box(3,3,1,13,9,3));pieces.add(box(3,3,13,13,9,15));pieces.add(box(12,9,12,13,16,13));pieces.add(box(7,13,11,13,14,12));}
   case 14->{pieces.add(box(1,0,2,15,9,14));pieces.add(box(0,9,1,16,11,15));pieces.add(box(1,11,13,15,15,14));}
   case 18->{pieces.add(box(5,0,5,11,4,11));pieces.add(box(3,4,3,13,7,13));pieces.add(box(1,7,2,15,10,14));pieces.add(box(0,10,1,16,13,15));pieces.add(box(0,13,0,16,16,16));}
   case 19->pieces.add(box(0,2,6,16,15,10));
   default->pieces.add(Shapes.block());
  }
  VoxelShape result=Shapes.empty();for(var piece:pieces)result=Shapes.or(result,piece);return result.optimize();
 }
 private static final class Shaped extends HorizontalDirectionalBlock {
  private final int kind;private final VoxelShape[] shapes=new VoxelShape[4];
  Shaped(Properties props,int kind){super(props);this.kind=kind;registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));shapes[0]=shape(kind);
   for(int i=1;i<4;i++){var rotated=new ArrayList<VoxelShape>();shapes[i-1].forAllBoxes((x,y,z,X,Y,Z)->rotated.add(Shapes.box(1-Z,y,x,1-z,Y,X)));var result=Shapes.empty();for(var p:rotated)result=Shapes.or(result,p);shapes[i]=result.optimize();}
  }
  @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec(){return simpleCodec(props->new Shaped(props,kind));}
  @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING);}
  @Override public BlockState getStateForPlacement(BlockPlaceContext ctx){return defaultBlockState().setValue(FACING,ctx.getHorizontalDirection().getOpposite());}
  @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return shapes[switch(state.getValue(FACING)){case EAST->1;case SOUTH->2;case WEST->3;default->0;}];}
  @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return kind==19?Shapes.empty():getShape(state,level,pos,context);}
 }
 private SakuraMaterials(){}
}
