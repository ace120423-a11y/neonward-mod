package jp.neonward;
public final class GuildCatalog {
 public static final String[] MATERIAL_IDS={"scrap_metal","circuit_shard","synthetic_fiber","servo_joint","optical_lens","reactor_cell","corrupted_chip","guardian_core"};
 public static final String[] MATERIAL_NAMES={"回収金属片","電子基板片","人工繊維","駆動ジョイント","光学レンズ","反応炉セル","破損暗号チップ","守護者コア"};
 public static final int[] PRICES={12,28,18,45,65,90,120,400};
 public record Quest(String title,int target,int reward,String condition){}
 public static final Quest[] QUESTS={new Quest("外域の治安回復",10,500,"探索エリアの敵を10体倒す"),new Quest("森林の掃討",6,450,"森林バイオームの敵を6体倒す"),new Quest("雪原の掃討",6,550,"雪原バイオームの敵を6体倒す"),new Quest("荒野の掃討",6,450,"荒野バイオームの敵を6体倒す"),new Quest("塔の巡回任務",12,900,"ダンジョン内の敵を12体倒す"),new Quest("階層守護者の討伐",1,1500,"ダンジョンの階層ボスを1体倒す"),new Quest("薬草の採取",4,200,"外域の薬草を4株採取する")};
 public static boolean matches(int quest,boolean wild,boolean dungeon,boolean boss,String habitat){return switch(quest){case 0->wild;case 1->wild&&habitat.equals("森林");case 2->wild&&habitat.equals("雪原");case 3->wild&&habitat.equals("荒野");case 4->dungeon;case 5->dungeon&&boss;default->false;};}
}
