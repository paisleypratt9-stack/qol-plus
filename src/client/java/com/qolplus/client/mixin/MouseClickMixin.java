package com.qolplus.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.qolplus.client.Zoom;

import net.minecraft.client.MouseHandler;

/** Scrolling while zoomed changes the zoom instead of the hotbar slot. */
@Mixin(MouseHandler.class)
public class MouseClickMixin {
	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void qolplus$zoomScroll(long handle, double xoffset, double yoffset, CallbackInfo ci) {
		if (Zoom.onScroll(yoffset)) ci.cancel();
	}
}
