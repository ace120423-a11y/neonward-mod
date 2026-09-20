package jp.neonward;
import java.util.*;
/** Server rules; the saved deck and hand survive closing the screen and restarts. */
public final class CasinoGames {
 public static final int BET=100;
 public static class Hand {public List<Integer> deck=new ArrayList<>(),player=new ArrayList<>(),dealer=new ArrayList<>();public int cursor,payout;public boolean active=true;public String result="";}
 static int value(int card){return card%13==0?11:Math.min(10,card%13+1);}
 static int total(List<Integer> cards){int n=0,aces=0;for(int c:cards){n+=value(c);if(c%13==0)aces++;}while(n>21&&aces-->0)n-=10;return n;}
 static int draw(Hand h){return h.deck.get(h.cursor++);}
 static Hand deal(Random r){Hand h=new Hand();for(int i=0;i<52;i++)h.deck.add(i);Collections.shuffle(h.deck,r);h.player.add(draw(h));h.dealer.add(draw(h));h.player.add(draw(h));h.dealer.add(draw(h));if(total(h.player)==21||total(h.dealer)==21){h.active=false;h.payout=total(h.player)==total(h.dealer)?100:total(h.player)==21?250:0;h.result=h.payout==250?"ブラックジャック！":h.payout==100?"引き分け":"ディーラーのブラックジャック";}return h;}
 static void hit(Hand h){if(!h.active)return;h.player.add(draw(h));if(total(h.player)>21){h.active=false;h.payout=0;h.result="バースト";}else if(total(h.player)==21)stand(h);}
 static void stand(Hand h){if(!h.active)return;while(total(h.dealer)<17)h.dealer.add(draw(h));int p=total(h.player),d=total(h.dealer);h.payout=d>21||p>d?200:p==d?100:0;h.result=h.payout==200?"あなたの勝ち":h.payout==100?"引き分け":"ディーラーの勝ち";h.active=false;}
 static String cards(List<Integer> cards,boolean hidden){StringBuilder b=new StringBuilder();String[] ranks={"A","2","3","4","5","6","7","8","9","10","J","Q","K"};String[] suits={"S","H","D","C"};for(int i=0;i<cards.size();i++){if(i>0)b.append("  ");int c=cards.get(i);b.append(hidden&&i>0?"[??]":"["+ranks[c%13]+suits[c/13]+"]");}return b.toString();}
 static final Set<Integer> RED=Set.of(1,3,5,7,9,12,14,16,18,19,21,23,25,27,30,32,34,36);
 static int roulettePayout(int number,String bet,int chosen){if(number<0||number>36)throw new IllegalArgumentException();boolean win=switch(bet){case "red"->RED.contains(number);case "black"->number!=0&&!RED.contains(number);case "odd"->number%2==1;case "even"->number!=0&&number%2==0;case "number"->chosen==number;default->false;};return win?(bet.equals("number")?3600:200):0;}
 public static final List<String> PRIZES=List.of("duck","teapot","gyro","coffin");
 static String award(MarketLedger.Account a,Random r){if(r.nextInt(100)!=0)return "";String id=PRIZES.get(r.nextInt(PRIZES.size()));if(a.casinoVehicles==null)a.casinoVehicles=new HashSet<>();if(a.casinoVehicles.add(id))return "景品当選！ "+VehicleCatalog.get(id).name()+"をガレージに登録";a.cash+=1000;return "景品重複ボーナス +1,000 Cr";}
 static boolean unlocked(MarketLedger.Account a,String id){return !PRIZES.contains(id)||a!=null&&a.casinoVehicles!=null&&a.casinoVehicles.contains(id);}
}
