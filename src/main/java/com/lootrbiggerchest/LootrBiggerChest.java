package com.lootrbiggerchest;

import com.lootrbiggerchest.menu.LootrBiggerChestMenu;
import com.lootrbiggerchest.menu.LootrBiggerChestMenu.ContainerType;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import noobanidus.mods.lootr.common.api.data.ILootrInfo;
import noobanidus.mods.lootr.common.api.data.ILootrInfoProvider;
import noobanidus.mods.lootr.common.block.entity.LootrBarrelBlockEntity;
import noobanidus.mods.lootr.common.block.entity.LootrChestBlockEntity;
import noobanidus.mods.lootr.common.block.entity.LootrInventoryBlockEntity;
import noobanidus.mods.lootr.common.block.entity.LootrShulkerBlockEntity;
import noobanidus.mods.lootr.common.entity.LootrChestMinecartEntity;

import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

@Mod(LootrBiggerChest.MOD_ID)
public class LootrBiggerChest {

    public static final String MOD_ID = "lootrbiggerchest";
    public static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    private static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<LootrBiggerChestMenu>> CHEST_MENU =
            MENU_TYPES.register("lootr_chest", () ->
                    IMenuTypeExtension.create((id, inv, data) ->
                            LootrBiggerChestMenu.fromNetwork(id, inv, data, ContainerType.CHEST)));

    public static final DeferredHolder<MenuType<?>, MenuType<LootrBiggerChestMenu>> BARREL_MENU =
            MENU_TYPES.register("lootr_barrel", () ->
                    IMenuTypeExtension.create((id, inv, data) ->
                            LootrBiggerChestMenu.fromNetwork(id, inv, data, ContainerType.BARREL)));

    public static final DeferredHolder<MenuType<?>, MenuType<LootrBiggerChestMenu>> SHULKER_MENU =
            MENU_TYPES.register("lootr_shulker", () ->
                    IMenuTypeExtension.create((id, inv, data) ->
                            LootrBiggerChestMenu.fromNetwork(id, inv, data, ContainerType.SHULKER)));

    public static final DeferredHolder<MenuType<?>, MenuType<LootrBiggerChestMenu>> MINECART_MENU =
            MENU_TYPES.register("lootr_minecart", () ->
                    IMenuTypeExtension.create((id, inv, data) ->
                            LootrBiggerChestMenu.fromNetwork(id, inv, data, ContainerType.MINECART)));

