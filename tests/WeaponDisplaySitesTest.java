package jp.neonward;

import java.nio.file.*;
import java.util.*;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;

/** Pure reservation tests. All files are isolated in build/display-check; no Minecraft world is opened. */
public final class WeaponDisplaySitesTest {
    static void check(boolean value, String reason) { if (!value) throw new AssertionError(reason); }
    static void rejects(String json) {
        try { LeisureSites.decode(json); } catch (RuntimeException expected) { return; }
        throw new AssertionError("Invalid reservation accepted");
    }
    public static void main(String[] args) throws Exception {
        var anchors = new BlockPos[]{new BlockPos(310,65,620),new BlockPos(358,65,620),new BlockPos(160,65,863),new BlockPos(79,65,484),new BlockPos(327,65,600)};
        String json = LeisureSites.encode(anchors);
        check(Arrays.equals(anchors, LeisureSites.decode(json)), "fixed anchor round trip");
        rejects("{}"); rejects("broken"); rejects(json.replace("\"version\": 1", "\"version\": 2"));
        var root = JsonParser.parseString(json).getAsJsonObject();
        root.getAsJsonArray("sites").get(0).getAsJsonObject().addProperty("x", 329);
        check(Arrays.equals(anchors, LeisureSites.decode(root.toString())), "legacy food coordinates become inert placeholder; other anchors unchanged");
        root.getAsJsonArray("sites").get(0).getAsJsonObject().addProperty("y", 65.5);
        check(Arrays.equals(anchors, LeisureSites.decode(root.toString())), "placeholder skips physical coordinate validation");
        root.getAsJsonArray("sites").get(1).getAsJsonObject().addProperty("x", 377);
        rejects(root.toString());
        root = JsonParser.parseString(json).getAsJsonObject();
        root.getAsJsonArray("sites").get(1).getAsJsonObject().addProperty("y", 65.5);
        rejects(root.toString());
        root = JsonParser.parseString(json).getAsJsonObject();
        root.getAsJsonArray("sites").remove(0); rejects(root.toString());
        check(LeisureSites.overlaps(anchors[0], anchors[0].offset(2,0,2)), "shared edge footprints rejected");
        check(!LeisureSites.overlaps(anchors[0], anchors[0].offset(3,0,0)), "disjoint footprints accepted");
        var ids = new HashSet<UUID>();
        for (int i=1;i<5;i++) { check(ids.add(LeisureSites.staffId(i)), "unique staff ID"); check(LeisureSites.staffId(i).equals(LeisureSites.staffId(i)), "stable staff ID"); }
        check(LeisureSites.at(-1)==null && LeisureSites.at(5)==null && LeisureSites.at(0)==null && LeisureSites.destination(1)==null, "food has no counter; uninitialized kiosks fail closed");
        check(LeisureSites.search(null,0,null)==null && LeisureSites.layout(null,0).isEmpty(), "food search and construction never access the world");
        check(Objects.equals(LeisureSites.destination(0), ExistingEateries.destination()), "food travel delegates to existing eateries");
        Path directory = Files.createTempDirectory(Path.of("."), "reservation-test-");
        Path file = directory.resolve("leisure_sites.json");
        LeisureSites.saveNew(file, anchors);
        String saved = Files.readString(file);
        check(Arrays.equals(anchors, LeisureSites.decode(saved)), "durable picked coordinates");
        anchors[1] = anchors[1].east();
        boolean rejected = false;
        try { LeisureSites.saveNew(file, anchors); } catch (java.io.IOException expected) { rejected = true; }
        check(rejected && Files.readString(file).equals(saved), "existing reservations are never overwritten or relocated");
        System.out.println("LEISURE_SITES_PASS: four fixed kiosks, inert v1 food placeholder, no food search/build, strict kiosk validation, deterministic IDs, atomic save, no overwrite");
    }
}
