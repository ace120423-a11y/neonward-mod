package jp.neonward;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
public class ChainPullIntegration {
 static void check(boolean v,String text){if(!v)throw new AssertionError("CHAIN: "+text);}
 public static void run(ServerPlayer p){
  var l=p.level();var old=p.position();var item=p.getMainHandItem();var e=NeonHostiles.TYPES.get("neon_runner").create(l,EntitySpawnReason.COMMAND);
  try{
   p.setPos(3050,65,8);p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(NeonArsenal.ITEMS.get("chain_kusarigama")));
   e.setNoAi(true);e.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);e.getAttribute(Attributes.ARMOR).setBaseValue(0);e.setHealth(100);e.setPos(3050,65,12);l.addFreshEntity(e);
   for(int y=65;y<69;y++)l.setBlock(new BlockPos(3050,y,10),Blocks.AIR.defaultBlockState(),3);
   long now=l.getGameTime();var end=e.position().add(0,1,0);
   ChainPull.ACTIVE.put(p.getUUID(),new ChainPull.Cast(p,e,7,end,now));ChainPull.tick();check(e.getHealth()==100,"no damage before chain arrives");
   ChainPull.ACTIVE.put(p.getUUID(),new ChainPull.Cast(p,e,7,end,now-5));ChainPull.tick();check(e.getHealth()==93&&e.getDeltaMovement().z<0,"one hit and pull on arrival");
   ChainPull.tick();check(e.getHealth()==93,"no repeated damage");
   ChainPull.ACTIVE.put(p.getUUID(),new ChainPull.Cast(p,e,7,end,now-18));ChainPull.tick();check(ChainPull.ACTIVE.isEmpty(),"expires");
   e.invulnerableTime=0;e.setHealth(100);e.setDeltaMovement(Vec3.ZERO);
   for(int y=65;y<69;y++)l.setBlock(new BlockPos(3050,y,10),Blocks.STONE.defaultBlockState(),3);
   ChainPull.ACTIVE.put(p.getUUID(),new ChainPull.Cast(p,e,7,end,now-5));ChainPull.tick();check(e.getHealth()==100&&e.getDeltaMovement().lengthSqr()==0,"wall blocks arrival damage and pull");
   p.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);ChainPull.tick();check(ChainPull.ACTIVE.isEmpty(),"weapon switch cancels");
   p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(NeonArsenal.ITEMS.get("chain_kusarigama")));
   ChainPull.ACTIVE.put(p.getUUID(),new ChainPull.Cast(p,null,7,end,now-5));ChainPull.tick();check(e.getHealth()==100,"miss cannot damage");
   System.out.println("CHAIN_QA_PASS: flight delay, single hit, pull, wall, miss, expiry, switch cleanup");
  }finally{ChainPull.ACTIVE.clear();e.discard();p.setPos(old);p.setItemSlot(EquipmentSlot.MAINHAND,item);for(int y=65;y<69;y++)l.setBlock(new BlockPos(3050,y,10),Blocks.AIR.defaultBlockState(),3);}
 }
}
