package jp.neonward.mixin;

import jp.neonward.CameraScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class CameraMovementMixin extends ClientInput {
 @Inject(method="tick",at=@At("HEAD"),cancellable=true)
 private void cameraMovement(CallbackInfo ci){
  if(!CameraScreen.active())return;
  var mc=Minecraft.getInstance();var o=mc.options;
  // Poll only movement keys. Do not forward attack, use, or inventory actions through the phone.
  keyPresses=mc.isWindowActive()?new Input(neonDown(o.keyUp),neonDown(o.keyDown),neonDown(o.keyLeft),neonDown(o.keyRight),neonDown(o.keyJump),neonDown(o.keyShift),neonDown(o.keySprint)):Input.EMPTY;
  moveVector=new Vec2((keyPresses.left()?1:0)-(keyPresses.right()?1:0),(keyPresses.forward()?1:0)-(keyPresses.backward()?1:0)).normalized();
  ci.cancel();
 }
 @Unique private static boolean neonDown(KeyMapping mapping){
  var key=InputConstants.getKey(mapping.saveString());
  return key.getType()==InputConstants.Type.KEYSYM&&key.getValue()>=0&&InputConstants.isKeyDown(Minecraft.getInstance().getWindow(),key.getValue());
 }
}
