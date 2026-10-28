package com.qolplus.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.qolplus.client.Freelook;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;

/** While freelooking, moving the mouse turns the camera instead of your player. */
@Mixin(Entity.class)
public class FreelookTurnMixin {
	@Inject(method = "turn", at = @At("HEAD"), cancellable = true)
	private void qolplus$freelookTurn(double xo, double yo, CallbackInfo ci) {
		if ((Object) this instanceof LocalPlayer && Freelook.isActive()) {
			Freelook.turn(xo, yo);
			ci.cancel();
		}
	}
}
