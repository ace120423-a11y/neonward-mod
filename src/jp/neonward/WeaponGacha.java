package jp.neonward;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.server.level.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;

/** Separate weapon cabinet next to the cyberware machine. */
public final class WeaponGacha {
 static final double X=518,Y=65,Z=447;
 static final String TOUCH="nw_casino_touch_weapon_capsule";
 static long ends;static int tier;static String result="1回1,000 Cr / 10連対応";
 static boolean near(ServerPlayer p){return p.isAlive()&&NeonCasino.inside(p)&&p.distanceToSqr(X,Y+1,Z)<=25;}
 static void init(){
  UseEntityCallback.EVENT.register((p,l,h,e,hit)->{if(!e.entityTags().contains(TOUCH))return InteractionResult.PASS;if(p instanceof ServerPlayer sp&&h==InteractionHand.MAIN_HAND)CasinoGacha.open(sp,true);return InteractionResult.SUCCESS;});
  ServerLifecycleEvents.SERVER_STOPPED.register(s->{ends=0;tier=0;result="1回1,000 Cr / 10連対応";});
  ServerTickEvents.END_SERVER_TICK.register(s->{var l=s.overworld();long now=l.getGameTime();if(now%(now<ends?4:20)!=0||!l.isPositionEntityTicking(BlockPos.containing(X,Y,Z)))return;if(l.players().stream().anyMatch(p->p.distanceToSqr(X,Y,Z)<1024))cabinet(l,now);});
 }
 static void b(ServerLevel l,String id,double x,double y,double z,float w,float h,float d,String block){CasinoProps.box(l,"weapon_capsule_"+id,X+x,Y+y,Z+z,w,h,d,block);}
 static void cabinet(ServerLevel l,long now){
  CasinoProps.touch(l,"weapon_capsule",X,Y,Z,1.8f,2.9f);
  b(l,"base",0,0,0,1.85f,.9f,1.3f,"polished_blackstone");b(l,"back",0,.9,-.45,1.7f,1.5f,.15f,"black_concrete");
  for(int s:new int[]{-1,1}){b(l,"frame"+s,s*.8,.9,0,.12f,1.5f,1.2f,"iron_block");b(l,"light"+s,s*.72,1,.58,.04f,1.25f,.04f,now<ends&&now%8==0?"cyan_concrete":"magenta_concrete");}
  b(l,"glass",0,.95,.5,1.45f,1.35f,.03f,"purple_stained_glass");b(l,"top",0,2.4,0,1.85f,.4f,1.3f,"black_concrete");
  // A sword and a rifle silhouette distinguish this cabinet without extra mobs.
  b(l,"blade",-.35,1.4,.15,.12f,.75f,.1f,"iron_block");b(l,"guard",-.35,1.32,.15,.4f,.08f,.14f,"gold_block");b(l,"grip",-.35,1.1,.15,.1f,.22f,.12f,"black_concrete");
  b(l,"rifle",.25,1.7,.15,.65f,.18f,.2f,"gray_concrete");b(l,"barrel",.65,1.74,.15,.2f,.07f,.1f,"iron_block");b(l,"rifle_grip",.1,1.5,.15,.12f,.2f,.17f,"black_concrete");b(l,"rifle_light",.3,1.88,.26,.4f,.04f,.03f,"cyan_concrete");
  b(l,"console",0,.91,.61,1.7f,.12f,.44f,"gray_concrete");b(l,"button",-.4,1.04,.63,.24f,.05f,.2f,"emerald_block");b(l,"hatch",0,.3,.66,.85f,.4f,.04f,"black_concrete");
  CasinoProps.text(l,"weapon_capsule_brand",X,Y+2.53,Z+.67,"WEAPON CAPSULE",0xff71ca,.33f,false);
  CasinoProps.text(l,"weapon_capsule_status",X,Y+.77,Z+.7,now<ends?"ARMORY SCANNING...":result,now<ends?0x67fff0:CyberwareCatalog.COLORS[tier],.21f,false);
 }
}
