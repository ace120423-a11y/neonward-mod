package jp.neonward;
public enum LootProfile {
 FIELD(8,15,new int[]{60,25,10,4,1}),DUNGEON(16,30,new int[]{60,25,10,4,1}),BOSS(60,80,new int[]{10,20,40,25,5});
 public final int weapons,cyberware;private final int[] tiers;
 LootProfile(int weapons,int cyberware,int[] tiers){this.weapons=weapons;this.cyberware=cyberware;this.tiers=tiers;}
 public int tier(java.util.random.RandomGenerator random){int n=random.nextInt(100);for(int i=0;i<tiers.length;i++){n-=tiers[i];if(n<0)return i;}throw new AssertionError();}
}
