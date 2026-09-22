package jp.neonward.mixin;
import jp.neonward.WeaponMotion;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(HumanoidModel.class)
public class WeaponBodyPoseMixin {
 @Inject(method="setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V",at=@At("TAIL"))
 private void motion(HumanoidRenderState s,CallbackInfo ci){WeaponMotion.third((HumanoidModel<?>)(Object)this,s);}
}
