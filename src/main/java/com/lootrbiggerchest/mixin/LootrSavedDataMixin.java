package com.lootrbiggerchest.mixin;

import com.lootrbiggerchest.LootrBiggerChest;
import com.lootrbiggerchest.LootrBiggerChestConfig;
import com.lootrbiggerchest.menu.LootrBiggerChestMenu.ContainerType;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import noobanidus.mods.lootr.common.api.LootrAPI;
import noobanidus.mods.lootr.common.api.data.ILootrInfoProvider;
import noobanidus.mods.lootr.common.api.data.LootFiller;
import noobanidus.mods.lootr.common.data.LootrInventory;
import noobanidus.mods.lootr.common.data.LootrSavedData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mixin(value = LootrSavedData.class, remap = false)
public abstract class LootrSavedDataMixin implements LootrBiggerChest.PlayerSizeAccess {

    @Shadow
    @Final
    private Map<UUID, LootrInventory> inventories;

    @Unique
    private final Map<UUID, LootrBiggerChest.PlayerContainerSize> lootrbiggerchest$playerSizes =
            new HashMap<>();

    @Unique
    private ListTag lootrbiggerchest$legacyCustomPlayerSizes;

    @Unique
    private boolean lootrbiggerchest$legacyCustomSize;

    @Override
    public LootrBiggerChest.PlayerContainerSize lootrbiggerchest$getPlayerSize(UUID playerId) {
        return lootrbiggerchest$playerSizes.get(playerId);
    }

    @Override
    public LootrBiggerChest.PlayerContainerSize lootrbiggerchest$rememberPlayerSize(
            UUID playerId, LootrBiggerChest.PlayerContainerSize size) {
        LootrBiggerChest.PlayerContainerSize previous = lootrbiggerchest$playerSizes.put(playerId, size);
        if (!size.equals(previous)) ((LootrSavedData) (Object) this).markChanged();
        return size;
    }

    @Override
    public void lootrbiggerchest$loadPlayerSizes(CompoundTag root) {
        LootrSavedData self = (LootrSavedData) (Object) this;
        lootrbiggerchest$playerSizes.clear();
        lootrbiggerchest$legacyCustomPlayerSizes = null;
        lootrbiggerchest$legacyCustomSize = false;
        if (self.isInfoReferenceInventory()) {
            if (root.get(LootrBiggerChest.PLAYER_SIZES_KEY) instanceof ListTag sizes) {
                lootrbiggerchest$legacyCustomPlayerSizes = sizes.copy();
            }
            lootrbiggerchest$legacyCustomSize = root.getBoolean("LBCLegacyCustom")
                    || !LootrBiggerChest.readPlayerSizes(root).isEmpty();
            return;
        }
        lootrbiggerchest$playerSizes.putAll(LootrBiggerChest.readPlayerSizes(root));
    }

    @Override
    public void lootrbiggerchest$savePlayerSizes(CompoundTag root) {
        if (((LootrSavedData) (Object) this).isInfoReferenceInventory()) {
            if (lootrbiggerchest$legacyCustomPlayerSizes != null) {
                root.put(LootrBiggerChest.PLAYER_SIZES_KEY, lootrbiggerchest$legacyCustomPlayerSizes.copy());
            }
            if (lootrbiggerchest$legacyCustomSize) root.putBoolean("LBCLegacyCustom", true);
            return;
        }
        if (!lootrbiggerchest$playerSizes.isEmpty()) {
            LootrBiggerChest.writePlayerSizes(root, lootrbiggerchest$playerSizes);
        }
    }

