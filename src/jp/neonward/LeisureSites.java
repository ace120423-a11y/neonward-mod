package jp.neonward;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Four fixed street kiosks; food uses existing eateries. Reservation slot zero is a v1 placeholder. */
public final class LeisureSites {
    public static final Block[] COUNTERS = new Block[5];
    private static final String[] IDS = {"food", "pets", "black", "range", "display"};
    private static final String[] NAMES = {"NEON KITCHEN", "PET LINK", "BLACK ALLEY", "BLACK STEEL", "SHOWCASE"};
    private static final int[][] REQUESTED = {{310,620}, {358,620}, {160,863}, {79,484}, {327,600}};
    private static final BlockPos[] ACTIVE = new BlockPos[5];
    private static final BlockPos FOOD_PLACEHOLDER = new BlockPos(310, 65, 620);
    private static boolean initialized;

    private LeisureSites() {}
    public static BlockPos at(int kind) { return kind > 0 && kind < ACTIVE.length ? ACTIVE[kind] : null; }
    public static PhoneTravel.Point destination(int kind) {
        if (kind == 0) return ExistingEateries.destination();
        var pos = at(kind);
        return pos == null ? null : new PhoneTravel.Point(NAMES[kind], pos.getX(), pos.getZ() - 1);
    }
    public static void init() {
        if (initialized) return;
        initialized = true;
        for (int i = 0; i < COUNTERS.length; i++) COUNTERS[i] = NeonZones.terminal("leisure_" + IDS[i] + "_counter");
        UseBlockCallback.EVENT.register((p, l, hand, hit) -> {
            var block = l.getBlockState(hit.getBlockPos()).getBlock();
            for (int i = 1; i < COUNTERS.length; i++) if (block == COUNTERS[i]) {
                if (p instanceof ServerPlayer sp && l.dimension() == Level.OVERWORLD
                    && hit.getBlockPos().equals(at(i))) LeisureShop.open(sp, i, false);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
        ServerLifecycleEvents.SERVER_STARTED.register(LeisureSites::start);
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> Arrays.fill(ACTIVE, null));
        // Wait for entity chunks to finish loading before checking deterministic UUIDs.
        ServerTickEvents.END_SERVER_TICK.register(s -> {
            if (s.getTickCount() % 100 != 0) return;
            var level = s.overworld();
            for (int i = 1; i < ACTIVE.length; i++) if (ACTIVE[i] != null && level.isPositionEntityTicking(ACTIVE[i])) ensureStaff(level, i);
        });
    }

    static void start(MinecraftServer server) {
        Arrays.fill(ACTIVE, null);
        var level = server.overworld();
        Path file = server.getWorldPath(LevelResource.ROOT).resolve("neonward/leisure_sites.json");
        try {
            BlockPos[] anchors;
            if (Files.exists(file)) anchors = decode(Files.readString(file, StandardCharsets.UTF_8));
            else {
                anchors = new BlockPos[5];
                anchors[0] = FOOD_PLACEHOLDER;
                for (int i = 1; i < anchors.length; i++) {
                    anchors[i] = search(level, i, anchors);
                    if (anchors[i] == null) throw new IllegalStateException("No safe 3x3 kiosk footprint for " + IDS[i]);
                }
                // Reserve all exact coordinates durably before the first world mutation.
                saveNew(file, anchors);
            }
            for (int i = 1; i < anchors.length; i++) {
                var pos = anchors[i];
                loadFootprint(level, pos);
                if (!compatible(level, pos, i)) {
                    System.err.println("Leisure site obstructed; unchanged and disabled: " + IDS[i] + " " + pos);
                    continue;
                }
                boolean complete = true;
                for (var entry : layout(pos, i).entrySet()) {
                    var existing = level.getBlockState(entry.getKey());
                    if (existing.equals(entry.getValue())) continue;
                    // Check again immediately before each write. Never replace an existing block.
                    if (!existing.isAir() || !level.setBlock(entry.getKey(), entry.getValue(), 3)) { complete = false; break; }
                }
                if (complete && compatible(level, pos, i)) ACTIVE[i] = pos;
            }
        } catch (Exception ex) {
            Arrays.fill(ACTIVE, null);
            System.err.println("Leisure sites disabled (no relocation): " + ex);
        }
    }

    static BlockPos search(ServerLevel level, int kind, BlockPos[] reserved) {
        if (kind == 0) return null;
        int x = REQUESTED[kind][0], z = REQUESTED[kind][1];
        for (int radius = 0; radius <= 18; radius++) for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
            if (Math.max(Math.abs(dx), Math.abs(dz)) != radius || dx * dx + dz * dz > 18 * 18) continue;
            var pos = new BlockPos(x + dx, 65, z + dz);
            if (!CityProtection.contains(level, pos) || CityProtection.southQuarter(pos) != (kind == 2)) continue;
            if (Arrays.stream(reserved).skip(1).filter(Objects::nonNull).anyMatch(p -> overlaps(p, pos))) continue;
            loadFootprint(level, pos);
            // First placement must be on the street, never inside a house with a generic solid floor.
            // The north arrival cell remains unobstructed beneath the kiosk's later canopy.
            if (!PhoneTravel.openSky(level, Vec3.atBottomCenterOf(pos.north()))) continue;
            if (!floor(level, pos)) continue;
            boolean air = true;
            for (var cell : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 3, 1)))
                if (!level.getBlockState(cell).isAir() || !level.getFluidState(cell).isEmpty()) { air = false; break; }
            if (air && level.getEntities(null, new AABB(pos.getX()-1, 65, pos.getZ()-1, pos.getX()+2, 69, pos.getZ()+2)).isEmpty()) return pos;
        }
        return null;
    }
    private static void loadFootprint(ServerLevel level, BlockPos pos) {
        for (int x : new int[]{-1, 1}) for (int z : new int[]{-1, 1}) level.getChunkAt(pos.offset(x, 0, z));
    }
    static boolean overlaps(BlockPos a, BlockPos b) { return Math.abs(a.getX() - b.getX()) <= 2 && Math.abs(a.getZ() - b.getZ()) <= 2; }
    private static boolean floor(ServerLevel level, BlockPos pos) {
        for (var floor : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, -1, 1))) {
            var state = level.getBlockState(floor);
            if (!level.getWorldBorder().isWithinBounds(floor) || !state.isFaceSturdy(level, floor, Direction.UP)
                || !level.getFluidState(floor).isEmpty() || PhoneTravel.hazard(state.getBlock())) return false;
        }
        return true;
    }
    private static boolean compatible(ServerLevel level, BlockPos pos, int kind) {
        if (!floor(level, pos)) return false;
        var plan = layout(pos, kind);
        for (var cell : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 3, 1))) {
            var existing = level.getBlockState(cell);
            if (!level.getFluidState(cell).isEmpty() || !existing.isAir() && !existing.equals(plan.get(cell))) return false;
        }
        return true;
    }
    static Map<BlockPos, BlockState> layout(BlockPos pos, int kind) {
        if (kind == 0) return Map.of();
        var result = new LinkedHashMap<BlockPos, BlockState>();
        var color = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(
            new String[]{"orange_concrete", "lime_concrete", "purple_concrete", "red_concrete", "cyan_concrete"}[kind]));
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++)
            result.put(pos.offset(x, 3, z), (z == -1 && x != 0 ? Blocks.SEA_LANTERN : color).defaultBlockState());
        for (int x : new int[]{-1, 1}) for (int y = 0; y < 3; y++)
            result.put(pos.offset(x, y, 1), Blocks.POLISHED_DEEPSLATE.defaultBlockState());
        result.put(pos, COUNTERS[kind].defaultBlockState());
        return result;
    }
    static UUID staffId(int kind) { return UUID.nameUUIDFromBytes(("neonward/leisure/site/" + IDS[kind]).getBytes(StandardCharsets.UTF_8)); }
    private static void ensureStaff(ServerLevel level, int kind) {
        if (kind == 0) return;
        var counter = ACTIVE[kind];
        if (!level.getBlockState(counter).is(COUNTERS[kind])) return;
        var home = counter.south();
        if (!level.isPositionEntityTicking(home) || !level.getBlockState(home).isAir() || !level.getBlockState(home.above()).isAir()) return;
        var uuid = staffId(kind);
        var existing = level.getEntity(uuid);
        if (existing != null && !(existing instanceof CityResident)) return;
        CityResident npc = (CityResident) existing;
        if (npc == null) {
            if (CityResidents.TYPES.size() < 6) return;
            npc = new CityResident(CityResidents.TYPES.get(kind == 2 ? 5 : 2), level);
            npc.setUUID(uuid);
            configure(npc, home, kind);
            if (!level.noCollision(npc) || !level.addFreshEntity(npc)) return;
        } else configure(npc, home, kind);
        // Only this service's marked duplicates are removed; unrelated residents are untouched.
        for (var duplicate : level.getEntitiesOfClass(CityResident.class, new AABB(counter).inflate(8)))
            if (duplicate != npc && duplicate.job.equals("leisure_" + kind)) duplicate.discard();
    }
    private static void configure(CityResident npc, BlockPos home, int kind) {
        npc.home = home;
        npc.job = "leisure_" + kind;
        npc.shopStaff = true;
        npc.setNoAi(true);
        npc.setNoGravity(true);
        npc.setPersistenceRequired();
        npc.setInvulnerable(true);
        npc.setDeltaMovement(Vec3.ZERO);
        npc.setPos(home.getX() + .5, home.getY(), home.getZ() + .5);
        npc.setYRot(180);
        npc.setYHeadRot(180);
        npc.yBodyRot = 180;
        npc.setCustomName(Component.literal(NAMES[kind]));
        npc.setCustomNameVisible(true);
    }

    static String encode(BlockPos[] anchors) {
        var root = new JsonObject(); root.addProperty("version", 1);
        var sites = new JsonArray();
        for (int i = 0; i < 5; i++) {
            var site = new JsonObject(); site.addProperty("id", IDS[i]);
            var anchor = i == 0 ? FOOD_PLACEHOLDER : anchors[i];
            site.addProperty("x", anchor.getX()); site.addProperty("y", 65); site.addProperty("z", anchor.getZ()); sites.add(site);
        }
        root.add("sites", sites);
        return new GsonBuilder().setPrettyPrinting().create().toJson(root);
    }
    static BlockPos[] decode(String json) {
        var root = JsonParser.parseString(json).getAsJsonObject();
        if (integer(root, "version") != 1) throw new IllegalArgumentException("site version");
        var sites = root.getAsJsonArray("sites");
        if (sites == null || sites.size() != 5) throw new IllegalArgumentException("five fixed sites required");
        var result = new BlockPos[5];
        for (int i = 0; i < 5; i++) {
            var site = sites.get(i).getAsJsonObject();
            if (!IDS[i].equals(site.get("id").getAsString())) throw new IllegalArgumentException("site identity/order");
            // Old food coordinates are inert. Do not inspect terrain, relocate, or remove any blocks.
            if (i == 0) { result[i] = FOOD_PLACEHOLDER; continue; }
            int x = integer(site, "x"), y = integer(site, "y"), z = integer(site, "z");
            long dx = (long)x - REQUESTED[i][0], dz = (long)z - REQUESTED[i][1];
            if (y != 65 || Math.abs(dx) > 18 || Math.abs(dz) > 18 || dx * dx + dz * dz > 324) throw new IllegalArgumentException("site outside reservation area");
            result[i] = new BlockPos(x, y, z);
            for (int j = 1; j < i; j++) if (overlaps(result[j], result[i])) throw new IllegalArgumentException("overlapping sites");
        }
        return result;
    }
    private static int integer(JsonObject object, String key) { return object.get(key).getAsBigDecimal().intValueExact(); }
    static void saveNew(Path file, BlockPos[] anchors) throws java.io.IOException {
        // Validate before touching disk; malformed/partial prior records are never overwritten.
        String json = encode(anchors); decode(json);
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), "leisure-sites-", ".tmp");
        try {
            Files.writeString(temporary, json, StandardCharsets.UTF_8);
            try (var channel = java.nio.channels.FileChannel.open(temporary, StandardOpenOption.WRITE)) { channel.force(true); }
            if (Files.exists(file)) throw new java.io.IOException("Site reservation already exists");
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE);
        } finally { Files.deleteIfExists(temporary); }
    }
}
