package com.qolplus.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.qolplus.client.Freecam;
import com.qolplus.client.Freelook;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;

/** While freelooking or in freecam, moving the mouse turns the camera instead of your player. */
@Mixin(Entity.class)
public class FreelookTurnMixin {
	@Inject(method = "turn", at = @At("HEAD"), cancellable = true)
	private void qolplus$freelookTurn(double xo, double yo, CallbackInfo ci) {
		if (!((Object) this instanceof LocalPlayer)) return;
		if (Freecam.isActive()) {
			Freecam.turn(xo, yo);
			ci.cancel();
		} else if (Freelook.isActive()) {
			Freelook.turn(xo, yo);
			ci.cancel();
		}
	}
}
