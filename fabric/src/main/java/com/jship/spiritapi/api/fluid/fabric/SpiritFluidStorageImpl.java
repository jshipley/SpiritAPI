package com.jship.spiritapi.api.fluid.fabric;

import java.util.function.Predicate;

import com.jship.spiritapi.api.fluid.SpiritFluidStorage;

import dev.architectury.fluid.FluidStack;
import dev.architectury.hooks.fluid.fabric.FluidStackHooksFabric;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

public class SpiritFluidStorageImpl extends SpiritFluidStorage {

    public final long maxAmount;
    public final long transferRate;
    private Runnable onCommit;
    public final SingleVariantStorage<FluidVariant> fabricFluidStorage;
    public final Predicate<FluidStack> validFluid;

    private SpiritFluidStorageImpl(long maxAmount, long transferRate, Runnable onCommit) {
        this(maxAmount, transferRate, onCommit, (fluid) -> true);
    }

    private SpiritFluidStorageImpl(long maxAmount, long transferRate, Runnable onCommit, Predicate<FluidStack> validFluid) {
        this.maxAmount = maxAmount;
        this.transferRate = transferRate;
        this.onCommit = onCommit;
        this.validFluid = validFluid;
        this.fabricFluidStorage = new SingleVariantStorage<FluidVariant>() {
            @Override
            protected FluidVariant getBlankVariant() {
                return FluidVariant.blank();
            }
            @Override
            protected long getCapacity(FluidVariant variant) {
                return SpiritFluidStorageImpl.this.maxAmount;
            }
            @Override
            protected void onFinalCommit() {
                SpiritFluidStorageImpl.this.onCommit.run();
            }
        };
    }

    public static SpiritFluidStorage create(long capacity, long transferRate, Runnable onCommit) {
        return new SpiritFluidStorageImpl(capacity, transferRate, onCommit);
    }

    public static SpiritFluidStorage create(long maxAmount, long transferRate, Runnable onCommit, Predicate<FluidStack> validFluid) {
        throw new AssertionError();
    }

    @Override
    public int getTanks() {
        return fabricFluidStorage.getSlots().size();
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return FluidStackHooksFabric.fromFabric(fabricFluidStorage.getSlot(tank));
    }

    @Override
    public long getTankCapacity(int tank) {
        return fabricFluidStorage.getCapacity();
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        if (!validFluid.test(stack)) return false;
        if (fabricFluidStorage.isResourceBlank()) return true;
        FluidStack storageStack = FluidStackHooksFabric.fromFabric(fabricFluidStorage);
        return storageStack.isFluidEqual(stack) && storageStack.isComponentEqual(stack);
    }

    @Override
    public long fill(FluidStack resource, boolean simulate) {
        long inserted = 0;
        try (var tx = Transaction.openOuter()) {
            inserted = fabricFluidStorage.insert(FluidStackHooksFabric.toFabric(resource), Math.min(resource.getAmount(), this.transferRate), tx);
            if (inserted > 0 && !simulate) tx.commit();
        }
        return inserted;
    }

    @Override
    public FluidStack drain(FluidStack resource, boolean simulate) {
        long extracted = 0;
        try (var tx = Transaction.openOuter()) {
            extracted = fabricFluidStorage.extract(FluidStackHooksFabric.toFabric(resource), Math.min(resource.getAmount(), this.transferRate), tx);
            if (extracted > 0 && !simulate) tx.commit();
        }
        return FluidStackHooksFabric.fromFabric(fabricFluidStorage.getResource(), extracted);
    }

    public FluidStack drain(long maxDrain, boolean simulate) {
        long extracted = 0;
        try (var tx = Transaction.openOuter()) {
            extracted = fabricFluidStorage.extract(fabricFluidStorage.getResource(), Math.min(maxDrain, transferRate), tx);
            if (extracted > 0 && !simulate) tx.commit();
        }
        return FluidStackHooksFabric.fromFabric(fabricFluidStorage.getResource(), extracted);
    }

    @Override
    public CompoundTag serializeNbt(HolderLookup.Provider provider) {
        CompoundTag nbt = new CompoundTag();
        SingleVariantStorage.writeNbt(fabricFluidStorage, FluidVariant.CODEC, nbt, provider);
        return nbt;
    }

    @Override
    public void deserializeNbt(HolderLookup.Provider provider, CompoundTag nbt) {
        SingleVariantStorage.readNbt(fabricFluidStorage, FluidVariant.CODEC, () -> FluidVariant.blank(), nbt, provider);
    }
}
