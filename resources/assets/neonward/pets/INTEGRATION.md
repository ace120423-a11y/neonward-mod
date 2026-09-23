Pet subsystem integration (hooks implemented)
================================================

Added to MarketLedger.Account:

    public Set<Integer> petOwned = new HashSet<>();

This field MUST be in the market account: StockMarket.save atomically writes cash
and ownership in one JSON replacement. Missing/null legacy fields are supported.
Ledger validation rejects nonnull petOwned entries outside 0..4.

NeonWard now calls PetCompanions.init() after StockMarket.init().
NeonClient now calls PetCompanions.initClient() after common init.
No language entries or mixins are required; shop labels use NAMES.

Public shop API (server thread only)
-----------------------------------
Kinds: 0 dog (1800 Cr), 1 cat (1800), 2 snake (2400), 3 crow (2800), 4 robot (4500).
NAMES, price(kind), owned(player, kind), purchase(player, kind), summon(player, kind),
recall(player), and activeKind(player) (-1 when none).

purchase/summon results:
  1 OK; 2 already owned (purchase only; no second charge)
 -1 invalid kind; -2 unavailable/dead/spectator/off-thread; -3 insufficient cash
 -4 save failure (cash and ownership rolled back); -5 not owned; -6 no safe space.
Invalid price queries return 0; the purchase API rejects invalid ids.
The shop must enforce its own counter distance/session authorization before purchase.
Summoning is free; the player must own the selected kind. Show ownership separately
from active state. One active companion is shared across all pet kinds per player.

Companions are intentionally ephemeral: recall on logout, death, spectator mode,
dimension change, chunk unload, or server shutdown. Owned pets can be summoned again.
Dimension change does not automatically summon in the destination. Existing pet stays
if replacement placement fails. Safe recovery searches loaded nearby ground without
editing blocks or acquiring chunk tickets. No attack/breeding/loot goals, block
interaction, portal travel, farm trampling, or pushing other entities. Crow makes short
bounded flights with a checked arc, swept collision checks and verified ground landing.
Ground companions choose a safe nearby idle path every 3–8 seconds, within four blocks
of their owner, and retain it until arrival or an obstacle/owner movement invalidates it.

Assets are generated with tools/generate_pet_assets.py. Dog/cat use custom articulated
native meshes; snake/crow/robot preserve the prototype silhouettes using native model
parts, not display entities. Walking/slither/robot gait stops when movement stops.

Verification: python tools/check_pets.py compiles to build/pet-check and runs rollback,
legacy persistence, duplicate purchase, bounds, funds, and ownership isolation tests.
It compiles the actual shared MarketLedger with its petOwned field.
Live multiplayer/lifecycle and visual checks still require an isolated game session.
