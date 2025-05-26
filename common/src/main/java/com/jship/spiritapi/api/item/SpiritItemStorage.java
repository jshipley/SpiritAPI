package com.jship.spiritapi.api.item;

import java.util.List;
import java.util.function.Predicate;

import com.jship.spiritapi.api.util.INBTSerializable;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public abstract class SpiritItemStorage implements INBTSerializable<CompoundTag>{

    @ExpectPlatform
    public static SpiritItemStorage create(List<SlotConfig> slotConfigs, Runnable onCommit) {
        throw new AssertionError();
    }

    public abstract int getSlots();

    public abstract ItemStack getStackInSlot(int slot);

    public abstract ItemStack insertItem(int slot, ItemStack stack, boolean simulate);

    public abstract ItemStack extractItem(int slot, int amount, boolean simulate);

    public abstract int getSlotLimit(int slot);

    public abstract boolean isItemValid(int slot, ItemStack stack);

    public abstract CompoundTag serializeNbt(HolderLookup.Provider provider);

    public abstract void deserializeNbt(HolderLookup.Provider provider, CompoundTag nbt);

    public record SlotConfig(boolean canInsert, boolean canExtract, int maxStackSize, Predicate<ItemStack> validItem) {}
}
