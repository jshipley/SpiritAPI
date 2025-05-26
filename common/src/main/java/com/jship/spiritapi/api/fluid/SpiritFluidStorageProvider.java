package com.jship.spiritapi.api.fluid;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Direction;

public interface SpiritFluidStorageProvider {
    
    @Nullable public SpiritFluidStorage getFluidStorage(Direction face);
}