    @Override
    public void lootrbiggerchest$loadInventories(CompoundTag root, HolderLookup.Provider provider) {
        LootrSavedData self = (LootrSavedData) (Object) this;
        boolean custom = self.isInfoReferenceInventory();
        ListTag entries = root.getList("inventories", Tag.TAG_COMPOUND);
        for (int index = 0; index < entries.size(); index++) {
            CompoundTag entry = entries.getCompound(index);
            UUID id = entry.getUUID("uuid");
            LootrInventory previous = inventories.get(id);
            CompoundTag tag = entry.getCompound("chest");
            int maximumSlots = Math.max(previous.getInventoryContents().size(), LootrBiggerChest.MAX_SLOTS);
            LootrBiggerChest.PlayerContainerSize stored = LootrBiggerChest.readStoredInventorySize(tag);
            if (custom && previous.getInventoryContents().size() > LootrBiggerChest.MAX_SLOTS) stored = null;
            if (stored == null && !custom) stored = lootrbiggerchest$playerSizes.get(id);
            int baseline = entry.contains("size", Tag.TAG_INT) ? entry.getInt("size")
                    : previous.getInventoryContents().size();
            if (baseline <= 0 || baseline > maximumSlots) baseline = previous.getInventoryContents().size();
            int legacySize = LootrBiggerChest.readLegacyIntInventorySize(tag, baseline);
            boolean legacy = legacySize > 0 || LootrBiggerChest.hasHighIntSlotItems(tag);
            if (stored == null && !legacy) continue;
            int required = LootrBiggerChest.requiredSlotsFromItems(
                    tag, "Lootr player inventory", maximumSlots, provider);
            if (stored != null) stored = LootrBiggerChest.ensurePlayerSizeContainsItems(stored, required);
            int slots = stored != null ? stored.slots()
                    : Math.max(legacySize > 0 ? legacySize : baseline, required);
            NonNullList<ItemStack> items = NonNullList.withSize(slots, ItemStack.EMPTY);
            LootrBiggerChest.loadAllItemsWithIntSlots(
                    tag, items, "Lootr player inventory", maximumSlots, provider);
            LootrInventory loaded = new LootrInventory(self, items);
            LootrBiggerChest.SizedInventoryAccess sized = (LootrBiggerChest.SizedInventoryAccess) loaded;
            if (stored != null) {
                sized.lootrbiggerchest$setStoredSize(stored);
                if (!custom) lootrbiggerchest$rememberPlayerSize(id, stored);
            }
            if (legacy) sized.lootrbiggerchest$markLegacyIntSlots();
            inventories.put(id, loaded);
            if (custom) lootrbiggerchest$legacyCustomSize = true;
            if (previous.getInventoryContents().size() != slots
                    || LootrBiggerChest.itemsNeedNormalization(tag, items, maximumSlots, provider)) {
                self.markChanged();
            }
        }
    }

    @Inject(method = "load", at = @At("RETURN"))
    private static void restorePlayerInventories(CompoundTag root, HolderLookup.Provider provider,
                                                  CallbackInfoReturnable<LootrSavedData> cir) {
        LootrBiggerChest.PlayerSizeAccess access = (LootrBiggerChest.PlayerSizeAccess) cir.getReturnValue();
        access.lootrbiggerchest$loadPlayerSizes(root);
        access.lootrbiggerchest$loadInventories(root, provider);
    }

    @Inject(method = "save(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/nbt/CompoundTag;",
            at = @At("RETURN"))
    private void savePlayerSizes(CompoundTag root, HolderLookup.Provider provider,
                                  CallbackInfoReturnable<CompoundTag> cir) {
        lootrbiggerchest$savePlayerSizes(cir.getReturnValue());
    }

    @Unique
    private NonNullList<ItemStack> lootrbiggerchest$allocateInventory(
            ILootrInfoProvider provider, UUID id) {
        ContainerType type = LootrBiggerChest.resolveContainerType(provider, provider.getInfoLevel());
        CompoundTag data = LootrBiggerChest.persistentData(provider);
        if (type == null || data == null) return provider.buildInitialInventory();
        boolean hadSize = LootrBiggerChest.readStoredInventorySize(data) != null;
        int[] dimensions = LootrBiggerChest.resolveOrCreateSize(data, type);
        LootrBiggerChest.PlayerContainerSize selected = LootrBiggerChest.choosePlayerSize(
                lootrbiggerchest$playerSizes, id, LootrBiggerChestConfig.isPlayerSpecificSizeEnabled(),
                new LootrBiggerChest.PlayerContainerSize(dimensions[0], dimensions[1]),
                () -> LootrBiggerChestConfig.pickSize(type));
        if (!hadSize && provider instanceof BlockEntity blockEntity) blockEntity.setChanged();
        ((LootrSavedData) (Object) this).markChanged();
        return NonNullList.withSize(selected.slots(), ItemStack.EMPTY);
    }

