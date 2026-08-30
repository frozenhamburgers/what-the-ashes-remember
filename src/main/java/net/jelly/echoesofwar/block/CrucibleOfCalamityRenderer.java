package net.jelly.echoesofwar.block;

import com.geckolib.renderer.GeoBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.phys.AABB;

public class CrucibleOfCalamityRenderer extends GeoBlockRenderer<CrucibleOfCalamityBlockEntity, BlockEntityRenderState> {
    // matches the geckolib model's actual extent: pivot at the controller block, cube spanning
    // +-24 (1.5 blocks) horizontally and 0-80 (5 blocks) vertically in the geo model's 1/16-block units
    private static final AABB LOCAL_RENDER_BOUNDING_BOX = new AABB(-15, 0, -15, 15, 50, 15);

    public CrucibleOfCalamityRenderer(BlockEntityRendererProvider.Context context) {
        super(context, new CrucibleOfCalamityGeoModel());
    }

    // the geckolib model extends well beyond this block's own 1x1x1 cell, so it must not be
    // culled based on the section its controller block sits in (see BeaconRenderer for the
    // same pattern with its beam)
    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    // NeoForge frustum-culls block entity renderers individually using this bounding box (even
    // when shouldRenderOffScreen() is true), defaulting to the controller's own unit cube - it
    // must be widened to the model's real extent or the model still vanishes whenever the
    // controller cell itself leaves the frustum
    @Override
    public AABB getRenderBoundingBox(CrucibleOfCalamityBlockEntity blockEntity) {
        return LOCAL_RENDER_BOUNDING_BOX.move(blockEntity.getBlockPos());
    }
}
