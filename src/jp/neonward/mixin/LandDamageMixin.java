package jp.neonward.mixin;
import jp.neonward.WestLand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LivingEntity.class)
public class LandDamageMixin {
 @Inject(method="hurtServer",at=@At("HEAD"),cancellable=true)
 private void protect(ServerLevel l,DamageSource source,float amount,CallbackInfoReturnable<Boolean> cir){var e=(LivingEntity)(Object)this;if(!(e instanceof Player)&&WestLand.area(l,e.blockPosition())&&(!(source.getEntity() instanceof Player p)||!WestLand.edit(l,p,e.blockPosition())))cir.setReturnValue(false);}
}
