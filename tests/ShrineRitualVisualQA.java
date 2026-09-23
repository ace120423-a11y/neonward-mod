package jp.neonward;

import java.nio.file.*;
import java.util.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.client.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;

/** Actual S2C-driven frames only: never calls receive/sample or inserts client animation state.
 * Eight native screenshots from a completed isolated world. Parent reviews the emitted PNGs.
 */
public final class ShrineRitualVisualQA implements ClientModInitializer {
    static final int[][] AGES={{30,52,98,128},{20,60,108,124}};
    static final String[][] LABELS={{"01-wash-ladle-dip","02-wash-left-palm-pour","03-wash-right-palm-pour","04-wash-palms"},{"05-prayer-first-bow","06-prayer-second-bow","07-prayer-first-clap","08-prayer-second-clap"}};
    volatile int stage;int warm,wait,shot,total,stopWait,settle;boolean prepared,confirmed,finished,cancelSent,begun;volatile boolean seen;
    volatile boolean ready,serverEnded;volatile Throwable failure;
    volatile int serverAge=-1,serverKind=-1;volatile boolean serverActive;
    static void check(boolean value,String why){if(!value)throw new AssertionError("SHRINE_RITUAL_VISUAL: "+why);}
    static void guard(net.minecraft.server.MinecraftServer server){
        var root=Path.of(System.getProperty("neonward.shrine.ritual.visual.root","__missing__")).toAbsolutePath().normalize();
        check(Files.isRegularFile(root.resolve("SHRINE_RITUAL_VISUAL_ONLY"))&&server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().equals(root.resolve("saves/Sakura-QA")),"disposable copied-world sentinel");
    }
    static ServerPlayer player(Minecraft mc){return mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();}
    void onServer(Minecraft mc,Runnable task){mc.getSingleplayerServer().execute(()->{try{task.run();}catch(Throwable error){failure=error;}});}
    void prepare(Minecraft mc){
        ready=false;serverEnded=false;seen=false;confirmed=false;cancelSent=false;begun=false;shot=wait=stopWait=settle=0;
        mc.gui.setScreen(null);mc.options.keyUse.setDown(false);
        mc.options.setCameraType(stage==1?CameraType.THIRD_PERSON_FRONT:CameraType.FIRST_PERSON);
        // F1 suppresses the actual first-person hand render pass, not just the overlay.
        if(mc.gui.hud.isHidden())mc.gui.hud.toggle();
        onServer(mc,()->{
            var p=player(mc);var server=p.level().getServer();guard(server);
            check(SakuraTown.ready(server),"completed production town");
            check(net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(p,ShrineRituals.Motion.TYPE),"actual client negotiated ritual packet channel");
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"time set noon");server.setWeatherParameters(6000,0,false,false);
            ShrineRituals.cancel(p);ShrineServices.SESSIONS.remove(p.getUUID());ShrineServices.LIMITS.remove(p.getUUID());
            p.removeAllEffects();p.setGameMode(GameType.SURVIVAL);p.setNoGravity(true);p.setInvulnerable(true);p.setDeltaMovement(Vec3.ZERO);
            p.stopUsingItem();p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            var account=StockMarket.ledger.account(p.getStringUUID());account.cash=10000;account.tutorialDone=true;WelcomeTutorial.pending.remove(p.getUUID());
            int kind=stage==1?0:1;var at=ShrineServices.anchor(kind);var pos=Vec3.atBottomCenterOf(at).add(2,0,0);
            check(p.teleportTo(server.overworld(),pos.x,pos.y,pos.z,Set.of(),90,stage==0?22:0,true),"ritual camera teleport");
            p.setYBodyRot(90);p.yBodyRotO=90;p.setYHeadRot(90);p.yHeadRotO=90;
            check(p.level().noCollision(p)&&ShrineServices.near(p,kind),"unobstructed authentic fixture approach");
            p.getInventory().setChanged();p.containerMenu.broadcastChanges();
            ready=true;
        });
    }
    void begin(Minecraft mc){
        onServer(mc,()->{
            var p=player(mc);int kind=stage==1?0:1;
            check(Math.abs(net.minecraft.util.Mth.wrapDegrees(p.yBodyRot-90))<3,"server body faces fixture before ritual");
            check(ShrineServices.useBlock(p,ShrineServices.anchor(kind).north())==1,"real multipart fixture interaction");
            check(StockMarket.ledger.account(p.getStringUUID()).cash==10000&&!ShrineBlessings.active(p),"no start debit or blessing");
            if(kind==0)check(!ShrineRituals.active(p),"prayer waits for UI confirmation");
        });
    }
    void snapshot(Minecraft mc){
        int requestedStage=stage;
        onServer(mc,()->{
            if(stage!=requestedStage)return;
            var p=player(mc);serverActive=ShrineRituals.active(p);serverKind=ShrineRituals.kind(p);serverAge=ShrineRituals.elapsed(p);
            if(serverActive)check(StockMarket.ledger.account(p.getStringUUID()).cash==10000&&!ShrineBlessings.active(p),"captured ritual still has no early reward");
            else if(seen){
                long cash=StockMarket.ledger.account(p.getStringUUID()).cash;
                check(stage==2?cash==10000&&!p.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION):stage==0?cash==10000&&p.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION):cash==9900&&(p.hasEffect(ShrineBlessings.ATTACK)!=p.hasEffect(ShrineBlessings.DEFENSE)),"real completion/cancellation result before visual stage ends");
                serverEnded=true;
            }
        });
    }
    void capture(Minecraft mc,ShrineRitualClient.Frame frame){
        int target=AGES[stage][shot];check(frame.age()<=target+8,"keyframe not skipped by lag");
        check(frame.kind()==(stage==0?1:0)&&serverActive&&serverKind==frame.kind()&&Math.abs(serverAge-frame.age())<=25,"client/server packet timeline synchronized");
        check(mc.gui.screen()==null,"start acknowledgement closed confirmation screen");
        check(mc.player.getMainHandItem().isEmpty()&&mc.player.getOffhandItem().isEmpty(),"cosmetic ladle/palms never mutate held inventory");
        if(stage==0){
            check(mc.options.getCameraType()==CameraType.FIRST_PERSON,"wash first person");
            check(!mc.gui.hud.isHidden()&&!mc.player.isInvisible(),"first-person render pass enabled");
            check(ShrineRitualMeshes.ladle!=null&&ShrineRitualMeshes.handle!=null,"native ladle draw path actually executed");
            int size=mc.player.getSkin().model()==net.minecraft.world.entity.player.PlayerModelType.SLIM?1:0;
            for(int side=0;side<2;side++)for(int part=0;part<3;part++)check(ShrineRitualMeshes.ARMS[side][size][part]!=null,"both articulated hand meshes actually rendered");
            if(shot==0)check(frame.ladle()==1&&frame.bow()>.1,"ladle dip phase");
            if(shot==1)check(frame.ladle()==1&&frame.pour()>.8,"left palm pour phase");
            if(shot==2)check(frame.ladle()==-1&&frame.pour()>.8,"right palm pour phase");
            if(shot==3)check(frame.join()>.55,"palms phase");
        }else{
            check(mc.options.getCameraType()==CameraType.THIRD_PERSON_FRONT,"prayer third person");
            check(Math.abs(net.minecraft.util.Mth.wrapDegrees(mc.player.yBodyRot-90))<5,"body orientation stayed fixture-facing without QA pose override");
            check(shot<2?frame.bow()>.5:frame.join()>.6,"bow/clap keyframe actually active");
        }
        mc.gui.hud.getChat().clearMessages(false);Screenshot.grab(mc,false);total++;
        System.out.println("SHRINE_RITUAL_SCREENSHOT "+LABELS[stage][shot]+" age="+frame.age()+" serverAge="+serverAge);shot++;
    }
    @Override public void onInitializeClient(){
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->{guard(server);WelcomeTutorial.pending.remove(handler.player.getUUID());if(StockMarket.ledger!=null)StockMarket.ledger.account(handler.player.getStringUUID()).tutorialDone=true;});
        ClientTickEvents.END_CLIENT_TICK.register(mc->{
            if(finished||mc.player==null||mc.level==null||mc.getSingleplayerServer()==null)return;
            try{
                if(failure!=null)throw new AssertionError("Ritual server QA failed",failure);
                ObjectiveHud.visible=false;
                if(++warm<80){mc.gui.setScreen(null);return;}
                if(stage==3){check(total==8,"eight real animation screenshots");finished=true;mc.options.setCameraType(CameraType.FIRST_PERSON);if(mc.gui.hud.isHidden())mc.gui.hud.toggle();System.out.println("SHRINE_RITUAL_CLIENT_QA_COMPLETE captures=8 real packets, no inventory props, completion and stop sync");mc.stop();return;}
                if(!prepared){prepared=true;prepare(mc);return;}
                check(++wait<600,"bounded ritual stage");if(!ready)return;
                mc.player.setYRot(90);mc.player.yRotO=90;mc.player.setXRot(stage==0?22:0);mc.player.xRotO=mc.player.getXRot();
                if(!begun){
                    // Normalize teleport interpolation BEFORE starting, then stop touching body/head pose.
                    mc.player.setYBodyRot(90);mc.player.yBodyRotO=90;mc.player.setYHeadRot(90);mc.player.yHeadRotO=90;
                    if(++settle<40)return;
                    begun=true;begin(mc);return;
                }
                if(stage==1&&!confirmed){
                    if(mc.gui.screen() instanceof ShrineScreen screen&&screen.kind==0){screen.send("offer",0);confirmed=true;}
                    return;
                }
                snapshot(mc);
                var frame=ShrineRitualClient.frame(mc.player.getUUID(),0);
                if(stage==2){
                    if(frame!=null){seen=true;if(frame.age()>=20&&!cancelSent){cancelSent=true;onServer(mc,()->{var p=player(mc);p.setPos(p.position().add(.8,0,0));});}}
                    if(cancelSent){
                        check(++stopWait<40,"cancellation state removed long before 160-tick expiry");
                        if(serverEnded&&frame==null&&!ShrineRitualClient.ACTIVE.containsKey(mc.player.getUUID())){System.out.println("SHRINE_RITUAL_PACKET_CANCEL_PASS active S2C received, movement cancelled, stop S2C cleared before expiry");stage++;}
                    }
                    return;
                }
                if(frame!=null){seen=true;if(shot<4&&frame.age()>=AGES[stage][shot])capture(mc,frame);}
                else if(seen&&serverEnded){
                    check(shot==4,"all requested keyframes before completion");
                    // Completion leaves no stale animation. Stage two separately verifies early stop delivery.
                    if(ShrineRitualClient.ACTIVE.containsKey(mc.player.getUUID())){check(++stopWait<30,"stop packet clears received state");return;}
                    System.out.println("SHRINE_RITUAL_PACKET_STOP_PASS kind="+(stage==0?1:0));stage++;prepared=false;
                }
            }catch(Throwable error){finished=true;error.printStackTrace();System.err.println("SHRINE_RITUAL_CLIENT_QA_FAILED stage="+stage);mc.stop();}
        });
    }
}
