package com.lootrbiggerchest.client;

public record ContainerScreenLayout(float scale, int logicalWidth, int logicalHeight,
                                    float offsetX, float offsetY) {

    public static ContainerScreenLayout fit(int viewportWidth, int viewportHeight,
                                             int imageWidth, int imageHeight) {
        float scale = Math.min(1.0F, Math.min(
                Math.max(1, viewportWidth - 16) / (float) imageWidth,
                Math.max(1, viewportHeight - 16) / (float) imageHeight));
        int logicalWidth = (int) Math.floor(viewportWidth / (double) scale);
        int logicalHeight = (int) Math.floor(viewportHeight / (double) scale);
        float offsetX = (float) ((viewportWidth - logicalWidth * (double) scale) / 2);
        float offsetY = (float) ((viewportHeight - logicalHeight * (double) scale) / 2);
        return new ContainerScreenLayout(scale, logicalWidth, logicalHeight, offsetX, offsetY);
    }

    public double toLogicalX(double screenX) {
        return (screenX - offsetX) / scale;
    }

    public double toLogicalY(double screenY) {
        return (screenY - offsetY) / scale;
    }

    public double toLogicalDelta(double delta) {
        return delta / scale;
    }

    public double toScreenX(double logicalX) {
        return offsetX + logicalX * scale;
    }

    public double toScreenY(double logicalY) {
        return offsetY + logicalY * scale;
    }
}
