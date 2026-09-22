package jp.neonward;

import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.*;
import net.minecraft.world.InteractionHand;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

/** Client-only presentation. Never changes reach, damage, timing or equipment. */
public final class WeaponMotion {
 public enum Kind { BLADE, SHORT, HEAVY_BLADE, BAT, HAMMER, SPEAR, CHAIN, DUAL, SCYTHE, FIST, PISTOL, RIFLE, HEAVY_GUN, SHIELD }
 public record Profile(Kind kind,float roll,float lift,float reach,float weight,float kick) {
  public boolean gun(){return kind==Kind.PISTOL||kind==Kind.RIFLE||kind==Kind.HEAVY_GUN;}
  public boolean support(){return kind==Kind.HEAVY_BLADE||kind==Kind.HAMMER||kind==Kind.SPEAR||kind==Kind.SCYTHE||kind==Kind.RIFLE||kind==Kind.HEAVY_GUN;}
 }
 public static final Map<String,Profile> PROFILES=Map.ofEntries(
  e("neon_blade",Kind.BLADE,-8,.02f,.03f,.8f,0),
  e("kurosame_katana",Kind.BLADE,-16,.03f,.06f,1,0),
  e("akatsuki_wakizashi",Kind.SHORT,-22,-.02f,.02f,.6f,0),
  e("raikiri_odachi",Kind.HEAVY_BLADE,12,.05f,-.08f,1.35f,0),
  e("shock_bat",Kind.BAT,16,.04f,-.03f,.85f,0),
  e("riot_bat",Kind.BAT,23,.05f,-.05f,1.1f,0),
  e("coil_hammer",Kind.HAMMER,10,.04f,-.08f,1.2f,0),
  e("pile_maul",Kind.HAMMER,18,.02f,-.12f,1.5f,0),
  e("volt_spear",Kind.SPEAR,-12,-.06f,.04f,.95f,0),
  e("chain_kusarigama",Kind.CHAIN,20,-.02f,0,1,0),
  e("neon_dualblades",Kind.DUAL,-12,.02f,.04f,.75f,0),
  e("reaper_scythe",Kind.SCYTHE,-10,-.02f,.18f,1.3f,0),
  e("impact_gauntlet",Kind.FIST,0,.03f,0,.85f,0),
  e("pulse_rifle",Kind.RIFLE,-4,-.02f,-.04f,1,5),
  e("kestrel_pistol",Kind.PISTOL,-7,.01f,0,.75f,8),
  e("oni_handcannon",Kind.PISTOL,-4,-.02f,-.07f,1.3f,15),
  e("wisp_compact",Kind.PISTOL,-11,.02f,.03f,.6f,5),
  e("longwatch_sniper",Kind.RIFLE,-2,-.04f,-.15f,1.4f,12),
  e("storm_machinegun",Kind.HEAVY_GUN,-6,-.07f,-.10f,1.3f,6),
  e("ion_railgun",Kind.RIFLE,-3,-.03f,-.12f,1.3f,14),
  e("plasma_launcher",Kind.HEAVY_GUN,-8,-.09f,-.14f,1.5f,13),
  e("arc_caster",Kind.RIFLE,-6,-.03f,-.03f,.85f,4),
  e("cryo_projector",Kind.HEAVY_GUN,-5,-.06f,-.08f,1.1f,3),
  e("tactical_crossbow",Kind.RIFLE,0,-.05f,-.05f,.9f,7),
  e("sentinel_shield",Kind.SHIELD,6,-.02f,0,1,0));
 static Map.Entry<String,Profile> e(String id,Kind k,float r,float y,float z,float w,float kick){return Map.entry(id,new Profile(k,r,y,z,w,kick));}
 public static Profile profile(ItemStack s){if(s.isEmpty())return null;var id=BuiltInRegistries.ITEM.getKey(s.getItem());return id.getNamespace().equals("neonward")?PROFILES.get(id.getPath()):null;}
 public static float strike(float t){return (float)Math.sin(Math.PI*Math.max(0,Math.min(1,t)));}
 public static float recoil(AbstractClientPlayer p,ItemStack item,float delta){float c=p.getCooldowns().getCooldownPercent(item,delta);return c<.55f?0:(c-.55f)/.45f;}
 public static void first(AbstractClientPlayer p,InteractionHand hand,ItemStack item,float swing,float delta,PoseStack pose){
  var f=profile(item);if(f==null||p.isFallFlying()||p.isVisuallySwimming())return;
  int side=(hand==InteractionHand.MAIN_HAND?p.getMainArm():p.getMainArm().getOpposite())==HumanoidArm.RIGHT?1:-1;
  float hit=strike(swing),wind=(float)Math.sin(Math.PI*Math.min(1,swing*2));
  boolean use=p.isUsingItem()&&p.getUsedItemHand()==hand;
  float breath=(float)Math.sin((p.tickCount+delta)*.09)*.003f;
  pose.translate(0,f.lift+breath,-f.reach);
  pose.mulPose(Axis.ZP.rotationDegrees(side*f.roll*(use?.25f:1)));
  float chain=f.kind==Kind.CHAIN?ChainPullClient.phase(p.getId(),delta):-1;
  if(chain>=0){float extend=Math.min(1,chain*ChainPull.DURATION/ChainPull.FLIGHT);float reel=Math.max(0,(chain-.3f)/.7f);pose.translate(-side*.08*extend,.06*extend,-.22*extend+.38*reel);pose.mulPose(Axis.XP.rotationDegrees(-18*extend+32*reel));pose.mulPose(Axis.ZP.rotationDegrees(side*(-18*extend+28*reel)));return;}
  if(f.gun()){float kick=recoil(p,item,delta);pose.translate(0,kick*.025,kick*.08);pose.mulPose(Axis.XP.rotationDegrees(-kick*f.kick));return;}
  if(f.kind==Kind.SHIELD){if(use){pose.translate(-side*.04,.035,-.035);pose.mulPose(Axis.YP.rotationDegrees(side*8));}return;}
  if(use){
   float charge=Math.min(1,(p.getTicksUsingItem()+delta)/30f);
   if(f.kind==Kind.SPEAR||f.kind==Kind.FIST){pose.translate(side*.03,charge*.06,charge*.16);pose.mulPose(Axis.XP.rotationDegrees(-charge*12));}
   else if(f.kind!=Kind.DUAL&&f.kind!=Kind.CHAIN&&f.kind!=Kind.SCYTHE){pose.translate(-side*.03,.03,0);}
   return;
  }
  switch(f.kind){
   case SPEAR -> {pose.translate(0,-hit*.04,-hit*.38);pose.mulPose(Axis.XP.rotationDegrees(-hit*12));}
   case FIST -> {pose.translate(-side*hit*.12,hit*.035,-hit*.22);pose.mulPose(Axis.YP.rotationDegrees(side*hit*12));}
   case HAMMER,HEAVY_BLADE -> {pose.translate(0,wind*.05*f.weight,-hit*.08);pose.mulPose(Axis.XP.rotationDegrees(-wind*15*f.weight+hit*12));}
   case BAT,SCYTHE,CHAIN -> {pose.translate(-side*hit*.11,hit*.02,0);pose.mulPose(Axis.ZP.rotationDegrees(side*hit*20*f.weight));pose.mulPose(Axis.YP.rotationDegrees(side*hit*14));}
   default -> {pose.translate(-side*hit*.06,0,-hit*.05);pose.mulPose(Axis.ZP.rotationDegrees(side*hit*14*f.weight));}
  }
 }
 static void arm(ModelPart arm,float x,float y,float z){arm.xRot=x;arm.yRot=y;arm.zRot=z;}
 public static void third(HumanoidModel<?> model,HumanoidRenderState s){
  if(!(s instanceof AvatarRenderState)||s.isFallFlying||s.isVisuallySwimming||s.swimAmount>.1f||s.deathTime>0)return;
  var item=s.getMainHandItemStack();var f=profile(item);
  if(f==null)return;
  boolean right=s.mainArm==HumanoidArm.RIGHT;int sign=right?1:-1;
  var main=right?model.rightArm:model.leftArm;var other=right?model.leftArm:model.rightArm;
  var off=right?s.leftHandItemStack:s.rightHandItemStack;
  boolean free=off.isEmpty()||f.kind==Kind.DUAL||f.kind==Kind.FIST;
  boolean use=s.isUsingItem&&s.useItemHand==InteractionHand.MAIN_HAND;
  float hit=strike(s.attackTime),breath=(float)Math.sin(s.ageInTicks*.09)*.015f;
  float look=(float)Math.toRadians(Math.max(-65,Math.min(65,s.xRot)));
  float base=-.45f,spread=0,tilt=sign*(float)Math.toRadians(f.roll)*.4f;
  switch(f.kind){
   case SHORT -> {base=-.60f;spread=-sign*.12f;}
   case HEAVY_BLADE -> {base=-.85f;spread=-sign*.14f;}
   case BAT -> {base=-1.65f;spread=sign*.16f;}
   case HAMMER -> {base=-1.05f;spread=-sign*.10f;}
   case SPEAR -> {base=-.85f;spread=-sign*.18f;}
   case CHAIN -> {base=-.62f;spread=sign*.18f;}
   case SCYTHE -> {base=-.8f;spread=-sign*.22f;}
   case DUAL -> {base=-.72f;spread=sign*.15f;}
   case FIST -> {base=-1.25f;spread=-sign*.18f;}
   case PISTOL -> {base=use?-1.48f+look:-.70f;spread=-sign*.08f;}
   case RIFLE,HEAVY_GUN -> {base=(use?-1.45f:-1.05f)+look*.65f;spread=-sign*.14f;}
   default -> {}
  }
  // Keep the vanilla shield block pose; do not steal an occupied support hand.
  if(f.kind==Kind.SHIELD){if(!use){main.xRot=-.4f;main.yRot=-sign*.12f;}return;}
  arm(main,base+breath,spread,tilt);
  if(f.kind==Kind.DUAL||f.kind==Kind.FIST)arm(other,base-breath,-spread,-tilt);
  else if(f.support()&&free)arm(other,base-.10f,sign*.80f,-sign*.12f);
  if(use&&!f.gun()){
   if(f.kind==Kind.DUAL){arm(model.rightArm,-1.25f+look*.5f,-.55f,.65f);arm(model.leftArm,-1.25f+look*.5f,.55f,-.65f);}
   else if(f.kind==Kind.SPEAR||f.kind==Kind.FIST){float charge=Math.min(1,s.ticksUsingItem/30f);main.xRot-=charge*.5f;main.yRot-=sign*charge*.2f;}
   else {arm(main,-1.2f,-sign*.65f,sign*.12f);if(f.support()&&free)arm(other,-1.15f,sign*.55f,-sign*.1f);}
  }else if(!f.gun()&&s.attackTime>0){
   var active=s.attackArm==HumanoidArm.RIGHT?model.rightArm:model.leftArm;int attackSign=s.attackArm==HumanoidArm.RIGHT?1:-1;
   if(f.kind==Kind.FIST||f.kind==Kind.SPEAR){active.xRot-=hit*.45f;active.yRot-=attackSign*hit*.22f;}
   else if(f.kind==Kind.HAMMER||f.kind==Kind.HEAVY_BLADE){active.xRot-=hit*1.25f;active.zRot+=attackSign*hit*.2f;}
   else {active.xRot-=hit*.85f;active.yRot-=attackSign*hit*.65f;active.zRot-=attackSign*hit*.35f;}
   if(f.support()&&free){other.xRot=main.xRot+.12f;other.yRot=main.yRot+sign*.5f;}
  }
  if(f.gun()&&s.attackTime>0){float kick=hit*f.kick*.012f;main.xRot-=kick;if(f.support()&&free)other.xRot-=kick;}
  float chain=f.kind==Kind.CHAIN&&s instanceof ChainPoseState cs?cs.neonward$chainPhase():-1;
  if(chain>=0){float extend=Math.min(1,chain*ChainPull.DURATION/ChainPull.FLIGHT),reel=Math.max(0,(chain-.3f)/.7f);arm(main,-.6f-extend*.9f+reel*.65f,-sign*(.1f+reel*.35f),sign*.08f);if(free)arm(other,-.7f-reel*.35f,sign*.6f,-sign*.12f);}
 }
 private WeaponMotion(){}
}
