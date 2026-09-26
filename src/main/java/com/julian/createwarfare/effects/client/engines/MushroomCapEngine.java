package com.julian.createwarfare.effects.client.engines;

import com.julian.createwarfare.effects.client.visual.VisualEffects;
import com.julian.createwarfare.effects.client.visual.VisualEffectsEngine;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;

public final class MushroomCapEngine implements VisualEffects {

    private static final int SEGMENTS = 48;
    private static final int PILLAR_BANDS = 40;
    private static final int CAP_BANDS = 56;

    private static final float PILLAR_DURATION = 180.0f;
    private static final float CAP_START = 120.0f;
    private static final float CAP_FORM_DURATION = 100.0f;
    private static final float GRAY_TRANSITION_DURATION = 400.0f;
    private static final float GRAY_HOLD_DURATION = 900.0f;
    private static final float DISSOLVE_DURATION = 500.0f;

    private static final float CELL_UPDATE_TICKS = 7.0f;
    private static final float RENDER_DISTANCE = 3072.0f;

    private static final int[][] FIRE_PALETTE = {
            {45, 4, 2},
            {80, 8, 2},
            {125, 15, 3},
            {170, 25, 4},
            {215, 45, 5},
            {245, 75, 8},
            {255, 115, 12},
            {255, 165, 25},
            {255, 205, 55},
            {255, 235, 130}
    };

    private static final int[][] GRAY_PALETTE = {
            {30, 30, 30},
            {42, 42, 42},
            {55, 55, 55},
            {70, 70, 70},
            {88, 88, 88},
            {108, 108, 108},
            {130, 130, 130},
            {150, 150, 150},
            {170, 170, 170},
            {190, 190, 190}
    };

    private final Vec3 origin;
    private final float height;
    private final float pillarRadius;
    private final float capRadius;
    private final long seed;

    private float age;
    private boolean alive = true;

    private MushroomCapEngine(
            Vec3 origin,
            float height,
            float pillarRadius,
            float capRadius
    ) {
        this.origin = origin;
        this.height = height;
        this.pillarRadius = pillarRadius;
        this.capRadius = capRadius;

        long x = Double.doubleToLongBits(origin.x);
        long y = Double.doubleToLongBits(origin.y);
        long z = Double.doubleToLongBits(origin.z);

        this.seed = mix64(
                x
                        ^ Long.rotateLeft(y, 21)
                        ^ Long.rotateLeft(z, 42)
        );
    }

    public static void spawn(
            Vec3 origin,
            float height,
            float pillarRadius,
            float capRadius
    ) {
        VisualEffectsEngine.add(
                new MushroomCapEngine(
                        origin,
                        height,
                        pillarRadius,
                        capRadius
                )
        );
    }

    @Override
    public void tick() {
        age++;

        float lifetime =
                PILLAR_DURATION
                        + CAP_FORM_DURATION
                        + GRAY_TRANSITION_DURATION
                        + GRAY_HOLD_DURATION
                        + DISSOLVE_DURATION;

        if (age >= lifetime) {
            alive = false;
        }
    }

    @Override
    public boolean isAlive() {
        return alive;
    }

    @Override
    public void render(
            PoseStack poseStack,
            Camera camera,
            MultiBufferSource.BufferSource buffer,
            float partialTick
    ) {
        double dx = origin.x - camera.getPosition().x;
        double dy = origin.y - camera.getPosition().y;
        double dz = origin.z - camera.getPosition().z;

        if (dx * dx + dy * dy + dz * dz
                > RENDER_DISTANCE * RENDER_DISTANCE) {
            return;
        }

        float renderAge = age + partialTick;

        poseStack.pushPose();

        poseStack.translate(
                origin.x,
                origin.y,
                origin.z
        );

        VertexConsumer consumer =
                buffer.getBuffer(RenderType.debugQuads());

        renderPillar(
                poseStack,
                consumer,
                renderAge
        );

        renderCap(
                poseStack,
                consumer,
                renderAge
        );

        poseStack.popPose();
    }

