package jp.neonward;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.level.Level;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.*;
/** Outdoor pedestrian navigation based on the city's actual saved walkable ground. */
public final class StreetNavigation {
 static final int W=608,H=1032;static byte[] grid;static MapPlaces.Place target;static List<BlockPos> path=List.of();static String status="";static int ticks,token;static boolean busy;
 static int index(int x,int z){return x>=-32&&x<576&&z>=-32&&z<1000?(z+32)*W+x+32:-1;}
 static BlockPos point(int i){return new BlockPos(i%W-32,65,i/W-32);}
 static int nearest(int x,int z){for(int r=0;r<=36;r++)for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++){if(Math.max(Math.abs(dx),Math.abs(dz))!=r)continue;int i=index(x+dx,z+dz);if(i>=0&&grid[i]>0)return i;}return -1;}
 public static List<BlockPos> route(int sx,int sz,int tx,int tz){int start=nearest(sx,sz),end=nearest(tx,tz);if(start<0||end<0)return List.of();int[] cost=new int[grid.length],prev=new int[grid.length];Arrays.fill(cost,Integer.MAX_VALUE);Arrays.fill(prev,-1);record Node(int id,int g,int f){}var open=new PriorityQueue<Node>(Comparator.comparingInt(Node::f));cost[start]=0;open.add(new Node(start,0,0));
  while(!open.isEmpty()){var n=open.poll();if(n.g()!=cost[n.id()])continue;if(n.id()==end){var out=new ArrayList<BlockPos>();for(int i=end;i!=-1;i=prev[i])out.add(point(i));Collections.reverse(out);return out;}
   int x=n.id()%W,z=n.id()/W;for(int[] v:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){int nx=x+v[0],nz=z+v[1];if(nx<0||nx>=W||nz<0||nz>=H)continue;int i=nz*W+nx;if(grid[i]==0)continue;int g=n.g()+(grid[i]==2?2:5);if(g<cost[i]){cost[i]=g;prev[i]=n.id();int h=(Math.abs(nx-end%W)+Math.abs(nz-end/W))*2;open.add(new Node(i,g,g+h));}}}
  return List.of();
 }
 static void start(MapPlaces.Place place){target=place;path=List.of();status="経路を計算中";token++;busy=false;calculate();}
 static void stop(){target=null;path=List.of();token++;busy=false;}
 static void calculate(){var mc=Minecraft.getInstance();if(target==null||mc.player==null||busy)return;if(mc.player.level().dimension()!=Level.OVERWORLD||Math.abs(mc.player.getY()-65)>4){status="地上の道路に出ると案内を開始";return;}if(grid==null){status="道路データを読み込めません";return;}int ticket=++token,sx=(int)Math.floor(mc.player.getX()),sz=(int)Math.floor(mc.player.getZ()),tx=(int)target.x(),tz=(int)target.z();busy=true;CompletableFuture.supplyAsync(()->route(sx,sz,tx,tz)).whenComplete((result,error)->mc.execute(()->{if(ticket!=token)return;busy=false;path=error==null?result:List.of();status=path.isEmpty()?"道路まで移動してください":"光る道をたどって入口へ";}));}
 static int closest(){var p=Minecraft.getInstance().player;int best=0;double distance=Double.MAX_VALUE;for(int i=0;i<path.size();i++){double d=p.position().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(path.get(i)));if(d<distance){distance=d;best=i;}}return best;}
 static void init(){try(var in=StreetNavigation.class.getResourceAsStream("/data/neonward/navigation.bin")){grid=Objects.requireNonNull(in).readAllBytes();if(grid.length!=W*H)throw new IllegalStateException("bad navigation grid");}catch(Exception e){grid=null;}
  ClientTickEvents.END_CLIENT_TICK.register(mc->{if(mc.player==null||mc.level==null){stop();return;}if(target==null)return;ticks++;boolean ground=mc.level.dimension()==Level.OVERWORLD&&Math.abs(mc.player.getY()-65)<=4;
   if(!ground){status="地上の道路に出ると案内を再開";return;}if(ticks%60==0)calculate();if(path.isEmpty())return;int closest=closest();if(closest>=path.size()-3&&mc.player.distanceToSqr(path.getLast().getX()+.5,65,path.getLast().getZ()+.5)<36){mc.player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("目的地に到着 / "+target.name()));stop();return;}
   if(ticks%6==0)for(int i=closest;i<Math.min(path.size(),closest+35);i+=3){var p=path.get(i);if(mc.level.hasChunkAt(p)&&mc.level.getBlockState(p).getCollisionShape(mc.level,p).isEmpty())mc.level.addParticle(new DustParticleOptions(0x50fff0,1.15f),p.getX()+.5,65.16,p.getZ()+.5,0,0,0);}
  });
  HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,NeonWard.id("street_navigation"),(g,dt)->{var mc=Minecraft.getInstance();if(mc.player==null||target==null||mc.gui.screen()!=null)return;int x=g.guiWidth()/2-125,y=8;g.fill(x,y,x+250,y+44,0xdd07151f);g.outline(x,y,250,44,0xff58eedd);String arrow="•";int distance=(int)Math.hypot(target.x()-mc.player.getX(),target.z()-mc.player.getZ());if(!path.isEmpty()){int n=closest();var next=path.get(Math.min(path.size()-1,n+7));double bearing=Math.toDegrees(Math.atan2(-(next.getX()+.5-mc.player.getX()),next.getZ()+.5-mc.player.getZ()));double angle=net.minecraft.util.Mth.wrapDegrees(bearing-mc.player.getYRot());arrow=Math.abs(angle)<25?"↑":Math.abs(angle)>150?"↓":angle>0?"←":"→";distance=path.size()-n;}
   g.text(mc.font,arrow+" "+mc.font.plainSubstrByWidth(target.name(),170)+"  "+distance+"m",x+8,y+7,0xff88fff1);g.text(mc.font,mc.font.plainSubstrByWidth(status,234),x+8,y+25,0xffd3e8ee);
  });
 }
}

