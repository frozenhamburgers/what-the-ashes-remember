package net.jelly.echoesofwar.worldgen;

import net.jelly.echoesofwar.EchoesofWar;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

// places containment_arena.nbt template unrotated
public class ContainmentArenaPiece extends TemplateStructurePiece {
    private static final Identifier TEMPLATE = Identifier.fromNamespaceAndPath(EchoesofWar.MODID, "containment_arena");

    public ContainmentArenaPiece(StructureTemplateManager structureTemplateManager, BlockPos position) {
        super(ModWorldgen.CONTAINMENT_ARENA_PIECE.get(), 0, structureTemplateManager, TEMPLATE, TEMPLATE.toString(), makeSettings(), position);
    }

    public ContainmentArenaPiece(StructureTemplateManager structureTemplateManager, CompoundTag tag) {
        super(ModWorldgen.CONTAINMENT_ARENA_PIECE.get(), tag, structureTemplateManager, location -> makeSettings());
    }

    private static StructurePlaceSettings makeSettings() {
        return new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE);
    }

    @Override
    protected void handleDataMarker(String markerId, BlockPos position, ServerLevelAccessor level, RandomSource random, BoundingBox chunkBB) {
    }
}
