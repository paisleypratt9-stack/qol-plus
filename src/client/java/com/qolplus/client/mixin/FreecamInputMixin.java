package com.qolplus.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.qolplus.client.Freecam;

import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;

/** While Freecam is on, movement keys fly the camera, so your player stands still. */
@Mixin(KeyboardInput.class)
public class FreecamInputMixin extends ClientInput {
	@Inject(method = "tick", at = @At("RETURN"))
	private void qolplus$freezePlayer(CallbackInfo ci) {
		if (!Freecam.isActive()) return;
		keyPresses = Input.EMPTY;
		moveVector = Vec2.ZERO;
	}
}
