package com.lootrbiggerchest.mixin;

import com.lootrbiggerchest.LootrBiggerChestConfig;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import noobanidus.mods.lootr.block.entities.LootrShulkerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LootrShulkerBlockEntity.class)
public class LootrShulkerBlockEntityMixin {

    private static final String ROWS_KEY = "LBCRows";
    private static final String COLS_KEY = "LBCCols";

    @Shadow(remap = false)
    private NonNullList<ItemStack> itemStacks;

    @Inject(method = "getContainerSize", at = @At("HEAD"), cancellable = true)
    private void modifyGetContainerSize(CallbackInfoReturnable<Integer> cir) {
        if (LootrBiggerChestConfig.isExpanded()) {
            CompoundTag data = ((BlockEntity) (Object) this).getPersistentData();
            int rows, cols;
            if (data.contains(ROWS_KEY)) {
                rows = data.getInt(ROWS_KEY);
                cols = data.getInt(COLS_KEY);
            } else {
                int[] size = LootrBiggerChestConfig.pickShulkerSize();
                rows = size[0];
                cols = size[1];
                data.putInt(ROWS_KEY, rows);
                data.putInt(COLS_KEY, cols);
            }
            int totalSize = rows * cols;
            if (itemStacks.size() != totalSize) {
                itemStacks = NonNullList.withSize(totalSize, ItemStack.EMPTY);
            }
            cir.setReturnValue(totalSize);
        }
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void onSaveAdditional(CompoundTag tag, CallbackInfo ci) {
        if (!LootrBiggerChestConfig.isExpanded()) return;
        CompoundTag data = ((BlockEntity) (Object) this).getPersistentData();
        if (data.contains(ROWS_KEY)) {
            tag.putInt(ROWS_KEY, data.getInt(ROWS_KEY));
            tag.putInt(COLS_KEY, data.getInt(COLS_KEY));
        }
    }

    @Inject(method = "load", at = @At("HEAD"))
    private void onLoad(CompoundTag tag, CallbackInfo ci) {
        if (!LootrBiggerChestConfig.isExpanded()) return;
        if (tag.contains(ROWS_KEY)) {
            CompoundTag data = ((BlockEntity) (Object) this).getPersistentData();
            data.putInt(ROWS_KEY, tag.getInt(ROWS_KEY));
            data.putInt(COLS_KEY, tag.getInt(COLS_KEY));
        }
    }
}
