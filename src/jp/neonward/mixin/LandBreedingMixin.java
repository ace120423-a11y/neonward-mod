package jp.neonward.mixin;
import jp.neonward.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Animal.class)
public class LandBreedingMixin {
 @Inject(method="aiStep",at=@At("RETURN"))
 private void stay(CallbackInfo ci){var a=(Animal)(Object)this;if(!a.level().isClientSide())LandAnimals.contain(a);}
 @Inject(method="finalizeSpawnChildFromBreeding",at=@At("HEAD"))
 private void inherit(ServerLevel l,Animal partner,net.minecraft.world.entity.AgeableMob child,CallbackInfo ci){int id=LandAnimals.plot((Animal)(Object)this);if(id>=0&&child!=null)LandAnimals.mark(child,id);}
 @Inject(method="spawnChildFromBreeding",at=@At("HEAD"),cancellable=true)
 private void limit(ServerLevel l,Animal partner,CallbackInfo ci){var a=(Animal)(Object)this;var pos=a.blockPosition();if(!WestLand.area(l,pos))return;int id=LandLayout.plot(pos.getX(),pos.getZ());if(id<0||l.getEntitiesOfClass(Animal.class,new AABB(LandLayout.x(id),48,LandLayout.z(id),LandLayout.x(id)+32,128,LandLayout.z(id)+32)).size()>=LandLayout.ANIMAL_LIMIT){a.resetLove();partner.resetLove();ci.cancel();}}
}
