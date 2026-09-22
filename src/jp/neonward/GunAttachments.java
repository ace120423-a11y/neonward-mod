package jp.neonward;
import java.util.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.network.chat.Component;

/** Eight designs, five tiers, four mutually exclusive mount points. */
public final class GunAttachments {
 public static final String[] IDS={"reflex","holo","scope4","scope8","extended_mag","quick_mag","grip","muzzle"};
 public static final String[] NAMES={"リフレックスサイト","ホロサイト","4倍スコープ","8倍スコープ","拡張マガジン","クイックマガジン","スタビライザーグリップ","レンジバレル"};
 public static final String[] SLOTS={"照準器","マガジン","グリップ","銃口"};
 public static final Item[] ITEMS=new Item[8];
 public static int slot(int type){return type<4?0:type<6?1:type-4;}
 public static boolean valid(int code){return code>=0&&code<40;}
 public static int code(ItemStack s){for(int i=0;i<ITEMS.length;i++)if(ITEMS[i]!=null&&s.is(ITEMS[i])){int tier=s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("nw_part_tier",-1);return tier>=0&&tier<5?i*5+tier:-1;}return -1;}
 public static String name(int code){return NAMES[code/5]+" / "+CyberwareCatalog.RARITIES[code%5];}
 public static String effect(int code){int type=code/5,tier=code%5;return switch(type){
  case 0,1,2,3->new String[]{"1倍ドット","1倍リング","4倍目盛り","8倍精密目盛り"}[type]+" / 有効射程 +"+(5+tier*5)+"%";
  case 4->"装填数 +"+(20+tier*10)+"%";
  case 5->"リロード時間 −"+(8+tier*4)+"%";
  case 6->"反動 −"+(10+tier*10)+"%";
  default->"射程 +"+(10+tier*5)+"%";
 };}
 public static ItemStack stack(int code){if(!valid(code))throw new IllegalArgumentException();var s=new ItemStack(ITEMS[code/5]);CustomData.update(DataComponents.CUSTOM_DATA,s,t->t.putInt("nw_part_tier",code%5));s.set(DataComponents.CUSTOM_NAME,Component.literal(name(code)).withColor(CyberwareCatalog.COLORS[code%5]));s.set(DataComponents.ITEM_MODEL,NeonWard.id("attachments/"+IDS[code/5]+"_"+code%5));return s;}
 public static boolean compatible(ItemStack gun,int code){if(!NeonArsenal.isGun(gun)||!valid(code))return false;int kind=GunVfx.profile(gun),type=code/5;
  if(type==3)return kind==4||kind==6;
  if(type==2)return kind!=1&&kind!=2&&kind!=3&&kind!=9;
  if(type==4&&kind==10)return false;
  if(type==7&&kind==9)return false;
  return true;
 }
 public static int installed(ItemStack gun,int mount){int c=gun.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("nw_attachment_"+mount,0)-1;return valid(c)&&slot(c/5)==mount&&compatible(gun,c)?c:-1;}
 static void install(ItemStack gun,int mount,int code){CustomData.update(DataComponents.CUSTOM_DATA,gun,t->{if(code<0)t.remove("nw_attachment_"+mount);else t.putInt("nw_attachment_"+mount,code+1);});}
 public static int capacity(ItemStack gun,int base){int c=installed(gun,1);return c>=0&&c/5==4?base+(base*(20+10*(c%5))+99)/100:base;}
 public static int reloadMillis(ItemStack gun,int base){int c=installed(gun,1);return c>=0&&c/5==5?(int)Math.round(base*(.92-.04*(c%5))):base;}
 public static float recoil(ItemStack gun){float f=1;int c=installed(gun,2);if(c>=0)f*=.9f-.1f*(c%5);c=installed(gun,0);if(c>=0)f*=.9f-.05f*(c%5);return f;}
 public static double range(ItemStack gun){int c=installed(gun,3);double f=c>=0?1.1+.05*(c%5):1;c=installed(gun,0);return f*(c>=0?1.05+.05*(c%5):1);}
 public static float zoom(ItemStack gun){int c=installed(gun,0);return c<0?(NeonArsenal.sniper(gun)?.35f:.8f):c/5==3?.125f:c/5==2?.25f:1;}
 public static int price(int code){return new int[]{800,2000,6000,18000,80000}[code%5]+(code/5==3?1200:code/5==2?600:0);}
 static ItemStack roll(java.util.random.RandomGenerator r){return stack(r.nextInt(8)*5+CyberwareGacha.rarity(r.nextInt(1000)));}
 static int dropChance(LootProfile profile){return profile==LootProfile.BOSS?60:profile==LootProfile.DUNGEON?12:6;}
 static void init(){
  for(int i=0;i<IDS.length;i++){String id="attachment_"+IDS[i];ITEMS[i]=Registry.register(BuiltInRegistries.ITEM,NeonWard.id(id),new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,NeonWard.id(id))).stacksTo(1)));}
  net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB,net.minecraft.resources.Identifier.withDefaultNamespace("combat"))).register(e->{for(int i=0;i<40;i++)e.accept(stack(i));});
  net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH.register((e,source)->{
   if(Underworld.hunter(e)||!(e instanceof net.minecraft.world.entity.monster.Enemy)||!(source.getEntity() instanceof net.minecraft.server.level.ServerPlayer)||!(e.level() instanceof net.minecraft.server.level.ServerLevel l))return;
   var r=java.util.concurrent.ThreadLocalRandom.current();var profile=NeonLoot.profile(e);if(r.nextInt(100)<dropChance(profile))l.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(l,e.getX(),e.getY()+.3,e.getZ(),roll(r)));
  });
  AttachmentService.init();AttachmentGacha.init();
 }
}
