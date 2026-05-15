package com.lootrbiggerchest;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LootrBiggerChestConfig {

    private static final Random RANDOM = new Random();

    public static final ForgeConfigSpec COMMON_SPEC;
    public static final Common COMMON;

    static {
        Pair<Common, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(Common::new);
        COMMON = pair.getLeft();
        COMMON_SPEC = pair.getRight();
    }

    public static class Common {
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
            builder.comment("容器尺寸配置");

            builder.push("箱子");
            chestRows = builder.defineInRange("行数", 3, 1, 20);
            chestColumns = builder.defineInRange("列数", 9, 1, 32);
            chestRandomSizes = builder.comment("随机容量组，格式: \"行,列,权重\" 如 [\"3,9,50\",\"6,9,30\"], 留空则使用固定行列")
                    .defineList("随机容量", List.of(), o -> o instanceof String s && s.matches("\\d+,\\d+,\\d+"));
            builder.pop();

            builder.push("木桶");
            barrelRows = builder.defineInRange("行数", 3, 1, 20);
            barrelColumns = builder.defineInRange("列数", 9, 1, 32);
            barrelRandomSizes = builder.comment("随机容量组，格式: \"行,列,权重\" 如 [\"3,9,50\",\"6,9,30\"], 留空则使用固定行列")
                    .defineList("随机容量", List.of(), o -> o instanceof String s && s.matches("\\d+,\\d+,\\d+"));
            builder.pop();

            builder.push("潜影盒");
            shulkerRows = builder.defineInRange("行数", 3, 1, 20);
            shulkerColumns = builder.defineInRange("列数", 9, 1, 32);
            shulkerRandomSizes = builder.comment("随机容量组，格式: \"行,列,权重\" 如 [\"3,9,50\",\"6,9,30\"], 留空则使用固定行列")
                    .defineList("随机容量", List.of(), o -> o instanceof String s && s.matches("\\d+,\\d+,\\d+"));
            builder.pop();

            builder.push("箱子矿车");
            minecartRows = builder.defineInRange("行数", 3, 1, 20);
            minecartColumns = builder.defineInRange("列数", 9, 1, 32);
            minecartRandomSizes = builder.comment("随机容量组，格式: \"行,列,权重\" 如 [\"3,9,50\",\"6,9,30\"], 留空则使用固定行列")
                    .defineList("随机容量", List.of(), o -> o instanceof String s && s.matches("\\d+,\\d+,\\d+"));
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

    public static int getChestSlots() { return getChestRows() * getChestColumns(); }
    public static int getBarrelSlots() { return getBarrelRows() * getBarrelColumns(); }
    public static int getShulkerSlots() { return getShulkerRows() * getShulkerColumns(); }
    public static int getMinecartSlots() { return getMinecartRows() * getMinecartColumns(); }

    public static boolean isExpanded() {
        return getChestRows() != 3 || getChestColumns() != 9
                || getBarrelRows() != 3 || getBarrelColumns() != 9
                || getShulkerRows() != 3 || getShulkerColumns() != 9
                || getMinecartRows() != 3 || getMinecartColumns() != 9
                || !COMMON.chestRandomSizes.get().isEmpty()
                || !COMMON.barrelRandomSizes.get().isEmpty()
                || !COMMON.shulkerRandomSizes.get().isEmpty()
                || !COMMON.minecartRandomSizes.get().isEmpty();
    }

    private static class PoolEntry {
        final int rows;
        final int cols;
        final int weight;
        PoolEntry(int rows, int cols, int weight) {
            this.rows = rows;
            this.cols = cols;
            this.weight = weight;
        }
    }

    private static int[] pickFromPool(List<? extends String> pool, int defaultRows, int defaultCols) {
        if (pool.isEmpty()) return new int[]{defaultRows, defaultCols};

        List<PoolEntry> entries = new ArrayList<>();
        int total = 0;

        for (String s : pool) {
            String[] p = s.split(",");
            int weight = Integer.parseInt(p[2]);
            if (weight <= 0) continue;
            entries.add(new PoolEntry(Integer.parseInt(p[0]), Integer.parseInt(p[1]), weight));
            total += weight;
        }

        if (entries.isEmpty()) return new int[]{defaultRows, defaultCols};

        int roll = RANDOM.nextInt(total);
        int cumulative = 0;
        for (PoolEntry e : entries) {
            cumulative += e.weight;
            if (roll < cumulative) return new int[]{e.rows, e.cols};
        }
        return new int[]{defaultRows, defaultCols};
    }

    public static int[] pickChestSize() {
        return pickFromPool(COMMON.chestRandomSizes.get(), getChestRows(), getChestColumns());
    }

    public static int[] pickBarrelSize() {
        return pickFromPool(COMMON.barrelRandomSizes.get(), getBarrelRows(), getBarrelColumns());
    }

    public static int[] pickShulkerSize() {
        return pickFromPool(COMMON.shulkerRandomSizes.get(), getShulkerRows(), getShulkerColumns());
    }

    public static int[] pickMinecartSize() {
        return pickFromPool(COMMON.minecartRandomSizes.get(), getMinecartRows(), getMinecartColumns());
    }

    public static final String RANDOM_KEY = "LBCIsRandom";

    public static boolean hasChestRandom() { return !COMMON.chestRandomSizes.get().isEmpty(); }
    public static boolean hasBarrelRandom() { return !COMMON.barrelRandomSizes.get().isEmpty(); }
    public static boolean hasShulkerRandom() { return !COMMON.shulkerRandomSizes.get().isEmpty(); }
    public static boolean hasMinecartRandom() { return !COMMON.minecartRandomSizes.get().isEmpty(); }

    public static int maxSlotCount() {
        return Math.max(Math.max(getChestSlots(), getBarrelSlots()),
                Math.max(getShulkerSlots(), getMinecartSlots()));
    }
}
