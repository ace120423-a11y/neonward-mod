package jp.neonward;
import java.net.URI;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.montoyo.wd.block.ScreenBlock;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.registry.BlockRegistry;
import net.montoyo.wd.utilities.BlockSide;
import net.montoyo.wd.utilities.math.Vector2i;

public final class NeonTelevision {
 public static final BlockPos ORIGIN=new BlockPos(72,219,218),REMOTE=new BlockPos(79,218,218);
 static void mute(ScreenBlockEntity be,BlockSide side){var data=be.getScreen(side);if(data!=null){data.autoVolume=false;data.autoVolumeMaxLevel=0f;}be.ytVolume=0f;be.setChanged();}
 static void tameVolume(ScreenBlockEntity be,BlockSide side){var data=be.getScreen(side);if(data==null)return;data.autoVolume=true;data.autoVolumeDistance=3f;data.autoVolumeMaxLevel=0.25f;be.ytVolume=0.25f;be.setChanged();}
 static void stopBrowser(ScreenBlockEntity be,BlockSide side){
  mute(be,side);be.removeScreen(side);be.clear();
  var level=be.getLevel();if(level!=null)level.sendBlockUpdated(be.getBlockPos(),be.getBlockState(),be.getBlockState(),3);
 }
 public static void init(){
  ServerTickEvents.END_SERVER_TICK.register(server->{if(server.getTickCount()%40!=0)return;var l=server.overworld();
   if(server.getTickCount()==40)for(int f=2;f<=20;f++)if(l.getBlockEntity(origin(f)) instanceof ScreenBlockEntity be){be.setURL(BlockSide.SOUTH,"about:blank");mute(be,BlockSide.SOUTH);}
   for(var owner:server.getPlayerList().getPlayers()){int f=CityApartments.floor(owner.getY());var origin=origin(f);if(f<2||owner.level()!=l||!CityApartments.owns(owner,f)||owner.distanceToSqr(origin.getX(),origin.getY(),origin.getZ())>1024||!l.isLoaded(origin)||!l.getBlockState(origin.offset(2,0,0)).is(NeonFurniture.BLOCKS.get("pulse_tv")))continue;
    boolean clear=true;for(int x=0;x<6;x++)for(int y=0;y<3;y++){var state=l.getBlockState(origin.offset(x,y,0));if(!state.isAir()&&!state.is(NeonFurniture.BLOCKS.get("pulse_tv")))clear=false;}if(!clear)continue;
    for(int x=0;x<6;x++)for(int y=0;y<3;y++)l.setBlock(origin.offset(x,y,0),BlockRegistry.SCREEN.defaultBlockState().setValue(ScreenBlock.HAS_TE,true),3);
    if(l.getBlockEntity(origin) instanceof ScreenBlockEntity be){be.addScreen(BlockSide.SOUTH,new Vector2i(6,3),"about:blank",new Vector2i(1280,720),owner,true);mute(be,BlockSide.SOUTH);}
    var remote=remote(f);if(l.getBlockState(remote).isAir())l.setBlock(remote,NeonFurniture.BLOCKS.get("tv_remote").defaultBlockState(),3);
   }
  });
  CommandRegistrationCallback.EVENT.register((d,c,e)->d.register(Commands.literal("neontv").then(Commands.argument("url",StringArgumentType.greedyString()).executes(ctx->set(ctx.getSource().getPlayerOrException(),StringArgumentType.getString(ctx,"url"))))));
 }
 static BlockPos origin(int f){return new BlockPos(72,CityApartments.base(f)+2,218);}
 static BlockPos remote(int f){return new BlockPos(79,CityApartments.base(f)+1,218);}
 static int set(ServerPlayer p,String input){
  int floor=CityApartments.floor(p.getY());var remote=remote(floor);
  if(p.isSpectator()||p.level().dimension()!=Level.OVERWORLD||!CityApartments.owns(p,floor)||p.distanceToSqr(remote.getX(),remote.getY(),remote.getZ())>144)return 0;
  if(input.startsWith("volume ")){if(!(p.level().getBlockEntity(origin(floor)) instanceof ScreenBlockEntity be))return 0;var data=be.getScreen(BlockSide.SOUTH);if(data==null)return 0;String value=input.substring(7).trim();if(value.equals("mute")){mute(be,BlockSide.SOUTH);return 1;}try{float v=value.equals("up")?Math.min(.25f,be.ytVolume+.05f):value.equals("down")?Math.max(0f,be.ytVolume-.05f):Math.max(0f,Math.min(.25f,Float.parseFloat(value)));data.autoVolume=false;data.autoVolumeMaxLevel=v;be.ytVolume=v;be.setChanged();return 1;}catch(NumberFormatException ex){return 0;}}
  String url;
  try{url=TelevisionUrl.normalize(input);}catch(IllegalArgumentException ex){p.sendOverlayMessage(Component.literal(ex.getMessage()));return 0;}
  if(p.level().getBlockEntity(origin(floor)) instanceof ScreenBlockEntity be){
   if(input.equals("off")){stopBrowser(be,BlockSide.SOUTH);}
   else {if(be.hasScreen(BlockSide.SOUTH))be.setURL(BlockSide.SOUTH,url);else be.addScreen(BlockSide.SOUTH,new Vector2i(6,3),url,new Vector2i(1280,720),p,true);tameVolume(be,BlockSide.SOUTH);be.setChanged();}
   p.sendOverlayMessage(Component.literal(input.equals("off")?"テレビを停止しました":"テレビのページを切り替えました"));return 1;
  }return 0;
 }
}
