package jp.neonward;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Enemy;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;

/** Applies to newly spawned mobs and existing mobs as their chunks load. */
public final class VanillaEnemyFilter {
 public static boolean blocked(Entity e) {
  return BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getNamespace().equals("minecraft")
      && (e instanceof Enemy || e.getType().getCategory()==MobCategory.MONSTER);
 }
 public static void init() {
  ServerEntityEvents.ENTITY_LOAD.register((entity,level)->{if(blocked(entity))entity.discard();});
 }
}
