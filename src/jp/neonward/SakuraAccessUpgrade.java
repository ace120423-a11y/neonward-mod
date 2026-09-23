package jp.neonward;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;

/** Independent migration: never change the released town blueprint or regenerate its buildings. */
public final class SakuraAccessUpgrade {
 static MinecraftServer owner;
 static String phase="waiting", fingerprint;
 static Path file;
 static int cursor;
 static List<Map.Entry<BlockPos,BlockState>> entries;
 static Map<BlockPos,BlockState> baseline, roads, clearance;
 public static void init(){
  ServerLifecycleEvents.SERVER_STARTED.register(s->{owner=s;phase="waiting";entries=null;});
  ServerTickEvents.END_SERVER_TICK.register(SakuraAccessUpgrade::tick);
  ServerLifecycleEvents.SERVER_STOPPED.register(s->{owner=null;release();});
 }
 public static boolean ready(MinecraftServer s){return owner==s&&phase.equals("complete");}
 public static String status(){return phase+" "+cursor;}
 static void start(MinecraftServer s){
  owner=s;cursor=0;phase="preflight";
  file=s.getWorldPath(LevelResource.ROOT).resolve("neonward/sakura_access_v1.json");
  try{
   baseline=SakuraTown.blueprint();roads=SakuraCityRoadPlan.build();clearance=new LinkedHashMap<>(SakuraCityRoadPlan.expected());
   roads.keySet().forEach(clearance::remove);
   var plan=new LinkedHashMap<BlockPos,BlockState>(SakuraBoundaryPlan.build());
   for(var e:roads.entrySet())if(plan.putIfAbsent(e.getKey(),e.getValue())!=null)throw new IllegalStateException("Overlapping plans");
   for(var e:clearance.entrySet())if(plan.putIfAbsent(e.getKey(),e.getValue())!=null)throw new IllegalStateException("Overlapping clearance");
   if(plan.isEmpty()||plan.size()>50000)throw new IllegalStateException("Unexpected plan size");
   entries=plan.entrySet().stream().sorted(Comparator.comparingLong(e->e.getKey().asLong())).toList();
   var digest=java.security.MessageDigest.getInstance("SHA-256");
   for(var e:entries){
    var p=e.getKey();
    if(p.getY()<63||p.getY()>80||LandLayout.area(p.getX(),p.getZ())||(!roads.containsKey(p)&&!clearance.containsKey(p)&&!SakuraTown.contains(s.overworld(),p)))throw new IllegalStateException("Outside upgrade scope: "+p);
    digest.update((p.toShortString()+e.getValue()+"\n").getBytes(StandardCharsets.UTF_8));
   }
   fingerprint=HexFormat.of().formatHex(digest.digest());
   if(Files.exists(file)){
    var saved=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
    if(saved.get("version").getAsInt()!=1||!saved.get("blueprint").getAsString().equals(fingerprint))throw new IllegalStateException("Unknown upgrade marker");
    String old=saved.get("phase").getAsString();
    if(old.equals("complete")){phase="complete";release();return;}
    if(!Set.of("building","preflight").contains(old))throw new IllegalStateException("Invalid upgrade state");
   }
   System.out.println("SAKURA_ACCESS_PREFLIGHT "+entries.size());
  }catch(Exception e){fail(e);}
 }
 static boolean allowed(ServerLevel l,BlockPos p,BlockState desired){
  if(l.getBlockEntity(p)!=null)return false;
  var old=l.getBlockState(p);
  if(clearance.containsKey(p))return old.equals(clearance.get(p));
  if(old.equals(desired))return true;
  if(roads.containsKey(p))return SakuraCityRoadPlan.allowed(l,p,desired);
  var expected=baseline.get(p);
  return expected==null?old.isAir():old.equals(expected);
 }
 static void tick(MinecraftServer s){
  if(owner!=s)return;
  if(phase.equals("waiting")){if(SakuraTown.ready(s))start(s);else return;}
  if(entries==null)return;
  try{
   var l=s.overworld();long stop=System.nanoTime()+4_000_000L;int work=0;
   while(cursor<entries.size()&&work++<600&&System.nanoTime()<stop){
    var e=entries.get(cursor);var p=e.getKey();l.getChunkAt(p);
    if(!allowed(l,p,e.getValue()))throw new IllegalStateException("Preserved unexpected block at "+p.toShortString()+": "+l.getBlockState(p));
    if(phase.equals("building")&&!clearance.containsKey(p)&&!l.getBlockState(p).equals(e.getValue())&&!l.setBlock(p,e.getValue(),2))throw new IllegalStateException("Placement failed: "+p);
    cursor++;
   }
   if(cursor==entries.size()){
    if(phase.equals("preflight")){save("building");phase="building";cursor=0;}
    else {l.getChunkSource().save(true);save("complete");phase="complete";release();System.out.println("SAKURA_ACCESS_READY roads and perimeter complete");}
   }
  }catch(Exception e){fail(e);}
 }
 static void save(String state)throws Exception{
  Files.createDirectories(file.getParent());var json=new JsonObject();json.addProperty("version",1);json.addProperty("blueprint",fingerprint);json.addProperty("phase",state);
  var temp=Files.createTempFile(file.getParent(),"sakura-access-",".tmp");
  try{Files.writeString(temp,json.toString(),StandardCharsets.UTF_8);try(var c=java.nio.channels.FileChannel.open(temp,StandardOpenOption.WRITE)){c.force(true);}Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}finally{Files.deleteIfExists(temp);}
 }
 static void release(){entries=null;baseline=null;roads=null;clearance=null;}
 static void fail(Exception e){phase="blocked";release();System.err.println("SAKURA_ACCESS_BLOCKED "+e);}
}
