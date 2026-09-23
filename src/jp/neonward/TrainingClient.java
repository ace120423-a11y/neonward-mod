package jp.neonward;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.client.Minecraft;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

/** Native target renderer and range-only meter, separate from gun/ammo/actionbar UI. */
public final class TrainingClient {
 static String last="";
 static boolean inside(Minecraft mc){return mc.player!=null&&mc.level!=null&&TrainingRange.contains(mc.level,mc.player.blockPosition());}
 public static void initClient(){
  EntityRendererRegistry.register(TrainingRange.DUMMY,ZombieRenderer::new);
  ClientPlayNetworking.registerGlobalReceiver(TrainingRange.Meter.TYPE,(meter,ctx)->ctx.client().execute(()->last=inside(ctx.client())?meter.line():""));
  ClientPlayConnectionEvents.DISCONNECT.register((handler,mc)->last="");
  ClientTickEvents.END_CLIENT_TICK.register(mc->{if(!inside(mc))last="";});
  HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,NeonWard.id("training_meter"),(g,dt)->{
   var mc=Minecraft.getInstance();if(!inside(mc)){last="";return;}if(last.isEmpty()||mc.gui.screen()!=null)return;
   String text=mc.font.plainSubstrByWidth(last,Math.max(1,g.guiWidth()-16));
   int center=g.guiWidth()/2,half=(mc.font.width(text)+1)/2;
   g.fill(center-half-5,20,center+half+5,37,0xd9081724);
   g.centeredText(mc.font,text,center,24,0xff83fff0);
  });
 }
}
