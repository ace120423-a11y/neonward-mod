package jp.neonward;
import java.util.*;
import com.google.gson.JsonObject;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.phys.AABB;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import com.mojang.brigadier.arguments.IntegerArgumentType;
public final class CompactShops {
 public static final ResourceKey<Level> DIM=ResourceKey.create(Registries.DIMENSION,NeonWard.id("compact_shops"));
 public record Shop(String title,int x,int z,int width,int depth,int accent){}
 public static final Shop[] ALL={new Shop("BLACK STEEL / 武器屋",78,477,15,13,0),new Shop("NEON THREADS / 服屋",243,490,15,13,1),new Shop("NEON GUILD / 企業受付",83,115,19,15,2),new Shop("NEURO CLINIC / 診療所",425,480,15,13,3),new Shop("PULSE EXCHANGE / マーケット",245,121,15,13,4)};
 static final Map<UUID,Integer> cooldown=new HashMap<>();
 static final Set<Integer> ready=new HashSet<>();
 static Block block(String id){return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(id));}
 static int ox(int id){return id*128;}
 static BlockPos pos(int id,int x,int y,int z){return new BlockPos(ox(id)+x,y,z);}
 static int room(Level l,BlockPos p){if(l.dimension()!=DIM)return -1;for(int i=0;i<ALL.length;i++)if(p.getX()>=ox(i)&&p.getX()<=ox(i)+ALL[i].width()&&p.getZ()>=0&&p.getZ()<=ALL[i].depth())return i;return -1;}
 static BlockPos counter(int id){return pos(id,id==2?6:7,65,3);}
 static BlockPos exchange(){return pos(2,12,65,3);}
 static void put(ServerLevel l,int id,int x,int y,int z,Block b){l.setBlock(pos(id,x,y,z),b.defaultBlockState(),3);}
 static void furniture(ServerLevel l,int id,int x,int y,int z,String name){put(l,id,x,y,z,NeonFurniture.BLOCKS.get(name));}
 static void door(ServerLevel l,BlockPos p){var state=AutoDoors.DOOR.defaultBlockState().setValue(DoorBlock.FACING,Direction.NORTH);l.setBlock(p,state.setValue(DoorBlock.HALF,DoubleBlockHalf.LOWER),3);l.setBlock(p.above(),state.setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER),3);l.scheduleTick(p,AutoDoors.DOOR,1);}
 static void build(ServerLevel l,int id){var s=ALL[id];int w=s.width(),d=s.depth();Block accent=id==0?block("red_concrete"):id==1?block("magenta_concrete"):block("cyan_concrete");
  for(int x=0;x<=w;x++)for(int z=0;z<=d;z++)for(int y=64;y<=71;y++){Block b=y==64?Blocks.POLISHED_DEEPSLATE:y==71?Blocks.SMOOTH_STONE:x==0||x==w||z==0||z==d?block("gray_concrete"):Blocks.AIR;put(l,id,x,y,z,b);}
  for(int x=1;x<w;x++){put(l,id,x,69,0,accent);put(l,id,x,69,d,accent);if(x%3==1)for(int z=2;z<d;z+=4)put(l,id,x,70,z,Blocks.SEA_LANTERN);}
  for(int z=1;z<d;z++){put(l,id,0,69,z,accent);put(l,id,w,69,z,accent);put(l,id,w/2,64,z,z%3==0?Blocks.SEA_LANTERN:Blocks.SMOOTH_STONE);}
  for(int x=3;x<w-2;x++){put(l,id,x,65,3,Blocks.POLISHED_BLACKSTONE);put(l,id,x,66,3,x%3==0?accent:Blocks.SMOOTH_STONE_SLAB);}
  if(id==0){put(l,id,7,65,3,Blocks.SMITHING_TABLE);for(int x:new int[]{2,w-2})for(int z:new int[]{4,7,10}){furniture(l,id,x,65,z,"weapon_rack");put(l,id,x,67,z,Blocks.SEA_LANTERN);}furniture(l,id,4,65,8,"work_desk");furniture(l,id,4,66,8,"tool_tray");}
  if(id==1){put(l,id,7,65,3,StreetFashion.COUNTER);for(int z:new int[]{5,8}){furniture(l,id,2,65,z,"neon_shelf");furniture(l,id,w-2,65,z,"neon_locker");}for(int z=9;z<d;z++)put(l,id,w-4,65,z,block("magenta_stained_glass"));for(int x=w-3;x<w;x++)put(l,id,x,68,9,block("magenta_concrete"));furniture(l,id,3,65,10,"shop_sofa_velvet");furniture(l,id,4,65,10,"shop_sofa_velvet");}
  if(id==2){l.setBlock(counter(id),GuildServices.QUEST.defaultBlockState(),3);l.setBlock(exchange(),GuildServices.EXCHANGE.defaultBlockState(),3);for(int x:new int[]{3,4,5})furniture(l,id,x,65,10,"shop_sofa_ivory");furniture(l,id,4,65,8,"shop_table_glass");furniture(l,id,w-3,65,8,"terminal_desk");for(int y=65;y<=67;y++)put(l,id,9,y,2,Blocks.AIR);}
  if(id==3){l.setBlock(counter(id),Cyberware.TERMINAL.defaultBlockState(),3);furniture(l,id,3,65,8,"work_desk");furniture(l,id,4,65,8,"work_desk");furniture(l,id,w-3,65,8,"terminal_desk");}
  if(id==4){furniture(l,id,7,65,3,"terminal_desk");}
  // Counters stay reachable from the public side; clear the interactable block's head space.
  l.setBlock(counter(id).above(),Blocks.AIR.defaultBlockState(),3);if(id==2)l.setBlock(exchange().above(),Blocks.AIR.defaultBlockState(),3);
  door(l,pos(id,w/2,65,d));door(l,pos(id,w/2+1,65,d));
  put(l,id,w/2,65,d+1,Blocks.BARRIER);put(l,id,w/2+1,65,d+1,Blocks.BARRIER);
  put(l,id,0,63,0,Blocks.LODESTONE);
 }
 static void staff(ServerLevel l,int id,int role,int x,String title,String job){var p=pos(id,x,65,1);UUID uuid=UUID.nameUUIDFromBytes(("compact/staff/"+id+"/"+job).getBytes(java.nio.charset.StandardCharsets.UTF_8));if(l.getEntity(uuid)!=null)return;var e=new CityResident(CityResidents.TYPES.get(role),l);e.setUUID(uuid);e.home=p;e.shopStaff=true;e.job=job;e.setCustomName(Component.literal(title));e.setPos(p.getX()+.5,65,1.5);e.setYRot(0);l.addFreshEntity(e);}
 static void label(ServerLevel l,int id,String tag,double x,double y,double z,String value){String key="compact_"+id+"_"+tag;if(!l.getEntitiesOfClass(Display.TextDisplay.class,new AABB(ox(id),63,0,ox(id)+ALL[id].width()+1,72,ALL[id].depth()+1),e->e.entityTags().contains(key)).isEmpty())return;var e=new Display.TextDisplay(EntityTypes.TEXT_DISPLAY,l);try{e.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,l.registryAccess(),net.minecraft.nbt.TagParser.parseCompoundFully("{billboard:'center',background:0,brightness:{block:15,sky:15},line_width:220}")));}catch(Exception ex){throw new IllegalStateException(ex);}((jp.neonward.mixin.MeterTextAccess)e).neonSetText(Component.literal(value).withStyle(net.minecraft.ChatFormatting.AQUA));e.addTag(key);e.setPos(ox(id)+x,y,z);l.addFreshEntity(e);}
 static void decorate(ServerLevel l,int id){var s=ALL[id];label(l,id,"title",s.width()/2.0,68.5,1.2,s.title());label(l,id,"exit",s.width()/2.0,68,s.depth()-.3,"EXIT / 自動ドアから街へ");if(id==2)TowerGuides.install(l);
  if(id==2){staff(l,id,10,6,"受付嬢 ミオ","compact_quest");staff(l,id,11,12,"換金担当 レン","compact_exchange");staff(l,id,11,9,"ギルド食料配布係","compact_food");label(l,id,"quest",6.5,67.4,3.5,"クエスト受注・報告");label(l,id,"exchange",12.5,67.4,3.5,"素材の換金");label(l,id,"food",9.5,67.4,3.5,"本日の食料配布");}
  else if(id==4){staff(l,id,0,7,"マーケット取引担当","compact_market");label(l,id,"trading",7.5,67.4,3.5,"株の購入・売却 / Cr");label(l,id,"guide",7.5,68,7,"6銘柄 / 店員か端末を右クリック");}
  else staff(l,id,id==1?2:0,7,id==0?"BLACK STEEL / 武器販売":id==3?"サイバーウェア技師":"NEON THREADS / 店員",id==0?"compact_arms":id==3?"compact_cyber":"compact_fashion");
  if(id==1)for(int i=0;i<3;i++){String tag="compact_mannequin_"+i;var at=pos(id,4+i*3,65,6);if(!l.getEntitiesOfClass(net.minecraft.world.entity.decoration.ArmorStand.class,new AABB(at).inflate(1),e->e.entityTags().contains(tag)).isEmpty())continue;var stand=new net.minecraft.world.entity.decoration.ArmorStand(EntityTypes.ARMOR_STAND,l);try{stand.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,l.registryAccess(),net.minecraft.nbt.TagParser.parseCompoundFully("{NoGravity:1b,Invulnerable:1b,ShowArms:1b,DisabledSlots:4144959}")));}catch(Exception ex){throw new IllegalStateException(ex);}for(int j=0;j<4;j++)stand.setItemSlot(StreetFashion.SLOTS[j],new ItemStack(StreetFashion.ITEMS.get(i*12+j)));stand.addTag(tag);stand.setPos(at.getX()+.5,65,6.5);l.addFreshEntity(stand);}
 }
 static void ensure(ServerLevel l,int id){if(ready.contains(id))return;for(int x=ox(id)>>4;x<=(ox(id)+ALL[id].width())>>4;x++)for(int z=0;z<=ALL[id].depth()>>4;z++)l.getChunk(x,z);if(!l.getBlockState(pos(id,0,63,0)).is(Blocks.LODESTONE)){build(l,id);l.getChunkSource().save(true);}decorate(l,id);ready.add(id);}
 static boolean enter(ServerPlayer p,int id){if(p.isPassenger()||p.isSpectator())return false;var l=p.level().getServer().getLevel(DIM);if(l==null){p.sendOverlayMessage(Component.literal("内装の読み込みには再起動が必要です"));return false;}ensure(l,id);var s=ALL[id];if(!p.teleportTo(l,ox(id)+s.width()/2+.5,65,s.depth()-2.5,Set.of(),180,0,true))return false;cooldown.put(p.getUUID(),l.getServer().getTickCount()+40);return true;}
 static boolean safeExit(ServerLevel l,BlockPos at){l.getChunkAt(at);return l.getBlockState(at.below()).isFaceSturdy(l,at.below(),Direction.UP)&&l.getBlockState(at).getCollisionShape(l,at).isEmpty()&&l.getBlockState(at.above()).getCollisionShape(l,at.above()).isEmpty()&&l.getFluidState(at).isEmpty()&&l.getFluidState(at.above()).isEmpty();}
 static void leave(ServerPlayer p,int id){var s=ALL[id];var l=p.level().getServer().overworld();double x=s.x()+1,z=s.z()+(id==4?4.5:3.5);var at=BlockPos.containing(x,65,z);if(!safeExit(l,at)){p.sendOverlayMessage(Component.literal("街側の出口がふさがっています。管理者に連絡してください"));return;}if(p.isPassenger())p.stopRiding();if(!p.teleportTo(l,x,65,z,Set.of(),0,0,true))return;p.fallDistance=0;p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);cooldown.put(p.getUUID(),p.level().getServer().getTickCount()+40);}
 static void open(ServerPlayer p,String kind,BlockPos at){var o=new JsonObject();o.addProperty("compact_open",kind);o.addProperty("x",at.getX());o.addProperty("y",at.getY());o.addProperty("z",at.getZ());ServerPlayNetworking.send(p,new StockMarket.Snapshot(o.toString()));}
 static boolean staffUse(ServerPlayer p,CityResident n){if(p.level().dimension()!=DIM||p.distanceToSqr(n)>36)return false;int id=room(p.level(),n.blockPosition());if(id<0)return false;if(id==4&&n.job.equals("compact_market")){if(p.isSpectator())return false;ServerPlayNetworking.send(p,new MarketInteriorClient.Open(counter(4)));return true;}if(id==3&&n.job.equals("compact_cyber")){var o=new JsonObject();o.addProperty("cyberware_open",true);o.addProperty("x",counter(3).getX());o.addProperty("y",counter(3).getY());o.addProperty("z",counter(3).getZ());ServerPlayNetworking.send(p,new StockMarket.Snapshot(o.toString()));return true;}String kind=switch(n.job){case "compact_exchange"->id==2?"exchange":"";case "compact_quest"->id==2?"quest":"";case "compact_fashion"->id==1?"fashion":"";case "compact_arms"->id==0?"arms":"";default->"";};if(kind.isEmpty())return false;open(p,kind,kind.equals("exchange")?exchange():counter(id));return true;}
 static int buy(ServerPlayer p,int product){if(room(p.level(),p.blockPosition())!=0||p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(counter(0)))>36||p.isSpectator()||StockMarket.ledger==null)return 0;var a=StockMarket.ledger.account(p.getStringUUID());String msg="通常品質の武器 / 高品質品は敵のドロップで入手";if(product>=0&&product<NeonArsenal.DROPS.size()){int free=p.getInventory().getFreeSlot(),price=price(product);if(free<0)msg="持ち物に空きを作ってください";else if(a.cash<price)msg="残高が足りません";else{a.cash-=price;try{StockMarket.save();p.getInventory().setItem(free,new ItemStack(NeonArsenal.DROPS.get(product)));p.getInventory().setChanged();msg="購入しました / "+price+" Cr";}catch(Exception e){a.cash+=price;msg="保存に失敗したため購入を中止しました";}}}var o=new JsonObject();o.addProperty("compact_arms",true);o.addProperty("cash",a.cash);o.addProperty("message",msg);ServerPlayNetworking.send(p,new StockMarket.Snapshot(o.toString()));return 1;}
 static int price(int i){return 600+i*180;}
 public static void init(){ServerLifecycleEvents.SERVER_STOPPED.register(s->{ready.clear();cooldown.clear();});ServerPlayConnectionEvents.DISCONNECT.register((h,s)->cooldown.remove(h.player.getUUID()));
  PayloadTypeRegistry.clientboundPlay().register(MarketInteriorClient.Open.TYPE,MarketInteriorClient.Open.CODEC);
  CommandRegistrationCallback.EVENT.register((d,c,e)->d.register(Commands.literal("shopPrepare").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).then(Commands.argument("shop",IntegerArgumentType.integer(0,ALL.length-1)).executes(ctx->{var l=ctx.getSource().getServer().getLevel(DIM);if(l==null)return 0;ensure(l,IntegerArgumentType.getInteger(ctx,"shop"));return 1;}))));
  CommandRegistrationCallback.EVENT.register((d,c,e)->{d.register(Commands.literal("neonarms").then(Commands.argument("product",IntegerArgumentType.integer(-1,NeonArsenal.DROPS.size()-1)).executes(ctx->buy(ctx.getSource().getPlayerOrException(),IntegerArgumentType.getInteger(ctx,"product")))));d.register(Commands.literal("shopExit").executes(ctx->{var p=ctx.getSource().getPlayerOrException();int id=room(p.level(),p.blockPosition());if(id<0&&p.level().dimension()==Level.OVERWORLD&&Math.abs(p.getX()-246)<12&&p.getZ()>100&&p.getZ()<124&&p.getY()>63&&p.getY()<75)id=4;if(id>=0){leave(p,id);return 1;}return 0;}));});
  UseBlockCallback.EVENT.register((p,l,h,hit)->{if(h!=InteractionHand.MAIN_HAND||l.dimension()!=DIM)return InteractionResult.PASS;int id=room(l,hit.getBlockPos());if(id==0&&hit.getBlockPos().equals(counter(0))){if(p instanceof ServerPlayer sp)open(sp,"arms",counter(0));return InteractionResult.SUCCESS;}if(id==3&&hit.getBlockPos().equals(counter(3))){if(p instanceof ServerPlayer sp)open(sp,"cyberware",counter(3));return InteractionResult.SUCCESS;}return InteractionResult.PASS;});
  ServerTickEvents.END_SERVER_TICK.register(s->{if(s.getTickCount()%5!=0)return;for(var p:s.getPlayerList().getPlayers()){if(s.getTickCount()<cooldown.getOrDefault(p.getUUID(),0))continue;if(p.level().dimension()==DIM){int id=room(p.level(),p.blockPosition());if(id<0){leave(p,2);continue;}if(p.getY()<64){leave(p,id);continue;}ensure((ServerLevel)p.level(),id);if(p.getZ()>ALL[id].depth()-.4&&Math.abs(p.getX()-(ox(id)+ALL[id].width()/2+1))<1.3)leave(p,id);if(s.getTickCount()%100==0){decorate((ServerLevel)p.level(),id);for(var enemy:p.level().getEntitiesOfClass(net.minecraft.world.entity.Mob.class,p.getBoundingBox().inflate(40),e->e instanceof Enemy))enemy.discard();}}else if(p.level().dimension()==Level.OVERWORLD&&p.getY()>=64.5&&p.getY()<67)for(int id=0;id<ALL.length;id++){var a=ALL[id];if(p.getX()>a.x()-.15&&p.getX()<a.x()+2.15&&p.getZ()>a.z()-1&&p.getZ()<a.z()+.65&&p.level().getBlockState(new BlockPos(a.x(),65,a.z())).is(AutoDoors.DOOR)){enter(p,id);break;}}}});
 }
}
