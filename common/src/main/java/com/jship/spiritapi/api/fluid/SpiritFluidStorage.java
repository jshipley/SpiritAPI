package com.jship.spiritapi.api.fluid;

import java.util.function.Predicate;

import com.jship.spiritapi.api.util.INBTSerializable;

import dev.architectury.fluid.FluidStack;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

public abstract class SpiritFluidStorage implements INBTSerializable<CompoundTag> {

    @ExpectPlatform
    public static SpiritFluidStorage create(long maxAmount, long transferRate, Runnable onCommit) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static SpiritFluidStorage create(long maxAmount, long transferRate, Runnable onCommit, Predicate<FluidStack> validFluid) {
        throw new AssertionError();
    }

    public abstract int getTanks();

    public abstract FluidStack getFluidInTank(int tank);

    public abstract long getTankCapacity(int tank);

    public abstract boolean isFluidValid(int tank, FluidStack stack);

    public abstract long fill(FluidStack resource, boolean simulate);

    public abstract FluidStack drain(FluidStack resource, boolean simulate);

    public abstract FluidStack drain(long maxDrain, boolean simulate);

    public abstract CompoundTag serializeNbt(HolderLookup.Provider provider);

    public abstract void deserializeNbt(HolderLookup.Provider provider, CompoundTag nbt);
}
