package jp.neonward.mixin;
import jp.neonward.Cyberware;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LivingEntity.class)
public class CyberDefenseMixin {
 @Inject(method="getDamageAfterArmorAbsorb",at=@At("RETURN"),cancellable=true)
 private void cyberDefense(DamageSource source,float amount,CallbackInfoReturnable<Float> result){if((Object)this instanceof ServerPlayer p&&!source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)){double defense=Cyberware.defense(p);if(defense>0)result.setReturnValue((float)(result.getReturnValue()/(1+defense)));}}
}
