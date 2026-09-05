package com.lootrbiggerchest.mixin;

import com.lootrbiggerchest.LootrBiggerChest;
import com.lootrbiggerchest.menu.LootrBiggerChestMenu.ContainerType;
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

    @Shadow(remap = false)
    private NonNullList<ItemStack> itemStacks;

    @Inject(method = "getContainerSize", at = @At("HEAD"), cancellable = true)
    private void modifyGetContainerSize(CallbackInfoReturnable<Integer> cir) {
        BlockEntity blockEntity = (BlockEntity) (Object) this;
        CompoundTag data = blockEntity.getPersistentData();
        if (!LootrBiggerChest.shouldManage(data, ContainerType.SHULKER)) return;
        LootrBiggerChest.ContainerStorageState before =
                LootrBiggerChest.captureStorageState(data, itemStacks);
        int[] size = LootrBiggerChest.resolveOrCreateSize(data, ContainerType.SHULKER);
        LootrBiggerChest.ResizedContainer resized = LootrBiggerChest.reconcileSizeWithItems(
                data, size, itemStacks, "Lootr shulker");
        itemStacks = resized.items();
        LootrBiggerChest.setChangedIfServer(blockEntity,
                LootrBiggerChest.storageStateChanged(before, data, itemStacks));
        cir.setReturnValue(resized.containerSize());
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void onSaveAdditional(CompoundTag tag, CallbackInfo ci) {
        LootrBiggerChest.saveSizeToTag(((BlockEntity) (Object) this).getPersistentData(), tag);
        if (LootrBiggerChest.hasSerializedItems(tag) || LootrBiggerChest.hasOccupiedItems(itemStacks)) {
            LootrBiggerChest.saveAllItemsWithIntSlots(tag, itemStacks);
        }
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void afterLoad(CompoundTag tag, CallbackInfo ci) {
        BlockEntity blockEntity = (BlockEntity) (Object) this;
        CompoundTag data = blockEntity.getPersistentData();
        LootrBiggerChest.loadSizeFromTag(tag, data);
        int required = LootrBiggerChest.requiredSlotsFromItems(tag, "Lootr shulker");
        int loadedSlots = LootrBiggerChest.loadedContainerSlots(
                data, itemStacks.size(), required);
        itemStacks = LootrBiggerChest.resizeItemsSafely(
                itemStacks, loadedSlots, "Lootr shulker");
        if (LootrBiggerChest.hasSerializedItems(tag)) {
            LootrBiggerChest.loadAllItemsWithIntSlots(tag, itemStacks, "Lootr shulker");
        }
    }
}
