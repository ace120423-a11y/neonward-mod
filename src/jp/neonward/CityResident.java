package jp.neonward;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.level.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.*;
import net.minecraft.network.chat.Component;

public class CityResident extends PathfinderMob {
 BlockPos home=BlockPos.ZERO;int chatUntil;boolean shopStaff;String job="bar";
 public CityResidents.Kind kind(){return CityResidents.KINDS.get(getType());}
 public CityResident(EntityType<? extends PathfinderMob> t,Level l){super(t,l);setPersistenceRequired();setCanPickUpLoot(false);setInvulnerable(true);}
 @Override protected void registerGoals(){
  goalSelector.addGoal(0,new FloatGoal(this));
  if(kind().role()<10)goalSelector.addGoal(4,new WaterAvoidingRandomStrollGoal(this,.75){@Override protected Vec3 getPosition(){if(shopStaff||tickCount<chatUntil)return null;for(int i=0;i<12;i++){var pos=home.offset(getRandom().nextInt(41)-20,0,getRandom().nextInt(41)-20);if(CityResidents.walkable(level(),pos,kind().role()>=4))return Vec3.atBottomCenterOf(pos);}return null;}});
  goalSelector.addGoal(6,new LookAtPlayerGoal(this,Player.class,6));goalSelector.addGoal(7,new RandomLookAroundGoal(this));
 }
 @Override public boolean removeWhenFarAway(double d){return false;}
 @Override public boolean hurtServer(ServerLevel l,net.minecraft.world.damagesource.DamageSource s,float amount){return false;}
 @Override public void tick(){super.tick();if(!level().isClientSide()&&(job.equals("fishing_buyer")||job.equals("fishing_rods"))){setNoAi(true);setNoGravity(true);setDeltaMovement(Vec3.ZERO);setPos(home.getX()+.5,home.getY(),home.getZ()+.5);setYRot(0);return;}if(!level().isClientSide()&&tickCount%40==0&&home!=BlockPos.ZERO){boolean slum=kind().role()>=4&&kind().role()<10;if(shopStaff||kind().role()>=10){if(distanceToSqr(Vec3.atBottomCenterOf(home))>1)setPos(home.getX()+.5,home.getY(),home.getZ()+.5);getNavigation().stop();}else if(blockPosition().distSqr(home)>1600||!CityProtection.contains(level(),blockPosition())||CityProtection.southQuarter(blockPosition())!=slum||getY()<63){getNavigation().stop();setPos(home.getX()+.5,home.getY(),home.getZ()+.5);}}}
 @Override protected InteractionResult mobInteract(Player p,InteractionHand hand){
  if(hand==InteractionHand.MAIN_HAND&&p instanceof ServerPlayer sp&&job.equals("car_sales")){sp.level().getServer().getCommands().performPrefixedCommand(sp.createCommandSourceStack(),"neongarage view");return InteractionResult.SUCCESS;}
  if(hand!=InteractionHand.MAIN_HAND)return InteractionResult.PASS;if(p instanceof ServerPlayer fisher&&FishingShop.use(fisher,this))return InteractionResult.SUCCESS;if(p instanceof ServerPlayer sp&&(job.equals("guild_food")||job.equals("compact_food"))){GuildServices.food(sp);return InteractionResult.SUCCESS;}if(p instanceof ServerPlayer sp&&CompactShops.staffUse(sp,this))return InteractionResult.SUCCESS;if(p instanceof ServerPlayer sp&&(kind().role()==8||kind().role()==9)){Underworld.request(sp,"view",0);return InteractionResult.SUCCESS;}if(p instanceof ServerPlayer sp&&(kind().role()==4||job.equals("dealer"))){MotorWorks.request(sp,"car","view");return InteractionResult.SUCCESS;}if(getName().getString().equals(RealEstate.AGENT)&&p instanceof ServerPlayer buyer){RealEstate.open(buyer);return InteractionResult.SUCCESS;}if(getName().getString().equals("家具屋・ミオ")&&p instanceof ServerPlayer seller){InteriorShop.request(seller,-1);return InteractionResult.SUCCESS;}if(job.equals("dice")&&p instanceof ServerPlayer sp){ParlorGames.request(sp,"dice","view",0);return InteractionResult.SUCCESS;}
  if(!level().isClientSide()){getNavigation().stop();getLookControl().setLookAt(p,30,30);chatUntil=tickCount+70;if(kind().role()<10)p.sendSystemMessage(Component.literal(getDisplayName().getString()+"："+(shopStaff&&!job.equals("bar")?shopDialogue():shopStaff?new String[]{"いらっしゃい。空いている席でゆっくりしていって。","今夜も営業中だよ。外の仕事、お疲れさま。","ここならひと息つける。くつろいでいきな。"}[getRandom().nextInt(3)]:CityResidents.dialogue(kind().role(),getRandom().nextInt(3)))));}
  return InteractionResult.SUCCESS;
 }
 String shopDialogue(){return switch(job){case "arms"->"刀から銃まで揃えている。装備を見ていきな。";case "armor"->"防具や装備の相談ならこちらへ。";case "fashion"->"和服も革ジャンもあるよ。カウンターで服を選んでね。";case "dealer"->"車種と性能を選んで、配車を試してみてください。";case "casino"->"1回100 Cr。倍率とルールを確認して遊んでね。";case "guild_food","compact_food"->"本日のギルド配給です。パンと携帯食をどうぞ。";default->"いらっしゃい。ゆっくり見ていって。";};}
 @Override protected void addAdditionalSaveData(ValueOutput o){super.addAdditionalSaveData(o);o.putBoolean("ShopStaff",shopStaff);o.putString("ShopJob",job);o.putInt("HomeX",home.getX());o.putInt("HomeY",home.getY());o.putInt("HomeZ",home.getZ());}
 @Override protected void readAdditionalSaveData(ValueInput i){super.readAdditionalSaveData(i);shopStaff=i.getBooleanOr("ShopStaff",false);job=i.getStringOr("ShopJob","bar");home=new BlockPos(i.getIntOr("HomeX",blockPosition().getX()),i.getIntOr("HomeY",65),i.getIntOr("HomeZ",blockPosition().getZ()));setInvulnerable(true);}
}
