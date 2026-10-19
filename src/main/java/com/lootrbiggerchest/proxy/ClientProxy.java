package com.lootrbiggerchest.proxy;

import com.lootrbiggerchest.LootrBiggerChest;
import com.lootrbiggerchest.client.ClientGuiCommands;
import com.lootrbiggerchest.client.ContainerGuiScale;
import com.lootrbiggerchest.client.ContainerGuiTextures;
import com.lootrbiggerchest.client.LootrBiggerChestScreen;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;

public class ClientProxy {

    public ClientProxy(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, ContainerGuiScale::onScreenInit);
        NeoForge.EVENT_BUS.addListener(ClientGuiCommands::onRegisterCommands);
        modBus.addListener(this::registerReloadListeners);
        modBus.addListener(this::registerScreens);
    }

    private void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(ContainerGuiTextures.INSTANCE);
    }

    private void registerScreens(RegisterMenuScreensEvent event) {
        event.register(LootrBiggerChest.CHEST_MENU.get(), LootrBiggerChestScreen::new);
        event.register(LootrBiggerChest.BARREL_MENU.get(), LootrBiggerChestScreen::new);
        event.register(LootrBiggerChest.SHULKER_MENU.get(), LootrBiggerChestScreen::new);
        event.register(LootrBiggerChest.MINECART_MENU.get(), LootrBiggerChestScreen::new);
    }
}
