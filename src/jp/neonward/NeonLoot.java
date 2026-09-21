package jp.neonward;
public final class NeonLoot {
 public static LootProfile profile(net.minecraft.world.entity.Entity enemy){if(SkySite.contains(enemy.level(),enemy.blockPosition()))return enemy instanceof SkyBoss&&enemy.entityTags().contains("nw_sky_boss")?LootProfile.BOSS:LootProfile.DUNGEON;if(!SpireSite.contains(enemy.level(),enemy.blockPosition()))return LootProfile.FIELD;return enemy instanceof CyberEnemy&&enemy.entityTags().contains("nw_spire_boss")?LootProfile.BOSS:LootProfile.DUNGEON;}
}
