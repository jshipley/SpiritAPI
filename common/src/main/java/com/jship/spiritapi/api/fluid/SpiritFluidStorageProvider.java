package com.jship.spiritapi.api.fluid;

import net.minecraft.core.Direction;

public interface SpiritFluidStorageProvider {
    
    public SpiritFluidStorage getFluidStorage(Direction face);
}
