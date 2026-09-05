package com.lootrbiggerchest.mixin;

import com.lootrbiggerchest.LootrBiggerChest;
import com.lootrbiggerchest.menu.LootrBiggerChestMenu.ContainerType;
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
        BlockEntity blockEntity = (BlockEntity) (Object) this;
        CompoundTag data = blockEntity.getPersistentData();
        if (!LootrBiggerChest.shouldManage(data, ContainerType.BARREL)) return;
        LootrBiggerChest.ContainerStorageState before =
                LootrBiggerChest.captureStorageState(data, items);
        int[] size = LootrBiggerChest.resolveOrCreateSize(data, ContainerType.BARREL);
        LootrBiggerChest.ResizedContainer resized = LootrBiggerChest.reconcileSizeWithItems(
                data, size, items, "Lootr barrel");
        items = resized.items();
        LootrBiggerChest.setChangedIfServer(blockEntity,
                LootrBiggerChest.storageStateChanged(before, data, items));
        cir.setReturnValue(resized.containerSize());
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void onSaveAdditional(CompoundTag tag, CallbackInfo ci) {
        LootrBiggerChest.saveSizeToTag(((BlockEntity) (Object) this).getPersistentData(), tag);
        if (LootrBiggerChest.hasSerializedItems(tag) || LootrBiggerChest.hasOccupiedItems(items)) {
            LootrBiggerChest.saveAllItemsWithIntSlots(tag, items);
        }
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void afterLoad(CompoundTag tag, CallbackInfo ci) {
        BlockEntity blockEntity = (BlockEntity) (Object) this;
        CompoundTag data = blockEntity.getPersistentData();
        LootrBiggerChest.loadSizeFromTag(tag, data);
        int required = LootrBiggerChest.requiredSlotsFromItems(tag, "Lootr barrel");
        int loadedSlots = LootrBiggerChest.loadedContainerSlots(data, items.size(), required);
        items = LootrBiggerChest.resizeItemsSafely(items, loadedSlots, "Lootr barrel");
        if (LootrBiggerChest.hasSerializedItems(tag)) {
            LootrBiggerChest.loadAllItemsWithIntSlots(tag, items, "Lootr barrel");
        }
    }
}
