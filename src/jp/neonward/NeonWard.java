package jp.neonward;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.Commands;
import net.minecraft.world.phys.Vec3;

public class NeonWard implements ModInitializer {
 public static final String ID="neonward";
 public static Identifier id(String path){return Identifier.fromNamespaceAndPath(ID,path);}
 public static final EntityType<StreetVehicle> BIKE=vehicle("bike",1.0f,1.3f);
 public static final EntityType<StreetVehicle> CAR=vehicle("car",3.7f,2.6f);
 static EntityType<StreetVehicle> vehicle(String name,float w,float h){
  var key=ResourceKey.create(Registries.ENTITY_TYPE,id(name));
  return Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<StreetVehicle>of(StreetVehicle::new,MobCategory.MISC).sized(w,h).clientTrackingRange(10).updateInterval(1).build(key));
 }
 public static final Item PHONE=item("phone",0),BIKE_KEY=item("bike_key",1),CAR_KEY=item("car_key",2);
 static Item item(String name,int kind){var key=ResourceKey.create(Registries.ITEM,id(name));return Registry.register(BuiltInRegistries.ITEM,key,new TechItem(new Item.Properties().setId(key).stacksTo(1),kind));}
 public void onInitialize(){
  net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(server->{
   for(var level:server.getAllLevels())
    level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.KEEP_INVENTORY,true,server);
  });
  WestLand.init();OrnamentalFish.init();FishingShop.init();NeonFurniture.init();CityProtection.init();NeonArsenal.init();NeonHostiles.init();SpireBosses.init();SkyBosses.init();SkySpire.init();NeonZones.init();StreetFashion.init();GuildServices.init();
  WeaponDisplay.init();
  VanillaEnemyFilter.init();VendingMachines.init();StockMarket.init();Cyberware.init();if(MediaBridge.available()){NeonTelevision.init();PortableTelevision.init();}
  PetCompanions.init();
  GlitchSigns.init();HologramFish.init();ClockworkMachinery.init();
  StreetLights.init();PulseNeon.init();SouthMaterials.init();
  SakuraMaterials.init();SakuraTown.init();SakuraAccessUpgrade.init();ShrineBlessings.init();ShrineServices.init();ShrineRituals.init();AccessoryEquipment.init();
  BackpackEquipment.init();BackpackShop.init();
  AutoDoors.init();CyberwareGacha.init();WeaponSales.init();GunAttachments.init();
  LiftSystem.init();VolcanicSpire.init();
  PhoneEquipment.init();LeisureShop.init();
  TrainingRange.init();
  PhoneTravel.init();PhoneGarage.init();PrivateHomes.init();SupportedSmallBlock.init();CityResidents.init();NeonCasino.init();ParlorGames.init();InteriorShop.init();PrivateFarms.init();Medicine.init();PhoneFriends.init();PhoneServices.init();PhoneCalls.init();RealEstate.init();CityApartments.init();CombatFeedback.init();MeleeElements.init();MotorWorks.init();Underworld.init();Aquariums.init();WelcomeTutorial.init();ObjectiveTracker.init();CompactShops.init();
  CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB,Identifier.withDefaultNamespace("tools_and_utilities"))).register(e->{e.accept(BIKE_KEY);e.accept(CAR_KEY);});
  CommandRegistrationCallback.EVENT.register((dispatcher,ctx,env)->dispatcher.register(Commands.literal("neon").then(Commands.literal("kit").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(c->{var p=c.getSource().getPlayerOrException();for(Item i:new Item[]{BIKE_KEY,CAR_KEY})if(!p.getInventory().add(new ItemStack(i)))p.drop(new ItemStack(i),false);p.sendSystemMessage(Component.literal("NEON TECH: スマホは専用枠に固定、Pで開きます。キーを道路で使うと車両を配置。車両に右クリックで乗車、W/Sで前進・後退、A/Dで操舵、Spaceでブレーキ、Shiftで降車。"));return 1;}))));
 }
 static class TechItem extends Item {
  final int kind;TechItem(Properties p,int k){super(p);kind=k;}
  @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
   if(kind==0){if(!level.isClientSide())PhoneEquipment.equip(player,player.getItemInHand(hand));return InteractionResult.SUCCESS;}
   if(level instanceof ServerLevel server){
    if(player.isPassenger())return InteractionResult.PASS;
    var nearby=server.getEntitiesOfClass(StreetVehicle.class,player.getBoundingBox().inflate(64));
    if(nearby.stream().filter(v->player.getUUID().equals(v.owner)).count()>=8){player.sendOverlayMessage(Component.literal("近くに自分の車両が8台あります。Shift＋右クリックで回収できます。"));return InteractionResult.FAIL;}
    var v=new StreetVehicle(kind==1?BIKE:CAR,level);Vec3 facing=Vec3.directionFromRotation(0,player.getYRot());
    v.setPos(player.getX()+facing.x*4,player.getY()+.1,player.getZ()+facing.z*4);v.setYRot(player.getYRot());v.owner=player.getUUID();
    if(!server.noCollision(v,v.getBoundingBox())){player.sendOverlayMessage(Component.literal("広い道路に向けて配置してください。"));return InteractionResult.FAIL;}
    server.addFreshEntity(v);player.getCooldowns().addCooldown(player.getItemInHand(hand),20);
   }
   return InteractionResult.SUCCESS;
  }
 }
}

