package jp.neonward;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/** Per-item magazine. Infinite reserves, server-authoritative debit and persisted reload deadline. */
public final class GunReload {
 public record Profile(int capacity,int millis){}
 static final Profile[] PROFILES={new Profile(30,2600),new Profile(12,2000),new Profile(6,2300),new Profile(20,2100),new Profile(5,3600),new Profile(100,4800),new Profile(3,3200),new Profile(6,3300),new Profile(15,2800),new Profile(40,3600),new Profile(1,1800)};
 static final String USED="neon_magazine",UNTIL="neon_reload_until";
 public static Profile profile(ItemStack s){return PROFILES[GunVfx.profile(s)];}
 static net.minecraft.nbt.CompoundTag data(ItemStack s){return s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();}
 static long until(ItemStack s){return data(s).getLongOr(UNTIL,0);}
 static int used(ItemStack s){return Math.clamp(data(s).getIntOr(USED,0),0,profile(s).capacity());}
 public static boolean emptyReload(ItemStack s){return used(s)>=profile(s).capacity();}
 public static boolean reloading(ItemStack s){return NeonArsenal.isGun(s)&&until(s)>System.currentTimeMillis();}
 public static int remaining(ItemStack s){long end=until(s);return end>0&&end<=System.currentTimeMillis()?profile(s).capacity():profile(s).capacity()-used(s);}
 public static float progress(ItemStack s){return Math.clamp(1-(until(s)-System.currentTimeMillis())/(float)profile(s).millis(),0,1);}
 static void sync(ServerPlayer p){p.getInventory().setChanged();p.containerMenu.broadcastChanges();}
 static boolean finish(ItemStack s,long now){long end=until(s);if(end<=0||end>now)return false;CustomData.update(DataComponents.CUSTOM_DATA,s,t->{t.putInt(USED,0);t.remove(UNTIL);});return true;}
 static boolean start(ServerPlayer p,ItemStack s,long now){
  if(!NeonArsenal.isGun(s)||!p.isAlive()||p.isSpectator())return false;
  finish(s,now);if(until(s)>now||used(s)==0)return false;
  CustomData.update(DataComponents.CUSTOM_DATA,s,t->t.putLong(UNTIL,now+profile(s).millis()));
  p.stopUsingItem();sync(p);p.level().playSound(null,p.blockPosition(),SoundEvents.CROSSBOW_LOADING_START.value(),SoundSource.PLAYERS,.5f,GunVfx.profile(s)==10?1.2f:.8f);return true;
 }
 static int manual(ServerPlayer p){var s=p.getItemInHand(NeonArsenal.gunHand(p));return start(p,s,System.currentTimeMillis())?1:0;}
 /** Call only after all other rejection checks and immediately before the successful shot. */
 static boolean take(ServerPlayer p,ItemStack s){return take(p,s,System.currentTimeMillis());}
 static boolean take(ServerPlayer p,ItemStack s,long now){
  if(!NeonArsenal.isGun(s)||!p.isAlive()||p.isSpectator())return false;
  finish(s,now);if(until(s)>now)return false;
  if(used(s)>=profile(s).capacity()){start(p,s,now);return false;}
  int n=used(s)+1;CustomData.update(DataComponents.CUSTOM_DATA,s,t->t.putInt(USED,n));
  if(n==profile(s).capacity())start(p,s,now);else sync(p);return true;
 }
 static void init(){ServerTickEvents.END_SERVER_TICK.register(server->{if(server.getTickCount()%4!=0)return;long now=System.currentTimeMillis();for(var p:server.getPlayerList().getPlayers()){boolean changed=false;for(int i=0;i<p.getInventory().getContainerSize();i++){var s=p.getInventory().getItem(i);if(NeonArsenal.isGun(s)&&finish(s,now)){changed=true;if(s==p.getMainHandItem()||s==p.getOffhandItem())p.level().playSound(null,p.blockPosition(),SoundEvents.CROSSBOW_LOADING_END.value(),SoundSource.PLAYERS,.4f,1);}}if(changed)sync(p);}});}
}
