package com.lootrbiggerchest.mixin;

import com.lootrbiggerchest.LootrBiggerChest;
import com.lootrbiggerchest.menu.LootrBiggerChestMenu;
import com.lootrbiggerchest.menu.LootrBiggerChestMenu.ContainerType;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import noobanidus.mods.lootr.common.api.LootrAPI;
import noobanidus.mods.lootr.common.api.MenuBuilder;
import noobanidus.mods.lootr.common.api.data.ILootrSavedData;
import noobanidus.mods.lootr.common.api.data.inventory.ILootrInventory;
import noobanidus.mods.lootr.common.data.LootrInventory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LootrInventory.class, remap = false)
public abstract class LootrInventoryMixin implements LootrBiggerChest.SizedInventoryAccess,
        ILootrInventory {

    @Shadow
    @Final
    @Mutable
    private NonNullList<ItemStack> contents;

    @Shadow
    private ILootrSavedData info;

    @Shadow
    private MenuBuilder menuBuilder;

    @Unique
    private LootrBiggerChest.PlayerContainerSize lootrbiggerchest$storedSize;

    @Unique
    private boolean lootrbiggerchest$legacyIntSlots;

    @Override
    public LootrBiggerChest.PlayerContainerSize lootrbiggerchest$getStoredSize() {
        return lootrbiggerchest$storedSize;
    }

    @Override
    public void lootrbiggerchest$setStoredSize(LootrBiggerChest.PlayerContainerSize size) {
        NonNullList<ItemStack> resized = LootrBiggerChest.resizeItemsSafely(
                contents, size.slots(), "Lootr player inventory");
        if (resized.size() != size.slots()) return;
        contents = resized;
        lootrbiggerchest$storedSize = size;
    }

    @Override
    public void lootrbiggerchest$markLegacyIntSlots() {
        lootrbiggerchest$legacyIntSlots = true;
    }

    @Inject(method = "getContainerSize", at = @At("HEAD"), cancellable = true)
    private void useInventoryCapacity(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(contents.size());
    }

    @Inject(method = "createMenu", at = @At("HEAD"), cancellable = true)
    private void createExpandedMenu(int id, Inventory inventory, Player player,
                                    CallbackInfoReturnable<AbstractContainerMenu> cir) {
        if (!(player instanceof ServerPlayer serverPlayer) || menuBuilder != null) return;
        ContainerType type = info.isInfoReferenceInventory() ? null
                : LootrBiggerChest.resolveContainerType(info, player.level());
        if (type == null) {
            if (lootrbiggerchest$storedSize == null) return;
            if (!info.isInfoReferenceInventory()) {
                throw new IllegalStateException("Cannot resolve Lootr container type for "
                        + info.getInfoKey());
            }
            type = ContainerType.CHEST;
        }
        if (lootrbiggerchest$storedSize == null) {
            lootrbiggerchest$storedSize = ((LootrBiggerChest.PlayerSizeAccess) info)
                    .lootrbiggerchest$getPlayerSize(LootrAPI.resolveServerPlayerTeam(serverPlayer));
            if (lootrbiggerchest$storedSize == null) {
                lootrbiggerchest$storedSize = LootrBiggerChest.recoverExistingPlayerSize(info, type, contents);
            }
        }
        LootrBiggerChest.ResizedContainer resized = LootrBiggerChest.reconcilePlayerMenuSize(
                lootrbiggerchest$storedSize, contents, "Lootr player inventory " + info.getInfoKey());
        LootrBiggerChest.PlayerContainerSize size = new LootrBiggerChest.PlayerContainerSize(
                resized.rows(), resized.columns());
        boolean changed = contents != resized.items() || !size.equals(lootrbiggerchest$storedSize);
        contents = resized.items();
        lootrbiggerchest$storedSize = size;
        if (!info.isInfoReferenceInventory()) {
            ((LootrBiggerChest.PlayerSizeAccess) info).lootrbiggerchest$rememberPlayerSize(
                    LootrAPI.resolveServerPlayerTeam(serverPlayer), size);
        }
        if (changed) info.markChanged();
        cir.setReturnValue(new LootrBiggerChestMenu(
                LootrBiggerChest.getMenu(type), id, inventory,
                (LootrInventory) (Object) this, size.rows(), size.columns(), type));
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        if (menu instanceof LootrBiggerChestMenu expanded) {
            buffer.writeEnum(expanded.getContainerType());
            buffer.writeInt(expanded.getRows());
            buffer.writeInt(expanded.getCols());
        }
    }

    @Inject(method = "saveToTag", at = @At("HEAD"), cancellable = true)
    private void writeIntSlotItems(HolderLookup.Provider provider,
                                   CallbackInfoReturnable<CompoundTag> cir) {
        if (lootrbiggerchest$storedSize == null && !lootrbiggerchest$legacyIntSlots) return;
        int maximumSlots = lootrbiggerchest$storedSize == null || info.isInfoReferenceInventory()
                ? contents.size() : LootrBiggerChest.MAX_SLOTS;
        CompoundTag result = LootrBiggerChest.saveAllItemsWithIntSlots(
                new CompoundTag(), contents, maximumSlots, provider);
        if (lootrbiggerchest$storedSize != null) {
            result.putInt(LootrBiggerChest.ROWS_KEY, lootrbiggerchest$storedSize.rows());
            result.putInt(LootrBiggerChest.COLS_KEY, lootrbiggerchest$storedSize.columns());
        }
        if (lootrbiggerchest$storedSize == null || contents.size() > LootrBiggerChest.MAX_SLOTS) {
            result.putInt(LootrBiggerChest.INT_INVENTORY_SIZE_KEY, contents.size());
        }
        cir.setReturnValue(result);
    }

}
