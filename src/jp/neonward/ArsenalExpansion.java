package jp.neonward;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.*;

/** Bounded server-side abilities. No explosions modify blocks, no client-provided targets. */
public final class ArsenalExpansion {
 public static final String[] MELEE={"volt_spear","chain_kusarigama","neon_dualblades","reaper_scythe","impact_gauntlet"};
 public static final String[] RANGED={"ion_railgun","plasma_launcher","arc_caster","cryo_projector","tactical_crossbow"};
 record Shot(ServerPlayer owner,ServerLevel level,ItemStack weapon,Vec3 position,Vec3 velocity,int life,float damage,int mode){}
 record Echo(ServerPlayer owner,LivingEntity target,float damage,int due){}
 static final List<Shot> SHOTS=new ArrayList<>();static final List<Echo> ECHOES=new ArrayList<>();
 static final Map<UUID,Integer> MELEE_NEXT=new HashMap<>();
 public static boolean melee(ItemStack s){return s.getItem() instanceof Blade;}
 static boolean boss(LivingEntity e){return e instanceof SpireBoss||e instanceof SkyBoss;}
 static boolean enemy(Player p,LivingEntity e){return e instanceof Enemy&&e.isAlive()&&!e.isAlliedTo(p)&&e.level()==p.level();}
 static int type(ItemStack s){return s.getItem() instanceof Blade b?b.type:-1;}
 static Item.Properties described(Item.Properties p,String text){return p.component(DataComponents.LORE,new net.minecraft.world.item.component.ItemLore(List.of(Component.literal(text).withColor(0x80ffee))));}
 public static void register(){
  float[] damage={5,4,2,6,5},speed={-2.6f,-2.4f,-1.5f,-2.9f,-2.3f};
  String[] meleeTips={"50%軽減 / 長リーチ突き / 右長押し→離す：溜め突進","50%軽減 / 右クリック：敵を引き寄せ / ボスは引き寄せ無効","45%軽減 / 出血：5秒間・毎秒2ダメージ / 右長押し：クロスガード・正面の弾を弾く","50%軽減 / 広い横薙ぎ・撃破時回復 / 右：薙ぎ払い","50%軽減 / 爆破＋ノックバック・地形破壊なし / 右長押し：溜めパンチ"};
  String[] gunTips={"右長押し1秒→左：貫通射撃","左：低速プラズマ弾 / 範囲爆発・地形破壊なし","左：最大4体へ連鎖電撃","左長押し：冷気連射 / ボスは弱い減速のみ","左：ボルト発射 / しゃがみ＋右：爆発・毒・電撃切替"};
  for(int i=0;i<MELEE.length;i++)NeonArsenal.add(MELEE[i],new Blade(described(NeonArsenal.properties(MELEE[i]).sword(ToolMaterial.DIAMOND,damage[i],speed[i]).repairable(Items.IRON_INGOT),meleeTips[i]),i));
  for(int i=0;i<RANGED.length;i++)NeonArsenal.add(RANGED[i],new Launcher(described(NeonArsenal.properties(RANGED[i]).durability(1200).enchantable(15).repairable(Items.IRON_INGOT),gunTips[i]),i));
  PairedHands.init();
  PairedEffects.init();
  ChainPull.init();
  ServerTickEvents.END_SERVER_TICK.register(ArsenalExpansion::tick);
  ServerLifecycleEvents.SERVER_STOPPED.register(s->{SHOTS.clear();ECHOES.clear();MELEE_NEXT.clear();});
  net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((h,s)->{MELEE_NEXT.remove(h.player.getUUID());SHOTS.removeIf(v->v.owner()==h.player);ECHOES.removeIf(v->v.owner()==h.player);});
 }
 static void particles(ServerLevel l,Vec3 a,Vec3 b,int color){int n=Math.max(1,Math.min(28,(int)(a.distanceTo(b)*2)));for(int i=0;i<=n;i++){var v=a.lerp(b,(double)i/n);l.sendParticles(new DustParticleOptions(color,.85f),v.x,v.y,v.z,1,0,0,0,0);}}
 static void burst(ServerLevel l,Vec3 center,int color){for(int i=0;i<16;i++){double a=i*Math.PI/8;var p=center.add(Math.cos(a)*1.5,.2,Math.sin(a)*1.5);l.sendParticles(new DustParticleOptions(color,1),p.x,p.y,p.z,1,0,0,0,0);}}
 static float scaled(ServerPlayer p,ItemStack s,float base){var cyber=p.getAttribute(Attributes.ATTACK_DAMAGE).getModifier(NeonWard.id("cyberware_2"));return GunEnchantments.damage(p.level(),s,base*(float)RolledWeapons.multiplier(s)*(1+(cyber==null?0:(float)cyber.amount())));}
 static boolean hurt(ServerPlayer p,LivingEntity e,float damage){return enemy(p,e)&&e.hurtServer(p.level(),p.damageSources().playerAttack(p),damage);}
 static void push(ServerPlayer p,LivingEntity e,double strength){if(boss(e))return;var d=e.position().subtract(p.position());e.knockback(strength,-d.x,-d.z,p.damageSources().playerAttack(p),0);}
 static List<LivingEntity> targets(ServerPlayer p,double range,double halfAngle){var look=MeleeReach.facing(p);var list=p.level().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(range,2,range),e->{var d=e.position().subtract(p.position());return enemy(p,e)&&Math.abs(d.y)<2.5&&d.lengthSqr()<=range*range&&d.normalize().dot(look)>=Math.cos(halfAngle)&&p.hasLineOfSight(e);});list.sort(Comparator.comparingDouble(p::distanceToSqr));return list.stream().limit(8).toList();}
 static void dash(ServerPlayer p,Vec3 direction,double distance){var start=p.position();var last=start;for(double step=.25;step<=distance;step+=.25){var next=start.add(direction.scale(step));var at=BlockPos.containing(next);if(!p.level().hasChunkAt(at)||!p.level().getWorldBorder().isWithinBounds(at)||!p.level().noCollision(p,p.getBoundingBox().move(next.subtract(start)))||p.level().getBlockState(at.below()).getCollisionShape(p.level(),at.below()).isEmpty())break;last=next;}if(!last.equals(start)){p.teleportTo(p.level(),last.x,last.y,last.z,Set.of(),p.getYRot(),p.getXRot(),true);p.fallDistance=0;}particles(p.level(),start.add(0,.5,0),last.add(0,.5,0),0x70fff0);}
 public static void impact(ServerPlayer p,LivingEntity victim,ItemStack s,float damage){int t=type(s);if(t<0)return;
  if(t==3&&!victim.isAlive())p.heal(2);
  int now=p.level().getServer().getTickCount();if(now<MELEE_NEXT.getOrDefault(p.getUUID(),0))return;MELEE_NEXT.put(p.getUUID(),now+10);
  particles(p.level(),p.getEyePosition(),victim.position().add(0,1,0),t==3?0xb978ff:0x63fff0);
  if(t==2){PairedEffects.bleed(p,victim);if(ECHOES.size()<48&&enemy(p,victim))ECHOES.add(new Echo(p,victim,damage*.35f,now+5));return;}
  if(t==4){PairedEffects.blast(p,victim,damage*.3f,.9);return;}
  for(var e:targets(p,t==0?4.5:t==3?3.6:t==1?3.5:2.1,t==0?.15:t==3?1.25:.65))if(e!=victim&&hurt(p,e,damage*.3f)){if(t==3&&!e.isAlive())p.heal(1);if(t==4)push(p,e,.7);}
 }
 public static class Blade extends Item {
  final int type;Blade(Properties p,int t){super(p);type=t;}
  public int getUseDuration(ItemStack s,LivingEntity e){return 72000;}
  public ItemUseAnimation getUseAnimation(ItemStack s){return type==2?ItemUseAnimation.BLOCK:ItemUseAnimation.BOW;}
  public InteractionResult use(Level l,Player p,InteractionHand hand){if(hand!=InteractionHand.MAIN_HAND||p.isSpectator()||p.getCooldowns().isOnCooldown(p.getItemInHand(hand)))return InteractionResult.FAIL;if(type==0||type==2||type==4)p.startUsingItem(hand);else if(p instanceof ServerPlayer sp)skill(sp,p.getItemInHand(hand),type,1);return InteractionResult.CONSUME;}
  public boolean releaseUsing(ItemStack s,Level l,LivingEntity e,int remaining){int charge=72000-remaining;if((type==0||type==4)&&e instanceof ServerPlayer p&&p.getMainHandItem()==s&&charge>=10&&!p.getCooldowns().isOnCooldown(s)){skill(p,s,type,Math.min(1.75f,1+charge/40f));return true;}return false;}
 }
 static void skill(ServerPlayer p,ItemStack s,int type,float power){if(type==2||!p.isAlive()||p.isPassenger())return;p.getCooldowns().addCooldown(s,45);var front=MeleeReach.facing(p);
  if(type==1){var victims=targets(p,9,.35);ChainPull.launch(p,victims.isEmpty()?null:victims.getFirst(),scaled(p,s,7));return;}
  if(type==0)dash(p,front,3.5);
  var ts=targets(p,type==1?9:type==0?5:type==3?3.8:2.8,type==1?.35:type==0?.25:1.2);
  if(type==4){for(var e:ts){float damage=scaled(p,s,16*power);if(hurt(p,e,damage)){PairedEffects.blast(p,e,damage*.35f,1.5);break;}}return;}
  for(var e:ts){
   if(hurt(p,e,scaled(p,s,(type==4?16:12)*power))){if(type==4)push(p,e,1.5);if(type==3&&!e.isAlive())p.heal(2);}
  }burst(p.level(),p.position().add(front.scale(1.5)),type==3?0xb978ff:0xffcb73);
 }
 public static class Launcher extends NeonArsenal.Rifle {
  final int type;Launcher(Properties p,int t){super(p,new float[]{32,18,10,5,12}[t],new int[]{35,30,14,6,18}[t],new int[]{100,45,32,16,60}[t],new int[]{0x65ecff,0xff60c8,0xffe26a,0x9deeff,0x90ff98}[t],1);type=t;}
  @Override public InteractionResult use(Level l,Player p,InteractionHand hand){if(type==4&&p.isShiftKeyDown()){if(!l.isClientSide()){var s=p.getItemInHand(hand);if(p.getCooldowns().isOnCooldown(s))return InteractionResult.FAIL;CustomData.update(DataComponents.CUSTOM_DATA,s,d->d.putInt("neon_bolt",(d.getIntOr("neon_bolt",0)+1)%3));p.getCooldowns().addCooldown(s,8);p.sendOverlayMessage(Component.literal("ボルト："+new String[]{"爆発","毒","電撃"}[mode(s)]));}return InteractionResult.CONSUME;}return super.use(l,p,hand);}
  @Override InteractionResult fire(Level world,Player player,InteractionHand hand){if(!(player instanceof ServerPlayer p))return InteractionResult.SUCCESS;var gun=p.getItemInHand(hand);if(!p.isAlive()||p.isSpectator()||p.getCooldowns().isOnCooldown(gun))return InteractionResult.FAIL;
   if(type==0&&(!p.isUsingItem()||p.getUseItem()!=gun||p.getTicksUsingItem()<20)){p.sendOverlayMessage(Component.literal("右長押しで1秒チャージしてから発射"));return InteractionResult.FAIL;}
   if((type==1||type==4)&&(SHOTS.size()>=64||SHOTS.stream().filter(s->s.owner()==p).count()>=6))return InteractionResult.FAIL;
   p.getCooldowns().addCooldown(gun,delay);var l=p.level();var start=p.getEyePosition();var end=l.clip(new ClipContext(start,start.add(p.getLookAngle().scale(range)),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p)).getLocation();float dmg=scaled(p,gun,damage);
   if(type==1||type==4){SHOTS.add(new Shot(p,l,gun.copy(),start,p.getLookAngle().scale(type==1?.65:2.0),type==1?70:35,dmg,type==1?-1:mode(gun)));}
   else if(type==0){var beamEnd=end;var list=l.getEntitiesOfClass(LivingEntity.class,new AABB(start,end).inflate(1),e->enemy(p,e)&&e.getBoundingBox().inflate(.15).clip(start,beamEnd).isPresent());list.sort(Comparator.comparingDouble(p::distanceToSqr));for(var e:list.stream().limit(6).toList())if(hurt(p,e,dmg))GunEnchantments.impact(l,gun,e,p.getLookAngle());particles(l,start,end,color);p.stopUsingItem();}
   else if(type==2){var first=ProjectileUtil.getEntityHitResult(p,start,end,p.getBoundingBox().expandTowards(end.subtract(start)).inflate(1),e->e instanceof LivingEntity v&&enemy(p,v),start.distanceToSqr(end));if(first!=null){var e=(LivingEntity)first.getEntity();arc(p,e,dmg);end=first.getLocation();}particles(l,start,end,color);}
   else {for(var e:targets(p,16,.38)){if(hurt(p,e,dmg))e.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOWNESS,40,boss(e)?0:2));}particles(l,start,end,color);}
   l.playSound(null,p.blockPosition(),SoundEvents.FIREWORK_ROCKET_BLAST,SoundSource.PLAYERS,.4f,1.3f);return InteractionResult.SUCCESS;
  }
 }
 static int mode(ItemStack s){return Math.floorMod(s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("neon_bolt",0),3);}
 static void arc(ServerPlayer p,LivingEntity first,float damage){var seen=new HashSet<UUID>();var current=first;for(int n=0;n<4;n++){seen.add(current.getUUID());hurt(p,current,damage);var from=current;var candidates=p.level().getEntitiesOfClass(LivingEntity.class,current.getBoundingBox().inflate(5),e->enemy(p,e)&&!seen.contains(e.getUUID())&&from.hasLineOfSight(e));if(candidates.isEmpty())break;candidates.sort(Comparator.comparingDouble(current::distanceToSqr));var next=candidates.getFirst();particles(p.level(),current.position().add(0,1,0),next.position().add(0,1,0),0xffe26a);current=next;damage*=.8f;}}
 static void detonate(Shot s,Vec3 at,LivingEntity direct){var p=s.owner();var l=p.level();int mode=s.mode();burst(l,at,mode==-1?0xff60c8:mode==1?0x90ff98:0xffdf73);
  if(mode<=0){int hit=0;for(var e:l.getEntitiesOfClass(LivingEntity.class,new AABB(at,at).inflate(3),e->enemy(p,e))){if(hit>=8)break;var eye=e.getEyePosition();if(l.clip(new ClipContext(at,eye,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p)).getType()!=HitResult.Type.MISS)continue;if(hurt(p,e,s.damage())){hit++;GunEnchantments.impact(l,s.weapon(),e,e.position().subtract(at));}}}
  else if(direct!=null&&hurt(p,direct,s.damage()))MeleeElements.applyElement(p,direct,mode==1?MeleeElements.Kind.POISON:MeleeElements.Kind.THUNDER,l.getGameTime());
 }
 static void tick(net.minecraft.server.MinecraftServer server){int now=server.getTickCount();for(var e:new ArrayList<>(ECHOES))if(now>=e.due()){ECHOES.remove(e);if(e.owner().isAlive()&&!e.owner().hasDisconnected()&&enemy(e.owner(),e.target())&&e.owner().distanceToSqr(e.target())<16&&e.owner().hasLineOfSight(e.target())){e.target().invulnerableTime=0;hurt(e.owner(),e.target(),e.damage());particles(e.owner().level(),e.owner().getEyePosition(),e.target().position().add(0,1,0),0xff83ce);}}
  for(var s:new ArrayList<>(SHOTS)){SHOTS.remove(s);var p=s.owner();if(!p.isAlive()||p.hasDisconnected()||p.level()!=s.level()||s.life()<=0||p.position().distanceToSqr(s.position())>160*160)continue;var l=p.level();var next=s.position().add(s.velocity());if(!l.hasChunkAt(BlockPos.containing(next)))continue;
   var wall=l.clip(new ClipContext(s.position(),next,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));var end=wall.getLocation();var hit=ProjectileUtil.getEntityHitResult(p,s.position(),end,new AABB(s.position(),end).inflate(1),e->e instanceof LivingEntity v&&enemy(p,v),s.position().distanceToSqr(end));
   particles(l,s.position(),hit!=null?hit.getLocation():end,s.mode()==-1?0xff60c8:0x9affad);
   if(hit!=null)detonate(s,hit.getLocation(),(LivingEntity)hit.getEntity());else if(wall.getType()!=HitResult.Type.MISS)detonate(s,end.subtract(s.velocity().normalize().scale(.1)),null);else SHOTS.add(new Shot(p,l,s.weapon(),next,s.velocity(),s.life()-1,s.damage(),s.mode()));
  }
 }
}
