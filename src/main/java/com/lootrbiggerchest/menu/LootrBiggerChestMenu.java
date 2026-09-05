package com.lootrbiggerchest.menu;

import com.lootrbiggerchest.LootrBiggerChest;
import com.lootrbiggerchest.LootrBiggerChestConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class LootrBiggerChestMenu extends AbstractContainerMenu {

    private final int rows;
    private final int cols;
    private final int containerSize;
    private final Container container;
    private final ContainerType containerType;

    public LootrBiggerChestMenu(MenuType<?> type, int containerId, Inventory playerInv,
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
        if (playerInv.player != null) this.container.startOpen(playerInv.player);
    }

    public LootrBiggerChestMenu(MenuType<?> type, int containerId, Inventory playerInv, int rows, int cols, ContainerType ct) {
        this(type, containerId, playerInv, new SimpleContainer(rows * cols), rows, cols, ct);
    }

    public static LootrBiggerChestMenu fromNetwork(int id, Inventory inv, FriendlyByteBuf data, ContainerType ct) {
        if (data == null || data.readableBytes() < Integer.BYTES * 2 + 1) {
            throw invalidOpeningData(id, ct, "missing authoritative extra data");
        }
        ContainerType receivedType;
        int rows;
        int cols;
        try {
            receivedType = data.readEnum(ContainerType.class);
            rows = data.readInt();
            cols = data.readInt();
        } catch (RuntimeException exception) {
            throw invalidOpeningData(id, ct, "malformed authoritative extra data");
        }
        if (receivedType != ct || !LootrBiggerChestConfig.isValidSize(rows, cols)) {
            throw invalidOpeningData(id, ct,
                    "received " + receivedType + " " + rows + "x" + cols);
        }
        int slots = Math.multiplyExact(rows, cols);
        LootrBiggerChestMenu menu = new LootrBiggerChestMenu(
                LootrBiggerChest.getMenu(ct), id, inv,
                new SimpleContainer(slots), rows, cols, ct);
        return menu;
    }

    private static IllegalStateException invalidOpeningData(int id, ContainerType type,
                                                            String reason) {
        LootrBiggerChest.LOGGER.error("Aborting {} menu {}: {}", type, id, reason);
        return new IllegalStateException("Cannot safely open " + type + " menu " + id + ": " + reason);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= this.slots.size()) return ItemStack.EMPTY;
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
