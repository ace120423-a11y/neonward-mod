package jp.neonward.mixin;
import jp.neonward.PairedHands;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ArmedEntityRenderState.class)
public class PairedStateMixin {
 @Inject(method="extractArmedEntityRenderState",at=@At("TAIL"))
 private static void pair(LivingEntity e,ArmedEntityRenderState state,ItemModelResolver resolver,float delta,CallbackInfo ci){
  if(!(e instanceof Player p)||!PairedHands.active(p))return;
  var item=p.getMainHandItem();boolean left=p.getMainArm()==HumanoidArm.RIGHT;
  state.swingAnimationType=item.getSwingAnimation().type();
  if(left){state.leftHandItemStack=item.copy();resolver.updateForLiving(state.leftHandItemState,item,ItemDisplayContext.THIRD_PERSON_LEFT_HAND,p);}
  else{state.rightHandItemStack=item.copy();resolver.updateForLiving(state.rightHandItemState,item,ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,p);}
 }
}
