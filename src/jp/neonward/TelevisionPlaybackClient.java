package jp.neonward;
import java.util.*;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.montoyo.wd.entity.*;
import net.montoyo.wd.utilities.browser.WDClientBrowser;
public final class TelevisionPlaybackClient {
 private record Key(String dimension,BlockPos pos){}
 private static final Map<ScreenData,ScreenBlockEntity> ACTIVE=new IdentityHashMap<>();
 private static final Set<Key> OFF=new HashSet<>();
 private static Key key(ScreenBlockEntity be){return new Key(be.getLevel().dimension().identifier().toString(),be.getBlockPos());}
 public static boolean blank(String url){return url==null||url.isBlank()||url.equalsIgnoreCase("about:blank");}
 public static boolean room(ScreenBlockEntity be){var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||be.getLevel()!=mc.level||be.isRemoved())return false;var at=be.getBlockPos();var p=mc.player.blockPosition();
  if(CityApartments.area(mc.level,at))return TelevisionRoomPolicy.apartment(at.getX(),at.getY(),at.getZ(),mc.player.getX(),mc.player.getY(),mc.player.getZ());
  if(mc.level.dimension()==PrivateHomes.DIMENSION){int x=Math.floorDiv(at.getX(),1024)*1024,z=Math.floorDiv(at.getZ(),1024)*1024;return p.getX()>x&&p.getX()<x+30&&p.getZ()>z&&p.getZ()<z+19&&p.getY()>=64&&p.getY()<82;}
  if(mc.level.dimension()==PrivateFarms.DIM)return Math.floorDiv(p.getX(),128)==Math.floorDiv(at.getX(),128)&&Math.floorDiv(p.getZ(),128)==Math.floorDiv(at.getZ(),128)&&p.distSqr(at)<=64;
  return false; // Unclassified screens must not leak audio into the city.
 }
 public static boolean allowed(ScreenData data,ScreenBlockEntity be){return be.getLevel()!=null&&!blank(data.url)&&!OFF.contains(key(be))&&room(be);}
 public static void track(ScreenData data,ScreenBlockEntity be){ACTIVE.put(data,be);}
 public static void forget(ScreenData data){ACTIVE.remove(data);}
 public static void halt(WDClientBrowser browser){if(browser==null)return;try{browser.stopLoad();browser.loadURL("about:blank");}catch(Exception ex){System.err.println("[Neon TV] Stop navigation failed: "+ex);}}
 public static void destroy(WDClientBrowser browser){if(browser==null)return;halt(browser);try{browser.setCloseAllowed();browser.close(true);}catch(Exception ex){System.err.println("[Neon TV] Browser close failed: "+ex);}}
 public static void state(JsonObject o){var k=new Key(o.get("dimension").getAsString(),new BlockPos(o.get("x").getAsInt(),o.get("y").getAsInt(),o.get("z").getAsInt()));boolean on=o.get("on").getAsBoolean();if(on)OFF.remove(k);else OFF.add(k);float volume=Math.clamp(o.get("volume").getAsFloat(),0f,.25f);for(var e:new ArrayList<>(ACTIVE.entrySet()))if(e.getValue().getLevel()!=null&&key(e.getValue()).equals(k)){var d=e.getKey();if(!on){d.url="about:blank";d.closeBrowser();}else{d.autoVolume=false;d.autoVolumeMaxLevel=volume;d.setVolume(volume);}}
  var mc=Minecraft.getInstance();if(mc.level!=null&&mc.level.dimension().identifier().toString().equals(k.dimension())&&mc.level.getBlockEntity(k.pos()) instanceof ScreenBlockEntity be){if(!on){for(int i=0;i<be.getScreenCount();i++){be.getScreen(i).url="about:blank";be.getScreen(i).closeBrowser();}be.clear();}else{be.ytVolume=volume;for(int i=0;i<be.getScreenCount();i++){var d=be.getScreen(i);d.autoVolume=false;d.autoVolumeMaxLevel=volume;}}}
 }
 public static void init(){ClientPlayConnectionEvents.DISCONNECT.register((handler,mc)->{for(var d:new ArrayList<>(ACTIVE.keySet()))d.closeBrowser();ACTIVE.clear();OFF.clear();});ClientTickEvents.END_CLIENT_TICK.register(mc->{for(var e:new ArrayList<>(ACTIVE.entrySet())){var d=e.getKey();var be=e.getValue();boolean attached=false;for(int i=0;i<be.getScreenCount();i++)if(be.getScreen(i)==d)attached=true;if(!attached||!allowed(d,be)){d.closeBrowser();continue;}if(d.browser!=null&&mc.player!=null&&mc.player.tickCount%10==0){d.autoVolume=false;float volume=Float.isFinite(d.autoVolumeMaxLevel)?Math.clamp(d.autoVolumeMaxLevel,0f,.25f):0f;d.setVolume(volume);}}});}
}
