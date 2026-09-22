package jp.neonward;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
public final class PairedEffectsIntegration {
 static void check(boolean b,String message){if(!b)throw new AssertionError("PAIRED_EFFECT: "+message);}
 public static void run(ServerPlayer p){
  var l=p.level();var before=p.position();List<Entity> spawned=new ArrayList<>();
  try{
   p.setPos(3050,65,8);
   var primary=NeonHostiles.TYPES.get("neon_runner").create(l,EntitySpawnReason.COMMAND);
   var nearby=NeonHostiles.TYPES.get("neon_runner").create(l,EntitySpawnReason.COMMAND);
   var hidden=NeonHostiles.TYPES.get("neon_runner").create(l,EntitySpawnReason.COMMAND);
   var cow=EntityTypes.COW.create(l,EntitySpawnReason.COMMAND);
   for(var e:List.of(primary,nearby,hidden,cow)){e.setNoAi(true);e.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);e.setHealth(100);spawned.add(e);}
   primary.setPos(3051,65,9);nearby.setPos(3052.5,65,9);hidden.setPos(3051,65,11);cow.setPos(3050,65,10);
   for(var e:spawned)l.addFreshEntity(e);
   var floor=new BlockPos(3051,64,9);l.setBlock(floor,Blocks.QUARTZ_BLOCK.defaultBlockState(),3);
   for(int y=65;y<68;y++)l.setBlock(new BlockPos(3051,y,10),Blocks.STONE.defaultBlockState(),3);
   primary.setDeltaMovement(Vec3.ZERO);float playerHealth=p.getHealth();
   PairedEffects.blast(p,primary,6,.9);
   check(primary.getDeltaMovement().lengthSqr()>0,"primary knocked back");
   check(primary.getHealth()==100,"blast does not double-hit primary");
   check(nearby.getHealth()<100&&nearby.getDeltaMovement().lengthSqr()>0,"nearby enemy damaged and knocked back");
   check(hidden.getHealth()==100,"wall blocks blast");
   check(cow.getHealth()==100&&p.getHealth()==playerHealth,"animals and player excluded");
   check(l.getBlockState(floor).is(Blocks.QUARTZ_BLOCK)&&l.getBlockState(new BlockPos(3051,65,10)).is(Blocks.STONE),"no terrain destruction");
   nearby.setHealth(100);nearby.invulnerableTime=0;ArsenalExpansion.MELEE_NEXT.clear();
   var glove=new ItemStack(NeonArsenal.ITEMS.get("impact_gauntlet"));
   ArsenalExpansion.impact(p,primary,glove,20);float health=nearby.getHealth();
   check(health<100,"normal melee dispatch invokes blast");
   ArsenalExpansion.impact(p,primary,glove,20);check(nearby.getHealth()==health,"normal explosion cooldown bounds spam");
   primary.setHealth(100);primary.invulnerableTime=0;nearby.setHealth(100);nearby.invulnerableTime=0;p.setYRot(0);p.setXRot(0);
   ArsenalExpansion.skill(p,glove,4,1.5f);
   check(primary.getHealth()<100&&nearby.getHealth()<100,"charged punch dispatch damages primary and splashes once");
   PairedEffects.BLEEDS.clear();ArsenalExpansion.MELEE_NEXT.clear();
   ArsenalExpansion.impact(p,primary,new ItemStack(NeonArsenal.ITEMS.get("neon_dualblades")),10);
   check(PairedEffects.BLEEDS.containsKey(primary),"dual melee dispatch applies bleed");
   var first=PairedEffects.BLEEDS.get(primary);PairedEffects.bleed(p,primary);
   check(PairedEffects.BLEEDS.size()==1&&PairedEffects.BLEEDS.get(primary).next()==first.next(),"refresh neither stacks nor postpones tick");
   PairedEffects.bleed(p,cow);check(!PairedEffects.BLEEDS.containsKey(cow),"bleed excludes animals");
   long now=l.getGameTime();primary.setHealth(100);primary.invulnerableTime=20;
   PairedEffects.BLEEDS.put(primary,new PairedEffects.Bleed(p,now+100,now));PairedEffects.tick();
   check(primary.getHealth()==98,"bleed deals 2 damage even during melee immunity window");
   check(PairedEffects.BLEEDS.get(primary).next()==now+20,"one second interval");
   PairedEffects.BLEEDS.put(primary,new PairedEffects.Bleed(p,now,now));primary.invulnerableTime=20;PairedEffects.tick();
   check(primary.getHealth()==96&&!PairedEffects.BLEEDS.containsKey(primary),"final tick damages once and removes effect");
   PairedEffects.BLEEDS.put(primary,new PairedEffects.Bleed(p,now-1,now));PairedEffects.tick();
   check(primary.getHealth()==96&&PairedEffects.BLEEDS.isEmpty(),"expired bleed never damages");
   PairedEffects.bleed(p,primary);p.setPos(500,65,8);PairedEffects.tick();check(PairedEffects.BLEEDS.isEmpty(),"leaving combat cleans up bleed");
   p.setPos(3050,65,8);MeleeElements.DOT.clear();MeleeElements.STUN.clear();MeleeElements.RESIST.clear();
   MeleeElements.applyElement(p,primary,MeleeElements.Kind.FIRE,now);
   check(MeleeElements.DOT.get(primary).end()==now+80,"fire still lasts 4 seconds");
   MeleeElements.applyElement(p,primary,MeleeElements.Kind.POISON,now);
   check(MeleeElements.DOT.get(primary).end()==now+100,"poison still lasts 5 seconds");
   MeleeElements.applyElement(p,primary,MeleeElements.Kind.THUNDER,now);
   check(MeleeElements.STUN.get(primary)==now+20,"normal stun still lasts 1 second");
   ElementVfx.BUDGET.clear();for(int i=0;i<100;i++)ElementVfx.emit(p,primary,ElementVfx.FIRE,10,false);
   check(ElementVfx.BUDGET.get(l)[1]==24,"visual network budget capped at 24 per level per tick");
   check(!new ElementVfx.Visual(9,0,0,0,0,0,12,false).valid(),"bad effect kind rejected");
   check(!new ElementVfx.Visual(1,0,Double.NaN,0,0,0,12,false).valid(),"nonfinite position rejected");
   check(!new ElementVfx.Visual(1,0,0,0,0,0,101,false).valid(),"unbounded duration rejected");
   MeleeElements.DOT.clear();MeleeElements.STUN.clear();MeleeElements.RESIST.clear();ElementVfx.BUDGET.clear();
   System.out.println("ELEMENT_VFX_SERVER_PASS: bounded cosmetic events, valid payloads, fire/poison/stun durations unchanged");
   System.out.println("PAIRED_EFFECTS_QA_PASS: explosion knockback/splash, wall and terrain safety, friendly exclusion, cooldown, dual bleed dispatch, refresh, 2 damage/second, immunity window, expiry and cleanup");
  }finally{p.setPos(before);for(var e:spawned)e.discard();PairedEffects.BLEEDS.clear();ArsenalExpansion.MELEE_NEXT.clear();ArsenalExpansion.ECHOES.clear();}
 }
}
