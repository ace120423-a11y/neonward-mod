package jp.neonward.mixin;
import jp.neonward.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.HumanoidModel;
import org.joml.Quaternionf;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=ItemInHandLayer.class,priority=1200)
public class ShrineRitualHeldMixin {
 @SuppressWarnings({"rawtypes","unchecked"})
 @Inject(method="submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/ArmedEntityRenderState;FF)V",at=@At("HEAD"),cancellable=true)
 private void ritual(PoseStack pose,SubmitNodeCollector nodes,int light,ArmedEntityRenderState state,float yaw,float pitch,CallbackInfo ci){
  var f=ShrineRitualClient.state(state);if(f==null)return;ci.cancel();if(f.ladle()==0)return;
  var arm=f.ladle()>0?HumanoidArm.RIGHT:HumanoidArm.LEFT;pose.pushPose();
  ((ArmedModel)((ItemInHandLayer)(Object)this).getParentModel()).translateToHand(state,arm,pose);
  pose.translate(0,.68,0);
  // Keep the wrist position, but remove arm pitch/yaw/roll before posing the cup.
  // Model space has -Y up and the player's right at -X; +side roll tips inward.
  var part=((HumanoidModel<?>)((ItemInHandLayer)(Object)this).getParentModel()).getArm(arm);
  pose.mulPose(new Quaternionf().rotationZYX(part.zRot,part.yRot,part.xRot).conjugate());
  pose.mulPose(Axis.ZP.rotation(f.ladle()*f.pour()*.85f));pose.scale(.65f,.65f,.65f);
  ShrineRitualMeshes.drawLadle(pose,nodes,light);pose.popPose();
 }
}
