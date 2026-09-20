package jp.neonward.mixin;
import jp.neonward.TailoredModels;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(HumanoidArmorLayer.class)
public class TailoredArmorMixin {
 @Inject(method="getArmorModel",at=@At("HEAD"),cancellable=true)
 private void tailored(HumanoidRenderState state,EquipmentSlot slot,CallbackInfoReturnable<HumanoidModel<?>> cir){var model=TailoredModels.get(state,slot);if(model!=null)cir.setReturnValue(model);}
}
