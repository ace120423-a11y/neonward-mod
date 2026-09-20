package jp.neonward;
import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
/** One persistent encounter per corridor, independent from boss spawning. */
public final class ClockworkPatrols {
 static void tick(ServerLevel l){
  if(NightSpire.progress==null||l.getDifficulty()==Difficulty.PEACEFUL)return;
  for(var p:l.players())if(p.isAlive()&&!p.isSpectator()&&SpireSite.contains(l,p.blockPosition())){
   int f=DungeonLayout.floor(p.getY()),stage=DungeonLayout.corridor(p.getZ());
   if(stage>=0&&ClockworkPuzzles.state(f).stage>stage)encounter(l,f,stage);
  }
 }
 static void encounter(ServerLevel l,int f,int stage){
  var progress=NightSpire.progress;if(progress.patrols==null)progress.patrols=new HashMap<>();
  var done=progress.patrols.computeIfAbsent(f,k->new HashSet<>());if(done.contains(stage))return;
  String tag="nw_patrol_"+stage;var existing=NightSpire.enemies(l,f).stream().filter(e->e.entityTags().contains(tag)).toList();
  // Entity chunks may already contain a wave after recovery from an interrupted save.
  if(existing.isEmpty()){
   int i=0;for(var pos:DungeonLayout.patrol(f,stage)){
    var spawned=NightSpire.spawn(l,f,pos[0]+.5-SpireSite.X,pos[1]+.5-SpireSite.Z,HostileRoster.ALL[(f+stage*3+i++)%11],false);
    spawned.addTag(tag);
   }
  }
  done.add(stage);try{NightSpire.save();}catch(Exception ex){done.remove(stage);System.err.println("[Clockwork] Patrol save will retry: "+ex);}
 }
}
