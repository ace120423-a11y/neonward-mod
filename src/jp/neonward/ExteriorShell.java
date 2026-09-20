package jp.neonward;
import com.google.gson.*;
import java.io.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** Replays our original wall geometry in the exploration dimension in bounded batches. */
final class ExteriorShell {
 static JsonArray commands;static long offset;static int cachedIndex=-1;static BlockState state;
 static void reset(){offset=0;cachedIndex=-1;}
 static int count(){load();return commands.size();}
 static void load(){if(commands!=null)return;try(var in=ExteriorShell.class.getResourceAsStream("/neonward_exterior_wall.json")){commands=JsonParser.parseReader(new InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonArray();}catch(Exception ex){throw new IllegalStateException("Exterior wall resource missing",ex);}}
 static void tick(ServerLevel l){
  load();if(NightSpire.progress.exterior>=commands.size())return;
  int budget=8000;
  while(budget>0&&NightSpire.progress.exterior<commands.size()){
   int i=NightSpire.progress.exterior;var a=commands.get(i).getAsJsonArray();int x=a.get(0).getAsInt(),y=a.get(1).getAsInt(),z=a.get(2).getAsInt(),w=a.get(3).getAsInt()-x+1,h=a.get(4).getAsInt()-y+1,d=a.get(5).getAsInt()-z+1;long total=(long)w*h*d;
   if(cachedIndex!=i){try{state=BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK,a.get(6).getAsString(),false).blockState();}catch(Exception ex){throw new IllegalStateException("Invalid original wall block",ex);}cachedIndex=i;}
   while(budget-->0&&offset<total){var pos=new BlockPos(x+(int)(offset%w),y+(int)((offset/w)%h),z+(int)(offset/(w*h)));if(!l.getBlockState(pos).equals(state))l.setBlock(pos,state,2);offset++;}
   if(offset>=total){offset=0;NightSpire.progress.exterior++;if(NightSpire.progress.exterior%100==0||NightSpire.progress.exterior==commands.size())try{NightSpire.save();}catch(Exception ex){System.err.println("[Neon exterior] "+ex);}}
  }
 }
}
