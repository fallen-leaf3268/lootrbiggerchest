package com.lootrbiggerchest.proxy;

import com.lootrbiggerchest.LootrBiggerChest;
import com.lootrbiggerchest.client.LootrBiggerChestScreen;
import net.minecraft.client.gui.screens.MenuScreens;

public class ClientProxy implements IProxy {

    @Override
    public void registerScreens() {
        MenuScreens.register(LootrBiggerChest.CHEST_MENU.get(), LootrBiggerChestScreen::new);
        MenuScreens.register(LootrBiggerChest.BARREL_MENU.get(), LootrBiggerChestScreen::new);
        MenuScreens.register(LootrBiggerChest.SHULKER_MENU.get(), LootrBiggerChestScreen::new);
        MenuScreens.register(LootrBiggerChest.MINECART_MENU.get(), LootrBiggerChestScreen::new);
    }
}
