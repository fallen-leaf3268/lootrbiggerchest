package com.lootrbiggerchest.client;

import com.lootrbiggerchest.menu.LootrBiggerChestMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class LootrBiggerChestScreen extends AbstractContainerScreen<LootrBiggerChestMenu> {

    private final int rows;
    private final int cols;

    public LootrBiggerChestScreen(LootrBiggerChestMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.rows = menu.getRows();
        this.cols = menu.getCols();
        this.imageWidth = ContainerGuiBackground.width(cols);
        this.imageHeight = ContainerGuiBackground.height(rows);
        this.titleLabelY = 6;
        this.inventoryLabelX = (this.imageWidth - 176) / 2 + 8;
        this.inventoryLabelY = rows * 18 + 20;
    }

    @Override
    public void added() {
        ContainerGuiScale.open(this);
        super.added();
    }

    @Override
    public void removed() {
        try {
            super.removed();
        } finally {
            ContainerGuiScale.close(this);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        var texture = ContainerGuiTextures.INSTANCE.find(menu.getContainerType(), rows, cols);
        if (texture.isPresent()) {
            graphics.blit(texture.get(), leftPos, topPos, imageWidth, imageHeight,
                    0.0F, 0.0F, imageWidth, imageHeight, imageWidth, imageHeight);
        } else {
            ContainerGuiBackground.render(graphics, leftPos, topPos, rows, cols);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
