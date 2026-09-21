package jp.neonward;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.piston.*;
/** Intentionally no lava, fire, explosives or moving/hopper automation in residential plots. */
public final class LandSafety {
 public static boolean forbidden(Block b){return b==Blocks.LAVA||b instanceof BaseFireBlock||b instanceof TntBlock||b instanceof PistonBaseBlock||b instanceof MovingPistonBlock||b instanceof HopperBlock||b instanceof DispenserBlock||b instanceof NetherPortalBlock||b==Blocks.RESPAWN_ANCHOR;}
 public static boolean sensitive(Level l,BlockPos p){if(WestLand.area(l,p))return true;for(var d:net.minecraft.core.Direction.values())if(WestLand.area(l,p.relative(d)))return true;return false;}
}
