package jp.neonward.mixin;
import jp.neonward.ShrineRitualClient;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=ItemInHandRenderer.class,priority=1200)
public class ShrineRitualHandsMixin {
 @Inject(method="submitHandsWithItems",at=@At("HEAD"),cancellable=true)
 private void ritual(float delta,PoseStack pose,SubmitNodeCollector nodes,LocalPlayer player,int light,CallbackInfo ci){if(ShrineRitualClient.firstPerson(delta,pose,nodes,player,light))ci.cancel();}
}
