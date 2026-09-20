package jp.neonward.mixin;
import jp.neonward.GunControls;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Camera.class)
public class GunFovMixin {
 @Inject(method="calculateFov",at=@At("RETURN"),cancellable=true)
 private void scope(float delta,CallbackInfoReturnable<Float> result){if(net.minecraft.client.Minecraft.getInstance().options.getCameraType().isFirstPerson()&&GunControls.aiming())result.setReturnValue(result.getReturnValue()*GunControls.zoom());}
}
