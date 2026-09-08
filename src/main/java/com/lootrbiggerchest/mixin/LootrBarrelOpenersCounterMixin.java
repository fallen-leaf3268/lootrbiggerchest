package com.lootrbiggerchest.mixin;

import com.lootrbiggerchest.menu.LootrBiggerChestMenu;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import noobanidus.mods.lootr.block.entities.LootrBarrelBlockEntity;
import noobanidus.mods.lootr.data.SpecialChestInventory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "noobanidus.mods.lootr.block.entities.LootrBarrelBlockEntity$1")
public abstract class LootrBarrelOpenersCounterMixin {

    @Shadow(remap = false)
    @Final
    private LootrBarrelBlockEntity this$0;

    @Inject(method = "isOwnContainer(Lnet/minecraft/world/entity/player/Player;)Z",
            at = @At("HEAD"), cancellable = true)
    private void recognizeExpandedMenu(Player player,
                                       CallbackInfoReturnable<Boolean> cir) {
        if (!(player.containerMenu instanceof LootrBiggerChestMenu menu)) return;
        Container container = menu.getContainer();
        if (!(container instanceof SpecialChestInventory inventory)) return;
        if (inventory.getTileId() != null
                ? inventory.getTileId().equals(this$0.getTileId())
                : inventory.getBlockEntity(this$0.getLevel()) == this$0) {
            cir.setReturnValue(true);
        }
    }
}
