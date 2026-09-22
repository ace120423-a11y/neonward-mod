package jp.neonward;
import com.google.gson.*;
import java.nio.file.*;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.*;
import net.minecraft.client.*;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
public final class ObjectiveHud {
 static JsonArray rows=new JsonArray();static boolean visible=true,configured;static int ticks;static KeyMapping toggle;static final Path SETTINGS=FabricLoader.getInstance().getConfigDir().resolve("neonward-objectives.json");
 static void receive(JsonObject o){rows=o.getAsJsonArray("rows");}
 static void save(){try{Files.createDirectories(SETTINGS.getParent());Files.writeString(SETTINGS,"{\"visible\":"+visible+",\"configured\":true}");}catch(Exception e){System.err.println("Objective HUD settings: "+e);}}
 static void init(){try{var o=JsonParser.parseString(Files.readString(SETTINGS)).getAsJsonObject();visible=o.get("visible").getAsBoolean();configured=o.has("configured")&&o.get("configured").getAsBoolean();}catch(Exception ignored){}
  toggle=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.neonward.objectives",GLFW.GLFW_KEY_TAB,KeyMapping.Category.register(NeonWard.id("objectives"))));
  ClientTickEvents.END_CLIENT_TICK.register(mc->{ticks++;if(!configured&&mc.options!=null){if(mc.options.keyPlayerList.saveString().equals("key.keyboard.tab")){mc.options.keyPlayerList.setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F8));KeyMapping.resetMapping();mc.options.save();}configured=true;save();}while(toggle.consumeClick())if(mc.gui.screen()==null&&mc.player!=null){visible=!visible;save();mc.player.sendOverlayMessage(Component.literal("案内・クエスト表示："+(visible?"ON":"OFF")));}if(mc.player==null)rows=new JsonArray();});
  HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,NeonWard.id("objectives"),(g,dt)->{var mc=Minecraft.getInstance();if(!visible||mc.player==null||mc.gui.screen()!=null||GunControls.aiming()&&GunAttachments.installed(mc.player.getUseItem(),0)>=0)return;int w=Math.min(218,g.guiWidth()/2),x=g.guiWidth()-w-8,y=58;int capacity=Math.max(1,(g.guiHeight()-y-90)/40);int pages=Math.max(1,(rows.size()+capacity-1)/capacity),page=(ticks/160)%pages,start=page*capacity,count=Math.min(capacity,rows.size()-start),h=62+Math.max(1,count)*40;
   g.fill(x,y,x+w,y+h,0xd9091420);g.fill(x,y,x+2,y+h,0xff55ffe7);g.text(mc.font,"GUIDE / "+toggle.getTranslatedKeyMessage().getString()+" 切替"+(pages>1?" "+(page+1)+"/"+pages:""),x+8,y+7,0xff66fff0);
   g.text(mc.font,mc.font.plainSubstrByWidth("スマホ "+NeonClient.phoneKey.getTranslatedKeyMessage().getString()+" / 地図から道案内",w-16),x+8,y+23,0xffd7e9ee);g.text(mc.font,mc.font.plainSubstrByWidth("案内を見直す：/tutorial",w-16),x+8,y+37,0xff9dabbc);
   if(rows.isEmpty())g.text(mc.font,mc.font.plainSubstrByWidth("企業タワーの受付で依頼を受注",w-16),x+8,y+62,0xffffc975);
   for(int i=0;i<count;i++){var row=rows.get(start+i).getAsJsonObject();int yy=y+57+i*40;g.text(mc.font,mc.font.plainSubstrByWidth(row.get("title").getAsString(),w-16),x+8,yy,0xfff3e5ee);int line=0;for(var part:mc.font.split(Component.literal(row.get("progress").getAsString()),w-16)){if(line>=2)break;g.text(mc.font,part,x+8,yy+12+line*11,0xff66ffd9);line++;}}
  });
 }
}
