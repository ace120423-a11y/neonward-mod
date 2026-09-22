package jp.neonward;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
public final class PhoneTravelIntegration implements net.fabricmc.api.ModInitializer {
 boolean done;
 static void check(boolean ok,String why){if(!ok)throw new AssertionError("TRAVEL: "+why);}
 public void onInitialize(){net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(s->{if(done||s.getTickCount()<40)return;done=true;var p=WestLandIntegration.visitor(s.overworld());try{run(p);System.out.println("TRAVEL_TEST_COMPLETE");}catch(Throwable t){System.out.println("TRAVEL_TEST_FAILED");t.printStackTrace();}finally{p.level().removePlayerImmediately(p,net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);}});}
 static void run(ServerPlayer p){
  var l=p.level();var server=l.getServer();
  ((net.fabricmc.fabric.api.attachment.v1.AttachmentTarget)p).setAttached(PhoneEquipment.SLOT,new ItemStack(NeonWard.PHONE));
  for(int i=0;i<PhoneTravel.POINTS.size();i++){
   var point=PhoneTravel.POINTS.get(i);var at=PhoneTravel.landing(l,p,point);
   System.out.println("TRAVEL_DEST "+i+" "+point.name()+" -> "+at);
   check(at!=null,"landing "+point.name());check(PhoneTravel.safeBody(l,p,at)&&!PhoneTravel.entrance(at),"safe outside "+point.name());
   p.teleportTo(l,181.5,65,154.5,Set.of(),0,0,true);PhoneTravel.NEXT.clear();
   check(PhoneTravel.travel(p,i)==1,"actual travel "+point.name());check(p.position().distanceTo(at)<.01,"actual arrival "+point.name());
  }
  // Every separate shop entrance uses the production entrance detector, in the very same tick as travel.
  int[] destinations={3,4,2,6,5};
  for(int id=0;id<CompactShops.ALL.length;id++){
   p.teleportTo(l,181.5,65,154.5,Set.of(),0,0,true);PhoneTravel.NEXT.clear();
   CompactShops.cooldown.put(p.getUUID(),server.getTickCount()+100);NeonZones.cooldown.put(p.getUUID(),(long)server.getTickCount()+100);PrivateHomes.cooldown.put(p.getUUID(),server.getTickCount()+100);
   check(PhoneTravel.travel(p,destinations[id])==1,"travel to shop "+id);
   check(!CompactShops.tryEnterFromStreet(p),"arrival does not auto-enter "+id);
   check(!NeonZones.cooldown.containsKey(p.getUUID())&&!PrivateHomes.cooldown.containsKey(p.getUUID()),"no stale entrance locks");
   var shop=CompactShops.ALL[id];p.setPos(shop.x()+1,65,shop.z()+.1);
   check(CompactShops.tryEnterFromStreet(p),"immediate entrance "+id);
   check(p.level().dimension()==CompactShops.DIM&&CompactShops.room(p.level(),p.blockPosition())==id,"correct room "+id);
   check(CompactShops.cooldown.get(p.getUUID())>server.getTickCount(),"normal exit bounce protection preserved");
   PhoneTravel.NEXT.clear();check(PhoneTravel.travel(p,0)==1,"travel out of shop "+id);
  }
  for(int f=2;f<=20;f++){
   p.setPos(80.5,CityApartments.base(f),240.5);PhoneTravel.NEXT.clear();
   check(PhoneTravel.source(p)&&PhoneTravel.travel(p,0)==1,"travel from apartment floor "+f);
  }
  var home=server.getLevel(PrivateHomes.DIMENSION);check(home!=null,"home dimension");
  p.teleportTo(home,1032.5,65,10.5,Set.of(),0,0,true);PhoneTravel.NEXT.clear();check(PhoneTravel.source(p)&&PhoneTravel.travel(p,0)==1,"private home departure");
  p.setPos(800,120,400);check(!PhoneTravel.source(p),"no dungeon/outside shortcut");p.setPos(181.5,65,154.5);
  // Synthetic obstacles live far outside the copied city; never modify the live save.
  var test=new PhoneTravel.Point("test",4000,4000);
  for(int x=3987;x<=4013;x++)for(int z=3987;z<=4013;z++){l.getChunk(x>>4,z>>4);for(int y=62;y<=73;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);}
  check(PhoneTravel.landing(l,p,test)==null,"void rejected");
  var base=new BlockPos(4000,64,4000);l.setBlock(base,Blocks.SMOOTH_STONE_SLAB.defaultBlockState(),2);
  var slab=PhoneTravel.landing(l,p,test);check(slab!=null&&slab.y==64.5,"half-slab landing");
  l.setBlock(base,Blocks.MAGMA_BLOCK.defaultBlockState(),2);check(PhoneTravel.landing(l,p,test)==null,"hazard floor rejected");
  l.setBlock(base,Blocks.STONE.defaultBlockState(),2);l.setBlock(base.above(),Blocks.WATER.defaultBlockState(),2);check(PhoneTravel.landing(l,p,test)==null,"water rejected");
  l.setBlock(base.above(),Blocks.AIR.defaultBlockState(),2);l.setBlock(base.above(4),Blocks.STONE.defaultBlockState(),2);
  check(PhoneTravel.landing(l,p,test)!=null,"awning allowed");
  for(int x=3999;x<=4001;x++)for(int z=3999;z<=4001;z++)for(int y=65;y<=69;y++)if(x!=4000||z!=4000)l.setBlock(new BlockPos(x,y,z),Blocks.STONE.defaultBlockState(),2);
  var sealed=PhoneTravel.landing(l,p,test);check(sealed==null,"sealed room rejected / no rooftop arrival: "+sealed);
  l.setBlock(new BlockPos(4010,64,4000),Blocks.STONE.defaultBlockState(),2);
  check(PhoneTravel.landing(l,p,test)!=null,"blocked center uses nearby safe street");
  System.out.println("TRAVEL_PASS: 17 real destinations, 5 immediate entrances, 19 apartment floors, private home, slab/awning/hazard/blocked/void");
 }
}
