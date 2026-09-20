package jp.neonward;
import java.util.*;
/** Geometry and rules are shared by the solution check, hints and visible machinery. */
final class MechanismRules {
 static final int PIPE=0,RACK=1,STEAM=2,LIGHT=3;
 static int kind(int floor,int stage){if(floor==1)return -1;int n=((floor-2)*3+stage)%7;return n<4?n:-1;}
 static int seed(int f,int s){return (f+s)%4;}
 static int[] initial(int kind,int f,int s){
  if(kind==STEAM)return new int[]{3,3,0,0};
  if(kind==RACK){int[] a={1,1,1,1};for(int i=0;i<4;i++)for(int j=0;j<1+(f+s+i)%3;j++)operate(kind,a,i);if(Arrays.stream(a).allMatch(v->v==1))operate(kind,a,0);return a;}
  return new int[4];
 }
 static void operate(int kind,int[] a,int i){
  if(i<0||i>=4)return;
  if(kind==PIPE)a[i]=(a[i]+1)%4;
  if(kind==RACK){a[i]=(a[i]+1)%3;a[(i+1)%4]=(a[(i+1)%4]+1)%3;}
  if(kind==STEAM){int donor=(i+1)%4;if(a[i]<3&&a[donor]>0){a[i]++;a[donor]--;}}
  if(kind==LIGHT)a[i]^=1;
 }
 static int[] stair(int f,int s){return (f+s)%2==0?new int[]{0,1,2,3}:new int[]{3,2,1,0};}
 static boolean solved(int kind,int[] a,int f,int s){return switch(kind){
  case PIPE -> pipeFlow(a,f,s)==4;
  case RACK -> Arrays.stream(a).allMatch(v->v==1);
  case STEAM -> Arrays.equals(a,stair(f,s));
  case LIGHT -> light(a,f,s).hit;
  default -> false;
 };}
 static int mask(int index,int rotation){int bits=index==0?5:3;for(int n=0;n<rotation;n++)bits=((bits<<1)&15)|(bits>>3);return bits;}
 static int[] rotate(int x,int y,int n){while(n-->0){int old=x;x=y;y=-old;}return new int[]{x,y};}
 static final int[][] PIPE_POS={{-1,1},{1,1},{1,-1},{-1,-1}};
 static int pipeFlow(int[] a,int f,int s){
  int seed=seed(f,s);int[] dirs={1,2,3,2},entry={3,3,0,1};
  for(int i=0;i<4;i++){int m=mask(i,a[i]);if((m&(1<<((entry[i]+seed)%4)))==0||(m&(1<<((dirs[i]+seed)%4)))==0)return i;}
  return 4;
 }
 static final int[][] LIGHT_POS={{-1,-1},{-1,1},{1,1},{1,-1}};
 record Ray(int x1,int y1,int x2,int y2){}
 record Beam(List<Ray> rays,boolean hit,int mirrors){}
 static Beam light(int[] a,int f,int s){
  int seed=seed(f,s),dir=(1+seed)%4;int[] start=rotate(-3,-1,seed);int x=start[0],y=start[1];List<Ray> rays=new ArrayList<>();Set<String> seen=new HashSet<>();int mirrors=0;
  for(int n=0;n<16;n++){
   int dx=new int[]{0,1,0,-1}[dir],dy=new int[]{1,0,-1,0}[dir],best=99,index=-1,nx=x,ny=y;
   for(int i=0;i<4;i++){int[] p=rotate(LIGHT_POS[i][0],LIGHT_POS[i][1],seed);int d=(p[0]-x)*dx+(p[1]-y)*dy;if(d>0&&((dx==0&&p[0]==x)||(dy==0&&p[1]==y))&&d<best){best=d;index=i;nx=p[0];ny=p[1];}}
   if(index<0){nx=dx==0?x:dx*3;ny=dy==0?y:dy*3;rays.add(new Ray(x,y,nx,ny));int[] end=rotate(1,-1,seed);return new Beam(rays,x==end[0]&&y==end[1]&&dir==(1+seed)%4,mirrors);}
   rays.add(new Ray(x,y,nx,ny));String key=index+":"+dir;if(!seen.add(key))return new Beam(rays,false,mirrors);
   x=nx;y=ny;mirrors++;dir=(a[index]==0?new int[]{1,0,3,2}:new int[]{3,2,1,0})[dir];
  }
  return new Beam(rays,false,mirrors);
 }
 static String title(int k){return new String[]{"配管の接続","連動歯車ラック","蒸気リフトの均衡","反射光の誘導"}[k];}
 static String hint(int k,int f,int s){return switch(k){
  case PIPE -> "管を90度ずつ回す\n入口から出口まで接続\n流れが止まる継ぎ目を探す";
  case RACK -> "操作した軸と次の軸が連動\n全ての歯車を中央の軸線へ\n回転が伝わる位置を探す";
  case STEAM -> "次の足場から圧力を1移す\n目標高さ："+Arrays.toString(stair(f,s))+"\n空のタンクからは移せない";
  default -> "鏡を切り替えて光を反射\n金色の光を受光器まで導く\n光が外れた鏡から調整";
 };}
}
