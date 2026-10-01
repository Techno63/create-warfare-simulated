package com.julian.createwarfare.effects.client.engines;

import com.julian.createwarfare.CreateWarfare;
import com.julian.createwarfare.effects.client.visual.VisualEffects;
import com.julian.createwarfare.effects.client.visual.VisualEffectsEngine;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = CreateWarfare.MODID, value = Dist.CLIENT)
public final class WaveEngine implements VisualEffects {

    private static final int SEGMENTS = 48;
    private static final int RINGS = 32;
    private static final float MAX_RENDER_DISTANCE = 3072.0f;
    private static final int FADE_TICKS = 20;
    private static final int WAVE_ALPHA = 140;
    private static final int MAX_SCREEN_TINT_ALPHA = 90;

    // 1 tick = 50 ms. Same clock as PressureWaveEffect, so they can't drift apart.
    private static final float NANOS_PER_TICK = 50_000_000.0f;

    // Offset in ticks. 0 = exact match. Negative = visual starts ahead (e.g. -1 for ping).
    private static final float VISUAL_OFFSET_TICKS = 0.0f;

    private static int activeTintColor = 0xffffff;
    private static int activeTintAlpha = 0;

    private static final RenderType WAVE_RENDER_TYPE = RenderType.create(
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

    private final Vec3 origin;
    private final float speed;        // blocks per tick
    private final float maxRadius;
    private final int color;
    private final float transparency;
    private final long startNanos = System.nanoTime();

    private boolean alive = true;

    private WaveEngine(Vec3 origin, float speed, float maxRadius, int color, float transparency) {
        this.origin = origin;
        this.speed = Math.max(speed, 0.01f);
        this.maxRadius = Math.max(maxRadius, 0.01f);
        this.color = color & 0xffffff;
        this.transparency = Mth.clamp(transparency, 0.0f, 1.0f);
    }

    public static void spawn(Vec3 origin, float speed, float maxRadius, int color, float transparency) {
        VisualEffectsEngine.add(new WaveEngine(origin, speed, maxRadius, color, transparency));
    }

    private float elapsedTicks() {
        return Math.max((System.nanoTime() - startNanos) / NANOS_PER_TICK - VISUAL_OFFSET_TICKS, 0.0f);
    }

    @Override
    public void tick() {
        if (elapsedTicks() >= maxRadius / speed + FADE_TICKS) {
            alive = false;
        }
    }

    @Override
    public void render(PoseStack poseStack, Camera camera, MultiBufferSource.BufferSource bufferSource, float partialTick) {
        Vec3 cam = camera.getPosition();

        float cx = (float) (origin.x - cam.x);
        float cy = (float) (origin.y - cam.y);
        float cz = (float) (origin.z - cam.z);

        double distanceSqr = (double) cx * cx + (double) cy * cy + (double) cz * cz;

        if (distanceSqr > MAX_RENDER_DISTANCE * MAX_RENDER_DISTANCE) {
            return;
        }

        float time = elapsedTicks();
        float radius = Math.min(time * speed, maxRadius);

        if (radius <= 0.0f) {
            return;
        }

        float growthTicks = maxRadius / speed;
        float fade = time > growthTicks ? Mth.clamp((time - growthTicks) / FADE_TICKS, 0.0f, 1.0f) : 0.0f;
        float visibility = 1.0f - transparency;

        int alpha = (int) (WAVE_ALPHA * visibility * (1.0f - fade));

        if (alpha <= 0) {
            return;
        }

        // Screen tint when the camera is inside the wave
        float distance = (float) Math.sqrt(distanceSqr);

        if (distance < radius) {
            float inside = 1.0f - Mth.clamp(distance / radius, 0.0f, 1.0f);
            inside *= inside;

            int tintAlpha = (int) (MAX_SCREEN_TINT_ALPHA * visibility * inside * (1.0f - fade));

            if (tintAlpha > activeTintAlpha) {
                activeTintAlpha = tintAlpha;
                activeTintColor = color;
            }
        }

        int r = (color >> 16) & 0xff;
        int g = (color >> 8) & 0xff;
        int b = color & 0xff;

        var buffer = bufferSource.getBuffer(WAVE_RENDER_TYPE);

        for (int ring = 0; ring < RINGS; ring++) {
            float phi0 = ((float) ring / RINGS) * Mth.PI;
            float phi1 = ((float) (ring + 1) / RINGS) * Mth.PI;

            float y0 = cy + Mth.cos(phi0) * radius;
            float y1 = cy + Mth.cos(phi1) * radius;
            float rr0 = Mth.sin(phi0) * radius;
            float rr1 = Mth.sin(phi1) * radius;

            for (int seg = 0; seg < SEGMENTS; seg++) {
                float t0 = ((float) seg / SEGMENTS) * Mth.TWO_PI;
                float t1 = ((float) (seg + 1) / SEGMENTS) * Mth.TWO_PI;

                float c0 = Mth.cos(t0), s0 = Mth.sin(t0);
                float c1 = Mth.cos(t1), s1 = Mth.sin(t1);

                buffer.addVertex(cx + c0 * rr0, y0, cz + s0 * rr0).setColor(r, g, b, alpha);
                buffer.addVertex(cx + c1 * rr0, y0, cz + s1 * rr0).setColor(r, g, b, alpha);
                buffer.addVertex(cx + c1 * rr1, y1, cz + s1 * rr1).setColor(r, g, b, alpha);
                buffer.addVertex(cx + c0 * rr1, y1, cz + s0 * rr1).setColor(r, g, b, alpha);
            }
        }
    }

    @SubscribeEvent
    public static void renderScreenTint(RenderGuiEvent.Post event) {
        if (activeTintAlpha <= 0) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        int argb = (activeTintAlpha << 24) | (activeTintColor & 0xffffff);

        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), argb);

        activeTintAlpha = 0;
    }

    @Override
    public boolean isAlive() {
        return alive;
    }
}