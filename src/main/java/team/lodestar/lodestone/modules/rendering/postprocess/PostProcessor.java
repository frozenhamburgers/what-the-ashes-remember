package team.lodestar.lodestone.modules.rendering.postprocess;

import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.client.renderer.PostChainConfig;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.UniformValue;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.StrictJsonParser;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import team.lodestar.lodestone.LodestoneLib;

import java.io.IOException;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * Abstract world-space post-process pass. Ported from Lodestone 1.21.
 * <p>
 * Config lives at {@code assets/<namespace>/shaders/post/<path>.json} but must use the new
 * {@code PostChainConfig} JSON schema, not the old Lodestone/vanilla post-chain schema - not
 * interchangeable. To read the world-space depth buffer, reference target "depth_main" in the
 * JSON; it's autopopulated from the main render target before passes run. Identifier paths
 * must be lowercase (e.g. depth_main, not depthMain).
 */
public abstract class PostProcessor {
    private static final String DEFAULT_UNIFORMS_NAME = "LodestoneDefaults";
    private static final String SAMPLER_INFO_NAME = "SamplerInfo";
    private static final Identifier MAIN_TARGET_ID = Identifier.withDefaultNamespace("main");
    private static final Identifier DEPTH_MAIN_TARGET_ID = Identifier.withDefaultNamespace("depth_main");

    /**
     * The current frame's world-to-view rotation matrix, updated in {@link #applyPostProcess()}
     * from {@link Camera#getViewRotationMatrix(Matrix4f)}. Lodestone 1.21 instead captured
     * {@code RenderSystem.getModelViewMatrix()} via a separate render-stage listener, which
     * read from a shared matrix stack with no defined ordering and drifted as the camera moved.
     */
    public static Matrix4f viewModelMatrix;

    private boolean initialized = false;
    private boolean isActive = true;
    protected double time;

    private PostChainConfig config;
    private final List<Pass> passes = new ArrayList<>();
    private final Map<Identifier, RenderTarget> internalTargets = new HashMap<>();
    private RenderTarget tempDepthBuffer;
    private GpuBuffer defaultUniformsBuffer;
    private int lastWidth = -1;
    private int lastHeight = -1;
    /** factors the scaled targets were last sized with, so a config change forces a resize */
    private Map<Identifier, Float> lastScaledFactors = Map.of();

    /**
     * Example: "mymod:foo" points to assets/mymod/shaders/post/foo.json
     */
    public abstract Identifier getPostChainLocation();

    public void init() {
        loadPostChain();
        initialized = true;
    }

    public final void loadPostChain() {
        closeChain();

        Minecraft mc = Minecraft.getInstance();
        Identifier base = getPostChainLocation();
        Identifier file = base.withPath(path -> "shaders/post/" + path + ".json");

        try {
            Resource resource = mc.getResourceManager().getResourceOrThrow(file);
            try (Reader reader = resource.openAsReader()) {
                JsonElement json = StrictJsonParser.parse(reader);
                config = PostChainConfig.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(JsonSyntaxException::new);
            }
        } catch (IOException | RuntimeException e) {
            LodestoneLib.LOGGER.error("Failed to load post-processing shader {}: ", file, e);
            config = null;
            return;
        }

        defaultUniformsBuffer = RenderSystem.getDevice()
                .createBuffer(() -> "Lodestone Default Uniforms", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, 256);

        int width = mc.getWindow().getWidth();
        int height = mc.getWindow().getHeight();
        Map<Identifier, Float> scaled = resolveScaledTargets();
        for (Map.Entry<Identifier, PostChainConfig.InternalTarget> entry : config.internalTargets().entrySet()) {
            RenderTarget target = new SimpleRenderTarget(entry.getKey().toString());
            int[] size = targetSize(entry.getKey(), width, height, scaled);
            target.createBuffers(size[0], size[1]);
            internalTargets.put(entry.getKey(), target);
        }

        if (!internalTargets.containsKey(DEPTH_MAIN_TARGET_ID) && referencesDepthMain()) {
            tempDepthBuffer = new SimpleRenderTarget("depth_main");
            tempDepthBuffer.createBuffers(width, height);
            internalTargets.put(DEPTH_MAIN_TARGET_ID, tempDepthBuffer);
        } else {
            tempDepthBuffer = internalTargets.get(DEPTH_MAIN_TARGET_ID);
        }

        for (PostChainConfig.Pass passConfig : config.passes()) {
            passes.add(new Pass(passConfig));
        }

        lastWidth = width;
        lastHeight = height;
        lastScaledFactors = scaled;
    }

