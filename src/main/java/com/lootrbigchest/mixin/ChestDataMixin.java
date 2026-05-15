package com.lootrbigchest.mixin;

import com.lootrbigchest.LootrBigChest;
import com.lootrbigchest.LootrBigChestConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import noobanidus.mods.lootr.api.LootFiller;
import noobanidus.mods.lootr.data.ChestData;
import noobanidus.mods.lootr.data.SpecialChestInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ChestData.class, remap = false)
public abstract class ChestDataMixin {

    @Accessor("size")
    public abstract void setChestDataSize(int size);

    @Accessor("size")
    public abstract int getChestDataSize();

    @Inject(method = "setSize", at = @At("HEAD"), cancellable = true)
    private void onSetSize(int size, CallbackInfo ci) {
        if (LootrBigChestConfig.isExpanded()) {
            ci.cancel();
        }
    }

    @Inject(method = "createInventory(Lnet/minecraft/server/level/ServerPlayer;Lnoobanidus/mods/lootr/api/LootFiller;Lnet/minecraft/world/level/block/entity/RandomizableContainerBlockEntity;)Lnoobanidus/mods/lootr/data/SpecialChestInventory;",
            at = @At("HEAD"))
    private void beforeCreateInventory(ServerPlayer player, LootFiller filler,
                                        RandomizableContainerBlockEntity tile,
                                        CallbackInfoReturnable<SpecialChestInventory> cir) {
        if (!LootrBigChestConfig.isExpanded()) return;
        if (tile == null) return;

        int[] size = LootrBigChest.getOrCreateSize(tile, null);
        if (size[0] != 3 || size[1] != 9) {
            setChestDataSize(size[0] * size[1]);
        }
    }
}
