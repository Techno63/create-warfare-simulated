package com.julian.createwarfare.network;

import com.julian.createwarfare.effects.client.ScreenBlurHandler;
import com.julian.createwarfare.effects.client.ScreenFlashHandler;
import com.julian.createwarfare.effects.client.ScreenGlowHandler;
import com.julian.createwarfare.effects.client.ScreenShakeHandler;
import com.julian.createwarfare.effects.client.ScreenVignetteHandler;
import com.julian.createwarfare.effects.sounds.HeartbeatHandler;
import com.julian.createwarfare.effects.sounds.TinnitusHandler;
import net.minecraft.world.phys.Vec3;

public final class ClientEffectPayloadHandler {

    private ClientEffectPayloadHandler() {
    }

    public static void handle(
            ClientEffectPayload payload
    ) {
        switch (payload.effect()) {
            case ClientEffectPayload.GLOW -> {
                ScreenGlowHandler.glow(
                        new Vec3(
                                payload.x(),
                                payload.y(),
                                payload.z()
                        ),
                        payload.value1(),
                        payload.value3(),
                        payload.value2(),
                        payload.value4()
                );
            }

            case ClientEffectPayload.FLASH -> {
                ScreenFlashHandler.flash(
                        payload.value1(),
                        payload.value4(),
                        payload.value3()
                );
            }

            case ClientEffectPayload.BLUR -> {
                ScreenBlurHandler.blur(
                        payload.value1(),
                        payload.value4()
                );
            }

            case ClientEffectPayload.SHAKE -> {
                ScreenShakeHandler.shake(
                        payload.value1(),
                        payload.value4()
                );
            }

            case ClientEffectPayload.VIGNETTE -> {
                ScreenVignetteHandler.vignette(
                        payload.value1(),
                        payload.value4()
                );
            }

            case ClientEffectPayload.HEARTBEAT -> {
                HeartbeatHandler.start(
                        payload.value3(),
                        payload.value4()
                );
            }

            case ClientEffectPayload.TINNITUS -> {
                TinnitusHandler.start(
                        payload.value4()
                );
            }
        }
    }
}