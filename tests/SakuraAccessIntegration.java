package jp.neonward;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.*;

/** Test-only entry point; the runner supplies disposable copies of the completed v54 world. */
public final class SakuraAccessIntegration implements ModInitializer {
 static int checks;boolean done;String mode,parcel,marker,untouched;
 Map<BlockPos,BlockState> plan,before;BlockPos probe;BlockState probeOld;
 static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError("SAKURA_ACCESS: "+why);}
 static Path root(MinecraftServer s){return s.getWorldPath(LevelResource.ROOT);}
 static String townMarker(MinecraftServer s)throws Exception{return Files.readString(root(s).resolve("neonward/sakura_town.json"));}
 static String hash(ServerLevel l,Collection<BlockPos> cells)throws Exception{
  var digest=java.security.MessageDigest.getInstance("SHA-256");
  for(var p:cells.stream().sorted(Comparator.comparingLong(BlockPos::asLong)).toList()){
   l.getChunkAt(p);digest.update((p.toShortString()+"="+l.getBlockState(p)+"\n").getBytes(StandardCharsets.UTF_8));
  }return HexFormat.of().formatHex(digest.digest());
 }
 Collection<BlockPos> untouchedCells(){var cells=new HashSet<>(SakuraTown.blueprint().keySet());cells.removeAll(plan.keySet());return cells;}
 void setup(MinecraftServer s)throws Exception{
  mode=System.getProperty("sakura.access.mode","upgrade");
  check(Files.exists(root(s).resolve("ACCESS_QA_COPY")),"runner-owned copy required");
  if(!mode.equals("fresh")){marker=townMarker(s);check(marker.contains("complete"),"completed v54 town marker required");}
  else check(!Files.exists(root(s).resolve("neonward/sakura_town.json")),"fresh copy has no town marker");
  plan=new LinkedHashMap<>(SakuraBoundaryPlan.build());
  for(var e:SakuraCityRoadPlan.build().entrySet())check(plan.putIfAbsent(e.getKey(),e.getValue())==null,"plans disjoint");
  check(!plan.isEmpty(),"nonempty access plan");var l=s.overworld();
  parcel=SakuraIntegration.parcelHash(l);if(!mode.equals("fresh"))untouched=hash(l,untouchedCells());
  if(mode.equals("fresh"))return;
  if(mode.equals("restart"))return;
  check(!Files.exists(root(s).resolve("neonward/sakura_access_v1.json")),"fresh upgrade copy has no access marker");
  var validationCells=new HashSet<>(plan.keySet());validationCells.addAll(SakuraCityRoadPlan.expected().keySet());
  before=new LinkedHashMap<>();for(var p:validationCells){l.getChunkAt(p);before.put(p,l.getBlockState(p));}
  if(mode.startsWith("blocked")){
   // Last target in installer order exercises the entire preflight before the obstruction.
   probe=(mode.equals("blocked-headroom")?SakuraCityRoadPlan.expected().keySet().stream().filter(p->p.getY()==65):plan.keySet().stream()).max(Comparator.comparingLong(BlockPos::asLong)).orElseThrow();
   check(l.getBlockEntity(probe)==null,"never replace an existing container in fixture");probeOld=l.getBlockState(probe);
   check(l.setBlock(probe,Blocks.CHEST.defaultBlockState(),2),"install disposable obstruction");
   check(l.getBlockEntity(probe) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity,"real chest fixture");
  }
 }
 void preserve(MinecraftServer s)throws Exception{
  check(marker.equals(townMarker(s)),"original town marker byte-for-byte unchanged");
  check(parcel.equals(SakuraIntegration.parcelHash(s.overworld())),"old parcel checksum preserved");
  check(untouched.equals(hash(s.overworld(),untouchedCells())),"town outside upgrade footprint unchanged");
 }
 static void perimeter(ServerLevel l){
  // Independent of build(): every district edge cell is a solid two-block-high barrier,
  // except the single five-wide east entrance. Also flood-fill outside the rectangle:
  // with that entrance sealed, no player-width crossing into the district may exist.
  int minX=SakuraTown.MIN_X,maxX=SakuraTown.MAX_X,minZ=SakuraTown.MIN_Z,maxZ=SakuraTown.MAX_Z;
  for(int x=minX;x<=maxX;x++)for(int z=minZ;z<=maxZ;z++){
   if(x!=minX&&x!=maxX&&z!=minZ&&z!=maxZ)continue;
   boolean gate=x==maxX&&Math.abs(z-SakuraTown.GATE_Z)<=2;
   // Raised west precinct has its walking surface at 68, not the street's 65.
   int feet=x==minX&&z>=312&&z<=424?68:65;
   for(int y=feet;y<=feet+1;y++){
    var p=new BlockPos(x,y,z);l.getChunkAt(p);var shape=l.getBlockState(p).getCollisionShape(l,p);
    check(gate?shape.isEmpty():net.minecraft.world.level.block.Block.isShapeFullBlock(shape),(gate?"gate clear ":"continuous full-height perimeter ")+p);
   }
  }
  var seen=new HashSet<BlockPos>();var queue=new ArrayDeque<BlockPos>();queue.add(new BlockPos(minX-1,65,minZ-1));
  while(!queue.isEmpty()){
   var p=queue.removeFirst();if(!seen.add(p))continue;
   check(!(p.getX()>minX&&p.getX()<maxX&&p.getZ()>minZ&&p.getZ()<maxZ),"sealed-gate flood cannot enter district");
   for(var d:List.of(net.minecraft.core.Direction.NORTH,net.minecraft.core.Direction.SOUTH,net.minecraft.core.Direction.EAST,net.minecraft.core.Direction.WEST)){
    var n=p.relative(d);if(n.getX()<minX-1||n.getX()>maxX+1||n.getZ()<minZ-1||n.getZ()>maxZ+1||seen.contains(n))continue;
    if(n.getX()==maxX&&Math.abs(n.getZ()-SakuraTown.GATE_Z)<=2)continue;
    var body=new AABB(n.getX()+.201,65.00001,n.getZ()+.201,n.getX()+.799,66.8,n.getZ()+.799);
    if(!l.getBlockCollisions(null,body).iterator().hasNext())queue.add(n);
   }
  }
 }
 void finish(MinecraftServer s)throws Exception{
  var l=s.overworld();preserve(s);
  if(mode.startsWith("blocked")){
   check(SakuraAccessUpgrade.status().startsWith("blocked"),"unknown chest blocks migration");
   var ordered=before.keySet().stream().sorted(Comparator.comparingLong(BlockPos::asLong)).toList();
   check(SakuraAccessUpgrade.cursor==ordered.indexOf(probe),"preflight reaches our exact obstruction, not an unrelated mismatch");
   check(!Files.exists(root(s).resolve("neonward/sakura_access_v1.json")),"no building marker before complete preflight");
   for(var e:before.entrySet())check(l.getBlockState(e.getKey()).equals(e.getKey().equals(probe)?Blocks.CHEST.defaultBlockState():e.getValue()),"zero migration writes before rejection "+e.getKey());
   check(l.getBlockEntity(probe) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest&&chest.isEmpty(),"obstruction retained intact");
   restoreProbe(s);
  }else if(mode.equals("restart")){
   check(SakuraAccessUpgrade.ready(s),"restart marker accepted");
   var p=BlockPos.of(Long.parseLong(Files.readString(root(s).resolve("ACCESS_QA_EDIT"))));
   check(l.getBlockState(p).is(Blocks.DIAMOND_BLOCK),"actual process restart preserves player edit");
   check(l.setBlock(p,plan.get(p),2),"restore our edit fixture after restart assertion");
  }else{
   check(SakuraAccessUpgrade.ready(s),"migration completed: "+SakuraAccessUpgrade.status());
   for(var e:plan.entrySet())check(l.getBlockState(e.getKey()).equals(e.getValue()),"exact installed block "+e.getKey());
   perimeter(l);
   var p=WestLandIntegration.visitor(l);
   try{
    var end=SakuraIntegration.walk(p,new Vec3(160.5,65,385.5),25.5,385.5,"city avenue west");
    end=SakuraIntegration.walk(p,end,25.5,368.5,"city avenue north turn");
    SakuraIntegration.walk(p,end,14.5,368.5,"city avenue to gate");SakuraIntegration.pathChecks(p);
   }finally{l.removePlayerImmediately(p,net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);}
   var edit=SakuraCityRoadPlan.build().keySet().iterator().next();check(l.getBlockEntity(edit)==null,"edit fixture has no block entity");
   check(l.setBlock(edit,Blocks.DIAMOND_BLOCK.defaultBlockState(),2),"persist player edit for restart");Files.writeString(root(s).resolve("ACCESS_QA_EDIT"),Long.toString(edit.asLong()));
  }
  System.out.println("SAKURA_ACCESS_TEST_COMPLETE mode="+mode+" checks="+checks);
 }
 void restoreProbe(MinecraftServer s){
  if(probe!=null&&s.overworld().getBlockState(probe).is(Blocks.CHEST)&&s.overworld().getBlockEntity(probe) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest&&chest.isEmpty()){
   check(s.overworld().setBlock(probe,probeOld,2),"restore only test-owned empty chest");probe=null;
  }
 }
 public void onInitialize(){
  ServerLifecycleEvents.SERVER_STARTED.register(s->{try{setup(s);}catch(Throwable e){done=true;System.out.println("SAKURA_ACCESS_TEST_FAILED");e.printStackTrace();}});
  ServerTickEvents.START_SERVER_TICK.register(s->{if(done||!"fresh".equals(mode)||marker!=null||!SakuraTown.ready(s))return;try{
   check(SakuraAccessUpgrade.status().startsWith("waiting")||SakuraAccessUpgrade.status().startsWith("preflight"),"fresh town baseline before access writes");
   marker=townMarker(s);untouched=hash(s.overworld(),untouchedCells());
  }catch(Throwable e){done=true;System.out.println("SAKURA_ACCESS_TEST_FAILED");e.printStackTrace();}});
  ServerTickEvents.END_SERVER_TICK.register(s->{if(done)return;try{
   if(SakuraTown.status().startsWith("blocked"))throw new AssertionError("base town blocked: "+SakuraTown.status());
   if(s.getTickCount()>8000)throw new AssertionError("bounded migration timeout: "+SakuraAccessUpgrade.status());
   if(!SakuraAccessUpgrade.ready(s)&&!SakuraAccessUpgrade.status().startsWith("blocked"))return;
   done=true;finish(s);
  }catch(Throwable e){done=true;System.out.println("SAKURA_ACCESS_TEST_FAILED");e.printStackTrace();}finally{if(done)restoreProbe(s);}});
 }
}
