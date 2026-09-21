package jp.neonward.mixin;
import jp.neonward.MeleeElements;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
/** Only the player's primary melee hit; gun commands and secondary damage never recurse. */
@Mixin(Player.class)
public class MeleeAttackMixin {
 @Redirect(method="attack",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;hurtOrSimulate(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
 private boolean neonMeleeHit(Entity target,DamageSource source,float amount){var p=(Player)(Object)this;var weapon=p.getWeaponItem();boolean expanded=jp.neonward.ArsenalExpansion.melee(weapon),melee=MeleeElements.kind(weapon)!=null||expanded;float damage=melee?amount*2f:amount;boolean hit=target.hurtOrSimulate(source,damage);if(hit&&melee&&p instanceof ServerPlayer sp&&target instanceof LivingEntity living){if(expanded)jp.neonward.ArsenalExpansion.impact(sp,living,weapon,damage);else MeleeElements.impact(sp,living,weapon,damage);}return hit;}
}
