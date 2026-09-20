package jp.neonward;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.network.chat.Component;
public final class JapaneseParlor {
 static Block TATAMI,SHOJI;static final Map<String,CityResident> NPC=new HashMap<>();
 static Block material(String name){var id=NeonWard.id(name);var b=new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK,id)).strength(2,8).sound(SoundType.WOOD));Registry.register(BuiltInRegistries.BLOCK,id,b);Registry.register(BuiltInRegistries.ITEM,id,new BlockItem(b,new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id)).useBlockDescriptionPrefix()));return b;}
 static class WashitsuStairs extends StairBlock {WashitsuStairs(BlockBehaviour.Properties p){super(Blocks.OAK_PLANKS.defaultBlockState(),p);}}
 static void registerStairs(){var id=NeonWard.id("washitsu_stairs");var b=new WashitsuStairs(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK,id)).strength(2,8).sound(SoundType.WOOD));Registry.register(BuiltInRegistries.BLOCK,id,b);Registry.register(BuiltInRegistries.ITEM,id,new BlockItem(b,new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id)).useBlockDescriptionPrefix()));}
 static void init(){registerStairs();for(int i=0;i<4;i++)material("fusuma_full_"+i);for(int i=0;i<2;i++)material("fusuma_cabinet_"+i);TATAMI=material("parlor_tatami");SHOJI=material("parlor_shoji");for(int i=0;i<6;i++)material("parlor_scroll_"+i);for(String name:new String[]{"parlor_plaster","parlor_fusuma","parlor_scroll","parlor_mat_a","parlor_mat_b"})material(name);net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(s->NPC.clear());}
 static void render(ServerLevel l,long now){if(l.isPositionEntityTicking(new BlockPos(521,65,451))&&l.getBlockState(new BlockPos(521,65,451)).is(Blocks.DARK_OAK_SLAB))mahjong(l);if(l.isPositionEntityTicking(new BlockPos(516,56,454))&&l.getBlockState(new BlockPos(516,55,454)).is(TATAMI)){koi(l);dice(l,now);}}
 static void mahjong(ServerLevel l){CasinoProps.touch(l,"mahjong",521.5,65,451.5,3f,1.15f);b(l,"mj_felt",521.5,65.8,451.5,2.8f,.08f,2.8f,"green_concrete");for(int x:new int[]{-1,1})for(int z:new int[]{-1,1})b(l,"mj_leg"+x+z,521.5+x*1.1,65,451.5+z*1.1,.18f,.8f,.18f,"dark_oak_planks");var g=StockMarket.ledger.mahjong;t(l,"mj_title",521.5,67.3,451.5,"四人麻雀 / 東風戦 / 空席CPU",0xefe1b1,.48f,false);
  for(int s=0;s<4;s++){double angle=s*Math.PI/2,dx=Math.sin(angle),dz=Math.cos(angle);npc(l,"mj_cpu_"+s,521.5+dx*2.1,65,451.5+dz*2.1,g.seats[s].name,g.seats[s].cpu,8);
   for(int j=0;j<14;j++){double across=(j-6.5)*.145;double x=521.5+dx*1.1+Math.cos(angle)*across,z=451.5+dz*1.1-Math.sin(angle)*across;b(l,"mj_hand"+s+"_"+j,x,65.89,z,j<g.seats[s].hand.size()?.12f:0,.23f,.12f,"white_concrete");}
   var river=g.seats[s].river;for(int j=0;j<24;j++){boolean shown=j<river.size();double a=(j%6-2.5)*.17,depth=.25+(j/6)*.17;double x=521.5+dx*depth+Math.cos(angle)*a,z=451.5+dz*depth-Math.sin(angle)*a;b(l,"mj_river"+s+"_"+j,x,65.9,z,shown?.15f:0,.035f,.16f,"white_concrete");t(l,"mj_ink"+s+"_"+j,x,65.944,z,shown?String.valueOf((char)(0xe100+river.get(j))):"",0xffffff,shown?.065f:0,true);}
  }t(l,"mj_score",521.5,66.6,453.05,g.phase.equals("lobby")?"右クリック → 着席 → 開始":g.message,0xe7ddab,.25f,false);
 }
 static void koi(ServerLevel l){CasinoProps.touch(l,"koi",501.5,56,453.5,3f,1.1f);b(l,"koi_felt",501.5,56.58,453.5,2.7f,.07f,1.8f,"red_concrete");for(int x:new int[]{-1,1})for(int z:new int[]{-1,1})b(l,"koi_leg"+x+z,501.5+x*1.05,56,453.5+z*.65,.16f,.58f,.16f,"dark_oak_planks");var g=StockMarket.ledger.koi;t(l,"koi_title",501.5,58.0,453.5,"花札・こいこい / 二人 / 空席CPU",0xffdda5,.38f,false);for(int i=0;i<16;i++){boolean yes=i<g.field.size();double x=500.5+(i%8)*.28,z=453.2+(i/8)*.4;b(l,"koi_card"+i,x,56.66,z,yes?.24f:0,.02f,.34f,"white_concrete");t(l,"koi_ink"+i,x,56.685,z,yes?KoiRound.MONTH[g.field.get(i)/4]:"",0x8c1d24,yes?.2f:0,true);}for(int i=0;i<2;i++)npc(l,"koi_cpu"+i,501.5,56,453.5+(i==0?2:-2),g.seats[i].name,g.seats[i].cpu,3);}
 static void dice(ServerLevel l,long now){CasinoProps.touch(l,"dice",516.5,56,454.5,3f,1.2f);var d=StockMarket.ledger.dice;boolean reveal=d.phase.equals("reveal"),shake=d.phase.equals("shake");double shakeX=shake?Math.sin(now*1.4)*.12:0;double lift=reveal?1.05:0;
  b(l,"dice_mat",516.5,56.04,454.5,3.6f,.035f,3.0f,"white_concrete");
  b(l,"bowl_top",516.5+shakeX,56.65+lift,454.5,.95f,.15f,.95f,"stripped_dark_oak_wood");for(int side:new int[]{-1,1}){b(l,"bowl_side_x"+side,516.5+shakeX+side*.43,56.17+lift,454.5,.12f,.5f,.95f,"stripped_spruce_wood");b(l,"bowl_side_z"+side,516.5+shakeX,56.17+lift,454.5+side*.43,.76f,.5f,.12f,"stripped_spruce_wood");}
  for(int i=0;i<2;i++){double x=516.28+i*.45;int value=i==0?d.a:d.b;b(l,"die"+i,x,56.09,454.5,.30f,.3f,.30f,"white_concrete");int[][] coords={{-1,-1},{1,1},{-1,1},{1,-1},{-1,0},{1,0},{0,0}};for(int k=0;k<7;k++){boolean dot=reveal&&(value==1?k==6:value==2?k<2:value==3?k<2||k==6:value==4?k<4:value==5?k<4||k==6:k<6);b(l,"pip"+i+"_"+k,x+coords[k][0]*.085,56.396,454.5+coords[k][1]*.085,dot?.055f:0,.009f,dot?.055f:0,value==1?"red_concrete":"black_concrete");}}
  npc(l,"dealer",516.5,56,452.2,"壺振り・お凛",true,3);t(l,"dealer_speech",516.5,58.55,452.2,d.call,0xffe5ad,.50f,false);t(l,"dice_result",516.5,57.15,456.1,reveal?d.a+" + "+d.b+" = "+(d.a+d.b)+((d.a+d.b)%2==0?" 丁":" 半"):"丁半 / 丁＝偶数・半＝奇数 / 100 Cr",0xffd885,.36f,false);
  int humans=d.bets.size();for(int i=0;i<3;i++)npc(l,"dice_guest"+i,514.7+i*1.8,56,456.8,"CPU 客"+(i+1)+(d.cpu[i]==0?"：丁":"：半"),i<4-humans,8);
 }
 static void npc(ServerLevel l,String key,double x,double y,double z,String name,boolean visible,int role){var n=NPC.get(key);if(n==null||n.isRemoved()){var uuid=UUID.nameUUIDFromBytes(("neonward/parlor/"+key).getBytes(java.nio.charset.StandardCharsets.UTF_8));var old=l.getEntity(uuid);if(old instanceof CityResident c)n=c;else if(visible){n=new CityResident(CityResidents.TYPES.get(role),l);n.setUUID(uuid);n.shopStaff=true;n.job=key.equals("dealer")?"dice":"parlor";n.home=BlockPos.containing(x,y,z);n.setPos(x,y,z);n.setCustomName(Component.literal(name));n.setCustomNameVisible(true);l.addFreshEntity(n);}if(n!=null)NPC.put(key,n);}if(n!=null){if(!visible){n.discard();NPC.remove(key);}else if(!n.getName().getString().equals(name))n.setCustomName(Component.literal(name));}}
 static void b(ServerLevel l,String key,double x,double y,double z,float w,float h,float d,String mat){CasinoProps.box(l,"japan_"+key,x,y,z,w,h,d,mat);}
 static void t(ServerLevel l,String key,double x,double y,double z,String value,int color,float scale,boolean flat){CasinoProps.text(l,"japan_"+key,x,y,z,value,color,scale,flat);}
}
