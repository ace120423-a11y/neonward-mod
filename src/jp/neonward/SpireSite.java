package jp.neonward;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
/** Exterior landmark and expanded interior deliberately occupy different spaces. */
public final class SpireSite {
 public static final int X=64,Z=128,OUTER_X=736,OUTER_Z=224;
 public static BlockPos pos(int x,int y,int z){return new BlockPos(X+x,y,Z+z);}
 public static boolean contains(Level l,BlockPos p){return l.dimension()==NeonZones.TOWER&&p.getX()>=0&&p.getX()<DungeonLayout.SIZE&&p.getZ()>=0&&p.getZ()<DungeonLayout.DEPTH&&p.getY()>=64&&p.getY()<784;}
 public static boolean exterior(Level l,BlockPos p){return l.dimension()==Level.OVERWORLD&&p.getX()>=OUTER_X-18&&p.getX()<=OUTER_X+56&&p.getZ()>=OUTER_Z-30&&p.getZ()<=OUTER_Z+56&&p.getY()>=48;}
 public static boolean structure(Level l,BlockPos p){return l.dimension()==NeonZones.TOWER||exterior(l,p);}
}
