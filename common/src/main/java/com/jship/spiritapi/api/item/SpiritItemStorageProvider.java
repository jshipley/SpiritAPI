package com.jship.spiritapi.api.item;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Direction;

public interface SpiritItemStorageProvider {
    
    @Nullable public SpiritItemStorage getItemStorage(Direction face);
}
