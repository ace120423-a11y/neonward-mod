package jp.neonward.mixin;
import jp.neonward.CameraScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GameRenderer.class)
public class CameraFrameMixin {
 @Inject(method="render",at=@At("RETURN"))
 private void capturePhoneFrame(CallbackInfo ci){if(Minecraft.getInstance().gui.screen() instanceof CameraScreen screen)screen.afterFrame();}
}
