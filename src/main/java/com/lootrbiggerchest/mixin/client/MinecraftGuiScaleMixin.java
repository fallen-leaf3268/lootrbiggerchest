package com.lootrbiggerchest.mixin.client;

import com.lootrbiggerchest.client.ContainerGuiScale;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftGuiScaleMixin {
    @Inject(method = "resizeDisplay", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/Window;setGuiScale(D)V", shift = At.Shift.AFTER))
    private void lootrbiggerchest$fitGuiScale(CallbackInfo callback) {
        ContainerGuiScale.onWindowResize();
    }
}