    public LootrBiggerChest(IEventBus modBus, ModContainer container) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            new com.lootrbiggerchest.proxy.ClientProxy(modBus);
        }
        container.registerConfig(ModConfig.Type.COMMON, LootrBiggerChestConfig.COMMON_SPEC);
        MENU_TYPES.register(modBus);
    }

    public static MenuType<LootrBiggerChestMenu> getMenu(ContainerType type) {
        return switch (type) {
            case CHEST -> CHEST_MENU.get();
            case BARREL -> BARREL_MENU.get();
            case SHULKER -> SHULKER_MENU.get();
            case MINECART -> MINECART_MENU.get();
        };
    }

    public static final String ROWS_KEY = "LBCRows";
    public static final String COLS_KEY = "LBCCols";
    public static final String PLAYER_SIZES_KEY = "LBCPlayerSizes";
    public static final String INT_INVENTORY_SIZE_KEY = "LBCIntInventorySize";
    public static final String PLAYER_KEY = "Player";
    public static final String ITEMS_KEY = "Items";
    public static final String SLOT_KEY = "Slot";
    public static final int MAX_SLOTS = LootrBiggerChestConfig.MAX_ROWS
            * LootrBiggerChestConfig.MAX_COLUMNS;

    public interface PlayerSizeAccess {
        PlayerContainerSize lootrbiggerchest$getPlayerSize(UUID playerId);
        PlayerContainerSize lootrbiggerchest$rememberPlayerSize(
                UUID playerId, PlayerContainerSize size);
        void lootrbiggerchest$loadPlayerSizes(CompoundTag root);
        void lootrbiggerchest$savePlayerSizes(CompoundTag root);
        void lootrbiggerchest$loadInventories(CompoundTag root, HolderLookup.Provider provider);
    }

    public interface SizedInventoryAccess {
        PlayerContainerSize lootrbiggerchest$getStoredSize();
        void lootrbiggerchest$setStoredSize(PlayerContainerSize size);
        void lootrbiggerchest$markLegacyIntSlots();
    }

    public record PlayerContainerSize(int rows, int columns) {
        public PlayerContainerSize {
            if (!LootrBiggerChestConfig.isValidSize(rows, columns)) {
                throw new IllegalArgumentException("Illegal container size " + rows + "x" + columns);
            }
        }

        public int slots() {
            return rows * columns;
        }
    }

    @Nullable
    public static ContainerType containerType(Object source) {
        if (source instanceof LootrInventoryBlockEntity) return null;
        if (source instanceof LootrBarrelBlockEntity) return ContainerType.BARREL;
        if (source instanceof LootrShulkerBlockEntity) return ContainerType.SHULKER;
        if (source instanceof LootrChestBlockEntity) return ContainerType.CHEST;
        if (source instanceof LootrChestMinecartEntity) return ContainerType.MINECART;
        return null;
    }

    @Nullable
    public static ContainerType resolveContainerType(ILootrInfo chestData, Level level) {
        if (level == null || level.isClientSide || chestData.isInfoReferenceInventory()) return null;
        Object container = chestData.getInfoContainer();
        ContainerType type = containerType(container);
        if (type == null || chestData instanceof ILootrInfoProvider && chestData != container) return null;
        ILootrInfoProvider physical = (ILootrInfoProvider) container;
        if (!chestData.getInfoUUID().equals(physical.getInfoUUID())
                || !chestData.getInfoKey().equals(physical.getInfoKey())
                || !chestData.getInfoDimension().equals(physical.getInfoDimension())
                || !chestData.getInfoPos().equals(physical.getInfoPos())) return null;
        return type;
    }

    @Nullable
    public static CompoundTag persistentData(ILootrInfo info) {
        Object container = info.getInfoContainer();
        if (container instanceof BlockEntity blockEntity) return blockEntity.getPersistentData();
        if (container instanceof Entity entity) return entity.getPersistentData();
        return null;
    }

    public static void writePlayerSizes(CompoundTag root,
                                        Map<UUID, PlayerContainerSize> sizes) {
        ListTag list = new ListTag();
        sizes.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    CompoundTag tag = new CompoundTag();
                    tag.putUUID(PLAYER_KEY, entry.getKey());
                    tag.putInt(ROWS_KEY, entry.getValue().rows());
                    tag.putInt(COLS_KEY, entry.getValue().columns());
                    list.add(tag);
                });
        root.put(PLAYER_SIZES_KEY, list);
    }

    public static Map<UUID, PlayerContainerSize> readPlayerSizes(CompoundTag root) {
        Map<UUID, PlayerContainerSize> result = new HashMap<>();
        ListTag list = root.getList(PLAYER_SIZES_KEY, Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            CompoundTag tag = list.getCompound(index);
            if (!tag.hasUUID(PLAYER_KEY)) continue;
            int rows = tag.getInt(ROWS_KEY);
            int columns = tag.getInt(COLS_KEY);
            if (!LootrBiggerChestConfig.isValidSize(rows, columns)) continue;
            result.put(tag.getUUID(PLAYER_KEY), new PlayerContainerSize(rows, columns));
        }
        return result;
    }

    @Nullable
    public static PlayerContainerSize readStoredInventorySize(CompoundTag chest) {
        if (!chest.contains(ROWS_KEY, Tag.TAG_INT)
                || !chest.contains(COLS_KEY, Tag.TAG_INT)) return null;
        int rows = chest.getInt(ROWS_KEY);
        int columns = chest.getInt(COLS_KEY);
        return LootrBiggerChestConfig.isValidSize(rows, columns)
                ? new PlayerContainerSize(rows, columns) : null;
    }

    public static PlayerContainerSize ensurePlayerSizeContainsItems(
            PlayerContainerSize requested, int occupiedSlots) {
        if (requested.slots() >= occupiedSlots) return requested;
        int[] recovered = recoverySizeFor(occupiedSlots);
        return new PlayerContainerSize(recovered[0], recovered[1]);
    }

    @Nullable
    public static PlayerContainerSize reconcileExistingPlayerSize(
            @Nullable PlayerContainerSize rootSize,
            @Nullable PlayerContainerSize inventorySize,
            int occupiedSlots) {
        PlayerContainerSize selected = inventorySize != null ? inventorySize : rootSize;
        return selected == null ? null : ensurePlayerSizeContainsItems(selected, occupiedSlots);
    }

    public static PlayerContainerSize choosePlayerSize(
            Map<UUID, PlayerContainerSize> sizes,
            UUID playerId,
            boolean independentEnabled,
            PlayerContainerSize publicSize,
            Supplier<int[]> picker) {
        PlayerContainerSize existing = sizes.get(playerId);
        if (existing != null) return existing;
        PlayerContainerSize selected;
        if (independentEnabled) {
            int[] picked = picker.get();
            selected = new PlayerContainerSize(picked[0], picked[1]);
        } else {
            selected = publicSize;
        }
        sizes.put(playerId, selected);
        return selected;
    }

    public static PlayerContainerSize recoverPlayerSize(int oldSlots, int highestSlot,
                                                         int preferredRows, int preferredColumns) {
        int required = Math.max(oldSlots, highestSlot + 1);
        if (LootrBiggerChestConfig.isValidSize(preferredRows, preferredColumns)
                && preferredRows * preferredColumns >= required) {
            return new PlayerContainerSize(preferredRows, preferredColumns);
        }
        int[] recovered = recoverySizeFor(required);
        return new PlayerContainerSize(recovered[0], recovered[1]);
    }

    public static PlayerContainerSize recoverExistingPlayerSize(
            ILootrInfo info, ContainerType type, NonNullList<ItemStack> items) {
        CompoundTag data = persistentData(info);
        if (data == null) {
            throw new IllegalStateException("Missing Lootr container during inventory migration: "
                    + info.getInfoKey());
        }
        boolean hadSize = readStoredInventorySize(data) != null;
        int[] requested = resolveOrCreateSize(data, type);
        if (!hadSize && info.getInfoContainer() instanceof BlockEntity blockEntity) {
            blockEntity.setChanged();
        }
        return recoverPlayerSize(Math.max(items.size(), info.getInfoContainerSize()),
                highestOccupiedSlot(items) - 1, requested[0], requested[1]);
    }

    public static int decodeSlot(CompoundTag itemTag) {
        Tag slotTag = itemTag.get(SLOT_KEY);
        if (slotTag instanceof ByteTag byteTag) return byteTag.getAsByte() & 255;
        if (slotTag instanceof IntTag intTag) return intTag.getAsInt();
        return -1;
    }

    public static boolean hasSerializedItems(CompoundTag tag) {
        return tag.contains(ITEMS_KEY, Tag.TAG_LIST);
    }

    public static boolean hasHighIntSlotItems(CompoundTag tag) {
        ListTag serialized = tag.getList(ITEMS_KEY, Tag.TAG_COMPOUND);
        for (int index = 0; index < serialized.size(); index++) {
            Tag slot = serialized.getCompound(index).get(SLOT_KEY);
            if (slot instanceof IntTag intTag && intTag.getAsInt() >= 256) return true;
        }
        return false;
    }

    public static int readLegacyIntInventorySize(CompoundTag tag, int existingSlots) {
        if (!tag.contains(INT_INVENTORY_SIZE_KEY, Tag.TAG_INT)) return -1;
        int size = tag.getInt(INT_INVENTORY_SIZE_KEY);
        return size > 0 && size <= Math.max(existingSlots, MAX_SLOTS) ? size : -1;
    }

    public static boolean itemsNeedNormalization(CompoundTag tag,
                                                   NonNullList<ItemStack> loadedItems,
                                                   int maximumSlots, HolderLookup.Provider provider) {
        CompoundTag canonical = saveAllItemsWithIntSlots(
                new CompoundTag(), loadedItems, maximumSlots, provider);
        return !Objects.equals(tag.get(ITEMS_KEY), canonical.get(ITEMS_KEY));
    }

    public static CompoundTag saveAllItemsWithIntSlots(CompoundTag tag,
                                                        NonNullList<ItemStack> items,
                                                        int maximumSlots, HolderLookup.Provider provider) {
        ListTag serialized = new ListTag();
        for (int slot = 0; slot < Math.min(items.size(), maximumSlots); slot++) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty()) continue;
            CompoundTag itemTag = (CompoundTag) stack.save(provider);
            itemTag.putInt(SLOT_KEY, slot);
            serialized.add(itemTag);
        }
        tag.put(ITEMS_KEY, serialized);
        return tag;
    }

    public static void loadAllItemsWithIntSlots(CompoundTag tag,
                                                 NonNullList<ItemStack> items,
                                                 String owner, int maximumSlots,
                                                 HolderLookup.Provider provider) {
        for (int slot = 0; slot < items.size(); slot++) items.set(slot, ItemStack.EMPTY);
        boolean[] populated = new boolean[Math.min(items.size(), maximumSlots)];
        ListTag serialized = tag.getList(ITEMS_KEY, Tag.TAG_COMPOUND);
        for (int index = 0; index < serialized.size(); index++) {
            CompoundTag itemTag = serialized.getCompound(index);
            int slot = decodeSlot(itemTag);
            if (slot < 0 || slot >= items.size() || slot >= maximumSlots) {
                LOGGER.warn("Ignoring invalid {} item slot {}", owner, slot);
                continue;
            }
            try {
                ItemStack stack = ItemStack.parseOptional(provider, itemTag);
                if (!stack.isEmpty() && !populated[slot]) {
                    items.set(slot, stack);
                    populated[slot] = true;
                }
            } catch (RuntimeException exception) {
                LOGGER.warn("Ignoring malformed {} item in slot {}: {}",
                        owner, slot, exception.getMessage());
            }
        }
    }

    public static int requiredSlotsFromItems(CompoundTag tag, String owner, int maximumSlots,
                                             HolderLookup.Provider provider) {
        if (!hasSerializedItems(tag)) return 0;
        int requiredSlots = 0;
        ListTag serialized = tag.getList(ITEMS_KEY, Tag.TAG_COMPOUND);
        for (int index = 0; index < serialized.size(); index++) {
            CompoundTag itemTag = serialized.getCompound(index);
            int slot = decodeSlot(itemTag);
            if (slot < 0 || slot >= maximumSlots) {
                LOGGER.warn("Ignoring invalid {} item slot {}", owner, slot);
                continue;
            }
            try {
                if (!ItemStack.parseOptional(provider, itemTag).isEmpty()) {
                    requiredSlots = Math.max(requiredSlots, slot + 1);
                }
            } catch (RuntimeException exception) {
                LOGGER.warn("Ignoring malformed {} item in slot {}: {}",
                        owner, slot, exception.getMessage());
            }
        }
        return requiredSlots;
    }

    public static int highestOccupiedSlot(NonNullList<ItemStack> items) {
        for (int slot = Math.min(items.size(), MAX_SLOTS) - 1; slot >= 0; slot--) {
            if (!items.get(slot).isEmpty()) return slot + 1;
        }
        return 0;
    }

    public static int[] resolveOrCreateSize(CompoundTag data, ContainerType type,
                                            Supplier<int[]> fallback) {
        boolean hasRows = data.contains(ROWS_KEY);
        boolean hasCols = data.contains(COLS_KEY);
        if (hasRows && hasCols) {
            int rows = data.getInt(ROWS_KEY);
            int cols = data.getInt(COLS_KEY);
            if (LootrBiggerChestConfig.isValidSize(rows, cols)) return new int[]{rows, cols};
            LOGGER.warn("Repairing invalid persisted {} size {}x{}", type, rows, cols);
        } else if (hasRows || hasCols) {
            LOGGER.warn("Repairing partial persisted {} size: rowsPresent={}, colsPresent={}",
                    type, hasRows, hasCols);
        }
        int[] candidate = fallback.get();
        int[] size = LootrBiggerChestConfig.isValidSize(candidate[0], candidate[1])
                ? candidate : LootrBiggerChestConfig.getFixedSize(type);
        data.putInt(ROWS_KEY, size[0]);
        data.putInt(COLS_KEY, size[1]);
        return new int[]{size[0], size[1]};
    }

    public static int[] resolveOrCreateSize(CompoundTag data, ContainerType type) {
        return resolveOrCreateSize(data, type, () -> LootrBiggerChestConfig.pickSize(type));
    }

    public static boolean shouldManage(CompoundTag data, ContainerType type) {
        return data.contains(ROWS_KEY) || data.contains(COLS_KEY)
                || LootrBiggerChestConfig.isExpanded(type);
    }

    public static NonNullList<ItemStack> resizeItemsSafely(NonNullList<ItemStack> source,
                                                            int targetSize, String owner) {
        if (source.size() == targetSize) return source;
        if (targetSize < source.size()) {
            for (int index = targetSize; index < source.size(); index++) {
                if (!source.get(index).isEmpty()) {
                    LOGGER.warn("Refusing to shrink {} from {} to {} because slot {} is occupied",
                            owner, source.size(), targetSize, index);
                    return source;
                }
            }
        }
        NonNullList<ItemStack> resized = NonNullList.withSize(targetSize, ItemStack.EMPTY);
        for (int index = 0; index < Math.min(source.size(), targetSize); index++) {
            resized.set(index, source.get(index));
        }
        return resized;
    }

    public record ResizedContainer(NonNullList<ItemStack> items, int rows, int columns) {}

    public static ResizedContainer reconcilePlayerMenuSize(PlayerContainerSize requested,
                                                             NonNullList<ItemStack> source,
                                                             String owner) {
        PlayerContainerSize safeSize = ensurePlayerSizeContainsItems(
                requested, highestOccupiedSlot(source));
        NonNullList<ItemStack> resized = resizeItemsSafely(source, safeSize.slots(), owner);
        if (resized.size() != safeSize.slots()) {
            throw new IllegalStateException("Cannot safely resize " + owner + " from "
                    + source.size() + " to " + safeSize.slots());
        }
        return new ResizedContainer(resized, safeSize.rows(), safeSize.columns());
    }

    public static int[] recoverySizeFor(int requiredSlots) {
        int maxSlots = LootrBiggerChestConfig.MAX_ROWS * LootrBiggerChestConfig.MAX_COLUMNS;
        if (requiredSlots > maxSlots) {
            throw new IllegalStateException("Cannot recover Lootr container with "
                    + requiredSlots + " slots; maximum is " + maxSlots);
        }
        int targetSlots = Math.max(1, requiredSlots);
        if (targetSlots == 27) return new int[]{3, 9};
        int bestRows = LootrBiggerChestConfig.MAX_ROWS;
        int bestColumns = LootrBiggerChestConfig.MAX_COLUMNS;
        int bestSlots = maxSlots + 1;
        for (int rows = 1; rows <= LootrBiggerChestConfig.MAX_ROWS; rows++) {
            for (int columns = 1; columns <= LootrBiggerChestConfig.MAX_COLUMNS; columns++) {
                int slots = rows * columns;
                if (slots == targetSlots) return new int[]{rows, columns};
                if (slots > targetSlots && slots < bestSlots) {
                    bestRows = rows;
                    bestColumns = columns;
                    bestSlots = slots;
                }
            }
        }
        return new int[]{bestRows, bestColumns};
    }

    public static void saveSizeToTag(CompoundTag persistentData, CompoundTag saveTag) {
        if (!persistentData.contains(ROWS_KEY) || !persistentData.contains(COLS_KEY)) return;
        int rows = persistentData.getInt(ROWS_KEY);
        int cols = persistentData.getInt(COLS_KEY);
        if (!LootrBiggerChestConfig.isValidSize(rows, cols)) return;
        saveTag.putInt(ROWS_KEY, rows);
        saveTag.putInt(COLS_KEY, cols);
    }

    public static void loadSizeFromTag(CompoundTag saveTag, CompoundTag persistentData) {
        if (!saveTag.contains(ROWS_KEY) && !saveTag.contains(COLS_KEY)) return;
        persistentData.remove(ROWS_KEY);
        persistentData.remove(COLS_KEY);
        if (saveTag.contains(ROWS_KEY)) persistentData.putInt(ROWS_KEY, saveTag.getInt(ROWS_KEY));
        if (saveTag.contains(COLS_KEY)) persistentData.putInt(COLS_KEY, saveTag.getInt(COLS_KEY));
    }

}
