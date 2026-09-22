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
import org.spongepowered.asm.mixin.Shadow;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemInHandRenderer.class)
public class GunAimMixin {
 @Shadow private void renderPlayerArm(PoseStack pose,SubmitNodeCollector nodes,int light,float equip,float swing,HumanoidArm arm){}
 @Inject(method="submitArmWithItem",at=@At("HEAD"),cancellable=true)
 private void aim(AbstractClientPlayer player,float delta,float pitch,InteractionHand hand,float swing,ItemStack stack,float equip,PoseStack pose,SubmitNodeCollector nodes,int light,CallbackInfo ci){
  if(stack.isEmpty()&&GunReload.reloading(player.getItemInHand(hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND))){ci.cancel();return;}
  // Render guns through one transform throughout idle, loading and completion.
  // Use the live held stack: vanilla's equipped snapshot can lag component-only ammo updates.
  if(NeonArsenal.isGun(stack)){
   var live=player.getItemInHand(hand);if(live.is(stack.getItem()))stack=live;
   int optic=GunAttachments.installed(stack,0);if(GunControls.aiming()&&optic>=0&&!GunReload.reloading(stack)){ci.cancel();return;}
   boolean loading=GunReload.reloading(stack);float t=loading?GunReload.progress(stack):0;
   var arm=hand==InteractionHand.MAIN_HAND?player.getMainArm():player.getMainArm().getOpposite();int side=arm==HumanoidArm.RIGHT?1:-1;
   if(!player.isInvisible()){
    pose.pushPose();pose.translate(-side*.4,.18,-.12);pose.mulPose(Axis.ZP.rotationDegrees(-side*12*GunReloadMotion.poseAmount(t)));renderPlayerArm(pose,nodes,light,0,0,arm);pose.popPose();
    if(player.getItemInHand(hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND).isEmpty()){pose.pushPose();GunReloadMotion.support(pose,stack,side,t);renderPlayerArm(pose,nodes,light,0,0,arm.getOpposite());pose.popPose();}
   }
   var renderer=(ItemInHandRenderer)(Object)this;
   pose.pushPose();GunReloadMotion.body(pose,stack,side,t);
   if(!loading){float kick=WeaponMotion.recoil(player,stack,delta);var f=WeaponMotion.profile(stack);pose.translate(0,kick*.025,kick*.08);if(f!=null)pose.mulPose(Axis.XP.rotationDegrees(-kick*f.kick()));}
   if(!loading){renderer.renderItem(player,GunAttachmentVisual.base(stack,false),ItemDisplayContext.NONE,pose,nodes,light);GunAttachmentVisual.draw(renderer,player,stack,pose,nodes,light,side,0,false);pose.popPose();ci.cancel();return;}
   renderer.renderItem(player,GunAttachmentVisual.base(stack,true),ItemDisplayContext.NONE,pose,nodes,light);
   GunAttachmentVisual.draw(renderer,player,stack,pose,nodes,light,side,t,true);
   pose.pushPose();GunReloadMotion.part(pose,stack,side,t);renderer.renderItem(player,GunReloadMotion.mesh(stack,"part"),ItemDisplayContext.NONE,pose,nodes,light);pose.popPose();
   int k=GunVfx.profile(stack);if(k>=1&&k<=3||k==10&&t>=.45f){pose.pushPose();GunReloadMotion.action(pose,stack,side,t);renderer.renderItem(player,GunReloadMotion.mesh(stack,"aux"),ItemDisplayContext.NONE,pose,nodes,light);pose.popPose();}
   if(k==0||k>=4&&k<=9){pose.pushPose();GunReloadMotion.mechanism(pose,stack,side,t);renderer.renderItem(player,GunReloadMotion.mesh(stack,"bolt"),ItemDisplayContext.NONE,pose,nodes,light);pose.popPose();}
   if(k==5){pose.pushPose();GunReloadMotion.cover(pose,t);renderer.renderItem(player,GunReloadMotion.mesh(stack,"cover"),ItemDisplayContext.NONE,pose,nodes,light);pose.popPose();}
   pose.popPose();ci.cancel();return;
  }
  if(!GunControls.aiming())return;
  if(hand!=player.getUsedItemHand()){if(player.getItemInHand(hand).isEmpty())ci.cancel();return;}
  if(!NeonArsenal.isGun(stack))return;
  var f=WeaponMotion.profile(stack);boolean longGun=f!=null&&f.support();
  var arm=hand==InteractionHand.MAIN_HAND?player.getMainArm():player.getMainArm().getOpposite();int side=arm==HumanoidArm.RIGHT?1:-1;
  float kick=WeaponMotion.recoil(player,stack,delta);
  if(!player.isInvisible()){
   pose.pushPose();pose.translate(-side*.48,-.07,kick*.035);renderPlayerArm(pose,nodes,light,0,0,arm);pose.popPose();
   if(longGun&&player.getItemInHand(hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND).isEmpty()){
    pose.pushPose();pose.translate(side*.55,-.20,-.25);pose.mulPose(Axis.ZP.rotationDegrees(-side*12));renderPlayerArm(pose,nodes,light,0,0,arm.getOpposite());pose.popPose();
   }
  }
  pose.pushPose();pose.translate(side*.015,longGun?-.34:-.225,-.85+kick*.08);
  if(f!=null){pose.translate(0,f.lift()*.3,-f.reach()*.2);pose.mulPose(Axis.XP.rotationDegrees(-kick*f.kick()));}
  pose.scale(.7f,.7f,.7f);
  ((ItemInHandRenderer)(Object)this).renderItem(player,stack,ItemDisplayContext.NONE,pose,nodes,light);pose.popPose();ci.cancel();
 }
}
