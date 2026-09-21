package jp.neonward;
import java.util.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
public final class OrnamentalFish {
 public static final String[] IDS={"neon_tetra","clownfish","royal_betta","angel_fish","kohaku_koi","moon_jelly"};
 public static final String[] NAMES={"ネオンテトラ","カクレクマノミ","ロイヤルベタ","エンゼルフィッシュ","紅白錦鯉","ミズクラゲ"};
 public static final int[] MIN={2,6,5,8,15,8},MAX={5,14,12,25,60,30},PRICE={90,130,220,180,300,260};
 public static final List<Item> ITEMS=new ArrayList<>();
 public static void init(){for(String name:IDS){var key=ResourceKey.create(Registries.ITEM,NeonWard.id(name));ITEMS.add(Registry.register(BuiltInRegistries.ITEM,key,new Item(new Item.Properties().setId(key).stacksTo(1))));}}
 public static int kind(ItemStack s){return ITEMS.indexOf(s.getItem());}
 public static boolean isFish(ItemStack s){return kind(s)>=0;}
}
