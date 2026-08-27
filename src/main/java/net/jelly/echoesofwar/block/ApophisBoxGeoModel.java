package net.jelly.echoesofwar.block;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.jelly.echoesofwar.EchoesofWar;
import net.minecraft.resources.Identifier;

public class ApophisBoxGeoModel extends GeoModel<ApophisBoxBlockEntity> {
    private static final Identifier MODEL = Identifier.fromNamespaceAndPath(EchoesofWar.MODID, "block/pandoras_box");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(EchoesofWar.MODID, "textures/block/apophis_pandoras_box.png");
    private static final Identifier ANIMATIONS = Identifier.fromNamespaceAndPath(EchoesofWar.MODID, "block/pandora_open");
    private static final Identifier ANIMATIONS_CLOSE = Identifier.fromNamespaceAndPath(EchoesofWar.MODID, "block/pandora_close");

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(ApophisBoxBlockEntity animatable) {
        return ANIMATIONS;
    }

    @Override
    public Identifier[] getAnimationResourceFallbacks(ApophisBoxBlockEntity animatable) {
        return new Identifier[] { ANIMATIONS_CLOSE };
    }
}
