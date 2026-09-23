package jp.neonward;

import java.util.*;
import net.minecraft.SharedConstants;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.*;

/** Standalone real-Minecraft stack/codec tests; never opens a world or changes shared registries. */
public final class WeaponDisplayTest {
    private static void check(boolean value, String reason) { if (!value) throw new AssertionError(reason); }
    private static WeaponDisplayEntity empty(UUID owner) {
        var display = new WeaponDisplayEntity(BlockPos.ZERO, Blocks.CHEST.defaultBlockState());
        display.assignOwner(owner);
        return display;
    }
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        // 26.2 item defaults normally arrive from server data packs. Bind minimal defaults in
        // this disposable JVM so the real stack codec can run without opening a server world.
        for (var item : List.of(Items.BOW, Items.STONE, Items.IRON_BLOCK))
            item.builtInRegistryHolder().bindComponents(DataComponents.COMMON_ITEM_COMPONENTS);
        // The transaction and serialization tests do not require Fabric registration or a running server.
        // Fabric adds intrusive-holder support at runtime. Use an existing type solely for this
        // offline entity's constructor; no type factory, registry entry, or world is modified.
        WeaponDisplay.TYPE = (BlockEntityType) net.minecraft.world.level.block.entity.BlockEntityTypes.CHEST;
        WeaponDisplay.ITEM = Items.IRON_BLOCK;
        var lookup = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        UUID owner = UUID.randomUUID(), visitor = UUID.randomUUID();
        var display = empty(owner);
        var held = new ItemStack(Items.BOW);
        held.set(DataComponents.CUSTOM_NAME, Component.literal("Original weapon"));
        held.set(DataComponents.DAMAGE, 17);
        held.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        held.set(DataComponents.ITEM_MODEL, net.minecraft.resources.Identifier.parse("neonward:test_weapon"));
        held.set(DataComponents.LORE, new ItemLore(List.of(Component.literal("Original lore"))));
        CustomData.update(DataComponents.CUSTOM_DATA, held, t -> {
            t.putInt("neon_weapon_power", 147);
            t.putInt("neon_weapon_tier", 3);
            t.putInt("nw_attachment_0", 5);
            t.putString("foreign_mod_component_data", "preserve me");
        });
        var original = held.copy();
        check(!display.deposit(visitor, "Visitor", held) && ItemStack.matches(held, original), "visitor deposit must not consume");
        check(!display.deposit(owner, "Owner", new ItemStack(Items.STONE)), "nonweapon rejected");
        check(display.deposit(owner, "OriginalName", held) && held.isEmpty(), "actual held item transferred");
        check(!display.deposit(owner, "Replacement", new ItemStack(Items.BOW)), "occupied stand rejects replacement");
        check(display.take(visitor).isEmpty(), "visitor cannot retrieve");
        display.assignOwner(visitor);
        check(display.ownedBy(owner), "cannot reassign an owned stand");
        display.displayedStack().setCount(0);
        check(ItemStack.matches(display.displayedStack(), original), "render copy cannot mutate authoritative item");
        var tag = display.getUpdateTag(lookup);
        var loaded = empty(visitor);
        loaded.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, lookup, tag));
        check(loaded.ownedBy(owner) && !loaded.ownedBy(visitor), "persisted UUID restored");
        check(loaded.depositorName().equals("OriginalName"), "original depositor name persisted");
        check(ItemStack.matches(loaded.displayedStack(), original), "all components survive NBT/client tag round trip");
        loaded.setRemoved(); // Chunk unload must not release the inventory.
        check(ItemStack.matches(loaded.displayedStack(), original), "chunk unload retains stored weapon");
        loaded.clearRemoved();
        check(ItemStack.matches(loaded.take(owner), original), "owner retrieves exact original stack");
        check(loaded.take(owner).isEmpty(), "repeated withdrawal never duplicates");
        var cleared = empty(owner);
        cleared.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, lookup, loaded.getUpdateTag(lookup)));
        check(cleared.displayedStack().isEmpty() && cleared.depositorName().isEmpty(), "empty state persists");
        var multiple = new ItemStack(Items.BOW, 2);
        check(cleared.deposit(owner, "Owner", multiple) && multiple.getCount() == 1 && cleared.displayedStack().getCount() == 1,
            "deposit transfers exactly one item");
        var breaking = empty(owner);
        check(breaking.deposit(owner, "Owner", original.copy()), "prepare occupied break");
        var drops = breaking.releaseForRemoval();
        check(drops.size() == 2 && ItemStack.matches(drops.get(0), original) && drops.get(1).is(WeaponDisplay.ITEM), "breaking returns original weapon and one stand");
        check(breaking.releaseForRemoval().isEmpty() && breaking.take(owner).isEmpty(), "break/retrieve replay cannot duplicate");
        check(!breaking.deposit(owner, "Owner", original.copy()), "removed display cannot accept another weapon");
        var restoredRemoved = empty(owner);
        restoredRemoved.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, lookup, breaking.getUpdateTag(lookup)));
        check(restoredRemoved.releaseForRemoval().isEmpty(), "removal marker round trips without drops");
        check(empty(owner).releaseForRemoval().size() == 1, "empty stand returns only pedestal");
        System.out.println("WEAPON_DISPLAY_PASS: authorization, actual transfer, no replacement/replay, component preservation, copy isolation, NBT sync, unload retention, empty persistence, occupied/empty break conservation");
    }
}
