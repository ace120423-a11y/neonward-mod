package jp.neonward;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerPlayer;

public final class MeleeReach {
 public static MeleeShape.Profile profile(ItemStack stack){var id=BuiltInRegistries.ITEM.getKey(stack.getItem());return id.getNamespace().equals("neonward")?MeleeShape.profile(id.getPath()):null;}
 static void modifier(Player p,net.minecraft.core.Holder<Attribute> type,String name,double value){
  var a=p.getAttribute(type);if(a==null)return;var id=NeonWard.id(name);var old=a.getModifier(id);
  if(old!=null&&old.amount()==value)return;if(old!=null)a.removeModifier(id);
  if(value!=0)a.addTransientModifier(new AttributeModifier(id,value,AttributeModifier.Operation.ADD_VALUE));
 }
 public static void update(Player p){
  var shape=p.isAlive()&&!p.isSpectator()?profile(p.getMainHandItem()):null;
  modifier(p,Attributes.ENTITY_INTERACTION_RANGE,"melee_reach",shape==null?0:shape.reach()-3);
  modifier(p,Attributes.ATTACK_SPEED,"wakizashi_speed",shape!=null&&shape.shape()==MeleeShape.Shape.WAKIZASHI?.8:0);
 }
 static Vec3 facing(Player p){var look=p.getLookAngle().multiply(1,0,1);return look.lengthSqr()<1e-6?Vec3.directionFromRotation(0,p.getYRot()):look.normalize();}
 public static boolean inShape(ServerPlayer p,LivingEntity target,LivingEntity impact,MeleeShape.Profile shape){
  var forward=facing(p);var offset=target.position().subtract(p.position());
  return MeleeShape.contains(shape,offset.dot(forward),offset.x*forward.z-offset.z*forward.x,
      target.position().subtract(impact.position()).length(),target.getY()-impact.getY())&&p.hasLineOfSight(target)&&impact.hasLineOfSight(target);
 }
}
