package jp.neonward;
import net.minecraft.world.item.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.client.renderer.*;
import net.minecraft.world.entity.LivingEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
public final class GunAttachmentVisual {
 public static ItemStack base(ItemStack gun,boolean loading){
  if(GunAttachments.installed(gun,0)<0)return loading?GunReloadMotion.mesh(gun,"body"):gun;
  int k=GunVfx.profile(gun);if(k!=0&&k!=4)return loading?GunReloadMotion.mesh(gun,"body"):gun;
  var copy=gun.copy();copy.set(DataComponents.ITEM_MODEL,NeonWard.id("attachments/bare_"+GunVfx.IDS[k]+(loading?"_body":"")));return copy;
 }
 public static void draw(ItemInHandRenderer renderer,LivingEntity player,ItemStack gun,PoseStack pose,SubmitNodeCollector nodes,int light,int side,float progress,boolean loading){
  int k=GunVfx.profile(gun);
  for(int mount=0;mount<4;mount++){int code=GunAttachments.installed(gun,mount);if(code<0)continue;pose.pushPose();
   if(mount==0){pose.translate(0,k==4?.38:.25,0);pose.scale(.55f,.55f,.55f);}
   else if(mount==1){if(loading)GunReloadMotion.part(pose,gun,side,progress);pose.translate(0,-.43,k<=5?.08:.25);pose.scale(.40f,.40f,.40f);}
   else if(mount==2){pose.translate(0,-.27,k<=5?.47:-.45);pose.scale(.35f,.35f,.35f);}
   else {pose.translate(0,0,k<=5?(k==4?1.5:1.0):-1.2);pose.scale(.40f,.40f,.40f);}
   renderer.renderItem(player,GunAttachments.stack(code),ItemDisplayContext.NONE,pose,nodes,light);pose.popPose();
  }
 }
}
