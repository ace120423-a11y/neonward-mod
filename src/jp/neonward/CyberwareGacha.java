package jp.neonward;

import java.util.*;
import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

/** A separate casino terminal; opening never charges, the server owns all rolls. */
public final class CyberwareGacha {
 static final int PRICE=1000;
 static final int[] WEIGHTS={415,300,200,80,5};
 static final String ODDS="コモン41.5% / アンコモン30% / レア20% / エピック8% / レジェンダリー0.5%";
 static final double X=514,Y=65,Z=447;
 static final String TOUCH="nw_casino_touch_cyber_capsule";
 static final java.security.SecureRandom RANDOM=new java.security.SecureRandom();
 static final Map<UUID,Integer> TOKENS=new HashMap<>();
 static long ends;static int tier;static String result="1,000 Cr / 右クリック";
 public record Snapshot(String json) implements CustomPacketPayload {
  public static final Type<Snapshot> TYPE=new Type<>(NeonWard.id("cyber_gacha"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Snapshot> CODEC=StreamCodec.composite(ByteBufCodecs.STRING_UTF8,Snapshot::json,Snapshot::new);
  public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 static int rarity(int n){if(n<0||n>=1000)throw new IllegalArgumentException();for(int i=0;i<WEIGHTS.length;i++){if(n<WEIGHTS[i])return i;n-=WEIGHTS[i];}throw new AssertionError();}
 static int roll(java.util.random.RandomGenerator rng){int t=rarity(rng.nextInt(1000));return rng.nextInt(10)*20+rng.nextInt(4)*5+t;}
 static boolean near(ServerPlayer p){return p.isAlive()&&NeonCasino.inside(p)&&p.distanceToSqr(X,Y+1,Z)<=25;}
 public static void init(){
  PayloadTypeRegistry.clientboundPlay().register(Snapshot.TYPE,Snapshot.CODEC);
  CommandRegistrationCallback.EVENT.register((d,c,e)->d.register(Commands.literal("neongacha").then(Commands.literal("view").executes(ctx->request(ctx.getSource().getPlayerOrException(),false,0))).then(Commands.literal("roll").then(Commands.argument("token",IntegerArgumentType.integer(0)).executes(ctx->request(ctx.getSource().getPlayerOrException(),true,IntegerArgumentType.getInteger(ctx,"token")))))));
  UseEntityCallback.EVENT.register((p,l,h,e,hit)->{if(!e.entityTags().contains(TOUCH))return InteractionResult.PASS;if(p instanceof ServerPlayer sp&&h==InteractionHand.MAIN_HAND)request(sp,false,0);return InteractionResult.SUCCESS;});
  ServerPlayConnectionEvents.DISCONNECT.register((h,s)->TOKENS.remove(h.player.getUUID()));
  ServerLifecycleEvents.SERVER_STOPPED.register(s->{TOKENS.clear();ends=0;tier=0;result="1,000 Cr / 右クリック";});
  ServerTickEvents.END_SERVER_TICK.register(s->{var l=s.overworld();long now=l.getGameTime();if(now% (now<ends?4:20)!=0||!l.isPositionEntityTicking(BlockPos.containing(X,Y,Z)))return;if(s.getPlayerList().getPlayers().stream().noneMatch(p->p.level()==l&&p.distanceToSqr(X,Y,Z)<1024))return;cabinet(l,now);});
 }
 static int request(ServerPlayer p,boolean buy,int token){
  if(!near(p))return 0;
  String message="部位・系統は均等 / 重複あり / 必ず1個入手";int prize=-1,value=0;long now=p.level().getGameTime();
  if(buy&&(!TOKENS.containsKey(p.getUUID())||TOKENS.get(p.getUUID())!=token))return 0;
  if(StockMarket.ledger==null)message="台帳を読み込めないため休止中です";
  else if(buy){var a=StockMarket.ledger.account(p.getStringUUID());
   if(now<ends)message="抽選演出が終わるまでお待ちください";
   else if(a.cash<PRICE)message="1,000 Cr必要です。残高が足りません";
   else if(p.getInventory().getFreeSlot()<0)message="持ち物に空きを1枠作ってください（課金なし）";
   else{var inv=Cyberware.inventory(p);long old=a.cash;int id=roll(RANDOM),bp=CyberwareCatalog.rollValue(id,RANDOM);
    try{if(!p.getInventory().add(Cyberware.stack(id,bp)))throw new IllegalStateException("full");a.cash-=PRICE;StockMarket.save();prize=id;value=bp;ends=now+60;tier=CyberwareCatalog.PARTS[id].tier();result=CyberwareCatalog.RARITIES[tier]+" GET!";TOKENS.put(p.getUUID(),RANDOM.nextInt(Integer.MAX_VALUE));p.getInventory().setChanged();p.containerMenu.broadcastChanges();message="持ち物に受け取りました / −1,000 Cr";}
    catch(Exception ex){a.cash=old;Cyberware.restore(p,inv);message="保存できなかったため抽選・支払いを取り消しました";}
   }
  }
  TOKENS.computeIfAbsent(p.getUUID(),u->RANDOM.nextInt(Integer.MAX_VALUE));
  var out=new JsonObject();out.addProperty("token",TOKENS.get(p.getUUID()));out.addProperty("cash",StockMarket.ledger==null?0:StockMarket.ledger.account(p.getStringUUID()).cash);out.addProperty("message",message);out.addProperty("prize",prize);out.addProperty("value",value);out.addProperty("remaining",Math.max(0,ends-now));ServerPlayNetworking.send(p,new Snapshot(out.toString()));return prize>=0?1:0;
 }
 static void b(ServerLevel l,String k,double dx,double dy,double dz,float w,float h,float d,String block){CasinoProps.box(l,"cyber_capsule_"+k,X+dx,Y+dy,Z+dz,w,h,d,block);}
 static void cabinet(ServerLevel l,long now){
  CasinoProps.touch(l,"cyber_capsule",X,Y,Z,1.8f,2.9f);
  b(l,"foot",0,0,0,1.85f,.14f,1.3f,"polished_blackstone");
  b(l,"base",0,.14,0,1.65f,.83f,1.08f,"black_concrete");
  b(l,"back",0,.97,-.45,1.64f,1.47f,.15f,"gray_concrete");
  for(int side:new int[]{-1,1}){b(l,"pillar"+side,side*.77,.95,0,.13f,1.6f,1.06f,"iron_block");b(l,"neon"+side,side*.71,1,.54,.045f,1.37f,.035f,now<ends&&now%8==0?"magenta_concrete":"cyan_concrete");}
  b(l,"glass",0,1,.49,1.37f,1.36f,.035f,"light_blue_stained_glass");
  b(l,"top",0,2.38,0,1.8f,.4f,1.16f,"black_concrete");
  b(l,"topline",0,2.79,0,1.83f,.045f,1.18f,"cyan_concrete");
  CasinoProps.text(l,"cyber_capsule_brand",X,Y+2.52,Z+.61,"CYBER CAPSULE",0x67fff0,.35f,false);
  for(int i=0;i<6;i++){double dx=(i%3-1)*.4,dy=1.15+(i/3)*.52+(now<ends?Math.sin(now*.35+i)*.065:0);String color=new String[]{"cyan_concrete","magenta_concrete","yellow_concrete","lime_concrete","blue_concrete","purple_concrete"}[i];b(l,"cap"+i,dx,dy,.18,.29f,.18f,.29f,color);b(l,"lid"+i,dx,dy+.18,.18,.29f,.12f,.29f,"white_concrete");b(l,"band"+i,dx,dy+.15,.18,.31f,.045f,.31f,"iron_block");}
  b(l,"console",0,.93,.61,1.72f,.12f,.46f,"gray_concrete");b(l,"button",-.52,1.055,.65,.24f,.05f,.2f,"emerald_block");b(l,"slit",.55,1.055,.65,.035f,.018f,.21f,"black_concrete");
  b(l,"hatch_frame",0,.28,.56,.9f,.48f,.055f,"iron_block");b(l,"hatch",0,.33,.595,.76f,.36f,.035f,"black_concrete");
  CasinoProps.text(l,"cyber_capsule_status",X,Y+.78,Z+.65,now<ends?"SCANNING...":result,now<ends?0x67fff0:CyberwareCatalog.COLORS[tier],.22f,false);
 }
}
