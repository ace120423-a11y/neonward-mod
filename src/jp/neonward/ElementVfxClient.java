package jp.neonward;
import java.util.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;

/** Bounded local geometry: at most 48 animations and 96 particle requests per tick. */
public final class ElementVfxClient implements ClientModInitializer {
 static final int LIMIT=48,PARTICLES=96;
 static final class Animation {
  final ElementVfx.Visual v;final ClientLevel level;final long start;boolean drawn;
  Animation(ElementVfx.Visual v,ClientLevel level,long start){this.v=v;this.level=level;this.start=start;}
 }
 static final List<Animation> ACTIVE=new ArrayList<>();
 static ClientLevel world;static int remaining,emitted,lastEmitted;static long totalEmitted;
 public void onInitializeClient(){
  ClientPlayNetworking.registerGlobalReceiver(ElementVfx.Visual.TYPE,(v,c)->c.client().execute(()->receive(v)));
  ClientPlayConnectionEvents.DISCONNECT.register((h,m)->{ACTIVE.clear();world=null;});
  ClientTickEvents.END_CLIENT_TICK.register(ElementVfxClient::tick);
 }
 static void receive(ElementVfx.Visual v){var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||!v.valid()||mc.player.distanceToSqr(v.x(),v.y(),v.z())>32*32)return;
  if(world!=mc.level){ACTIVE.clear();world=mc.level;}
  // Refresh one aura per target/attribute rather than stacking long-lived effects.
  if(v.aura())ACTIVE.removeIf(a->a.v.aura()&&a.v.target()==v.target()&&a.v.kind()==v.kind());
  if(ACTIVE.size()>=LIMIT)return;ACTIVE.add(new Animation(v,mc.level,mc.level.getGameTime()));
 }
 static void tick(Minecraft mc){
  if(mc.level!=world){ACTIVE.clear();world=mc.level;}if(mc.level==null||mc.player==null||mc.isPaused())return;
  remaining=PARTICLES;emitted=0;
  ACTIVE.removeIf(a->a.level!=mc.level||mc.level.getGameTime()-a.start>=a.v.duration()||(a.v.aura()&&(mc.level.getEntity(a.v.target())==null||!mc.level.getEntity(a.v.target()).isAlive())));
  // New impacts get priority over lingering status effects.
  for(int i=ACTIVE.size()-1;i>=0;i--){var a=ACTIVE.get(i);if(!a.v.aura())impact(mc,a);}
  for(var a:ACTIVE)if(a.v.aura())aura(mc,a);
  int players=0;for(var p:mc.level.players()){if(p.distanceToSqr(mc.player)>24*24||!p.isAlive()||p.isSpectator()||p.isInvisible())continue;if(players++>=16)break;weapon(mc,p);}
  lastEmitted=emitted;totalEmitted+=emitted;
 }
 static int color(int kind){return switch(kind){case 0->0x80edff;case 1->0xff922f;case 2->0x83dc49;case 3->0xffd38a;case 4->0xffba56;default->0xe72e63;};}
 static void particle(Minecraft mc,ParticleOptions type,Vec3 at,Vec3 velocity){if(remaining<=0||mc.player.distanceToSqr(at)>32*32)return;remaining--;emitted++;mc.level.addParticle(type,at.x,at.y,at.z,velocity.x,velocity.y,velocity.z);}
 static void dust(Minecraft mc,Vec3 at,int color,float size){particle(mc,new DustParticleOptions(color,size),at,Vec3.ZERO);}
 static void line(Minecraft mc,Vec3 from,Vec3 to,int color,float size,int count){for(int i=0;i<=count;i++)dust(mc,from.lerp(to,(double)i/count),color,size);}
 static Vec3 side(float yaw){double a=Math.toRadians(yaw);return new Vec3(Math.cos(a),0,Math.sin(a));}
 static Vec3 point(Vec3 center,Vec3 side,double x,double y){return center.add(side.scale(x)).add(0,y,0);}
 static void impact(Minecraft mc,Animation a){var v=a.v;int age=(int)(mc.level.getGameTime()-a.start);Vec3 c=new Vec3(v.x(),v.y(),v.z());if(mc.player.distanceToSqr(c)>32*32||age%2!=0&&a.drawn)return;boolean first=!a.drawn;a.drawn=true;var side=side(v.yaw());int k=v.kind();
  if(k!=ElementVfx.WAVE){double yaw=Math.toRadians(v.yaw());c=c.add(Math.sin(yaw)*.4,0,-Math.cos(yaw)*.4);}
  if(k==ElementVfx.THUNDER){if(age>6)return;
   for(int branch=0;branch<3;branch++){Vec3 from=c;for(int j=1;j<=4;j++){double x=(branch-1)*j*.25+Math.sin(j*9+branch+age)*.16,y=j*.20-.3;Vec3 to=point(c,side,x,y);line(mc,from,to,j==1?0xffffff:color(k),.58f,2);from=to;}}
   particle(mc,ParticleTypes.ELECTRIC_SPARK,c,new Vec3(0,.035,0));
  }else if(k==ElementVfx.WAVE||k==ElementVfx.BLAST){
   double radius=.3+age*(k==ElementVfx.WAVE?.24:.16);int count=18;
   for(int i=0;i<count;i++){double angle=i*Math.PI*2/count;var at=c.add(Math.cos(angle)*radius,0,Math.sin(angle)*radius);dust(mc,at,age<3?0xfff2cf:color(k),Math.max(.35f,1-age*.055f));
    if(i%3==0&&age<8)particle(mc,k==ElementVfx.BLAST?ParticleTypes.SMALL_FLAME:ParticleTypes.POOF,at,new Vec3(Math.cos(angle)*.035,.015,Math.sin(angle)*.035));}
   if(k==ElementVfx.BLAST&&first){for(int ray=0;ray<6;ray++){double angle=ray*Math.PI/3;line(mc,c,point(c,side,Math.cos(angle)*.4,Math.sin(angle)*.4),0xfff2d1,.8f,2);}}
  }else if(k==ElementVfx.BLEED){if(age>4)return;for(int cut=-1;cut<=1;cut+=2)line(mc,point(c,side,-.45,-.4*cut),point(c,side,.45,.4*cut),color(k),.6f,8);
  }else{
   for(int i=0;i<14;i++){double angle=-1.1+i*.17;Vec3 at=point(c,side,Math.sin(angle)*.85,Math.cos(angle)*.7-.35+age*.015);
    dust(mc,at,k==ElementVfx.POISON&&i%3==0?0x9b52c9:color(k),.65f);
    if(i%4==0)particle(mc,k==ElementVfx.FIRE?ParticleTypes.SMALL_FLAME:ParticleTypes.WITCH,at,new Vec3(0,k==ElementVfx.FIRE?.025:-.015,0));}
  }
 }
 static void aura(Minecraft mc,Animation a){var v=a.v;int age=(int)(mc.level.getGameTime()-a.start);if(age%5!=0)return;var e=mc.level.getEntity(v.target());if(e==null||e.distanceToSqr(mc.player)>24*24)return;
  var c=e.position().add(0,e.getBbHeight()*.55,0);double t=age*.5;int k=v.kind();
  if(k==ElementVfx.THUNDER){var start=c.add(Math.cos(t)*.4,.1,Math.sin(t)*.4);line(mc,start,start.add(.2,.25,-.2),color(k),.4f,3);particle(mc,ParticleTypes.ELECTRIC_SPARK,start,Vec3.ZERO);}
  else if(k==ElementVfx.FIRE){for(int i=0;i<3;i++)particle(mc,ParticleTypes.SMALL_FLAME,c.add(Math.cos(t+i)*.3,-.25,Math.sin(t+i)*.3),new Vec3(0,.015,0));particle(mc,ParticleTypes.SMOKE,c, new Vec3(0,.015,0));}
  else if(k==ElementVfx.POISON){for(int i=0;i<3;i++)dust(mc,c.add(Math.cos(t+i*2)*.35,Math.sin(t)*.2,Math.sin(t+i*2)*.35),i==0?0x9556bb:color(k),.55f);}
  else if(k==ElementVfx.BLEED){dust(mc,c.add(Math.sin(t)*.2,-.15,Math.cos(t)*.2),color(k),.55f);}
 }
 static int kind(net.minecraft.world.item.ItemStack item){var k=MeleeElements.kind(item);if(k!=null)return k.ordinal();if(PairedHands.gauntlet(item))return ElementVfx.BLAST;if(PairedHands.paired(item))return ElementVfx.BLEED;return -1;}
 static void weapon(Minecraft mc,net.minecraft.world.entity.player.Player p){var item=p.getMainHandItem();int k=kind(item);if(k<0)return;
  float attack=p.getAttackAnim(0);var look=p.getLookAngle();var lateral=new Vec3(look.z,0,-look.x).normalize();int hand=(p.swingingArm==net.minecraft.world.InteractionHand.OFF_HAND?-1:1)*(p.getMainArm()==HumanoidArm.RIGHT?1:-1);
  var c=p.getEyePosition().add(look.scale(.8)).add(0,-.25,0);
  if(attack>0&&!p.isUsingItem()){
   int tint=k==ElementVfx.BLEED?(hand>0?0x65f5ef:0xf45fc3):color(k);
   for(int i=0;i<7;i++){double t=Math.max(0,attack-i*.018),angle=(-1.15+t*2.3)*hand;var at=p.getEyePosition().add(look.scale(Math.cos(angle)*1.25)).add(lateral.scale(Math.sin(angle)*1.25)).add(0,-.30+Math.sin(t*Math.PI)*.18,0);dust(mc,at,tint,.45f);
    if(i==0&&(k==ElementVfx.FIRE||k==ElementVfx.POISON))particle(mc,k==ElementVfx.FIRE?ParticleTypes.SMALL_FLAME:ParticleTypes.WITCH,at,new Vec3(0,k==1?.015:-.02,0));}
  }else if(k==ElementVfx.BLAST&&p.isUsingItem()&&p.tickCount%3==0){float charge=Math.min(1,p.getTicksUsingItem()/30f);for(int s:new int[]{-1,1}){var fist=c.add(lateral.scale(s*.3));dust(mc,fist,charge>.7?0xffefbd:color(k),.45f+charge*.35f);particle(mc,ParticleTypes.ELECTRIC_SPARK,fist,Vec3.ZERO);}}
  else if(p.tickCount%12==0&&(k==ElementVfx.THUNDER||k==ElementVfx.POISON)){var tip=c.add(0,.15,0).add(lateral.scale(hand*.25));particle(mc,k==0?ParticleTypes.ELECTRIC_SPARK:ParticleTypes.WITCH,tip,new Vec3(0,k==0?0:-.025,0));}
 }
}
