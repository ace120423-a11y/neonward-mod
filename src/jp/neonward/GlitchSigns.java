package jp.neonward;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.entity.Display;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.minecraft.ChatFormatting;
import jp.neonward.mixin.MeterTextAccess;
public final class GlitchSigns {
 record Base(Component text,Vec3 pos){}
 static final Map<Display.TextDisplay,Base> bases=new WeakHashMap<>();
 public static void init(){ServerTickEvents.END_SERVER_TICK.register(server->{if(server.getTickCount()%4!=0)return;
  var seen=new HashSet<Integer>();for(var player:server.getPlayerList().getPlayers())for(var text:player.level().getEntitiesOfClass(Display.TextDisplay.class,player.getBoundingBox().inflate(64),e->e.entityTags().contains("nw_glitch_sign"))){if(!seen.add(text.getId()))continue;
   var access=(MeterTextAccess)text;var base=bases.computeIfAbsent(text,e->{Vec3 p=e.position();for(String tag:e.entityTags())if(tag.startsWith("nw_sign_xyz_")){try{var a=tag.substring(12).split("_");p=new Vec3(Double.parseDouble(a[0]),Double.parseDouble(a[1]),Double.parseDouble(a[2]));}catch(Exception ignored){}}return new Base(Component.literal(access.neonGetText().getString()),p);});
   long phase=Math.floorMod(player.level().getGameTime()+text.getUUID().hashCode(),160);boolean glitch=phase<8;boolean dim=phase>=8&&phase<12;
   access.neonSetText(base.text.copy().withStyle(glitch?ChatFormatting.LIGHT_PURPLE:dim?ChatFormatting.DARK_AQUA:ChatFormatting.AQUA));access.neonSetOpacity((byte)(dim?170:255));
   double j=glitch?(phase<4?.014:-.014):0;text.setPos(base.pos.x+j,base.pos.y,base.pos.z);
  }
 });}
}
