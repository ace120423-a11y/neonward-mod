package jp.neonward.mixin;
import jp.neonward.PhoneEquipment;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LocalPlayer.class)
public class PhoneDropMixin {
 @Inject(method="drop",at=@At("HEAD"),cancellable=true)
 private void keepPhone(boolean all,CallbackInfoReturnable<Boolean> cir){if(PhoneEquipment.isHeld((LocalPlayer)(Object)this))cir.setReturnValue(false);}
}
