package jp.neonward;
/** Thirty silhouettes from the approved volcano concept, numbered by floor. */
final class VolcanoRoster {
 static final String[] NAMES={"熔角の大甲虫","灼尾のサソリ","火鱗のサラマンダー","灰牙の狼","黒曜の大猪","煙殻の宿蟹","黒翼のコウモリ","硫黄の大蛇","熔角の山羊","背山の大亀","熔鉄の槍騎士","噴煙の魔人","歩く溶鉱炉","黒曜の大蜘蛛","鍛冶のケンタウロス","鎖鐘の守護者","灰燼の司祭","鋸輪の処刑人","牛頭の門番","四腕の神殿守護者","灰翼の竜","白金の不死鳥","溶岩の投石巨人","浮遊する火核碑","地潜りの大百足","六翼の火蛾","三頭の熔岩猟犬","火山の皇帝","角冠の古竜","火山の主・カルデラ"};
 static final String[] FORMS={"beetle","scorpion","salamander","wolf","boar","crab","bat","cobra","ram","turtle","knight","djinn","furnace","spider","centaur","bell","priest","saw","bull","guardian","dragon","phoenix","giant","monolith","centipede","moth","hound","emperor","wyrm","sovereign"};
 // Modes: cleave, slam, flame breath, visible fireball fan, charge, lava boulder.
 static final int[] MODES={0,1,2,4,4,3,3,2,4,5,0,3,2,1,1,1,3,4,0,1,2,3,5,3,4,3,2,0,2,5};
 // Body collision, excluding decorative wings, polearms and widely spread claws.
 static final float[] WIDTH={2.2f,2,1.6f,1.3f,2,2.2f,1,1.6f,1.5f,3,1.2f,1.2f,2.4f,2,1.8f,2.3f,1.2f,2.6f,1.9f,2.4f,2,1.2f,2.8f,2,1.7f,1.1f,2,1.8f,2,4};
 static final float[] HEIGHT={1.6f,3.3f,1.3f,2.9f,2.5f,2.7f,3.4f,3.2f,2.9f,2.5f,3.5f,3.5f,4.1f,2.7f,3.8f,2.8f,4.6f,2.7f,4.1f,4,3.7f,3.9f,4.1f,4.5f,4.1f,3.6f,2.9f,5.2f,5.1f,6.5f};
 static String id(int f){return "volcano_boss_"+f;}
 static double hp(int f){return 550+f*95+(f%10==0?600:0);}
 static double damage(int f){return 5+f*.32;}
 static boolean flying(int f){return f==7||f==12||f==21||f==22||f==24||f==26;}
}
