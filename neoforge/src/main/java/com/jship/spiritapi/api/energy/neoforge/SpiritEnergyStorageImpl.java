package com.jship.spiritapi.api.energy.neoforge;

import com.jship.spiritapi.api.energy.SpiritEnergyStorage;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.energy.EnergyStorage;

/**
 * The Neoforge implementation of SpiritEnergyStorage
 * Just a light wrapper around Neoforge's EnergyStorage
 */
public class SpiritEnergyStorageImpl extends SpiritEnergyStorage {

    private final Runnable onCommit;
    public final EnergyStorageSettable neoEnergyStorage;

    private class EnergyStorageSettable extends EnergyStorage {
        public EnergyStorageSettable(int maxAmount, int maxInsert, int maxExtract) {
            super(maxAmount, maxInsert, maxExtract);
        }

        public void setEnergyStored(int toStore) {
            this.energy = toStore;
            SpiritEnergyStorageImpl.this.onCommit.run();
        }
    }

    private SpiritEnergyStorageImpl(int maxAmount, int maxInsert, int maxExtract, Runnable onCommit) {
        this.onCommit = onCommit;
        this.neoEnergyStorage = new EnergyStorageSettable(maxAmount, maxInsert, maxExtract);
    }

    public static SpiritEnergyStorage create(long maxAmount, long maxInsert, long maxExtract, Runnable onCommit) {
        return new SpiritEnergyStorageImpl((int) maxAmount, (int) maxInsert, (int) maxExtract, onCommit);
    }

    public long receiveEnergy(long toReceive, boolean simulate) {
        int energyReceived = neoEnergyStorage.receiveEnergy((int) toReceive, simulate);
        if (energyReceived > 0 && !simulate) onCommit.run();

        return energyReceived;
    }

    public long extractEnergy(long toExtract, boolean simulate) {
        int energyExtracted = neoEnergyStorage.extractEnergy((int) toExtract, simulate);
        if (energyExtracted > 0 && !simulate) onCommit.run();

        return energyExtracted;
    }

    public long getEnergyStored() {
        return neoEnergyStorage.getEnergyStored();
    }

    public long getMaxEnergyStored() {
        return neoEnergyStorage.getMaxEnergyStored();
    }

    public void setEnergyStored(long toStore) {
        neoEnergyStorage.setEnergyStored((int) toStore);
    }

    public boolean canExtract() {
        return neoEnergyStorage.canExtract();
    }

    public boolean canReceive() {
        return neoEnergyStorage.canReceive();
    }

    public Tag serializeNbt(HolderLookup.Provider provider) {
        return neoEnergyStorage.serializeNBT(provider);
    }

    public void deserializeNbt(HolderLookup.Provider provider, Tag nbt) {
        neoEnergyStorage.deserializeNBT(provider, nbt);
    }
}
