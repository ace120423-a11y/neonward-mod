package jp.neonward;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
public final class CyberwareGachaClient implements ClientModInitializer {
 public void onInitializeClient(){ClientPlayNetworking.registerGlobalReceiver(CyberwareGacha.Snapshot.TYPE,(payload,ctx)->ctx.client().execute(()->{var data=com.google.gson.JsonParser.parseString(payload.json()).getAsJsonObject();if(!(ctx.client().gui.screen() instanceof CyberwareGachaScreen))ctx.client().gui.setScreen(new CyberwareGachaScreen());((CyberwareGachaScreen)ctx.client().gui.screen()).receive(data);}));}
}
