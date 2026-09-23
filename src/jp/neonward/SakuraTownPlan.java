package jp.neonward;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;

/** Pure, deterministic authored blueprint. No world access, registrations, entities, or installation.
 * Coordinates include explicit air in interiors and passageways; the installer must preflight ALL entries.
 * Thin foundations only. Parent owns terrain survey, east gate/tunnel, protection and incremental writes.
 */
public final class SakuraTownPlan {
    public static final int MIN_X=-216, MAX_X=-64, MIN_Z=296, MAX_Z=464;
    public static final int MIN_Y=63, MAX_Y=99, MAX_BLOCKS=200000;
    public static final int STREET_Y=64, STREET_Z=368;
    public static final int PRECINCT_MIN_X=-216, PRECINCT_MAX_X=-144, PRECINCT_MIN_Z=312, PRECINCT_MAX_Z=424;
    public static final BlockPos ENTRANCE=new BlockPos(-64,65,368);
    public static final BlockPos SAISEN=new BlockPos(-177,71,368);
    public static final BlockPos CHOZU=new BlockPos(-159,68,393);
    public static final BlockPos COUNTER=new BlockPos(-188,68,339);
    public static final BlockPos STAFF=new BlockPos(-190,68,339);
    public static final Direction STAFF_FACING=Direction.EAST;
    public static final BlockPos SHRINE_ENTRANCE=new BlockPos(-143,65,368);
    public record House(int id,int minX,int minZ,int maxX,int maxZ,BlockPos door,Direction facing) {}
    public static final List<House> HOUSES=houses();
    private final LinkedHashMap<BlockPos,BlockState> blocks=new LinkedHashMap<>();
    private SakuraTownPlan() {}

