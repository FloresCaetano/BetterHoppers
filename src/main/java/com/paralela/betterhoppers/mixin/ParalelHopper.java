package com.paralela.betterhoppers.mixin;

import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.*;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import org.jetbrains.annotations.Nullable;

import java.io.Console;
import java.util.Arrays;
import java.util.Stack;
import java.util.concurrent.Semaphore;

@Mixin(HopperBlockEntity.class)
public abstract class ParalelHopper {

    @Inject(
            method = "transfer(Lnet/minecraft/inventory/Inventory;Lnet/minecraft/inventory/Inventory;Lnet/minecraft/item/ItemStack;Lnet/minecraft/util/math/Direction;)Lnet/minecraft/item/ItemStack;",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void onTransfer(@Nullable Inventory from, Inventory to, ItemStack stack, @Nullable Direction side, CallbackInfoReturnable<ItemStack> info) {
        if (to instanceof SidedInventory sidedInventory && side != null) {
            int[] is = sidedInventory.getAvailableSlots(side);

            for (int i = 0; i < is.length && !stack.isEmpty(); i++) {
                stack = transfer(from, to, stack, is[i], side);
            }

            info.setReturnValue(stack);
        }

        int j = to.size();
        int numThreads = 4;
        int[] results = new int[numThreads];
        Thread[] threads = new Thread[numThreads];
        Arrays.fill(results, -1);
        Semaphore no_item_semaphore = new Semaphore(0);

        // Divide el trabajo entre los hilos
        int chunkSize = j / numThreads;
        for (int t = 0; t < numThreads; t++) {
            final int start = t * chunkSize;
            final int end = (t == numThreads - 1) ? j : (start + chunkSize);  // El último hilo toma el resto de las iteraciones

            ItemStack finalStack = stack;
            int finalT = t;
            threads[t] = new Thread(() -> {
                ItemStack localStack = finalStack.copy();
                for (int i = start; i < end && !localStack.isEmpty(); i++) {
                    if(canInsert(from, to, finalStack, i)){
                        System.err.println("El hilo " + String.valueOf(finalT) + "Econtro el valor" + " Slot: " + String.valueOf(i));
                        results[finalT] = i;
                    }
                }
                no_item_semaphore.release();
            });
            threads[t].start();
        }

        try {
            for(int i=0; i<numThreads; i++)
                no_item_semaphore.acquire();
        } catch (InterruptedException e) {
            System.out.println(e);
        }
        for(int slot : results){
            System.out.println(slot);
            if (slot != -1)
                stack = transfer(from, to, stack, slot, side);
        }

        info.setReturnValue(stack);
    }


    @Shadow
    private static ItemStack transfer(@Nullable Inventory from, Inventory to, ItemStack stack, int slot, @Nullable Direction side) {
        return ItemStack.EMPTY;
    }

    @Shadow
    private static boolean canMergeItems(ItemStack first, ItemStack second) {
        return false;
    }

    private static boolean canInsert(Inventory from, Inventory to, ItemStack stack, int slot) {
        ItemStack toStack = to.getStack(slot);
        return toStack.isEmpty() || canMergeItems(toStack, stack);
    }
}
