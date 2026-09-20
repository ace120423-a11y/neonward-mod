package jp.neonward;
import net.minecraft.client.Minecraft;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.*;
public final class GunControls {
 static long nextShot;
 public static boolean holding(){var mc=Minecraft.getInstance();return mc.player!=null&&!mc.player.isSpectator()&&mc.gui.screen()==null&&!CameraScreen.active()&&!PhoneEquipment.isHeld(mc.player)&&NeonArsenal.isGun(mc.player.getItemInHand(NeonArsenal.gunHand(mc.player)));}
 public static boolean aiming(){var mc=Minecraft.getInstance();return holding()&&mc.player.isUsingItem()&&NeonArsenal.isGun(mc.player.getUseItem());}
 public static float zoom(){var mc=Minecraft.getInstance();return aiming()?(NeonArsenal.sniper(mc.player.getUseItem())?.35f:.8f):1;}
 public static void init(){
  ClientTickEvents.START_CLIENT_TICK.register(mc->{if(!holding()){nextShot=0;return;}var gun=mc.player.getItemInHand(NeonArsenal.gunHand(mc.player));boolean click=false;while(mc.options.keyAttack.consumeClick())click=true;long now=System.currentTimeMillis();boolean wants=click||NeonArsenal.automatic(gun)&&mc.options.keyAttack.isDown();if(wants&&now>=nextShot&&!mc.player.getCooldowns().isOnCooldown(gun)){mc.player.connection.sendCommand("neongun fire");nextShot=now+(NeonArsenal.automatic(gun)?320:160);}});
  HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,NeonWard.id("gun_sights"),(g,d)->{if(!holding())return;var mc=Minecraft.getInstance();if(!mc.options.getCameraType().isFirstPerson())return;int x=g.guiWidth()/2,y=g.guiHeight()/2;if(aiming()){g.fill(x-1,y-1,x+2,y+2,0xff64ffe8);if(NeonArsenal.sniper(mc.player.getUseItem())){g.fill(x-65,y,x-9,y+1,0xff70f2df);g.fill(x+9,y,x+66,y+1,0xff70f2df);g.fill(x,y-45,x+1,y-9,0xff70f2df);g.fill(x,y+9,x+1,y+46,0xff70f2df);for(int j=15;j<60;j+=15){g.fill(x+j,y-2,x+j+1,y+3,0xff70f2df);g.fill(x-j,y-2,x-j+1,y+3,0xff70f2df);}}}g.centeredText(mc.font,"右長押し：構える / 左："+(NeonArsenal.automatic(mc.player.getItemInHand(NeonArsenal.gunHand(mc.player)))?"連射":"発射"),x,g.guiHeight()-62,0xff7bdcd8);});
 }
}
