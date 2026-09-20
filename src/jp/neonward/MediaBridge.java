package jp.neonward;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
public final class MediaBridge {
 public static boolean available(){return FabricLoader.getInstance().isModLoaded("webdisplays")&&FabricLoader.getInstance().isModLoaded("mcef");}
 public static boolean isScreen(BlockState s){return available()&&BuiltInRegistries.BLOCK.getKey(s.getBlock()).getNamespace().equals("webdisplays");}
}
