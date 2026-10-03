package com.julian.createwarfare.effects.client;

import com.julian.createwarfare.CreateWarfare;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@EventBusSubscriber(
        modid = CreateWarfare.MODID,
        value = Dist.CLIENT
)
public class ScreenGlowHandler {
    private static final List<Glow> GLOWS = new ArrayList<>();

    public static void glow(
            Vec3 position,
            float radius,
            int color,
            float intensity,
            int lifetime
    ) {
        if (Minecraft.getInstance().level == null) {
            return;
        }

        GLOWS.add(
                new Glow(
                        position,
                        radius,
                        color,
                        intensity,
                        lifetime
                )
        );
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Iterator<Glow> iterator = GLOWS.iterator();

        while (iterator.hasNext()) {
            Glow glow = iterator.next();

            glow.age++;

            if (glow.age >= glow.lifetime) {
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (GLOWS.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null) {
            GLOWS.clear();
            return;
        }

        float partialTick =
                event.getPartialTick()
                        .getGameTimeDeltaPartialTick(false);

        PoseStack poseStack =
                event.getGuiGraphics().pose();

        Camera camera =
                minecraft.gameRenderer.getMainCamera();

        int width =
                minecraft.getWindow().getGuiScaledWidth();

        int height =
                minecraft.getWindow().getGuiScaledHeight();

        for (Glow glow : GLOWS) {
            renderGlow(
                    poseStack,
                    camera,
                    glow,
                    partialTick,
                    width,
                    height
            );
        }
    }

    private static void renderGlow(
            PoseStack poseStack,
            Camera camera,
            Glow glow,
            float partialTick,
            int width,
            int height
    ) {
        Vec3 relative =
                glow.position.subtract(
                        camera.getPosition()
                );

        Vector3f forward =
                new Vector3f(camera.getLookVector());

        Vector3f up =
                new Vector3f(camera.getUpVector());

        Vector3f right =
                new Vector3f(up)
                        .cross(forward)
                        .normalize();

        double depth =
                relative.x * forward.x()
                        + relative.y * forward.y()
                        + relative.z * forward.z();

        if (depth <= 0.05) {
            return;
        }

        double horizontal =
                -(relative.x * right.x()
                        + relative.y * right.y()
                        + relative.z * right.z());

        double vertical =
                relative.x * up.x()
                        + relative.y * up.y()
                        + relative.z * up.z();

        float fov =
                (float) Minecraft.getInstance()
                        .options
                        .fov()
                        .get();

        double aspect =
                (double) width
                        / Math.max(1, height);

        double tanHalfFov =
                Math.tan(
                        Math.toRadians(fov) * 0.5
                );

        double ndcX =
                horizontal
                        / (depth * tanHalfFov * aspect);

        double ndcY =
                vertical
                        / (depth * tanHalfFov);

        float screenX =
                (float) (
                        (ndcX + 1.0)
                                * 0.5
                                * width
                );

        float screenY =
                (float) (
                        (1.0 - ndcY)
                                * 0.5
                                * height
                );

        float distance =
                (float) relative.length();

        float screenRadius =
                (float) (
                        width
                                * 0.115
                                * glow.radius
                                / Math.max(
                                distance,
                                0.1f
                        )
                );

        screenRadius =
                clamp(
                        screenRadius,
                        2.0f,
                        Math.min(width, height) * 2.0f
                );

        float progress =
                clamp01(
                        (glow.age + partialTick)
                                / glow.lifetime
                );

        float fadeIn =
                smoothStep(
                        0.0f,
                        0.045f,
                        progress
                );

        float fadeOut =
                1.0f
                        - smoothStep(
                        0.65f,
                        1.0f,
                        progress
                );

        float strength =
                glow.intensity
                        * fadeIn
                        * fadeOut;

        if (strength <= 0.001f) {
            return;
        }

        int red =
                (glow.color >> 16) & 0xFF;

        int green =
                (glow.color >> 8) & 0xFF;

        int blue =
                glow.color & 0xFF;

        renderSolidCore(
                poseStack,
                screenX,
                screenY,
                screenRadius,
                red,
                green,
                blue,
                strength
        );

        renderRadialGlow(
                poseStack,
                screenX,
                screenY,
                screenRadius,
                red,
                green,
                blue,
                strength
        );
    }

    private static void renderSolidCore(
            PoseStack poseStack,
            float centerX,
            float centerY,
            float radius,
            int red,
            int green,
            int blue,
            float strength
    ) {
        float coreRadius =
                radius * 0.34f;

        int alpha =
                Math.round(
                        255.0f
                                * 0.95f
                                * clamp01(strength)
                );

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(
                770,
                771
        );
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(
                GameRenderer::getPositionColorShader
        );

        Tesselator tesselator =
                Tesselator.getInstance();

        BufferBuilder buffer =
                tesselator.begin(
                        VertexFormat.Mode.TRIANGLE_FAN,
                        DefaultVertexFormat.POSITION_COLOR
                );

        buffer.addVertex(
                poseStack.last().pose(),
                centerX,
                centerY,
                0.0f
        ).setColor(
                red,
                green,
                blue,
                alpha
        );

        int segments = 96;

        for (int i = 0; i <= segments; i++) {
            double angle =
                    Math.PI * 2.0
                            * i
                            / segments;

            float cos =
                    (float) Math.cos(angle);

            float sin =
                    (float) Math.sin(angle);

            buffer.addVertex(
                    poseStack.last().pose(),
                    centerX + cos * coreRadius,
                    centerY + sin * coreRadius,
                    0.0f
            ).setColor(
                    red,
                    green,
                    blue,
                    alpha
            );
        }

        BufferUploader.drawWithShader(
                buffer.buildOrThrow()
        );

        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void renderRadialGlow(
            PoseStack poseStack,
            float centerX,
            float centerY,
            float radius,
            int red,
            int green,
            int blue,
            float strength
    ) {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(
                770,
                1
        );
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(
                GameRenderer::getPositionColorShader
        );

        Tesselator tesselator =
                Tesselator.getInstance();

        int rings = 96;
        int segments = 96;

        for (int ring = 0; ring < rings; ring++) {
            float outer =
                    radius
                            * (
                            1.0f
                                    - ring
                                    / (float) rings
                    );

            float inner =
                    radius
                            * (
                            1.0f
                                    - (ring + 1)
                                    / (float) rings
                    );

            float outerNormalized =
                    outer / radius;

            float innerNormalized =
                    inner / radius;

            float outerFalloff =
                    smoothFalloff(
                            outerNormalized
                    );

            float innerFalloff =
                    smoothFalloff(
                            innerNormalized
                    );

            float outerAlpha =
                    outerFalloff
                            * strength
                            * 0.95f;

            float innerAlpha =
                    innerFalloff
                            * strength
                            * 0.95f;

            int outerRed =
                    lighten(
                            red,
                            outerNormalized
                    );

            int outerGreen =
                    lighten(
                            green,
                            outerNormalized
                    );

            int outerBlue =
                    lighten(
                            blue,
                            outerNormalized
                    );

            int innerRed =
                    lighten(
                            red,
                            innerNormalized
                    );

            int innerGreen =
                    lighten(
                            green,
                            innerNormalized
                    );

            int innerBlue =
                    lighten(
                            blue,
                            innerNormalized
                    );

            BufferBuilder buffer =
                    tesselator.begin(
                            VertexFormat.Mode.TRIANGLE_STRIP,
                            DefaultVertexFormat.POSITION_COLOR
                    );

            for (int i = 0; i <= segments; i++) {
                double angle =
                        Math.PI * 2.0
                                * i
                                / segments;

                float cos =
                        (float) Math.cos(angle);

                float sin =
                        (float) Math.sin(angle);

                buffer.addVertex(
                        poseStack.last().pose(),
                        centerX + cos * outer,
                        centerY + sin * outer,
                        0.0f
                ).setColor(
                        outerRed,
                        outerGreen,
                        outerBlue,
                        Math.round(
                                clamp01(
                                        outerAlpha
                                ) * 255.0f
                        )
                );

                buffer.addVertex(
                        poseStack.last().pose(),
                        centerX + cos * inner,
                        centerY + sin * inner,
                        0.0f
                ).setColor(
                        innerRed,
                        innerGreen,
                        innerBlue,
                        Math.round(
                                clamp01(
                                        innerAlpha
                                ) * 255.0f
                        )
                );
            }

            BufferUploader.drawWithShader(
                    buffer.buildOrThrow()
            );
        }

        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static int lighten(
            int channel,
            float normalizedDistance
    ) {
        float x =
                clamp01(
                        normalizedDistance
                );

        x =
                smoothStep(
                        0.0f,
                        1.0f,
                        x
                );

        x =
                x * x
                        * (
                        3.0f
                                - 2.0f * x
                );

        return Math.round(
                channel
                        + (
                        255 - channel
                ) * x
        );
    }

    private static float smoothFalloff(
            float normalizedDistance
    ) {
        float x =
                clamp01(
                        normalizedDistance
                );

        float falloff =
                1.0f - x;

        falloff =
                smoothStep(
                        0.0f,
                        1.0f,
                        falloff
                );

        falloff =
                falloff * falloff;

        return falloff;
    }

    private static float smoothStep(
            float edge0,
            float edge1,
            float value
    ) {
        float x =
                clamp01(
                        (value - edge0)
                                / (edge1 - edge0)
                );

        return x
                * x
                * (
                3.0f
                        - 2.0f * x
        );
    }

    private static float clamp01(
            float value
    ) {
        return Math.max(
                0.0f,
                Math.min(
                        1.0f,
                        value
                )
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

    private static final class Glow {
        private final Vec3 position;
        private final float radius;
        private final int color;
        private final float intensity;
        private final int lifetime;
        private int age;

        private Glow(
                Vec3 position,
                float radius,
                int color,
                float intensity,
                int lifetime
        ) {
            this.position = position;
            this.radius = Math.max(
                    0.1f,
                    radius
            );
            this.color =
                    color & 0xFFFFFF;
            this.intensity =
                    Math.max(
                            0.0f,
                            intensity
                    );
            this.lifetime =
                    Math.max(
                            1,
                            lifetime
                    );
        }
    }
}