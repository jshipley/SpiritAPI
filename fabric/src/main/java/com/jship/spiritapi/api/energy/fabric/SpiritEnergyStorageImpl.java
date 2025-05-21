package com.jship.spiritapi.api.energy.fabric;

import com.jship.spiritapi.api.energy.SpiritEnergyStorage;

import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import team.reborn.energy.api.base.SimpleEnergyStorage;

/**
 * The Fabric implementation of SpiritEnergyStorage
 * A light Neoforge style wrapper around Team Reborn's (Fabric) SimpleEnergyStorage
 */
public class SpiritEnergyStorageImpl extends SpiritEnergyStorage {

    public final SimpleEnergyStorage fabricEnergyStorage;
    private final Runnable onCommit;

    private SpiritEnergyStorageImpl(long maxAmount, long maxInsert, long maxExtract, Runnable onCommit) {
        this.fabricEnergyStorage = new SimpleEnergyStorage(maxAmount, maxInsert, maxExtract) {
            @Override
            protected void onFinalCommit() {
                SpiritEnergyStorageImpl.this.onCommit.run();
            }
        };
        this.onCommit = onCommit;
    }

    public static SpiritEnergyStorage create(long maxAmount, long maxInsert, long maxExtract, Runnable onCommit) {
        return new SpiritEnergyStorageImpl(maxAmount, maxInsert, maxExtract, onCommit);
    }

    public long receiveEnergy(long toReceive, boolean simulate) {
        try (Transaction tx = Transaction.openOuter()) {
            long inserted = this.fabricEnergyStorage.extract(toReceive, tx);
            if (!simulate) tx.commit();
            
            return inserted;
        }
    }

    public long extractEnergy(long toExtract, boolean simulate) {
        try (Transaction tx = Transaction.openOuter()) {
            long extracted = this.fabricEnergyStorage.extract(toExtract, tx);
            if (!simulate) tx.commit();

            return extracted;
        }
    }

    public long getEnergyStored() {
        return fabricEnergyStorage.getAmount();
    }

    public long getMaxEnergyStored() {
        return fabricEnergyStorage.getCapacity();
    }

    public void setEnergyStored(long toStore) {
        fabricEnergyStorage.amount = toStore;
        this.onCommit.run();
    }

    public boolean canExtract() {
        return fabricEnergyStorage.supportsExtraction();
    }

    public boolean canReceive() {
        return fabricEnergyStorage.supportsInsertion();
    }
    
    @Override
    public Tag serializeNbt(HolderLookup.Provider provider) {
        return LongTag.valueOf(fabricEnergyStorage.amount);
    }

    public void deserializeNbt(HolderLookup.Provider provider, Tag nbt) {
        this.setEnergyStored(nbt.asLong().orElseGet(() -> 0L));
    }
}
