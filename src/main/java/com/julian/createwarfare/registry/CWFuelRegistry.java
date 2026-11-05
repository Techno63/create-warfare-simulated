package com.julian.createwarfare.registry;

import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.IdentityHashMap;
import java.util.Map;

public final class CWFuelRegistry {

    private static final Map<Fluid, Double> FUELS = new IdentityHashMap<>();

    static {
        register(Fluids.LAVA, 1000.0D);
    }

    private CWFuelRegistry() {
    }

    public static void register(Fluid fluid, double consumptionPerSecond) {
        if (fluid == null || consumptionPerSecond <= 0.0D) {
            throw new IllegalArgumentException("Invalid fuel registration");
        }

        FUELS.put(fluid, consumptionPerSecond);
    }

    public static boolean isFuel(Fluid fluid) {
        return FUELS.containsKey(fluid);
    }

    public static double getConsumptionRate(Fluid fluid) {
        return FUELS.getOrDefault(fluid, 0.0D);
    }
}