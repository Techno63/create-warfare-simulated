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

@EventBusSubscriber(
        modid = CreateWarfare.MODID,
        value = Dist.CLIENT
)
public final class WaveEngine implements VisualEffects {

    private static final int SEGMENTS = 48;
    private static final int RINGS = 32;

    private static final float MAX_RENDER_DISTANCE = 3072.0f;

    private static final int FADE_TICKS = 20;

    private static final int WAVE_ALPHA = 255;

    private static final int MAX_SCREEN_TINT_ALPHA = 200;

    private static final float NANOS_PER_TICK =
            50_000_000.0f;

    private static final float VISUAL_OFFSET_TICKS =
            0.0f;

    private static final float GLOW_1_SIZE =
            1.05f;

    private static final float GLOW_2_SIZE =
            1.12f;

    private static final float GLOW_3_SIZE =
            1.22f;

    private static final float GLOW_4_SIZE =
            1.35f;

    private static final float GLOW_1_ALPHA =
            0.16f;

    private static final float GLOW_2_ALPHA =
            0.09f;

    private static final float GLOW_3_ALPHA =
            0.045f;

    private static final float GLOW_4_ALPHA =
            0.02f;

    private static int activeTintColor =
            0xffffff;

    private static int activeTintAlpha =
            0;

    private static final RenderType WAVE_RENDER_TYPE =
            RenderType.create(
                    "createwarfare_fireball",
                    DefaultVertexFormat.POSITION_COLOR,
                    VertexFormat.Mode.QUADS,
                    256,
                    false,
                    true,
                    RenderType.CompositeState.builder()
                            .setShaderState(
                                    RenderStateShard.POSITION_COLOR_SHADER
                            )
                            .setTransparencyState(
                                    RenderStateShard.TRANSLUCENT_TRANSPARENCY
                            )
                            .setWriteMaskState(
                                    RenderStateShard.COLOR_DEPTH_WRITE
                            )
                            .setCullState(
                                    RenderStateShard.NO_CULL
                            )
                            .createCompositeState(false)
            );

    private static final RenderType WAVE_GLOW_RENDER_TYPE =
            RenderType.create(
                    "createwarfare_fireball_glow",
                    DefaultVertexFormat.POSITION_COLOR,
                    VertexFormat.Mode.QUADS,
                    256,
                    false,
                    true,
                    RenderType.CompositeState.builder()
                            .setShaderState(
                                    RenderStateShard.POSITION_COLOR_SHADER
                            )
                            .setTransparencyState(
                                    RenderStateShard.TRANSLUCENT_TRANSPARENCY
                            )
                            .setWriteMaskState(
                                    RenderStateShard.COLOR_WRITE
                            )
                            .setCullState(
                                    RenderStateShard.NO_CULL
                            )
                            .createCompositeState(false)
            );

    private final Vec3 origin;

    private final float speed;

    private final float maxRadius;

    private final int color;

    private final float alpha;

    private final int stayTicks;

    private final boolean glow;

    private final long startNanos =
            System.nanoTime();

    private boolean alive =
            true;

    private WaveEngine(
            Vec3 origin,
            float speed,
            float maxRadius,
            int color,
            float alpha,
            int stayTicks,
            boolean glow
    ) {
        this.origin =
                origin;

        this.speed =
                Math.max(
                        speed,
                        0.01f
                );

        this.maxRadius =
                Math.max(
                        maxRadius,
                        0.01f
                );

        this.color =
                color &
                        0xffffff;

        this.alpha =
                Mth.clamp(
                        alpha,
                        0.0f,
                        1.0f
                );

        this.stayTicks =
                Math.max(
                        stayTicks,
                        0
                );

        this.glow =
                glow;
    }

    public static void spawn(
            Vec3 origin,
            float speed,
            float maxRadius,
            int color,
            float alpha,
            int stayTicks,
            boolean glow
    ) {
        VisualEffectsEngine.add(
                new WaveEngine(
                        origin,
                        speed,
                        maxRadius,
                        color,
                        alpha,
                        stayTicks,
                        glow
                )
        );
    }

