package jp.neonward.mixin;
import jp.neonward.*;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.model.*;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemInHandLayer.class)
public abstract class PairedGloveLayerMixin {
 @Inject(method="submitArmWithItem",at=@At("HEAD"),cancellable=true)
 private void glove(ArmedEntityRenderState state,ItemStackRenderState itemState,ItemStack item,HumanoidArm arm,PoseStack pose,SubmitNodeCollector nodes,int light,CallbackInfo ci){
  if(!(state instanceof AvatarRenderState)||!PairedHands.gauntlet(state.getMainHandItemStack()))return;
  ci.cancel();pose.pushPose();((ArmedModel)((ItemInHandLayer)(Object)this).getParentModel()).translateToHand(state,arm,pose);PairedGloves.draw(pose,nodes,light,arm==HumanoidArm.LEFT);pose.popPose();
 }
}
