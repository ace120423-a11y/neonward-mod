package jp.neonward;

import java.util.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.*;
import net.minecraft.network.syncher.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.nbt.TagParser;
import net.minecraft.util.ProblemReporter;
import net.minecraft.network.chat.Component;
import jp.neonward.mixin.MeterTextAccess;
import jp.neonward.mixin.LiftDisplayAccess;
import com.mojang.math.Transformation;
import org.joml.Vector3f;
import org.joml.Quaternionf;

public class VerticalLift extends Entity {
 static final EntityDataAccessor<Integer> LIFT=SynchedEntityData.defineId(VerticalLift.class,EntityDataSerializers.INT),PHASE=SynchedEntityData.defineId(VerticalLift.class,EntityDataSerializers.INT),TARGET=SynchedEntityData.defineId(VerticalLift.class,EntityDataSerializers.INT);
 final ArrayDeque<Integer> queue=new ArrayDeque<>();int timer=0,modelVersion=0;boolean restored=false,arriving=false;int lastDoorPhase=-1;
 public VerticalLift(EntityType<? extends VerticalLift> t,Level l){super(t,l);setNoGravity(true);}
 public int liftId(){return entityData.get(LIFT);} public int phase(){return entityData.get(PHASE);} public int target(){return entityData.get(TARGET);}
 public LiftSystem.Plan plan(){return LiftSystem.PLANS.get(Math.clamp(liftId(),0,LiftSystem.PLANS.size()-1));}
 public boolean doorsOpen(){return phase()==0;}
 public boolean inside(Player p){return p.getVehicle()==this||(Math.abs(p.getX()-getX())<1.42&&Math.abs(p.getZ()-getZ())<1.42&&p.getY()>=getY()-.1&&p.getY()<getY()+2.6);}
 public void configure(int id){entityData.set(LIFT,id);var p=plan();setPos(p.x()+1.5,p.floors()[0],p.z()+1.5);}
 @Override protected void defineSynchedData(SynchedEntityData.Builder b){b.define(LIFT,0);b.define(PHASE,0);b.define(TARGET,0);}
 @Override protected void readAdditionalSaveData(ValueInput i){modelVersion=i.getIntOr("ModelVersion",0);entityData.set(LIFT,Math.clamp(i.getIntOr("Lift",0),0,LiftSystem.PLANS.size()-1));entityData.set(PHASE,Math.clamp(i.getIntOr("Phase",0),0,3));entityData.set(TARGET,Math.clamp(i.getIntOr("Target",0),0,plan().floors().length-1));timer=i.getIntOr("Timer",0);for(String s:i.getStringOr("Queue","").split(","))try{int f=Integer.parseInt(s);if(f>=0&&f<plan().floors().length&&!queue.contains(f))queue.add(f);}catch(NumberFormatException ignored){}}
 @Override protected void addAdditionalSaveData(ValueOutput o){o.putInt("ModelVersion",modelVersion);o.putInt("Lift",liftId());o.putInt("Phase",phase());o.putInt("Target",target());o.putInt("Timer",timer);o.putString("Queue",String.join(",",queue.stream().map(String::valueOf).toList()));}
 @Override public boolean hurtServer(ServerLevel l,DamageSource s,float n){return false;}
 @Override public boolean isPickable(){return true;}
 @Override public boolean canBeCollidedWith(Entity e){return false;}
 @Override protected boolean canAddPassenger(Entity e){return e instanceof Display||(e instanceof Player&&getPassengers().stream().filter(p->p instanceof Player).count()<9);}
 @Override public InteractionResult interact(Player p,InteractionHand hand,Vec3 at){return InteractionResult.SUCCESS;}
 public boolean request(int floor){if(floor<0||floor>=plan().floors().length)return false;if(doorsOpen()&&Math.abs(getY()-plan().floors()[floor])<.05)return true;if(!queue.contains(floor)&&(phase()==0||floor!=target()))queue.add(floor);return true;}
 void platform(int floor,boolean present){var p=plan();for(int x=p.x();x<p.x()+3;x++)for(int z=p.z();z<p.z()+3;z++){var pos=new BlockPos(x,p.floors()[floor]-1,z);var s=level().getBlockState(pos);if(present&&(s.isAir()||s.is(LiftSystem.FLOOR)))level().setBlock(pos,LiftSystem.FLOOR.defaultBlockState(),3);else if(!present&&s.is(LiftSystem.FLOOR))level().setBlock(pos,Blocks.AIR.defaultBlockState(),3);}}
 boolean thresholdOccupied(){var p=plan();return !level().getEntitiesOfClass(Player.class,new AABB(p.x()-.15,getY(),p.z()+2.85,p.x()+2.15,getY()+2.5,p.z()+4),a->!a.isSpectator()).isEmpty();}
 boolean clearPath(int destination){var p=plan();int lo=(int)Math.floor(Math.min(getY(),p.floors()[destination])),hi=(int)Math.ceil(Math.max(getY(),p.floors()[destination]))+2;for(int y=lo;y<=hi;y++)for(int x=p.x();x<p.x()+3;x++)for(int z=p.z();z<p.z()+3;z++){var s=level().getBlockState(new BlockPos(x,y,z));if(!s.isAir()&&!s.is(LiftSystem.FLOOR))return false;}return true;}
 @Override public void tick(){
  super.tick();if(level().isClientSide())return;var p=plan();
  if(!level().getBlockState(p.anchor()).is(LiftSystem.ANCHOR)){for(var part:List.copyOf(getPassengers()))if(part instanceof Display)part.discard();ejectPassengers();discard();return;}
  if(!restored){restored=true;if(phase()==0)platform(p.nearest(getY()),true);}
  if(modelVersion!=2){for(var e:List.copyOf(getPassengers()))if(e instanceof Display)e.discard();modelVersion=2;}
  if(getPassengers().stream().noneMatch(e->e instanceof Display.BlockDisplay))buildModel();
  if(phase()==0&&timer>0)timer--;
  if(phase()==0&&timer<=0&&!queue.isEmpty()&&!thresholdOccupied()){
   int dest=queue.peek();if(Math.abs(getY()-p.floors()[dest])<.05)queue.remove();
   else if(clearPath(dest)){entityData.set(TARGET,queue.remove());entityData.set(PHASE,1);timer=25;}
   else if(tickCount%40==0)for(var player:level().getEntitiesOfClass(Player.class,getBoundingBox().inflate(4)))player.sendOverlayMessage(Component.literal("昇降路に障害物があります。運転停止中。"));
  }
  else if(phase()==1){
   if(thresholdOccupied()){entityData.set(PHASE,0);queue.addFirst(target());}
   else if(--timer<=0){
    var riders=level().getEntitiesOfClass(Player.class,getBoundingBox(),a->inside(a)&&!a.isSpectator());
    if(riders.size()>9||riders.stream().anyMatch(a->a.isPassenger()&&a.getVehicle()!=this)){entityData.set(PHASE,0);queue.addFirst(target());}
    else {boolean okay=true;for(var rider:riders)if(rider.getVehicle()!=this&&!rider.startRiding(this,true,true))okay=false;
     if(okay){platform(p.nearest(getY()),false);entityData.set(PHASE,2);}else {entityData.set(PHASE,0);queue.addFirst(target());ejectPlayers();}
    }
   }
  }else if(phase()==2){
   double d=p.floors()[target()]-getY();double step=Math.copySign(Math.min(Math.abs(d),Math.min(.22,.045+Math.abs(d)*.12)),d);
   setPos(getX(),getY()+step,getZ());
   if(Math.abs(d)<.005){setPos(getX(),p.floors()[target()],getZ());platform(target(),true);entityData.set(PHASE,3);timer=18;((ServerLevel)level()).playSound(null,blockPosition(),net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BELL.value(),net.minecraft.sounds.SoundSource.BLOCKS,.8f,1.2f);}
  }else if(phase()==3&&--timer<=0){entityData.set(PHASE,0);ejectPlayers();timer=60;}
  if(phase()!=lastDoorPhase){animateDoors();lastDoorPhase=phase();}
  if(tickCount%8==0)updateMonitors();
  for(var passenger:getPassengers())if(passenger instanceof Player player)player.fallDistance=0;
 }
 void ejectPlayers(){arriving=true;for(var e:List.copyOf(getPassengers()))if(e instanceof Player)e.stopRiding();arriving=false;}
 @Override protected void positionRider(Entity e,MoveFunction move){if(e instanceof Display){move.accept(e,getX(),getY(),getZ());return;}int i=0;for(var a:getPassengers()){if(a==e)break;if(a instanceof Player)i++;}double[][] places={{0,0},{-.8,0},{.8,0},{0,-.8},{-.8,-.8},{.8,-.8},{0,.8},{-.8,.8},{.8,.8}};move.accept(e,getX()+places[i%9][0],getY()+.02,getZ()+places[i%9][1]);}
 @Override public Vec3 getDismountLocationForPassenger(LivingEntity e){if(arriving||doorsOpen())return new Vec3(e.getX(),getY()+.02,e.getZ());var p=plan();int f=p.nearest(getY());return new Vec3(p.x()+1,p.floors()[f]+.02,p.z()+4.65);}
 void loadDisplay(Display d,String snbt){try{d.load(TagValueInput.create(ProblemReporter.DISCARDING,level().registryAccess(),TagParser.parseCompoundFully(snbt)));}catch(Exception e){throw new IllegalStateException("Lift display",e);}}
 void part(String block,double x,double y,double z,double w,double h,double d,String tag){var part=new Display.BlockDisplay(EntityTypes.BLOCK_DISPLAY,level());loadDisplay(part,String.format(Locale.ROOT,"{block_state:{Name:'%s'},transformation:{translation:[%sf,%sf,%sf],scale:[%sf,%sf,%sf],left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f]},brightness:{block:15,sky:15},teleport_duration:1,interpolation_duration:15,Tags:['nw_lift_body','%s']}",block,x,y,z,w,h,d,tag));part.setPos(position());((ServerLevel)level()).addFreshEntity(part);part.startRiding(this,true,true);}
 void buildModel(){
  part("neonward:lift_floor",-1.48,-.18,-1.48,2.96,.18,2.96,"deck");
  part("minecraft:sea_lantern",-1.48,2.8,-1.48,2.96,.15,2.96,"roof");
  part("minecraft:tinted_glass",-1.48,0,-1.48,.12,2.8,2.96,"left");part("minecraft:tinted_glass",1.36,0,-1.48,.12,2.8,2.96,"right");
  part("minecraft:black_concrete",-1.48,0,-1.48,2.96,2.8,.12,"back");
  part("minecraft:sea_lantern",-1.3,.15,-1.34,.045,2.55,.05,"strip");part("minecraft:sea_lantern",1.25,.15,-1.34,.045,2.55,.05,"strip");
  part("neonward:lift_console",.35,.85,-1.34,.55,.85,.055,"buttons");
  part("minecraft:iron_block",-1.46,0,1.38,1.46,2.75,.10,"nw_lift_leaf_l");part("minecraft:iron_block",0,0,1.38,1.46,2.75,.10,"nw_lift_leaf_r");
  var text=new Display.TextDisplay(EntityTypes.TEXT_DISPLAY,level());loadDisplay(text,"{text:{text:'階数ボタン / 右クリック',color:'aqua'},background:0,line_width:220,brightness:{block:15,sky:15},transformation:{translation:[0f,1.95f,-1.30f],scale:[0.6f,0.6f,0.6f],left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f]},Tags:['nw_lift_body','nw_lift_inside_monitor']}");text.setPos(position());((ServerLevel)level()).addFreshEntity(text);text.startRiding(this,true,true);lastDoorPhase=-1;
 }
 void animateDoors(){boolean open=doorsOpen();for(var e:getPassengers())if(e instanceof Display d){boolean left=e.entityTags().contains("nw_lift_leaf_l"),right=e.entityTags().contains("nw_lift_leaf_r");if(!left&&!right)continue;float x=left?(open?-2.90f:-1.46f):(open?1.44f:0);var a=(LiftDisplayAccess)d;a.neonLiftDelay(0);a.neonLiftTransform(new Transformation(new Vector3f(x,0,1.38f),new Quaternionf(),new Vector3f(1.46f,2.75f,.10f),new Quaternionf()));}}
 public String status(){var p=plan();int f=p.nearest(getY())+1;String dir=phase()==2?(p.floors()[target()]>getY()?"▲ 上昇":"▼ 下降"):phase()==1?"扉が閉まります":phase()==3?"到着":"待機";return f+"階  "+dir+"  |  "+(phase()==0?"呼び出し受付":(target()+1)+"階へ")+"  |  NEON LIFT   ";}
 void updateMonitors(){
  var p=plan();String full=status();int offset=(tickCount/8)%full.length();String scroll=(full+full).substring(offset,offset+Math.min(8,full.length()));
  String heading=String.format(Locale.ROOT,"%02dF  %s",p.nearest(getY())+1,phase()==2?(p.floors()[target()]>getY()?"▲":"▼")+(target()+1)+"F":phase()==0?"待機":phase()==3?"到着":"出発");
  var inside=Component.literal(heading+"\n"+scroll+"\n右クリック：階数").withStyle(net.minecraft.ChatFormatting.AQUA);
  for(var e:getPassengers())if(e instanceof Display.TextDisplay d)((MeterTextAccess)d).neonSetText(inside);
  var server=(ServerLevel)level();for(int i=0;i<p.floors().length;i++){
   int y=p.floors()[i];if(server.players().stream().noneMatch(a->a.distanceToSqr(p.x()+1,y,p.z()+4)<6400))continue;
   String tag="nw_lift_monitor_"+p.id()+"_"+i;
   var found=server.getEntitiesOfClass(Display.TextDisplay.class,new AABB(p.x()-1,y+2,p.z()+3,p.x()+3,y+4,p.z()+5),a->a.entityTags().contains(tag));
   Display.TextDisplay text;if(found.isEmpty()){
    text=new Display.TextDisplay(EntityTypes.TEXT_DISPLAY,level());loadDisplay(text,"{text:{text:'NEON LIFT',color:'aqua'},background:0,line_width:300,brightness:{block:15,sky:15},transformation:{translation:[0f,0f,0f],scale:[0.30f,0.30f,0.30f],left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f]},Tags:['nw_lift_monitor','"+tag+"']}");text.setPos(p.x()+1,y+2.34,p.z()+4.015);server.addFreshEntity(text);
   }else text=found.getFirst();
   if(!text.entityTags().contains("nw_lift_monitor_v2")){text.setPos(p.x()+1,y+2.12,p.z()+4.015);((LiftDisplayAccess)text).neonLiftTransform(new Transformation(new Vector3f(),new Quaternionf(),new Vector3f(1,1,1),new Quaternionf()));text.addTag("nw_lift_monitor_v2");}
   ((MeterTextAccess)text).neonSetText(Component.literal(heading+"\n"+scroll).withStyle(phase()==2?net.minecraft.ChatFormatting.LIGHT_PURPLE:net.minecraft.ChatFormatting.AQUA));
  }
 }
}
