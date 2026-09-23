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

/** Six native terrain captures. Reads installed migration geometry; never creates substitute blocks. */
public final class SakuraAccessVisualQA implements ClientModInitializer {
    static final String[] SHOTS={"01-city-road-toward-gate","02-city-route-overview","03-district-east-gate-wall","04-entire-perimeter-aerial","05-raised-west-shrine-wall","06-northwest-wall-corner"};
    static final Vec3[] CAMERAS={new Vec3(25.5,65,368.5),new Vec3(80.5,145,415.5),new Vec3(-75.5,65,368.5),new Vec3(-140,240,380),new Vec3(-204.5,74,403.5),new Vec3(-207.5,74,306.5)};
    static final Vec3[] LOOKS={new Vec3(-10,67,368.5),new Vec3(80,64,375),new Vec3(-63.5,68.5,368.5),new Vec3(-140,65,380.1),new Vec3(-216,70,368),new Vec3(-216,68,296)};
    int stage,warm,age,wait,shots;boolean pending,done;volatile boolean ready;volatile Throwable failure;
    static void check(boolean ok,String why){if(!ok)throw new AssertionError("SAKURA_ACCESS_VISUAL: "+why);}
    static void guard(net.minecraft.server.MinecraftServer server){
        var root=Path.of(System.getProperty("neonward.sakura.access.visual.root","__missing__")).toAbsolutePath().normalize();
        check(Files.isRegularFile(root.resolve("SAKURA_ACCESS_VISUAL_ONLY"))&&server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().equals(root.resolve("saves/Sakura-Access-QA")),"disposable copied-world sentinel");
    }
    static ServerPlayer player(Minecraft mc){return mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();}
    static void validateInstalled(ServerPlayer p){
        var l=p.level();var all=new LinkedHashMap<>(SakuraBoundaryPlan.build());all.putAll(SakuraCityRoadPlan.build());
        var loaded=new HashSet<net.minecraft.world.level.ChunkPos>();
        for(var entry:all.entrySet()){
            var chunk=new net.minecraft.world.level.ChunkPos(entry.getKey().getX()>>4,entry.getKey().getZ()>>4);if(loaded.add(chunk))l.getChunk(chunk.x(),chunk.z());
            check(l.getBlockState(entry.getKey()).equals(entry.getValue()),"actual installed wall/road state "+entry.getKey());
        }
        for(int z=366;z<=370;z++)for(int y=65;y<=68;y++)check(l.getBlockState(new BlockPos(-64,y,z)).isAir(),"five-wide gate opening remains clear");
        System.out.println("SAKURA_ACCESS_VISUAL_GEOMETRY_PASS actual installed cells="+all.size());
    }
    void prepare(Minecraft mc){
        ready=false;mc.gui.setScreen(null);mc.options.keyUse.setDown(false);mc.options.setCameraType(CameraType.FIRST_PERSON);
        if(!mc.gui.hud.isHidden())mc.gui.hud.toggle(); // Architecture-only captures; no hand-animation tests here.
        int index=stage;
        mc.getSingleplayerServer().execute(()->{try{
            var p=player(mc);var server=p.level().getServer();guard(server);
            check(SakuraTown.ready(server)&&SakuraAccessUpgrade.ready(server),"completed town/access migration, no QA construction");
            if(index==0)validateInstalled(p);
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"time set noon");server.setWeatherParameters(6000,0,false,false);
            p.setGameMode(GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);
            p.stopUsingItem();p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            WelcomeTutorial.pending.remove(p.getUUID());StockMarket.ledger.account(p.getStringUUID()).tutorialDone=true;
            var pos=CAMERAS[index];var target=LOOKS[index];var level=server.overworld();level.getChunkAt(BlockPos.containing(pos));
            if(index==0||index==2){var foot=new net.minecraft.world.phys.AABB(pos.x-.29,pos.y-.025,pos.z-.29,pos.x+.29,pos.y,pos.z+.29);check(level.getBlockCollisions(p,foot).iterator().hasNext(),"human-height view has real floor support");}
            var delta=target.subtract(pos.add(0,p.getEyeHeight(),0));float yaw=(float)Math.toDegrees(Math.atan2(-delta.x,delta.z)),pitch=(float)-Math.toDegrees(Math.atan2(delta.y,Math.hypot(delta.x,delta.z)));
            check(p.teleportTo(level,pos.x,pos.y,pos.z,Set.of(),yaw,pitch,true)&&level.noCollision(p),"camera outside actual terrain "+index);
            ready=true;
        }catch(Throwable e){failure=e;}});
    }
    void capture(Minecraft mc){
        check(mc.gui.screen()==null,"architecture not obscured by UI");
        check(mc.player.position().distanceToSqr(CAMERAS[stage])<.5,"synchronized intended camera");
        if(stage==0)check(mc.level.getBlockState(new BlockPos(17,64,368)).equals(SakuraCityRoadPlan.build().get(new BlockPos(17,64,368))),"new gate arrow rendered from synchronized world");
        if(stage==2)for(int z:new int[]{365,371})check(mc.level.getBlockState(new BlockPos(-64,68,z)).equals(SakuraBoundaryPlan.build().get(new BlockPos(-64,68,z))),"gate wall joins synchronized");
        if(stage==3)for(int x:new int[]{-216,-64})for(int z:new int[]{296,464})check(mc.level.getBlockState(new BlockPos(x,68,z)).equals(SakuraBoundaryPlan.build().get(new BlockPos(x,68,z))),"all perimeter corners loaded for overview");
        if(stage==4)check(mc.level.getBlockState(new BlockPos(-216,71,368)).equals(SakuraBoundaryPlan.build().get(new BlockPos(-216,71,368))),"raised west coping synchronized");
        mc.gui.hud.getChat().clearMessages(false);Screenshot.grab(mc,false);shots++;System.out.println("SAKURA_ACCESS_SCREENSHOT "+SHOTS[stage]);
    }
    @Override public void onInitializeClient(){
        ServerPlayConnectionEvents.JOIN.register((h,s,server)->{guard(server);WelcomeTutorial.pending.remove(h.player.getUUID());if(StockMarket.ledger!=null)StockMarket.ledger.account(h.player.getStringUUID()).tutorialDone=true;});
        ClientTickEvents.END_CLIENT_TICK.register(mc->{
            if(done||mc.player==null||mc.level==null||mc.getSingleplayerServer()==null)return;
            try{
                if(failure!=null)throw new AssertionError("Access geometry/camera setup failed",failure);ObjectiveHud.visible=false;
                if(++warm<80){mc.gui.setScreen(null);return;}
                if(stage==SHOTS.length){check(shots==6,"all six native screenshots");done=true;if(mc.gui.hud.isHidden())mc.gui.hud.toggle();System.out.println("SAKURA_ACCESS_CLIENT_QA_COMPLETE captures=6 installed roads/perimeter, no substitute geometry");mc.stop();return;}
                if(!pending){pending=true;age=wait=0;prepare(mc);return;}
                if(!ready){check(++wait<400,"bounded setup");return;}
                mc.gui.setScreen(null);var delta=LOOKS[stage].subtract(mc.player.getEyePosition());float yaw=(float)Math.toDegrees(Math.atan2(-delta.x,delta.z)),pitch=(float)-Math.toDegrees(Math.atan2(delta.y,Math.hypot(delta.x,delta.z)));
                mc.player.setYRot(yaw);mc.player.yRotO=yaw;mc.player.setXRot(pitch);mc.player.xRotO=pitch;
                if(++age==110)capture(mc);
                if(age==135){stage++;pending=false;ready=false;}
            }catch(Throwable e){done=true;e.printStackTrace();System.err.println("SAKURA_ACCESS_CLIENT_QA_FAILED stage="+stage);mc.stop();}
        });
    }
}
