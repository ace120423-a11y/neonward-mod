package jp.neonward.mixin;
import jp.neonward.CityProtection;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import java.util.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ServerExplosion.class)
public class CityExplosionMixin {
 @Shadow @Final private ServerLevel level;
 @Inject(method="calculateExplodedPositions",at=@At("RETURN"),cancellable=true)
 private void protectCity(CallbackInfoReturnable<List<BlockPos>> cir){var kept=new ArrayList<>(cir.getReturnValue());kept.removeIf(p->CityProtection.structure(level,p));cir.setReturnValue(kept);}
}
