package com.lootrbiggerchest.client;

import com.lootrbiggerchest.menu.LootrBiggerChestMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEX);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
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

    private void fillInterior(GuiGraphics g, int x, int y, int w, int h) {
        g.blit(TEX, x, y, w, h, (float) BG_TEX_X, (float) BG_TEX_Y, 1, 1, 256, 256);
    }

    private void blitLeftBorder(GuiGraphics g, int x, int y, int texY, int h) {
        g.blit(TEX, x, y, 0, texY, BORDER, h);
    }

    private void blitRightBorder(GuiGraphics g, int x, int y, int texY, int h) {
        g.blit(TEX, x, y, 169, texY, BORDER, h);
    }

    private void blitSlotArea(GuiGraphics g, int x, int y, int texY, int h, int totalW) {
        for (int sx = 0; sx < totalW; sx += VANILLA_SLOT_W) {
            int tw = Math.min(VANILLA_SLOT_W, totalW - sx);
            g.blit(TEX, x + sx, y, BORDER, texY, tw, h);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
