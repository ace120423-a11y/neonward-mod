package jp.neonward;
import net.minecraft.server.level.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
public final class AttachmentGacha {
 static final double X=522,Y=65,Z=447;
 static final String TOUCH="nw_casino_touch_attachment_capsule";
 static long ends;static int tier;
 static boolean near(ServerPlayer p){return p.isAlive()&&NeonCasino.inside(p)&&p.distanceToSqr(X,Y+1,Z)<=25;}
 static void b(ServerLevel l,String id,double x,double y,double z,float w,float h,float d,String material){CasinoProps.box(l,"attachment_capsule_"+id,X+x,Y+y,Z+z,w,h,d,material);}
 static void cabinet(ServerLevel l,long now){
  CasinoProps.touch(l,"attachment_capsule",X,Y,Z,1.8f,2.9f);
  b(l,"base",0,0,0,1.85f,.9f,1.3f,"polished_blackstone");b(l,"back",0,.9,-.45,1.7f,1.5f,.15f,"black_concrete");b(l,"glass",0,.95,.5,1.45f,1.35f,.03f,"cyan_stained_glass");b(l,"top",0,2.4,0,1.85f,.4f,1.3f,"black_concrete");
  for(int s:new int[]{-1,1}){b(l,"rail"+s,s*.8,.9,0,.1f,1.5f,1.2f,"iron_block");b(l,"light"+s,s*.72,1,.58,.04f,1.25f,.04f,now<ends&&now%8==0?"white_concrete":"lime_concrete");}
  // A hollow optic, rail and magazine distinguish this machine from the weapon cabinet.
  b(l,"optic_low",-.25,1.6,.14,.55f,.08f,.32f,"iron_block");b(l,"optic_top",-.25,2,.14,.55f,.08f,.32f,"iron_block");
  for(int s:new int[]{-1,1})b(l,"optic_side"+s,-.25+s*.24,1.68,.14,.07f,.32f,.32f,"gray_concrete");
  b(l,"lens",-.25,1.69,.27,.38f,.27f,.03f,"light_blue_stained_glass");b(l,"mag",.35,1.15,.15,.28f,.66f,.27f,"gray_concrete");b(l,"mag_stripe",.35,1.6,.29,.2f,.05f,.03f,"lime_concrete");
  b(l,"console",0,.91,.61,1.7f,.12f,.44f,"gray_concrete");b(l,"button",-.4,1.04,.63,.24f,.05f,.2f,"emerald_block");
  CasinoProps.text(l,"attachment_capsule_brand",X,Y+2.53,Z+.67,"MOD CAPSULE",0xb9ff76,.33f,false);
  CasinoProps.text(l,"attachment_capsule_status",X,Y+.77,Z+.7,now<ends?"CALIBRATING...":"1回1,000 Cr / 10連",now<ends?0xb9ff76:CyberwareCatalog.COLORS[tier],.21f,false);
 }
 static void init(){
  net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.register((p,l,h,e,hit)->{if(!e.entityTags().contains(TOUCH))return InteractionResult.PASS;if(p instanceof ServerPlayer sp&&h==InteractionHand.MAIN_HAND)AttachmentService.open(sp,"gacha");return InteractionResult.SUCCESS;});
  net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(s->{ends=0;tier=0;});
  net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(s->{var l=s.overworld();long now=l.getGameTime();if(now%(now<ends?4:20)!=0||!l.isPositionEntityTicking(BlockPos.containing(X,Y,Z)))return;if(l.players().stream().anyMatch(p->p.distanceToSqr(X,Y,Z)<1024))cabinet(l,now);});
 }
}
