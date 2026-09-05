package com.lootrbiggerchest.mixin;

import com.lootrbiggerchest.LootrBiggerChest;
import com.lootrbiggerchest.menu.LootrBiggerChestMenu;
import com.lootrbiggerchest.menu.LootrBiggerChestMenu.ContainerType;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import noobanidus.mods.lootr.api.MenuBuilder;
import noobanidus.mods.lootr.data.ChestData;
import noobanidus.mods.lootr.data.SpecialChestInventory;
import noobanidus.mods.lootr.entity.LootrChestMinecartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraftforge.network.NetworkHooks;

import java.util.OptionalInt;

@Pseudo
@Mixin(targets = "noobanidus.mods.lootr.data.SpecialChestInventory")
public class SpecialChestInventoryMixin implements LootrBiggerChest.AuthoritativeMenuProvider,
        LootrBiggerChest.SizedInventoryAccess {

    @Unique
    private LootrBiggerChest.PlayerContainerSize lootrbiggerchest$storedSize;

    @Shadow(remap = false)
    private NonNullList<ItemStack> contents;

    @Shadow(remap = false)
    @Final
    private ChestData newChestData;

    @Shadow(remap = false)
    private MenuBuilder menuBuilder;

    @Override
    public LootrBiggerChest.PlayerContainerSize lootrbiggerchest$getStoredSize() {
        return lootrbiggerchest$storedSize;
    }

    @Override
    public void lootrbiggerchest$setStoredSize(
            LootrBiggerChest.PlayerContainerSize size) {
        NonNullList<ItemStack> resized = LootrBiggerChest.resizeItemsSafely(
                contents, size.slots(), "Lootr player inventory");
        if (resized.size() != size.slots()) return;
        contents = resized;
        lootrbiggerchest$storedSize = size;
    }

    @Unique
    private LootrBiggerChest.ChestDataAccess lootrbiggerchest$chestData(
            String stage, Object playerId) {
        if (newChestData == null) {
            SpecialChestInventory self = (SpecialChestInventory) (Object) this;
            LootrBiggerChest.LOGGER.error(
                    "lootr_chest_data_missing stage={} player={} tile={} storedSize={} contentsSize={}",
                    stage, playerId, self.getTileId(), lootrbiggerchest$storedSize,
                    contents == null ? -1 : contents.size());
            throw new IllegalStateException("Missing Lootr ChestData during " + stage);
        }
        return (LootrBiggerChest.ChestDataAccess) newChestData;
    }

    @Group(name = "lootr_nbt_constructor", min = 1)
    @Inject(method = "<init>(Lnoobanidus/mods/lootr/data/ChestData;Lnet/minecraft/nbt/CompoundTag;Ljava/lang/String;)V",
            at = @At("RETURN"), remap = false, require = 0)
    private void afterNbtConstructor(ChestData chestData, CompoundTag tag, String name,
                                     CallbackInfo ci) {
        lootrbiggerchest$restoreLoadedInventory(tag, -1);
    }

    @Group(name = "lootr_nbt_constructor", min = 1)
    @Inject(method = "<init>(Lnoobanidus/mods/lootr/data/ChestData;ILnet/minecraft/nbt/CompoundTag;Ljava/lang/String;)V",
            at = @At("RETURN"), remap = false, require = 0)
    private void afterSizedNbtConstructor(ChestData chestData, int size, CompoundTag tag,
                                          String name, CallbackInfo ci) {
        lootrbiggerchest$restoreLoadedInventory(tag, size);
    }

    @Unique
    private void lootrbiggerchest$restoreLoadedInventory(CompoundTag tag,
                                                          int savedSlots) {
        LootrBiggerChest.ChestDataAccess chestData =
                lootrbiggerchest$chestData("restore", "none");
        int oldContentsSize = contents.size();
        int oldGlobalSize = chestData.lootrbiggerchest$getSize();
        LootrBiggerChest.PlayerContainerSize stored =
                LootrBiggerChest.readStoredInventorySize(tag);
        if (stored != null) {
            int requiredSlots = LootrBiggerChest.requiredSlotsFromItems(
                    tag, "Lootr player inventory");
            LootrBiggerChest.PlayerContainerSize safeStored =
                    LootrBiggerChest.ensurePlayerSizeContainsItems(stored, requiredSlots);
            lootrbiggerchest$setStoredSize(safeStored);
            LootrBiggerChest.loadAllItemsWithIntSlots(
                    tag, contents, "Lootr player inventory");
            chestData.lootrbiggerchest$growSize(safeStored.slots());
            boolean dirty = oldContentsSize != contents.size()
                    || !safeStored.equals(stored)
                    || LootrBiggerChest.itemsNeedNormalization(tag, contents.size());
            if (dirty) newChestData.setDirty();
            return;
        }
        int allocationBaseline = savedSlots > 0 ? savedSlots
                : Math.max(oldContentsSize, oldGlobalSize);
        int required = Math.max(LootrBiggerChest.requiredSlotsFromItems(
                tag, "Lootr player inventory"), allocationBaseline);
        int exactSize = required;
        contents = LootrBiggerChest.resizeItemsSafely(
                contents, exactSize, "Lootr player inventory");
        if (exactSize > oldGlobalSize) {
            chestData.lootrbiggerchest$growSize(exactSize);
        }
        boolean normalizeItems = LootrBiggerChest.itemsNeedNormalization(tag, exactSize);
        boolean loadedItems = LootrBiggerChest.hasSerializedItems(tag);
        if (loadedItems) {
            LootrBiggerChest.loadAllItemsWithIntSlots(tag, contents, "Lootr player inventory");
        }
        boolean dirty = normalizeItems || oldContentsSize != contents.size()
                || oldGlobalSize != chestData.lootrbiggerchest$getSize();
        if (dirty) newChestData.setDirty();
    }

    @Override
    @Unique
    public LootrBiggerChest.AuthoritativeMenuSpec lootrbiggerchest$prepareMenu(Player player) {
        SpecialChestInventory self = (SpecialChestInventory) (Object) this;
        if (menuBuilder != null) return null;
        LootrBiggerChest.ChestDataAccess chestData =
                lootrbiggerchest$chestData("prepare_menu", player.getUUID());
        if (chestData.lootrbiggerchest$isCustom()) return null;
        Level level = player.level();
        ContainerType type = LootrBiggerChest.resolveContainerType(newChestData, level);

        if (type == null) {
            LootrBiggerChest.LOGGER.error(
                    "lootr_type_unresolved player={} tile={} contentsSize={}",
                    player.getUUID(), self.getTileId(), contents.size());
            throw new IllegalStateException("Cannot resolve Lootr container type for player "
                    + player.getUUID());
        }

        if (lootrbiggerchest$storedSize != null) {
            return lootrbiggerchest$finishMenuSize(player, type,
                    LootrBiggerChest.reconcilePlayerMenuSize(
                            lootrbiggerchest$storedSize, contents,
                            "Lootr player inventory " + self.getTileId()));
        }

        BaseContainerBlockEntity tile = LootrBiggerChest.findLootrBlockEntity(newChestData, level);
        LootrChestMinecartEntity entity = type == ContainerType.MINECART
                ? LootrBiggerChest.findLootrMinecart(newChestData, level) : null;
        if (tile == null && entity == null) {
            LootrBiggerChest.LOGGER.error(
                    "lootr_container_missing player={} tile={} type={}",
                    player.getUUID(), self.getTileId(), type);
            throw new IllegalStateException("Lootr container disappeared before opening for player "
                    + player.getUUID());
        }
        CompoundTag data = tile != null
                ? tile.getPersistentData() : entity.getPersistentData();

        int oldContentsSize = contents.size();
        int oldGlobalSize = chestData.lootrbiggerchest$getSize();
        LootrBiggerChest.ContainerStorageState before =
                LootrBiggerChest.captureStorageState(data, contents);
        int[] size = LootrBiggerChest.resolveOrCreateSize(data, type);
        int minimumSlots = Math.max(oldContentsSize, oldGlobalSize);
        LootrBiggerChest.ResizedContainer resized = LootrBiggerChest.reconcileSizeWithItems(
                data, size, contents, minimumSlots, "Lootr player inventory " + self.getTileId());
        contents = resized.items();
        if (resized.containerSize() > oldGlobalSize) {
            chestData.lootrbiggerchest$growSize(resized.containerSize());
        }
        if (oldContentsSize != contents.size()
                || oldGlobalSize != chestData.lootrbiggerchest$getSize()) {
            newChestData.setDirty();
        }
        if (tile != null) {
            LootrBiggerChest.setChangedIfServer(tile,
                    LootrBiggerChest.storageStateChanged(before, data, contents));
        }
        return lootrbiggerchest$finishMenuSize(player, type, resized);
    }

    @Unique
    private LootrBiggerChest.AuthoritativeMenuSpec lootrbiggerchest$finishMenuSize(
            Player player, ContainerType type, LootrBiggerChest.ResizedContainer resized) {
        LootrBiggerChest.PlayerContainerSize finalSize =
                new LootrBiggerChest.PlayerContainerSize(resized.rows(), resized.columns());
        boolean changed = contents != resized.items()
                || !finalSize.equals(lootrbiggerchest$storedSize);
        contents = resized.items();
        lootrbiggerchest$storedSize = finalSize;
        lootrbiggerchest$chestData("persist_size", player.getUUID())
                .lootrbiggerchest$growSize(finalSize.slots());
        LootrBiggerChest.PlayerContainerSize persisted =
                ((LootrBiggerChest.PlayerSizeAccess) newChestData)
                        .lootrbiggerchest$rememberPlayerSize(player.getUUID(), finalSize);
        if (changed) newChestData.setDirty();
        SpecialChestInventory self = (SpecialChestInventory) (Object) this;
        if (!finalSize.equals(persisted)) {
            LootrBiggerChest.LOGGER.error(
                    "lootr_size_conflict player={} tile={} type={} inventorySize={} persistedSize={}",
                    player.getUUID(), self.getTileId(), type, finalSize, persisted);
            throw new IllegalStateException("Conflicting persisted Lootr player size for "
                    + player.getUUID() + ": inventory=" + finalSize.rows() + "x"
                    + finalSize.columns() + ", root=" + persisted.rows() + "x"
                    + persisted.columns());
        }
        return new LootrBiggerChest.AuthoritativeMenuSpec(
                type, finalSize.rows(), finalSize.columns());
    }

    @Inject(method = "writeItems()Lnet/minecraft/nbt/CompoundTag;",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void writeIntSlotItems(CallbackInfoReturnable<CompoundTag> cir) {
        CompoundTag result = LootrBiggerChest.saveAllItemsWithIntSlots(
                new CompoundTag(), contents);
        if (lootrbiggerchest$storedSize != null) {
            result.putInt(LootrBiggerChest.ROWS_KEY, lootrbiggerchest$storedSize.rows());
            result.putInt(LootrBiggerChest.COLS_KEY, lootrbiggerchest$storedSize.columns());
        }
        cir.setReturnValue(result);
    }

    @Inject(method = "createMenu(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/inventory/AbstractContainerMenu;",
            at = @At("HEAD"), cancellable = true, remap = true)
    private void createExpandedMenu(int id, Inventory inventory, Player player,
                                    CallbackInfoReturnable<AbstractContainerMenu> cir) {
        LootrBiggerChest.AuthoritativeMenuSpec spec = lootrbiggerchest$prepareMenu(player);
        if (spec == null) return;
        SpecialChestInventory self = (SpecialChestInventory) (Object) this;
        cir.setReturnValue(new LootrBiggerChestMenu(
                LootrBiggerChest.getMenu(spec.type()), id, inventory, self,
                spec.rows(), spec.columns(), spec.type()));
    }
}

@Mixin(ServerPlayer.class)
abstract class ServerPlayerMenuMixin {

    @Inject(method = "openMenu(Lnet/minecraft/world/MenuProvider;)Ljava/util/OptionalInt;",
            at = @At("HEAD"), cancellable = true, remap = true)
    private void openWithAuthoritativeDimensions(MenuProvider provider,
                                                 CallbackInfoReturnable<OptionalInt> cir) {
        if (provider instanceof LootrBiggerChest.AuthoritativeMenuProvider authoritative) {
            ServerPlayer serverPlayer = (ServerPlayer) (Object) this;
            LootrBiggerChest.AuthoritativeMenuSpec spec =
                    authoritative.lootrbiggerchest$prepareMenu(serverPlayer);
            if (spec != null) {
                NetworkHooks.openScreen(serverPlayer, provider, buffer -> {
                    buffer.writeEnum(spec.type());
                    buffer.writeInt(spec.rows());
                    buffer.writeInt(spec.columns());
                });
                cir.setReturnValue(OptionalInt.of(serverPlayer.containerMenu.containerId));
            }
        }
    }
}
