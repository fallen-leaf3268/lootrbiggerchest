package com.lootrbiggerchest.mixin;

import com.lootrbiggerchest.LootrBiggerChest;
import com.lootrbiggerchest.menu.LootrBiggerChestMenu.ContainerType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

import noobanidus.mods.lootr.entity.LootrChestMinecartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LootrChestMinecartEntity.class)
public class LootrChestMinecartEntityMixin {

    @Inject(method = "getContainerSize", at = @At("HEAD"), cancellable = true, remap = false)
    private void modifyGetContainerSize(CallbackInfoReturnable<Integer> cir) {
        CompoundTag data = ((Entity) (Object) this).getPersistentData();
        if (!LootrBiggerChest.shouldManage(data, ContainerType.MINECART)) return;
        int[] size = LootrBiggerChest.resolveOrCreateSize(data, ContainerType.MINECART);
        AbstractMinecartContainerAccessor accessor = (AbstractMinecartContainerAccessor) this;
        LootrBiggerChest.ResizedContainer resized = LootrBiggerChest.reconcileSizeWithItems(
                data, size, accessor.getItemStacks(), "Lootr minecart");
        accessor.setItemStacks(resized.items());
        cir.setReturnValue(resized.containerSize());
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void onAddAdditionalSaveData(CompoundTag tag, CallbackInfo ci) {
        LootrBiggerChest.saveSizeToTag(((Entity) (Object) this).getPersistentData(), tag);
        AbstractMinecartContainerAccessor accessor = (AbstractMinecartContainerAccessor) this;
        if (LootrBiggerChest.hasSerializedItems(tag)
                || LootrBiggerChest.hasOccupiedItems(accessor.getItemStacks())) {
            LootrBiggerChest.saveAllItemsWithIntSlots(tag, accessor.getItemStacks());
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void afterReadAdditionalSaveData(CompoundTag tag, CallbackInfo ci) {
        CompoundTag data = ((Entity) (Object) this).getPersistentData();
        LootrBiggerChest.loadSizeFromTag(tag, data);
        int required = LootrBiggerChest.requiredSlotsFromItems(tag, "Lootr minecart");
        AbstractMinecartContainerAccessor accessor = (AbstractMinecartContainerAccessor) this;
        int loadedSlots = LootrBiggerChest.loadedContainerSlots(
                data, accessor.getItemStacks().size(), required);
        accessor.setItemStacks(LootrBiggerChest.resizeItemsSafely(
                accessor.getItemStacks(), loadedSlots, "Lootr minecart"));
        if (LootrBiggerChest.hasSerializedItems(tag)) {
            LootrBiggerChest.loadAllItemsWithIntSlots(
                    tag, accessor.getItemStacks(), "Lootr minecart");
        }
    }
}
