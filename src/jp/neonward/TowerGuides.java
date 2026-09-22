package jp.neonward;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;

/** Separate guides in the corporate guild lobby, clear of counters and doors. */
final class TowerGuides {
 static void install(ServerLevel lobby){
  install(lobby,NeonZones.ENTRY_POS,NeonZones.ENTRY,"NIGHT SPIRE","機関とボスの塔・30階","右クリック：塔の正面");
  install(lobby,SkySpire.CITY_GUIDE,SkySpire.ENTRY,"FROST CITADEL","雪城・アスレチック30階","右クリック：城の正面");
  install(lobby,VolcanicSpire.GUIDE,VolcanicSpire.ENTRY,"VOLCANIC TOWER","火山・討伐とボス30階","右クリック：火山の正面");
 }
 private static void install(ServerLevel l,BlockPos p,Block block,String title,String detail,String action){
  if(l.dimension()!=CompactShops.DIM||!l.hasChunkAt(p))return;
  if(l.isEmptyBlock(p)&&l.isEmptyBlock(p.above())&&!l.isEmptyBlock(p.below()))l.setBlock(p,block.defaultBlockState(),3);
  if(l.getBlockState(p).is(block))NeonZones.sign(l,p.above(),title,detail,action,"入口から攻略開始");
 }
 static void removeStreetGuides(ServerLevel city){
  remove(city,new BlockPos(184,65,154),NeonZones.ENTRY,"NIGHT SPIRE");
  remove(city,new BlockPos(187,65,154),SkySpire.ENTRY,"FROST CITADEL");
 }
 private static void remove(ServerLevel l,BlockPos p,Block block,String title){
  if(!l.hasChunkAt(p))return;
  if(l.getBlockEntity(p.above()) instanceof SignBlockEntity sign&&sign.getFrontText().getMessage(0,false).getString().equals(title))l.setBlock(p.above(),Blocks.AIR.defaultBlockState(),3);
  if(l.getBlockState(p).is(block))l.setBlock(p,Blocks.AIR.defaultBlockState(),3);
 }
}
