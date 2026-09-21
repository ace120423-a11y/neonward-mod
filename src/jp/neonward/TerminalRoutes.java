package jp.neonward;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
/** Shop identity includes its dimension and room, never just coordinates. */
public final class TerminalRoutes {
 public static boolean roomAllowed(Level level,BlockPos pos,int expected){return level.dimension()!=CompactShops.DIM||CompactShops.room(level,pos)==expected;}
}
