package jp.neonward.mixin;
import jp.neonward.*;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.Avatar;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.resources.Identifier;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(AvatarRenderer.class)
public class PairedPoseMixin {
 @Inject(method="extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",at=@At("TAIL"))
 private void pose(Avatar p,AvatarRenderState state,float delta,CallbackInfo ci){
  if(PairedHands.paired(p.getMainHandItem())){state.leftArmPose=HumanoidModel.ArmPose.ITEM;state.rightArmPose=HumanoidModel.ArmPose.ITEM;}
 }
 @Inject(method="renderHand",at=@At("TAIL"))
 private void glove(PoseStack pose,SubmitNodeCollector nodes,int light,Identifier skin,ModelPart arm,boolean sleeve,CallbackInfo ci){
  var p=net.minecraft.client.Minecraft.getInstance().player;
  if(p!=null&&PairedHands.gauntlet(p.getMainHandItem())){pose.pushPose();arm.translateAndRotate(pose);PairedGloves.draw(pose,nodes,light,arm.x>0);pose.popPose();}
 }
}
