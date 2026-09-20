package jp.neonward;
import java.util.*;
/** Persistent player-owned progression. All prices and transitions are server-owned. */
public final class StreetProgress {
 public static final class Tune {public int engine,handling,cargo,paint,neon;}
 public Map<String,Tune> vehicles=new HashMap<>();
 public int reputation,heat,mission=-1,stage,escapeSeconds;public long deadline,cooldown;public String recovery="";
 public int targetX,targetZ;
 public Tune tune(String id){return vehicles.computeIfAbsent(id,k->new Tune());}
 public static int price(Tune t,String upgrade){return switch(upgrade){case "engine"->t.engine>=3?0:1500*(t.engine+1);case "handling"->t.handling>=3?0:1000*(t.handling+1);case "cargo"->t.cargo>=3?0:1200*(t.cargo+1);case "paint"->500;case "neon"->750;default->throw new IllegalArgumentException("改造項目を選んでください");};}
 public static long upgrade(Tune t,String kind,long cash,boolean maxCargo){int cost=price(t,kind);if(cost==0||kind.equals("cargo")&&maxCargo)throw new IllegalArgumentException("この項目は上限です");if(cash<cost)throw new IllegalArgumentException("残高が足りません");switch(kind){case "engine"->t.engine++;case "handling"->t.handling++;case "cargo"->t.cargo++;case "paint"->t.paint=(t.paint+1)%5;case "neon"->t.neon=(t.neon+1)%4;}return cash-cost;}
 public int rank(){return Math.min(5,reputation/5);}
 public long reward(){return (mission==1?2400:mission==2?2000:1400)+rank()*400L;}
 public void clear(){mission=-1;stage=0;deadline=0;escapeSeconds=0;recovery="";}
 public void accept(int type,long now){if(type<0||type>2)throw new IllegalArgumentException("依頼を選んでください");if(mission>=0)throw new IllegalArgumentException("今の依頼を終えてください");if(now<cooldown)throw new IllegalArgumentException("次の仕事は少し待ってください");mission=type;stage=0;deadline=now+15*60_000;heat=0;escapeSeconds=0;}
 public long claim(long now){if(mission<0||stage!=2||heat>0)throw new IllegalArgumentException("追跡を振り切り、目標を完了してから報告してください");if(now>deadline)throw new IllegalArgumentException("制限時間を過ぎました");long money=reward();reputation++;clear();cooldown=now+30_000;return money;}
}
