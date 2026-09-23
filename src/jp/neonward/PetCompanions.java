package jp.neonward;

import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.network.chat.Component;

/** Server-thread-only shop API. Ownership persists; summoned companions deliberately do not. */
public final class PetCompanions {
 public static final String[] NAMES={"犬","猫","蛇","カラス","小型ロボ"};
 public static final int OK=1,ALREADY_OWNED=2,INVALID=-1,UNAVAILABLE=-2,NO_FUNDS=-3,SAVE_FAILED=-4,NOT_OWNED=-5,NO_SPACE=-6;
 static final List<EntityType<PetEntity>> TYPES=new ArrayList<>();
 private static final Map<UUID,PetEntity> ACTIVE=new HashMap<>();
 private static boolean initialized;
 public static void init(){
  if(initialized)return;initialized=true;
  String[] ids={"dog","cat","snake","crow","robot"};
  for(int i=0;i<ids.length;i++){
   var key=ResourceKey.create(Registries.ENTITY_TYPE,NeonWard.id("pets/"+ids[i]));
   var type=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<PetEntity>of(PetEntity::new,MobCategory.MISC)
       .sized(i==2?.55f:.65f,i==2?.28f:i==3?.8f:1.05f).clientTrackingRange(8).updateInterval(2).noSave().noSummon().noLootTable().fireImmune().build(key));
   TYPES.add(type);
   FabricDefaultAttributeRegistry.register(type,Mob.createMobAttributes().add(Attributes.MAX_HEALTH,20).add(Attributes.MOVEMENT_SPEED,.28).add(Attributes.FOLLOW_RANGE,24).add(Attributes.KNOCKBACK_RESISTANCE,1));
  }
  ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->recall(handler.player));
  ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player,from,to)->recall(player));
  ServerTickEvents.END_SERVER_TICK.register(server->{
   for(var entry:new ArrayList<>(ACTIVE.entrySet())){
    var pet=entry.getValue();var owner=server.getPlayerList().getPlayer(entry.getKey());
    if(owner==null||!owner.isAlive()||owner.isSpectator()||owner.level()!=pet.level()||pet.isRemoved()||!owned(owner,pet.kind()))remove(entry.getKey());
   }
  });
  ServerEntityEvents.ENTITY_UNLOAD.register((entity,level)->{
   if(entity instanceof PetEntity pet){ACTIVE.values().removeIf(value->value==pet);if(!pet.isRemoved())pet.discard();}
  });
  ServerLifecycleEvents.SERVER_STOPPING.register(server->{for(UUID id:new ArrayList<>(ACTIVE.keySet()))remove(id);});
  ServerLifecycleEvents.SERVER_STOPPED.register(server->ACTIVE.clear());
 }
 public static void initClient(){PetRenderer.init();}
 public static long price(int kind){return PetPurchase.valid(kind)?PetPurchase.PRICES[kind]:0;}
 private static boolean available(ServerPlayer p){return p!=null&&p.level().getServer().isSameThread()&&p.isAlive()&&!p.isSpectator()&&StockMarket.ledger!=null;}
 public static boolean owned(ServerPlayer p,int kind){
  if(!PetPurchase.valid(kind)||p==null||StockMarket.ledger==null)return false;
  var account=StockMarket.ledger.accounts.get(p.getStringUUID());return account!=null&&account.petOwned!=null&&account.petOwned.contains(kind);
 }
 public static int purchase(ServerPlayer p,int kind){
  if(!PetPurchase.valid(kind))return INVALID;if(!available(p))return UNAVAILABLE;
  var ledger=StockMarket.ledger;String id=p.getStringUUID();boolean fresh=!ledger.accounts.containsKey(id);var a=ledger.account(id);
  int result=PetPurchase.buy(new PetPurchase.Account(){
   public long cash(){return a.cash;}public void cash(long value){a.cash=value;}
   public Set<Integer> pets(){return a.petOwned;}public void pets(Set<Integer> value){a.petOwned=value;}
  },kind,StockMarket::save);
  if(fresh&&result!=OK)ledger.accounts.remove(id);
  return result;
 }
 public static int summon(ServerPlayer p,int kind){
  if(!PetPurchase.valid(kind))return INVALID;if(!available(p)||TYPES.size()!=5)return UNAVAILABLE;if(!owned(p,kind))return NOT_OWNED;
  var pet=new PetEntity(TYPES.get(kind),p.level());var spot=PetSpawn.find(p,pet);
  if(spot==null){pet.discard();return NO_SPACE;}
  pet.owner=p.getUUID();pet.setPos(spot);pet.setYRot(p.getYRot());pet.setYBodyRot(p.getYRot());
  pet.setCustomName(Component.literal(NAMES[kind]+" / "+p.getName().getString()));
  // Do not destroy a working companion when the replacement spawn is rejected.
  if(!p.level().addFreshEntity(pet)){pet.discard();return UNAVAILABLE;}
  remove(p.getUUID());ACTIVE.put(p.getUUID(),pet);return OK;
 }
 public static void recall(ServerPlayer p){if(p!=null&&p.level().getServer().isSameThread())remove(p.getUUID());}
 public static int activeKind(ServerPlayer p){var pet=ACTIVE.get(p.getUUID());return pet==null||pet.isRemoved()?-1:pet.kind();}
 static boolean active(PetEntity pet){return pet.owner!=null&&ACTIVE.get(pet.owner)==pet;}
 private static void remove(UUID id){var pet=ACTIVE.remove(id);if(pet!=null&&!pet.isRemoved())pet.discard();}
 private PetCompanions(){}
}
