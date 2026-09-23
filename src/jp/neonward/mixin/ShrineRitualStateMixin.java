package jp.neonward.mixin;
import jp.neonward.*;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.*;
@Mixin(AvatarRenderState.class)
public class ShrineRitualStateMixin implements ShrineRitualPose {
 @Unique private ShrineRitualClient.Frame neonward$ritual;
 public ShrineRitualClient.Frame neonward$ritual(){return neonward$ritual;}
 public void neonward$ritual(ShrineRitualClient.Frame frame){neonward$ritual=frame;}
}
