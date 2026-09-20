package jp.neonward;
import java.util.*;
import java.nio.file.*;
import com.google.gson.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;

public final class CityResidents {
 public record Kind(String id,String name,int role){}
 public static final Kind[] ALL={new Kind("city_executive","企業の会社員",0),new Kind("city_courier","街の配達員",1),new Kind("city_shopper","街の買い物客",2),new Kind("city_kimono","ネオン和服の住人",3),new Kind("slum_mechanic","横丁の整備工",4),new Kind("slum_bartender","横丁のバーテンダー",5),new Kind("slum_biker","横丁のバイカー",6),new Kind("slum_scavenger","横丁の回収屋",7),new Kind("mafia_suit","マフィアの幹部",8),new Kind("mafia_guard","マフィアの用心棒",9),new Kind("guild_receptionist","受付嬢 ミオ",10),new Kind("guild_cashier","換金担当 レン",11)};
 public static final Map<EntityType<?>,Kind>KINDS=new LinkedHashMap<>();public static final List<EntityType<CityResident>> TYPES=new ArrayList<>();
 static JsonArray plan;static Set<String> created;static Path file;static boolean failed;
 public static boolean walkable(Level l,BlockPos p,boolean slum){if(p.getY()!=65||!CityProtection.contains(l,p)||CityProtection.southQuarter(p)!=slum)return false;String floor=BuiltInRegistries.BLOCK.getKey(l.getBlockState(p.below()).getBlock()).getPath();return Set.of("black_concrete","polished_blackstone","polished_deepslate").contains(floor)&&l.getBlockState(p).isAir()&&l.getBlockState(p.above()).isAir()&&l.getFluidState(p).isEmpty();}
 public static String dialogue(int role,int i){if(role==10)return "依頼の受注と報酬のお受け取りはこちらです。";if(role==11)return "回収した素材を買い取ります。";if(role>=8)return new String[]{"ここでは騒ぎを起こすなよ。","奥のバーには、表に出ない話が集まる。","横丁は俺たちの顔が利く場所だ。"}[i];if(role>=4)return new String[]{"この横丁、古く見えてもいい店が多いんだ。","地下バーなら LOWLIFE の看板を探しな。","継ぎ足した配管も、まだまだ現役さ。"}[i];return new String[]{"依頼を探しているなら、企業ビルの受付へ。","南門の先の横丁、仕事帰りによく寄るよ。","東門の外の塔は危険だ。装備を整えてからな。"}[i];}
 public static void init(){
  for(var k:ALL){var key=ResourceKey.create(Registries.ENTITY_TYPE,NeonWard.id(k.id()));var type=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<CityResident>of(CityResident::new,MobCategory.MISC).sized(.6f,1.85f).clientTrackingRange(8).build(key));KINDS.put(type,k);TYPES.add(type);FabricDefaultAttributeRegistry.register(type,CityResident.createMobAttributes().add(Attributes.MAX_HEALTH,40).add(Attributes.MOVEMENT_SPEED,.24).add(Attributes.FOLLOW_RANGE,24));}
  try(var in=CityResidents.class.getResourceAsStream("/data/neonward/residents.json")){plan=JsonParser.parseReader(new java.io.InputStreamReader(Objects.requireNonNull(in),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonArray();}catch(Exception e){throw new IllegalStateException(e);}
  ServerLifecycleEvents.SERVER_STARTED.register(s->{file=s.getWorldPath(LevelResource.ROOT).resolve("neonward/residents.json");created=new HashSet<>();failed=false;try{if(Files.exists(file))for(var v:JsonParser.parseString(Files.readString(file)).getAsJsonArray())created.add(v.getAsString());}catch(Exception e){failed=true;System.err.println("Resident population save cannot be read: "+e);}});
  ServerLifecycleEvents.SERVER_STOPPED.register(s->{created=null;});
  ServerTickEvents.END_SERVER_TICK.register(s->{if(s.getTickCount()%100!=0||created==null||failed)return;var l=s.overworld();boolean changed=false;int spawned=0;
   for(var value:plan){var p=value.getAsJsonObject();String id=p.get("key").getAsString();boolean staff=p.has("shopStaff")&&p.get("shopStaff").getAsBoolean();if(created.contains(id)&&!staff)continue;var pos=new BlockPos(p.get("x").getAsInt(),p.has("y")?p.get("y").getAsInt():65,p.get("z").getAsInt());if(!l.isPositionEntityTicking(pos))continue;int role=p.get("role").getAsInt();if(l.getBlockState(pos.below()).isAir()||!l.getBlockState(pos).isAir()||!l.getBlockState(pos.above()).isAir())continue;
    var uuid=UUID.nameUUIDFromBytes(("neonward/resident/"+id).getBytes(java.nio.charset.StandardCharsets.UTF_8));if(l.getEntity(uuid)==null){var npc=new CityResident(TYPES.get(role),l);npc.setUUID(uuid);npc.home=pos;npc.job=p.has("job")?p.get("job").getAsString():"bar";npc.shopStaff=p.has("shopStaff")&&p.get("shopStaff").getAsBoolean();npc.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);npc.setYRot(p.has("yaw")?p.get("yaw").getAsFloat():role>=10?0:(role*53)%360);npc.setCustomName(Component.literal(p.get("name").getAsString()));if(!l.noCollision(npc)||!l.addFreshEntity(npc))continue;}
    if(created.add(id))changed=true;if(++spawned>=6&&!staff)break;
   }
   if(changed)try{l.getChunkSource().save(true);Files.createDirectories(file.getParent());var tmp=file.resolveSibling("residents.json.tmp");Files.writeString(tmp,new Gson().toJson(created));Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(Exception e){failed=true;System.err.println("Resident population persistence failed: "+e);}
  });
 }
}
