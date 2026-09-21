package jp.neonward.mixin;
import jp.neonward.WestLand;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(FlowingFluid.class)
public class LandFluidMixin {
 @Inject(method="spreadTo",at=@At("HEAD"),cancellable=true)
 private void boundary(LevelAccessor access,BlockPos pos,BlockState block,Direction direction,FluidState fluid,CallbackInfo ci){if(access instanceof Level l&&!WestLand.transfer(l,pos.relative(direction.getOpposite()),pos))ci.cancel();}
}
