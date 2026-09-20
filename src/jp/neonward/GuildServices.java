package jp.neonward;
import java.util.*;
import com.google.gson.*;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;

public final class GuildServices {
 public static Block QUEST,EXCHANGE;public static final BlockPos QUEST_POS=new BlockPos(80,65,99),EXCHANGE_POS=new BlockPos(86,65,99);
 public static final List<Item> MATERIALS=new ArrayList<>();
 public static void init(){
  QUEST=NeonZones.terminal("guild_quest_counter");EXCHANGE=NeonZones.terminal("guild_exchange_counter");
  for(int i=0;i<GuildCatalog.MATERIAL_IDS.length;i++){var key=ResourceKey.create(Registries.ITEM,NeonWard.id(GuildCatalog.MATERIAL_IDS[i]));MATERIALS.add(Registry.register(BuiltInRegistries.ITEM,key,new Item(new Item.Properties().setId(key))));}
  UseBlockCallback.EVENT.register((p,l,h,hit)->{var state=l.getBlockState(hit.getBlockPos());return !l.isClientSide()&&(state.is(QUEST)||state.is(EXCHANGE))?InteractionResult.SUCCESS:InteractionResult.PASS;});
  CommandRegistrationCallback.EVENT.register((d,c,e)->{var root=Commands.literal("neonguild");for(var verb:List.of("view","sell","accept","claim","exam","promote")){String action=verb;root.then(Commands.literal(verb).then(Commands.argument("x",IntegerArgumentType.integer()).then(Commands.argument("y",IntegerArgumentType.integer()).then(Commands.argument("z",IntegerArgumentType.integer()).then(Commands.argument("index",IntegerArgumentType.integer(-1,7)).executes(ctx->request(ctx.getSource().getPlayerOrException(),new BlockPos(IntegerArgumentType.getInteger(ctx,"x"),IntegerArgumentType.getInteger(ctx,"y"),IntegerArgumentType.getInteger(ctx,"z")),action,IntegerArgumentType.getInteger(ctx,"index"))))))));}d.register(root);});
  ServerTickEvents.END_SERVER_TICK.register(s->{if(s.getTickCount()%100!=0)return;var l=s.overworld();install(l,QUEST_POS,QUEST,"クエスト受注","討伐依頼・報酬受取");install(l,EXCHANGE_POS,EXCHANGE,"素材換金所","回収素材をCrで買取");});
  ServerLivingEntityEvents.AFTER_DEATH.register((e,source)->{if(Underworld.hunter(e)||!(e instanceof Enemy)||!(source.getEntity() instanceof ServerPlayer p)||!(e.level() instanceof ServerLevel l))return;
   var r=java.util.concurrent.ThreadLocalRandom.current();boolean boss=NeonLoot.profile(e)==LootProfile.BOSS;
   if(boss||r.nextInt(100)<80){int material=r.nextInt(3);if(e instanceof SpireBoss bossMob){material=3+(bossMob.spec().floor()%4);}else if(e instanceof CyberEnemy enemy){int k=Arrays.asList(HostileRoster.ALL).indexOf(enemy.kind());material=new int[]{0,3,2,1,0,4,3,3,5,6,1,5}[k];}l.addFreshEntity(new ItemEntity(l,e.getX(),e.getY()+.3,e.getZ(),new ItemStack(MATERIALS.get(material),(boss?r.nextInt(3,7):r.nextInt(1,4))+r.nextInt(GunEnchantments.looting(source,l)+1))));}
   if(boss)l.addFreshEntity(new ItemEntity(l,e.getX(),e.getY()+.3,e.getZ(),new ItemStack(MATERIALS.get(7))));
   if(StockMarket.ledger==null)return;var a=StockMarket.ledger.account(p.getStringUUID());String before=StockMarket.JSON.toJson(a);boolean wasReady=GuildRanks.ready(a);var prior=new HashMap<>(a.guildQuests);
   String biome=l.getBiome(e.blockPosition()).unwrapKey().map(k->k.identifier().getPath()).orElse("plains");int floor=e instanceof SpireBoss sb?sb.spec().floor():Math.max(1,((int)e.getY()-64)/24+1);
   if(GuildRanks.progress(a,NeonZones.isField(l,e.blockPosition()),SpireSite.contains(l,e.blockPosition()),boss,HostileRoster.habitat(biome),floor))try{StockMarket.save();for(var q:a.guildQuests.keySet())if(prior.getOrDefault(q,0)<GuildRanks.accepted(a,q).target()&&a.guildQuests.get(q)>=GuildRanks.accepted(a,q).target())p.sendSystemMessage(Component.literal("依頼達成："+GuildRanks.accepted(a,q).title()+" / 受付へ報告してね"));if(!wasReady&&GuildRanks.ready(a))p.sendSystemMessage(Component.literal("昇級試験の目標達成！ 企業ビルの受付で合格報告できます"));}catch(Exception ex){StockMarket.ledger.accounts.put(p.getStringUUID(),StockMarket.JSON.fromJson(before,MarketLedger.Account.class));p.sendSystemMessage(Component.literal("依頼の進行を保存できませんでした"));}
  });
 }
 static void install(ServerLevel l,BlockPos p,Block counter,String title,String subtitle){if(!l.hasChunkAt(p)||!l.getBlockState(p).isAir()||l.getBlockState(p.below()).isAir())return;l.setBlock(p,counter.defaultBlockState(),3);for(int dx:new int[]{-1,1})if(l.getBlockState(p.offset(dx,0,0)).isAir())l.setBlock(p.offset(dx,0,0),Blocks.POLISHED_BLACKSTONE.defaultBlockState(),3);NeonZones.sign(l,p.above(),"NEON GUILD",title,subtitle,"右クリックで利用");}
 static int[] counts(ServerPlayer p){int[] n=new int[MATERIALS.size()];for(int s=0;s<p.getInventory().getContainerSize();s++){var stack=p.getInventory().getItem(s);int i=MATERIALS.indexOf(stack.getItem());if(i>=0)n[i]+=stack.getCount();}return n;}
 static void food(ServerPlayer p){
  if(StockMarket.ledger==null||p.isSpectator())return;
  var a=StockMarket.ledger.account(p.getStringUUID());long day=p.level().getGameTime()/24000L;
  if(a.guildMealDay==day){p.sendOverlayMessage(Component.literal("本日のギルド配給は受け取り済みです"));return;}
  var before=new ArrayList<ItemStack>();for(int i=0;i<p.getInventory().getContainerSize();i++)before.add(p.getInventory().getItem(i).copy());long oldDay=a.guildMealDay;
  boolean added=p.getInventory().add(new ItemStack(Items.BREAD,10))&&p.getInventory().add(new ItemStack(Items.COOKED_BEEF,10));
  if(!added){for(int i=0;i<before.size();i++)p.getInventory().setItem(i,before.get(i));p.getInventory().setChanged();p.sendOverlayMessage(Component.literal("配給を受け取るため、持ち物に空きを作ってください"));return;}
  a.guildMealDay=day;try{StockMarket.save();p.getInventory().setChanged();p.containerMenu.broadcastChanges();p.sendSystemMessage(Component.literal("ギルド配給：パン10個 / 携帯食10個を受け取りました"));}catch(Exception ex){a.guildMealDay=oldDay;for(int i=0;i<before.size();i++)p.getInventory().setItem(i,before.get(i));p.getInventory().setChanged();p.sendOverlayMessage(Component.literal("配給の保存に失敗したため取り消しました"));}
 }
 static int request(ServerPlayer p,BlockPos pos,String action,int index){
  if(p.isSpectator()||(p.level().dimension()!=Level.OVERWORLD&&CompactShops.room(p.level(),pos)!=2)||p.distanceToSqr(Vec3.atCenterOf(pos))>36||StockMarket.ledger==null)return 0;var block=p.level().getBlockState(pos);boolean exchange=block.is(EXCHANGE);if(!exchange&&!block.is(QUEST))return 0;
  var account=StockMarket.ledger.account(p.getStringUUID());if(account.guildQuests==null)account.guildQuests=new HashMap<>();String before=StockMarket.JSON.toJson(account);var inventory=new ArrayList<ItemStack>();for(int i=0;i<p.getInventory().getContainerSize();i++)inventory.add(p.getInventory().getItem(i).copy());String message="素材を売却して街の共通通貨Crを獲得できます";
  try{
   if(action.equals("sell")&&exchange){long total=0;int sold=0;for(int s=0;s<p.getInventory().getContainerSize();s++){var stack=p.getInventory().getItem(s);int i=MATERIALS.indexOf(stack.getItem());if(i>=0&&(index==-1||index==i)){sold+=stack.getCount();total=Math.addExact(total,(long)stack.getCount()*GuildCatalog.PRICES[i]);p.getInventory().setItem(s,ItemStack.EMPTY);}}account.cash=Math.addExact(account.cash,total);message=sold==0?"売却できる素材を持っていません":sold+"個の素材を売却 / +"+total+" Cr";}
   else if(!exchange&&index>=0&&index<GuildCatalog.QUESTS.length&&action.equals("accept"))message=GuildRanks.accept(account,index);
   else if(!exchange&&index>=0&&index<GuildCatalog.QUESTS.length&&action.equals("claim"))message=GuildRanks.claim(account,index);
   else if(!exchange&&action.equals("exam"))message=GuildRanks.exam(account);
   else if(!exchange&&action.equals("promote"))message=GuildRanks.promote(account);
   else if(!exchange)message=account.guildExamActive?GuildRanks.description(account):"現在ランクの依頼を5件報告 → 昇級試験 → 上位依頼を解放";
   StockMarket.save();p.getInventory().setChanged();p.containerMenu.broadcastChanges();
  }catch(Exception ex){account=StockMarket.JSON.fromJson(before,MarketLedger.Account.class);StockMarket.ledger.accounts.put(p.getStringUUID(),account);for(int i=0;i<inventory.size();i++)p.getInventory().setItem(i,inventory.get(i));p.getInventory().setChanged();p.containerMenu.broadcastChanges();message="保存に失敗したため、取引を取り消しました";}
  var data=new JsonObject();data.addProperty("guild",true);data.addProperty("cash",account.cash);data.addProperty("message",message);data.add("counts",StockMarket.JSON.toJsonTree(counts(p)));data.add("quests",StockMarket.JSON.toJsonTree(account.guildQuests));data.addProperty("rank",GuildRanks.rank(account));data.addProperty("reports",account.guildRankReports);data.addProperty("exam_active",account.guildExamActive);data.addProperty("exam_ready",GuildRanks.ready(account));data.addProperty("exam_text",GuildRanks.description(account));var specs=new JsonArray();for(int i=0;i<GuildCatalog.QUESTS.length;i++){var q=account.guildQuests.containsKey(i)?GuildRanks.accepted(account,i):GuildRanks.quest(i,GuildRanks.rank(account));var v=new JsonObject();v.addProperty("title",q.title());v.addProperty("target",q.target());v.addProperty("reward",q.reward());v.addProperty("condition",q.condition());v.addProperty("locked",GuildRanks.rank(account)<GuildRanks.minimum(i)&&!account.guildQuests.containsKey(i));specs.add(v);}data.add("specs",specs);ServerPlayNetworking.send(p,new StockMarket.Snapshot(data.toString()));return 1;
 }
}
