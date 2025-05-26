package com.jship.spiritapi.api.fluid.fabric;

import com.jship.spiritapi.api.fluid.SpiritFluidStorage;
import com.jship.spiritapi.api.fluid.SpiritFluidStorageProvider;

import dev.architectury.fluid.FluidStack;
import dev.architectury.hooks.fluid.fabric.FluidStackHooksFabric;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class SpiritFluidUtilImpl {

    public static boolean isFluidItem(ItemStack container) {
        return FluidStorage.ITEM.find(container, ContainerItemContext.withConstant(container)) != null;
    }

    public static long getFluidItemCapacity(ItemStack container) {
        long capacity = 0;
        for (var view : FluidStorage.ITEM.find(container, ContainerItemContext.withConstant(container))) {
            if (view.getCapacity() > capacity)
                capacity = view.getCapacity();
        }
        return capacity;
    }

    public static FluidStack getFluidFromItem(ItemStack filledContainer) {
        var storage = FluidStorage.ITEM.find(filledContainer, ContainerItemContext.withConstant(filledContainer));
        if (storage != null) {
            for (var view : storage) {
                var fluidStack = FluidStack.create(view.getResource().getFluid(), view.getAmount());
                fluidStack.applyComponents(view.getResource().getComponents());
                return fluidStack;
            }
        }
        return FluidStack.empty();
    }

    public static ItemStack getItemFromFluid(FluidStack fluid, ItemStack container) {
        var storage = FluidStorage.ITEM.find(container, ContainerItemContext.withConstant(container));
        if (storage != null) {
            try (var tx = Transaction.openOuter()) {
                storage.insert(FluidStackHooksFabric.toFabric(fluid), fluid.getAmount(), tx);
                tx.commit();
                return container;
            }
        }
        return ItemStack.EMPTY;
    }

    public static long drainBlockPos(SpiritFluidStorage fluidStorage, Level level, BlockPos pos, Direction facing,
            boolean simulate) {
        if (fluidStorage.getFluidInTank(0).getAmount() >= fluidStorage.getTankCapacity(0))
            return 0;
        Storage<FluidVariant> sourceStorage = FluidStorage.SIDED.find(level, pos, facing.getOpposite());
        if (sourceStorage == null)
            return 0;
        return drainFluidStorage(fluidStorage, sourceStorage, simulate);
    }

    public static long drainVehicle(SpiritFluidStorage fluidStorage, Level level, VehicleEntity vehicle,
            boolean simulate) {
        // given the lack of an fluid api for entities, only care about fluid vehicles
        // using this api for now.
        if (vehicle instanceof SpiritFluidStorageProvider fluidVehicle
                && fluidVehicle.getFluidStorage(Direction.DOWN) != null) {
            return drainFluidStorage(fluidStorage,
                    ((SpiritFluidStorageImpl) fluidVehicle.getFluidStorage(Direction.DOWN)).fabricFluidStorage,
                    simulate);
        }
        return 0;
    }

    public static boolean drainItem(SpiritFluidStorage fluidStorage, Player player, InteractionHand hand,
            boolean simulate) {
        long drained = 0;
        SingleVariantStorage<FluidVariant> fabricFluidStorage = ((SpiritFluidStorageImpl) fluidStorage).fabricFluidStorage;
        Storage<FluidVariant> itemStorage = FluidStorage.ITEM.find(
                player.getItemInHand(hand),
                ContainerItemContext.forPlayerInteraction(player, hand));
        if ((fabricFluidStorage.getAmount() >= fabricFluidStorage.getCapacity()) || itemStorage == null)
            return false;

        try (var tx = Transaction.openOuter()) {
            for (var view : itemStorage.nonEmptyViews()) {
                try (var nestedTx = tx.openNested()) {
                    FluidVariant resource = fabricFluidStorage.isResourceBlank()
                            ? view.getResource()
                            : fabricFluidStorage.getResource();
                    long containerAmount = view.getAmount();
                    long maxExtract = Math.min(((SpiritFluidStorageImpl) fluidStorage).transferRate,
                            view.getCapacity());
                    long hopperSpace = fabricFluidStorage.getCapacity() - fabricFluidStorage.getAmount();
                    maxExtract = Math.min(maxExtract, hopperSpace);
                    long extracted = view.extract(resource, maxExtract, nestedTx);
                    long inserted = fabricFluidStorage.insert(resource, extracted, nestedTx);
                    if (extracted == inserted &&
                            extracted > 0 &&
                            (extracted == containerAmount || view.getCapacity() > FluidStack.bucketAmount())) {
                        drained = extracted;
                        if (!simulate) {
                            var fluid = resource.getFluid();
                            var sound = fluid.getPickupSound().orElse(fluid.is(FluidTags.LAVA) ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY);
                            player.level().playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS);
                            nestedTx.commit();
                            tx.commit();
                        }
                        break;
                    }
                }
            }
        }
        return drained > 0;
    }

    private static long drainFluidStorage(SpiritFluidStorage fluidStorage, Storage<FluidVariant> sourceStorage,
            boolean simulate) {
        SingleVariantStorage<FluidVariant> fabricFluidStorage = ((SpiritFluidStorageImpl) fluidStorage).fabricFluidStorage;
        long drained = 0;
        try (var tx = Transaction.openOuter()) {
            for (var view : sourceStorage.nonEmptyViews()) {
                try (var nestedTx = tx.openNested()) {
                    FluidVariant resource = fabricFluidStorage.isResourceBlank()
                            ? view.getResource()
                            : fabricFluidStorage.getResource();
                    long maxExtract = Math.min(
                            ((SpiritFluidStorageImpl) fluidStorage).transferRate,
                            fabricFluidStorage.getCapacity() - fabricFluidStorage.getAmount());
                    long extracted = view.extract(resource, maxExtract, nestedTx);
                    long inserted = fabricFluidStorage.insert(resource, extracted, nestedTx);
                    if (extracted == inserted) {
                        drained = extracted;
                        if (!simulate)
                            nestedTx.commit();
                        // Only extract from one storage per tick
                        break;
                    }
                }
            }
            if (drained > 0 && !simulate)
                tx.commit();
        }
        return drained;
    }

    public static long fillBlockPos(SpiritFluidStorage fluidStorage, Level level, BlockPos pos, Direction facing,
            boolean simulate) {
        if (fluidStorage.getFluidInTank(0).isEmpty())
            return 0;
        Storage<FluidVariant> sourceStorage = FluidStorage.SIDED.find(level, pos, facing.getOpposite());
        if (sourceStorage == null)
            return 0;
        return fillFluidStorage(fluidStorage, sourceStorage, simulate);
    }

    public static long fillVehicle(SpiritFluidStorage fluidStorage, Level level, VehicleEntity vehicle,
            boolean simulate) {
        // given the lack of an fluid api for entities, only care about fluid vehicles
        // using this api for now.
        if (vehicle instanceof SpiritFluidStorageProvider fluidVehicle
                && fluidVehicle.getFluidStorage(Direction.DOWN) != null) {
            return fillFluidStorage(fluidStorage,
                    ((SpiritFluidStorageImpl) fluidVehicle.getFluidStorage(Direction.DOWN)).fabricFluidStorage,
                    simulate);
        }
        return 0;
    }

    public static boolean fillItem(SpiritFluidStorage fluidStorage, Player player, InteractionHand hand,
            boolean simulate) {
        long filled = 0;
        SingleVariantStorage<FluidVariant> fabricFluidStorage = ((SpiritFluidStorageImpl) fluidStorage).fabricFluidStorage;
        Storage<FluidVariant> itemStorage = FluidStorage.ITEM.find(
                player.getItemInHand(hand),
                player.isCreative()
                        ? ContainerItemContext.forCreativeInteraction(player, player.getItemInHand(hand))
                        : ContainerItemContext.forPlayerInteraction(player, hand));
        if ((fabricFluidStorage.getAmount() == 0) || itemStorage == null)
            return false;

        try (var tx = Transaction.openOuter()) {
            FluidVariant resource = fabricFluidStorage.getResource();
            long maxExtract = Math.min(((SpiritFluidStorageImpl) fluidStorage).transferRate,
                    fabricFluidStorage.getAmount());
            long inserted = itemStorage.insert(resource, maxExtract, tx);
            long extracted = fabricFluidStorage.extract(resource, inserted, tx);
            // may not be good enough if an item has multiple fluid storages with different
            // sizes...
            long itemStorageCapacity = itemStorage.iterator().next().getCapacity();
            if (inserted == extracted &&
                    (inserted == itemStorageCapacity || itemStorageCapacity > FluidStack.bucketAmount())) {
                filled = inserted;
                if (!simulate) {
                    var fluid = resource.getFluid();
                    var sound = fluid.getPickupSound().orElse(fluid.is(FluidTags.LAVA) ? SoundEvents.BUCKET_FILL_LAVA : SoundEvents.BUCKET_FILL);
                    player.level().playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS);
                    tx.commit();
                }
            }
        }
        return filled > 0;
    }

    private static long fillFluidStorage(SpiritFluidStorage fluidStorage, Storage<FluidVariant> destStorage,
            boolean simulate) {
        long filled = 0;
        SingleVariantStorage<FluidVariant> fabricFluidStorage = ((SpiritFluidStorageImpl) fluidStorage).fabricFluidStorage;
        try (var tx = Transaction.openOuter()) {
            FluidVariant resource = fabricFluidStorage.getResource();
            long extracted = fabricFluidStorage.extract(resource, ((SpiritFluidStorageImpl) fluidStorage).transferRate,
                    tx);
            long inserted = destStorage.insert(resource, extracted, tx);
            if (inserted == extracted)
                filled = inserted;
            if (filled > 0 && !simulate)
                tx.commit();
        }
        return filled;
    }
}
