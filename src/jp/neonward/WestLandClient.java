package jp.neonward;
import com.google.gson.JsonParser;
public final class WestLandClient implements net.fabricmc.api.ClientModInitializer {
 public void onInitializeClient(){LandCatalog.init();net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(WestLand.State.TYPE,(packet,c)->c.client().execute(()->{var o=JsonParser.parseString(packet.json()).getAsJsonObject();WestLand.CLIENT_OWNERS.clear();for(var e:o.getAsJsonObject("owners").entrySet())WestLand.CLIENT_OWNERS.put(Integer.parseInt(e.getKey()),e.getValue().getAsString());if(o.has("plot")){if(c.client().gui.screen() instanceof LandScreen s&&s.plot==o.get("plot").getAsInt()&&s.terminal==o.get("terminal").getAsInt())s.receive(o);else c.client().gui.setScreen(new LandScreen(o));}}));net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((h,c)->WestLand.CLIENT_OWNERS.clear());}
}
