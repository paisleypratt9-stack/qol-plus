package com.qolplus.client;

import com.qolplus.QolConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

/** Hit Color: changes the red flash mobs and players get when hurt to any color. */
public final class HitColor {
	/** Vanilla's hurt tint (red, about 70% strong). */
	private static final int VANILLA = 0xB2FF0000;

	/** Added to the overlay texture by HitColorMixin. */
	public interface Recolorable {
		void qolplus$setHurtColor(int argb);
	}

	private static int applied = VANILLA;

	private HitColor() {
	}

	public static void tick(Minecraft mc) {
		QolConfig c = QolConfig.get();
		int wanted = c.hitColor
				? (c.hitOpacity * 255 / 100) << 24 | Mth.hsvToRgb(c.hitHue / 360f, 0.85f, 1.0f)
				: VANILLA;
		if (wanted == applied) return;
		((Recolorable) mc.gameRenderer.overlayTexture()).qolplus$setHurtColor(wanted);
		applied = wanted;
	}
}
