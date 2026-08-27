package net.jelly.echoesofwar.entity.apophis;

import net.jelly.marionette_lib.utility.FabrikAnimator;
import net.jelly.marionette_lib.utility.MarionettePart;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

// a FabrikAnimator for a worm: chain is a single forward pass from the head
// with every other segment trailing the one ahead of it, plus a hook to perturb each trailing
// segment before it settles.
public class ApophisChainAnimator extends FabrikAnimator {

    /** lets the owner perturb a trailing segment's position mid-solve (gravity, terrain conforming) */
    @FunctionalInterface
    public interface SegmentAdjuster {
        Vec3 adjust(int index, Vec3 naturalPosition);
    }

    /** the base class keeps its own private copy of this, with no accessor, so hold onto one here too */
    private final MarionettePart<?>[] parts;
    private SegmentAdjuster adjuster = (index, naturalPosition) -> naturalPosition;

    public ApophisChainAnimator(Entity owner, MarionettePart<?>[] parts) {
        super(owner, parts);
        this.parts = parts;
    }

    public void setSegmentAdjuster(SegmentAdjuster adjuster) {
        this.adjuster = adjuster;
    }

    @Override
    public void tickMultipart() {
        solveFromHead(getFabrikTarget());
        for (MarionettePart<?> part : parts) part.tick();
    }

    // solves head-to-tail: the head is set directly to the target, and every other segment aims for
    // its natural position then pertubed by SegmentAdjuster.
    private void solveFromHead(Vec3 target) {
        for (int i = parts.length - 1; i >= 0; i--) {
            MarionettePart<?> segment = parts[i];
            Vec3 behindEnd = (i == 0) ? root() : parts[i - 1].getEndPos();

            Vec3 endPos;
            if (i == parts.length - 1) {
                endPos = target;
            } else {
                MarionettePart<?> ahead = parts[i + 1];
                Vec3 anchor = ahead.getEndPos();
                double bond = ahead.getLength();
                Vec3 naturalPos = ahead.getRootPos();
                Vec3 desiredPos = adjuster.adjust(i, naturalPos);
                Vec3 aimOffset = desiredPos.subtract(anchor);
                endPos = aimOffset.lengthSqr() > 1.0E-6
                        ? anchor.add(aimOffset.normalize().scale(bond))
                        : naturalPos;
            }

            // direction has to be set before the position, since setEndPos() places the segment's center by walking half a length back along it
            Vec3 bone = endPos.subtract(behindEnd);
            if (bone.lengthSqr() > 1.0E-6) segment.setPartDirection(bone);
            segment.setEndPos(endPos);
        }
    }
}
