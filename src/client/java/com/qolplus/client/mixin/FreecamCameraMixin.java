package com.qolplus.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.qolplus.client.Freecam;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;

/** While Freecam is on, the camera sits at the freecam spot and counts as detached so your player is drawn. */
@Mixin(Camera.class)
public abstract class FreecamCameraMixin {
	@Shadow
	private boolean detached;

	@Shadow
	protected abstract void setRotation(float yRot, float xRot);

	@Shadow
	protected abstract void setPosition(Vec3 position);

	@Inject(method = "alignWithEntity", at = @At("TAIL"))
	private void qolplus$freecam(float partialTicks, CallbackInfo ci) {
		if (!Freecam.isActive()) return;
		detached = true;
		setRotation(Freecam.yaw(), Freecam.pitch());
		setPosition(Freecam.frame());
	}
}
