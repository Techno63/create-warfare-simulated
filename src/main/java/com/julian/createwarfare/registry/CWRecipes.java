package com.julian.createwarfare.registry;

import com.julian.createwarfare.CreateWarfare;
import com.julian.createwarfare.recipes.CentrifugingRecipe;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

public class CWRecipes {
    public static final CreateRegistrate REGISTRATE = CreateWarfare.REGISTRATE;

    public static final RegistryEntry<RecipeType<?>, RecipeType<CentrifugingRecipe>> CENTRIFUGING_TYPE =
            REGISTRATE.simple(
                    "centrifuging",
                    Registries.RECIPE_TYPE,
                    () -> RecipeType.simple(
                            ResourceLocation.fromNamespaceAndPath(
                                    CreateWarfare.MODID,
                                    "centrifuging"
                            )
                    )
            );

    public static final RegistryEntry<RecipeSerializer<?>, CentrifugingRecipe.Serializer> CENTRIFUGING_SERIALIZER =
            REGISTRATE.simple(
                    "centrifuging",
                    Registries.RECIPE_SERIALIZER,
                    CentrifugingRecipe.Serializer::new
            );

    public static void register() {}
}