package com.qolplus.client.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.mojang.blaze3d.platform.NativeImage;
import com.qolplus.client.HitColor;

import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Lets Hit Color repaint the red flash mobs get when they're hurt (the top half of this small texture). */
@Mixin(OverlayTexture.class)
public class HitColorMixin implements HitColor.Recolorable {
	@Shadow
	@Final
	private DynamicTexture texture;

	@Override
	public void qolplus$setHurtColor(int argb) {
		NativeImage pixels = texture.getPixels();
		if (pixels == null) return;
		for (int y = 0; y < 8; y++) {
			for (int x = 0; x < 16; x++) pixels.setPixel(x, y, argb);
		}
		texture.upload();
	}
}
