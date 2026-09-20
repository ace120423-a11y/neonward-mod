package jp.neonward.mixin;
import net.minecraft.world.entity.Display;
import com.mojang.math.Transformation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(Display.class)
public interface LiftDisplayAccess {
 @Invoker("setPosRotInterpolationDuration") void neonPositionDuration(int ticks);
 @Invoker("setTransformation") void neonLiftTransform(Transformation t);
 @Invoker("setTransformationInterpolationDelay") void neonLiftDelay(int ticks);
}
