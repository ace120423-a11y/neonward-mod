package jp.neonward;
import java.util.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
public final class GunPresentationIntegration {
 static void check(boolean b,String why){if(!b)throw new AssertionError("GUN_FX: "+why);}
 static void run(net.minecraft.server.level.ServerPlayer p){var inv=Cyberware.inventory(p);var pos=p.position();float yaw=p.getYRot(),pitch=p.getXRot();
  try{var l=p.level();p.setPos(3050.5,70,2);p.setYRot(0);p.setXRot(0);
   for(int x=3049;x<=3052;x++)for(int z=1;z<=13;z++)for(int y=70;y<=73;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),3);
   var cryo=new ItemStack(NeonArsenal.ITEMS.get("cryo_projector"));p.setItemSlot(EquipmentSlot.MAINHAND,cryo);p.stopUsingItem();var gun=(NeonArsenal.Rifle)cryo.getItem();
   check(gun.fire(l,p,InteractionHand.MAIN_HAND)==InteractionResult.FAIL,"cryo requires held aim");p.startUsingItem(InteractionHand.MAIN_HAND);check(gun.fire(l,p,InteractionHand.MAIN_HAND)==InteractionResult.SUCCESS,"cryo fires while aiming");p.stopUsingItem();for(int i=0;i<7;i++)p.getCooldowns().tick();check(gun.fire(l,p,InteractionHand.MAIN_HAND)==InteractionResult.FAIL,"stop aim stops firing");
   var bow=new ItemStack(NeonArsenal.ITEMS.get("tactical_crossbow"));p.setItemSlot(EquipmentSlot.MAINHAND,bow);check(((NeonArsenal.Rifle)bow.getItem()).fire(l,p,InteractionHand.MAIN_HAND)==InteractionResult.SUCCESS,"crossbow fire");
   check(VisibleBolts.FLYING.size()==1,"visible arrow created");var s=ArsenalExpansion.SHOTS.getLast();var b=VisibleBolts.FLYING.get(s);
   check(b!=null&&b.isNoPhysics()&&!b.shouldBeSaved(),"cosmetic arrow cannot collide or persist");
   var step=new ArsenalExpansion.Shot(p,l,bow,s.position().add(s.velocity()),s.velocity(),34,s.damage(),s.mode());VisibleBolts.advance(s,step);check(b.position().equals(step.position()),"arrow follows authoritative flight");
   VisibleBolts.hit(step,step.position(),null);check(VisibleBolts.FLYING.isEmpty()&&b.remaining==30,"wall sticks with expiry");for(int i=0;i<30;i++)b.tick();check(b.isRemoved(),"stuck arrow removed");
   for(int i=0;i<11;i++)check(GunVfx.profile(new ItemStack(NeonArsenal.ITEMS.get(GunVfx.IDS[i])))==i,"unique gun profile");
   check(!new GunVfx.Visual(0,99,0,0,Vec3.ZERO,Vec3.ZERO).valid(),"invalid profile rejected");check(!new GunVfx.Visual(0,1,0,0,Vec3.ZERO,new Vec3(Double.NaN,0,0)).valid(),"nonfinite rejected");
   System.out.println("GUN_PRESENTATION_PASS: profiles, cryo held use, single-authority arrow flight, pickup disabled, sticking, expiry, malformed events");
  }finally{p.stopUsingItem();Cyberware.restore(p,inv);p.setPos(pos);p.setYRot(yaw);p.setXRot(pitch);VisibleBolts.clear();ArsenalExpansion.SHOTS.clear();}
 }
}
