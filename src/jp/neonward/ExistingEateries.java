package jp.neonward;
import java.util.*;
import java.nio.charset.StandardCharsets;
import com.google.gson.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
/** Reuses the original signed buildings and their resident UUIDs. Never constructs a new food stall. */
public final class ExistingEateries {
 public record Site(int id,String name,BlockPos staff,float yaw,int[] meals,PhoneTravel.Point arrival){
  public UUID uuid(){return UUID.nameUUIDFromBytes(("neonward/resident/bar_staff_"+id).getBytes(StandardCharsets.UTF_8));}
 }
 public static final List<Site> ALL=load();
 static final Map<UUID,Site> BY_NPC=new HashMap<>();
 static {for(var site:ALL)BY_NPC.put(site.uuid(),site);}
 private static JsonArray json(String name){try(var in=ExistingEateries.class.getResourceAsStream("/data/neonward/"+name)){return JsonParser.parseReader(new java.io.InputStreamReader(Objects.requireNonNull(in),StandardCharsets.UTF_8)).getAsJsonArray();}catch(Exception e){throw new IllegalStateException("Existing restaurant catalog: "+name,e);}}
 static List<Site> load(){var result=new ArrayList<Site>();var signs=json("map_bars.json");
  for(var value:json("residents.json")){var r=value.getAsJsonObject();String key=r.get("key").getAsString();if(!key.matches("bar_staff_[0-9]+"))continue;int id=Integer.parseInt(key.substring(10));String suffix=" ["+(id+1)+"]";JsonObject sign=null;
   for(var v:signs){var candidate=v.getAsJsonObject();if(candidate.get("name").getAsString().endsWith(suffix)){sign=candidate;break;}}
   if(sign==null)throw new IllegalStateException("Missing restaurant sign "+key);
   String name=sign.get("name").getAsString();var staff=new BlockPos(r.get("x").getAsInt(),r.get("y").getAsInt(),r.get("z").getAsInt());
   result.add(new Site(id,name,staff,r.get("yaw").getAsFloat(),menu(name),new PhoneTravel.Point(name,(int)Math.floor(sign.get("x").getAsDouble()),sign.get("z").getAsInt())));
  }if(result.isEmpty())throw new IllegalStateException("No existing restaurants");return List.copyOf(result);
 }
 static int[] menu(String name){
  if(name.startsWith("焼鳥"))return new int[]{0,12,13,15,9,3};
  if(name.startsWith("餃子"))return new int[]{4,12,15,9,3};
  if(name.startsWith("深夜食堂"))return new int[]{6,1,2,3};
  if(name.startsWith("小料理"))return new int[]{7,6,13,16,3};
  if(name.startsWith("もつ煮"))return new int[]{5,12,14,15,9,3};
  if(name.startsWith("屋台酒場"))return new int[]{8,0,12,13,14,9};
  if(name.startsWith("レコード酒場"))return new int[]{10,11,7,15,16,17,9};
  if(name.startsWith("立ち呑み"))return new int[]{12,13,14,15,7,0,9};
  if(name.startsWith("錆色酒場"))return new int[]{14,15,12,5,8,9};
  return new int[]{12,13,14,15,16,17,0,7,9};
 }
 public static Site site(int id){return ALL.stream().filter(s->s.id()==id).findFirst().orElse(null);}
 public static PhoneTravel.Point destination(){return ALL.stream().filter(s->s.name().startsWith("焼鳥")).findFirst().orElseThrow().arrival();}
 static boolean near(ServerPlayer p,int id){var s=site(id);if(s==null||p.level().dimension()!=Level.OVERWORLD||p.distanceToSqr(Vec3.atBottomCenterOf(s.staff()))>36)return false;
  return p.level().getEntity(s.uuid()) instanceof CityResident npc&&npc.isAlive()&&npc.shopStaff&&npc.home.equals(s.staff())&&npc.position().distanceToSqr(Vec3.atBottomCenterOf(s.staff()))<1;
 }
 static Site nearest(ServerPlayer p){return ALL.stream().filter(s->near(p,s.id())).min(Comparator.comparingDouble(s->p.distanceToSqr(Vec3.atBottomCenterOf(s.staff())))).orElse(null);}
 static boolean use(ServerPlayer p,CityResident npc){var s=BY_NPC.get(npc.getUUID());if(s==null)return false;if(near(p,s.id()))LeisureShop.openFood(p,s.id());return true;}
 static boolean freeze(CityResident npc){if(npc.level().isClientSide()||npc.level().dimension()!=Level.OVERWORLD)return false;var s=BY_NPC.get(npc.getUUID());if(s==null||!npc.shopStaff||!npc.home.equals(s.staff()))return false;
  npc.setNoAi(true);npc.setNoGravity(true);npc.getNavigation().stop();npc.setDeltaMovement(Vec3.ZERO);npc.setPos(Vec3.atBottomCenterOf(s.staff()));npc.setYRot(s.yaw());npc.setYHeadRot(s.yaw());npc.yBodyRot=s.yaw();
  String title=s.name()+" / 店主";if(!npc.getName().getString().equals(title))npc.setCustomName(Component.literal(title));return true;
 }
}
