package jp.neonward.mixin;
import jp.neonward.DualGuard;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LivingEntity.class)
public class DualGuardMixin {
 @Inject(method="hurtServer",at=@At("HEAD"),cancellable=true)
 private void deflect(ServerLevel level,DamageSource source,float amount,CallbackInfoReturnable<Boolean> ci){if((Object)this instanceof ServerPlayer p&&DualGuard.projectile(p,source))ci.setReturnValue(false);}
}
