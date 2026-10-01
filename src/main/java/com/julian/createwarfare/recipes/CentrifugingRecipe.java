package com.julian.createwarfare.recipes;

import com.julian.createwarfare.registry.CWRecipes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;

public class CentrifugingRecipe implements Recipe<RecipeWrapper> {

    private final Ingredient input;
    private final ItemStack primaryOutput;
    private final ItemStack secondaryOutput;
    private final int processingTime;
    private final float successChance;

    public CentrifugingRecipe(
            Ingredient input,
            ItemStack primaryOutput,
            ItemStack secondaryOutput,
            int processingTime,
            float successChance
    ) {
        this.input = input;
        this.primaryOutput = primaryOutput;
        this.secondaryOutput = secondaryOutput;
        this.processingTime = processingTime;
        this.successChance = successChance;
    }

    public Ingredient getInput() {
        return input;
    }

    public ItemStack getPrimaryOutput() {
        return primaryOutput;
    }

    public ItemStack getSecondaryOutput() {
        return secondaryOutput;
    }

    public int getProcessingTime() {
        return processingTime;
    }

    public float getSuccessChance() {
        return successChance;
    }

    @Override
    public boolean matches(
            RecipeWrapper container,
            Level level
    ) {
        return input.test(container.getItem(0));
    }

    @Override
    public ItemStack assemble(
            RecipeWrapper container,
            HolderLookup.Provider registries
    ) {
        return primaryOutput.copy();
    }

    @Override
    public boolean canCraftInDimensions(
            int width,
            int height
    ) {
        return true;
    }

    @Override
    public ItemStack getResultItem(
            HolderLookup.Provider registries
    ) {
        return primaryOutput;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return CWRecipes.CENTRIFUGING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return CWRecipes.CENTRIFUGING_TYPE.get();
    }

    public static class Serializer
            implements RecipeSerializer<CentrifugingRecipe> {

        public static final MapCodec<CentrifugingRecipe> CODEC =
                RecordCodecBuilder.mapCodec(instance ->
                        instance.group(
                                Ingredient.CODEC_NONEMPTY
                                        .fieldOf("ingredient")
                                        .forGetter(
                                                CentrifugingRecipe::getInput
                                        ),
                                ItemStack.CODEC
                                        .fieldOf("primary_output")
                                        .forGetter(
                                                CentrifugingRecipe::getPrimaryOutput
                                        ),
                                ItemStack.OPTIONAL_CODEC
                                        .optionalFieldOf(
                                                "secondary_output",
                                                ItemStack.EMPTY
                                        )
                                        .forGetter(
                                                CentrifugingRecipe::getSecondaryOutput
                                        ),
                                Codec.INT
                                        .optionalFieldOf(
                                                "processing_time",
                                                200
                                        )
                                        .forGetter(
                                                CentrifugingRecipe::getProcessingTime
                                        ),
                                Codec.FLOAT
                                        .optionalFieldOf(
                                                "success_chance",
                                                1.0f
                                        )
                                        .forGetter(
                                                CentrifugingRecipe::getSuccessChance
                                        )
                        ).apply(
                                instance,
                                CentrifugingRecipe::new
                        )
                );

        public static final StreamCodec<
                RegistryFriendlyByteBuf,
                CentrifugingRecipe
                > STREAM_CODEC =
                StreamCodec.composite(
                        Ingredient.CONTENTS_STREAM_CODEC,
                        CentrifugingRecipe::getInput,
                        ItemStack.STREAM_CODEC,
                        CentrifugingRecipe::getPrimaryOutput,
                        ItemStack.OPTIONAL_STREAM_CODEC,
                        CentrifugingRecipe::getSecondaryOutput,
                        ByteBufCodecs.VAR_INT,
                        CentrifugingRecipe::getProcessingTime,
                        ByteBufCodecs.FLOAT,
                        CentrifugingRecipe::getSuccessChance,
                        CentrifugingRecipe::new
                );

        @Override
        public MapCodec<CentrifugingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<
                RegistryFriendlyByteBuf,
                CentrifugingRecipe
                > streamCodec() {
            return STREAM_CODEC;
        }
    }
}