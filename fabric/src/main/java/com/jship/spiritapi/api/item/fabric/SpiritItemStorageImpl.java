package com.jship.spiritapi.api.item.fabric;

import java.util.List;

import com.jship.spiritapi.api.item.SpiritItemStorage;

import lombok.val;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

public class SpiritItemStorageImpl extends SpiritItemStorage {

    private final List<SlotConfig> slotConfigs;
    private final SimpleContainer inventory;
    public final InventoryStorage fabricItemStorage;
    private final Runnable onCommit;

    public SpiritItemStorageImpl(List<SlotConfig> slotConfigs, Runnable onCommit) {
        this.slotConfigs = slotConfigs;
        this.inventory = new SimpleContainer(slotConfigs.size()) {
            @Override
            public boolean canPlaceItem(int slot, ItemStack stack) {
                return SpiritItemStorageImpl.this.slotConfigs.get(slot).canInsert()
                        && (this.items.get(slot).isEmpty()
                                || this.items.get(slot).getCount() < slotConfigs.get(slot).maxStackSize())
                        && slotConfigs.get(slot).validItem().test(stack) && super.canAddItem(stack);
            }

            @Override
            public boolean canTakeItem(Container container, int slot, ItemStack stack) {
                return SpiritItemStorageImpl.this.slotConfigs.get(slot).canExtract()
                        && super.canTakeItem(container, slot, stack);
            }

            @Override
            public void setChanged() {
                SpiritItemStorageImpl.this.onCommit.run();
            }
        };
        this.fabricItemStorage = InventoryStorage.of(inventory, null);
        this.onCommit = onCommit;
    }

    public static SpiritItemStorage create(List<SlotConfig> slotConfigs, Runnable onCommit) {
        return new SpiritItemStorageImpl(slotConfigs, onCommit);
    }

    @Override
    public int getSlots() {
        return fabricItemStorage.getSlotCount();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return fabricItemStorage.getSlot(slot).getResource().toStack();
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        try (val tx = Transaction.openOuter()) {
            val slotMaxSize = Math.min(this.slotConfigs.get(slot).maxStackSize(), stack.getMaxStackSize());
            val maxInsertedAmount = Math.min(stack.getCount(), slotMaxSize - this.fabricItemStorage.getSlot(slot).getAmount());
            val insertedAmount = (int)this.fabricItemStorage.getSlot(slot).insert(ItemVariant.of(stack), maxInsertedAmount, tx);

            if (insertedAmount == 0)
                return stack;

            if (!simulate) {
                tx.commit();
                this.onCommit.run();
            }
            stack.shrink(insertedAmount);
            return stack;
        }
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        try (val tx = Transaction.openOuter()) {
            val variant = this.fabricItemStorage.getSlot(slot).getResource();
            val extractedAmount = (int)this.fabricItemStorage.getSlot(slot).extract(variant, variant.toStack().getCount(), tx);

            if (extractedAmount == 0)
                return ItemStack.EMPTY;

            if (!simulate) {
                tx.commit();
                this.onCommit.run();
            }

            return variant.toStack(extractedAmount);
        }
    }

    @Override
    public int getSlotLimit(int slot) {
        return slotConfigs.get(slot).maxStackSize();
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slotConfigs.get(slot).validItem().test(stack);
    }

    @Override
    public CompoundTag serializeNbt(HolderLookup.Provider lookupProvider) {
        CompoundTag nbt = new CompoundTag();
        nbt.put("Items", inventory.createTag(lookupProvider));
        return nbt;
    }

    @Override
    public void deserializeNbt(HolderLookup.Provider lookupProvider, CompoundTag nbt) {
        val itemsTag = (ListTag)nbt.get("Items");

        this.inventory.items.clear();
        
        for (int i = 0; i < itemsTag.size(); i++) {
            val stack = ItemStack.parse(lookupProvider, itemsTag.getCompound(i));
            try (val ctx = Transaction.openOuter()) {
                if (stack.isPresent() && !stack.get().isEmpty()) {
                    if (this.fabricItemStorage.insert(ItemVariant.of(stack.get()), stack.get().getCount(), ctx) > 0) {
                        ctx.commit();
                        this.onCommit.run();
                    }
                }
            }
        }
    }
}
