package jp.neonward.mixin;
import jp.neonward.WestLand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(FarmlandBlock.class)
public class LandTrampleMixin {
 @Inject(method="fallOn",at=@At("HEAD"),cancellable=true)
 private void protect(Level l,BlockState state,BlockPos pos,Entity e,double distance,CallbackInfo ci){if(WestLand.area(l,pos))ci.cancel();}
}
