package jp.neonward;
import java.util.*;
import com.google.gson.*;
public final class MapPlaces {
 public record Place(String name,double x,double z){}
 public static final List<Place> SLUM=new ArrayList<>(List.of(new Place("トンネル・街への出口",160,795),new Place("地下バー LOWLIFE",146,833),new Place("地下・マフィア事務所",128,877),new Place("マイホーム HIDEOUT",221,949),new Place("横丁・中央路地",160,863)));
 static {try(var in=MapPlaces.class.getResourceAsStream("/data/neonward/map_bars.json")){for(var e:JsonParser.parseReader(new java.io.InputStreamReader(Objects.requireNonNull(in),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonArray()){var p=e.getAsJsonObject();SLUM.add(new Place(p.get("name").getAsString(),p.get("x").getAsDouble(),p.get("z").getAsDouble()));}}catch(Exception e){throw new IllegalStateException("Map places",e);}}
}
