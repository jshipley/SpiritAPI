package com.jship.spiritapi.api.energy;

import com.jship.spiritapi.api.util.INBTSerializable;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.nbt.Tag;

/**
 * A common Energy storage
 */
public abstract class SpiritEnergyStorage implements INBTSerializable<Tag> {

    @ExpectPlatform
    public static SpiritEnergyStorage create(long maxAmount, long maxInsert, long maxExtract, Runnable onCommit) {
        throw new AssertionError();
    }

    public abstract long receiveEnergy(long toReceive, boolean simulate);

    public abstract long extractEnergy(long toExtract, boolean simulate);

    public abstract long getEnergyStored();

    public abstract long getMaxEnergyStored();

    public abstract void setEnergyStored(long toStore);

    public abstract boolean canExtract();

    public abstract boolean canReceive();
}
