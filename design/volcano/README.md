# Volcanic Tower

Thirty separately composed articulated meshes. Shared code is limited to geometric primitives and the existing native Frost mesh renderer; Frost assets are not regenerated.

`python tools/build_volcano_models.py` regenerates the volcano meshes, deterministic material atlases, catalog and three review sheets. Coordinates are metres, Y up, facing -Z. Collision dimensions are body-sized in `VolcanoRoster`, excluding long weapons and decorative wing tips.

## Validation

Run `python build.py` to completion before starting tests. Test tools create isolated saves under `build/`; they never open the production world for writing.

- `python tests/run_phone_travel.py --volcano`: all 30 rooms, 900 kills, one boss per room, duplicate-load rejection, visible projectile release timing and cleanup, two server-side player fixtures, both checkpoints and restart isolation.
- `python tests/run_paired_client.py --volcano`: all 30 native entity renderers and three synchronized 23F throwing poses, with 33 screenshots and a client-entity presence assertion.
- `python tests/run_paired_client.py --volcano-tour`: copies the completed volcano test save, visits the front, enters floor 1 and returns to the guild, recording four screenshots.
- `python tests/run_land_integration.py`: existing land, paired weapons, chain pull, gacha, housing sales, gun presentation/reload and attachment regressions.
- `python tests/run_phone_travel.py`: existing 17 travel destinations, immediate entry into five shops, apartment and private-home departure, unsafe/blocked landing rejection.

The two-player progression test uses server-side fixtures, not two remote human clients. These checks do not measure real-world Internet latency or guarantee frame rate with every shader/mod combination.

## Lifecycle

Only requested rooms are built, with bounded per-tick block work. A room has at most six tracked wave enemies and one boss. Boss projectiles and fire patches have bounded counts/lifetimes and are cleared on death, lost targets or removal. Empty encounters are retired after ten seconds; clearing a floor saves a checkpoint, not partial kill counts. Runtime encounter tokens reject leftovers from a previous server session. Cached room completion is checked against landing blocks before transfer.

There is no real flowing lava or permanent fire placement in attack effects. All volcano world changes are confined to its new dimension, except the dedicated guild guide block/sign. Existing tower progression and player-owned terrain are untouched.
