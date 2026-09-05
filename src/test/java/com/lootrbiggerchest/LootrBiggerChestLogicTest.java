package com.lootrbiggerchest;

import com.lootrbiggerchest.menu.LootrBiggerChestMenu.ContainerType;
import com.lootrbiggerchest.menu.LootrBiggerChestMenu;
import net.minecraft.SharedConstants;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LootrBiggerChestLogicTest {

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void playerSpecificSizeConfigurationDefaultsToDisabled() throws Exception {
        String config = Files.readString(Path.of(
                "src/main/java/com/lootrbiggerchest/LootrBiggerChestConfig.java"));
        assertTrue(config.contains(".define(\"\u73a9\u5bb6\u72ec\u7acb\u5bb9\u5668\u5927\u5c0f\", false)"));
    }

    @Test
    void playerContainerSizesRoundTripAndRejectInvalidEntries() {
        UUID first = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID second = UUID.fromString("00000000-0000-0000-0000-000000000002");
        Map<UUID, LootrBiggerChest.PlayerContainerSize> sizes = new HashMap<>();
        sizes.put(second, new LootrBiggerChest.PlayerContainerSize(2, 5));
        sizes.put(first, new LootrBiggerChest.PlayerContainerSize(20, 32));

        CompoundTag root = new CompoundTag();
        LootrBiggerChest.writePlayerSizes(root, sizes);
        ListTag serialized = root.getList(LootrBiggerChest.PLAYER_SIZES_KEY, CompoundTag.TAG_COMPOUND);
        CompoundTag missingUuid = new CompoundTag();
        missingUuid.putInt(LootrBiggerChest.ROWS_KEY, 3);
        missingUuid.putInt(LootrBiggerChest.COLS_KEY, 9);
        serialized.add(missingUuid);
        CompoundTag invalidSize = new CompoundTag();
        invalidSize.putUUID(LootrBiggerChest.PLAYER_KEY, UUID.randomUUID());
        invalidSize.putInt(LootrBiggerChest.ROWS_KEY, 21);
        invalidSize.putInt(LootrBiggerChest.COLS_KEY, 9);
        serialized.add(invalidSize);

        Map<UUID, LootrBiggerChest.PlayerContainerSize> loaded =
                LootrBiggerChest.readPlayerSizes(root);

        assertEquals(first, serialized.getCompound(0).getUUID(LootrBiggerChest.PLAYER_KEY));
        assertEquals(second, serialized.getCompound(1).getUUID(LootrBiggerChest.PLAYER_KEY));
        assertEquals(sizes, loaded);
        assertEquals(640, loaded.get(first).slots());
    }

    @Test
    void inventoryDimensionsShareTheChestCompoundWithIntSlotItems() {
        CompoundTag chest = new CompoundTag();
        chest.putInt(LootrBiggerChest.ROWS_KEY, 2);
        chest.putInt(LootrBiggerChest.COLS_KEY, 5);

        assertEquals(new LootrBiggerChest.PlayerContainerSize(2, 5),
                LootrBiggerChest.readStoredInventorySize(chest));
    }

    @Test
    void storedInventoryDimensionsRequireTwoValidIntTags() {
        CompoundTag missing = new CompoundTag();
        missing.putInt(LootrBiggerChest.ROWS_KEY, 2);
        assertNull(LootrBiggerChest.readStoredInventorySize(missing));

        CompoundTag wrongType = new CompoundTag();
        wrongType.putString(LootrBiggerChest.ROWS_KEY, "2");
        wrongType.putInt(LootrBiggerChest.COLS_KEY, 5);
        assertNull(LootrBiggerChest.readStoredInventorySize(wrongType));

        CompoundTag invalid = new CompoundTag();
        invalid.putInt(LootrBiggerChest.ROWS_KEY, 21);
        invalid.putInt(LootrBiggerChest.COLS_KEY, 5);
        assertNull(LootrBiggerChest.readStoredInventorySize(invalid));
    }

    @Test
    void storedPlayerSizeGrowsOnlyWhenOccupiedItemsRequireIt() {
        LootrBiggerChest.PlayerContainerSize stored =
                new LootrBiggerChest.PlayerContainerSize(2, 5);

        assertEquals(stored, LootrBiggerChest.ensurePlayerSizeContainsItems(stored, 10));
        assertEquals(new LootrBiggerChest.PlayerContainerSize(3, 9),
                LootrBiggerChest.ensurePlayerSizeContainsItems(stored, 27));
    }

    @Test
    void validInventorySizeOverridesConflictingRootSize() {
        LootrBiggerChest.PlayerContainerSize root =
                new LootrBiggerChest.PlayerContainerSize(20, 32);
        LootrBiggerChest.PlayerContainerSize inventory =
                new LootrBiggerChest.PlayerContainerSize(2, 5);

        assertEquals(inventory, LootrBiggerChest.reconcileExistingPlayerSize(
                root, inventory, 9));
    }

    @Test
    void recoveryUsesExactThenMinimalDeterministicLayouts() {
        assertArrayEquals(new int[]{1, 10}, LootrBiggerChest.recoverySizeFor(10));
        assertArrayEquals(new int[]{1, 32}, LootrBiggerChest.recoverySizeFor(32));
        assertArrayEquals(new int[]{16, 32}, LootrBiggerChest.recoverySizeFor(512));
        assertArrayEquals(new int[]{3, 11}, LootrBiggerChest.recoverySizeFor(33));
        assertArrayEquals(new int[]{2, 19}, LootrBiggerChest.recoverySizeFor(37));
    }

    @Test
    void firstValidDuplicateItemSlotWins() {
        CompoundTag first = new ItemStack(net.minecraft.world.item.Items.STONE)
                .save(new CompoundTag());
        first.putInt(LootrBiggerChest.SLOT_KEY, 3);
        CompoundTag later = new ItemStack(net.minecraft.world.item.Items.DIRT)
                .save(new CompoundTag());
        later.putInt(LootrBiggerChest.SLOT_KEY, 3);
        CompoundTag empty = new CompoundTag();
        empty.putInt(LootrBiggerChest.SLOT_KEY, 4);
        CompoundTag validAfterEmpty = new ItemStack(net.minecraft.world.item.Items.STONE)
                .save(new CompoundTag());
        validAfterEmpty.putInt(LootrBiggerChest.SLOT_KEY, 4);
        ListTag serialized = new ListTag();
        serialized.add(first);
        serialized.add(later);
        serialized.add(empty);
        serialized.add(validAfterEmpty);
        CompoundTag tag = new CompoundTag();
        tag.put(LootrBiggerChest.ITEMS_KEY, serialized);
        NonNullList<ItemStack> loaded = NonNullList.withSize(5, ItemStack.EMPTY);

        LootrBiggerChest.loadAllItemsWithIntSlots(tag, loaded, "test");

        assertTrue(loaded.get(3).is(net.minecraft.world.item.Items.STONE));
        assertTrue(loaded.get(4).is(net.minecraft.world.item.Items.STONE));
    }

    @Test
    void playerInventoryPersistenceSupportsBothLootrNbtConstructors() throws Exception {
        String chestData = Files.readString(Path.of(
                "src/main/java/com/lootrbiggerchest/mixin/ChestDataMixin.java"));
        String inventory = Files.readString(Path.of(
                "src/main/java/com/lootrbiggerchest/mixin/SpecialChestInventoryMixin.java"));

        assertTrue(inventory.contains("@Group(name = \"lootr_nbt_constructor\", min = 1)"));
        assertTrue(inventory.contains("<init>(Lnoobanidus/mods/lootr/data/ChestData;"
                + "Lnet/minecraft/nbt/CompoundTag;Ljava/lang/String;)V"));
        assertTrue(inventory.contains("<init>(Lnoobanidus/mods/lootr/data/ChestData;I"
                + "Lnet/minecraft/nbt/CompoundTag;Ljava/lang/String;)V"));
        assertTrue(inventory.contains("lootrbiggerchest$restoreLoadedInventory(tag, size)"));
        assertTrue(chestData.contains("method = \"getInventory(Lnet/minecraft/server/level/"
                + "ServerPlayer;)Lnoobanidus/mods/lootr/data/SpecialChestInventory;\""));
        assertTrue(chestData.contains("lootrbiggerchest$loadingSizes"));
        assertFalse(chestData.contains("inventory.getTile("));
        assertFalse(chestData.contains("inventory.getBlockEntity("));
        assertFalse(inventory.contains("self.getTile("));
        assertFalse(inventory.contains("self.getBlockEntity("));
        String setter = inventory.substring(inventory.indexOf("lootrbiggerchest$setStoredSize"),
                inventory.indexOf("afterNbtConstructor"));
        assertTrue(setter.contains("resizeItemsSafely"));
        assertTrue(setter.indexOf("resizeItemsSafely") < setter.indexOf("storedSize = size"));
        assertTrue(setter.contains("resized.size() != size.slots()"));
        assertFalse(inventory.contains("newChestData.getSize()"));
        assertTrue(chestData.contains("public int lootrbiggerchest$getSize()"));
        assertTrue(chestData.contains("public void lootrbiggerchest$growSize(int requestedSize)"));
        assertFalse(chestData.contains("@Invoker"));
        assertTrue(chestData.contains("method = \"setSize(I)V\""));
        assertTrue(chestData.contains("require = 0"));
        assertTrue(chestData.contains("lootrbiggerchest$replacePlayerSize"));
        assertTrue(chestData.contains("reconcileExistingPlayerSize"));
    }

    @Test
    void specialChestInventoryShadowFieldUsesFinalAnnotationWithoutNullInitializer() throws Exception {
        String inventory = Files.readString(Path.of(
                "src/main/java/com/lootrbiggerchest/mixin/SpecialChestInventoryMixin.java"));
        String expectedShadowBlock = "@Shadow(remap = false)\n    @Final\n    private ChestData newChestData;";

        assertTrue(inventory.contains("import org.spongepowered.asm.mixin.Final;"));
        assertTrue(inventory.contains(expectedShadowBlock));
        assertFalse(inventory.contains("private ChestData newChestData = null;"));
    }

    @Test
    void productionSourcesExcludeDetailedDiagnosticsButKeepErrors() throws Exception {
        List<String> sources = List.of(
                Files.readString(Path.of(
                        "src/main/java/com/lootrbiggerchest/mixin/SpecialChestInventoryMixin.java")),
                Files.readString(Path.of(
                        "src/main/java/com/lootrbiggerchest/menu/LootrBiggerChestMenu.java")),
                Files.readString(Path.of(
                        "src/main/java/com/lootrbiggerchest/client/LootrBiggerChestScreen.java")));
        String combined = String.join("\n", sources);

        assertFalse(combined.contains("LOGGER.debug"));
        for (String event : List.of(
                "lootr_restore_",
                "lootr_menu_prepare",
                "lootr_size_reuse",
                "lootr_size_resolved",
                "lootr_size_persisted",
                "lootr_menu_open",
                "lootr_menu_received",
                "lootr_client_menu_created",
                "lootr_screen_created")) {
            assertFalse(combined.contains(event),
                    "Production source contains diagnostic event " + event);
        }
        assertTrue(sources.get(0).contains("LootrBiggerChest.LOGGER.error"));
        assertTrue(sources.get(0).contains("lootr_size_conflict"));
        assertTrue(sources.get(1).contains("LootrBiggerChest.LOGGER.error"));
    }

    @Test
    void existingPlayerSizeWinsRegardlessOfCurrentToggle() {
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000010");
        Map<UUID, LootrBiggerChest.PlayerContainerSize> sizes = new HashMap<>();
        sizes.put(player, new LootrBiggerChest.PlayerContainerSize(4, 8));

        assertEquals(new LootrBiggerChest.PlayerContainerSize(4, 8),
                LootrBiggerChest.choosePlayerSize(sizes, player, false,
                        new LootrBiggerChest.PlayerContainerSize(3, 9),
                        () -> new int[]{20, 32}));
        assertEquals(1, sizes.size());
    }

    @Test
    void newPlayerUsesPickerAndStoresSizeWhenIndependentModeIsEnabled() {
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000011");
        Map<UUID, LootrBiggerChest.PlayerContainerSize> sizes = new HashMap<>();

        LootrBiggerChest.PlayerContainerSize selected = LootrBiggerChest.choosePlayerSize(
                sizes, player, true, new LootrBiggerChest.PlayerContainerSize(3, 9),
                () -> new int[]{20, 32});

        assertEquals(new LootrBiggerChest.PlayerContainerSize(20, 32), selected);
        assertEquals(selected, sizes.get(player));
    }

    @Test
    void playersAndContainersResolveIndependently() {
        UUID firstPlayer = UUID.fromString("00000000-0000-0000-0000-000000000021");
        UUID secondPlayer = UUID.fromString("00000000-0000-0000-0000-000000000022");
        Map<UUID, LootrBiggerChest.PlayerContainerSize> firstContainer = new HashMap<>();
        Map<UUID, LootrBiggerChest.PlayerContainerSize> secondContainer = new HashMap<>();

        assertEquals(new LootrBiggerChest.PlayerContainerSize(2, 5),
                LootrBiggerChest.choosePlayerSize(firstContainer, firstPlayer, true,
                        new LootrBiggerChest.PlayerContainerSize(3, 9), () -> new int[]{2, 5}));
        assertEquals(new LootrBiggerChest.PlayerContainerSize(4, 8),
                LootrBiggerChest.choosePlayerSize(firstContainer, secondPlayer, true,
                        new LootrBiggerChest.PlayerContainerSize(3, 9), () -> new int[]{4, 8}));
        assertEquals(new LootrBiggerChest.PlayerContainerSize(1, 32),
                LootrBiggerChest.choosePlayerSize(secondContainer, firstPlayer, true,
                        new LootrBiggerChest.PlayerContainerSize(3, 9), () -> new int[]{1, 32}));

        assertEquals(2, firstContainer.size());
        assertEquals(1, secondContainer.size());
    }

    @Test
    void playerCreationAllocationIsStrictAndClearsStaleBindings() throws Exception {
        assertNull(LootrBiggerChest.containerType(null));
        assertNull(LootrBiggerChest.containerType(new Object()));

        String core = Files.readString(Path.of(
                "src/main/java/com/lootrbiggerchest/LootrBiggerChest.java"));
        String chestData = Files.readString(Path.of(
                "src/main/java/com/lootrbiggerchest/mixin/ChestDataMixin.java"));
        String special = Files.readString(Path.of(
                "src/main/java/com/lootrbiggerchest/mixin/SpecialChestInventoryMixin.java"));
        String randomizableDescriptor = "createInventory(Lnet/minecraft/server/level/ServerPlayer;"
                + "Lnoobanidus/mods/lootr/api/LootFiller;"
                + "Lnet/minecraft/world/level/block/entity/RandomizableContainerBlockEntity;)"
                + "Lnoobanidus/mods/lootr/data/SpecialChestInventory;";
        String baseDescriptor = "createInventory(Lnet/minecraft/server/level/ServerPlayer;"
                + "Lnoobanidus/mods/lootr/api/LootFiller;"
                + "Lnet/minecraft/world/level/block/entity/BaseContainerBlockEntity;"
                + "Ljava/util/function/Supplier;Ljava/util/function/LongSupplier;)"
                + "Lnoobanidus/mods/lootr/data/SpecialChestInventory;";

        assertTrue(core.contains("interface SizedInventoryAccess"));
        assertTrue(core.contains("instanceof LootrBarrelBlockEntity"));
        assertTrue(core.contains("instanceof LootrShulkerBlockEntity"));
        assertTrue(core.contains("instanceof LootrChestBlockEntity"));
        assertTrue(core.contains("instanceof LootrChestMinecartEntity"));
        assertTrue(core.contains("return null;"));
        assertEquals(2, chestData.split("@Redirect\\(", -1).length - 1);
        assertTrue(chestData.contains(randomizableDescriptor));
        assertTrue(chestData.contains(baseDescriptor));
        String remappedAllocation = "at = @At(value = \"INVOKE\", target = "
                + "\"Lnet/minecraft/core/NonNullList;withSize(ILjava/lang/Object;)"
                + "Lnet/minecraft/core/NonNullList;\", remap = true)";
        assertEquals(2, chestData.split(java.util.regex.Pattern.quote(
                remappedAllocation), -1).length - 1);
        assertFalse(chestData.contains("IntSupplier"));
        assertTrue(chestData.contains("if (custom || type == null)"));
        assertTrue(chestData.contains("lootrbiggerchest$clearPendingCreation(player);"));
        assertTrue(chestData.contains("lootrbiggerchest$takePendingSize(player,"));
        assertTrue(chestData.contains("if (size == null || type == null"
                + " || pendingSource != source)"));
        assertTrue(chestData.contains("private UUID uuid;"));
        assertTrue(chestData.contains("((ServerLevel) player.level()).getEntity(uuid)"));
        assertTrue(chestData.contains("instanceof LootrChestMinecartEntity cart"));
        assertTrue(chestData.contains("resolveOrCreateSize(cart.getPersistentData(), type)"));
        assertFalse(chestData.contains("LootrBiggerChestConfig.getFixedSize(type)"));
        assertTrue(special.contains("implements LootrBiggerChest.AuthoritativeMenuProvider,"));
        assertTrue(special.contains("LootrBiggerChest.SizedInventoryAccess"));
        assertTrue(special.contains("lootrbiggerchest$storedSize"));
    }

    @Test
    void newPlayerUsesPublicSizeAndStoresItWhenIndependentModeIsDisabled() {
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000012");
        Map<UUID, LootrBiggerChest.PlayerContainerSize> sizes = new HashMap<>();
        LootrBiggerChest.PlayerContainerSize publicSize =
                new LootrBiggerChest.PlayerContainerSize(3, 9);

        LootrBiggerChest.PlayerContainerSize selected = LootrBiggerChest.choosePlayerSize(
                sizes, player, false, publicSize, () -> new int[]{20, 32});

        assertEquals(publicSize, selected);
        assertEquals(selected, sizes.get(player));
    }

    @Test
    void chestDataPersistsPlayerSizesWithoutClearingThemOnRefresh() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/lootrbiggerchest/mixin/ChestDataMixin.java"));
        assertTrue(source.contains("implements LootrBiggerChest.ChestDataAccess, LootrBiggerChest.PlayerSizeAccess"));
        assertTrue(source.contains("method = \"load(Lnet/minecraft/nbt/CompoundTag;)Lnoobanidus/mods/lootr/data/ChestData;\""));
        assertTrue(source.contains("method = \"save(Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/nbt/CompoundTag;\""));
        assertFalse(source.contains("method = \"clear\""));
    }

    @Test
    void recoveryPreservesExactLegacyShapeWhenPossible() {
        assertEquals(new LootrBiggerChest.PlayerContainerSize(2, 5),
                LootrBiggerChest.recoverPlayerSize(10, 9, 2, 5));
        assertTrue(LootrBiggerChest.recoverPlayerSize(512, 511, 1, 1).slots() >= 512);
    }

    @Test
    void validatesPublishedDimensionRange() {
        assertTrue(LootrBiggerChestConfig.isValidSize(1, 1));
        assertTrue(LootrBiggerChestConfig.isValidSize(20, 32));
        assertFalse(LootrBiggerChestConfig.isValidSize(0, 9));
        assertFalse(LootrBiggerChestConfig.isValidSize(21, 9));
        assertFalse(LootrBiggerChestConfig.isValidSize(3, 33));
    }

    @Test
    void ignoresInvalidPoolEntriesAndUsesValidEntry() {
        int[] size = LootrBiggerChestConfig.pickFromPool(
                List.of("0,9,10", "20,32,5", "3,9,0", "bad"),
                3, 9, new Random(0), ContainerType.CHEST);
        assertArrayEquals(new int[]{20, 32}, size);
    }

    @Test
    void eachContainerTypeKeepsItsOwnConfiguredPicker() throws Exception {
        assertArrayEquals(new int[]{1, 1}, LootrBiggerChestConfig.pickFromPool(
                List.of("1,1,1"), 3, 9, new Random(0), ContainerType.CHEST));
        assertArrayEquals(new int[]{2, 5}, LootrBiggerChestConfig.pickFromPool(
                List.of("2,5,1"), 3, 9, new Random(0), ContainerType.BARREL));
        assertArrayEquals(new int[]{20, 32}, LootrBiggerChestConfig.pickFromPool(
                List.of("20,32,1"), 3, 9, new Random(0), ContainerType.SHULKER));
        assertArrayEquals(new int[]{4, 8}, LootrBiggerChestConfig.pickFromPool(
                List.of("4,8,1"), 3, 9, new Random(0), ContainerType.MINECART));

        String config = Files.readString(Path.of(
                "src/main/java/com/lootrbiggerchest/LootrBiggerChestConfig.java"));
        String getPool = config.substring(config.indexOf("getPool(ContainerType type)"),
                config.indexOf("public static boolean isExpanded(ContainerType type)"));
        assertTrue(getPool.contains("case CHEST -> COMMON.chestRandomSizes.get();"));
        assertTrue(getPool.contains("case BARREL -> COMMON.barrelRandomSizes.get();"));
        assertTrue(getPool.contains("case SHULKER -> COMMON.shulkerRandomSizes.get();"));
        assertTrue(getPool.contains("case MINECART -> COMMON.minecartRandomSizes.get();"));
    }

    @Test
    void disablingAndReenablingPreservesRecordedSizesAndOnlyPicksForUnknownPlayers() {
        UUID known = UUID.fromString("00000000-0000-0000-0000-000000000031");
        UUID disabledNewcomer = UUID.fromString("00000000-0000-0000-0000-000000000032");
        UUID enabledNewcomer = UUID.fromString("00000000-0000-0000-0000-000000000033");
        Map<UUID, LootrBiggerChest.PlayerContainerSize> sizes = new HashMap<>();
        sizes.put(known, new LootrBiggerChest.PlayerContainerSize(20, 32));
        int[] pickerCalls = {0};

        LootrBiggerChest.PlayerContainerSize knownWhileDisabled =
                LootrBiggerChest.choosePlayerSize(sizes, known, false,
                        new LootrBiggerChest.PlayerContainerSize(3, 9), () -> {
                            pickerCalls[0]++;
                            return new int[]{1, 1};
                        });
        LootrBiggerChest.PlayerContainerSize newcomerWhileDisabled =
                LootrBiggerChest.choosePlayerSize(sizes, disabledNewcomer, false,
                        new LootrBiggerChest.PlayerContainerSize(3, 9), () -> {
                            pickerCalls[0]++;
                            return new int[]{1, 1};
                        });

        assertEquals(new LootrBiggerChest.PlayerContainerSize(20, 32), knownWhileDisabled);
        assertEquals(new LootrBiggerChest.PlayerContainerSize(3, 9), newcomerWhileDisabled);
        assertEquals(0, pickerCalls[0]);

        assertEquals(knownWhileDisabled, LootrBiggerChest.choosePlayerSize(
                sizes, known, true, new LootrBiggerChest.PlayerContainerSize(1, 1), () -> {
                    pickerCalls[0]++;
                    return new int[]{2, 5};
                }));
        assertEquals(new LootrBiggerChest.PlayerContainerSize(3, 9),
                LootrBiggerChest.choosePlayerSize(
                        sizes, disabledNewcomer, true,
                        new LootrBiggerChest.PlayerContainerSize(1, 1), () -> {
                            pickerCalls[0]++;
                            return new int[]{2, 5};
                        }));
        assertEquals(0, pickerCalls[0]);
        LootrBiggerChest.PlayerContainerSize selectedAfterReenable =
                LootrBiggerChest.choosePlayerSize(sizes, enabledNewcomer, true,
                        new LootrBiggerChest.PlayerContainerSize(1, 1), () -> {
                            pickerCalls[0]++;
                            return new int[]{4, 8};
                        });

        assertEquals(new LootrBiggerChest.PlayerContainerSize(4, 8), selectedAfterReenable);
        assertEquals(selectedAfterReenable, sizes.get(enabledNewcomer));
        assertEquals(1, pickerCalls[0]);
    }

    @Test
    void playerSizeRootRecordSurvivesInventoryRefresh() {
        UUID first = UUID.fromString("00000000-0000-0000-0000-000000000040");
        UUID second = UUID.fromString("00000000-0000-0000-0000-000000000041");
        Map<UUID, LootrBiggerChest.PlayerContainerSize> sizes = new HashMap<>();
        sizes.put(first, new LootrBiggerChest.PlayerContainerSize(4, 8));
        sizes.put(second, new LootrBiggerChest.PlayerContainerSize(20, 32));
        CompoundTag root = new CompoundTag();
        LootrBiggerChest.writePlayerSizes(root, sizes);

        root.put("inventories", new ListTag());

        assertEquals(sizes, LootrBiggerChest.readPlayerSizes(root));
    }

    @Test
    void playerSpecificSizeIntegrationMatrixRemainsWired() throws Exception {
        String core = Files.readString(Path.of(
                "src/main/java/com/lootrbiggerchest/LootrBiggerChest.java"));
        String chestData = Files.readString(Path.of(
                "src/main/java/com/lootrbiggerchest/mixin/ChestDataMixin.java"));
        String special = Files.readString(Path.of(
                "src/main/java/com/lootrbiggerchest/mixin/SpecialChestInventoryMixin.java"));
        String tests = Files.readString(Path.of(
                "src/test/java/com/lootrbiggerchest/LootrBiggerChestLogicTest.java"));
        String randomizableDescriptor = "createInventory(Lnet/minecraft/server/level/ServerPlayer;"
                + "Lnoobanidus/mods/lootr/api/LootFiller;"
                + "Lnet/minecraft/world/level/block/entity/RandomizableContainerBlockEntity;)"
                + "Lnoobanidus/mods/lootr/data/SpecialChestInventory;";
        String baseDescriptor = "createInventory(Lnet/minecraft/server/level/ServerPlayer;"
                + "Lnoobanidus/mods/lootr/api/LootFiller;"
                + "Lnet/minecraft/world/level/block/entity/BaseContainerBlockEntity;"
                + "Ljava/util/function/Supplier;Ljava/util/function/LongSupplier;)"
                + "Lnoobanidus/mods/lootr/data/SpecialChestInventory;";
        int firstRedirect = chestData.indexOf("@Redirect(");
        int secondRedirect = chestData.indexOf("@Redirect(", firstRedirect + 1);
        assertTrue(firstRedirect >= 0 && secondRedirect > firstRedirect);
        String randomizableRedirect = chestData.substring(firstRedirect,
                chestData.indexOf("@Inject(", firstRedirect));
        String baseRedirect = chestData.substring(secondRedirect,
                chestData.indexOf("@Inject(", secondRedirect));

        assertEquals(-1, chestData.indexOf("@Redirect(", secondRedirect + 1));
        assertTrue(randomizableRedirect.contains("method = \"" + randomizableDescriptor + "\""));
        assertTrue(baseRedirect.contains("method = \"" + baseDescriptor + "\""));
        assertTrue(randomizableRedirect.contains("NonNullList;withSize(ILjava/lang/Object;)"));
        assertTrue(baseRedirect.contains("NonNullList;withSize(ILjava/lang/Object;)"));
        assertEquals(2, chestData.split(
                "NonNullList\\.withSize\\(selected\\.slots\\(\\)", -1).length - 1);
        assertTrue(core.contains("instanceof LootrChestBlockEntity) return ContainerType.CHEST"));
        assertTrue(core.contains("instanceof LootrBarrelBlockEntity) return ContainerType.BARREL"));
        assertTrue(core.contains("instanceof LootrShulkerBlockEntity) return ContainerType.SHULKER"));
        assertTrue(core.contains("instanceof LootrChestMinecartEntity) return ContainerType.MINECART"));
        assertTrue(core.contains("public static final String PLAYER_SIZES_KEY = \"LBCPlayerSizes\""));
        assertTrue(chestData.contains("lootrbiggerchest$loadPlayerSizes(root)"));
        assertTrue(chestData.contains("lootrbiggerchest$savePlayerSizes(cir.getReturnValue())"));
        assertTrue(special.contains("result.putInt(LootrBiggerChest.ROWS_KEY"));
        assertTrue(special.contains("result.putInt(LootrBiggerChest.COLS_KEY"));

        String prepare = special.substring(special.indexOf("lootrbiggerchest$prepareMenu"),
                special.indexOf("writeIntSlotItems"));
        int stored = prepare.indexOf("if (lootrbiggerchest$storedSize != null)");
        int physical = prepare.indexOf("resolveOrCreateSize");
        assertTrue(stored >= 0 && (physical < 0 || stored < physical));
        assertTrue(prepare.contains("lootrbiggerchest$isCustom()) return null"));
        assertEquals(2, chestData.split("if \\(custom \\|\\| type == null\\)", -1).length - 1);
        assertTrue(randomizableRedirect.contains("if (custom || type == null)"));
        assertTrue(baseRedirect.contains("if (custom || type == null)"));

        assertTrue(tests.contains("void roundTripsEveryByteBoundaryWithIntSlots()"));
        assertTrue(tests.contains("assertInstanceOf(IntTag.class"));
        assertTrue(tests.contains("void capsRecoveredLayoutAtMaximumLegalCapacity()"));
        assertTrue(tests.contains("void allPublishedSizesHaveSafeSlotCounts()"));
        assertTrue(tests.contains("assertEquals(640, Math.multiplyExact(20, 32))"));
    }

    @Test
    void rejectsEntirePoolWhenWeightTotalOverflowsLong() {
        int[] size = LootrBiggerChestConfig.pickFromPool(
                List.of("20,32," + Long.MAX_VALUE, "1,1," + Long.MAX_VALUE),
                3, 9, new Random(0), ContainerType.CHEST);
        assertArrayEquals(new int[]{3, 9}, size);
    }

    @Test
    void roundTripsEveryByteBoundaryWithIntSlots() {
        int[] occupied = {255, 256, 511, 512, 639};
        NonNullList<ItemStack> original = NonNullList.withSize(640, ItemStack.EMPTY);
        for (int slot : occupied) original.set(slot, new ItemStack(net.minecraft.world.item.Items.STONE));

        CompoundTag tag = LootrBiggerChest.saveAllItemsWithIntSlots(new CompoundTag(), original);
        ListTag serialized = tag.getList(LootrBiggerChest.ITEMS_KEY, CompoundTag.TAG_COMPOUND);
        assertEquals(occupied.length, serialized.size());
        for (int index = 0; index < occupied.length; index++) {
            assertInstanceOf(IntTag.class, serialized.getCompound(index).get(LootrBiggerChest.SLOT_KEY));
            assertEquals(occupied[index], LootrBiggerChest.decodeSlot(serialized.getCompound(index)));
        }

        NonNullList<ItemStack> loaded = NonNullList.withSize(640, ItemStack.EMPTY);
        LootrBiggerChest.loadAllItemsWithIntSlots(tag, loaded, "test");
        for (int slot : occupied) assertTrue(loaded.get(slot).is(net.minecraft.world.item.Items.STONE));
    }

    @Test
    void readsLegacyUnsignedByteSlotAndNewIntSlot() {
        CompoundTag legacy = new CompoundTag();
        legacy.putByte(LootrBiggerChest.SLOT_KEY, (byte) 255);
        CompoundTag current = new CompoundTag();
        current.putInt(LootrBiggerChest.SLOT_KEY, 639);

        assertEquals(255, LootrBiggerChest.decodeSlot(legacy));
        assertEquals(639, LootrBiggerChest.decodeSlot(current));
    }

    @Test
    void recoversFromSerializedHighSlotAndGlobalMinimum() {
        CompoundTag item = new ItemStack(net.minecraft.world.item.Items.STONE).save(new CompoundTag());
        item.putInt(LootrBiggerChest.SLOT_KEY, 511);
        ListTag items = new ListTag();
        items.add(item);
        CompoundTag tag = new CompoundTag();
        tag.put(LootrBiggerChest.ITEMS_KEY, items);
        assertEquals(512, LootrBiggerChest.requiredSlotsFromItems(tag, "test"));

        LootrBiggerChest.ResizedContainer result = LootrBiggerChest.reconcileSizeWithItems(
                tag, new int[]{1, 1}, NonNullList.withSize(27, ItemStack.EMPTY), 640, "test");
        assertEquals(640, result.containerSize());
        assertEquals(20, result.rows());
        assertEquals(32, result.columns());
    }

    @Test
    void specialInventoryCanPreserveExactChestDataSlotCount() {
        NonNullList<ItemStack> original = NonNullList.withSize(27, ItemStack.EMPTY);
        assertEquals(28, LootrBiggerChest.resizeItemsSafely(
                original, 28, "special test").size());
    }

    @Test
    void preservesLoadedForgeDataWhenTopLevelSizeIsAbsent() {
        CompoundTag persistentData = new CompoundTag();
        persistentData.putInt(LootrBiggerChest.ROWS_KEY, 20);
        persistentData.putInt(LootrBiggerChest.COLS_KEY, 32);

        LootrBiggerChest.loadSizeFromTag(new CompoundTag(), persistentData);

        assertEquals(20, persistentData.getInt(LootrBiggerChest.ROWS_KEY));
        assertEquals(32, persistentData.getInt(LootrBiggerChest.COLS_KEY));
    }

    @Test
    void detectsFirstPersistedSizeCreation() {
        CompoundTag data = new CompoundTag();
        NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
        LootrBiggerChest.ContainerStorageState before =
                LootrBiggerChest.captureStorageState(data, items);

        LootrBiggerChest.resolveOrCreateSize(data, ContainerType.CHEST,
                () -> new int[]{3, 9});

        assertTrue(LootrBiggerChest.storageStateChanged(before, data, items));
        assertFalse(LootrBiggerChest.storageStateChanged(
                LootrBiggerChest.captureStorageState(data, items), data, items));
    }

    @Test
    void missingAuthoritativeMenuDataFailsClosed() {
        assertThrows(IllegalStateException.class, () -> LootrBiggerChestMenu.fromNetwork(
                7, new Inventory(null), null, ContainerType.CHEST));
    }

    @Test
    void specialMenuUsesStoredPlayerDimensionsBeforePhysicalContainerDimensions() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/lootrbiggerchest/mixin/SpecialChestInventoryMixin.java"));
        String prepare = source.substring(source.indexOf("lootrbiggerchest$prepareMenu"),
                source.indexOf("writeIntSlotItems"));
        int storedBranch = prepare.indexOf("if (lootrbiggerchest$storedSize != null)");
        int physical = prepare.indexOf("resolveOrCreateSize");

        assertTrue(storedBranch >= 0);
        assertTrue(physical < 0 || storedBranch < physical);
        assertTrue(prepare.contains("reconcilePlayerMenuSize"));
    }

    @Test
    void occupiedTailRecoversPlayerMenuWithoutDroppingItems() {
        NonNullList<ItemStack> contents = NonNullList.withSize(27, ItemStack.EMPTY);
        ItemStack marker = new ItemStack(net.minecraft.world.item.Items.STONE);
        contents.set(26, marker);

        LootrBiggerChest.ResizedContainer result = LootrBiggerChest.reconcilePlayerMenuSize(
                new LootrBiggerChest.PlayerContainerSize(1, 1), contents, "test");

        assertEquals(27, result.containerSize());
        assertSame(marker, result.items().get(26));
    }

    @Test
    void menuHooksUseExactRemappedDescriptorsAndStableChestDataLookup() throws Exception {
        String special = Files.readString(Path.of(
                "src/main/java/com/lootrbiggerchest/mixin/SpecialChestInventoryMixin.java"));
        String core = Files.readString(Path.of(
                "src/main/java/com/lootrbiggerchest/LootrBiggerChest.java"));
        String createMenu = "createMenu(ILnet/minecraft/world/entity/player/Inventory;"
                + "Lnet/minecraft/world/entity/player/Player;)"
                + "Lnet/minecraft/world/inventory/AbstractContainerMenu;";
        String openMenu = "openMenu(Lnet/minecraft/world/MenuProvider;)Ljava/util/OptionalInt;";

        assertTrue(special.contains("@Inject(method = \"" + createMenu + "\""));
        assertTrue(special.contains("@Inject(method = \"" + openMenu + "\""));
        assertTrue(special.contains("remap = true"));
        assertTrue(core.contains("resolveContainerType(ChestData chestData"));
        assertTrue(core.contains("chestData.getPos()"));
        assertTrue(core.contains("chestData.getEntityId()"));
        assertFalse(special.contains("self.getTile("));
        assertFalse(special.contains("self.getBlockEntity("));
    }

    @Test
    void specialMenuRefmapUsesTheExactInjectionSelector() throws Exception {
        String build = Files.readString(Path.of("build.gradle"));
        String createMenu = "createMenu(ILnet/minecraft/world/entity/player/Inventory;"
                + "Lnet/minecraft/world/entity/player/Player;)"
                + "Lnet/minecraft/world/inventory/AbstractContainerMenu;";

        assertTrue(build.contains("'" + createMenu + "': "
                + "'Lnoobanidus/mods/lootr/data/SpecialChestInventory;m_7208_"));
        assertTrue(build.contains("refmap.data.searge."
                + "'com/lootrbiggerchest/mixin/SpecialChestInventoryMixin'"));
        assertFalse(build.contains("'createMenu': "
                + "'Lnoobanidus/mods/lootr/data/SpecialChestInventory;m_7208_"));
    }

    @Test
    void menuPreparationOnlyDelegatesCustomInventoriesBackToLootr() throws Exception {
        String special = Files.readString(Path.of(
                "src/main/java/com/lootrbiggerchest/mixin/SpecialChestInventoryMixin.java"));
        String chestData = Files.readString(Path.of(
                "src/main/java/com/lootrbiggerchest/mixin/ChestDataMixin.java"));
        String prepare = special.substring(special.indexOf("lootrbiggerchest$prepareMenu"),
                special.indexOf("writeIntSlotItems"));

        assertTrue(prepare.contains("lootrbiggerchest$isCustom()) return null"));
        assertFalse(prepare.contains("!LootrBiggerChest.shouldManage"));
        assertTrue(prepare.contains("if (type == null)"));
        assertTrue(prepare.contains("throw new IllegalStateException("));
        assertTrue(chestData.contains("public boolean lootrbiggerchest$isCustom()"));
        assertTrue(chestData.contains("return custom;"));
    }

    @Test
    void canonicalIntItemsDoNotNeedNormalization() {
        NonNullList<ItemStack> contents = NonNullList.withSize(28, ItemStack.EMPTY);
        contents.set(27, new ItemStack(net.minecraft.world.item.Items.STONE));
        CompoundTag tag = LootrBiggerChest.saveAllItemsWithIntSlots(
                new CompoundTag(), contents);

        assertFalse(LootrBiggerChest.itemsNeedNormalization(tag, contents.size()));
    }

    @Test
    void legacyInvalidAndDuplicateItemsNeedNormalization() {
        CompoundTag legacyItem = new ItemStack(net.minecraft.world.item.Items.STONE)
                .save(new CompoundTag());
        legacyItem.putByte(LootrBiggerChest.SLOT_KEY, (byte) 27);
        ListTag legacyItems = new ListTag();
        legacyItems.add(legacyItem);
        CompoundTag legacyTag = new CompoundTag();
        legacyTag.put(LootrBiggerChest.ITEMS_KEY, legacyItems);
        assertTrue(LootrBiggerChest.itemsNeedNormalization(legacyTag, 28));

        CompoundTag invalidItem = new ItemStack(net.minecraft.world.item.Items.STONE)
                .save(new CompoundTag());
        invalidItem.putInt(LootrBiggerChest.SLOT_KEY, 28);
        ListTag invalidItems = new ListTag();
        invalidItems.add(invalidItem);
        CompoundTag invalidTag = new CompoundTag();
        invalidTag.put(LootrBiggerChest.ITEMS_KEY, invalidItems);
        assertTrue(LootrBiggerChest.itemsNeedNormalization(invalidTag, 28));

        CompoundTag first = new ItemStack(net.minecraft.world.item.Items.STONE)
                .save(new CompoundTag());
        first.putInt(LootrBiggerChest.SLOT_KEY, 27);
        CompoundTag second = first.copy();
        ListTag duplicateItems = new ListTag();
        duplicateItems.add(first);
        duplicateItems.add(second);
        CompoundTag duplicateTag = new CompoundTag();
        duplicateTag.put(LootrBiggerChest.ITEMS_KEY, duplicateItems);
        assertTrue(LootrBiggerChest.itemsNeedNormalization(duplicateTag, 28));
    }

    @Test
    void thirdRoundSafetyMechanismsRemainWired() throws Exception {
        String special = Files.readString(Path.of(
                "src/main/java/com/lootrbiggerchest/mixin/SpecialChestInventoryMixin.java"));
        String chest = Files.readString(Path.of(
                "src/main/java/com/lootrbiggerchest/mixin/ChestBlockEntityMixin.java"));
        String barrel = Files.readString(Path.of(
                "src/main/java/com/lootrbiggerchest/mixin/LootrBarrelBlockEntityMixin.java"));
        String shulker = Files.readString(Path.of(
                "src/main/java/com/lootrbiggerchest/mixin/LootrShulkerBlockEntityMixin.java"));
        String mixins = Files.readString(Path.of(
                "src/main/resources/lootrbiggerchest.mixins.json"));

        assertTrue(special.contains("@Mixin(ServerPlayer.class)"));
        assertTrue(special.contains("@Inject(method = \"openMenu("
                + "Lnet/minecraft/world/MenuProvider;)Ljava/util/OptionalInt;\""));
        assertFalse(special.contains("ChestUtilMenuMixin"));
        assertTrue(mixins.contains("ServerPlayerMenuMixin"));
        assertFalse(mixins.contains("ChestUtilMenuMixin"));
        for (String blockMixin : List.of(chest, barrel, shulker)) {
            assertTrue(blockMixin.contains("setChangedIfServer"));
        }
        assertTrue(special.contains("itemsNeedNormalization"));
        assertEquals("2", LootrBiggerChest.NETWORK_PROTOCOL);
    }

    @Test
    void containerLoadDefersSizeCreationAndAvoidsSetLevelDirtyMarking() throws Exception {
        String core = Files.readString(Path.of(
                "src/main/java/com/lootrbiggerchest/LootrBiggerChest.java"));
        String chest = Files.readString(Path.of(
                "src/main/java/com/lootrbiggerchest/mixin/ChestBlockEntityMixin.java"));
        String barrel = Files.readString(Path.of(
                "src/main/java/com/lootrbiggerchest/mixin/LootrBarrelBlockEntityMixin.java"));
        String shulker = Files.readString(Path.of(
                "src/main/java/com/lootrbiggerchest/mixin/LootrShulkerBlockEntityMixin.java"));
        String minecart = Files.readString(Path.of(
                "src/main/java/com/lootrbiggerchest/mixin/LootrChestMinecartEntityMixin.java"));
        String mixins = Files.readString(Path.of(
                "src/main/resources/lootrbiggerchest.mixins.json"));

        assertFalse(core.contains("interface PendingDirtyAccess"));
        assertFalse(chest.contains("@Mixin(BlockEntity.class)"));
        assertFalse(chest.contains("@Inject(method = \"setLevel\""));
        assertFalse(mixins.contains("BlockEntityLifecycleMixin"));

        for (String source : List.of(chest, barrel, shulker, minecart)) {
            assertFalse(source.contains("pendingDirty"));
            assertTrue(source.contains("loadedContainerSlots"));
            assertEquals(1, source.lines()
                    .filter(line -> line.contains("resolveOrCreateSize(")).count());
        }
    }

    @Test
    void loadedContainerSlotsPreservesExistingAndSerializedCapacityWithoutCreatingSize() {
        CompoundTag missing = new CompoundTag();
        assertEquals(80, LootrBiggerChest.loadedContainerSlots(missing, 27, 80));
        assertFalse(missing.contains(LootrBiggerChest.ROWS_KEY));
        assertFalse(missing.contains(LootrBiggerChest.COLS_KEY));

        CompoundTag stored = new CompoundTag();
        stored.putInt(LootrBiggerChest.ROWS_KEY, 20);
        stored.putInt(LootrBiggerChest.COLS_KEY, 32);
        assertEquals(640, LootrBiggerChest.loadedContainerSlots(stored, 27, 80));
    }

    @Test
    void secondRoundSafetyMechanismsRemainWired() throws Exception {
        assertEquals("2", LootrBiggerChest.NETWORK_PROTOCOL);
        String special = Files.readString(Path.of(
                "src/main/java/com/lootrbiggerchest/mixin/SpecialChestInventoryMixin.java"));
        String chest = Files.readString(Path.of(
                "src/main/java/com/lootrbiggerchest/mixin/ChestBlockEntityMixin.java"));
        String menu = Files.readString(Path.of(
                "src/main/java/com/lootrbiggerchest/menu/LootrBiggerChestMenu.java"));

        assertFalse(special.contains("((ChestDataMixin)"));
        assertTrue(special.contains("ChestDataAccess"));
        assertTrue(special.contains("NetworkHooks.openScreen"));
        assertTrue(special.contains("buffer.writeEnum(spec.type())"));
        assertFalse(special.substring(special.indexOf("afterNbtConstructor"),
                special.indexOf("writeIntSlotItems")).contains("recoverySizeFor"));
        assertTrue(chest.contains("@Inject(method = \"load\", at = @At(\"TAIL\"))"));
        assertFalse(chest.contains("@Inject(method = \"load\", at = @At(\"HEAD\"))"));
        assertFalse(menu.contains("CLIENT_SIZES"));
        assertFalse(menu.contains("safe 3x9 fallback"));
    }

    @Test
    void repairsPartialOrInvalidPersistentSize() {
        CompoundTag partial = new CompoundTag();
        partial.putInt(LootrBiggerChest.ROWS_KEY, 20);
        assertArrayEquals(new int[]{3, 9}, LootrBiggerChest.resolveOrCreateSize(
                partial, ContainerType.CHEST, () -> new int[]{3, 9}));
        assertEquals(3, partial.getInt(LootrBiggerChest.ROWS_KEY));
        assertEquals(9, partial.getInt(LootrBiggerChest.COLS_KEY));
    }

    @Test
    void repairsColumnOnlyPersistentSize() {
        CompoundTag data = new CompoundTag();
        data.putInt(LootrBiggerChest.COLS_KEY, 9);

        assertArrayEquals(new int[]{3, 9}, LootrBiggerChest.resolveOrCreateSize(
                data, ContainerType.CHEST, () -> new int[]{3, 9}));
        assertEquals(3, data.getInt(LootrBiggerChest.ROWS_KEY));
        assertEquals(9, data.getInt(LootrBiggerChest.COLS_KEY));
    }

    @Test
    void repairsZeroPersistentSize() {
        CompoundTag data = new CompoundTag();
        data.putInt(LootrBiggerChest.ROWS_KEY, 0);
        data.putInt(LootrBiggerChest.COLS_KEY, 0);

        assertArrayEquals(new int[]{3, 9}, LootrBiggerChest.resolveOrCreateSize(
                data, ContainerType.CHEST, () -> new int[]{3, 9}));
        assertEquals(3, data.getInt(LootrBiggerChest.ROWS_KEY));
        assertEquals(9, data.getInt(LootrBiggerChest.COLS_KEY));
    }

    @Test
    void repairsOutOfRangePersistentRow() {
        CompoundTag data = new CompoundTag();
        data.putInt(LootrBiggerChest.ROWS_KEY, 21);
        data.putInt(LootrBiggerChest.COLS_KEY, 9);

        assertArrayEquals(new int[]{3, 9}, LootrBiggerChest.resolveOrCreateSize(
                data, ContainerType.CHEST, () -> new int[]{3, 9}));
        assertEquals(3, data.getInt(LootrBiggerChest.ROWS_KEY));
        assertEquals(9, data.getInt(LootrBiggerChest.COLS_KEY));
    }

    @Test
    void repairsOutOfRangePersistentColumn() {
        CompoundTag data = new CompoundTag();
        data.putInt(LootrBiggerChest.ROWS_KEY, 3);
        data.putInt(LootrBiggerChest.COLS_KEY, 33);

        assertArrayEquals(new int[]{3, 9}, LootrBiggerChest.resolveOrCreateSize(
                data, ContainerType.CHEST, () -> new int[]{3, 9}));
        assertEquals(3, data.getInt(LootrBiggerChest.ROWS_KEY));
        assertEquals(9, data.getInt(LootrBiggerChest.COLS_KEY));
    }

    @Test
    void preservesExistingValidPersistentSize() {
        CompoundTag data = new CompoundTag();
        data.putInt(LootrBiggerChest.ROWS_KEY, 20);
        data.putInt(LootrBiggerChest.COLS_KEY, 32);
        assertArrayEquals(new int[]{20, 32}, LootrBiggerChest.resolveOrCreateSize(
                data, ContainerType.CHEST, () -> new int[]{3, 9}));
    }

    @Test
    void keepsPersistedExpandedSizeManagedWhenConfigurationUsesDefaults() {
        CompoundTag data = new CompoundTag();
        data.putInt(LootrBiggerChest.ROWS_KEY, 20);
        data.putInt(LootrBiggerChest.COLS_KEY, 32);

        assertTrue(LootrBiggerChest.shouldManage(data, ContainerType.CHEST));
        assertArrayEquals(new int[]{20, 32}, LootrBiggerChest.resolveOrCreateSize(
                data, ContainerType.CHEST, () -> new int[]{3, 9}));
    }

    @Test
    void savesAndLoadsPersistedSizeWithoutConfigurationGate() {
        CompoundTag persistentData = new CompoundTag();
        persistentData.putInt(LootrBiggerChest.ROWS_KEY, 20);
        persistentData.putInt(LootrBiggerChest.COLS_KEY, 32);
        CompoundTag saveTag = new CompoundTag();

        LootrBiggerChest.saveSizeToTag(persistentData, saveTag);

        CompoundTag loadedData = new CompoundTag();
        LootrBiggerChest.loadSizeFromTag(saveTag, loadedData);
        assertTrue(LootrBiggerChest.shouldManage(loadedData, ContainerType.CHEST));
        assertArrayEquals(new int[]{20, 32}, LootrBiggerChest.resolveOrCreateSize(
                loadedData, ContainerType.CHEST, () -> new int[]{3, 9}));
    }

    @Test
    void expandsWithoutDroppingExistingStacks() {
        NonNullList<ItemStack> original = NonNullList.withSize(2, ItemStack.EMPTY);
        ItemStack marker = new ItemStack(net.minecraft.world.item.Items.STONE);
        original.set(1, marker);
        NonNullList<ItemStack> resized = LootrBiggerChest.resizeItemsSafely(original, 640, "test");
        assertEquals(640, resized.size());
        assertSame(marker, resized.get(1));
    }

    @Test
    void refusesShrinkWhenTruncatedAreaContainsItems() {
        NonNullList<ItemStack> original = NonNullList.withSize(27, ItemStack.EMPTY);
        original.set(26, new ItemStack(net.minecraft.world.item.Items.STONE));
        assertSame(original, LootrBiggerChest.resizeItemsSafely(original, 1, "test"));
    }

    @Test
    void restoresLayoutAndPersistentSizeWhenShrinkWouldHideItems() {
        CompoundTag data = new CompoundTag();
        data.putInt(LootrBiggerChest.ROWS_KEY, 1);
        data.putInt(LootrBiggerChest.COLS_KEY, 1);
        NonNullList<ItemStack> original = NonNullList.withSize(27, ItemStack.EMPTY);
        ItemStack marker = new ItemStack(net.minecraft.world.item.Items.STONE);
        original.set(26, marker);

        LootrBiggerChest.ResizedContainer result = LootrBiggerChest.reconcileSizeWithItems(
                data, new int[]{1, 1}, original, "test");

        assertEquals(3, result.rows());
        assertEquals(9, result.columns());
        assertEquals(27, result.containerSize());
        assertEquals(result.containerSize(), result.items().size());
        assertSame(marker, result.items().get(26));
        assertEquals(3, data.getInt(LootrBiggerChest.ROWS_KEY));
        assertEquals(9, data.getInt(LootrBiggerChest.COLS_KEY));
    }

    @Test
    void capsRecoveredLayoutAtMaximumLegalCapacity() {
        CompoundTag data = new CompoundTag();
        data.putInt(LootrBiggerChest.ROWS_KEY, 1);
        data.putInt(LootrBiggerChest.COLS_KEY, 1);
        NonNullList<ItemStack> original = NonNullList.withSize(640, ItemStack.EMPTY);
        ItemStack marker = new ItemStack(net.minecraft.world.item.Items.STONE);
        original.set(639, marker);

        LootrBiggerChest.ResizedContainer result = LootrBiggerChest.reconcileSizeWithItems(
                data, new int[]{1, 1}, original, "test");

        assertEquals(20, result.rows());
        assertEquals(32, result.columns());
        assertEquals(640, result.containerSize());
        assertEquals(result.containerSize(), result.items().size());
        assertSame(marker, result.items().get(639));
        assertEquals(20, data.getInt(LootrBiggerChest.ROWS_KEY));
        assertEquals(32, data.getInt(LootrBiggerChest.COLS_KEY));
    }

    @Test
    void allowsShrinkWhenTruncatedAreaIsEmpty() {
        NonNullList<ItemStack> original = NonNullList.withSize(27, ItemStack.EMPTY);
        NonNullList<ItemStack> resized = LootrBiggerChest.resizeItemsSafely(original, 1, "test");
        assertEquals(1, resized.size());
    }

    @Test
    void blocksOnlyChestDataShrink() {
        assertFalse(LootrBiggerChest.canResizeChestData(27, 1));
        assertTrue(LootrBiggerChest.canResizeChestData(-1, 1));
        assertTrue(LootrBiggerChest.canResizeChestData(27, 640));
    }

    @Test
    void quickMoveRejectsInvalidSlotIndex() {
        LootrBiggerChestMenu menu = new LootrBiggerChestMenu(null, 0, new Inventory(null),
                new SimpleContainer(1), 1, 1, ContainerType.CHEST);

        assertSame(ItemStack.EMPTY, menu.quickMoveStack(null, -1));
        assertSame(ItemStack.EMPTY, menu.quickMoveStack(null, menu.slots.size()));
    }

    @Test
    void allPublishedSizesHaveSafeSlotCounts() {
        assertEquals(1, Math.multiplyExact(1, 1));
        assertEquals(640, Math.multiplyExact(20, 32));
        for (int rows = 1; rows <= 20; rows++) {
            for (int cols = 1; cols <= 32; cols++) {
                assertTrue(LootrBiggerChestConfig.isValidSize(rows, cols));
                assertTrue(rows * cols <= 640);
            }
        }
    }
}
