package jp.neonward;
import java.util.*;
public final class HousingSalesIntegration {
 static void check(boolean b,String why){if(!b)throw new AssertionError("HOUSING: "+why);}
 static void rejected(Runnable r){try{r.run();}catch(IllegalArgumentException e){return;}throw new AssertionError("Expected ownership/replay rejection");}
 public static void run(){
  var l=new MarketLedger();String id=UUID.randomUUID().toString(),other=UUID.randomUUID().toString();var a=l.account(id);a.cash=100000;
  CityApartments.buy(l,id,3);long before=a.cash;HousingSales.sell(l,id,true,0);
  check(a.cash==before+20000&&a.cityFloor==0&&CityApartments.vacant(l,3),"city full refund and vacant");
  rejected(()->HousingSales.sell(l,id,true,0));check(a.cash==100000,"no duplicate refund");
  l.account(other).cash=20000;CityApartments.buy(l,other,3);CityApartments.buy(l,id,4);
  rejected(()->HousingSales.sell(l,id,true,0));check(a.cityFloor==4&&l.account(other).cityFloor==3,"stale sale cannot sell repurchase or another owner");
  HousingSales.sell(l,id,true,1);check(a.cash==100000,"buy another floor then sell");
  a.cityFloor=20;HousingSales.sell(l,id,true,2);check(HousingSales.state(l,id).founderReleased&&CityApartments.vacant(l,20),"founder release persists");CityApartments.buy(l,id,20);check(a.cityFloor==20,"20F can be repurchased");
  PrivateHomes.purchase(l,id);int slot=a.homeSlot;a.homeReady=true;long cash=a.cash;
  HousingSales.sell(l,id,false,3);check(a.homeSlot==0&&a.cash==cash+10000&&a.homeReady,"private refund without deleting room");
  var loaded=StockMarket.JSON.fromJson(StockMarket.JSON.toJson(l),MarketLedger.class);a=loaded.account(id);
  PrivateHomes.purchase(loaded,id);check(a.homeSlot==slot&&a.homeReady&&a.cash==cash,"repurchase same room without respawning furniture");
  var empty=new MarketLedger();rejected(()->HousingSales.sell(empty,other,false,0));
  var live=StockMarket.ledger;var file=StockMarket.file;
  try{StockMarket.ledger=loaded;String snapshot=StockMarket.JSON.toJson(loaded);StockMarket.file=file.getParent();boolean failed=false;
   try{HousingSales.sell(loaded,id,false,HousingSales.state(loaded,id).revision);StockMarket.save();}catch(Exception e){failed=true;StockMarket.ledger=StockMarket.JSON.fromJson(snapshot,MarketLedger.class);}
   check(failed&&StockMarket.ledger.account(id).homeSlot==slot&&StockMarket.ledger.account(id).cash==cash,"failed save restores ownership and money");
   StockMarket.ledger.validate();
  }finally{StockMarket.ledger=live;StockMarket.file=file;}
  System.out.println("HOUSING_SALES_PASS: full refunds, repurchase, replay, isolation, founder, persistence, no regeneration, save rollback");
 }
}
