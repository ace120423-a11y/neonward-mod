package jp.neonward;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
/** Vast vaulted galleries; the clear navigation plane never contains decorative obstacles. */
public final class ClockworkInterior {
 static final Block COPPER=b("waxed_cut_copper"),OLD=b("waxed_weathered_cut_copper");
 static Block b(String n){return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(n));}
 static BlockState of(Block b){return b.defaultBlockState();}
 static BlockState state(boolean[][] open,int x,int y,int z,int floor){
  boolean edge=x==0||z==0||x==116||z==180;boolean wall=!open[x][z];
  if((z==24||z==74||z==124)&&x>=54&&x<=64&&y>0&&y<23)return of(COPPER);
  int corridor=DungeonLayout.corridor(z);
  if(corridor>=0&&!wall){
   if(y==0)return of((x+z)%7==0?Blocks.OCHRE_FROGLIGHT:Blocks.DEEPSLATE_TILES);
   if(y==11)return of((x+z)%6==0?Blocks.OCHRE_FROGLIGHT:COPPER);
   if(y>0&&y<11)return of(Blocks.AIR);
  }
  if(y==0&&x%15==9&&z%15==9)return of(Blocks.OCHRE_FROGLIGHT);
  if(y==0)return of(x%15==0||z%15==0?COPPER:(x+z)%4==0?Blocks.POLISHED_BLACKSTONE:Blocks.DEEPSLATE_TILES);
  if(y==23)return of(x%15>=6&&x%15<=12&&z%15>=6&&z%15<=12?Blocks.OCHRE_FROGLIGHT:Blocks.DARK_OAK_PLANKS);
  if(edge||wall){
   if(y==1||y==17||y==22)return of(COPPER);
   if(y>=5&&y<=14&&((x%15==0&&z%15>=5&&z%15<=12)||(z%15==0&&x%15>=5&&x%15<=12)))return of(b("brown_stained_glass"));
   if(y%6==0&&(x+z)%7==0)return of(Blocks.OCHRE_FROGLIGHT);
   return of((x%15<2||z%15<2)?Blocks.POLISHED_BLACKSTONE:OLD);
  }
  if(y>=1&&y<23&&(Math.abs(x-30)<=1||Math.abs(x-86)<=1)&&Math.abs(z%50-15)<=1)return of(y%5==0?COPPER:Blocks.POLISHED_BLACKSTONE);
  int boilerX=x<58?15:101;int bx=x-boilerX,bz=z%50-15;
  if(y>=1&&y<=7&&bx*bx+bz*bz<=30)return of(y==1||y==7?COPPER:((bx*bx+bz*bz>=16)?OLD:Blocks.OCHRE_FROGLIGHT));
  if(y>=19&&(x%15==0||z%15==0))return of(y==21?Blocks.OCHRE_FROGLIGHT:b("waxed_exposed_cut_copper"));
  if(y==18&&(x%15==1||z%15==1))return of(Blocks.IRON_BARS);
  // Suspended boiler drums and pipe bundles emphasize the height without blocking players.
  if(y>=14&&y<=17&&x%30>=7&&x%30<=11&&z%30>=7&&z%30<=11)return of(y==14||y==17?COPPER:Blocks.POLISHED_BLACKSTONE);
  return of(Blocks.AIR);
 }
}
