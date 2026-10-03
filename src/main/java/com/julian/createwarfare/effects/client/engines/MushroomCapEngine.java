package com.julian.createwarfare.effects.client.engines;

import com.julian.createwarfare.effects.client.visual.VisualEffects;
import com.julian.createwarfare.effects.client.visual.VisualEffectsEngine;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class MushroomCapEngine implements VisualEffects {

    private static final float T_SPREAD = 0.18f;
    private static final float T_COMPRESS = 0.32f;
    private static final float T_RISE = 0.66f;

    private static final int RINGS = 56;
    private static final int SEGS = 28;
    private static final int PROFILE_POINTS = 9;
    private static final float CELL = 0.5f;
    private static final float NOISE_FREQ = 0.45f;

    private static final float LIGHT_X = 0.30f;
    private static final float LIGHT_Y = 0.88f;
    private static final float LIGHT_Z = 0.36f;

    private final Vec3 origin;
    private final float baseSmokeRadius;
    private final float baseSmokeHeight;
    private final float pillarHeight;
    private final float pillarRadius;
    private final float capHeight;
    private final float capRadius;
    private final int lifetime;

    private final float[] ar = new float[PROFILE_POINTS];
    private final float[] ay = new float[PROFILE_POINTS];
    private final float[] cum = new float[PROFILE_POINTS];
    private final float[] vx = new float[RINGS * SEGS];
    private final float[] vy = new float[RINGS * SEGS];
    private final float[] vz = new float[RINGS * SEGS];
    private final float[] cosTable = new float[SEGS];
    private final float[] sinTable = new float[SEGS];

    private int age;

    public MushroomCapEngine(
            Vec3 origin,
            float baseSmokeRadius,
            float baseSmokeHeight,
            float pillarHeight,
            float pillarRadius,
            float capHeight,
            float capRadius,
            int lifetime
    ) {
        this.origin = origin;
        this.baseSmokeRadius = baseSmokeRadius;
        this.baseSmokeHeight = baseSmokeHeight;
        this.pillarHeight = pillarHeight;
        this.pillarRadius = pillarRadius;
        this.capHeight = capHeight;
        this.capRadius = capRadius;
        this.lifetime = Math.max(1, lifetime);

        for (int j = 0; j < SEGS; j++) {
            double a = Math.PI * 2.0 * j / SEGS;
            cosTable[j] = (float) Math.cos(a);
            sinTable[j] = (float) Math.sin(a);
        }
    }

    public static void spawn(
            Vec3 origin,
            float baseSmokeRadius,
            float baseSmokeHeight,
            float pillarHeight,
            float pillarRadius,
            float capHeight,
            float capRadius,
            int lifetime
    ) {
        VisualEffectsEngine.add(
                new MushroomCapEngine(
                        origin,
                        baseSmokeRadius,
                        baseSmokeHeight,
                        pillarHeight,
                        pillarRadius,
                        capHeight,
                        capRadius,
                        lifetime
                )
        );
    }

    @Override
    public void tick() {
        age++;
    }

    @Override
    public boolean isAlive() {
        return age < lifetime * 2;
    }

    @Override
    public void render(
            PoseStack poseStack,
            Camera camera,
            MultiBufferSource.BufferSource bufferSource,
            float partialTick
    ) {
        float shapeT = clamp01(
                (age + partialTick) / lifetime
        );

        buildVertices(shapeT);

        float fadeProgress = 0f;

        if (age >= lifetime) {
            fadeProgress = clamp01(
                    (age + partialTick - lifetime)
                            / lifetime
            );
        }

        poseStack.pushPose();

        poseStack.translate(
                origin.x,
                origin.y,
                origin.z
        );

        Matrix4f matrix = poseStack.last().pose();

        VertexConsumer consumer =
                bufferSource.getBuffer(
                        RenderType.debugQuads()
                );

        float lightLength = (float) Math.sqrt(
                LIGHT_X * LIGHT_X
                        + LIGHT_Y * LIGHT_Y
                        + LIGHT_Z * LIGHT_Z
        );

        for (int i = 0; i < RINGS - 1; i++) {
            for (int j = 0; j < SEGS; j++) {
                int j1 = (j + 1) % SEGS;

                int a = i * SEGS + j;
                int b = (i + 1) * SEGS + j;
                int c = (i + 1) * SEGS + j1;
                int d = i * SEGS + j1;

                float ux = vx[b] - vx[a];
                float uy = vy[b] - vy[a];
                float uz = vz[b] - vz[a];

                float wx = vx[d] - vx[a];
                float wy = vy[d] - vy[a];
                float wz = vz[d] - vz[a];

                float nx = uy * wz - uz * wy;
                float ny = uz * wx - ux * wz;
                float nz = ux * wy - uy * wx;

                float length = (float) Math.sqrt(
                        nx * nx
                                + ny * ny
                                + nz * nz
                );

                if (length < 1e-5f) {
                    nx = cosTable[j];
                    ny = 0f;
                    nz = sinTable[j];
                    length = 1f;
                }

                float dot = (
                        nx * LIGHT_X
                                + ny * LIGHT_Y
                                + nz * LIGHT_Z
                ) / (length * lightLength);

                float cx = (vx[a] + vx[c]) * 0.5f;
                float cy = (vy[a] + vy[c]) * 0.5f;
                float cz = (vz[a] + vz[c]) * 0.5f;

                float tone = noise(
                        cx * 0.3f + 17f,
                        cy * 0.3f,
                        cz * 0.3f + 5f
                ) - 0.5f;

                float gray = clamp(
                        0.13f
                                + 0.27f * Math.max(0f, dot)
                                + 0.10f * tone,
                        0.06f,
                        0.50f
                );

                int g = Math.round(
                        gray * 255f
                );

                float alpha;

                if (age < lifetime) {
                    alpha = 1f;
                } else {
                    float normalizedY = clamp01(
                            (cy - ay[0])
                                    / Math.max(
                                    ay[PROFILE_POINTS - 1] - ay[0],
                                    0.001f
                            )
                    );

                    float fadeFront = fadeProgress;
                    float fadeWidth = 0.18f;

                    alpha = clamp01(
                            (normalizedY - (fadeFront - fadeWidth))
                                    / fadeWidth
                    );
                }

                int aColor = Math.round(
                        alpha * 255f
                );

                consumer.addVertex(
                        matrix,
                        vx[a],
                        vy[a],
                        vz[a]
                ).setColor(
                        g,
                        g,
                        g,
                        aColor
                );

                consumer.addVertex(
                        matrix,
                        vx[b],
                        vy[b],
                        vz[b]
                ).setColor(
                        g,
                        g,
                        g,
                        aColor
                );

                consumer.addVertex(
                        matrix,
                        vx[c],
                        vy[c],
                        vz[c]
                ).setColor(
                        g,
                        g,
                        g,
                        aColor
                );

                consumer.addVertex(
                        matrix,
                        vx[d],
                        vy[d],
                        vz[d]
                ).setColor(
                        g,
                        g,
                        g,
                        aColor
                );
            }
        }

        poseStack.popPose();
    }

    private void buildVertices(float t) {
        float spread = easeOutCubic(
                phase(
                        t,
                        0f,
                        T_SPREAD
                )
        );

        float compress = smooth(
                phase(
                        t,
                        T_SPREAD,
                        T_COMPRESS
                )
        );

        float rise = smooth(
                phase(
                        t,
                        T_COMPRESS,
                        T_RISE
                )
        );

        float cap = smooth(
                phase(
                        t,
                        T_RISE,
                        1f
                )
        );

        float compressedBaseR = Math.max(
                pillarRadius * 1.8f,
                baseSmokeRadius * 0.28f
        );

        float baseR = lerp(
                baseSmokeRadius * spread,
                compressedBaseR,
                compress
        );

        float stemR = Math.min(
                baseR,
                pillarRadius
        );

        float hb = baseSmokeHeight
                * (
                0.5f
                        + 0.5f * spread
        )
                * (
                1f
                        + 0.5f * compress
        );

        float topY = hb
                + Math.max(
                0f,
                pillarHeight - hb
        ) * rise;

        ar[0] = 0f;
        ay[0] = 0f;

        ar[1] = baseR;
        ay[1] = 0f;

        ar[2] = baseR;
        ay[2] = hb;

        ar[3] = stemR;
        ay[3] = hb;

        ar[4] = stemR;
        ay[4] = topY;

        ar[5] = lerp(
                stemR,
                capRadius * 0.65f,
                cap
        );

        ay[5] = topY
                - capHeight
                * 0.10f
                * cap;

        ar[6] = lerp(
                stemR,
                capRadius,
                cap
        );

        ay[6] = topY
                + capHeight
                * 0.25f
                * cap;

        ar[7] = capRadius
                * 0.70f
                * cap;

        ay[7] = topY
                + capHeight
                * 0.70f
                * cap;

        ar[8] = 0f;

        ay[8] = topY
                + capHeight
                * cap;

        cum[0] = 0f;

        for (int i = 1; i < PROFILE_POINTS; i++) {
            cum[i] = cum[i - 1]
                    + (float) Math.hypot(
                    ar[i] - ar[i - 1],
                    ay[i] - ay[i - 1]
            );
        }

        float total = Math.max(
                cum[PROFILE_POINTS - 1],
                1e-4f
        );

        float scroll = t * 3f;

        float amp = 0.12f
                + 0.10f * cap;

        for (int i = 0; i < RINGS; i++) {
            float s = total
                    * i
                    / (RINGS - 1);

            int k = 1;

            while (
                    k < PROFILE_POINTS - 1
                            && cum[k] < s
            ) {
                k++;
            }

            float segment = cum[k] - cum[k - 1];

            float f = segment < 1e-6f
                    ? 0f
                    : (s - cum[k - 1]) / segment;

            float radius = lerp(
                    ar[k - 1],
                    ar[k],
                    f
            );

            float y = lerp(
                    ay[k - 1],
                    ay[k],
                    f
            );

            for (int j = 0; j < SEGS; j++) {
                float cos = cosTable[j];
                float sin = sinTable[j];

                float n = noise(
                        radius
                                * cos
                                * NOISE_FREQ,
                        y
                                * NOISE_FREQ
                                - scroll,
                        radius
                                * sin
                                * NOISE_FREQ
                );

                float r = radius
                        * (
                        1f
                                - amp * n
                );

                int index = i * SEGS + j;

                vx[index] = snap(
                        r * cos
                );

                vy[index] = snap(
                        y
                );

                vz[index] = snap(
                        r * sin
                );
            }
        }
    }

    private static float snap(float value) {
        return Math.round(
                value / CELL
        ) * CELL;
    }

    private static float lerp(
            float a,
            float b,
            float t
    ) {
        return a + (b - a) * t;
    }

    private static float clamp01(float value) {
        return clamp(
                value,
                0f,
                1f
        );
    }

    private static float clamp(
            float value,
            float min,
            float max
    ) {
        return Math.max(
                min,
                Math.min(
                        max,
                        value
                )
        );
    }

    private static float phase(
            float t,
            float start,
            float end
    ) {
        return clamp01(
                (t - start)
                        / (end - start)
        );
    }

    private static float smooth(float x) {
        return x
                * x
                * (
                3f
                        - 2f * x
        );
    }

    private static float easeOutCubic(float x) {
        float inverse = 1f - x;

        return 1f
                - inverse
                * inverse
                * inverse;
    }

    private static float hash(
            int x,
            int y,
            int z
    ) {
        int h = x * 374761393
                + y * 668265263
                + z * 1274126177;

        h = (
                h ^ (h >>> 13)
        ) * 1274126177;

        return (
                (h ^ (h >>> 16))
                        & 0xFFFFFF
        ) / (float) 0xFFFFFF;
    }

    private static float noise(
            float x,
            float y,
            float z
    ) {
        int xi = (int) Math.floor(x);
        int yi = (int) Math.floor(y);
        int zi = (int) Math.floor(z);

        float fx = smooth(x - xi);
        float fy = smooth(y - yi);
        float fz = smooth(z - zi);

        float c000 = hash(
                xi,
                yi,
                zi
        );

        float c100 = hash(
                xi + 1,
                yi,
                zi
        );

        float c010 = hash(
                xi,
                yi + 1,
                zi
        );

        float c110 = hash(
                xi + 1,
                yi + 1,
                zi
        );

        float c001 = hash(
                xi,
                yi,
                zi + 1
        );

        float c101 = hash(
                xi + 1,
                yi,
                zi + 1
        );

        float c011 = hash(
                xi,
                yi + 1,
                zi + 1
        );

        float c111 = hash(
                xi + 1,
                yi + 1,
                zi + 1
        );

        float x00 = lerp(
                c000,
                c100,
                fx
        );

        float x10 = lerp(
                c010,
                c110,
                fx
        );

        float x01 = lerp(
                c001,
                c101,
                fx
        );

        float x11 = lerp(
                c011,
                c111,
                fx
        );

        return lerp(
                lerp(
                        x00,
                        x10,
                        fy
                ),
                lerp(
                        x01,
                        x11,
                        fy
                ),
                fz
        );
    }
}