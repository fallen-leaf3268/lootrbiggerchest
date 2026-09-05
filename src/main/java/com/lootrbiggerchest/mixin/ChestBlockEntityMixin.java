package com.lootrbiggerchest.mixin;

import com.lootrbiggerchest.LootrBiggerChest;
import com.lootrbiggerchest.menu.LootrBiggerChestMenu.ContainerType;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import noobanidus.mods.lootr.block.entities.LootrChestBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ChestBlockEntity.class)
public class ChestBlockEntityMixin {

    @Shadow
    private NonNullList<ItemStack> items;

    @Inject(method = "getContainerSize", at = @At("HEAD"), cancellable = true)
    private void modifyChestSize(CallbackInfoReturnable<Integer> cir) {
        if (!((Object) this instanceof LootrChestBlockEntity)) return;
        BlockEntity blockEntity = (BlockEntity) (Object) this;
        CompoundTag data = blockEntity.getPersistentData();
        if (!LootrBiggerChest.shouldManage(data, ContainerType.CHEST)) return;
        LootrBiggerChest.ContainerStorageState before =
                LootrBiggerChest.captureStorageState(data, items);
        int[] size = LootrBiggerChest.resolveOrCreateSize(data, ContainerType.CHEST);
        LootrBiggerChest.ResizedContainer resized = LootrBiggerChest.reconcileSizeWithItems(
                data, size, items, "Lootr chest");
        items = resized.items();
        LootrBiggerChest.setChangedIfServer(blockEntity,
                LootrBiggerChest.storageStateChanged(before, data, items));
        cir.setReturnValue(resized.containerSize());
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void onSaveAdditional(CompoundTag tag, CallbackInfo ci) {
        if (!((Object) this instanceof LootrChestBlockEntity)) return;
        LootrBiggerChest.saveSizeToTag(((BlockEntity) (Object) this).getPersistentData(), tag);
        if (LootrBiggerChest.hasSerializedItems(tag) || LootrBiggerChest.hasOccupiedItems(items)) {
            LootrBiggerChest.saveAllItemsWithIntSlots(tag, items);
        }
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void afterLoad(CompoundTag tag, CallbackInfo ci) {
        if (!((Object) this instanceof LootrChestBlockEntity)) return;
        BlockEntity blockEntity = (BlockEntity) (Object) this;
        CompoundTag data = blockEntity.getPersistentData();
        LootrBiggerChest.loadSizeFromTag(tag, data);
        int required = LootrBiggerChest.requiredSlotsFromItems(tag, "Lootr chest");
        int loadedSlots = LootrBiggerChest.loadedContainerSlots(data, items.size(), required);
        items = LootrBiggerChest.resizeItemsSafely(items, loadedSlots, "Lootr chest");
        if (LootrBiggerChest.hasSerializedItems(tag)) {
            LootrBiggerChest.loadAllItemsWithIntSlots(tag, items, "Lootr chest");
        }
    }
}
