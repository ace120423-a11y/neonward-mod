package jp.neonward;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.Commands;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import com.mojang.brigadier.arguments.IntegerArgumentType;
public final class InteriorShop {
 static final BlockPos POS=new BlockPos(327,65,605);
 static void init(){
 CommandRegistrationCallback.EVENT.register((d,c,e)->d.register(Commands.literal("interiors").then(Commands.argument("product",IntegerArgumentType.integer(-1,InteriorCatalog.PRODUCTS.length-1)).executes(ctx->request(ctx.getSource().getPlayerOrException(),IntegerArgumentType.getInteger(ctx,"product"))))));
 UseBlockCallback.EVENT.register((p,l,h,hit)->{if(h==InteractionHand.MAIN_HAND&&hit.getBlockPos().equals(POS)&&l.getBlockState(POS).is(NeonFurniture.BLOCKS.get("interior_counter"))){if(p instanceof ServerPlayer sp)request(sp,-1);return InteractionResult.SUCCESS;}return InteractionResult.PASS;});
 ServerTickEvents.END_SERVER_TICK.register(server->{if(server.getTickCount()%100!=0)return;var l=server.overworld();if(l.isPositionEntityTicking(POS)&&l.getBlockState(POS).is(NeonFurniture.BLOCKS.get("interior_counter")))JapaneseParlor.npc(l,"interior_sales",327.5,65,604.5,"家具屋・ミオ",true,2);});
 }
 static int request(ServerPlayer p,int product){if(p.isSpectator()||p.level().dimension()!=net.minecraft.world.level.Level.OVERWORLD||p.distanceToSqr(327.5,65,605.5)>64||!p.level().getBlockState(POS).is(NeonFurniture.BLOCKS.get("interior_counter"))||StockMarket.ledger==null)return 0;
 var a=StockMarket.ledger.account(p.getStringUUID());String msg="展示品を見て選べます。購入後は自宅へ設置。";
 if(product>=0&&product<InteriorCatalog.PRODUCTS.length){var item=InteriorCatalog.PRODUCTS[product];int free=-1;for(int i=0;i<36;i++)if(p.getInventory().getItem(i).isEmpty()){free=i;break;}
 if(free<0)msg="持ち物に空きを作ってね";else if(a.cash<item.price())msg="残高が足りません";else {a.cash-=item.price();try{StockMarket.save();p.getInventory().setItem(free,new ItemStack(NeonFurniture.BLOCKS.get(item.id())));p.getInventory().setChanged();msg=item.name()+"を購入しました";}catch(Exception ex){a.cash+=item.price();msg="保存できないため購入を取り消しました";}}}
 var o=new JsonObject();o.addProperty("interiors",true);o.addProperty("cash",a.cash);o.addProperty("message",msg);ServerPlayNetworking.send(p,new StockMarket.Snapshot(o.toString()));return 1;}
}
