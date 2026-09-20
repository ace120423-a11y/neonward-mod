package jp.neonward;
import java.util.*;
import com.google.gson.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import com.mojang.brigadier.arguments.StringArgumentType;

/** One private, mutually accepted call per player. Raw mono PCM stays in memory. */
public final class PhoneCalls {
 public record Audio(String session,byte[] pcm) implements CustomPacketPayload {
  public static final Type<Audio> TYPE=new Type<>(NeonWard.id("phone_audio"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Audio> CODEC=new StreamCodec<>(){
   public Audio decode(RegistryFriendlyByteBuf b){return new Audio(b.readUtf(36),b.readByteArray(1280));}
   public void encode(RegistryFriendlyByteBuf b,Audio a){b.writeUtf(a.session,36);b.writeByteArray(a.pcm);}
  };public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 static class Call {final UUID caller,callee;final String id=UUID.randomUUID().toString();final long started=System.currentTimeMillis();boolean active;Call(UUID a,UUID b){caller=a;callee=b;}UUID other(UUID p){return caller.equals(p)?callee:caller;}}
 static final Map<UUID,Call> calls=new HashMap<>();static final Map<UUID,long[]> rates=new HashMap<>();static final Map<UUID,Long> dialTimes=new HashMap<>();
 static void init(){PayloadTypeRegistry.serverboundPlay().register(Audio.TYPE,Audio.CODEC);PayloadTypeRegistry.clientboundPlay().register(Audio.TYPE,Audio.CODEC);
  ServerPlayNetworking.registerGlobalReceiver(Audio.TYPE,(a,c)->c.server().execute(()->relay(c.player(),a)));
  CommandRegistrationCallback.EVENT.register((d,c,e)->{var root=Commands.literal("neoncall");for(String action:List.of("view","accept","end"))root.then(Commands.literal(action).executes(ctx->request(ctx.getSource().getPlayerOrException(),action,"")));root.then(Commands.literal("dial").then(Commands.argument("target",StringArgumentType.word()).executes(ctx->request(ctx.getSource().getPlayerOrException(),"dial",StringArgumentType.getString(ctx,"target")))));d.register(root);});
  ServerPlayConnectionEvents.DISCONNECT.register((h,s)->end(s,h.player.getUUID(),"相手が接続を終了しました"));
  ServerLifecycleEvents.SERVER_STOPPED.register(s->{calls.clear();rates.clear();dialTimes.clear();});
  ServerTickEvents.END_SERVER_TICK.register(s->{if(s.getTickCount()%20!=0)return;for(var call:new HashSet<>(calls.values())){var a=s.getPlayerList().getPlayer(call.caller);var b=s.getPlayerList().getPlayer(call.callee);if(a==null||b==null||!PhoneFriends.linked(StockMarket.ledger,call.caller.toString(),call.callee.toString())||PhoneEquipment.get(a).isEmpty()||PhoneEquipment.get(b).isEmpty()){end(s,call.caller,"通話を終了しました");continue;}if(!call.active&&System.currentTimeMillis()-call.started>30000)end(s,call.caller,"応答がありませんでした");}});
 }
 static int request(ServerPlayer p,String action,String target){var s=p.level().getServer();if(action.equals("end")){end(s,p.getUUID(),"通話を終了しました");return 1;}if(StockMarket.ledger==null||p.isSpectator()||PhoneEquipment.get(p).isEmpty())return 0;String message="通話中のみマイクを使用します";try{
  if(action.equals("dial")){UUID id=UUID.fromString(target);var other=s.getPlayerList().getPlayer(id);if(other==null)throw new IllegalArgumentException("相手はオフラインです");if(other.isSpectator()||PhoneEquipment.get(other).isEmpty())throw new IllegalArgumentException("相手のスマホを利用できません");if(!PhoneFriends.linked(StockMarket.ledger,p.getStringUUID(),target))throw new IllegalArgumentException("フレンドにのみ電話できます");if(calls.containsKey(id)||calls.containsKey(p.getUUID()))throw new IllegalArgumentException("通話中または呼び出し中です");long now=System.currentTimeMillis();if(now-dialTimes.getOrDefault(p.getUUID(),0L)<5000)throw new IllegalArgumentException("少し待ってからかけ直してください");dialTimes.put(p.getUUID(),now);var call=new Call(p.getUUID(),id);calls.put(id,call);calls.put(p.getUUID(),call);state(other,"着信 / 電話アプリで応答できます");other.sendSystemMessage(Component.literal("NEON LINK / "+p.getGameProfile().name()+"から着信。電話アプリで応答"));message="相手の応答を待っています";}
  else if(action.equals("accept")){var call=calls.get(p.getUUID());if(call==null||!call.callee.equals(p.getUUID())||call.active)throw new IllegalArgumentException("応答できる着信がありません");call.active=true;var other=s.getPlayerList().getPlayer(call.caller);if(other!=null)state(other,"接続しました");message="接続しました";}
 }catch(Exception ex){message=ex instanceof IllegalArgumentException?ex.getMessage():"電話に接続できません";}state(p,message);return 1;}
 static boolean authorized(UUID who,Audio a){var c=calls.get(who);return c!=null&&c.active&&c.id.equals(a.session)&&a.pcm.length==1280;}
 static void relay(ServerPlayer p,Audio a){if(!authorized(p.getUUID(),a))return;long now=System.currentTimeMillis();var r=rates.computeIfAbsent(p.getUUID(),k->new long[]{now,0});if(now-r[0]>=1000){r[0]=now;r[1]=0;}if(++r[1]>30)return;var c=calls.get(p.getUUID());var other=p.level().getServer().getPlayerList().getPlayer(c.other(p.getUUID()));if(other!=null)ServerPlayNetworking.send(other,a);}
 static void end(MinecraftServer s,UUID id,String msg){var c=calls.remove(id);if(c==null)return;calls.remove(c.other(id));rates.remove(c.caller);rates.remove(c.callee);for(UUID p:List.of(c.caller,c.callee)){var player=s.getPlayerList().getPlayer(p);if(player!=null)state(player,msg);}}
 static void state(ServerPlayer p,String msg){var o=new JsonObject();o.addProperty("call_ui",true);var c=calls.get(p.getUUID());o.addProperty("state",c==null?"idle":c.active?"active":c.callee.equals(p.getUUID())?"incoming":"outgoing");o.addProperty("session",c==null?"":c.id);o.addProperty("message",msg);var other=c==null?null:p.level().getServer().getPlayerList().getPlayer(c.other(p.getUUID()));o.addProperty("name",other==null?"":other.getGameProfile().name());ServerPlayNetworking.send(p,new StockMarket.Snapshot(o.toString()));}
}
