package jp.neonward;
import net.minecraft.core.component.*;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.player.Player;
/** Mirrors Minecraft 26.2 shield defaults; blocking itself stays vanilla. */
public final class NeonShield {
 public static Item ITEM;
 static void register(){var p=NeonArsenal.properties("sentinel_shield").durability(336).repairable(net.minecraft.tags.ItemTags.WOODEN_TOOL_MATERIALS).equippableUnswappable(net.minecraft.world.entity.EquipmentSlot.OFFHAND)
  .component(DataComponents.BANNER_PATTERNS,net.minecraft.world.level.block.entity.BannerPatternLayers.EMPTY)
  .component(DataComponents.ITEM_MODEL,NeonWard.id("sentinel_shield"))
  .component(DataComponents.BREAK_SOUND,net.minecraft.sounds.SoundEvents.SHIELD_BREAK)
  .delayedComponent(DataComponents.BLOCKS_ATTACKS,lookup->new net.minecraft.world.item.component.BlocksAttacks(.25f,1f,java.util.List.of(new net.minecraft.world.item.component.BlocksAttacks.DamageReduction(90f,java.util.Optional.empty(),0f,1f)),new net.minecraft.world.item.component.BlocksAttacks.ItemDamageFunction(3f,1f,1f),java.util.Optional.of(lookup.getOrThrow(net.minecraft.tags.DamageTypeTags.BYPASSES_SHIELD)),java.util.Optional.of(net.minecraft.sounds.SoundEvents.SHIELD_BLOCK),java.util.Optional.of(net.minecraft.sounds.SoundEvents.SHIELD_BREAK)));
  p.component(DataComponents.ITEM_NAME,net.minecraft.network.chat.Component.literal("センチネル・シールド"));ITEM=NeonArsenal.add("sentinel_shield",new ShieldItem(p));}
 static boolean offhandReady(Player p){var s=p.getOffhandItem();return s.has(DataComponents.BLOCKS_ATTACKS)&&!p.getCooldowns().isOnCooldown(s);}
}
