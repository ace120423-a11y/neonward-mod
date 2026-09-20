package jp.neonward.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.projectile.FishingHook;
@Mixin(FishingHook.class)
public class FishingCollectibleMixin {
 @ModifyArg(method="retrieve",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/item/ItemEntity;<init>(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;)V"),index=4)
 private ItemStack neonCaught(ItemStack stack){return jp.neonward.Aquariums.caught(stack);}
}
