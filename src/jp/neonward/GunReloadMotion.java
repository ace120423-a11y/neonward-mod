package jp.neonward;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;

/** Four phases: present, extract, insert, close/charge. Mirrors with the weapon hand. */
public final class GunReloadMotion {
 static float smooth(float a,float b,float t){float x=Math.clamp((t-a)/(b-a),0,1);return x*x*x*(x*(x*6-15)+10);}
 public static float extraction(float t){return smooth(.12f,.35f,t)*(1-smooth(.54f,.78f,t));}
 public static float rack(float t){return (float)Math.sin(Math.PI*smooth(.79f,.95f,t));}
 public static float poseAmount(float t){return smooth(0,.13f,t)*(1-smooth(.9f,1,t));}
 public static ItemStack mesh(ItemStack s,String part){var copy=s.copy();copy.set(DataComponents.ITEM_MODEL,NeonWard.id("reload/"+GunVfx.IDS[GunVfx.profile(s)]+"_"+part));return copy;}
 public static void body(PoseStack pose,ItemStack s,int side,float t){float f=poseAmount(t);int k=GunVfx.profile(s);pose.translate(side*(.12+.06*f),-.32+.37*f,-.85-.2*f);pose.mulPose(Axis.ZP.rotationDegrees(side*f*(k==10?12:k==9?32:-28)));pose.mulPose(Axis.XP.rotationDegrees(f*(k==10?-18:12)));pose.mulPose(Axis.YP.rotationDegrees(side*f*12));float scale=.7f-.1f*f;pose.scale(scale,scale,scale);if(k<=5)pose.mulPose(Axis.YP.rotationDegrees(180));}
 public static void part(PoseStack pose,ItemStack s,int side,float t){float d=extraction(t);int k=GunVfx.profile(s);
  if(k==10){pose.translate(0,0,-.32*smooth(.12f,.54f,t)*(1-smooth(.8f,.97f,t)));return;}
  if(k==6||k==8){pose.translate(side*.35*d,.22*d,0);pose.mulPose(Axis.ZP.rotationDegrees(side*d*18));}
  else if(k==7||k==9){pose.translate(side*.28*d,-.35*d,.12*d);pose.mulPose(Axis.XP.rotationDegrees(d*20));}
  else{pose.translate(-side*.08*d,-.52*d,.08*d);pose.mulPose(Axis.ZP.rotationDegrees(side*d*12));}
 }
 /** Empty pistols stay locked back until the fresh magazine is seated; tactical reloads retain the chambered round. */
 public static float slide(ItemStack s,float t){return GunReload.emptyReload(s)?1-smooth(.81f,.88f,t):0;}
 public static float handle(ItemStack s,float t){return GunReload.emptyReload(s)?smooth(.78f,.85f,t)*(1-smooth(.88f,.94f,t)):0;}
 public static float boltOpen(float t){return smooth(.04f,.13f,t)*(1-smooth(.84f,.96f,t));}
 public static float boltTravel(float t){return smooth(.13f,.21f,t)*(1-smooth(.75f,.84f,t));}
 public static float coverOpen(float t){return smooth(.04f,.15f,t)*(1-smooth(.70f,.78f,t));}
 public static void action(PoseStack pose,ItemStack s,int side,float t){int k=GunVfx.profile(s);if(k==10){float d=1-smooth(.48f,.78f,t);pose.translate(-side*.25*d,.20*d,.15*d);pose.mulPose(Axis.YP.rotationDegrees(side*d*25));}else pose.translate(0,0,-.16*slide(s,t));}
 public static void mechanism(PoseStack pose,ItemStack s,int side,float t){
  int k=GunVfx.profile(s);
  if(k==4){pose.translate(0,0,-.28*boltTravel(t));pose.translate(0,.1,0);pose.mulPose(Axis.ZP.rotationDegrees(-side*65*boltOpen(t)));pose.translate(0,-.1,0);}
  else if(k==0||k==5)pose.translate(0,0,-.24*handle(s,t));
  else if(k==9){pose.translate(0,.02,0);pose.mulPose(Axis.YP.rotationDegrees(90*extraction(t)));pose.translate(0,-.02,0);}
  else {float latch=smooth(.05f,.14f,t)*(1-smooth(.78f,.9f,t));pose.translate(side*.12*latch,.06*latch,0);pose.mulPose(Axis.ZP.rotationDegrees(side*35*latch));}
 }
 public static void cover(PoseStack pose,float t){pose.translate(0,.2,.3);pose.mulPose(Axis.XP.rotationDegrees(-75*coverOpen(t)));pose.translate(0,-.2,-.3);}
 public static void support(PoseStack pose,ItemStack s,int side,float t){float d=extraction(t);int k=GunVfx.profile(s);float cock=k==4?Math.max(boltOpen(t),boltTravel(t)):k==1||k==2||k==3?GunReload.emptyReload(s)?rack(t):0:handle(s,t);pose.translate(side*(k==10?.48:.45)-side*.18*cock,.08-.24*d+.18*cock,-.22+.1*d+.12*cock);pose.mulPose(Axis.ZP.rotationDegrees(-side*(12+d*18+cock*20)));pose.mulPose(Axis.XP.rotationDegrees(-d*12-cock*18));}
}
