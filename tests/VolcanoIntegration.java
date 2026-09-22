package jp.neonward;
import java.util.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
public final class VolcanoIntegration implements net.fabricmc.api.ModInitializer {
 boolean done;int next=0;ServerPlayer p,q;
 static void check(boolean v,String why){if(!v)throw new AssertionError("VOLCANO: "+why);}
 public void onInitialize(){net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(s->{if(done||s.getTickCount()<40)return;try{
  var l=s.getLevel(VolcanicSpire.DIM);check(l!=null,"dimension registered");if(p==null){p=WestLandIntegration.visitor(l);q=WestLandIntegration.visitor(l);p.setPos(-128,65,15);q.setPos(-128,65,15);p.getAbilities().invulnerable=true;q.getAbilities().invulnerable=true;}
  if(next==0&&!VolcanicSpire.progress.front){VolcanicSpire.WAITING.put(p.getUUID(),0);VolcanicSpire.build(l);return;}
  if(next==0){check(l.getBlockState(VolcanicSpire.ENTRY_POS).is(VolcanicSpire.ENTRY),"front entrance");check(l.getBlockState(new BlockPos(-128,64,15)).isFaceSturdy(l,new BlockPos(-128,64,15),net.minecraft.core.Direction.UP),"safe front platform");var lobby=s.getLevel(CompactShops.DIM);CompactShops.ensure(lobby,2);TowerGuides.install(lobby);check(lobby.getBlockState(VolcanicSpire.GUIDE).is(VolcanicSpire.ENTRY),"guild guide");check(lobby.getBlockState(SkySpire.CITY_GUIDE).is(SkySpire.ENTRY)&&lobby.getBlockState(NeonZones.ENTRY_POS).is(NeonZones.ENTRY),"old guides unchanged");next=1;}
  if(!VolcanicSpire.progress.built.contains(next)){VolcanicSpire.WAITING.put(p.getUUID(),next);VolcanicSpire.build(l);return;}
  VolcanicSpire.WAITING.clear();exercise(l,next++);
  if(next>30){var old=VolcanicSpire.progress.checkpoint.get(p.getStringUUID());VolcanicSpire.NEXT.clear();VolcanicSpire.enter(p,false);check(VolcanicSpire.WAITING.get(p.getUUID())==old,"resume checkpoint");VolcanicSpire.NEXT.clear();VolcanicSpire.enter(p,true);check(VolcanicSpire.WAITING.get(p.getUUID())==1&&VolcanicSpire.progress.cleared.get(p.getStringUUID())==0,"restart only self");check(VolcanicSpire.progress.cleared.get(q.getStringUUID())==30,"friend progress preserved");check(VolcanicSpire.progress.built.size()==30,"all thirty floors built");VolcanicSpire.save();System.out.println("VOLCANO_TEST_COMPLETE 30 floors / 900 kills / 30 bosses / 2 players / motion timelines / gates / cleanup / restart");done=true;p.level().removePlayerImmediately(p,Entity.RemovalReason.DISCARDED);q.level().removePlayerImmediately(q,Entity.RemovalReason.DISCARDED);}
 }catch(Throwable t){done=true;System.out.println("VOLCANO_TEST_FAILED");t.printStackTrace();}});}
 void exercise(ServerLevel l,int f){
  VolcanicSpire.arrive(p,f);VolcanicSpire.arrive(q,f);var r=VolcanicSpire.RUNS.get(f);check(r==VolcanicSpire.run(l,f),"shared encounter");
  check(l.getBlockState(new BlockPos(VolcanoLayout.x(f)+32,65,56)).is(Blocks.OBSIDIAN),"closed gate");
  for(int n=0;n<35&&r.kills<30;n++){VolcanicSpire.encounter(l,f,r);check(r.mobs.size()<=6,"six mob cap");for(var mob:new ArrayList<>(r.mobs.values())){check(VolcanicSpire.floor(mob)==f,"correct mob floor");mob.hurtServer(l,l.damageSources().playerAttack(p),100000);check(r.kills<=30,"count bounded");}}
  check(r.kills==30&&r.mobs.isEmpty(),"exact 30 kills");check(l.getBlockState(new BlockPos(VolcanoLayout.x(f)+32,65,56)).isAir(),"door opened");
  p.setPos(VolcanoLayout.x(f)+32,65,70);q.setPos(VolcanoLayout.x(f)+37,65,70);VolcanicSpire.encounter(l,f,r);var b=r.boss;check(b!=null&&b.floor()==f,"correct boss type");VolcanicSpire.encounter(l,f,r);check(r.boss==b,"one boss only");check(NeonLoot.profile(b)==LootProfile.BOSS,"boss loot profile");
  b.startAttack(p.position());check(b.action()==VolcanoRoster.MODES[f-1]+1,"synced attack mode");
  var duplicate=new VolcanoBoss(VolcanoBosses.TYPES.get(f-1),l);duplicate.setPos(b.position());VolcanicSpire.tag(duplicate,f,r);l.addFreshEntity(duplicate);check(duplicate.isRemoved(),"duplicate same-run boss rejected on load");
  for(int t=0;t<31;t++)b.tick();check(b.shots.isEmpty(),"no projectile before release");b.tick();
  if(VolcanoRoster.MODES[f-1]==5||VolcanoRoster.MODES[f-1]==3){check(!b.shots.isEmpty(),"visible projectile on release");check(!b.shots.getFirst().visual().shouldBeSaved(),"no saved projectile debris");}
  for(int t=0;t<95;t++)b.tick();check(b.shots.size()<=8&&b.patches.size()<=8,"bounded attacks");
  b.hurtServer(l,l.damageSources().playerAttack(p),1000000);check(r.defeated,"boss defeat");check(b.shots.isEmpty()&&b.patches.isEmpty(),"death cleans attacks");
  check(VolcanicSpire.progress.checkpoint.get(p.getStringUUID())==Math.min(30,f+1)&&VolcanicSpire.progress.checkpoint.get(q.getStringUUID())==Math.min(30,f+1),"both player checkpoints saved");
  check(l.getBlockState(new BlockPos(VolcanoLayout.x(f)+29,65,98)).is(Blocks.SHROOMLIGHT),"next gate appears");VolcanicSpire.encounter(l,f,r);check(r.boss==b,"no respawn after clear");
  VolcanicSpire.dispose(r);VolcanicSpire.RUNS.remove(f);for(var e:new ArrayList<net.minecraft.world.entity.Entity>(l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(VolcanoLayout.x(f),60,0,VolcanoLayout.x(f)+65,90,105))))e.discard();
  System.out.println("VOLCANO_FLOOR_PASS "+f);
 }
}
