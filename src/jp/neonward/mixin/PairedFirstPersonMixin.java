package jp.neonward.mixin;
import jp.neonward.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.player.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemInHandRenderer.class)
public abstract class PairedFirstPersonMixin {
 @Shadow private void submitArmWithItem(AbstractClientPlayer p,float delta,float pitch,InteractionHand hand,float swing,ItemStack item,float equip,PoseStack pose,SubmitNodeCollector nodes,int light){}
 @Shadow private void renderPlayerArm(PoseStack pose,SubmitNodeCollector nodes,int light,float equip,float swing,HumanoidArm arm){}
 @Inject(method="submitHandsWithItems",at=@At("HEAD"),cancellable=true)
 private void pair(float delta,PoseStack pose,SubmitNodeCollector nodes,LocalPlayer p,int light,CallbackInfo ci){
  if(!PairedHands.active(p))return;
  ci.cancel();if(CameraScreen.active()||p.isInvisible())return;
  float swing=p.getAttackAnim(delta);var weapon=p.getMainHandItem();
  for(var hand:InteractionHand.values()){
   float handSwing=hand==p.swingingArm?swing:0;
   pose.pushPose();
   if(DualGuard.guarding(p)){
    int side=(hand==InteractionHand.MAIN_HAND?p.getMainArm():p.getMainArm().getOpposite())==HumanoidArm.RIGHT?1:-1;
    pose.translate(side*.19,-.30,-.9+(side==1?0:.035));
    pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(side*42));
    pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-90));
    ((ItemInHandRenderer)(Object)this).renderItem(p,weapon,side==1?net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_RIGHT_HAND:net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_LEFT_HAND,pose,nodes,light);
   }else if(PairedHands.gauntlet(weapon)){
    WeaponMotion.first(p,hand,weapon,handSwing,delta,pose);
    float charge=p.isUsingItem()?Math.min(1,p.getTicksUsingItem()/30f):0;
    pose.translate(0,.08+charge*.10,charge*.12);
    renderPlayerArm(pose,nodes,light,0,handSwing,hand==InteractionHand.MAIN_HAND?p.getMainArm():p.getMainArm().getOpposite());
   }else submitArmWithItem(p,delta,p.getXRot(delta),hand,handSwing,weapon,0,pose,nodes,light);
   pose.popPose();
  }
 }
}
