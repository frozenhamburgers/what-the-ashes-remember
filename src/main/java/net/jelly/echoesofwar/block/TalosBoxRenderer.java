package net.jelly.echoesofwar.block;

import com.geckolib.renderer.GeoBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

public class TalosBoxRenderer extends GeoBlockRenderer<TalosBoxBlockEntity, BlockEntityRenderState> {
    public TalosBoxRenderer(BlockEntityRendererProvider.Context context) {
        super(context, new TalosBoxGeoModel());
    }
}
