package jp.neonward;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
final class VolcanoBosses {
 static final List<EntityType<VolcanoBoss>> TYPES=new ArrayList<>();
 static void init(){for(int f=1;f<=30;f++){
  var key=ResourceKey.create(Registries.ENTITY_TYPE,NeonWard.id(VolcanoRoster.id(f)));
  var type=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<VolcanoBoss>of(VolcanoBoss::new,MobCategory.MONSTER).sized(VolcanoRoster.WIDTH[f-1],VolcanoRoster.HEIGHT[f-1]).clientTrackingRange(12).updateInterval(2).fireImmune().noLootTable().build(key));TYPES.add(type);
  FabricDefaultAttributeRegistry.register(type,VolcanoBoss.createAttributes().add(Attributes.MAX_HEALTH,VolcanoRoster.hp(f)).add(Attributes.ARMOR,4+f*.25).add(Attributes.ATTACK_DAMAGE,VolcanoRoster.damage(f)).add(Attributes.MOVEMENT_SPEED,.23).add(Attributes.FOLLOW_RANGE,64).add(Attributes.SPAWN_REINFORCEMENTS_CHANCE,0).add(Attributes.KNOCKBACK_RESISTANCE,1));
 }}
}
