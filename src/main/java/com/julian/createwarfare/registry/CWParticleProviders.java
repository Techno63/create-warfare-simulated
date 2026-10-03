package com.julian.createwarfare.registry;

import com.julian.createwarfare.CreateWarfare;
import com.julian.createwarfare.effects.particles.explosions.ExplosionParticle;
import com.julian.createwarfare.effects.particles.shockwaves.ShockwaveParticle;
import com.julian.createwarfare.effects.particles.smoke.SmokeParticle;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;


@EventBusSubscriber(modid = CreateWarfare.MODID, value = Dist.CLIENT)
public class CWParticleProviders {

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(
                CWParticles.SHOCKWAVE.get(),
                ShockwaveParticle.Provider::new
        );

        event.registerSpriteSet(
                CWParticles.EXPLOSION.get(),
                ExplosionParticle.Provider::new
        );

        event.registerSpriteSet(
                CWParticles.SMOKE.get(),
                SmokeParticle.Provider::new
        );
    }
}
