package jp.neonward.mixin;
import jp.neonward.*;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(PistonBaseBlock.class)
public class LandPistonMixin {
 @Inject(method="isPushable",at=@At("HEAD"),cancellable=true)
 private static void protect(BlockState state,Level l,BlockPos pos,Direction dir,boolean destroy,Direction piston,CallbackInfoReturnable<Boolean> cir){if(LandSafety.sensitive(l,pos))cir.setReturnValue(false);}
}
