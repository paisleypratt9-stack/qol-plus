package com.qolplus.client;

import com.qolplus.QolConfig;

import net.minecraft.client.Minecraft;

/**
 * Auto Render Distance (lowers render distance when FPS drops below the target, raises it again when there's room)
 * and No Entity Shadows (switches off vanilla's shadow option while on, puts it back when turned off).
 */
public final class Performance {
	private static int lowSeconds;
	private static int highSeconds;
	private static int tickCounter;
	private static Boolean savedShadows;

	private Performance() {
	}

	public static void tick(Minecraft mc) {
		QolConfig c = QolConfig.get();
		updateShadows(mc, c.noEntityShadows);
		if (!c.autoRenderDistance || mc.level == null || mc.gui.screen() != null) return;
		if (++tickCounter < 20) return; // check once a second
		tickCounter = 0;

		int fps = mc.getFps();
		int distance = mc.options.renderDistance().get();
		lowSeconds = fps < c.targetFps ? lowSeconds + 1 : 0;
		highSeconds = fps > c.targetFps * 1.4 ? highSeconds + 1 : 0;

		// Drop quickly when it lags, climb back slowly so it doesn't bounce up and down.
		if (lowSeconds >= 2 && distance > c.minRenderDistance) {
			mc.options.renderDistance().set(distance - 1);
			lowSeconds = 0;
		} else if (highSeconds >= 6 && distance < c.maxRenderDistance) {
			mc.options.renderDistance().set(distance + 1);
			highSeconds = 0;
		}
	}

	private static void updateShadows(Minecraft mc, boolean hide) {
		if (hide && savedShadows == null) {
			savedShadows = mc.options.entityShadows().get();
			mc.options.entityShadows().set(false);
		} else if (!hide && savedShadows != null) {
			mc.options.entityShadows().set(savedShadows);
			savedShadows = null;
		}
	}
}
