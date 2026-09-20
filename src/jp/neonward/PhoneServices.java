package jp.neonward;

import java.util.*;
import com.google.gson.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

/** Phone requests are authenticated by the connection, never by an ID in the payload. */
public final class PhoneServices {
 public record Request(String json) implements CustomPacketPayload {
  public static final Type<Request> TYPE=new Type<>(NeonWard.id("phone_action"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Request> CODEC=new StreamCodec<>(){
   public Request decode(RegistryFriendlyByteBuf b){return new Request(b.readUtf(40000));}
   public void encode(RegistryFriendlyByteBuf b,Request p){b.writeUtf(p.json,40000);}
  };public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 public static class Entry {public String id=UUID.randomUUID().toString(),from="",to="",text="",photo="";public long time=System.currentTimeMillis();public Set<String> likes=new HashSet<>();}
 static final Map<UUID,Long> last=new HashMap<>();
 static final String[] TITLES={"街の新入り","探索者","熟練ランナー","最高位の冒険者","家主","都市農家","ガレージコレクター"};
 static boolean unlocked(MarketLedger.Account a,int i){return switch(i){case 0->true;case 1->GuildRanks.rank(a)>=1;case 2->GuildRanks.rank(a)>=4;case 3->GuildRanks.rank(a)>=6;case 4->a.homeSlot>0||a.cityFloor>0;case 5->a.farmSlot>0;case 6->!a.casinoVehicles.isEmpty();default->false;};}
 static String title(MarketLedger.Account a){return unlocked(a,a.phoneTitle)?TITLES[a.phoneTitle]:TITLES[0];}
 static String str(JsonObject o,String k){return o.has(k)?o.get(k).getAsString():"";}
 static String clean(String s,int max){s=s.replaceAll("[\\p{Cntrl}]", "").strip();if(s.codePointCount(0,s.length())>max)throw new IllegalArgumentException(max+"文字まで入力できます");return s;}
 static void init(){PayloadTypeRegistry.serverboundPlay().register(Request.TYPE,Request.CODEC);ServerPlayNetworking.registerGlobalReceiver(Request.TYPE,(p,c)->c.server().execute(()->{try{request(c.player(),JsonParser.parseString(p.json()).getAsJsonObject());}catch(Exception ignored){}}));ServerLifecycleEvents.SERVER_STOPPED.register(s->last.clear());}
 static String transfer(MarketLedger l,String who,String other,long amount,String nonce){var a=l.account(who);if(!PhoneFriends.linked(l,who,other))throw new IllegalArgumentException("フレンドから送金先を選んでください");if(amount<1||amount>1000000)throw new IllegalArgumentException("1～1,000,000 Crで入力してください");UUID.fromString(nonce);if(a.phoneReceipts.contains(nonce))return "この送金は処理済みです";if(a.cash<amount)throw new IllegalArgumentException("残高が足りません");var b=l.account(other);long total=Math.addExact(b.cash,amount);a.cash-=amount;b.cash=total;a.phoneReceipts.add(nonce);while(a.phoneReceipts.size()>100)a.phoneReceipts.removeFirst();Entry e=new Entry();e.from=who;e.to=other;e.text=amount+" Cr を送金";l.phoneTransfers.add(e);while(l.phoneTransfers.size()>500)l.phoneTransfers.removeFirst();return b.profileName+"へ "+amount+" Cr を送りました";}
 static void validatePhoto(String s) throws Exception {if(s.isEmpty())return;byte[] bytes=Base64.getDecoder().decode(s);if(bytes.length>4000)throw new IllegalArgumentException("写真が大きすぎます");try(var in=javax.imageio.ImageIO.createImageInputStream(new java.io.ByteArrayInputStream(bytes))){var readers=javax.imageio.ImageIO.getImageReaders(in);if(!readers.hasNext())throw new IllegalArgumentException("写真を読み込めません");var r=readers.next();try{r.setInput(in);if(r.getWidth(0)>160||r.getHeight(0)>160||r.getWidth(0)<1||r.getHeight(0)<1)throw new IllegalArgumentException("写真のサイズが不正です");}finally{r.dispose();}}}
 static String mutate(MarketLedger l,String who,JsonObject o) throws Exception {String action=str(o,"action"),other=str(o,"target");var a=l.account(who);switch(action){
  case "transfer":return transfer(l,who,other,o.get("amount").getAsLong(),str(o,"nonce"));
  case "talk":{if(!PhoneFriends.linked(l,who,other))throw new IllegalArgumentException("フレンドにのみ送信できます");String text=clean(str(o,"text"),180);if(text.isEmpty())throw new IllegalArgumentException("メッセージを入力してください");Entry e=new Entry();e.from=who;e.to=other;e.text=text;l.phoneMessages.add(e);while(l.phoneMessages.size()>2000)l.phoneMessages.removeFirst();return "送信しました";}
  case "post":{String text=clean(str(o,"text"),180),photo=str(o,"photo");if(text.isEmpty()&&photo.isEmpty())throw new IllegalArgumentException("本文か写真を選んでください");validatePhoto(photo);Entry e=new Entry();e.from=who;e.text=text;e.photo=photo;l.phonePosts.add(e);while(l.phonePosts.size()>200)l.phonePosts.removeFirst();return "PULSEに投稿しました";}
  case "delete":{boolean removed=l.phonePosts.removeIf(e->e.id.equals(other)&&e.from.equals(who));return removed?"自分の投稿を削除しました":"投稿が見つかりません";}
  case "like":{for(var e:l.phonePosts)if(e.id.equals(other)){if(!e.likes.remove(who))e.likes.add(who);return "リアクションを更新しました";}return "投稿が見つかりません";}
  case "title":{int i=o.get("index").getAsInt();if(!unlocked(a,i))throw new IllegalArgumentException("まだ獲得していない称号です");a.phoneTitle=i;return "称号を「"+title(a)+"」に設定しました";}
  case "city_buy":return CityApartments.buy(l,who,o.has("floor")?o.get("floor").getAsInt():0);
  case "home_buy":return PrivateHomes.purchase(l,who);
  case "farm_buy":return PrivateFarms.buy(l,who);
  case "quest_accept":{int q=o.get("index").getAsInt();if(q<0||q>=GuildCatalog.QUESTS.length)throw new IllegalArgumentException("依頼を選んでください");return GuildRanks.accept(a,q);}
  case "quest_cancel":{int q=o.get("index").getAsInt();a.guildQuests.remove(q);a.guildQuestRanks.remove(q);return "依頼を取り消しました";}
  case "view":return "";
  default:throw new IllegalArgumentException("操作を確認してください");
 }}
 static int request(ServerPlayer p,JsonObject o){if(StockMarket.ledger==null||p.isSpectator()||PhoneEquipment.get(p).isEmpty())return 0;long now=System.currentTimeMillis();if(now-last.getOrDefault(p.getUUID(),0L)<180){send(p,o,"少し待ってから操作してください",false);return 0;}last.put(p.getUUID(),now);String before=StockMarket.JSON.toJson(StockMarket.ledger),msg="";boolean ok=false;String action=str(o,"action");try{PhoneFriends.prepare(p);msg=mutate(StockMarket.ledger,p.getStringUUID(),o);if(!action.equals("view"))StockMarket.save();ok=true;}catch(Exception e){StockMarket.ledger=StockMarket.JSON.fromJson(before,MarketLedger.class);msg=e instanceof IllegalArgumentException?e.getMessage():"保存できなかったため変更を取り消しました";}
  send(p,o,msg,ok);if(ok&&(action.equals("talk")||action.equals("transfer"))){try{var other=p.level().getServer().getPlayerList().getPlayer(UUID.fromString(str(o,"target")));if(other!=null)other.sendSystemMessage(Component.literal("NEON LINK / "+p.getGameProfile().name()+"から"+(action.equals("talk")?"トークが届きました":"送金が届きました")));}catch(Exception ignored){}}
  if(ok&&action.equals("post")){for(String friend:a.friends)try{if(!PhoneFriends.linked(StockMarket.ledger,p.getStringUUID(),friend))continue;var other=p.level().getServer().getPlayerList().getPlayer(UUID.fromString(friend));if(other!=null){other.sendSystemMessage(Component.literal("NEON LINK / "+p.getGameProfile().name()+"がPULSEに投稿しました"));other.playNotifySound(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.value(),net.minecraft.sounds.SoundSource.PLAYERS,.65f,1.7f);}}catch(Exception ignored){}}
  return 1;}
 static JsonObject snapshot(MarketLedger l,String who,JsonObject req){var a=l.account(who);JsonObject o=new JsonObject();o.addProperty("phone_ui",true);o.addProperty("self",who);o.addProperty("cash",a.cash);o.addProperty("rank",GuildRanks.rank(a));o.addProperty("city",a.cityFloor);o.addProperty("city_free",CityApartments.available(l));JsonArray floors=new JsonArray();for(int f=2;f<=20;f++){var entry=new JsonObject();entry.addProperty("floor",f);entry.addProperty("vacant",CityApartments.vacant(l,f));floors.add(entry);}o.add("city_floors",floors);o.addProperty("home",a.homeSlot);o.addProperty("farm",a.farmSlot);o.addProperty("title",title(a));o.addProperty("title_index",a.phoneTitle);o.addProperty("exam",GuildRanks.rank(a)==6?"Sランク / 回数無制限で再受注可能":a.guildExamActive?GuildRanks.description(a):"現在ランクの依頼報告 "+a.guildRankReports+" / 5件");
  JsonArray friends=new JsonArray();for(String id:a.friends)if(PhoneFriends.linked(l,who,id)){var f=new JsonObject();f.addProperty("id",id);f.addProperty("name",l.account(id).profileName);friends.add(f);}o.add("friends",friends);
  String app=str(req,"app"),target=str(req,"target");o.addProperty("app",app);o.addProperty("target",target);JsonArray rows=new JsonArray();
  if(app.equals("2")){if(PhoneFriends.linked(l,who,target))for(var e:l.phoneMessages)if((e.from.equals(who)&&e.to.equals(target))||(e.from.equals(target)&&e.to.equals(who)))rows.add(entry(l,e,who));while(rows.size()>50)rows.remove(0);}
  else if(app.equals("3")){int page=req.has("page")?Math.max(0,req.get("page").getAsInt()):0;o.addProperty("pages",Math.max(1,(l.phonePosts.size()+1)/2));int start=l.phonePosts.size()-1-Math.min(page,Math.max(0,(l.phonePosts.size()-1)/2))*2;for(int i=start;i>=0&&i>start-2;i--)rows.add(entry(l,l.phonePosts.get(i),who));}
  else if(app.equals("7")){for(var e:l.phoneTransfers)if(e.from.equals(who)||e.to.equals(who))rows.add(entry(l,e,who));while(rows.size()>10)rows.remove(0);}
  else if(app.equals("9")){for(int i=0;i<GuildCatalog.QUESTS.length;i++){boolean accepted=a.guildQuests.containsKey(i);var q=accepted?GuildRanks.accepted(a,i):GuildRanks.quest(i,GuildRanks.rank(a));var v=new JsonObject();v.addProperty("index",i);v.addProperty("title",q.title());v.addProperty("detail",q.condition());v.addProperty("reward",q.reward());v.addProperty("progress",a.guildQuests.getOrDefault(i,0));v.addProperty("goal",q.target());v.addProperty("accepted",accepted);v.addProperty("unlocked",GuildRanks.rank(a)>=GuildRanks.minimum(i));rows.add(v);}}
  else if(app.equals("10")){for(int i=0;i<TITLES.length;i++){var v=new JsonObject();v.addProperty("index",i);v.addProperty("title",TITLES[i]);v.addProperty("unlocked",unlocked(a,i));v.addProperty("detail",new String[]{"街の住民になる","Eランク到達","Bランク到達","Sランク到達","個別住宅を購入","個別農地を購入","カジノ景品車両を獲得"}[i]);rows.add(v);}}
  o.add("rows",rows);return o;
 }
 static JsonObject entry(MarketLedger l,Entry e,String who){var v=new JsonObject();v.addProperty("id",e.id);v.addProperty("name",l.account(e.from).profileName);v.addProperty("toName",e.to.isEmpty()?"":l.account(e.to).profileName);v.addProperty("own",e.from.equals(who));v.addProperty("text",e.text);v.addProperty("photo",e.photo);v.addProperty("time",e.time);v.addProperty("likes",e.likes.size());v.addProperty("liked",e.likes.contains(who));return v;}
 static void send(ServerPlayer p,JsonObject req,String msg,boolean ok){var o=snapshot(StockMarket.ledger,p.getStringUUID(),req);o.addProperty("message",msg);o.addProperty("ok",ok);o.addProperty("action",str(req,"action"));ServerPlayNetworking.send(p,new StockMarket.Snapshot(o.toString()));}
}
