package com.lootrbiggerchest.client;

import com.lootrbiggerchest.menu.LootrBiggerChestMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class LootrBiggerChestScreen extends AbstractContainerScreen<LootrBiggerChestMenu> {

    private final int rows;
    private final int cols;
    private ContainerScreenLayout layout;
    private boolean renderingScaled;

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
    protected void init() {
        this.layout = ContainerScreenLayout.fit(this.width, this.height,
                this.imageWidth, this.imageHeight);
        this.leftPos = (layout.logicalWidth() - this.imageWidth) / 2;
        this.topPos = (layout.logicalHeight() - this.imageHeight) / 2;
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
        this.renderBackground(graphics);
        int viewportWidth = this.width;
        int viewportHeight = this.height;
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(layout.offsetX(), layout.offsetY(), 0.0F);
            graphics.pose().scale(layout.scale(), layout.scale(), 1.0F);
            this.width = layout.logicalWidth();
            this.height = layout.logicalHeight();
            this.renderingScaled = true;
            super.render(graphics, Mth.floor(layout.toLogicalX(mouseX)),
                    Mth.floor(layout.toLogicalY(mouseY)), partialTick);
        } finally {
            this.width = viewportWidth;
            this.height = viewportHeight;
            this.renderingScaled = false;
            graphics.pose().popPose();
        }
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(layout.toLogicalX(mouseX), layout.toLogicalY(mouseY), button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(layout.toLogicalX(mouseX), layout.toLogicalY(mouseY), button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return super.mouseDragged(layout.toLogicalX(mouseX), layout.toLogicalY(mouseY), button,
                layout.toLogicalDelta(dragX), layout.toLogicalDelta(dragY));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return super.mouseScrolled(layout.toLogicalX(mouseX), layout.toLogicalY(mouseY), amount);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(layout.toLogicalX(mouseX), layout.toLogicalY(mouseY));
    }

    @Override
    public int getGuiLeft() {
        return layout == null || renderingScaled ? super.getGuiLeft()
                : Mth.floor(layout.toScreenX(this.leftPos));
    }

    @Override
    public int getGuiTop() {
        return layout == null || renderingScaled ? super.getGuiTop()
                : Mth.floor(layout.toScreenY(this.topPos));
    }

    @Override
    public int getXSize() {
        return layout == null || renderingScaled ? super.getXSize()
                : Mth.ceil(this.imageWidth * (double) layout.scale());
    }

    @Override
    public int getYSize() {
        return layout == null || renderingScaled ? super.getYSize()
                : Mth.ceil(this.imageHeight * (double) layout.scale());
    }
}
