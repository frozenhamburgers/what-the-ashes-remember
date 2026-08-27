package net.jelly.echoesofwar.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.jelly.marionette_lib.utility.Marionette;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// ensure Marionette entities can still render even if their base entity is supposed to be culled
@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @ModifyExpressionValue(
            method = "extractVisibleEntities",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;isSectionCompiledAndVisible(Lnet/minecraft/core/BlockPos;)Z"
            )
    )
    private boolean echoesofwar$renderMarionettesFromAnyAnchorSection(boolean sectionVisible, @Local Entity entity) {
        return sectionVisible || entity instanceof Marionette;
    }
}
