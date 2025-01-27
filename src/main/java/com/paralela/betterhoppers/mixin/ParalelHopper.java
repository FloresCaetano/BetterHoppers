package com.paralela.betterhoppers.mixin;

import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import org.jetbrains.annotations.Nullable;

@Mixin(HopperBlockEntity.class)
public abstract class ParalelHopper {

    @Inject(
            method = "transfer(Lnet/minecraft/inventory/Inventory;Lnet/minecraft/inventory/Inventory;Lnet/minecraft/item/ItemStack;Lnet/minecraft/util/math/Direction;)Lnet/minecraft/item/ItemStack;",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void onTransfer(@Nullable Inventory from, Inventory to, ItemStack stack, @Nullable Direction side, CallbackInfoReturnable<ItemStack> info) {
        System.out.println("Salida custom");
        if (to instanceof SidedInventory sidedInventory && side != null) {
            int[] is = sidedInventory.getAvailableSlots(side);

            for (int i = 0; i < is.length && !stack.isEmpty(); i++) {
                stack = transfer(from, to, stack, is[i], side);
            }

            info.setReturnValue(stack);
        }

        int j = to.size();

        for (int i = 0; i < j && !stack.isEmpty(); i++) {
            stack = transfer(from, to, stack, i, side);
        }

        info.setReturnValue(stack);
    }

    @Shadow
    private static ItemStack transfer(@Nullable Inventory from, Inventory to, ItemStack stack, int slot, @Nullable Direction side) {
        return ItemStack.EMPTY;
    }
}
