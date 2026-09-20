package jp.neonward;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.Commands;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import java.util.*;

public final class PhoneEquipment {
 static final Map<UUID,Long> lastHold=new HashMap<>();
 public static final AttachmentType<ItemStack> SLOT=AttachmentRegistry.<ItemStack>builder()
  .persistent(ItemStack.CODEC).copyOnDeath().syncWith(ItemStack.STREAM_CODEC,AttachmentSyncPredicate.all())
  .buildAndRegister(NeonWard.id("phone_slot"));
 public static ItemStack get(Player p){return ((AttachmentTarget)p).getAttachedOrElse(SLOT,ItemStack.EMPTY);}
 public static final AttachmentType<Boolean> HELD=AttachmentRegistry.<Boolean>builder()
  .syncWith(net.minecraft.network.codec.ByteBufCodecs.BOOL,AttachmentSyncPredicate.all())
  .buildAndRegister(NeonWard.id("phone_held"));
 public static boolean isHeld(Player p){return ((AttachmentTarget)p).getAttachedOrElse(HELD,false)&&!get(p).isEmpty();}
 public static void equip(Player p,ItemStack held){
  if(!held.is(NeonWard.PHONE)||held.isEmpty())return;
  if(!get(p).isEmpty()){p.sendOverlayMessage(Component.literal("スマホはすでに専用枠に装備されています。Pで開けます。"));return;}
  ((AttachmentTarget)p).setAttached(SLOT,held.copyWithCount(1));held.shrink(1);
  p.sendSystemMessage(Component.literal("スマホを専用枠に固定しました。Pで開けます。取り外しや投げ捨てはできません。"));
 }
 public static void init(){
  ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->lastHold.remove(handler.player.getUUID()));
  CommandRegistrationCallback.EVENT.register((d,c,e)->d.register(Commands.literal("neonphone").then(Commands.literal("hold").executes(ctx->{
   var p=ctx.getSource().getPlayerOrException();if(p.isSpectator())return 0;long now=p.level().getGameTime();if(now-lastHold.getOrDefault(p.getUUID(),-100L)<6)return 0;lastHold.put(p.getUUID(),now);
   boolean held=!isHeld(p);((AttachmentTarget)p).setAttached(HELD,held);
   p.sendOverlayMessage(Component.literal(held?"スマホ：右クリックで開く / Pでしまう":"スマホを専用枠にしまいました"));return 1;
  }))));
  ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->{
   var p=handler.player;
   if(get(p).isEmpty())((AttachmentTarget)p).setAttached(SLOT,new ItemStack(NeonWard.PHONE));
  });
  CommandRegistrationCallback.EVENT.register((d,c,e)->d.register(Commands.literal("neonphone").then(Commands.literal("unequip").executes(ctx->{
   var p=ctx.getSource().getPlayerOrException();p.sendSystemMessage(Component.literal("スマホは専用枠に固定されています。取り外したり捨てたりはできません。"));return 0;
  }))));
 }
}
