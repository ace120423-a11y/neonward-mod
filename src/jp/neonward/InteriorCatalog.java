package jp.neonward;
public final class InteriorCatalog {
 public record Product(String id,String name,String kind,int price) {}
 public static final Product[] PRODUCTS={
new Product("shop_sofa_velvet","ベルベット・ソファー","seat",450),
new Product("shop_sofa_leather","ブラックレザー・ソファー","seat",550),
new Product("shop_sofa_ivory","アイボリー・ソファー","seat",650),
new Product("shop_sofa_indigo","藍染・ローソファー","seat",750),
new Product("shop_sofa_industrial","インダストリアル・ソファー","seat",850),
new Product("shop_sofa_mint","ミント・ラウンジソファー","seat",950),
new Product("shop_tv_retro","レトロウッドTV","tv",1200),
new Product("shop_tv_chrome","クロームTV","tv",1500),
new Product("shop_tv_neon","ネオンフレームTV","tv",1800),
new Product("shop_tv_ivory","アイボリーTV","tv",2100),
new Product("shop_table_oak","オーク・ローテーブル","table",300),
new Product("shop_table_glass","ガラス・ネオンテーブル","table",400),
new Product("shop_table_black","黒漆・ローテーブル","table",500),
new Product("shop_table_copper","銅縁・テーブル","table",600),
new Product("shop_chair_executive","レザー・ワークチェア","seat",350),
new Product("shop_chair_gaming","ネオン・ゲーミングチェア","seat",500),
new Product("shop_chair_fabric","ファブリック・チェア","seat",650),
new Product("shop_shelf_oak","木製ブックシェルフ","storage",500),
new Product("shop_shelf_steel","スチール・収納棚","storage",650),
new Product("shop_shelf_red","赤漆・飾り棚","storage",800),
new Product("display_aquarium","アクア・展示水槽","aquarium",1800)};
 static void register(){NeonFurniture.register("interior_counter","desk",8);for(var p:PRODUCTS)if(!p.kind().equals("aquarium"))NeonFurniture.register(p.id(),p.kind(),p.kind().equals("tv")?5:2);}
}
