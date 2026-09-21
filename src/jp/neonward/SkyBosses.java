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

public final class SkyBosses {
 public static final Map<EntityType<?>,SkyRoster.Kind> KINDS=new LinkedHashMap<>();
 public static final Map<String,EntityType<SkyBoss>> TYPES=new LinkedHashMap<>();
 public static void init(){for(var k:SkyRoster.ALL){
  var key=ResourceKey.create(Registries.ENTITY_TYPE,NeonWard.id(k.id()));
  var type=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<SkyBoss>of(SkyBoss::new,MobCategory.MONSTER).sized(k.width(),k.height()).clientTrackingRange(10).notInPeaceful().noLootTable().build(key));
  KINDS.put(type,k);TYPES.put(k.id(),type);
  FabricDefaultAttributeRegistry.register(type,SkyBoss.createAttributes().add(Attributes.MAX_HEALTH,k.hp()*4.5).add(Attributes.ARMOR,4+k.floor()*.2).add(Attributes.MOVEMENT_SPEED,.23).add(Attributes.ATTACK_DAMAGE,k.damage()).add(Attributes.FOLLOW_RANGE,96).add(Attributes.SPAWN_REINFORCEMENTS_CHANCE,0).add(Attributes.KNOCKBACK_RESISTANCE,1));
 }}
 static SkyBoss spawn(ServerLevel l,int f,double x,double z){
  var k=SkyRoster.ALL[f-1];var e=new SkyBoss(TYPES.get(k.id()),l);e.setPos(SkySite.X+x,DungeonLayout.base(f)+1,SkySite.Z+z);e.setPersistenceRequired();e.addTag("nw_sky_floor_"+f);e.addTag("nw_sky_boss");e.setCustomName(Component.literal(f+"F / "+k.name()).withColor(k.color()));e.setHealth(e.getMaxHealth());l.addFreshEntity(e);return e;
 }

}
