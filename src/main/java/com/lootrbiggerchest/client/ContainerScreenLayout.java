package com.lootrbiggerchest.client;

public final class ContainerScreenLayout {

    private ContainerScreenLayout() {
    }

    public static double fitScale(int framebufferWidth, int framebufferHeight, double preferredScale,
                                  int imageWidth, int imageHeight) {
        return Math.min(preferredScale, Math.min(
                framebufferWidth / (imageWidth + 16.0),
                framebufferHeight / (imageHeight + 16.0)));
    }
}
