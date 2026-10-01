package com.julian.createwarfare.blocks;

import com.julian.createwarfare.recipes.CentrifugingRecipe;
import com.julian.createwarfare.registry.CWBlockEntities;
import com.julian.createwarfare.registry.CWBlocks;
import com.julian.createwarfare.registry.CWRecipes;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;

public class CentrifugeBlockEntity extends KineticBlockEntity {

    private static final int INVENTORY_SIZE = 9;

    private final ItemStackHandler inputInventory = new ItemStackHandler(INVENTORY_SIZE) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final ItemStackHandler primaryInventory = new ItemStackHandler(INVENTORY_SIZE) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final ItemStackHandler secondaryInventory = new ItemStackHandler(INVENTORY_SIZE) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private int processingProgress = 0;

    public CentrifugeBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state
    ) {
        super(type, pos, state);
    }

    @Override
    public float calculateStressApplied() {
        return 1024.0f;
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                CWBlockEntities.CENTRIFUGE.get(),
                (blockEntity, side) -> blockEntity.getItemHandler()
        );
    }

    @Override
    public void tick() {
        super.tick();

        if (level == null || level.isClientSide)
            return;

        CentrifugeBlockEntity bottom = getBottom();

        if (bottom != this)
            return;

        int height = getHeight();

        if (height < 2) {
            processingProgress = 0;
            return;
        }

        ItemStack input = inputInventory.getStackInSlot(0);

        if (input.isEmpty()) {
            processingProgress = 0;
            return;
        }

        CentrifugingRecipe recipe = getRecipe();

        if (recipe == null) {
            processingProgress = 0;
            return;
        }

        float speed = Math.abs(getSpeed());

        if (speed < 256.0f) {
            processingProgress = 0;
            return;
        }

        if (!canInsert(
                secondaryInventory,
                recipe.getSecondaryOutput()
        )) {
            processingProgress = 0;
            return;
        }

        int machineMultiplier = height - 1;

        int processingTime = Math.max(
                1,
                (int) Math.ceil(
                        recipe.getProcessingTime()
                                * 256.0
                                / speed
                                / machineMultiplier
                )
        );

        processingProgress++;

        if (processingProgress >= processingTime) {
            processRecipe(recipe);
            processingProgress = 0;
        }

        setChanged();
    }

    private void processRecipe(CentrifugingRecipe recipe) {
        inputInventory.extractItem(
                0,
                1,
                false
        );

        CentrifugeBlockEntity top = getTop();

        if (level.random.nextFloat() < recipe.getSuccessChance()) {
            insertOutput(
                    top.primaryInventory,
                    recipe.getPrimaryOutput()
            );
        }

        insertOutput(
                secondaryInventory,
                recipe.getSecondaryOutput()
        );

        setChanged();
        top.setChanged();
    }

    private void insertOutput(
            ItemStackHandler inventory,
            ItemStack output
    ) {
        if (output.isEmpty())
            return;

        ItemStack remaining = output.copy();

        for (int slot = 0; slot < INVENTORY_SIZE; slot++) {
            if (remaining.isEmpty())
                return;

            remaining = inventory.insertItem(
                    slot,
                    remaining,
                    false
            );
        }
    }

    private boolean canInsert(
            ItemStackHandler inventory,
            ItemStack output
    ) {
        if (output.isEmpty())
            return true;

        ItemStack remaining = output.copy();

        for (int slot = 0; slot < INVENTORY_SIZE; slot++) {
            remaining = inventory.insertItem(
                    slot,
                    remaining,
                    true
            );

            if (remaining.isEmpty())
                return true;
        }

        return false;
    }

    private CentrifugingRecipe getRecipe() {
        if (level == null)
            return null;

        List<RecipeHolder<CentrifugingRecipe>> recipes =
                level.getRecipeManager()
                        .getAllRecipesFor(
                                CWRecipes.CENTRIFUGING_TYPE.get()
                        );

        ItemStack input = inputInventory.getStackInSlot(0);

        for (RecipeHolder<CentrifugingRecipe> holder : recipes) {
            CentrifugingRecipe recipe = holder.value();

            if (recipe.getInput().test(input))
                return recipe;
        }

        return null;
    }

    private int getHeight() {
        if (level == null)
            return 1;

        int height = 1;
        BlockPos current = worldPosition;

        while (level.getBlockState(current.above())
                .is(CWBlocks.CENTRIFUGE.get())) {

            current = current.above();
            height++;
        }

        return height;
    }

    private CentrifugeBlockEntity getBottom() {
        if (level == null)
            return this;

        BlockPos current = worldPosition;

        while (level.getBlockState(current.below())
                .is(CWBlocks.CENTRIFUGE.get())) {

            current = current.below();
        }

        if (level.getBlockEntity(current)
                instanceof CentrifugeBlockEntity centrifuge) {

            return centrifuge;
        }

        return this;
    }

    private CentrifugeBlockEntity getTop() {
        if (level == null)
            return this;

        BlockPos current = worldPosition;

        while (level.getBlockState(current.above())
                .is(CWBlocks.CENTRIFUGE.get())) {

            current = current.above();
        }

        if (level.getBlockEntity(current)
                instanceof CentrifugeBlockEntity centrifuge) {

            return centrifuge;
        }

        return this;
    }

    public ItemStackHandler getInputInventory() {
        return inputInventory;
    }

    public ItemStackHandler getPrimaryInventory() {
        return primaryInventory;
    }

    public ItemStackHandler getSecondaryInventory() {
        return secondaryInventory;
    }

    private IItemHandler getItemHandler() {
        CentrifugeBlockEntity bottom = getBottom();
        CentrifugeBlockEntity top = getTop();

        if (bottom == top) {
            return new CombinedItemHandler(
                    bottom.inputInventory,
                    bottom.primaryInventory,
                    bottom.secondaryInventory
            );
        }

        if (this == bottom) {
            return new CombinedItemHandler(
                    bottom.inputInventory,
                    null,
                    bottom.secondaryInventory
            );
        }

        if (this == top) {
            return new CombinedItemHandler(
                    null,
                    top.primaryInventory,
                    null
            );
        }

        return new CombinedItemHandler(
                null,
                null,
                null
        );
    }

    @Override
    protected void write(
            CompoundTag tag,
            HolderLookup.Provider registries,
            boolean clientPacket
    ) {
        super.write(
                tag,
                registries,
                clientPacket
        );

        tag.put(
                "InputInventory",
                inputInventory.serializeNBT(registries)
        );

        tag.put(
                "PrimaryInventory",
                primaryInventory.serializeNBT(registries)
        );

        tag.put(
                "SecondaryInventory",
                secondaryInventory.serializeNBT(registries)
        );

        tag.putInt(
                "ProcessingProgress",
                processingProgress
        );
    }

    @Override
    protected void read(
            CompoundTag tag,
            HolderLookup.Provider registries,
            boolean clientPacket
    ) {
        super.read(
                tag,
                registries,
                clientPacket
        );

        inputInventory.deserializeNBT(
                registries,
                tag.getCompound("InputInventory")
        );

        primaryInventory.deserializeNBT(
                registries,
                tag.getCompound("PrimaryInventory")
        );

        secondaryInventory.deserializeNBT(
                registries,
                tag.getCompound("SecondaryInventory")
        );

        processingProgress = tag.getInt(
                "ProcessingProgress"
        );
    }

    private static class CombinedItemHandler implements IItemHandler {

        private final ItemStackHandler input;
        private final ItemStackHandler primary;
        private final ItemStackHandler secondary;

        private CombinedItemHandler(
                ItemStackHandler input,
                ItemStackHandler primary,
                ItemStackHandler secondary
        ) {
            this.input = input;
            this.primary = primary;
            this.secondary = secondary;
        }

        @Override
        public int getSlots() {
            int slots = 0;

            if (input != null)
                slots += input.getSlots();

            if (primary != null)
                slots += primary.getSlots();

            if (secondary != null)
                slots += secondary.getSlots();

            return slots;
        }

        private ItemStackHandler getInventory(int slot) {
            int offset = 0;

            if (input != null) {
                if (slot < offset + input.getSlots())
                    return input;

                offset += input.getSlots();
            }

            if (primary != null) {
                if (slot < offset + primary.getSlots())
                    return primary;

                offset += primary.getSlots();
            }

            if (secondary != null) {
                if (slot < offset + secondary.getSlots())
                    return secondary;
            }

            return null;
        }

        private int getLocalSlot(int slot) {
            int offset = 0;

            if (input != null) {
                if (slot < offset + input.getSlots())
                    return slot - offset;

                offset += input.getSlots();
            }

            if (primary != null) {
                if (slot < offset + primary.getSlots())
                    return slot - offset;

                offset += primary.getSlots();
            }

            if (secondary != null) {
                if (slot < offset + secondary.getSlots())
                    return slot - offset;
            }

            return 0;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            ItemStackHandler inventory = getInventory(slot);

            if (inventory == null)
                return ItemStack.EMPTY;

            return inventory.getStackInSlot(
                    getLocalSlot(slot)
            );
        }

        @Override
        public ItemStack insertItem(
                int slot,
                ItemStack stack,
                boolean simulate
        ) {
            ItemStackHandler inventory = getInventory(slot);

            if (inventory == null)
                return stack;

            if (inventory != input)
                return stack;

            return inventory.insertItem(
                    getLocalSlot(slot),
                    stack,
                    simulate
            );
        }

        @Override
        public ItemStack extractItem(
                int slot,
                int amount,
                boolean simulate
        ) {
            ItemStackHandler inventory = getInventory(slot);

            if (inventory == null)
                return ItemStack.EMPTY;

            if (inventory == input)
                return ItemStack.EMPTY;

            return inventory.extractItem(
                    getLocalSlot(slot),
                    amount,
                    simulate
            );
        }

        @Override
        public int getSlotLimit(int slot) {
            ItemStackHandler inventory = getInventory(slot);

            if (inventory == null)
                return 0;

            return inventory.getSlotLimit(
                    getLocalSlot(slot)
            );
        }

        @Override
        public boolean isItemValid(
                int slot,
                ItemStack stack
        ) {
            ItemStackHandler inventory = getInventory(slot);

            if (inventory == null)
                return false;

            return inventory == input
                    && inventory.isItemValid(
                    getLocalSlot(slot),
                    stack
            );
        }
    }
}