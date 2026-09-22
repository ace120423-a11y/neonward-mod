package jp.neonward;
import java.util.*;
import com.google.gson.*;
import java.io.InputStreamReader;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionResult;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
/** Nineteen physical floors: one floor per account, never sold twice. */
public final class CityApartments {
 static final int PRICE=20000;static String founder;static Set<BlockPos> fixed=new HashSet<>();
 static int floor(double y){return Math.clamp((int)Math.floor((y-65)/8)+1,1,20);}
 static int base(int f){return 65+(f-1)*8;}
 static boolean area(Level l,BlockPos p){return l.dimension()==Level.OVERWORLD&&p.getX()>=66&&p.getX()<=103&&p.getZ()>=217&&p.getZ()<=259&&p.getY()>=72&&p.getY()<=225;}
 static boolean shaft(BlockPos p){return p.getX()>=91&&p.getX()<=96&&p.getZ()>=225&&p.getZ()<=230;}
 static boolean owns(Player p,int f){if(!(p instanceof ServerPlayer sp))return false;var a=PrivateHomes.account(sp);return a!=null&&a.cityFloor==f;}
 static boolean canAccess(Player p,int f){if(!(p instanceof ServerPlayer sp)||StockMarket.ledger==null)return false;String owner=null;for(var e:StockMarket.ledger.accounts.entrySet())if(e.getValue().cityFloor==f){owner=e.getKey();break;}return owner!=null&&(owner.equals(sp.getStringUUID())||PhoneFriends.linked(StockMarket.ledger,owner,sp.getStringUUID()));}
 static boolean editable(Level l,Player p,BlockPos pos){if(!area(l,pos)||shaft(pos))return false;int f=floor(pos.getY());if(!(p instanceof ServerPlayer)&&(p==null||floor(p.getY())!=f))return false;int offset=pos.getY()-base(f);return pos.getX()>66&&pos.getX()<103&&pos.getZ()>217&&pos.getZ()<259&&offset>=0&&offset<=(f==20?6:5)&&!fixed.contains(new BlockPos(pos.getX(),offset,pos.getZ()));}
 static int available(MarketLedger l){for(int f=2;f<=20;f++){int n=f;if(l.accounts.values().stream().noneMatch(a->a.cityFloor==n))return f;}return 0;}
 static boolean vacant(MarketLedger l,int f){return f>=2&&f<=20&&l.accounts.values().stream().noneMatch(a->a.cityFloor==f);}
 static String buy(MarketLedger l,String who,int f){var a=l.account(who);if(a.cityFloor>0)return "購入済み / "+a.cityFloor+"階があなたの部屋です";if(f<2||f>20)throw new IllegalArgumentException("空いている2〜20階から選んでください");if(!vacant(l,f))throw new IllegalArgumentException("その階は売約済みです。別の空室を選んでください");if(a.cash<PRICE)throw new IllegalArgumentException("残高が足りません");a.cash-=PRICE;a.cityFloor=f;return f+"階を購入しました / 専用表札・家具配置対応";}
 static void init(){try(var r=new InputStreamReader(Objects.requireNonNull(CityApartments.class.getResourceAsStream("/data/neonward/housing/city_apartments.json")),java.nio.charset.StandardCharsets.UTF_8)){var o=JsonParser.parseReader(r).getAsJsonObject();founder=o.get("founder").getAsString();for(var e:o.getAsJsonArray("fixed")){var a=e.getAsJsonArray();fixed.add(new BlockPos(a.get(0).getAsInt(),a.get(1).getAsInt(),a.get(2).getAsInt()));}}catch(Exception e){throw new IllegalStateException("City apartment layout",e);}
  ServerLifecycleEvents.SERVER_STARTED.register(s->{if(StockMarket.ledger==null)return;var a=StockMarket.ledger.account(founder);if(a.cityFloor==0&&!HousingSales.state(StockMarket.ledger,founder).founderReleased&&StockMarket.ledger.accounts.values().stream().noneMatch(v->v.cityFloor==20)){a.cityFloor=20;try{StockMarket.save();}catch(Exception e){a.cityFloor=0;}}});
  UseBlockCallback.EVENT.register((p,l,h,hit)->InteractionResult.PASS);
  ServerTickEvents.END_SERVER_TICK.register(s->{if(StockMarket.ledger==null)return;var l=s.overworld();if(s.getTickCount()%10==0)for(var p:s.getPlayerList().getPlayers())if(!p.isSpectator()&&area(p.level(),p.blockPosition())&&!shaft(p.blockPosition())&&!canAccess(p,floor(p.getY()))){if(p.isPassenger())p.stopRiding();p.teleportTo(l,97.5,65,233.5,Set.of(),0,0,true);p.sendOverlayMessage(Component.literal("所有者または登録フレンドだけが入れます"));}
   if(s.getTickCount()%100==0)for(int f=2;f<=20;f++){int y=base(f),n=f;if(!l.isPositionEntityTicking(new BlockPos(94,y,233)))continue;var owner=StockMarket.ledger.accounts.values().stream().filter(a->a.cityFloor==n).findFirst();String label=f+"F / "+owner.map(a->a.profileName+" の家").orElse("空室 / 不動産屋で購入");CasinoProps.text(l,"city_home_owner_"+f,94, y+3,232.9,label,owner.isPresent()?0x65ffe4:0xffc77a,.4f,false);}
  });
 }
}
