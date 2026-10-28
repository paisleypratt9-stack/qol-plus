package com.qolplus.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.platform.InputConstants;
import com.qolplus.client.HudWidgets;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/** Counts attack and use presses for the CPS counter, whatever key or mouse button they're bound to. */
@Mixin(KeyMapping.class)
public class KeyClickMixin {
	@Inject(method = "click", at = @At("HEAD"))
	private static void qolplus$countClick(InputConstants.Key key, CallbackInfo ci) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.gui.screen() != null) return;
		if (mc.options.keyAttack.matches(key)) HudWidgets.onClick(0);
		else if (mc.options.keyUse.matches(key)) HudWidgets.onClick(1);
	}
}
