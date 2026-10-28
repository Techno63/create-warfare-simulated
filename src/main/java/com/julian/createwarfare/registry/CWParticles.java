package com.julian.createwarfare.registry;

import com.julian.createwarfare.effects.particles.smoke.SmokeParticleType;
import com.julian.createwarfare.effects.particles.smoke_explosion.SmokeExplosionParticleType;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;

import static com.julian.createwarfare.CreateWarfare.REGISTRATE;

public class CWParticles {
    public static final RegistryEntry<ParticleType<?>, SmokeExplosionParticleType> SMOKE_EXPLOSION =
            REGISTRATE.simple(
                    "smoke_explosion",
                    Registries.PARTICLE_TYPE,
                    SmokeExplosionParticleType::new
            );

    public static final RegistryEntry<ParticleType<?>, SmokeParticleType> SMOKE =
            REGISTRATE.simple(
                    "smoke",
                    Registries.PARTICLE_TYPE,
                    SmokeParticleType::new
            );

    public static void register() {
    }
}
