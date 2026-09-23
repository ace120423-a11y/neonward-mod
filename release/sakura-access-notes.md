# Sakura access update v0.1.55

## Scope
City-side surface route: (160,65,385) west to (25,65,385), north to (25,65,368), west to the Sakura gate and existing tunnel. The survey found buildings on a straight eastward extension at z368, so the route follows existing streets. Floor-only modifications, flat curb bands and west/north arrows; headroom is validated, never cleared. Existing parcel gate at z154 is untouched.

Entire district perimeter x=-216..-64, z=296..464 is enclosed, except five-wide east entrance z366..370. West shrine raised surface is respected. Plaster/timber/stone/tiled coping uses existing native blocks; no new entities or forced chunk tickets.

## Migration
Original SakuraTown blueprint and saved fingerprint are unchanged. SakuraAccessUpgrade waits for it, then preflights 8,941 cells (4,849 placement targets plus 4,092 validation-only road-clearance cells). It uses a separate versioned fingerprint marker and bounded 600-cell/4ms tick batches. Block entities and unexpected states are rejected. A completed marker skips rebuilding, preserving later world edits.

## Verification
- Build: `python build.py`.
- Fresh terrain plus existing shrine/accessory regression: `build/sakura-integration-20260924-013240/smoke.log`, geometry=3304/services=263; both original town and access migration completed.
- v54 completed-world upgrade: `build/sakura-access-20260924-013436/smoke.log`; upgrade 7,672 checks, real process restart 1,032, floor obstruction 9,979, headroom obstruction 9,979.
- Exact target states, continuous perimeter (including raised west wall), sealed-gate exterior flood, real collision walking city-to-shrine, unchanged original marker, untouched town-cell checksum and parcel checksum verified.
- Obstruction scenarios verify zero writes before rejection and intact chest. Test-only diamond is restored after restart verification; no QA world is copied to production.
- Distribution embeds the exact tested jar and retains seven mods; ZIP integrity and index version checked.
- Native visual QA: `build/sakura-access-client-20260924-013608/sakura-access-screenshots.json`, six captures passed and reviewed (city approach/overview, entrance join, entire enclosure, raised shrine wall and corner). QA excludes WebDisplays and only saves a disposable copy; that copy is never deployed.

## Deployment
With Java/game/server processes stopped, backed up world/mods/config and old client jar to `C:/Users/ace12/Documents/NeonWardServer/backups/20260924-013805-169111`. Installed tested jar on server and local client, SHA256 `858FDAE12FA9A811502F49B682D728C21C720BCF9CDA9E1C87CDCFB4624F93BD`. Server left stopped; production generation is deferred until next startup. No test save copied back.
