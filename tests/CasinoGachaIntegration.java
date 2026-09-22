package jp.neonward;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.core.component.DataComponents;

public final class CasinoGachaIntegration {
 static void check(boolean b,String why){if(!b)throw new AssertionError("GACHA: "+why);}
 static void clear(ServerPlayer p){for(int i=0;i<36;i++)p.getInventory().setItem(i,ItemStack.EMPTY);}
 static int token(ServerPlayer p,boolean weapon){CasinoGacha.request(p,weapon,false,0,1);return CasinoGacha.SESSIONS.get(p.getUUID()).token();}
 static int count(ServerPlayer p){int count=0;for(int i=0;i<36;i++)count+=p.getInventory().getItem(i).getCount();return count;}
 public static void run(ServerPlayer p){var inventory=Cyberware.inventory(p);var pos=p.position();var file=StockMarket.file;String ledger=StockMarket.JSON.toJson(StockMarket.ledger);
  try{
   check(UiCommandLimiter.isUi("neonweapongacha roll 42 10"),"weapon gacha uses non-kicking UI throttle");
   int[] weights=new int[5];for(int i=0;i<1000;i++)weights[CyberwareGacha.rarity(i)]++;check(Arrays.equals(weights,new int[]{419,300,200,80,1}),"exact cyberware odds");
   check(CasinoGacha.weapons().size()==24&&!CasinoGacha.weapons().contains(NeonShield.ITEM),"24 damage-rated weapons, no armor or shield");
   for(int roll=0;roll<1000;roll++){final int n=roll;var item=CasinoGacha.weapon(new Random(17){@Override public int nextInt(int bound){return bound==1000?n:super.nextInt(bound);}});var data=item.get(DataComponents.CUSTOM_DATA).copyTag();check(data.getIntOr("neon_weapon_tier",-1)==CyberwareGacha.rarity(roll),"weapon shares exact rarity mapping");int tier=data.getIntOr("neon_weapon_tier",-1),power=data.getIntOr("neon_weapon_power",-1);check(power>=WeaponLoot.MIN[tier]&&power<=WeaponLoot.MAX[tier],"quality range");}
   for(boolean weapon:new boolean[]{false,true}){
    p.setPos(weapon?518:514,65,449);clear(p);CyberwareGacha.ends=0;WeaponGacha.ends=0;var a=StockMarket.ledger.account(p.getStringUUID());a.cash=50000;
    int token=token(p,weapon);check(a.cash==50000&&count(p)==0,"opening free");
    check(CasinoGacha.request(p,weapon,true,token,10)==10&&a.cash==40000&&count(p)==10,"ten prizes and exact debit");
    check(CasinoGacha.request(p,weapon,true,token,10)==0&&a.cash==40000&&count(p)==10,"replay cannot charge twice");
    int next=CasinoGacha.SESSIONS.get(p.getUUID()).token();check(CasinoGacha.request(p,weapon,true,next,10)==0&&a.cash==40000,"animation lock");
    CyberwareGacha.ends=0;WeaponGacha.ends=0;clear(p);
    check(CasinoGacha.request(p,weapon,true,next,1)==1&&a.cash==39000&&count(p)==1,"single still works");
    CyberwareGacha.ends=0;WeaponGacha.ends=0;clear(p);a.cash=9999;token=token(p,weapon);check(CasinoGacha.request(p,weapon,true,token,10)==0&&a.cash==9999&&count(p)==0,"insufficient funds no partial purchase");
    a.cash=50000;for(int i=0;i<27;i++)p.getInventory().setItem(i,new ItemStack(Items.STONE,64));check(CasinoGacha.request(p,weapon,true,token,10)==0&&a.cash==50000&&count(p)==27*64,"nine empty slots cannot buy ten");
    clear(p);StockMarket.file=file.getParent();check(CasinoGacha.request(p,weapon,true,token,10)==0&&a.cash==50000&&count(p)==0,"save failure rolls back all ten and cash");StockMarket.file=file;
    check(CasinoGacha.request(p,!weapon,true,token,10)==0&&a.cash==50000,"cross-terminal token rejected");
    check(CasinoGacha.request(p,weapon,true,token,9)==0&&a.cash==50000,"invalid batch rejected");
    p.setPos(40,65,9);check(CasinoGacha.request(p,weapon,true,token,10)==0&&a.cash==50000,"remote purchase rejected");
   }
   System.out.println("CASINO_GACHA_QA_PASS: exact shared odds including legendary 0.1%, 24 weapons, single/ten both terminals, replay, busy, funds, capacity, rollback, location/mode validation");
  }finally{Cyberware.restore(p,inventory);p.setPos(pos);StockMarket.file=file;StockMarket.ledger=StockMarket.JSON.fromJson(ledger,MarketLedger.class);CasinoGacha.SESSIONS.clear();CyberwareGacha.ends=0;WeaponGacha.ends=0;}
 }
}
