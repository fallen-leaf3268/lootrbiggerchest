package com.lootrbiggerchest.mixin;

import com.lootrbiggerchest.LootrBiggerChest;
import com.lootrbiggerchest.LootrBiggerChestConfig;
import com.lootrbiggerchest.menu.LootrBiggerChestMenu;
import com.lootrbiggerchest.menu.LootrBiggerChestMenu.ContainerType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import noobanidus.mods.lootr.block.entities.LootrBarrelBlockEntity;
import noobanidus.mods.lootr.block.entities.LootrShulkerBlockEntity;
import noobanidus.mods.lootr.data.SpecialChestInventory;
import noobanidus.mods.lootr.entity.LootrChestMinecartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SpecialChestInventory.class)
public class SpecialChestInventoryMixin {

    private static final String ROWS_KEY = "LBCRows";
    private static final String COLS_KEY = "LBCCols";

    @Inject(method = "createMenu", at = @At("HEAD"))
    private void beforeCreateMenu(int id, Inventory inventory, Player player,
                                  CallbackInfoReturnable<AbstractContainerMenu> cir) {
        SpecialChestInventory self = (SpecialChestInventory) (Object) this;
        if (!LootrBiggerChestConfig.isExpanded()) return;

        Level level = player.level();
        BaseContainerBlockEntity tile = self.getTile(level);
        int rows, cols;

        if (tile instanceof LootrBarrelBlockEntity) {
            CompoundTag data = tile.getPersistentData();
            if (data.contains(ROWS_KEY)) {
                rows = data.getInt(ROWS_KEY);
                cols = data.getInt(COLS_KEY);
            } else {
                int[] size = LootrBiggerChestConfig.pickBarrelSize();
                rows = size[0];
                cols = size[1];
                data.putInt(ROWS_KEY, rows);
                data.putInt(COLS_KEY, cols);
            }
        } else if (tile instanceof LootrShulkerBlockEntity) {
            CompoundTag data = tile.getPersistentData();
            if (data.contains(ROWS_KEY)) {
                rows = data.getInt(ROWS_KEY);
                cols = data.getInt(COLS_KEY);
            } else {
                int[] size = LootrBiggerChestConfig.pickShulkerSize();
                rows = size[0];
                cols = size[1];
                data.putInt(ROWS_KEY, rows);
                data.putInt(COLS_KEY, cols);
            }
        } else if (tile != null) {
            CompoundTag data = tile.getPersistentData();
            if (data.contains(ROWS_KEY)) {
                rows = data.getInt(ROWS_KEY);
                cols = data.getInt(COLS_KEY);
            } else {
                int[] size = LootrBiggerChestConfig.pickChestSize();
                rows = size[0];
                cols = size[1];
                data.putInt(ROWS_KEY, rows);
                data.putInt(COLS_KEY, cols);
            }
        } else {
            LootrChestMinecartEntity entity = self.getEntity(level);
            if (entity != null) {
                CompoundTag data = entity.getPersistentData();
                if (data.contains(ROWS_KEY)) {
                    rows = data.getInt(ROWS_KEY);
                    cols = data.getInt(COLS_KEY);
                } else {
                    int[] size = LootrBiggerChestConfig.pickMinecartSize();
                    rows = size[0];
                    cols = size[1];
                    data.putInt(ROWS_KEY, rows);
                    data.putInt(COLS_KEY, cols);
                }
            } else {
                int[] size = LootrBiggerChestConfig.pickMinecartSize();
                rows = size[0];
                cols = size[1];
            }
        }

        int newSize = rows * cols;
        int currentSize = self.getContainerSize();
        if (currentSize != newSize) {
            self.resizeInventory(newSize);
        }

        LootrBiggerChest.PENDING_MENU_SIZE.remove();
        LootrBiggerChest.PENDING_MENU_SIZE.set(new int[]{rows, cols});

        if (player instanceof ServerPlayer sp) {
            LootrBiggerChest.sendMenuSize(sp, id, rows, cols);
        }
    }

    @Inject(method = "createMenu", at = @At("RETURN"), cancellable = true)
    private void afterCreateMenu(int id, Inventory inventory, Player player,
                                 CallbackInfoReturnable<AbstractContainerMenu> cir) {
        SpecialChestInventory self = (SpecialChestInventory) (Object) this;
        if (!LootrBiggerChestConfig.isExpanded()) return;

        Level level = player.level();
        BaseContainerBlockEntity tile = self.getTile(level);
        ContainerType ct;
        int rows, cols;

        if (tile instanceof LootrBarrelBlockEntity) {
            ct = ContainerType.BARREL;
            CompoundTag data = tile.getPersistentData();
            rows = data.getInt(ROWS_KEY);
            cols = data.getInt(COLS_KEY);
        } else if (tile instanceof LootrShulkerBlockEntity) {
            ct = ContainerType.SHULKER;
            CompoundTag data = tile.getPersistentData();
            rows = data.getInt(ROWS_KEY);
            cols = data.getInt(COLS_KEY);
        } else if (tile != null) {
            ct = ContainerType.CHEST;
            CompoundTag data = tile.getPersistentData();
            rows = data.getInt(ROWS_KEY);
            cols = data.getInt(COLS_KEY);
        } else {
            ct = ContainerType.MINECART;
            LootrChestMinecartEntity entity = self.getEntity(level);
            if (entity != null) {
                CompoundTag data = entity.getPersistentData();
                rows = data.getInt(ROWS_KEY);
                cols = data.getInt(COLS_KEY);
            } else {
                int[] size = LootrBiggerChestConfig.pickMinecartSize();
                rows = size[0];
                cols = size[1];
            }
        }

        cir.setReturnValue(new LootrBiggerChestMenu(
                LootrBiggerChest.getMenu(ct), id, inventory, self, rows, cols, ct));
    }
}
