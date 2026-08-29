package net.jelly.echoesofwar.block;

import com.geckolib.renderer.GeoBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

public class CrucibleOfCalamityRenderer extends GeoBlockRenderer<CrucibleOfCalamityBlockEntity, BlockEntityRenderState> {
    public CrucibleOfCalamityRenderer(BlockEntityRendererProvider.Context context) {
        super(context, new CrucibleOfCalamityGeoModel());
    }
}
