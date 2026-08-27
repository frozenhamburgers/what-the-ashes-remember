package net.jelly.marionette_lib.utility;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * A chain of {@link MarionettePart} segments driven by a {@link FabrikAnimator}.
 * Build one with {@link #builder(Entity, PartFactory)} instead of hand-constructing
 * a part array and animator; both stay available directly via {@link #parts()} and
 * {@link #animator()} for anything the builder doesn't cover.
 */
public class Limb<T extends MarionettePart<?>> {
    private final T[] parts;
    private final FabrikAnimator animator;

    private Limb(T[] parts, FabrikAnimator animator) {
        this.parts = parts;
        this.animator = animator;
    }

    public T[] parts() {
        return parts;
    }

    public FabrikAnimator animator() {
        return animator;
    }

    /** Builds a chain of plain {@link MarionettePart}s — the common case, no subclass needed. */
    public static <P extends Entity> Builder<P, MarionettePart<P>> builder(P parent) {
        PartFactory<P, MarionettePart<P>> factory = MarionettePart::new;
        return new Builder<>(parent, factory);
    }

    /** Builds a chain of a custom {@link MarionettePart} subclass, for parts with non-default behavior. */
    public static <P extends Entity, T extends MarionettePart<P>> Builder<P, T> builder(P parent, PartFactory<P, T> factory) {
        return new Builder<>(parent, factory);
    }

    @FunctionalInterface
    public interface PartFactory<P extends Entity, T extends MarionettePart<P>> {
        T create(P parent, float sizeXZ, float sizeY, float length);
    }

    public static class Builder<P extends Entity, T extends MarionettePart<P>> {
        private final P parent;
        private final PartFactory<P, T> factory;
        private final List<T> parts = new ArrayList<>();
        private boolean followRootOnly = false;
        private Vec3 root = null;

        private Builder(P parent, PartFactory<P, T> factory) {
            this.parent = parent;
            this.factory = factory;
        }

        /** Appends {@code count} segments sharing the same dimensions. */
        public Builder<P, T> segments(int count, float sizeXZ, float sizeY, float length) {
            for (int i = 0; i < count; i++) segment(sizeXZ, sizeY, length);
            return this;
        }

        /** Appends a single segment. Call repeatedly (mixed with {@link #segments}) for tapering or irregular chains. */
        public Builder<P, T> segment(float sizeXZ, float sizeY, float length) {
            parts.add(factory.create(parent, sizeXZ, sizeY, length));
            return this;
        }

        /** See {@link FabrikAnimator#setFollowRootOnly(boolean)}. */
        public Builder<P, T> followRootOnly(boolean followRootOnly) {
            this.followRootOnly = followRootOnly;
            return this;
        }

        /** See {@link FabrikAnimator#setRoot(Vec3)}. */
        public Builder<P, T> root(Vec3 root) {
            this.root = root;
            return this;
        }

        @SuppressWarnings("unchecked")
        public Limb<T> build() {
            if (parts.isEmpty()) throw new IllegalStateException("Limb must have at least one segment");
            T[] array = parts.toArray((T[]) Array.newInstance(parts.get(0).getClass(), parts.size()));
            FabrikAnimator animator = new FabrikAnimator(parent, array);
            animator.setFollowRootOnly(followRootOnly);
            if (root != null) animator.setRoot(root);
            return new Limb<>(array, animator);
        }
    }
}
