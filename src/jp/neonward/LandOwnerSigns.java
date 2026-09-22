package jp.neonward;
import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.*;
import net.minecraft.network.chat.Component;
import com.mojang.math.Transformation;
import org.joml.Vector3f;
import org.joml.Quaternionf;
import jp.neonward.mixin.MeterTextAccess;
/** Nine static, full-bright display entities per owned plot. Never places blocks in player builds. */
public final class LandOwnerSigns {
 static final Map<ServerLevel,Map<String,Display>> CACHE=new WeakHashMap<>();
 static Vec3 center(int id){return new Vec3(LandLayout.x(id)+24,67,id<4?137:166);}
 static String tag(int id,String part){return "nw_land_owner_"+id+"_"+part;}
 static Display find(ServerLevel l,int id,String part,boolean text,String block){String tag=tag(id,part);var cache=CACHE.computeIfAbsent(l,k->new HashMap<>());var old=cache.get(tag);if(old!=null&&!old.isRemoved())return old;
  Display found=null;for(var e:l.getEntitiesOfClass(Display.class,new AABB(center(id),center(id)).inflate(8),e->e.entityTags().contains(tag))){if(found==null)found=e;else e.discard();}
  boolean fresh=found==null;if(fresh)found=text?new Display.TextDisplay(EntityTypes.TEXT_DISPLAY,l):new Display.BlockDisplay(EntityTypes.BLOCK_DISPLAY,l);
  ClockworkFeedback.load(found,l,"{Invulnerable:1b,NoGravity:1b,brightness:{block:15,sky:15},view_range:2.0f,Tags:['"+tag+"'],"+(text?"text:{text:''},billboard:'fixed',background:0,line_width:400":"block_state:{Name:'minecraft:"+block+"'}")+"}");
  found.setPos(center(id));cache.put(tag,found);if(fresh)l.addFreshEntity(found);return found;
 }
 static void box(ServerLevel l,int id,String part,double dx,double y,float w,float h,float d,String block){var c=center(id);var e=find(l,id,part,false,block);e.setPos(c.x+dx,y,c.z);ClockworkDisplays.transform(e,new Transformation(new Vector3f(-w/2,0,-d/2),new Quaternionf(),new Vector3f(w,h,d),new Quaternionf()));}
 static void lettering(ServerLevel l,int id,boolean back,String name){var c=center(id);var e=(Display.TextDisplay)find(l,id,back?"back_text":"front_text",true,"");e.setPos(c.x,67.25,c.z+(back?-.19:.19));var value=Component.literal("LAND "+(id+1)+"  /  PRIVATE\n").withColor(0x68fff0).append(Component.literal(name+"\n").withColor(0xffffff)).append(Component.literal("所有者 / OWNER").withColor(0xffd578));var access=(MeterTextAccess)e;if(!access.neonGetText().equals(value))access.neonSetText(value);ClockworkDisplays.transform(e,new Transformation(new Vector3f(),new Quaternionf().rotationY(back?(float)Math.PI:0),new Vector3f(.8f),new Quaternionf()));}
 static void refresh(ServerLevel l,int id){if(StockMarket.ledger==null)return;var owner=StockMarket.ledger.westLand.get(id);if(owner==null||!l.isPositionEntityTicking(BlockPos.containing(center(id))))return;
  box(l,id,"left_post",-2.7,65,.22f,2.4f,.24f,"polished_deepslate");box(l,id,"right_post",2.7,65,.22f,2.4f,.24f,"polished_deepslate");
  box(l,id,"panel",0,67.05,6.3f,2.25f,.28f,"black_concrete");box(l,id,"top",0,69.3,6.55f,.1f,.34f,"cyan_concrete");box(l,id,"bottom",0,66.95,6.55f,.1f,.34f,"magenta_concrete");box(l,id,"left",-3.225,67.05,.1f,2.25f,.34f,"cyan_concrete");box(l,id,"right",3.225,67.05,.1f,2.25f,.34f,"cyan_concrete");
  lettering(l,id,false,owner.name);lettering(l,id,true,owner.name);
  var at=WestLand.terminal(id,0).above();if(l.getBlockEntity(at) instanceof net.minecraft.world.level.block.entity.SignBlockEntity sign){var text=sign.getFrontText().setMessage(2,Component.literal("所有者: "+owner.name)).setMessage(3,Component.literal("購入済み / UUID管理"));if(!sign.getFrontText().getMessage(2,false).getString().equals("所有者: "+owner.name)){sign.setText(text,true);sign.setText(text,false);sign.setChanged();l.sendBlockUpdated(at,l.getBlockState(at),l.getBlockState(at),3);}}
 }
 static void tick(net.minecraft.server.MinecraftServer server){if(server.getTickCount()%40!=0||StockMarket.ledger==null)return;for(int id=0;id<8;id++)refresh(server.overworld(),id);}
}
