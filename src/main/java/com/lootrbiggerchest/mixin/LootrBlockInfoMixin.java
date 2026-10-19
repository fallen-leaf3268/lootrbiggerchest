package com.lootrbiggerchest.mixin;

import com.lootrbiggerchest.LootrBiggerChest;
import com.lootrbiggerchest.menu.LootrBiggerChestMenu.ContainerType;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import noobanidus.mods.lootr.common.api.data.ILootrInfoProvider;
import noobanidus.mods.lootr.common.block.entity.LootrBarrelBlockEntity;
import noobanidus.mods.lootr.common.block.entity.LootrChestBlockEntity;
import noobanidus.mods.lootr.common.block.entity.LootrShulkerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = {LootrChestBlockEntity.class, LootrBarrelBlockEntity.class,
        LootrShulkerBlockEntity.class}, remap = false)
public abstract class LootrBlockInfoMixin {

    @Inject(method = "getInfoContainerSize", at = @At("HEAD"), cancellable = true)
    private void provideExpandedCapacity(CallbackInfoReturnable<Integer> cir) {
        BlockEntity blockEntity = (BlockEntity) (Object) this;
        ILootrInfoProvider provider = (ILootrInfoProvider) this;
        ContainerType type = LootrBiggerChest.containerType(this);
        if (type == null || blockEntity.getLevel() == null || blockEntity.getLevel().isClientSide
                || provider.isInfoReferenceInventory()) return;
        CompoundTag data = blockEntity.getPersistentData();
        if (!LootrBiggerChest.shouldManage(data, type)) return;
        boolean hadSize = LootrBiggerChest.readStoredInventorySize(data) != null;
        int[] size = LootrBiggerChest.resolveOrCreateSize(data, type);
        if (!hadSize) blockEntity.setChanged();
        cir.setReturnValue(size[0] * size[1]);
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void saveDimensions(CompoundTag tag, HolderLookup.Provider provider, CallbackInfo ci) {
        LootrBiggerChest.saveSizeToTag(((BlockEntity) (Object) this).getPersistentData(), tag);
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void loadDimensions(CompoundTag tag, HolderLookup.Provider provider, CallbackInfo ci) {
        LootrBiggerChest.loadSizeFromTag(tag, ((BlockEntity) (Object) this).getPersistentData());
    }
}
