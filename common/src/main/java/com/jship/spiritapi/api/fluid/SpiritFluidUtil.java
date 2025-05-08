package com.jship.spiritapi.api.fluid;

import dev.architectury.fluid.FluidStack;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class SpiritFluidUtil {

    @ExpectPlatform
    public static boolean isFluidItem(ItemStack container) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static long getFluidItemCapacity(ItemStack container) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static FluidStack getFluidFromItem(ItemStack filledContainer) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static ItemStack getItemFromFluid(FluidStack fluid, ItemStack container) {
        throw new AssertionError();
    }
    
    @ExpectPlatform
    public static long drainBlockPos(SpiritFluidStorage fluidStorage, Level level, BlockPos pos, Direction facing, boolean simulate) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static long drainVehicle(SpiritFluidStorage fluidStorage, Level level, VehicleEntity vehicle, boolean simulate) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean drainItem(SpiritFluidStorage fluidStorage, Player player, InteractionHand hand, boolean simulate) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static long fillBlockPos(SpiritFluidStorage fluidStorage, Level level, BlockPos pos, Direction facing, boolean simulate) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static long fillVehicle(SpiritFluidStorage fluidStorage, Level level, VehicleEntity vehicle, boolean simulate) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean fillItem(SpiritFluidStorage fluidStorage, Player player, InteractionHand hand, boolean simulate) {
        throw new AssertionError();
    }
}
