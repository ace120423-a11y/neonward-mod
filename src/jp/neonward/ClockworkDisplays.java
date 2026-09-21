package jp.neonward;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;
import com.mojang.math.Transformation;
import jp.neonward.mixin.LiftDisplayAccess;
/** Pending entities remain registered until visible to chunk queries; static poses aren't resent. */
final class ClockworkDisplays {
 static final Map<ServerLevel,Map<String,Display>> CACHE=new WeakHashMap<>();
 static final Set<Display> PUBLISHED=Collections.newSetFromMap(new WeakHashMap<>());
 static final Map<Display,Transformation> POSES=new WeakHashMap<>();
 static int updates;
 static void reset(){CACHE.clear();PUBLISHED.clear();POSES.clear();updates=0;}
 static boolean initialized(Display d){return PUBLISHED.contains(d);}
 /** Only generated, reconstructible tower decorations; never mobs or player displays. */
 static boolean decoration(Entity e){
  return e instanceof Display&&SpireSite.contains(e.level(),e.blockPosition())&&e.entityTags().stream().anyMatch(t->t.startsWith("nw_feedback_")||t.startsWith("nw_gear_at_")||t.startsWith("nw_handwheel_v2_"));
 }
 static boolean nearby(int floor,Set<Integer> occupied){return occupied.stream().anyMatch(f->Math.abs(f-floor)<=1);}
 static int prune(ServerLevel l,Set<Integer> occupied){
  var stale=new ArrayList<Display>();
  for(var e:l.getAllEntities())if(decoration(e)&&!nearby(DungeonLayout.floor(e.getY()),occupied)){
   stale.add((Display)e);if(stale.size()>=256)break;
  }
  for(var d:stale){PUBLISHED.remove(d);POSES.remove(d);d.discard();}
  var cache=CACHE.get(l);if(cache!=null)cache.values().removeIf(Entity::isRemoved);
  ClockworkMachinery.ANGLES.keySet().removeIf(Entity::isRemoved);
  ClockworkMachinery.WHEEL_TICKS.keySet().removeIf(Entity::isRemoved);
  return stale.size();
 }
 static <T extends Display>T find(ServerLevel l,String id,Vec3 at,Class<T> type){
  if(!l.isPositionEntityTicking(BlockPos.containing(at)))return null;
  var cache=CACHE.computeIfAbsent(l,k->new HashMap<>());var cached=cache.get(id);
  if(cached!=null&&!cached.isRemoved())return type.cast(cached);
  T found=null;
  for(var e:l.getAllEntities())if(type.isInstance(e)&&e.entityTags().contains(id)){
   if(found==null){found=type.cast(e);PUBLISHED.add(found);}else e.discard();
  }
  if(found==null)found=type.cast(type==Display.TextDisplay.class?new Display.TextDisplay(EntityTypes.TEXT_DISPLAY,l):new Display.BlockDisplay(EntityTypes.BLOCK_DISPLAY,l));
  cache.put(id,found);return found;
 }
 static void publish(ServerLevel l,Display d){if(PUBLISHED.add(d))l.addFreshEntity(d);}
 static void transform(Display d,Transformation pose){
  var old=POSES.get(d);if(old!=null&&old.getMatrix().equals(pose.getMatrix()))return;
  var a=(LiftDisplayAccess)d;a.neonLiftTransform(pose);a.neonLiftDelay(0);POSES.put(d,pose);updates++;
 }
}
