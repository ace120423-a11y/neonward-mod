package jp.neonward;
import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
/** A reel is settled at the server tick of its stop press, not at the start of the spin. */
final class ManualSlots {
 static int symbol(MarketLedger.SlotState s,int reel,long tick){return s.stopped[reel]?s.reels[reel]:Math.floorMod((int)((tick-s.started)/3)+s.offsets[reel],6);}
 static int request(ServerPlayer p,int machine,String action){if(!CasinoSlots.near(p,machine)||StockMarket.ledger==null)return 0;String before=StockMarket.JSON.toJson(StockMarket.ledger),msg="START → 左・中・右のボタンで好きな順に停止";var s=StockMarket.ledger.slot(machine);var a=StockMarket.ledger.account(p.getStringUUID());long now=p.level().getGameTime();boolean changed=false;
  if(action.equals("spin")){if(s.active)msg="この台はプレイ中です";else if(a.cash<100)msg="100 Cr必要です";else {a.cash-=100;s.active=true;s.owner=p.getStringUUID();s.started=now;for(int r=0;r<3;r++){s.offsets[r]=NeonCasino.RANDOM.nextInt(6);s.stopped[r]=false;}s.result="STOP BUTTONS";changed=true;}}
  else if(action.startsWith("stop")&&action.length()==5&&action.charAt(4)>='0'&&action.charAt(4)<='2'){int r=action.charAt(4)-'0';if(!s.active)msg="まずSTARTを押してください";else if(!s.owner.equals(p.getStringUUID()))msg="プレイ中の人だけが停止できます";else if(s.stopped[r])msg="このリールは停止済みです";else {s.reels[r]=symbol(s,r,now);s.stopped[r]=true;changed=true;if(s.stopped[0]&&s.stopped[1]&&s.stopped[2])msg=finish(s,a);}}
  else if(!action.equals("view"))return 0;
  if(changed)try{StockMarket.save();}catch(Exception e){StockMarket.ledger=StockMarket.JSON.fromJson(before,MarketLedger.class);msg="保存できなかったため操作を取り消しました";}
  send(p,machine,msg);return 1;
 }
 static String finish(MarketLedger.SlotState s,MarketLedger.Account a){int payout=100*NeonCasino.multiplier(s.reels[0],s.reels[1],s.reels[2]);a.cash+=payout;s.active=false;s.result="WIN "+payout+" Cr";String bonus=CasinoGames.award(a,NeonCasino.RANDOM);return "払戻し "+payout+" Cr"+(bonus.isEmpty()?"":" / "+bonus);}
 static void send(ServerPlayer p,int i,String msg){var s=StockMarket.ledger.slot(i);JsonObject o=new JsonObject();o.addProperty("casino",true);o.addProperty("slotMachine",i);o.addProperty("page",0);o.addProperty("cash",StockMarket.ledger.account(p.getStringUUID()).cash);o.addProperty("manualSlots",true);o.addProperty("slotActive",s.active);o.addProperty("slotOwner",s.owner.equals(p.getStringUUID()));o.addProperty("slotElapsed",p.level().getGameTime()-s.started);o.add("slotOffsets",StockMarket.JSON.toJsonTree(s.offsets));o.add("slotStopped",StockMarket.JSON.toJsonTree(s.stopped));o.add("lastReels",StockMarket.JSON.toJsonTree(s.reels));o.addProperty("message",msg);ServerPlayNetworking.send(p,new StockMarket.Snapshot(o.toString()));}
}
