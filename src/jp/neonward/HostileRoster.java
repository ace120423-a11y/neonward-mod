package jp.neonward;

/** Shared gameplay data; weights make heavy units less common than street gangs. */
public final class HostileRoster {
 public record Kind(String id,String name,double hp,double armor,double speed,double damage,String weapon,int interval,int range,int color,int weight,String role) {}
 public static final Kind[] ALL={
  new Kind("scrap_raider","スクラップ・レイダー",28,2,.25,4,"riot_bat",0,0,0xffa34d,18,"バットで襲う廃品回収ギャング"),
  new Kind("chrome_ronin","クローム浪人",36,4,.29,7,"kurosame_katana",0,0,0xff477d,12,"刀で迫る近接戦闘員"),
  new Kind("neon_runner","ネオン・ランナー",22,0,.36,4,"akatsuki_wakizashi",0,0,0x41ffcf,12,"間合いを詰める高速ダッシュ"),
  new Kind("street_enforcer","ストリート・エンフォーサー",30,3,.25,4,"kestrel_pistol",42,22,0x5ae9ff,15,"予告照準からハンドガンを発射"),
  new Kind("belt_gunner","ベルト・ガンナー",44,5,.21,2,"storm_machinegun",12,24,0xffce55,9,"短い間隔で射撃する機関銃兵"),
  new Kind("ghost_sniper","ゴースト・スナイパー",26,1,.23,10,"longwatch_sniper",100,40,0xac88ff,7,"長い照準予告の後に高威力射撃"),
  new Kind("riot_bulwark","ライオット・バルワーク",70,14,.18,6,"coil_hammer",0,0,0x548bff,7,"大きな肩装甲を持つ重装兵"),
  new Kind("pile_breaker","パイル・ブレイカー",55,6,.22,10,"pile_maul",0,0,0xff7845,6,"重い一撃で吹き飛ばす作業用改造兵"),
  new Kind("arc_trooper","アーク・トルーパー",38,5,.24,4,"pulse_rifle",55,23,0x5ffff4,6,"命中時に短時間の移動低下を与える電撃兵"),
  new Kind("hex_netrunner","ヘックス・ネットランナー",28,2,.27,2,"wisp_compact",65,20,0xee61ff,5,"照準を当てて移動を妨害するハッカー"),
  new Kind("patch_medic","パッチ・メディック",34,3,.26,3,"kestrel_pistol",65,18,0x65ff94,5,"近くの敵一体を定期的に修復する支援兵"),
  new Kind("iron_colossus","アイアン・コロッサス",180,12,.18,12,"pile_maul",0,0,0xff4242,1,"大型強敵。予告の後、周囲に衝撃波"),
  new Kind("neon_bomber","ネオン・ボマー",34,2,.24,12,"pile_maul",0,0,0xffed5b43,7,"接近すると警告音を鳴らして自爆"),
  new Kind("mirage_stalker","ミラージュ・ストーカー",28,1,.31,5,"akatsuki_wakizashi",0,0,0xffb86cff,7,"短い間隔で姿を消して背後へ跳ぶ"),
  new Kind("signal_hacker","シグナル・ハッカー",30,2,.26,3,"wisp_compact",65,20,0xff36d9ff,6,"通信妨害で暗闇と移動低下を与える")
 };
 public static Kind choose(java.util.random.RandomGenerator random){int n=random.nextInt(java.util.Arrays.stream(ALL).mapToInt(Kind::weight).sum());for(var k:ALL){n-=k.weight();if(n<0)return k;}throw new AssertionError();}
 public static boolean insideCity(int x,int z){return x>=-32&&x<=575&&z>=-32&&z<=703;}
 public static String habitat(String biome){
  if(biome.contains("desert")||biome.contains("badlands")||biome.contains("savanna"))return "荒野";
  if(biome.contains("snow")||biome.contains("frozen")||biome.contains("ice")||biome.contains("grove"))return "雪原";
  if(biome.contains("swamp")||biome.contains("mangrove"))return "湿地";
  if(biome.contains("forest")||biome.contains("jungle")||biome.contains("taiga"))return "森林";
  if(biome.contains("peak")||biome.contains("mountain")||biome.contains("hill")||biome.contains("slope"))return "山岳";
  return "平原・海岸";
 }
 public static int[] pool(String biome){return switch(habitat(biome)){
  case "荒野"->new int[]{0,3,4,7,11,12,14};case "雪原"->new int[]{5,6,8,10,11,13,14};case "湿地"->new int[]{2,8,9,10,12,13};
  case "森林"->new int[]{1,2,5,9,12,13};case "山岳"->new int[]{5,6,7,8,11,12,13};default->new int[]{0,1,3,4,10,12,13,14};};}
 public static Kind chooseForBiome(java.util.random.RandomGenerator random,String biome){var pool=pool(biome);int total=0;for(int i:pool)total+=ALL[i].weight();int n=random.nextInt(total);for(int i:pool){n-=ALL[i].weight();if(n<0)return ALL[i];}throw new AssertionError();}
}
