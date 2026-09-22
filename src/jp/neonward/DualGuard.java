package jp.neonward;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.*;

/** Frontal bullet deflection only; no melee/explosion blocking or reflected friendly fire. */
public final class DualGuard {
 private static final java.util.Map<ServerPlayer,Integer> LAST_EFFECT=new java.util.WeakHashMap<>();
 public static boolean guarding(Player p){return p.isAlive()&&!p.isSpectator()&&p.isUsingItem()&&p.getUsedItemHand()==InteractionHand.MAIN_HAND&&p.getUseItem()==p.getMainHandItem()&&ArsenalExpansion.type(p.getMainHandItem())==2;}
 static boolean front(Player p,Vec3 towardSource){return guarding(p)&&towardSource.lengthSqr()>.0001&&p.getLookAngle().dot(towardSource.normalize())>=.5;}
 public static boolean hitscan(LivingEntity target,Vec3 origin){if(!(target instanceof ServerPlayer p)||!front(p,origin.subtract(p.getEyePosition())))return false;effect(p);return true;}
 public static boolean projectile(ServerPlayer p,DamageSource source){
  if(!source.is(DamageTypeTags.IS_PROJECTILE)||source.is(DamageTypeTags.IS_EXPLOSION)||source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)||source.getEntity()==p)return false;
  Vec3 direction=null;
  if(source.getDirectEntity() instanceof Projectile shot&&shot.getDeltaMovement().lengthSqr()>.0001)direction=shot.getDeltaMovement().scale(-1);
  else if(source.getSourcePosition()!=null)direction=source.getSourcePosition().subtract(p.getEyePosition());
  if(direction==null||!front(p,direction))return false;
  if(source.getDirectEntity() instanceof Projectile shot)shot.discard();
  effect(p);return true;
 }
 static void effect(ServerPlayer p){
  // Limit audiovisual spam without limiting how many incoming bullets are blocked.
  if(p.tickCount-LAST_EFFECT.getOrDefault(p,-100)<3)return;
  LAST_EFFECT.put(p,p.tickCount);
  var at=p.getEyePosition().add(p.getLookAngle().scale(.8));
  p.level().sendParticles(ParticleTypes.ELECTRIC_SPARK,at.x,at.y,at.z,8,.15,.15,.15,.15);
  p.level().playSound(null,p.blockPosition(),SoundEvents.SHIELD_BLOCK.value(),SoundSource.PLAYERS,.55f,1.5f);
 }
 private DualGuard(){}
}
