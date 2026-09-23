package jp.neonward;

import java.util.*;
import com.google.gson.Gson;

/** Run without a server or world. Includes save failure and legacy JSON migration. */
public final class PetPurchaseTest {
 static class Wallet implements PetPurchase.Account {
  long cash=10000;Set<Integer> petOwned=new HashSet<>();
  public long cash(){return cash;}public void cash(long value){cash=value;}
  public Set<Integer> pets(){return petOwned;}public void pets(Set<Integer> value){petOwned=value;}
 }
 static int checks;
 static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
 public static void main(String[] args){
  Gson json=new Gson();Wallet a=json.fromJson("{\"cash\":10000}",Wallet.class);
  MarketLedger market=new MarketLedger();var real=market.account("first-player");real.cash=8200;real.petOwned.add(0);
  var reopened=json.fromJson(json.toJson(market),MarketLedger.class);reopened.validate();
  check(reopened.account("first-player").cash==8200&&reopened.account("first-player").petOwned.equals(Set.of(0)),"actual market persistence includes ownership and cash");
  check(reopened.account("second-player").petOwned.isEmpty(),"actual market ownership isolation");
  var legacy=json.fromJson("{\"cash\":10000}",MarketLedger.Account.class);check(legacy.petOwned.isEmpty(),"actual legacy account migration");
  real.petOwned.add(5);boolean rejected=false;try{market.validate();}catch(IllegalStateException expected){rejected=true;}check(rejected,"market rejects invalid pet kind");
  check(a.petOwned!=null&&a.petOwned.isEmpty(),"legacy account defaults to no pets");
  final String[] disk={json.toJson(a)};final int[] saves={0};
  PetPurchase.Save save=()->{disk[0]=json.toJson(a);saves[0]++;};
  check(PetPurchase.buy(a,0,save)==1,"dog purchase");
  Wallet loaded=json.fromJson(disk[0],Wallet.class);
  check(loaded.cash==8200&&loaded.petOwned.equals(Set.of(0)),"cash and pet share persisted snapshot");
  check(PetPurchase.buy(a,0,save)==2&&a.cash==8200&&saves[0]==1,"duplicate click cannot double debit");
  check(PetPurchase.buy(a,-1,save)==-1&&PetPurchase.buy(a,5,save)==-1&&saves[0]==1,"invalid ids cannot save or charge");
  Set<Integer> before=a.petOwned;long cash=a.cash;String persisted=disk[0];
  check(PetPurchase.buy(a,4,()->{throw new java.io.IOException("atomic replace failed");})==-4,"save failure reported");
  check(a.cash==cash&&a.petOwned==before&&!a.petOwned.contains(4)&&persisted.equals(disk[0]),"failed save restores exact prior account");
  a.cash=1799;check(PetPurchase.buy(a,1,save)==-3&&a.cash==1799&&!a.petOwned.contains(1),"insufficient cash");
  a.cash=1800;check(PetPurchase.buy(a,1,save)==1&&a.cash==0,"exact cash balance");
  a.petOwned=null;a.cash=10000;check(PetPurchase.buy(a,2,save)==1&&a.petOwned.equals(Set.of(2)),"explicit legacy null migrates");
  a.petOwned=null;cash=a.cash;check(PetPurchase.buy(a,3,()->{throw new Exception();})==-4&&a.petOwned==null&&a.cash==cash,"null rollback");
  a.cash=Long.MAX_VALUE;check(PetPurchase.buy(a,4,save)==1&&a.cash==Long.MAX_VALUE-4500,"large balance does not overflow");
  Wallet other=new Wallet();check(!other.petOwned.contains(4),"ownership is per account");
  for(int kind=0;kind<5;kind++){Wallet buyer=new Wallet();check(PetPurchase.buy(buyer,kind,()->{})==1&&buyer.petOwned.equals(Set.of(kind))&&buyer.cash==10000-PetPurchase.PRICES[kind],"catalog "+kind);}
  System.out.println("PASS PetPurchaseTest: "+checks+" checks");
 }
}
