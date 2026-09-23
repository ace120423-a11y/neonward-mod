package jp.neonward;

import java.nio.file.*;
import java.util.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;

/** Fifteen bounded native-render captures from a COMPLETED isolated SakuraIntegration copy.
 * Never constructs substitute geometry. Shrine screens use real S2C quotes and authentic miko use.
 * The runner supplies the sentinel, copied world, completed artifact and offline account.
 */
public final class SakuraVisualQA implements ClientModInitializer {
 static final String[] SHOTS={"01-town-aerial","02-city-side-gate","03-western-tunnel","04-edo-street","05-shrine-approach","06-shrine-aerial","07-temizu-basin","08-saisen-box","09-miko-counter","10-offering-confirmation","11-temizu-menu","12-amulet-shop","13-travel-charm-hand","14-guard-charm-hand","15-fortune-charm-hand"};
 int stage,age,warm,wait,shots;boolean pending,finished;volatile boolean ready;volatile Throwable failure;volatile Vec3 look;
 static void check(boolean ok,String reason){if(!ok)throw new AssertionError("SAKURA_VISUAL: "+reason);}
 static void guard(net.minecraft.server.MinecraftServer server){
  var root=Path.of(System.getProperty("neonward.sakura.visual.root","__missing__")).toAbsolutePath().normalize();
  check(Files.isRegularFile(root.resolve("SAKURA_VISUAL_ONLY"))&&server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().equals(root.resolve("saves/Sakura-QA")),"isolated copied-world sentinel");
 }
 static ServerPlayer player(Minecraft mc){return mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();}
 static void hideHud(Minecraft mc,boolean hidden){if(mc.gui.hud.isHidden()!=hidden)mc.gui.hud.toggle();}
 void onServer(Minecraft mc,Runnable action){mc.getSingleplayerServer().execute(()->{try{action.run();}catch(Throwable e){failure=e;}});}
 void face(Minecraft mc){
  if(look==null||mc.player==null)return;var delta=look.subtract(mc.player.getEyePosition());float yaw=(float)Math.toDegrees(Math.atan2(-delta.x,delta.z));float pitch=(float)-Math.toDegrees(Math.atan2(delta.y,Math.hypot(delta.x,delta.z)));
  mc.player.setYRot(yaw);mc.player.yRotO=yaw;mc.player.setXRot(pitch);mc.player.xRotO=pitch;
 }
 void camera(ServerPlayer p,Vec3 position,Vec3 target){
  var l=p.level().getServer().overworld();l.getChunkAt(BlockPos.containing(position));
  var delta=target.subtract(position.add(0,p.getEyeHeight(),0));float yaw=(float)Math.toDegrees(Math.atan2(-delta.x,delta.z));float pitch=(float)-Math.toDegrees(Math.atan2(delta.y,Math.hypot(delta.x,delta.z)));
  check(p.teleportTo(l,position.x,position.y,position.z,Set.of(),yaw,pitch,true),"camera teleport "+stage);check(l.noCollision(p),"camera is not inside terrain "+stage);look=target;
 }
 void groundCamera(ServerPlayer p,Vec3 position,Vec3 target){
  var l=p.level().getServer().overworld();l.getChunkAt(BlockPos.containing(position));
  var foot=new net.minecraft.world.phys.AABB(position.x-.29,position.y-.025,position.z-.29,position.x+.29,position.y,position.z+.29);
  check(l.getBlockCollisions(p,foot).iterator().hasNext(),"human-height camera has actual floor support "+stage);
  camera(p,position,target);
 }
 void closeup(ServerPlayer p,BlockPos at){groundCamera(p,Vec3.atBottomCenterOf(at).add(3,0,0),Vec3.atCenterOf(at));}
 void prepare(Minecraft mc,int index){
  mc.gui.setScreen(null);mc.options.keyUse.setDown(false);hideHud(mc,index<9&&index!=6&&index!=7);look=null;
  onServer(mc,()->{
   var p=player(mc);var server=p.level().getServer();guard(server);check(SakuraTown.ready(server),"completed copied town remains ready: "+SakuraTown.status());
   var l=server.overworld();server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"time set noon");server.setWeatherParameters(6000,0,false,false);p.setGameMode(GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();p.setNoGravity(true);
   p.stopUsingItem();p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
   check(StockMarket.ledger!=null,"ledger loaded");StockMarket.ledger.account(p.getStringUUID()).tutorialDone=true;WelcomeTutorial.pending.remove(p.getUUID());
   double centerX=(SakuraTownPlan.MIN_X+SakuraTownPlan.MAX_X)*.5;
   if(index==0){
    for(int x=SakuraTownPlan.MIN_X;x<=SakuraTownPlan.MAX_X;x+=16)for(int z=SakuraTownPlan.MIN_Z;z<=SakuraTownPlan.MAX_Z;z+=16)l.getChunk(x>>4,z>>4);
    camera(p,new Vec3(SakuraTownPlan.MAX_X-8,108,SakuraTownPlan.STREET_Z+52),new Vec3(centerX,69,SakuraTownPlan.STREET_Z));
   }else if(index==1){groundCamera(p,new Vec3(14.5,65,SakuraTown.GATE_Z+.5),new Vec3(-16.5,66.5,SakuraTown.GATE_Z+.5));}
   else if(index==2){groundCamera(p,new Vec3(SakuraTownPlan.ENTRANCE.getX()+35.5,65,SakuraTown.GATE_Z+.5),Vec3.atBottomCenterOf(SakuraTownPlan.ENTRANCE).add(-12,1.5,0));}
   else if(index==3){camera(p,Vec3.atBottomCenterOf(SakuraTownPlan.ENTRANCE).add(-8,0,0),new Vec3(SakuraTownPlan.SHRINE_ENTRANCE.getX()+5,70,SakuraTownPlan.STREET_Z+.5));}
   else if(index==4){groundCamera(p,new Vec3(SakuraTownPlan.SAISEN.getX()+16.5,68,SakuraTownPlan.STREET_Z+.5),Vec3.atBottomCenterOf(SakuraTownPlan.SAISEN).add(-4,3,0));}
   else if(index==5){camera(p,Vec3.atBottomCenterOf(SakuraTownPlan.SAISEN).add(20,29,24),Vec3.atBottomCenterOf(SakuraTownPlan.SAISEN).add(-7,3,0));}
   else if(index==6){groundCamera(p,Vec3.atBottomCenterOf(SakuraTownPlan.CHOZU).add(4,0,0),Vec3.atCenterOf(SakuraTownPlan.CHOZU));}
   else if(index==7){closeup(p,SakuraTownPlan.SAISEN);}
   else if(index==9||index==10){
    int kind=index-9;var at=ShrineServices.anchor(kind);groundCamera(p,Vec3.atBottomCenterOf(at).add(2,0,0),Vec3.atCenterOf(at));
    long cash=StockMarket.ledger.account(p.getStringUUID()).cash;
    var part=ShrineServices.fixtureParts(kind).keySet().stream().filter(pos->!pos.equals(at)).findFirst().orElseThrow();
    check(ShrineServices.useBlock(p,part)==1,"real shrine quote from non-center fixture part "+kind);check(StockMarket.ledger.account(p.getStringUUID()).cash==cash,"opening quote never charges");
   }else{
    var at=SakuraTownPlan.COUNTER;var facing=SakuraTownPlan.STAFF_FACING;
    int distance=index==8?7:3; // Full small-office frontage in scenery; legal counter reach for UI.
    groundCamera(p,Vec3.atBottomCenterOf(at).add(facing.getStepX()*distance,0,facing.getStepZ()*distance),Vec3.atBottomCenterOf(SakuraTownPlan.STAFF).add(0,1.3,0));
    if(index==11){
     var npc=l.getEntity(ShrineServices.STAFF_UUID);check(npc instanceof CityResident,"production auto-spawned miko available");
     var account=StockMarket.ledger.account(p.getStringUUID());account.cash=Math.max(account.cash,5000);check(ShrineServices.use(p,(CityResident)npc)&&ShrineServices.SESSIONS.get(p.getUUID()).kind()==2,"real miko opens real shrine shop payload");
    }else if(index>=12){p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ShrineServices.AMULETS[index-12]));}
   }
   p.getInventory().setChanged();p.containerMenu.broadcastChanges();ready=true;
  });
 }
 void captureChecks(Minecraft mc){
  if(stage>=9&&stage<=11){
   check(mc.gui.screen() instanceof ShrineScreen,"shrine screen still present at capture");var screen=(ShrineScreen)mc.gui.screen();int kind=stage-9;
   check(screen.kind==kind&&screen.heading.equals(ShrineServices.TITLES[kind])&&!screen.pending&&screen.rows.size()==(kind==2?3:1),"actual shrine screen kind/title/rows");
   if(kind==2)for(int i=0;i<3;i++)check(screen.rows.get(i).getAsJsonObject().get("name").getAsString().equals(ShrineServices.NAMES[i])&&screen.rows.get(i).getAsJsonObject().get("icon").getAsInt()==i,"native charm icons match exact catalog");
  }else check(mc.gui.screen()==null,"world shot not obscured by another screen");
  if(stage==6||stage==7){int kind=stage==6?1:0;var parts=ShrineServices.fixtureParts(kind);
   check(parts.entrySet().stream().allMatch(e->mc.level.getBlockState(e.getKey()).equals(e.getValue())),"complete multipart fixture synchronized with correct facing");
   var hit=mc.level.clip(new net.minecraft.world.level.ClipContext(mc.player.getEyePosition(),Vec3.atCenterOf(ShrineServices.anchor(kind)),net.minecraft.world.level.ClipContext.Block.OUTLINE,net.minecraft.world.level.ClipContext.Fluid.NONE,mc.player));
   check(hit.getType()==net.minecraft.world.phys.HitResult.Type.MISS||parts.containsKey(hit.getBlockPos()),"fixture view not occluded by pillar or other structure");
  }
  if(stage==8||stage==11){
   var npc=mc.level.getEntitiesOfClass(CityResident.class,mc.player.getBoundingBox().inflate(16),e->e.getUUID().equals(ShrineServices.STAFF_UUID));
   check(npc.size()==1&&npc.getFirst().position().distanceToSqr(Vec3.atBottomCenterOf(SakuraTownPlan.STAFF))<.1,"exactly one synchronized anchored miko");
  }
  if(stage>=12)check(mc.player.getMainHandItem().is(ShrineServices.AMULETS[stage-12]),"correct native charm in hand");
 }
 @Override public void onInitializeClient(){
  ServerPlayConnectionEvents.JOIN.register((h,s,server)->{guard(server);WelcomeTutorial.pending.remove(h.player.getUUID());if(StockMarket.ledger!=null)StockMarket.ledger.account(h.player.getStringUUID()).tutorialDone=true;});
  ClientTickEvents.END_CLIENT_TICK.register(mc->{
   if(finished||mc.player==null||mc.level==null||mc.getSingleplayerServer()==null)return;
   try{
    if(failure!=null)throw new AssertionError("Sakura server camera/service setup failed",failure);ObjectiveHud.visible=false;
    if(++warm<60){mc.gui.setScreen(null);return;}
    if(stage==SHOTS.length){check(shots==SHOTS.length,"all fifteen captures saved");finished=true;hideHud(mc,false);System.out.println("SAKURA_CLIENT_QA_COMPLETE captures="+shots+" real terrain, miko, three shrine screens and three charm models");mc.stop();return;}
    if(!pending){pending=true;ready=false;age=wait=0;prepare(mc,stage);return;}
    if(!ready){check(++wait<600,"bounded stage setup timeout");return;}
    age++;face(mc);mc.gui.hud.getChat().clearMessages(false);
    if(stage<9||stage>=12)mc.gui.setScreen(null);
    if(age==95){captureChecks(mc);Screenshot.grab(mc,false);shots++;System.out.println("SAKURA_SCREENSHOT "+SHOTS[stage]);}
    if(age==115){stage++;pending=false;ready=false;}
   }catch(Throwable e){finished=true;hideHud(mc,false);e.printStackTrace();System.err.println("SAKURA_CLIENT_QA_FAILED stage="+stage);mc.stop();}
  });
 }
}
