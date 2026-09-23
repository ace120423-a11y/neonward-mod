package jp.neonward;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Pure, read-only-surveyed city-side gate approach. Never reads/writes a world or loads chunks.
 * Survey: production world/dimensions/minecraft/overworld/region/r.0.0.mca, 2026-09-24.
 * No straight extension through the buildings at x38+ on z368. Instead follows existing streets.
 * Installer MUST preflight expected() and reject block entities before ANY writes, then recheck
 * immediately before each placement. Only build() is a write set; clearance is validation-only.
 * Existing gate/land routes at z154 and every cell outside the explicit footprint are untouched.
 */
public final class SakuraCityRoadPlan {
 public static final int FLOOR_Y=64,MIN_X=8,MAX_X=160,MIN_Z=366,MAX_Z=388;
 public static final String SURVEY_REGION_SHA256="87274d84ef9dc425a96b3a25f196b8c716cb620e2fde01eb2f7fae07b8204f3d";
 public static final List<BlockPos> ROUTE=List.of(new BlockPos(8,65,368),new BlockPos(25,65,368),new BlockPos(25,65,385),new BlockPos(160,65,385));
 private static final int[][] CROSS_STREET_DASHES={{26,29},{38,41},{50,53},{62,65},{74,77},{86,89},{98,101},{110,113},{122,125},{131,134},{143,146}};
 private SakuraCityRoadPlan(){}
 public static boolean footprint(int x,int z){
  return x>=8&&x<=28&&z>=366&&z<=370
   ||x>=22&&x<=28&&z>=366&&z<=388
   ||x>=22&&x<=160&&z>=383&&z<=388;
 }
 /** Includes validation-only headroom; build() writes FLOOR_Y only. */
 public static boolean contains(BlockPos pos){return pos.getY()>=64&&pos.getY()<=68&&footprint(pos.getX(),pos.getZ());}
 /** Migration guard: deny block entities first, then require this exact target and surveyed state.
  * Completed cells are accepted for idempotent resume; no broad material whitelist or clearing.
  */
 public static boolean allowed(ServerLevel level,BlockPos pos,BlockState desired){
  if(level.getBlockEntity(pos)!=null)return false;
  if(level.dimension()!=Level.OVERWORLD||pos.getY()!=FLOOR_Y||!footprint(pos.getX(),pos.getZ()))return false;
  var target=Targets.BLOCKS.get(pos);if(target==null||!target.equals(desired))return false;
  for(int y=65;y<=68;y++){
   var above=new BlockPos(pos.getX(),y,pos.getZ());
   if(level.getBlockEntity(above)!=null||!level.getBlockState(above).equals(Blocks.AIR.defaultBlockState()))return false;
  }
  var old=level.getBlockState(pos);
  return old.equals(target)||old.equals(surveyedFloor(pos.getX(),pos.getZ()));
 }
 private static final class Targets {static final Map<BlockPos,BlockState> BLOCKS=build();}
 private static BlockState concrete(String color){
  var block=BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(color+"_concrete"));
  if(block==null||block==Blocks.AIR)throw new IllegalStateException("Missing road concrete: "+color);
  return block.defaultBlockState();
 }
 private static BlockState surveyedFloor(int x,int z){
  if(x<=20)return Blocks.POLISHED_ANDESITE.defaultBlockState();
  if(x==21)return Blocks.SMOOTH_STONE.defaultBlockState();
  if(x==25&&(z>=366&&z<=369||z>=378&&z<=381))return concrete("yellow");
  if(z==385)for(var dash:CROSS_STREET_DASHES)if(x>=dash[0]&&x<=dash[1])return concrete("yellow");
  return concrete("black");
 }
 /** Exact surveyed states including four blocks of clear headroom; missing cells must fail closed. */
 public static Map<BlockPos,BlockState> expected(){
  var result=new LinkedHashMap<BlockPos,BlockState>();
  for(int x=MIN_X;x<=MAX_X;x++)for(int z=MIN_Z;z<=MAX_Z;z++)if(footprint(x,z)){
   result.put(new BlockPos(x,FLOOR_Y,z),surveyedFloor(x,z));
   for(int y=65;y<=68;y++)result.put(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
  }
  return Collections.unmodifiableMap(result);
 }
 /** Surface-only plan: extends asphalt into the gate and marks the existing L-route toward Sakura.
  * Unchanged pavement is retained in this map so the complete route is checked by migration/tests.
  * No foundations, above-ground clearing, poles, furniture, building or container replacements.
  */
 public static Map<BlockPos,BlockState> build(){
  var result=new LinkedHashMap<BlockPos,BlockState>();
  for(int x=MIN_X;x<=MAX_X;x++)for(int z=MIN_Z;z<=MAX_Z;z++)if(footprint(x,z)){
   var state=surveyedFloor(x,z);
   if(x<=21)state=z==366||z==370?Blocks.POLISHED_ANDESITE.defaultBlockState():concrete("black");
   // Redraw both longer legs with continuous flat curb bands, leaving all junction mouths open.
   if((x==22||x==28)&&z>=371&&z<=381)state=Blocks.POLISHED_ANDESITE.defaultBlockState();
   if((z==383||z==388)&&x>=29&&x<=145)state=Blocks.POLISHED_ANDESITE.defaultBlockState();
   result.put(new BlockPos(x,FLOOR_Y,z),state);
  }
  // West-facing arrows on the existing cross street, then north at the turn and west into gate.
  for(int tip:new int[]{32,64,96,128}){
   mark(result,tip,385);for(int d=1;d<=2;d++){mark(result,tip+d,385-d);mark(result,tip+d,385+d);}
   for(int d=1;d<=6;d++)mark(result,tip+d,385);
  }
  mark(result,25,374);for(int d=1;d<=2;d++){mark(result,25-d,374+d);mark(result,25+d,374+d);}
  for(int d=1;d<=5;d++)mark(result,25,374+d);
  mark(result,17,368);mark(result,18,367);mark(result,18,369);
  for(int x=18;x<=21;x++)mark(result,x,368);
  return Collections.unmodifiableMap(result);
 }
 private static void mark(Map<BlockPos,BlockState> plan,int x,int z){
  var p=new BlockPos(x,FLOOR_Y,z);if(!plan.containsKey(p))throw new IllegalStateException("Road marking outside surveyed footprint: "+p);
  plan.put(p,concrete("white"));
 }
 /** Pure preflight for supplied snapshots; allowApplied supports resuming this exact plan only.
  * The caller must capture block-entity presence independently, even when block states match.
  */
 public static List<String> validate(Map<BlockPos,BlockState> actual,Set<BlockPos> blockEntities,boolean allowApplied){
  Objects.requireNonNull(actual);Objects.requireNonNull(blockEntities);
  var failures=new ArrayList<String>();var targets=build();
  for(var e:expected().entrySet()){
   var p=e.getKey();var state=actual.get(p);
   if(blockEntities.contains(p)){failures.add("Protected block entity at "+p.toShortString());continue;}
   if(!e.getValue().equals(state)&&!(allowApplied&&targets.containsKey(p)&&targets.get(p).equals(state)))failures.add("Survey mismatch at "+p.toShortString()+": "+state+"; expected "+e.getValue());
  }
  return List.copyOf(failures);
 }
}
