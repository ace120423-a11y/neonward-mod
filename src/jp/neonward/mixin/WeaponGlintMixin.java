package jp.neonward.mixin;
import jp.neonward.NeonArsenal;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ItemStack.class)
public class WeaponGlintMixin {
 @Inject(method="hasFoil",at=@At("HEAD"),cancellable=true)
 private void enchantedWeaponGlint(CallbackInfoReturnable<Boolean> cir){var stack=(ItemStack)(Object)this;if(NeonArsenal.DROPS.contains(stack.getItem())&&stack.isEnchanted())cir.setReturnValue(true);}
}
