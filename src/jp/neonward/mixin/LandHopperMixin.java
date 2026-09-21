package jp.neonward.mixin;
import jp.neonward.LandSafety;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(HopperBlockEntity.class)
public class LandHopperMixin {
 @Inject(method="tryMoveItems",at=@At("HEAD"),cancellable=true)
 private static void moving(Level l,BlockPos pos,net.minecraft.world.level.block.state.BlockState state,HopperBlockEntity hopper,java.util.function.BooleanSupplier supplier,CallbackInfoReturnable<Boolean> cir){if(LandSafety.sensitive(l,pos))cir.setReturnValue(false);}
 @Inject(method="suckInItems",at=@At("HEAD"),cancellable=true)
 private static void protect(Level l,Hopper hopper,CallbackInfoReturnable<Boolean> cir){if(LandSafety.sensitive(l,BlockPos.containing(hopper.getLevelX(),hopper.getLevelY(),hopper.getLevelZ())))cir.setReturnValue(false);}
}
