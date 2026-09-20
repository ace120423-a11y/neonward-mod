package jp.neonward;

import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.*;
import net.minecraft.nbt.TagParser;
import net.minecraft.util.ProblemReporter;
import com.mojang.math.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import jp.neonward.mixin.LiftDisplayAccess;

/** One fixed anchor, four interpolated projection meshes: no AI or collision. */
public final class HologramFish extends Entity {
 public static final BlockPos PLAZA=new BlockPos(160,112,154);
 public static final EntityType<HologramFish> TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,NeonWard.id("hologram_fish"),EntityType.Builder.<HologramFish>of(HologramFish::new,MobCategory.MISC).sized(.1f,.1f).clientTrackingRange(24).updateInterval(20).build(ResourceKey.create(Registries.ENTITY_TYPE,NeonWard.id("hologram_fish"))));
 static final String[] MESH={"holo_fish_body","holo_fish_tail","holo_fish_fin","holo_fish_fin"};
 public HologramFish(EntityType<? extends HologramFish> type,Level level){super(type,level);setNoGravity(true);setInvulnerable(true);}
 public static void init(){
  for(String name:new LinkedHashSet<>(List.of(MESH))){var id=NeonWard.id(name);Registry.register(BuiltInRegistries.BLOCK,id,new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK,id)).noCollision().noOcclusion().strength(-1,3600000)));}
  ServerTickEvents.END_SERVER_TICK.register(server->{
   if(server.getTickCount()%100!=0)return;var l=server.overworld();if(!l.isPositionEntityTicking(PLAZA))return;
   if(l.getEntitiesOfClass(HologramFish.class,new AABB(PLAZA).inflate(2)).isEmpty()){
    HologramFish fish=new HologramFish(TYPE,l);fish.setPos(PLAZA.getX(),PLAZA.getY(),PLAZA.getZ());l.addFreshEntity(fish);
   }
  });
 }
 @Override protected void defineSynchedData(SynchedEntityData.Builder b){}
 @Override protected void readAdditionalSaveData(ValueInput i){}
 @Override protected void addAdditionalSaveData(ValueOutput o){}
 @Override public boolean hurtServer(ServerLevel l,DamageSource s,float amount){return false;}
 @Override public boolean isPickable(){return false;}
 @Override public boolean canBeCollidedWith(Entity e){return false;}
 @Override protected boolean canAddPassenger(Entity e){return e instanceof Display;}
 @Override protected void positionRider(Entity e,MoveFunction move){move.accept(e,getX(),getY(),getZ());}
 @Override public void tick(){
  super.tick();if(level().isClientSide())return;
  if(getPassengers().size()!=4){for(var e:List.copyOf(getPassengers()))e.discard();build();}
  if(tickCount%4==0)animate();
 }
 void build(){
  for(int i=0;i<4;i++){
   var d=new Display.BlockDisplay(EntityTypes.BLOCK_DISPLAY,level());
   String nbt="{block_state:{Name:'neonward:"+MESH[i]+"'},brightness:{block:15,sky:15},view_range:8f,width:100f,height:40f,shadow_radius:0f,interpolation_duration:4,start_interpolation:0,Invulnerable:1b,Tags:['nw_holo_part_"+i+"']}";
   try{d.load(TagValueInput.create(ProblemReporter.DISCARDING,level().registryAccess(),TagParser.parseCompoundFully(nbt)));}catch(Exception ex){throw new IllegalStateException("Goldfish hologram",ex);}
   d.setPos(position());((ServerLevel)level()).addFreshEntity(d);d.startRiding(this,true,true);
  }
  animate();
 }
 void animate(){
  double t=level().getGameTime()/20.0,a=t*2*Math.PI/105;
  Vector3f swim=new Vector3f((float)(22*Math.cos(a)),(float)(3*Math.sin(a*2)),(float)(18*Math.sin(a)));
  Quaternionf heading=new Quaternionf().rotationY((float)Math.atan2(-22*Math.sin(a),18*Math.cos(a)));
  for(var e:getPassengers())if(e instanceof Display d){
   int part=-1;for(int i=0;i<4;i++)if(e.entityTags().contains("nw_holo_part_"+i))part=i;
   if(part<0)continue;
   Vector3f offset=new Vector3f();Quaternionf local=new Quaternionf();
   if(part==1){offset.set(0,0,-5);local.rotationY((float)(.30*Math.sin(t*2.8)));}
   if(part==2||part==3){float side=part==2?1:-1;offset.set(side*2.4f,-.8f,1.3f);local.rotationY(side*(float)(.60+.22*Math.sin(t*2.8+.8))).rotateZ(side*.22f);}
   heading.transform(offset);offset.add(swim);
   Quaternionf rotation=new Quaternionf(heading).mul(local);
   Vector3f scale=new Vector3f(8,8,8);if(part==3)scale.x=-8;
   var access=(LiftDisplayAccess)d;access.neonLiftDelay(0);access.neonLiftTransform(new Transformation(offset,rotation,scale,new Quaternionf()));
  }
 }
}
