package com.jship.spiritapi.api.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.Tag;

public interface INBTSerializable<T extends Tag> {
    T serializeNbt(HolderLookup.Provider provider);

    void deserializeNbt(HolderLookup.Provider provider, T nbt);
}