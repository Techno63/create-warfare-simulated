package com.julian.createwarfare.effects.client.engines;

import com.julian.createwarfare.effects.client.visual.VisualEffects;
import com.julian.createwarfare.effects.client.visual.VisualEffectsEngine;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class FireballEngine implements VisualEffects {

    private static final int SEGMENTS = 48;
    private static final int RINGS = 32;
    private static final float MAX_RENDER_DISTANCE = 3072.0f;
    private static final int FADE_TICKS = 20;

    private static final RenderType FIREBALL_RENDER_TYPE = RenderType.create(
            "createwarfare_fireball",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            256,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false)
    );

    private static final RenderType FIREBALL_GLOW_RENDER_TYPE = RenderType.create(
            "createwarfare_fireball_glow",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            256,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.ADDITIVE_TRANSPARENCY)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false)
    );

    private final Vec3 origin;
    private final float speed;
    private final float maxRadius;

    private float radius;
    private int age;
    private boolean alive = true;

    private FireballEngine(
            Vec3 origin,
            float speed,
            float maxRadius
    ) {
        this.origin = origin;
        this.speed = Math.max(speed, 0.01f);
        this.maxRadius = Math.max(maxRadius, 0.01f);
    }

    public static void spawn(
            Vec3 origin,
            float speed,
            float maxRadius
    ) {
        VisualEffectsEngine.add(
                new FireballEngine(
                        origin,
                        speed,
                        maxRadius
                )
        );
    }

    @Override
    public void tick() {
        age++;
        radius += speed;

        int fadeEndTicks = (int) Math.ceil(maxRadius / speed) + FADE_TICKS;

        if (age >= fadeEndTicks) {
            alive = false;
        }
    }

    @Override
    public void render(
            PoseStack poseStack,
            Camera camera,
            MultiBufferSource.BufferSource bufferSource,
            float partialTick
    ) {
        Vec3 cameraPosition = camera.getPosition();

        double dx = origin.x - cameraPosition.x;
        double dy = origin.y - cameraPosition.y;
        double dz = origin.z - cameraPosition.z;

        if (dx * dx + dy * dy + dz * dz >
                MAX_RENDER_DISTANCE * MAX_RENDER_DISTANCE) {
            return;
        }

        float renderRadius = radius;

        if (radius <= 0.0f) {
            return;
        }

        float growthTicks = (float) Math.ceil(maxRadius / speed);
        float fadeStart = growthTicks;
        float fadeTime = FADE_TICKS;

        float fadeProgress = 0.0f;

        if (age > fadeStart) {
            fadeProgress = Mth.clamp(
                    (age - fadeStart + partialTick) / fadeTime,
                    0.0f,
                    1.0f
            );
        }

        int red = 255;
        int green = 250;
        int blue = 235;
        int alpha = (int) (255.0f * (1.0f - fadeProgress));

        int glowRed = 255;
        int glowGreen = 140;
        int glowBlue = 40;
        int glowAlpha = (int) (160.0f * (1.0f - fadeProgress));
        float glowRadius = renderRadius * 1.25f;

        if (alpha <= 0 && glowAlpha <= 0) {
            return;
        }

        float centerX = (float) (origin.x - cameraPosition.x);
        float centerY = (float) (origin.y - cameraPosition.y);
        float centerZ = (float) (origin.z - cameraPosition.z);

        if (alpha > 0) {
            var buffer = bufferSource.getBuffer(
                    FIREBALL_RENDER_TYPE
            );

            for (int ring = 0; ring < RINGS; ring++) {
                float v0 = (float) ring / RINGS;
                float v1 = (float) (ring + 1) / RINGS;

                float phi0 = v0 * Mth.PI;
                float phi1 = v1 * Mth.PI;

                float y0 = Mth.cos(phi0);
                float y1 = Mth.cos(phi1);

                float ringRadius0 = Mth.sin(phi0);
                float ringRadius1 = Mth.sin(phi1);

                for (int segment = 0; segment < SEGMENTS; segment++) {
                    float u0 = (float) segment / SEGMENTS;
                    float u1 = (float) (segment + 1) / SEGMENTS;

                    float theta0 = u0 * Mth.TWO_PI;
                    float theta1 = u1 * Mth.TWO_PI;

                    float x00 = centerX + Mth.cos(theta0) * ringRadius0 * renderRadius;
                    float z00 = centerZ + Mth.sin(theta0) * ringRadius0 * renderRadius;
                    float y00 = centerY + y0 * renderRadius;

                    float x01 = centerX + Mth.cos(theta1) * ringRadius0 * renderRadius;
                    float z01 = centerZ + Mth.sin(theta1) * ringRadius0 * renderRadius;
                    float y01 = centerY + y0 * renderRadius;

                    float x11 = centerX + Mth.cos(theta1) * ringRadius1 * renderRadius;
                    float z11 = centerZ + Mth.sin(theta1) * ringRadius1 * renderRadius;
                    float y11 = centerY + y1 * renderRadius;

                    float x10 = centerX + Mth.cos(theta0) * ringRadius1 * renderRadius;
                    float z10 = centerZ + Mth.sin(theta0) * ringRadius1 * renderRadius;
                    float y10 = centerY + y1 * renderRadius;

                    buffer.addVertex(x00, y00, z00)
                            .setColor(red, green, blue, alpha);

                    buffer.addVertex(x01, y01, z01)
                            .setColor(red, green, blue, alpha);

                    buffer.addVertex(x11, y11, z11)
                            .setColor(red, green, blue, alpha);

                    buffer.addVertex(x10, y10, z10)
                            .setColor(red, green, blue, alpha);
                }
            }
        }

        if (glowAlpha > 0) {
            var glowBuffer = bufferSource.getBuffer(
                    FIREBALL_GLOW_RENDER_TYPE
            );

            for (int ring = 0; ring < RINGS; ring++) {
                float v0 = (float) ring / RINGS;
                float v1 = (float) (ring + 1) / RINGS;

                float phi0 = v0 * Mth.PI;
                float phi1 = v1 * Mth.PI;

                float y0 = Mth.cos(phi0);
                float y1 = Mth.cos(phi1);

                float ringRadius0 = Mth.sin(phi0);
                float ringRadius1 = Mth.sin(phi1);

                for (int segment = 0; segment < SEGMENTS; segment++) {
                    float u0 = (float) segment / SEGMENTS;
                    float u1 = (float) (segment + 1) / SEGMENTS;

                    float theta0 = u0 * Mth.TWO_PI;
                    float theta1 = u1 * Mth.TWO_PI;

                    float x00 = centerX + Mth.cos(theta0) * ringRadius0 * glowRadius;
                    float z00 = centerZ + Mth.sin(theta0) * ringRadius0 * glowRadius;
                    float y00 = centerY + y0 * glowRadius;

                    float x01 = centerX + Mth.cos(theta1) * ringRadius0 * glowRadius;
                    float z01 = centerZ + Mth.sin(theta1) * ringRadius0 * glowRadius;
                    float y01 = centerY + y0 * glowRadius;

                    float x11 = centerX + Mth.cos(theta1) * ringRadius1 * glowRadius;
                    float z11 = centerZ + Mth.sin(theta1) * ringRadius1 * glowRadius;
                    float y11 = centerY + y1 * glowRadius;

                    float x10 = centerX + Mth.cos(theta0) * ringRadius1 * glowRadius;
                    float z10 = centerZ + Mth.sin(theta0) * ringRadius1 * glowRadius;
                    float y10 = centerY + y1 * glowRadius;

                    glowBuffer.addVertex(x00, y00, z00)
                            .setColor(glowRed, glowGreen, glowBlue, glowAlpha);

                    glowBuffer.addVertex(x01, y01, z01)
                            .setColor(glowRed, glowGreen, glowBlue, glowAlpha);

                    glowBuffer.addVertex(x11, y11, z11)
                            .setColor(glowRed, glowGreen, glowBlue, glowAlpha);

                    glowBuffer.addVertex(x10, y10, z10)
                            .setColor(glowRed, glowGreen, glowBlue, glowAlpha);
                }
            }
        }

    }

    @Override
    public boolean isAlive() {
        return alive;
    }
}