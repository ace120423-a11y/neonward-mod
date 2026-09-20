package jp.neonward;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.*;
import net.minecraft.world.item.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.commands.Commands;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import com.google.gson.JsonObject;
import java.util.*;
public final class PrivateFarms {
 public static final ResourceKey<Level> DIM=ResourceKey.create(Registries.DIMENSION,NeonWard.id("private_farms"));static final int PRICE=5000;static final BlockPos COUNTER=new BlockPos(327,65,634);
 static BlockPos origin(int slot){return new BlockPos((slot%512)*128,0,(slot/512)*128);}
 static boolean owns(Level l,Player p,BlockPos pos){if(l.dimension()!=DIM||p==null)return false;if(l.isClientSide())return Math.floorDiv(pos.getX(),128)==Math.floorDiv(p.blockPosition().getX(),128)&&Math.floorDiv(pos.getZ(),128)==Math.floorDiv(p.blockPosition().getZ(),128);if(StockMarket.ledger==null)return false;var a=StockMarket.ledger.account(p.getStringUUID());if(a.farmSlot<=0)return false;var o=origin(a.farmSlot);return pos.getX()>=o.getX()&&pos.getX()<=o.getX()+24&&pos.getZ()>=o.getZ()&&pos.getZ()<=o.getZ()+24&&pos.getY()>=63&&pos.getY()<=83;}
 static boolean edit(Level l,Player p,BlockPos pos){if(!owns(l,p,pos))return false;var o=l.isClientSide()?new BlockPos(Math.floorDiv(p.blockPosition().getX(),128)*128,0,Math.floorDiv(p.blockPosition().getZ(),128)*128):origin(StockMarket.ledger.account(p.getStringUUID()).farmSlot);int x=pos.getX()-o.getX(),z=pos.getZ()-o.getZ();return x>=2&&x<=21&&z>=4&&z<=21&&pos.getY()>=64&&pos.getY()<=76;}
 static boolean hoe(Level l,Player p,BlockPos pos,ItemStack s){return edit(l,p,pos)&&pos.getY()==64&&s.getItem() instanceof HoeItem;}
 static void init(){CommandRegistrationCallback.EVENT.register((d,c,e)->{var root=Commands.literal("neonfarm");for(String a:List.of("view","buy","enter","leave"))root.then(Commands.literal(a).executes(ctx->request(ctx.getSource().getPlayerOrException(),a)));d.register(root);});
 UseBlockCallback.EVENT.register((p,l,h,hit)->{if(h!=InteractionHand.MAIN_HAND)return InteractionResult.PASS;if(l.dimension()==Level.OVERWORLD&&hit.getBlockPos().equals(COUNTER)){if(p instanceof ServerPlayer sp)request(sp,"view");return InteractionResult.SUCCESS;}if(l.dimension()==DIM){if(!owns(l,p,hit.getBlockPos()))return InteractionResult.FAIL;var a=StockMarket.ledger.account(p.getStringUUID());if(hit.getBlockPos().equals(origin(a.farmSlot).offset(12,65,1))){if(p instanceof ServerPlayer sp)leave(sp);return InteractionResult.SUCCESS;}}return InteractionResult.PASS;});
 ServerTickEvents.END_SERVER_TICK.register(s->{if(s.getTickCount()%20==0)for(var p:s.getPlayerList().getPlayers())if(p.level().dimension()==DIM&&!owns(p.level(),p,p.blockPosition()))leave(p);});}
 static String buy(MarketLedger l,String id){var a=l.account(id);if(a.farmSlot>0)return "購入済みです";if(a.cash<PRICE)return "残高が足りません";int n=Math.max(1,l.nextFarmSlot);for(var other:l.accounts.values())n=Math.max(n,other.farmSlot+1);if(n>=262144)return "区画が満室です";a.cash-=PRICE;a.farmSlot=n;l.nextFarmSlot=n+1;return "個別農地を購入しました";}
 static int request(ServerPlayer p,String action){if(p.isSpectator()||StockMarket.ledger==null)return 0;if(action.equals("leave")&&p.level().dimension()==DIM){leave(p);return 1;}if(p.level().dimension()!=Level.OVERWORLD||p.distanceToSqr(327.5,65,634.5)>64)return 0;String msg="自分専用の20×18区画 / 5,000 Cr / 水路・道具・種付き";
 if(action.equals("buy")){String before=StockMarket.JSON.toJson(StockMarket.ledger);try{msg=buy(StockMarket.ledger,p.getStringUUID());StockMarket.save();}catch(Exception ex){StockMarket.ledger=StockMarket.JSON.fromJson(before,MarketLedger.class);msg="保存できないため購入を取り消しました";}}
 var a=StockMarket.ledger.account(p.getStringUUID());if(action.equals("enter")&&a.farmSlot>0){var l=p.level().getServer().getLevel(DIM);if(l==null)return 0;try{if(!a.farmReady){build(l,a.farmSlot);l.getChunkSource().save(true);a.farmReady=true;try{StockMarket.save();}catch(Exception ex){a.farmReady=false;throw ex;}}var o=origin(a.farmSlot);if(p.isPassenger())p.stopRiding();p.teleportTo(l,o.getX()+12.5,65,o.getZ()+2.5,Set.of(),0,0,true);return 1;}catch(Exception ex){msg="区画を保存できませんでした。再度入場してください";}}
 JsonObject o=new JsonObject();o.addProperty("farm",true);o.addProperty("cash",a.cash);o.addProperty("owned",a.farmSlot>0);o.addProperty("message",msg);ServerPlayNetworking.send(p,new StockMarket.Snapshot(o.toString()));return 1;}
 static void leave(ServerPlayer p){if(p.isPassenger())p.stopRiding();p.teleportTo(p.level().getServer().overworld(),327.5,65,636.5,Set.of(),0,0,true);}
 static void build(ServerLevel l,int slot){var o=origin(slot);for(int x=0;x<=24;x++)for(int z=0;z<=24;z++){var floor=o.offset(x,63,z);if(l.isEmptyBlock(floor))l.setBlock(floor,Blocks.BEDROCK.defaultBlockState(),2);var soil=o.offset(x,64,z);if(l.isEmptyBlock(soil))l.setBlock(soil,(x>=2&&x<=21&&z>=4&&z<=21?Blocks.DIRT:Blocks.POLISHED_DEEPSLATE).defaultBlockState(),2);if(x==0||x==24||z==0||z==24){if(l.isEmptyBlock(o.offset(x,65,z)))l.setBlock(o.offset(x,65,z),Blocks.OAK_FENCE.defaultBlockState(),3);}}
 for(int x:new int[]{5,14})for(int z:new int[]{8,17}){l.setBlock(o.offset(x,64,z),Blocks.WATER.defaultBlockState(),3);}
 for(int x:new int[]{1,23})for(int z:new int[]{3,12,23})if(l.isEmptyBlock(o.offset(x,65,z)))l.setBlock(o.offset(x,65,z),Blocks.SEA_LANTERN.defaultBlockState(),3);
 l.setBlock(o.offset(12,65,1),Blocks.EMERALD_BLOCK.defaultBlockState(),3);var chest=o.offset(3,65,2);if(l.isEmptyBlock(chest)){l.setBlock(chest,Blocks.BARREL.defaultBlockState(),3);if(l.getBlockEntity(chest) instanceof net.minecraft.world.Container c){c.setItem(0,new ItemStack(Items.IRON_HOE));c.setItem(1,new ItemStack(Items.WHEAT_SEEDS,32));c.setItem(2,new ItemStack(Items.CARROT,16));c.setItem(3,new ItemStack(Items.POTATO,16));c.setItem(4,new ItemStack(Items.BEETROOT_SEEDS,16));}}
 CasinoProps.text(l,"farm_exit_"+slot,o.getX()+12.5,67,o.getZ()+1.5,"農園 / 緑の端末を右クリックで街へ",0x88ffcc,.5f,false);
 }
}
