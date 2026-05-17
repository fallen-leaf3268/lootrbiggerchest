package com.lootrbiggerchest.mixin;

import com.lootrbiggerchest.LootrBiggerChest;
import com.lootrbiggerchest.LootrBiggerChestConfig;
import com.lootrbiggerchest.menu.LootrBiggerChestMenu;
import com.lootrbiggerchest.menu.LootrBiggerChestMenu.ContainerType;
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

import java.util.Iterator;

@Mixin(value = SpecialChestInventory.class)
public class SpecialChestInventoryMixin {

    @Inject(method = "createMenu", at = @At("HEAD"))
    private void beforeCreateMenu(int id, Inventory inventory, Player player,
                                  CallbackInfoReturnable<AbstractContainerMenu> cir) {
        SpecialChestInventory self = (SpecialChestInventory) (Object) this;
        if (!LootrBiggerChestConfig.isExpanded()) return;

        Level level = player.level();
        BaseContainerBlockEntity tile = self.getTile(level);
        int rows, cols;

        if (tile != null) {
            int[] size = LootrBiggerChest.getOrCreateSize(tile, null);
            rows = size[0]; cols = size[1];
        } else {
            LootrChestMinecartEntity entity = self.getEntity(level);
            if (entity != null) {
                int[] size = LootrBiggerChest.resolveOrCreateSize(entity.getPersistentData(), LootrBiggerChestConfig::pickMinecartSize);
                rows = size[0]; cols = size[1];
            } else {
                int[] size = LootrBiggerChestConfig.pickMinecartSize();
                rows = size[0]; cols = size[1];
            }
        }

        int newSize = rows * cols;
        int currentSize = self.getContainerSize();
        if (currentSize != newSize) {
            self.resizeInventory(newSize);
        }

        if (LootrBiggerChest.PENDING_MENU_SIZES.size() > 100) {
            Iterator<Integer> it = LootrBiggerChest.PENDING_MENU_SIZES.keySet().iterator();
            for (int i = 0; i < 25 && it.hasNext(); i++) { it.next(); it.remove(); }
        }
        LootrBiggerChest.PENDING_MENU_SIZES.put(id, new int[]{rows, cols});

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
        } else if (tile instanceof LootrShulkerBlockEntity) {
            ct = ContainerType.SHULKER;
        } else if (tile != null) {
            ct = ContainerType.CHEST;
        } else {
            ct = ContainerType.MINECART;
        }

        if (ct != ContainerType.MINECART) {
            int[] size = LootrBiggerChest.getOrCreateSize(tile, null);
            rows = size[0]; cols = size[1];
        } else {
            LootrChestMinecartEntity entity = self.getEntity(level);
            if (entity != null) {
                int[] size = LootrBiggerChest.resolveOrCreateSize(entity.getPersistentData(), LootrBiggerChestConfig::pickMinecartSize);
                rows = size[0]; cols = size[1];
            } else {
                int[] size = LootrBiggerChestConfig.pickMinecartSize();
                rows = size[0]; cols = size[1];
            }
        }

        cir.setReturnValue(new LootrBiggerChestMenu(
                LootrBiggerChest.getMenu(ct), id, inventory, self, rows, cols, ct));
    }
}
