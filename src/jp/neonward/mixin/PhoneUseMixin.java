package jp.neonward.mixin;
import jp.neonward.PhoneEquipment;
import jp.neonward.PhoneScreen;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(Minecraft.class)
public class PhoneUseMixin {
 private boolean phone(){var mc=(Minecraft)(Object)this;return mc.player!=null&&PhoneEquipment.isHeld(mc.player);}
 @Inject(method="startUseItem",at=@At("HEAD"),cancellable=true)
 private void openPhone(CallbackInfo ci){var mc=(Minecraft)(Object)this;
  if(mc.player!=null&&mc.player.getVehicle() instanceof jp.neonward.VerticalLift lift){mc.gui.setScreen(new jp.neonward.LiftScreen(lift));ci.cancel();return;}
  if(mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult h&&h.getEntity() instanceof jp.neonward.VerticalLift)return;
  if(mc.level!=null&&mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult h&&(mc.level.getBlockState(h.getBlockPos()).is(jp.neonward.LiftSystem.PANEL)||mc.level.getBlockState(h.getBlockPos()).is(jp.neonward.VendingMachines.BLOCK)))return;
  if(mc.level!=null&&mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult h&&(mc.level.getBlockState(h.getBlockPos()).is(jp.neonward.Cyberware.TERMINAL)||mc.level.getBlockState(h.getBlockPos()).is(jp.neonward.StreetFashion.COUNTER)||jp.neonward.StockMarket.isPC(mc.level.getBlockState(h.getBlockPos()))||mc.level.getBlockState(h.getBlockPos()).is(jp.neonward.NeonFurniture.BLOCKS.get("tv_remote"))||jp.neonward.MediaBridge.isScreen(mc.level.getBlockState(h.getBlockPos()))))return;if(mc.level!=null&&mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult nh&&(mc.level.getBlockState(nh.getBlockPos()).is(jp.neonward.GuildServices.QUEST)||mc.level.getBlockState(nh.getBlockPos()).is(jp.neonward.GuildServices.EXCHANGE)||mc.level.getBlockState(nh.getBlockPos()).is(jp.neonward.NeonZones.ENTRY)||mc.level.getBlockState(nh.getBlockPos()).is(jp.neonward.NeonZones.EXIT)||mc.level.getBlockState(nh.getBlockPos()).is(jp.neonward.NeonZones.NEXT)))return;if(phone()){mc.gui.setScreen(new PhoneScreen());ci.cancel();}}
 @Inject(method="startAttack",at=@At("HEAD"),cancellable=true)
 private void noAttack(CallbackInfoReturnable<Boolean> cir){if(phone())cir.setReturnValue(false);}
 @Inject(method="continueAttack",at=@At("HEAD"),cancellable=true)
 private void noBreak(boolean held,CallbackInfo ci){if(phone())ci.cancel();}
}

