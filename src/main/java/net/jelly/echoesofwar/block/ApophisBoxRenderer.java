package net.jelly.echoesofwar.block;

import com.geckolib.renderer.GeoBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

public class ApophisBoxRenderer extends GeoBlockRenderer<ApophisBoxBlockEntity, BlockEntityRenderState> {
    public ApophisBoxRenderer(BlockEntityRendererProvider.Context context) {
        super(context, new ApophisBoxGeoModel());
    }
}
