package jp.neonward;
import java.util.*;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import com.mojang.brigadier.arguments.IntegerArgumentType;
public final class NeonCasino {
 public static final BlockPos DESK=new BlockPos(520,65,468),DEALER=new BlockPos(344,65,580),ROULETTE=new BlockPos(510,65,464),BLACKJACK=new BlockPos(498,65,464);
 static final Map<UUID,Long> cooldown=new HashMap<>();static final Random RANDOM=new java.security.SecureRandom();
 static int multiplier(int a,int b,int c){return a==b&&b==c?20:a==b||a==c||b==c?1:0;}
 public static boolean inside(ServerPlayer p){return !p.isSpectator()&&p.level().dimension().equals(Level.OVERWORLD)&&p.getX()>=494&&p.getX()<527&&p.getZ()>=445&&p.getZ()<471&&p.getY()>=64&&p.getY()<73;}
 public static void init(){
  net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player,level,hand,hit)->{var pos=hit.getBlockPos();if(!level.dimension().equals(Level.OVERWORLD)||!pos.equals(DESK)&&!pos.equals(ROULETTE)&&!pos.equals(BLACKJACK))return net.minecraft.world.InteractionResult.PASS;if(player instanceof ServerPlayer sp&&hand==net.minecraft.world.InteractionHand.MAIN_HAND)request(sp,"view",pos.equals(BLACKJACK)?1:pos.equals(ROULETTE)?2:0);return net.minecraft.world.InteractionResult.SUCCESS;});
  ServerLifecycleEvents.SERVER_STOPPED.register(s->cooldown.clear());CasinoWheel.init();CasinoProps.init();CasinoSlots.init();
  CommandRegistrationCallback.EVENT.register((d,c,e)->{var root=Commands.literal("neoncasino");for(String action:new String[]{"view","spin","deal","hit","stand","red","black","odd","even"})root.then(Commands.literal(action).executes(ctx->request(ctx.getSource().getPlayerOrException(),action,0)));root.then(Commands.literal("number").then(Commands.argument("number",IntegerArgumentType.integer(0,36)).executes(ctx->request(ctx.getSource().getPlayerOrException(),"number",IntegerArgumentType.getInteger(ctx,"number")))));d.register(root);});
 }
 static int request(ServerPlayer p,boolean spin){return request(p,spin?"spin":"view",0);}
 static int request(ServerPlayer p,String action,int chosen){return request(p,action,chosen,-1);}
 static int request(ServerPlayer p,String action,int chosen,int machine){
  if(machine>=0&&(!CasinoSlots.near(p,machine)||!Set.of("view","spin").contains(action)))return 0;
  if(!inside(p))return 0;JsonObject out=new JsonObject();out.addProperty("casino",true);out.addProperty("slotMachine",machine);if(action.equals("view"))out.addProperty("page",chosen);String message="各ゲーム1回100 Cr。景品抽選は終了したゲームごとに1%。";boolean changed=false;int wheel=-1;
  if(StockMarket.ledger==null)message="口座を読み込めません。営業を一時停止しています。";
  else {
   String before=StockMarket.JSON.toJson(StockMarket.ledger);var a=StockMarket.ledger.account(p.getStringUUID());long now=p.level().getGameTime();
   if(!action.equals("view")){
    boolean move=action.equals("hit")||action.equals("stand");boolean roulette=Set.of("red","black","odd","even","number").contains(action);
    if(now<cooldown.getOrDefault(p.getUUID(),0L))message="少し待ってから操作してください。";
    else if(a.blackjack!=null&&a.blackjack.active&&!move)message="進行中のブラックジャックを終えてください。";
    else if(move&&(a.blackjack==null||!a.blackjack.active))message="まずカードを配ってください。";
    else if(action.equals("spin")&&(machine<0?CasinoProps.busy(now):CasinoSlots.busy(machine,now)))message="リールが止まるまでお待ちください。";
    else if(roulette&&CasinoWheel.busy(now))message="ルーレットが止まるまでお待ちください。";
    else if(!move&&a.cash<100)message="100 Cr必要です。残高が足りません。";
    else if(!move&&!roulette&&!action.equals("spin")&&!action.equals("deal"))message="不正な操作です。";
    else {
     cooldown.put(p.getUUID(),now+(move?5:40));changed=true;boolean finished=false;
     if(action.equals("spin")){int x=RANDOM.nextInt(6),y=RANDOM.nextInt(6),z=RANDOM.nextInt(6);int prize=100*multiplier(x,y,z);a.cash+=prize-100;StockMarket.ledger.casinoReels=new int[]{x,y,z};StockMarket.ledger.casinoSlotResult="払戻し "+prize+" Cr";if(machine>=0){var slot=StockMarket.ledger.slot(machine);slot.reels=new int[]{x,y,z};slot.result="WIN "+prize+" Cr";}out.addProperty("reels",(x+1)+" | "+(y+1)+" | "+(z+1));message="スロット払戻し "+prize+" Cr";finished=true;}
     else if(roulette){wheel=RANDOM.nextInt(37);int prize=CasinoGames.roulettePayout(wheel,action,chosen);a.cash+=prize-100;StockMarket.ledger.casinoWheel=wheel;out.addProperty("roulette",wheel);message="ルーレット "+wheel+" / 払戻し "+prize+" Cr";finished=true;}
     else {if(action.equals("deal")){a.cash-=100;a.blackjack=CasinoGames.deal(RANDOM);}else if(action.equals("hit"))CasinoGames.hit(a.blackjack);else CasinoGames.stand(a.blackjack);
      if(!a.blackjack.active){a.cash+=a.blackjack.payout;message=a.blackjack.result+" / 払戻し "+a.blackjack.payout+" Cr";finished=true;}else message="ヒットで追加、スタンドで勝負。";
     }
     if(finished){String bonus=CasinoGames.award(a,RANDOM);if(!bonus.isEmpty()){message+=" / "+bonus;out.addProperty("prize",bonus);}}
    }
   }
   if(changed)try{StockMarket.save();if(out.has("prize"))p.sendSystemMessage(net.minecraft.network.chat.Component.literal(out.get("prize").getAsString()).withColor(0xffd86a));if(wheel>=0)CasinoWheel.spin(now,wheel);if(action.equals("spin")){if(machine<0)CasinoProps.spin(now);else CasinoSlots.spin(machine,now);}if(Set.of("deal","hit","stand").contains(action))CasinoProps.hand(p,a.blackjack);}catch(Exception ex){StockMarket.ledger=StockMarket.JSON.fromJson(before,MarketLedger.class);out.remove("roulette");out.remove("reels");out.remove("prize");message="保存できなかったため操作を取り消しました。";}
   a=StockMarket.ledger.account(p.getStringUUID());out.addProperty("cash",a.cash);out.addProperty("lastRoulette",StockMarket.ledger.casinoWheel);out.add("lastReels",StockMarket.JSON.toJsonTree(machine<0?StockMarket.ledger.casinoReels:StockMarket.ledger.slot(machine).reels));out.addProperty("slotRemaining",Math.max(0,(machine<0?CasinoProps.slotEnd:CasinoSlots.end[machine])-now));
   if(a.blackjack!=null){if(machine<0&&action.equals("view"))CasinoProps.hand(p,a.blackjack);var h=a.blackjack;out.addProperty("handActive",h.active);out.addProperty("playerCards",CasinoGames.cards(h.player,false));out.addProperty("dealerCards",CasinoGames.cards(h.dealer,h.active));out.addProperty("playerTotal",CasinoGames.total(h.player));out.addProperty("dealerTotal",h.active?CasinoGames.value(h.dealer.getFirst()):CasinoGames.total(h.dealer));}
   out.add("casinoVehicles",StockMarket.JSON.toJsonTree(a.casinoVehicles));
  }
  out.addProperty("message",message);ServerPlayNetworking.send(p,new StockMarket.Snapshot(out.toString()));return 1;
 }
}
