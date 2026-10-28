package com.qolplus.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/** Where the container's window sits on screen, so the search bar and slot highlights line up with it. */
@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccessor {
	@Accessor("leftPos")
	int qolplus$getLeftPos();

	@Accessor("topPos")
	int qolplus$getTopPos();

	@Accessor("imageWidth")
	int qolplus$getImageWidth();
}
