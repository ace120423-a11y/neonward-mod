package jp.neonward.mixin;
import jp.neonward.ChainPoseState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.*;
@Mixin(AvatarRenderState.class)
public class ChainPoseStateMixin implements ChainPoseState {
 @Unique private float neonward$phase=-1;
 public float neonward$chainPhase(){return neonward$phase;}
 public void neonward$chainPhase(float phase){neonward$phase=phase;}
}
