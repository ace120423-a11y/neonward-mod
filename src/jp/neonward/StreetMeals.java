package jp.neonward;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
/** Ordinary consumable foods; timed effects persist with the player's vanilla effect data. */
public final class StreetMeals {
 // Stable indices/IDs: existing saved stacks and shop subsets retain the first four entries.
 static final String[] IDS={"street_skewer","street_ramen","street_curry","street_tea","street_gyoza","street_motsu_stew","street_grilled_fish","street_seasonal_plate","street_oden","street_zero_beer","street_coffee","street_toast","street_beer","street_sake","street_shochu","street_highball","street_umeshu","street_cocktail"};
 static final String[] NAMES={"炭火の串焼き","横丁ラーメン","スパイスカレー","集中ブレンド茶","鉄板焼き餃子","もつ煮込み","焼き魚定食","季節の小鉢","屋台おでん","ノンアルコールビール","深煎りブレンドコーヒー","喫茶店の厚切りトースト","ネオン生ビール","月灯り純米酒","横丁焼酎ロック","琥珀ハイボール","青梅の梅酒","ネオン・サンセット"};
 // Buff family: speed, regeneration, resistance, focus. Repeated meals refresh, not stack.
 static final int[] EFFECT_KINDS={0,1,2,3,0,1,2,1,2,0,3,0,0,1,2,0,1,3};
 private static final String[] BUFF_LABELS={"移動速度+20% / 5分","再生 I / 60秒","耐性 I / 3分","討伐経験値+20% / 10分"};
 static final String[] EFFECTS=java.util.Arrays.stream(EFFECT_KINDS).mapToObj(k->BUFF_LABELS[k]).toArray(String[]::new);
 static final int[] PRICES={180,240,320,280,200,260,340,160,220,180,260,150,180,260,220,240,280,320};
 static final Item[] ITEMS=new Item[IDS.length];static Holder<MobEffect> FOCUS;
 static boolean drink(int kind){return kind==3||kind==9||kind==10||kind>=12&&kind<IDS.length;}
 static class Focus extends MobEffect {Focus(){super(MobEffectCategory.BENEFICIAL,0xffbd59);}}
 static class Meal extends Item {
  final int kind;Meal(Properties p,int k){super(p);kind=k;}
  @Override public ItemStack finishUsingItem(ItemStack stack,Level l,LivingEntity e){var out=super.finishUsingItem(stack,l,e);if(!l.isClientSide())apply(e,kind);return out;}
 }
 static void apply(LivingEntity p,int kind){if(kind<0||kind>=EFFECT_KINDS.length)return;switch(EFFECT_KINDS[kind]){
  case 0->p.addEffect(new MobEffectInstance(MobEffects.SPEED,6000,0));
  case 1->p.addEffect(new MobEffectInstance(MobEffects.REGENERATION,1200,0));
  case 2->p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,3600,0));
  case 3->p.addEffect(new MobEffectInstance(FOCUS,12000,0));
 }}
 static void init(){FOCUS=Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,NeonWard.id("meal_focus"),new Focus());
  for(int i=0;i<IDS.length;i++){var id=NeonWard.id(IDS[i]);ITEMS[i]=Registry.register(BuiltInRegistries.ITEM,id,new Meal(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id)).food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(drink(i)?2:8).saturationModifier(.6f).alwaysEdible().build(),drink(i)?net.minecraft.world.item.component.Consumables.DEFAULT_DRINK:net.minecraft.world.item.component.Consumables.DEFAULT_FOOD),i));}
  net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH.register((e,source)->{if(e instanceof net.minecraft.world.entity.monster.Enemy&&source.getEntity() instanceof ServerPlayer p&&p.hasEffect(FOCUS)&&e.level() instanceof net.minecraft.server.level.ServerLevel l){int xp=e.getExperienceReward(l,p);if(xp>0)p.giveExperiencePoints(Math.max(1,xp/5));}});
 }
}