    private boolean referencesDepthMain() {
        return config.passes().stream().flatMap(PostChainConfig.Pass::referencedTargets).anyMatch(DEPTH_MAIN_TARGET_ID::equals);
    }

    private void closeChain() {
        passes.forEach(Pass::close);
        passes.clear();
        internalTargets.values().forEach(RenderTarget::destroyBuffers);
        internalTargets.clear();
        tempDepthBuffer = null;
        if (defaultUniformsBuffer != null) {
            defaultUniformsBuffer.close();
            defaultUniformsBuffer = null;
        }
    }

    /**
     * Copies the main render target's depth buffer into "depth_main", if referenced. Called once
     * per frame, before any processor's {@link #applyPostProcess()}.
     * <p>
     * Resize must happen here rather than only in {@link #applyPostProcess()}: this runs first,
     * so on the frame the window grows the depth target would still be the old size and the copy
     * would throw.
     */
    public void copyDepthBuffer() {
        if (isActive && tempDepthBuffer != null) {
            resizeIfNeeded();
            tempDepthBuffer.copyDepthFrom(Minecraft.getInstance().getMainRenderTarget());
        }
    }

    /**
     * Called when the client level unloads. Processors are long-lived singletons, so anything
     * held on behalf of that level must be cleared here or it keeps rendering into the next one.
     */
    public void onClientLevelUnload() {
    }

    public void resize(int width, int height) {
        if (config == null) return;
        Map<Identifier, Float> scaled = resolveScaledTargets();
        for (Map.Entry<Identifier, RenderTarget> entry : internalTargets.entrySet()) {
            int[] size = targetSize(entry.getKey(), width, height, scaled);
            entry.getValue().resize(size[0], size[1]);
        }
        lastWidth = width;
        lastHeight = height;
        lastScaledFactors = scaled;
    }

    // recomputed rather than cached, so resizeIfNeeded() can detect a config-driven factor change
    private Map<Identifier, Float> resolveScaledTargets() {
        Map<String, Float> declared = getScaledTargets();
        if (declared.isEmpty()) return Map.of();
        Map<Identifier, Float> resolved = new HashMap<>();
        declared.forEach((name, factor) -> resolved.put(Identifier.parse(name), Mth.clamp(factor, 0.05f, 1.0f)));
        return resolved;
    }

    // explicit width in the json wins; otherwise a target named by getScaledTargets() tracks its
    // fraction of the window, and everything else (including the depth copy) matches it exactly
    private int[] targetSize(Identifier id, int windowWidth, int windowHeight, Map<Identifier, Float> scaled) {
        PostChainConfig.InternalTarget targetConfig = config.internalTargets().get(id);
        if (targetConfig != null && targetConfig.width().isPresent()) {
            return new int[]{targetConfig.width().get(), targetConfig.height().orElse(windowHeight)};
        }
        Float factor = scaled.get(id);
        if (factor != null) {
            return new int[]{Math.max(1, Math.round(windowWidth * factor)),
                    Math.max(1, Math.round(windowHeight * factor))};
        }
        return new int[]{windowWidth, windowHeight};
    }

