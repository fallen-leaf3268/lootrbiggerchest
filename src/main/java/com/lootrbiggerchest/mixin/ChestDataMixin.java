package com.lootrbiggerchest.mixin;

import com.lootrbiggerchest.LootrBiggerChest;
import com.lootrbiggerchest.LootrBiggerChestConfig;
import com.lootrbiggerchest.menu.LootrBiggerChestMenu.ContainerType;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import noobanidus.mods.lootr.api.LootFiller;
import noobanidus.mods.lootr.data.ChestData;
import noobanidus.mods.lootr.data.SpecialChestInventory;
import noobanidus.mods.lootr.entity.LootrChestMinecartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

@Mixin(value = ChestData.class, remap = false)
public abstract class ChestDataMixin implements LootrBiggerChest.ChestDataAccess, LootrBiggerChest.PlayerSizeAccess {

    @Unique
    private final Map<UUID, LootrBiggerChest.PlayerContainerSize>
            lootrbiggerchest$playerSizes = new HashMap<>();

    @Unique
    private final Map<UUID, LootrBiggerChest.PlayerContainerSize>
            lootrbiggerchest$pendingCreatedSizes = new HashMap<>();

    @Unique
    private final Map<UUID, Object> lootrbiggerchest$pendingCreatedSources = new HashMap<>();

    @Unique
    private static final ThreadLocal<ArrayDeque<Integer>> lootrbiggerchest$loadingSizes =
            ThreadLocal.withInitial(ArrayDeque::new);

    @Shadow(remap = false)
    private boolean custom;

    @Shadow(remap = false)
    private UUID uuid;

    @Shadow(remap = false)
    private int size;

    @Override
    public int lootrbiggerchest$getSize() {
        return size;
    }

    @Override
    public void lootrbiggerchest$growSize(int requestedSize) {
        if (requestedSize <= size) return;
        size = requestedSize;
        ((ChestData) (Object) this).setDirty();
    }

    @Override
    public boolean lootrbiggerchest$isCustom() {
        return custom;
    }

    @Override
    public LootrBiggerChest.PlayerContainerSize lootrbiggerchest$getPlayerSize(UUID playerId) {
        return lootrbiggerchest$playerSizes.get(playerId);
    }

    @Override
    public LootrBiggerChest.PlayerContainerSize lootrbiggerchest$rememberPlayerSize(
            UUID playerId, LootrBiggerChest.PlayerContainerSize size) {
        LootrBiggerChest.PlayerContainerSize previous =
                lootrbiggerchest$playerSizes.putIfAbsent(playerId, size);
        if (previous == null) ((ChestData) (Object) this).setDirty();
        return previous == null ? size : previous;
    }

    @Unique
    private void lootrbiggerchest$replacePlayerSize(
            UUID playerId, LootrBiggerChest.PlayerContainerSize replacement) {
        LootrBiggerChest.PlayerContainerSize previous =
                lootrbiggerchest$playerSizes.put(playerId, replacement);
        lootrbiggerchest$growSize(replacement.slots());
        if (!replacement.equals(previous)) ((ChestData) (Object) this).setDirty();
    }

    @Override
    public void lootrbiggerchest$loadPlayerSizes(CompoundTag root) {
        lootrbiggerchest$playerSizes.clear();
        lootrbiggerchest$playerSizes.putAll(LootrBiggerChest.readPlayerSizes(root));
        for (LootrBiggerChest.PlayerContainerSize playerSize
                : lootrbiggerchest$playerSizes.values()) {
            lootrbiggerchest$growSize(playerSize.slots());
        }
    }

    @Override
    public void lootrbiggerchest$savePlayerSizes(CompoundTag root) {
        LootrBiggerChest.writePlayerSizes(root, lootrbiggerchest$playerSizes);
    }

    @Unique
    private void lootrbiggerchest$clearPendingCreation(ServerPlayer player) {
        UUID playerId = player.getUUID();
        lootrbiggerchest$pendingCreatedSizes.remove(playerId);
        lootrbiggerchest$pendingCreatedSources.remove(playerId);
    }

    @Unique
    private LootrBiggerChest.PlayerContainerSize lootrbiggerchest$selectForCreation(
            ServerPlayer player, ContainerType type,
            LootrBiggerChest.PlayerContainerSize publicSize, Object source) {
        LootrBiggerChest.PlayerContainerSize selected = LootrBiggerChest.choosePlayerSize(
                lootrbiggerchest$playerSizes,
                player.getUUID(),
                LootrBiggerChestConfig.isPlayerSpecificSizeEnabled(),
                publicSize,
                () -> LootrBiggerChestConfig.pickSize(type));
        lootrbiggerchest$pendingCreatedSizes.put(player.getUUID(), selected);
        lootrbiggerchest$pendingCreatedSources.put(player.getUUID(), source);
        lootrbiggerchest$growSize(selected.slots());
        ((ChestData) (Object) this).setDirty();
        return selected;
    }

