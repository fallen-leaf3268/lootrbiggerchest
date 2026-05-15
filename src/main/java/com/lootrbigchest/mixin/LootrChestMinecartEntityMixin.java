package com.lootrbigchest.mixin;

import com.lootrbigchest.LootrBigChest;
import com.lootrbigchest.LootrBigChestConfig;
import com.lootrbigchest.menu.LootrBigChestMenu;
import com.lootrbigchest.menu.LootrBigChestMenu.ContainerType;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import noobanidus.mods.lootr.entity.LootrChestMinecartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LootrChestMinecartEntity.class)
public class LootrChestMinecartEntityMixin {

    private static final String ROWS_KEY = "LBCRows";
    private static final String COLS_KEY = "LBCCols";

    @Inject(method = "getContainerSize", at = @At("HEAD"), cancellable = true)
    private void modifyGetContainerSize(CallbackInfoReturnable<Integer> cir) {
        if (LootrBigChestConfig.isExpanded()) {
            CompoundTag data = ((Entity) (Object) this).getPersistentData();
            int rows, cols;
            if (data.contains(ROWS_KEY)) {
                rows = data.getInt(ROWS_KEY);
                cols = data.getInt(COLS_KEY);
            } else {
                int[] size = LootrBigChestConfig.pickMinecartSize();
                rows = size[0];
                cols = size[1];
                data.putInt(ROWS_KEY, rows);
                data.putInt(COLS_KEY, cols);
            }
            int totalSize = rows * cols;
            AbstractMinecartContainerAccessor acc = (AbstractMinecartContainerAccessor) this;
            NonNullList<ItemStack> stacks = acc.getItemStacks();
            if (stacks.size() != totalSize) {
                acc.setItemStacks(NonNullList.withSize(totalSize, ItemStack.EMPTY));
            }
            cir.setReturnValue(totalSize);
        }
    }

    @Inject(method = "createMenu", at = @At("HEAD"), cancellable = true)
    private void onCreateMenu(int id, Inventory playerInventoryIn,
                              CallbackInfoReturnable<AbstractContainerMenu> cir) {
        if (!LootrBigChestConfig.isExpanded()) return;

        CompoundTag data = ((Entity) (Object) this).getPersistentData();
        int rows, cols;
        if (data.contains(ROWS_KEY)) {
            rows = data.getInt(ROWS_KEY);
            cols = data.getInt(COLS_KEY);
        } else {
            int[] size = LootrBigChestConfig.pickMinecartSize();
            rows = size[0];
            cols = size[1];
            data.putInt(ROWS_KEY, rows);
            data.putInt(COLS_KEY, cols);
        }

        if (playerInventoryIn.player instanceof ServerPlayer sp) {
            LootrBigChest.sendMenuSize(sp, id, rows, cols);
        }

        cir.setReturnValue(new LootrBigChestMenu(
                LootrBigChest.getMenu(ContainerType.MINECART), id, playerInventoryIn,
                (Container) (Object) this, rows, cols, ContainerType.MINECART));
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void onAddAdditionalSaveData(CompoundTag tag, CallbackInfo ci) {
        if (!LootrBigChestConfig.isExpanded()) return;
        CompoundTag data = ((Entity) (Object) this).getPersistentData();
        if (data.contains(ROWS_KEY)) {
            tag.putInt(ROWS_KEY, data.getInt(ROWS_KEY));
            tag.putInt(COLS_KEY, data.getInt(COLS_KEY));
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("HEAD"))
    private void onReadAdditionalSaveData(CompoundTag tag, CallbackInfo ci) {
        if (!LootrBigChestConfig.isExpanded()) return;
        if (tag.contains(ROWS_KEY)) {
            CompoundTag data = ((Entity) (Object) this).getPersistentData();
            data.putInt(ROWS_KEY, tag.getInt(ROWS_KEY));
            data.putInt(COLS_KEY, tag.getInt(COLS_KEY));
        }
    }
}
