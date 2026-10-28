package com.julian.createwarfare.effects.client.engines;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = "createwarfare", value = Dist.CLIENT)
public final class TracerEngine {

    private static final Map<Integer, Tracer> TRACERS = new HashMap<>();

    private static final int MAX_SAMPLES = 12;
    private static final int SIDES = 8;

    private static final double MAX_RENDER_DISTANCE_SQR = 1600.0;
    private static final long INTERPOLATION_DELAY_NS = 85_000_000L;
    private static final long EXTRAPOLATION_LIMIT_NS = 25_000_000L;
    private static final long FADE_DURATION_NS = 180_000_000L;
    private static final long FADE_START_NS = 45_000_000L;

    private static final double MIN_LENGTH = 0.7;
    private static final double SPEED_LENGTH_FACTOR = 0.17;
    private static final double MAX_LENGTH = 10.0;

    private static final double WIDTH_MULTIPLIER = 1.4;
    private static final double MIN_WIDTH = 0.025;
    private static final double MAX_WIDTH = 0.22;

    private static final int[] TAIL_COLOR = {255, 145, 8};
    private static final int[] MID_COLOR = {255, 205, 12};
    private static final int[] TIP_COLOR = {255, 250, 170};

    private TracerEngine() {
    }

    public static void accept(Vec3 position, float width, float speed, int id) {
        if (position == null) {
            return;
        }

        long now = System.nanoTime();
        Tracer tracer = TRACERS.get(id);

        float safeWidth = Mth.clamp(width * (float) WIDTH_MULTIPLIER, (float) MIN_WIDTH, (float) MAX_WIDTH);
        float safeSpeed = Mth.clamp(speed, 0.0F, 256.0F);

        if (tracer == null) {
            TRACERS.put(id, new Tracer(position, safeWidth, safeSpeed, now));
            return;
        }

        Sample last = tracer.samples.peekLast();

        if (last != null && last.position.distanceToSqr(position) > 4096.0) {
            tracer.samples.clear();
            tracer.samples.add(new Sample(position, now));
            tracer.width = safeWidth;
            tracer.speed = safeSpeed;
            tracer.lastSeen = now;
            return;
        }

        if (last != null && now <= last.time) {
            now = last.time + 1L;
        }

        if (last != null && last.position.distanceToSqr(position) < 1.0E-10) {
            tracer.lastSeen = now;
            tracer.width = safeWidth;
            tracer.speed = safeSpeed;
            return;
        }

        tracer.samples.addLast(new Sample(position, now));

        while (tracer.samples.size() > MAX_SAMPLES) {
            tracer.samples.removeFirst();
        }

        tracer.width = safeWidth;
        tracer.speed = safeSpeed;
        tracer.lastSeen = now;
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null || minecraft.player == null) {
            TRACERS.clear();
            return;
        }

        long now = System.nanoTime();
        long targetTime = now - INTERPOLATION_DELAY_NS;
        Vec3 camera = event.getCamera().getPosition();

        List<TracerRenderData> visible = new ArrayList<>();
        Iterator<Map.Entry<Integer, Tracer>> iterator = TRACERS.entrySet().iterator();

        while (iterator.hasNext()) {
            Tracer tracer = iterator.next().getValue();
            long age = now - tracer.lastSeen;

            if (age > FADE_DURATION_NS || tracer.samples.isEmpty()) {
                iterator.remove();
                continue;
            }

            Vec3 position = interpolate(tracer, targetTime, now);

            if (position.distanceToSqr(camera) > MAX_RENDER_DISTANCE_SQR) {
                continue;
            }

            Vec3 direction = getDirection(tracer, targetTime);

            if (direction.lengthSqr() < 1.0E-8) {
                continue;
            }

            float fade = age <= FADE_START_NS
                    ? 1.0F
                    : 1.0F - Mth.clamp(
                    (age - FADE_START_NS) / (float) (FADE_DURATION_NS - FADE_START_NS),
                    0.0F,
                    1.0F
            );

            if (fade <= 0.001F) {
                continue;
            }

            visible.add(new TracerRenderData(
                    position,
                    direction.normalize(),
                    tracer.width,
                    tracer.speed,
                    fade
            ));
        }

        if (visible.isEmpty()) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();

        poseStack.pushPose();

        VertexConsumer consumer = buffers.getBuffer(RenderType.lightning());

        for (TracerRenderData tracer : visible) {
            renderTracer(consumer, poseStack, tracer, camera);
        }