    private static List<House> houses() {
        var result=new ArrayList<House>();
        for(int row=0;row<2;row++)for(int column=0;column<4;column++) {
            int id=row*4+column,x=-135+18*column,w=12+(id%3),depth=12+(id%3)*2;
            int z=row==0?350-depth+1:389;
            var facing=row==0?Direction.SOUTH:Direction.NORTH;
            int doorX=x+w/2+(id%2==0?-1:1);
            result.add(new House(id,x,z,x+w-1,z+depth-1,new BlockPos(doorX,65,row==0?350:389),facing));
        }
        return List.copyOf(result);
    }
    public static Map<BlockPos,BlockState> build() {
        var p=new SakuraTownPlan();
        p.streets();
        for(var house:HOUSES)p.house(house);
        p.canal();
        p.shrine();
        for(int x=-140;x<=-68;x+=18) {p.cherry(x,361,65,0);p.cherry(x,379,65,1);}
        p.cherry(-209,411,68,2);p.cherry(-208,321,68,3);p.cherry(-166,325,68,1);p.cherry(-166,415,68,0);
        // Reassert every public walking approach after decorative canopies and rails.
        p.box(-141,65,364,-64,68,372,Blocks.AIR.defaultBlockState());
        for(var h:HOUSES)p.approach(h);
        SakuraMaterials.footprint("sakura_saisen_box",SAISEN,Direction.EAST).forEach(p::put);
        SakuraMaterials.footprint("sakura_chozu_basin",CHOZU,Direction.EAST).forEach(p::put);
        p.put(COUNTER,face("omamori_counter",Direction.EAST));
        p.box(STAFF.getX(),68,STAFF.getZ(),STAFF.getX(),70,STAFF.getZ(),Blocks.AIR.defaultBlockState());
        if(p.blocks.size()>MAX_BLOCKS)throw new IllegalStateException("Sakura blueprint exceeds budget: "+p.blocks.size());
        return Collections.unmodifiableMap(p.blocks);
    }
    private static BlockState mat(String id) {return SakuraMaterials.get("sakura_"+id).defaultBlockState();}
    private static BlockState face(String id,Direction direction) {return SakuraMaterials.facing("sakura_"+id,direction);}
    private void put(BlockPos pos,BlockState state) {put(pos.getX(),pos.getY(),pos.getZ(),state);}
    private void put(int x,int y,int z,BlockState state) {
        if(x<MIN_X||x>MAX_X||z<MIN_Z||z>MAX_Z||y<MIN_Y||y>MAX_Y)throw new IllegalArgumentException("Sakura bounds: "+x+","+y+","+z);
        blocks.put(new BlockPos(x,y,z),Objects.requireNonNull(state));
    }
    private void box(int x,int y,int z,int X,int Y,int Z,BlockState s) {
        for(int yy=y;yy<=Y;yy++)for(int zz=z;zz<=Z;zz++)for(int xx=x;xx<=X;xx++)put(xx,yy,zz,s);
    }
    private void path(int x,int z,int X,int Z,int surface) {
        box(x,surface,z,X,surface,Z,mat("stone_paving"));
        box(x,surface+1,z,X,surface+4,Z,Blocks.AIR.defaultBlockState());
    }
    private void streets() {
        path(-143,362,-64,375,64);
        path(-140,330,-66,333,64);path(-140,409,-66,413,64);
        for(int x=-139;x<=-67;x+=18)path(x,331,x+2,412,64);
        path(-68,358,-64,380,64);
        for(int x=-139;x<=-85;x+=18) {
            put(x,65,376,mat("stone_lantern"));
            put(x,65,360,mat("stone_lantern"));
        }
    }
    private void house(House h) {
        int x=h.minX(),X=h.maxX(),z=h.minZ(),Z=h.maxZ(),ridge=69+(Z-z+2)/4;
        // Entire room/attic envelope is explicit, preventing vegetation or unknown interior blocks from being hidden.
        box(x-1,65,z-1,X+1,ridge+1,Z+1,Blocks.AIR.defaultBlockState());
        box(x,64,z,X,64,Z,mat("board"));
        box(x,65,z,X,68,z,mat("plaster"));box(x,65,Z,X,68,Z,mat("plaster"));
        box(x,65,z,x,68,Z,mat("plaster"));box(X,65,z,X,68,Z,mat("plaster"));
        for(int xx=x;xx<=X;xx+=4)for(int zz:new int[]{z,Z})box(xx,65,zz,xx,68,zz,mat("dark_timber"));
        for(int xx:new int[]{x,X})for(int zz:new int[]{z,Z})box(xx,65,zz,xx,68,zz,mat("dark_timber"));
        for(int yy:new int[]{65,68}) {
            box(x,yy,z,X,yy,z,mat("dark_timber"));box(x,yy,Z,X,yy,Z,mat("dark_timber"));
            box(x,yy,z,x,yy,Z,mat("dark_timber"));box(X,yy,z,X,yy,Z,mat("dark_timber"));
        }
        int front=h.facing()==Direction.SOUTH?Z:z,back=h.facing()==Direction.SOUTH?z:Z;
        for(int xx=x+2;xx<X-1;xx++)if(Math.abs(xx-h.door().getX())>1) {
            put(xx,66,front,face(h.id()%2==0?"lattice":"shoji",h.facing()));
            put(xx,67,front,face("shoji",h.facing()));
        }
        // Rear windows and differing front awning widths distinguish the eight shop-houses.
        for(int xx=x+2;xx<X-1;xx+=3)put(xx,67,back,face("lattice",h.facing().getOpposite()));
        door(h.door(),h.facing());
        box(x,68,z,X,68,Z,mat("board")); // Continuous opaque ceiling below shaped roof courses.
        roof(x-1,z-1,X+1,Z+1,69);
        int awningZ=front+h.facing().getStepZ();
        for(int xx=x+1;xx<=X-1-(h.id()%3);xx++)put(xx,68,awningZ,face("kawara_slope",h.facing()));
        put(x+2,67,awningZ,face("paper_lantern",h.facing()));
        put(X-2,67,awningZ,face("paper_lantern",h.facing()));
        // A rear shelf/bench only; the door-to-room route stays empty.
        int interiorBack=back+h.facing().getStepZ();
        for(int xx=x+2;xx<x+5;xx++)put(xx,65,interiorBack,mat("board"));
        put(X-2,65,interiorBack,Blocks.FLOWER_POT.defaultBlockState());
    }
    private void door(BlockPos p,Direction facing) {
        var state=Blocks.DARK_OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING,facing)
            .setValue(DoorBlock.HINGE,DoorHingeSide.LEFT).setValue(DoorBlock.OPEN,false).setValue(DoorBlock.POWERED,false);
        put(p,state.setValue(DoorBlock.HALF,DoubleBlockHalf.LOWER));
        put(p.above(),state.setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER));
    }
    /** Shallow stepped machiya roof: rise once per two cells, with closed gables and sealed crest. */
    private void roof(int x,int z,int X,int Z,int base) {
        int half=(Z-z)/2;
        for(int step=0;step<=half;step++) {
            int y=base+step/2;
            for(int xx=x;xx<=X;xx++) {
                // Full tile on level courses avoids sawtooth gaps between equal-height slopes.
                put(xx,y,z+step,step%2==0?mat("kawara_tile"):face("kawara_slope",Direction.NORTH));
                put(xx,y,Z-step,step%2==0?mat("kawara_tile"):face("kawara_slope",Direction.SOUTH));
            }
        }
        int crestNorth=z+half,crestSouth=Z-half;
        for(int zz=z;zz<=Z;zz++) {
            int slopeY=base+Math.min(zz-z,Z-zz)/2;
            for(int xx:new int[]{x,X})for(int yy=base;yy<slopeY;yy++)
                put(xx,yy,zz,mat(yy==base||zz==crestNorth||zz==crestSouth?"dark_timber":"plaster"));
        }
        for(int xx=x;xx<=X;xx++)for(int zz=crestNorth;zz<=crestSouth;zz++) {
            put(xx,base+half/2,zz,mat("kawara_tile"));
            put(xx,base+half/2+1,zz,face("kawara_ridge",Direction.EAST));
        }
    }
    /** Four shallow planes, clipped by the short ends: the long ridge always runs along Z. */
    private void hipRoof(int x,int z,int X,int Z,int base) {
        box(x,base-1,z,X,base-1,Z,mat("dark_timber")); // Unbroken soffit: no daylight strips.
        for(int xx=x;xx<=X;xx++)for(int zz=z;zz<=Z;zz++) {
            int west=xx-x,east=X-xx,north=zz-z,south=Z-zz;
            int inset=Math.min(Math.min(west,east),Math.min(north,south)),y=base+inset/2;
            Direction out=west==inset?Direction.WEST:east==inset?Direction.EAST:north==inset?Direction.NORTH:Direction.SOUTH;
            put(xx,y,zz,inset%2==0?mat("shrine_roof_tile"):face("shrine_roof_slope",out));
            // A closed soffit/riser below raised courses makes the hip a weather-tight shell.
            if(inset>0)put(xx,y-1,zz,mat("dark_timber"));
        }
        int half=(X-x)/2;
        for(int xx=x+half;xx<=X-half;xx++)for(int zz=z+half;zz<=Z-half;zz++) {
            put(xx,base+half/2,zz,mat("shrine_roof_tile"));
            put(xx,base+half/2+1,zz,face("shrine_roof_ridge",Direction.SOUTH));
        }
    }
    private void canal() {
        box(-141,63,355,-66,63,359,mat("stone_paving"));
        box(-141,64,355,-66,64,355,mat("stone_paving"));box(-141,64,359,-66,64,359,mat("stone_paving"));
        box(-141,64,356,-141,64,358,mat("stone_paving"));box(-66,64,356,-66,64,358,mat("stone_paving"));
        box(-140,64,356,-67,64,358,Blocks.WATER.defaultBlockState());
        box(-140,65,356,-67,68,358,Blocks.AIR.defaultBlockState());
        for(var h:HOUSES)if(h.facing()==Direction.SOUTH)bridge(h.door().getX());
        for(int x=-138;x<=-84;x+=18)bridge(x);
    }
    private void bridge(int x) {
        box(x-2,64,354,x+2,64,360,mat("board"));
        for(int xx:new int[]{x-2,x+2})for(int z=356;z<=358;z++)put(xx,65,z,Blocks.DARK_OAK_FENCE.defaultBlockState());
        box(x-1,65,354,x+1,68,360,Blocks.AIR.defaultBlockState());
    }
    private void approach(House h) {
        int x=h.door().getX();
        if(h.facing()==Direction.SOUTH) {
            box(x-1,64,351,x+1,64,362,mat("board"));
            box(x-1,65,351,x+1,67,362,Blocks.AIR.defaultBlockState());
        } else {
            box(x-1,64,374,x+1,64,388,mat("stone_paving"));
            box(x-1,65,374,x+1,67,388,Blocks.AIR.defaultBlockState());
        }
        // Never overwrite the authored door when clearing its exterior approach.
    }
    private void shrine() {
        // One elevated paving layer and perimeter fascia, not a dense underground fill.
        box(PRECINCT_MIN_X,67,PRECINCT_MIN_Z,PRECINCT_MAX_X,67,PRECINCT_MAX_Z,mat("stone_paving"));
        box(PRECINCT_MIN_X,68,PRECINCT_MIN_Z,PRECINCT_MAX_X,73,PRECINCT_MAX_Z,Blocks.AIR.defaultBlockState());
        for(int y=64;y<67;y++) {
            box(-216,y,312,-144,y,312,mat("stone_paving"));box(-216,y,424,-144,y,424,mat("stone_paving"));
            box(-216,y,312,-216,y,424,mat("stone_paving"));box(-144,y,312,-144,y,424,mat("stone_paving"));
        }
        for(int x=-216;x<=-144;x++)for(int z:new int[]{312,424})put(x,68,z,mat("stone_paving"));
        for(int z=312;z<=424;z++)put(-216,68,z,mat("stone_paving"));
        // Three broad stair treads climb west from street y64 to precinct y67.
        for(int step=0;step<3;step++)for(int z=362;z<=374;z++) {
            int x=-142-step;put(x,65+step,z,stairs(Direction.WEST));
            box(x,66+step,z,x,70,z,Blocks.AIR.defaultBlockState());
        }
        box(-148,68,362,-145,72,374,Blocks.AIR.defaultBlockState());
        torii(-152,68,368);
        hall();
        office();temizu();gardens();
        for(int x=-170;x<=-154;x+=8)for(int z:new int[]{356,380})put(x,68,z,mat("stone_lantern"));
    }
    private void hall() {
        // Broad east-facing facade: 25 along Z, only 11 deep along X.
        // Deck/box/stairs retain their public anchors, but the old oversized footprint is gone.
        box(-198,68,355,-174,69,355,mat("stone_paving"));
        box(-198,68,381,-174,69,381,mat("stone_paving"));
        box(-198,68,356,-198,69,380,mat("stone_paving"));
        box(-174,68,356,-174,69,380,mat("stone_paving"));
        box(-198,70,355,-174,70,381,mat("board"));
        box(-199,71,354,-174,83,382,Blocks.AIR.defaultBlockState());
        box(-196,71,356,-196,74,380,mat("plaster"));
        box(-196,71,356,-186,74,356,mat("plaster"));
        box(-196,71,380,-186,74,380,mat("plaster"));
        for(int z=357;z<=379;z++)if(z<367||z>369)
            box(-186,71,z,-186,73,z,face("lattice",Direction.EAST));
        door(new BlockPos(-186,71,368),Direction.EAST);
        // Four columns define three bays. Four red shaft blocks, restrained gold foot/capital.
        for(int z:new int[]{356,364,372,380}) {
            put(-185,71,z,mat("dark_timber"));
            box(-185,72,z,-185,75,z,mat("vermilion_timber"));
        }
        box(-196,75,356,-186,75,356,mat("dark_timber"));
        box(-196,75,380,-186,75,380,mat("dark_timber"));
        box(-196,75,356,-196,75,380,mat("dark_timber"));
        box(-185,75,356,-185,75,380,mat("dark_timber"));
        // Restore red shafts where the longitudinal lintel intersects their top course.
        for(int z:new int[]{356,364,372,380})put(-185,75,z,mat("vermilion_timber"));
        hipRoof(-198,354,-182,382,76);
        for(int z:new int[]{356,364,372,380}) {
            put(-185,75,z,mat("vermilion_timber"));
            put(-185,76,z,face("shrine_bracket",Direction.EAST));
            put(-184,75,z,face("shrine_bracket",Direction.EAST));
        }
        porch();
        for(int x=-197;x<=-175;x++)for(int z:new int[]{355,381})put(x,71,z,Blocks.DARK_OAK_FENCE.defaultBlockState());
        for(int z=355;z<=381;z++)if(z<364||z>372)put(-174,71,z,Blocks.DARK_OAK_FENCE.defaultBlockState());
        for(int step=0;step<3;step++)for(int z=364;z<=372;z++)put(-171-step,68+step,z,stairs(Direction.WEST));
        for(int z:new int[]{360,376})put(-176,71,z,mat("stone_lantern"));
    }
    private void porch() {
        // Seven-wide projecting gable is subordinate to the long main roof, not the whole facade.
        for(int z:new int[]{365,371}) {
            put(-175,71,z,mat("dark_timber"));
            box(-175,72,z,-175,74,z,mat("vermilion_timber"));
        }
        box(-184,75,365,-175,75,371,mat("dark_timber"));
        for(int z:new int[]{365,371})put(-175,75,z,face("shrine_bracket",Direction.EAST));
        for(int z=366;z<=370;z++)put(-175,74,z,face("shrine_rope",Direction.EAST));
        for(int x=-184;x<=-175;x++)for(int z=365;z<=371;z++) {
            int rise=Math.min(z-365,371-z),y=76+rise;
            // Merge into, rather than punch through, the existing hip at the porch's rear.
            var at=new BlockPos(x,y,z);var existing=blocks.get(at);
            if(existing==null||existing.isAir())put(at,rise==3?mat("shrine_roof_tile"):face("shrine_roof_slope",z<368?Direction.NORTH:Direction.SOUTH));
            if(x==-175)for(int yy=76;yy<y;yy++)put(x,yy,z,mat(yy==y-1||z==368?"dark_timber":"plaster"));
            if(rise==3)put(x,y+1,z,face("shrine_roof_ridge",Direction.EAST));
        }
        // Small turned-up hood lip above the approach, with three blocks of head clearance.
        for(int z=365;z<=371;z++)put(-174,Math.abs(z-368)==3?77:76,z,mat("shrine_roof_tile"));
    }
    private static BlockState stairs(Direction facing) {
        return Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING,facing);
    }
    private void torii(int x,int y,int z) {
        for(int zz:new int[]{z-4,z+4}) {
            box(x-1,y,zz-1,x+1,y,zz+1,mat("stone_paving"));
            box(x,y+1,zz,x,y+6,zz,mat("vermilion_timber"));
        }
        box(x,y+5,z-5,x,y+5,z+5,mat("vermilion_timber"));
        box(x,y+7,z-5,x,y+7,z+5,mat("dark_timber"));
        put(x,y+8,z-5,mat("kawara_tile"));put(x,y+8,z+5,mat("kawara_tile"));
        put(x,y+6,z,mat("board"));
    }
    private void office() {
        // Eleven-wide service front, seven-deep room. Existing service cells remain exact.
        box(-194,68,334,-188,71,344,Blocks.AIR.defaultBlockState());
        box(-194,68,334,-194,71,344,mat("plaster"));
        box(-194,68,334,-188,71,334,mat("plaster"));
        box(-194,68,344,-188,71,344,mat("plaster"));
        for(int x:new int[]{-194,-188})for(int z:new int[]{334,344})box(x,68,z,x,71,z,mat("dark_timber"));
        for(int z=336;z<=342;z++)if(z!=339)put(-188,68,z,mat("board"));
        hipRoof(-195,333,-186,345,73);
        put(-187,71,335,face("paper_lantern",Direction.EAST));
        put(-187,71,343,face("paper_lantern",Direction.EAST));
    }
    private void temizu() {
        // Seven-square shelter around the unchanged basin, with a four-block open ceiling.
        box(-162,68,390,-156,71,396,Blocks.AIR.defaultBlockState());
        for(int x:new int[]{-162,-156})for(int z:new int[]{390,396})box(x,68,z,x,71,z,mat("dark_timber"));
        hipRoof(-163,389,-155,397,73);
    }
    private void gardens() {
        // Deliberate garden islands break up the plaza while leaving the central and service routes paved.
        garden(-212,317,-183,328,false);
        garden(-213,348,-203,390,true);
        garden(-207,403,-180,418,false);
        garden(-168,317,-149,346,true);
        garden(-150,393,-147,416,false);
        path(-201,330,-184,332,67);
        path(-183,337,-173,341,67);
        path(-170,365,-156,371,67);
        path(-162,385,-156,388,67);
    }
    private void garden(int x,int z,int X,int Z,boolean moss) {
        // Gravel receives its own thin support layer; nothing can fall into the raised base.
        if(!moss)box(x,66,z,X,66,Z,mat("stone_paving"));
        box(x,67,z,X,67,Z,(moss?Blocks.MOSS_BLOCK:Blocks.GRAVEL).defaultBlockState());
        for(int xx=x;xx<=X;xx+=5)for(int zz:new int[]{z,Z})put(xx,67,zz,mat("stone_paving"));
    }
    private void cherry(int x,int z,int base,int variant) {
        // Fixed branch/crown geometry: no random growth or sapling ticks.
        put(x,base-1,z,Blocks.DIRT.defaultBlockState());
        box(x,base,z,x,base+5,z,Blocks.CHERRY_LOG.defaultBlockState());
        int branch=variant%2==0?1:-1;
        for(int n=1;n<=3;n++)put(x+n*branch,base+4,z,Blocks.CHERRY_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.X));
        var leaves=Blocks.CHERRY_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT,true).setValue(LeavesBlock.DISTANCE,1);
        for(int dy=4;dy<=7;dy++)for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++) {
            int radius=dy==7?2:dy==4?3:4;
            if(Math.abs(dx)+Math.abs(dz)>radius+2||Math.max(Math.abs(dx),Math.abs(dz))>radius)continue;
            var p=new BlockPos(x+dx,base+dy,z+dz);
            var old=blocks.get(p);
            if(old==null||old.isAir())put(p,leaves);
        }
    }
}
