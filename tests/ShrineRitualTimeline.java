package jp.neonward;

import java.util.ArrayDeque;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

/** Test-only real-tick sequencer. Never advances world time, edits ritual timestamps, or blocks the server. */
public final class ShrineRitualTimeline {
    @FunctionalInterface public interface Action { void run() throws Exception; }
    private record Step(int after,Action action) {}
    private static final java.util.List<ShrineRitualTimeline> RUNNING=new java.util.ArrayList<>();
    private static boolean registered;
    private final MinecraftServer server;
    private final ArrayDeque<Step> steps=new ArrayDeque<>();
    private final CompletableFuture<Void> result=new CompletableFuture<>();
    private int origin,deadline;
    private boolean started;
    public ShrineRitualTimeline(MinecraftServer server){this.server=server;}
    /** Delay is relative to the preceding action; zero-delay assertions share that same tick. */
    public ShrineRitualTimeline after(int ticks,Action action){
        if(started||ticks<0)throw new IllegalStateException("Timeline already running or negative delay");
        steps.add(new Step(ticks,action));return this;
    }
    public CompletableFuture<Void> start(int budgetTicks){
        if(started||!server.isSameThread()||budgetTicks<1)throw new IllegalStateException("Server-thread bounded start required");
        started=true;origin=server.getTickCount();deadline=origin+budgetTicks;
        if(!registered){
            registered=true;
            ServerTickEvents.END_SERVER_TICK.register(s->{for(var timeline:java.util.List.copyOf(RUNNING))if(timeline.server==s)timeline.tick();});
            ServerLifecycleEvents.SERVER_STOPPING.register(s->{for(var timeline:java.util.List.copyOf(RUNNING))if(timeline.server==s)timeline.fail(new AssertionError("Server stopped before ritual QA completion"));});
        }
        RUNNING.add(this);tick();return result;
    }
    private void tick(){
        if(result.isDone())return;
        try{
            int now=server.getTickCount();
            if(now>deadline)throw new AssertionError("Ritual QA exceeded bounded real-tick deadline");
            while(!steps.isEmpty()&&now-origin>=steps.peek().after()){
                var step=steps.remove();origin=now;step.action().run();
            }
            if(steps.isEmpty()){RUNNING.remove(this);result.complete(null);}
        }catch(Throwable failure){fail(failure);}
    }
    private void fail(Throwable failure){RUNNING.remove(this);result.completeExceptionally(failure);}
}
