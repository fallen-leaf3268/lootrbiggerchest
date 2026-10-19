package com.lootrbiggerchest.mixin;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.vehicle.AbstractMinecartContainer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractMinecartContainer.class)
public interface AbstractMinecartContainerAccessor {

    @Accessor("itemStacks")
    void setItemStacks(NonNullList<ItemStack> value);
}