    private float elapsedTicks() {
        return Math.max(
                (
                        System.nanoTime() -
                                startNanos
                ) /
                        NANOS_PER_TICK -
                        VISUAL_OFFSET_TICKS,
                0.0f
        );
    }

    @Override
    public void tick() {
        float growthTicks =
                maxRadius /
                        speed;

        float totalTicks;

        if (stayTicks > 0) {
            totalTicks =
                    growthTicks +
                            stayTicks;
        } else {
            totalTicks =
                    growthTicks +
                            FADE_TICKS;
        }

        if (
                elapsedTicks() >=
                        totalTicks
        ) {
            alive =
                    false;
        }
    }

    @Override
    public void render(
            PoseStack poseStack,
            Camera camera,
            MultiBufferSource.BufferSource bufferSource,
            float partialTick
    ) {
        Vec3 cam =
                camera.getPosition();

        float cx =
                (float) (
                        origin.x -
                                cam.x
                );

        float cy =
                (float) (
                        origin.y -
                                cam.y
                );

        float cz =
                (float) (
                        origin.z -
                                cam.z
                );

        double distanceSqr =
                (double) cx * cx +
                        (double) cy * cy +
                        (double) cz * cz;

        if (
                distanceSqr >
                        MAX_RENDER_DISTANCE *
                                MAX_RENDER_DISTANCE
        ) {
            return;
        }

        float time =
                elapsedTicks();

        float growthTicks =
                maxRadius /
                        speed;

        float radius =
                Math.min(
                        time * speed,
                        maxRadius
                );

        if (radius <= 0.0f) {
            return;
        }

        float visibility;

        if (stayTicks > 0) {
            if (time <= growthTicks) {
                visibility =
                        1.0f;
            } else {
                float fadeProgress =
                        Mth.clamp(
                                (
                                        time -
                                                growthTicks
                                ) /
                                        stayTicks,
                                0.0f,
                                1.0f
                        );

                visibility =
                        1.0f -
                                fadeProgress;
            }
        } else {
            float totalEffectTicks =
                    growthTicks +
                            FADE_TICKS;

            visibility =
                    1.0f -
                            Mth.clamp(
                                    time /
                                            totalEffectTicks,
                                    0.0f,
                                    1.0f
                            );
        }

        float finalAlpha =
                alpha *
                        visibility;

        int alphaValue =
                Mth.clamp(
                        Math.round(
                                WAVE_ALPHA *
                                        finalAlpha
                        ),
                        0,
                        255
                );

        if (alphaValue <= 0) {
            return;
        }

        float distance =
                (float)
                        Math.sqrt(
                                distanceSqr
                        );

        if (distance < radius) {
            float inside =
                    1.0f -
                            Mth.clamp(
                                    distance /
                                            radius,
                                    0.0f,
                                    1.0f
                            );

            inside *=
                    inside;

            int tintAlpha =
                    Mth.clamp(
                            Math.round(
                                    MAX_SCREEN_TINT_ALPHA *
                                            alpha *
                                            inside *
                                            visibility
                            ),
                            0,
                            255
                    );

            if (
                    tintAlpha >
                            activeTintAlpha
            ) {
                activeTintAlpha =
                        tintAlpha;

                activeTintColor =
                        color;
            }
        }

        int r =
                (color >> 16) &
                        0xff;

        int g =
                (color >> 8) &
                        0xff;

        int b =
                color &
                        0xff;

        if (glow) {
            renderWave(
                    bufferSource,
                    cx,
                    cy,
                    cz,
                    radius *
                            GLOW_4_SIZE,
                    r,
                    g,
                    b,
                    Math.round(
                            alphaValue *
                                    GLOW_4_ALPHA
                    ),
                    WAVE_GLOW_RENDER_TYPE
            );

            renderWave(
                    bufferSource,
                    cx,
                    cy,
                    cz,
                    radius *
                            GLOW_3_SIZE,
                    r,
                    g,
                    b,
                    Math.round(
                            alphaValue *
                                    GLOW_3_ALPHA
                    ),
                    WAVE_GLOW_RENDER_TYPE
            );

            renderWave(
                    bufferSource,
                    cx,
                    cy,
                    cz,
                    radius *
                            GLOW_2_SIZE,
                    r,
                    g,
                    b,
                    Math.round(
                            alphaValue *
                                    GLOW_2_ALPHA
                    ),
                    WAVE_GLOW_RENDER_TYPE
            );

            renderWave(
                    bufferSource,
                    cx,
                    cy,
                    cz,
                    radius *
                            GLOW_1_SIZE,
                    r,
                    g,
                    b,
                    Math.round(
                            alphaValue *
                                    GLOW_1_ALPHA
                    ),
                    WAVE_GLOW_RENDER_TYPE
            );
        }

        renderWave(
                bufferSource,
                cx,
                cy,
                cz,
                radius,
                r,
                g,
                b,
                alphaValue,
                WAVE_RENDER_TYPE
        );
    }

