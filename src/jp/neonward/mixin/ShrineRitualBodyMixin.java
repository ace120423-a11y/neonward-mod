package jp.neonward.mixin;
import jp.neonward.ShrineRitualClient;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=HumanoidModel.class,priority=800)
public class ShrineRitualBodyMixin {
 @Inject(method="setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V",at=@At("TAIL"))
 private void ritual(HumanoidRenderState state,CallbackInfo ci){ShrineRitualClient.pose((HumanoidModel<?>)(Object)this,state);}
}
