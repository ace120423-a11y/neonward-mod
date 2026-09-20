package jp.neonward;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.*;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.world.InteractionResult;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class NeonClient implements ClientModInitializer {

 static KeyMapping cyberKey,phoneKey;static int drivenId=-1;static float drivenYaw;static long phoneInputUntil;
 public void onInitializeClient(){ GunControls.init();ObjectiveHud.init();StreetNavigation.init();PhoneAudio.init();NeonEnchantEffects.init(); net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(StockMarket.Snapshot.TYPE,(payload,ctx)->ctx.client().execute(()->{if(payload.json().contains("\"compact_open\"")){var o=com.google.gson.JsonParser.parseString(payload.json()).getAsJsonObject();var at=new net.minecraft.core.BlockPos(o.get("x").getAsInt(),o.get("y").getAsInt(),o.get("z").getAsInt());String kind=o.get("compact_open").getAsString();ctx.client().gui.setScreen(kind.equals("arms")?new ArmsShopScreen():kind.equals("fashion")?new FashionScreen(at):new GuildScreen(at,kind.equals("exchange")));}else if(payload.json().contains("\"compact_arms\"")){if(ctx.client().gui.screen() instanceof ArmsShopScreen arms)arms.receive(com.google.gson.JsonParser.parseString(payload.json()).getAsJsonObject());}else if(payload.json().contains("\"objectives_ui\"")){ObjectiveHud.receive(com.google.gson.JsonParser.parseString(payload.json()).getAsJsonObject());}else if(payload.json().contains("\"welcome_ui\"")){ctx.client().gui.setScreen(new WelcomeScreen());}else if(payload.json().contains("\"street_ui\"")){var o=com.google.gson.JsonParser.parseString(payload.json()).getAsJsonObject();String page=o.get("street_ui").getAsString();if(!(ctx.client().gui.screen() instanceof StreetScreen st)||!st.page.equals(page))ctx.client().gui.setScreen(new StreetScreen(page));((StreetScreen)ctx.client().gui.screen()).receive(o);}else if(payload.json().contains("\"estate_ui\"")){var s=new PhoneAppScreen(8);ctx.client().gui.setScreen(s);s.receive(com.google.gson.JsonParser.parseString(payload.json()).getAsJsonObject());}else if(payload.json().contains("\"call_ui\"")){PhoneAudio.receive(com.google.gson.JsonParser.parseString(payload.json()).getAsJsonObject());}else if(payload.json().contains("\"phone_ui\"")){if(ctx.client().gui.screen() instanceof PhoneAppScreen ps)ps.receive(com.google.gson.JsonParser.parseString(payload.json()).getAsJsonObject());}else if(payload.json().contains("\"friends_ui\"")){var o=com.google.gson.JsonParser.parseString(payload.json()).getAsJsonObject();if(ctx.client().gui.screen() instanceof FriendsScreen fs)fs.receive(o);}else if(payload.json().contains("\"medicine\"")){var o=com.google.gson.JsonParser.parseString(payload.json()).getAsJsonObject();var pos=new net.minecraft.core.BlockPos(o.get("x").getAsInt(),o.get("y").getAsInt(),o.get("z").getAsInt());boolean shop=o.get("shop").getAsBoolean();if(!(ctx.client().gui.screen() instanceof MedicineScreen ms)||!ms.pos.equals(pos))ctx.client().gui.setScreen(new MedicineScreen(pos,shop));((MedicineScreen)ctx.client().gui.screen()).receive(o);}else if(payload.json().contains("\"interior_tv\"")){var o=com.google.gson.JsonParser.parseString(payload.json()).getAsJsonObject();ctx.client().gui.setScreen(new TelevisionScreen(new net.minecraft.core.BlockPos(o.get("x").getAsInt(),o.get("y").getAsInt(),o.get("z").getAsInt())));}else if(payload.json().contains("\"farm\"")){var o=com.google.gson.JsonParser.parseString(payload.json()).getAsJsonObject();if(!(ctx.client().gui.screen() instanceof FarmScreen))ctx.client().gui.setScreen(new FarmScreen());((FarmScreen)ctx.client().gui.screen()).receive(o);}else if(payload.json().contains("\"interiors\"")){var o=com.google.gson.JsonParser.parseString(payload.json()).getAsJsonObject();if(!(ctx.client().gui.screen() instanceof InteriorShopScreen))ctx.client().gui.setScreen(new InteriorShopScreen());((InteriorShopScreen)ctx.client().gui.screen()).receive(o);}else if(payload.json().contains("\"parlor\"")){var o=com.google.gson.JsonParser.parseString(payload.json()).getAsJsonObject();String kind=o.get("parlor").getAsString();if(ctx.client().gui.screen() instanceof ParlorScreen ps&&ps.kind.equals(kind))ps.receive(o);else if(o.get("open").getAsBoolean()){var screen=new ParlorScreen(kind);ctx.client().gui.setScreen(screen);screen.receive(o);}}else if(payload.json().contains("\"garage\"")){if(ctx.client().gui.screen() instanceof GarageScreen garage)garage.receive(com.google.gson.JsonParser.parseString(payload.json()).getAsJsonObject());}else if(payload.json().contains("\"casino\"")){var o=com.google.gson.JsonParser.parseString(payload.json()).getAsJsonObject();if(!(ctx.client().gui.screen() instanceof CasinoScreen cs)||cs.machine!=(o.has("slotMachine")?o.get("slotMachine").getAsInt():-1))ctx.client().gui.setScreen(new CasinoScreen(o.has("page")?o.get("page").getAsInt():0,o.has("slotMachine")?o.get("slotMachine").getAsInt():-1));((CasinoScreen)ctx.client().gui.screen()).receive(o);}else if(payload.json().contains("private_home")){var o=com.google.gson.JsonParser.parseString(payload.json()).getAsJsonObject();if(!(ctx.client().gui.screen() instanceof HomeScreen))ctx.client().gui.setScreen(new HomeScreen());((HomeScreen)ctx.client().gui.screen()).receive(o);}else if(ctx.client().gui.screen() instanceof GuildScreen guild)guild.receive(com.google.gson.JsonParser.parseString(payload.json()).getAsJsonObject());else if(ctx.client().gui.screen() instanceof CyberwareScreen cyber)cyber.receive(com.google.gson.JsonParser.parseString(payload.json()).getAsJsonObject());else if(ctx.client().gui.screen() instanceof FashionScreen fashion)fashion.receive(com.google.gson.JsonParser.parseString(payload.json()).getAsJsonObject());else if(ctx.client().gui.screen() instanceof StockScreen screen&&!payload.json().contains("fashion_cash")&&!payload.json().contains("cyberware"))screen.receive(com.google.gson.JsonParser.parseString(payload.json()).getAsJsonObject());}));
  net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((p,l,hand,hit)->{var s=l.getBlockState(hit.getBlockPos());if(l.isClientSide()&&(hit.getBlockPos().equals(NeonCasino.DESK)||hit.getBlockPos().equals(NeonCasino.ROULETTE)||hit.getBlockPos().equals(NeonCasino.BLACKJACK))){Minecraft.getInstance().gui.setScreen(new CasinoScreen(hit.getBlockPos().equals(NeonCasino.ROULETTE)?2:hit.getBlockPos().equals(NeonCasino.BLACKJACK)?1:0));return InteractionResult.SUCCESS;}if(l.isClientSide()&&hit.getBlockPos().equals(NeonCasino.DEALER)){Minecraft.getInstance().gui.setScreen(new GarageScreen());return InteractionResult.SUCCESS;}if(l.isClientSide()&&(s.is(GuildServices.QUEST)||s.is(GuildServices.EXCHANGE))&&!p.isSpectator()){Minecraft.getInstance().gui.setScreen(new GuildScreen(hit.getBlockPos(),s.is(GuildServices.EXCHANGE)));return InteractionResult.SUCCESS;}if(l.isClientSide()&&s.is(Cyberware.TERMINAL)&&!p.isSpectator()){Minecraft.getInstance().gui.setScreen(new CyberwareScreen(hit.getBlockPos()));return InteractionResult.SUCCESS;}if(l.isClientSide()&&s.is(StreetFashion.COUNTER)&&!p.isSpectator()){Minecraft.getInstance().gui.setScreen(new FashionScreen(hit.getBlockPos()));return InteractionResult.SUCCESS;}if(l.isClientSide()&&s.is(NeonFurniture.BLOCKS.get("tv_remote"))&&!p.isSpectator()){Minecraft.getInstance().gui.setScreen(new TelevisionScreen());return InteractionResult.SUCCESS;}if(l.isClientSide()&&StockMarket.isPC(s)&&!p.isSpectator()){Minecraft.getInstance().gui.setScreen(new StockScreen(hit.getBlockPos()));return InteractionResult.SUCCESS;}if(l.isClientSide()&&s.is(VendingMachines.BLOCK)&&!p.isSpectator()){Minecraft.getInstance().gui.setScreen(new VendingScreen(hit.getBlockPos().below(s.getValue(VendingMachines.Machine.PART))));return InteractionResult.SUCCESS;}return InteractionResult.PASS;});
  net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((p,l,h,hit)->{var b=l.getBlockState(hit.getBlockPos()).getBlock();if(l.isClientSide()&&h==net.minecraft.world.InteractionHand.MAIN_HAND&&(b==NeonFurniture.BLOCKS.get("med_workbench")||b==NeonFurniture.BLOCKS.get("med_counter"))){Minecraft.getInstance().gui.setScreen(new CyberwareScreen(hit.getBlockPos()));}return InteractionResult.PASS;});
  net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(StockMarket.Snapshot.TYPE,(payload,ctx)->{if(payload.json().contains("\"cyberware_open\""))ctx.client().execute(()->{var o=com.google.gson.JsonParser.parseString(payload.json()).getAsJsonObject();ctx.client().gui.setScreen(new CyberwareScreen(new net.minecraft.core.BlockPos(o.get("x").getAsInt(),o.get("y").getAsInt(),o.get("z").getAsInt())));});});
  CityResidentRenderer.init();CyberEnemyRenderer.init();SpireBossRenderer.init();EntityRendererRegistry.register(LiftSystem.CAB,NoopRenderer::new);
  EntityRendererRegistry.register(HologramFish.TYPE,NoopRenderer::new);
  net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.register((player,level,hand,entity,hit)->{if(level.isClientSide()&&entity instanceof CityResident r&&r.getDisplayName().getString().equals("サイバーウェア技師")){Minecraft.getInstance().gui.setScreen(new CyberwareScreen(entity.blockPosition()));}return InteractionResult.PASS;});
  net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.register((player,level,hand,entity,hit)->{if(level.isClientSide()&&entity instanceof CityResident r){String n=r.getDisplayName().getString();if(n.startsWith("NEON MOTOR")){Minecraft.getInstance().gui.setScreen(new GarageScreen());return InteractionResult.SUCCESS;}if(n.startsWith("CASINO")){Minecraft.getInstance().gui.setScreen(new CasinoScreen(n.contains("ルーレット")?2:n.contains("カード")?1:0));return InteractionResult.SUCCESS;}if(n.equals("服屋の店員")){Minecraft.getInstance().gui.setScreen(new FashionScreen(StreetFashion.SHOP));return InteractionResult.SUCCESS;}}if(level.isClientSide()&&entity instanceof CityResident resident&&resident.kind().role()>=10){boolean exchange=resident.kind().role()==11;Minecraft.getInstance().gui.setScreen(new GuildScreen(exchange?GuildServices.EXCHANGE_POS:GuildServices.QUEST_POS,exchange));return InteractionResult.SUCCESS;}if(level.isClientSide()&&entity instanceof VerticalLift lift&&lift.inside(player)){Minecraft.getInstance().gui.setScreen(new LiftScreen(lift));return InteractionResult.SUCCESS;}return InteractionResult.PASS;});
  EntityRendererRegistry.register(NeonFurniture.SEAT,NoopRenderer::new);
  EntityRendererRegistry.register(NeonWard.BIKE,NoopRenderer::new);EntityRendererRegistry.register(NeonWard.CAR,NoopRenderer::new);
  cyberKey=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.neonward.cyberware",GLFW.GLFW_KEY_K,KeyMapping.Category.register(NeonWard.id("cyberware"))));
  ClientTickEvents.END_CLIENT_TICK.register(mc->{while(cyberKey.consumeClick())if(mc.player!=null&&mc.gui.screen()==null)mc.gui.setScreen(new CyberwareScreen(mc.player.blockPosition()));});
  phoneKey=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.neonward.phone",GLFW.GLFW_KEY_P,KeyMapping.Category.register(NeonWard.id("street_tech"))));
  ClientTickEvents.END_CLIENT_TICK.register(mc->{
   if(mc.player!=null&&mc.player.getVehicle() instanceof StreetVehicle v&&!v.bike()){
    float yaw=v.getYRot();if(drivenId!=v.getId()){mc.player.setYRot(yaw);mc.player.setXRot(15);drivenId=v.getId();}
    else mc.player.setYRot(mc.player.getYRot()+net.minecraft.util.Mth.wrapDegrees(yaw-drivenYaw));
    drivenYaw=yaw;
   }else drivenId=-1;
  });
  ScreenEvents.AFTER_INIT.register((mc,screen,sw,sh)->{
   if(screen.getClass().getName().contains("SocialInteractionsScreen")){mc.gui.setScreen(new PhoneScreen());return;}
   if(!(screen instanceof InventoryScreen)&&!(screen instanceof CreativeModeInventoryScreen))return;
   int x=Math.min(sw-55,sw/2+(screen instanceof CreativeModeInventoryScreen?102:94)),y=sh/2+55;
   Screens.getWidgets(screen).add(Button.builder(Component.literal("持つ／しまう"),b->{if(mc.player!=null)mc.player.connection.sendCommand("neonphone hold");mc.gui.setScreen(null);}).bounds(x,y+26,50,18).build());
   ScreenEvents.afterExtract(screen).register((s,g,mx,my,delta)->{
    g.fill(x,y,x+24,y+24,0xffa3afbb);g.fill(x+2,y+2,x+22,y+22,0xff101722);
    if(mc.player!=null)g.item(PhoneEquipment.get(mc.player),x+4,y+4);
    g.text(mc.font,"固定",x+27,y+8,0xffff75bd);
   });
  });
  HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,NeonWard.id("dashboard"),(g,delta)->{
   var mc=Minecraft.getInstance();if(mc.player==null||!(mc.player.getVehicle() instanceof StreetVehicle v)||v.bike()||mc.gui.screen()!=null)return;
   int x=g.guiWidth()/2-78,y=g.guiHeight()-83,k=v.speedKmh();
   g.fill(x,y,x+156,y+46,0xe6091520);g.outline(x,y,156,46,0xff40eada);
   g.text(mc.font,"GRID COUPE / DRIVE",x+8,y+6,0xff8afcf0);
   g.centeredText(mc.font,String.format(java.util.Locale.ROOT,"%03d km/h   [%s]",Math.abs(k),k<0?"R":k>0?"D":"N"),x+78,y+19,0xffffffff);
   g.fill(x+8,y+33,x+148,y+37,0xff254251);g.fill(x+8,y+33,x+8+Math.min(140,Math.abs(k)*140/50),y+37,0xfffa58b6);
  });
  // The equipped item is stored on the player, separately from the nine hotbar slots.
  HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,NeonWard.id("phone_pocket"),(g,delta)->{
   var mc=Minecraft.getInstance();if(mc.player==null||mc.player.isSpectator())return;
   int x=g.guiWidth()/2+121,y=g.guiHeight()-23;
   if(x+23>g.guiWidth()){x=g.guiWidth()-25;y-=28;}
   g.fill(x-1,y-1,x+23,y+23,PhoneEquipment.isHeld(mc.player)?0xffff62af:0xffa3afbb);g.fill(x,y,x+22,y+22,0xff25303c);
   g.fill(x+2,y+2,x+20,y+20,0xff080e18);g.fill(x+1,y+21,x+21,y+22,0xffff62af);
   var phone=PhoneEquipment.get(mc.player);
   if(!phone.isEmpty())g.item(phone,x+3,y+3);
   else g.centeredText(mc.font,"+",x+11,y+7,0xff71818b);
   g.centeredText(mc.font,phoneKey.getTranslatedKeyMessage(),x+11,y-10,0xff8dfbf0);
  });
  ClientTickEvents.END_CLIENT_TICK.register(mc->{while(phoneKey.consumeClick()){long now=System.currentTimeMillis();if(now<phoneInputUntil)continue;phoneInputUntil=now+350;if(mc.player!=null&&mc.gui.screen()==null&&!mc.player.isSpectator()){
   if(mc.gui.screen() instanceof PhoneScreen||mc.gui.screen() instanceof PhoneAppScreen||mc.gui.screen() instanceof FriendsScreen||mc.gui.screen() instanceof CityMapScreen){mc.gui.setScreen(null);continue;}if(!PhoneEquipment.get(mc.player).isEmpty()){if(!PhoneEquipment.isHeld(mc.player))mc.player.connection.sendCommand("neonphone hold");mc.gui.setScreen(new PhoneScreen());}
   else mc.player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("スマホを読み込めていません。再接続してください。"));
  }}});
 }
 static void photo(){Minecraft.getInstance().gui.setScreen(new CameraScreen());}
}

