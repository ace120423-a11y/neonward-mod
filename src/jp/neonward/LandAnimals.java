package jp.neonward;
import net.minecraft.world.entity.Entity;
/** Purchased animals and their offspring stay inside their original parcel. */
public final class LandAnimals {
 public static int plot(Entity e){for(int id=0;id<8;id++)if(e.entityTags().contains("nw_land_plot_"+id))return id;return -1;}
 public static void mark(Entity e,int id){e.addTag("nw_land_animal");e.addTag("nw_land_plot_"+id);}
 public static void contain(Entity e){int id=plot(e);if(id<0||e.level().dimension()!=net.minecraft.world.level.Level.OVERWORLD)return;double x=Math.clamp(e.getX(),LandLayout.x(id)+1.2,LandLayout.x(id)+30.8),z=Math.clamp(e.getZ(),LandLayout.z(id)+1.2,LandLayout.z(id)+30.8);if(x!=e.getX()||z!=e.getZ()){e.setPos(x,e.getY(),z);e.setDeltaMovement(0,e.getDeltaMovement().y,0);if(e instanceof net.minecraft.world.entity.Mob m)m.getNavigation().stop();}}
}
