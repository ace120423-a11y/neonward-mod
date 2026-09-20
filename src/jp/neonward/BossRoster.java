package jp.neonward;
/** One identity, anatomy and combat program per floor. */
public final class BossRoster {
 public record Kind(int floor,String id,String name,String form,String attack,String hint,String motion,int color,float width,float height) {
  public double hp(){return 160+floor*24+(floor%5==0?120:0);}
  public double damage(){return 5+floor*.3;}
  public HostileRoster.Kind legacy(){return new HostileRoster.Kind(id,name,hp(),4+floor*.2,.23,damage(),"",0,0,color,0,"boss");}
 }
 public static final Kind[] ALL={
  new Kind(1,"boss_clamp_warden","クランプ・ウォーデン","六脚装甲ガニ","双鋏圧砕","左右の鋏を避け、中央の追撃から離れる","stalk",0xff9944,2.6f,1.7f),
  new Kind(2,"boss_cross_drone","クロス・ドローン","四連ローター十字機","十字照射","十字の線の間に立つ","orbit",0x40eaff,2.8f,2.2f),
  new Kind(3,"boss_needle_empress","ニードル・エンプレス","尾針サソリ","尾針穿孔","足元の印を離れ、尾針の直線から横へ","stalk",0xe668ff,2.2f,2.9f),
  new Kind(4,"boss_razor_hound","レイザー・ハウンド","刃背の四足猟犬","三連疾走","突進線を横にかわす。三回目まで油断しない","rush",0xff536c,1.5f,2f),
  new Kind(5,"boss_bastion_crawler","バスティオン・クローラー","双履帯移動砲台","五連迫撃","着弾円を順に避ける","anchor",0xffc44c,2.8f,2.2f),
  new Kind(6,"boss_jelly_oracle","ジェリー・オラクル","発光傘と触手のクラゲ","拡散電環","広がる輪をジャンプで越える","float",0x63ffd9,2f,3.2f),
  new Kind(7,"boss_scythe_mantis","サイズ・マンティス","二枚鎌の機械カマキリ","交差三日月","斜めの斬線を避けて最後は背後へ","strafe",0x96ff48,2f,2.9f),
  new Kind(8,"boss_coil_serpent","コイル・サーペント","節状の電磁ヘビ","蛇行放電","蛇行する発光列から離れる","slither",0x65a8ff,1.8f,1.2f),
  new Kind(9,"boss_mono_reaper","モノ・リーパー","巨大一輪と左右刃","轢断車輪","直線をかわして着地点から離れる","rush",0xff6540,2.4f,2.5f),
  new Kind(10,"boss_prism_eye","プリズム・アイ","三重環の巨大眼球","三角屈折","三角形の照射線を避ける","anchor",0xf582ff,3f,3.8f),
  new Kind(11,"boss_iron_centaur","アイアン・ケンタウロス","槍腕の四脚騎兵","蹄撃突貫","最初に距離を取り、突進を横へ避ける","rush",0xe9b66b,2f,3.5f),
  new Kind(12,"boss_echo_wraith","エコー・レイス","音響翼のコウモリ","音翼共鳴","左右の翼の間にある前後の隙間へ","orbit",0xb383ff,2.8f,3.3f),
  new Kind(13,"boss_spiral_hermit","スパイラル・ハーミット","巻き殻の装甲巻貝","螺旋機雷","外側から順番に起爆する円を避ける","anchor",0xf49e63,2f,2.3f),
  new Kind(14,"boss_kraken_router","クラーケン・ルーター","八腕の通信タコ","八方接続","八本の放射線の隙間へ","float",0x44e3b2,2.6f,3f),
  new Kind(15,"boss_forge_golem","フォージ・ゴーレム","炉心と巨大金床拳","鍛造床打ち","赤い床の帯から安全な列へ移る","stalk",0xff752d,2.8f,3.6f),
  new Kind(16,"boss_mirror_moth","ミラー・モス","二対の結晶蝶翼","鏡面挟撃","両側から来る線の中央を避ける","orbit",0x97eeff,2.7f,3.1f),
  new Kind(17,"boss_obelisk_zero","オベリスク・ゼロ","浮遊石碑と公転角柱","零点方格","格子の線の間に立つ","anchor",0x79c9ff,2.8f,3.8f),
  new Kind(18,"boss_glacier_ram","グレイシャー・ラム","巨大角の六角氷装羊","凍角雪崩","足元の凍結円と遅れて来る突進に注意","rush",0xacf4ff,2.3f,2.5f),
  new Kind(19,"boss_rail_leviathan","レール・リヴァイアサン","三両連結の装甲列車","軌道転轍","二本の線路を避け、横断砲撃も確認","patrol",0x55bcff,1.7f,2f),
  new Kind(20,"boss_clock_archon","クロック・アルコン","歯車円盤と時計針","時針断層","順番に点灯する時計針の間を移る","anchor",0xffe69c,2.8f,3.5f),
  new Kind(21,"boss_carrion_vulture","キャリオン・ヴァルチャー","鋼の羽根と鉤爪の禿鷹","急降下狩猟","影の円を避け、羽根の直線から離れる","orbit",0xe87878,2.9f,2.9f),
  new Kind(22,"boss_blood_bloom","ブラッド・ブルーム","肉質花弁と機械根","吸血開花","花弁状の危険円を避けて中心へ","anchor",0xff446e,2.8f,3.3f),
  new Kind(23,"boss_drill_mole","ドリル・モール","採掘ドリルの地底獣","穿地連爆","連続する地割れを横に避ける","slither",0xd89d56,2.7f,1.5f),
  new Kind(24,"boss_funeral_bell","フューネラル・ベル","浮遊する鐘と鎖","葬鐘三響","三つの衝撃輪をジャンプで越える","float",0xd77bfa,2f,3.5f),
  new Kind(25,"boss_oni_executioner","オニ・エクスキューショナー","角面と背負い大太刀の鬼","断罪抜刀","扇状の斬撃を背後へ避け、十字斬りをかわす","strafe",0xff414d,1.8f,4f),
  new Kind(26,"boss_solar_scarab","ソーラー・スカラベ","太陽円盤を背負う甲虫","日輪焦土","外周が光る間は中央、次は中央から離れる","stalk",0xffd94b,2.6f,3.4f),
  new Kind(27,"boss_abyss_ray","アビス・レイ","幅広の鰭と長い毒尾のエイ","深淵掃流","左右の帯を避けて中央の追撃もかわす","orbit",0x587cff,2.8f,2f),
  new Kind(28,"boss_crown_hydra","クラウン・ヒュドラ","三頭の王冠竜","三頭連砲","三本の照準線を順番に横へかわす","anchor",0xb0ff77,2.8f,3.5f),
  new Kind(29,"boss_seraph_engine","セラフ・エンジン","六翼と光輪の天使機関","六翼審判","六方向の光線と足元の印を避ける","float",0xffefbd,3f,4.3f),
  new Kind(30,"boss_night_sovereign","ナイト・ソヴリン","機械竜と王冠炉心","王域終焉","扇状ブレス、着弾円、衝撃輪の順に対応","stalk",0xff5bba,3f,4.2f),
 };
 public static Kind floor(int floor){return ALL[Math.max(1,Math.min(30,floor))-1];}
}
