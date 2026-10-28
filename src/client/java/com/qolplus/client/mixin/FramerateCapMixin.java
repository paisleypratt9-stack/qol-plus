package com.qolplus.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.blaze3d.platform.FramerateLimitTracker;
import com.qolplus.QolConfig;

import net.minecraft.client.Minecraft;

/** FPS Caps: lower FPS while the game is in the background, and while a menu or inventory is open in a world. */
@Mixin(FramerateLimitTracker.class)
public class FramerateCapMixin {
	@Inject(method = "getFramerateLimit", at = @At("RETURN"), cancellable = true)
	private void qolplus$cap(CallbackInfoReturnable<Integer> cir) {
		QolConfig c = QolConfig.get();
		if (!c.fpsCaps) return;
		Minecraft mc = Minecraft.getInstance();
		int limit = cir.getReturnValue();
		if (!mc.getWindow().isFocused()) limit = Math.min(limit, c.backgroundFps);
		else if (mc.level != null && mc.gui.screen() != null) limit = Math.min(limit, c.menuFps);
		cir.setReturnValue(limit);
	}
}
