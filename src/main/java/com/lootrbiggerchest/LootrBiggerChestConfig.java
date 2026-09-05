package com.lootrbiggerchest;

import com.lootrbiggerchest.menu.LootrBiggerChestMenu.ContainerType;
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LootrBiggerChestConfig {

    public static final int MIN_ROWS = 1;
    public static final int MAX_ROWS = 20;
    public static final int MIN_COLUMNS = 1;
    public static final int MAX_COLUMNS = 32;

    private static final Random RANDOM = new Random();

    public static final ForgeConfigSpec COMMON_SPEC;
    public static final Common COMMON;

    static {
        Pair<Common, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(Common::new);
        COMMON = pair.getLeft();
        COMMON_SPEC = pair.getRight();
    }

    public static class Common {
        public final ForgeConfigSpec.BooleanValue playerSpecificContainerSize;
        public final ForgeConfigSpec.IntValue chestRows;
        public final ForgeConfigSpec.IntValue chestColumns;
        public final ForgeConfigSpec.IntValue barrelRows;
        public final ForgeConfigSpec.IntValue barrelColumns;
        public final ForgeConfigSpec.IntValue shulkerRows;
        public final ForgeConfigSpec.IntValue shulkerColumns;
        public final ForgeConfigSpec.IntValue minecartRows;
        public final ForgeConfigSpec.IntValue minecartColumns;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> chestRandomSizes;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> barrelRandomSizes;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> shulkerRandomSizes;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> minecartRandomSizes;

        Common(ForgeConfigSpec.Builder builder) {
            playerSpecificContainerSize = builder
                    .comment("开启后，每个 Lootr 容器为每名玩家独立计算并持久化容量；已有玩家尺寸不会重新随机")
                    .define("玩家独立容器大小", false);

            builder.comment("容器尺寸配置");

            builder.push("箱子");
            chestRows = builder.defineInRange("行数", 3, MIN_ROWS, MAX_ROWS);
            chestColumns = builder.defineInRange("列数", 9, MIN_COLUMNS, MAX_COLUMNS);
            chestRandomSizes = builder.comment("随机容量组，格式: \"行,列,权重\" 如 [\"3,9,50\",\"6,9,30\"], 留空则使用固定行列")
                    .defineList("随机容量", List.of(), String.class::isInstance);
            builder.pop();

            builder.push("木桶");
            barrelRows = builder.defineInRange("行数", 3, MIN_ROWS, MAX_ROWS);
            barrelColumns = builder.defineInRange("列数", 9, MIN_COLUMNS, MAX_COLUMNS);
            barrelRandomSizes = builder.comment("随机容量组，格式: \"行,列,权重\" 如 [\"3,9,50\",\"6,9,30\"], 留空则使用固定行列")
                    .defineList("随机容量", List.of(), String.class::isInstance);
            builder.pop();

            builder.push("潜影盒");
            shulkerRows = builder.defineInRange("行数", 3, MIN_ROWS, MAX_ROWS);
            shulkerColumns = builder.defineInRange("列数", 9, MIN_COLUMNS, MAX_COLUMNS);
            shulkerRandomSizes = builder.comment("随机容量组，格式: \"行,列,权重\" 如 [\"3,9,50\",\"6,9,30\"], 留空则使用固定行列")
                    .defineList("随机容量", List.of(), String.class::isInstance);
            builder.pop();

            builder.push("箱子矿车");
            minecartRows = builder.defineInRange("行数", 3, MIN_ROWS, MAX_ROWS);
            minecartColumns = builder.defineInRange("列数", 9, MIN_COLUMNS, MAX_COLUMNS);
            minecartRandomSizes = builder.comment("随机容量组，格式: \"行,列,权重\" 如 [\"3,9,50\",\"6,9,30\"], 留空则使用固定行列")
                    .defineList("随机容量", List.of(), String.class::isInstance);
            builder.pop();
        }
    }

    public static int getChestRows() { return COMMON.chestRows.get(); }
    public static int getChestColumns() { return COMMON.chestColumns.get(); }
    public static int getBarrelRows() { return COMMON.barrelRows.get(); }
    public static int getBarrelColumns() { return COMMON.barrelColumns.get(); }
    public static int getShulkerRows() { return COMMON.shulkerRows.get(); }
    public static int getShulkerColumns() { return COMMON.shulkerColumns.get(); }
    public static int getMinecartRows() { return COMMON.minecartRows.get(); }
    public static int getMinecartColumns() { return COMMON.minecartColumns.get(); }

    public static boolean isPlayerSpecificSizeEnabled() {
        return COMMON.playerSpecificContainerSize.get();
    }

    public static int getChestSlots() { return getChestRows() * getChestColumns(); }
    public static int getBarrelSlots() { return getBarrelRows() * getBarrelColumns(); }
    public static int getShulkerSlots() { return getShulkerRows() * getShulkerColumns(); }
    public static int getMinecartSlots() { return getMinecartRows() * getMinecartColumns(); }

    public static boolean isValidSize(int rows, int cols) {
        return rows >= MIN_ROWS && rows <= MAX_ROWS
                && cols >= MIN_COLUMNS && cols <= MAX_COLUMNS;
    }

    public static int[] getFixedSize(ContainerType type) {
        return switch (type) {
            case CHEST -> new int[]{getChestRows(), getChestColumns()};
            case BARREL -> new int[]{getBarrelRows(), getBarrelColumns()};
            case SHULKER -> new int[]{getShulkerRows(), getShulkerColumns()};
            case MINECART -> new int[]{getMinecartRows(), getMinecartColumns()};
        };
    }

    private static List<? extends String> getPool(ContainerType type) {
        return switch (type) {
            case CHEST -> COMMON.chestRandomSizes.get();
            case BARREL -> COMMON.barrelRandomSizes.get();
            case SHULKER -> COMMON.shulkerRandomSizes.get();
            case MINECART -> COMMON.minecartRandomSizes.get();
        };
    }

    public static boolean isExpanded(ContainerType type) {
        int[] fixed = getFixedSize(type);
        return fixed[0] != 3 || fixed[1] != 9 || !getPool(type).isEmpty();
    }

    public static boolean isExpanded() {
        for (ContainerType type : ContainerType.values()) {
            if (isExpanded(type)) return true;
        }
        return false;
    }

    static int[] pickFromPool(List<? extends String> pool, int defaultRows, int defaultCols,
                              Random random, ContainerType type) {
        List<PoolEntry> entries = new ArrayList<>();
        long totalWeight = 0L;
        for (String value : pool) {
            try {
                String[] parts = value.split(",", -1);
                if (parts.length != 3) throw new IllegalArgumentException("expected row,column,weight");
                int rows = Integer.parseInt(parts[0].trim());
                int cols = Integer.parseInt(parts[1].trim());
                long weight = Long.parseLong(parts[2].trim());
                if (!isValidSize(rows, cols) || weight <= 0L) {
                    throw new IllegalArgumentException("dimension or weight outside allowed range");
                }
                totalWeight = Math.addExact(totalWeight, weight);
                entries.add(new PoolEntry(rows, cols, weight));
            } catch (ArithmeticException exception) {
                LootrBiggerChest.LOGGER.warn("Ignoring entire {} random size pool because its total weight overflows long",
                        type);
                return new int[]{defaultRows, defaultCols};
            } catch (RuntimeException exception) {
                LootrBiggerChest.LOGGER.warn("Ignoring invalid {} random size entry '{}': {}",
                        type, value, exception.getMessage());
            }
        }
        if (entries.isEmpty() || totalWeight <= 0L) return new int[]{defaultRows, defaultCols};
        long roll = random.nextLong(totalWeight);
        long cumulative = 0L;
        for (PoolEntry entry : entries) {
            cumulative += entry.weight();
            if (roll < cumulative) return new int[]{entry.rows(), entry.cols()};
        }
        return new int[]{defaultRows, defaultCols};
    }

    public static int[] pickSize(ContainerType type) {
        int[] fixed = getFixedSize(type);
        return pickFromPool(getPool(type), fixed[0], fixed[1], RANDOM, type);
    }

    public static int[] pickChestSize() { return pickSize(ContainerType.CHEST); }
    public static int[] pickBarrelSize() { return pickSize(ContainerType.BARREL); }
    public static int[] pickShulkerSize() { return pickSize(ContainerType.SHULKER); }
    public static int[] pickMinecartSize() { return pickSize(ContainerType.MINECART); }

    private record PoolEntry(int rows, int cols, long weight) {}
}
