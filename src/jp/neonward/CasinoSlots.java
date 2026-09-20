package jp.neonward;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.commands.Commands;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Display;
import net.minecraft.world.phys.AABB;
/** Six independent, physical slot terminals. The dedicated endpoint exposes only view/spin. */
public final class CasinoSlots {
 static final int COUNT=6;static final double Z=446.7;
 static final long[] start=new long[COUNT],end=new long[COUNT];
 static final String[] TRIM={"red_concrete","blue_concrete","yellow_concrete","purple_concrete","cyan_concrete","lime_concrete"};
 static final String[] TITLE={"LUCKY SEVEN","CHROME BAR","GOLD RUSH","NIGHT STAR","BLUE DIAMOND","WILD CLOVER"};
 static double x(int i){return 498.0+i*2.3;}
 static boolean near(ServerPlayer p,int i){return i>=0&&i<COUNT&&NeonCasino.inside(p)&&p.distanceToSqr(x(i),65.8,Z)<25;}
 static boolean busy(int i,long now){return StockMarket.ledger!=null&&StockMarket.ledger.slot(i).active;}
 static boolean animating(long now){for(int i=0;i<COUNT;i++)if(busy(i,now))return true;return false;}
 static void spin(int i,long now){start[i]=now;end[i]=now+80;}
 static int request(ServerPlayer p,int i,String action){return ManualSlots.request(p,i,action);}
 public static void init(){ServerLifecycleEvents.SERVER_STARTED.register(s->{Arrays.fill(start,-1000);Arrays.fill(end,0);});
  CommandRegistrationCallback.EVENT.register((d,c,e)->{var id=Commands.argument("machine",IntegerArgumentType.integer(0,5));for(String action:new String[]{"view","spin","stop0","stop1","stop2"})id.then(Commands.literal(action).executes(ctx->request(ctx.getSource().getPlayerOrException(),IntegerArgumentType.getInteger(ctx,"machine"),action)));d.register(Commands.literal("neonslot").then(id));});
  UseEntityCallback.EVENT.register((p,l,h,e,hit)->{for(int i=0;i<COUNT;i++)if(e.entityTags().contains("nw_casino_touch_bank_"+i)){if(p instanceof ServerPlayer sp&&h==net.minecraft.world.InteractionHand.MAIN_HAND)request(sp,i,"view");return net.minecraft.world.InteractionResult.SUCCESS;}return net.minecraft.world.InteractionResult.PASS;});
 }
 static void render(ServerLevel l,long now){
  // Retire only the former freestanding slot model; retain its reception counter and storage.
  for(var e:l.getEntitiesOfClass(net.minecraft.world.entity.Entity.class,new AABB(518,65,466,523,69,470),e->e.entityTags().contains("nw_casino_touch_slot")||e.entityTags().stream().anyMatch(t->t.startsWith("nw_casino_prop_"))))e.discard();
  for(int i=0;i<COUNT;i++)if(l.isPositionEntityTicking(net.minecraft.core.BlockPos.containing(x(i),65,Z)))cabinet(l,i,now);
 }
 static void cabinet(ServerLevel l,int i,long now){double x=x(i),y=65,z=Z;String trim=TRIM[i];
  CasinoProps.touch(l,"bank_"+i,x,y,z,1.85f,2.8f);
  b(l,i,"plinth",x,y,z,1.82f,.12f,1.26f,"polished_blackstone");
  b(l,i,"pedestal",x,y+.12,z,1.58f,.66f,1.05f,"gray_concrete");
  b(l,i,"cabinet",x,y+.75,z-.13,1.70f,1.95f,.90f,"black_concrete");
  for(int side:new int[]{-1,1}){
   b(l,i,"side"+side,x+side*.80,y+.73,z-.1,.10f,1.95f,.91f,"iron_block");
   b(l,i,"edge"+side,x+side*.74,y+.86,z+.35,.045f,1.73f,.06f,trim);
   b(l,i,"headcap"+side,x+side*.83,y+2.59,z-.12,.13f,.15f,.95f,"light_gray_concrete");
   for(int v=0;v<4;v++)b(l,i,"vent"+side+v,x+side*.854,y+1.0+v*.11,z-.28,.012f,.035f,.38f,"black_concrete");
  }
  b(l,i,"top",x,y+2.66,z-.12,1.76f,.10f,.96f,"iron_block");
  b(l,i,"marquee_frame",x,y+2.25,z+.35,1.46f,.40f,.10f,trim);
  b(l,i,"marquee",x,y+2.30,z+.411,1.32f,.30f,.035f,"black_concrete");
  t(l,i,"brand",x,y+2.37,z+.44,TITLE[i],0xffda72,.34f);
  b(l,i,"glass_frame",x,y+1.33,z+.35,1.43f,.85f,.07f,"iron_block");
  b(l,i,"glass_shadow",x,y+1.37,z+.397,1.34f,.77f,.04f,"black_concrete");
  var state=StockMarket.ledger==null?new MarketLedger.SlotState():StockMarket.ledger.slot(i);
  for(int r=0;r<3;r++){double rx=x+(r-1)*.43;boolean moving=state.active&&!state.stopped[r];int n=ManualSlots.symbol(state,r,now);
   b(l,i,"reel"+r,rx,y+1.40,z+.43,.39f,.69f,.025f,"white_concrete");
   // Curved reel edges recede into a shadowed window; center symbols meet the payline.
   b(l,i,"shade_top"+r,rx,y+2.035,z+.449,.39f,.045f,.017f,"light_gray_concrete");
   b(l,i,"shade_bottom"+r,rx,y+1.40,z+.449,.39f,.045f,.017f,"light_gray_concrete");
   for(int row=-1;row<=1;row++){int symbol=Math.floorMod(n+row,6);double offset=moving?(now%4/4.0-.5)*.05:0;t(l,i,"symbol"+r+"_"+row,rx,y+1.68+row*.205+offset,z+.464,CasinoProps.SYMBOLS[symbol],row==0?CasinoProps.COLORS[symbol]:0x666b78,row==0?.48f:.31f);}
  }
  for(int side:new int[]{-1,1})t(l,i,"payline"+side,x+side*.72,y+1.70,z+.47,side<0?">":"<",0xff3939,.4f);
  b(l,i,"console",x,y+1.11,z+.47,1.60f,.14f,.56f,"light_gray_concrete");
  b(l,i,"console_face",x,y+1.245,z+.49,1.48f,.025f,.47f,"black_concrete");
  for(int r=0;r<3;r++){b(l,i,"button_bezel"+r,x+(r-1)*.31,y+1.27,z+.58,.21f,.025f,.19f,"iron_block");b(l,i,"stop_button"+r,x+(r-1)*.31,y+1.295,z+.58,.16f,.035f,.14f,"red_concrete");}
  b(l,i,"start_button",x-.60,y+1.27,z+.58,.19f,.06f,.19f,"emerald_block");
  b(l,i,"coin_plate",x+.60,y+1.28,z+.55,.18f,.018f,.23f,"iron_block");
  b(l,i,"coin_slit",x+.60,y+1.3,z+.55,.025f,.015f,.15f,"black_concrete");
  b(l,i,"win_panel",x,y+.94,z+.392,1.23f,.13f,.022f,"black_concrete");
  t(l,i,"win",x,y+.965,z+.415,busy(i,now)?"PLAYING...":state.result,0x7dff90,.19f);
  b(l,i,"tray_back",x,y+.34,z+.47,1.26f,.32f,.13f,"black_concrete");
  b(l,i,"tray_floor",x,y+.31,z+.63,1.35f,.045f,.41f,"iron_block");
  b(l,i,"tray_lip",x,y+.35,z+.82,1.35f,.13f,.045f,"light_gray_concrete");
  for(int side:new int[]{-1,1})b(l,i,"tray_side"+side,x+side*.65,y+.35,z+.65,.045f,.18f,.39f,"iron_block");
  for(int c=0;c<4;c++)b(l,i,"medal"+c,x-.25+c*.16,y+.36,z+.62,.12f,.022f,.11f,"gold_block");
  b(l,i,"lever",x+.95,y+1.25,z+.12,.065f,.49f,.065f,"iron_block");
  b(l,i,"handle",x+.95,y+1.71,z+.12,.17f,.17f,.17f,"red_concrete");
  t(l,i,"number",x,y+.12,z+.545,"SLOT 0"+(i+1)+" / 100 Cr",0xe7d9b4,.20f);
  b(l,i,"stool_base",x,y+.02,z+1.55,.65f,.06f,.65f,"iron_block");
  b(l,i,"stool_post",x,y+.08,z+1.55,.12f,.46f,.12f,"iron_block");
  b(l,i,"stool_seat",x,y+.54,z+1.55,.67f,.15f,.63f,"red_concrete");
 }
 static void b(ServerLevel l,int i,String key,double x,double y,double z,float w,float h,float d,String block){CasinoProps.box(l,"slot6_"+i+"_"+key,x,y,z,w,h,d,block);}
 static void t(ServerLevel l,int i,String key,double x,double y,double z,String value,int color,float scale){CasinoProps.text(l,"slot6_"+i+"_"+key,x,y,z,value,color,scale,false);}
}
