package com.lootrbiggerchest.client;

import com.lootrbiggerchest.LootrBiggerChest;
import com.lootrbiggerchest.menu.LootrBiggerChestMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class LootrBiggerChestScreen extends AbstractContainerScreen<LootrBiggerChestMenu> {

    private static final ResourceLocation PANEL_TEXTURE =
            new ResourceLocation(LootrBiggerChest.MOD_ID, "textures/gui/panel.png");
    private static final ResourceLocation SLOT_TEXTURE =
            new ResourceLocation(LootrBiggerChest.MOD_ID, "textures/gui/slot.png");

    private static final int BORDER = 4;
    private static final int PANEL_SIZE = 24;
    private static final int[] PANEL_CUTS = {0, BORDER, PANEL_SIZE - BORDER, PANEL_SIZE};
    private static final int SLOT = 18;

    private ContainerScreenLayout layout;
    private boolean renderingScaled;

    public LootrBiggerChestScreen(LootrBiggerChestMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        int rows = menu.getRows();
        int cols = menu.getCols();
        this.imageWidth = Math.max(cols, 9) * SLOT + 14;
        this.imageHeight = rows * SLOT + 114;
        this.titleLabelY = 6;
        this.inventoryLabelX = (this.imageWidth - 176) / 2 + 8;
        this.inventoryLabelY = rows * SLOT + 20;
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
        int[] xCuts = {leftPos, leftPos + BORDER,
                leftPos + imageWidth - BORDER, leftPos + imageWidth};
        int[] yCuts = {topPos, topPos + BORDER,
                topPos + imageHeight - BORDER, topPos + imageHeight};
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                graphics.blit(PANEL_TEXTURE, xCuts[column], yCuts[row],
                        xCuts[column + 1] - xCuts[column], yCuts[row + 1] - yCuts[row],
                        PANEL_CUTS[column], PANEL_CUTS[row],
                        PANEL_CUTS[column + 1] - PANEL_CUTS[column],
                        PANEL_CUTS[row + 1] - PANEL_CUTS[row], PANEL_SIZE, PANEL_SIZE);
            }
        }
        for (Slot slot : this.menu.slots) {
            graphics.blit(SLOT_TEXTURE, leftPos + slot.x - 1, topPos + slot.y - 1,
                    SLOT, SLOT, 0.0F, 0.0F, SLOT, SLOT, SLOT, SLOT);
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
