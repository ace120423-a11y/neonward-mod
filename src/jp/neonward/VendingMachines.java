package jp.neonward;
import java.util.*;
import com.mojang.serialization.MapCodec;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.commands.Commands;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import net.minecraft.util.RandomSource;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class VendingMachines {
 public static Machine BLOCK;public static Item SODA,COFFEE,BAR;
 public static final String[] NAMES={"ネオンソーダ","ナイトシフト・コーヒー","パワーバー"};
 public static final int[] PRICES={120,180,240};
 static final Map<UUID,Long> last=new HashMap<>();
 static Item food(String name,int nutrition,boolean drink){var id=NeonWard.id(name);return Registry.register(BuiltInRegistries.ITEM,id,new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id)).food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(nutrition).saturationModifier(.6f).alwaysEdible().build(),drink?net.minecraft.world.item.component.Consumables.DEFAULT_DRINK:net.minecraft.world.item.component.Consumables.DEFAULT_FOOD)));}
 public static void init(){
  net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server->last.clear());
  SODA=food("neon_soda",3,true);COFFEE=food("night_coffee",4,true);BAR=food("power_bar",6,false);
  var id=NeonWard.id("vending_machine");BLOCK=new Machine(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK,id)).strength(3,8).sound(SoundType.METAL).noOcclusion().lightLevel(s->15));Registry.register(BuiltInRegistries.BLOCK,id,BLOCK);Registry.register(BuiltInRegistries.ITEM,id,new BlockItem(BLOCK,new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id)).useBlockDescriptionPrefix()));
  CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB,net.minecraft.resources.Identifier.withDefaultNamespace("functional_blocks"))).register(e->e.accept(BLOCK));
  CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB,net.minecraft.resources.Identifier.withDefaultNamespace("food_and_drinks"))).register(e->{e.accept(SODA);e.accept(COFFEE);e.accept(BAR);});
  CommandRegistrationCallback.EVENT.register((d,c,e)->{
   var product=Commands.argument("product",IntegerArgumentType.integer(0,2)).executes(ctx->buy(ctx.getSource().getPlayerOrException(),new BlockPos(IntegerArgumentType.getInteger(ctx,"x"),IntegerArgumentType.getInteger(ctx,"y"),IntegerArgumentType.getInteger(ctx,"z")),IntegerArgumentType.getInteger(ctx,"product")));
   d.register(Commands.literal("neonvend").then(Commands.argument("x",IntegerArgumentType.integer()).then(Commands.argument("y",IntegerArgumentType.integer()).then(Commands.argument("z",IntegerArgumentType.integer()).then(product)))));
  });
 }
 static int message(ServerPlayer p,String s){p.sendOverlayMessage(Component.literal(s));return 0;}
 static int buy(ServerPlayer p,BlockPos pos,int product){
  if(p.isSpectator()||p.distanceToSqr(Vec3.atCenterOf(pos))>36)return 0;
  var state=p.level().getBlockState(pos);if(!state.is(BLOCK)||state.getValue(Machine.PART)!=0||!p.level().getBlockState(pos.above()).is(BLOCK))return 0;
  long now=p.level().getGameTime();if(now-last.getOrDefault(p.getUUID(),-100L)<20)return message(p,"商品を取り出しています。少し待ってね。");
  var stack=new ItemStack(new Item[]{SODA,COFFEE,BAR}[product]);if(StockMarket.ledger==null)return message(p,"街通貨の台帳を読み込めません。");var account=StockMarket.ledger.account(p.getStringUUID());
  boolean room=false;for(int i=0;i<36;i++){var s=p.getInventory().getItem(i);if(s.isEmpty()||ItemStack.isSameItemSameComponents(s,stack)&&s.getCount()<s.getMaxStackSize())room=true;}
  if(!room)return message(p,"持ち物に空きを作ってください。");if(account.cash<PRICES[product])return message(p,"Crが足りません（必要額 "+PRICES[product]+" Cr）。");
  var before=new ArrayList<ItemStack>();for(int i=0;i<36;i++)before.add(p.getInventory().getItem(i).copy());long oldCash=account.cash;account.cash-=PRICES[product];
  try{if(!p.getInventory().add(stack))throw new IllegalStateException("inventory full");StockMarket.save();p.getInventory().setChanged();last.put(p.getUUID(),now);p.level().playSound(null,pos,net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.value(),net.minecraft.sounds.SoundSource.BLOCKS,.6f,1.4f);message(p,NAMES[product]+"を購入しました（"+PRICES[product]+" Cr）。");return 1;}catch(Exception ex){account.cash=oldCash;for(int i=0;i<before.size();i++)p.getInventory().setItem(i,before.get(i));p.getInventory().setChanged();return message(p,"購入を保存できなかったため取り消しました。");}
 }
 public static class Machine extends HorizontalDirectionalBlock {
  public static final IntegerProperty PART=IntegerProperty.create("part",0,1);public static final MapCodec<Machine> CODEC=simpleCodec(Machine::new);
  Machine(BlockBehaviour.Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(PART,0));}
  @Override public MapCodec<? extends HorizontalDirectionalBlock> codec(){return CODEC;}
  @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,PART);}
  @Override public BlockState getStateForPlacement(BlockPlaceContext c){var p=c.getClickedPos();return p.getY()+1<c.getLevel().getMaxY()&&c.getLevel().isEmptyBlock(p.above())&&c.getLevel().getBlockState(p.below()).isFaceSturdy(c.getLevel(),p.below(),Direction.UP)?defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite()):null;}
  @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity e,ItemStack stack){l.setBlock(p.above(),s.setValue(PART,1),3);}
  @Override protected boolean canSurvive(BlockState s,LevelReader l,BlockPos p){var below=l.getBlockState(p.below());return s.getValue(PART)==0?below.isFaceSturdy(l,p.below(),Direction.UP):below.is(this)&&below.getValue(PART)==0&&below.getValue(FACING)==s.getValue(FACING);}
  @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess t,BlockPos p,Direction d,BlockPos q,BlockState other,RandomSource r){return d==Direction.DOWN&&!canSurvive(s,l,p)?Blocks.AIR.defaultBlockState():super.updateShape(s,l,t,p,d,q,other,r);}
  @Override public BlockState playerWillDestroy(Level l,BlockPos p,BlockState s,Player player){if(!l.isClientSide()&&s.getValue(PART)==1&&l.getBlockState(p.below()).is(this))l.destroyBlock(p.below(),!player.isCreative());return super.playerWillDestroy(l,p,s,player);}
  @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Block.box(1,0,1,15,16,15);}
  @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult h){return InteractionResult.SUCCESS;}
 }
}
