package jp.neonward;
/** Separate from cyberware: a kill can yield either, both, or neither. */
public final class WeaponLoot {
 public static final int CHANCE_PERCENT=8;
 public static final int[] MIN={100,115,135,160,200},MAX={110,130,155,190,240};
 public record Quality(int tier,int power){}
 public static Quality quality(java.util.random.RandomGenerator random){return quality(random,LootProfile.FIELD);}
 public static Quality quality(java.util.random.RandomGenerator random,LootProfile profile){int t=profile.tier(random);return new Quality(t,random.nextInt(MIN[t],MAX[t]+1));}
 public static int roll(java.util.random.RandomGenerator random,int size){return roll(random,size,LootProfile.FIELD);}
 public static int roll(java.util.random.RandomGenerator random,int size,LootProfile profile){return roll(random,size,profile,0);}
 public static int roll(java.util.random.RandomGenerator random,int size,LootProfile profile,int looting){if(size<=0||random.nextInt(100)>=Math.min(100,profile.weapons+2*Math.clamp(looting,0,3)))return -1;return random.nextInt(size);}
}
