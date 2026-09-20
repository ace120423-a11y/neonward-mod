package jp.neonward;
import java.util.*;
/** Expansive sequential mechanism halls. */
public final class DungeonLayout {
 public static final int SIZE=117,DEPTH=181,FLOORS=30,STEP=24;
 public static int base(int floor){return 64+(floor-1)*STEP;}
 public static boolean[][] plan(int floor){
  boolean[][] open=new boolean[SIZE][DEPTH];
  for(int x=1;x<SIZE-1;x++)for(int z=1;z<DEPTH-1;z++)open[x][z]=true;
  for(int stage=0;stage<3;stage++){
   int start=24+stage*50;
   for(int x=1;x<116;x++)for(int z=start;z<=start+26;z++)open[x][z]=false;
   int[][] route={{59,0},{59,7},{30,7},{30,18},{87,18},{87,26},{59,26}};
   for(int i=1;i<route.length;i++){
    int x0=mirror(floor,stage,route[i-1][0]),x1=mirror(floor,stage,route[i][0]);
    for(int x=Math.min(x0,x1)-3;x<=Math.max(x0,x1)+3;x++)for(int z=start+Math.min(route[i-1][1],route[i][1])-3;z<=start+Math.max(route[i-1][1],route[i][1])+3;z++)if(x>0&&x<116&&z>start&&z<start+26)open[x][z]=true;
   }
   // Optional maintenance alcoves branch away from the route without skipping a lock.
   int branch=mirror(floor,stage,30);
   for(int x=branch-13;x<=branch+3;x++)for(int z=start+11;z<=start+15;z++)if(x>0&&x<116)open[x][z]=true;
   for(int x=54;x<=64;x++){open[x][start]=true;open[x][start+26]=true;}
  }
  return open;
 }
 static int mirror(int floor,int stage,int x){return (floor+stage)%2==0?x:118-x;}
 static int corridor(double z){for(int i=0;i<3;i++)if(z>24+i*50&&z<50+i*50)return i;return -1;}
 static int[][] patrol(int floor,int stage){int z=24+stage*50;return new int[][]{{mirror(floor,stage,40),z+7},{mirror(floor,stage,30),z+13},{mirror(floor,stage,60),z+18},{mirror(floor,stage,87),z+22}};}
 public static int floor(double y){return Math.max(1,Math.min(FLOORS,1+((int)y-65)/STEP));}
 public static final String[] THEMES={"青磁回路層","紫電通信層","黄銅工業層","氷晶記憶層","紅蓮隔離層","黄金中枢層"};
 public static int theme(int floor){return (floor-1)/5;}
}
