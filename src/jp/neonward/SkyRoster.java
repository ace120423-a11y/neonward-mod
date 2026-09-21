package jp.neonward;
/** Dedicated second-tower roster. Values are independent of first-tower balance changes. */
public final class SkyRoster {
 public record Kind(int floor,String id,String name,String form,String attack,String hint,String motion,int color,float width,float height){
  public double hp(){return 160+floor*24+(floor%5==0?120:0);}
  public double damage(){return 5+floor*.3;}
  public HostileRoster.Kind legacy(){return new HostileRoster.Kind(id,name,hp(),4+floor*.2,.23,damage(),"",0,0,color,0,"sky_boss");}
 }
 public static final Kind[] ALL={
  new Kind(1,"sky_guardian_1","風甲の門番","六脚装甲ガニ","双鋏圧砕","左右の鋏を避け、中央の追撃から離れる","stalk",0xff9944,2.6f,1.7f),
  new Kind(2,"sky_guardian_2","スカイ・ローター","四連ローター十字機","十字照射","十字の線の間に立つ","orbit",0x40eaff,2.8f,2.2f),
  new Kind(3,"sky_guardian_3","風切りの針妃","尾針サソリ","尾針穿孔","足元の印を離れ、尾針の直線から横へ","stalk",0xe668ff,2.2f,2.9f),
  new Kind(4,"sky_guardian_4","グラビティ・ハウンド","刃背の四足猟犬","三連疾走","突進線を横にかわす。三回目まで油断しない","rush",0xff536c,1.5f,2f),
  new Kind(5,"sky_guardian_5","スカイ・バリスタ","双履帯移動砲台","五連迫撃","着弾円を順に避ける","anchor",0xffc44c,2.8f,2.2f),
  new Kind(6,"sky_guardian_6","雲海のクラゲ","発光傘と触手のクラゲ","拡散電環","広がる輪をジャンプで越える","float",0x63ffd9,2f,3.2f),
  new Kind(7,"sky_guardian_7","跳刃マンティス","二枚鎌の機械カマキリ","交差三日月","斜めの斬線を避けて最後は背後へ","strafe",0x96ff48,2f,2.9f),
  new Kind(8,"sky_guardian_8","雷索の大蛇","節状の電磁ヘビ","蛇行放電","蛇行する発光列から離れる","slither",0x65a8ff,1.8f,1.2f),
  new Kind(9,"sky_guardian_9","アクセル・ホイール","巨大一輪と左右刃","轢断車輪","直線をかわして着地点から離れる","rush",0xff6540,2.4f,2.5f),
  new Kind(10,"sky_guardian_10","虹晶の監視眼","三重環の巨大眼球","三角屈折","三角形の照射線を避ける","anchor",0xf582ff,3f,3.8f),
  new Kind(11,"sky_guardian_11","嵐脚ケンタウロス","槍腕の四脚騎兵","蹄撃突貫","最初に距離を取り、突進を横へ避ける","rush",0xe9b66b,2f,3.5f),
  new Kind(12,"sky_guardian_12","音速の翼竜","音響翼のコウモリ","音翼共鳴","左右の翼の間にある前後の隙間へ","orbit",0xb383ff,2.8f,3.3f),
  new Kind(13,"sky_guardian_13","磁殻の守護者","巻き殻の装甲巻貝","螺旋機雷","外側から順番に起爆する円を避ける","anchor",0xf49e63,2f,2.3f),
  new Kind(14,"sky_guardian_14","八腕の航路主","八腕の通信タコ","八方接続","八本の放射線の隙間へ","float",0x44e3b2,2.6f,3f),
  new Kind(15,"sky_guardian_15","落星の鍛冶師","炉心と巨大金床拳","鍛造床打ち","赤い床の帯から安全な列へ移る","stalk",0xff752d,2.8f,3.6f),
  new Kind(16,"sky_guardian_16","逆光の幻蝶","二対の結晶蝶翼","鏡面挟撃","両側から来る線の中央を避ける","orbit",0x97eeff,2.7f,3.1f),
  new Kind(17,"sky_guardian_17","空中庭園の石碑","浮遊石碑と公転角柱","零点方格","格子の線の間に立つ","anchor",0x79c9ff,2.8f,3.8f),
  new Kind(18,"sky_guardian_18","氷橋の突撃羊","巨大角の六角氷装羊","凍角雪崩","足元の凍結円と遅れて来る突進に注意","rush",0xacf4ff,2.3f,2.5f),
  new Kind(19,"sky_guardian_19","天空レール機関","三両連結の装甲列車","軌道転轍","二本の線路を避け、横断砲撃も確認","patrol",0x55bcff,1.7f,2f),
  new Kind(20,"sky_guardian_20","振り子の裁定者","歯車円盤と時計針","時針断層","順番に点灯する時計針の間を移る","anchor",0xffe69c,2.8f,3.5f),
  new Kind(21,"sky_guardian_21","暴風の死鳥","鋼の羽根と鉤爪の禿鷹","急降下狩猟","影の円を避け、羽根の直線から離れる","orbit",0xe87878,2.9f,2.9f),
  new Kind(22,"sky_guardian_22","空中花園の捕食花","肉質花弁と機械根","吸血開花","花弁状の危険円を避けて中心へ","anchor",0xff446e,2.8f,3.3f),
  new Kind(23,"sky_guardian_23","螺旋の掘削獣","採掘ドリルの地底獣","穿地連爆","連続する地割れを横に避ける","slither",0xd89d56,2.7f,1.5f),
  new Kind(24,"sky_guardian_24","鳴動の風鐘","浮遊する鐘と鎖","葬鐘三響","三つの衝撃輪をジャンプで越える","float",0xd77bfa,2f,3.5f),
  new Kind(25,"sky_guardian_25","断空の剣鬼","角面と背負い大太刀の鬼","断罪抜刀","扇状の斬撃を背後へ避け、十字斬りをかわす","strafe",0xff414d,1.8f,4f),
  new Kind(26,"sky_guardian_26","日蝕の甲虫","太陽円盤を背負う甲虫","日輪焦土","外周が光る間は中央、次は中央から離れる","stalk",0xffd94b,2.6f,3.4f),
  new Kind(27,"sky_guardian_27","雲底のエイ","幅広の鰭と長い毒尾のエイ","深淵掃流","左右の帯を避けて中央の追撃もかわす","orbit",0x587cff,2.8f,2f),
  new Kind(28,"sky_guardian_28","三冠の雷竜","三頭の王冠竜","三頭連砲","三本の照準線を順番に横へかわす","anchor",0xb0ff77,2.8f,3.5f),
  new Kind(29,"sky_guardian_29","天頂の六翼","六翼と光輪の天使機関","六翼審判","六方向の光線と足元の印を避ける","float",0xffefbd,3f,4.3f),
  new Kind(30,"sky_guardian_30","天空王アストラ","機械竜と王冠炉心","王域終焉","扇状ブレス、着弾円、衝撃輪の順に対応","stalk",0xff5bba,3f,4.2f),
 };
 private static final String[] MODELS={"boss_clamp_warden","boss_cross_drone","boss_needle_empress","boss_razor_hound","boss_bastion_crawler","boss_jelly_oracle","boss_scythe_mantis","boss_coil_serpent","boss_mono_reaper","boss_prism_eye","boss_iron_centaur","boss_echo_wraith","boss_spiral_hermit","boss_kraken_router","boss_forge_golem","boss_mirror_moth","boss_obelisk_zero","boss_glacier_ram","boss_rail_leviathan","boss_clock_archon","boss_carrion_vulture","boss_blood_bloom","boss_drill_mole","boss_funeral_bell","boss_oni_executioner","boss_solar_scarab","boss_abyss_ray","boss_crown_hydra","boss_seraph_engine","boss_night_sovereign"};
 static String model(Kind k){return MODELS[k.floor()-1];}
}
