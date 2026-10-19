package com.lootrbiggerchest.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

public final class ContainerGuiBackground {
    public static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    private static final int TOP_H = 17;
    private static final int ROW_H = 18;
    private static final int GAP_H = 14;
    private static final int GAP_TEX_Y = 125;
    private static final int BORDER = 7;
    private static final int SLOT = 18;
    private static final int VANILLA_SLOT_W = 162;
    private static final int PLAYER_INV_H = 83;
    private static final int PLAYER_INV_TEX_Y = 139;
    private static final int SHADOW_CORNER_Y = 219;
    private static final int SHADOW_EXT = 3;

    private ContainerGuiBackground() {
    }

    public static int width(int cols) {
        return Math.max(cols, 9) * SLOT + 14;
    }

    public static int height(int rows) {
        return rows * SLOT + 114;
    }

    public static void render(GuiGraphics graphics, int x, int y, int rows, int cols) {
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        draw((dx, dy, w, h, u, v, sw, sh) -> graphics.blit(TEXTURE,
                x + dx, y + dy, w, h, (float) u, (float) v, sw, sh, 256, 256), rows, cols);
    }

    public static BufferedImage createImage(BufferedImage source, int rows, int cols) {
        BufferedImage image = new BufferedImage(width(cols), height(rows), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            draw((x, y, w, h, u, v, sw, sh) -> {
                Graphics2D part = (Graphics2D) graphics.create(x, y, w, h);
                try {
                    part.drawImage(source, new AffineTransform(
                            w * 256.0 / (sw * source.getWidth()), 0,
                            0, h * 256.0 / (sh * source.getHeight()),
                            -u * w / (double) sw, -v * h / (double) sh), null);
                } finally {
                    part.dispose();
                }
            }, rows, cols);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static void draw(Blitter target, int rows, int cols) {
        int imageWidth = width(cols);
        int interiorW = imageWidth - 2 * BORDER;
        int rightX = imageWidth - BORDER;
        int slotW = cols * SLOT;
        int slotX = (imageWidth - slotW) / 2;

        blitSlotArea(target, BORDER, 0, 0, TOP_H, interiorW);
        blitBorder(target, 0, 0, 0, 0, TOP_H);
        blitBorder(target, rightX, 0, 169, 0, TOP_H);

        for (int r = 0; r < rows; r++) {
            int ry = TOP_H + r * ROW_H;
            int texY = r == 0 ? 17 : r == rows - 1 ? 107 : 35;
            target.blit(BORDER, ry, interiorW, ROW_H, 10, 130, 1, 1);
            blitBorder(target, 0, ry, 0, texY, ROW_H);
            blitBorder(target, rightX, ry, 169, texY, ROW_H);
            blitSlotArea(target, slotX, ry, texY, ROW_H, slotW);
        }

        int gapY = TOP_H + rows * ROW_H;
        blitSlotArea(target, BORDER, gapY, GAP_TEX_Y, GAP_H, interiorW);
        blitBorder(target, 0, gapY, 0, GAP_TEX_Y, GAP_H);
        blitBorder(target, rightX, gapY, 169, GAP_TEX_Y, GAP_H);

        int invY = gapY + GAP_H;
        int invX = (imageWidth - 176) / 2;
        target.blit(invX, invY, 176, PLAYER_INV_H, 0, PLAYER_INV_TEX_Y, 176, PLAYER_INV_H);

        if (imageWidth > 176) {
            int leftW = invX;
            int rightW = imageWidth - invX - 176;
            if (leftW > 0) {
                int cw = Math.min(BORDER, leftW);
                target.blit(0, invY, cw, 3, 0, SHADOW_CORNER_Y, cw, 3);
                if (leftW > BORDER) {
                    int fw = leftW - BORDER + SHADOW_EXT;
                    target.blit(BORDER, invY, fw, 2, 5, 220, 1, 1);
                }
            }
            if (rightW > 0) {
                int cw = Math.min(BORDER, rightW);
                int rx = imageWidth - cw;
                target.blit(rx, invY, cw, 3, 176 - cw, SHADOW_CORNER_Y, cw, 3);
                if (rightW > BORDER) {
                    int fw = rightW - BORDER + SHADOW_EXT;
                    target.blit(invX + 176 - SHADOW_EXT, invY, fw, 2, 5, 220, 1, 1);
                }
            }
        }
    }

    private static void blitBorder(Blitter target, int x, int y, int texX, int texY, int h) {
        target.blit(x, y, BORDER, h, texX, texY, BORDER, h);
    }

    private static void blitSlotArea(Blitter target, int x, int y, int texY, int h, int totalW) {
        for (int sx = 0; sx < totalW; sx += VANILLA_SLOT_W) {
            int tw = Math.min(VANILLA_SLOT_W, totalW - sx);
            target.blit(x + sx, y, tw, h, BORDER, texY, tw, h);
        }
    }

    @FunctionalInterface
    private interface Blitter {
        void blit(int x, int y, int w, int h, int u, int v, int sourceWidth, int sourceHeight);
    }
}
