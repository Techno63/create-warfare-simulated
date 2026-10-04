package com.julian.createwarfare.effects.client.engines;

import com.julian.createwarfare.effects.client.visual.VisualEffects;
import com.julian.createwarfare.effects.client.visual.VisualEffectsEngine;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class MushroomCapEngine {
    private static final List<MushroomCloud> CLOUDS = new ArrayList<>();

    private static final float CUBE_SIZE = 3.0f;
    private static final float DRAW_SIZE = 3.04f;

    private static final int LIFETIME = 600;
    private static final int FADE_START = 500;

    private static final float LAYER_END = 6.0f;
    private static final float FORM_START = 4.0f;
    private static final float PILLAR_END = 115.0f;

    private static final float CAP_START = 112.0f;
    private static final float CAP_END = 225.0f;

    private static final float MAX_RENDER_DISTANCE = 1800.0f;

    private static final int FRONT = 0;
    private static final int BACK = 1;
    private static final int LEFT = 2;
    private static final int RIGHT = 3;
    private static final int TOP = 4;
    private static final int BOTTOM = 5;

    private static final RenderType SMOKE_RENDER_TYPE = RenderType.create(
            "createwarfare_mushroom_smoke",
            DefaultVertexFormat.POSITION_COLOR,
            com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,
            1048576,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTextureState(RenderStateShard.NO_TEXTURE)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setOutputState(RenderStateShard.TRANSLUCENT_TARGET)
                    .createCompositeState(false)
    );

    public static void spawn(
            Vec3 position,
            float baseSmokeRadius,
            float baseSmokeHeight,
            float pillarHeight,
            float pillarRadius,
            float capHeight,
            float capRadius,
            int lifetime
    ) {
        MushroomCloud cloud = new MushroomCloud(
                position,
                Math.max(CUBE_SIZE, baseSmokeRadius),
                Math.max(CUBE_SIZE, baseSmokeHeight),
                Math.max(CUBE_SIZE, pillarHeight),
                Math.max(CUBE_SIZE, pillarRadius),
                Math.max(CUBE_SIZE, capHeight),
                Math.max(CUBE_SIZE, capRadius)
        );

        CLOUDS.add(cloud);
        VisualEffectsEngine.add(cloud);
    }

    public static void tick() {
        Iterator<MushroomCloud> iterator = CLOUDS.iterator();

        while (iterator.hasNext()) {
            MushroomCloud cloud = iterator.next();

            if (!cloud.isAlive()) {
                iterator.remove();
            }
        }
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float smooth(float value) {
        value = clamp(value, 0.0f, 1.0f);
        return value * value * (3.0f - 2.0f * value);
    }

    private static float smoother(float value) {
        value = clamp(value, 0.0f, 1.0f);
        return value * value * value * (
                value * (value * 6.0f - 15.0f) + 10.0f
        );
    }

    private static float easeOutCubic(float value) {
        value = clamp(value, 0.0f, 1.0f);

        float inverse = 1.0f - value;

        return 1.0f - inverse * inverse * inverse;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float noise(int x, int y, int z) {
        int n =
                x * 374761393
                        + y * 668265263
                        + z * 2147483647;

        n = (n ^ (n >> 13)) * 1274126177;
        n ^= n >> 16;

        return (n & 0x7fffffff) / 2147483647.0f;
    }

    private static int decode21(int value) {
        value &= 0x1fffff;

        if ((value & 0x100000) != 0) {
            value |= ~0x1fffff;
        }

        return value;
    }

    private static float radiusAtHeight(
            float y,
            float baseHeight,
            float pillarHeight,
            float pillarRadius,
            float capHeight,
            float capRadius,
            float baseRadius
    ) {
        float pillarTop =
                baseHeight + pillarHeight;

        if (y <= baseHeight) {
            float t =
                    smooth(
                            y / Math.max(0.001f, baseHeight)
                    );

            float bulge =
                    (float) Math.sin(t * Math.PI);

            return lerp(
                    baseRadius * 0.94f,
                    pillarRadius * 1.08f,
                    t
            ) * (
                    1.0f + 0.07f * bulge
            );
        }

        if (y <= pillarTop) {
            float t =
                    clamp(
                            (y - baseHeight)
                                    / Math.max(0.001f, pillarHeight),
                            0.0f,
                            1.0f
                    );

            return pillarRadius * (
                    1.0f
                            + 0.025f
                            * (float) Math.sin(
                            t * Math.PI * 2.0f
                    )
            );
        }

        float t =
                clamp(
                        (y - pillarTop)
                                / Math.max(0.001f, capHeight),
                        0.0f,
                        1.0f
                );

        if (t < 0.14f) {
            return lerp(
                    pillarRadius,
                    capRadius * 1.02f,
                    smooth(t / 0.14f)
            );
        }

        if (t < 0.70f) {
            float u =
                    (t - 0.14f) / 0.56f;

            float dome =
                    (float) Math.sin(u * Math.PI);

            return capRadius * (
                    0.97f + 0.16f * dome
            );
        }

        return lerp(
                capRadius,
                pillarRadius * 0.28f,
                smooth(
                        (t - 0.70f) / 0.30f
                )
        );
    }

    private static class MushroomCloud implements VisualEffects {
        private final Vec3 position;

        private final float baseRadius;
        private final float baseHeight;

        private final float pillarHeight;
        private final float pillarRadius;

        private final float capHeight;
        private final float capRadius;

        private final float totalHeight;
        private final float pillarTop;

        private final float baseFraction;
        private final float pillarFraction;

        private final Set<Long> occupied =
                new HashSet<>();

        private SurfaceFace[] faces;

        private int age;

        private MushroomCloud(
                Vec3 position,
                float baseRadius,
                float baseHeight,
                float pillarHeight,
                float pillarRadius,
                float capHeight,
                float capRadius
        ) {
            this.position = position;

            this.baseRadius = baseRadius;
            this.baseHeight = baseHeight;

            this.pillarHeight = pillarHeight;
            this.pillarRadius = pillarRadius;

            this.capHeight = capHeight;
            this.capRadius = capRadius;

            this.totalHeight =
                    baseHeight
                            + pillarHeight
                            + capHeight;

            this.pillarTop =
                    baseHeight
                            + pillarHeight;

            this.baseFraction =
                    baseHeight / totalHeight;

            this.pillarFraction =
                    pillarTop / totalHeight;

            buildStructure();
            buildSurface();

            occupied.clear();
        }

        private void buildStructure() {
            int minY = -1;

            int maxY =
                    Mth.ceil(
                            totalHeight / CUBE_SIZE
                    ) + 1;

            int maxRadius =
                    Mth.ceil(
                            Math.max(
                                    baseRadius,
                                    capRadius
                            ) / CUBE_SIZE
                    ) + 2;

            for (int y = minY; y <= maxY; y++) {
                float worldY =
                        y * CUBE_SIZE
                                + CUBE_SIZE * 0.5f;

                float radius =
                        radiusAtHeight(
                                Math.max(0.0f, worldY),
                                baseHeight,
                                pillarHeight,
                                pillarRadius,
                                capHeight,
                                capRadius,
                                baseRadius
                        );

                int r =
                        Math.min(
                                maxRadius,
                                Mth.ceil(
                                        radius / CUBE_SIZE
                                ) + 1
                        );

                for (int x = -r; x <= r; x++) {
                    for (int z = -r; z <= r; z++) {
                        float worldX =
                                x * CUBE_SIZE
                                        + CUBE_SIZE * 0.5f;

                        float worldZ =
                                z * CUBE_SIZE
                                        + CUBE_SIZE * 0.5f;

                        float distance =
                                Mth.sqrt(
                                        worldX * worldX
                                                + worldZ * worldZ
                                );

                        if (distance <= radius) {
                            occupied.add(
                                    key(x, y, z)
                            );
                        }
                    }
                }
            }
        }

        private void buildSurface() {
            ArrayList<SurfaceFace> result =
                    new ArrayList<>();

            for (long packed : occupied) {
                int x =
                        decode21(
                                (int) (packed >> 42)
                        );

                int y =
                        decode21(
                                (int) (packed >> 21)
                        );

                int z =
                        decode21(
                                (int) packed
                        );

                float n =
                        noise(x, y, z);

                float normalizedY =
                        clamp(
                                (
                                        y * CUBE_SIZE
                                                + CUBE_SIZE * 0.5f
                                ) / totalHeight,
                                0.0f,
                                1.0f
                        );

                int gray =
                        Mth.clamp(
                                (int) (
                                        78.0f
                                                - normalizedY * 16.0f
                                                + n * 8.0f
                                ),
                                38,
                                105
                        );

                int alpha =
                        Mth.clamp(
                                (int) (
                                        225.0f
                                                + n * 15.0f
                                ),
                                0,
                                240
                        );

                float phase =
                        x * 0.37f
                                + y * 0.19f
                                + z * 0.43f;

                float waveX =
                        0.06f
                                * CUBE_SIZE
                                * (0.65f + n * 0.35f);

                float waveZ =
                        0.055f
                                * CUBE_SIZE
                                * (0.65f + n * 0.35f);

                if (!occupied.contains(
                        key(x, y, z - 1)
                )) {
                    result.add(
                            new SurfaceFace(
                                    x,
                                    y,
                                    z,
                                    FRONT,
                                    normalizedY,
                                    gray,
                                    alpha,
                                    phase,
                                    waveX,
                                    waveZ
                            )
                    );
                }

                if (!occupied.contains(
                        key(x, y, z + 1)
                )) {
                    result.add(
                            new SurfaceFace(
                                    x,
                                    y,
                                    z,
                                    BACK,
                                    normalizedY,
                                    gray,
                                    alpha,
                                    phase + 1.37f,
                                    waveX,
                                    waveZ
                            )
                    );
                }

                if (!occupied.contains(
                        key(x - 1, y, z)
                )) {
                    result.add(
                            new SurfaceFace(
                                    x,
                                    y,
                                    z,
                                    LEFT,
                                    normalizedY,
                                    gray,
                                    alpha,
                                    phase + 2.41f,
                                    waveX,
                                    waveZ
                            )
                    );
                }

                if (!occupied.contains(
                        key(x + 1, y, z)
                )) {
                    result.add(
                            new SurfaceFace(
                                    x,
                                    y,
                                    z,
                                    RIGHT,
                                    normalizedY,
                                    gray,
                                    alpha,
                                    phase + 3.17f,
                                    waveX,
                                    waveZ
                            )
                    );
                }

                if (!occupied.contains(
                        key(x, y + 1, z)
                )) {
                    result.add(
                            new SurfaceFace(
                                    x,
                                    y,
                                    z,
                                    TOP,
                                    normalizedY,
                                    gray,
                                    alpha,
                                    phase + 4.23f,
                                    waveX,
                                    waveZ
                            )
                    );
                }

                if (!occupied.contains(
                        key(x, y - 1, z)
                )) {
                    result.add(
                            new SurfaceFace(
                                    x,
                                    y,
                                    z,
                                    BOTTOM,
                                    normalizedY,
                                    gray,
                                    alpha,
                                    phase + 5.19f,
                                    waveX,
                                    waveZ
                            )
                    );
                }
            }

            faces =
                    result.toArray(
                            new SurfaceFace[0]
                    );
        }

        @Override
        public void tick() {
            age++;
        }

        @Override
        public void render(
                PoseStack poseStack,
                Camera camera,
                MultiBufferSource.BufferSource bufferSource,
                float partialTick
        ) {
            double dx =
                    position.x
                            - camera.getPosition().x;

            double dy =
                    position.y
                            - camera.getPosition().y;

            double dz =
                    position.z
                            - camera.getPosition().z;

            double distanceSq =
                    dx * dx
                            + dy * dy
                            + dz * dz;

            if (
                    distanceSq
                            > MAX_RENDER_DISTANCE
                            * MAX_RENDER_DISTANCE
            ) {
                return;
            }

            float time =
                    age + partialTick;

            float layerProgress =
                    easeOutCubic(
                            clamp(
                                    time / LAYER_END,
                                    0.0f,
                                    1.0f
                            )
                    );

            float pillarProgress =
                    smoother(
                            clamp(
                                    (
                                            time - FORM_START
                                    ) / (
                                            PILLAR_END
                                                    - FORM_START
                                    ),
                                    0.0f,
                                    1.0f
                            )
                    );

            float capProgress =
                    smoother(
                            clamp(
                                    (
                                            time - CAP_START
                                    ) / (
                                            CAP_END
                                                    - CAP_START
                                    ),
                                    0.0f,
                                    1.0f
                            )
                    );

            float compression =
                    lerp(
                            1.0f,
                            0.50f,
                            pillarProgress
                    );

            float fade = 1.0f;

            if (time >= FADE_START) {
                fade =
                        smooth(
                                clamp(
                                        (
                                                LIFETIME
                                                        - time
                                        ) / (
                                                float) (
                                                LIFETIME
                                                        - FADE_START
                                        ),
                                        0.0f,
                                        1.0f
                                )
                        );
            }

            if (fade <= 0.0f) {
                return;
            }

            float distance =
                    (float) Math.sqrt(
                            distanceSq
                    );

            float capDistanceProgress =
                    clamp(
                            (
                                    distance - 80.0f
                            ) / (
                                    1600.0f - 80.0f
                            ),
                            0.0f,
                            1.0f
                    );

            float capDistanceAlpha =
                    lerp(
                            1.0f,
                            0.01f,
                            smoother(
                                    capDistanceProgress
                            )
                    );

            float driftX =
                    pillarProgress
                            * 0.035f
                            * (float) Math.sin(
                            time * 0.008f
                    );

            float driftZ =
                    pillarProgress
                            * 0.030f
                            * (float) Math.cos(
                            time * 0.007f
                    );

            float wave1 =
                    (float) Math.sin(
                            time * 0.021f
                    );

            float wave2 =
                    (float) Math.cos(
                            time * 0.017f
                    );

            Matrix4f matrix =
                    poseStack.last().pose();

            VertexConsumer buffer =
                    bufferSource.getBuffer(
                            SMOKE_RENDER_TYPE
                    );

            for (SurfaceFace face : faces) {
                float normalizedY =
                        face.normalizedY;

                float y =
                        face.y * CUBE_SIZE
                                + CUBE_SIZE * 0.5f;

                float targetY;
                float radialScale;
                float alphaMultiplier;

                if (
                        normalizedY
                                <= baseFraction
                ) {
                    float local =
                            clamp(
                                    y / Math.max(
                                            0.001f,
                                            baseHeight
                                    ),
                                    0.0f,
                                    1.0f
                            );

                    if (time < LAYER_END) {
                        targetY =
                                local * baseHeight;

                        radialScale =
                                layerProgress;

                        alphaMultiplier =
                                layerProgress;
                    } else {
                        targetY =
                                local
                                        * baseHeight
                                        * compression;

                        radialScale =
                                compression;

                        alphaMultiplier =
                                1.0f;
                    }
                } else if (
                        normalizedY
                                <= pillarFraction
                ) {
                    float pillarLocal =
                            clamp(
                                    (
                                            y - baseHeight
                                    ) / Math.max(
                                            0.001f,
                                            pillarHeight
                                    ),
                                    0.0f,
                                    1.0f
                            );

                    float reveal =
                            smooth(
                                    clamp(
                                            (
                                                    pillarProgress
                                                            - pillarLocal
                                                            * 0.84f
                                            ) / 0.16f,
                                            0.0f,
                                            1.0f
                                    )
                            );

                    if (reveal <= 0.001f) {
                        continue;
                    }

                    float pillarHeightNow =
                            pillarHeight
                                    * pillarProgress;

                    targetY =
                            baseHeight
                                    * compression
                                    + pillarLocal
                                    * pillarHeightNow;

                    radialScale =
                            lerp(
                                    0.72f,
                                    1.0f,
                                    pillarProgress
                            );

                    alphaMultiplier =
                            reveal;
                } else {
                    if (time < CAP_START) {
                        continue;
                    }

                    float horizontalRadius =
                            Mth.sqrt(
                                    (
                                            face.x
                                                    * CUBE_SIZE
                                    ) * (
                                            face.x
                                                    * CUBE_SIZE
                                    )
                                            + (
                                            face.z
                                                    * CUBE_SIZE
                                    ) * (
                                            face.z
                                                    * CUBE_SIZE
                                    )
                            );

                    float ringProgress =
                            clamp(
                                    horizontalRadius
                                            / Math.max(
                                            1.0f,
                                            capRadius
                                    ),
                                    0.0f,
                                    1.0f
                            );

                    float ringReveal =
                            smooth(
                                    clamp(
                                            (
                                                    capProgress
                                                            - ringProgress
                                                            * 0.78f
                                            ) / 0.22f,
                                            0.0f,
                                            1.0f
                                    )
                            );

                    if (
                            ringReveal
                                    <= 0.001f
                    ) {
                        continue;
                    }

                    float capLocal =
                            clamp(
                                    (
                                            y - pillarTop
                                    ) / Math.max(
                                            0.001f,
                                            capHeight
                                    ),
                                    0.0f,
                                    1.0f
                            );

                    float capVertical =
                            smooth(
                                    clamp(
                                            capProgress
                                                    * 1.20f
                                                    - capLocal
                                                    * 0.20f,
                                            0.0f,
                                            1.0f
                                    )
                            );

                    targetY =
                            baseHeight
                                    * compression
                                    + pillarHeight
                                    + capLocal
                                    * capHeight
                                    * capVertical;

                    radialScale =
                            lerp(
                                    0.22f,
                                    1.0f,
                                    ringReveal
                            );

                    alphaMultiplier =
                            ringReveal
                                    * capDistanceAlpha;
                }

                float waveStrength =
                        pillarProgress
                                * (
                                0.65f
                                        + normalizedY
                                        * 0.95f
                        );

                float waveX =
                        (
                                wave1
                                        * face.waveX
                                        + wave2
                                        * face.waveZ
                        ) * waveStrength;

                float waveZ =
                        (
                                wave2
                                        * face.waveX
                                        - wave1
                                        * face.waveZ
                        ) * waveStrength;

                float x =
                        face.x
                                * CUBE_SIZE
                                * radialScale
                                + waveX
                                + driftX
                                * normalizedY;

                float z =
                        face.z
                                * CUBE_SIZE
                                * radialScale
                                + waveZ
                                + driftZ
                                * normalizedY;

                targetY +=
                        pillarProgress
                                * normalizedY
                                * 0.08f
                                * (float) Math.sin(
                                time * 0.015f
                                        + face.phase
                        );

                int alpha =
                        Mth.clamp(
                                (int) (
                                        face.alpha
                                                * alphaMultiplier
                                                * fade
                                ),
                                0,
                                255
                        );

                if (alpha <= 0) {
                    continue;
                }

                renderFace(
                        matrix,
                        buffer,
                        face,
                        (float) position.x + x,
                        (float) position.y + targetY,
                        (float) position.z + z,
                        face.red,
                        face.green,
                        face.blue,
                        alpha
                );
            }
        }

        private void renderFace(
                Matrix4f matrix,
                VertexConsumer buffer,
                SurfaceFace face,
                float centerX,
                float centerY,
                float centerZ,
                int red,
                int green,
                int blue,
                int alpha
        ) {
            float half =
                    DRAW_SIZE * 0.5f;

            float x0 =
                    centerX - half;

            float x1 =
                    centerX + half;

            float y0 =
                    centerY - half;

            float y1 =
                    centerY + half;

            float z0 =
                    centerZ - half;

            float z1 =
                    centerZ + half;

            switch (face.direction) {
                case FRONT:
                    vertex(
                            buffer,
                            matrix,
                            x0,
                            y0,
                            z0,
                            red,
                            green,
                            blue,
                            alpha
                    );
                    vertex(
                            buffer,
                            matrix,
                            x1,
                            y0,
                            z0,
                            red,
                            green,
                            blue,
                            alpha
                    );
                    vertex(
                            buffer,
                            matrix,
                            x1,
                            y1,
                            z0,
                            red,
                            green,
                            blue,
                            alpha
                    );
                    vertex(
                            buffer,
                            matrix,
                            x0,
                            y1,
                            z0,
                            red,
                            green,
                            blue,
                            alpha
                    );
                    break;

                case BACK:
                    vertex(
                            buffer,
                            matrix,
                            x1,
                            y0,
                            z1,
                            red,
                            green,
                            blue,
                            alpha
                    );
                    vertex(
                            buffer,
                            matrix,
                            x0,
                            y0,
                            z1,
                            red,
                            green,
                            blue,
                            alpha
                    );
                    vertex(
                            buffer,
                            matrix,
                            x0,
                            y1,
                            z1,
                            red,
                            green,
                            blue,
                            alpha
                    );
                    vertex(
                            buffer,
                            matrix,
                            x1,
                            y1,
                            z1,
                            red,
                            green,
                            blue,
                            alpha
                    );
                    break;

                case LEFT:
                    vertex(
                            buffer,
                            matrix,
                            x0,
                            y0,
                            z1,
                            red,
                            green,
                            blue,
                            alpha
                    );
                    vertex(
                            buffer,
                            matrix,
                            x0,
                            y0,
                            z0,
                            red,
                            green,
                            blue,
                            alpha
                    );
                    vertex(
                            buffer,
                            matrix,
                            x0,
                            y1,
                            z0,
                            red,
                            green,
                            blue,
                            alpha
                    );
                    vertex(
                            buffer,
                            matrix,
                            x0,
                            y1,
                            z1,
                            red,
                            green,
                            blue,
                            alpha
                    );
                    break;

                case RIGHT:
                    vertex(
                            buffer,
                            matrix,
                            x1,
                            y0,
                            z0,
                            red,
                            green,
                            blue,
                            alpha
                    );
                    vertex(
                            buffer,
                            matrix,
                            x1,
                            y0,
                            z1,
                            red,
                            green,
                            blue,
                            alpha
                    );
                    vertex(
                            buffer,
                            matrix,
                            x1,
                            y1,
                            z1,
                            red,
                            green,
                            blue,
                            alpha
                    );
                    vertex(
                            buffer,
                            matrix,
                            x1,
                            y1,
                            z0,
                            red,
                            green,
                            blue,
                            alpha
                    );
                    break;

                case TOP:
                    vertex(
                            buffer,
                            matrix,
                            x0,
                            y1,
                            z0,
                            red,
                            green,
                            blue,
                            alpha
                    );
                    vertex(
                            buffer,
                            matrix,
                            x1,
                            y1,
                            z0,
                            red,
                            green,
                            blue,
                            alpha
                    );
                    vertex(
                            buffer,
                            matrix,
                            x1,
                            y1,
                            z1,
                            red,
                            green,
                            blue,
                            alpha
                    );
                    vertex(
                            buffer,
                            matrix,
                            x0,
                            y1,
                            z1,
                            red,
                            green,
                            blue,
                            alpha
                    );
                    break;

                case BOTTOM:
                    vertex(
                            buffer,
                            matrix,
                            x0,
                            y0,
                            z1,
                            red,
                            green,
                            blue,
                            alpha
                    );
                    vertex(
                            buffer,
                            matrix,
                            x1,
                            y0,
                            z1,
                            red,
                            green,
                            blue,
                            alpha
                    );
                    vertex(
                            buffer,
                            matrix,
                            x1,
                            y0,
                            z0,
                            red,
                            green,
                            blue,
                            alpha
                    );
                    vertex(
                            buffer,
                            matrix,
                            x0,
                            y0,
                            z0,
                            red,
                            green,
                            blue,
                            alpha
                    );
                    break;

                default:
                    break;
            }
        }

        private void vertex(
                VertexConsumer buffer,
                Matrix4f matrix,
                float x,
                float y,
                float z,
                int red,
                int green,
                int blue,
                int alpha
        ) {
            buffer.addVertex(
                    matrix,
                    x,
                    y,
                    z
            ).setColor(
                    red,
                    green,
                    blue,
                    alpha
            );
        }

        @Override
        public boolean isAlive() {
            return age < LIFETIME;
        }
    }

    private static class SurfaceFace {
        private final int x;
        private final int y;
        private final int z;

        private final int direction;

        private final float normalizedY;

        private final int red;
        private final int green;
        private final int blue;
        private final int alpha;

        private final float phase;
        private final float waveX;
        private final float waveZ;

        private SurfaceFace(
                int x,
                int y,
                int z,
                int direction,
                float normalizedY,
                int gray,
                int alpha,
                float phase,
                float waveX,
                float waveZ
        ) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.direction = direction;
            this.normalizedY = normalizedY;

            this.red = gray;
            this.green = gray;
            this.blue =
                    Mth.clamp(
                            gray - 4,
                            0,
                            255
                    );

            this.alpha = alpha;
            this.phase = phase;
            this.waveX = waveX;
            this.waveZ = waveZ;
        }
    }

    private static long key(
            int x,
            int y,
            int z
    ) {
        return ((long) (x & 0x1fffff) << 42)
                | ((long) (y & 0x1fffff) << 21)
                | (z & 0x1fffff);
    }
}