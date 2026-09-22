package jp.neonward;
import net.minecraft.world.item.ItemStack;
public final class GunReloadIntegration {
 static void check(boolean b,String why){if(!b)throw new AssertionError("RELOAD: "+why);}
 static void run(net.minecraft.server.level.ServerPlayer p){
  var inv=Cyberware.inventory(p);
  try{
   for(String id:GunVfx.IDS){
    var s=new ItemStack(NeonArsenal.ITEMS.get(id));long now=System.currentTimeMillis();var profile=GunReload.profile(s);
    check(!GunReload.start(p,s,now),"full reload rejected");
    for(int n=0;n<profile.capacity();n++){check(GunReload.take(p,s,now),"shot accepted "+id);check(GunReload.used(s)==n+1,"single debit");}
    long end=GunReload.until(s);check(end==now+profile.millis(),"auto reload deadline");
    check(GunReload.emptyReload(s),"empty state synchronized");
    check(!GunReload.take(p,s,now+1),"reload blocks shots");check(!GunReload.start(p,s,now+1)&&GunReload.until(s)==end,"spam cannot restart");
    var copy=s.copy();check(GunReload.until(copy)==end&&GunReload.used(copy)==profile.capacity(),"copy preserves state");
    check(!GunReload.finish(s,end-1),"not early");check(GunReload.finish(s,end)&&GunReload.used(s)==0&&GunReload.until(s)==0,"refill");
    check(GunReload.used(copy)==profile.capacity(),"distinct stacks");
    check(GunReload.take(p,s,end),"next magazine");
    if(profile.capacity()>1)check(GunReload.start(p,s,end),"manual partial reload");
    if(profile.capacity()>1)check(!GunReload.emptyReload(s),"tactical reload retains chambered state");
   }
   System.out.println("GUN_RELOAD_PASS: all 11 capacities, debit, auto/manual, spam, deadline, independent copies");
  }finally{Cyberware.restore(p,inv);}
 }
}
