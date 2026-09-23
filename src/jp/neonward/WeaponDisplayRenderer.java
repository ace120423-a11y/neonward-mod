package jp.neonward;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.*;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.Vec3;

public final class WeaponDisplayRenderer implements BlockEntityRenderer<WeaponDisplayEntity, WeaponDisplayRenderer.State> {
    private final ItemModelResolver resolver;
    public WeaponDisplayRenderer(BlockEntityRendererProvider.Context context) { resolver = context.itemModelResolver(); }
    public static void init() { BlockEntityRendererRegistry.register(WeaponDisplay.TYPE, WeaponDisplayRenderer::new); }
    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState item = new ItemStackRenderState();
        float rotation;
    }
    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(WeaponDisplayEntity entity, State state, float partial, Vec3 camera, ModelFeatureRenderer.CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partial, camera, overlay);
        state.rotation = entity.getBlockState().getValue(HorizontalDirectionalBlock.FACING).toYRot();
        // Full stack: custom model, enchantments, damage, attachment components, and resource-pack predicates.
        resolver.updateForTopItem(state.item, entity.displayedStack(), ItemDisplayContext.FIXED, entity.getLevel(), null, entity.getBlockPos().hashCode());
    }
    @Override public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(.5, 1.45, .5);
        pose.mulPose(Axis.YP.rotationDegrees(-state.rotation));
        pose.scale(.85f, .85f, .85f);
        state.item.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }
}
