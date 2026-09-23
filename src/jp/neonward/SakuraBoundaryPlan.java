package jp.neonward;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Separate, pure perimeter migration; never append this to SakuraTown.blueprint().
 * No world access, registration, terrain generation, entities, or placement side effects.
 *
 * Baseline: completed original SakuraTown (ground64, cleared65..70), overlaid with
 * SakuraTownPlan. The raised west foundation67/fascia68 already exists. Surveyed
 * higher cells without authored geometry are air. expectedBaseline() is an exact
 * preflight expectation, NOT permission to replace arbitrary natural/material blocks.
 * Main must preserve block entities and unexpected/player-modified cells, verify the
 * unchanged entrance/tunnel separately, and persist this migration independently.
 */
public final class SakuraBoundaryPlan {
    public static final int MIN_X=SakuraTownPlan.MIN_X, MAX_X=SakuraTownPlan.MAX_X;
    public static final int MIN_Z=SakuraTownPlan.MIN_Z, MAX_Z=SakuraTownPlan.MAX_Z;
    public static final int GATE_MIN_Z=366, GATE_MAX_Z=370;
    public static final int GROUND_Y=64, RAISED_Y=67;
    public static final int RAISED_MIN_Z=312, RAISED_MAX_Z=424;
    public static final int MIN_Y=64, MAX_Y=72, EXPECTED_CELLS=3826;
    private static final int TRANSITION_LENGTH=5;

    private SakuraBoundaryPlan() {}

    /** Five-wide east mouth; no floor/air/ceiling edits here, preserving the existing tunnel access. */
    public static boolean opening(int x,int z) {
        return x==MAX_X&&z>=GATE_MIN_Z&&z<=GATE_MAX_Z;
    }

    public static boolean perimeter(int x,int z) {
        return x>=MIN_X&&x<=MAX_X&&z>=MIN_Z&&z<=MAX_Z
            &&(x==MIN_X||x==MAX_X||z==MIN_Z||z==MAX_Z);
    }

    /** Existing load-bearing surface, not a new raised floor. Lower fascia64..66 remains untouched. */
    public static int baseY(int x,int z) {
        return x==MIN_X&&z>=RAISED_MIN_Z&&z<=RAISED_MAX_Z?RAISED_Y:GROUND_Y;
    }

    private static boolean transition(int x,int z) {
        return x==MIN_X&&((z>=RAISED_MIN_Z-TRANSITION_LENGTH&&z<RAISED_MIN_Z)
            ||(z>RAISED_MAX_Z&&z<=RAISED_MAX_Z+TRANSITION_LENGTH));
    }

    private static boolean gatePost(int x,int z) {
        return x==MAX_X&&(z==GATE_MIN_Z-1||z==GATE_MAX_Z+1);
    }

    /** Tile underside. Normal wall has four full solid cells above its supporting surface. */
    public static int copingY(int x,int z) {
        if(gatePost(x,z))return 71; // Flush solid join to tunnel side walls65..70 at x=-63.
        return baseY(x,z)+4+(transition(x,z)?1:0);
    }

    public static Map<BlockPos,BlockState> build() {
        var plan=new LinkedHashMap<BlockPos,BlockState>();
        for(int x=MIN_X;x<=MAX_X;x++)for(int z=MIN_Z;z<=MAX_Z;z++) {
            if(!perimeter(x,z)||opening(x,z))continue;
            int base=baseY(x,z),cap=copingY(x,z);
            boolean corner=(x==MIN_X||x==MAX_X)&&(z==MIN_Z||z==MAX_Z);
            boolean pier=corner||gatePost(x,z)
                ||(x==MIN_X||x==MAX_X?(z-MIN_Z)%8==0:(x-MIN_X)%8==0)
                ||x==MIN_X&&(z==RAISED_MIN_Z||z==RAISED_MAX_Z);
            put(plan,x,base,z,material("stone_paving"));
            for(int y=base+1;y<cap;y++) {
                String material=pier?"dark_timber":y==base+1?"stone_paving":"plaster";
                put(plan,x,y,z,material(material));
            }
            // Full tile seals the wall; narrow ridge provides native Japanese tiled coping.
            put(plan,x,cap,z,material("kawara_tile"));
            Direction outward=x==MIN_X?Direction.WEST:x==MAX_X?Direction.EAST:z==MIN_Z?Direction.NORTH:Direction.SOUTH;
            put(plan,x,cap+1,z,corner?material("kawara_tile"):SakuraMaterials.facing("sakura_kawara_ridge",outward));
        }
        if(plan.size()!=EXPECTED_CELLS)throw new IllegalStateException("Unexpected Sakura perimeter geometry: "+plan.size());
        return Collections.unmodifiableMap(plan);
    }

    /** Same keys as build(). Accept desired state too for resumable installation; never broad material whitelists.
     * Original street paving at the gate posts and west fascia are resolved from the authored plan.
     * Main still needs an independent migration receipt/fingerprint and block-entity checks.
     */
    public static Map<BlockPos,BlockState> expectedBaseline() {
        var authored=SakuraTownPlan.build();
        var expected=new LinkedHashMap<BlockPos,BlockState>();
        for(var at:build().keySet()) {
            var prior=authored.get(at);
            if(prior==null)prior=(at.getY()==GROUND_Y?Blocks.GRASS_BLOCK:Blocks.AIR).defaultBlockState();
            expected.put(at,prior);
        }
        return Collections.unmodifiableMap(expected);
    }

    private static BlockState material(String id) {return SakuraMaterials.get("sakura_"+id).defaultBlockState();}

    private static void put(Map<BlockPos,BlockState> plan,int x,int y,int z,BlockState state) {
        if(!perimeter(x,z)||opening(x,z)||y<MIN_Y||y>MAX_Y)
            throw new IllegalArgumentException("Out-of-scope Sakura boundary cell: "+x+","+y+","+z);
        plan.put(new BlockPos(x,y,z),state);
    }
}
