package jp.neonward.mixin;
import jp.neonward.WestLand;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(BucketItem.class)
public class LandBucketMixin {
 @Inject(method="use",at=@At("HEAD"),cancellable=true)
 private void protect(Level l,Player p,InteractionHand hand,CallbackInfoReturnable<InteractionResult> cir){if(p.pick(5,0,true) instanceof BlockHitResult b){var hit=b.getBlockPos();var to=hit.relative(b.getDirection());if((WestLand.area(l,hit)||WestLand.area(l,to))&&(!WestLand.edit(l,p,hit)||!WestLand.edit(l,p,to)))cir.setReturnValue(InteractionResult.FAIL);}}
}