    private void renderPillar(
            PoseStack poseStack,
            VertexConsumer consumer,
            float renderAge
    ) {
        float progress = smoothStep(
                Math.min(
                        renderAge / PILLAR_DURATION,
                        1.0f
                )
        );

        if (progress <= 0.0f) {
            return;
        }

        float currentHeight = height * progress;
        float stemTopRadius = pillarRadius * 1.12f;

        float[][] previous =
                new float[SEGMENTS][3];

        float[][] current =
                new float[SEGMENTS][3];

        for (int band = 0; band < PILLAR_BANDS; band++) {
            float t0 = band / (float) PILLAR_BANDS;
            float t1 = (band + 1) / (float) PILLAR_BANDS;

            float y0 = currentHeight * t0;
            float y1 = currentHeight * t1;

            float r0 = lerp(
                    pillarRadius,
                    stemTopRadius,
                    smoothStep(t0)
            );

            float r1 = lerp(
                    pillarRadius,
                    stemTopRadius,
                    smoothStep(t1)
            );

            fillStemRing(
                    previous,
                    t0,
                    y0,
                    r0
            );

            fillStemRing(
                    current,
                    t1,
                    y1,
                    r1
            );

            float dissolve0 =
                    dissolveAmount(
                            t0,
                            renderAge
                    );

            float dissolve1 =
                    dissolveAmount(
                            t1,
                            renderAge
                    );

            for (int segment = 0;
                 segment < SEGMENTS;
                 segment++) {

                int next =
                        (segment + 1) % SEGMENTS;

                float[] a = previous[segment];
                float[] b = previous[next];
                float[] c = current[next];
                float[] d = current[segment];

                float cell =
                        cellValue(
                                band,
                                segment,
                                renderAge
                        );

                int[] color =
                        cellColor(
                                cell,
                                grayAmount(
                                        renderAge,
                                        t0
                                )
                        );

                addQuad(
                        consumer,
                        poseStack.last(),
                        a,
                        b,
                        c,
                        d,
                        color,
                        Math.min(
                                dissolve0,
                                dissolve1
                        )
                );
            }

            float[][] swap = previous;
            previous = current;
            current = swap;
        }
    }

    private void renderCap(
            PoseStack poseStack,
            VertexConsumer consumer,
            float renderAge
    ) {
        if (renderAge < CAP_START) {
            return;
        }

        float progress = smoothStep(
                Math.min(
                        (renderAge - CAP_START)
                                / CAP_FORM_DURATION,
                        1.0f
                )
        );

        if (progress <= 0.0f) {
            return;
        }

        float capHeight =
                height * 0.35f * progress;

        float stemTopY =
                height
                        * Math.min(
                        renderAge
                                / PILLAR_DURATION,
                        1.0f
                );

        float stemTopRadius =
                pillarRadius * 1.12f;

        float[][] previous =
                new float[SEGMENTS][3];

        float[][] current =
                new float[SEGMENTS][3];

        for (int band = 0;
             band < CAP_BANDS;
             band++) {

            float t0 =
                    band / (float) CAP_BANDS;

            float t1 =
                    (band + 1)
                            / (float) CAP_BANDS;

            fillCapRing(
                    previous,
                    t0,
                    stemTopY,
                    stemTopRadius,
                    capHeight
            );

            fillCapRing(
                    current,
                    t1,
                    stemTopY,
                    stemTopRadius,
                    capHeight
            );

            float vertical0 =
                    capVerticalPosition(t0);

            float vertical1 =
                    capVerticalPosition(t1);

            float dissolve0 =
                    dissolveAmount(
                            vertical0,
                            renderAge - CAP_START
                    );

            float dissolve1 =
                    dissolveAmount(
                            vertical1,
                            renderAge - CAP_START
                    );

            for (int segment = 0;
                 segment < SEGMENTS;
                 segment++) {

                int next =
                        (segment + 1) % SEGMENTS;

                float[] a = previous[segment];
                float[] b = previous[next];
                float[] c = current[next];
                float[] d = current[segment];

                float cell =
                        cellValue(
                                band + 1000,
                                segment,
                                renderAge
                        );

                int[] color =
                        cellColor(
                                cell,
                                grayAmount(
                                        renderAge,
                                        vertical0
                                )
                        );

                addQuad(
                        consumer,
                        poseStack.last(),
                        a,
                        b,
                        c,
                        d,
                        color,
                        Math.min(
                                dissolve0,
                                dissolve1
                        )
                );
            }

            float[][] swap = previous;
            previous = current;
            current = swap;
        }
    }

