package jp.neonward;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public final class CityProtection {
 public static boolean southQuarter(BlockPos p){return p.getX()>=124&&p.getX()<=196&&p.getZ()>=704&&p.getZ()<=803 || p.getX()>=48&&p.getX()<=272&&p.getZ()>=795&&p.getZ()<=968;}
 public static boolean contains(Level l,BlockPos p){return l.dimension()==CompactShops.DIM||l.dimension()==Level.OVERWORLD&&(p.getX()>=-32&&p.getX()<=575&&p.getZ()>=-32&&p.getZ()<=703 || southQuarter(p));}
 public static boolean structure(Level l,BlockPos p){return l.dimension()==CompactShops.DIM||l.dimension()==PrivateFarms.DIM||l.dimension()==PrivateHomes.DIMENSION||contains(l,p)||SpireSite.structure(l,p)||l.dimension()==NeonZones.TOWER;}
 public static boolean decoration(net.minecraft.world.entity.Entity e){String id=net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath();return e instanceof Display||e instanceof ArmorStand||id.equals("item_frame")||id.equals("glow_item_frame")||id.equals("painting");}
 public static void init(){
  AttackBlockCallback.EVENT.register((p,l,hand,pos,face)->{if(HomeBuildingRules.canBreak(l,p,pos))return InteractionResult.PASS;if(l.isClientSide())p.sendOverlayMessage(Component.literal("建物・固定設備は保護されています（右クリックで使用）"));return InteractionResult.FAIL;});
  PlayerBlockBreakEvents.BEFORE.register((l,p,pos,state,be)->HomeBuildingRules.canBreak(l,p,pos));
  UseBlockCallback.EVENT.register((p,l,h,hit)->!PrivateFarms.hoe(l,p,hit.getBlockPos(),p.getItemInHand(h))&&HomeBuildingRules.altersWorld(p.getItemInHand(h))&&(structure(l,hit.getBlockPos())||structure(l,hit.getBlockPos().relative(hit.getDirection())))?InteractionResult.FAIL:InteractionResult.PASS);
  UseItemCallback.EVENT.register((p,l,h)->structure(l,p.blockPosition())&&HomeBuildingRules.altersWorld(p.getItemInHand(h))?InteractionResult.FAIL:InteractionResult.PASS);
  AttackEntityCallback.EVENT.register((p,l,hand,e,hit)->contains(l,e.blockPosition())&&decoration(e)?InteractionResult.FAIL:InteractionResult.PASS);
  ServerTickEvents.END_SERVER_TICK.register(server->{if(server.getTickCount()%40!=0)return;for(var p:server.getPlayerList().getPlayers())for(var e:p.level().getEntities(p,p.getBoundingBox().inflate(64),CityProtection::decoration))if(contains(p.level(),e.blockPosition()))e.setInvulnerable(true);});
 }
}
