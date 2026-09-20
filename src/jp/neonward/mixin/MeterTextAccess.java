package jp.neonward.mixin;
import net.minecraft.world.entity.Display;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(Display.TextDisplay.class)
public interface MeterTextAccess {
 @Invoker("setText") void neonSetText(Component text);
 @Invoker("getText") Component neonGetText();
 @Invoker("setTextOpacity") void neonSetOpacity(byte opacity);
}
