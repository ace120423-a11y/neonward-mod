package jp.neonward;
import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import net.minecraft.util.RandomSource;

public class AutoDoors {
 public static SensorDoor DOOR;
 public static void init(){
  var id=NeonWard.id("automatic_door");DOOR=new SensorDoor(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK,id)).strength(3,8).sound(SoundType.METAL).noOcclusion().lightLevel(s->6));
  Registry.register(BuiltInRegistries.BLOCK,id,DOOR);Registry.register(BuiltInRegistries.ITEM,id,new BlockItem(DOOR,new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id)).useBlockDescriptionPrefix()));
  CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB,Identifier.withDefaultNamespace("functional_blocks"))).register(e->e.accept(DOOR));
 }
 public static class SensorDoor extends DoorBlock {
  public static final MapCodec<SensorDoor> CODEC=simpleCodec(SensorDoor::new);
  SensorDoor(BlockBehaviour.Properties p){super(BlockSetType.IRON,p);}
  @Override public MapCodec<? extends DoorBlock> codec(){return CODEC;}
  @Override protected void onPlace(BlockState s,Level l,BlockPos p,BlockState old,boolean moved){super.onPlace(s,l,p,old,moved);if(!l.isClientSide()&&s.getValue(HALF)==DoubleBlockHalf.LOWER)l.scheduleTick(p,this,5);}
  @Override protected void tick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){
   if(s.getValue(HALF)!=DoubleBlockHalf.LOWER)return;
   var area=new AABB(p).expandTowards(0,1,0).inflate(2.2,.2,2.2);
   boolean near=!l.getEntitiesOfClass(Player.class,area,a->a.isAlive()&&!a.isSpectator()&&!a.entityTags().contains("nw_lift_riding")).isEmpty();
   // Never close the panel through a player standing directly in the threshold.
   near|=!l.getEntitiesOfClass(Player.class,new AABB(p).expandTowards(0,1,0).inflate(.2,0,.2),a->a.isAlive()&&!a.isSpectator()).isEmpty();
   Boolean landing=LiftSystem.landingDoorMayOpen(l,p);if(landing!=null)near=near&&landing;
   if(s.getValue(OPEN)!=near)setOpen(null,l,s,p,near);
   l.scheduleTick(p,this,5);
  }
  @Override protected void neighborChanged(BlockState s,Level l,BlockPos p,Block b,Orientation o,boolean moved){if(!l.isClientSide()&&s.getValue(HALF)==DoubleBlockHalf.LOWER)l.scheduleTick(p,this,1);}
  @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){if(!l.isClientSide())l.scheduleTick(s.getValue(HALF)==DoubleBlockHalf.UPPER?p.below():p,this,1);return InteractionResult.SUCCESS;}
  @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){if(s.getValue(OPEN))return s.getValue(FACING).getAxis()==Direction.Axis.X?Block.box(7,0,0,9,16,1):Block.box(0,0,7,1,16,9);return s.getValue(FACING).getAxis()==Direction.Axis.X?Block.box(7,0,0,9,16,16):Block.box(0,0,7,16,16,9);}
  @Override protected VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return s.getValue(OPEN)?Shapes.empty():getShape(s,l,p,c);}
 }
}