    private void fillStemRing(
            float[][] ring,
            float t,
            float y,
            float radius
    ) {
        float wobble =
                1.0f
                        + 0.035f
                        * noise(
                        seed,
                        (long)
                                (t * 10000.0f),
                        12
                );

        for (int i = 0;
             i < SEGMENTS;
             i++) {

            float angle =
                    (float)
                            (i * Math.PI * 2.0
                                    / SEGMENTS);

            float localWobble =
                    1.0f
                            + 0.025f
                            * noise(
                            seed,
                            i,
                            (long)
                                    (t * 10000.0f)
                    );

            float r =
                    radius
                            * wobble
                            * localWobble;

            ring[i][0] =
                    (float)
                            Math.cos(angle) * r;

            ring[i][1] = y;

            ring[i][2] =
                    (float)
                            Math.sin(angle) * r;
        }
    }

    private void fillCapRing(
            float[][] ring,
            float t,
            float stemTopY,
            float stemTopRadius,
            float capHeight
    ) {
        float radius;
        float y;

        if (t <= 0.20f) {
            float p = t / 0.20f;

            radius =
                    lerp(
                            stemTopRadius,
                            capRadius * 0.72f,
                            smoothStep(p)
                    );

            y =
                    stemTopY
                            - capHeight
                            * 0.38f
                            * smoothStep(p);

        } else if (t <= 0.48f) {
            float p =
                    (t - 0.20f) / 0.28f;

            radius =
                    lerp(
                            capRadius * 0.72f,
                            capRadius,
                            smoothStep(p)
                    );

            y =
                    stemTopY
                            - capHeight * 0.38f
                            + capHeight
                            * 0.92f
                            * smoothStep(p);

        } else if (t <= 0.72f) {
            float p =
                    (t - 0.48f) / 0.24f;

            radius =
                    lerp(
                            capRadius,
                            capRadius * 0.84f,
                            smoothStep(p)
                    );

            y =
                    stemTopY
                            + capHeight * 0.54f
                            + capHeight
                            * 0.46f
                            * smoothStep(p);

        } else {
            float p =
                    (t - 0.72f) / 0.28f;

            radius =
                    lerp(
                            capRadius * 0.84f,
                            0.0f,
                            smoothStep(p)
                    );

            y =
                    stemTopY
                            + capHeight
                            + capHeight
                            * 0.06f
                            * smoothStep(p);
        }

        for (int i = 0;
             i < SEGMENTS;
             i++) {

            float angle =
                    (float)
                            (i * Math.PI * 2.0
                                    / SEGMENTS);

            float localWobble =
                    1.0f
                            + 0.035f
                            * noise(
                            seed,
                            i,
                            (long)
                                    (t * 10000.0f)
                    );

            float r =
                    radius * localWobble;

            ring[i][0] =
                    (float)
                            Math.cos(angle) * r;

            ring[i][1] = y;

            ring[i][2] =
                    (float)
                            Math.sin(angle) * r;
        }
    }

    private float capVerticalPosition(
            float t
    ) {
        if (t <= 0.20f) {
            return 1.0f - t / 0.20f;
        }

        if (t <= 0.48f) {
            return 0.8f
                    - (t - 0.20f)
                    / 0.28f
                    * 0.45f;
        }

        if (t <= 0.72f) {
            return 0.35f
                    + (t - 0.48f)
                    / 0.24f
                    * 0.45f;
        }

        return 0.8f
                + (t - 0.72f)
                / 0.28f
                * 0.2f;
    }

    private float cellValue(
            int band,
            int segment,
            float renderAge
    ) {
        long frame =
                (long)
                        Math.floor(
                                renderAge
                                        / CELL_UPDATE_TICKS
                        );

        float center =
                noise(
                        seed,
                        band * 928371L,
                        segment * 74129L
                                + frame
                );

        float left =
                noise(
                        seed,
                        band * 928371L,
                        (segment - 1)
                                * 74129L
                                + frame
                );

        float right =
                noise(
                        seed,
                        band * 928371L,
                        (segment + 1)
                                * 74129L
                                + frame
                );

        float up =
                noise(
                        seed,
                        (band + 1)
                                * 928371L,
                        segment * 74129L
                                + frame
                );

        float down =
                noise(
                        seed,
                        (band - 1)
                                * 928371L,
                        segment * 74129L
                                + frame
                );

        return center * 0.55f
                + left * 0.1f
                + right * 0.1f
                + up * 0.125f
                + down * 0.125f;
    }

