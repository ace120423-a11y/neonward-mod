package jp.neonward.mixin;
import jp.neonward.CameraScreen;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Hud.class)
public class CameraHudMixin {
 @Inject(method="extractRenderState",at=@At("HEAD"),cancellable=true)
 private void hidePhoneHud(CallbackInfo ci){if(CameraScreen.active())ci.cancel();}
}
