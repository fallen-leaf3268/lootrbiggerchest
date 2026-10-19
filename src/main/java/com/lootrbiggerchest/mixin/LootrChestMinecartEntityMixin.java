package com.lootrbiggerchest.mixin;

import com.lootrbiggerchest.LootrBiggerChest;
import com.lootrbiggerchest.menu.LootrBiggerChestMenu.ContainerType;
import net.minecraft.nbt.CompoundTag;

import noobanidus.mods.lootr.common.entity.LootrChestMinecartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LootrChestMinecartEntity.class, remap = false)
public abstract class LootrChestMinecartEntityMixin {

    @Inject(method = "getInfoContainerSize", at = @At("HEAD"), cancellable = true)
    private void provideExpandedCapacity(CallbackInfoReturnable<Integer> cir) {
        LootrChestMinecartEntity self = (LootrChestMinecartEntity) (Object) this;
        if (self.level().isClientSide || self.isInfoReferenceInventory()) return;
        CompoundTag data = self.getPersistentData();
        if (!LootrBiggerChest.shouldManage(data, ContainerType.MINECART)) return;
        int[] size = LootrBiggerChest.resolveOrCreateSize(data, ContainerType.MINECART);
        cir.setReturnValue(size[0] * size[1]);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void saveDimensions(CompoundTag tag, CallbackInfo ci) {
        LootrChestMinecartEntity self = (LootrChestMinecartEntity) (Object) this;
        LootrBiggerChest.saveSizeToTag(self.getPersistentData(), tag);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void loadDimensions(CompoundTag tag, CallbackInfo ci) {
        LootrChestMinecartEntity self = (LootrChestMinecartEntity) (Object) this;
        CompoundTag data = self.getPersistentData();
        LootrBiggerChest.loadSizeFromTag(tag, data);
    }
}
