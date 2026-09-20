package jp.neonward;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.Difficulty;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;

public final class NeonHostiles {
 public static final Map<EntityType<?>,HostileRoster.Kind> KINDS=new LinkedHashMap<>();
 public static final Map<String,EntityType<CyberEnemy>> TYPES=new LinkedHashMap<>();
 public static final List<Item> EGGS=new ArrayList<>();
 public static void init(){
  for(var k:HostileRoster.ALL){
   var key=ResourceKey.create(Registries.ENTITY_TYPE,NeonWard.id(k.id()));
   var type=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<CyberEnemy>of(CyberEnemy::new,MobCategory.MONSTER).sized(.6f,1.95f).clientTrackingRange(10).notInPeaceful().noLootTable().build(key));
   KINDS.put(type,k);TYPES.put(k.id(),type);
   FabricDefaultAttributeRegistry.register(type,CyberEnemy.createAttributes().add(Attributes.MAX_HEALTH,k.hp()).add(Attributes.ARMOR,k.armor()).add(Attributes.MOVEMENT_SPEED,k.speed()).add(Attributes.ATTACK_DAMAGE,k.damage()).add(Attributes.FOLLOW_RANGE,48).add(Attributes.SPAWN_REINFORCEMENTS_CHANCE,0).add(Attributes.KNOCKBACK_RESISTANCE,k.armor()>=10?.65:0).add(Attributes.SCALE,k.id().equals("iron_colossus")?1.45:k.id().equals("riot_bulwark")?1.15:1));
   var ik=ResourceKey.create(Registries.ITEM,NeonWard.id(k.id()+"_spawn_egg"));
   EGGS.add(Registry.register(BuiltInRegistries.ITEM,ik,new SpawnEggItem(new Item.Properties().setId(ik).spawnEgg(type))));
  }
  CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB,Identifier.withDefaultNamespace("spawn_eggs"))).register(e->EGGS.forEach(e::accept));
  ServerTickEvents.END_SERVER_TICK.register(server->{
   if(server.getTickCount()%100!=0)return;
   var l=server.overworld();if(l==null||l.getDifficulty()==Difficulty.PEACEFUL||!l.isSpawningMonsters()||!l.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS))return;
   int total=0;for(var e:l.getAllEntities())if(e instanceof CyberEnemy)total++;if(total>=96)return;
   for(var p:l.players()){
    if(total>=96)break;
    if(p.isSpectator()||!p.isAlive()||!NeonZones.isField(l,p.blockPosition()))continue;
    if(l.getEntitiesOfClass(CyberEnemy.class,p.getBoundingBox().inflate(96)).size()>=16)continue;
    for(int tries=0;tries<12;tries++){
     double a=l.getRandom().nextDouble()*Math.PI*2,r=28+l.getRandom().nextInt(33);
     int x=(int)Math.floor(p.getX()+Math.cos(a)*r),z=(int)Math.floor(p.getZ()+Math.sin(a)*r);
     if(!NeonZones.isField(l,new BlockPos(x,80,z))||!l.hasChunk(x>>4,z>>4))continue;
     int y=l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);var pos=new BlockPos(x,y,z);
     if(!l.getBlockState(pos.below()).isFaceSturdy(l,pos.below(),Direction.UP)||!l.getFluidState(pos).isEmpty()||l.players().stream().anyMatch(q->q.distanceToSqr(x+.5,y,z+.5)<24*24))continue;
     var biome=l.getBiome(pos).unwrapKey().map(key->key.identifier().getPath()).orElse("plains");var k=HostileRoster.chooseForBiome(java.util.concurrent.ThreadLocalRandom.current(),biome);var mob=new CyberEnemy(TYPES.get(k.id()),l);mob.setPos(x+.5,y,z+.5);
     if(!l.noCollision(mob,mob.getBoundingBox())||l.containsAnyLiquid(mob.getBoundingBox()))continue;
     if(l.addFreshEntity(mob))total++;break;
    }
   }
  });
 }
}
