package jp.neonward;
import java.util.*;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.*;
import net.minecraft.util.ProblemReporter;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
public final class Aquariums {
 public static Block BLOCK;static BlockEntityType<Tank> TYPE;
 static boolean fish(ItemStack s){return OrnamentalFish.isFish(s)||s.is(Items.COD)||s.is(Items.SALMON)||s.is(Items.TROPICAL_FISH)||s.is(Items.PUFFERFISH);}
 public static ItemStack caught(ItemStack s){if(!fish(s))return s;var random=java.util.concurrent.ThreadLocalRandom.current();if(!OrnamentalFish.isFish(s)&&random.nextInt(100)<35)s=new ItemStack(OrnamentalFish.ITEMS.get(random.nextInt(OrnamentalFish.ITEMS.size())));int k=OrnamentalFish.kind(s);int length=k<0?10+random.nextInt(51):random.nextInt(OrnamentalFish.MIN[k],OrnamentalFish.MAX[k]+1);var n=s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();n.putInt("neon_fish_cm",length);n.putString("neon_fish_id",UUID.randomUUID().toString());s.set(DataComponents.CUSTOM_DATA,CustomData.of(n));s.set(DataComponents.CUSTOM_NAME,Component.literal((k<0?s.getHoverName().getString():OrnamentalFish.NAMES[k])+" / "+length+" cm"));return s;}
 static void register(){var id=NeonWard.id("display_aquarium");BLOCK=Registry.register(BuiltInRegistries.BLOCK,id,new TankBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK,id)).strength(1.5f).noOcclusion().lightLevel(s->10)));NeonFurniture.BLOCKS.put("display_aquarium",BLOCK);Registry.register(BuiltInRegistries.ITEM,id,new BlockItem(BLOCK,new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id)).useBlockDescriptionPrefix()));TYPE=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,id,new BlockEntityType<>(Tank::new,Set.of(BLOCK)));}
 static void init(){UseBlockCallback.EVENT.register((p,l,h,hit)->{if(h!=InteractionHand.MAIN_HAND||!l.getBlockState(hit.getBlockPos()).is(BLOCK))return InteractionResult.PASS;
  if(p instanceof ServerPlayer sp&&l.getBlockEntity(hit.getBlockPos()) instanceof Tank tank){
   if(p.isSpectator()||CityProtection.structure(l,hit.getBlockPos())&&!HomeBuildingRules.editablePosition(l,p,hit.getBlockPos())||!tank.owner.isEmpty()&&!tank.owner.equals(p.getStringUUID())){p.sendOverlayMessage(Component.literal("自分の水槽だけ操作できます"));return InteractionResult.FAIL;}
   var held=p.getItemInHand(h);
   if(p.isShiftKeyDown()){if(tank.items.getFirst().isEmpty())p.sendOverlayMessage(Component.literal("魚を持って右クリックで入れます"));else if(p.getInventory().getFreeSlot()<0)p.sendOverlayMessage(Component.literal("魚を戻すため持ち物に空きを作ってください"));else{p.getInventory().add(tank.items.getFirst());tank.items.set(0,ItemStack.EMPTY);tank.clearDisplay();tank.setChanged();}}
   else if(fish(held)&&tank.items.getFirst().isEmpty()){tank.items.set(0,held.copyWithCount(1));held.shrink(1);tank.owner=p.getStringUUID();tank.setChanged();p.sendOverlayMessage(Component.literal("水槽に "+tank.items.getFirst().getHoverName().getString()+" を入れました"));}
   else p.sendOverlayMessage(Component.literal(tank.items.getFirst().isEmpty()?"魚を持って右クリック / しゃがみ右クリックで取り出す":tank.items.getFirst().getHoverName().getString()+" / しゃがみ右クリックで取り出す"));
  }return InteractionResult.SUCCESS;});}
 public static class TankBlock extends Block implements EntityBlock {
  TankBlock(BlockBehaviour.Properties p){super(p);}
  protected MapCodec<? extends Block> codec(){return simpleCodec(TankBlock::new);}
  public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Tank(p,s);}
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return l.isClientSide()||type!=TYPE?null:(world,pos,state,be)->((Tank)be).tick();}
 }
 public static class Tank extends BlockEntity {
  NonNullList<ItemStack> items=NonNullList.withSize(1,ItemStack.EMPTY);String owner="";int clock;Display.ItemDisplay ornament;List<Display.BlockDisplay> mesh=new ArrayList<>();
  Tank(BlockPos p,BlockState s){super(TYPE,p,s);}
  // Models are normalized to at most 0.46 blocks in every axis. Catch length is metadata only.
  void ornamentalTick(ServerLevel server){
   int kind=OrnamentalFish.kind(items.getFirst());boolean jelly=kind==5;
   if(ornament==null||!ornament.isAlive()){clearDisplay();ornament=new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY,server){@Override public boolean shouldBeSaved(){return false;}};
    String n="{item:{id:'neonward:"+OrnamentalFish.IDS[kind]+"',count:1},item_display:'none',brightness:{block:12,sky:12},teleport_duration:4,transformation:{scale:[0.46f,0.46f,0.46f]}}";
    try{ornament.load(TagValueInput.create(ProblemReporter.DISCARDING,server.registryAccess(),TagParser.parseCompoundFully(n)));}catch(Exception ex){throw new IllegalStateException(ex);}
    ornament.setPos(worldPosition.getX()+.5,worldPosition.getY()+.48,worldPosition.getZ()+.5);server.addFreshEntity(ornament);
   }
   double t=clock*(jelly?.012:.025),radius=jelly?.035:.10;
   ornament.setPos(worldPosition.getX()+.5+Math.cos(t)*radius,worldPosition.getY()+.48+Math.sin(t*(jelly?2:.7))*(jelly?.055:.018),worldPosition.getZ()+.5+Math.sin(t)*radius);
   ornament.setYRot((float)(jelly?t*12:-t*180/Math.PI));
  }
  protected void saveAdditional(ValueOutput out){super.saveAdditional(out);ContainerHelper.saveAllItems(out,items);out.putString("TankOwner",owner);}
  protected void loadAdditional(ValueInput in){super.loadAdditional(in);items=NonNullList.withSize(1,ItemStack.EMPTY);ContainerHelper.loadAllItems(in,items);owner=in.getStringOr("TankOwner","");}
  void clearDisplay(){for(var e:mesh)e.discard();mesh.clear();if(ornament!=null){ornament.discard();ornament=null;}}
  @Override public void setRemoved(){clearDisplay();super.setRemoved();}
  @Override public void preRemoveSideEffects(BlockPos p,BlockState s){clearDisplay();if(level!=null)Containers.dropContents(level,p,items);items.clear();super.preRemoveSideEffects(p,s);}
  void tick(){if(!(level instanceof ServerLevel server)||++clock%4!=0)return;if(items.getFirst().isEmpty()){clearDisplay();return;}if(OrnamentalFish.isFish(items.getFirst())){ornamentalTick(server);return;}if(mesh.isEmpty()||mesh.stream().anyMatch(e->!e.isAlive())){clearDisplay();String body=items.getFirst().is(Items.SALMON)?"red_terracotta":items.getFirst().is(Items.TROPICAL_FISH)?"orange_concrete":items.getFirst().is(Items.PUFFERFISH)?"yellow_terracotta":"light_gray_terracotta";
    double[][] dims={{-.14,0,-.06,.28,.12,.12},{-.24,.01,-.08,.10,.09,.16},{.08,.065,-.065,.035,.03,.015},{-.03,.10,-.02,.10,.05,.04}};
    for(int i=0;i<4;i++){var d=dims[i];var e=new Display.BlockDisplay(EntityTypes.BLOCK_DISPLAY,server){@Override public boolean shouldBeSaved(){return false;}};String block=i==2?"black_concrete":body;
     String n=String.format(Locale.ROOT,"{block_state:{Name:'minecraft:%s'},brightness:{block:12,sky:12},teleport_duration:4,transformation:{translation:[%sf,%sf,%sf],scale:[%sf,%sf,%sf]}}",block,d[0],d[1],d[2],d[3],d[4],d[5]);try{e.load(TagValueInput.create(ProblemReporter.DISCARDING,server.registryAccess(),TagParser.parseCompoundFully(n)));}catch(Exception ex){throw new IllegalStateException(ex);}e.setPos(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5);server.addFreshEntity(e);mesh.add(e);}
   }
   double angle=clock*.025,x=worldPosition.getX()+.5+Math.cos(angle)*.16,z=worldPosition.getZ()+.5+Math.sin(angle)*.14,y=worldPosition.getY()+.46+Math.sin(angle*.7)*.06;for(var e:mesh){e.setPos(x,y,z);e.setYRot((float)(-angle*180/Math.PI));}
  }
 }
}
