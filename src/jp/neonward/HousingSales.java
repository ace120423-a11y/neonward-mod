package jp.neonward;

/** Ownership changes only: never regenerate or delete a furnished room. */
public final class HousingSales {
 public static class State {public long revision;public int retiredHome;public boolean founderReleased;}
 static State state(MarketLedger l,String id){return l.housingSales.computeIfAbsent(id,k->new State());}
 static String sell(MarketLedger l,String id,boolean city,long revision){
  var a=l.account(id);var s=state(l,id);
  if(revision!=s.revision)throw new IllegalArgumentException("所有情報が更新されています。画面を開き直してください");
  if((city?a.cityFloor:a.homeSlot)<=0)throw new IllegalArgumentException("売却できる自分の家がありません");
  long cash=Math.addExact(a.cash,city?CityApartments.PRICE:PrivateHomes.PRICE);
  long next=Math.incrementExact(s.revision);
  if(city){a.cityFloor=0;s.founderReleased=true;}else{s.retiredHome=a.homeSlot;a.homeSlot=0;}
  a.cash=cash;s.revision=next;
  return "売却しました / "+(city?CityApartments.PRICE:PrivateHomes.PRICE)+" Cr を返金";
 }
}
