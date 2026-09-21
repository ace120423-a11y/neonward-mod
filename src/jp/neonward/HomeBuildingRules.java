package jp.neonward;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;

public final class HomeBuildingRules {
 static final Set<String> PLUMBING=Set.of("shower_panel","tech_toilet","neon_bath","suite_bath");
 static Set<BlockPos> fixed;
 public static boolean movable(Block block){var key=BuiltInRegistries.BLOCK.getKey(block);String id=key.getPath();return key.getNamespace().equals("webdisplays")&&id.equals("screen")||key.getNamespace().equals("minecraft")&&Set.of("enchanting_table","crafting_table","furnace","blast_furnace","smoker","anvil","chipped_anvil","damaged_anvil","smithing_table").contains(id)||NeonFurniture.BLOCKS.get(id)==block&&!PLUMBING.contains(id)&&!id.equals("neon_nameplate");}
 public static boolean editablePosition(Level l,Player p,BlockPos pos){
  if(WestLand.area(l,pos))return WestLand.edit(l,p,pos);
  if(l.dimension()==Level.OVERWORLD)return CityApartments.editable(l,p,pos);if(l.dimension()!=PrivateHomes.DIMENSION)return false;
  int slot;
  if(p instanceof ServerPlayer sp){if(!PrivateHomes.insidePosition(sp))return false;slot=Math.floorDiv((int)Math.floor(sp.getX()),1024)+Math.floorDiv((int)Math.floor(sp.getZ()),1024)*512;}
  else{if(p==null)return false;slot=Math.floorDiv(p.blockPosition().getX(),1024)+Math.floorDiv(p.blockPosition().getZ(),1024)*512;}
  var o=PrivateHomes.origin(slot);var r=pos.subtract(o);
  if(r.getX()<2||r.getX()>26||r.getZ()<2||r.getZ()>13||r.getY()<65||r.getY()>70)return false;
  if(fixed==null){fixed=new HashSet<>();var palette=PrivateHomes.template.getAsJsonArray("palette");for(var e:PrivateHomes.template.getAsJsonArray("blocks")){var a=e.getAsJsonArray();var id=palette.get(a.get(3).getAsInt()).getAsJsonObject().get("name").getAsString();var b=BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.parse(id));if(!movable(b))fixed.add(new BlockPos(a.get(0).getAsInt(),a.get(1).getAsInt(),a.get(2).getAsInt()));}}
  return !fixed.contains(r);
 }
 public static boolean canBreak(Level l,Player p,BlockPos pos){if(WestLand.area(l,pos))return WestLand.edit(l,p,pos);return PrivateFarms.edit(l,p,pos)&&pos.getY()>=65||!CityProtection.structure(l,pos)||editablePosition(l,p,pos)&&movable(l.getBlockState(pos).getBlock());}
 public static boolean canPlace(BlockPlaceContext context,Block block){
  var l=context.getLevel();var pos=context.getClickedPos();
  if(WestLand.area(l,pos)){
   if(!WestLand.edit(l,context.getPlayer(),pos))return false;
   if(block instanceof net.minecraft.world.level.block.BedBlock&&!WestLand.edit(l,context.getPlayer(),pos.relative(context.getHorizontalDirection())))return false;
   if((block instanceof net.minecraft.world.level.block.DoorBlock||block instanceof net.minecraft.world.level.block.DoublePlantBlock)&&!WestLand.edit(l,context.getPlayer(),pos.above()))return false;
   return !LandSafety.forbidden(block);
  }
  if(PrivateFarms.edit(l,context.getPlayer(),pos)&&pos.getY()>=65)return true;if(!CityProtection.structure(l,pos))return true;
  if(!movable(block)||!editablePosition(l,context.getPlayer(),pos))return false;
  if(block instanceof net.minecraft.world.level.block.BedBlock&&!editablePosition(l,context.getPlayer(),pos.relative(context.getHorizontalDirection())))return false;
  return true;
 }
 public static boolean altersWorld(ItemStack stack){String id=BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();return stack.getItem() instanceof BucketItem||id.endsWith("_bucket")||id.endsWith("_spawn_egg")||id.endsWith("_boat")||id.endsWith("_raft")||id.endsWith("minecart")||id.endsWith("_axe")||id.endsWith("_hoe")||id.endsWith("_shovel")||Set.of("bone_meal","honeycomb","shears","flint_and_steel","fire_charge","armor_stand","painting","item_frame","glow_item_frame","end_crystal").contains(id);}
}
