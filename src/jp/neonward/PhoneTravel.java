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
  if(l.dimension()==Level.OVERWORLD)return city(x,y,z)||front(x,y,z,SpireSite.OUTER_X+3.5,SpireSite.OUTER_Z-8.5)||front(x,y,z,912,214);
  if(l.dimension()==NeonZones.TOWER)return foyer(x,y,z,40.5,9.5);
  if(l.dimension()==SkySpire.DIM)return foyer(x,y,z,SkySpire.padX(1,0)+.5,10.5);
  return l.dimension()==CompactShops.DIM&&CompactShops.room(l,p.blockPosition())>=0;
 }
 static boolean city(double x,double y,double z){return x>=-32&&x<=576&&z>=-32&&z<=704&&y>=60&&y<=72;}
 static boolean front(double x,double y,double z,double cx,double cz){return y>=60&&y<=78&&Math.hypot(x-cx,z-cz)<=24;}
 static boolean foyer(double x,double y,double z,double cx,double cz){return y>=65&&y<=75&&Math.hypot(x-cx,z-cz)<=9;}
 static int message(ServerPlayer p,String s){p.sendSystemMessage(Component.literal("NEON TRAVEL / "+s));return 0;}
 static Vec3 landing(ServerLevel l,ServerPlayer p,Point dest){
  for(int r=0;r<=6;r++)for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++){
   if(Math.max(Math.abs(dx),Math.abs(dz))!=r)continue;
   for(int y:new int[]{65,66,64,67,63,68}){var at=new BlockPos(dest.x()+dx,y,dest.z()+dz);l.getChunk(at.getX()>>4,at.getZ()>>4);
    var below=l.getBlockState(at.below());if(!below.isFaceSturdy(l,at.below(),Direction.UP)||below.is(Blocks.MAGMA_BLOCK)||below.is(Blocks.CAMPFIRE)||below.is(Blocks.SOUL_CAMPFIRE)||below.is(Blocks.CACTUS))continue;
    // Stay outdoors rather than finding an empty room behind the entrance.
    if(!l.getWorldBorder().isWithinBounds(at)||!l.canSeeSky(at)||!l.getBlockState(at).isAir()||!l.getBlockState(at.above()).isAir())continue;
    var v=new Vec3(at.getX()+.5,y,at.getZ()+.5);var box=p.getBoundingBox().move(v.subtract(p.position()));if(l.noCollision(p,box))return v;
   }
  }return null;
 }
 static int travel(ServerPlayer p,int index){
  if(index<0||index>=POINTS.size())return 0;
  if(!p.isAlive()||p.isSpectator()||PhoneEquipment.get(p).isEmpty())return message(p,"スマホを装備してください");
  int now=p.level().getServer().getTickCount();if(now<NEXT.getOrDefault(p.getUUID(),0))return 0;NEXT.put(p.getUUID(),now+60);
  if(p.isPassenger())return message(p,"車両から降りてから利用してください");
  if(!source(p))return message(p,"街中か、塔の入口付近で利用できます");
  var l=p.level().getServer().overworld();var dest=POINTS.get(index);var at=landing(l,p,dest);if(at==null)return message(p,"到着地点が塞がっているため移動を中止しました");
  if(!p.teleportTo(l,at.x,at.y,at.z,Set.of(),180,0,true))return message(p,"移動できませんでした");
  p.fallDistance=0;p.setDeltaMovement(Vec3.ZERO);NeonZones.cooldown.put(p.getUUID(),(long)now+100);CompactShops.cooldown.put(p.getUUID(),now+100);
  NightSpire.waiting.remove(p.getUUID());SkySpire.waiting.remove(p.getUUID());SkySpire.steps.remove(p.getUUID());
  message(p,dest.name()+"へ移動しました");return 1;
 }
 public static void init(){CommandRegistrationCallback.EVENT.register((d,c,e)->d.register(Commands.literal("neontravel").then(Commands.argument("point",IntegerArgumentType.integer(0,POINTS.size()-1)).executes(ctx->travel(ctx.getSource().getPlayerOrException(),IntegerArgumentType.getInteger(ctx,"point"))))));ServerPlayConnectionEvents.DISCONNECT.register((h,s)->NEXT.remove(h.player.getUUID()));ServerLifecycleEvents.SERVER_STOPPED.register(s->NEXT.clear());}
}
