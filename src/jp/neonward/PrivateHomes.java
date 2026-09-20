package jp.neonward;

import java.util.*;
import java.io.InputStreamReader;
import com.google.gson.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.*;
import jp.neonward.mixin.MeterTextAccess;

public final class PrivateHomes {
 public static final ResourceKey<Level> DIMENSION=ResourceKey.create(Registries.DIMENSION,NeonWard.id("private_homes"));
 public static final int PRICE=10000;
 static JsonObject template;
 static final Map<UUID,Integer> cooldown=new HashMap<>();
 public static BlockPos origin(int slot){return new BlockPos((slot%512)*1024,0,(slot/512)*1024);}
 static MarketLedger.Account account(ServerPlayer p){return StockMarket.ledger==null?null:StockMarket.ledger.account(p.getStringUUID());}
 static boolean near(ServerPlayer p){return p.level().dimension()==Level.OVERWORLD&&p.distanceToSqr(221.5,65,950.5)<49;}
 static boolean ownsPosition(ServerPlayer p){var a=account(p);if(a==null||a.homeSlot==0)return false;var o=origin(a.homeSlot);return p.getX()>=o.getX()-1&&p.getX()<o.getX()+31&&p.getZ()>=o.getZ()-1&&p.getZ()<o.getZ()+20&&p.getY()>=63&&p.getY()<82;}
 static boolean canAccessPosition(ServerPlayer p){if(!insidePosition(p))return false;int slot=Math.floorDiv((int)Math.floor(p.getX()),1024)+Math.floorDiv((int)Math.floor(p.getZ()),1024)*512;String owner=null;for(var e:StockMarket.ledger.accounts.entrySet())if(e.getValue().homeSlot==slot){owner=e.getKey();break;}return owner!=null&&(owner.equals(p.getStringUUID())||PhoneFriends.linked(StockMarket.ledger,owner,p.getStringUUID()));}
 static boolean insidePosition(ServerPlayer p){int slot=Math.floorDiv((int)Math.floor(p.getX()),1024)+Math.floorDiv((int)Math.floor(p.getZ()),1024)*512;var o=origin(slot);return p.getX()>=o.getX()-1&&p.getX()<o.getX()+31&&p.getZ()>=o.getZ()-1&&p.getZ()<o.getZ()+20&&p.getY()>=63&&p.getY()<82;}
 public static void init(){
  try(var in=PrivateHomes.class.getResourceAsStream("/data/neonward/housing/slum.json")){template=JsonParser.parseReader(new InputStreamReader(Objects.requireNonNull(in),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}catch(Exception e){throw new IllegalStateException("Home template missing",e);}
  CommandRegistrationCallback.EVENT.register((d,c,e)->{var root=Commands.literal("neonhome");for(String action:List.of("view","buy","enter","leave"))root.then(Commands.literal(action).executes(ctx->request(ctx.getSource().getPlayerOrException(),action)));d.register(root);});
  UseBlockCallback.EVENT.register((p,l,h,hit)->{
   var pos=hit.getBlockPos();
   if(l.dimension()==Level.OVERWORLD&&pos.getX()==221&&pos.getZ()==949&&pos.getY()>=65&&pos.getY()<=68){if(p instanceof ServerPlayer sp&&h==InteractionHand.MAIN_HAND)request(sp,"view");return InteractionResult.SUCCESS;}
   if(l.dimension()==DIMENSION&&p instanceof ServerPlayer sp&&!canAccessPosition(sp))return InteractionResult.FAIL;
   return InteractionResult.PASS;
  });
  ServerLifecycleEvents.SERVER_STOPPED.register(s->cooldown.clear());
  ServerTickEvents.END_SERVER_TICK.register(s->{for(var p:s.getPlayerList().getPlayers()){
   if(s.getTickCount()<cooldown.getOrDefault(p.getUUID(),0))continue;
   if(p.level().dimension()==DIMENSION){
    if(!canAccessPosition(p)){leave(p);continue;}
    var a=account(p);var o=origin(a.homeSlot);
    if(p.getZ()>o.getZ()+15.5&&Math.abs(p.getX()-(o.getX()+8.5))<1.4){leave(p);continue;}
    if(s.getTickCount()%100==0&&ownsPosition(p))nameplate(p);
   }else if(p.level().dimension()==Level.OVERWORLD&&p.getX()>220&&p.getX()<223&&p.getZ()>948&&p.getZ()<950&&p.getY()>=64&&p.getY()<68){
    var a=account(p);if(a!=null&&a.homeSlot>0)enter(p);else{p.teleportTo(s.overworld(),221.5,65,951.5,Set.of(),180,0,true);reply(p,"スラムの隠れ家 / 家具付き 10,000 Cr");cooldown.put(p.getUUID(),s.getTickCount()+30);}
   }
  }});
 }
 static int request(ServerPlayer p,String action){
  if(p.isSpectator())return 0;
  if(action.equals("leave")&&p.level().dimension()==DIMENSION){leave(p);return 1;}
  if(!near(p))return 0;
  if(StockMarket.ledger==null){reply(p,"口座を読み込めません。購入を停止しています");return 0;}
  var a=account(p);String message="家具付き / 寝室・生活スペース・バスルーム・武器庫";
  if(action.equals("buy")){
   String before=StockMarket.JSON.toJson(StockMarket.ledger);
   try{message=purchase(StockMarket.ledger,p.getStringUUID());StockMarket.save();}catch(Exception ex){StockMarket.ledger=StockMarket.JSON.fromJson(before,MarketLedger.class);message="保存に失敗しました。代金は引かれていません";}
  }else if(action.equals("enter")){if(a.homeSlot>0){enter(p);return 1;}message="先に物件を購入してください";}
  reply(p,message);return 1;
 }
 static String purchase(MarketLedger ledger,String id){
  var a=ledger.account(id);if(a.homeSlot>0)return "購入済みです。追加の支払いはありません";
  if(a.cash<PRICE)return "残高が足りません";
  int slot=Math.max(1,ledger.nextHomeSlot);for(var other:ledger.accounts.values())slot=Math.max(slot,other.homeSlot+1);
  if(slot>=262144)return "現在、新しい部屋を用意できません";
  a.cash-=PRICE;a.homeSlot=slot;ledger.nextHomeSlot=slot+1;return "購入しました。あなただけの隠れ家です";
 }
 static void reply(ServerPlayer p,String msg){var a=account(p);var o=new JsonObject();o.addProperty("private_home",true);o.addProperty("cash",a==null?0:a.cash);o.addProperty("owned",a!=null&&a.homeSlot>0);o.addProperty("message",msg);ServerPlayNetworking.send(p,new StockMarket.Snapshot(o.toString()));}
 static <T extends Comparable<T>> BlockState property(BlockState s,Property<T> prop,String value){return prop.getValue(value).map(v->s.setValue(prop,v)).orElse(s);}
 static void build(ServerLevel l,int slot){
  var o=origin(slot);var palette=new ArrayList<BlockState>();
  for(var element:template.getAsJsonArray("palette")){var e=element.getAsJsonObject();var b=BuiltInRegistries.BLOCK.getValue(Identifier.parse(e.get("name").getAsString()));var state=b.defaultBlockState();for(var prop:e.getAsJsonObject("props").entrySet()){var key=b.getStateDefinition().getProperty(prop.getKey());if(key!=null)state=property(state,key,prop.getValue().getAsString());}palette.add(state);}
  for(var value:template.getAsJsonArray("blocks")){var a=value.getAsJsonArray();var pos=o.offset(a.get(0).getAsInt(),a.get(1).getAsInt(),a.get(2).getAsInt());if(l.getBlockState(pos).isAir())l.setBlock(pos,palette.get(a.get(3).getAsInt()),2);}
  for(int x=0;x<30;x++)for(int z=0;z<20;z++)if(l.getBlockState(o.offset(x,64,z)).isAir())l.setBlock(o.offset(x,64,z),Blocks.POLISHED_BLACKSTONE.defaultBlockState(),2);
  // Auto doors and flickering lights need their scheduled ticks after template placement.
  for(var value:template.getAsJsonArray("blocks")){var a=value.getAsJsonArray();var pos=o.offset(a.get(0).getAsInt(),a.get(1).getAsInt(),a.get(2).getAsInt());var b=l.getBlockState(pos).getBlock();String id=BuiltInRegistries.BLOCK.getKey(b).getPath();if(id.equals("automatic_door")||id.startsWith("pulse_neon"))l.scheduleTick(pos,b,1);}
 }
 static void enter(ServerPlayer p){
  var a=account(p);if(a==null||a.homeSlot==0)return;var l=p.level().getServer().getLevel(DIMENSION);if(l==null){reply(p,"部屋を準備できません。再起動を確認してください");return;}
  try{if(!a.homeReady){build(l,a.homeSlot);l.getChunkSource().save(true);a.homeReady=true;try{StockMarket.save();}catch(Exception e){a.homeReady=false;throw e;}}}catch(Exception e){reply(p,"部屋の保存に失敗しました。購入情報は保持されています");return;}
  var o=origin(a.homeSlot);if(p.isPassenger())p.stopRiding();p.teleportTo(l,o.getX()+8.5,65,o.getZ()+12.5,Set.of(),180,0,true);cooldown.put(p.getUUID(),p.level().getServer().getTickCount()+40);nameplate(p);
 }
 static void leave(ServerPlayer p){if(p.isPassenger())p.stopRiding();p.teleportTo(p.level().getServer().overworld(),221.5,65,952.5,Set.of(),0,0,true);cooldown.put(p.getUUID(),p.level().getServer().getTickCount()+40);}
 static void nameplate(ServerPlayer p){var a=account(p);if(a==null||a.homeSlot==0)return;var l=p.level();var o=origin(a.homeSlot);var found=l.getEntitiesOfClass(Display.TextDisplay.class,new AABB(Vec3.atLowerCornerOf(o.offset(0,64,0)),Vec3.atLowerCornerOf(o.offset(30,80,20))),e->e.entityTags().contains("nw_owner_plate"));
  var text=found.isEmpty()?new Display.TextDisplay(EntityTypes.TEXT_DISPLAY,l):found.getFirst();if(found.isEmpty())ClockworkFeedback.load(text,l,"{text:{text:''},line_width:240,background:0,brightness:{block:15,sky:15},Invulnerable:1b,Tags:['nw_owner_plate']}");text.setPos(o.getX()+8.5,68,o.getZ()+13.95);text.setYRot(180);text.setInvulnerable(true);text.entityTags().add("nw_owner_plate");((MeterTextAccess)text).neonSetText(Component.literal(p.getGameProfile().name()+" の家\nHIDEOUT").withColor(0x6ffff0));if(found.isEmpty())l.addFreshEntity(text);
 }
}
