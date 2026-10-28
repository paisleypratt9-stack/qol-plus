package com.qolplus.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.qolplus.QolConfig;

import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;

/** Fullbright: lights the world like a permanent night vision effect, without the potion. */
@Mixin(LightmapRenderStateExtractor.class)
public class FullbrightMixin {
	@Inject(method = "extract", at = @At("RETURN"))
	private void qolplus$fullbright(LightmapRenderState renderState, float partialTicks, CallbackInfo ci) {
		if (QolConfig.get().fullbright && renderState.needsUpdate) renderState.nightVisionEffectIntensity = 1.0f;
	}
}
