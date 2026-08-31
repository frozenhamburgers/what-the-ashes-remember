package net.jelly.echoesofwar.block;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.jelly.echoesofwar.EchoesofWar;
import net.minecraft.resources.Identifier;

public class CrucibleOfCalamityGeoModel extends GeoModel<CrucibleOfCalamityBlockEntity> {
    private static final Identifier MODEL = Identifier.fromNamespaceAndPath(EchoesofWar.MODID, "block/crucible_of_calamity");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(EchoesofWar.MODID, "textures/block/crucible_of_calamity.png");
    private static final Identifier ANIMATIONS = Identifier.fromNamespaceAndPath(EchoesofWar.MODID, "block/pulley");

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(CrucibleOfCalamityBlockEntity animatable) {
        return ANIMATIONS;
    }
}
