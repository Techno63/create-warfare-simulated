package com.julian.createwarfare.effects.server.particles;

import com.julian.createwarfare.effects.particles.smoke.SmokeParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@EventBusSubscriber(modid = "createwarfare")
public final class SplashParticleEffect {
    private static final List<Splash> ACTIVE = new ArrayList<>();

    private static final int MAX_VIEW_DISTANCE = 256;
    private static final double MAX_VIEW_DISTANCE_SQR = (double) MAX_VIEW_DISTANCE * MAX_VIEW_DISTANCE;
    private static final int MAX_ACTIVE_SPLASHES = 8;
    private static final float MAX_SCALE = 4.0F;

    private static final int BURST_TICKS = 3;
    private static final int CORE_PARTICLES = 7;
    private static final int CROWN_PARTICLES = 9;
    private static final int DROPLET_PARTICLES = 4;
    private static final int LIFETIME = 55;

    private static final double CONE_ANGLE = Math.toRadians(20.0D);
    private static final double CONE_HEIGHT = 2.4D;
    private static final double CORE_RADIUS = 0.65D;
    private static final double CROWN_RADIUS = 2.5D;

    private SplashParticleEffect() {
    }

    public static void start(ServerLevel level, BlockPos pos, float scale) {
        if (pos != null) {
            start(level, Vec3.atBottomCenterOf(pos), scale);
        }
    }

    public static void start(ServerLevel level, Vec3 pos, float scale) {
        if (level == null || pos == null || !Float.isFinite(scale) || scale <= 0.0F) {
            return;
        }

        scale = Mth.clamp(scale, 0.1F, MAX_SCALE);

        if (ACTIVE.size() >= MAX_ACTIVE_SPLASHES) {
            ACTIVE.remove(0);
        }

        List<ServerPlayer> recipients = new ArrayList<>();
        double viewDistanceSqr = MAX_VIEW_DISTANCE_SQR * Math.max(1.0D, scale * scale);

        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(pos) <= viewDistanceSqr) {
                recipients.add(player);
            }
        }

        if (!recipients.isEmpty()) {
            ACTIVE.add(new Splash(level, pos, scale, recipients, level.random));
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            tick(level);
        }
    }

    public static void tick(ServerLevel level) {
        Iterator<Splash> iterator = ACTIVE.iterator();

        while (iterator.hasNext()) {
            Splash splash = iterator.next();

            if (splash.level == level && !splash.tick()) {
                iterator.remove();
            }
        }
    }

    private static final class Splash {
        private final ServerLevel level;
        private final Vec3 origin;
        private final float scale;
        private final List<ServerPlayer> recipients;
        private final RandomSource random;

        private int age;

        private Splash(
                ServerLevel level,
                Vec3 origin,
                float scale,
                List<ServerPlayer> recipients,
                RandomSource random
        ) {
            this.level = level;
            this.origin = origin;
            this.scale = scale;
            this.recipients = recipients;
            this.random = random;
        }

        private boolean tick() {
            if (age < BURST_TICKS) {
                emitCore();
                emitCone();
                emitDroplets();
            }

            age++;
            return age < LIFETIME;
        }

        private void emitCore() {
            for (int i = 0; i < CORE_PARTICLES; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double radius = Math.sqrt(random.nextDouble()) * CORE_RADIUS * scale;

                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                double y = random.nextDouble() * 0.035D * scale;

                double speed = (0.12D + random.nextDouble() * 0.24D) * scale;
                double upwardSpeed = (0.12D + random.nextDouble() * 0.22D) * scale;

                emit(
                        origin.x + x,
                        origin.y + y,
                        origin.z + z,
                        splashColor(),
                        randomSize(2.8F, 4.0F),
                        25 + random.nextInt(18),
                        new Vec3(
                                Math.cos(angle) * speed,
                                upwardSpeed,
                                Math.sin(angle) * speed
                        )
                );
            }
        }

        private void emitCone() {
            for (int i = 0; i < CROWN_PARTICLES; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double heightProgress = 0.15D + random.nextDouble() * 0.85D;

                double height = CONE_HEIGHT * scale * heightProgress;
                double radius = Math.tan(CONE_ANGLE) * height;
                radius *= 0.8D + random.nextDouble() * 0.4D;

                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;

                double outwardSpeed = (0.18D + random.nextDouble() * 0.32D) * scale;
                double upwardSpeed = (0.22D + random.nextDouble() * 0.28D) * scale;

                emit(
                        origin.x + x,
                        origin.y + height,
                        origin.z + z,
                        random.nextFloat() < 0.82F ? whiteColor() : splashColor(),
                        randomSize(2.6F, 3.8F),
                        28 + random.nextInt(20),
                        new Vec3(
                                Math.cos(angle) * outwardSpeed,
                                upwardSpeed,
                                Math.sin(angle) * outwardSpeed
                        )
                );
            }
        }

        private void emitDroplets() {
            for (int i = 0; i < DROPLET_PARTICLES; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double heightProgress = 0.25D + random.nextDouble() * 0.75D;
                double height = CONE_HEIGHT * scale * heightProgress;
                double radius = Math.tan(CONE_ANGLE) * height;

                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;

                double outwardSpeed = (0.22D + random.nextDouble() * 0.38D) * scale;
                double upwardSpeed = (0.35D + random.nextDouble() * 0.45D) * scale;

                emit(
                        origin.x + x,
                        origin.y + height,
                        origin.z + z,
                        whiteColor(),
                        randomSize(2.0F, 3.2F),
                        35 + random.nextInt(20),
                        new Vec3(
                                Math.cos(angle) * outwardSpeed,
                                upwardSpeed,
                                Math.sin(angle) * outwardSpeed
                        )
                );
            }
        }

        private float randomSize(float minimum, float maximum) {
            return (minimum + random.nextFloat() * (maximum - minimum)) * (float) Math.sqrt(scale);
        }

        private int splashColor() {
            return switch (random.nextInt(6)) {
                case 0, 1, 2 -> 0xFFFFFF;
                case 3 -> 0xF2FAFF;
                case 4 -> 0xDCEFFF;
                default -> 0xBDE4FF;
            };
        }

        private int whiteColor() {
            return switch (random.nextInt(5)) {
                case 0, 1, 2 -> 0xFFFFFF;
                case 3 -> 0xFAFDFF;
                default -> 0xEAF7FF;
            };
        }

        private void emit(
                double x,
                double y,
                double z,
                int color,
                float size,
                int lifetime,
                Vec3 velocity
        ) {
            SmokeParticleOptions particle = new SmokeParticleOptions(
                    size,
                    color,
                    color,
                    false,
                    lifetime,
                    velocity,
                    3
            );

            ClientboundLevelParticlesPacket packet = new ClientboundLevelParticlesPacket(
                    particle,
                    true,
                    x,
                    y,
                    z,
                    0.0F,
                    0.0F,
                    0.0F,
                    0.0F,
                    1
            );

            for (ServerPlayer player : recipients) {
                if (!player.isRemoved() && player.level() == level) {
                    player.connection.send(packet);
                }
            }
        }
    }
}