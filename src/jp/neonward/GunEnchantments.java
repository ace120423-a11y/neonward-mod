package jp.neonward;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
public final class GunEnchantments {
 public static int level(ServerLevel l,ItemStack s,ResourceKey<Enchantment> key){return EnchantmentHelper.getItemEnchantmentLevel(l.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key),s);}
 public static float damage(ServerLevel l,ItemStack s,float base){int power=level(l,s,Enchantments.POWER);return base*(power==0?1:1+.25f*(power+1));}
 public static void impact(ServerLevel l,ItemStack s,LivingEntity target,Vec3 direction){int punch=level(l,s,Enchantments.PUNCH);if(punch>0)target.knockback(punch*.6,-direction.x,-direction.z,target.damageSources().generic(),0);if(level(l,s,Enchantments.FLAME)>0)target.igniteForSeconds(5);}
 public static int looting(net.minecraft.world.damagesource.DamageSource source,ServerLevel l){return source.getEntity() instanceof LivingEntity e?Math.min(3,level(l,e.getMainHandItem(),Enchantments.LOOTING)):0;}
}
