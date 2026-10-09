package com.lootrbiggerchest.client;

import com.lootrbiggerchest.menu.LootrBiggerChestMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class LootrBiggerChestScreen extends AbstractContainerScreen<LootrBiggerChestMenu> {

    private static final ResourceLocation TEX =
            new ResourceLocation("textures/gui/container/generic_54.png");

    private static final int TOP_H = 17;
    private static final int ROW_H = 18;
    private static final int GAP_H = 14;
    private static final int GAP_TEX_Y = 125;
    private static final int BORDER = 7;
    private static final int SLOT = 18;
    private static final int VANILLA_SLOT_W = 162;
    private static final int PLAYER_INV_H = 83;
    private static final int PLAYER_INV_TEX_Y = 139;
    private static final int BG_TEX_X = 10;
    private static final int BG_TEX_Y = 130;
    private static final int SHADOW_CORNER_Y = 219;
    private static final int SHADOW_EXT = 3;

    private final int rows;
    private final int cols;
    private ContainerScreenLayout layout;
    private boolean renderingScaled;

    public LootrBiggerChestScreen(LootrBiggerChestMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.rows = menu.getRows();
        this.cols = menu.getCols();
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
        int x = this.leftPos;
        int y = this.topPos;
        int interiorW = this.imageWidth - 2 * BORDER;
        int rightX = x + this.imageWidth - BORDER;
        int slotW = cols * SLOT;
        int slotX = x + (this.imageWidth - slotW) / 2;

        blitSlotArea(graphics, x + BORDER, y, 0, TOP_H, interiorW);
        blitLeftBorder(graphics, x, y, 0, TOP_H);
        blitRightBorder(graphics, rightX, y, 0, TOP_H);

        for (int r = 0; r < rows; r++) {
            int ry = y + TOP_H + r * ROW_H;
            int texY = rowTexY(r);
            fillInterior(graphics, x + BORDER, ry, interiorW, ROW_H);
            blitLeftBorder(graphics, x, ry, texY, ROW_H);
            blitRightBorder(graphics, rightX, ry, texY, ROW_H);
            blitSlotArea(graphics, slotX, ry, texY, ROW_H, slotW);
        }

        int gapY = y + TOP_H + rows * ROW_H;
        blitSlotArea(graphics, x + BORDER, gapY, GAP_TEX_Y, GAP_H, interiorW);
        blitLeftBorder(graphics, x, gapY, GAP_TEX_Y, GAP_H);
        blitRightBorder(graphics, rightX, gapY, GAP_TEX_Y, GAP_H);

        int invY = gapY + GAP_H;
        int invX = x + (this.imageWidth - 176) / 2;
        graphics.blit(TEX, invX, invY, 0, PLAYER_INV_TEX_Y, 176, PLAYER_INV_H);

        if (this.imageWidth > 176) {
            int leftW = invX - x;
            int rightW = x + this.imageWidth - invX - 176;
            if (leftW > 0) {
                int cw = Math.min(BORDER, leftW);
                graphics.blit(TEX, x, invY, 0, SHADOW_CORNER_Y, cw, 3);
                if (leftW > BORDER) {
                    int fw = leftW - BORDER + SHADOW_EXT;
                    graphics.blit(TEX, x + BORDER, invY, fw, 2, 5f, 220f, 1, 1, 256, 256);
                }
            }
            if (rightW > 0) {
                int cw = Math.min(BORDER, rightW);
                int rx = x + this.imageWidth - cw;
                graphics.blit(TEX, rx, invY, 176 - cw, SHADOW_CORNER_Y, cw, 3);
                if (rightW > BORDER) {
                    int fw = rightW - BORDER + SHADOW_EXT;
                    graphics.blit(TEX, invX + 176 - SHADOW_EXT, invY, fw, 2, 5f, 220f, 1, 1, 256, 256);
                }
            }
        }
    }

    private int rowTexY(int r) {
        if (r == 0) return 17;
        if (r == rows - 1) return 107;
        return 35;
    }

    private void fillInterior(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.blit(TEX, x, y, w, h, (float) BG_TEX_X, (float) BG_TEX_Y, 1, 1, 256, 256);
    }

    private void blitLeftBorder(GuiGraphics graphics, int x, int y, int texY, int h) {
        graphics.blit(TEX, x, y, 0, texY, BORDER, h);
    }

    private void blitRightBorder(GuiGraphics graphics, int x, int y, int texY, int h) {
        graphics.blit(TEX, x, y, 169, texY, BORDER, h);
    }

    private void blitSlotArea(GuiGraphics graphics, int x, int y, int texY, int h, int totalW) {
        for (int sx = 0; sx < totalW; sx += VANILLA_SLOT_W) {
            int tw = Math.min(VANILLA_SLOT_W, totalW - sx);
            graphics.blit(TEX, x + sx, y, BORDER, texY, tw, h);
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