    @Redirect(method = "createInventory(Lnoobanidus/mods/lootr/common/api/data/ILootrInfoProvider;Lnet/minecraft/server/level/ServerPlayer;Lnoobanidus/mods/lootr/common/api/data/LootFiller;)Lnoobanidus/mods/lootr/common/data/LootrInventory;",
            at = @At(value = "INVOKE", target = "Lnoobanidus/mods/lootr/common/api/data/ILootrInfoProvider;buildInitialInventory()Lnet/minecraft/core/NonNullList;"))
    private NonNullList<ItemStack> allocatePlayerInventory(ILootrInfoProvider source,
            ILootrInfoProvider provider, ServerPlayer player, LootFiller filler) {
        return lootrbiggerchest$allocateInventory(source, LootrAPI.resolveServerPlayerTeam(player));
    }

    @Redirect(method = "createInventory(Lnoobanidus/mods/lootr/common/api/data/ILootrInfoProvider;Ljava/util/UUID;Lnoobanidus/mods/lootr/common/api/data/LootFiller;)Lnoobanidus/mods/lootr/common/data/LootrInventory;",
            at = @At(value = "INVOKE", target = "Lnoobanidus/mods/lootr/common/api/data/ILootrInfoProvider;buildInitialInventory()Lnet/minecraft/core/NonNullList;"))
    private NonNullList<ItemStack> allocateUuidInventory(ILootrInfoProvider source,
            ILootrInfoProvider provider, UUID id, LootFiller filler) {
        return lootrbiggerchest$allocateInventory(source, id);
    }

    @Inject(method = "createInventory(Lnoobanidus/mods/lootr/common/api/data/ILootrInfoProvider;Lnet/minecraft/server/level/ServerPlayer;Lnoobanidus/mods/lootr/common/api/data/LootFiller;)Lnoobanidus/mods/lootr/common/data/LootrInventory;",
            at = @At("RETURN"))
    private void bindPlayerSize(ILootrInfoProvider provider, ServerPlayer player, LootFiller filler,
                                 CallbackInfoReturnable<LootrInventory> cir) {
        lootrbiggerchest$bindSize(cir.getReturnValue(), LootrAPI.resolveServerPlayerTeam(player), provider);
    }

    @Inject(method = "createInventory(Lnoobanidus/mods/lootr/common/api/data/ILootrInfoProvider;Ljava/util/UUID;Lnoobanidus/mods/lootr/common/api/data/LootFiller;)Lnoobanidus/mods/lootr/common/data/LootrInventory;",
            at = @At("RETURN"))
    private void bindUuidSize(ILootrInfoProvider provider, UUID id, LootFiller filler,
                               CallbackInfoReturnable<LootrInventory> cir) {
        lootrbiggerchest$bindSize(cir.getReturnValue(), id, provider);
    }

    @Unique
    private void lootrbiggerchest$bindSize(LootrInventory inventory, UUID id, ILootrInfoProvider provider) {
        if (LootrBiggerChest.resolveContainerType(provider, provider.getInfoLevel()) == null
                || LootrBiggerChest.persistentData(provider) == null) return;
        LootrBiggerChest.PlayerContainerSize size = lootrbiggerchest$playerSizes.get(id);
        if (inventory != null && size != null) {
            ((LootrBiggerChest.SizedInventoryAccess) inventory).lootrbiggerchest$setStoredSize(size);
        }
    }

    @Inject(method = "getInventory(Ljava/util/UUID;)Lnoobanidus/mods/lootr/common/data/LootrInventory;",
            at = @At("RETURN"))
    private void reconcileExistingInventory(UUID id, CallbackInfoReturnable<LootrInventory> cir) {
        LootrSavedData self = (LootrSavedData) (Object) this;
        LootrInventory inventory = cir.getReturnValue();
        if (inventory == null || self.isInfoReferenceInventory()) return;
        ContainerType type = LootrBiggerChest.resolveContainerType(self, self.getInfoLevel());
        if (type == null) return;
        LootrBiggerChest.SizedInventoryAccess sized = (LootrBiggerChest.SizedInventoryAccess) inventory;
        LootrBiggerChest.PlayerContainerSize size = LootrBiggerChest.reconcileExistingPlayerSize(
                lootrbiggerchest$playerSizes.get(id), sized.lootrbiggerchest$getStoredSize(),
                LootrBiggerChest.highestOccupiedSlot(inventory.getInventoryContents()));
        if (size == null) {
            size = LootrBiggerChest.recoverExistingPlayerSize(self, type, inventory.getInventoryContents());
        }
        boolean changed = !size.equals(sized.lootrbiggerchest$getStoredSize());
        sized.lootrbiggerchest$setStoredSize(size);
        lootrbiggerchest$rememberPlayerSize(id, size);
        if (changed) self.markChanged();
    }
}
