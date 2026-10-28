package com.julian.createwarfare.registry;

import com.julian.createwarfare.CreateWarfare;
import com.julian.createwarfare.effects.particles.smoke.SmokeParticle;
import com.julian.createwarfare.effects.particles.smoke_explosion.SmokeExplosionParticle;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@EventBusSubscriber(modid = CreateWarfare.MODID, value = Dist.CLIENT)
public class CWParticleProviders {
    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(
                CWParticles.SMOKE_EXPLOSION.get(),
                SmokeExplosionParticle.Provider::new
        );

        event.registerSpriteSet(
                CWParticles.SMOKE.get(),
                SmokeParticle.Provider::new
        );
    }
}
