package jp.neonward.mixin;
import jp.neonward.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Hud.class)
public class ScopeCrosshairMixin {
 @Inject(method="extractCrosshair",at=@At("HEAD"),cancellable=true)
 private void optic(GuiGraphicsExtractor g,DeltaTracker delta,CallbackInfo ci){
  var mc=Minecraft.getInstance();if(mc.options.getCameraType().isFirstPerson()&&GunControls.aiming()&&GunAttachments.installed(mc.player.getUseItem(),0)>=0)ci.cancel();
 }
}
