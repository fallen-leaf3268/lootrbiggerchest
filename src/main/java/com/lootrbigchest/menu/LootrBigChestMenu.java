package com.lootrbigchest.menu;

import com.lootrbigchest.LootrBigChest;
import com.lootrbigchest.LootrBigChestConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class LootrBigChestMenu extends AbstractContainerMenu {

    private final int rows;
    private final int cols;
    private final int containerSize;
    private final Container container;
    private final ContainerType containerType;

    public LootrBigChestMenu(MenuType<?> type, int containerId, Inventory playerInv,
                             Container container, int rows, int cols, ContainerType ct) {
        super(type, containerId);
        this.rows = rows;
        this.cols = cols;
        this.containerType = ct;
        this.containerSize = rows * cols;
        this.container = container;
        checkContainerSize(container, containerSize);

        int guiWidth = Math.max(cols, 9) * 18 + 14;
        int containerX = (guiWidth - cols * 18) / 2 + 1;
        int playerInvX = (guiWidth - 9 * 18) / 2 + 1;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                this.addSlot(new Slot(container, col + row * cols,
                        containerX + col * 18, 18 + row * 18));
            }
        }

        int playerInvY = 18 + rows * 18 + 14;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9,
                        playerInvX + col * 18, playerInvY + row * 18));
            }
        }

        int hotbarY = playerInvY + 3 * 18 + 4;
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, playerInvX + col * 18, hotbarY));
        }
    }

    public LootrBigChestMenu(MenuType<?> type, int containerId, Inventory playerInv, int rows, int cols, ContainerType ct) {
        this(type, containerId, playerInv, new SimpleContainer(rows * cols), rows, cols, ct);
    }

    public LootrBigChestMenu(int containerId, Inventory playerInv, ContainerType ct) {
        this(LootrBigChest.getMenu(ct), containerId, playerInv,
                new SimpleContainer(getRows(ct) * getCols(ct)), getRows(ct), getCols(ct), ct);
    }

    @SuppressWarnings("resource")
    public static LootrBigChestMenu fromNetwork(int id, Inventory inv, FriendlyByteBuf data, ContainerType ct) {
        MenuType<LootrBigChestMenu> type = LootrBigChest.getMenu(ct);
        try {
            int[] pending = LootrBigChest.PENDING_MENU_SIZE.get();
            if (pending != null) {
                int rows = pending[0];
                int cols = pending[1];
                data.writeInt(rows);
                data.writeInt(cols);
                return new LootrBigChestMenu(type, id, inv, new SimpleContainer(rows * cols), rows, cols, ct);
            }
            if (data != null && data.readableBytes() >= 8) {
                int rows = data.readInt();
                int cols = data.readInt();
                return new LootrBigChestMenu(type, id, inv, new SimpleContainer(rows * cols), rows, cols, ct);
            }
            int[] clientSize = LootrBigChest.CLIENT_SIZES.remove(Integer.valueOf(id));
            if (clientSize != null) {
                return new LootrBigChestMenu(type, id, inv,
                        new SimpleContainer(clientSize[0] * clientSize[1]), clientSize[0], clientSize[1], ct);
            }
            int rows = getRows(ct);
            int cols = getCols(ct);
            return new LootrBigChestMenu(type, id, inv, new SimpleContainer(rows * cols), rows, cols, ct);
        } finally {
            LootrBigChest.PENDING_MENU_SIZE.remove();
        }
    }

    private static int getRows(ContainerType ct) {
        return switch (ct) {
            case CHEST -> LootrBigChestConfig.getChestRows();
            case BARREL -> LootrBigChestConfig.getBarrelRows();
            case SHULKER -> LootrBigChestConfig.getShulkerRows();
            case MINECART -> LootrBigChestConfig.getMinecartRows();
        };
    }

    private static int getCols(ContainerType ct) {
        return switch (ct) {
            case CHEST -> LootrBigChestConfig.getChestColumns();
            case BARREL -> LootrBigChestConfig.getBarrelColumns();
            case SHULKER -> LootrBigChestConfig.getShulkerColumns();
            case MINECART -> LootrBigChestConfig.getMinecartColumns();
        };
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            moved = stack.copy();
            int playerStart = containerSize;
            int playerEnd = containerSize + 36;
            if (slotIndex < playerStart) {
                if (!this.moveItemStackTo(stack, playerStart, playerEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(stack, 0, playerStart, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return moved;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }

    public Container getContainer() {
        return this.container;
    }

    public int getRows() { return rows; }
    public int getCols() { return cols; }
    public ContainerType getContainerType() { return containerType; }

    public enum ContainerType { CHEST, BARREL, SHULKER, MINECART }
}
