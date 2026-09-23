# Backpack implementation / v0.1.57

## User-approved design
- Market interior stock counter screen gains a separate bag-sales button.
- Small: +9 slots / 3,000 Cr. Medium: +18 / 8,000 Cr. Large: +27 / 20,000 Cr.
- Inventory gains a Backpack button opening one dedicated bag equipment slot and the extra cargo slots. Existing three accessory slots are unchanged.
- Extra cargo is accessible while equipped. Unequipping or switching bags preserves cargo on the original item. The vanilla 36-slot Inventory array is not patched.

## Safety
- Player attachment persists the equipped item and copies it on death. Bag contents use vanilla ItemContainerContents, retaining their normal components.
- Native server container operations handle pickup, swap, drag, quick-move and cursor return. Expired/closed/superseded menus cannot overwrite another equipped bag.
- Bags cannot be nested. Storage carrying items, block-entity data and oversized/malformed bags are refused rather than silently truncated. Empty ordinary building items remain usable.
- Shop quotes are separate from stocks/equipment; location, life state, token, expiration, price, inventory space and ledger save are verified server-side. Processed requests rotate the quote token.
- No equipment-content broadcast to nearby players; the owning player's native menu synchronizes its slots.

Implementation build/testing was performed in disposable local worlds only. After explicit publication approval, the tested artifact was installed with both server and Minecraft stopped. Backup: `C:/Users/ace12/Documents/NeonWardServer/backups/20260924-021913-120475` (world, mods, configuration, and previous client jar). Installed server/client and distribution SHA256: `B50B86EEF8ECFBF84EBB736C84524EE2D1DD0172B3E756C460C91121CE338262`. Release target: v0.1.57.

## Verification
- `python build.py` passed.
- `build/backpack-integration-20260924-020534/smoke.log`: 360 checks passed. Covers all three capacities, right/left pickup, both drag styles, hotbar swap, quick-move, full-inventory removal, filled bag replacement, item/entity codec persistence, Fabric death copy, stale/disconnected/dead-player menus, rejected nesting and malformed bags, three prices, replay/range/session guards, full inventory and ledger-save rollback.
- The early test failure at the ninth slot exposed an inherited `Slot.index` name collision; the slot closure now uses `cargoIndex`. Empty-container default components in 26.2 are handled without allowing filled containers. A malformed-item test reference was copied to avoid expected-value mutation by vanilla pickup.
- Final artifact rerun: `build/backpack-integration-20260924-021317/smoke.log` passed all 360 checks.
- Native-client QA: `build/backpack-client-20260924-021358/client.log` passed six captures, real container clicks, server inventory conservation, actual market clerk entry and bag-sales button, synchronized capacities and prices, and Sodium model rendering. Screenshots were visually inspected.
- The initial client test exposed the stock screen's periodic refresh colliding with the shared UI command limiter. Bag-sales transition now pauses stock refreshes and waits 250 ms before opening; the final test deliberately presses at stock refresh tick 40 and succeeds. No spam protection was removed.
- Final existing-feature regression: `build/land-integration-20260924-021419/smoke.log` passed with normal world-save shutdown.
- The disposable flat QA world also logs the existing Sakura road migration rejecting unexpected air; that migration preserves unexpected terrain and is unrelated to backpacks. No production terrain was loaded or changed.
