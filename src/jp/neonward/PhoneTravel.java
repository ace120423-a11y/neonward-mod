package jp.neonward;
import java.util.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.commands.Commands;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.server.level.*;
import net.minecraft.world.level.Level;
import net.minecraft.core.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
public final class PhoneTravel {
 public record Point(String name,int x,int z){}
 public static final List<Point> POINTS=List.of(new Point("中央広場",181,154),new Point("マイホーム前",108,237),new Point("企業ビル・ギルド前",84,119),new Point("武器屋前",79,481),new Point("服屋前",244,494),new Point("マーケット前",246,126),new Point("診療所前",426,484),new Point("北門",160,-12),new Point("東門",552,154),new Point("南門",160,680),new Point("西門",-12,154),new Point("車屋",334,580),new Point("カジノ",510,468),new Point("家具屋",327,600),new Point("釣り堀・土地案内",327,634),new Point("薬局・調合台",347,600),new Point("不動産屋",378,640));
 static final Map<UUID,Integer> NEXT=new HashMap<>();
 static boolean source(ServerPlayer p){
  var l=p.level();double x=p.getX(),y=p.getY(),z=p.getZ();
  if(l.dimension()==Level.OVERWORLD)return city(x,y,z)||CityApartments.area(l,p.blockPosition())||front(x,y,z,SpireSite.OUTER_X+3.5,SpireSite.OUTER_Z-8.5)||front(x,y,z,912,214);
  if(l.dimension()==PrivateHomes.DIMENSION)return PrivateHomes.insidePosition(p);
  if(l.dimension()==VolcanicSpire.DIM)return VolcanicSpire.front(p)||VolcanicSpire.floor(p)>0&&p.getZ()<11;
  if(l.dimension()==NeonZones.TOWER)return foyer(x,y,z,40.5,9.5);
  if(l.dimension()==SkySpire.DIM)return foyer(x,y,z,SkySpire.padX(1,0)+.5,10.5);
  return l.dimension()==CompactShops.DIM&&CompactShops.room(l,p.blockPosition())>=0;
 }
 static boolean city(double x,double y,double z){return x>=-32&&x<=576&&z>=-32&&z<=704&&y>=60&&y<=72;}
 static boolean front(double x,double y,double z,double cx,double cz){return y>=60&&y<=78&&Math.hypot(x-cx,z-cz)<=24;}
 static boolean foyer(double x,double y,double z,double cx,double cz){return y>=65&&y<=75&&Math.hypot(x-cx,z-cz)<=9;}
 static int message(ServerPlayer p,String s){p.sendSystemMessage(Component.literal("NEON TRAVEL / "+s));return 0;}
 static Vec3 landing(ServerLevel l,ServerPlayer p,Point dest){
  // Search only around the street-level arrival point, never on upper floors.
  for(int y:new int[]{64,65,63,66,62,67})for(int r=0;r<=12;r++)for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++){
   if(Math.max(Math.abs(dx),Math.abs(dz))!=r)continue;
   {var support=new BlockPos(dest.x()+dx,y,dest.z()+dz);l.getChunkAt(support);
    var state=l.getBlockState(support);var shape=state.getCollisionShape(l,support);if(shape.isEmpty()||!l.getFluidState(support).isEmpty()||hazard(state.getBlock()))continue;
    var bounds=shape.bounds();if(bounds.minX>.2||bounds.maxX<.8||bounds.minZ>.2||bounds.maxZ<.8)continue;
    var v=new Vec3(support.getX()+.5,y+bounds.maxY,support.getZ()+.5);
    if(safeBody(l,p,v)&&streetConnected(l,p,v)&&!entrance(v))return v;
   }
  }return null;
 }
 static boolean hazard(net.minecraft.world.level.block.Block b){return b==Blocks.MAGMA_BLOCK||b==Blocks.CAMPFIRE||b==Blocks.SOUL_CAMPFIRE||b==Blocks.CACTUS||b==Blocks.FIRE||b==Blocks.SOUL_FIRE||b==Blocks.SWEET_BERRY_BUSH||b==Blocks.WITHER_ROSE||b==Blocks.POWDER_SNOW;}
 static boolean safeBody(ServerLevel l,ServerPlayer p,Vec3 v){
  var box=standingBox(v);
  if(!l.getWorldBorder().isWithinBounds(box)||!l.noCollision(p,box))return false;
  for(var at:BlockPos.betweenClosed(BlockPos.containing(box.minX,box.minY,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ)))if(!l.getFluidState(at).isEmpty()||hazard(l.getBlockState(at).getBlock()))return false;
  return true;
 }
 static net.minecraft.world.phys.AABB standingBox(Vec3 v){return new net.minecraft.world.phys.AABB(v.x-.3,v.y,v.z-.3,v.x+.3,v.y+1.8,v.z+.3);}
 static boolean openSky(ServerLevel l,Vec3 v){return l.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,(int)Math.floor(v.x),(int)Math.floor(v.z))<=v.y;}
 static boolean streetConnected(ServerLevel l,ServerPlayer p,Vec3 v){
  // An awning is fine if the player can walk straight out to open sky.
  // Solid walls/closed rooms still cannot be selected as a street destination.
  if(openSky(l,v))return true;
  for(var d:Direction.Plane.HORIZONTAL)for(int n=1;n<=4;n++){
   var q=v.add(d.getStepX()*n,0,d.getStepZ()*n);l.getChunkAt(BlockPos.containing(q));
   if(!safeBody(l,p,q)||!l.noCollision(p,standingBox(v).expandTowards(d.getStepX()*n,0,d.getStepZ()*n)))break;if(openSky(l,q))return true;
  }return false;
 }
 static boolean entrance(Vec3 v){for(var s:CompactShops.ALL)if(v.x>s.x()-.75&&v.x<s.x()+2.75&&v.z>s.z()-1.6&&v.z<s.z()+1.25)return true;return false;}
 static int travel(ServerPlayer p,int index){
  if(index<0||index>=POINTS.size())return 0;
  if(!p.isAlive()||p.isSpectator()||PhoneEquipment.get(p).isEmpty())return message(p,"スマホを装備してください");
  int now=p.level().getServer().getTickCount();if(now<NEXT.getOrDefault(p.getUUID(),0))return 0;NEXT.put(p.getUUID(),now+60);
  if(p.isPassenger())return message(p,"車両から降りてから利用してください");
  if(!source(p))return message(p,"街中・マイホーム内・塔の入口付近で利用できます");
  var l=p.level().getServer().overworld();var dest=POINTS.get(index);var at=landing(l,p,dest);if(at==null)return message(p,"到着地点が塞がっているため移動を中止しました");
  if(!p.teleportTo(l,at.x,at.y,at.z,Set.of(),180,0,true))return message(p,"移動できませんでした");
  p.fallDistance=0;p.setDeltaMovement(Vec3.ZERO);
  // Arrival is outside door triggers. A travel throttle must not lock building entrances.
  NeonZones.cooldown.remove(p.getUUID());CompactShops.cooldown.remove(p.getUUID());PrivateHomes.cooldown.remove(p.getUUID());
  NightSpire.waiting.remove(p.getUUID());SkySpire.waiting.remove(p.getUUID());SkySpire.steps.remove(p.getUUID());
  VolcanicSpire.WAITING.remove(p.getUUID());
  message(p,dest.name()+"へ移動しました");return 1;
 }
 public static void init(){CommandRegistrationCallback.EVENT.register((d,c,e)->d.register(Commands.literal("neontravel").then(Commands.argument("point",IntegerArgumentType.integer(0,POINTS.size()-1)).executes(ctx->travel(ctx.getSource().getPlayerOrException(),IntegerArgumentType.getInteger(ctx,"point"))))));ServerPlayConnectionEvents.DISCONNECT.register((h,s)->NEXT.remove(h.player.getUUID()));ServerLifecycleEvents.SERVER_STOPPED.register(s->NEXT.clear());}
}