    private static void renderWave(
            MultiBufferSource.BufferSource bufferSource,
            float cx,
            float cy,
            float cz,
            float radius,
            int r,
            int g,
            int b,
            int alphaValue,
            RenderType renderType
    ) {
        if (alphaValue <= 0) {
            return;
        }

        var buffer =
                bufferSource.getBuffer(
                        renderType
                );

        for (
                int ring = 0;
                ring < RINGS;
                ring++
        ) {
            float phi0 =
                    (
                            (float) ring /
                                    RINGS
                    ) *
                            Mth.PI;

            float phi1 =
                    (
                            (float) (
                                    ring + 1
                            ) /
                                    RINGS
                    ) *
                            Mth.PI;

            float y0 =
                    cy +
                            Mth.cos(phi0) *
                                    radius;

            float y1 =
                    cy +
                            Mth.cos(phi1) *
                                    radius;

            float rr0 =
                    Mth.sin(phi0) *
                            radius;

            float rr1 =
                    Mth.sin(phi1) *
                            radius;

            for (
                    int seg = 0;
                    seg < SEGMENTS;
                    seg++
            ) {
                float t0 =
                        (
                                (float) seg /
                                        SEGMENTS
                        ) *
                                Mth.TWO_PI;

                float t1 =
                        (
                                (float) (
                                        seg + 1
                                ) /
                                        SEGMENTS
                        ) *
                                Mth.TWO_PI;

                float c0 =
                        Mth.cos(t0);

                float s0 =
                        Mth.sin(t0);

                float c1 =
                        Mth.cos(t1);

                float s1 =
                        Mth.sin(t1);

                buffer
                        .addVertex(
                                cx +
                                        c0 *
                                                rr0,
                                y0,
                                cz +
                                        s0 *
                                                rr0
                        )
                        .setColor(
                                r,
                                g,
                                b,
                                alphaValue
                        );

                buffer
                        .addVertex(
                                cx +
                                        c1 *
                                                rr0,
                                y0,
                                cz +
                                        s1 *
                                                rr0
                        )
                        .setColor(
                                r,
                                g,
                                b,
                                alphaValue
                        );

                buffer
                        .addVertex(
                                cx +
                                        c1 *
                                                rr1,
                                y1,
                                cz +
                                        s1 *
                                                rr1
                        )
                        .setColor(
                                r,
                                g,
                                b,
                                alphaValue
                        );

                buffer
                        .addVertex(
                                cx +
                                        c0 *
                                                rr1,
                                y1,
                                cz +
                                        s0 *
                                                rr1
                        )
                        .setColor(
                                r,
                                g,
                                b,
                                alphaValue
                        );
            }
        }
    }

    @SubscribeEvent
    public static void renderScreenTint(
            RenderGuiEvent.Post event
    ) {
        if (activeTintAlpha <= 0) {
            return;
        }

        GuiGraphics graphics =
                event.getGuiGraphics();

        int argb =
                (activeTintAlpha << 24) |
                        (
                                activeTintColor &
                                        0xffffff
                        );

        graphics.fill(
                0,
                0,
                graphics.guiWidth(),
                graphics.guiHeight(),
                argb
        );

        activeTintAlpha =
                0;
    }

    @Override
    public boolean isAlive() {
        return alive;
    }
}