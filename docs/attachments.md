# Attachment implementation (not yet released)

Use B while holding a ranged weapon. Four mounts: optic, magazine, grip, muzzle.
Eight designs and five tiers; physical parts are returned when swapped or detached.
Reloading blocks modification. Magazine changes preserve remaining rounds, not a free refill.
Guns with installed parts must be stripped before sale.

## Acquisition

- Dedicated MOD CAPSULE at casino (522,65,447), separate interaction tag and service.
- 1,000 Cr single / 10,000 Cr ten. Ten has no guarantee or altered odds.
- Exact rarity weights: 419 / 300 / 200 / 80 / 1 of 1,000.
- Weapon shop's attachment button offers all 40 variants at fixed, displayed prices.
- Enemy drop chance: field 6%, dungeon 12%, boss 60%; rarity uses the same weights.
- The existing weapon and cyberware gacha pools are unchanged.

## Effects

Tiers 0..4:

- Reflex / holographic: 1x; precision optics: 4x / 8x. Magnification does not increase with tier.
- Optic effective range +5/10/15/20/25%; recoil presentation reduction 10/15/20/25/30%.
- Extended magazine +20/30/40/50/60%, rounded upward.
- Quick magazine reload time -8/12/16/20/24%.
- Grip recoil presentation -10/20/30/40/50%.
- Range barrel effective range +10/15/20/25/30%.
- 8x only fits sniper and railgun. 4x excludes pistols, compact and cryo.
- Crossbow cannot take extended magazines; cryo cannot take a range barrel.

## Visibility and safety

First-person physical hardware matches mounted state, with bare scope bases for pulse/sniper.
The four reticles keep the center clear, use subdued fixed brightness and peripheral marks.
Scoped view hides the vanilla crosshair and objective guide, and moves ammunition to the edge.
Equipment, purchase and gacha sessions have distinct modes, server nonces and location checks.
Inventory/funds failures roll back. Replayed successes do not charge or award again.
Both server and participants require the new build because new items are registered.

## Checks

AttachmentIntegration: all variants, exact odds, inventory swaps, compatibility, ammo preservation,
reload restrictions, full-inventory rollback, shop/gacha, replay and cash rollback.
AttachmentQA: four optics, four mounted-gun views, equipment, gacha and shop screens, ten results.
check_attachment_assets.py: packaged hardware models, texture keys, item links and replacement bases.
