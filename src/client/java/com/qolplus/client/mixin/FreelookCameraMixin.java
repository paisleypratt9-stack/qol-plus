package com.qolplus.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.qolplus.client.Freelook;

import net.minecraft.client.Camera;

/** While freelooking, the camera uses the freelook angles instead of where the player is facing. */
@Mixin(Camera.class)
public class FreelookCameraMixin {
	@ModifyArg(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V"), index = 0)
	private float qolplus$freelookYaw(float yRot) {
		return Freelook.isActive() ? Freelook.yaw() : yRot;
	}

	@ModifyArg(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V"), index = 1)
	private float qolplus$freelookPitch(float xRot) {
		return Freelook.isActive() ? Freelook.pitch() : xRot;
	}
}
