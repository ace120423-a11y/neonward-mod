package jp.neonward.mixin;
import jp.neonward.PhoneEquipment;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LivingEntity.class)
public class PhoneHandMixin {
 @Inject(method="getItemBySlot",at=@At("HEAD"),cancellable=true)
 private void phoneHand(EquipmentSlot slot,CallbackInfoReturnable<ItemStack> cir){
  if(slot==EquipmentSlot.MAINHAND&&(Object)this instanceof Player p&&PhoneEquipment.isHeld(p))cir.setReturnValue(PhoneEquipment.get(p));
 }
}
