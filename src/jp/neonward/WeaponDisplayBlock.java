package jp.neonward;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class WeaponDisplayBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public WeaponDisplayBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return simpleCodec(WeaponDisplayBlock::new); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext c) {
        return c.getPlayer() != null && HomeBuildingRules.canPlace(c, this)
            ? defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite()) : null;
    }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return Shapes.or(Block.box(1, 0, 1, 15, 3, 15), Block.box(4, 3, 4, 12, 12, 12), Block.box(1, 12, 1, 15, 15, 15));
    }
    @Override public BlockEntity newBlockEntity(BlockPos p, BlockState s) { return new WeaponDisplayEntity(p, s); }
    @Override public void setPlacedBy(Level l, BlockPos pos, BlockState s, LivingEntity placer, ItemStack stack) {
        if (!l.isClientSide() && placer instanceof Player p && l.getBlockEntity(pos) instanceof WeaponDisplayEntity display)
            display.assignOwner(p.getUUID());
    }
    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState s, Level l, BlockPos pos, Player p, InteractionHand hand, BlockHitResult hit) {
        return interact(l, pos, p, hand);
    }
    @Override protected InteractionResult useWithoutItem(BlockState s, Level l, BlockPos pos, Player p, BlockHitResult hit) {
        return interact(l, pos, p, InteractionHand.MAIN_HAND);
    }
    private InteractionResult interact(Level l, BlockPos pos, Player p, InteractionHand hand) {
        if (!l.isClientSide() && l.getBlockEntity(pos) instanceof WeaponDisplayEntity display) display.interact(p, hand);
        // Consume interaction even on rejection: never fire a held gun or open another UI here.
        return InteractionResult.SUCCESS;
    }
    @Override public net.minecraft.network.chat.MutableComponent getName() { return Component.literal("武器展示台"); }
}
