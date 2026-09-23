package jp.neonward;

import java.util.HashSet;
import java.util.Set;

/** Small transaction boundary: the caller persists this state and cash in the SAME atomic save. */
final class PetPurchase {
 static final long[] PRICES={1800,1800,2400,2800,4500};
 static final int OK=1,ALREADY_OWNED=2,INVALID=-1,UNAVAILABLE=-2,NO_FUNDS=-3,SAVE_FAILED=-4,NOT_OWNED=-5,NO_SPACE=-6;
 interface Account {long cash();void cash(long value);Set<Integer> pets();void pets(Set<Integer> value);}
 interface Save {void run() throws Exception;}
 static boolean valid(int kind){return kind>=0&&kind<PRICES.length;}
 static int buy(Account account,int kind,Save save){
  if(!valid(kind))return INVALID;
  Set<Integer> old=account.pets();
  if(old!=null&&old.contains(kind))return ALREADY_OWNED;
  long cash=account.cash();if(cash<PRICES[kind])return NO_FUNDS;
  Set<Integer> next=old==null?new HashSet<>():new HashSet<>(old);next.add(kind);
  account.cash(cash-PRICES[kind]);account.pets(next);
  try{save.run();return OK;}
  catch(Exception failure){account.cash(cash);account.pets(old);return SAVE_FAILED;}
 }
 private PetPurchase(){}
}
