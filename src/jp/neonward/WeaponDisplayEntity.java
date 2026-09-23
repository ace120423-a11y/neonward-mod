package jp.neonward;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;

/** No Container interface: hoppers and menus cannot bypass ownership or extract the display copy. */
public final class WeaponDisplayEntity extends BlockEntity {
    private ItemStack weapon = ItemStack.EMPTY;
    private UUID owner;
    private String depositor = "";
    private boolean released;

    public WeaponDisplayEntity(BlockPos pos, BlockState state) { super(WeaponDisplay.TYPE, pos, state); }
    public ItemStack displayedStack() { return weapon.copy(); }
    public boolean ownedBy(UUID id) { return id != null && id.equals(owner); }
    public String depositorName() { return depositor; }
    void assignOwner(UUID id) { if (owner == null) { owner = Objects.requireNonNull(id); changed(); } }

    /** Called only on the server main thread after vanilla block reach validation. */
    void interact(Player p, InteractionHand hand) {
        if (!(level instanceof ServerLevel) || isRemoved() || released || !p.isAlive() || p.isSpectator()
            || p.level() != level || p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(worldPosition)) > 64
            || level.getBlockEntity(worldPosition) != this) return;
        if (!weapon.isEmpty() && !p.isShiftKeyDown()) { describe(p); return; }
        if (!ownedBy(p.getUUID())) { message(p, "展示品の出し入れは展示台の所有者だけができます"); return; }
        if (!HomeBuildingRules.canBreak(level, p, worldPosition)) { message(p, "この場所の家具を編集できません"); return; }
        if (!weapon.isEmpty()) {
            // Require a free slot. A rejected withdrawal never mutates either inventory.
            int slot = p.getInventory().getFreeSlot();
            if (slot < 0) { message(p, "取り出すには持ち物に空き枠が必要です"); return; }
            var result = take(p.getUUID());
            p.getInventory().setItem(slot, result);
            p.getInventory().setChanged();
            p.containerMenu.broadcastChanges();
            return;
        }
        if (p.isShiftKeyDown()) { message(p, "武器を手に持ち右クリックで展示します"); return; }
        if (!deposit(p.getUUID(), p.getGameProfile().name(), p.getItemInHand(hand))) {
            message(p, "武器を手に持ち右クリックで展示します"); return;
        }
        p.getInventory().setChanged();
        p.containerMenu.broadcastChanges();
        describe(p);
    }

    // Package-private transaction seams for integration tests; callers must validate world/player access.
    boolean deposit(UUID actor, String name, ItemStack held) {
        if (released || !ownedBy(actor) || !weapon.isEmpty() || !WeaponDisplay.accepts(held)) return false;
        weapon = held.split(1); // Actual item, even in creative. All data components travel with it.
        depositor = name;
        changed();
        return true;
    }
    ItemStack take(UUID actor) {
        if (released || !ownedBy(actor)) return ItemStack.EMPTY;
        var result = weapon;
        weapon = ItemStack.EMPTY;
        depositor = "";
        changed();
        return result;
    }
    private void changed() {
        setChanged();
        if (level instanceof ServerLevel) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
    private static void message(Player p, String text) { p.sendSystemMessage(Component.literal(text)); }
    private void describe(Player p) {
        p.sendSystemMessage(weapon.getHoverName().copy().append(" / 展示者: " + depositor));
        message(p, String.format(Locale.ROOT, "攻撃性能: %.0f%%", RolledWeapons.multiplier(weapon) * 100));
        if (weapon.getItem() instanceof NeonArsenal.Rifle gun) {
            message(p, String.format(Locale.ROOT, "ダメージ: %.2f / 射程: %.1f", gun.damage * RolledWeapons.multiplier(weapon), gun.range * GunAttachments.range(weapon)));
            for (int mount = 0; mount < 4; mount++) {
                int code = GunAttachments.installed(weapon, mount);
                if (code >= 0) message(p, GunAttachments.name(code) + " / " + GunAttachments.effect(code));
            }
        }
        for (var entry : weapon.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).modifiers())
            p.sendSystemMessage(Component.translatable(entry.attribute().value().getDescriptionId())
                .append(": " + entry.modifier().amount() + " (" + entry.modifier().operation() + ")"));
        for (var line : weapon.getOrDefault(DataComponents.LORE, ItemLore.EMPTY).lines()) p.sendSystemMessage(line);
        message(p, "所有者はShift＋右クリックで取り出せます");
    }

    @Override protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (!weapon.isEmpty()) out.store("Weapon", ItemStack.CODEC, weapon);
        if (owner != null) out.putString("Owner", owner.toString());
        out.putString("Depositor", depositor);
        out.putBoolean("Released", released);
    }
    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        weapon = in.read("Weapon", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        try { owner = UUID.fromString(in.getStringOr("Owner", "")); } catch (IllegalArgumentException e) { owner = null; }
        depositor = in.getStringOr("Depositor", "");
        released = in.getBooleanOr("Released", false);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider lookup) { return saveWithoutMetadata(lookup); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    @Override public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        // This is the sole drop path. No loot table, Container drops, or client-side drops.
        // Unloading a chunk calls setRemoved, not this method, so unloading never drops items.
        if (level instanceof ServerLevel server) for (var stack : releaseForRemoval()) drop(server, stack);
        super.preRemoveSideEffects(pos, state);
    }
    List<ItemStack> releaseForRemoval() {
        if (released) return List.of();
        var drops = new ArrayList<ItemStack>();
        if (!weapon.isEmpty()) drops.add(weapon);
        drops.add(new ItemStack(WeaponDisplay.ITEM));
        weapon = ItemStack.EMPTY;
        depositor = "";
        released = true;
        setChanged();
        return drops;
    }
    private void drop(ServerLevel server, ItemStack stack) {
        if (stack.isEmpty()) return;
        var item = new ItemEntity(server, worldPosition.getX() + .5, worldPosition.getY() + .6, worldPosition.getZ() + .5, stack);
        item.setTarget(owner);
        item.setUnlimitedLifetime();
        item.setInvulnerable(true);
        item.setDefaultPickUpDelay();
        server.addFreshEntity(item);
    }
}
