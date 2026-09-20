package jp.neonward;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;

public final class NeonZones {
 public static final ResourceKey<Level> WILDS=ResourceKey.create(Registries.DIMENSION,NeonWard.id("wilds")),TOWER=ResourceKey.create(Registries.DIMENSION,NeonWard.id("clockwork_interior"));
 public static Block ENTRY,EXIT,NEXT;public static final BlockPos ENTRY_POS=new BlockPos(184,65,154);
 static final int[][] DEST={{160,-46},{590,154},{160,718},{-46,154}};
 static final int[][] INSIDE={{160,-8},{552,154},{160,680},{-8,154}};
 static final Map<UUID,Long> cooldown=new HashMap<>();
 static Block terminal(String id){var key=NeonWard.id(id);var b=new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK,key)).strength(-1,3600000).lightLevel(s->15).noOcclusion()){
  @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(net.minecraft.world.level.block.state.BlockState s,net.minecraft.world.level.BlockGetter l,BlockPos p,net.minecraft.world.phys.shapes.CollisionContext c){return id.equals("clockwork_puzzle_panel")?Block.box(1,0,0,15,29,15):super.getShape(s,l,p,c);}
 };Registry.register(BuiltInRegistries.BLOCK,key,b);Registry.register(BuiltInRegistries.ITEM,key,new BlockItem(b,new Item.Properties().setId(ResourceKey.create(Registries.ITEM,key)).useBlockDescriptionPrefix()));return b;}
 public static void init(){
  ENTRY=terminal("spire_access_terminal");EXIT=terminal("city_return_terminal");NEXT=terminal("spire_ascent_terminal");
  NightSpire.init();ClockworkPuzzles.init();
  UseBlockCallback.EVENT.register((p,l,h,hit)->{var b=l.getBlockState(hit.getBlockPos()).getBlock();if(b!=ENTRY&&b!=EXIT&&b!=NEXT)return InteractionResult.PASS;if(p instanceof ServerPlayer sp&&!p.isSpectator()){
   if(b==ENTRY){if(hit.getBlockPos().equals(ENTRY_POS)){move(sp,sp.level().getServer().overworld(),new Vec3(SpireSite.OUTER_X+3.5,65,SpireSite.OUTER_Z-8.5),0);sp.sendSystemMessage(Component.literal("NIGHT SPIRE / 東門の外に建つ30階の塔です。正面入口から入れます。"));}else NightSpire.enter(sp,p.isShiftKeyDown());}else if(b==EXIT)NightSpire.leave(sp);else if(SpireSite.contains(l,sp.blockPosition()))NightSpire.ascend(sp);
  }return InteractionResult.SUCCESS;});
  ServerLifecycleEvents.SERVER_STOPPED.register(s->cooldown.clear());
  ServerTickEvents.END_SERVER_TICK.register(server->{
   if(server.getTickCount()%20!=0)return;
   var city=server.overworld();
   if(city.hasChunkAt(ENTRY_POS)&&city.getBlockState(ENTRY_POS).isAir()){city.setBlock(ENTRY_POS,ENTRY.defaultBlockState(),3);sign(city,ENTRY_POS.above(),"NIGHT SPIRE","東門の外・30階の塔","右クリック：塔の正面","入口から攻略開始");}
   // Vanilla monsters are also removed inside the safe city.
   for(var e:city.getAllEntities())if(e instanceof Enemy&&CityProtection.contains(city,e.blockPosition()))e.discard();
   for(var p:server.getPlayerList().getPlayers()){
    long now=server.getTickCount();if(now<cooldown.getOrDefault(p.getUUID(),0L))continue;
    // Recover legacy exploration saves; city gates now lead straight into the Overworld.
    if(p.level().dimension()==WILDS)city(p);
    if(p.level().dimension().identifier().equals(NeonWard.id("night_spire")))NightSpire.enter(p,false);
    if(p.level().dimension()==Level.OVERWORLD&&p.getX()>=738&&p.getX()<=741&&p.getZ()>=224&&p.getZ()<=228&&p.getY()>=64&&p.getY()<71)NightSpire.enter(p,true);
    if(SpireSite.exterior(p.level(),p.blockPosition())&&p.getX()>=736&&p.getX()<775&&p.getZ()>=224&&p.getZ()<263&&p.getY()>75)NightSpire.enter(p,false);
   }
  });
 }
 static int gate(double x,double z){if(Math.abs(x-160)<=16&&z<=-18&&z>=-36)return 0;if(Math.abs(z-154)<=16&&x>=562&&x<=580)return 1;if(Math.abs(x-160)<=16&&z>=690&&z<=708)return 2;if(Math.abs(z-154)<=16&&x<=-18&&x>=-36)return 3;return -1;}
 public static boolean isField(Level l,BlockPos p){return l.dimension()==Level.OVERWORLD&&!CityProtection.contains(l,p)&&!SpireSite.structure(l,p);}
 public static boolean safeOutpost(Level l,BlockPos p){if(l.dimension()!=WILDS)return false;if(HostileRoster.insideCity(p.getX(),p.getZ()))return true;for(var c:DEST)if(Math.abs(p.getX()-c[0])<=16&&Math.abs(p.getZ()-c[1])<=16)return true;return false;}
 static void wilds(ServerPlayer p,int gate){
  var l=p.level().getServer().overworld();int x=DEST[gate][0],z=DEST[gate][1];
  move(p,l,new Vec3(x+.5,65,z+.5),new float[]{180,-90,0,90}[gate]);

 }
 static int returnGate(double x,double z){if(Math.abs(x-160)<=16&&z>=-34&&z<=-16)return 0;if(Math.abs(z-154)<=16&&x<=578&&x>=560)return 1;if(Math.abs(x-160)<=16&&z<=706&&z>=688)return 2;if(Math.abs(z-154)<=16&&x>=-34&&x<=-16)return 3;return -1;}
 static void cityGate(ServerPlayer p,int gate){move(p,p.level().getServer().overworld(),new Vec3(INSIDE[gate][0]+.5,65,INSIDE[gate][1]+.5),new float[]{0,90,180,-90}[gate]);p.sendSystemMessage(Component.literal("NEON WARD / 安全エリア"));}
 public static void city(ServerPlayer p){move(p,p.level().getServer().overworld(),new Vec3(181.5,65,154.5));p.sendSystemMessage(Component.literal("NEON WARD / 安全エリアに帰還"));}
 static void move(ServerPlayer p,ServerLevel l,Vec3 pos){move(p,l,pos,180);}
 static void move(ServerPlayer p,ServerLevel l,Vec3 pos,float yaw){if(p.isPassenger())p.stopRiding();p.teleportTo(l,pos.x,pos.y,pos.z,Set.of(),yaw,0,true);cooldown.put(p.getUUID(),(long)l.getServer().getTickCount()+100);}
 public static void sign(ServerLevel l,BlockPos pos,String... lines){
  if(!l.getBlockState(pos).isAir())return;l.setBlock(pos,Blocks.OAK_SIGN.defaultBlockState(),3);
  if(l.getBlockEntity(pos) instanceof SignBlockEntity sign){var text=new SignText().setColor(DyeColor.CYAN).setHasGlowingText(true);for(int i=0;i<Math.min(4,lines.length);i++)text=text.setMessage(i,Component.literal(lines[i]));sign.setText(text,true);sign.setText(text,false);sign.setChanged();l.sendBlockUpdated(pos,l.getBlockState(pos),l.getBlockState(pos),3);}
 }
}