    private int[] cellColor(
            float value,
            float grayAmount
    ) {
        float clamped =
                Math.max(
                        0.0f,
                        Math.min(
                                0.999f,
                                value
                        )
                );

        int fireIndex =
                (int)
                        (clamped
                                * FIRE_PALETTE.length);

        int grayIndex =
                (int)
                        (clamped
                                * GRAY_PALETTE.length);

        int[] fire =
                FIRE_PALETTE[fireIndex];

        int[] gray =
                GRAY_PALETTE[grayIndex];

        return new int[] {
                (int)
                        lerp(
                                fire[0],
                                gray[0],
                                grayAmount
                        ),
                (int)
                        lerp(
                                fire[1],
                                gray[1],
                                grayAmount
                        ),
                (int)
                        lerp(
                                fire[2],
                                gray[2],
                                grayAmount
                        )
        };
    }

    private float grayAmount(
            float renderAge,
            float vertical
    ) {
        float start =
                PILLAR_DURATION
                        + CAP_FORM_DURATION
                        + vertical
                        * GRAY_TRANSITION_DURATION;

        return smoothStep(
                Math.min(
                        Math.max(
                                (renderAge - start)
                                        / GRAY_TRANSITION_DURATION,
                                0.0f
                        ),
                        1.0f
                )
        );
    }

    private float dissolveAmount(
            float vertical,
            float renderAge
    ) {
        float dissolveStart =
                PILLAR_DURATION
                        + CAP_FORM_DURATION
                        + GRAY_TRANSITION_DURATION
                        + GRAY_HOLD_DURATION;

        float localStart =
                dissolveStart
                        + (1.0f - vertical)
                        * DISSOLVE_DURATION;

        return 1.0f
                - smoothStep(
                Math.min(
                        Math.max(
                                (renderAge - localStart)
                                        / DISSOLVE_DURATION,
                                0.0f
                        ),
                        1.0f
                )
        );
    }

    private static void addQuad(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float[] a,
            float[] b,
            float[] c,
            float[] d,
            int[] color,
            float visibility
    ) {
        if (visibility <= 0.0f) {
            return;
        }

        consumer.addVertex(
                pose,
                a[0],
                a[1],
                a[2]
        ).setColor(
                color[0],
                color[1],
                color[2],
                255
        );

        consumer.addVertex(
                pose,
                b[0],
                b[1],
                b[2]
        ).setColor(
                color[0],
                color[1],
                color[2],
                255
        );

        consumer.addVertex(
                pose,
                c[0],
                c[1],
                c[2]
        ).setColor(
                color[0],
                color[1],
                color[2],
                255
        );

        consumer.addVertex(
                pose,
                d[0],
                d[1],
                d[2]
        ).setColor(
                color[0],
                color[1],
                color[2],
                255
        );
    }

    private static float lerp(
            float a,
            float b,
            float t
    ) {
        return a + (b - a) * t;
    }

    private static float smoothStep(
            float value
    ) {
        value =
                Math.max(
                        0.0f,
                        Math.min(
                                1.0f,
                                value
                        )
                );

        return value
                * value
                * (3.0f - 2.0f * value);
    }

    private static float noise(
            long seed,
            long x,
            long y
    ) {
        long h = seed;

        h ^=
                x * 0x9e3779b97f4a7c15L;

        h ^=
                y * 0xc2b2ae3d27d4eb4fL;

        h = mix64(h);

        return (float)
                ((h >>> 11) * 0x1.0p-53);
    }

    private static long mix64(
            long value
    ) {
        value =
                (value ^ (value >>> 30))
                        * 0xbf58476d1ce4e5b9L;

        value =
                (value ^ (value >>> 27))
                        * 0x94d049bb133111ebL;

        return value
                ^ (value >>> 31);
    }
}