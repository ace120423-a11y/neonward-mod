package jp.neonward.mixin;
import jp.neonward.PairedHands;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Minecraft.class)
public abstract class PairedAttackMixin {
 @Unique private LocalPlayer neonward$pairPlayer;
 @Unique private Item neonward$pairItem;
 @Unique private boolean neonward$nextLeft;
 @Inject(method="tick",at=@At("HEAD"))
 private void neonward$resetPair(CallbackInfo ci){
  var p=((Minecraft)(Object)this).player;
  Item item=p!=null&&PairedHands.active(p)?p.getMainHandItem().getItem():null;
  if(p!=neonward$pairPlayer||item!=neonward$pairItem){neonward$pairPlayer=p;neonward$pairItem=item;neonward$nextLeft=false;}
 }
 @Redirect(method="startAttack",at=@At(value="INVOKE",target="Lnet/minecraft/client/player/LocalPlayer;swing(Lnet/minecraft/world/InteractionHand;)V"))
 private void neonward$alternate(LocalPlayer p,InteractionHand vanilla){
  if(!PairedHands.active(p)){p.swing(vanilla);return;}
  if(p!=neonward$pairPlayer||p.getMainHandItem().getItem()!=neonward$pairItem){neonward$pairPlayer=p;neonward$pairItem=p.getMainHandItem().getItem();neonward$nextLeft=false;}
  // Match vanilla's acceptance gate; ignored rapid clicks do not advance the pair.
  if(p.swinging&&p.swingTime>=0&&p.swingTime<((PairedSwingAccess)p).neonward$swingDuration()/2){p.swing(p.swingingArm);return;}
  HumanoidArm arm=neonward$nextLeft?HumanoidArm.LEFT:HumanoidArm.RIGHT;
  neonward$nextLeft=!neonward$nextLeft;
  // LocalPlayer.swing also sends the vanilla animation packet to other players.
  p.swing(arm==p.getMainArm()?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND);
 }
}
