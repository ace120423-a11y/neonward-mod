package jp.neonward;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.Commands;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import com.mojang.brigadier.arguments.*;
import com.google.gson.JsonObject;
import net.montoyo.wd.block.ScreenBlock;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.registry.BlockRegistry;
import net.montoyo.wd.utilities.BlockSide;
import net.montoyo.wd.utilities.math.Vector2i;
public final class PortableTelevision {
 static void tameVolume(ScreenBlockEntity be,BlockSide side){var data=be.getScreen(side);if(data==null)return;data.autoVolume=true;data.autoVolumeDistance=3f;data.autoVolumeMaxLevel=0.25f;be.ytVolume=0.25f;be.setChanged();}
 static void stopBrowser(ScreenBlockEntity be,BlockSide side){be.ytVolume=0f;be.removeScreen(side);be.clear();var level=be.getLevel();if(level!=null)level.sendBlockUpdated(be.getBlockPos(),be.getBlockState(),be.getBlockState(),3);}
 static boolean allowed(ServerPlayer p,BlockPos pos){return !p.isSpectator()&&p.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<49&&(HomeBuildingRules.editablePosition(p.level(),p,pos)||PrivateFarms.edit(p.level(),p,pos))&&net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(p.level().getBlockState(pos).getBlock()).getPath().startsWith("shop_tv_");}
 static void init(){CommandRegistrationCallback.EVENT.register((d,c,e)->d.register(Commands.literal("interiortv").then(Commands.argument("x",IntegerArgumentType.integer()).then(Commands.argument("y",IntegerArgumentType.integer()).then(Commands.argument("z",IntegerArgumentType.integer()).then(Commands.argument("url",StringArgumentType.greedyString()).executes(ctx->set(ctx.getSource().getPlayerOrException(),new BlockPos(IntegerArgumentType.getInteger(ctx,"x"),IntegerArgumentType.getInteger(ctx,"y"),IntegerArgumentType.getInteger(ctx,"z")),StringArgumentType.getString(ctx,"url")))))))));}
 static void open(ServerPlayer p,BlockPos pos){if(!allowed(p,pos)){p.sendOverlayMessage(Component.literal("購入したテレビを自分のマイホームに置いて使ってね"));return;}var o=new JsonObject();o.addProperty("interior_tv",true);o.addProperty("x",pos.getX());o.addProperty("y",pos.getY());o.addProperty("z",pos.getZ());ServerPlayNetworking.send(p,new StockMarket.Snapshot(o.toString()));}
 static int set(ServerPlayer p,BlockPos pos,String input){if(!allowed(p,pos))return 0;var l=p.level();var at=pos.above();if(!HomeBuildingRules.editablePosition(l,p,at)&&!PrivateFarms.edit(l,p,at))return 0;var side=BlockSide.valueOf(l.getBlockState(pos).getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING).name());if(input.startsWith("volume ")&&l.getBlockEntity(at) instanceof ScreenBlockEntity be){var data=be.getScreen(side);if(data==null)return 0;String value=input.substring(7).trim();if(value.equals("mute")){be.ytVolume=0f;data.autoVolume=false;data.autoVolumeMaxLevel=0f;be.setChanged();return 1;}try{float v=value.equals("up")?Math.min(.25f,be.ytVolume+.05f):value.equals("down")?Math.max(0f,be.ytVolume-.05f):Math.max(0f,Math.min(.25f,Float.parseFloat(value)));data.autoVolume=false;data.autoVolumeMaxLevel=v;be.ytVolume=v;be.setChanged();return 1;}catch(NumberFormatException ex){return 0;}}String url;try{url=TelevisionUrl.normalize(input);}catch(Exception e){p.sendOverlayMessage(Component.literal("URLを確認してください"));return 0;}
 if(l.isEmptyBlock(at)){l.setBlock(at,BlockRegistry.SCREEN.defaultBlockState().setValue(ScreenBlock.HAS_TE,true),3);if(l.getBlockEntity(at) instanceof ScreenBlockEntity be){be.addScreen(side,new Vector2i(1,1),url,new Vector2i(854,480),p,true);tameVolume(be,side);return 1;}}
 else if(l.getBlockEntity(at) instanceof ScreenBlockEntity be){if(input.equals("off")){stopBrowser(be,side);}else{if(be.hasScreen(side))be.setURL(side,url);else be.addScreen(side,new Vector2i(1,1),url,new Vector2i(854,480),p,true);tameVolume(be,side);be.setChanged();}return 1;}p.sendOverlayMessage(Component.literal("テレビの上1ブロックを空けてください"));return 0;
 }
}
