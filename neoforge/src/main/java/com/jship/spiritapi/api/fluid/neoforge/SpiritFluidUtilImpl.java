package com.jship.spiritapi.api.fluid.neoforge;

import com.jship.spiritapi.api.fluid.SpiritFluidStorage;

import dev.architectury.fluid.FluidStack;
import dev.architectury.hooks.fluid.forge.FluidStackHooksForge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

public class SpiritFluidUtilImpl {

    public static boolean isFluidItem(ItemStack container) {
        return container.getCapability(Capabilities.FluidHandler.ITEM) != null;
    }

    public static long getFluidItemCapacity(ItemStack container) {
        int capacity = 0;
        var capability = container.getCapability(Capabilities.FluidHandler.ITEM);
        for (int i = 0; i < capability.getTanks(); i++) {
            capacity = Math.max(capacity, capability.getTankCapacity(i));
        }
        return capacity;
    }

    public static FluidStack getFluidFromItem(ItemStack filledContainer) {
        var capability = filledContainer.getCapability(Capabilities.FluidHandler.ITEM);
        if (capability != null) {
            for (int i = 0; i < capability.getTanks(); i++) {
                return FluidStackHooksForge.fromForge(capability.getFluidInTank(i));
            }
        }
        return FluidStack.empty();
    }

    public static ItemStack getItemFromFluid(FluidStack fluid, ItemStack container) {
        var capability = container.getCapability(Capabilities.FluidHandler.ITEM);
        if (capability != null) {
            var filled = capability.fill(FluidStackHooksForge.toForge(fluid), FluidAction.SIMULATE);
            if (filled > 0) {
                capability.fill(FluidStackHooksForge.toForge(fluid), FluidAction.EXECUTE);
                return container;
            }
        }
        return ItemStack.EMPTY;
    }

    public static long drainBlockPos(SpiritFluidStorage fluidStorage, Level level, BlockPos pos, Direction facing, boolean simulate) {
        if (fluidStorage.getFluidInTank(0).getAmount() >= fluidStorage.getTankCapacity(0)) return 0;
        IFluidHandler fluidHandler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, facing.getOpposite());
        if (fluidHandler == null) return 0;
        return drainFluidStorage(fluidStorage, fluidHandler, simulate);
    }

    public static long drainVehicle(SpiritFluidStorage fluidStorage, Level level, VehicleEntity vehicle, boolean simulate) {
        var vehicleFluidHandler = vehicle.getCapability(Capabilities.FluidHandler.ENTITY, Direction.DOWN);
        return drainFluidStorage(fluidStorage, vehicleFluidHandler, simulate);
    }

    public static boolean drainItem(SpiritFluidStorage fluidStorage, Player player, InteractionHand hand, boolean simulate) {
        FluidActionResult result = FluidUtil.tryEmptyContainerAndStow(
            player.getItemInHand(hand),
            ((SpiritFluidStorageImpl)fluidStorage).neoFluidTank,
            player.getCapability(Capabilities.ItemHandler.ENTITY), (int)FluidStack.bucketAmount(), player, !simulate);
        if (result.isSuccess() && !simulate)
            player.setItemInHand(hand, result.getResult());
        return result.isSuccess();
        
    }

    private static long drainFluidStorage(SpiritFluidStorage fluidStorage, IFluidHandler sourceStorage, boolean simulate) {
        return FluidUtil.tryFluidTransfer(((SpiritFluidStorageImpl)fluidStorage).neoFluidTank, sourceStorage, ((SpiritFluidStorageImpl)fluidStorage).transferRate, !simulate).getAmount();
    }

    public static long fillBlockPos(SpiritFluidStorage fluidStorage, Level level, BlockPos pos, Direction facing, boolean simulate) {
        if (fluidStorage.getFluidInTank(0).isEmpty()) return 0;
        var sourceStorage = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, facing.getOpposite());
        if (sourceStorage == null) return 0;
        return fillFluidStorage(fluidStorage, sourceStorage, simulate);
    }

    public static long fillVehicle(SpiritFluidStorage fluidStorage, Level level, VehicleEntity vehicle, boolean simulate) {
        var vehicleFluidHandler = vehicle.getCapability(Capabilities.FluidHandler.ENTITY, Direction.DOWN);
        return fillFluidStorage(fluidStorage, vehicleFluidHandler, simulate);
    }

    public static boolean fillItem(SpiritFluidStorage fluidStorage, Player player, InteractionHand hand, boolean simulate) {
        FluidActionResult result = FluidUtil.tryFillContainerAndStow(
            player.getItemInHand(hand),
            ((SpiritFluidStorageImpl)fluidStorage).neoFluidTank,
            player.getCapability(Capabilities.ItemHandler.ENTITY),
            (int)FluidStack.bucketAmount(), player, !simulate);
        if (result.isSuccess() && !simulate)
            player.setItemInHand(hand, result.getResult());
        return result.isSuccess();
    }

    private static long fillFluidStorage(SpiritFluidStorage fluidStorage, IFluidHandler destStorage, boolean simulate) {
        return FluidUtil.tryFluidTransfer(destStorage, ((SpiritFluidStorageImpl)fluidStorage).neoFluidTank, ((SpiritFluidStorageImpl)fluidStorage).transferRate, !simulate).getAmount();
    }
}
