package com.lootrbiggerchest.mixin;

import com.lootrbiggerchest.LootrBiggerChest;
import com.lootrbiggerchest.LootrBiggerChestConfig;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import noobanidus.mods.lootr.block.entities.LootrBarrelBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LootrBarrelBlockEntity.class)
public class LootrBarrelBlockEntityMixin {

    @Shadow(remap = false)
    private NonNullList<ItemStack> items;

    @Inject(method = "getContainerSize", at = @At("HEAD"), cancellable = true)
    private void modifyGetContainerSize(CallbackInfoReturnable<Integer> cir) {
        if (!LootrBiggerChestConfig.isExpanded()) return;
        CompoundTag data = ((BlockEntity) (Object) this).getPersistentData();
        int[] size = LootrBiggerChest.resolveOrCreateSize(data, LootrBiggerChestConfig::pickBarrelSize);
        int totalSize = size[0] * size[1];
        if (items.size() != totalSize) {
            items = NonNullList.withSize(totalSize, ItemStack.EMPTY);
        }
        cir.setReturnValue(totalSize);
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void onSaveAdditional(CompoundTag tag, CallbackInfo ci) {
        if (!LootrBiggerChestConfig.isExpanded()) return;
        LootrBiggerChest.saveSizeToTag(((BlockEntity) (Object) this).getPersistentData(), tag);
    }

    @Inject(method = "load", at = @At("HEAD"))
    private void onLoad(CompoundTag tag, CallbackInfo ci) {
        if (!LootrBiggerChestConfig.isExpanded()) return;
        LootrBiggerChest.loadSizeFromTag(tag, ((BlockEntity) (Object) this).getPersistentData());
    }
}
