package jp.neonward;
public final class WeaponSalesClient implements net.fabricmc.api.ClientModInitializer {
 public void onInitializeClient(){net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(WeaponSales.Reply.TYPE,(p,c)->c.client().execute(()->{if(c.client().gui.screen() instanceof WeaponSellScreen s)s.receive(com.google.gson.JsonParser.parseString(p.json()).getAsJsonObject());}));}
}
