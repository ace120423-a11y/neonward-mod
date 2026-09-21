package jp.neonward;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
public final class SkySite {
 public static final int X=64,Z=128;
 public static boolean contains(Level l,BlockPos p){return l.dimension()==SkySpire.DIM&&p.getX()>=0&&p.getX()<117&&p.getZ()>=0&&p.getZ()<181&&p.getY()>=64&&p.getY()<784;}
}
