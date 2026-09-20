package jp.neonward;
import java.util.*;
/** Physical implants: one per body slot, no capacity budget. */
public final class CyberwareCatalog {
 public static final String[] SLOTS={"前頭葉","腕","骨格","神経系","外皮","OS","視覚","手","循環器","脚"};
 public static final String[] STATS={"最大HP","防御力","攻撃力","移動速度","攻撃速度","ノックバック耐性","採掘速度","ジャンプ力","幸運"};
 public static final String[] RARITIES={"コモン","アンコモン","レア","エピック","レジェンダリー"};
 public static final int[] COLORS={0xffbdc8cf,0xff62df96,0xff61b8ff,0xffce84ff,0xffffb94f};
 public static final String[] BASE_IDS={"cortex","arms","skeleton","nerves","skin","os","optics","hands","heart","legs"};
 public static final String[] NAMES={"コア・リンク","筋束サーボ","チタン骨格","反射ブースター","皮下プレート","PULSE OS","解析レンズ","グリップ安定器","バイタルハート","強化腱"};
 public static final String[] FAMILIES={"攻撃特化","HP特化","防御特化","移動特化"};
 public static final String[] FAMILY_IDS={"attack","health","armor","speed"};
 public record Part(String id,String name,int slot,int tier,int family,int stat,double amount,String effect){}
 public static final Part[] PARTS=new Part[200];
 public static final int[] MIN={100,400,700,1100,1600},MAX={300,600,1000,1500,2200};
 static {int[] stat={2,0,1,3};for(int s=0;s<10;s++)for(int f=0;f<4;f++)for(int t=0;t<5;t++){String effect=STATS[stat[f]]+" +"+percent(MIN[t])+"〜"+percent(MAX[t])+"%";PARTS[index(s,f,t)]=new Part("cyber_"+BASE_IDS[s]+"_"+FAMILY_IDS[f]+"_"+t,NAMES[s]+"・"+FAMILIES[f],s,t,f,stat[f],MIN[t]/10000.0,effect);}}
 public static String percent(int bp){return java.math.BigDecimal.valueOf(bp,2).stripTrailingZeros().toPlainString();}
 public static int clampRoll(int id,int roll){int t=PARTS[id].tier();return Math.max(MIN[t],Math.min(MAX[t],roll));}
 public static int rollValue(int id,java.util.random.RandomGenerator rng){int t=PARTS[id].tier();return rng.nextInt((MAX[t]-MIN[t])/10+1)*10+MIN[t];}
 public static String effect(int id,int roll){return STATS[PARTS[id].stat()]+" +"+percent(clampRoll(id,roll))+"%";}
 public static int index(int slot,int family,int tier){return slot*20+family*5+tier;}
 public static void normalize(MarketLedger.Account a){if(a.cyberRolls==null)a.cyberRolls=new HashMap<>();if(a.cyberSlots==null)a.cyberSlots=new HashMap<>();if(a.cyberOwned==null)a.cyberOwned=new HashSet<>();if(a.cyberPending==null)a.cyberPending=new ArrayList<>();a.cyberSlots.entrySet().removeIf(e->e.getKey()==null||e.getValue()==null||e.getValue()<0||e.getValue()>=PARTS.length||PARTS[e.getValue()].slot()!=e.getKey());a.cyberPending.removeIf(i->i==null||i<0||i>=PARTS.length);a.cyberRolls.keySet().retainAll(a.cyberSlots.keySet());for(var e:a.cyberSlots.entrySet())a.cyberRolls.put(e.getKey(),clampRoll(e.getValue(),a.cyberRolls.getOrDefault(e.getKey(),MIN[PARTS[e.getValue()].tier()])));}
 public static boolean migrate(MarketLedger.Account a){if(a.cyberVersion>=3){normalize(a);return false;}if(a.cyberRolls==null)a.cyberRolls=new HashMap<>();if(a.cyberSlots==null)a.cyberSlots=new HashMap<>();if(a.cyberOwned==null)a.cyberOwned=new HashSet<>();if(a.cyberPending==null)a.cyberPending=new ArrayList<>();if(a.cyberVersion==2){a.cyberVersion=3;normalize(a);return true;}var oldSlots=new HashMap<>(a.cyberSlots);a.cyberSlots.clear();int version=a.cyberVersion;for(var e:oldSlots.entrySet()){Integer old=e.getValue();int limit=version==0?20:50,per=version==0?2:5;if(old!=null&&old>=0&&old<limit&&e.getKey()!=null&&old/per==e.getKey())a.cyberSlots.put(e.getKey(),legacy(old,version));}if(version==0){for(Integer old:a.cyberOwned)if(old!=null&&old>=0&&old<20&&!oldSlots.containsValue(old))a.cyberPending.add(legacy(old,0));}else{a.cyberPending.replaceAll(i->i!=null&&i>=0&&i<50?legacy(i,1):-1);}a.cyberOwned.clear();a.cyberVersion=3;normalize(a);return true;}
 public static int rollDrop(java.util.random.RandomGenerator rng){return rollDrop(rng,LootProfile.FIELD);}
 public static int rollDrop(java.util.random.RandomGenerator rng,LootProfile profile){return rollDrop(rng,profile,0);}
 public static int rollDrop(java.util.random.RandomGenerator rng,LootProfile profile,int looting){if(rng.nextInt(100)>=Math.min(100,profile.cyberware+3*Math.clamp(looting,0,3)))return -1;int tier=profile.tier(rng);return index(rng.nextInt(10),rng.nextInt(4),tier);}
 static int legacy(int old,int version){int slot=old/(version==0?2:5),tier=version==0?(old%2==0?1:3):old%5;int[] family={1,0,1,3,2,3,0,2,1,3};return index(slot,family[slot],tier);}
 public static double[] bonuses(MarketLedger.Account a){double[] b=new double[STATS.length];for(int i:a.cyberSlots.values()){var p=PARTS[i];b[p.stat()]+=a.cyberRolls.getOrDefault(p.slot(),MIN[p.tier()])/10000.0;}return b;}
}
