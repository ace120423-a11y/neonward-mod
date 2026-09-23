package jp.neonward;

import java.util.Arrays;

/** One bucket per server tick: constant memory even for shotgun/AOE bursts. */
public final class TrainingStats {
 public static final int WINDOW=100;
 private final long[] ticks=new long[WINDOW];
 private final double[] damage=new double[WINDOW];
 private double total,last;
 private long hits;
 public TrainingStats(){reset();}
 public void reset(){Arrays.fill(ticks,Long.MIN_VALUE);Arrays.fill(damage,0);total=last=0;hits=0;}
 public void hit(long tick,double amount){
  if(!Double.isFinite(amount)||amount<=0)return;
  int slot=Math.floorMod(tick,WINDOW);
  if(ticks[slot]!=tick){ticks[slot]=tick;damage[slot]=0;}
  damage[slot]+=amount;total+=amount;last=amount;hits++;
 }
 /** Fixed five-second denominator; decays to zero after 100 server ticks. */
 public double dps(long now){double sum=0;for(int i=0;i<WINDOW;i++)if(ticks[i]<=now&&ticks[i]>now-WINDOW)sum+=damage[i];return sum/5;}
 public double total(){return total;}
 public double last(){return last;}
 public long hits(){return hits;}
}
