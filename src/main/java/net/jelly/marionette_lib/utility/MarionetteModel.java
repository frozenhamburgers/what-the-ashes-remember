package net.jelly.marionette_lib.utility;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Base model for a {@link Marionette} entity's renderer.
 * <p>
 * 26.1.2 port: {@code setupAnim} no longer receives the live entity, only an immutable
 * per-frame {@link EntityRenderState}. This class requires its state type {@code S} to implement
 * {@link MarionetteRenderState}; the owning entity's {@code EntityRenderer} populates it, typically
 * by overriding {@code extractRenderState} and copying {@link Marionette#getMarionettePartDirections()}
 * / {@link Marionette#getMarionettePartOffsets(net.minecraft.world.entity.Entity, float)} into it.
 */
public abstract class MarionetteModel<S extends EntityRenderState & MarionetteModel.MarionetteRenderState> extends EntityModel<S> {

    /** array of model parts corresponding to segment names */
    protected ModelPart[] allSegments;

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        return LayerDefinition.create(meshdefinition, 16, 16);
    }

    /**
     * @param root         the model's root part, as built from {@link #createBodyLayer()}. Every
     *                     name in {@code segmentNames} must be a (possibly nested) child of it -
     *                     {@code renderToBuffer} renders the whole tree starting from this root.
     * @param segmentNames names of the folders containing the geometry of each animatable segment
     *                     in Blockbench. The index of a name corresponds to the part entity at the
     *                     same index in the multipart entity's part list.
     */
    public MarionetteModel(ModelPart root, String[] segmentNames) {
        super(root);
        allSegments = new ModelPart[segmentNames.length];
        for (int i = 0; i < segmentNames.length; i++) {
            allSegments[i] = root.getChild(segmentNames[i]);
        }
    }

    // below this squared horizontal magnitude (~1.8 degrees off vertical), a direction is too close
    // to straight up/down to compute a trustworthy yaw from - see the loop below
    private static final double YAW_STABILITY_THRESHOLD_SQ = 0.001;

    @Override
    public void setupAnim(S state) {
        Vec3[] directions = state.marionettePartDirections();
        Vec3[] offsets = state.marionettePartOffsets();
        float previousYaw = 0f;
        for (int i = 0; i < allSegments.length; i++) {
            Vec3 dirVec = directions[i];

            // atan2(-x, z) is unstable near-vertical (tiny FABRIK noise can swing yaw up to 180
            // degrees frame to frame), so fall back to the previous segment's yaw in that case
            double horizontalSq = dirVec.x * dirVec.x + dirVec.z * dirVec.z;
            float yaw = horizontalSq > YAW_STABILITY_THRESHOLD_SQ
                    ? (float) Math.atan2(-dirVec.x, dirVec.z)
                    : previousYaw;
            previousYaw = yaw;

            float pitch = (float) Math.asin(Mth.clamp(dirVec.y, -1.0, 1.0));
            allSegments[i].setRotation(-pitch, yaw, 0f);

            // default part position is (0,24,0); 16 because 1 block is 16 units in model space
            Vec3 offset = offsets[i];
            allSegments[i].setPos((float) (16f * offset.x), (float) (24 - 16f * offset.y), (float) (-16f * offset.z));
        }
    }

    /**
     * Implemented by a creature's {@link EntityRenderState} subclass so {@link MarionetteModel} can
     * read per-segment direction/offset data without the live entity. Populate both arrays from an
     * overridden {@code extractRenderState}, e.g.:
     * <pre>{@code
     * @Override
     * public void extractRenderState(WormEntity entity, WormRenderState state, float partialTick) {
     *     super.extractRenderState(entity, state, partialTick);
     *     state.partDirections = entity.getMarionettePartDirections();
     *     state.partOffsets = entity.getMarionettePartOffsets(entity, partialTick);
     * }
     * }</pre>
     */
    public interface MarionetteRenderState {
        Vec3[] marionettePartDirections();

        Vec3[] marionettePartOffsets();
    }
}
