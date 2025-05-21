package com.jship.spiritapi.api.fluid.neoforge;

import java.util.function.Predicate;

import com.jship.spiritapi.api.fluid.SpiritFluidStorage;

import dev.architectury.fluid.FluidStack;
import dev.architectury.hooks.fluid.forge.FluidStackHooksForge;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public class SpiritFluidStorageImpl extends SpiritFluidStorage {

    public final int transferRate;
    private final Runnable onCommit;
    public final FluidTank neoFluidTank;

    private SpiritFluidStorageImpl(int maxAmount, int transferRate,
            Predicate<net.neoforged.neoforge.fluids.FluidStack> isValid, Runnable onCommit) {
        this.transferRate = transferRate;
        this.onCommit = onCommit;
        this.neoFluidTank = new FluidTank((int) maxAmount, isValid) {
            @Override
            protected void onContentsChanged() {
                SpiritFluidStorageImpl.this.onCommit.run();
            }
        };
    }

    public static SpiritFluidStorage create(long maxAmount, long transferRate, Runnable onCommit) {
        return new SpiritFluidStorageImpl((int) maxAmount, (int) transferRate, (f) -> true, onCommit);
    }

    public static SpiritFluidStorage create(long maxAmount, long transferRate, Runnable onCommit, Predicate<FluidStack> validFluid) {
        return new SpiritFluidStorageImpl((int) maxAmount, (int) transferRate, (f) -> validFluid.test(FluidStackHooksForge.fromForge(f)), onCommit);
    }

    @Override
    public int getTanks() {
        return neoFluidTank.getTanks();
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return FluidStackHooksForge.fromForge(neoFluidTank.getFluidInTank(tank));
    }

    @Override
    public long getTankCapacity(int tank) {
        return neoFluidTank.getTankCapacity(tank);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return neoFluidTank.isFluidValid(tank, FluidStackHooksForge.toForge(stack));
    }

    @Override
    public long fill(FluidStack resource, boolean simulate) {
        var fillFluid = FluidStackHooksForge
                .toForge(resource.copyWithAmount(Math.min(resource.getAmount(), transferRate)));
        return neoFluidTank.fill(fillFluid, simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE);
    }

    @Override
    public FluidStack drain(FluidStack resource, boolean simulate) {
        var drainFluid = FluidStackHooksForge
                .toForge(resource.copyWithAmount(Math.min(resource.getAmount(), transferRate)));
        return FluidStackHooksForge.fromForge(
                neoFluidTank.drain(drainFluid, simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE));
    }

    @Override
    public FluidStack drain(long maxDrain, boolean simulate) {
        return FluidStackHooksForge
                .fromForge(neoFluidTank.drain((int) Math.min(maxDrain, transferRate), simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE));
    }

    @Override
    public CompoundTag serializeNbt(HolderLookup.Provider provider) {
        CompoundTag nbt = new CompoundTag();
        return neoFluidTank.writeToNBT(provider, nbt);
    }

    @Override
    public void deserializeNbt(HolderLookup.Provider provider, CompoundTag nbt) {
        neoFluidTank.readFromNBT(provider, nbt);
    }
}
