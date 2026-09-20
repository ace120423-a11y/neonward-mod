package jp.neonward;

import java.util.*;
import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.commands.Commands;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import net.minecraft.server.level.ServerLevel;

public class NeonFurniture {
 public static final Map<String,Block> BLOCKS=new LinkedHashMap<>();
 public static BlockEntityType<Storage> STORAGE;
 public static final EntityType<FurnitureSeat> SEAT=Registry.register(BuiltInRegistries.ENTITY_TYPE,NeonWard.id("furniture_seat"),EntityType.Builder.<FurnitureSeat>of(FurnitureSeat::new,MobCategory.MISC).sized(.1f,.1f).clientTrackingRange(6).updateInterval(1).build(ResourceKey.create(Registries.ENTITY_TYPE,NeonWard.id("furniture_seat"))));
 public static void init(){
  register("neon_bed","bed",8);register("neon_sofa","seat",7);register("tech_chair","seat",5);
  register("hacker_desk","desk",10);register("weapon_rack","storage",7);register("neon_vendor","storage",12);
  register("shower_panel","shower",7);register("tech_toilet","seat",4);register("neon_bath","bath",8);
  register("cable_bundle","small",3);register("tool_tray","small",3);register("neon_cans","small",7);
  register("neon_shelf","storage",6);register("neon_counter","storage",5);register("neon_locker","storage",7);register("neon_table","table",6);
  register("work_desk","desk",6);register("terminal_desk","desk",10);register("neon_nameplate","small",12);register("tv_remote","remote",8);register("pulse_tv","tv",12);register("suite_bath","suite_bath",8);
  InteriorCatalog.register();Aquariums.register();Medicine.register();var storageBlocks=new HashSet<Block>();for(var b:BLOCKS.values())if(b instanceof StorageBlock)storageBlocks.add(b);
  STORAGE=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,NeonWard.id("furniture_storage"),new BlockEntityType<>(Storage::new,storageBlocks));
  CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB,net.minecraft.resources.Identifier.withDefaultNamespace("functional_blocks"))).register(e->BLOCKS.values().forEach(e::accept));
  CommandRegistrationCallback.EVENT.register((d,c,e)->d.register(Commands.literal("neon").then(Commands.literal("furniture").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(ctx->{
   var p=ctx.getSource().getPlayerOrException();for(var b:BLOCKS.values()){var stack=new ItemStack(b,8);if(!p.getInventory().add(stack))p.drop(stack,false);}
   p.sendSystemMessage(Component.literal("NEON INTERIORS：家具"+BLOCKS.size()+"種類。右クリックで設置、椅子は右クリックで座る／Shiftで降りる。ラック・棚・飲料庫は収納、シャワーは右クリックで水を出せます。"));return 1;
  }))));
 }
 static void register(String name,String kind,int light){
  var id=NeonWard.id(name);var props=BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK,id)).strength(1.5f,5).sound(SoundType.METAL).noOcclusion().lightLevel(s->light);
  Block b=kind.equals("bed")?new ConnectedFurniture.Bed(props):kind.equals("storage")?new StorageBlock(props):kind.equals("small")?new SupportedSmallBlock(props):new FurnitureBlock(props,kind);
  Registry.register(BuiltInRegistries.BLOCK,id,b);BLOCKS.put(name,b);
  var ip=new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id)).useBlockDescriptionPrefix();
  Registry.register(BuiltInRegistries.ITEM,id,new BlockItem(b,ip));
 }
 public static class FurnitureBlock extends HorizontalDirectionalBlock {
  final String kind;
  FurnitureBlock(BlockBehaviour.Properties p,String kind){super(p);this.kind=kind;registerDefaultState(ConnectedFurniture.empty(stateDefinition.any().setValue(FACING,Direction.NORTH)));}
  @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec(){return simpleCodec(p->new FurnitureBlock(p,kind));}
  @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING);ConnectedFurniture.define(b);}
  @Override public BlockState getStateForPlacement(BlockPlaceContext c){return ConnectedFurniture.update(defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite()),c.getLevel(),c.getClickedPos());}
  @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess t,BlockPos p,Direction d,BlockPos q,BlockState o,net.minecraft.util.RandomSource r){return ConnectedFurniture.update(s,l,p);}
  @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){if(kind.equals("suite_bath"))return Shapes.or(Block.box(-16,0,-8,32,3,24),Block.box(-16,3,-8,-13,13,24),Block.box(29,3,-8,32,13,24),Block.box(-13,3,-8,29,13,-5),Block.box(-13,3,21,29,13,24));if(kind.equals("tv"))return Block.box(0,0,12,16,28,15);return Block.box(1,0,1,15,kind.equals("small")?5:kind.equals("table")?8:kind.equals("seat")?9:16,15);}
  @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos pos,Player p,BlockHitResult hit){
   if(kind.equals("tv")&&net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(this).getPath().startsWith("shop_tv_")){if(p instanceof net.minecraft.server.level.ServerPlayer sp&&MediaBridge.available())PortableTelevision.open(sp,pos);return InteractionResult.SUCCESS;}if(kind.equals("seat")){
    if(l instanceof ServerLevel server&&!p.isPassenger()){
     if(!server.getEntitiesOfClass(FurnitureSeat.class,new AABB(pos)).isEmpty())return InteractionResult.SUCCESS;
     var seat=new FurnitureSeat(SEAT,l);seat.anchor=pos.immutable();seat.setPos(pos.getX()+.5,pos.getY()+.42,pos.getZ()+.5);seat.setYRot(s.getValue(FACING).toYRot());server.addFreshEntity(seat);p.startRiding(seat,true,true);
    }return InteractionResult.SUCCESS;
   }
   if(kind.equals("shower")||kind.equals("bath")||kind.equals("suite_bath")){
    if(l instanceof ServerLevel server){server.sendParticles(net.minecraft.core.particles.ParticleTypes.SPLASH,pos.getX()+.5,pos.getY()+.8,pos.getZ()+.5,45,.3,.35,.3,.1);server.playSound(null,pos,net.minecraft.sounds.SoundEvents.GENERIC_SPLASH,net.minecraft.sounds.SoundSource.BLOCKS,.5f,1.3f);}return InteractionResult.SUCCESS;
   }
   return InteractionResult.PASS;
  }
 }
 public static class StorageBlock extends FurnitureBlock implements EntityBlock {
  StorageBlock(BlockBehaviour.Properties p){super(p,"storage");}
  @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec(){return simpleCodec(StorageBlock::new);}
  @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Storage(p,s);}
  @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){if(!l.isClientSide()&&l.getBlockEntity(p) instanceof Storage storage)player.openMenu(storage);return InteractionResult.SUCCESS;}
 }
 public static class Storage extends BaseContainerBlockEntity {
  NonNullList<ItemStack> items=NonNullList.withSize(27,ItemStack.EMPTY);
  Storage(BlockPos p,BlockState s){super(STORAGE,p,s);}
  @Override public int getContainerSize(){return 27;}
  @Override protected NonNullList<ItemStack> getItems(){return items;}
  @Override protected void setItems(NonNullList<ItemStack> v){items=v;}
  @Override protected Component getDefaultName(){return Component.translatable(getBlockState().getBlock().getDescriptionId());}
  @Override protected AbstractContainerMenu createMenu(int id,Inventory inv){return ChestMenu.threeRows(id,inv,this);}
  @Override protected void saveAdditional(ValueOutput o){super.saveAdditional(o);ContainerHelper.saveAllItems(o,items);}
  @Override protected void loadAdditional(ValueInput i){super.loadAdditional(i);items=NonNullList.withSize(27,ItemStack.EMPTY);ContainerHelper.loadAllItems(i,items);}
  @Override public void preRemoveSideEffects(BlockPos p,BlockState s){if(level!=null)Containers.dropContents(level,worldPosition,this);super.preRemoveSideEffects(p,s);}
 }
}