    @Unique
    private LootrBiggerChest.PlayerContainerSize lootrbiggerchest$takePendingSize(
            ServerPlayer player, Object source, ContainerType type) {
        UUID playerId = player.getUUID();
        LootrBiggerChest.PlayerContainerSize size =
                lootrbiggerchest$pendingCreatedSizes.remove(playerId);
        Object pendingSource = lootrbiggerchest$pendingCreatedSources.remove(playerId);
        if (size == null || type == null || pendingSource != source) return null;
        return size;
    }

    @Unique
    private LootrChestMinecartEntity lootrbiggerchest$getLiveMinecart(ServerPlayer player) {
        if (uuid == null) return null;
        Entity entity = ((ServerLevel) player.level()).getEntity(uuid);
        return entity instanceof LootrChestMinecartEntity cart ? cart : null;
    }

    @Inject(
            method = "createInventory(Lnet/minecraft/server/level/ServerPlayer;Lnoobanidus/mods/lootr/api/LootFiller;Lnet/minecraft/world/level/block/entity/RandomizableContainerBlockEntity;)Lnoobanidus/mods/lootr/data/SpecialChestInventory;",
            at = @At("HEAD"), remap = false)
    private void clearPendingRandomizableCreation(ServerPlayer player, LootFiller filler,
            RandomizableContainerBlockEntity tile,
            CallbackInfoReturnable<SpecialChestInventory> cir) {
        lootrbiggerchest$clearPendingCreation(player);
    }

