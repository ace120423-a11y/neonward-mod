package jp.neonward;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;

/** Startup-only, four-block accents for the original south-bars layout; never replaces existing blocks.
 * Geometry source: work/build_south_bars.py and work/build_bar_staff.py (counter one cell ahead of staff,
 * rear wall one cell behind, counters at y65, cans on even x, tool tray at the row's right end).
 * Parent hook: EateryDecor.init() once during common initialization, after the eatery catalog is available.
 */
public final class EateryDecor {
    private static final Set<String> WALLS = Set.of("patched_brick", "grimy_concrete", "peeling_plaster", "rusted_corrugated");
    private static boolean initialized;
    record Theme(String color, String appliance, String menu) {}
    record Plan(ExistingEateries.Site site, Direction facing, Map<BlockPos, BlockState> blocks, BlockPos sign) {}

    private EateryDecor() {}
    public static void init() {
        if (initialized) return;
        initialized = true;
        ServerLifecycleEvents.SERVER_STARTED.register(EateryDecor::start);
    }
    static Theme theme(String name) {
        if (name.startsWith("焼鳥")) return new Theme("red", "campfire", "炭火焼鳥 / 串焼き");
        if (name.startsWith("餃子")) return new Theme("orange", "iron_trapdoor", "鉄板餃子 / ビール");
        if (name.startsWith("深夜食堂")) return new Theme("blue", "smoker", "夜の定食 / カレー");
        if (name.startsWith("小料理")) return new Theme("cyan", "flower_pot", "季節の小鉢 / 定食");
        if (name.startsWith("もつ煮")) return new Theme("brown", "cauldron", "もつ煮 / おつまみ");
        if (name.startsWith("レコード酒場")) return new Theme("purple", "jukebox", "音楽 / 小鉢 / 一杯");
        if (name.startsWith("立ち呑み")) return new Theme("white", "brewing_stand", "一杯 / 小鉢 / 串焼き");
        if (name.startsWith("錆色酒場")) return new Theme("gray", "waxed_weathered_copper_grate", "煮込み / 路地の一杯");
        if (name.startsWith("屋台酒場")) return new Theme("green", "smoker", "屋台めし / 串焼き");
        return new Theme("black", "barrel", "酒 / 串焼き / 小鉢");
    }
    private static Block vanilla(String id) {
        var block = BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(id));
        if (block == null || block == Blocks.AIR) throw new IllegalStateException("Missing decor block: " + id);
        return block;
    }
    private static boolean named(BlockState state, String path) {
        var id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id.getNamespace().equals("neonward") && id.getPath().equals(path);
    }
    private static boolean clear(ServerLevel level, BlockPos pos) {
        return level.getWorldBorder().isWithinBounds(pos) && level.getBlockState(pos).isAir()
            && level.getFluidState(pos).isEmpty() && level.getBlockEntity(pos) == null;
    }

    /** Read-only dry-run hook. Null means uncertain/changed geometry or occupied targets: skip whole site. */
    static Plan plan(ServerLevel level, ExistingEateries.Site site) {
        if (site.staff().getY() != 65 || site.yaw() != 0 && site.yaw() != 180) return null;
        var facing = site.yaw() == 0 ? Direction.SOUTH : Direction.NORTH;
        var staff = site.staff();
        var counter = staff.relative(facing);
        var door = new BlockPos(staff.getX(), 65, site.arrival().z());
        int depth = (door.getZ() - staff.getZ()) * facing.getStepZ();
        if (depth < 5 || depth > 16 || site.arrival().x() != staff.getX()) return null;
        // These are the catalog's 50 verified existing buildings, not an exploratory terrain search.
        // Load their bounded inspection areas so startup decoration also reaches distant restaurants.
        for (var pos : List.of(staff.offset(-8,0,-2), staff.offset(8,0,2), door)) level.getChunkAt(pos);
        if (!named(level.getBlockState(counter), "neon_counter") || !clear(level, staff) || !clear(level, staff.above())) return null;
        if (!named(level.getBlockState(door), "automatic_door") || !named(level.getBlockState(door.above()), "automatic_door")) return null;
        if (!level.getBlockState(door).hasProperty(BlockStateProperties.HORIZONTAL_FACING)
            || level.getBlockState(door).getValue(BlockStateProperties.HORIZONTAL_FACING) != facing) return null;
        if (!level.getBlockState(door.above(2)).is(vanilla("black_concrete"))) return null;
        var floor = level.getBlockState(staff.below());
        if (!floor.is(Blocks.DARK_OAK_PLANKS) && !named(floor, "oily_floor")) return null;

        var tops = new ArrayList<BlockPos>();
        int left = counter.getX(), right = left;
        while (left > counter.getX()-8 && named(level.getBlockState(new BlockPos(left-1,65,counter.getZ())), "neon_counter")) left--;
        while (right < counter.getX()+8 && named(level.getBlockState(new BlockPos(right+1,65,counter.getZ())), "neon_counter")) right++;
        if (right-left < 3 || right-left > 12) return null;
        for (int x=left; x<=right; x++) {
            // Original even-x cans and the right-hand tool tray stay untouched, even if later removed.
            var pos = new BlockPos(x,66,counter.getZ());
            if ((x&1)==1 && x!=right && clear(level,pos)) tops.add(pos);
        }
        tops.sort(Comparator.comparingInt(p -> Math.abs(p.getX()-staff.getX())));
        var appliance = tops.stream().filter(p -> p.getX()!=staff.getX()).findFirst().orElse(null);
        if (appliance == null) return null;
        // Preserve the staff's face/interaction ray: neither counter accent may use its center column.
        var sign = tops.stream().filter(p -> p.getX()!=staff.getX() && !p.equals(appliance)).findFirst().orElse(null);
        if (sign == null) return null;
        var theme = theme(site.name());
        var placements = new LinkedHashMap<BlockPos,BlockState>();
        var device = vanilla(theme.appliance()).defaultBlockState();
        if (device.hasProperty(BlockStateProperties.LIT)) device = device.setValue(BlockStateProperties.LIT,false);
        if (device.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) device = device.setValue(BlockStateProperties.HORIZONTAL_FACING,facing);
        if (device.hasProperty(BlockStateProperties.HALF)) device = device.setValue(BlockStateProperties.HALF,Half.BOTTOM);
        placements.put(appliance,device);
        placements.put(sign,Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION,facing==Direction.SOUTH?0:8));
        for (int dx : new int[]{-1,1}) {
            var banner = staff.offset(dx,2,0);
            var backing = level.getBlockState(banner.relative(facing.getOpposite()));
            var id = BuiltInRegistries.BLOCK.getKey(backing.getBlock());
            if (!id.getNamespace().equals("neonward") || !WALLS.contains(id.getPath()) || !clear(level,banner.below())) return null;
            placements.put(banner,vanilla(theme.color()+"_wall_banner").defaultBlockState().setValue(WallBannerBlock.FACING,facing));
        }
        for (var entry : placements.entrySet()) {
            if (!clear(level,entry.getKey()) || !entry.getValue().canSurvive(level,entry.getKey())
                || !level.getEntities(null,new AABB(entry.getKey())).isEmpty()) return null;
        }
        return new Plan(site,facing,Collections.unmodifiableMap(placements),sign);
    }

    static void start(MinecraftServer server) {
        Path file = server.getWorldPath(LevelResource.ROOT).resolve("neonward/eatery_decor.json");
        try {
            JsonObject records = read(file);
            int installed=0, skipped=0;
            for (var site : ExistingEateries.ALL) {
                String key = Integer.toString(site.id());
                // Attempt markers are permanent: neither player edits nor crash-interrupted sites are regenerated.
                if (records.has(key)) continue;
                var plan = plan(server.overworld(),site);
                if (plan == null) { skipped++; System.err.println("Eatery decor skipped: changed/uncertain/occupied geometry at " + site.name()); continue; }
                var record = new JsonObject();
                record.addProperty("name",site.name()); record.addProperty("status","reserved");
                record.addProperty("staff",site.staff().toShortString());
                var positions = new JsonArray(); for (var p : plan.blocks().keySet()) positions.add(p.toShortString()); record.add("positions",positions);
                records.add(key,record);
                save(file,records); // Failure here stops before any block is placed.
                boolean success = apply(server.overworld(),plan);
                record.addProperty("status",success?"installed":"skipped");
                save(file,records);
                if (success) installed++; else skipped++;
            }
            System.out.println("EATERY_DECOR: installed="+installed+", skipped="+skipped+" (four blocks/site, no entities)");
        } catch (Exception failure) { System.err.println("Eatery decor stopped; existing blocks preserved: "+failure); }
    }

    private static boolean apply(ServerLevel level, Plan plan) {
        // Revalidate the complete plan immediately before mutation, on the single server thread.
        for (var entry : plan.blocks().entrySet()) if (!clear(level,entry.getKey()) || !entry.getValue().canSurvive(level,entry.getKey())) return false;
        var written = new ArrayList<BlockPos>();
        try {
            for (var entry : plan.blocks().entrySet()) {
                if (!clear(level,entry.getKey())) throw new IllegalStateException("Decor target changed");
                written.add(entry.getKey());
                if (!level.setBlock(entry.getKey(),entry.getValue(),3)) throw new IllegalStateException("Decor placement rejected");
            }
            if (!(level.getBlockEntity(plan.sign()) instanceof SignBlockEntity sign)) throw new IllegalStateException("Menu sign unavailable");
            String name = plan.site().name().replaceFirst(" \\[\\d+\\]$", "");
            var menu = theme(name).menu().split(" / ",2);
            var text = new SignText().setColor(DyeColor.YELLOW).setHasGlowingText(true)
                .setMessage(0,Component.literal(name)).setMessage(1,Component.literal(menu[0]))
                .setMessage(2,Component.literal(menu.length>1?menu[1]:""))
                .setMessage(3,Component.literal("店主に話しかける"));
            sign.setText(text,true); sign.setText(text,false); sign.setWaxed(true); sign.setChanged();
            level.sendBlockUpdated(plan.sign(),level.getBlockState(plan.sign()),level.getBlockState(plan.sign()),3);
            return true;
        } catch (Exception failure) {
            // Only roll back cells written by this synchronous attempt and still matching its exact state.
            Collections.reverse(written);
            for (var pos : written) if (level.getBlockState(pos).equals(plan.blocks().get(pos))) level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
            System.err.println("Eatery decor rolled back: "+plan.site().name()+": "+failure);
            return false;
        }
    }
    private static JsonObject read(Path file) throws java.io.IOException {
        if (!Files.exists(file)) return new JsonObject();
        var root = JsonParser.parseString(Files.readString(file,StandardCharsets.UTF_8)).getAsJsonObject();
        if (root.get("version").getAsBigDecimal().intValueExact()!=1) throw new IllegalArgumentException("Decor save version");
        var records = root.getAsJsonObject("sites");
        for (var entry : records.entrySet()) {
            if (!entry.getKey().matches("[0-9]+") || !Set.of("reserved","installed","skipped").contains(entry.getValue().getAsJsonObject().get("status").getAsString()))
                throw new IllegalArgumentException("Invalid decor reservation");
        }
        return records;
    }
    private static void save(Path file, JsonObject records) throws java.io.IOException {
        var root = new JsonObject(); root.addProperty("version",1); root.add("sites",records);
        Files.createDirectories(file.getParent());
        Path tmp = Files.createTempFile(file.getParent(),"eatery-decor-",".tmp");
        try {
            Files.writeString(tmp,new GsonBuilder().setPrettyPrinting().create().toJson(root),StandardCharsets.UTF_8);
            try (var channel=java.nio.channels.FileChannel.open(tmp,StandardOpenOption.WRITE)) { channel.force(true); }
            Files.move(tmp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(tmp); }
    }
}
