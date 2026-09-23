package jp.neonward.mixin;

import jp.neonward.ShrineBlessings;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Includes melee, hitscan weapons and projectiles whose damage source retains its owner. */
@Mixin(LivingEntity.class)
public class ShrineAttackMixin {
 @ModifyVariable(method="hurtServer",at=@At("HEAD"),argsOnly=true,ordinal=0)
 private float shrineAttack(float amount,ServerLevel level,DamageSource source,float original){
  return source.getEntity() instanceof ServerPlayer p?amount*ShrineBlessings.attackMultiplier(p):amount;
 }
}
