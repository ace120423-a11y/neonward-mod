package jp.neonward;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
public final class AttachmentIntegration {
 static void check(boolean b,String why){if(!b)throw new AssertionError("ATTACHMENT: "+why);}
 static void clear(ServerPlayer p){for(int i=0;i<36;i++)p.getInventory().setItem(i,ItemStack.EMPTY);}
 static int open(ServerPlayer p,String mode){check(AttachmentService.open(p,mode)==1,"open "+mode);return AttachmentService.SESSIONS.get(p.getUUID()).token();}
 static int count(ServerPlayer p){int n=0;for(int i=0;i<36;i++)if(GunAttachments.code(p.getInventory().getItem(i))>=0)n++;return n;}
 static void run(ServerPlayer p){
  var inventory=Cyberware.inventory(p);var pos=p.position();var file=StockMarket.file;String ledger=StockMarket.JSON.toJson(StockMarket.ledger);
  try{
   check(UiCommandLimiter.isUi("neonattach roll 1 10"),"non-kicking throttle");
   for(int c=0;c<40;c++)check(GunAttachments.code(GunAttachments.stack(c))==c,"40 exact item tiers");
   int[] weights=new int[5];for(int i=0;i<1000;i++){final int n=i;var s=GunAttachments.roll(new Random(1){public int nextInt(int bound){return bound==1000?n:super.nextInt(bound);}});weights[GunAttachments.code(s)%5]++;}
   check(Arrays.equals(weights,new int[]{419,300,200,80,1}),"exact gacha odds");
   check(GunAttachments.dropChance(LootProfile.BOSS)>GunAttachments.dropChance(LootProfile.FIELD),"boss drops higher");
   check(CasinoGacha.weapons().size()==24,"weapon pool unchanged");
   clear(p);var gun=new ItemStack(NeonArsenal.ITEMS.get("kestrel_pistol"));p.setItemInHand(InteractionHand.MAIN_HAND,gun);p.getInventory().setItem(9,GunAttachments.stack(24));
   int token=open(p,"equip");check(AttachmentService.request(p,"install",token,9)==1,"install mag");
   check(GunReload.profile(gun).capacity()==20&&GunReload.remaining(gun)==12,"capacity / no free reload");
   check(AttachmentService.request(p,"install",token,9)==0,"no replay");
   p.getInventory().setItem(9,GunAttachments.stack(29));token=open(p,"equip");check(AttachmentService.request(p,"install",token,9)==1,"swap");
   check(count(p)==1&&GunAttachments.installed(gun,1)==29&&GunReload.profile(gun).millis()==1520,"old mag returned / faster reload");
   var saved=gun.copy();check(GunAttachments.installed(saved,1)==29,"copy retains mounts");
   token=open(p,"equip");check(AttachmentService.request(p,"remove",token,1)==1&&count(p)==2,"detach returns item");
   p.getInventory().setItem(10,GunAttachments.stack(19));token=open(p,"equip");check(AttachmentService.request(p,"install",token,10)==0,"pistol rejects 8x");
   check(!p.getInventory().getItem(10).isEmpty(),"incompatible remains");
   p.getInventory().setItem(11,GunAttachments.stack(0));token=open(p,"equip");check(AttachmentService.request(p,"install",token,11)==1,"optic");
   for(int i=0;i<36;i++)if(p.getInventory().getItem(i).isEmpty())p.getInventory().setItem(i,new ItemStack(Items.STONE,64));
   token=open(p,"equip");check(AttachmentService.request(p,"remove",token,0)==0&&GunAttachments.installed(p.getMainHandItem(),0)==0,"full inventory rollback");
   clear(p);gun=new ItemStack(NeonArsenal.ITEMS.get("pulse_rifle"));p.setItemInHand(InteractionHand.MAIN_HAND,gun);p.getInventory().setItem(9,GunAttachments.stack(24));token=open(p,"equip");gun.setCount(0);check(AttachmentService.request(p,"install",token,9)==0,"removed weapon");
   gun=new ItemStack(NeonArsenal.ITEMS.get("pulse_rifle"));p.setItemInHand(InteractionHand.MAIN_HAND,gun);CustomData.update(DataComponents.CUSTOM_DATA,gun,t->t.putInt(GunReload.USED,1));GunReload.start(p,gun,System.currentTimeMillis());token=open(p,"equip");check(AttachmentService.request(p,"install",token,9)==0,"loading swap blocked");
   clear(p);p.setPos(522,65,449);AttachmentGacha.ends=0;var account=StockMarket.ledger.account(p.getStringUUID());account.cash=50000;
   token=open(p,"gacha");check(AttachmentService.request(p,"roll",token,10)==10&&account.cash==40000&&count(p)==10,"ten gacha");
   check(AttachmentService.request(p,"roll",token,10)==0&&account.cash==40000,"gacha replay");token=AttachmentService.SESSIONS.get(p.getUUID()).token();check(AttachmentService.request(p,"roll",token,1)==0,"animation lock");
   AttachmentGacha.ends=0;clear(p);account.cash=9999;check(AttachmentService.request(p,"roll",token,10)==0&&count(p)==0&&account.cash==9999,"insufficient");
   account.cash=50000;for(int i=0;i<27;i++)p.getInventory().setItem(i,new ItemStack(Items.STONE,64));check(AttachmentService.request(p,"roll",token,10)==0&&account.cash==50000,"nine spaces");clear(p);
   StockMarket.file=file.getParent();check(AttachmentService.request(p,"roll",token,10)==0&&account.cash==50000&&count(p)==0,"save rollback");StockMarket.file=file;
   check(AttachmentService.request(p,"buy",token,0)==0,"mode isolation");check(AttachmentService.request(p,"roll",token,9)==0,"invalid batch");
   p.setPos(40,65,9);check(AttachmentService.request(p,"roll",token,10)==0,"distance");check(AttachmentService.open(p,"shop")==0,"remote shop");
   var level=p.level().getServer().getLevel(CompactShops.DIM);var buyer=WestLandIntegration.visitor(level);
   try{buyer.setPos(net.minecraft.world.phys.Vec3.atCenterOf(CompactShops.counter(0)));StockMarket.ledger.account(buyer.getStringUUID()).cash=10000;int t=open(buyer,"shop");check(AttachmentService.request(buyer,"buy",t,0)==1&&count(buyer)==1,"shop purchase");check(StockMarket.ledger.account(buyer.getStringUUID()).cash==9200,"shop exact price");}finally{level.removePlayerImmediately(buyer,net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);AttachmentService.SESSIONS.remove(buyer.getUUID());}
   for(String id:GunVfx.IDS){var s=new ItemStack(NeonArsenal.ITEMS.get(id));for(int tier=0;tier<5;tier++){GunAttachments.install(s,2,30+tier);check(GunAttachments.recoil(s)>0&&GunAttachments.recoil(s)<=.9,"bounded recoil");}check(GunReload.profile(s).capacity()>=1,"all guns capacity");}
   System.out.println("ATTACHMENT_PASS: 40 parts, exact odds, equip/swap/detach, compatibility, capacity/ammo, reload, full inventory rollback, shop, gacha single-mode/ten, replay, money rollback, range guards");
  }finally{Cyberware.restore(p,inventory);p.setPos(pos);StockMarket.file=file;StockMarket.ledger=StockMarket.JSON.fromJson(ledger,MarketLedger.class);AttachmentService.SESSIONS.clear();AttachmentGacha.ends=0;}
 }
}
