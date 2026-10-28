package com.qolplus.client;

import com.qolplus.QolConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;

/** Hold the Zoom keybind to zoom. Eases in and out smoothly, scroll while zoomed to zoom further, optional smooth camera. */
public final class Zoom {
	private static final double MIN_ZOOM = 1.5;
	private static final double MAX_ZOOM = 50;

	private static double current = 1;
	private static double scroll = 1;
	private static long lastFrame;
	private static boolean wasZooming;
	private static boolean changedSmoothCamera;
	private static boolean savedSmoothCamera;

	private Zoom() {
	}

	/** Called by the camera mixin every frame with the normal FOV; returns the zoomed one. */
	public static float apply(float fov) {
		QolConfig c = QolConfig.get();
		long now = Util.getMillis();
		double seconds = lastFrame == 0 ? 0 : Math.min(0.1, (now - lastFrame) / 1000.0);
		lastFrame = now;

		boolean zooming = QolPlusClient.isZooming();
		if (!zooming) scroll = 1;
		double target = zooming ? Math.clamp(c.zoomFactor * scroll, MIN_ZOOM, MAX_ZOOM) : 1;
		current = c.zoomSmooth ? current + (target - current) * (1 - Math.exp(-seconds * 12)) : target;
		if (Math.abs(current - 1) < 0.001) current = 1;

		if (zooming != wasZooming) {
			wasZooming = zooming;
			updateSmoothCamera(zooming && c.zoomSmoothCamera);
		}
		return (float) (fov / current);
	}

	private static void updateSmoothCamera(boolean on) {
		Minecraft mc = Minecraft.getInstance();
		if (on && !changedSmoothCamera) {
			savedSmoothCamera = mc.options.smoothCamera;
			mc.options.smoothCamera = true;
			changedSmoothCamera = true;
		} else if (!on && changedSmoothCamera) {
			mc.options.smoothCamera = savedSmoothCamera;
			changedSmoothCamera = false;
		}
	}

	/** Called by the scroll mixin; returns true when the scroll was used for zooming (so the hotbar doesn't move). */
	public static boolean onScroll(double amount) {
		QolConfig c = QolConfig.get();
		if (!c.zoomScroll || !QolPlusClient.isZooming() || amount == 0) return false;
		double total = Math.clamp(c.zoomFactor * scroll * Math.pow(1.2, Math.signum(amount)), MIN_ZOOM, MAX_ZOOM);
		scroll = total / c.zoomFactor;
		return true;
	}
}
