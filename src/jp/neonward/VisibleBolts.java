package jp.neonward;
import java.util.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.arrow.*;
import net.minecraft.world.phys.Vec3;
/** Vanilla arrow model with the existing authoritative flight/collision, never a second damage source. */
public final class VisibleBolts {
 static final Map<ArsenalExpansion.Shot,Bolt> FLYING=new IdentityHashMap<>();
 static final List<Bolt> ALL=new ArrayList<>();
 static final class Bolt extends Arrow {
  final ServerPlayer shooter;int remaining=80;LivingEntity stuck;Vec3 offset;
  Bolt(ServerPlayer p){super(EntityTypes.ARROW,p.level());shooter=p;setNoPhysics(true);setNoGravity(true);setSilent(true);setInvulnerable(true);pickup=AbstractArrow.Pickup.DISALLOWED;setBaseDamage(0);}
  @Override public boolean shouldBeSaved(){return false;}
  @Override public void playerTouch(net.minecraft.world.entity.player.Player p){}
  @Override public void tick(){if(--remaining<=0||shooter.hasDisconnected()||!shooter.isAlive()||shooter.level()!=level()){discard();return;}if(stuck!=null){if(!stuck.isAlive()||stuck.level()!=level()){discard();return;}setPos(stuck.position().add(offset));needsSync=true;}}
 }
 static void create(ArsenalExpansion.Shot s){if(ALL.size()>=96)return;var b=new Bolt(s.owner());place(b,s.position(),s.velocity());if(s.level().addFreshEntity(b)){ALL.add(b);FLYING.put(s,b);}}
 static void place(Bolt b,Vec3 at,Vec3 direction){b.setPos(at);b.setYRot((float)Math.toDegrees(Math.atan2(direction.x,direction.z)));b.setXRot((float)Math.toDegrees(Math.atan2(direction.y,direction.horizontalDistance())));b.setDeltaMovement(Vec3.ZERO);b.needsSync=true;}
 static void advance(ArsenalExpansion.Shot old,ArsenalExpansion.Shot next){var b=FLYING.remove(old);if(b!=null&&!b.isRemoved()){place(b,next.position(),next.velocity());FLYING.put(next,b);}}
 static void hit(ArsenalExpansion.Shot s,Vec3 at,LivingEntity target){var b=FLYING.remove(s);if(b!=null){place(b,at.subtract(s.velocity().normalize().scale(.18)),s.velocity());b.remaining=30;b.stuck=target;b.offset=target==null?null:b.position().subtract(target.position());}}
 static void remove(ArsenalExpansion.Shot s){var b=FLYING.remove(s);if(b!=null)b.discard();}
 static void clean(){ALL.removeIf(Entity::isRemoved);FLYING.values().removeIf(Entity::isRemoved);}
 static void clear(){ALL.forEach(Entity::discard);ALL.clear();FLYING.clear();}
}
