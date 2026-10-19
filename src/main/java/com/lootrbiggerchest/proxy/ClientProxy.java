package com.lootrbiggerchest.proxy;

import com.lootrbiggerchest.LootrBiggerChest;
import com.lootrbiggerchest.client.LootrBiggerChestScreen;
import com.lootrbiggerchest.client.ClientGuiCommands;
import com.lootrbiggerchest.client.ContainerGuiTextures;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public class ClientProxy implements IProxy {

    public ClientProxy() {
        MinecraftForge.EVENT_BUS.addListener(ClientGuiCommands::onRegisterCommands);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::registerReloadListeners);
    }

    private void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(ContainerGuiTextures.INSTANCE);
    }

    @Override
    public void registerScreens() {
        MenuScreens.register(LootrBiggerChest.CHEST_MENU.get(), LootrBiggerChestScreen::new);
        MenuScreens.register(LootrBiggerChest.BARREL_MENU.get(), LootrBiggerChestScreen::new);
        MenuScreens.register(LootrBiggerChest.SHULKER_MENU.get(), LootrBiggerChestScreen::new);
        MenuScreens.register(LootrBiggerChest.MINECART_MENU.get(), LootrBiggerChestScreen::new);
    }
}
