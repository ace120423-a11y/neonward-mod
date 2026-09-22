package jp.neonward;
import java.util.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.core.particles.*;
import net.minecraft.sounds.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;

/** Locally drawn effects, bounded to 64 events / 128 particles per client tick. */
public final class GunVfxClient implements ClientModInitializer {
 static final String[] SOUNDS={"pulse","pistol","handcannon","compact","sniper","machinegun","rail","plasma","arc","cryo","crossbow"};
 static final int[] COLORS={0x5dfff0,0xffd889,0xffae54,0xff59b4,0x79eaff,0xffbb55,0x65ecff,0xff60c8,0xffe26a,0x9deeff,0x90ff98};
 static final class Effect {final GunVfx.Visual visual;final long time;boolean drawn;Effect(GunVfx.Visual v,long t){visual=v;time=t;}GunVfx.Visual v(){return visual;}long start(){return time;}}
 static final List<Effect> ACTIVE=new ArrayList<>();
 static final Map<Integer,Long> CRYO=new HashMap<>();
 static final Map<Integer,Loop> LOOPS=new HashMap<>();
 static ClientLevel world;static int budget,lastParticles;
 public void onInitializeClient(){ClientPlayNetworking.registerGlobalReceiver(GunVfx.Visual.TYPE,(v,c)->c.client().execute(()->receive(v)));ClientTickEvents.END_CLIENT_TICK.register(GunVfxClient::tick);ClientPlayConnectionEvents.DISCONNECT.register((h,m)->clear());}
 static SoundEvent sound(String id){return SoundEvent.createVariableRangeEvent(NeonWard.id("gun."+id));}
 static void clear(){var mc=Minecraft.getInstance();for(var s:LOOPS.values())mc.getSoundManager().stop(s);LOOPS.clear();ACTIVE.clear();CRYO.clear();world=null;}
 static void receive(GunVfx.Visual v){var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||!v.valid())return;if(world!=mc.level){clear();world=mc.level;}
  if(mc.player.distanceToSqr(v.from())>32*32&&mc.player.distanceToSqr(v.to())>32*32)return;
  if(v.kind()==9&&v.phase()==0){CRYO.put(v.owner(),mc.level.getGameTime()+8);return;}
  if(ACTIVE.size()>=64)return;ACTIVE.add(new Effect(v,mc.level.getGameTime()));
  if(v.phase()==0||v.phase()==1){var at=v.phase()==0?v.from():v.to();if(mc.player.distanceToSqr(at)<24*24){String key=v.phase()==0?SOUNDS[v.kind()]:(v.kind()==7||v.kind()==10&&v.mode()==0?"detonate":"impact");mc.getSoundManager().play(new SimpleSoundInstance(sound(key),SoundSource.PLAYERS,v.phase()==0?.6f:.35f,1,RandomSource.create(),at.x,at.y,at.z));}}
 }
 static void particle(Minecraft mc,ParticleOptions p,Vec3 at,Vec3 speed){if(budget<=0||mc.player.distanceToSqr(at)>32*32)return;budget--;mc.level.addParticle(p,at.x,at.y,at.z,speed.x,speed.y,speed.z);}
 static void dust(Minecraft mc,Vec3 at,int color,float size){particle(mc,new DustParticleOptions(color,size),at,Vec3.ZERO);}
 static void line(Minecraft mc,Vec3 a,Vec3 b,int color,float size,int n){for(int i=0;i<=n;i++)dust(mc,a.lerp(b,(double)i/n),color,size);}
 static Vec3 muzzle(Player p){var f=p.getLookAngle();var mc=Minecraft.getInstance();if(p==mc.player&&mc.options.getCameraType().isFirstPerson()&&p.isUsingItem())return p.getEyePosition().add(f.scale(.9)).add(0,-.07,0);int hand=(NeonArsenal.gunHand(p)==net.minecraft.world.InteractionHand.MAIN_HAND?1:-1)*(p.getMainArm()==net.minecraft.world.entity.HumanoidArm.RIGHT?1:-1);return p.getEyePosition().add(f.scale(.7)).add(new Vec3(f.z,0,-f.x).normalize().scale(-.18*hand)).add(0,-.2,0);}
 static boolean spraying(Minecraft mc,Player p){return p.isAlive()&&!p.isSpectator()&&p.isUsingItem()&&GunVfx.profile(p.getUseItem())==9&&CRYO.getOrDefault(p.getId(),0L)>mc.level.getGameTime()&&(p!=mc.player||mc.gui.screen()==null&&mc.options.keyAttack.isDown());}
 static final class Loop extends AbstractTickableSoundInstance {
  final Player player;final ClientLevel level;final boolean charge;
  Loop(Player p,boolean charge){super(sound(charge?"charge":"cryo"),SoundSource.PLAYERS,RandomSource.create());player=p;level=Minecraft.getInstance().level;this.charge=charge;looping=true;volume=charge?.32f:.4f;}
  public void tick(){var mc=Minecraft.getInstance();if(mc.level!=level||mc.player==null||mc.isPaused()||!player.isAlive()||player.isRemoved()||player.distanceToSqr(mc.player)>24*24||!(charge?player.isUsingItem()&&GunVfx.profile(player.getUseItem())==6:spraying(mc,player))||(player==mc.player&&mc.gui.screen()!=null)){stop();return;}x=player.getX();y=player.getEyeY();z=player.getZ();pitch=charge?.7f+Math.min(1,player.getTicksUsingItem()/20f)*.5f:1;}
 }
 static void tick(Minecraft mc){if(mc.level!=world){clear();world=mc.level;}if(mc.level==null||mc.player==null)return;if(mc.isPaused()){for(var l:LOOPS.values())mc.getSoundManager().stop(l);LOOPS.clear();return;}budget=128;
  long now=mc.level.getGameTime();CRYO.entrySet().removeIf(e->e.getValue()<=now);LOOPS.entrySet().removeIf(e->e.getValue().isStopped());ACTIVE.removeIf(e->now-e.start()>10);
  for(var e:ACTIVE)draw(mc,e,(int)(now-e.start()));
  int players=0;for(var p:mc.level.players()){if(p.distanceToSqr(mc.player)>24*24||players++>=16)continue;boolean cryo=spraying(mc,p),charge=p.isUsingItem()&&GunVfx.profile(p.getUseItem())==6&&p.isAlive()&&(p!=mc.player||mc.gui.screen()==null);
   var loop=LOOPS.get(p.getId());if(loop!=null&&(!cryo&&!charge||loop.charge!=charge)){mc.getSoundManager().stop(loop);LOOPS.remove(p.getId());loop=null;}
   if((cryo||charge)&&loop==null){loop=new Loop(p,charge);loop.tick();if(!loop.isStopped()){LOOPS.put(p.getId(),loop);mc.getSoundManager().play(loop);}}
   if(cryo){var from=muzzle(p);var f=p.getLookAngle();var end=mc.level.clip(new ClipContext(from,from.add(f.scale(16)),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p)).getLocation();double len=from.distanceTo(end);for(int i=0;i<18;i++){double d=(i+mc.level.getRandom().nextDouble())/18*len,a=mc.level.getRandom().nextDouble()*Math.PI*2;var side=new Vec3(f.z,0,-f.x).normalize();var at=from.add(f.scale(d)).add(side.scale(Math.cos(a)*d*.12)).add(0,Math.sin(a)*d*.08,0);particle(mc,ParticleTypes.SNOWFLAKE,at,f.scale(.09));if(i%4==0)dust(mc,at,0xb1f6ff,.5f);}}
   if(charge){var c=muzzle(p);double t=now*.8;for(int j=0;j<5;j++){double a=t+j*Math.PI*2/5;dust(mc,c.add(Math.cos(a)*.12,Math.sin(a)*.12,0),0x9ffaff,.4f);}if(p.tickCount%3==0)particle(mc,ParticleTypes.ELECTRIC_SPARK,c,Vec3.ZERO);}
  }lastParticles=128-budget;
 }
 static void draw(Minecraft mc,Effect e,int age){var v=e.v();int kind=v.kind(),phase=v.phase(),color=COLORS[kind];if(kind==10)color=v.mode()==1?0x91ed51:v.mode()==2?0x9becff:0xffb653;var from=v.from();var to=v.to();var dir=to.subtract(from).normalize();
  if(phase==0){if(e.drawn)return;e.drawn=true;var owner=mc.level.getEntity(v.owner());var muzzle=owner instanceof Player p?muzzle(p):from.add(dir.scale(.7));
   for(int i=0;i<(kind==2||kind==4?9:5);i++){double a=i*2.399;var at=muzzle.add(Math.cos(a)*.09,Math.sin(a)*.09,0);dust(mc,at,i%2==0?0xffffe0:color,kind==2?1:.55f);}
   if(kind<6){line(mc,muzzle,to,color,.45f,Math.min(36,(int)from.distanceTo(to)+1));particle(mc,ParticleTypes.SMOKE,muzzle,new Vec3(0,.015,0));}
   else if(kind==6){line(mc,muzzle,to,0xe4ffff,.7f,44);for(int i=0;i<20;i++){double t=i/19.0,a=i*.9;var at=muzzle.lerp(to,t).add(Math.cos(a)*.14,Math.sin(a)*.14,0);dust(mc,at,color,1);}}
   else if(kind==8)lightning(mc,muzzle,to);
  }else if(phase==2){if(e.drawn)return;e.drawn=true;if(kind==8){lightning(mc,from,to);return;}if(kind==7){for(int i=0;i<10;i++){double a=i*Math.PI/5;dust(mc,to.add(Math.cos(a)*.16,Math.sin(a)*.16,0),i%2==0?0xffd9f7:color,.8f);}}else line(mc,from,to,color,.3f,3);
  }else if(phase==1){if(age>6||age%2!=0)return;if(kind==7||kind==10&&v.mode()==0){double r=.15+age*.25;for(int i=0;i<20;i++){double a=i*Math.PI/10;dust(mc,to.add(Math.cos(a)*r,.08,Math.sin(a)*r),color,.9f-age*.08f);}if(age==0)particle(mc,ParticleTypes.EXPLOSION,to,Vec3.ZERO);}
   else if(kind==8||kind==10&&v.mode()==2){lightning(mc,to.add(-.3,0,0),to.add(.3,.7,0));}
   else if(kind==10&&v.mode()==1){for(int i=0;i<8;i++)particle(mc,ParticleTypes.WITCH,to.add(Math.sin(i)*.3,age*.03,Math.cos(i)*.3),new Vec3(0,.02,0));}
   else if(age==0)for(int i=0;i<6;i++)particle(mc,ParticleTypes.ELECTRIC_SPARK,to,new Vec3(Math.sin(i)*.04,.03,Math.cos(i)*.04));
  }
 }
 static void lightning(Minecraft mc,Vec3 from,Vec3 to){Vec3 prev=from;for(int i=1;i<=12;i++){var at=from.lerp(to,i/12.0);if(i<12)at=at.add(Math.sin(i*8.1)*.22,Math.cos(i*5.4)*.22,0);line(mc,prev,at,i%3==0?0xffffff:0xffe87b,.55f,2);if(i%4==0)line(mc,at,at.add(.3,.35,0),0x8feaff,.4f,3);prev=at;}}
}
