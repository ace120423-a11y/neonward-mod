package jp.neonward;
import net.minecraft.world.level.block.*;
/** Separate horizontal rooms: only occupied rooms tick, no 30-storey entity stack. */
final class VolcanoLayout {
 static final Block GLOW_GLASS=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("orange_stained_glass"));
 static final int WIDTH=65,DEPTH=105,HEIGHT=18,TOTAL=WIDTH*DEPTH*HEIGHT;
 static int x(int floor){return (floor-1)*128;}
 static Block block(int x,int y,int z,int f){
  if(y==0){if((x%8==4&&z%8==4)||((x<4||x>60)&&(z%13<3)))return Blocks.SHROOMLIGHT;return ((x+z+f)%17==0)?Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS:Blocks.POLISHED_BLACKSTONE_BRICKS;}
  if(x==0||x==64||z==0||z==104||y==17)return y%5==0?Blocks.CHISELED_POLISHED_BLACKSTONE:Blocks.BASALT;
  if(z==56)return x>=28&&x<=36&&y<=7?Blocks.OBSIDIAN:Blocks.POLISHED_BLACKSTONE_BRICKS;
  // Low relief pillars stay at the outer edges, leaving combat space unobstructed.
  if((x<=3||x>=61)&&z%16<=2)return y==8||y==14?Blocks.SHROOMLIGHT:Blocks.POLISHED_BASALT;
  if(y==1&&(z==10||z==59)&&x%8==0)return Blocks.SHROOMLIGHT;
  if(y==16&&((x+f)%12==0)&&z%16<3)return Blocks.SHROOMLIGHT;
  if(y==16&&((x*7+z*11+f)%97==0))return Blocks.POINTED_DRIPSTONE;
  if(y==1&&z>60&&(x<3||x>61))return GLOW_GLASS;
  return null;
 }
 static final int FRONT_TOTAL=97*97*81;
 static Block front(int x,int y,int z){
  int dx=x-48,dz=z-48;double r=Math.hypot(dx,dz);
  if(y==0)return Math.abs(dx)<5&&z<40?Blocks.POLISHED_BLACKSTONE_BRICKS:Blocks.BASALT;
  // A passable bridge and gate carved into a jagged volcanic fortress.
  if(Math.abs(dx)<=5&&z<40&&y<12)return null;
  double rim=36-y*.35+Math.sin(Math.atan2(dz,dx)*9)*3;
  if(y<65&&r>rim-3&&r<rim+1){if((x+z)%13==0)return Blocks.SHROOMLIGHT;return y%9==0?Blocks.BLACKSTONE:Blocks.BASALT;}
  if(z==32&&Math.abs(dx)<=17&&y<24){if(Math.abs(dx)<=5&&y<12)return null;return y%7==0?Blocks.CHISELED_POLISHED_BLACKSTONE:Blocks.POLISHED_BLACKSTONE_BRICKS;}
  if((Math.abs(dx)==19||Math.abs(dx)==30)&&z>30&&z<70&&z%18<4&&y<42+(z%7))return y%10==0?Blocks.SHROOMLIGHT:Blocks.POLISHED_BASALT;
  if(y==1&&r>24&&r<38&&(x*3+z)%17<3)return GLOW_GLASS;
  if(y==55&&r<15)return Blocks.SHROOMLIGHT;
  return null;
 }
}
