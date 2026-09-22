package jp.neonward;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

/** Main-hand bonuses only. Inventory/offhand weapons never grant passive protection. */
public final class MeleeGuard {
 public static boolean guarding(Player p){return p.isAlive()&&!p.isSpectator()&&p.isUsingItem()&&p.getUsedItemHand()==InteractionHand.MAIN_HAND&&MeleeElements.kind(p.getMainHandItem())!=null&&p.getUseItem()==p.getMainHandItem();}
 public static float multiplier(Player p,DamageSource source){
  if(!p.isAlive()||p.isSpectator()||source.is(DamageTypeTags.BYPASSES_INVULNERABILITY))return 1f;
  int skillType=ArsenalExpansion.type(p.getMainHandItem());
  if(skillType==0||skillType==1||skillType==3||skillType==4)return .5f;
  if(PairedHands.paired(p.getMainHandItem()))return .55f;
  if(guarding(p))return .5f;
  return MeleeElements.kind(p.getMainHandItem())==MeleeElements.Kind.WAVE?.55f:1f;
 }
 public static void updateSpeed(Player p){
  MeleeReach.update(p);
  var attribute=p.getAttribute(Attributes.MOVEMENT_SPEED);if(attribute==null)return;
  var id=NeonWard.id("melee_blade_speed");var kind=MeleeElements.kind(p.getMainHandItem());
  boolean active=p.isAlive()&&!p.isSpectator()&&kind!=null&&kind!=MeleeElements.Kind.WAVE;
  if(active){if(attribute.getModifier(id)==null)attribute.addTransientModifier(new AttributeModifier(id,.4,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));}
  else if(attribute.getModifier(id)!=null)attribute.removeModifier(id);
 }
 public static String description(ItemStack stack){return MeleeElements.kind(stack)==MeleeElements.Kind.WAVE?"メイン手：被ダメージ45%軽減 / 右長押しガード中は合計50%軽減":"メイン手：移動速度+40% / 右長押しガード中は被ダメージ50%軽減";}
 public static final class Weapon extends Item {
  public Weapon(Properties properties){super(properties);}
  @Override public int getUseDuration(ItemStack stack,LivingEntity entity){return 72000;}
  @Override public ItemUseAnimation getUseAnimation(ItemStack stack){return ItemUseAnimation.BLOCK;}
  @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
   if(hand!=InteractionHand.MAIN_HAND||player.isSpectator())return InteractionResult.PASS;
   if(NeonShield.offhandReady(player))return InteractionResult.PASS;
   player.startUsingItem(hand);return InteractionResult.CONSUME;
  }
 }
}
