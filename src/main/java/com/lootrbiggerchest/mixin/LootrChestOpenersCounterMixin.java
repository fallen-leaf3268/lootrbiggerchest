package com.lootrbiggerchest.mixin;

import com.lootrbiggerchest.menu.LootrBiggerChestMenu;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import noobanidus.mods.lootr.block.entities.LootrChestBlockEntity;
import noobanidus.mods.lootr.data.SpecialChestInventory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "noobanidus.mods.lootr.block.entities.LootrChestBlockEntity$1")
public abstract class LootrChestOpenersCounterMixin {

    @Shadow(remap = false)
    @Final
    private LootrChestBlockEntity this$0;

    @Inject(method = "isOwnContainer(Lnet/minecraft/world/entity/player/Player;)Z",
            at = @At("HEAD"), cancellable = true)
    private void recognizeExpandedMenu(Player player,
                                       CallbackInfoReturnable<Boolean> cir) {
        if (!(player.containerMenu instanceof LootrBiggerChestMenu menu)) return;
        Container container = menu.getContainer();
        if (container instanceof SpecialChestInventory inventory
                && this$0.getTileId().equals(inventory.getTileId())) {
            cir.setReturnValue(true);
        }
    }
}
