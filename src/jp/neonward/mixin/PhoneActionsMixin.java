package jp.neonward.mixin;
import jp.neonward.PhoneEquipment;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ServerGamePacketListenerImpl.class)
public class PhoneActionsMixin {
 @Shadow public ServerPlayer player;
 @Inject(method="handlePlayerAction",at=@At("HEAD"),cancellable=true)
 private void protectPocket(ServerboundPlayerActionPacket packet,CallbackInfo ci){
  if(PhoneEquipment.isHeld(player))ci.cancel();
 }
}
