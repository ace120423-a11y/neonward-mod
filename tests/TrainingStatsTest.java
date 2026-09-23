package jp.neonward;

/** Standalone: compile TrainingStats, UiCommandLimiter and this class; run jp.neonward.TrainingStatsTest. */
public final class TrainingStatsTest {
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 static void equal(double expected,double actual){check(Math.abs(expected-actual)<.00001,expected+" != "+actual);}
 public static void main(String[] args){
  var a=new TrainingStats();var b=new TrainingStats();
  equal(0,a.dps(0));a.hit(0,10);a.hit(0,5);a.hit(99,20);
  equal(7,a.dps(99));equal(4,a.dps(100));equal(0,a.dps(199));
  equal(35,a.total());equal(20,a.last());check(a.hits()==3,"counts hits even within one tick");
  equal(0,b.total());equal(0,b.dps(99));
  a.hit(100,7);equal(5.4,a.dps(100));equal(1.4,a.dps(199));equal(0,a.dps(200));
  a.hit(101,Double.NaN);a.hit(101,Double.POSITIVE_INFINITY);a.hit(101,-1);a.hit(101,0);
  check(a.hits()==4,"reject nonpositive/nonfinite damage");
  a.reset();equal(0,a.total());equal(0,a.last());equal(0,a.dps(100));check(a.hits()==0,"reset count");
  for(int tick=0;tick<10000;tick++)for(int i=0;i<100;i++)a.hit(tick,1);
  equal(2000,a.dps(9999));equal(1000000,a.total());equal(0,a.dps(10099));
  check(UiCommandLimiter.isUi("neonrange")&&UiCommandLimiter.isUi("neonrange leave"),"command root registered");
  check(!UiCommandLimiter.isUi("neonrangeOther leave"),"root match is exact");
  var limiter=new UiCommandLimiter();check(limiter.accept("neonrange reset",0),"first command accepted");
  check(!limiter.accept("neonrange reset",1),"spam throttled");check(limiter.accept("neonrange leave",150_000_000L),"exit works after normal UI throttle");
  System.out.println("TRAINING_STATS_PASS: rolling boundary, decay, same-tick aggregation, independent players, reset, invalid input, million-hit bounded storage");
 }
}
