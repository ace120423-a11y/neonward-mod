package jp.neonward;

import java.util.*;
import java.io.*;
import com.google.gson.*;
import com.mojang.serialization.MapCodec;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.Commands;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.item.*;
import net.minecraft.server.level.*;
import net.minecraft.world.phys.*;
import net.minecraft.network.chat.Component;

public final class LiftSystem {
 public record Plan(int id,String name,int x,int z,int[] floors){
  public int nearest(double y){int f=0;for(int i=1;i<floors.length;i++)if(Math.abs(floors[i]-y)<Math.abs(floors[f]-y))f=i;return f;}
  public BlockPos anchor(){return new BlockPos(x-1,floors[0]-2,z-1);}
  public AABB shaft(){return new AABB(x,floors[0]-1,z,x+3,floors[floors.length-1]+4,z+3);}
 }
 public static final List<Plan> PLANS=load();
 public static final EntityType<VerticalLift> CAB=Registry.register(BuiltInRegistries.ENTITY_TYPE,NeonWard.id("lift_cabin"),EntityType.Builder.<VerticalLift>of(VerticalLift::new,MobCategory.MISC).sized(2.98f,2.95f).clientTrackingRange(12).updateInterval(1).build(ResourceKey.create(Registries.ENTITY_TYPE,NeonWard.id("lift_cabin"))));
 public static Block ANCHOR,FLOOR,PANEL,MONITOR,CONSOLE;
 static final Map<ServerLevel,Map<Integer,Long>> missing=new WeakHashMap<>();
 static boolean ready(ServerLevel l,Plan p){for(int x=(p.x-1)>>4;x<=(p.x+3)>>4;x++)for(int z=(p.z-1)>>4;z<=(p.z+3)>>4;z++)if(!l.areEntitiesActuallyLoadedAndTicking(new ChunkPos(x,z)))return false;return true;}
 static List<Plan> load(){
  try(var r=new InputStreamReader(Objects.requireNonNull(LiftSystem.class.getResourceAsStream("/lifts.json")),java.nio.charset.StandardCharsets.UTF_8)){
   var result=new ArrayList<Plan>();for(var e:JsonParser.parseReader(r).getAsJsonArray()){var o=e.getAsJsonObject();var fs=o.getAsJsonArray("floors");int[] ys=new int[fs.size()];for(int i=0;i<ys.length;i++)ys[i]=fs.get(i).getAsJsonObject().get("y").getAsInt();result.add(new Plan(o.get("id").getAsInt(),o.get("name").getAsString(),o.get("x").getAsInt(),o.get("z").getAsInt(),ys));}return List.copyOf(result);
  }catch(Exception e){throw new IllegalStateException("Lift plan",e);}
 }
 static Block block(String n,boolean panel){var id=NeonWard.id(n);var p=BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK,id)).strength(-1,3600000).sound(SoundType.METAL).lightLevel(s->n.equals("lift_monitor")?15:7);Block b=panel?new Panel(p):new Block(p);Registry.register(BuiltInRegistries.BLOCK,id,b);Registry.register(BuiltInRegistries.ITEM,id,new BlockItem(b,new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id)).useBlockDescriptionPrefix()));return b;}
 public static void init(){
  ANCHOR=block("lift_anchor",false);FLOOR=block("lift_floor",false);PANEL=block("lift_panel",true);MONITOR=block("lift_monitor",false);CONSOLE=block("lift_console",false);
  ServerTickEvents.END_SERVER_TICK.register(server->{if(server.getTickCount()%20!=0)return;var l=server.overworld();var absent=missing.computeIfAbsent(l,k->new HashMap<>());for(var p:PLANS){if(!ready(l,p)||!l.getBlockState(p.anchor()).is(ANCHOR)){absent.remove(p.id);continue;}var all=l.getEntitiesOfClass(VerticalLift.class,p.shaft().inflate(1));if(all.isEmpty()){long since=absent.computeIfAbsent(p.id,k->l.getGameTime());if(l.getGameTime()-since>=100){var v=new VerticalLift(CAB,l);v.configure(p.id);l.addFreshEntity(v);absent.remove(p.id);}}else absent.remove(p.id); }});
  CommandRegistrationCallback.EVENT.register((d,c,e)->d.register(Commands.literal("neonlift").then(Commands.literal("select").then(Commands.argument("lift",IntegerArgumentType.integer(0,PLANS.size()-1)).then(Commands.argument("floor",IntegerArgumentType.integer(1,64)).executes(ctx->{
   var player=ctx.getSource().getPlayerOrException();var p=PLANS.get(IntegerArgumentType.getInteger(ctx,"lift"));int floor=IntegerArgumentType.getInteger(ctx,"floor")-1;
   if(player.level()!=player.level().getServer().overworld()||player.isSpectator())return 0;
   if(p.id()==2&&floor>0&&!CityApartments.owns(player,floor+1)){player.sendOverlayMessage(Component.literal("購入した自分の階と1階だけ選択できます"));return 0;}var cab=find((ServerLevel)player.level(),p);if(cab==null||!cab.inside(player)){player.sendOverlayMessage(Component.literal("エレベーターの箱に入って操作してください。"));return 0;}return cab.request(floor)?1:0;
  }))))));
 }
 public static VerticalLift find(ServerLevel l,Plan p){return l.getEntitiesOfClass(VerticalLift.class,p.shaft().inflate(1)).stream().filter(v->v.liftId()==p.id).findFirst().orElse(null);}
 public static Plan atPanel(BlockPos pos){for(var p:PLANS)if(pos.getX()==p.x+2&&pos.getZ()==p.z+3)for(int y:p.floors)if(pos.getY()==y+1)return p;return null;}
 public static Boolean landingDoorMayOpen(ServerLevel l,BlockPos pos){if(l.dimension()!=Level.OVERWORLD)return null;for(var p:PLANS)if(pos.getX()>=p.x&&pos.getX()<=p.x+1&&pos.getZ()==p.z+3)for(int y:p.floors)if(pos.getY()==y&&l.getBlockState(p.anchor()).is(ANCHOR)){var v=find(l,p);return v!=null&&v.doorsOpen()&&Math.abs(v.getY()-y)<.05;}return null;}
 public static class Panel extends Block {
  public static final MapCodec<Panel> CODEC=simpleCodec(Panel::new);
  Panel(BlockBehaviour.Properties p){super(p);}
  @Override public MapCodec<? extends Block> codec(){return CODEC;}
  @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos pos,Player player,BlockHitResult hit){if(l instanceof ServerLevel server&&l.dimension()==Level.OVERWORLD&&!player.isSpectator()){var p=atPanel(pos);if(p!=null){int target=p.nearest(pos.getY()-1);if(p.id()==2&&target>0&&!CityApartments.owns(player,target+1))return InteractionResult.FAIL;var cab=find(server,p);if(cab!=null){cab.request(p.nearest(pos.getY()-1));player.sendOverlayMessage(Component.literal("呼び出し受付 / 現在 "+(p.nearest(cab.getY())+1)+"階"));}}}return InteractionResult.SUCCESS;}
 }
}
