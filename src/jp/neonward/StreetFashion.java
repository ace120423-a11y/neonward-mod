package jp.neonward;
import java.util.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.item.equipment.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.Commands;
import net.minecraft.world.phys.Vec3;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.google.gson.JsonObject;

public final class StreetFashion {
 public static final String[] IDS={"kimono","kimono_crimson","kimono_ivory","denim","denim_washed","denim_black","leather","leather_wine","leather_white"};
 public static final String[] NAMES={"夜桜・藍の和装","紅蓮・赤の和装","月白・白の和装","インディゴ・Gジャン","ウォッシュド・Gジャン","スモーク・Gジャン","ブラックトップ・革ジャン","バーガンディ・革ジャン","アイボリー・革ジャン"};
 static final Map<Item,Item> LEGACY=new HashMap<>();
 public static final String[] PARTS={"アイウェア・帽子","ジャケット・トップス","パンツ","シューズ"};
 public static final String[] SUFFIX={"head","top","pants","shoes"};
 public static final EquipmentSlot[] SLOTS={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
 public static final List<Item> ITEMS=new ArrayList<>();
 public static final BlockPos SHOP=new BlockPos(248,65,485);
 public static Block COUNTER;
 public static Item.Properties cosmetic(Item.Properties p,String asset,EquipmentSlot slot){return p.stacksTo(1).component(DataComponents.EQUIPPABLE,Equippable.builder(slot).setAsset(ResourceKey.create(EquipmentAssets.ROOT_ID,NeonWard.id(asset))).setDamageOnHurt(false).build());}
 public static int price(int product){return new int[]{150,500,300,250}[product%4]+new int[]{300,350,400,100,150,150,200,250,300}[product/4];}
 public static void init(){
  for(String style:IDS)for(int j=0;j<4;j++){String id=style+"_"+SUFFIX[j];var p=new Item.Properties().setId(ResourceKey.create(Registries.ITEM,NeonWard.id(id)));ITEMS.add(Registry.register(BuiltInRegistries.ITEM,NeonWard.id(id),new Item(cosmetic(p,j==3?style+"_footwear":style,SLOTS[j]))));}
  String[] retired={"afterglow","executive","ronin","nomad","chrome","runner","velvet","hazard"};
  for(int k=0;k<retired.length;k++)for(int j=0;j<4;j++){String key=retired[k]+"_"+SUFFIX[j];var old=Registry.register(BuiltInRegistries.ITEM,NeonWard.id(key),new Item(cosmetic(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,NeonWard.id(key))),retired[k],SLOTS[j])));LEGACY.put(old,ITEMS.get((k==2?0:k==3||k==7?3:6)*4+j));}
  ServerTickEvents.END_SERVER_TICK.register(server->{if(server.getTickCount()%40==0)for(var player:server.getPlayerList().getPlayers())for(int i=0;i<player.getInventory().getContainerSize();i++){var old=player.getInventory().getItem(i);Item replacement=LEGACY.get(old.getItem());if(replacement!=null){player.getInventory().setItem(i,new ItemStack(replacement,old.getCount()));player.getInventory().setChanged();}}});
  var id=NeonWard.id("fashion_counter");COUNTER=Registry.register(BuiltInRegistries.BLOCK,id,new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK,id)).strength(3,8).noOcclusion().lightLevel(s->12)));
  Registry.register(BuiltInRegistries.ITEM,id,new BlockItem(COUNTER,new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id))));
  CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB,net.minecraft.resources.Identifier.withDefaultNamespace("combat"))).register(e->ITEMS.forEach(e::accept));
  CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB,net.minecraft.resources.Identifier.withDefaultNamespace("functional_blocks"))).register(e->e.accept(COUNTER));
  ServerTickEvents.END_SERVER_TICK.register(s->{if(s.getTickCount()%100==0){var l=s.overworld();if(l.isPositionEntityTicking(SHOP)){if(l.isEmptyBlock(SHOP)&&l.getBlockState(SHOP.below()).isSolid())l.setBlock(SHOP,COUNTER.defaultBlockState(),3);if(l.getBlockState(SHOP).is(COUNTER))display(l);}}});
  CommandRegistrationCallback.EVENT.register((d,c,e)->d.register(Commands.literal("neonfashion").then(Commands.argument("x",IntegerArgumentType.integer()).then(Commands.argument("y",IntegerArgumentType.integer()).then(Commands.argument("z",IntegerArgumentType.integer()).then(Commands.argument("product",IntegerArgumentType.integer(-1,IDS.length*4-1)).executes(ctx->request(ctx.getSource().getPlayerOrException(),new BlockPos(IntegerArgumentType.getInteger(ctx,"x"),IntegerArgumentType.getInteger(ctx,"y"),IntegerArgumentType.getInteger(ctx,"z")),IntegerArgumentType.getInteger(ctx,"product")))))))));
 }
 static int request(ServerPlayer p,BlockPos pos,int product){
  if(p.isSpectator()||p.distanceToSqr(Vec3.atCenterOf(pos))>36||!p.level().getBlockState(pos).is(COUNTER)||!TerminalRoutes.roomAllowed(p.level(),pos,1))return 0;
  if(StockMarket.ledger==null){reply(p,0,"口座を読み込めません");return 0;}
  var ledger=StockMarket.ledger;var account=ledger.account(p.getStringUUID());String msg="服は外見専用・防御力なし";
  if(product>=0){
   int free=-1;for(int i=0;i<36;i++)if(p.getInventory().getItem(i).isEmpty()){free=i;break;}
   if(free<0){reply(p,account.cash,"持ち物に空きを作ってね");return 0;}
   if(account.cash<price(product)){reply(p,account.cash,"残高が足りません");return 0;}
   account.cash-=price(product);
   try{StockMarket.save();}catch(Exception ex){account.cash+=price(product);reply(p,account.cash,"保存できませんでした。購入を取り消しました");return 0;}
   p.getInventory().setItem(free,new ItemStack(ITEMS.get(product)));p.getInventory().setChanged();
   msg=NAMES[product/4]+"を購入 / "+price(product)+" Cr";
  }else try{StockMarket.save();}catch(Exception ex){reply(p,account.cash,"口座を保存できません");return 0;}
  reply(p,account.cash,msg);return 1;
 }
 static void reply(ServerPlayer p,long cash,String message){var data=new JsonObject();data.addProperty("fashion_cash",cash);data.addProperty("message",message);ServerPlayNetworking.send(p,new StockMarket.Snapshot(data.toString()));}
 static void display(net.minecraft.server.level.ServerLevel l){
  for(var label:l.getEntitiesOfClass(net.minecraft.world.entity.Display.TextDisplay.class,new net.minecraft.world.phys.AABB(SHOP).inflate(3),e->e.entityTags().contains("nw_fashion_label")))((jp.neonward.mixin.MeterTextAccess)label).neonSetText(Component.literal("NEON THREADS / 服屋\n和服・Gジャン・革ジャン / 36点").withStyle(net.minecraft.ChatFormatting.AQUA));
  if(l.getEntitiesOfClass(net.minecraft.world.entity.Display.TextDisplay.class,new net.minecraft.world.phys.AABB(SHOP).inflate(3),e->e.entityTags().contains("nw_fashion_label")).isEmpty()){
   var t=new net.minecraft.world.entity.Display.TextDisplay(net.minecraft.world.entity.EntityTypes.TEXT_DISPLAY,l);
   try{t.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,l.registryAccess(),net.minecraft.nbt.TagParser.parseCompoundFully("{text:{text:'NEON THREADS / 服屋\\n右クリックで購入・36点',color:'aqua'},billboard:'center',line_width:260,background:0,brightness:{block:15,sky:15},Invulnerable:1b,Tags:['nw_fashion_label']}")));}catch(Exception ex){throw new IllegalStateException(ex);}
   t.setPos(248.5,66.5,485.5);l.addFreshEntity(t);
  }
  int[] xs={250,256,259},styles={0,3,6};
  for(int i=0;i<3;i++){
   var at=new BlockPos(xs[i],65,482);String tag="nw_fashion_model_"+i;
   var existing=l.getEntitiesOfClass(net.minecraft.world.entity.decoration.ArmorStand.class,new net.minecraft.world.phys.AABB(at).inflate(1),e->e.entityTags().contains(tag));for(var stand:existing)for(int j=0;j<4;j++)if(!stand.getItemBySlot(SLOTS[j]).is(ITEMS.get(styles[i]*4+j)))stand.setItemSlot(SLOTS[j],new ItemStack(ITEMS.get(styles[i]*4+j)));
   if(!l.isEmptyBlock(at)||!l.isEmptyBlock(at.above())||!l.getEntitiesOfClass(net.minecraft.world.entity.decoration.ArmorStand.class,new net.minecraft.world.phys.AABB(at).inflate(1),e->e.entityTags().contains(tag)).isEmpty())continue;
   var stand=new net.minecraft.world.entity.decoration.ArmorStand(net.minecraft.world.entity.EntityTypes.ARMOR_STAND,l);
   try{stand.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,l.registryAccess(),net.minecraft.nbt.TagParser.parseCompoundFully("{NoGravity:1b,Invulnerable:1b,ShowArms:1b,DisabledSlots:4144959,Tags:['"+tag+"']}")));}catch(Exception ex){throw new IllegalStateException(ex);}
   for(int j=0;j<4;j++)stand.setItemSlot(SLOTS[j],new ItemStack(ITEMS.get(styles[i]*4+j)));stand.setPos(xs[i]+.5,65,482.5);stand.setYRot(0);l.addFreshEntity(stand);
  }
 }
}
