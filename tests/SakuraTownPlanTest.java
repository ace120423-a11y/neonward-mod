package jp.neonward;

import java.util.*;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/** Offline blueprint geometry test, never opens a world. Materials use vanilla stand-ins in this JVM only. */
public final class SakuraTownPlanTest {
    static void check(boolean condition,String reason){if(!condition)throw new AssertionError(reason);}
    static boolean clear(Map<BlockPos,BlockState> plan,BlockPos p){var s=plan.get(p);return s!=null&&(s.isAir()||s.getBlock() instanceof DoorBlock);}
    static boolean stand(Map<BlockPos,BlockState> plan,BlockPos feet){var floor=plan.get(feet.below());return floor!=null&&!floor.isAir()&&!floor.is(Blocks.WATER)&&!(floor.getBlock() instanceof FenceBlock)&&clear(plan,feet)&&clear(plan,feet.above());}
    static void roof(Map<BlockPos,BlockState> plan,int x,int z,int X,int Z,int base){
        int half=(Z-z)/2;
        for(int zz=z;zz<=Z;zz++) {
            int y=base+Math.min(zz-z,Z-zz)/2;
            for(int xx=x;xx<=X;xx++)check(plan.containsKey(new BlockPos(xx,y,zz))&&!plan.get(new BlockPos(xx,y,zz)).isAir(),"continuous roof plane");
            for(int xx:new int[]{x,X})for(int yy=base;yy<y;yy++)check(plan.containsKey(new BlockPos(xx,yy,zz))&&!plan.get(new BlockPos(xx,yy,zz)).isAir(),"closed gable triangle");
        }
        for(int xx=x;xx<=X;xx++)for(int zz=z+half;zz<=Z-half;zz++) {
            check(plan.get(new BlockPos(xx,base+half/2,zz)).is(Blocks.STONE),"sealed one/two-cell crest");
            check(plan.get(new BlockPos(xx,base+half/2+1,zz)).is(Blocks.STONE),"continuous ridge");
        }
    }
    static void ceiling(Map<BlockPos,BlockState> plan,int x,int z,int X,int Z,int y){
        for(int xx=x;xx<=X;xx++)for(int zz=z;zz<=Z;zz++)
            check(plan.get(new BlockPos(xx,y,zz)).is(Blocks.STONE),"opaque ceiling "+new BlockPos(xx,y,zz));
    }
    static void hip(Map<BlockPos,BlockState> plan,int x,int z,int X,int Z,int base){
        for(int xx=x;xx<=X;xx++)for(int zz=z;zz<=Z;zz++){
            int inset=Math.min(Math.min(xx-x,X-xx),Math.min(zz-z,Z-zz));
            var state=plan.get(new BlockPos(xx,base+inset/2,zz));
            check(state!=null&&!state.isAir(),"sealed hip roof");
        }
        int half=(X-x)/2;
        for(int xx=x+half;xx<=X-half;xx++)for(int zz=z+half;zz<=Z-half;zz++)
            check(plan.get(new BlockPos(xx,base+half/2+1,zz)).is(Blocks.PRISMARINE_BRICKS),"patina ridge along Z");
    }
    static void fixtureSpace(Map<BlockPos,BlockState> plan,BlockPos anchor,int depth){
        for(int back=0;back<depth;back++)for(int width=-1;width<=1;width++){
            var cell=anchor.offset(-back,0,width);
            check(plan.get(cell.below()).is(Blocks.STONE),"fixture remains on intact deck/paving "+cell);
            check(clear(plan,cell.above())&&clear(plan,cell.above(2)),"fixture overhead clearance "+cell);
        }
    }
    static void fixtureParts(Map<BlockPos,BlockState> plan,BlockPos anchor,String[][] rows){
        var expected=new HashSet<BlockPos>();
        var types=new HashSet<Block>();
        for(int back=0;back<rows.length;back++)for(int width=-1;width<=1;width++){
            var cell=anchor.offset(-back,0,width);
            var block=SakuraMaterials.get("sakura_"+rows[back][width+1]);
            check(plan.get(cell).is(block),"exact multipart ID/offset "+cell);
            expected.add(cell);types.add(block);
        }
        var actual=new HashSet<BlockPos>();
        for(var entry:plan.entrySet())if(types.contains(entry.getValue().getBlock()))actual.add(entry.getKey());
        check(actual.equals(expected),"exact fixture footprint, no stray parts");
    }
    @SuppressWarnings("unchecked")
    public static void main(String[] args)throws Exception{
        SharedConstants.tryDetectVersion();Bootstrap.bootStrap();
        // Geometry is independent of resource registration. Use existing blocks without registering anything.
        var f=SakuraMaterials.class.getDeclaredField("BLOCKS");f.setAccessible(true);
        var materials=(Map<String,Block>)f.get(null);
        for(var id:SakuraMaterials.IDS)materials.put(id,Blocks.STONE);
        materials.put("sakura_shrine_roof_tile",Blocks.PRISMARINE);
        materials.put("sakura_shrine_roof_slope",Blocks.DARK_PRISMARINE);
        materials.put("sakura_shrine_roof_ridge",Blocks.PRISMARINE_BRICKS);
        materials.put("sakura_shrine_bracket",Blocks.CHISELED_STONE_BRICKS);
        materials.put("sakura_shrine_rope",Blocks.DIAMOND_BLOCK);
        // Distinct stand-ins let this test catch exchanged multipart IDs, not just occupied cells.
        String[] parts={"saisen_left","saisen_box","saisen_right","chozu_front_left","chozu_basin","chozu_front_right","chozu_back_left","chozu_back_center","chozu_back_right"};
        Block[] partBlocks={Blocks.COBBLESTONE,Blocks.ANDESITE,Blocks.DIORITE,Blocks.GRANITE,Blocks.BRICKS,Blocks.SANDSTONE,Blocks.RED_SANDSTONE,Blocks.POLISHED_ANDESITE,Blocks.POLISHED_DIORITE};
        for(int i=0;i<parts.length;i++){
            check(materials.containsKey("sakura_"+parts[i]),"registered fixture part "+parts[i]);
            materials.put("sakura_"+parts[i],partBlocks[i]);
        }
        var plan=SakuraTownPlan.build();
        check(plan.size()<=200000,"block budget");check(plan.equals(SakuraTownPlan.build()),"deterministic build");
        for(var p:plan.keySet())check(p.getX()>=-216&&p.getX()<=-64&&p.getZ()>=296&&p.getZ()<=464,"surveyed boundary "+p);
        check(SakuraTownPlan.HOUSES.size()==8,"eight distinct houses");
        for(var h:SakuraTownPlan.HOUSES){check(plan.get(h.door()).getBlock() instanceof DoorBlock,"door exists "+h.id());check(plan.get(h.door().above()).getBlock() instanceof DoorBlock,"upper door "+h.id());}
        for(var h:SakuraTownPlan.HOUSES){
            roof(plan,h.minX()-1,h.minZ()-1,h.maxX()+1,h.maxZ()+1,69);
            ceiling(plan,h.minX(),h.minZ(),h.maxX(),h.maxZ(),68);
        }
        hip(plan,-198,354,-182,382,76);hip(plan,-195,333,-186,345,73);hip(plan,-163,389,-155,397,73);
        ceiling(plan,-196,356,-186,380,75);
        ceiling(plan,-194,334,-188,344,72);
        ceiling(plan,-162,390,-156,396,72);
        for(int z:new int[]{356,364,372,380}) {
            check(plan.get(new BlockPos(-185,76,z)).is(Blocks.CHISELED_STONE_BRICKS),"shaped capital survives roofing");
            for(int y=71;y<=75;y++)check(plan.get(new BlockPos(-185,y,z)).is(Blocks.STONE),"front pillar intact");
        }
        check(plan.values().stream().noneMatch(s->s.is(Blocks.GOLD_BLOCK)),"no giant gold cubes");
        for(int x=-184;x<=-175;x++)check(plan.get(new BlockPos(x,80,368)).is(Blocks.PRISMARINE_BRICKS),"porch ridge");
        for(int z=366;z<=370;z++)check(plan.get(new BlockPos(-175,74,z)).is(Blocks.DIAMOND_BLOCK),"porch rope above head clearance");
        for(int x:new int[]{-162,-156})for(int z:new int[]{390,396})for(int y=68;y<=71;y++)
            check(plan.get(new BlockPos(x,y,z)).is(Blocks.STONE),"temizu supports preserved by garden paths");
        for(int x:new int[]{-194,-188})for(int z:new int[]{334,344})for(int y=68;y<=71;y++)
            check(plan.get(new BlockPos(x,y,z)).is(Blocks.STONE),"office supports preserved by garden paths");
        check(SakuraTownPlan.SAISEN.equals(new BlockPos(-177,71,368)),"offering anchor unchanged");
        check(SakuraTownPlan.CHOZU.equals(new BlockPos(-159,68,393)),"basin anchor unchanged");
        check(SakuraTownPlan.COUNTER.equals(new BlockPos(-188,68,339)),"counter anchor unchanged");
        check(SakuraTownPlan.STAFF.equals(new BlockPos(-190,68,339)),"staff anchor unchanged");
        fixtureSpace(plan,SakuraTownPlan.SAISEN,1);
        fixtureSpace(plan,SakuraTownPlan.CHOZU,2);
        fixtureParts(plan,SakuraTownPlan.SAISEN,new String[][]{{"saisen_left","saisen_box","saisen_right"}});
        fixtureParts(plan,SakuraTownPlan.CHOZU,new String[][]{{"chozu_front_left","chozu_basin","chozu_front_right"},{"chozu_back_left","chozu_back_center","chozu_back_right"}});
        for(int z=363;z<=373;z++)check(plan.get(new BlockPos(-152,75,z)).is(Blocks.STONE),"eleven-wide torii lintel");
        for(int z:new int[]{362,374})check(plan.getOrDefault(new BlockPos(-152,75,z),Blocks.AIR.defaultBlockState()).isAir(),"torii stays compact");
        // The deck's entire exposed perimeter must connect continuously to the precinct paving.
        for(int x=-198;x<=-174;x++)for(int z=355;z<=381;z++)if(x==-198||x==-174||z==355||z==381)
            for(int y=67;y<=70;y++)check(plan.get(new BlockPos(x,y,z)).is(Blocks.STONE),"hall foundation reaches ground "+new BlockPos(x,y,z));
        for(int step=0;step<3;step++)for(int z=364;z<=372;z++) {
            var stair=plan.get(new BlockPos(-171-step,68+step,z));
            check(stair.is(Blocks.STONE_BRICK_STAIRS)&&stair.getValue(StairBlock.FACING)==Direction.WEST,"hall stairs unchanged");
        }
        var visited=new HashSet<BlockPos>();var queue=new ArrayDeque<BlockPos>();
        var entrance=SakuraTownPlan.ENTRANCE;check(stand(plan,entrance),"entrance walkable");visited.add(entrance);queue.add(entrance);
        while(!queue.isEmpty()){
            var p=queue.remove();
            for(var d:Direction.Plane.HORIZONTAL)for(int dy=-1;dy<=1;dy++){
                var next=p.relative(d).offset(0,dy,0);
                if(stand(plan,next)&&visited.add(next))queue.add(next);
            }
        }
        for(var h:SakuraTownPlan.HOUSES)check(visited.contains(h.door()),"reachable house "+h.id()+" "+h.door());
        check(visited.contains(SakuraTownPlan.STAFF),"reachable office staff");
        check(visited.contains(SakuraTownPlan.COUNTER.east()),"reachable amulet counter front");
        check(visited.contains(SakuraTownPlan.CHOZU.east()),"reachable purification basin");
        check(visited.contains(SakuraTownPlan.SAISEN.east()),"reachable offering box/deck");
        check(visited.contains(new BlockPos(-186,71,368)),"reachable hall interior door");
        for(var fixture:List.of(SakuraTownPlan.SAISEN,SakuraTownPlan.CHOZU))for(int width=-1;width<=1;width++)
            check(visited.contains(fixture.east().offset(0,0,width)),"reachable full-width fixture front "+fixture);
        for(var entry:plan.entrySet()) {
            if(entry.getValue().is(Blocks.WATER))for(var d:List.of(Direction.DOWN,Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST)) {
                var boundary=plan.get(entry.getKey().relative(d));
                check(boundary!=null&&!boundary.isAir()&&!(boundary.getBlock() instanceof FenceBlock),"sealed canal at "+entry.getKey()+" "+d);
            }
            if(entry.getValue().is(Blocks.CHERRY_LEAVES))check(entry.getValue().getValue(LeavesBlock.PERSISTENT),"persistent blossom canopy");
            if(entry.getValue().is(Blocks.GRAVEL))check(plan.get(entry.getKey().below()).is(Blocks.STONE),"garden gravel cannot fall");
        }
        long air=plan.values().stream().filter(BlockState::isAir).count();
        System.out.println("SAKURA_PLAN_PASS entries="+plan.size()+" air="+air+" solidsAndWater="+(plan.size()-air)+" reachable="+visited.size()+" houses=8 shrine=73x113; deterministic, surveyed bounds, all doors/services reachable, sealed water, persistent leaves");
    }
}
