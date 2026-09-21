package jp.neonward.mixin;
import jp.neonward.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Level.class)
public class LandWorldMixin {
 @Inject(method="setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",at=@At("HEAD"),cancellable=true)
 private void preventDanger(BlockPos pos,BlockState state,int flags,int depth,CallbackInfoReturnable<Boolean> cir){if(WestLand.area((Level)(Object)this,pos)&&LandSafety.forbidden(state.getBlock()))cir.setReturnValue(false);}
}
