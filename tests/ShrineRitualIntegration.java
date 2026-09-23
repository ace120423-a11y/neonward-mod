package jp.neonward;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Disposable-world real-tick ritual checks. Parent awaits run(); no sleeping or synthetic completion. */
public final class ShrineRitualIntegration {
    private final MinecraftServer server;
    private final List<Probe> players=new ArrayList<>();
    private final java.nio.file.Path ledgerFile=StockMarket.file;
    private final Map<net.minecraft.core.BlockPos,net.minecraft.world.level.block.state.BlockState> changed=new HashMap<>();
    private Probe p,other;
    private int checks,token;
    private net.minecraft.core.Holder<MobEffect> blessing;
    private ShrineRitualIntegration(MinecraftServer server){this.server=server;}
    static final class Probe extends ServerPlayer {
        boolean disconnected;
        Probe(ServerLevel level,com.mojang.authlib.GameProfile profile){super(level.getServer(),level,profile,ClientInformation.createDefault());}
        @Override public boolean hasDisconnected(){return disconnected;}
    }
    private void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError("SHRINE_RITUAL: "+why);}
    private long cash(Probe who){return StockMarket.ledger.account(who.getStringUUID()).cash;}
    private Probe fresh(int kind){
        var profile=new com.mojang.authlib.GameProfile(UUID.randomUUID(),"RitualQA");
        var who=new Probe(server.overworld(),profile);
        who.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),who,net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false)){
            @Override public boolean hasClientLoaded(){return true;}
        };
        players.add(who);who.setGameMode(GameType.SURVIVAL);who.setNoGravity(true);who.setInvulnerable(true);
        who.setPos(Vec3.atBottomCenterOf(ShrineServices.anchor(kind)).add(2,0,0));server.overworld().addNewPlayer(who);
        StockMarket.ledger.account(who.getStringUUID()).cash=10000;return who;
    }
    private int prayer(Probe who){
        ShrineServices.LIMITS.computeIfAbsent(who.getUUID(),id->new ShrineServices.Limits()).open=0;
        check(ShrineServices.open(who,0)==1&&!ShrineRituals.active(who),"prayer requires confirmation");
        int quote=ShrineServices.SESSIONS.get(who.getUUID()).token();
        check(ShrineServices.request(who,"offer",quote,0)==1&&ShrineRituals.kind(who)==0,"confirmed prayer starts");
        return quote;
    }
    private void wash(Probe who){
        who.setPos(Vec3.atBottomCenterOf(SakuraTownPlan.CHOZU).add(2,0,0));
        who.addEffect(new MobEffectInstance(MobEffects.POISON,2000,0));
        check(ShrineServices.useBlock(who,SakuraTownPlan.CHOZU.north())==1&&ShrineRituals.kind(who)==1,"non-center basin click starts without confirmation");
    }
    private void noEarly(Probe who,long expected){
        check(cash(who)==expected&&!ShrineBlessings.active(who)&&!who.hasEffect(MobEffects.REGENERATION),"no premature debit/blessing/regeneration");
    }
    private void cancelled(Probe who){check(!ShrineRituals.active(who),"ritual cancelled");noEarly(who,10000);}
    private void motion(Probe who,int kind,int elapsed,int duration){
        var r=ShrineRituals.ACTIVE.get(who.getUUID());check(r!=null,"active authoritative snapshot");
        var packet=ShrineRituals.packet(r,true);var json=com.google.gson.JsonParser.parseString(packet.json()).getAsJsonObject();
        check(json.get("uuid").getAsString().equals(who.getStringUUID())&&json.get("kind").getAsInt()==kind&&json.get("elapsed").getAsInt()==elapsed&&json.get("duration").getAsInt()==duration&&json.get("active").getAsBoolean(),"authoritative motion packet identity/timing");
        check(!com.google.gson.JsonParser.parseString(ShrineRituals.packet(r,false).json()).getAsJsonObject().get("active").getAsBoolean(),"stop packet clears animation");
    }
    private void combatAndPersistence(){
        var attacker=fresh(0);var defender=fresh(0);defender.setInvulnerable(false);
        check(Cyberware.defense(defender)==0&&defender.getArmorValue()==0&&defender.getMainHandItem().isEmpty()&&defender.getOffhandItem().isEmpty(),"defender has no equipment/cyberware reduction");
        var cow=new net.minecraft.world.entity.animal.cow.Cow(net.minecraft.world.entity.EntityTypes.COW,server.overworld());
        cow.setPos(attacker.position());
        var arrow=new net.minecraft.world.entity.projectile.arrow.Arrow(net.minecraft.world.entity.EntityTypes.ARROW,server.overworld());arrow.setOwner(attacker);
        try{
            for(var source:List.of(attacker.damageSources().playerAttack(attacker),attacker.damageSources().arrow(arrow,attacker))){
                attacker.removeAllEffects();cow.invulnerableTime=0;cow.setHealth(cow.getMaxHealth());float before=cow.getHealth();
                check(cow.hurtServer(server.overworld(),source,4),"baseline player-owned damage reaches actual hurtServer");float baseline=before-cow.getHealth();
                attacker.addEffect(new MobEffectInstance(ShrineBlessings.ATTACK,6000,0));cow.invulnerableTime=0;cow.setHealth(cow.getMaxHealth());before=cow.getHealth();
                check(cow.hurtServer(server.overworld(),source,4),"blessed player-owned damage reaches actual hurtServer");float blessed=before-cow.getHealth();
                check(Math.abs(baseline-4)<.0001&&Math.abs(blessed-baseline*1.1)<.0001,"runtime melee/projectile attack injection gives exactly +10%: "+baseline+" -> "+blessed);
            }
            attacker.removeAllEffects();defender.removeAllEffects();defender.invulnerableTime=0;defender.setHealth(20);float before=defender.getHealth();
            check(defender.hurtServer(server.overworld(),defender.damageSources().generic(),4),"baseline unequipped incoming damage");float baseline=before-defender.getHealth();
            defender.addEffect(new MobEffectInstance(ShrineBlessings.DEFENSE,6000,0));defender.invulnerableTime=0;defender.setHealth(20);before=defender.getHealth();
            check(defender.hurtServer(server.overworld(),defender.damageSources().generic(),4),"blessed unequipped incoming damage");float reduced=before-defender.getHealth();
            check(Math.abs(baseline-4)<.0001&&Math.abs(reduced-baseline*.9)<.0001,"runtime defense injection gives exactly -10%: "+baseline+" -> "+reduced);
            var ops=net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE,server.registryAccess());
            for(var type:List.of(ShrineBlessings.ATTACK,ShrineBlessings.DEFENSE)){
                var effect=new MobEffectInstance(type,4321,0);
                var tag=MobEffectInstance.CODEC.encodeStart(ops,effect).getOrThrow();
                var restored=MobEffectInstance.CODEC.parse(ops,tag).getOrThrow();
                check(restored.getEffect().equals(type)&&restored.getDuration()==4321&&restored.getAmplifier()==0,"registered blessing NBT roundtrip preserves identity/duration");
            }
            System.out.println("SHRINE_BLESSING_COMBAT_PASS actual melee4->4.4 owned-arrow4->4.4 defense4->3.6; both saved effect codecs retain4321ticks");
        }finally{cow.discard();arrow.discard();defender.setInvulnerable(true);}
    }
    private void cleanup() throws Exception {
        StockMarket.file=ledgerFile;
        changed.forEach((at,state)->server.overworld().setBlock(at,state,3));changed.clear();
        for(var who:players){ShrineRituals.cancel(who);ShrineServices.SESSIONS.remove(who.getUUID());ShrineServices.LIMITS.remove(who.getUUID());who.level().removePlayerImmediately(who,Entity.RemovalReason.DISCARDED);StockMarket.ledger.accounts.remove(who.getStringUUID());}
        StockMarket.save();
    }
    public static CompletableFuture<Integer> run(MinecraftServer server){
        var qa=new ShrineRitualIntegration(server);return qa.schedule();
    }
    private CompletableFuture<Integer> schedule(){
        var timeline=new ShrineRitualTimeline(server);
        timeline.after(0,()->{combatAndPersistence();p=fresh(0);token=prayer(p);noEarly(p,10000);motion(p,0,0,200);check(ShrineServices.request(p,"offer",token,0)==0,"confirmation replay rejected while running");})
        .after(199,()->{check(ShrineRituals.active(p),"prayer active through tick 199");noEarly(p,10000);motion(p,0,199,200);})
        .after(1,()->{
            check(!ShrineRituals.active(p)&&cash(p)==9900&&ShrineBlessings.active(p),"prayer completes at tick 200 exactly once");
            check(p.hasEffect(ShrineBlessings.ATTACK)!=p.hasEffect(ShrineBlessings.DEFENSE),"exactly one random attack/defense blessing");
            blessing=p.hasEffect(ShrineBlessings.ATTACK)?ShrineBlessings.ATTACK:ShrineBlessings.DEFENSE;
            check(p.getEffect(blessing).getDuration()==6000&&p.getEffect(blessing).getAmplifier()==0,"blessing lasts exactly 6000 ticks");
            check(!ShrineBlessings.grant(p)&&p.getEffect(blessing).getDuration()==6000,"grant helper cannot reroll/refresh active blessing");
            check(StockMarket.JSON.fromJson(java.nio.file.Files.readString(ledgerFile),MarketLedger.class).account(p.getStringUUID()).cash==9900,"completion debit durable");
            check(ShrineServices.request(p,"offer",token,0)==0,"completed confirmation cannot replay");
            ShrineServicesIntegration.limits(p).open=0;check(ShrineServices.open(p,0)==1,"fresh quote while blessed");
            check(ShrineServices.request(p,"offer",ShrineServices.SESSIONS.get(p.getUUID()).token(),0)==0&&cash(p)==9900,"active blessing cannot reroll or stack");
            check(p.hasEffect(blessing)&&p.getEffect(blessing).getDuration()==6000,"denied quote preserves chosen blessing and duration");
        })
        .after(2,()->{check(cash(p)==9900&&ShrineBlessings.active(p),"completion not repeated on later ticks");p=fresh(1);wash(p);noEarly(p,10000);motion(p,1,0,160);})
        .after(159,()->{check(ShrineRituals.active(p)&&p.hasEffect(MobEffects.POISON),"wash does not cleanse at tick 159");noEarly(p,10000);motion(p,1,159,160);})
        .after(1,()->{
            check(!ShrineRituals.active(p)&&!p.hasEffect(MobEffects.POISON)&&cash(p)==10000,"wash free/cleanses only at completion");
            var regen=p.getEffect(MobEffects.REGENERATION);check(regen!=null&&regen.getDuration()==100&&regen.getAmplifier()==0,"wash completion grants regeneration100");
            check(ShrineServices.useBlock(p,SakuraTownPlan.CHOZU)==0,"wash replay/cooldown denied");
            p=fresh(0);token=prayer(p);p.setPos(p.position().add(.751,0,0));
        })
        .after(1,()->{cancelled(p);check(ShrineServices.request(p,"offer",token,0)==0,"cancelled quote cannot replay");p=fresh(0);prayer(p);p.setPos(p.position().add(.74,0,0));})
        .after(1,()->{check(ShrineRituals.active(p),"movement below threshold remains active");ShrineRituals.cancel(p);cancelled(p);p=fresh(0);prayer(p);p.setInvulnerable(false);check(p.hurtServer(p.level(),p.damageSources().generic(),1),"real damage applied");})
        .after(1,()->{cancelled(p);p=fresh(0);prayer(p);p.setHealth(0);})
        .after(1,()->{cancelled(p);p=fresh(0);prayer(p);p.disconnected=true;})
        .after(1,()->{cancelled(p);p=fresh(0);prayer(p);var target=server.getLevel(Level.NETHER);check(target!=null&&p.teleportTo(target,.5,80,.5,Set.of(),0,0,true),"real dimension change");})
        .after(1,()->{cancelled(p);p=fresh(0);prayer(p);var at=SakuraTownPlan.SAISEN.north();changed.put(at,p.level().getBlockState(at));p.level().setBlock(at,Blocks.AIR.defaultBlockState(),3);})
        .after(1,()->{cancelled(p);changed.forEach((at,state)->server.overworld().setBlock(at,state,3));changed.clear();p=fresh(0);prayer(p);other=fresh(1);wash(other);})
        .after(159,()->{noEarly(p,10000);noEarly(other,10000);check(ShrineRituals.active(p)&&ShrineRituals.active(other),"concurrent players remain independent");})
        .after(1,()->{check(!ShrineRituals.active(other)&&other.hasEffect(MobEffects.REGENERATION)&&ShrineRituals.active(p),"wash completes independently");noEarly(p,10000);})
        .after(40,()->{check(cash(p)==9900&&ShrineBlessings.active(p)&&cash(other)==10000,"concurrent prayer completion independent");p=fresh(0);prayer(p);})
        .after(199,()->{noEarly(p,10000);StockMarket.file=ledgerFile.getParent();})
        .after(1,()->{StockMarket.file=ledgerFile;cancelled(p);check(ShrineServicesIntegration.limits(p).offer==0,"failed completion save grants no cooldown");token=prayer(p);})
        .after(199,()->{noEarly(p,10000);StockMarket.ledger.account(p.getStringUUID()).cash=99;})
        .after(1,()->{
            check(!ShrineRituals.active(p)&&cash(p)==99&&!ShrineBlessings.active(p),"completion rechecks funds");
            p=fresh(0);check(ShrineBlessings.grant(p),"fresh blessing grant");
            blessing=p.hasEffect(ShrineBlessings.ATTACK)?ShrineBlessings.ATTACK:ShrineBlessings.DEFENSE;
            check(p.getEffect(blessing).getDuration()==6000,"registered effect grant duration");
            // Controlled near-expiry fixture. Probe connections are not registered with the network
            // listener, which normally invokes ServerPlayer.doTick -> Player.tick -> tickEffects.
            // The two following real server ticks explicitly supply that missing engine call.
            // Do not mutate duration again, fast-forward world time, or remove the effect to pass.
            p.removeEffect(blessing);p.addEffect(new MobEffectInstance(blessing,2,0));
        })
        .after(1,()->{
            check(p.getEffect(blessing)!=null&&p.getEffect(blessing).getDuration()==2,"synthetic connection does not invoke player engine ticks");
            p.doTick();
            check(p.getEffect(blessing)!=null&&p.getEffect(blessing).getDuration()==1,"first genuine player engine tick decrements duration2->1");
            check(ShrineBlessings.active(p)&&!ShrineBlessings.grant(p),"one-tick remainder still prevents reroll");
        })
        .after(1,()->{
            p.doTick();
            check(!ShrineBlessings.active(p),"second genuine player engine tick removes expired effect");
            check(ShrineBlessings.grant(p),"natural effect expiration permits later grant");
            System.out.println("SHRINE_BLESSING_EXPIRY_PASS engine doTick duration2->1->removed, regrant allowed");
        });
        var result=new CompletableFuture<Integer>();
        timeline.start(1600).whenComplete((unused,failure)->{
            try{cleanup();}catch(Throwable cleanupFailure){if(failure==null)failure=cleanupFailure;else failure.addSuppressed(cleanupFailure);}
            if(failure!=null)result.completeExceptionally(failure);
            else{System.out.println("SHRINE_RITUAL_SERVER_PASS checks="+checks+" real-tick staging/cancellation/concurrency/replay/durable completion");result.complete(checks);}
        });
        return result;
    }
}
