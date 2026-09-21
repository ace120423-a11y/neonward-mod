# Snow castle boss model implementation

The approved sheets are the art direction. The game assets are original, articulated low-poly interpretations, not a pixel-exact reconstruction of the illustrations. The review images are offline geometry renders, not Minecraft screenshots.

- 30 separate meshes: `frost_01.json` through `frost_30.json`.
- Shared eight-material atlas: ice, snow/ivory, iron, aged gold, navy cloth, red cloth, emissive blue and translucent glass.
- Dedicated skeleton groups for legs, tails, wings, windmill sails, floating coffins/cradle/doll, chains and the final heart.
- Names/localizations and collision dimensions follow the new catalogue. Existing entity IDs and saved tower progress remain compatible.
- First-tower models, boss HP formula and damage formula are unchanged. Existing attack programs are retained; attacks have not been fully redesigned to match each new creature.
- Some very fine illustration detail (filigree, fabric embroidery, dense architectural tracery) is simplified for game geometry. Full in-game visual fidelity, transparency ordering, animation and performance need client verification.

Build assets: `python tools/build_frost_bosses.py`.
Validate all rigs: `python tools/check_frost_models.py`.
Review sheets: `model-review/sheet-1.png`, `sheet-2.png`, `sheet-3.png`.

Verified: generated mesh hierarchy/UV/numeric integrity and per-model mesh budget; Java build; isolated dedicated server boot and summoning all 30 entity types. The server test does not verify client rendering or actual combat.