    /**
     * Internal targets rendered below window resolution, as target name to fraction of the window.
     * Empty by default (every target tracks the window).
     * <p>
     * Names, not {@link Identifier}s: the json's keys parse through {@code Identifier.CODEC},
     * which gives a bare name the default namespace, so handing back an id built with the mod's
     * own namespace would silently match nothing. Parse the names the same way here to avoid that.
     * <p>
     * Replaces Lodestone 1.21's single {@code getScaledTargetName()}/{@code
     * getScaledTargetFactor()} pair, which could only describe one sub-resolution target.
     */
    protected Map<String, Float> getScaledTargets() {
        return Map.of();
    }

    // vanilla no longer fires a window-resize event this can hook without its own mixin, so
    // internal targets are resized lazily here on the first frame at a new window size
    private void resizeIfNeeded() {
        Minecraft mc = Minecraft.getInstance();
        int width = mc.getWindow().getWidth();
        int height = mc.getWindow().getHeight();
        if (width != lastWidth || height != lastHeight || !resolveScaledTargets().equals(lastScaledFactors)) {
            resize(width, height);
        }
    }

    private void updateDefaultUniforms() {
        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer.getMainCamera();
        Matrix4f invView = new Matrix4f(viewModelMatrix).invert();
        float aspectRatio = (float) mc.getWindow().getWidth() / (float) mc.getWindow().getHeight();
        float farPlane = getDepthFar(camera);

        // RenderSystem no longer exposes the projection matrix as a plain Matrix4f (only as an
        // opaque GpuBufferSlice via getProjectionMatrixBuffer()), so it's rebuilt here the same
        // way net.minecraft.client.renderer.Projection does internally.
        Matrix4f projection = new Matrix4f()
                .setPerspective(
                        (float) Math.toRadians(camera.getFov()),
                        aspectRatio,
                        Camera.PROJECTION_Z_NEAR,
                        farPlane,
                        RenderSystem.getDevice().isZZeroToOne()
                );
        Matrix4f invProj = projection.invert();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            Std140Builder builder = Std140Builder.onStack(stack, 256);
            builder.putVec3(camera.position().toVector3f());
            builder.putVec3(camera.forwardVector());
            builder.putVec3(camera.upVector());
            builder.putVec3(camera.leftVector());
            builder.putMat4f(invView);
            builder.putMat4f(invProj);
            // Note: view-bob compensation from the original Lodestone LodestoneRenderSystem
            // is not ported (out of scope) - bobOffset is always zero here.
            builder.putVec3(0F, 0F, 0F);
            builder.putFloat((float) time);
            builder.putFloat(Camera.PROJECTION_Z_NEAR);
            builder.putFloat(farPlane);
            builder.putFloat((float) Math.toRadians(camera.getFov()));
            builder.putFloat(aspectRatio);
            ByteBuffer bytes = builder.get();
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(defaultUniformsBuffer.slice(0, bytes.remaining()), bytes);
        }
    }

    /**
     * {@code Camera.depthFar} has no vanilla getter - requires an access transformer
     * (see {@code META-INF/accesstransformer.cfg}: {@code public net.minecraft.client.Camera depthFar}).
     */
    private static float getDepthFar(Camera camera) {
        return camera.depthFar;
    }

    public void applyPostProcess() {
        if (!isActive) return;
        if (!initialized) init();
        if (config == null) return;

        Minecraft mc = Minecraft.getInstance();
        float partialTicks = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        time += partialTicks / 20.0;

        viewModelMatrix = mc.gameRenderer.getMainCamera().getViewRotationMatrix(new Matrix4f());

        resizeIfNeeded();
        beforeProcess(viewModelMatrix);
        if (!isActive) return;

        updateDefaultUniforms();
        for (Pass pass : passes) {
            pass.execute();
        }

        afterProcess();
    }

    /**
     * Set any per-frame instance/effect state here (e.g. upload data buffers). Uniforms
     * shared by every pass (time, camera, matrices) are handled automatically.
     */
    public abstract void beforeProcess(Matrix4f viewModelMatrix);

    public abstract void afterProcess();

    /**
     * Subclasses (e.g. {@link MultiInstancePostProcessor}) override this to declare
     * additional per-pipeline uniforms before a pass's {@link RenderPipeline} is built.
     */
    protected void declareExtraUniforms(RenderPipeline.Builder builder) {
    }

    /**
     * Subclasses override this to bind the values for any uniforms declared in
     * {@link #declareExtraUniforms(RenderPipeline.Builder)}.
     */
    protected void bindExtraUniforms(RenderPass renderPass) {
    }

    public void setActive(boolean active) {
        this.isActive = active;
        if (!active) time = 0.0;
    }

    public final boolean isActive() {
        return isActive;
    }

    private RenderTarget resolveTarget(Identifier id) {
        if (id.equals(MAIN_TARGET_ID)) {
            return Minecraft.getInstance().getMainRenderTarget();
        }
        RenderTarget external = resolveExternalTargets().get(id);
        if (external != null) {
            return external;
        }
        RenderTarget target = internalTargets.get(id);
        if (target == null) {
            throw new IllegalStateException("Missing post-process target " + id);
        }
        return target;
    }

    /**
     * Targets the chain may sample but does not own (e.g. a buffer written by world rendering).
     * Never created/resized/destroyed by this class - the supplier owns their lifetime. Must NOT
     * also appear under {@code "targets"} in the chain's json, or the chain allocates a second,
     * unrelated buffer of the same name.
     * <p>
     * Keys are parsed with {@link Identifier#parse} like {@link #getScaledTargets()}'s - a bare
     * name lands in the {@code minecraft} namespace, so give these an explicit one.
     */
    protected Map<String, RenderTarget> getExternalTargets() {
        return Map.of();
    }

    private Map<Identifier, RenderTarget> resolveExternalTargets() {
        Map<String, RenderTarget> declared = getExternalTargets();
        if (declared.isEmpty()) return Map.of();
        Map<Identifier, RenderTarget> resolved = new HashMap<>();
        declared.forEach((name, target) -> resolved.put(Identifier.parse(name), target));
        return resolved;
    }

    private record ResolvedInput(String samplerName, GpuTextureView view, boolean bilinear) {
    }

    private ResolvedInput resolveInput(PostChainConfig.Input input) {
        return switch (input) {
            case PostChainConfig.TargetInput targetInput -> {
                RenderTarget target = resolveTarget(targetInput.targetId());
                GpuTextureView view = targetInput.useDepthBuffer() ? target.getDepthTextureView() : target.getColorTextureView();
                if (view == null) {
                    throw new IllegalStateException("Missing " + (targetInput.useDepthBuffer() ? "depth" : "color") + " texture for target " + targetInput.targetId());
                }
                yield new ResolvedInput(targetInput.samplerName(), view, targetInput.bilinear());
            }
            case PostChainConfig.TextureInput textureInput -> {
                AbstractTexture texture = Minecraft.getInstance()
                        .getTextureManager()
                        .getTexture(textureInput.location().withPath(path -> "textures/effect/" + path + ".png"));
                yield new ResolvedInput(textureInput.samplerName(), texture.getTextureView(), textureInput.bilinear());
            }
        };
    }

    private static class SimpleRenderTarget extends RenderTarget {
        SimpleRenderTarget(String label) {
            super(label, true);
        }
    }

    private class Pass {
        private final RenderPipeline pipeline;
        private final Identifier outputTargetId;
        private final List<PostChainConfig.Input> inputs;
        private final Map<String, GpuBuffer> staticUniforms = new HashMap<>();
        private final GpuBuffer samplerInfoBuffer;

        Pass(PostChainConfig.Pass passConfig) {
            RenderPipeline.Builder builder = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
                    .withLocation(getPostChainLocation().withSuffix("/" + passes.size()))
                    .withVertexShader(passConfig.vertexShaderId())
                    .withFragmentShader(passConfig.fragmentShaderId());

            for (PostChainConfig.Input input : passConfig.inputs()) {
                builder.withSampler(input.samplerName() + "Sampler");
            }

            builder.withUniform(DEFAULT_UNIFORMS_NAME, UniformType.UNIFORM_BUFFER);
            builder.withUniform(SAMPLER_INFO_NAME, UniformType.UNIFORM_BUFFER);
            for (String uniformGroupName : passConfig.uniforms().keySet()) {
                builder.withUniform(uniformGroupName, UniformType.UNIFORM_BUFFER);
            }
            declareExtraUniforms(builder);

            this.pipeline = builder.build();
            this.outputTargetId = passConfig.outputTarget();
            this.inputs = passConfig.inputs();
            // vec2 OutSize + one vec2 InSize per input, matching vanilla PostPass's own
            // "SamplerInfo" uniform block convention, refreshed every execute() since target
            // sizes can change across resizes without the pass itself being rebuilt.
            this.samplerInfoBuffer = RenderSystem.getDevice()
                    .createBuffer(() -> pipeline.getLocation() + " / SamplerInfo", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, 16L + inputs.size() * 16L);

            for (Map.Entry<String, List<UniformValue>> entry : passConfig.uniforms().entrySet()) {
                List<UniformValue> values = entry.getValue();
                if (values.isEmpty()) continue;

                Std140SizeCalculator calculator = new Std140SizeCalculator();
                for (UniformValue value : values) value.addSize(calculator);

                try (MemoryStack stack = MemoryStack.stackPush()) {
                    Std140Builder builder2 = Std140Builder.onStack(stack, calculator.get());
                    for (UniformValue value : values) value.writeTo(builder2);
                    ByteBuffer bytes = builder2.get();
                    GpuBuffer buffer = RenderSystem.getDevice()
                            .createBuffer(() -> pipeline.getLocation() + " / " + entry.getKey(), GpuBuffer.USAGE_UNIFORM, bytes);
                    staticUniforms.put(entry.getKey(), buffer);
                }
            }
        }

        void execute() {
            RenderTarget outputTarget = resolveTarget(outputTargetId);
            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            List<ResolvedInput> resolvedInputs = inputs.stream().map(PostProcessor.this::resolveInput).toList();

            try (MemoryStack stack = MemoryStack.stackPush()) {
                Std140Builder infoBuilder = Std140Builder.onStack(stack, 16 + resolvedInputs.size() * 16);
                infoBuilder.putVec2(outputTarget.width, outputTarget.height);
                for (ResolvedInput input : resolvedInputs) {
                    infoBuilder.putVec2(input.view().getWidth(0), input.view().getHeight(0));
                }
                ByteBuffer infoBytes = infoBuilder.get();
                encoder.writeToBuffer(samplerInfoBuffer.slice(0, infoBytes.remaining()), infoBytes);
            }

            try (RenderPass renderPass = encoder.createRenderPass(
                    () -> "Lodestone post pass",
                    outputTarget.getColorTextureView(),
                    OptionalInt.empty(),
                    outputTarget.useDepth ? outputTarget.getDepthTextureView() : null,
                    OptionalDouble.empty()
            )) {
                renderPass.setPipeline(pipeline);
                renderPass.setUniform(DEFAULT_UNIFORMS_NAME, defaultUniformsBuffer);
                renderPass.setUniform(SAMPLER_INFO_NAME, samplerInfoBuffer);
                for (Map.Entry<String, GpuBuffer> entry : staticUniforms.entrySet()) {
                    renderPass.setUniform(entry.getKey(), entry.getValue());
                }
                bindExtraUniforms(renderPass);

                for (ResolvedInput input : resolvedInputs) {
                    renderPass.bindTexture(
                            input.samplerName() + "Sampler",
                            input.view(),
                            RenderSystem.getSamplerCache().getClampToEdge(input.bilinear() ? FilterMode.LINEAR : FilterMode.NEAREST)
                    );
                }

                renderPass.draw(0, 3);
            }
        }

        void close() {
            staticUniforms.values().forEach(GpuBuffer::close);
            staticUniforms.clear();
            samplerInfoBuffer.close();
        }
    }
}
