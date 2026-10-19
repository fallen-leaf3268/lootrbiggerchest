package com.lootrbiggerchest.mixin;

import com.lootrbiggerchest.LootrBiggerChest;
import com.lootrbiggerchest.menu.LootrBiggerChestMenu.ContainerType;
import net.minecraft.nbt.CompoundTag;

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
        LootrChestMinecartEntity self = (LootrChestMinecartEntity) (Object) this;
        CompoundTag data = self.getPersistentData();
        if (!LootrBiggerChest.shouldManage(data, ContainerType.MINECART)) return;
        int[] size = LootrBiggerChest.resolveOrCreateSize(data, ContainerType.MINECART);
        AbstractMinecartContainerAccessor accessor = (AbstractMinecartContainerAccessor) this;
        LootrBiggerChest.ResizedContainer resized = LootrBiggerChest.reconcileSizeWithItems(
                data, size, self.getItemStacks(), "Lootr minecart");
        accessor.setItemStacks(resized.items());
        cir.setReturnValue(resized.containerSize());
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void onAddAdditionalSaveData(CompoundTag tag, CallbackInfo ci) {
        LootrChestMinecartEntity self = (LootrChestMinecartEntity) (Object) this;
        LootrBiggerChest.saveSizeToTag(self.getPersistentData(), tag);
        if (LootrBiggerChest.hasSerializedItems(tag)
                || LootrBiggerChest.hasOccupiedItems(self.getItemStacks())) {
            LootrBiggerChest.saveAllItemsWithIntSlots(tag, self.getItemStacks());
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void afterReadAdditionalSaveData(CompoundTag tag, CallbackInfo ci) {
        LootrChestMinecartEntity self = (LootrChestMinecartEntity) (Object) this;
        CompoundTag data = self.getPersistentData();
        LootrBiggerChest.loadSizeFromTag(tag, data);
        int required = LootrBiggerChest.requiredSlotsFromItems(tag, "Lootr minecart");
        AbstractMinecartContainerAccessor accessor = (AbstractMinecartContainerAccessor) this;
        int loadedSlots = LootrBiggerChest.loadedContainerSlots(
                data, self.getItemStacks().size(), required);
        accessor.setItemStacks(LootrBiggerChest.resizeItemsSafely(
                self.getItemStacks(), loadedSlots, "Lootr minecart"));
        if (LootrBiggerChest.hasSerializedItems(tag)) {
            LootrBiggerChest.loadAllItemsWithIntSlots(
                    tag, self.getItemStacks(), "Lootr minecart");
        }
    }
}
