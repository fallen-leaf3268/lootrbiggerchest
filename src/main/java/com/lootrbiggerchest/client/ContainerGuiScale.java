package com.lootrbiggerchest.client;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public final class ContainerGuiScale {
    private static final Set<LootrBiggerChestScreen> OPEN_SCREENS =
            Collections.newSetFromMap(new IdentityHashMap<>());

    private ContainerGuiScale() {}

    public static void open(LootrBiggerChestScreen screen) {
        OPEN_SCREENS.add(screen);
        apply(Minecraft.getInstance());
    }

    public static void close(LootrBiggerChestScreen screen) {
        if (OPEN_SCREENS.remove(screen)) apply(Minecraft.getInstance());
    }

    public static void onWindowResize() {
        if (!OPEN_SCREENS.isEmpty()) updateScale(Minecraft.getInstance());
    }

    public static void onScreenInit(ScreenEvent.Init.Pre event) {
        Minecraft client = Minecraft.getInstance();
        var screen = event.getScreen();
        if (screen != client.screen) return;
        if (screen instanceof LootrBiggerChestScreen container) OPEN_SCREENS.add(container);
        if (OPEN_SCREENS.isEmpty()) return;
        apply(client);
        var window = client.getWindow();
        screen.width = window.getGuiScaledWidth();
        screen.height = window.getGuiScaledHeight();
    }

    private static void apply(Minecraft client) {
        if (!updateScale(client)) return;
        var window = client.getWindow();
        ClientHooks.resizeGuiLayers(client, window.getGuiScaledWidth(), window.getGuiScaledHeight());
    }

    private static boolean updateScale(Minecraft client) {
        var window = client.getWindow();
        double scale = window.calculateScale(client.options.guiScale().get(), client.isEnforceUnicode());
        for (LootrBiggerChestScreen screen : OPEN_SCREENS) {
            scale = ContainerScreenLayout.fitScale(window.getWidth(), window.getHeight(),
                    scale, screen.getXSize(), screen.getYSize());
        }
        if (Double.compare(window.getGuiScale(), scale) == 0) return false;
        window.setGuiScale(scale);
        return true;
    }
}
