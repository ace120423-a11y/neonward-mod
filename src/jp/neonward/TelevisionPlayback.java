package jp.neonward;
import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerLevel;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.utilities.BlockSide;
/** The authoritative TV power/volume state is also sent to clients without a loaded chunk. */
public final class TelevisionPlayback {
 public static void sync(ScreenBlockEntity be,boolean on){be.setChanged();var level=be.getLevel();if(!(level instanceof ServerLevel l))return;l.sendBlockUpdated(be.getBlockPos(),be.getBlockState(),be.getBlockState(),3);var o=new JsonObject();o.addProperty("tv_state",true);o.addProperty("dimension",l.dimension().identifier().toString());o.addProperty("x",be.getBlockPos().getX());o.addProperty("y",be.getBlockPos().getY());o.addProperty("z",be.getBlockPos().getZ());o.addProperty("on",on);o.addProperty("volume",Float.isFinite(be.ytVolume)?Math.clamp(be.ytVolume,0f,.25f):0f);for(var p:l.getServer().getPlayerList().getPlayers())if(p.level()==l)ServerPlayNetworking.send(p,new StockMarket.Snapshot(o.toString()));}
 public static void stop(ScreenBlockEntity be){be.ytVolume=0f;for(int i=0;i<be.getScreenCount();i++){var d=be.getScreen(i);d.url="about:blank";d.rememberedVideoTime=0;d.autoVolume=false;d.autoVolumeMaxLevel=0;d.enableAudio=false;}for(var side:BlockSide.values())if(be.hasScreen(side))be.removeScreen(side);be.clear();sync(be,false);}
}