        buffers.endBatch(RenderType.lightning());
        poseStack.popPose();
    }

    private static Vec3 interpolate(Tracer tracer, long targetTime, long now) {
        List<Sample> samples = new ArrayList<>(tracer.samples);

        if (samples.isEmpty()) {
            return Vec3.ZERO;
        }

        if (samples.size() == 1) {
            return samples.get(0).position;
        }

        Sample first = samples.get(0);
        Sample last = samples.get(samples.size() - 1);

        if (targetTime <= first.time) {
            return first.position;
        }

        for (int i = 0; i < samples.size() - 1; i++) {
            Sample a = samples.get(i);
            Sample b = samples.get(i + 1);

            if (targetTime < a.time || targetTime > b.time) {
                continue;
            }

            long interval = b.time - a.time;

            if (interval <= 0L) {
                return b.position;
            }

            double t = Mth.clamp(
                    (double) (targetTime - a.time) / interval,
                    0.0,
                    1.0
            );

            Vec3 delta = b.position.subtract(a.position);

            if (delta.lengthSqr() < 1.0E-12) {
                return a.position;
            }

            Vec3 tangentA = tangent(samples, i);
            Vec3 tangentB = tangent(samples, i + 1);

            double seconds = interval / 1_000_000_000.0;
            double t2 = t * t;
            double t3 = t2 * t;

            double h00 = 2.0 * t3 - 3.0 * t2 + 1.0;
            double h10 = t3 - 2.0 * t2 + t;
            double h01 = -2.0 * t3 + 3.0 * t2;
            double h11 = t3 - t2;

            Vec3 result = a.position.scale(h00)
                    .add(tangentA.scale(h10 * seconds))
                    .add(b.position.scale(h01))
                    .add(tangentB.scale(h11 * seconds));

            Vec3 linear = a.position.lerp(b.position, t);

            double maxDeviation = Math.max(0.015, delta.length() * 0.12);

            if (!Double.isFinite(result.x)
                    || !Double.isFinite(result.y)
                    || !Double.isFinite(result.z)
                    || result.distanceTo(linear) > maxDeviation) {
                return linear;
            }

            return result;
        }

        long extra = Math.min(
                Math.max(0L, now - last.time),
                EXTRAPOLATION_LIMIT_NS
        );

        Vec3 velocity = tangent(samples, samples.size() - 1);
        double maxExtrapolation = Math.max(0.5, tracer.speed * 0.025);
        Vec3 offset = velocity.scale(extra / 1_000_000_000.0);

        if (offset.length() > maxExtrapolation) {
            offset = offset.normalize().scale(maxExtrapolation);
        }

        return last.position.add(offset);
    }

    private static Vec3 tangent(List<Sample> samples, int index) {
        if (samples.size() < 2) {
            return Vec3.ZERO;
        }

        int before = Math.max(0, index - 1);
        int after = Math.min(samples.size() - 1, index + 1);

        Sample a = samples.get(before);
        Sample b = samples.get(after);

        long interval = b.time - a.time;

        if (interval <= 0L) {
            return Vec3.ZERO;
        }

        Vec3 velocity = b.position.subtract(a.position)
                .scale(1_000_000_000.0 / interval);

        double maximumVelocity = 1200.0;

        if (velocity.lengthSqr() > maximumVelocity * maximumVelocity) {
            velocity = velocity.normalize().scale(maximumVelocity);
        }

        return velocity;
    }

    private static Vec3 getDirection(Tracer tracer, long targetTime) {
        List<Sample> samples = new ArrayList<>(tracer.samples);

        if (samples.size() < 2) {
            return Vec3.ZERO;
        }

        for (int i = 0; i < samples.size() - 1; i++) {
            Sample a = samples.get(i);
            Sample b = samples.get(i + 1);

            if (targetTime >= a.time && targetTime <= b.time) {
                Vec3 difference = b.position.subtract(a.position);

                if (difference.lengthSqr() > 1.0E-8) {
                    return difference.normalize();
                }
            }
        }

        for (int i = samples.size() - 1; i > 0; i--) {
            Vec3 difference = samples.get(i).position
                    .subtract(samples.get(i - 1).position);

            if (difference.lengthSqr() > 1.0E-8) {
                return difference.normalize();
            }
        }

        return Vec3.ZERO;
    }

    private static void renderTracer(
            VertexConsumer consumer,
            PoseStack poseStack,
            TracerRenderData tracer,
            Vec3 camera
    ) {
        Vec3 direction = tracer.direction;

        if (direction.lengthSqr() < 1.0E-8) {
            return;
        }

        double length = Mth.clamp(
                MIN_LENGTH + tracer.speed * SPEED_LENGTH_FACTOR,
                MIN_LENGTH,
                MAX_LENGTH
        );

        Vec3 end = tracer.position;
        Vec3 start = end.subtract(direction.scale(length));

        Vec3 reference = Math.abs(direction.y) > 0.92
                ? new Vec3(1.0, 0.0, 0.0)
                : new Vec3(0.0, 1.0, 0.0);

        Vec3 right = direction.cross(reference).normalize();

        if (right.lengthSqr() < 1.0E-8) {
            right = new Vec3(1.0, 0.0, 0.0);
        }

        Vec3 up = direction.cross(right).normalize();

        if (up.lengthSqr() < 1.0E-8) {
            return;
        }

        Vec3 relativeStart = start.subtract(camera);
        Vec3 relativeEnd = end.subtract(camera);

        drawPrism(
                consumer,
                poseStack,
                relativeStart,
                relativeEnd,
                right,
                up,
                tracer.width,
                tracer.fade
        );
    }

    private static void drawPrism(
            VertexConsumer consumer,
            PoseStack poseStack,
            Vec3 start,
            Vec3 end,
            Vec3 right,
            Vec3 up,
            double width,
            float fade
    ) {
        Vec3[] startRing = new Vec3[SIDES];
        Vec3[] endRing = new Vec3[SIDES];

        double startRadius = width * 0.30;
        double endRadius = width * 1.05;

        for (int i = 0; i < SIDES; i++) {
            double angle = Math.PI * 2.0 * i / SIDES;
            Vec3 radial = right.scale(Math.cos(angle))
                    .add(up.scale(Math.sin(angle)));

            startRing[i] = start.add(radial.scale(startRadius));
            endRing[i] = end.add(radial.scale(endRadius));
        }

        PoseStack.Pose pose = poseStack.last();

        for (int i = 0; i < SIDES; i++) {
            int next = (i + 1) % SIDES;

            quad(
                    consumer,
                    pose,
                    startRing[i],
                    startRing[next],
                    endRing[next],
                    endRing[i],
                    colorAt(0.0, fade),
                    colorAt(0.0, fade),
                    colorAt(1.0, fade),
                    colorAt(1.0, fade)
            );
        }

        for (int i = 0; i < SIDES; i++) {
            int next = (i + 1) % SIDES;

            quad(
                    consumer,
                    pose,
                    endRing[i],
                    endRing[next],
                    end,
                    end,
                    colorAt(1.0, fade),
                    colorAt(1.0, fade),
                    colorAt(1.0, fade),
                    colorAt(1.0, fade)
            );
        }
    }

    private static int[] colorAt(double progress, float fade) {
        double t = Mth.clamp(progress, 0.0, 1.0);

        int red;
        int green;
        int blue;

        if (t < 0.68) {
            double blend = t / 0.68;
            red = lerpColor(TAIL_COLOR[0], MID_COLOR[0], blend);
            green = lerpColor(TAIL_COLOR[1], MID_COLOR[1], blend);
            blue = lerpColor(TAIL_COLOR[2], MID_COLOR[2], blend);
        } else {
            double blend = (t - 0.68) / 0.32;
            red = lerpColor(MID_COLOR[0], TIP_COLOR[0], blend);
            green = lerpColor(MID_COLOR[1], TIP_COLOR[1], blend);
            blue = lerpColor(MID_COLOR[2], TIP_COLOR[2], blend);
        }

        return new int[]{
                red,
                green,
                blue,
                Mth.clamp(Math.round(255.0F * fade), 0, 255)
        };
    }

    private static int lerpColor(int a, int b, double t) {
        return Mth.clamp((int) Math.round(a + (b - a) * t), 0, 255);
    }

    private static void quad(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            Vec3 a,
            Vec3 b,
            Vec3 c,
            Vec3 d,
            int[] colorA,
            int[] colorB,
            int[] colorC,
            int[] colorD
    ) {
        vertex(consumer, pose, a, colorA);
        vertex(consumer, pose, b, colorB);
        vertex(consumer, pose, c, colorC);
        vertex(consumer, pose, d, colorD);
    }

    private static void vertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            Vec3 position,
            int[] color
    ) {
        consumer.addVertex(
                pose,
                (float) position.x,
                (float) position.y,
                (float) position.z
        ).setColor(color[0], color[1], color[2], color[3]);
    }

    private record Sample(Vec3 position, long time) {
    }

    private record TracerRenderData(
            Vec3 position,
            Vec3 direction,
            float width,
            float speed,
            float fade
    ) {
    }

    private static final class Tracer {
        private final ArrayDeque<Sample> samples = new ArrayDeque<>();
        private long lastSeen;
        private float width;
        private float speed;

        private Tracer(Vec3 position, float width, float speed, long now) {
            samples.add(new Sample(position, now));
            this.lastSeen = now;
            this.width = width;
            this.speed = Math.max(0.0F, speed);
        }
    }
}