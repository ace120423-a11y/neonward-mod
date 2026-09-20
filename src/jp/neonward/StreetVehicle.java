package jp.neonward;
import java.util.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.*;
import net.minecraft.world.*;
import net.minecraft.nbt.TagParser;
import net.minecraft.util.ProblemReporter;
import net.minecraft.network.chat.Component;

public class StreetVehicle extends Entity {
 static final net.minecraft.network.syncher.EntityDataAccessor<Integer> SPEED=SynchedEntityData.defineId(StreetVehicle.class,net.minecraft.network.syncher.EntityDataSerializers.INT);
 public int speedKmh(){return entityData.get(SPEED);}
 static final net.minecraft.network.syncher.EntityDataAccessor<String> VARIANT=SynchedEntityData.defineId(StreetVehicle.class,net.minecraft.network.syncher.EntityDataSerializers.STRING);
 public VehicleCatalog.Spec spec(){var s=VehicleCatalog.get(entityData.get(VARIANT));return s!=null&&s.bike()==bike()?s:VehicleCatalog.get(bike()?"bike":"car");}
 public void variant(String id){var s=VehicleCatalog.get(id);if(s==null||s.bike()!=bike())throw new IllegalArgumentException(id);entityData.set(VARIANT,id);modelVersion=0;}
 final net.minecraft.core.NonNullList<net.minecraft.world.item.ItemStack> cargo=net.minecraft.core.NonNullList.withSize(54,net.minecraft.world.item.ItemStack.EMPTY);
 boolean showroom;public UUID owner;private double speed;private int modelVersion;
 public StreetVehicle(EntityType<? extends StreetVehicle> type,Level level){super(type,level);}
 boolean bike(){return getType()==NeonWard.BIKE;}
 void refreshModel(){modelVersion=0;}
 void stopForDelivery(){speed=0;setDeltaMovement(Vec3.ZERO);}
 @Override protected void defineSynchedData(SynchedEntityData.Builder b){b.define(SPEED,0);b.define(VARIANT,"");}
 @Override protected void readAdditionalSaveData(ValueInput i){speed=0;showroom=i.getBooleanOr("Showroom",false);entityData.set(VARIANT,i.getStringOr("Variant",bike()?"bike":"car"));net.minecraft.world.ContainerHelper.loadAllItems(i,cargo);modelVersion=i.getIntOr("ModelVersion",0);try{owner=UUID.fromString(i.getStringOr("Owner",""));}catch(IllegalArgumentException e){owner=null;}}
 @Override protected void addAdditionalSaveData(ValueOutput o){o.putBoolean("Showroom",showroom);o.putString("Variant",spec().id());net.minecraft.world.ContainerHelper.saveAllItems(o,cargo);o.putInt("ModelVersion",modelVersion);if(owner!=null)o.putString("Owner",owner.toString());}
 @Override public boolean hurtServer(ServerLevel l,DamageSource s,float amount){return false;}
 @Override public boolean isPickable(){return true;}
 @Override public boolean canBeCollidedWith(Entity e){return !hasPassenger(e);}
 @Override protected boolean canAddPassenger(Entity e){return e instanceof Display || (e instanceof Player&&getPassengers().stream().filter(p->p instanceof Player).count()<spec().seats());}
 @Override public InteractionResult interact(Player p,InteractionHand hand,Vec3 at){
  if(!level().isClientSide()){
   if(entityTags().contains("nw_recovery")&&!p.getUUID().equals(owner)){p.sendOverlayMessage(Component.literal("依頼主専用の回収車です"));return InteractionResult.FAIL;}
   if(showroom){p.sendOverlayMessage(Component.literal("展示車です。受付かスマホのガレージから呼び出せます。"));return InteractionResult.SUCCESS;}
   if(p.isShiftKeyDown()){
    if(entityTags().contains("nw_recovery"))return InteractionResult.FAIL;
    if(owner!=null&&!owner.equals(p.getUUID())){p.sendOverlayMessage(Component.literal("荷室を開けられるのは所有者だけです。"));return InteractionResult.FAIL;}
    openCargo(p);
   }else {if(!canAddPassenger(p)){p.sendOverlayMessage(Component.literal("満席です。"));return InteractionResult.FAIL;}p.startRiding(this,true,true);p.setYRot(getYRot());p.setXRot(12);}

  }
  return InteractionResult.SUCCESS;
 }
 @Override public void tick(){
  super.tick();
  if(level().isClientSide())return;
  if(modelVersion!=4){for(var part:List.copyOf(getPassengers()))if(part instanceof Display)part.discard();}
  if(getPassengers().stream().noneMatch(e->e instanceof Display)){buildModel();modelVersion=4;}
  if(spec().id().equals("teapot")&&tickCount%16==0){double r=Math.toRadians(getYRot());((ServerLevel)level()).sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,getX()-Math.sin(r)*1.46,getY()+4.08,getZ()+Math.cos(r)*1.46,2,.09,.12,.09,.014);}
  if(showroom)return;
  Player rider=(Player)getPassengers().stream().filter(e->e instanceof Player).findFirst().orElse(null);
  if(rider instanceof ServerPlayer sp){
   var in=sp.getLastClientInput();int throttle=(in.forward()?1:0)-(in.backward()?1:0);
   speed=VehiclePhysics.speed(speed,throttle,in.jump(),MotorWorks.performance(this));
   if(in.jump())speed*=1-.09*MotorWorks.tune(this).handling;
   float steer=((in.left()?-1:0)+(in.right()?1:0))*(1+.1f*MotorWorks.tune(this).handling);
   if(Math.abs(speed)>.015)setYRot(getYRot()+steer*(bike()?3.4f:2.4f)*(float)Math.signum(speed));
   if(bike()&&tickCount%10==0)sp.sendOverlayMessage(Component.literal(spec().name()+"  "+Math.round(Math.abs(speed)*72)+" km/h  | Space:ブレーキ"));
  }else speed*=.85;
  Vec3 direction=Vec3.directionFromRotation(0,getYRot());
  double dy=onGround()?-.03:Math.max(-1.5,getDeltaMovement().y-.08);
  double beforeX=getX(),beforeZ=getZ();setDeltaMovement(direction.x*speed,dy,direction.z*speed);move(MoverType.SELF,getDeltaMovement());
  if(horizontalCollision)speed=0;
  int actual=(int)Math.round(Math.hypot(getX()-beforeX,getZ()-beforeZ)*72)* (speed<0?-1:1);
  entityData.set(SPEED,actual);
  if(!bike()&&tickCount%5==0)for(var part:getPassengers())if(part instanceof Display.TextDisplay text){
   ((jp.neonward.mixin.MeterTextAccess)text).neonSetText(Component.literal(String.format(java.util.Locale.ROOT,"%03d km/h  %s",Math.abs(actual),actual<0?"R":actual>0?"D":"N")).withStyle(net.minecraft.ChatFormatting.AQUA));
  }
 }
 @Override protected void positionRider(Entity passenger,MoveFunction move){
  // Entity command tags are not synced to clients. Store all model offsets in
  // Display transformations, which are synced, instead of consulting those tags.
  if(passenger instanceof Display){passenger.setYRot(getYRot());move.accept(passenger,getX(),getY(),getZ());return;}
  int idx=-1;for(String tag:passenger.entityTags())if(tag.startsWith("nwpart_"))try{idx=Integer.parseInt(tag.substring(7));}catch(Exception ignored){}
  var specs=model();int seat=getPassengers().stream().filter(e->e instanceof Player).toList().indexOf(passenger);double x=bike()?0:seat%2==0?-.615:.615,y=bike()?(spec().id().equals("gyro")?1.0:.8):.675,z=bike()?(seat<=0?-.1:-1.15):(seat<2?-.90:-2.13);
  if(idx>=0&&idx<specs.size()){var a=specs.get(idx);x=a.x;y=a.y;z=a.z;passenger.setYRot(getYRot());}
  double angle=Math.toRadians(getYRot()),c=Math.cos(angle),s=Math.sin(angle);
  move.accept(passenger,getX()+x*c-z*s,getY()+y,getZ()+x*s+z*c);
 }
 @Override public Vec3 getDismountLocationForPassenger(LivingEntity p){
  for(Vec3 offset:List.of(new Vec3(2.5,.2,0),new Vec3(-2.5,.2,0),new Vec3(0,.2,-3))){Vec3 pos=position().add(offset);if(level().noCollision(p,p.getBoundingBox().move(pos.subtract(p.position()))))return pos;}
  return position().add(0,2,0);
 }
 record Part(double x,double y,double z,double w,double h,double d,String block){}
 List<Part> model(){return MotorWorks.model(this);}
 boolean cargoEmpty(){return cargo.stream().allMatch(net.minecraft.world.item.ItemStack::isEmpty);}
 void openCargo(Player p){
  var storage=new net.minecraft.world.Container(){
   public int getContainerSize(){return MotorWorks.performance(StreetVehicle.this).rows()*9;}
   public boolean isEmpty(){return cargoEmpty();}
   public net.minecraft.world.item.ItemStack getItem(int i){return cargo.get(i);}
   public net.minecraft.world.item.ItemStack removeItem(int i,int n){return net.minecraft.world.ContainerHelper.removeItem(cargo,i,n);}
   public net.minecraft.world.item.ItemStack removeItemNoUpdate(int i){return net.minecraft.world.ContainerHelper.takeItem(cargo,i);}
   public void setItem(int i,net.minecraft.world.item.ItemStack s){cargo.set(i,s);setChanged();}
   public void setChanged(){}
   public boolean stillValid(Player who){return isAlive()&&who.distanceToSqr(StreetVehicle.this)<64&&(owner==null||owner.equals(who.getUUID()));}
   public void clearContent(){cargo.clear();}
  };
  p.openMenu(new net.minecraft.world.SimpleMenuProvider((id,inv,who)->new net.minecraft.world.inventory.ChestMenu(switch(MotorWorks.performance(this).rows()){case 1->net.minecraft.world.inventory.MenuType.GENERIC_9x1;case 2->net.minecraft.world.inventory.MenuType.GENERIC_9x2;case 3->net.minecraft.world.inventory.MenuType.GENERIC_9x3;case 4->net.minecraft.world.inventory.MenuType.GENERIC_9x4;case 5->net.minecraft.world.inventory.MenuType.GENERIC_9x5;default->net.minecraft.world.inventory.MenuType.GENERIC_9x6;},id,inv,storage,MotorWorks.performance(this).rows()),Component.literal(spec().name()+" / 荷室")));
 }
 void buildModel(){
  int i=0;for(Part a:model()){
   Display.BlockDisplay part=new Display.BlockDisplay(EntityTypes.BLOCK_DISPLAY,level());
   String nbt=String.format(Locale.ROOT,"{block_state:{Name:'minecraft:%s'},transformation:{scale:[%sf,%sf,%sf],translation:[%sf,%sf,%sf],left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f]},brightness:{block:15,sky:15},teleport_duration:1,Tags:['nwpart_%d']}",a.block,a.w,a.h,a.d,a.x,a.y,a.z,i++);
   try{part.load(TagValueInput.create(ProblemReporter.DISCARDING,level().registryAccess(),TagParser.parseCompoundFully(nbt)));}catch(Exception e){throw new IllegalStateException("Vehicle model",e);}
   part.setPos(position());((ServerLevel)level()).addFreshEntity(part);part.startRiding(this,true,true);
  }
  if(!bike()){
   Display.TextDisplay meter=new Display.TextDisplay(EntityTypes.TEXT_DISPLAY,level());
   String data="{text:{text:'000 km/h  N',color:'aqua'},line_width:160,background:0,brightness:{block:15,sky:15},transformation:{scale:[0.4f,0.4f,0.4f],translation:[-0.60f,1.90f,0.015f],left_rotation:[0f,1f,0f,0f],right_rotation:[0f,0f,0f,1f]}}";
   try{meter.load(TagValueInput.create(ProblemReporter.DISCARDING,level().registryAccess(),TagParser.parseCompoundFully(data)));}catch(Exception e){throw new IllegalStateException("Dashboard",e);}
   meter.setPos(position());((ServerLevel)level()).addFreshEntity(meter);meter.startRiding(this,true,true);
  }

 }
}