    @Redirect(
            method = "createInventory(Lnet/minecraft/server/level/ServerPlayer;Lnoobanidus/mods/lootr/api/LootFiller;Lnet/minecraft/world/level/block/entity/RandomizableContainerBlockEntity;)Lnoobanidus/mods/lootr/data/SpecialChestInventory;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/NonNullList;withSize(ILjava/lang/Object;)Lnet/minecraft/core/NonNullList;", remap = true),
            remap = false)
    private NonNullList<ItemStack> allocatePlayerInventory(int ignoredSize, Object empty,
            ServerPlayer player, LootFiller filler, RandomizableContainerBlockEntity tile) {
        LootrChestMinecartEntity cart = null;
        Object source = tile;
        if (tile == null) {
            cart = lootrbiggerchest$getLiveMinecart(player);
            if (cart == null) {
                return NonNullList.withSize(ignoredSize, ItemStack.EMPTY);
            }
            source = cart;
        }
        ContainerType type = LootrBiggerChest.containerType(source);
        if (custom || type == null) {
            return NonNullList.withSize(ignoredSize, ItemStack.EMPTY);
        }
        int[] publicDimensions = tile == null
                ? LootrBiggerChest.resolveOrCreateSize(cart.getPersistentData(), type)
                : LootrBiggerChest.resolveOrCreateSize(tile.getPersistentData(), type);
        LootrBiggerChest.PlayerContainerSize selected = lootrbiggerchest$selectForCreation(
                player, type,
                new LootrBiggerChest.PlayerContainerSize(
                        publicDimensions[0], publicDimensions[1]),
                source);
        return NonNullList.withSize(selected.slots(), ItemStack.EMPTY);
    }

    @Inject(
            method = "createInventory(Lnet/minecraft/server/level/ServerPlayer;Lnoobanidus/mods/lootr/api/LootFiller;Lnet/minecraft/world/level/block/entity/RandomizableContainerBlockEntity;)Lnoobanidus/mods/lootr/data/SpecialChestInventory;",
            at = @At("RETURN"), remap = false)
    private void bindCreatedInventorySize(ServerPlayer player, LootFiller filler,
            RandomizableContainerBlockEntity tile,
            CallbackInfoReturnable<SpecialChestInventory> cir) {
        Object source = tile == null
                ? lootrbiggerchest$getLiveMinecart(player)
                : tile;
        ContainerType type = LootrBiggerChest.containerType(source);
        LootrBiggerChest.PlayerContainerSize size =
                lootrbiggerchest$takePendingSize(player, source, type);
        if (size != null && cir.getReturnValue() != null) {
            ((LootrBiggerChest.SizedInventoryAccess) cir.getReturnValue())
                    .lootrbiggerchest$setStoredSize(size);
        }
    }

    @Inject(
            method = "createInventory(Lnet/minecraft/server/level/ServerPlayer;Lnoobanidus/mods/lootr/api/LootFiller;Lnet/minecraft/world/level/block/entity/BaseContainerBlockEntity;Ljava/util/function/Supplier;Ljava/util/function/LongSupplier;)Lnoobanidus/mods/lootr/data/SpecialChestInventory;",
            at = @At("HEAD"), remap = false)
    private void clearPendingBaseCreation(ServerPlayer player, LootFiller filler,
            BaseContainerBlockEntity blockEntity,
            Supplier<ResourceLocation> tableSupplier, LongSupplier seedSupplier,
            CallbackInfoReturnable<SpecialChestInventory> cir) {
        lootrbiggerchest$clearPendingCreation(player);
    }

    @Redirect(
            method = "createInventory(Lnet/minecraft/server/level/ServerPlayer;Lnoobanidus/mods/lootr/api/LootFiller;Lnet/minecraft/world/level/block/entity/BaseContainerBlockEntity;Ljava/util/function/Supplier;Ljava/util/function/LongSupplier;)Lnoobanidus/mods/lootr/data/SpecialChestInventory;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/NonNullList;withSize(ILjava/lang/Object;)Lnet/minecraft/core/NonNullList;", remap = true),
            remap = false)
    private NonNullList<ItemStack> allocatePlayerBaseInventory(int ignoredSize, Object empty,
            ServerPlayer player, LootFiller filler, BaseContainerBlockEntity blockEntity,
            Supplier<ResourceLocation> tableSupplier, LongSupplier seedSupplier) {
        ContainerType type = LootrBiggerChest.containerType(blockEntity);
        if (custom || type == null) {
            return NonNullList.withSize(ignoredSize, ItemStack.EMPTY);
        }
        int[] publicDimensions = LootrBiggerChest.resolveOrCreateSize(
                blockEntity.getPersistentData(), type);
        LootrBiggerChest.PlayerContainerSize selected = lootrbiggerchest$selectForCreation(
                player, type,
                new LootrBiggerChest.PlayerContainerSize(
                        publicDimensions[0], publicDimensions[1]),
                blockEntity);
        return NonNullList.withSize(selected.slots(), ItemStack.EMPTY);
    }

    @Inject(
            method = "createInventory(Lnet/minecraft/server/level/ServerPlayer;Lnoobanidus/mods/lootr/api/LootFiller;Lnet/minecraft/world/level/block/entity/BaseContainerBlockEntity;Ljava/util/function/Supplier;Ljava/util/function/LongSupplier;)Lnoobanidus/mods/lootr/data/SpecialChestInventory;",
            at = @At("RETURN"), remap = false)
    private void bindCreatedBaseInventorySize(ServerPlayer player, LootFiller filler,
            BaseContainerBlockEntity blockEntity,
            Supplier<ResourceLocation> tableSupplier, LongSupplier seedSupplier,
            CallbackInfoReturnable<SpecialChestInventory> cir) {
        ContainerType type = LootrBiggerChest.containerType(blockEntity);
        LootrBiggerChest.PlayerContainerSize size =
                lootrbiggerchest$takePendingSize(player, blockEntity, type);
        if (size != null && cir.getReturnValue() != null) {
            ((LootrBiggerChest.SizedInventoryAccess) cir.getReturnValue())
                    .lootrbiggerchest$setStoredSize(size);
        }
    }

    @Inject(method = "load(Lnet/minecraft/nbt/CompoundTag;)Lnoobanidus/mods/lootr/data/ChestData;",
            at = @At("HEAD"), remap = false)
    private static void captureInventorySizes(CompoundTag root,
            CallbackInfoReturnable<ChestData> cir) {
        ArrayDeque<Integer> queue = lootrbiggerchest$loadingSizes.get();
        queue.clear();
        ListTag inventories = root.getList("inventories", Tag.TAG_COMPOUND);
        for (int index = 0; index < inventories.size(); index++) {
            CompoundTag entry = inventories.getCompound(index);
            queue.addLast(entry.contains("size", Tag.TAG_INT) ? entry.getInt("size") : -1);
        }
    }

    @ModifyArg(
            method = "load(Lnet/minecraft/nbt/CompoundTag;)Lnoobanidus/mods/lootr/data/ChestData;",
            at = @At(value = "INVOKE", target = "Lnoobanidus/mods/lootr/data/SpecialChestInventory;<init>(Lnoobanidus/mods/lootr/data/ChestData;ILnet/minecraft/nbt/CompoundTag;Ljava/lang/String;)V"),
            index = 1, remap = false, require = 0)
    private static int useSavedInventorySize(int globalSize) {
        Integer savedSize = lootrbiggerchest$loadingSizes.get().pollFirst();
        return savedSize != null && savedSize > 0 && savedSize <= LootrBiggerChest.MAX_SLOTS
                ? savedSize : globalSize;
    }

    @Inject(method = "load(Lnet/minecraft/nbt/CompoundTag;)Lnoobanidus/mods/lootr/data/ChestData;",
            at = @At("RETURN"), remap = false)
    private static void loadPlayerSizes(CompoundTag root,
            CallbackInfoReturnable<ChestData> cir) {
        ((LootrBiggerChest.PlayerSizeAccess) cir.getReturnValue())
                .lootrbiggerchest$loadPlayerSizes(root);
        lootrbiggerchest$loadingSizes.remove();
    }

    @Inject(method = "save(Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/nbt/CompoundTag;",
            at = @At("RETURN"), remap = false)
    private void savePlayerSizes(CompoundTag root,
            CallbackInfoReturnable<CompoundTag> cir) {
        lootrbiggerchest$savePlayerSizes(cir.getReturnValue());
    }

    @Inject(
            method = "getInventory(Lnet/minecraft/server/level/ServerPlayer;)Lnoobanidus/mods/lootr/data/SpecialChestInventory;",
            at = @At("RETURN"), remap = false)
    private void reconcileExistingInventory(ServerPlayer player,
            CallbackInfoReturnable<SpecialChestInventory> cir) {
        SpecialChestInventory inventory = cir.getReturnValue();
        if (inventory == null) return;
        LootrBiggerChest.SizedInventoryAccess sized =
                (LootrBiggerChest.SizedInventoryAccess) inventory;
        LootrBiggerChest.PlayerContainerSize rootSize =
                lootrbiggerchest$playerSizes.get(player.getUUID());
        LootrBiggerChest.PlayerContainerSize inventorySize =
                sized.lootrbiggerchest$getStoredSize();
        int occupiedSlots = LootrBiggerChest.highestOccupiedSlot(
                inventory.getInventoryContents());

        LootrBiggerChest.PlayerContainerSize safeSize =
                LootrBiggerChest.reconcileExistingPlayerSize(
                        rootSize, inventorySize, occupiedSlots);
        if (safeSize != null) {
            sized.lootrbiggerchest$setStoredSize(safeSize);
            if (!safeSize.equals(rootSize)) {
                lootrbiggerchest$replacePlayerSize(player.getUUID(), safeSize);
            }
            if (!safeSize.equals(inventorySize)) {
                ((ChestData) (Object) this).setDirty();
            }
            return;
        }

        LootrBiggerChest.PlayerContainerSize recovered =
                lootrbiggerchest$recoverExistingSize(player, inventory);
        sized.lootrbiggerchest$setStoredSize(recovered);
        lootrbiggerchest$replacePlayerSize(player.getUUID(), recovered);
    }

    @Unique
    private LootrBiggerChest.PlayerContainerSize lootrbiggerchest$recoverExistingSize(
            ServerPlayer player, SpecialChestInventory inventory) {
        int slots = inventory.getContainerSize();
        int occupiedSlots = LootrBiggerChest.highestOccupiedSlot(
                inventory.getInventoryContents());
        CompoundTag physicalData = null;
        ChestData self = (ChestData) (Object) this;
        BaseContainerBlockEntity blockEntity =
                LootrBiggerChest.findLootrBlockEntity(self, player.level());
        if (LootrBiggerChest.containerType(blockEntity) != null) {
            physicalData = blockEntity.getPersistentData();
        } else {
            LootrChestMinecartEntity entity =
                    LootrBiggerChest.findLootrMinecart(self, player.level());
            if (entity != null) physicalData = entity.getPersistentData();
        }
        LootrBiggerChest.PlayerContainerSize storedPhysical = physicalData == null
                ? null : LootrBiggerChest.readStoredInventorySize(physicalData);
        if (storedPhysical != null && storedPhysical.slots() == slots) {
            return storedPhysical;
        }
        if (slots == 27) {
            return LootrBiggerChest.recoverPlayerSize(
                    slots, occupiedSlots - 1, 3, 9);
        }
        int[] recovered = LootrBiggerChest.recoverySizeFor(
                Math.max(slots, occupiedSlots));
        return new LootrBiggerChest.PlayerContainerSize(recovered[0], recovered[1]);
    }

    @Inject(method = "setSize(I)V", at = @At("HEAD"), cancellable = true,
            remap = false, require = 0)
    private void onSetSize(int requestedSize, CallbackInfo ci) {
        if (!lootrbiggerchest$playerSizes.isEmpty()) {
            lootrbiggerchest$growSize(requestedSize);
            ci.cancel();
            return;
        }
        if (!LootrBiggerChest.canResizeChestData(size, requestedSize)) {
            LootrBiggerChest.LOGGER.warn("Refusing Lootr ChestData shrink from {} to {}",
                    size, requestedSize);
            ci.cancel();
        }
    }

    @Inject(method = "setSize(I)V", at = @At("TAIL"), remap = false, require = 0)
    private void afterSetSize(int requestedSize, CallbackInfo ci) {
        ((ChestData) (Object) this).setDirty();
    }
}
