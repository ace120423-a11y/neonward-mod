package jp.neonward.mixin;
import jp.neonward.CameraScreen;
import net.minecraft.client.renderer.ItemInHandRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemInHandRenderer.class)
public class CameraHandMixin {
 @Inject(method="submitHandsWithItems",at=@At("HEAD"),cancellable=true)
 private void hidePhoneHand(CallbackInfo ci){if(CameraScreen.active())ci.cancel();}
}
