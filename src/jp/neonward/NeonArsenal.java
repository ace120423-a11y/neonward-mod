package jp.neonward;
import java.util.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraft.world.item.equipment.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.*;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.component.DataComponents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public final class NeonArsenal {
 static final java.util.Map<java.util.UUID,Long> LAST_SHOT=new java.util.concurrent.ConcurrentHashMap<>();
 public static final Map<String,Item> ITEMS=new LinkedHashMap<>();
 public static Item CELL,BLADE,RIFLE;
 public static final java.util.List<Item> DROPS=new java.util.ArrayList<>();
 static Item.Properties properties(String name){return new Item.Properties().setId(ResourceKey.create(Registries.ITEM,NeonWard.id(name)));}
 static Item add(String name,Item item){Registry.register(BuiltInRegistries.ITEM,NeonWard.id(name),item);ITEMS.put(name,item);return item;}
 public static void init(){
  CommandRegistrationCallback.EVENT.register((d,c,e)->d.register(Commands.literal("neongun").then(Commands.literal("fire").executes(ctx->{var p=ctx.getSource().getPlayerOrException();if(p.isSpectator()||!p.isAlive())return 0;var hand=gunHand(p);if(p.getItemInHand(hand).getItem() instanceof Rifle gun){long now=System.currentTimeMillis(),gap=automatic(p.getItemInHand(hand))?280:140,last=LAST_SHOT.getOrDefault(p.getUUID(),0L);if(now-last<gap)return 0;LAST_SHOT.put(p.getUUID(),now);return gun.fire(p.level(),p,hand)==InteractionResult.FAIL?0:1;}return 0;}))));
  CELL=add("energy_cell",new Item(properties("energy_cell")));
  BLADE=add("neon_blade",new Item(properties("neon_blade").sword(ToolMaterial.DIAMOND,4,-2.2f).repairable(Items.IRON_INGOT)));
  RIFLE=add("pulse_rifle",new Rifle(properties("pulse_rifle").durability(900).enchantable(15).repairable(Items.IRON_INGOT)));
  add("kurosame_katana",new Item(properties("kurosame_katana").sword(ToolMaterial.DIAMOND,4,-2.4f).repairable(Items.IRON_INGOT)));
  add("akatsuki_wakizashi",new Item(properties("akatsuki_wakizashi").sword(ToolMaterial.IRON,3,-1.8f).repairable(Items.IRON_INGOT)));
  add("raikiri_odachi",new Item(properties("raikiri_odachi").sword(ToolMaterial.NETHERITE,6,-3f).repairable(Items.IRON_INGOT)));
  add("kestrel_pistol",new Rifle(properties("kestrel_pistol").durability(700).enchantable(15).repairable(Items.IRON_INGOT),7,7,40,0x5dfff0,1.9f));
  add("oni_handcannon",new Rifle(properties("oni_handcannon").durability(850).enchantable(15).repairable(Items.IRON_INGOT),12,16,48,0xffbf58,1.25f));
  add("wisp_compact",new Rifle(properties("wisp_compact").durability(550).enchantable(15).repairable(Items.IRON_INGOT),5,5,28,0xff59b4,2f));
  add("shock_bat",new Item(properties("shock_bat").sword(ToolMaterial.IRON,3,-1.7f).repairable(Items.IRON_INGOT)));
  add("riot_bat",new Item(properties("riot_bat").sword(ToolMaterial.DIAMOND,4,-2.2f).repairable(Items.IRON_INGOT)));
  add("coil_hammer",new Item(properties("coil_hammer").sword(ToolMaterial.DIAMOND,7,-3.0f).repairable(Items.IRON_INGOT)));
  add("pile_maul",new Item(properties("pile_maul").sword(ToolMaterial.NETHERITE,10,-3.3f).repairable(Items.IRON_INGOT)));
  add("longwatch_sniper",new Rifle(properties("longwatch_sniper").durability(950).enchantable(15).repairable(Items.IRON_INGOT),28,36,160,0x79eaff,.85f));
  add("storm_machinegun",new Rifle(properties("storm_machinegun").durability(1600).enchantable(15).repairable(Items.IRON_INGOT),6,3,64,0xffbb55,1.6f));
  DROPS.addAll(ITEMS.values().stream().filter(i->i!=CELL).toList());
  net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH.register((entity,source)->{if(!Underworld.hunter(entity)&&entity instanceof net.minecraft.world.entity.monster.Enemy&&source.getEntity() instanceof net.minecraft.server.level.ServerPlayer&&entity.level() instanceof ServerLevel level){var random=java.util.concurrent.ThreadLocalRandom.current();int index=WeaponLoot.roll(random,DROPS.size(),NeonLoot.profile(entity),GunEnchantments.looting(source,level));if(index>=0)level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(level,entity.getX(),entity.getY()+.3,entity.getZ(),RolledWeapons.create(DROPS.get(index),random,NeonLoot.profile(entity))));}});
  for(var type:List.of(ArmorType.HELMET,ArmorType.CHESTPLATE,ArmorType.LEGGINGS,ArmorType.BOOTS)){String name="sentinel_"+type.getName();add(name,new Item(StreetFashion.cosmetic(properties(name),"sentinel",type.getSlot())));}
  CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB,net.minecraft.resources.Identifier.withDefaultNamespace("combat"))).register(e->ITEMS.values().forEach(e::accept));
  CommandRegistrationCallback.EVENT.register((d,c,e)->d.register(Commands.literal("neon").then(Commands.literal("arsenal").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(ctx->{var p=ctx.getSource().getPlayerOrException();for(var i:ITEMS.values()){if(i==CELL)continue;var stack=new ItemStack(i);if(!p.getInventory().add(stack))p.drop(stack,false);}p.sendSystemMessage(Component.literal("NEON ARSENAL：武器・防具一式（弾無限）。刀は左クリック、銃は右長押しで構え、左で発射（マシンガンは長押し連射）。"));return 1;}))));
  ServerTickEvents.END_SERVER_TICK.register(server->{for(var p:server.getPlayerList().getPlayers())for(int i=0;i<p.getInventory().getContainerSize();i++){var s=p.getInventory().getItem(i);if(ITEMS.containsValue(s.getItem()))s.set(DataComponents.DAMAGE,0);}});
 }
 public static boolean isGun(ItemStack stack){return stack.getItem() instanceof Rifle;}
 public static boolean automatic(ItemStack stack){return stack.is(ITEMS.get("storm_machinegun"));}
 public static boolean sniper(ItemStack stack){return stack.is(ITEMS.get("longwatch_sniper"));}
 public static InteractionHand gunHand(Player p){return isGun(p.getMainHandItem())?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND;}
 public static class Rifle extends Item {
  final float damage,pitch;final int delay,range,color;
  Rifle(Properties p){this(p,10,10,64,0x5dfff0,1.8f);}
  Rifle(Properties p,float damage,int delay,int range,int color,float pitch){super(p);this.damage=damage;this.delay=delay;this.range=range;this.color=color;this.pitch=pitch;}
  @Override public int getUseDuration(ItemStack stack,LivingEntity entity){return 72000;}
  @Override public ItemUseAnimation getUseAnimation(ItemStack stack){return ItemUseAnimation.BOW;}
  @Override public InteractionResult use(Level world,Player p,InteractionHand hand){p.startUsingItem(hand);return InteractionResult.CONSUME;}
  InteractionResult fire(Level world,Player p,InteractionHand hand){
   ItemStack gun=p.getItemInHand(hand);if(p.getCooldowns().isOnCooldown(gun))return InteractionResult.FAIL;
   if(!(world instanceof ServerLevel l))return InteractionResult.SUCCESS;
   if(automatic(gun)){var data=gun.getOrDefault(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag();long reloadUntil=data.getLongOr("neon_reload_until",0L);if(System.currentTimeMillis()<reloadUntil)return InteractionResult.FAIL;int shots=data.getIntOr("neon_magazine",0);if(shots>=100){net.minecraft.world.item.component.CustomData.update(DataComponents.CUSTOM_DATA,gun,t->{t.putLong("neon_reload_until",System.currentTimeMillis()+1200);t.putInt("neon_magazine",0);});p.sendOverlayMessage(Component.literal("マシンガンをリロード中…"));return InteractionResult.FAIL;}}
   p.getCooldowns().addCooldown(gun,delay);
   Vec3 start=p.getEyePosition(),end=start.add(p.getLookAngle().scale(range));
   var wall=l.clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));end=wall.getLocation();
   var hit=ProjectileUtil.getEntityHitResult(p,start,end,p.getBoundingBox().expandTowards(end.subtract(start)).inflate(1),e->e instanceof LivingEntity&&!(e instanceof Player)&&!(e instanceof net.minecraft.world.entity.decoration.ArmorStand)&&e.isAlive()&&!e.isSpectator(),start.distanceToSqr(end));
   if(hit!=null){end=hit.getLocation();hit.getEntity().invulnerableTime=0;var cyber=p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getModifier(NeonWard.id("cyberware_2"));boolean damaged=hit.getEntity().hurtServer(l,p.damageSources().playerAttack(p),GunEnchantments.damage(l,gun,damage*(float)RolledWeapons.multiplier(gun)*(1+(cyber==null?0:(float)cyber.amount()))));if(damaged&&hit.getEntity() instanceof LivingEntity victim)GunEnchantments.impact(l,gun,victim,p.getLookAngle());}
   Vec3 ray=end.subtract(start);int steps=Math.max(1,(int)(ray.length()*2));
   for(int i=1;i<=steps;i++){Vec3 at=start.add(ray.scale((double)i/steps));l.sendParticles(new DustParticleOptions(color,1.1f),at.x,at.y,at.z,1,0,0,0,0);}
   l.playSound(null,p.blockPosition(),SoundEvents.FIREWORK_ROCKET_BLAST,SoundSource.PLAYERS,.55f,pitch);
   if(NeonArsenal.automatic(gun))net.minecraft.world.item.component.CustomData.update(DataComponents.CUSTOM_DATA,gun,t->{t.putInt("neon_magazine",t.getIntOr("neon_magazine",0)+1);});return InteractionResult.SUCCESS;
  }
 }
}
