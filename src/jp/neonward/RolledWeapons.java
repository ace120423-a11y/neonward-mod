package jp.neonward;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.*;
import java.util.*;
public final class RolledWeapons {
 public static double multiplier(ItemStack stack){var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();int tier=data.getIntOr("neon_weapon_tier",-1);if(tier<0||tier>=5)return 1;int power=data.getIntOr("neon_weapon_power",100);return Math.max(WeaponLoot.MIN[tier],Math.min(WeaponLoot.MAX[tier],power))/100.0;}
 public static ItemStack create(Item item,java.util.random.RandomGenerator random){return create(item,random,LootProfile.FIELD);}
 public static ItemStack create(Item item,java.util.random.RandomGenerator random,LootProfile profile){
  if(item==NeonShield.ITEM)return new ItemStack(item); // Vanilla shield protection has no damage-quality roll.
  var roll=WeaponLoot.quality(random,profile);int tier=roll.tier(),power=roll.power();var stack=new ItemStack(item);var name=stack.getHoverName().copy().append(" ["+CyberwareCatalog.RARITIES[tier]+"]").withStyle(s->s.withColor(CyberwareCatalog.COLORS[tier]).withItalic(false));stack.set(DataComponents.CUSTOM_NAME,name);
  CustomData.update(DataComponents.CUSTOM_DATA,stack,t->{t.putInt("neon_weapon_tier",tier);t.putInt("neon_weapon_power",power);});
  stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE,tier>=3);
  var lore=new ArrayList<Component>(stack.getOrDefault(DataComponents.LORE,ItemLore.EMPTY).lines());lore.add(Component.literal("武器攻撃性能 "+power+"% / 基準武器比").withStyle(net.minecraft.ChatFormatting.AQUA));
  if(item instanceof NeonArsenal.Rifle gun)lore.add(Component.literal(String.format(Locale.ROOT,"基礎ダメージ %.2f / 弾薬無限",gun.damage*power/100.0)).withStyle(net.minecraft.ChatFormatting.GRAY));
  else {var attrs=stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.EMPTY);var entries=new ArrayList<ItemAttributeModifiers.Entry>();for(var e:attrs.modifiers()){var modifier=e.modifier();if(e.attribute().equals(Attributes.ATTACK_DAMAGE)&&modifier.operation()==AttributeModifier.Operation.ADD_VALUE)modifier=new AttributeModifier(modifier.id(),modifier.amount()*power/100.0,modifier.operation());entries.add(new ItemAttributeModifiers.Entry(e.attribute(),modifier,e.slot(),e.display()));}stack.set(DataComponents.ATTRIBUTE_MODIFIERS,new ItemAttributeModifiers(entries));}
  lore.add(Component.literal("ドロップ個体値 / 持ち替え・再起動で変化しません").withStyle(net.minecraft.ChatFormatting.GRAY));stack.set(DataComponents.LORE,new ItemLore(lore));return stack;
 }
}
