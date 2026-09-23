package jp.neonward;

import java.util.Set;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

/** Parent hooks: init after NeonFurniture.init; initClient on the client; sell ITEM. */
public final class WeaponDisplay {
    public static Block BLOCK;
    public static Item ITEM;
    public static BlockEntityType<WeaponDisplayEntity> TYPE;
    private WeaponDisplay() {}

    public static void init() {
        if (BLOCK != null) return;
        var id = NeonWard.id("weapon_display");
        BLOCK = Registry.register(BuiltInRegistries.BLOCK, id, new WeaponDisplayBlock(
            BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, id))
                .strength(2.5f, 3600000).sound(SoundType.METAL).noOcclusion()
                .lightLevel(s -> 7).pushReaction(PushReaction.BLOCK).noLootTable()));
        ITEM = Registry.register(BuiltInRegistries.ITEM, id, new BlockItem(BLOCK,
            new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id)).useBlockDescriptionPrefix()));
        TYPE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id,
            new BlockEntityType<>(WeaponDisplayEntity::new, Set.of(BLOCK)));
        // HomeBuildingRules.movable already consults this registry (including its placement mixin).
        NeonFurniture.BLOCKS.put("weapon_display", BLOCK);
        AttackBlockCallback.EVENT.register((p, l, hand, pos, face) ->
            l.getBlockEntity(pos) instanceof WeaponDisplayEntity display && !mayBreak(p, display)
                ? InteractionResult.FAIL : InteractionResult.PASS);
        PlayerBlockBreakEvents.BEFORE.register((l, p, pos, state, be) ->
            !(be instanceof WeaponDisplayEntity display) || mayBreak(p, display));
        // Sneaking with a held item otherwise bypasses block.useItemOn in vanilla.
        UseBlockCallback.EVENT.register((p, l, hand, hit) -> {
            if (!(l.getBlockEntity(hit.getBlockPos()) instanceof WeaponDisplayEntity display)) return InteractionResult.PASS;
            if (!l.isClientSide()) display.interact(p, hand);
            return InteractionResult.SUCCESS;
        });
    }

    private static boolean mayBreak(Player p, WeaponDisplayEntity display) {
        return !p.isSpectator() && p.isAlive() && display.ownedBy(p.getUUID())
            && HomeBuildingRules.canBreak(p.level(), p, display.getBlockPos());
    }

    public static boolean accepts(ItemStack stack) {
        return !stack.isEmpty() && (NeonArsenal.DROPS.contains(stack.getItem())
            || stack.is(net.minecraft.tags.ItemTags.SWORDS) || stack.is(Items.BOW)
            || stack.is(Items.CROSSBOW) || stack.is(Items.TRIDENT) || stack.is(Items.MACE));
    }

    public static void initClient() { WeaponDisplayRenderer.init(); }
}
