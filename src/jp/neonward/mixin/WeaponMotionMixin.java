package jp.neonward.mixin;
import jp.neonward.WeaponMotion;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemInHandRenderer.class)
public class WeaponMotionMixin {
 @Shadow private void renderPlayerArm(PoseStack pose,SubmitNodeCollector nodes,int light,float equip,float swing,HumanoidArm arm){}
 @Inject(method="submitArmWithItem",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",ordinal=0,shift=At.Shift.AFTER))
 private void motion(AbstractClientPlayer p,float delta,float pitch,InteractionHand hand,float swing,ItemStack item,float equip,PoseStack pose,SubmitNodeCollector nodes,int light,CallbackInfo ci){
  var f=WeaponMotion.profile(item);
  if(f!=null&&f.gun()&&!p.isInvisible()&&!p.isFallFlying()&&!p.isVisuallySwimming()){
   var arm=hand==InteractionHand.MAIN_HAND?p.getMainArm():p.getMainArm().getOpposite();int side=arm==HumanoidArm.RIGHT?1:-1;
   pose.pushPose();pose.translate(side*.08,-.16,.02);renderPlayerArm(pose,nodes,light,equip,0,arm);pose.popPose();
   if(f.support()&&p.getItemInHand(hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND).isEmpty()){
    pose.pushPose();pose.translate(side*.90,-.18,-.30);renderPlayerArm(pose,nodes,light,equip,0,arm.getOpposite());pose.popPose();
   }
  }
  WeaponMotion.first(p,hand,item,swing,delta,pose);
 }
}
