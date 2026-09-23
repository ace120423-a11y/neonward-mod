package jp.neonward;
import java.util.*;
import com.google.gson.*;
import net.minecraft.server.level.*;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.commands.Commands;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
/** Every screen has a short-lived, one-use server quote bound to its own service. */
public final class LeisureShop {
 static final String[] NAMES={"飲食店 / 横丁","PET LINK / ペットショップ","BLACK ALLEY / 闇商人","BLACK STEEL / 射撃訓練所","SHOWCASE / 武器展示台"};
 record Offer(String name,String effect,long price,ItemStack stack,int pet){}
 record Session(int kind,int token,long expires,long day,boolean remote,int eatery){Session(int kind,int token,long expires,long day,boolean remote){this(kind,token,expires,day,remote,-1);}}
 static final Map<UUID,Session> SESSIONS=new HashMap<>();static final java.security.SecureRandom RANDOM=new java.security.SecureRandom();
 public record Reply(String json) implements CustomPacketPayload {public static final Type<Reply> TYPE=new Type<>(NeonWard.id("leisure_shop_v1"));public static final StreamCodec<RegistryFriendlyByteBuf,Reply> CODEC=StreamCodec.composite(ByteBufCodecs.STRING_UTF8,Reply::json,Reply::new);public Type<? extends CustomPacketPayload> type(){return TYPE;}}
 static long day(ServerPlayer p){return p.level().getServer().overworld().getGameTime()/24000;}
 static ExistingEateries.Site foodSite(ServerPlayer p){var s=SESSIONS.get(p.getUUID());var site=s!=null&&s.kind()==0?ExistingEateries.site(s.eatery()):ExistingEateries.nearest(p);return site==null?ExistingEateries.ALL.getFirst():site;}
 static int openFood(ServerPlayer p,int id){if(!p.isAlive()||p.isSpectator()||!ExistingEateries.near(p,id))return 0;var q=quote(p,0,false);SESSIONS.put(p.getUUID(),new Session(0,q.token(),q.expires(),q.day(),false,id));reply(p,"店名に合わせた料理 / 価格と効果を確認してください");return 1;}
 static boolean near(ServerPlayer p,int kind){if(kind==0)return ExistingEateries.nearest(p)!=null;var at=LeisureSites.at(kind);return at!=null&&p.level().dimension()==Level.OVERWORLD&&p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(at))<=49&&p.level().getBlockState(at).is(LeisureSites.COUNTERS[kind]);}
 static Session quote(ServerPlayer p,int kind,boolean remote){var prev=SESSIONS.get(p.getUUID());int token;do{token=RANDOM.nextInt(Integer.MAX_VALUE);}while(prev!=null&&prev.token()==token);return new Session(kind,token,p.level().getGameTime()+1200,day(p),remote,prev!=null&&prev.kind()==kind?prev.eatery():-1);}
 static int open(ServerPlayer p,int kind,boolean remote){if(kind==0){var site=ExistingEateries.nearest(p);return remote||site==null?0:openFood(p,site.id());}if(!p.isAlive()||p.isSpectator()||kind<0||kind>=5||remote&&kind!=1||!remote&&!near(p,kind))return 0;SESSIONS.put(p.getUUID(),quote(p,kind,remote));reply(p,kind==2?"場所は固定 / 品揃えはゲーム内1日ごとに更新":remote?"購入済みペット / 呼び出し・帰還":"購入前に価格と内容を確認してください");return 1;}
 static boolean use(ServerPlayer p,CityResident npc){if(!npc.job.startsWith("leisure_"))return false;for(int i=0;i<5;i++)if(npc.job.equals("leisure_"+i)&&near(p,i)){open(p,i,false);break;}return true;}
 static List<Offer> offers(ServerPlayer p,int kind,long day){var out=new ArrayList<Offer>();
  if(kind==0)for(int i:foodSite(p).meals())out.add(new Offer(StreetMeals.NAMES[i],StreetMeals.EFFECTS[i],StreetMeals.PRICES[i],new ItemStack(StreetMeals.ITEMS[i]),-1));
  if(kind==1)for(int i=0;i<PetCompanions.NAMES.length;i++)out.add(new Offer(PetCompanions.NAMES[i],PetCompanions.owned(p,i)?"購入済み / 連れ歩きは1人1匹":"永久購入 / 呼び出し・帰還可能",PetCompanions.price(i),ItemStack.EMPTY,i));
  if(kind==2){var r=new Random(0x424c41434bL+day*7919);for(int i=0;i<2;i++){int tier=(day%7==0&&i==1)?4:2+r.nextInt(2);var weapons=NeonArsenal.DROPS.stream().filter(w->w!=NeonShield.ITEM).toList();var item=weapons.get(r.nextInt(weapons.size()));var stack=RolledWeapons.create(item,new WeaponLoot.Quality(tier,r.nextInt(WeaponLoot.MIN[tier],WeaponLoot.MAX[tier]+1)));out.add(new Offer(stack.getHoverName().getString(),"個体値 "+Math.round(RolledWeapons.multiplier(stack)*100)+"% / 本日の武器",tier==4?150000:tier==3?26000:9000,stack,-1));}for(int i=0;i<3;i++){int code=r.nextInt(8)*5+2+r.nextInt(3);out.add(new Offer(GunAttachments.name(code),GunAttachments.effect(code),GunAttachments.price(code)*2L,GunAttachments.stack(code),-1));}}
  if(kind==3)out.add(new Offer("訓練エリアへ入る","ダメージ・DPS計測 / 武器と属性を試せます",0,ItemStack.EMPTY,-1));
  if(kind==4)out.add(new Offer("武器展示台","自宅に設置 / 武器を預けて飾る / 性能表示",1200,new ItemStack(WeaponDisplay.BLOCK),-1));
  return out;
 }
 static int request(ServerPlayer p,String action,int token,int value){var s=SESSIONS.get(p.getUUID());if(s==null||s.token()!=token||!p.isAlive()||p.isSpectator()||p.level().getGameTime()>s.expires()||!s.remote()&&!(s.kind()==0?ExistingEateries.near(p,s.eatery()):near(p,s.kind())))return 0;
  if(s.kind()==2&&s.day()!=day(p)){SESSIONS.put(p.getUUID(),quote(p,s.kind(),s.remote()));reply(p,"品揃えが更新されました。選び直してください（課金なし）");return 0;}
  if(action.equals("recall")&&s.kind()==1){PetCompanions.recall(p);SESSIONS.put(p.getUUID(),quote(p,1,s.remote()));reply(p,"ペットを帰還させました");return 1;}
  var rows=offers(p,s.kind(),s.day());if(value<0||value>=rows.size())return 0;var row=rows.get(value);int done=0;String message="この操作はできません";
  if(action.equals("summon")&&s.kind()==1&&PetCompanions.owned(p,value)){done=PetCompanions.summon(p,value);message=done>0?"ペットを呼び出しました":"安全な場所で呼び出してください";}
  else if(action.equals("enter")&&s.kind()==3&&!s.remote()){SESSIONS.remove(p.getUUID());return TrainingRange.enter(p)?1:0;}
  else if(action.equals("buy")&&!s.remote()&&s.kind()!=3){
   if(s.kind()==1){done=PetCompanions.purchase(p,value);message=done>0?"購入しました。呼び出すと連れ歩けます":"購入できません（残高・所有状況を確認してください）";}
   else if(StockMarket.ledger==null)message="台帳を読み込めないため取引を停止中です";
   else {var a=StockMarket.ledger.account(p.getStringUUID());int free=p.getInventory().getFreeSlot();if(free<0)message="持ち物に空きを1枠作ってください（課金なし）";else if(a.cash<row.price())message="残高が足りません（課金なし）";else{long cash=a.cash;var inv=Cyberware.inventory(p);try{a.cash-=row.price();p.getInventory().setItem(free,row.stack().copy());StockMarket.save();done=1;message=row.name()+"を購入しました";}catch(Exception ex){a.cash=cash;Cyberware.restore(p,inv);message="保存できないため購入を取り消しました";}}}
  }
  // All attempts rotate the token; double-clicked or delayed commands cannot charge twice.
  SESSIONS.put(p.getUUID(),quote(p,s.kind(),s.remote()));p.getInventory().setChanged();p.containerMenu.broadcastChanges();reply(p,message);return done;
 }
 static void reply(ServerPlayer p,String message){var s=SESSIONS.get(p.getUUID());if(s==null)return;var o=new JsonObject();o.addProperty("kind",s.kind());o.addProperty("title",s.kind()==0?foodSite(p).name():NAMES[s.kind()]);o.addProperty("token",s.token());o.addProperty("message",message);o.addProperty("cash",StockMarket.ledger==null?0:StockMarket.ledger.account(p.getStringUUID()).cash);var rows=new JsonArray();for(var v:offers(p,s.kind(),s.day())){var r=new JsonObject();r.addProperty("name",v.name());r.addProperty("effect",v.effect());r.addProperty("price",v.price());String action=s.kind()==3?"enter":s.kind()==1&&PetCompanions.owned(p,v.pet())?"summon":"buy";r.addProperty("action",action);r.addProperty("enabled",!s.remote()||action.equals("summon"));rows.add(r);}o.add("rows",rows);if(ServerPlayNetworking.canSend(p,Reply.TYPE))ServerPlayNetworking.send(p,new Reply(o.toString()));}
 static void init(){StreetMeals.init();LeisureSites.init();EateryDecor.init();PayloadTypeRegistry.clientboundPlay().register(Reply.TYPE,Reply.CODEC);
  CommandRegistrationCallback.EVENT.register((d,c,e)->{var root=Commands.literal("neonleisure").then(Commands.literal("pets").executes(ctx->open(ctx.getSource().getPlayerOrException(),1,true)));for(var action:List.of("buy","summon","recall","enter"))root.then(Commands.literal(action).then(Commands.argument("token",IntegerArgumentType.integer(0)).then(Commands.argument("value",IntegerArgumentType.integer(0,10)).executes(ctx->request(ctx.getSource().getPlayerOrException(),action,IntegerArgumentType.getInteger(ctx,"token"),IntegerArgumentType.getInteger(ctx,"value"))))));d.register(root);});
  ServerPlayConnectionEvents.DISCONNECT.register((h,s)->SESSIONS.remove(h.player.getUUID()));net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(s->SESSIONS.clear());
 }
}
