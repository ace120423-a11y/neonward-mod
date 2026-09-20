package jp.neonward.mixin;
import net.minecraft.world.level.block.EnchantingTableBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(EnchantingTableBlock.class)
public class EnchantParticlesMixin {
 @Inject(method="animateTick",at=@At("HEAD"),cancellable=true)
 private void replaceMagicGlyphs(CallbackInfo ci){ci.cancel();}
}
