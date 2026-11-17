package com.example.bountyhighlighter.mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(HandledScreen.class)
public interface HandledScreenAccessor {
    @Accessor("x") int bh$getX();
    @Accessor("y") int bh$getY();
    @Invoker("getSlotAt") Slot bh$getSlotAt(double x, double y);
}
