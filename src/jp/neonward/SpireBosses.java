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
  FabricDefaultAttributeRegistry.register(type,SpireBoss.createAttributes().add(Attributes.MAX_HEALTH,k.hp()).add(Attributes.ARMOR,4+k.floor()*.2).add(Attributes.MOVEMENT_SPEED,.23).add(Attributes.ATTACK_DAMAGE,k.damage()).add(Attributes.FOLLOW_RANGE,32).add(Attributes.SPAWN_REINFORCEMENTS_CHANCE,0).add(Attributes.KNOCKBACK_RESISTANCE,1));
 }}
 static SpireBoss spawn(ServerLevel l,int f,double x,double z){
  var k=BossRoster.floor(f);var e=new SpireBoss(TYPES.get(k.id()),l);e.setPos(SpireSite.X+x,DungeonLayout.base(f)+1,SpireSite.Z+z);e.setPersistenceRequired();e.addTag("nw_spire_floor_"+f);e.addTag("nw_spire_boss");e.setCustomName(Component.literal(f+"F / "+k.name()).withColor(k.color()));e.setHealth(e.getMaxHealth());l.addFreshEntity(e);return e;
 }
 /** Only legacy bosses change; cleared floors, player checkpoints and normal enemies survive. */
 static void migrate(ServerLevel l){
  var old=new ArrayList<CyberEnemy>();for(var e:l.getAllEntities())if(e instanceof CyberEnemy c&&!(c instanceof SpireBoss)&&c.isAlive()&&c.entityTags().contains("nw_spire_boss"))old.add(c);
  for(var e:old){int f=DungeonLayout.floor(e.getY());var b=spawn(l,f,32.5,32.5);if(l.getEntity(b.getUUID())==b){b.setHealth(Math.max(1,b.getMaxHealth()*e.getHealth()/e.getMaxHealth()));e.discard();}}
 }
}
