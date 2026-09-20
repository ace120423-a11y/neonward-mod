package jp.neonward;
import java.util.*;
import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.Commands;
public final class Medicine {
 static Block HERB;static Item LEAF;public static final String[] NAMES={"MED-4 回復アンプル","MED-8 強化回復アンプル","REGEN 再生アンプル","空き瓶"};static final int[] COST={120,300,260,12},HERBS={2,5,4,0};
 static class Herb extends BushBlock {Herb(BlockBehaviour.Properties p){super(p);}}
 static void register(){NeonFurniture.register("med_workbench","desk",8);NeonFurniture.register("med_counter","desk",8);var id=NeonWard.id("wild_medicinal_herb");HERB=Registry.register(BuiltInRegistries.BLOCK,id,new Herb(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK,id)).noCollision().noOcclusion().instabreak().sound(SoundType.GRASS).lightLevel(s->5)));var leaf=NeonWard.id("medicinal_leaf");LEAF=Registry.register(BuiltInRegistries.ITEM,leaf,new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,leaf))));}
 static ItemStack product(int i){if(i==3){var s=new ItemStack(Items.GLASS_BOTTLE);s.set(DataComponents.CUSTOM_NAME,Component.literal(NAMES[i]));return s;}var s=new ItemStack(Items.POTION);s.set(DataComponents.POTION_CONTENTS,new PotionContents(i==0?Potions.HEALING:i==1?Potions.STRONG_HEALING:Potions.REGENERATION));s.set(DataComponents.CUSTOM_NAME,Component.literal(NAMES[i]));s.set(DataComponents.ITEM_MODEL,NeonWard.id("med_ampoule_"+i));return s;}
 static void init(){CommandRegistrationCallback.EVENT.register((d,c,e)->{var root=Commands.literal("neonmed");for(String action:List.of("view","buy","mix"))root.then(Commands.literal(action).then(Commands.argument("x",IntegerArgumentType.integer()).then(Commands.argument("y",IntegerArgumentType.integer()).then(Commands.argument("z",IntegerArgumentType.integer()).then(Commands.argument("product",IntegerArgumentType.integer(-1,3)).executes(ctx->request(ctx.getSource().getPlayerOrException(),new BlockPos(IntegerArgumentType.getInteger(ctx,"x"),IntegerArgumentType.getInteger(ctx,"y"),IntegerArgumentType.getInteger(ctx,"z")),action,IntegerArgumentType.getInteger(ctx,"product"))))))));d.register(root);});
 UseBlockCallback.EVENT.register((p,l,h,hit)->{var b=l.getBlockState(hit.getBlockPos()).getBlock();if(h==InteractionHand.MAIN_HAND&&(b==NeonFurniture.BLOCKS.get("med_workbench")||b==NeonFurniture.BLOCKS.get("med_counter"))){if(p instanceof ServerPlayer sp)request(sp,hit.getBlockPos(),"view",-1);return InteractionResult.SUCCESS;}return InteractionResult.PASS;});
 PlayerBlockBreakEvents.AFTER.register((l,p,pos,s,be)->{if(s.is(HERB)&&p instanceof ServerPlayer sp&&NeonZones.isField(l,pos)&&StockMarket.ledger!=null){var a=StockMarket.ledger.account(p.getStringUUID());if(a.guildQuests.containsKey(6)){int old=a.guildQuests.get(6),goal=GuildRanks.accepted(a,6).target();if(old<goal){a.guildQuests.put(6,old+1);try{StockMarket.save();sp.sendOverlayMessage(Component.literal("薬草採取 "+(old+1)+" / "+goal+(old+1==goal?" / 受付で報告できます":"")));}catch(Exception ex){a.guildQuests.put(6,old);}}}}});
 ServerTickEvents.END_SERVER_TICK.register(server->{if(server.getTickCount()%200!=0)return;for(var p:server.getPlayerList().getPlayers()){var l=p.level();if(!NeonZones.isField(l,p.blockPosition()))continue;for(int n=0;n<12;n++){int x=p.blockPosition().getX()+l.getRandom().nextInt(49)-24,z=p.blockPosition().getZ()+l.getRandom().nextInt(49)-24;if(!l.hasChunkAt(new BlockPos(x,64,z)))continue;var at=l.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,new BlockPos(x,0,z));if(NeonZones.isField(l,at)&&l.isEmptyBlock(at)&&l.getBlockState(at.below()).is(Blocks.GRASS_BLOCK)&&l.canSeeSky(at)){l.setBlock(at,HERB.defaultBlockState(),3);break;}}}});}
 static int count(ServerPlayer p,Item item){int n=0;for(int i=0;i<36;i++)if(p.getInventory().getItem(i).is(item))n+=p.getInventory().getItem(i).getCount();return n;}
 static void take(ServerPlayer p,Item item,int n){for(int i=0;i<36&&n>0;i++){var s=p.getInventory().getItem(i);if(s.is(item)){int k=Math.min(n,s.getCount());s.shrink(k);n-=k;}}}
 static int request(ServerPlayer p,BlockPos pos,String action,int i){if(p.isSpectator()||p.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)>36||StockMarket.ledger==null)return 0;var b=p.level().getBlockState(pos).getBlock();boolean shop=b==NeonFurniture.BLOCKS.get("med_counter");if(!shop&&b!=NeonFurniture.BLOCKS.get("med_workbench"))return 0;if(p.level().dimension()==PrivateHomes.DIMENSION&&!PrivateHomes.ownsPosition(p))return 0;if(p.level().dimension()==PrivateFarms.DIM&&!PrivateFarms.owns(p.level(),p,pos))return 0;var a=StockMarket.ledger.account(p.getStringUUID());String msg=shop?"完成品をCrで購入できます":"薬草＋空き瓶で調合 / 外域の青緑の薬草を採取";
 if(i>=0&&i<4&&!action.equals("view")){int free=-1;for(int n=0;n<36;n++)if(p.getInventory().getItem(n).isEmpty()){free=n;break;}if(free<0)msg="持ち物に空きを作ってね";else if(shop&&!action.equals("buy")||!shop&&(!action.equals("mix")||i==3))msg="この設備では選べません";else if(shop&&a.cash<COST[i])msg="残高が足りません";else if(!shop&&(count(p,LEAF)<HERBS[i]||count(p,Items.GLASS_BOTTLE)<1))msg="薬草"+HERBS[i]+"個と空き瓶1個が必要です";else {long before=a.cash;var items=new ArrayList<ItemStack>();for(int n=0;n<36;n++)items.add(p.getInventory().getItem(n).copy());try{if(shop)a.cash-=COST[i];else{take(p,LEAF,HERBS[i]);take(p,Items.GLASS_BOTTLE,1);}StockMarket.save();p.getInventory().setItem(free,product(i));p.getInventory().setChanged();p.containerMenu.broadcastChanges();msg=NAMES[i]+(shop?"を購入しました":"を調合しました");}catch(Exception ex){a.cash=before;for(int n=0;n<36;n++)p.getInventory().setItem(n,items.get(n));msg="保存できないため取り消しました";}}}
 var o=new JsonObject();o.addProperty("medicine",true);o.addProperty("shop",shop);o.addProperty("x",pos.getX());o.addProperty("y",pos.getY());o.addProperty("z",pos.getZ());o.addProperty("cash",a.cash);o.addProperty("herbs",count(p,LEAF));o.addProperty("bottles",count(p,Items.GLASS_BOTTLE));o.addProperty("message",msg);ServerPlayNetworking.send(p,new StockMarket.Snapshot(o.toString()));return 1;}
}
