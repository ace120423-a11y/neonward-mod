package jp.neonward;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;

/** Prayer blessings use saved vanilla effect durations; praying again never rerolls them. */
public final class ShrineBlessings {
 public static final int DURATION=6000;
 public static Holder<MobEffect> ATTACK,DEFENSE;
 private static final class Blessing extends MobEffect {
  Blessing(int color){super(MobEffectCategory.BENEFICIAL,color);}
 }
 public static void init(){
  ATTACK=Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,NeonWard.id("shrine_attack"),new Blessing(0xee974e));
  DEFENSE=Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,NeonWard.id("shrine_defense"),new Blessing(0x62b8d5));
 }
 public static boolean active(ServerPlayer p){return p.hasEffect(ATTACK)||p.hasEffect(DEFENSE);}
 public static boolean grant(ServerPlayer p){
  if(active(p)||!p.isAlive())return false;
  return p.addEffect(new MobEffectInstance(p.getRandom().nextBoolean()?ATTACK:DEFENSE,DURATION,0));
 }
 public static float attackMultiplier(ServerPlayer p){return p.hasEffect(ATTACK)?1.1f:1f;}
 public static float defenseMultiplier(ServerPlayer p){return p.hasEffect(DEFENSE)?.9f:1f;}
 private ShrineBlessings(){}
}
