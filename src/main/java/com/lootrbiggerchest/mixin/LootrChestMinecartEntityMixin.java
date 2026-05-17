package com.lootrbiggerchest.mixin;

import com.lootrbiggerchest.LootrBiggerChest;
import com.lootrbiggerchest.LootrBiggerChestConfig;
import com.lootrbiggerchest.menu.LootrBiggerChestMenu;
import com.lootrbiggerchest.menu.LootrBiggerChestMenu.ContainerType;
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

    @Inject(method = "getContainerSize", at = @At("HEAD"), cancellable = true)
    private void modifyGetContainerSize(CallbackInfoReturnable<Integer> cir) {
        if (!LootrBiggerChestConfig.isExpanded()) return;
        CompoundTag data = ((Entity) (Object) this).getPersistentData();
        int[] size = LootrBiggerChest.resolveOrCreateSize(data, LootrBiggerChestConfig::pickMinecartSize);
        int totalSize = size[0] * size[1];
        AbstractMinecartContainerAccessor acc = (AbstractMinecartContainerAccessor) this;
        NonNullList<ItemStack> stacks = acc.getItemStacks();
        if (stacks.size() != totalSize) {
            acc.setItemStacks(NonNullList.withSize(totalSize, ItemStack.EMPTY));
        }
        cir.setReturnValue(totalSize);
    }

    @Inject(method = "createMenu", at = @At("HEAD"), cancellable = true)
    private void onCreateMenu(int id, Inventory playerInventoryIn,
                              CallbackInfoReturnable<AbstractContainerMenu> cir) {
        if (!LootrBiggerChestConfig.isExpanded()) return;

        CompoundTag data = ((Entity) (Object) this).getPersistentData();
        int[] size = LootrBiggerChest.resolveOrCreateSize(data, LootrBiggerChestConfig::pickMinecartSize);
        int rows = size[0], cols = size[1];

        if (playerInventoryIn.player instanceof ServerPlayer sp) {
            LootrBiggerChest.sendMenuSize(sp, id, rows, cols);
        }

        cir.setReturnValue(new LootrBiggerChestMenu(
                LootrBiggerChest.getMenu(ContainerType.MINECART), id, playerInventoryIn,
                (Container) (Object) this, rows, cols, ContainerType.MINECART));
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void onAddAdditionalSaveData(CompoundTag tag, CallbackInfo ci) {
        if (!LootrBiggerChestConfig.isExpanded()) return;
        LootrBiggerChest.saveSizeToTag(((Entity) (Object) this).getPersistentData(), tag);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("HEAD"))
    private void onReadAdditionalSaveData(CompoundTag tag, CallbackInfo ci) {
        if (!LootrBiggerChestConfig.isExpanded()) return;
        LootrBiggerChest.loadSizeFromTag(tag, ((Entity) (Object) this).getPersistentData());
    }
}
