package com.jship.spiritapi.neoforge;

import net.neoforged.fml.common.Mod;

import com.jship.spiritapi.SpiritAPI;

@Mod(SpiritAPI.MOD_ID)
public final class SpiritAPINeoforge {
    public SpiritAPINeoforge() {
        // Run our common setup.
        SpiritAPI.init();
    }
}
