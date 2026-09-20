package jp.neonward;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.world.entity.Display;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import com.mojang.math.Transformation;
import org.joml.Vector3f;
import org.joml.Quaternionf;
public final class CasinoWheel {
 static final int[] ORDER={0,32,15,19,4,21,2,25,17,34,6,27,13,36,11,30,8,23,10,5,24,16,33,1,20,14,31,9,22,18,29,7,28,12,35,3,26};
 static long started=-1000,end;static double angle,startAngle,target;
 static int index(int n){for(int i=0;i<37;i++)if(ORDER[i]==n)return i;throw new IllegalArgumentException();}
 static boolean busy(long now){return now<end;}
 static void spin(long now,int number){started=now;end=now+140;startAngle=angle;double desired=-index(number)*Math.PI*2/37;target=angle+Math.PI*8+((desired-angle)%(Math.PI*2)+Math.PI*2)%(Math.PI*2);}
 public static void init(){ServerLifecycleEvents.SERVER_STARTED.register(s->{end=0;started=-1000;angle=StockMarket.ledger==null?0:-index(StockMarket.ledger.casinoWheel)*Math.PI*2/37;});
  ServerTickEvents.END_SERVER_TICK.register(s->{var l=s.overworld();long now=l.getGameTime();if(now%2!=0||!busy(now)&&now%20!=0||!l.isPositionEntityTicking(new net.minecraft.core.BlockPos(510,65,456))||!l.getBlockState(NeonCasino.ROULETTE).is(NeonFurniture.BLOCKS.get("neon_counter")))return;
   if(end>0){double t=Math.clamp((now-started)/140.0,0,1);angle=startAngle+(target-startAngle)*(1-Math.pow(1-t,3));}
   for(int i=0;i<37;i++){double a=angle+i*Math.PI*2/37;int n=ORDER[i];String block=n==0?"emerald_block":CasinoGames.RED.contains(n)?"red_concrete":"black_concrete";
    cube(l,"segment_"+i,Math.sin(a)*4.8,0,Math.cos(a)*4.8,.71f,.16f,1.5f,(float)a,block);
    var at=new Vec3(510.5,65.7,456.5);String tag="nw_casino_number_"+i;var d=ClockworkDisplays.find(l,tag,at,Display.TextDisplay.class);if(d!=null){if(!ClockworkDisplays.initialized(d)){ClockworkFeedback.load(d,l,"{text:{text:''},billboard:'center',background:0,brightness:{block:15,sky:15},Invulnerable:1b,interpolation_duration:2,teleport_duration:2,Tags:['"+tag+"']}");d.setPos(at);((jp.neonward.mixin.MeterTextAccess)d).neonSetText(Component.literal(""+n).withColor(0xffffff));}((jp.neonward.mixin.LiftDisplayAccess)d).neonPositionDuration(2);d.setPos(510.5+Math.sin(a)*5.15,66.05,456.5+Math.cos(a)*5.15);ClockworkDisplays.transform(d,new Transformation(new Vector3f(),new Quaternionf(),new Vector3f(.45f),new Quaternionf()));ClockworkDisplays.publish(l,d);}
   }
   cube(l,"hub",0,.08,0,1.4f,.45f,1.4f,0,"gold_block");cube(l,"pointer",0,.30,6,.22f,.24f,.6f,0,"sea_lantern");
   double ball=busy(now)?-angle*2:0;cube(l,"ball",Math.sin(ball)*4.9,.36,Math.cos(ball)*4.9,.20f,.20f,.20f,0,"quartz_block");
   ClockworkFeedback.text(l,"casino_result",0,510.5,69.2,456.5,busy(now)?"ROULETTE / 回転中":"ROULETTE / "+(StockMarket.ledger==null?0:StockMarket.ledger.casinoWheel),0xffd674,.8f);
  });
 }
 static void cube(net.minecraft.server.level.ServerLevel l,String key,double x,double y,double z,float w,float h,float depth,float rot,String block){var at=new Vec3(510.5,65.55,456.5);String tag="nw_casino_wheel_"+key;var d=ClockworkDisplays.find(l,tag,at,Display.BlockDisplay.class);if(d==null)return;
  if(!ClockworkDisplays.initialized(d)){ClockworkFeedback.load(d,l,"{block_state:{Name:'minecraft:"+block+"'},Invulnerable:1b,brightness:{block:15,sky:15},width:15f,height:6f,interpolation_duration:2,Tags:['"+tag+"']}");d.setPos(at);}
  var rotation=new Quaternionf().rotationY(rot);var offset=new Vector3f(-w/2,0,-depth/2).rotate(rotation).add((float)x,(float)y,(float)z);
  ClockworkDisplays.transform(d,new Transformation(offset,rotation,new Vector3f(w,h,depth),new Quaternionf()));ClockworkDisplays.publish(l,d);
 }
}
