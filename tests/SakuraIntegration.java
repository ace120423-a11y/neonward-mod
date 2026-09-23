package jp.neonward;
import java.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;

/** Runs only in the runner's isolated terrain copy. */
public final class SakuraIntegration implements ModInitializer {
 // ShrineServicesIntegration.runWhenReady also awaits ShrineRitualIntegration's real-tick future.
 // Keep the visitor and COMPLETE marker inside that completion callback; never join on the server thread.
 static int checks;boolean done;String parcelBefore;
 static void check(boolean value,String why){checks++;if(!value)throw new AssertionError("SAKURA: "+why);}
 public void onInitialize(){ServerTickEvents.END_SERVER_TICK.register(server->{
  if(done)return;
  if(parcelBefore==null){
   try{check(SakuraTown.status().startsWith("preflight"),"parcel baseline captured before construction");parcelBefore=parcelHash(server.overworld());System.out.println("SAKURA_PARCEL_BEFORE "+parcelBefore);}
   catch(Throwable e){done=true;System.out.println("SAKURA_TEST_FAILED");e.printStackTrace();return;}
  }
  if(!SakuraTown.ready(server)&&!SakuraTown.status().startsWith("blocked")){if(server.getTickCount()%200==0)System.out.println("SAKURA_PROGRESS "+SakuraTown.status());return;}
  done=true;try{check(SakuraTown.ready(server),"builder not blocked: "+SakuraTown.status());String after=parcelHash(server.overworld());check(parcelBefore.equals(after),"old parcel flooring and old gate samples unchanged during construction");System.out.println("SAKURA_PARCEL_AFTER "+after);var p=WestLandIntegration.visitor(server.overworld());try{run(p);ShrineServicesIntegration.runWhenReady(p).whenComplete((count,failure)->{try{if(failure!=null){System.out.println("SAKURA_TEST_FAILED");failure.printStackTrace();}else System.out.println("SAKURA_TEST_COMPLETE geometry="+checks+" services="+count);}finally{p.level().removePlayerImmediately(p,net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);}});}catch(Throwable e){p.level().removePlayerImmediately(p,net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);throw e;}}catch(Throwable e){System.out.println("SAKURA_TEST_FAILED");e.printStackTrace();}
 });}
 static String parcelHash(ServerLevel level)throws Exception{
  var digest=java.security.MessageDigest.getInstance("SHA-256");var samples=new ArrayList<BlockPos>();
  // Structural flooring only: no ticking crops, doors, inventories or entities are sampled or changed.
  for(int id=0;id<LandLayout.COUNT;id++)for(int dx:new int[]{0,15,31})for(int dz:new int[]{0,15,31})for(int y=63;y<=64;y++)samples.add(new BlockPos(LandLayout.x(id)+dx,y,LandLayout.z(id)+dz));
  for(int x=-32;x<=4;x+=4)for(int y=63;y<=64;y++)samples.add(new BlockPos(x,y,154));
  for(var at:samples){check(!SakuraTown.contains(level,at),"old parcel sample outside new construction");level.getChunkAt(at);digest.update((at.toShortString()+"="+level.getBlockState(at)+"\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));}
  return HexFormat.of().formatHex(digest.digest());
 }
 static double standingHeight(ServerPlayer p,double x,double z,double previous,String route){
  var level=p.level();level.getChunkAt(BlockPos.containing(x,previous,z));var heights=new TreeSet<Double>(Comparator.reverseOrder());
  var footprint=new AABB(x-.299,previous-.61,z-.299,x+.299,previous+.601,z+.299);
  for(var shape:level.getBlockCollisions(p,footprint))for(var box:shape.toAabbs())if(box.maxY>=previous-.601&&box.maxY<=previous+.601&&box.maxX>x-.299&&box.minX<x+.299&&box.maxZ>z-.299&&box.minZ<z+.299)heights.add(box.maxY);
  for(double y:heights){var body=new AABB(x-.299,y+.00001,z-.299,x+.299,y+1.8,z+.299);
   if(!level.getBlockCollisions(p,body).iterator().hasNext()&&level.getFluidState(BlockPos.containing(x,y+.1,z)).isEmpty())return y;
  }
  throw new AssertionError("SAKURA: no supported 1.8m walking clearance within one step on "+route+" at "+x+","+previous+","+z);
 }
 static Vec3 walk(ServerPlayer p,Vec3 start,double endX,double endZ,String route){
  double distance=Math.hypot(endX-start.x,endZ-start.z),y=start.y;int steps=Math.max(1,(int)Math.ceil(distance*8));
  for(int i=0;i<=steps;i++){double t=(double)i/steps,x=start.x+(endX-start.x)*t,z=start.z+(endZ-start.z)*t;double next=standingHeight(p,x,z,y,route);check(Math.abs(next-y)<=.60001,"walkable stair rise/drop "+route);y=next;}
  var end=new Vec3(endX,y,endZ);System.out.println("SAKURA_WALK "+route+" -> "+end);return end;
 }
 static void pathChecks(ServerPlayer p){
  var entrance=SakuraTownPlan.ENTRANCE;var saisen=SakuraTownPlan.SAISEN;var basin=SakuraTownPlan.CHOZU;var counter=SakuraTownPlan.COUNTER;
  var road=walk(p,new Vec3(14.5,65,SakuraTown.GATE_Z+.5),entrance.getX()+.5,entrance.getZ()+.5,"city gate and lowered tunnel");
  var plaza=walk(p,road,SakuraTownPlan.PRECINCT_MAX_X-11.5,entrance.getZ()+.5,"street, outer steps and torii");check(Math.abs(plaza.y-68)<.001,"precinct reachable at standing y68");
  var offering=walk(p,plaza,saisen.getX()+2.5,saisen.getZ()+.5,"inner shrine steps to offering front");check(Math.abs(offering.y-saisen.getY())<.001,"offering front reaches raised floor without flight");
  var waterApproach=walk(p,plaza,basin.getX()+2.5,plaza.z,"plaza to basin aisle");var water=walk(p,waterApproach,waterApproach.x,basin.getZ()+.5,"basin front");check(Math.abs(water.y-basin.getY())<.001,"basin has supported standing approach");
  var officeApproach=walk(p,plaza,plaza.x,counter.getZ()+.5,"plaza to office aisle");var office=walk(p,officeApproach,counter.getX()+2.5,counter.getZ()+.5,"amulet counter front");check(Math.abs(office.y-counter.getY())<.001,"office has supported standing approach");
 }
 static void run(ServerPlayer p)throws Exception{
  checks+=AccessoryEquipmentIntegration.run(p);
  var l=p.level();
  pathChecks(p);
  for(int x=SakuraTownPlan.ENTRANCE.getX();x<=4;x++)for(int dz=-2;dz<=2;dz++){int z=SakuraTown.GATE_Z+dz;var at=new Vec3(x+.5,65,z+.5);check(PhoneTravel.safeBody(l,p,at),"new tunnel standing headroom "+x+","+z);check(l.getBlockState(new BlockPos(x,64,z)).isFaceSturdy(l,new BlockPos(x,64,z),net.minecraft.core.Direction.UP),"tunnel support "+x+","+z);}
  for(var pos:List.of(SakuraTownPlan.ENTRANCE,SakuraTownPlan.SAISEN,SakuraTownPlan.CHOZU,SakuraTownPlan.COUNTER)){
   check(SakuraTown.contains(l,pos)&&CityProtection.contains(l,pos)&&!NeonZones.isField(l,pos),"safe district "+pos);
   check(!LandLayout.area(pos.getX(),pos.getZ()),"separate from purchased land");
  }
  check(l.getBlockState(SakuraTownPlan.SAISEN).is(SakuraMaterials.get("sakura_saisen_box")),"offering box installed");
  check(l.getBlockState(SakuraTownPlan.CHOZU).is(SakuraMaterials.get("sakura_chozu_basin")),"handwashing basin installed");
  check(l.getBlockState(SakuraTownPlan.COUNTER).is(SakuraMaterials.get("sakura_omamori_counter")),"amulet counter installed");
  var probe=new BlockPos(-100,110,350);var old=l.getBlockState(probe);check(old.isAir()&&l.getBlockEntity(probe)==null,"container fixture cell must be vacant; never delete an existing chest");
  try{l.setBlock(probe,Blocks.CHEST.defaultBlockState(),3);check(!SakuraTown.allowed(l,probe,Blocks.AIR.defaultBlockState()),"existing container never overwritten");}
  finally{if(l.getBlockState(probe).is(Blocks.CHEST)&&l.getBlockEntity(probe) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest&&chest.isEmpty())l.setBlock(probe,old,3);}
  var center=SakuraTownPlan.ENTRANCE.below();var before=l.getBlockState(center);check(l.getBlockEntity(center)==null,"restart fixture never replaces a block entity");l.setBlock(center,Blocks.DIAMOND_BLOCK.defaultBlockState(),3);
  try{SakuraTown.start(l.getServer());SakuraTown.tick(l.getServer());check(SakuraTown.ready(l.getServer())&&l.getBlockState(center).is(Blocks.DIAMOND_BLOCK),"restart preserves completed world edits");}finally{if(l.getBlockState(center).is(Blocks.DIAMOND_BLOCK))l.setBlock(center,before,3);}
  check(!SakuraTown.contains(l,new BlockPos(-100,65,154)),"old west gate excluded");
  System.out.println("SAKURA_GEOMETRY_PASS separate gate, supported tunnel, shrine anchors, safe zone, no parcel overlap, container preservation, restart idempotence");
 }
}
