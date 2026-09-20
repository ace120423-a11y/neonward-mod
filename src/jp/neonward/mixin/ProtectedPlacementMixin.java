package jp.neonward.mixin;
import jp.neonward.HomeBuildingRules;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(BlockItem.class)
public class ProtectedPlacementMixin {
 @Inject(method="canPlace",at=@At("HEAD"),cancellable=true)
 private void protectPlacement(BlockPlaceContext ctx,BlockState state,CallbackInfoReturnable<Boolean> cir){if(!HomeBuildingRules.canPlace(ctx,state.getBlock()))cir.setReturnValue(false);}
 @Inject(method="placeBlock",at=@At("HEAD"),cancellable=true)
 private void protectActualPlacement(BlockPlaceContext ctx,BlockState state,CallbackInfoReturnable<Boolean> cir){if(!HomeBuildingRules.canPlace(ctx,state.getBlock()))cir.setReturnValue(false);}
}
