package jp.neonward.mixin;
import jp.neonward.*;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import com.mojang.math.Axis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemInHandRenderer.class)
public class GunAimMixin {
 @Inject(method="submitArmWithItem",at=@At("HEAD"),cancellable=true)
 private void aim(AbstractClientPlayer player,float delta,float pitch,InteractionHand hand,float swing,ItemStack stack,float equip,PoseStack pose,SubmitNodeCollector nodes,int light,CallbackInfo ci){if(!GunControls.aiming())return;if(hand!=player.getUsedItemHand()){ci.cancel();return;}if(!NeonArsenal.isGun(stack))return;pose.pushPose();boolean longGun=stack.is(NeonArsenal.RIFLE)||NeonArsenal.sniper(stack)||NeonArsenal.automatic(stack);pose.translate(0,longGun?-.34:-.225,-.85);pose.mulPose(Axis.YP.rotationDegrees(180));pose.scale(.9f,.9f,.9f);((ItemInHandRenderer)(Object)this).renderItem(player,stack,ItemDisplayContext.NONE,pose,nodes,light);pose.popPose();ci.cancel();}
}
