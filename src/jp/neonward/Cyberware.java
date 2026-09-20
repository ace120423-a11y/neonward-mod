package jp.neonward;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.commands.Commands;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.phys.Vec3;
import com.google.gson.*;

public final class Cyberware {
 static final int[] SELL_PRICES={250,500,1000,2000,4000};
 public static Block TERMINAL;
 public static final java.util.List<Item> ITEMS=new java.util.ArrayList<>();
 public static final BlockPos CLINIC=new BlockPos(428,65,475);
 public static void init(){
  for(var part:CyberwareCatalog.PARTS){var id=NeonWard.id(part.id());var lore=new net.minecraft.world.item.component.ItemLore(java.util.List.of(net.minecraft.network.chat.Component.literal(CyberwareCatalog.RARITIES[part.tier()]+" / "+CyberwareCatalog.SLOTS[part.slot()]).withStyle(st->st.withColor(CyberwareCatalog.COLORS[part.tier()])),net.minecraft.network.chat.Component.literal(part.effect()).withStyle(net.minecraft.ChatFormatting.AQUA),net.minecraft.network.chat.Component.literal("敵から入手 / 診療所で装着・取り外し")));ITEMS.add(Registry.register(BuiltInRegistries.ITEM,id,new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id)).stacksTo(16).component(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE,part.tier()>=3).component(net.minecraft.core.component.DataComponents.ITEM_NAME,net.minecraft.network.chat.Component.translatable("item.neonward."+part.id()).withStyle(st->st.withColor(CyberwareCatalog.COLORS[part.tier()]))).component(net.minecraft.core.component.DataComponents.LORE,lore))));}
  CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB,net.minecraft.resources.Identifier.withDefaultNamespace("combat"))).register(e->ITEMS.forEach(e::accept));
  net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH.register((entity,source)->{if(!Underworld.hunter(entity)&&entity instanceof net.minecraft.world.entity.monster.Enemy&&source.getEntity() instanceof ServerPlayer&&entity.level() instanceof net.minecraft.server.level.ServerLevel level){int part=CyberwareCatalog.rollDrop(java.util.concurrent.ThreadLocalRandom.current(),NeonLoot.profile(entity),GunEnchantments.looting(source,level));if(part>=0)level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(level,entity.getX(),entity.getY()+.3,entity.getZ(),stack(part,CyberwareCatalog.rollValue(part,java.util.concurrent.ThreadLocalRandom.current()))));}});

  var id=NeonWard.id("cyberware_terminal");TERMINAL=Registry.register(BuiltInRegistries.BLOCK,id,new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK,id)).strength(3,8).noOcclusion().lightLevel(s->12)));
  Registry.register(BuiltInRegistries.ITEM,id,new BlockItem(TERMINAL,new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id))));
  CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB,net.minecraft.resources.Identifier.withDefaultNamespace("functional_blocks"))).register(e->e.accept(TERMINAL));
  CommandRegistrationCallback.EVENT.register((d,c,e)->{var root=Commands.literal("neoncyber");for(String action:new String[]{"view","install","remove","sell"})root.then(Commands.literal(action).then(Commands.argument("x",IntegerArgumentType.integer()).then(Commands.argument("y",IntegerArgumentType.integer()).then(Commands.argument("z",IntegerArgumentType.integer()).then(Commands.argument("part",IntegerArgumentType.integer(-1,CyberwareCatalog.PARTS.length-1)).executes(ctx->request(ctx.getSource().getPlayerOrException(),new BlockPos(IntegerArgumentType.getInteger(ctx,"x"),IntegerArgumentType.getInteger(ctx,"y"),IntegerArgumentType.getInteger(ctx,"z")),action,IntegerArgumentType.getInteger(ctx,"part"))))))));d.register(root);});
  ServerTickEvents.END_SERVER_TICK.register(s->{if(StockMarket.ledger!=null&&s.getTickCount()%20==0)for(var p:s.getPlayerList().getPlayers())apply(p);if(s.getTickCount()%100==0){var l=s.overworld();if(l.isPositionEntityTicking(CLINIC)&&l.isEmptyBlock(CLINIC)&&l.getBlockState(CLINIC.below()).isSolid())l.setBlock(CLINIC,TERMINAL.defaultBlockState(),3);}});
 }
 public static ItemStack stack(int id,int bp){int value=CyberwareCatalog.clampRoll(id,bp);var part=CyberwareCatalog.PARTS[id];var stack=new ItemStack(ITEMS.get(id));net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,stack,tag->tag.putInt("neon_cyber_roll",value));stack.set(net.minecraft.core.component.DataComponents.LORE,new net.minecraft.world.item.component.ItemLore(java.util.List.of(net.minecraft.network.chat.Component.literal(CyberwareCatalog.RARITIES[part.tier()]+" / "+CyberwareCatalog.SLOTS[part.slot()]).withStyle(st->st.withColor(CyberwareCatalog.COLORS[part.tier()])),net.minecraft.network.chat.Component.literal(CyberwareCatalog.effect(id,value)).withStyle(net.minecraft.ChatFormatting.AQUA),net.minecraft.network.chat.Component.literal("固定個体値 / 診療所で装着・取り外し"))));return stack;}
 static int value(ItemStack stack,int id){var data=stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY);return CyberwareCatalog.clampRoll(id,data.copyTag().getIntOr("neon_cyber_roll",CyberwareCatalog.MIN[CyberwareCatalog.PARTS[id].tier()]));}
 public static double defense(ServerPlayer p){if(StockMarket.ledger==null)return 0;var a=StockMarket.ledger.accounts.get(p.getStringUUID());return a==null||a.cyberVersion<3?0:CyberwareCatalog.bonuses(a)[1];}
 static boolean canEdit(ServerPlayer p,BlockPos pos){var b=p.level().getBlockState(pos).getBlock();boolean station=b==TERMINAL||b==NeonFurniture.BLOCKS.get("med_workbench")||b==NeonFurniture.BLOCKS.get("med_counter");boolean personal=pos.closerToCenterThan(p.position(),2.0);return !p.isSpectator()&&((p.distanceToSqr(Vec3.atCenterOf(pos))<=36&&station)||personal);}
 static boolean canSell(ServerPlayer p,BlockPos pos){return !p.isSpectator()&&p.level().getBlockState(pos).is(GuildServices.EXCHANGE)&&p.distanceToSqr(Vec3.atCenterOf(pos))<=36;}
 static java.util.List<ItemStack> inventory(ServerPlayer p){var copy=new java.util.ArrayList<ItemStack>();for(int i=0;i<p.getInventory().getContainerSize();i++)copy.add(p.getInventory().getItem(i).copy());return copy;}
 static void restore(ServerPlayer p,java.util.List<ItemStack> copy){for(int i=0;i<copy.size();i++)p.getInventory().setItem(i,copy.get(i));p.getInventory().setChanged();p.containerMenu.broadcastChanges();}
 static MarketLedger.Account prepare(ServerPlayer p){if(StockMarket.ledger==null)return null;var a=StockMarket.ledger.account(p.getStringUUID());String before=StockMarket.JSON.toJson(a);if(CyberwareCatalog.migrate(a))try{StockMarket.save();}catch(Exception ex){StockMarket.ledger.accounts.put(p.getStringUUID(),StockMarket.JSON.fromJson(before,MarketLedger.Account.class));return null;}if(!a.cyberPending.isEmpty()){var inv=inventory(p);int id=a.cyberPending.getFirst();if(p.getInventory().add(stack(id,CyberwareCatalog.MIN[CyberwareCatalog.PARTS[id].tier()]))){a.cyberPending.removeFirst();try{StockMarket.save();p.getInventory().setChanged();}catch(Exception ex){a.cyberPending.addFirst(id);restore(p,inv);}}else restore(p,inv);}return a;}
 static int request(ServerPlayer p,BlockPos pos,String action,int part){
  boolean edit=canEdit(p,pos),sell=canSell(p,pos);String message="敵から入手 / 同じ部位に1個 / 容量制限なし";
  var a=prepare(p);if(a==null){reply(p,pos,false,"データを保存・読み込みできません");return 0;}
  if(!action.equals("view")){
   if((action.equals("sell")?!edit&&!sell:!edit)){reply(p,pos,false,action.equals("sell")?"診療所または換金所で売却できます":"装着・取り外しは診療所の端末で行えます");return 0;}
   String before=StockMarket.JSON.toJson(a);var inv=inventory(p);boolean changed=false;
   if(action.equals("remove")&&part>=0&&part<10){Integer old=a.cyberSlots.get(part);if(old==null)message="この部位は未装着です";else if(p.getInventory().add(stack(old,a.cyberRolls.getOrDefault(CyberwareCatalog.PARTS[old].slot(),CyberwareCatalog.MIN[CyberwareCatalog.PARTS[old].tier()])))){a.cyberSlots.remove(part);a.cyberRolls.remove(part);changed=true;message="取り外したパーツを持ち物に戻しました";}else message="持ち物に空きを作ってください";}
   else if(action.equals("sell")&&part>=0&&part<CyberwareCatalog.PARTS.length){int found=-1;for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).is(ITEMS.get(part))){found=i;break;}if(found<0)message="売却できる余りのパーツを持っていません";else{int price=SELL_PRICES[CyberwareCatalog.PARTS[part].tier()];p.getInventory().getItem(found).shrink(1);a.cash+=price;changed=true;message=CyberwareCatalog.PARTS[part].name()+"を"+price+" Crで売却しました";}}
   else if(action.equals("install")&&part>=0&&part<CyberwareCatalog.PARTS.length){var implant=CyberwareCatalog.PARTS[part];Integer old=a.cyberSlots.get(implant.slot());int found=-1,best=-1;for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).is(ITEMS.get(part))&&value(p.getInventory().getItem(i),part)>best){found=i;best=value(p.getInventory().getItem(i),part);}
    if(java.util.Objects.equals(old,part)&&a.cyberRolls.getOrDefault(implant.slot(),0)>=best)message="装着中の個体値が同じか上です";
    else if(found<0)message="このパーツを持っていません。敵から入手できます";
    else {p.getInventory().getItem(found).shrink(1);if(old!=null&&!p.getInventory().add(stack(old,a.cyberRolls.getOrDefault(CyberwareCatalog.PARTS[old].slot(),CyberwareCatalog.MIN[CyberwareCatalog.PARTS[old].tier()]))))message="交換前のパーツを戻す空きが必要です";else{a.cyberSlots.put(implant.slot(),part);a.cyberRolls.put(implant.slot(),best);changed=true;message=implant.name()+" / "+CyberwareCatalog.RARITIES[implant.tier()]+"を装着";}}
   }else message="不明な操作です";
   if(changed){try{StockMarket.save();p.getInventory().setChanged();p.containerMenu.broadcastChanges();}catch(Exception ex){StockMarket.ledger.accounts.put(p.getStringUUID(),StockMarket.JSON.fromJson(before,MarketLedger.Account.class));restore(p,inv);message="保存できなかったため変更を取り消しました";}}else restore(p,inv);
   apply(p);
  }
  reply(p,pos,edit,message);return 1;
 }
 static void reply(ServerPlayer p,BlockPos pos,boolean edit,String message){var o=new JsonObject();o.addProperty("cyberware",true);o.addProperty("editable",edit);o.addProperty("sellable",edit||canSell(p,pos));o.addProperty("message",message);o.addProperty("x",pos.getX());o.addProperty("y",pos.getY());o.addProperty("z",pos.getZ());int[] counts=new int[CyberwareCatalog.PARTS.length],best=new int[CyberwareCatalog.PARTS.length];for(int j=0;j<p.getInventory().getContainerSize();j++){var stack=p.getInventory().getItem(j);int i=ITEMS.indexOf(stack.getItem());if(i>=0){counts[i]+=stack.getCount();best[i]=Math.max(best[i],value(stack,i));}}o.add("rolls",StockMarket.JSON.toJsonTree(best));o.add("inventory",StockMarket.JSON.toJsonTree(counts));if(StockMarket.ledger!=null)o.add("account",StockMarket.JSON.toJsonTree(StockMarket.ledger.account(p.getStringUUID())));ServerPlayNetworking.send(p,new StockMarket.Snapshot(o.toString()));}
 static void apply(ServerPlayer p){var a=prepare(p);double[] b=new double[9];if(a!=null){CyberwareCatalog.normalize(a);b=CyberwareCatalog.bonuses(a);}b[1]=0;var attrs=java.util.List.of(Attributes.MAX_HEALTH,Attributes.ARMOR,Attributes.ATTACK_DAMAGE,Attributes.MOVEMENT_SPEED,Attributes.ATTACK_SPEED,Attributes.KNOCKBACK_RESISTANCE,Attributes.BLOCK_BREAK_SPEED,Attributes.JUMP_STRENGTH,Attributes.LUCK);
  for(int i=0;i<b.length;i++){var attribute=p.getAttribute(attrs.get(i));if(attribute==null)continue;var id=NeonWard.id("cyberware_"+i);var old=attribute.getModifier(id);var op=(i==0||i==2||i==3||i==4||i==6||i==7)?AttributeModifier.Operation.ADD_MULTIPLIED_BASE:AttributeModifier.Operation.ADD_VALUE;if(old!=null&&old.amount()==b[i]&&old.operation()==op)continue;if(old!=null)attribute.removeModifier(id);if(b[i]!=0)attribute.addTransientModifier(new AttributeModifier(id,b[i],op));}
  if(p.getHealth()>p.getMaxHealth())p.setHealth(p.getMaxHealth());
 }
}
