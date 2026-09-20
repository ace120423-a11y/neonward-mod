package jp.neonward.mixin;
import jp.neonward.GunControls;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(Minecraft.class)
public class GunInputMixin {
 @Inject(method="startAttack",at=@At("HEAD"),cancellable=true)
 private void noMelee(CallbackInfoReturnable<Boolean> out){if(GunControls.holding())out.setReturnValue(false);}
 @Inject(method="continueAttack",at=@At("HEAD"),cancellable=true)
 private void noMining(boolean held,CallbackInfo out){if(GunControls.holding())out.cancel();}
}
