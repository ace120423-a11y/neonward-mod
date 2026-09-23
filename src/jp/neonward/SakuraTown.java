package jp.neonward;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;

/** One-time, preflighted construction. No periodic rebuilding, entity decorations or chunk tickets. */
public final class SakuraTown {
 public static final int MIN_X=-216,MAX_X=-64,MIN_Z=296,MAX_Z=464,GATE_Z=368;
 static final int BUDGET=1200;
 static final Set<String> NATURAL=Set.of("air","cave_air","void_air","grass_block","dirt","coarse_dirt","rooted_dirt","stone","sand","sandstone","gravel","water","short_grass","tall_grass","fern","large_fern","leaf_litter","bush","firefly_bush","sugar_cane","vine","oak_log","oak_leaves","birch_log","birch_leaves");
 static final Set<String> OLD_ROAD=Set.of("black_concrete","gray_concrete","light_gray_concrete","cyan_concrete","polished_deepslate","iron_bars","sea_lantern");
 static final Set<String> GATE_WALL=Set.of("polished_blackstone","deepslate_tiles","black_concrete","cyan_stained_glass","magenta_stained_glass","sea_lantern","pearlescent_froglight","iron_bars","iron_block","waxed_cut_copper");
 static MinecraftServer owner;static List<Map.Entry<BlockPos,BlockState>> entries;static Path file;
 static String phase="idle",fingerprint="";static int cursor;static boolean initialized;
 public static boolean contains(Level level,BlockPos p){return level.dimension()==Level.OVERWORLD&&((p.getX()>=MIN_X&&p.getX()<=MAX_X&&p.getZ()>=MIN_Z&&p.getZ()<=MAX_Z)||(p.getX()>MAX_X&&p.getX()<=4&&Math.abs(p.getZ()-GATE_Z)<=7)||(p.getX()>=5&&p.getX()<=7&&Math.abs(p.getZ()-GATE_Z)<=2));}
 public static boolean ready(MinecraftServer server){return owner==server&&phase.equals("complete");}
 public static String status(){return phase+" "+cursor+"/"+(entries==null?0:entries.size());}
 public static void init(){if(initialized)return;initialized=true;ServerLifecycleEvents.SERVER_STARTED.register(SakuraTown::start);ServerTickEvents.END_SERVER_TICK.register(SakuraTown::tick);ServerLifecycleEvents.SERVER_STOPPED.register(s->{owner=null;entries=null;phase="idle";});}
 static LinkedHashMap<BlockPos,BlockState> blueprint(){
  var plan=new LinkedHashMap<BlockPos,BlockState>();
  // Surveyed site has only low natural terrain here. Explicit air also preflights every walking lane.
  for(int x=MIN_X;x<=MAX_X;x++)for(int z=MIN_Z;z<=MAX_Z;z++){
   plan.put(new BlockPos(x,63,z),Blocks.DIRT.defaultBlockState());plan.put(new BlockPos(x,64,z),Blocks.GRASS_BLOCK.defaultBlockState());
   for(int y=65;y<=70;y++)plan.put(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
  }
  plan.putAll(SakuraTownPlan.build());
  // Human-scale passage: five-wide core, four blocks clear height; separate from the parcel gate.
  for(int x=-63;x<=4;x++)for(int z=GATE_Z-4;z<=GATE_Z+4;z++){
   boolean side=Math.abs(z-GATE_Z)>=3;
   plan.put(new BlockPos(x,63,z),Blocks.STONE_BRICKS.defaultBlockState());
   plan.put(new BlockPos(x,64,z),SakuraMaterials.get("sakura_stone_paving").defaultBlockState());
   for(int y=65;y<=70;y++){
    BlockState state=Blocks.AIR.defaultBlockState();
    if(side||y>=69)state=SakuraMaterials.get((x+64)%8==0?"sakura_dark_timber":"sakura_plaster").defaultBlockState();
    if(y==67&&Math.abs(z-GATE_Z)==2&&(x+64)%8==0)state=SakuraMaterials.get("sakura_paper_lantern").defaultBlockState();
    plan.put(new BlockPos(x,y,z),state);
   }
  }
  for(int x:new int[]{-63,-22,-11,4})for(int z=GATE_Z-5;z<=GATE_Z+5;z++)for(int y=65;y<=73;y++){
   if(Math.abs(z-GATE_Z)==5||y>=71)plan.put(new BlockPos(x,y,z),SakuraMaterials.get(y==73?"sakura_kawara_tile":"sakura_dark_timber").defaultBlockState());
  }
  // Surveyed three-cell gap between the new gate and the existing city sidewalk at x8.
  for(int x=5;x<=7;x++)for(int z=GATE_Z-2;z<=GATE_Z+2;z++){
   plan.put(new BlockPos(x,63,z),Blocks.STONE_BRICKS.defaultBlockState());
   plan.put(new BlockPos(x,64,z),SakuraMaterials.get("sakura_stone_paving").defaultBlockState());
   for(int y=65;y<=67;y++)plan.put(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
  }
  return plan;
 }
 static void start(MinecraftServer server){owner=server;cursor=0;phase="preflight";file=server.getWorldPath(LevelResource.ROOT).resolve("neonward/sakura_town.json");
  try{
   var plan=blueprint();if(plan.isEmpty()||plan.size()>650000)throw new IllegalStateException("Blueprint size");
   var digest=java.security.MessageDigest.getInstance("SHA-256");
   entries=plan.entrySet().stream().sorted(Comparator.comparingInt((Map.Entry<BlockPos,BlockState> e)->e.getValue().getFluidState().isEmpty()?0:1).thenComparingInt(e->e.getKey().getX()).thenComparingInt(e->e.getKey().getZ()).thenComparingInt(e->e.getKey().getY())).toList();
   for(var e:entries){var p=e.getKey();if(!contains(server.overworld(),p)||p.getY()<56||p.getY()>115||LandLayout.area(p.getX(),p.getZ()))throw new IllegalStateException("Out-of-scope blueprint "+p);digest.update((p.toShortString()+e.getValue()+"\n").getBytes(StandardCharsets.UTF_8));}
   fingerprint=HexFormat.of().formatHex(digest.digest());
   if(Files.exists(file)){var saved=JsonParser.parseString(Files.readString(file)).getAsJsonObject();if(saved.get("version").getAsInt()!=1||!saved.get("blueprint").getAsString().equals(fingerprint))throw new IllegalStateException("Unexpected saved construction plan");String old=saved.get("phase").getAsString();if(old.equals("complete")){phase="complete";entries=null;return;}if(!Set.of("building","preflight").contains(old))throw new IllegalStateException("Invalid construction state");}
   System.out.println("SAKURA_PLAN: "+entries.size()+" cells; checking all before any placement");
  }catch(Exception ex){fail(ex);}
 }
 static boolean allowed(ServerLevel level,BlockPos pos,BlockState desired){
  var old=level.getBlockState(pos);if(old.equals(desired))return true;if(level.getBlockEntity(pos)!=null)return false;
  var key=BuiltInRegistries.BLOCK.getKey(old.getBlock());
  if(key.getNamespace().equals("neonward"))return key.getPath().equals("wild_medicinal_herb");
  if(!key.getNamespace().equals("minecraft"))return false;
  return NATURAL.contains(key.getPath())||((pos.getX()<=-32||pos.getY()<=64)&&OLD_ROAD.contains(key.getPath()))||(pos.getX()>=-22&&pos.getX()<=-11&&Math.abs(pos.getZ()-GATE_Z)<=7&&GATE_WALL.contains(key.getPath()));
 }
 static void tick(MinecraftServer server){if(owner!=server||entries==null||!(phase.equals("preflight")||phase.equals("building")))return;
  var level=server.overworld();long stop=System.nanoTime()+8_000_000L;int work=0;
  try{
   while(cursor<entries.size()&&work++<BUDGET&&System.nanoTime()<stop){var e=entries.get(cursor);var pos=e.getKey();level.getChunkAt(pos);
    if(!allowed(level,pos,e.getValue()))throw new IllegalStateException("Preserved unexpected existing block at "+pos.toShortString()+": "+level.getBlockState(pos));
    if(phase.equals("building")&&!level.getBlockState(pos).equals(e.getValue())&&!level.setBlock(pos,e.getValue(),2))throw new IllegalStateException("Placement rejected at "+pos);
    cursor++;
   }
   if(cursor==entries.size()){
    if(phase.equals("preflight")){save("building");phase="building";cursor=0;System.out.println("SAKURA_BUILD: preflight passed; bounded construction started");}
    else{level.getChunkSource().save(true);save("complete");phase="complete";System.out.println("SAKURA_READY: "+cursor+" cells; Edo town, shrine and separate western tunnel ready");entries=null;}
   }
  }catch(Exception ex){fail(ex);}
 }
 static void save(String state)throws Exception{Files.createDirectories(file.getParent());var value=new JsonObject();value.addProperty("version",1);value.addProperty("blueprint",fingerprint);value.addProperty("phase",state);var temp=Files.createTempFile(file.getParent(),"sakura-",".tmp");try{Files.writeString(temp,value.toString(),StandardCharsets.UTF_8);try(var channel=java.nio.channels.FileChannel.open(temp,StandardOpenOption.WRITE)){channel.force(true);}Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}finally{Files.deleteIfExists(temp);}}
 static void fail(Exception ex){phase="blocked";entries=null;System.err.println("SAKURA_BLOCKED: construction stopped without replacing unexpected blocks: "+ex);}
}
