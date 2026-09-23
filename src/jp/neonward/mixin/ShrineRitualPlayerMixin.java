package jp.neonward.mixin;
import jp.neonward.ShrineRitualClient;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=PlayerModel.class,priority=800)
public class ShrineRitualPlayerMixin {
 @Inject(method="setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V",at=@At("TAIL"))
 private void ritual(AvatarRenderState state,CallbackInfo ci){ShrineRitualClient.pose((PlayerModel)(Object)this,state);}
}
