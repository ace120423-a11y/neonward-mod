package jp.neonward.mixin;
import jp.neonward.MeleeElements;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Mob.class)
public class MeleeStunMixin {
 @Inject(method="serverAiStep",at=@At("HEAD"),cancellable=true)
 private void neonStun(CallbackInfo ci){var mob=(Mob)(Object)this;if(MeleeElements.stunned(mob)){mob.getNavigation().stop();mob.setDeltaMovement(0,mob.getDeltaMovement().y,0);ci.cancel();}}
}
