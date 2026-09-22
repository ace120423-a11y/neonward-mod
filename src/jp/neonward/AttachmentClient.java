package jp.neonward;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
public final class AttachmentClient implements ClientModInitializer {
 static KeyMapping key;
 public void onInitializeClient(){
  key=net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(new KeyMapping("key.neonward.attachments",org.lwjgl.glfw.GLFW.GLFW_KEY_B,KeyMapping.Category.register(NeonWard.id("attachments"))));
  ClientTickEvents.END_CLIENT_TICK.register(mc->{boolean click=false;while(key.consumeClick())click=true;if(click&&GunControls.holding())mc.player.connection.sendCommand("neonattach equip");});
  ClientPlayNetworking.registerGlobalReceiver(AttachmentService.Reply.TYPE,(payload,ctx)->ctx.client().execute(()->{var mc=ctx.client();if(!(mc.gui.screen() instanceof AttachmentScreen))mc.gui.setScreen(new AttachmentScreen());((AttachmentScreen)mc.gui.screen()).receive(com.google.gson.JsonParser.parseString(payload.json()).getAsJsonObject());}));
  net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback.EVENT.register((s,c,t,lines)->{int code=GunAttachments.code(s);if(code>=0)lines.add(net.minecraft.network.chat.Component.literal(GunAttachments.effect(code)).withColor(0xb9ff76));if(NeonArsenal.isGun(s)){lines.add(net.minecraft.network.chat.Component.literal("B：アタッチメント / 銃ごとに保存").withColor(0xb9ff76));for(int i=0;i<4;i++){code=GunAttachments.installed(s,i);if(code>=0)lines.add(net.minecraft.network.chat.Component.literal(GunAttachments.name(code)+" / "+GunAttachments.effect(code)).withColor(CyberwareCatalog.COLORS[code%5]));}}});
 }
}
