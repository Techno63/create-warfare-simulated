package com.julian.createwarfare.registry;

import com.julian.createwarfare.effects.particles.explosions.SmokeExplosionParticleType;
import com.julian.createwarfare.effects.particles.shockwaves.ShockwaveParticleType;
import com.julian.createwarfare.effects.particles.smoke.SmokeParticleType;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;

import static com.julian.createwarfare.CreateWarfare.REGISTRATE;

public class CWParticles {

    public static final RegistryEntry<ParticleType<?>, ShockwaveParticleType> SHOCKWAVE =
            REGISTRATE.simple(
                    "shockwave",
                    Registries.PARTICLE_TYPE,
                    () -> new ShockwaveParticleType(false)
            );

    public static final RegistryEntry<ParticleType<?>, SmokeParticleType> SMOKE =
            REGISTRATE.simple(
                    "smoke",
                    Registries.PARTICLE_TYPE,
                    () -> new SmokeParticleType(true)
            );

    public static final RegistryEntry<ParticleType<?>, SmokeExplosionParticleType> SMOKE_EXPLOSION =
            REGISTRATE.simple(
                    "smoke_explosion",
                    Registries.PARTICLE_TYPE,
                    SmokeExplosionParticleType::new
            );

    public static void register() {}
}
