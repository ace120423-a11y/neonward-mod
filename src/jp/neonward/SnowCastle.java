package jp.neonward;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Castle geometry stays within the original second-tower footprint. No entities or ticking effects. */
final class SnowCastle {
 static Block exterior(int x,int y,int z){
  // Local coordinates 0..24, height 0..120. Four turrets and a central keep.
  if(y==0)return Blocks.STONE_BRICKS;
  for(int cx:new int[]{3,21})for(int cz:new int[]{3,21}){
   int r=Math.max(Math.abs(x-cx),Math.abs(z-cz));
   if(r<=3){
    int height=cz==21?48:34;
    if(y<=height){
     if(y==height)return Blocks.SNOW_BLOCK;
     if(r==3)return y%10>=4&&y%10<=6&&(x==cx||z==cz)?Blocks.BLUE_ICE:Blocks.QUARTZ_BRICKS;
     if(y%10==0)return Blocks.SMOOTH_QUARTZ;
    }
    int roof=y-height;
    if(roof>0&&roof<=12&&r<=3-(roof-1)/3)return roof%3==0?Blocks.SNOW_BLOCK:Blocks.PACKED_ICE;
   }
  }
  if(x>=7&&x<=17&&z>=10&&z<=22){
   if(y<=57&&(x==7||x==17||z==10||z==22)){
    if(y%12>=4&&y%12<=8&&(x==12||z==16))return Blocks.BLUE_ICE;
    return y%12==0?Blocks.CHISELED_QUARTZ_BLOCK:Blocks.QUARTZ_BRICKS;
   }
   if(y==57)return Blocks.SNOW_BLOCK;
   int roof=y-57;
   if(roof>0&&roof<=18&&Math.abs(x-12)<=5-(roof-1)/3)return roof%3==0?Blocks.SNOW_BLOCK:Blocks.PACKED_ICE;
  }
  boolean wall=x==0||x==24||z==0||z==24;
  if(wall&&y<=15){
   // Walk-through gate, with an ice arch above it.
   if(z==0&&Math.abs(x-12)<=2&&y<=7)return null;
   if(z==0&&Math.abs(x-12)<=3&&y==8)return Blocks.BLUE_ICE;
   if(y<12)return y==1||y==11?Blocks.STONE_BRICKS:Blocks.QUARTZ_BRICKS;
   if(y==12)return Blocks.SNOW_BLOCK;
   if((x+z)%4<2)return y==15?Blocks.SNOW_BLOCK:Blocks.QUARTZ_BRICKS;
  }
  if(y==1)return Math.abs(x-12)<=2&&z<10?Blocks.POLISHED_DIORITE:Blocks.SNOW_BLOCK;
  return null;
 }
 static Block interior(int x,int y,int z,int f){
  if(z>=152&&z<=179&&x>=1&&x<=115){
   if(y==0)return (x+z)%12==0?Blocks.SEA_LANTERN:(x%8<2?Blocks.BLUE_ICE:Blocks.POLISHED_DIORITE);
   if(x==1||x==115||z==179){
    if(y==11)return Blocks.SNOW_BLOCK;
    return y>=3&&y<=8&&(x%12<3||z%12<3)?Blocks.BLUE_ICE:Blocks.QUARTZ_BRICKS;
   }
   // Buttresses and a raised snowy cornice, outside the battle space.
   if((x==2||x==114)&&z%6==0)return y==10?Blocks.SEA_LANTERN:Blocks.CHISELED_QUARTZ_BLOCK;
  }
  if(z>=4&&z<152&&x>=48&&x<=70){
   if(y==0)return (x+z)%9==0?Blocks.BLUE_ICE:Blocks.SNOW_BLOCK;
   if(x==48||x==70)return y>=3&&y<=8&&z%8<3?Blocks.BLUE_ICE:(y==10?Blocks.SNOW_BLOCK:Blocks.QUARTZ_BRICKS);
   if((x==49||x==69)&&z%8==0)return y==9?Blocks.SEA_LANTERN:Blocks.CHISELED_QUARTZ_BLOCK;
   if(y==11&&(z%8==0||x<=51||x>=67))return Blocks.PACKED_ICE;
  }
  return null;
 }
}
