package jp.neonward;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class MarketInteriorClient implements ClientModInitializer {
 public record Open(BlockPos pos) implements CustomPacketPayload {
  public static final Type<Open> TYPE=new Type<>(NeonWard.id("market_open"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Open> CODEC=StreamCodec.composite(BlockPos.STREAM_CODEC,Open::pos,Open::new);
  public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 @Override public void onInitializeClient(){
  ClientPlayNetworking.registerGlobalReceiver(Open.TYPE,(packet,context)->context.client().execute(()->context.client().gui.setScreen(new StockScreen(packet.pos()))));
 }
}
