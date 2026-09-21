package jp.neonward;

import java.util.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

public final class SpireBosses {
 public static final Map<EntityType<?>,BossRoster.Kind> KINDS=new LinkedHashMap<>();
 public static final Map<String,EntityType<SpireBoss>> TYPES=new LinkedHashMap<>();
 public static void init(){for(var k:BossRoster.ALL){
  var key=ResourceKey.create(Registries.ENTITY_TYPE,NeonWard.id(k.id()));
  var type=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<SpireBoss>of(SpireBoss::new,MobCategory.MONSTER).sized(k.width(),k.height()).clientTrackingRange(10).notInPeaceful().noLootTable().build(key));
  KINDS.put(type,k);TYPES.put(k.id(),type);
  FabricDefaultAttributeRegistry.register(type,SpireBoss.createAttributes().add(Attributes.MAX_HEALTH,k.hp()*4.5).add(Attributes.ARMOR,4+k.floor()*.2).add(Attributes.MOVEMENT_SPEED,.23).add(Attributes.ATTACK_DAMAGE,k.damage()).add(Attributes.FOLLOW_RANGE,96).add(Attributes.SPAWN_REINFORCEMENTS_CHANCE,0).add(Attributes.KNOCKBACK_RESISTANCE,1));
 }}
 static SpireBoss spawn(ServerLevel l,int f,double x,double z){
  var k=BossRoster.floor(f);var e=new SpireBoss(TYPES.get(k.id()),l);e.setPos(SpireSite.X+x,DungeonLayout.base(f)+1,SpireSite.Z+z);e.setPersistenceRequired();e.addTag("nw_spire_floor_"+f);e.addTag("nw_spire_boss");e.addTag("nw_spire_run_"+NightSpire.progress.run);e.setCustomName(Component.literal(f+"F / "+k.name()).withColor(k.color()));e.setHealth(e.getMaxHealth());return l.addFreshEntity(e)?e:null;
 }
 /** Only legacy bosses change; cleared floors, player checkpoints and normal enemies survive. */
 static void migrate(ServerLevel l){
  var p=NightSpire.progress;boolean changed=false;
  // Records remain occupied when the entity's chunk unloads. A narrow spatial query
  // must never turn "not currently visible" into permission to spawn another boss.
  for(var e:NightSpire.enemiesAll(l)){
   if(p.run>0&&!e.entityTags().contains("nw_spire_run_"+p.run)){e.discard();continue;}
   if(!e.entityTags().contains("nw_spire_boss"))continue;
   int f=e instanceof SpireBoss b?b.spec().floor():DungeonLayout.floor(e.getY());
   if(!e.isAlive()){if(e.getStringUUID().equals(p.bosses.get(f))){p.bosses.remove(f);changed=true;}continue;}
   String owner=p.bosses.get(f);
   if(owner!=null&&!owner.equals(e.getStringUUID())){e.discard();continue;}
   if(!(e instanceof SpireBoss)){var b=spawn(l,f,32.5,32.5);if(b==null)continue;b.setHealth(Math.max(1,b.getMaxHealth()*e.getHealth()/e.getMaxHealth()));e.discard();e=b;}
   if(!e.getStringUUID().equals(p.bosses.put(f,e.getStringUUID())))changed=true;
  }
  if(changed)try{NightSpire.save();}catch(Exception ex){System.err.println("[Night Spire] Boss reconciliation save failed: "+ex);}
 }
}
