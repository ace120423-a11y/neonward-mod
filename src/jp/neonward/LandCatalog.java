package jp.neonward;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;
/** Explicit allow-list: never exposes commands, weapons, furniture or spawn eggs. */
public final class LandCatalog {
 public record Product(String id,String category,int price){}
 public static final List<Product> PRODUCTS=new ArrayList<>();
 static void add(String id,String category,int price){var item=BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(id));if(item!=null&&item!=Items.AIR)PRODUCTS.add(new Product(id,category,price));}
 static void init(){PRODUCTS.clear();
  for(String id:List.of("dirt","grass_block","coarse_dirt","sand","red_sand","gravel","mud","clay"))add(id,"土・砂",4);
  for(String base:List.of("stone","cobblestone","mossy_cobblestone","stone_brick","mossy_stone_brick","deepslate_brick","deepslate_tile","cobbled_deepslate","polished_deepslate","tuff","polished_tuff","tuff_brick","brick","sandstone","red_sandstone","quartz","blackstone","polished_blackstone","polished_blackstone_brick")){
   add(switch(base){case "stone_brick","mossy_stone_brick","deepslate_brick","tuff_brick","brick","polished_blackstone_brick"->base+"s";case "deepslate_tile"->"deepslate_tiles";case "quartz"->"quartz_block";default->base;},"石材",12);for(String suffix:List.of("_stairs","_slab","_wall"))add(base+suffix,"石材",10);
  }
  for(String wood:List.of("oak","spruce","birch","jungle","acacia","dark_oak","mangrove","cherry","pale_oak","bamboo"))for(String suffix:List.of("_planks","_log","_stairs","_slab","_fence","_fence_gate","_door","_trapdoor"))add(wood+suffix,"木材",8);
  for(String color:List.of("white","orange","magenta","light_blue","yellow","lime","pink","gray","light_gray","cyan","purple","blue","brown","green","red","black"))for(String suffix:List.of("_concrete","_terracotta","_wool","_stained_glass","_stained_glass_pane"))add(color+suffix,"色・装飾",16);
  for(String id:List.of("glass","glass_pane","torch","lantern","sea_lantern","glowstone","ladder","chest","barrel","crafting_table","furnace","copper_block","iron_bars"))add(id,"設備",id.equals("sea_lantern")||id.equals("glowstone")?80:24);
  for(String id:List.of("wheat_seeds","beetroot_seeds","pumpkin_seeds","melon_seeds","carrot","potato"))add(id,"農業",20);add("water_bucket","農業",100);add("iron_hoe","農業",200);
  PRODUCTS.add(new Product("cow","家畜",1000));PRODUCTS.add(new Product("pig","家畜",800));PRODUCTS.add(new Product("chicken","家畜",500));
 }
 public static boolean page(Product p,int terminal){return terminal==1?!p.category().equals("農業")&&!p.category().equals("家畜"):terminal==2?p.category().equals("家畜"):terminal==3&&p.category().equals("農業");}
}
