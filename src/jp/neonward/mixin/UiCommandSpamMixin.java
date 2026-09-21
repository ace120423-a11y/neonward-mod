package jp.neonward.mixin;

import jp.neonward.UiCommandLimiter;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class UiCommandSpamMixin {
 @Unique private final UiCommandLimiter neonward$uiLimiter=new UiCommandLimiter();
 @Shadow protected abstract void performUnsignedChatCommand(String command);

 // Runs after vanilla input validation and scheduling onto the server thread.
 // Reuse vanilla parsing/permissions; skip only command spam accounting for our UI.
 @Inject(method="lambda$handleChatCommand$0",at=@At("HEAD"),cancellable=true)
 private void neonward$handleUi(ServerboundChatCommandPacket packet,CallbackInfo ci){
  String command=packet.command();
  if(!UiCommandLimiter.isUi(command))return;
  ci.cancel();
  if(neonward$uiLimiter.accept(command,System.nanoTime()))performUnsignedChatCommand(command);
 }
}
