package com.qolplus.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.qolplus.client.Zoom;

import net.minecraft.client.Camera;

/** Applies the (smooth) zoom to the camera's field of view. */
@Mixin(Camera.class)
public class CameraZoomMixin {
	@Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
	private void qolplus$zoom(float partialTicks, CallbackInfoReturnable<Float> cir) {
		cir.setReturnValue(Zoom.apply(cir.getReturnValue()));
	}
}
