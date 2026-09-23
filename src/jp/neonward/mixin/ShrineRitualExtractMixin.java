package jp.neonward.mixin;
import jp.neonward.*;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=AvatarRenderer.class,priority=800)
public class ShrineRitualExtractMixin {
 @Inject(method="extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",at=@At("TAIL"))
 private void ritual(Avatar player,AvatarRenderState state,float delta,CallbackInfo ci){ShrineRitualClient.extract(player,state,delta);}
}
